Change History
==============


1.2.1 for Minecraft 1.18.2 with Forge 40.0.0 (2025-03-22)
---------------------------------------------------------

Bug fixes:

- Prevent possible crash on bad start-up.


1.2.0 for Minecraft 1.18.2 with Forge 40.0.0 (2024-04-20)
---------------------------------------------------------

New features:

- Added the "collector.mc_dimension_tick_errors" setting to control how to handle inconsistent dimension ticks from misbehaved mods. The new default behavior is to log a debug message rather than crash.

Bug fixes:

- Do not warn when the mod is not installed on the client.
- Support inconsistent dimension ticks from misbehaved mods.
- Support multithreaded dimension ticks.

Miscellaneous:

- Minor documentation.


1.1.0 for Minecraft 1.18.2 with Forge 40.0.0 (2024-04-11)
---------------------------------------------------------

New features:

- Add entity tracking with the "mc_entities_total" metric. This can be disabled by setting "collector.mc_entities" to "false" in the config.

Bug fixes:

- Client-side server and level ticks no longer crash Minecraft.

Miscellaneous:

- Added "HACKING.md".
- Added "dashboards.md".
- Added "metrics.md".
