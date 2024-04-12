Change History
==============


1.1.0 for MC 1.18.2 (TBD)
-------------------------

New features:

- Support MC 1.18.2.
- Add entity tracking with the "mc_entities_total" metric. This can be disabled by setting "collector.mc_entities" to "false" in the config.

Bug fixes:

- The "mc_player_list" metric sets the "id" label with the profile UUID only when it is available.
- Client-side server and world ticks no longer crash Minecraft.

Miscellaneous:

- Added "HACKING.md".
- Added "metrics.md".
