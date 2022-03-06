package com.github.cpburnz.minecraft_prometheus_exporter;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * This class defines the mod config. This is used to load and generate the
 * "prometheus_exporter.cfg" config file.
 */
public class Config {

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

	/**
	 * Whether collecting metrics about the JVM process is enabled.
	 */
	public boolean collector_jvm;

	/**
	 * Whether collecting metrics about the Minecraft server is enabled.
	 */
	public boolean collector_mc;

	/**
	 * The Forge config file specification.
	 */
	@SuppressWarnings("FieldCanBeLocal")
	private Configuration forge_spec;

	/**
	 * The mod configuration specification.
	 */
	@SuppressWarnings("FieldCanBeLocal")
	private InternalSpec internal_spec;

	/**
	 * Whether the configuration has been loaded.
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
	 * Construct the instance.
	 */
	public Config() {
		// Nothing to do.
	}

	/**
	 * @return Whether the configuration is loaded.
	 */
	@SuppressWarnings("unused")
	public boolean isLoaded() {
		return this.is_loaded;
	}

	/**
	 * Load the values from the config file.
	 *
	 * @param file The config file to load.
	 */
	public void loadValues(File file) {
		// Setup config specs.
		this.forge_spec = new Configuration(file, true);
		this.internal_spec = new InternalSpec(this.forge_spec);

		// Get config values.
		this.collector_jvm = this.internal_spec.collector_jvm.getBoolean();
		this.collector_mc = this.internal_spec.collector_mc.getBoolean();
		this.web_listen_address = this.internal_spec.web_listen_address.getString();
		this.web_listen_port = this.internal_spec.web_listen_port.getInt();

		// Record that the config is loaded.
		this.is_loaded = true;

		LOG.debug("collector.jvm: {}", this.collector_jvm);
		LOG.debug("collector.mc: {}", this.collector_mc);
		LOG.debug("web.listen_address: {}", this.web_listen_address);
		LOG.debug("web.listen_port: {}", this.web_listen_port);
	}

	/**
	 * This class is used to define the mod config file properties.
	 */
	private static class InternalSpec {

		/**
		 * The default address to listen on. This defaults to listening everywhere
		 * because it is the most useful default.
		 */
		private static final String DEFAULT_ADDRESS = "0.0.0.0";

		/**
		 * The default TCP port ot use. This is completely arbitrary. It was derived
		 * from the Minecraft port (25565) and the Prometheus exporter ports
		 * (9100+).
		 */
		private static final int DEFAULT_PORT = 19565;

		/**
		 * The maximum TCP port.
		 */
		private static final int TCP_PORT_MAX = 65535;

		/**
		 * The minimum TCP port.
		 */
		private static final int TCP_PORT_MIN = 0;

		public final Property collector_jvm;
		public final Property collector_mc;
		public final Property web_listen_address;
		public final Property web_listen_port;

		/**
		 * Construct the instance.
		 *
		 * @param config The Forge config file specification.
		 */
		public InternalSpec(Configuration config) {
			config.getCategory("collector").setComment("Collector settings.");

			this.collector_jvm = config.get("collector", "jvm", true);
			this.collector_jvm.setComment(
				"Enable collecting metrics about the JVM process."
			);

			this.collector_mc = config.get("collector", "mc", true);
			this.collector_mc.setComment(
				"Enable collecting metrics about the Minecraft server."
			);

			config.getCategory("web").setComment("Web server settings.");

			this.web_listen_address = config.get("web", "listen_address", DEFAULT_ADDRESS);
			this.web_listen_address.setComment(
				"The IP address to listen on. To only allow connections from the local "
				+ "machine, use \"127.0.0.1\". To allow connections from remote "
				+ "machines, use \"0.0.0.0\"."
			);

			this.web_listen_port = config.get("web", "listen_port", DEFAULT_PORT)
				.setMinValue(TCP_PORT_MIN)
				.setMaxValue(TCP_PORT_MAX);
			this.web_listen_port.setComment(
				"The TCP port to listen on. Ports 1-1023 will not work unless "
				+ "Minecraft is run as root which is not recommended. Range is "
				+ TCP_PORT_MIN + " - " + TCP_PORT_MAX
			);

			if (config.hasChanged()) {
				config.save();
			}
		}
	}
}
