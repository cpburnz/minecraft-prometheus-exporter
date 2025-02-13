This mod provides a Prometheus exporter for Minecraft. It exports metrics
related to the Minecraft server and the JVM for consumption by the open-source
systems monitoring toolkit, [Prometheus]. The mod is intended for server-side
use, and does not need to be installed client-side. You can find the latest
builds for various versions of Minecraft and mod loaders in [Releases].

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
with Forge and Fabric. It will be automatically generated upon server start if
it does not already exist. The default configuration can be seen in the example
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
[Releases]: https://github.com/cpburnz/minecraft-prometheus-exporter/wiki/Releases
[dashboards.md]: https://github.com/cpburnz/minecraft-prometheus-exporter/blob/mc1.16.x/dashboards.md
[issue on GitHub]: https://github.com/cpburnz/minecraft-prometheus-exporter/issues
[metrics.md]: https://github.com/cpburnz/minecraft-prometheus-exporter/blob/mc1.16.x/metrics.md
[output.txt]: https://github.com/cpburnz/minecraft-prometheus-exporter/blob/mc1.16.x/examples/output.txt
[prometheus_exporter-server.toml]: https://github.com/cpburnz/minecraft-prometheus-exporter/blob/mc1.16.x/examples/prometheus_exporter-server.toml
