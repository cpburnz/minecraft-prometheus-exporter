Metrics
=======


JVM Metrics: jvm
----------------

See the [JVM instrumentation module](https://prometheus.github.io/client_java/instrumentation/jvm/) and [Simpleclient JVM Metrics](https://prometheus.github.io/client_java/migration/simpleclient/#jvm-metrics) for a list of the 0.16.0 metric names.


Minecraft Metrics: mc
---------------------


### mc_dimension_chunks_loaded {id, name}

The number of loaded dimension chunks.

| Label | Meaning                |
|-------|------------------------|
| id    | The dimension id [^1]. |
| name  | The dimension name.    |


### mc_dimension_tick_seconds {id, name}

A histogram of the dimension tick times (in seconds).

| Label | Meaning                |
|-------|------------------------|
| id    | The dimension id [^1]. |
| name  | The dimension name.    |

_Changed in version 1.0_: Renamed metric "mc_world_tick_seconds" to "mc_dimension_tick_seconds".


#### mc_dimension_tick_seconds_bucket {id, name, le}

The number of dimension ticks per quantile.

| Label | Meaning                                                |
|-------|--------------------------------------------------------|
| le    | The quantile: 0.01, 0.025, 0.05, 0.10, 0.25, 0.5, 1.0. |


#### mc_dimension_tick_seconds_count {id, name}

The number of dimension ticks.


#### mc_dimension_tick_seconds_created {id, name}

The UNIX timestamp when the metric was created (in seconds).


#### mc_dimension_tick_seconds_sum {id, name}

The sum of the duration of the dimension ticks (in seconds).


### mc_entities_total {dim, dim_id, id, type}

The number of entities in each dimension by type.

| Label  | Meaning                                                         |
|--------|-----------------------------------------------------------------|
| dim    | The dimension name.                                             |
| dim_id | The dimension id [^1].                                          |
| id     | The entity id.                                                  |
| type   | The entity type: the mob or creature name; or "Item" for items. |

_In Minecraft 1.7.10 only_: The "id" label will be set.


### mc_player_list {id, name}

The players connected to the server.

| Label  | Meaning          |
|--------|------------------|
| id     | The player UUID. |
| name   | The player name. |


### mc_player_stat_total {code, name, player_id, player_name>

The general player stats. These are persisted after player sign-out. Achievements and item stats are not currently recorded.

| Label       | Meaning                |
|-------------|------------------------|
| code        | The stat code name.    |
| name        | The stat display name. |
| player_id   | The player UUID.       |
| player_name | The player name.       |

See [examples/output.txt](https://github.com/cpburnz/minecraft-prometheus-exporter/blob/mc1.7.10/examples/output.txt) for the list of general stats.


### mc_server_tick_seconds

A histogram of the server tick times (in seconds).


#### mc_server_tick_seconds_bucket {le},

The number of server ticks per quantile.

| Label | Meaning                                                |
|-------|--------------------------------------------------------|
| le    | The quantile: 0.01, 0.025, 0.05, 0.10, 0.25, 0.5, 1.0. |


#### mc_server_tick_seconds_count

The number of server ticks.


#### mc_server_tick_seconds_created

The UNIX timestamp when the metric was created (in seconds).


#### mc_server_tick_seconds_sum

The sum of the duration of the server ticks (in seconds).

### mc_dimension_tileentities {id, name}

The number of loaded ticking tileentities in a dim.

| Label  | Meaning             |
|--------|---------------------|
| id     | The dimension id.   |
| name   | The dimension name. |

#### mc_dimension_tileentities_detailed {id, name}

The number of loaded ticking tileentities in a dim.

| Label    | Meaning                            |
|----------|------------------------------------|
| dim_id   | The dimension id.                  |
| dim      | The dimension name.                |
| te_class | The Tile Entites class identifier. |
| te_name  | The Tile Entities name.            |


### mc_teams_chunk_claims {team_id, team_name, team_type, dim_id, dim_name}

The number of chunks claimed by each team in each dimension.

| Label     | Meaning                                        |
|-----------|------------------------------------------------|
| team_id   | The unique team identifier.                    |
| team_name | The team name.                                 |
| team_type | The team type (e.g., "admin", "player", etc.). |
| dim_id    | The dimension id.                              |
| dim_name  | The dimension name.                            |


#### mc_teams_chunk_loads {team_id, team_name, team_type, dim_id, dim_name}

Number of chunks being force loaded per team per dim.

| Label     | Meaning                                        |
|-----------|------------------------------------------------|
| team_id   | The unique team identifier.                    |
| team_name | The team name.                                 |
| team_type | The team type (e.g., "admin", "player", etc.). |
| dim_id    | The dimension id.                              |
| dim_name  | The dimension name.                            |


#### mc_teams_players {team_id, team_name, player_uuid, player_name}

The players belonging to each team.

| Label        | Meaning                     |
|--------------|-----------------------------|
| team_id      | The unique team identifier. |
| team_name    | The team name.              |
| player_uuid  | The player's UUID.          |
| player_name  | The player's name.          |


[^1]: Starting in Minecraft 1.16, dimensions no longer have ids. In order to maintain compatibility with older versions, in Minecraft 1.16+ the ids for the overworld, the nether, and the end will be hardcoded as 0, -1, and 1, respectively. Custom dimensions will have an id computed as `dim.hashCode()`.
