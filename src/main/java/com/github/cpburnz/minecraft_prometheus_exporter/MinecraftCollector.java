package com.github.cpburnz.minecraft_prometheus_exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.Histogram;

/**
 * The MinecraftCollector class collects stats from the Minecraft server for
 * export.
 */
public class MinecraftCollector extends Collector implements Collector.Describable {

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

	/**
	 * The histogram buckets to use for ticks.
	 */
	private static final double[] TICK_BUCKETS = new double[] {
		0.01,
		0.025,
		0.05,
		0.10,
		0.25,
		0.5,
		1.0,
	};

	/**
	 * The server configuration.
	 */
	private final ServerConfig config;

	/**
	 * Histogram metrics for dimension tick timing.
	 */
	private final Histogram dim_tick_seconds;

	/**
	 * Maps each dimension to its active timer when timing a dimension (world)
	 * tick.
	 *
	 * <p>Track each dimension separately in order to support multi-threading.
	 * Minecraft (as of at least 1.20) still does not run server-side dimension
	 * ticks in multiple threads. However, some mods do for their custom
	 * dimensions (e.g., Vault Hunters).</p>
	 */
	private final ConcurrentHashMap<ResourceKey<Level>, Histogram.Timer> dim_tick_timers;

	/**
	 * The Minecraft server.
	 */
	private final MinecraftServer mc_server;

	/**
	 * Histogram metrics for server tick timing.
	 */
	private final Histogram server_tick_seconds;

	/**
	 * The active timer when timing a server tick.
	 */
	@Nullable
	private Histogram.Timer server_tick_timer;

	/**
	 * Constructs the instance.
	 *
	 * @param config The mod configuration.
	 * @param mc_server The Minecraft server.
	 */
	public MinecraftCollector(ServerConfig config, MinecraftServer mc_server) {
		this.config = config;
		this.dim_tick_timers = new ConcurrentHashMap<>(3);
		this.mc_server = mc_server;

		// Setup server metrics.
		this.server_tick_seconds = Histogram.build()
			.buckets(TICK_BUCKETS)
			.name("mc_server_tick_seconds")
			.help("Stats on server tick times.")
			.create();

		this.dim_tick_seconds = Histogram.build()
			.buckets(TICK_BUCKETS)
			.name("mc_dimension_tick_seconds")
			.labelNames("id", "name")
			.help("Stats on dimension tick times.")
			.create();
	}

	/**
	 * Return all metrics for the collector.
	 *
	 * @return The collector metrics.
	 */
	@Override
	public List<MetricFamilySamples> collect() {
		try {
			// Collect metrics.
			MetricFamilySamples player_list = this.collectPlayerList();
			List<MetricFamilySamples> server_ticks = this.server_tick_seconds.collect();
			MetricFamilySamples dim_chunks_loaded = this.collectDimensionChunksLoaded();
			List<MetricFamilySamples> dim_ticks = this.dim_tick_seconds.collect();

			MetricFamilySamples entities = null;
			int entities_init = 0;
			if (this.config.collector_mc_entities) {
				entities = collectEntitiesTotal();
				entities_init = 1;
			}

			// Aggregate metrics.
			ArrayList<MetricFamilySamples> metrics = new ArrayList<>(
				1 /* player_list */
				+ entities_init
				+ server_ticks.size()
				+ 1 /* dim_chunks_loaded */
				+ dim_ticks.size()
			);
			metrics.add(player_list);
			if (entities != null) {
				metrics.add(entities);
			}
			metrics.addAll(server_ticks);
			metrics.add(dim_chunks_loaded);
			metrics.addAll(dim_ticks);

			return metrics;
		} catch (Exception e) {
			LOG.error("Failed to collect metrics.", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Get the number of loaded dimension chunks.
	 *
	 * @return The dimension chunks loaded metric.
	 */
	private GaugeMetricFamily collectDimensionChunksLoaded() {
		GaugeMetricFamily metric = newDimensionChunksLoadedMetric();
		for (ServerLevel world : this.mc_server.getAllLevels()) {
			ResourceKey<Level> dim = world.dimension();
			String id_str = Integer.toString(getDimensionId(dim));
			String name = dim.location().getPath();
			int loaded = world.getChunkSource().getLoadedChunksCount();
			metric.addMetric(List.of(id_str, name), loaded);
		}
		return metric;
	}

	/**
	 * Get the entities per dimension.
	 *
	 * @return The entities total metric.
	 */
	private GaugeMetricFamily collectEntitiesTotal() {
		// Aggregate stats.
		HashMap<EntityKey, Integer> entity_totals = new HashMap<>();
		for (ServerLevel world : this.mc_server.getAllLevels()) {
			// Get dimension info.
			ResourceKey<Level> dim_resource = world.dimension();
			int dim_id = getDimensionId(dim_resource);
			String dim = dim_resource.location().getPath();

			// Get entity info.
			for (Entity entity : world.getAllEntities()) {
				if (!(entity instanceof Player)) {
					// Get entity type.
					String entity_type;
					if (entity instanceof ItemEntity) {
						// Merge items. Do not count items individually by type.
						entity_type = "Item";
					} else {
						entity_type = entity.getName().getString();
					}

					EntityKey entity_key = new EntityKey(dim, dim_id, entity_type);
					entity_totals.merge(entity_key, 1, Integer::sum);
				}
			}
		}

		// Record metrics.
		GaugeMetricFamily metric = newEntitiesTotalMetric();
		for (var entry : entity_totals.entrySet()) {
			EntityKey entity_key = entry.getKey();
			double total = entry.getValue();
			String dim_id_str = Integer.toString(entity_key.dim_id);
			metric.addMetric(
				List.of(entity_key.dim, dim_id_str, entity_key.type), total
			);
		}
		return metric;
	}

	/**
	 * Get the active players.
	 *
	 * @return The player list metric.
	 */
	private GaugeMetricFamily collectPlayerList() {
		GaugeMetricFamily metric = newPlayerListMetric();
		for (ServerPlayer player : this.mc_server.getPlayerList().getPlayers()) {
			// Get player profile.
			GameProfile profile = player.getGameProfile();

			// Get player info.
			// - WARNING: Either "id" or "name" can be null.
			String id_str = Objects.toString(profile.getId(), "");
			String name = ObjectUtils.defaultIfNull(profile.getName(), "");

			metric.addMetric(List.of(id_str, name), 1);
		}
		return metric;
	}

	/**
	 * Return all metric descriptions for the collector.
	 *
	 * @return The collector metric descriptions.
	 */
	@Override
	public List<MetricFamilySamples> describe() {
		// Aggregate metric descriptions.
		ArrayList<MetricFamilySamples> descs = new ArrayList<>();
		descs.add(newPlayerListMetric());
		if (this.config.collector_mc_entities) {
			descs.add(newEntitiesTotalMetric());
		}
		descs.addAll(this.server_tick_seconds.describe());
		descs.add(newDimensionChunksLoadedMetric());
		descs.addAll(this.dim_tick_seconds.describe());
		return descs;
	}

	/**
	 * Get the dimension id.
	 *
	 * <p>With the new version of Minecraft, 1.16, a dimension no longer has an
	 * id. However, to keep backward compatibility with older versions of the
	 * exporter, we need this method. Vanilla dimensions use fixed id values (-1,
	 * 0, 1), and the id of a custom dimension is now calculated from the
	 * dimension name.</p>
	 *
	 * @param dim The dimension.
	 */
	private static int getDimensionId(ResourceKey<Level> dim) {
		if (dim.equals(Level.OVERWORLD)) {
			return 0;
		} else if (dim.equals(Level.END)) {
			return 1;
		} else if (dim.equals(Level.NETHER)) {
			return -1;
		} else {
			String name = dim.location().getPath();
			return name.hashCode();
		}
	}

	/**
	 * Create a new metric for the dimension chunks loaded.
	 *
	 * @return The dimension chunks loaded metric.
	 */
	private static GaugeMetricFamily newDimensionChunksLoadedMetric() {
		return new GaugeMetricFamily(
			"mc_dimension_chunks_loaded",
			"The number of loaded dimension chunks.",
			List.of("id", "name")
		);
	}

	/**
	 * Create a new metric for the total entities.
	 *
	 * @return The entities total metric.
	 */
	private static GaugeMetricFamily newEntitiesTotalMetric() {
		return new GaugeMetricFamily(
			"mc_entities_total",
			"The number of entities in each dimension by type.",
			List.of("dim", "dim_id", "type")
		);
	}

	/**
	 * Create a new metric for the player list.
	 *
	 * @return The player list metric.
	 */
	private static GaugeMetricFamily newPlayerListMetric() {
		return new GaugeMetricFamily(
			"mc_player_list",
			"The players connected to the server.",
			List.of("id", "name")
		);
	}

	/**
	 * Record when a dimension tick begins.
	 *
	 * @param dim The dimension.
	 */
	public void startDimensionTick(ResourceKey<Level> dim) {
		// Get dimension name.
		String name = dim.location().getPath();

		// Check for forgotten timer.
		Histogram.Timer timer = this.dim_tick_timers.get(dim);
		if (timer != null) {
			switch (this.config.collector_mc_dimension_tick_errors) {
				case IGNORE -> {}  // Ignore error.
				case LOG -> LOG.debug(
					"Dimension {} tick started before stopping previous tick.",
					name
				);
				case STRICT -> throw new IllegalStateException(
					"Dimension " + name + " tick started before stopping previous tick."
				);
			}

			// Stop forgotten timer.
			timer.close();
			timer = null;
		}

		// Start timer for tick.
		String id_str = Integer.toString(getDimensionId(dim));
		timer = this.dim_tick_seconds.labels(id_str, name).startTimer();
		this.dim_tick_timers.put(dim, timer);
	}

	/**
	 * Record when a server tick begins.
	 */
	public void startServerTick() {
		if (this.server_tick_timer != null) {
			throw new IllegalStateException(
				"Server tick started before stopping previous tick."
			);
		}

		this.server_tick_timer = this.server_tick_seconds.startTimer();
	}

	/**
	 * Record when a dimension tick finishes.
	 *
	 * @param dim The dimension.
	 */
	public void stopDimensionTick(ResourceKey<Level> dim) {
		// Get dimension name.
		String name = dim.location().getPath();

		// Get active timer.
		Histogram.Timer timer = this.dim_tick_timers.remove(dim);
		if (timer == null) {
			switch (this.config.collector_mc_dimension_tick_errors) {
				case IGNORE -> {}  // Ignore error.
				case LOG -> LOG.debug(
					"Dimension {} tick stopped without an active tick.",
					name
				);
				case STRICT -> throw new IllegalStateException(
					"Dimension " + name + " tick stopped without an active tick."
				);
			}

			// No timer to stop.
			return;
		}

		// Record duration of tick.
		timer.close();
	}

	/**
	 * Record when a server tick finishes.
	 */
	public void stopServerTick() {
		if (this.server_tick_timer == null) {
			throw new IllegalStateException(
				"Server tick stopped without an active tick."
			);
		}

		this.server_tick_timer.observeDuration();
		this.server_tick_timer = null;
	}

	/**
	 * The EntityKey class is used to count entities per dimension.
	 *
	 * @param dim The dimension name.
	 * @param dim_id The dimension id.
	 * @param type The entity type.
	 */
	private record EntityKey(String dim, int dim_id, String type) {
		// Empty.
	}
}
