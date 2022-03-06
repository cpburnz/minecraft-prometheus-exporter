package com.github.cpburnz.minecraft_prometheus_exporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;

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
			+ 1 /* dimension_chunks_loaded */
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
		for (WorldServer world : this.mc_server.worlds) {
			DimensionType dim = world.provider.getDimensionType();
			String id_str = Integer.toString(dim.getId());
			String name = dim.getName();
			int loaded = world.getChunkProvider().getLoadedChunkCount();
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
		for (EntityPlayerMP player : this.mc_server.getPlayerList().getPlayers()) {
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
	 * @param dim The dimension type.
	 */
	public void startDimensionTick(DimensionType dim) {
		int id = dim.getId();
		if (this.dim_tick_timer != null) {
			throw new IllegalStateException(
				"Dimension " + id + " tick started before stopping previous tick for "
				+ "dimension " + this.dim_tick_id + "."
			);
		}

		String id_str = Integer.toString(id);
		String name = dim.getName();
		this.dim_tick_id = id;
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
	 * @param dim The dimension type.
	 */
	public void stopDimensionTick(DimensionType dim) {
		int id = dim.getId();
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
	 * Record when a server tick finishes.
	 */
	public void stopServerTick() {
		if (this.server_tick_timer == null) {
			throw new IllegalStateException("Server tick stopped without an active tick.");
		}

		this.server_tick_timer.observeDuration();
		this.server_tick_timer = null;
	}
}
