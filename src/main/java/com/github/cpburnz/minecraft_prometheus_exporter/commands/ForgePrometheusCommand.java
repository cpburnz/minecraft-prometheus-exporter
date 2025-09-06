package com.github.cpburnz.minecraft_prometheus_exporter.commands;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import com.github.cpburnz.minecraft_prometheus_exporter.PrometheusExporterMod;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;

/**
 * The ForgePrometheusCommand class defines the "prometheus" command for Forge.
 */
public class ForgePrometheusCommand extends CommandBase implements PrometheusCommand {

	/**
	 * Get the available options for tab completion given the arguments.
	 *
	 * @param sender The sender.
	 * @param args The arguments.
	 * @return The completion options.
	 */
	@Override
	public @Nullable List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length != 1) {
			return null;
		}

		// Check subcommands matching arg.
		return getListOfStringsMatchingLastWord(args, CommandArg.ARG_VALUES);
	}

	/**
	 * Restart the prometheus exporter.
	 *
	 * @param sender The sender.
	 */
	private void execRestart(ICommandSender sender) {
		this.execStop(sender);
		this.execStart(sender);
	}

	/**
	 * Start the prometheus exporter.
	 *
	 * @param sender The sender.
	 */
	private void execStart(ICommandSender sender) {
		PrometheusExporterMod mod = PrometheusExporterMod.INSTANCE;
		if (!mod.isExporterRunning()) {
			try {
				mod.startExporter();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			this.sendAdminMessage(sender, "commands.prometheus.start.success");
		} else {
			this.sendChatMessage(sender, "commands.prometheus.start.invalid");
		}
	}

	/**
	 * Stop the prometheus exporter.
	 *
	 * @param sender The sender.
	 */
	private void execStop(ICommandSender sender) {
		PrometheusExporterMod mod = PrometheusExporterMod.INSTANCE;
		if (mod.isExporterRunning()) {
			mod.stopExporter();
			this.sendAdminMessage(sender, "commands.prometheus.stop.success");
		} else {
			this.sendChatMessage(sender, "commands.prometheus.stop.invalid");
		}
	}

	/**
	 * Get the command aliases.
	 *
	 * @return The aliases.
	 */
	@Override
	public @Nullable List<String> getCommandAliases() {
		return ALIASES;
	}

	/**
	 * Get the command name.
	 *
	 * @return The command name.
	 */
	@Override
	public String getCommandName() {
		return NAME;
	}

	/**
	 * Get the command usage.
	 *
	 * @param sender The sender.
	 * @return The usage.
	 */
	@Override
	public String getCommandUsage(ICommandSender sender) {
		return USAGE;
	}

	/**
	 * Get the required permission level for this command.
	 *
	 * @return The required permission level.
	 */
	@Override
	public int getRequiredPermissionLevel() {
		return PERMISSION_LEVEL;
	}

	/**
	 * Process the command.
	 *
	 * @param sender The sender.
	 * @param args The arguments.
	 */
	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length != 1) {
			throw new WrongUsageException("commands.prometheus.usage");
		}

		// Parse subcommand.
		CommandArg cmd;
		try {
			cmd = CommandArg.from(args[0]);
		} catch (IllegalArgumentException e) {
			throw new WrongUsageException("commands.prometheus.usage");
		}

		switch (cmd) {
			case RESTART -> execRestart(sender);
			case START -> execStart(sender);
			case STOP -> execStop(sender);
		}
	}

	/**
	 * Send the message to admins.
	 *
	 * @param sender The sender.
	 * @param msgKey The message translation key.
	 * @param msgParams The message parameters.
	 */
	private void sendAdminMessage(ICommandSender sender, String msgKey, Object... msgParams) {
		func_152373_a(sender, this, msgKey, msgParams);
	}

	/**
	 * Send the message to the user.
	 *
	 * @param sender The sender.
	 * @param msgKey The message translation key.
	 * @param msgParams The message parameters.
	 */
	private void sendChatMessage(ICommandSender sender, String msgKey, Object... msgParams) {
		sender.addChatMessage(new ChatComponentTranslation(msgKey, msgParams));
	}
}
