package com.github.cpburnz.minecraft_prometheus_exporter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.GaugeMetricFamily;

/**
 * The MinecraftCollector class collects stats from the Minecraft server for
 * export.
 */
public class ForgeMinecraftCollector extends MinecraftCollector {

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

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
		TObjectIntHashMap<EntityKey> entity_totals = new TObjectIntHashMap<>();
		for (WorldServer world : this.mc_server.worldServers) {
			// Get world info.
			int dim_id = getDimensionId(world.provider);
			String dim = getDimensionName(world.provider);

			// Get entity info.
			List loaded_entities = world.loadedEntityList;
			for (int i = loaded_entities.size(); i-- > 0; ) {
				Object entityObj;
				try {
					entityObj = loaded_entities.get(i);
				} catch (IndexOutOfBoundsException e) {
					LOG.debug("Dimension {} loaded entity list shrank.", dim_id);
					break;
				}

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
		for (Object playerObj : this.mc_server.getConfigurationManager().playerEntityList) {
			// Get player profile.
			EntityPlayerMP player = (EntityPlayerMP)playerObj;
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
