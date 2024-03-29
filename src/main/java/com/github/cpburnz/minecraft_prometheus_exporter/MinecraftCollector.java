package com.github.cpburnz.minecraft_prometheus_exporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.Histogram;


/**
 * This class collects stats from the Minecraft server for export.
 */
public class MinecraftCollector extends Collector implements Collector.Describable {

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
	 * The active dimension being timed.
	 */
	@Nullable
	private String dim_tick_name;

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
	 * @param mc_server The Minecraft server.
	 */
	public MinecraftCollector(MinecraftServer mc_server) {
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
		// Collect metrics.
		MetricFamilySamples player_list = this.collectPlayerList();
		List<MetricFamilySamples> server_ticks = this.server_tick_seconds.collect();
		MetricFamilySamples dim_chunks_loaded = this.collectDimensionChunksLoaded();
		List<MetricFamilySamples> dim_ticks = this.dim_tick_seconds.collect();

		// Aggregate metrics.
		ArrayList<MetricFamilySamples> metrics = new ArrayList<>(
			1 /* player_list */
			+ server_ticks.size()
			+ 1 /* dim_chunks_loaded */
			+ dim_ticks.size()
		);
		metrics.add(player_list);
		metrics.addAll(server_ticks);
		metrics.add(dim_chunks_loaded);
		metrics.addAll(dim_ticks);

		return metrics;
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
			metric.addMetric(Arrays.asList(id_str, name), loaded);
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
			GameProfile profile = player.getGameProfile();
			String id_str = profile.getId().toString();
			String name = profile.getName();
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
		descs.addAll(this.server_tick_seconds.describe());
		descs.add(newDimensionChunksLoadedMetric());
		descs.addAll(this.dim_tick_seconds.describe());
		return descs;
	}

	/**
	 * Get the dimension id.
	 *
	 * With the new version of Minecraft, v16, a dimension no longer has an id.
	 * However, to keep backward compatibility with older versions of the
	 * exporter, we need this method. Vanilla dimensions use fixed id values (-1,
	 * 0, 1), and the id of a custom dimension is now calculated from the
	 * dimension name.
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
			Arrays.asList("id", "name")
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
	public void startDimensionTick(ResourceKey<Level> dim) {
		String name = dim.location().getPath();
		if (this.dim_tick_timer != null) {
			throw new IllegalStateException(
				"Dimension " + name + " tick started before stopping previous tick for "
				+ "dimension " + this.dim_tick_name + "."
			);
		}

		String id_str = Integer.toString(getDimensionId(dim));
		this.dim_tick_name = name;
		this.dim_tick_timer = this.dim_tick_seconds.labels(id_str, name).startTimer();
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
		String name = dim.location().getPath();
		if (this.dim_tick_timer == null) {
			throw new IllegalStateException(
				"Dimension " + name + " tick stopped without an active tick."
			);
		} else if (!name.equals(this.dim_tick_name)) {
			throw new IllegalStateException(
				"Dimension " + name + " tick stopped while in an active tick for "
				+ "dimension " + this.dim_tick_name + "."
			);
		}

		this.dim_tick_timer.observeDuration();
		this.dim_tick_timer = null;
		this.dim_tick_name = null;
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
}
