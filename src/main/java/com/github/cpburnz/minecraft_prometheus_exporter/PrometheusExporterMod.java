package com.github.cpburnz.minecraft_prometheus_exporter;

import java.io.IOException;
import java.net.BindException;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cpburnz.minecraft_prometheus_exporter.collectors.Chunks;
import com.github.cpburnz.minecraft_prometheus_exporter.collectors.Entities;
import com.github.cpburnz.minecraft_prometheus_exporter.collectors.PlayerStatistics;
import com.github.cpburnz.minecraft_prometheus_exporter.collectors.Players;
import com.github.cpburnz.minecraft_prometheus_exporter.collectors.Teams;
import com.github.cpburnz.minecraft_prometheus_exporter.collectors.Ticks;
import com.github.cpburnz.minecraft_prometheus_exporter.collectors.TileEntities;
import com.github.cpburnz.minecraft_prometheus_exporter.commands.ForgePrometheusCommand;
import com.github.cpburnz.minecraft_prometheus_exporter.prometheus_exporter.Tags;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.relauncher.Side;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

/**
 * The PrometheusExporterMod class defines the mod.
 */
@Mod(
    modid = PrometheusExporterMod.MODID,
    version = Tags.VERSION,
    name = PrometheusExporterMod.NAME,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*")
public class PrometheusExporterMod {

    public static final String NAME = "Prometheus Exporter";
    public static final String MODID = "prometheus_exporter";

    /**
     * The mod instance.
     */
    @Mod.Instance
    public static PrometheusExporterMod INSTANCE;

    /**
     * The logger to use.
     */
    public static final Logger LOG = LogManager.getLogger(MODID);

    private @Nullable HTTPServer http_server;
    /**
     * Whether the exporter is running.
     */
    private boolean is_running;

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
        if (ExporterConfig.collector.jwm_collector) DefaultExports.register(CollectorRegistry.defaultRegistry);

        if (ExporterConfig.collector.entities) new Entities(this.mc_server).register();
        if (ExporterConfig.collector.tileentities) new TileEntities(this.mc_server).register();
        if (ExporterConfig.collector.ticks) new Ticks(this.mc_server).register();
        if (ExporterConfig.collector.chunks) new Chunks(this.mc_server).register();
        if (ExporterConfig.collector.players) new Players(this.mc_server).register();
        if (ExporterConfig.collector.player_statistics) new PlayerStatistics(this.mc_server).register();
        if (ExporterConfig.collector.teams && ModCompat.ServerUtilities.isLoaded())
            new Teams(this.mc_server).register();
    }

    /**
     * Start the HTTP server.
     *
     * @throws IOException When an I/O error occurs while starting the HTTP
     *                     server.
     */
    private void initHttpServer() throws IOException {
        // WARNING: Make sure the HTTP server thread is daemonized, otherwise the
        // Minecraft server process will not properly terminate.
        String address = ExporterConfig.web.listen_address;
        int port = ExporterConfig.web.listen_port;
        try {
            this.http_server = new HTTPServer(address, port, true);
            LOG.info("Listening on {}:{}", address, port);
        } catch (BindException e) {
            LOG.error("Failed to start prometheus exporter, port {} already in use.", port);
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
     * Called before any other phase. Configuration files should be read.
     *
     * @param event The event.
     */
    @Mod.EventHandler
    public void onPreInitialization(FMLPreInitializationEvent event) {
        // Register the server config.
        try {
            ConfigurationManager.registerConfig(ExporterConfig.class);
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }

        if (event.getSide() == Side.CLIENT) return;

        // Register event handlers.
        FMLCommonHandler.instance()
            .bus()
            .register(this);
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
     * Called when the server has started.
     *
     * @param event The event.
     *
     * @throws IOException When an I/O error occurs while starting the HTTP
     *                     server.
     */
    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) throws IOException {
        this.startExporter();
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
     * Start the exporter by starting the HTTP server and registering the
     * metric collectors.
     *
     * @throws IOException           When an I/O error occurs while starting the HTTP
     *                               server.
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
