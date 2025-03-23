package com.github.cpburnz.minecraft_prometheus_exporter.neoforge;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cpburnz.minecraft_prometheus_exporter.base.ServerConfig;

/**
 * The NeoServerConfig class defines the server-side mod config. This is used to
 * load and generate the "prometheus_exporter-server.toml" config file.
 */
public class NeoServerConfig extends ServerConfig {

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

	/**
	 * The server-side config specifications.
	 */
	private final InternalSpec internal_spec;

	/**
	 * The NeoForge config specification.
	 */
	private final ModConfigSpec neo_spec;

	/**
	 * Construct the instance.
	 */
	public NeoServerConfig() {
		// Setup config specs.
		Pair<InternalSpec, ModConfigSpec> result = (
			new ModConfigSpec.Builder().configure(InternalSpec::new)
		);
		this.internal_spec = result.getLeft();
		this.neo_spec = result.getRight();
	}

	/**
	 * Load the values from the server-side specification.
	 */
	public void loadValues() {
		// Get config values.
		this.collector_jvm = this.internal_spec.collector_jvm.get();
		this.collector_mc = this.internal_spec.collector_mc.get();
		this.collector_mc_dimension_tick_errors = (
			this.internal_spec.collector_mc_dimension_tick_errors.get()
		);
		this.collector_mc_entities = this.internal_spec.collector_mc_entities.get();
		this.web_listen_address = this.internal_spec.web_listen_address.get();
		this.web_listen_port = this.internal_spec.web_listen_port.get();

		// Record that the config is loaded.
		this.setIsLoaded(true);

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
	 * @param context The mod loading context.
	 */
	public void register(ModLoadingContext context) {
		context.getActiveContainer().registerConfig(ModConfig.Type.SERVER, this.neo_spec);
	}

	/**
	 * The InternalSpec class is used to define the server-side NeoForge config
	 * specifications.
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

		public final ModConfigSpec.BooleanValue collector_jvm;
		public final ModConfigSpec.BooleanValue collector_mc;
		public final ModConfigSpec.EnumValue<TickErrorPolicy> collector_mc_dimension_tick_errors;
		public final ModConfigSpec.BooleanValue collector_mc_entities;
		public final ModConfigSpec.ConfigValue<String> web_listen_address;
		public final ModConfigSpec.IntValue web_listen_port;

		/**
		 * Construct the instance.
		 *
		 * @param builder The NeoForge config builder.
		 */
		public InternalSpec(ModConfigSpec.Builder builder) {
			builder
				.comment("Collector settings.")
				.push("collector");

			this.collector_jvm = builder
				.comment("Enable collecting metrics about the JVM process.")
				.define("jvm", true);

			this.collector_mc = builder
				.comment("Enable collecting metrics about the Minecraft server.")
				.define("mc", true);

			this.collector_mc_dimension_tick_errors = builder
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

			this.collector_mc_entities = builder
				.comment(
					"Enable collecting metrics about the entities in each dimension "
					+ "(world)."
				)
				.define("mc_entities", true);

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
