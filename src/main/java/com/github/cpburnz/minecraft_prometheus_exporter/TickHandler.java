package com.github.cpburnz.minecraft_prometheus_exporter;

import net.minecraft.world.WorldProvider;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class TickHandler {

    /**
     * Called on the server tick.
     *
     * @param event The event.
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        // Record server tick.
        if (Prometheus_Exporter.mc_collector != null) {
            if (event.phase == TickEvent.Phase.START) {
                Prometheus_Exporter.mc_collector.startServerTick();
            } else if (event.phase == TickEvent.Phase.END) {
                Prometheus_Exporter.mc_collector.stopServerTick();
            }
        }
    }

    /**
     * Called on a dimension tick.
     *
     * @param event The event.
     */
    @SubscribeEvent
    public void onDimensionTick(TickEvent.WorldTickEvent event) {
        // Record dimension tick.
        if (Prometheus_Exporter.mc_collector != null) {
            WorldProvider dim = event.world.provider;
            if (event.phase == TickEvent.Phase.START) {
                Prometheus_Exporter.mc_collector.startDimensionTick(dim);
            } else if (event.phase == TickEvent.Phase.END) {
                Prometheus_Exporter.mc_collector.stopDimensionTick(dim);
            }
        }
    }
}
