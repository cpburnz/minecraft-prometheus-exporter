package com.github.cpburnz.minecraft_prometheus_exporter.base;

/**
 * The PrometheusCommand class defines the "prometheus" command.
 */
public interface PrometheusCommand {

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

		/**
		 * @return The argument value.
		 */
		public String getValue() {
			return this.val;
		}
	}
}
