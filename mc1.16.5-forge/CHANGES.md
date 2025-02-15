
Change History
==============


1.2.1 for Minecraft 1.16.5 with Forge 36.0.0 (2025-02-15)
---------------------------------------------------------

Bug fixes:

- Prevent possible crash on bad start-up.


1.2.0 for Minecraft 1.16.5 with Forge 36.0.0 (Unreleased)
---------------------------------------------------------

New features:

- Added the "collector.mc_dimension_tick_errors" setting to control how to handle inconsistent dimension ticks from misbehaved mods. The new default behavior is to log a debug message rather than crash.

Bug fixes:

- Support inconsistent dimension ticks from misbehaved mods.
- Support multithreaded dimension ticks.

Miscellaneous:

- Minor documentation.



1.1.0 for Minecraft 1.16.5 with Forge 36.0.0 (Unreleased)
---------------------------------------------------------

New features:

- Add entity tracking with the "mc_entities_total" metric. This can be disabled by setting "collector.mc_entities" to "false" in the config.

Bug fixes:

- Client-side server and level ticks no longer crash Minecraft.

Miscellaneous:

- Added "HACKING.md".
- Added "dashboards.md".
- Added "metrics.md".


1.0.0 for Minecraft 1.16.5 with Forge 36.2.0 (2022-11-24)
---------------------------------------------------------

- [Issue #9]: Release 1.0.0 for 1.16.5.
- Breaking Change: The "mc_world_tick_seconds" metric has been renamed to "mc_dimension_tick_seconds".


[Issue #9]: https://github.com/cpburnz/minecraft-prometheus-exporter/issues/9
