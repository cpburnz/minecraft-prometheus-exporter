package com.github.cpburnz.minecraft_prometheus_exporter;

import java.io.IOException;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import net.minecraft.server.MinecraftServer;

/**
 * This class is the Prometheus Exporter mod.
 */
@Mod(
	modid=PrometheusExporterMod.MOD_ID,
	useMetadata=true
)
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
	 * The mod configuration.
	 */
	@SuppressWarnings("FieldMayBeFinal")
	private Config config;

	/**
	 * Construct the instance.
	 */
	public PrometheusExporterMod() {
		// Register to receive events.
		MinecraftForge.EVENT_BUS.register(this);

		// Register the server config.
		this.config = new Config();
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

	/**
	 * Called before any other phase. Configuration files should be read.
	 *
	 * @param event The event.
	 */
	@Mod.EventHandler
	public void onPreInitialization(FMLPreInitializationEvent event) {
		this.config.loadValues(event.getSuggestedConfigurationFile());
	}

	/**
	 * Called when the server is starting up.
	 *
	 * @param event The event.
	 */
	@Mod.EventHandler
	public void onServerStaring(FMLServerStartingEvent event) {
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
	 * Called on the server tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		// TODO: Event is not triggering. ~Caleb, 2022-03-06

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
		// TODO: Event is not triggering. ~Caleb, 2022-03-06

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
