package com.github.cpburnz.minecraft_prometheus_exporter.forge;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.Gauge;
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
	 * The initial capacity to use for the stat names map. This was counted from
	 * Stats.
	 */
	private static final int STATS_INIT = 76;

	/**
	 * The Minecraft server.
	 */
	private final MinecraftServer mc_server;

	/**
	 * Maps each player id to his name and stats. This is used to persist player
	 * stats after sign-out.
	 */
	private final HashMap<UUID, PlayerInfo> players;

	/**
	 * Maps each stat id to its name. This is used to cache stat names.
	 *
	 * <p>NOTICE: In 1.21.11, StatType uses IdentityHashMap internally to map
	 * Identity to Stat. Let's do the same.</p>
	 */
	private final IdentityHashMap<Identifier, String> stat_names;

	/**
	 * Constructs the instance.
	 *
	 * @param config The mod configuration.
	 * @param mc_server The Minecraft server.
	 */
	public ForgeMinecraftCollector(
		ServerConfig config,
		MinecraftServer mc_server
	) {
		super(config);
		this.mc_server = mc_server;
		this.players = new HashMap<>(PLAYERS_INIT);
		this.stat_names = new IdentityHashMap<>(STATS_INIT);

	}

	/**
	 * Get the number of loaded dimension chunks.
	 *
	 * @return The dimension chunks loaded metric.
	 */
	@Override
	protected GaugeMetricFamily collectDimensionChunksLoaded() {
		try (Gauge.Timer timer = this.startScrapeTimer(NAME_DIMENSION_CHUNKS_LOADED)) {
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
	}

	/**
	 * Get the entities per dimension.
	 *
	 * @return The entities total metric.
	 */
	@Override
	protected GaugeMetricFamily collectEntitiesTotal() {
		try (Gauge.Timer timer = this.startScrapeTimer(NAME_ENTITIES_TOTAL)) {
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
	}

	/**
	 * Get the active players.
	 *
	 * @return The player list metric.
	 */
	@Override
	protected GaugeMetricFamily collectPlayerList() {
		try (Gauge.Timer timer = this.startScrapeTimer(NAME_PLAYER_LIST)) {
			GaugeMetricFamily metric = newPlayerListMetric();
			for (ServerPlayer player : this.mc_server.getPlayerList().getPlayers()) {
				// Get player profile.
				GameProfile profile = player.getGameProfile();

				// Get player info.
				// - NOTICE: Both "id" and "name" are required to be non-null, unlike in
				//   Minecraft 1.19 and earlier.
				String id_str = profile.id().toString();
				String name = profile.name();

				metric.addMetric(List.of(id_str, name), 1);
			}
			return metric;
		}
	}

	/**
	 * Get the general player stats.
	 *
	 * @return The player stats metric.
	 */
	@Override
	protected GaugeMetricFamily collectPlayerStatsTotal() {
		try (Gauge.Timer timer = this.startScrapeTimer(NAME_PLAYER_STAT_TOTAL)) {
			// Cache player list and stats.
			for (ServerPlayer player : this.mc_server.getPlayerList().getPlayers()) {
				// Get player profile.
				GameProfile profile = player.getGameProfile();

				// Get player info.
				// - NOTICE: Both "id" and "name" are required to be non-null, unlike in
				//   Minecraft 1.19 and earlier.
				UUID player_id = profile.id();
				String player_name = profile.name();

				ServerStatsCounter stats = player.getStats();
				@Nullable PlayerInfo player_info = this.players.get(player_id);
				if (player_info != null) {
					player_info.name = player_name;
					player_info.stats = stats;
				} else {
					player_info = new PlayerInfo(player_id, player_name, stats);
					this.players.put(player_id, player_info);
				}
			}

			// Collect player stats.
			GaugeMetricFamily metric = newPlayerStatsTotalMetric();
			for (PlayerInfo player_info : this.players.values()) {
				String player_id_str = player_info.id.toString();
				String player_name = player_info.name;
				ServerStatsCounter stats = player_info.stats;

				for (Stat<Identifier> stat : Stats.CUSTOM) {
					// Get stat info.
					int stat_val = stats.getValue(stat);
					Identifier stat_id = stat.getValue();
					String stat_code = stat_id.toString();

					// Get stat name.
					@Nullable String stat_name = this.stat_names.get(stat_id);
					if (stat_name == null) {
						// Cache stat name.
						Component stat_msg = Component.translatable(stat_id.toLanguageKey("stat"));
						stat_name = stat_msg.getString();
						this.stat_names.put(stat_id, stat_name);
					}

					// Record score.
					metric.addMetric(List.of(
						stat_code, stat_name, player_id_str, player_name
					), stat_val);
				}
			}
			return metric;
		}
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
	 *
	 * @return The dimension id.
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
	 *
	 * @return The dimension name.
	 */
	private static String getDimensionName(ResourceKey<Level> dim) {
		return dim.identifier().getPath();
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

	/**
	 * The PlayerInfo class contains the player name and stats.
	 */
	private static class PlayerInfo {

		/**
		 * The player id.
		 */
		final UUID id;

		/**
		 * The player name,
		 */
		String name;

		/**
		 * The statistics file.
		 */
		ServerStatsCounter stats;

		/**
		 * Constructs the PlayerInfo instance.
		 *
		 * @param id The player id.
		 * @param name The player name.
		 * @param stats The statistics file.
		 */
		PlayerInfo(UUID id, String name, ServerStatsCounter stats) {
			this.id = id;
			this.name = name;
			this.stats = stats;
		}
	}
}
