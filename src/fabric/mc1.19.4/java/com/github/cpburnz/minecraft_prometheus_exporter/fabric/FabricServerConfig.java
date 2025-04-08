package com.github.cpburnz.minecraft_prometheus_exporter.fabric;

import java.nio.file.Path;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;

import com.github.cpburnz.minecraft_prometheus_exporter.base.ServerConfig;

/**
 * The FabricServerConfig class defines the server-side mod config. This is used
 * to load and generate the "prometheus_exporter-server.toml" config file.
 */
public class FabricServerConfig extends ServerConfig {

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

	/**
	 * The internal server-side config specifications.
	 */
	private final InternalSpec internal_spec;

	/**
	 * Construct the instance.
	 */
	public FabricServerConfig() {
		this.internal_spec = new InternalSpec();
	}

	/**
	 * Load the server-side config.
	 *
	 * @param file The config file path.
	 */
	public void loadFile(Path file) {
		this.internal_spec.loadFile(file);
		this.loadValues();
	}

	/**
	 * Load the values from the server-side config.
	 */
	private void loadValues() {
		// Get loaded config.
		assert this.internal_spec.config != null;
		UnmodifiableConfig config = this.internal_spec.config.unmodifiable();

		// Get config values.
		this.collector_jvm = config.get("collector.jvm");
		this.collector_mc = config.get("collector.mc");
		this.collector_mc_dimension_tick_errors = config.getEnum(
			"collector.mc_dimension_tick_errors",
			TickErrorPolicy.class,
			EnumGetMethod.NAME
		);
		this.collector_mc_entities = config.get("collector.mc_entities");
		this.web_listen_address = config.get("web.listen_address");
		this.web_listen_port = config.get("web.listen_port");

		LOG.debug("collector.jvm: {}", this.collector_jvm);
		LOG.debug("collector.mc: {}", this.collector_mc);
		LOG.debug(
			"collector.mc_dimension_tick_errors: {}",
			this.collector_mc_dimension_tick_errors
		);
		LOG.debug("collector.mc_entities: {}", this.collector_mc_entities);
		LOG.debug("web.listen_address: {}", this.web_listen_address);
		LOG.debug("web.listen_port: {}", this.web_listen_port);

		assert this.collector_mc_dimension_tick_errors != null;
		assert this.web_listen_address != null;

		// Record that the config is loaded.
		this.setIsLoaded(true);
	}

	/**
	 * The InternalSpec class is used to define the server-side config
	 * specifications. Fabric does not yet provide config file loading, so this
	 * class handles it.
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

		/**
		 * The loaded config.
		 */
		@Nullable
		private CommentedConfig config;

		/**
		 * The config specification.
		 */
		private final ConfigSpec spec;

		/**
		 * Construct the instance.
		 */
		public InternalSpec() {
			this.spec = newSpec();
		}

		/**
		 * Load the config.
		 *
		 * @param file The config file path.
		 */
		public void loadFile(Path file) {
			LOG.debug("Load config file {}.", file);
			try (
				CommentedFileConfig config = CommentedFileConfig
					.builder(file, TomlFormat.instance())
					.sync()
					.build()
					.checked()
			) {
				this.config = config;

				// Load config.
				config.load();

				// Correct config from spec.
				int changes = this.spec.correct(config, ((_action, path, bad_value, new_value) -> {
					if (!(new_value instanceof Config)) {
						String name = String.join(".", path);
						LOG.debug("Corrected {} from {} to {}.", name, bad_value, new_value);
					}
				}));

				// BUG: NightConfig is not correcting enums for some strange reason.
				@Nullable TickErrorPolicy tick_errors = config.getEnum(
					"collector.mc_dimension_tick_errors",
					TickErrorPolicy.class,
					EnumGetMethod.NAME
				);
				if (tick_errors == null) {
					tick_errors = TickErrorPolicy.LOG;
					config.set("collector.mc_dimension_tick_errors", tick_errors);
					changes += 1;
					LOG.debug(
						"Corrected {} from {} to {}.",
						"collector.mc_dimension_tick_errors",
						null,
						tick_errors
					);
				}

				// Set comments on config.
				changes += this.setComments();

				// Save config if there are any changes.
				if (changes > 0) {
					LOG.debug("Save config file {}.", file);
					config.save();
				}
			}
		}

		/**
		 * Create the config specification.
		 *
		 * @return The config specification.
		 */
		private static ConfigSpec newSpec() {
			ConfigSpec spec = new ConfigSpec();

			spec.define("collector.jvm", true);
			spec.define("collector.mc", true);
			// BUG: NightConfig is not correcting enums for some strange reason.
			spec.defineEnum(
				"collector.mc_dimension_tick_errors",
				TickErrorPolicy.LOG,
				EnumGetMethod.NAME
			);
			spec.define("collector.mc_entities", true);
			spec.define("web.listen_address", DEFAULT_ADDRESS);
			spec.defineInRange("web.listen_port", DEFAULT_PORT, TCP_PORT_MIN, TCP_PORT_MAX);

			return spec;
		}

		/**
		 * Set the comment on a config field.
		 *
		 * @param path The field path.
		 * @param comment The comment.
		 *
		 * @return Whether the comment was changed.
		 */
		private int setComment(String path, String comment) {
			assert this.config != null;
			@Nullable String old = this.config.setComment(path, comment);
			return comment.equals(old) ? 0 : 1;
		}

		/**
		 * Set the comments on the config.
		 *
		 * @return The number of changed comments.
		 */
		private int setComments() {
			int changes = 0;

			changes += this.setComment("collector", "Collector settings.");

			changes += this.setComment(
				"collector.jvm",
				"Enable collecting metrics about the JVM process."
			);

			changes += this.setComment(
				"collector.mc",
				"Enable collecting metrics about the Minecraft server."
			);

			changes += this.setComment(
				"collector.mc_dimension_tick_errors",
				(
					"Configure how to handle dimension (world) tick errors. Some mods "
					+ "handle the tick events for their custom dimensions, and may not "
					+ "reliably start and stop ticks as expected.\n"
					+ "  IGNORE: Ignore tick errors. If a mod really botches tick events, "
					+ "it could emit up to 20 log statements per second for each "
					+ "dimension. This would cause large ballooning of the \"logs/debug.txt\" "
					+ "file. Use this setting, or figure out how to filter out DEBUG "
					+ "messages for \"com.github.cpburnz.minecraft_prometheus_exporter.MinecraftCollector/\" "
					+ "in \"log4j2.xml\".\n"
					+ "  LOG: Log tick errors. This is the new default.\n"
					+ "  STRICT: Raise an exception on tick error. This will crash the "
					+ "server if an error occurs."
				)
			);

			changes += this.setComment(
				"collector.mc_entities",
				"Enable collecting metrics about the entities in each dimension (world)."
			);

			changes += this.setComment("web", "Web server settings.");

			changes += this.setComment(
				"web.listen_address",
				(
					"The IP address to listen on. To only allow connections from the local "
					+ "machine, use \"127.0.0.1\". To allow connections from remote "
					+ "machines, use \"0.0.0.0\"."
				)
			);

			changes += this.setComment(
				"web.listen_port",
				(
					"The TCP port to listen on. Ports 1-1023 will not work unless "
					+ "Minecraft is run as root which is not recommended."
				)
			);

			return changes;
		}
	}
}
