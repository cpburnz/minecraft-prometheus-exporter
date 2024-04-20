Change History
==============


1.2.0 for MC 1.18.2 (TBD)
--------------------------------

New features:

- Added the "collector.mc_dimension_tick_errors" setting to control how to handle inconsistent dimension ticks from misbehaved mods. The new default behavior is to log a debug message rather than crash.

Bug fixes:

- Do not warn when the mod is not installed on the client.
- Support inconsistent dimension ticks from misbehaved mods.
- Support multithreaded dimension ticks.

Miscellaneous:

- Minor documentation.


1.1.0 for MC 1.18.2 (2024-04-11)
--------------------------------

New features:

- Support MC 1.18.2.
- Add entity tracking with the "mc_entities_total" metric. This can be disabled by setting "collector.mc_entities" to "false" in the config.

Bug fixes:

- The "mc_player_list" metric sets the "id" label with the profile UUID only when it is available.
- Client-side server and world ticks no longer crash Minecraft.

Miscellaneous:

- Added "HACKING.md".
- Added "metrics.md".


1.0.0 for MC 1.18.1 (2022-03-06)
--------------------------------

- Properly clean up on world shutdown to fix client crashes when starting a second world.
- Breaking Change: The "mc_world_tick_seconds" metric has been renamed to "mc_dimension_tick_seconds".


0.3.3 for MC 1.18.1 (2022-02-28)
--------------------------------

- [Issue #4](https://github.com/cpburnz/minecraft-prometheus-exporter/issues/4): Update to MC 1.18.1.
