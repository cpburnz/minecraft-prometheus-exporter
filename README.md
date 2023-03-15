This mod provides a Prometheus exporter for Minecraft. It exports metrics
related to the Minecraft server and the JVM for consumption by the open-source
systems monitoring toolkit, [Prometheus]. The mod is intended for server-side
use, and does not need to be installed client-side. This currently has builds
for the following versions:

- Minecraft 1.19.3 with Forge 44.1.0.
- Minecraft 1.19.2 with Forge 43.0.0.
- Minecraft 1.18.1 with Forge 39.0.0.
- Minecraft 1.16.5 with Forge 36.2.0.
- Minecraft 1.15.2 with Forge 21.2.0.
- Minecraft 1.14.4 with Forge 28.1.0.
- Minecraft 1.12.2 with Forge 14.23.0.


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

The mod configuration is located at *world/serverconfig/prometheus_exporter-server.toml*.
It will be automatically generated upon server start if it does not already
exist. The default configuration can be seen in the example [prometheus_exporter-server.toml].


Exporter Output
---------------

A sample output from the exporter can be seen in the example [output.txt].
Please note as of version 1.0.0 the "mc_world_tick_seconds" metric has been
renamed to "mc_dimension_tick_seconds".


[Curse Forge]: https://www.curseforge.com/minecraft/mc-mods/prometheus-exporter
[GitHub]: https://github.com/cpburnz/minecraft-prometheus-exporter/releases
[Prometheus]: https://prometheus.io/
[output.txt]: https://github.com/cpburnz/minecraft-prometheus-exporter/blob/mc1.19.3/examples/output.txt
[prometheus_exporter-server.toml]: https://github.com/cpburnz/minecraft-prometheus-exporter/blob/mc1.19.3/examples/prometheus_exporter-server.toml
