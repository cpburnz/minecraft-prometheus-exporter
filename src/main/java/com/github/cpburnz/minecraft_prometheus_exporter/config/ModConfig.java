package com.github.cpburnz.minecraft_prometheus_exporter.config;

/**
 * The ModConfig class defines the base mod config. This is used to store
 * settings from the "prometheus_exporter.cfg" config file.
 */
public abstract class ModConfig {

	/**
	 * Whether to collect metrics about the JVM process.
	 */
	public boolean collector_jvm;

	/**
	 * Whether to collect metrics about the Minecraft server.
	 */
	public boolean collector_mc;

	/**
	 * How to handle dimension (world) tick event errors.
	 */
	public TickErrorPolicy collector_mc_dimension_tick_errors;

	/**
	 * Whether to collect metrics about the entities in each dimension (world).
	 */
	public boolean collector_mc_entities;

	/**
	 * Whether to collect metrics about general player stats.
	 */
	public boolean collector_mc_player_stats;

	/**
	 * Whether the config has been loaded.
	 */
	private boolean is_loaded;

	/**
	 * The IP address to listen on.
	 */
	public String web_listen_address;

	/**
	 * The TCP port to listen on.
	 */
	public int web_listen_port;

	/**
	 * @return Whether the config is loaded.
	 */
	public boolean isLoaded() {
		return this.is_loaded;
	}

	/**
	 * Set whether the config is loaded.
	 *
	 * @param is_loaded Is the config loaded.
	 */
	@SuppressWarnings("SameParameterValue")
	protected void setIsLoaded(boolean is_loaded) {
		this.is_loaded = is_loaded;
	}

	/**
	 * The TickErrorPolicy enum defines how to handle dimension (world) tick event
	 * errors.
	 */
	public enum TickErrorPolicy {
		/**
		 * When a tick error occurs, ignore the error.
		 */
		IGNORE,

		/**
		 * When a tick error occurs, log the error.
		 */
		LOG,

		/**
		 * When a tick error occurs, raise an IllegalStateException.
		 */
		STRICT
	}
}
