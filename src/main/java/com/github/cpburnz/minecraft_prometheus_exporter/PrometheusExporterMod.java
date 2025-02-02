package com.github.cpburnz.minecraft_prometheus_exporter;

import java.io.IOException;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

/**
 * This class is the Prometheus Exporter mod.
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
	@SuppressWarnings({"unused", "FieldCanBeLocal"})
	private HTTPServer http_server;

	/**
	 * The Minecraft metrics collector.
	 */
	private MinecraftCollector mc_collector;

	/**
	 * The Minecraft server.
	 */
	private MinecraftServer mc_server;

	/**
	 * The server configuration.
	 */
	private final ServerConfig config;

	/**
	 * Construct the instance.
	 */
	public PrometheusExporterMod() {
		// Register to receive events.
		MinecraftForge.EVENT_BUS.register(this);

		// Register the server config.
		this.config = new ServerConfig();
		this.config.register();
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
		this.http_server.close();
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

	// NOTE: Does not receive event.
	/**
	 * Called when the mod configuration is loaded. This occurs right before the
	 * `FMLServerAboutToStartEvent` event.
	 *
	 * @param event The event.
	 */
	/*
	@SubscribeEvent
	public void onConfigLoad(ModConfig.Loading event) {
		LOG.info("CONFIG LOAD");
		ModConfig config = event.getConfig();
		if (config.getType() == ModConfig.Type.SERVER) {
			this.config.loadConfig(config);
		}
	}
	*/

	// NOTE: Does not receive event.
	/**
	 * Called when the server-side mod configuration is reloaded.
	 *
	 * @param event The event.
	 */
	/*
	@SubscribeEvent
	public void onConfigReload(ModConfig.ConfigReloading event) {
		LOG.info("CONFIG RELOAD");
		// TODO: Restart HTTP server when config is reloaded.
		// TODO: Reinitialize the collectors.
		ModConfig config = event.getConfig();
		if (config.getType() == ModConfig.Type.SERVER) {
			this.config.loadConfig(config);
		}
	}
	*/

	/**
	 * Called on a dimension tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onDimensionTick(TickEvent.WorldTickEvent event) {
		// Record dimension tick.
		if (this.mc_collector != null && event.side == LogicalSide.SERVER) {
			RegistryKey<World> dim = event.world.dimension();
			if (event.phase == TickEvent.Phase.START) {
				this.mc_collector.startDimensionTick(dim);
			} else if (event.phase == TickEvent.Phase.END) {
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
	public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
		// NOTE: The `ModConfig.Loading` event is not being received.
		// NOTE: This appears to be the earliest event where Forge has loaded the
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
	public void onServerStarted(FMLServerStartedEvent event) throws IOException {
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
	public void onServerStopped(FMLServerStoppedEvent event) {
		// Unregister collectors.
		this.closeCollectors();

		// Stop HTTP server.
		this.closeHttpServer();
	}

	/**
	 * Called on the server tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		// Record server tick.
		if (this.mc_collector != null && event.side == LogicalSide.SERVER) {
			if (event.phase == TickEvent.Phase.START) {
				this.mc_collector.startServerTick();
			} else if (event.phase == TickEvent.Phase.END) {
				this.mc_collector.stopServerTick();
			}
		}
	}
}
