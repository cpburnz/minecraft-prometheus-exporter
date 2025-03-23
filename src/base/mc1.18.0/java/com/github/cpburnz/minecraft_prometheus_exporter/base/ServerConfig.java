package com.github.cpburnz.minecraft_prometheus_exporter.base;

/**
 * The ServerConfig class defines the base server-side mod config. This is used
 * to store settings from the "prometheus_exporter-server.toml" config file.
 */
public abstract class ServerConfig {
	/**
	 * Whether collecting metrics about the JVM process is enabled.
	 */
	public boolean collector_jvm;

	/**
	 * Whether collecting metrics about the Minecraft server is enabled.
	 */
	public boolean collector_mc;

	/**
	 * How to handle dimension (world) tick event errors.
	 */
	public TickErrorPolicy collector_mc_dimension_tick_errors;

	/**
	 * Whether collecting metrics about the entities in each dimension (world) is
	 * enabled.
	 */
	public boolean collector_mc_entities;

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
