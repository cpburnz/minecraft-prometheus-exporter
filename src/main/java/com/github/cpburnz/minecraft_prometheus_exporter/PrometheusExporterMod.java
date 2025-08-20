package com.github.cpburnz.minecraft_prometheus_exporter;

import java.io.IOException;
import java.net.BindException;

import javax.annotation.Nullable;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

import com.github.cpburnz.minecraft_prometheus_exporter.collectors.ForgeMinecraftCollector;
import com.github.cpburnz.minecraft_prometheus_exporter.commands.ForgePrometheusCommand;
import com.github.cpburnz.minecraft_prometheus_exporter.config.ForgeModConfig;
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
	 * The mod instance.
	 */
	@Mod.Instance
	public static PrometheusExporterMod INSTANCE;

	/**
	 * The logger to use.
	 */
	public static final Logger LOG = LogManager.getLogger(Tags.MODID);

	/**
	 * The mod configuration.
	 */
	private ForgeModConfig config;

	/**
	 * The HTTP server.
	 */
	private @Nullable HTTPServer http_server;

	/**
	 * Whether the exporter is running.
	 */
	private boolean is_running;

	/**
	 * The Minecraft metrics collector.
	 */
	private @Nullable ForgeMinecraftCollector mc_collector;

	/**
	 * The Minecraft server.
	 */
	private MinecraftServer mc_server;

	/**
	 * Constructs the instance.
	 */
	public PrometheusExporterMod() {
		// Nothing to do.
	}

	/**
	 * Unregister the metrics collectors.
	 */
	private void closeCollectors() {
		// Unregister all collectors.
		CollectorRegistry.defaultRegistry.clear();
		this.mc_collector = null;
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
		}
	}

	/**
	 * Register the metrics collectors.
	 */
	private void initCollectors() {
		// Collect JVM stats.
		if (this.config.collector_jvm) {
			DefaultExports.register(CollectorRegistry.defaultRegistry);
		}

		// Collect Minecraft stats.
		if (this.config.collector_mc) {
			this.mc_collector = new ForgeMinecraftCollector(this.config, this.mc_server);
			this.mc_collector.register();
		}
	}

	/**
	 * Start the HTTP server.
	 *
	 * @throws IOException When an I/O error occurs while starting the HTTP
	 * server.
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
			LOG.error("Failed to start HTTP server, port {} already in use.", port);
		}
	}

	/**
	 * Check whether the exporter is running.
	 *
	 * @return Whether the exporter is running.
	 */
	public boolean isExporterRunning() {
		return this.is_running;
	}

	/**
	 * Called on a dimension tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onDimensionTick(TickEvent.WorldTickEvent event) {
		// Record dimension tick.
		if (this.mc_collector != null) {
			WorldProvider dim = event.world.provider;
			if (event.phase == TickEvent.Phase.START) {
				this.mc_collector.startDimensionTick(dim);
			} else if (event.phase == TickEvent.Phase.END) {
				this.mc_collector.stopDimensionTick(dim);
			}
		}
	}
	/**
	 * Called before any other phase. Configuration files should be read.
	 *
	 * @param event The event.
	 */
	@Mod.EventHandler
	public void onPreInitialization(FMLPreInitializationEvent event) {
		// Load the config.
		this.config = new ForgeModConfig();
		this.config.loadValues(event.getSuggestedConfigurationFile());

		// Register event handlers.
		FMLCommonHandler.instance().bus().register(this);
	}

	/**
	 * Called when the server has started.
	 *
	 * @param event The event.
	 *
	 * @throws IOException When an I/O error occurs while starting the HTTP
	 * server.
	 */
	@Mod.EventHandler
	public void onServerStarted(FMLServerStartedEvent event) throws IOException {
		this.startExporter();
	}

	/**
	 * Called when the server is starting up.
	 *
	 * @param event The event.
	 */
	@Mod.EventHandler
	public void onServerStarting(FMLServerStartingEvent event) {
		// Register server commands in this event handler.
		event.registerServerCommand(new ForgePrometheusCommand());

		// Record the Minecraft server.
		this.mc_server = event.getServer();
	}

	/**
	 * Called when the server has stopped.
	 *
	 * @param event The event.
	 */
	@Mod.EventHandler
	public void onServerStopped(FMLServerStoppedEvent event) {
		this.stopExporter();
		this.mc_server = null;
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
	 * Start the exporter by starting the HTTP server and registering the
	 * metric collectors.
	 *
	 * @throws IOException When an I/O error occurs while starting the HTTP
	 * server.
	 * @throws IllegalStateException When the exporter is already running.
	 */
	public void startExporter() throws IOException {
		if (this.is_running) {
			throw new IllegalStateException("Exporter is already running.");
		}

		// Start HTTP server.
		this.initHttpServer();

		// Register collectors.
		this.initCollectors();

		this.is_running = true;
	}

	/**
	 * Stop the exporter by stopping the HTTP server and unregistering the metric
	 * collectors.
	 *
	 * @throws IllegalStateException When the exporter is not running.
	 */
	public void stopExporter() {
		if (!this.is_running) {
			throw new IllegalStateException("Exporter is not running.");
		}

		// Close collectors.
		this.closeCollectors();

		// Stop HTTP server.
		this.closeHttpServer();

		this.is_running = false;
	}
}
