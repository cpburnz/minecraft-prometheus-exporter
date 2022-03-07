package com.github.cpburnz.minecraft_prometheus_exporter;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class defines the server-side mod config.
 */
public class ServerConfig {

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
	 * The Forge config specification.
	 */
	private final ForgeConfigSpec forge_spec;

	/**
	 * The server-side config specifications.
	 */
	private final InternalSpec internal_spec;

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
	 * Construct the instance.
	 */
	public ServerConfig() {
		// Setup config specs.
		Pair<InternalSpec, ForgeConfigSpec> result = new ForgeConfigSpec.Builder().configure(InternalSpec::new);
		this.internal_spec = result.getLeft();
		this.forge_spec = result.getRight();
	}

	/**
	 * @return Whether the config is loaded.
	 */
	@SuppressWarnings("unused")
	public boolean isLoaded() {
		return this.is_loaded;
	}

	/**
	 * Load the values from the server-side specification.
	 */
	public void loadValues() {
		// Get config values.
		this.collector_jvm = this.internal_spec.collector_jvm.get();
		this.collector_mc = this.internal_spec.collector_mc.get();
		this.web_listen_address = this.internal_spec.web_listen_address.get();
		this.web_listen_port = this.internal_spec.web_listen_port.get();

		// Record that the config is loaded.
		this.is_loaded = true;

		LOG.debug("collector.jvm: {}", this.collector_jvm);
		LOG.debug("collector.mc: {}", this.collector_mc);
		LOG.debug("web.listen_address: {}", this.web_listen_address);
		LOG.debug("web.listen_port: {}", this.web_listen_port);
	}

	/**
	 * Register the server-side config with Forge.
	 */
	public void register() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, this.forge_spec);
	}

	/**
	 * This class is used to define the server-side Forge config specifications.
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

		public final ForgeConfigSpec.BooleanValue collector_jvm;
		public final ForgeConfigSpec.BooleanValue collector_mc;
		public final ForgeConfigSpec.ConfigValue<String> web_listen_address;
		public final ForgeConfigSpec.IntValue web_listen_port;

		/**
		 * Construct the instance.
		 *
		 * @param builder The Forge config builder.
		 */
		public InternalSpec(ForgeConfigSpec.Builder builder) {
			builder
				.comment("Collector settings.")
				.push("collector");

			this.collector_jvm = builder
				.comment("Enable collecting metrics about the JVM process.")
				.define("jvm", true);

			this.collector_mc = builder
				.comment("Enable collecting metrics about the Minecraft server.")
				.define("mc", true);

			builder.pop();
			builder
				.comment("Web server settings.")
				.push("web");

			this.web_listen_address = builder
				.comment(
					"The IP address to listen on. To only allow connections from the "
					+ "local machine, use \"127.0.0.1\". To allow connections from "
					+ "remote machines, use \"0.0.0.0\"."
				)
				.define("listen_address", DEFAULT_ADDRESS);

			this.web_listen_port = builder
				.comment(
					"The TCP port to listen on. Ports 1-1023 will not work unless "
					+ "Minecraft is run as root which is not recommended."
				)
				.defineInRange("listen_port", DEFAULT_PORT, TCP_PORT_MIN, TCP_PORT_MAX);

			builder.pop();
		}
	}
}
