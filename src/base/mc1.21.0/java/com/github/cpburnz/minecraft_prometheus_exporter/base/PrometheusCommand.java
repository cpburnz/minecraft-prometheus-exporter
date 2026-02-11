package com.github.cpburnz.minecraft_prometheus_exporter.base;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The PrometheusCommand class defines the "prometheus" command.
 */
public interface PrometheusCommand {

	/**
	 * The command aliases.
	 */
	List<String> ALIASES = List.of("prom");

	// TODO: I can't get ChatComponentTranslation to work with strings defined in
	// "assets/prometheus_exporter/lang/en_us.json".

//	/**
//	 * The message for command usage.
//	 */
//	String MSG_USAGE = "/prometheus <start|stop|restart>";

	/**
	 * The message for when starting the exporter is invalid.
	 */
	String MSG_START_INVALID = "Prometheus exporter is already running.";

	/**
	 * The message for when starting the exporter succeeded.
	 */
	String MSG_START_SUCCESS = "Prometheus exporter started.";

	/**
	 * The message for when stopping the exporter is invalid.
	 */
	String MSG_STOP_INVALID = "Prometheus exporter is already stopped.";

	/**
	 * The message for when stopping the exporter succeeded.
	 */
	String MSG_STOP_SUCCESS = "Prometheus exporter stopped.";

	/**
	 * The command name.
	 */
	String NAME = "prometheus";

	/**
	 * The CommandArg enum defines the subcommands.
	 */
	enum CommandArg {
		RESTART("restart"),
		START("start"),
		STOP("stop");

//		/**
//		 * Contains each argument value.
//		 */
//		public static final String[] ARG_VALUES = Arrays.stream(CommandArg.values())
//			.map(CommandArg::getValue)
//			.toArray(String[]::new);

//		/**
//		 * Maps argument value to command arg.
//		 */
//		private static final Map<String, CommandArg> FROM = Arrays.stream(CommandArg.values())
//			.collect(Collectors.toMap(CommandArg::getValue, v -> v));

		/**
		 * The argument value.
		 */
		private final String val;

		/**
		 * Construct the CommandArg enum.
		 *
		 * @param val The command arg.
		 */
		CommandArg(String val) {
			this.val = val;
		}

//		/**
//		 * Get the command arg by value.
//		 *
//		 * @param val The argument value.
//		 *
//		 * @return The command arg.
//		 *
//		 * @throws IllegalArgumentException When the command arg does not exist.
//		 */
//		public static CommandArg from(String val) {
//			CommandArg arg = FROM.get(val);
//			if (arg != null) {
//				return arg;
//			} else {
//				throw new IllegalArgumentException(val);
//			}
//		}

		/**
		 * @return The argument value.
		 */
		public String getValue() {
			return this.val;
		}
	}
}
