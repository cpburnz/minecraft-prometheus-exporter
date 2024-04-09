
Change History
==============


1.1.0 for MC 1.18.2 (TBD)
-------------------------

New features:

- Support MC 1.18.2.
- Add entity tracking with the "mc_entities_total{dim, dim_id, type}" metric. This can be disabled by setting "collector.mc_entities" to "false" in the config.

Bug fixes:

- The "mc_player_list" metric sets the "id" attribute with the profile UUID only when it is available.
