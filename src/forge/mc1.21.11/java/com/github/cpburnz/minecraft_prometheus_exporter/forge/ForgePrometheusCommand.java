package com.github.cpburnz.minecraft_prometheus_exporter.forge;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

import com.github.cpburnz.minecraft_prometheus_exporter.PrometheusExporterMod;
import com.github.cpburnz.minecraft_prometheus_exporter.base.PrometheusCommand;
import com.github.cpburnz.minecraft_prometheus_exporter.base.ServerConfig;
/**
 * The ForgePrometheusCommand class defines the "prometheus" command for Forge.
 */
public class ForgePrometheusCommand implements PrometheusCommand {

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
	 * @return Success.
	 */
	private int execRestart(CommandContext<CommandSourceStack> context) {
		this.execStop(context);
		this.execStart(context);
		return Command.SINGLE_SUCCESS;
	}

	/**
	 * Start the prometheus exporter.
	 *
	 * @param context The command context.
	 *
	 * @return Success.
	 */
	private int execStart(CommandContext<CommandSourceStack> context) {
		PrometheusExporterMod mod = PrometheusExporterMod.INSTANCE;
		if (!mod.isExporterRunning()) {
			try {
				mod.startExporter();
			} catch (IOException e) {
				throw new RuntimeException(e);
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
	 * @return Success.
	 */
	private int execStop(CommandContext<CommandSourceStack> context) {
		PrometheusExporterMod mod = PrometheusExporterMod.INSTANCE;
		if (mod.isExporterRunning()) {
			mod.stopExporter();
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
		Stream.of(List.of(NAME), ALIASES).flatMap(List::stream).forEach((name) -> {
			LiteralArgumentBuilder<CommandSourceStack> builder = Commands
				.literal(name)
				.requires(permissionLevel());

			builder.then(literal(CommandArg.RESTART).executes(this::execRestart));
			builder.then(literal(CommandArg.START).executes(this::execStart));
			builder.then(literal(CommandArg.STOP).executes(this::execStop));

			dispatcher.register(builder);
		});
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
