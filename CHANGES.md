Change History
==============


1.2.0 for MC 1.19.3 (2024-04-20)
--------------------------------

New features:

- Added the "collector.mc_dimension_tick_errors" setting to control how to handle inconsistent dimension ticks from misbehaved mods. The new default behavior is to log a debug message rather than crash.

Bug fixes:

- Do not warn when the mod is not installed on the client (Forge only backported to MC 1.19.2 & 1.19.4).
- Support inconsistent dimension ticks from misbehaved mods.
- Support multithreaded dimension ticks.

Miscellaneous:

- Minor documentation.



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
