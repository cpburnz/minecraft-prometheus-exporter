package com.github.cpburnz.minecraft_prometheus_exporter;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.toml.TomlFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * The ServerConfig class defines the server-side mod config. This is used to
 * load and generate the "prometheus_exporter-server.toml" config file.
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
	 * How to handle dimension (world) tick event errors.
	 */
	public TickErrorPolicy collector_mc_dimension_tick_errors;

	/**
	 * Whether collecting metrics about the entities in each dimension (world) is
	 * enabled.
	 */
	public boolean collector_mc_entities;

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
		this.internal_spec = new InternalSpec();
	}

	/**
	 * @return Whether the config is loaded.
	 */
	@SuppressWarnings("unused")
	public boolean isLoaded() {
		return this.is_loaded;
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
		// Get config values.
		UnmodifiableCommentedConfig config = this.internal_spec.config;
		this.collector_jvm = config.get("collector.jvm");
		this.collector_mc = config.get("collector.mc");
		this.collector_mc_dimension_tick_errors = config.get(
			"collector.mc_dimension_tick_errors"
		);
		this.collector_mc_entities = config.get("collector.mc_entities");
		this.web_listen_address = config.get("web.listen_address");
		this.web_listen_port = config.get("web.listen_port");

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
	 * The InternalSpec class is used to define the server-side Forge config
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

		/**
		 * The loaded config.
		 */
		@Nullable
		private UnmodifiableCommentedConfig config;

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
		 * Add comments to the config.
		 *
		 * @param config The config.
		 */
		private static void addComments(CommentedConfig config) {
			config.setComment("collector", "Collector settings.");

			config.setComment(
				"collector.jvm",
				"Enable collecting metrics about the JVM process."
			);

			config.setComment(
				"collector.mc",
				"Enable collecting metrics about the Minecraft server."
			);

			config.setComment(
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

			config.setComment(
				"collector.mc_entities",
				"Enable collecting metrics about the entities in each dimension (world)."
			);

			config.setComment("web", "Web server settings.");

			config.setComment(
				"web.listen_address",
				(
					"The IP address to listen on. To only allow connections from the local "
					+ "machine, use \"127.0.0.1\". To allow connections from remote "
					+ "machines, use \"0.0.0.0\"."
				)
			);

			config.setComment(
				"web.listen_port",
				(
					"The TCP port to listen on. Ports 1-1023 will not work unless "
					+ "Minecraft is run as root which is not recommended."
				)
			);
		}

		/**
		 * Load the config.
		 *
		 * @param file The config file path.
		 */
		public void loadFile(Path file) {
			// TODO: Load config file.
			// TODO: Config class must support comments.
			try (FileConfig config = FileConfig.of(file, TomlFormat.instance())) {
				this.config = TomlFormat.instance().createParser().parse(file, FileNotFoundAction.READ_NOTHING);

				// Correct config from spec.
				this.spec.correct(config, ((action, path, bad_value, new_value) -> {
					String name = String.join(".", path);
					LOG.debug("Corrected {} from {} to {}.", name, bad_value, new_value);
				}));

				// Add comments to config.
				addComments(config);

				// TODO: Save config if there are changes.


				this.config = config.unmodifiable();
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
