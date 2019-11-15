package com.github.cpburnz.minecraft_prometheus_exporter;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

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
	 * The default port to use.
	 */
	private static final int DEFAULT_PORT = 19565;

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

	/**
	 * The HTTP port to use.
	 */
	private int http_port;

	/**
	 * The HTTP server.
	 */
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
	 * Construct the instance.
	 */
	public PrometheusExporterMod() {
		this.http_port = DEFAULT_PORT;

		// Register to recieve events.
		MinecraftForge.EVENT_BUS.register(this);
	}

	/**
	 * Initialize the metrics collectors.
	 */
	private void initCollectors() {
		// Collect JVM stats.
		DefaultExports.initialize();

		// Collect Minecraft stats.
		this.mc_collector = new MinecraftCollector(this.mc_server).register();
	}

	/**
	 * Load the mod configuration file.
	 */
	private void loadConfig() {
		// TODO: Load the config file.
	}

	/**
	 * Called when the server is starting.
	 *
	 * @param event The event.
	 * @throws IOException
	 */
	@SubscribeEvent
	public void onServerStarting(FMLServerStartingEvent event) throws IOException {
		// Record the Minecraft server.
		this.mc_server = event.getServer();

		// Initialize HTTP server.
		this.http_server = new HTTPServer(this.http_port);

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
		if (event.phase == TickEvent.Phase.START) {
			this.mc_collector.startServerTick();
		} else if (event.phase == TickEvent.Phase.END) {
			this.mc_collector.stopServerTick();
		}
	}

	/**
	 * Called before the server starts.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onSetup(FMLCommonSetupEvent event) {
		this.loadConfig();
	}

	/**
	 * Called on a world tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onWorldTick(TickEvent.WorldTickEvent event) {
		// Record world tick.
		if (event.phase == TickEvent.Phase.START) {
			this.mc_collector.startWorldTick(event.world);
		} else if (event.phase == TickEvent.Phase.END) {
			this.mc_collector.stopWorldTick(event.world);
		}
	}
}
