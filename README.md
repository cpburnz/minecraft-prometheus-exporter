This mod provides a Prometheus exporter for Minecraft. It exports metrics
related to the Minecraft server and the JVM for consumption by the open-source
systems monitoring toolkit, [Prometheus](https://prometheus.io/). The mod is
intended for server-side use, and does not need to be installed client-side.
This is currently made for Minecraft 1.15.2 with Forge 31.2.0.


Installation
------------

The Prometheus Exporter mod only needs to be installed on the server. To
install it, copy the JAR (*Prometheus-Exporter-{MC Version}-forge-{Mod Version}.jar*)
to the server *mods/* directory. Since this mod does not add anything to the
Minecraft world, it can be safely upgraded by simply replacing an older
version with a newer version.


Configuration
-------------

The mod configuration is located at *world/serverconfig/prometheus_exporter-server.toml*.
It will be automatically generated upon server start if it does not already
exist. The default configuration can be seen in the example
[prometheus_exporter-server.toml](https://github.com/cpburnz/minecraft-prometheus-exporter/blob/master/examples/prometheus_exporter-server.toml).


Exporter Output
---------------

A sample output from the exporter can be seen in the example
[output.txt](https://github.com/cpburnz/minecraft-prometheus-exporter/blob/master/examples/output.txt).
