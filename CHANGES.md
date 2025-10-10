Change History
==============

1.4.0 for Minecraft 1.7.10 with Forge 10.13.0 (2025-10-10)
----------------------------------------------------------

New features:

- Separated the Collectors out into individual files, located in collectors/
- Added new collectors for metrics as driven by ServerUtilities (FTBTeams)
- Added a new collector for TileEntities, per dim & identifier
- Generally user newer 1.7.10 mod format & try to unify both

1.3.0 for Minecraft 1.7.10 with Forge 10.13.0 (2025-09-13)
----------------------------------------------------------

New features:

- [Issue #38](https://github.com/cpburnz/minecraft-prometheus-exporter/issues/38): The exporter can now be managed with the commands "/prometheus start", "/prometheus stop", and "/prometheus restart". The player requires op or level 4 permissions which can be configured by the "command.permission_level" setting.

- [Issue #39](https://github.com/cpburnz/minecraft-prometheus-exporter/issues/39)/[Issue #40](https://github.com/cpburnz/minecraft-prometheus-exporter/issues/40): Added player stats in the new "mc_player_stat_total" metric. These can be disabled by setting the "collector.mc_player_stats" setting to false.


1.2.1 for Minecraft 1.7.10 with Forge 10.13.0 (Unreleased)
----------------------------------------------------------

Bug fixes:

- Prevent possible crash on bad start-up.


1.2.0 for Minecraft 1.7.10 with Forge 10.13.0 (Unreleased)
----------------------------------------------------------

New features:

- Added the "collector.mc_dimension_tick_errors" setting to control how to handle inconsistent dimension ticks from misbehaved mods. The new default behavior is to log a debug message rather than crash.

Bug fixes:

- Support inconsistent dimension ticks from misbehaved mods.
- Support multithreaded dimension ticks.

Miscellaneous:

- Minor documentation.


1.1.0 for Minecraft 1.7.10 with Forge 10.13.0 (2024-04-11)
----------------------------------------------------------

New features:

- [Pull #17](https://github.com/cpburnz/minecraft-prometheus-exporter/pull/17): Add entity tracking with the "mc_entities_total" metric. This can be disabled by setting "collector.mc_entities" to "false" in the config.

Bug fixes:

- The "mc_player_list" metric sets the "id" label with the profile UUID only when it is available.

Miscellaneous:

- Added "HACKING.md".
- Added "metrics.md".


1.0.0 for Minecraft 1.7.10 with Forge 10.13.0 (2023-12-17)
----------------------------------------------------------

New features:

- [Issue #13](https://github.com/cpburnz/minecraft-prometheus-exporter/issues/13)/[Pull #15](https://github.com/cpburnz/minecraft-prometheus-exporter/pull/15): Support MC 1.7.10.
