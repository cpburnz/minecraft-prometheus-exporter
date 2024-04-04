
Change History
==============

1.1.0 for MC 1.7.10 (TBD)
--------------------------------

New features:

- [Pull #17](https://github.com/cpburnz/minecraft-prometheus-exporter/pull/17): Add entity tracking with the "mc_entity_list{type, id}" metric. This can be disabled by setting "collector.mc_entity_list" to "false" in the config.

Bug fixes:

- The "mc_player_list" metric sets the "id" attribute with the profile UUID when it is available.


1.0.0 for MC 1.7.10 (2023-12-17)
--------------------------------

- [Issue #13](https://github.com/cpburnz/minecraft-prometheus-exporter/issues/13)/[Pull #15](https://github.com/cpburnz/minecraft-prometheus-exporter/pull/15): Support MC 1.7.10.
