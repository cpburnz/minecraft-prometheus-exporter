Change History
==============


1.2.1 for Minecraft 1.20.1 with Forge 47.0.0 (2024-11-02)
---------------------------------------------------------

New features:

- Add entity tracking with the "mc_entities_total" metric. This can be disabled by setting "collector.mc_entities" to "false" in the config.
- Added the "collector.mc_dimension_tick_errors" setting to control how to handle inconsistent dimension ticks from misbehaved mods. The new default behavior is to log a debug message rather than crash.

Bug fixes:

- Client-side server and level ticks no longer crash Minecraft.
- Support inconsistent dimension ticks from misbehaved mods.
- Support multithreaded dimension ticks.
- Prevent possible crash on bad start-up.
