Change History
==============


1.1.0 for MC 1.19.3 (2024-04-13)
--------------------------------

New features:

- Add entity tracking with the "mc_entities_total" metric. This can be disabled by setting "collector.mc_entities" to "false" in the config.

Bug fixes:

- The "mc_player_list" metric sets the "id" label with the profile UUID only when it is available.
- Client-side server and world ticks no longer crash Minecraft.

Miscellaneous:

- Added "HACKING.md".
- Added "dashboards.md".
- Added "metrics.md".


1.0.0 for MC 1.19.3 (2023-03-14)
--------------------------------

- Support MC 1.19.3.


1.0.0 for MC 1.19.2 (2022-10-27)
--------------------------------

- [Pull #7](https://github.com/cpburnz/minecraft-prometheus-exporter/pull/7): Update to MC 1.19.2.
