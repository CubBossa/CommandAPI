package dev.jorel.commandapi.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jorel.commandapi.CommandAPIHandler;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.nms.NMS;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class CommandArgument extends Argument<CommandResult> implements IGreedyArgument {
	public CommandArgument(String nodeName) {
		super(nodeName, StringArgumentType.greedyString());

		applySuggestions();
	}

	private void applySuggestions() {
		super.replaceSuggestions((info, builder) -> {
			// Extract information
			CommandSender sender = info.sender();
			CommandMap commandMap = CommandAPIHandler.getInstance().getNMS().getSimpleCommandMap();
			String command = info.currentArg();

			// Setup context for errors
			StringReader context = new StringReader(command);

			if (!command.contains(" ")) {
				// Suggesting command name
				ArgumentSuggestions replacement = replacements.getNextSuggestion(sender, context);
				if (replacement != null)
					return replacement.suggest(new SuggestionInfo(sender, new Object[0], command, command), builder);

				List<String> results = commandMap.tabComplete(sender, command);
				// No applicable commands
				if (results == null)
					throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(context);

				// Remove / that gets prefixed to command name if the sender is a player
				if (sender instanceof Player)
					results.stream().map(s -> s.substring(1)).forEach(builder::suggest);
				else
					results.forEach(builder::suggest);

				return builder.buildFuture();
			}


			// Verify commandLabel
			String commandLabel = command.substring(0, command.indexOf(" "));
			Command target = commandMap.getCommand(commandLabel);
			if (target == null)
				throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(context);

			// Get arguments
			String[] arguments = command.split(" ");
			if (!arguments[0].isEmpty() && command.endsWith(" ")) {
				// If command ends with space add an empty argument
				arguments = Arrays.copyOf(arguments, arguments.length + 1);
				arguments[arguments.length - 1] = "";
			}

			// Build suggestion
			builder = builder.createOffset(builder.getStart() + command.lastIndexOf(" ") + 1);

			int lastIndex = arguments.length - 1;
			String[] previousArguments = Arrays.copyOf(arguments, lastIndex);
			ArgumentSuggestions replacement = replacements.getNextSuggestion(sender, context, previousArguments);
			if (replacement != null)
				return replacement.suggest(new SuggestionInfo(sender, previousArguments, command, arguments[lastIndex]), builder);

			// Get location sender is looking at if they are a Player, matching vanilla behavior
			// No builtin Commands use the location parameter, but they could
			Location location = null;
			if (sender instanceof Player player) {
				Block block = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
				if (block != null) location = block.getLocation();
			}

			// Build suggestions for new argument
			target.tabComplete(sender, commandLabel, arguments, location).forEach(builder::suggest);
			return builder.buildFuture();
		});
	}

	SuggestionsBranch replacements = SuggestionsBranch.suggest();

	public CommandArgument replaceSuggestions(ArgumentSuggestions... suggestions) {
		replacements = SuggestionsBranch.suggest(suggestions);
		return this;
	}

	@Override
	public CommandArgument replaceSuggestions(ArgumentSuggestions suggestions) {
		return replaceSuggestions(new ArgumentSuggestions[]{suggestions});
	}

	public Argument<CommandResult> branchSuggestions(SuggestionsBranch... branches) {
		replacements.branch(branches);
		return this;
	}

	@Override
	public Class<CommandResult> getPrimitiveType() {
		return CommandResult.class;
	}

	@Override
	public CommandAPIArgumentType getArgumentType() {
		return CommandAPIArgumentType.COMMAND;
	}

	@Override
	public <CommandSourceStack> CommandResult parseArgument(NMS<CommandSourceStack> nms, CommandContext<CommandSourceStack> cmdCtx, String key, Object[] previousArgs) throws CommandSyntaxException {
		// Extract information
		String command = cmdCtx.getArgument(key, String.class);
		CommandMap commandMap = nms.getSimpleCommandMap();
		CommandSender sender = nms.getSenderForCommand(cmdCtx, false);

		StringReader context = new StringReader(command);

		// Verify command
		String[] arguments = command.split(" ");
		String commandLabel = arguments[0];
		Command target = commandMap.getCommand(commandLabel);
		if (target == null)
			throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(context);

		// check all replacements
		replacements.enforceReplacements(sender, context, arguments);

		return new CommandResult(target, Arrays.copyOfRange(arguments, 1, arguments.length));
	}
}
