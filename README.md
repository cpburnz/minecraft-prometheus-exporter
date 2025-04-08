This mod provides a Prometheus exporter for Minecraft. It exports metrics
related to the Minecraft server and the JVM for consumption by the open-source
systems monitoring toolkit, [Prometheus]. The mod is intended for server-side
use, and does not need to be installed client-side. This currently has builds
for the following versions:

- [Minecraft 1.21.5 with Fabric 0.119.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.21.5-fabric-1.2.1).
- [Minecraft 1.21.4 with Fabric 0.115.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.21.4-fabric-1.2.1).
- [Minecraft 1.21.4 with Forge 54.0.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.21.4-forge-1.2.1).
- [Minecraft 1.21.4 with NeoForge 21.4.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.21.4-neoforge-1.2.1).
- [Minecraft 1.21.1 with Fabric 0.103.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.21.1-fabric-1.2.1).
- [Minecraft 1.21.1 with Forge 52.0.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.21.1-forge-1.2.1).
- [Minecraft 1.21.1 with NeoForge 21.1.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.21.1-neoforge-1.2.1).
- [Minecraft 1.20.6 with Fabric 0.100.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.20.6-fabric-1.2.1).
- [Minecraft 1.20.6 with Forge 50.1.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.20.6-forge-1.2.1).
- [Minecraft 1.20.6 with NeoForge 20.6.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.20.6-neoforge-1.2.1).
- [Minecraft 1.20.4 with Fabric 0.97.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.20.4-fabric-1.2.1).
- [Minecraft 1.20.4 with Forge 49.1.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.20.4-forge-1.2.1).
- [Minecraft 1.20.4 with NeoForge 20.4.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.20.4-neoforge-1.2.1).
- [Minecraft 1.20.2 with Forge 48.1.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.20.2-forge-1.2.1).
- [Minecraft 1.20.1 with Fabric 0.92.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.20.1-fabric-1.2.1).
- [Minecraft 1.20.1 with Forge 47.1.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.20.1-forge-1.0.0).
- [Minecraft 1.19.4 with Fabric 0.87.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.19.4-fabric-1.2.1).
- [Minecraft 1.19.3 with Forge 44.1.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.19.3-forge-1.0.0).
- [Minecraft 1.19.2 with Fabric 0.77.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.19.2-fabric-1.2.1).
- [Minecraft 1.19.2 with Forge 43.0.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.19.2-forge-1.2.1).
- [Minecraft 1.18.2 with Forge 40.0.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.18.2-forge-1.2.1).
- [Minecraft 1.18.2 with Fabric 0.77.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.18.2-fabric-1.2.1).
- [Minecraft 1.18.1 with Forge 39.0.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.18.1-forge-1.0.0).
- [Minecraft 1.16.5 with Fabric 0.42.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.16.5-fabric-1.2.1).
- [Minecraft 1.16.5 with Forge 36.0.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.16.5-forge-1.2.1).
- [Minecraft 1.15.2 with Forge 21.2.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/v0.3.1).
- [Minecraft 1.14.4 with Forge 28.1.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/v0.3.0).
- [Minecraft 1.12.2 with Forge 14.23.0](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.12.2-forge-1.0.0).
- [Minecraft 1.7.10 with Forge 10.13.4](https://github.com/cpburnz/minecraft-prometheus-exporter/releases/tag/1.7.10-forge-1.1.0).

Is there a newer version of Minecraft not listed? Is the mod outdated for one of
the listed Minecraft versions? Let me know by opening an [issue on GitHub].


Installation
------------

The Prometheus Exporter mod only needs to be installed on the server. It can be
downloaded from [GitHub] and [Curse Forge]. To install it, copy the JAR
(*Prometheus-Exporter-{MC Version}-{Mod Loader}-{Mod Version}.jar*) to the
server *mods/* directory. Since this mod does not add anything to the Minecraft
world, it can be safely upgraded by simply replacing an older version with a
newer version.


Configuration
-------------

The mod configuration is located at *world/serverconfig/prometheus_exporter-server.toml*
with Forge and Fabric, and *config/prometheus_exporter-server.toml* with
NeoForge. It will be automatically generated upon server start if it does not
already exist. The default configuration can be seen in the example
[prometheus_exporter-server.toml].


Exporter
--------

The metrics are documented in [metrics.md].

A sample output from the exporter can be seen in the example [output.txt].


Dashboards
----------

Known compatible Grafana dashboards are listed in [dashboards.md].


[Curse Forge]: https://www.curseforge.com/minecraft/mc-mods/prometheus-exporter
[GitHub]: https://github.com/cpburnz/minecraft-prometheus-exporter/releases
[Prometheus]: https://prometheus.io/
[dashboards.md]: https://github.com/cpburnz/minecraft-prometheus-exporter/blob/master/dashboards.md
[issue on GitHub]: https://github.com/cpburnz/minecraft-prometheus-exporter/issues
[metrics.md]: https://github.com/cpburnz/minecraft-prometheus-exporter/blob/master/metrics.md
[output.txt]: https://github.com/cpburnz/minecraft-prometheus-exporter/blob/master/examples/output.txt
[prometheus_exporter-server.toml]: https://github.com/cpburnz/minecraft-prometheus-exporter/blob/master/examples/prometheus_exporter-server.toml
