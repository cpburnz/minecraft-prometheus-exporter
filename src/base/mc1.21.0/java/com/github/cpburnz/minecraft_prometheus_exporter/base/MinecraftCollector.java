package com.github.cpburnz.minecraft_prometheus_exporter.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

import io.prometheus.client.Gauge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.Histogram;

/**
 * The MinecraftCollector class defines the base implementation for collecting
 * stats from the Minecraft server for export.
 */
public abstract class MinecraftCollector extends Collector implements Collector.Describable {

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

	/**
	 * The name of the dimension chunks loaded metric.
	 */
	protected static final String NAME_DIMENSION_CHUNKS_LOADED = "mc_dimension_chunks_loaded";

	/**
	 * The name of the dimension ticks seconds metric.
	 */
	private static final String NAME_DIMENSION_TICK_SECONDS = "mc_dimension_tick_seconds";

	/**
	 * The name of the entities total metric.
	 */
	protected static final String NAME_ENTITIES_TOTAL = "mc_entities_total";

	/**
	 * The name of the player list metric.
	 */
	protected static final String NAME_PLAYER_LIST = "mc_player_list";

	/**
	 * The name of the scrape duration seconds metric.
	 */
	private static final String NAME_SCRAPE_DURATION_SECONDS = "mc_scrape_duration_seconds";

	/**
	 * The name of the server tick seconds metric.
	 */
	private static final String NAME_SERVER_TICK_SECONDS = "mc_server_tick_seconds";

	/**
	 * The histogram buckets to use for ticks.
	 */
	protected static final double[] TICK_BUCKETS = new double[] {
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
	protected final ServerConfig config;

	/**
	 * Histogram metrics for dimension tick timing.
	 */
	private final Histogram dim_tick_seconds;

	/**
	 * Maps each dimension (unique name) to its active timer when timing a
	 * dimension (world) tick.
	 *
	 * <p>Track each dimension separately in order to support multi-threading.
	 * Minecraft (as of at least 1.20) still does not run server-side dimension
	 * ticks in multiple threads. However, some mods do for their custom
	 * dimensions (e.g., Vault Hunters).</p>
	 */
	private final ConcurrentHashMap<String, Histogram.Timer> dim_tick_timers;

	/**
	 * Gauge metric for recording scrape durations.
	 */
	private final Gauge scrape_duration;

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
	 */
	public MinecraftCollector(ServerConfig config) {
		this.config = config;
		this.dim_tick_timers = new ConcurrentHashMap<>(3);

		// Setup scrape metrics.
		this.scrape_duration = Gauge.build()
			.name(NAME_SCRAPE_DURATION_SECONDS)
			.help("How long each metric took to be scraped.")
			.labelNames("metric")
			.create();

		// Setup server metrics.
		this.server_tick_seconds = Histogram.build()
			.buckets(TICK_BUCKETS)
			.name(NAME_SERVER_TICK_SECONDS)
			.help("Stats on server tick times.")
			.create();

		this.dim_tick_seconds = Histogram.build()
			.buckets(TICK_BUCKETS)
			.name(NAME_DIMENSION_TICK_SECONDS)
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
			this.scrape_duration.clear();
			MetricFamilySamples player_list = this.collectPlayerList();
			List<MetricFamilySamples> server_ticks = this.collectServerTickSeconds();
			MetricFamilySamples dim_chunks_loaded = this.collectDimensionChunksLoaded();
			List<MetricFamilySamples> dim_ticks = this.collectDimensionTickSeconds();

			MetricFamilySamples entities = null;
			int entities_init = 0;
			if (this.config.collector_mc_entities) {
				entities = collectEntitiesTotal();
				entities_init = 1;
			}

			// Collect scrape durations last.
			List<MetricFamilySamples> scrape_durations = this.scrape_duration.collect();

			// Aggregate metrics.
			ArrayList<MetricFamilySamples> metrics = new ArrayList<>(
				1 /* player_list */
				+ entities_init
				+ server_ticks.size()
				+ 1 /* dim_chunks_loaded */
				+ dim_ticks.size()
				+ scrape_durations.size()
			);
			metrics.add(player_list);
			if (entities != null) {
				metrics.add(entities);
			}
			metrics.addAll(server_ticks);
			metrics.add(dim_chunks_loaded);
			metrics.addAll(dim_ticks);
			metrics.addAll(scrape_durations);

			return metrics;
		} catch (Exception e) {
			LOG.error("Failed to collect metrics.", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Collect the metrics for the number of loaded dimension chunks.
	 *
	 * @return The dimension chunks loaded metric.
	 */
	protected abstract GaugeMetricFamily collectDimensionChunksLoaded();

	/**
	 * Collect the metrics for the dimension ticks (in seconds).
	 *
	 * @return The server tick metrics.
	 */
	private List<MetricFamilySamples> collectDimensionTickSeconds() {
		try (Gauge.Timer timer = this.startScrapeTimer(NAME_DIMENSION_TICK_SECONDS)) {
			return this.dim_tick_seconds.collect();
		}
	}

	/**
	 * Collect the metrics for the entities per dimension.
	 *
	 * @return The entities total metric.
	 */
	protected abstract GaugeMetricFamily collectEntitiesTotal();

	/**
	 * Collect the metrics for the active players.
	 *
	 * @return The player list metric.
	 */
	protected abstract GaugeMetricFamily collectPlayerList();

	/**
	 * Collect the metrics for the server ticks (in seconds).
	 *
	 * @return The server tick metrics.
	 */
	private List<MetricFamilySamples> collectServerTickSeconds() {
		try (Gauge.Timer timer = this.startScrapeTimer(NAME_SERVER_TICK_SECONDS)) {
			return this.server_tick_seconds.collect();
		}
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
	 * Create a new metric for the dimension chunks loaded.
	 *
	 * @return The dimension chunks loaded metric.
	 */
	protected static GaugeMetricFamily newDimensionChunksLoadedMetric() {
		return new GaugeMetricFamily(
			NAME_DIMENSION_CHUNKS_LOADED,
			"The number of loaded dimension chunks.",
			List.of("id", "name")
		);
	}

	/**
	 * Create a new metric for the total entities.
	 *
	 * @return The entities total metric.
	 */
	protected static GaugeMetricFamily newEntitiesTotalMetric() {
		return new GaugeMetricFamily(
			NAME_ENTITIES_TOTAL,
			"The number of entities in each dimension by type.",
			List.of("dim", "dim_id", "type")
		);
	}

	/**
	 * Create a new metric for the player list.
	 *
	 * @return The player list metric.
	 */
	protected static GaugeMetricFamily newPlayerListMetric() {
		return new GaugeMetricFamily(
			NAME_PLAYER_LIST,
			"The players connected to the server.",
			List.of("id", "name")
		);
	}

	/**
	 * Record when a dimension tick begins.
	 *
	 * @param dim The unique dimension name.
	 * @param dim_id The dimension id.
	 */
	protected void startDimensionTick(String dim, int dim_id) {
		// Check for forgotten timer.
		Histogram.Timer timer = this.dim_tick_timers.get(dim);
		if (timer != null) {
			switch (this.config.collector_mc_dimension_tick_errors) {
				case IGNORE -> {}  // Ignore error.
				case LOG -> LOG.debug(
					"Dimension {} tick started before stopping previous tick.", dim
				);
				case STRICT -> throw new IllegalStateException((
					"Dimension " + dim + " tick started before stopping previous tick."
				));
			}

			// Stop forgotten timer.
			timer.close();
			timer = null;
		}

		// Start timer for tick.
		String id_str = Integer.toString(dim_id);
		timer = this.dim_tick_seconds.labels(id_str, dim).startTimer();
		this.dim_tick_timers.put(dim, timer);
	}

	/**
	 * Start a new timer for a scrape duration.
	 *
	 * @param name The metric name.
	 *
	 * @return The timer.
	 */
	protected Gauge.Timer startScrapeTimer(String name) {
		return this.scrape_duration.labels(name).startTimer();
	}

	/**
	 * Record when a server tick begins.
	 */
	public void startServerTick() {
		if (this.server_tick_timer != null) {
			throw new IllegalStateException((
				"Server tick started before stopping previous tick."
			));
		}

		this.server_tick_timer = this.server_tick_seconds.startTimer();
	}

	/**
	 * Record when a dimension tick finishes.
	 *
	 * @param dim The unique dimension name.
	 */
	protected void stopDimensionTick(String dim) {
		// Get active timer.
		Histogram.Timer timer = this.dim_tick_timers.remove(dim);
		if (timer == null) {
			switch (this.config.collector_mc_dimension_tick_errors) {
				case IGNORE -> {}  // Ignore error.
				case LOG -> LOG.debug(
					"Dimension {} tick stopped without an active tick.", dim
				);
				case STRICT -> throw new IllegalStateException((
					"Dimension " + dim + " tick stopped without an active tick."
				));
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
			throw new IllegalStateException((
				"Server tick stopped without an active tick."
			));
		}

		this.server_tick_timer.observeDuration();
		this.server_tick_timer = null;
	}

	/**
	 * The EntityKey class is used to count entities per dimension.
	 *
	 * @param dim The unique dimension name.
	 * @param dim_id The dimension id.
	 * @param type The entity type.
	 */
	public record EntityKey(String dim, int dim_id, String type) {
		// Empty.
	}
}
