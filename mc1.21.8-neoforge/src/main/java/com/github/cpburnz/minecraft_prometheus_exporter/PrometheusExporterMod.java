package com.github.cpburnz.minecraft_prometheus_exporter;

import java.io.IOException;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

import com.github.cpburnz.minecraft_prometheus_exporter.neoforge.NeoMinecraftCollector;
import com.github.cpburnz.minecraft_prometheus_exporter.neoforge.NeoServerConfig;

/**
 * The PrometheusExporterMod class defines the mod.
 */
@Mod(PrometheusExporterMod.MOD_ID)
public class PrometheusExporterMod {

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

	/**
	 * The mod id.
	 */
	public static final String MOD_ID = "prometheus_exporter";

	/**
	 * The HTTP server.
	 */
	private HTTPServer http_server;

	/**
	 * The Minecraft metrics collector.
	 */
	private NeoMinecraftCollector mc_collector;

	/**
	 * The Minecraft server.
	 */
	private MinecraftServer mc_server;

	/**
	 * The server configuration.
	 */
	private final NeoServerConfig config;

	/**
	 * Construct the instance.
	 */
	public PrometheusExporterMod() {
		// Register to receive events.
		NeoForge.EVENT_BUS.register(this);

		// Register the server config.
		this.config = new NeoServerConfig();
		this.config.register(ModLoadingContext.get());
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
			this.mc_collector = new NeoMinecraftCollector(this.config, this.mc_server);
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
	 * Called at the start of a dimension tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onDimensionTickStart(LevelTickEvent.Pre event) {
		// Record dimension tick.
		if (this.mc_collector != null) {
			Level level = event.getLevel();
			if (!level.isClientSide()) {
				ResourceKey<Level> dim = level.dimension();
				this.mc_collector.startDimensionTick(dim);
			}
		}
	}

	/**
	 * Called at the end of a dimension tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onDimensionTickStop(LevelTickEvent.Post event) {
		// Record dimension tick.
		if (this.mc_collector != null) {
			Level level = event.getLevel();
			if (!level.isClientSide()) {
				ResourceKey<Level> dim = level.dimension();
				this.mc_collector.stopDimensionTick(dim);
			}
		}
	}

	/**
	 * Called before the server begins loading anything.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onServerAboutToStart(ServerAboutToStartEvent event) {
		// NOTE: This appears to be the earliest event where NeoForge has loaded the
		// server-side config.
		this.config.loadValues();
	}

	/**
	 * Called when the server has started.
	 *
	 * @param event The event.
	 * @throws IOException
	 */
	@SubscribeEvent
	public void onServerStarted(ServerStartedEvent event) throws IOException {
		// Record the Minecraft server.
		this.mc_server = event.getServer();

		// Initialize HTTP server.
		this.initHttpServer();

		// Initialize collectors.
		this.initCollectors();
	}

	/**
	 * Called when the server has stopped.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onServerStopped(ServerStoppedEvent event) {
		// Unregister collectors.
		this.closeCollectors();

		// Stop HTTP server.
		this.closeHttpServer();
	}

	/**
	 * Called at the start of the server tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onServerTickStart(ServerTickEvent.Pre event) {
		// Record server tick.
		if (this.mc_collector != null) {
			this.mc_collector.startServerTick();
		}
	}

	/**
	 * Called at the end of the server tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onServerTickStop(ServerTickEvent.Post event) {
		// Record server tick.
		if (this.mc_collector != null) {
			this.mc_collector.stopServerTick();
		}
	}
}
