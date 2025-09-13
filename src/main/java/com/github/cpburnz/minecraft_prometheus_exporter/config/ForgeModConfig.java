package com.github.cpburnz.minecraft_prometheus_exporter.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The ForgeModConfig class defines the mod config. This is used to load and
 * generate the "prometheus_exporter.cfg" config file.
 */
public class ForgeModConfig extends ModConfig {

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

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
		this.collector_mc_entities = (
			this.internal_spec.collector_mc_entities.getBoolean()
		);
		this.collector_mc_player_stats = (
			this.internal_spec.collector_mc_player_stats.getBoolean()
		);
		this.command_permission_level = (
			this.internal_spec.command_permission_level.getInt()
		);
		this.web_listen_address = this.internal_spec.web_listen_address.getString();
		this.web_listen_port = this.internal_spec.web_listen_port.getInt();

		// Parse tick errors value.
		String raw_tick_errors = (
			this.internal_spec.collector_mc_dimension_tick_errors.getString()
		);
		try {
			this.collector_mc_dimension_tick_errors = (
				TickErrorPolicy.valueOf(raw_tick_errors)
			);
		} catch (IllegalArgumentException e) {
			this.collector_mc_dimension_tick_errors = TickErrorPolicy.LOG;
			LOG.debug(
				"Failed to parse {} value {}, default {}.",
				"collector.mc_dimension_tick_errors",
				raw_tick_errors,
				TickErrorPolicy.LOG
			);
		}

		// Record that the config is loaded.
		this.setIsLoaded(true);

		LOG.debug("collector.jvm: {}", this.collector_jvm);
		LOG.debug("collector.mc: {}", this.collector_mc);
		LOG.debug(
			"collector.mc_dimension_tick_errors: {}",
			this.collector_mc_dimension_tick_errors
		);
		LOG.debug("collector.mc_entities: {}", this.collector_mc_entities);
		LOG.debug("collector.mc_player_stats: {}", this.collector_mc_player_stats);
		LOG.debug("command.permission_level: {}", this.command_permission_level);
		LOG.debug("web.listen_address: {}", this.web_listen_address);
		LOG.debug("web.listen_port: {}", this.web_listen_port);
	}

	/**
	 * This class is used to define the Forge configuration specifications.
	 */
	private static class InternalSpec {

		/**
		 * The default address to listen on. This defaults to listening everywhere
		 * because it is the most useful default.
		 */
		private static final String DEFAULT_ADDRESS = "0.0.0.0";

		/**
		 * The default required permission level to execute commands.
		 */
		private static final int DEFAULT_PERMISSION_LEVEL = 4;

		/**
		 * The default TCP port ot use. This is completely arbitrary. It was derived
		 * from the Minecraft port (25565) and the Prometheus exporter ports
		 * (9100+).
		 */
		private static final int DEFAULT_PORT = 19565;

		/**
		 * The minimum permission level.
		 */
		private static final int PERMISSION_LEVEL_MIN = 0;

		/**
		 * The maximum permission level.
		 */
		private static final int PERMISSION_LEVEL_MAX = 4;

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
		public final Property collector_mc_dimension_tick_errors;
		public final Property collector_mc_entities;
		public final Property collector_mc_player_stats;
		public final Property command_permission_level;
		public final Property web_listen_address;
		public final Property web_listen_port;

		/**
		 * Construct the instance.
		 *
		 * @param config The Forge config file specification.
		 */
		public InternalSpec(Configuration config) {
			config.getCategory("collector")
				.setComment("Collector settings.");

			this.collector_jvm = config.get("collector", "jvm", true);
			this.collector_jvm.comment = (
				"Enable collecting metrics about the JVM process."
			);

			this.collector_mc = config.get("collector", "mc", true);
			this.collector_mc.comment = (
				"Enable collecting metrics about the Minecraft server."
			);

			this.collector_mc_dimension_tick_errors = config
				.get("collector", "mc_dimension_tick_errors", TickErrorPolicy.LOG.name());
			this.collector_mc_dimension_tick_errors.comment = (
				"Configure how to handle dimension (world) tick errors. Some mods "
				+ "handle the tick events for their custom dimensions, and may not "
				+ "reliably start and stop ticks as expected."
				+ "\n"
				+ "  IGNORE: Ignore tick errors. If a mod really botches tick events, "
				+ "it could emit up to 20 log statements per second for each "
				+ "dimension. This would cause large ballooning of the "
				+ "\"logs/debug.txt\" file. Use this setting, or figure out how to "
				+ "filter out DEBUG messages for "
				+ "\"com.github.cpburnz.minecraft_prometheus_exporter.MinecraftCollector/\" "
				+ "in \"log4j2.xml\"."
				+ "\n"
				+ "  LOG: Log tick errors. This is the new default."
				+ "\n"
				+ "  STRICT: Raise an exception on tick error. This will crash the "
				+ "server if an error occurs."
			);

			this.collector_mc_entities = config.get("collector", "mc_entities", true);
			this.collector_mc_entities.comment = (
				"Enable collecting metrics about the entities in each dimension "
				+ "(world)."
			);

			this.collector_mc_player_stats = config
				.get("collector", "mc_player_stats", true);
			this.collector_mc_player_stats.comment = (
				"Enable collecting metrics about general player stats."
			);

			config.getCategory("command")
				.setComment("Command settings.");

			this.command_permission_level = config.get(
				"command", "permission_level", DEFAULT_PERMISSION_LEVEL
			).setMinValue(PERMISSION_LEVEL_MIN)
				.setMaxValue(PERMISSION_LEVEL_MAX);
			this.command_permission_level.comment = (
				"The permission level required to run the \"/prometheus\" command. "
				+ "Range is " + PERMISSION_LEVEL_MIN + "-" + PERMISSION_LEVEL_MAX + "."
				+ "\n"
				+ "  0: Any player."
				+ "\n"
				+ "  1-4: Varying levels of \"op\". The default permission for op is 4."
			);

			config.getCategory("web")
				.setComment("Web server settings.");

			this.web_listen_address = config.get("web", "listen_address", DEFAULT_ADDRESS);
			this.web_listen_address.comment = (
				"The IP address to listen on. To only allow connections from the local "
				+ "machine, use \"127.0.0.1\". To allow connections from remote "
				+ "machines, use \"0.0.0.0\"."
			);

			this.web_listen_port = config.get("web", "listen_port", DEFAULT_PORT)
				.setMinValue(TCP_PORT_MIN)
				.setMaxValue(TCP_PORT_MAX);
			this.web_listen_port.comment = (
				"The TCP port to listen on. Ports 1-1023 will not work unless "
				+ "Minecraft is run as root which is not recommended. Range is "
				+ TCP_PORT_MIN + "-" + TCP_PORT_MAX + "."
			);

			if (config.hasChanged()) {
				config.save();
			}
		}
	}
}
