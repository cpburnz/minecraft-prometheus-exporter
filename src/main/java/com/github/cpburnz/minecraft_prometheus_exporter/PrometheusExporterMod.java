package com.github.cpburnz.minecraft_prometheus_exporter;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * This class is the Prometheus Exporter mod.
 */
@Mod(PrometheusExporterMod.MOD_ID)
public class PrometheusExporterMod {

	/**
	 * The mod id.
	 */
	public static final String MOD_ID = "prometheus_exporter";

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

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
	@SuppressWarnings("FieldMayBeFinal")
	private ServerConfig config;

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
	 * Initialize the metrics collectors.
	 */
	private void initCollectors() {
		// Collect JVM stats.
		if (this.config.collector_jvm) {
			DefaultExports.initialize();
		}

		// Collect Minecraft stats.
		if (this.config.collector_mc) {
			this.mc_collector = new MinecraftCollector(this.mc_server);
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

	// NOTE: Does not receive event (has not been reevaluated since 1.14.4).
	/*
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

	// NOTE: Does not receive event (has not been reevaluated since 1.14.4).
	/*
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
	 * Called when the server has started.
	 *
	 * @param event The event.
	 * @throws IOException
	 */
	@SubscribeEvent
	public void onServerStarted(ServerStartedEvent event) throws IOException {
		// NOTE: The `ModConfig.Loading` event is not being received. However, the
		// config appears to be loaded by Forge. Load the values here on server
		// start. This series of events has not been reevaluated since 1.14.4.
		this.config.loadValues();

		// Record the Minecraft server.
		this.mc_server = event.getServer();

		// Initialize HTTP server.
		this.initHttpServer();

		// Initialize collectors.
		this.initCollectors();
	}

	/**
	 * Called on the server tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		// Record server tick.
		if (this.mc_collector != null) {
			if (event.phase == TickEvent.Phase.START) {
				this.mc_collector.startServerTick();
			} else if (event.phase == TickEvent.Phase.END) {
				this.mc_collector.stopServerTick();
			}
		}
	}

	/**
	 * Called on a world tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onWorldTick(TickEvent.WorldTickEvent event) {
		// Record world tick.
		if (this.mc_collector != null) {
			if (event.phase == TickEvent.Phase.START) {
				this.mc_collector.startWorldTick(event.world);
			} else if (event.phase == TickEvent.Phase.END) {
				this.mc_collector.stopWorldTick(event.world);
			}
		}
	}
}
