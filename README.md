This mod provides a Prometheus exporter for Minecraft. It exports metrics
related to the Minecraft server and the JVM for consumption by the open-source
systems monitoring toolkit, [Prometheus]. The mod is intended for server-side
use, and does not need to be installed client-side. This currently has builds
for the following versions:

- Minecraft 1.20.1 with Forge 47.1.0.
- Minecraft 1.19.3 with Forge 44.1.0.
- Minecraft 1.19.2 with Forge 43.0.0.
- Minecraft 1.18.1 with Forge 39.0.0.
- Minecraft 1.16.5 with Forge 36.2.0.
- Minecraft 1.15.2 with Forge 21.2.0.
- Minecraft 1.14.4 with Forge 28.1.0.
- Minecraft 1.12.2 with Forge 14.23.0.
- Minecraft 1.7.10 with Forge 10.13.4.

Is there a newer version of Minecraft not listed? Let me know by opening an
[issue on GitHub].


Installation
------------

The Prometheus Exporter mod only needs to be installed on the server. It can be
downloaded from [GitHub] and [Curse Forge]. To install it, copy the JAR
(*Prometheus-Exporter-{MC Version}-forge-{Mod Version}.jar*) to the server
*mods/* directory. Since this mod does not add anything to the Minecraft world,
it can be safely upgraded by simply replacing an older version with a newer
version.


Configuration
-------------

The mod configuration is located at *config/prometheus_exporter.cfg*.
It will be automatically generated upon server start if it does not already
exist. The default configuration can be seen in the example [prometheus_exporter.cfg].


Exporter Output
---------------

A sample output from the exporter can be seen in the example [output.txt].
Please note as of version 1.0.0 the "mc_world_tick_seconds" metric has been
renamed to "mc_dimension_tick_seconds".


Dashboards
----------

Known compatible Grafana dashboards:

- [Minecraft Server Stats] built by [randombk].


[Curse Forge]: https://www.curseforge.com/minecraft/mc-mods/prometheus-exporter
[GitHub]: https://github.com/cpburnz/minecraft-prometheus-exporter/releases
[Minecraft Server Stats]: https://grafana.com/grafana/dashboards/16508-minecraft-server-stats/
[Prometheus]: https://prometheus.io/
[issue on GitHub]: https://github.com/cpburnz/minecraft-prometheus-exporter/issues
[output.txt]: https://github.com/cpburnz/minecraft-prometheus-exporter/blob/mc1.7.10/examples/output.txt
[prometheus_exporter.cfg]: https://github.com/cpburnz/minecraft-prometheus-exporter/blob/mc1.7.10/examples/prometheus_exporter.cfg
[randombk]: https://github.com/randombk
