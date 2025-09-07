package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import java.util.Arrays;
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
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.GaugeMetricFamily;

import com.github.cpburnz.minecraft_prometheus_exporter.config.ModConfig;

/**
 * The MinecraftCollector class collects stats from the Minecraft server for
 * export.
 */
public class ForgeMinecraftCollector extends MinecraftCollector {

	/**
	 * The initial capacity to use for the entities total map. This is arbitrary.
	 */
	private static final int ENTITIES_TOTAL_INIT = 20;

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

	/**
	 * The number of total entities collected last time. This is used to set the
	 * initial capacity of the map.
	 */
	private int last_entities_total = ENTITIES_TOTAL_INIT;

	/**
	 * The Minecraft server.
	 */
	private final MinecraftServer mc_server;

	/**
	 * Constructs the instance.
	 *
	 * @param config The mod configuration.
	 * @param mc_server The Minecraft server.
	 */
	public ForgeMinecraftCollector(ModConfig config, MinecraftServer mc_server) {
		super(config);
		this.mc_server = mc_server;
	}

	/**
	 * Get the number of loaded dimension chunks.
	 *
	 * @return The dimension chunks loaded metric.
	 */
	@Override
	protected GaugeMetricFamily collectDimensionChunksLoaded() {
		GaugeMetricFamily metric = newDimensionChunksLoadedMetric();
		for (WorldServer world : this.mc_server.worldServers) {
			String id_str = Integer.toString(getDimensionId(world.provider));
			String name = getDimensionName(world.provider);
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
	@Override
	protected GaugeMetricFamily collectEntitiesTotal() {
		// Aggregate stats.
		TObjectIntHashMap<EntityKey> entity_totals = new TObjectIntHashMap<>(
			this.last_entities_total
		);
		for (WorldServer world : this.mc_server.worldServers) {
			// Get world info.
			int dim_id = getDimensionId(world.provider);
			String dim = getDimensionName(world.provider);

			// Get entity info.
			List<Entity> loaded_entities = world.loadedEntityList;
			for (int i = loaded_entities.size(); i-- > 0; ) {
				Entity entity;
				try {
					entity = loaded_entities.get(i);
				} catch (IndexOutOfBoundsException e) {
					LOG.debug("Dimension {} loaded entity list shrank.", dim_id);
					break;
				}

				if (!(entity instanceof EntityPlayer)) {
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

		// Record the number of entities collected.
		this.last_entities_total = entity_totals.size();

		// Record metrics.
		GaugeMetricFamily metric = newEntitiesTotalMetric();
		for (EntityKey entity_key : entity_totals.keySet()) {
			double total = entity_totals.get(entity_key);
			String dim_id_str = Integer.toString(entity_key.dim_id);
			String id_str = Integer.toString(entity_key.id);
			metric.addMetric(
				Arrays.asList(entity_key.dim, dim_id_str, id_str, entity_key.type), total
			);
		}
		return metric;
	}

	/**
	 * Get the active players.
	 *
	 * @return The player list metric.
	 */
	@Override
	protected GaugeMetricFamily collectPlayerList() {
		GaugeMetricFamily metric = newPlayerListMetric();
		for (EntityPlayerMP player : this.mc_server.getConfigurationManager().playerEntityList) {
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
	 * Get the general player stats.
	 *
	 * @return The player stats metric.
	 */
	@Override
	protected GaugeMetricFamily collectPlayerStats() {
		// Record player ids.
		GaugeMetricFamily metric = newPlayerStatsMetric();
		for (EntityPlayerMP player : this.mc_server.getConfigurationManager().playerEntityList) {
			// Get player profile.
			GameProfile profile = player.getGameProfile();

			// Get player info.
			// - WARNING: Either "id" or "name" can be null in Minecraft 1.19 and
			//   earlier.
			@Nullable UUID player_id = profile.getId();
			@Nullable String player_name = profile.getName();
			String player_id_str = Objects.toString(player_id, "");
			String player_name_str = ObjectUtils.defaultIfNull(player_name, "");

			StatisticsFile stats_file = player.func_147099_x();
			for (StatBase stat : StatList.generalStats) {
				// Get stat value.
				// - NOTICE: Despite its name, this reads the value.
				int stat_val = stats_file.writeStat(stat);
				String stat_code = stat.statId;
				String stat_name = stat.func_150951_e().getUnformattedText(); // TODO: Cache this.

				// Record score.
				metric.addMetric(Arrays.asList(
					stat_code,
					stat_name,
					player_id_str,
					player_name_str
				), stat_val);
			}
		}
		return metric;
	}

	/**
	 * Get the dimension id.
	 *
	 * @param dim The world provider.
	 */
	private static int getDimensionId(WorldProvider dim) {
		return dim.dimensionId;
	}

	/**
	 * Get the dimension name.
	 *
	 * @param dim The world provider.
	 *
	 * @return The dimension name.
	 */
	private static String getDimensionName(WorldProvider dim) {
		return dim.getDimensionName();
	}

	/**
	 * Record when a dimension tick begins.
	 *
	 * @param dim The world provider.
	 */
	public void startDimensionTick(WorldProvider dim) {
		// Get dimension info.
		String name = getDimensionName(dim);
		int dim_id = getDimensionId(dim);

		super.startDimensionTick(dim_id, name);
	}

	/**
	 * Record when a dimension tick finishes.
	 *
	 * @param dim The dimension type.
	 */
	public void stopDimensionTick(WorldProvider dim) {
		// Get dimension info.
		int dim_id = getDimensionId(dim);

		super.stopDimensionTick(dim_id);
	}
}
