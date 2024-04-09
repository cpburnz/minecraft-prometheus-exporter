package com.github.cpburnz.minecraft_prometheus_exporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
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
	 * The mod configuration.
	 */
	private final Config config;

	/**
	 * The active dimension id being timed.
	 */
	@Nullable
	private Integer dim_tick_id;

	/**
	 * Histogram metrics for dimension tick timing.
	 */
	private final Histogram dim_tick_seconds;

	/**
	 * The active timer when timing a dimension tick.
	 */
	@Nullable
	private Histogram.Timer dim_tick_timer;

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
	public MinecraftCollector(Config config, MinecraftServer mc_server) {
		this.config = config;
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
				+ 1 /* dimension_chunks_loaded */
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
		for (WorldServer world : DimensionManager.getWorlds()) {
			String id_str = Integer.toString(world.provider.dimensionId);
			String name = world.provider.getDimensionName();
			int loaded = world.getChunkProvider().getLoadedChunkCount();
			metric.addMetric(Arrays.asList(id_str, name), loaded);
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
		TObjectIntHashMap<EntityKey> entity_totals = new TObjectIntHashMap<>();
		for (WorldServer world : mc_server.worldServers) {
			// Get world info.
			int dim_id = world.provider.dimensionId;
			String dim = world.provider.getDimensionName();

			// Get entity info.
			List loaded_entities = world.loadedEntityList;
			for (int i = loaded_entities.size(); i-- > 0; ) {
				Object entityObj = loaded_entities.get(i);
				if (entityObj instanceof Entity && !(entityObj instanceof EntityPlayer)) {
					Entity entity = (Entity)entityObj;

					// Get entity type.
					String entity_type = EntityList.getEntityString(entity);
					if (entity_type == null && entity instanceof IMob) {
						entity_type = entity.getClass().getName();
					}

					if (entity_type != null) {
						int entity_id = EntityList.getEntityID(entity);
						EntityKey entity_key = new EntityKey(
							dim, dim_id, entity_id, entity_type
						);
						entity_totals.adjustOrPutValue(entity_key, 1, 1);
					}
				}
			}
		}

		// Record metrics.
		GaugeMetricFamily metric = newEntitiesTotalMetric();
		for (EntityKey entity_key : entity_totals.keySet()) {
			double total = entity_totals.get(entity_key);
			String dim_id_str = Integer.toString(entity_key.dim_id);
			String id_str = Integer.toString(entity_key.id);
			metric.addMetric(
				Arrays.asList(entity_key.dim, dim_id_str, id_str, entity_key.type),
				total
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
		for (Object playerObj : this.mc_server.getConfigurationManager().playerEntityList) {
			// Get player profile.
			EntityPlayerMP player = (EntityPlayerMP)playerObj;
			GameProfile profile = player.getGameProfile();

			// Get player info.
			String id_str = "";
			UUID id = profile.getId();
			if (id != null) {
				id_str = id.toString();
			}

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
		descs.add(newEntitiesTotalMetric());
		descs.addAll(this.server_tick_seconds.describe());
		descs.add(newDimensionChunksLoadedMetric());
		descs.addAll(this.dim_tick_seconds.describe());
		return descs;
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
			Arrays.asList("id", "name"));
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
			Arrays.asList("dim", "dim_id", "id", "type")
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
	 * @param dim The dimension type.
	 */
	public void startDimensionTick(WorldProvider dim) {
		int id = dim.dimensionId;
		if (this.dim_tick_timer != null) {
			throw new IllegalStateException(
				"Dimension " + id + " tick started before stopping previous tick for "
				+ "dimension " + this.dim_tick_id + "."
			);
		}

		String id_str = Integer.toString(id);
		String name = dim.getDimensionName();
		this.dim_tick_id = id;
		this.dim_tick_timer = this.dim_tick_seconds.labels(id_str, name)
			.startTimer();
	}

	/**
	 * Record when a dimension tick finishes.
	 *
	 * @param dim The dimension type.
	 */
	public void stopDimensionTick(WorldProvider dim) {
		int id = dim.dimensionId;
		if (this.dim_tick_timer == null) {
			throw new IllegalStateException(
				"Dimension " + id + " tick stopped without an active tick."
			);
		} else if (this.dim_tick_id != null && this.dim_tick_id != id) {
			throw new IllegalStateException(
				"Dimension " + id + " tick stopped while in an active tick for "
				+ "dimension " + this.dim_tick_id + "."
			);
		}

		this.dim_tick_timer.observeDuration();
		this.dim_tick_timer = null;
		this.dim_tick_id = null;
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
	 * Record when a server tick finishes.
	 */
	public void stopServerTick() {
		if (this.server_tick_timer == null) {
			throw new IllegalStateException(
				"Server tick stopped without an active tick."
			);
		}

		server_tick_timer.observeDuration();
		this.server_tick_timer = null;
	}

	/**
	 * The EntityKey class is used to count entities per dimension.
	 */
	private static class EntityKey {
		public final String dim;
		public final int dim_id;
		public final int id;
		public final String type;

		/**
		 * Construct the instance.
		 *
		 * @param dim The dimension name.
		 * @param dim_id The dimension id.
		 * @param id The entity id.
		 * @param type The entity type.
		 */
		public EntityKey(String dim, int dim_id, int id, String type) {
			this.dim = dim;
			this.dim_id = dim_id;
			this.id = id;
			this.type = type;
		}

		/**
		 * Determine whether the other object is equal to this one.
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (!(obj instanceof EntityKey)) {
				return false;
			}

			EntityKey other = (EntityKey)obj;
			return (
				Objects.equals(this.dim, other.dim)
				&& this.dim_id == other.dim_id
				&& this.id == other.id
				&& Objects.equals(this.type, other.type)
			);
		}

		/**
		 * Get a hash code value for the object.
		 */
		@Override
		public int hashCode() {
			return Objects.hash(this.dim, this.dim_id, this.id, this.type);
		}
	}
}
