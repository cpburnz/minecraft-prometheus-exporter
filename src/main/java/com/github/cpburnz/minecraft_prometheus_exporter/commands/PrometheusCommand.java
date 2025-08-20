package com.github.cpburnz.minecraft_prometheus_exporter.commands;

import java.util.Arrays;
import java.util.List;

/**
 * The PrometheusCommand class defines the "prometheus" command.
 */
public interface PrometheusCommand {

	/**
	 * The command aliases.
	 */
	@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
	List<String> ALIASES = Arrays.asList("prom");

	/**
	 * The command name.
	 */
	String NAME = "prometheus";

	/**
	 * The required permission level to use this command. This is restricted to
	 * "op".
	 */
	int PERMISSION_LEVEL = 2;

	/**
	 * The command usage.
	 */
	String USAGE = "commands.prometheus.usage";

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
