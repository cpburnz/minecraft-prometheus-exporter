package com.github.cpburnz.minecraft_prometheus_exporter;

import java.io.IOException;
import java.net.BindException;

import net.minecraft.server.MinecraftServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

import com.github.cpburnz.minecraft_prometheus_exporter.prometheus_exporter.Tags;

/**
 * The PrometheusExporterMod class defines the mod.
 */
@Mod(
	modid = Tags.MODID,
	version = Tags.VERSION,
	name = Tags.MODNAME,
	acceptedMinecraftVersions = "[1.7.10]",
	acceptableRemoteVersions = "*"
)
public class PrometheusExporterMod {

	/**
	 * The logger to use.
	 */
	public static final Logger LOG = LogManager.getLogger(Tags.MODID);

	/**
	 * The HTTP server.
	 */
	private HTTPServer http_server;

	/**
	 * The Minecraft metrics collector.
	 */
	public static MinecraftCollector mc_collector;

	/**
	 * The Minecraft server.
	 */
	private MinecraftServer mc_server;

	/**
	 * The mod configuration.
	 */
	private Config config;

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
	 * Register the metrics collectors.
	 */
	private void initCollectors() {
		// Collect JVM stats.
		if (this.config.collector_jvm) {
			DefaultExports.initialize();
		}

		// Collect Minecraft stats.
		if (this.config.collector_mc) {
			mc_collector = new MinecraftCollector(this.config, this.mc_server);
			mc_collector.register();
		}
	}

	/**
	 * Start the HTTP server.
	 */
	private void initHttpServer() throws IOException {
		// WARNING: Make sure the HTTP server thread is daemonized, otherwise the
		// Minecraft server process will not properly terminate.
		String address = this.config.web_listen_address;
		int port = this.config.web_listen_port;
		try {
			this.http_server = new HTTPServer(address, port, true);
			LOG.info("Listening on {}:{}", address, port);
		} catch (BindException e) {
			LOG.error(
				"Failed to start prometheus exporter, port " + port + " already in use."
			);
		}
	}

	/**
	 * Called before any other phase. Configuration files should be read.
	 *
	 * @param event The event.
	 */
	@Mod.EventHandler
	public void onPreInitialization(FMLPreInitializationEvent event) {
		// Register the server config.
		this.config = new Config();
		this.config.loadValues(event.getSuggestedConfigurationFile());

		// Register event handlers.
		FMLCommonHandler.instance()
			.bus()
			.register(new TickHandler());
	}

	/**
	 * Called when the server is starting up.
	 *
	 * @param event The event.
	 */
	@Mod.EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		// Register server commands in this event handler.

		// Record the Minecraft server.
		this.mc_server = event.getServer();
	}

	/**
	 * Called when the server has started.
	 *
	 * @param event The event.
	 * @throws IOException
	 */
	@Mod.EventHandler
	public void onServerStarted(FMLServerStartedEvent event) throws IOException {
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
	@Mod.EventHandler
	public void onServerStopped(FMLServerStoppedEvent event) {
		// Close collectors.
		this.closeCollectors();

		// Stop HTTP server.
		this.closeHttpServer();
	}
}
