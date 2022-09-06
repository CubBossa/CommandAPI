/*******************************************************************************
 * Copyright 2018, 2021 Jorel Ali (Skepter) - MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package dev.jorel.commandapi;

import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CommandArgument;
import dev.jorel.commandapi.arguments.CommandResult;
import dev.jorel.commandapi.arguments.SuggestionsBranch;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map.Entry;

public class CommandAPIMain extends JavaPlugin {

	@Override
	public void onLoad() {
		// Configure the NBT API - we're not allowing tracking at all, according
		// to the CommandAPI's design principles. The CommandAPI isn't used very
		// much, so this tiny proportion of servers makes very little impact to
		// the NBT API's stats.
		MinecraftVersion.disableBStats();
		MinecraftVersion.disableUpdateCheck();

		// Config loading
		CommandAPI.logger = getLogger();
		saveDefaultConfig();
		CommandAPI.config = new InternalConfig(getConfig(), NBTContainer.class, NBTContainer::new, new File(getDataFolder(), "command_registration.json"));

		// Check dependencies for CommandAPI
		CommandAPIHandler.getInstance().checkDependencies();

		// Convert all plugins to be converted
		for (Entry<JavaPlugin, String[]> pluginToConvert : CommandAPI.config.getPluginsToConvert()) {
			if (pluginToConvert.getValue().length == 0) {
				Converter.convert(pluginToConvert.getKey());
			} else {
				for (String command : pluginToConvert.getValue()) {
					new AdvancedConverter(pluginToConvert.getKey(), command).convert();
				}
			}
		}

		// Convert all arbitrary commands
		for (String commandName : CommandAPI.config.getCommandsToConvert()) {
			new AdvancedConverter(commandName).convertCommand();
		}
	}

	@Override
	public void onEnable() {
		CommandAPI.onEnable(this);

		// TODO: Move these test commands to ArgumentTests
		new CommandAPICommand("commandargument")
			.withArguments(new CommandArgument("command"))
			.executes((sender, args) -> {
				CommandAPI.logInfo("Argument parsed successfully");
				((CommandResult) args[0]).execute(sender);
			}).register();

		new CommandAPICommand("multipleCommands")
			.withArguments(
				new CommandArgument("command")
					.branchSuggestions(
						SuggestionsBranch.suggest(
								ArgumentSuggestions.strings("give"),
								suggestOnlinePlayers()
							).branch(
								SuggestionsBranch.suggest(
									ArgumentSuggestions.strings("diamond", "minecraft:diamond"),
									ArgumentSuggestions.empty()
								),
								SuggestionsBranch.suggest(
									ArgumentSuggestions.strings("dirt", "minecraft:dirt"),
									null,
									ArgumentSuggestions.empty()
								)
							),
						SuggestionsBranch.suggest(
							ArgumentSuggestions.strings("tp"),
							suggestOnlinePlayers(),
							suggestOnlinePlayers()
						)
					)
			).executes((sender, args) -> {
				CommandAPI.logInfo("Argument parsed successfully");
				((CommandResult) args[0]).execute(sender);
			}).register();

		new CommandAPICommand("restrictedcommand")
			.withArguments(new CommandArgument("command")
				.replaceSuggestions(
					ArgumentSuggestions.strings("give"),
					ArgumentSuggestions.strings(info -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toArray(String[]::new)),
					ArgumentSuggestions.strings("diamond", "minecraft:diamond"),
					ArgumentSuggestions.empty()
				)
			).executesPlayer((sender, args) -> {
				CommandAPI.logInfo("Argument parsed successfully");
				((CommandResult) args[0]).execute(sender);
			}).register();
	}

	private ArgumentSuggestions suggestOnlinePlayers() {
		return ArgumentSuggestions.strings(info -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toArray(String[]::new));
	}
}
