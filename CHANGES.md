
Change History
==============


1.2.1 for Minecraft 1.19.2 (2024-09-04)
---------------------------------------

New features:

- Add entity tracking with the "mc_entities_total" metric. This can be disabled by setting "collector.mc_entities" to "false" in the config.
- Added the "collector.mc_dimension_tick_errors" setting to control how to handle inconsistent dimension ticks from misbehaved mods. The new default behavior is to log a debug message rather than crash.

Bug fixes:

- Client-side server and level ticks no longer crash Minecraft.
- Do not warn when the mod is not installed on the client.
- Support inconsistent dimension ticks from misbehaved mods.
- Support multithreaded dimension ticks.
- Prevent possible crash on bad start-up.


1.0.0 for Minecraft 1.19.2 (2022-10-27)
---------------------------------------

- [Pull #7](https://github.com/cpburnz/minecraft-prometheus-exporter/pull/7): Update to MC 1.19.2.
