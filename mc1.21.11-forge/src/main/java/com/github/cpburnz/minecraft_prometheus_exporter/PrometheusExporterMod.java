package com.github.cpburnz.minecraft_prometheus_exporter;

import java.io.IOException;
import java.net.BindException;
import javax.annotation.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

import com.github.cpburnz.minecraft_prometheus_exporter.forge.ForgeMinecraftCollector;
import com.github.cpburnz.minecraft_prometheus_exporter.forge.ForgePrometheusCommand;
import com.github.cpburnz.minecraft_prometheus_exporter.forge.ForgeServerConfig;

/**
 * The PrometheusExporterMod class defines the mod.
 */
@Mod(PrometheusExporterMod.MOD_ID)
public class PrometheusExporterMod {

	/**
	 * The mod instance.
	 */
	private static PrometheusExporterMod INSTANCE;

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

	/**
	 * The mod id.
	 */
	public static final String MOD_ID = "prometheus_exporter";

	/**
	 * The server configuration.
	 */
	private final ForgeServerConfig config;

	/**
	 * The HTTP server.
	 */
	@Nullable
	private HTTPServer http_server;

	/**
	 * Whether the exporter is running.
	 */
	private boolean is_running;

	/**
	 * The Minecraft metrics collector.
	 */
	@Nullable
	private ForgeMinecraftCollector mc_collector;

	/**
	 * The Minecraft server.
	 */
	@Nullable
	private MinecraftServer mc_server;

	/**
	 * Construct the instance.
	 *
	 * @param context The mod loading context.
	 */
	public PrometheusExporterMod(FMLJavaModLoadingContext context) {
		// Record instance.
		INSTANCE = this;

		// Register to receive events.
		MinecraftForge.EVENT_BUS.register(this);

		// Register the server config.
		this.config = new ForgeServerConfig();
		this.config.register(context);
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
			DefaultExports.register(CollectorRegistry.defaultRegistry);
		}

		// Collect Minecraft stats.
		if (this.config.collector_mc) {
			this.mc_collector = new ForgeMinecraftCollector(
				this.config, this.mc_server
			);
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
		try {
			this.http_server = new HTTPServer(address, port, true);
			LOG.info("Listening on {}:{}", address, port);
		} catch (BindException e) {
			LOG.error("Failed to start HTTP server, port {} already in use.", port);
		}
	}

	/**
	 * Get the mod instance.
	 *
	 * @return The instance.
	 */
	public static PrometheusExporterMod instance() {
		return INSTANCE;
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
	 * Called before a dimension tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onDimensionTickPre(TickEvent.LevelTickEvent.Pre event) {
		// Record dimension tick.
		if (this.mc_collector != null && event.side() == LogicalSide.SERVER) {
			@SuppressWarnings("resource")
			ResourceKey<Level> dim = event.level().dimension();
			this.mc_collector.startDimensionTick(dim);
		}
	}

	/**
	 * Called after a dimension tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onDimensionTickPost(TickEvent.LevelTickEvent.Post event) {
		// Record dimension tick.
		if (this.mc_collector != null && event.side() == LogicalSide.SERVER) {
			@SuppressWarnings("resource")
			ResourceKey<Level> dim = event.level().dimension();
			this.mc_collector.stopDimensionTick(dim);
		}
	}

	/**
	 * Called when commands should be registered.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		ForgePrometheusCommand.register(event.getDispatcher(), this.config);
	}

	/**
	 * Called before the server begins loading anything.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onServerAboutToStart(ServerAboutToStartEvent event) {
		// NOTE: This appears to be the earliest event where Forge has loaded the
		// server-side config.
		this.config.loadValues();
	}

	/**
	 * Called when the server has started.
	 *
	 * @param event The event.
	 *
	 * @throws IOException When an I/O error occurs while starting the HTTP
	 * server.
	 */
	@SubscribeEvent
	public void onServerStarted(ServerStartedEvent event) throws IOException {
		// Record the Minecraft server.
		this.mc_server = event.getServer();

		// Start the exporter.
		this.startExporter();
	}

	/**
	 * Called when the server has stopped.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onServerStopped(ServerStoppedEvent event) {
		this.stopExporter();
		this.mc_server = null;
	}

	/**
	 * Called before the server tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onServerTickPre(TickEvent.ServerTickEvent.Pre event) {
		// Record server tick.
		if (this.mc_collector != null) {
			this.mc_collector.startServerTick();
		}
	}

	/**
	 * Called after the server tick.
	 *
	 * @param event The event.
	 */
	@SubscribeEvent
	public void onServerTickPost(TickEvent.ServerTickEvent.Post event) {
		// Record server tick.
		if (this.mc_collector != null) {
			this.mc_collector.stopServerTick();
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
