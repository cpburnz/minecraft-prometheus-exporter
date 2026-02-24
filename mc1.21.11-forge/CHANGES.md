Change History
==============


1.4.0 for Minecraft 1.21.11 with Forge 61.0.0 (2026-02-23)
----------------------------------------------------------

New features:

- [Issue #44](https://github.com/cpburnz/minecraft-prometheus-exporter/issues/44): Support Minecraft 1.21.11 with Forge.

- Added scrape stats in the new "mc_scrape_duration_seconds" metric.


1.3.0 for Minecraft 1.21.11 with Forge 61.0.0 (2025-09-13)
----------------------------------------------------------

New features:

- [Issue #38](https://github.com/cpburnz/minecraft-prometheus-exporter/issues/38): The exporter can now be managed with the commands "/prometheus start", "/prometheus stop", and "/prometheus restart". The player requires op or level 4 permissions which can be configured by the "command.permission_level" setting.

- [Issue #39](https://github.com/cpburnz/minecraft-prometheus-exporter/issues/39)/[Issue #40](https://github.com/cpburnz/minecraft-prometheus-exporter/issues/40): Added player stats in the new "mc_player_stat_total" metric. These can be disabled by setting the "collector.mc_player_stats" setting to false.


1.2.1 for Minecraft 1.21.11 with Forge 61.0.0 (Unreleased)
----------------------------------------------------------

Bug fixes:

- Prevent possible crash on bad start-up.


1.2.0 for Minecraft 1.21.11 with Forge 61.0.0 (Unreleased)
----------------------------------------------------------

New features:

- Added the "collector.mc_dimension_tick_errors" setting to control how to handle inconsistent dimension ticks from misbehaved mods. The new default behavior is to log a debug message rather than crash.

Bug fixes:

- Do not warn when the mod is not installed on the client.
- Support inconsistent dimension ticks from misbehaved mods.
- Support multithreaded dimension ticks.

Miscellaneous:

- Minor documentation.


1.1.0 for Minecraft 1.21.11 with Forge 61.0.0 (Unreleased)
----------------------------------------------------------

New features:

- Add entity tracking with the "mc_entities_total" metric. This can be disabled by setting "collector.mc_entities" to "false" in the config.

Bug fixes:

- Client-side server and level ticks no longer crash Minecraft.

Miscellaneous:

- Added "HACKING.md".
- Added "dashboards.md".
- Added "metrics.md".
