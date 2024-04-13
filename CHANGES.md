Change History
==============


1.1.0.post1 for MC 1.18.2 (Unreleased)
--------------------------------------

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
