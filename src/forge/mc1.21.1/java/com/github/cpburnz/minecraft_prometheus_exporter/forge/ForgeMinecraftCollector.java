package com.github.cpburnz.minecraft_prometheus_exporter.forge;

import java.util.HashMap;
import java.util.List;

import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.GaugeMetricFamily;

import com.github.cpburnz.minecraft_prometheus_exporter.base.MinecraftCollector;
import com.github.cpburnz.minecraft_prometheus_exporter.base.ServerConfig;

/**
 * The ForgeMinecraftCollector class collects stats from the Forge Minecraft
 * server for export.
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
	public ForgeMinecraftCollector(ServerConfig config, MinecraftServer mc_server) {
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
		for (ServerLevel world : this.mc_server.getAllLevels()) {
			ResourceKey<Level> dim = world.dimension();
			String id_str = Integer.toString(getDimensionId(dim));
			String name = getDimensionName(dim);
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
	@Override
	protected GaugeMetricFamily collectEntitiesTotal() {
		// Aggregate stats.
		HashMap<EntityKey, Integer> entity_totals = new HashMap<>();
		for (ServerLevel world : this.mc_server.getAllLevels()) {
			// Get dimension info.
			ResourceKey<Level> dim = world.dimension();
			int dim_id = getDimensionId(dim);
			String dim_name = getDimensionName(dim);

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

					EntityKey entity_key = new EntityKey(dim_name, dim_id, entity_type);
					entity_totals.merge(entity_key, 1, Integer::sum);
				}
			}
		}

		// Record metrics.
		GaugeMetricFamily metric = newEntitiesTotalMetric();
		for (var entry : entity_totals.entrySet()) {
			EntityKey entity_key = entry.getKey();
			double total = entry.getValue();
			String dim_id_str = Integer.toString(entity_key.dim_id());
			metric.addMetric(
				List.of(entity_key.dim(), dim_id_str, entity_key.type()), total
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
		for (ServerPlayer player : this.mc_server.getPlayerList().getPlayers()) {
			// Get player profile.
			GameProfile profile = player.getGameProfile();

			// Get player info.
			// - NOTICE: Both "id" and "name" are required to be non-null, unlike in
			//   Minecraft 1.19 and earlier.
			String id_str = profile.getId().toString();
			String name = profile.getName();

			metric.addMetric(List.of(id_str, name), 1);
		}
		return metric;
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
			String name = getDimensionName(dim);
			return name.hashCode();
		}
	}

	/**
	 * Get the dimension name.
	 *
	 * @param dim The dimension.
	 */
	private static String getDimensionName(ResourceKey<Level> dim) {
		return dim.location().getPath();
	}

	/**
	 * Record when a dimension tick begins.
	 *
	 * @param dim The dimension.
	 */
	public void startDimensionTick(ResourceKey<Level> dim) {
		// Get dimension info.
		String name = getDimensionName(dim);
		int dim_id = getDimensionId(dim);

		super.startDimensionTick(name, dim_id);
	}

	/**
	 * Record when a dimension tick finishes.
	 *
	 * @param dim The dimension.
	 */
	public void stopDimensionTick(ResourceKey<Level> dim) {
		// Get dimension info.
		String name = getDimensionName(dim);

		super.stopDimensionTick(name);
	}
}
