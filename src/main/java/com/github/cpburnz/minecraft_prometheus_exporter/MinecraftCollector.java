package com.github.cpburnz.minecraft_prometheus_exporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.Histogram;

/**
 * This class collects stats from the Minecraft server for export.
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
	private final ConcurrentHashMap<RegistryKey<World>, Histogram.Timer> dim_tick_timers;

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
			List<MetricFamilySamples> server_tick = this.server_tick_seconds.collect();
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
				+ server_tick.size()
				+ 1 /* dim_chunks_loaded */
				+ dim_ticks.size()
			);
			metrics.add(player_list);
			if (entities != null) {
				metrics.add(entities);
			}
			metrics.addAll(server_tick);
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
		for (ServerWorld world : this.mc_server.getAllLevels()) {
			String id = Integer.toString(getDimensionId(world.dimension()));
			String name = world.dimension().location().getPath();
			int loaded = world.getChunkSource().getLoadedChunksCount();
			metric.addMetric(Arrays.asList(id, name), loaded);
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
		for (ServerWorld world : this.mc_server.getAllLevels()) {
			// Get dimension info.
			RegistryKey<World> dim_reg = world.dimension();
			int dim_id = getDimensionId(dim_reg);
			String dim = dim_reg.location().getPath();

			// Get entity info.
			for (Entity entity : world.getAllEntities()) {
				if (!(entity instanceof PlayerEntity)) {
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
		for (Map.Entry<EntityKey, Integer> entry : entity_totals.entrySet()) {
			EntityKey entity_key = entry.getKey();
			double total = entry.getValue();
			String dim_id_str = Integer.toString(entity_key.dim_id);
			metric.addMetric(
				Arrays.asList(entity_key.dim, dim_id_str, entity_key.type), total
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
		for (ServerPlayerEntity player : this.mc_server.getPlayerList().getPlayers()) {
			// Get player profile.
			GameProfile profile = player.getGameProfile();

			// Get player info.
			// - WARNING: Either "id" or "name" can be null in Minecraft 1.19 and
			//   earlier.
			String id_str = Objects.toString(profile.getId(), "");
			String name = ObjectUtils.defaultIfNull(profile.getName(), "");
			metric.addMetric(Arrays.asList(id_str, name), 1);
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
	 * @param dim The dimension (world).
	 */
	private static int getDimensionId(RegistryKey<World> dim) {
		if (dim == World.OVERWORLD) {
			return 0;
		} else if (dim == World.END) {
			return 1;
		} else if (dim == World.NETHER) {
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
			Arrays.asList("id", "name")
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
			Arrays.asList("dim", "dim_id", "type")
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
			Arrays.asList("id", "name")
		);
	}

	/**
	 * Record when a dimension tick begins.
	 *
	 * @param dim The dimension.
	 */
	public void startDimensionTick(RegistryKey<World> dim) {
		// Get dimension name.
		String name = dim.location().getPath();

		// Check for forgotten timer.
		Histogram.Timer timer = this.dim_tick_timers.get(dim);
		if (timer != null) {
			switch (this.config.collector_mc_dimension_tick_errors) {
				case IGNORE:
					// Ignore error.
					break;

				case LOG:
					LOG.debug(
						"Dimension {} tick started before stopping previous tick.", name
					);
					break;

				case STRICT:
					throw new IllegalStateException(
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
	public void stopDimensionTick(RegistryKey<World> dim) {
		// Get dimension name.
		String name = dim.location().getPath();

		// Get active timer.
		Histogram.Timer timer = this.dim_tick_timers.remove(dim);
		if (timer == null) {
			switch (this.config.collector_mc_dimension_tick_errors) {
				case IGNORE:
					// Ignore error.
					break;

				case LOG:
					LOG.debug(
						"Dimension {} tick stopped without an active tick.", name
					);
					break;

				case STRICT:
					throw new IllegalStateException(
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
	 */
	private static final class EntityKey {

		public final String dim;
		public final int dim_id;
		public final String type;

		/**
		 * Constructs the instance.
		 *
		 * @param dim The dimension name.
		 * @param dim_id The dimension id.
		 * @param type The entity type.
		 */
		EntityKey(String dim, int dim_id, String type) {
			this.dim = dim;
			this.dim_id = dim_id;
			this.type = type;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			} if (!(obj instanceof EntityKey)) {
				return false;
			}

			EntityKey other = (EntityKey)obj;
			return (
				Objects.equals(other.dim, this.dim)
				&& Objects.equals(other.dim_id, this.dim_id)
				&& Objects.equals(other.type, this.type)
			);
		}

		@Override
		public int hashCode() {
			return Objects.hash(dim, dim_id, type);
		}
	}
}
