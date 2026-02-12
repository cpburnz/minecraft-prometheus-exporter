package com.github.cpburnz.minecraft_prometheus_exporter.forge;

import java.util.function.Predicate;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSetSupplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.cpburnz.minecraft_prometheus_exporter.PrometheusExporterMod;
import com.github.cpburnz.minecraft_prometheus_exporter.base.PrometheusCommand;
import com.github.cpburnz.minecraft_prometheus_exporter.base.ServerConfig;

/**
 * The ForgePrometheusCommand class defines the "prometheus" command for Forge.
 */
public class ForgePrometheusCommand implements PrometheusCommand {

	/**
	 * The value indicating the command failed.
	 */
	private static final int COMMAND_FAILURE = 0;

	/**
	 * The logger to use.
	 */
	private static final Logger LOG = LogManager.getLogger();

	/**
	 * The server configuration.
	 */
	private final ServerConfig config;

	/**
	 * Constructs the instance.
	 *
	 * @param config The server configuration.
	 */
	public ForgePrometheusCommand(ServerConfig config) {
		this.config = config;
	}

	/**
	 * Restart the prometheus exporter.
	 *
	 * @param context The command context.
	 *
	 * @return Success (1) or failure (0).
	 */
	private int execRestart(CommandContext<CommandSourceStack> context) {
		this.execStop(context);
		return this.execStart(context);
	}

	/**
	 * Start the prometheus exporter.
	 *
	 * @param context The command context.
	 *
	 * @return Success (1) or failure (0).
	 */
	private int execStart(CommandContext<CommandSourceStack> context) {
		PrometheusExporterMod mod = PrometheusExporterMod.instance();
		if (!mod.isExporterRunning()) {
			try {
				mod.startExporter();
			} catch (Exception e) {
				LOG.error("Failed to start exporter.", e);
				return COMMAND_FAILURE;
			}
			this.sendAdminMessage(context, MSG_START_SUCCESS);
		} else {
			this.sendChatMessage(context, MSG_START_INVALID);
		}
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * Stop the prometheus exporter.
	 *
	 * @param context The command context.
	 *
	 * @return Success (1) or failure (0).
	 */
	private int execStop(CommandContext<CommandSourceStack> context) {
		PrometheusExporterMod mod = PrometheusExporterMod.instance();
		if (mod.isExporterRunning()) {
			try {
				mod.stopExporter();
			} catch (Exception e) {
				LOG.error("Failed to stop exporter.", e);
				return COMMAND_FAILURE;
			}
			this.sendAdminMessage(context, MSG_STOP_SUCCESS);
		} else {
			this.sendChatMessage(context, MSG_STOP_INVALID);
		}
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * Get the literal for the command arg.
	 *
	 * @return The literal.
	 */
	private LiteralArgumentBuilder<CommandSourceStack> literal(CommandArg val) {
		return Commands.literal(val.getValue());
	}

	/**
	 * Get the requirement for the configured permission level.
	 *
	 * @return The requirement.
	 */
	private <T extends PermissionSetSupplier> Predicate<T> permissionLevel() {
		return Commands.hasPermission(
			new PermissionCheck.Require(
				new Permission.HasCommandLevel(
					PermissionLevel.byId(config.command_permission_level)
				)
			)
		);
	}

	/**
	 * Register the "prometheus" command.
	 *
	 * @param dispatcher The command dispatcher.
	 * @param config The server configuration.
	 */
	public static void register(
		CommandDispatcher<CommandSourceStack> dispatcher,
		ServerConfig config
	) {
		new ForgePrometheusCommand(config).register(dispatcher);
	}

	/**
	 * Register the "prometheus" command.
	 *
	 * @param dispatcher The command dispatcher.
	 */
	private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> builder = Commands
			.literal(NAME)
			.requires(permissionLevel());

		builder.then(literal(CommandArg.RESTART).executes(this::execRestart));
		builder.then(literal(CommandArg.START).executes(this::execStart));
		builder.then(literal(CommandArg.STOP).executes(this::execStop));

		dispatcher.register(builder);
	}

	/**
	 * Send the message to admins.
	 *
	 * @param context The command context.
	 * @param msgFormat The message format.
	 * @param msgParams The message parameters.
	 */
	private void sendAdminMessage(
		CommandContext<CommandSourceStack> context,
		String msgFormat,
		Object... msgParams
	) {
		context.getSource().sendSuccess(
			() -> Component.translatable(msgFormat, msgParams), true
		);
	}

	/**
	 * Send the message to the user.
	 *
	 * @param context The command context.
	 * @param msgFormat The message format.
	 * @param msgParams The message parameters.
	 */
	private void sendChatMessage(
		CommandContext<CommandSourceStack> context,
		String msgFormat,
		Object... msgParams
	) {
		context.getSource().sendSuccess(
			() -> Component.translatable(msgFormat, msgParams), false
		);
	}
}
