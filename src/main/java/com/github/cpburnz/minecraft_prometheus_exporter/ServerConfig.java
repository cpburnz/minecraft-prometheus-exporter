package com.github.cpburnz.minecraft_prometheus_exporter;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

/**
 * The ServerConfig class defines the server-side mod config. This is used to
 * load and generate the "prometheus_exporter-server.toml" config file.
 */
public class ServerConfig {

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogUtils.getLogger();

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
	 * The NeoForge config specification.
	 */
	private final ModConfigSpec spec;

	/**
	 * The individual config value handles.
	 */
	private final ModConfigSpec.BooleanValue collector_jvm_value;
	private final ModConfigSpec.BooleanValue collector_mc_value;
	private final ModConfigSpec.EnumValue<TickErrorPolicy> collector_mc_dimension_tick_errors_value;
	private final ModConfigSpec.BooleanValue collector_mc_entities_value;
	private final ModConfigSpec.ConfigValue<String> web_listen_address_value;
	private final ModConfigSpec.IntValue web_listen_port_value;

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
		// The default address to listen on. This defaults to listening everywhere
		// because it is the most useful default.
		String defaultAddress = "0.0.0.0";

		// The default TCP port ot use. This is completely arbitrary. It was derived
		// from the Minecraft port (25565) and the Prometheus exporter ports
		// (9100+).
		int defaultPort = 19565;

		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

		builder
			.comment("Collector settings.")
			.push("collector");

		this.collector_jvm_value = builder
			.comment("Enable collecting metrics about the JVM process.")
			.define("jvm", true);

		this.collector_mc_value = builder
			.comment("Enable collecting metrics about the Minecraft server.")
			.define("mc", true);

		this.collector_mc_dimension_tick_errors_value = builder
			.comment(
				(
					"Configure how to handle dimension (world) tick errors. Some mods "
					+ "handle the tick events for their custom dimensions, and may not "
					+ "reliably start and stop ticks as expected."
				),
				(
					"  IGNORE: Ignore tick errors. If a mod really botches tick "
					+ "events, it could emit up to 20 log statements per second for "
					+ "each dimension. This would cause large ballooning of the "
					+ "\"logs/debug.txt\" file. Use this setting, or figure out how to "
					+ "filter out DEBUG messages for "
					+ "\"com.github.cpburnz.minecraft_prometheus_exporter.MinecraftCollector/\" "
					+ "in \"log4j2.xml\"."
				),
				"  LOG: Log tick errors. This is the new default.",
				(
					"  STRICT: Raise an exception on tick error. This will crash the "
					+ "server if an error occurs."
				)
			)
			.defineEnum("mc_dimension_tick_errors", TickErrorPolicy.LOG);

		this.collector_mc_entities_value = builder
			.comment(
				"Enable collecting metrics about the entities in each dimension "
				+ "(world)."
			)
			.define("mc_entities", true);

		builder.pop();
		builder
			.comment("Web server settings.")
			.push("web");

		this.web_listen_address_value = builder
			.comment(
				"The IP address to listen on. To only allow connections from the "
				+ "local machine, use \"127.0.0.1\". To allow connections from "
				+ "remote machines, use \"0.0.0.0\"."
			)
			.define("listen_address", defaultAddress);

		this.web_listen_port_value = builder
			.comment(
				"The TCP port to listen on. Ports 1-1023 will not work unless "
				+ "Minecraft is run as root which is not recommended."
			)
			.defineInRange("listen_port", defaultPort, 0, 65535);

		builder.pop();

		this.spec = builder.build();
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
		this.collector_jvm = this.collector_jvm_value.get();
		this.collector_mc = this.collector_mc_value.get();
		this.collector_mc_dimension_tick_errors = this.collector_mc_dimension_tick_errors_value.get();
		this.collector_mc_entities = this.collector_mc_entities_value.get();
		this.web_listen_address = this.web_listen_address_value.get();
		this.web_listen_port = this.web_listen_port_value.get();

		// Record that the config is loaded.
		this.is_loaded = true;

		LOG.debug("collector.jvm: {}", this.collector_jvm);
		LOG.debug("collector.mc: {}", this.collector_mc);
		LOG.debug(
			"collector.mc_dimension_tick_errors: {}",
			this.collector_mc_dimension_tick_errors
		);
		LOG.debug("collector.mc_entities: {}", this.collector_mc_entities);
		LOG.debug("web.listen_address: {}", this.web_listen_address);
		LOG.debug("web.listen_port: {}", this.web_listen_port);
	}

	/**
	 * Register the server-side config with NeoForge.
	 *
	 * @param modContainer The mod container to register the config against.
	 */
	public void register(ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.SERVER, this.spec);
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
