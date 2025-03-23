package com.github.cpburnz.minecraft_prometheus_exporter.fabric;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.GaugeMetricFamily;

import com.github.cpburnz.minecraft_prometheus_exporter.base.MinecraftCollector;
import com.github.cpburnz.minecraft_prometheus_exporter.base.ServerConfig;

/**
 * The FabricMinecraftCollector class collects stats from the Fabric Minecraft
 * server for export.
 */
public class FabricMinecraftCollector extends MinecraftCollector {

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
	public FabricMinecraftCollector(ServerConfig config, MinecraftServer mc_server) {
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
		for (ServerWorld world : this.mc_server.getWorlds()) {
			RegistryKey<World> dim_key = world.getRegistryKey();
			String id_str = Integer.toString(getDimensionId(dim_key));
			String name = getDimensionName(dim_key);
			int loaded = world.getChunkManager().getLoadedChunkCount();
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
		for (ServerWorld world : this.mc_server.getWorlds()) {
			// Get dimension info.
			RegistryKey<World> dim_key = world.getRegistryKey();
			int dim_id = getDimensionId(dim_key);
			String dim_name = getDimensionName(dim_key);

			// Get entity info.
			for (Entity entity : world.iterateEntities()) {
				if (!(entity instanceof ServerPlayerEntity)) {
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
		for (ServerPlayerEntity player : this.mc_server.getPlayerManager().getPlayerList()) {
			// Get player profile.
			GameProfile profile = player.getGameProfile();

			// Get player info.
			// - WARNING: Either "id" or "name" can be null in Minecraft 1.19 and
			//   earlier.
			String id_str = Objects.toString(profile.getId(), "");
			String name = ObjectUtils.defaultIfNull(profile.getName(), "");

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
	 * @param dim_key The dimension key.
	 *
	 * @return The dimension id.
	 */
	private static int getDimensionId(RegistryKey<World> dim_key) {
		if (dim_key.equals(World.OVERWORLD)) {
			return 0;
		} else if (dim_key.equals(World.END)) {
			return 1;
		} else if (dim_key.equals(World.NETHER)) {
			return -1;
		} else {
			return getDimensionName(dim_key).hashCode();
		}
	}

	/**
	 * Get the dimension name.
	 *
	 * @param dim_key The dimension key.
	 *
	 * @return The dimension name.
	 */
	private static String getDimensionName(RegistryKey<World> dim_key) {
		return dim_key.getValue().getPath();
	}

	/**
	 * Record when a dimension (world) tick begins.
	 *
	 * @param world The world.
	 */
	public void startDimensionTick(ServerWorld world) {
		// Get dimension info.
		RegistryKey<World> dim_key = world.getRegistryKey();
		String name = getDimensionName(dim_key);
		int dim_id = getDimensionId(dim_key);

		super.startDimensionTick(name, dim_id);
	}

	/**
	 * Record when a dimension (world) tick finishes.
	 *
	 * @param world The world.
	 */
	public void stopDimensionTick(ServerWorld world) {
		// Get dimension info.
		RegistryKey<World> dim_key = world.getRegistryKey();
		String name = getDimensionName(dim_key);

		super.stopDimensionTick(name);
	}
}
