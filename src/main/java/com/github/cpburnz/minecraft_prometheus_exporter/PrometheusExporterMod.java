package com.github.cpburnz.minecraft_prometheus_exporter;

import java.io.IOException;
import java.nio.file.Path;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import org.jetbrains.annotations.Nullable;

/**
 * The PrometheusExporterMod class defines the mod.
 */
public class PrometheusExporterMod implements
	DedicatedServerModInitializer,
	ServerLifecycleEvents.ServerStarted,
	ServerLifecycleEvents.ServerStarting,
	ServerLifecycleEvents.ServerStopped,
	ServerTickEvents.EndTick,
	ServerTickEvents.EndWorldTick,
	ServerTickEvents.StartTick,
	ServerTickEvents.StartWorldTick
{

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

	/**
	 * The HTTP server.
	 */
	@Nullable
	private HTTPServer http_server;

	/**
	 * The Minecraft metrics collector.
	 */
	@Nullable
	private MinecraftCollector mc_collector;

	/**
	 * The Minecraft server.
	 */
	@Nullable
	private MinecraftServer mc_server;

	/**
	 * The server configuration.
	 */
	private final ServerConfig config;

	/**
	 * Construct the instance.
	 */
	public PrometheusExporterMod() {
		this.config = new ServerConfig();
	}

	/**
	 * Unregister the metrics collectors.
	 */
	private void closeCollectors() {
		// Unregister all collectors.
		CollectorRegistry.defaultRegistry.clear();
	}

	/**
	 * Stop the HTTP server.
	 */
	private void closeHttpServer() {
		// WARNING: Remember to stop the HTTP server. Otherwise, the Minecraft
		// client will crash because the TCP port will already be in use when trying
		// to load a second saved world.
		if (this.http_server != null) {
			this.http_server.close();
			this.http_server = null;
		} else {
			LOG.warn("Cannot close http_server=null.");
		}
	}

	/**
	 * Initialize the metrics collectors.
	 */
	private void initCollectors() {
		// Collect JVM stats.
		if (this.config.collector_jvm) {
			DefaultExports.initialize();
		}

		// Collect Minecraft stats.
		if (this.config.collector_mc) {
			this.mc_collector = new MinecraftCollector(this.config, this.mc_server);
			this.mc_collector.register();
		}
	}

	/**
	 * Initialize the HTTP server.
	 */
	private void initHttpServer() throws IOException {
		// WARNING: Make sure the HTTP server thread is daemonized, otherwise the
		// Minecraft server process will not properly terminate.
		String address = this.config.web_listen_address;
		int port = this.config.web_listen_port;
		this.http_server = new HTTPServer(address, port, true);
		LOG.info("Listening on {}:{}", address, port);
	}

	/**
	 * Called at the end of the server tick.
	 *
	 * @param server The server.
	 */
	@Override
	public void onEndTick(MinecraftServer server) {
		// Record server tick.
		if (this.mc_collector != null) {
			this.mc_collector.stopServerTick();
		}
	}

	/**
	 * Called at the end of a dimension (world) tick.
	 *
	 * @param world The world.
	 */
	@Override
	public void onEndTick(ServerWorld world) {
		// Record dimension tick.
		if (this.mc_collector != null && !world.isClient()) {
			this.mc_collector.stopDimensionTick(world);
		}
	}

	/**
	 * Runs the mod initializer on the server environment.
	 */
	@Override
	public void onInitializeServer() {
		// Register to receive events.
		ServerLifecycleEvents.SERVER_STARTED.register(this);
		ServerLifecycleEvents.SERVER_STARTING.register(this);
		ServerLifecycleEvents.SERVER_STOPPED.register(this);
		ServerTickEvents.END_SERVER_TICK.register(this);
		ServerTickEvents.END_WORLD_TICK.register(this);
		ServerTickEvents.START_SERVER_TICK.register(this);
		ServerTickEvents.START_WORLD_TICK.register(this);
	}

	/**
	 * Called when the server is starting.
	 *
	 * @param server The server.
	 */
	@Override
	public void onServerStarting(MinecraftServer server) {
		// Record the Minecraft server.
		this.mc_server = server;

		// Load the server config.
		// - NOTICE: Fabric does not yet provide config file loading.
		Path config_file;
		this.config.loadFile(config_file);
	}

	/**
	 * Called when the server has started.
	 *
	 * @param server The server.
	 */
	@Override
	public void onServerStarted(MinecraftServer server) {
		// Initialize HTTP server.
		try {
			this.initHttpServer();
		} catch (IOException e) {
			LOG.error("Failed to initialize HTTP server.", e);
			return;
		}

		// Initialize collectors.
		this.initCollectors();
	}

	/**
	 * Called when the server has stopped.
	 *
	 * @param server The server.
	 */
	@Override
	public void onServerStopped(MinecraftServer server) {
		// Unregister collectors.
		this.closeCollectors();

		// Stop HTTP server.
		this.closeHttpServer();
	}

	/**
	 * Called at the start of the server tick.
	 *
	 * @param server The server.
	 */
	@Override
	public void onStartTick(MinecraftServer server) {
		// Record server tick.
		if (this.mc_collector != null) {
			this.mc_collector.startServerTick();
		}
	}

	/**
	 * Called at the start of a dimension (world) tick.
	 *
	 * @param world The world.
	 */
	@Override
	public void onStartTick(ServerWorld world) {
		// Record dimension tick.
		if (this.mc_collector != null && !world.isClient()) {
			this.mc_collector.startDimensionTick(world);
		}
	}
}
