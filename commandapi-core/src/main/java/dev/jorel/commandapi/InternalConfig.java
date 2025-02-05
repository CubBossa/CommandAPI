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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import dev.jorel.commandapi.nms.NMS;

/**
 * Configuration wrapper class. The config.yml file used by the CommandAPI is
 * only ever read from, nothing is ever written to it. That's why there's only
 * getter methods.
 */
public class InternalConfig {

	// Output registering and unregistering of commands
	private final boolean verboseOutput;

	// Whether we should suppress all logs
	private final boolean silentLogs;

	// Whether we should use the latest NMS version (which may not be compatible)
	private final boolean useLatestNMSVersion;

	// The message to display when an executor implementation is missing
	private final String message_missingExecutorImplementation;

	// Create a command_registration.json file
	private final File dispatcherFile;

	// List of plugins to convert
	private final Map<JavaPlugin, String[]> pluginsToConvert;

	// List of plugins which should ignore proxied senders
	private final List<String> skipSenderProxy;

	// List of arbitrary commands to convert
	private final List<String> commandsToConvert;

	private final Class<?> nbtContainerClass;
	private final Function<Object, ?> nbtContainerConstructor;

	private final NMS<?> customNMS;

	InternalConfig(FileConfiguration fileConfig, Class<?> nbtContainerClass,
		Function<Object, ?> nbtContainerConstructor, File dispatcherFile) {
		this.verboseOutput = fileConfig.getBoolean("verbose-outputs");
		this.silentLogs = fileConfig.getBoolean("silent-logs");
		this.useLatestNMSVersion = fileConfig.getBoolean("use-latest-nms-version");
		this.message_missingExecutorImplementation = fileConfig.getString("messages.missing-executor-implementation");
		this.dispatcherFile = fileConfig.getBoolean("create-dispatcher-json") ? dispatcherFile : null;
		this.pluginsToConvert = new HashMap<>();
		this.skipSenderProxy = new ArrayList<>();
		this.commandsToConvert = new ArrayList<>();
		this.nbtContainerClass = nbtContainerClass;
		this.nbtContainerConstructor = nbtContainerConstructor;
		this.customNMS = null;

		if (!fileConfig.getList("plugins-to-convert").isEmpty()
			&& fileConfig.getMapList("plugins-to-convert").isEmpty()) {
			CommandAPI.getLogger()
				.severe("plugins-to-convert has an invalid type. Did you miss a colon (:) after a plugin name?");
		}

		for (Map<?, ?> map : fileConfig.getMapList("plugins-to-convert")) {
			String[] pluginCommands;
			if (map.values() == null || (map.values().size() == 1 && map.values().iterator().next() == null)) {
				pluginCommands = new String[0];
			} else {
				@SuppressWarnings("unchecked")
				List<String> commands = (List<String>) map.values().iterator().next();
				pluginCommands = commands.toArray(new String[0]);
			}

			String pluginName = (String) map.keySet().iterator().next();
			Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
			if (plugin != null) {
				if (plugin instanceof JavaPlugin javaPlugin) {
					pluginsToConvert.put(javaPlugin, pluginCommands);
				} else {
					new InvalidPluginException("Plugin " + pluginName + " is not a JavaPlugin!").printStackTrace();
				}
			} else {
				new InvalidPluginException("Could not find a plugin " + pluginName + "! Has it been loaded properly?")
					.printStackTrace();
			}
		}

		for (String pluginName : fileConfig.getStringList("skip-sender-proxy")) {
			if (Bukkit.getPluginManager().getPlugin(pluginName) != null) {
				this.skipSenderProxy.add(pluginName);
			} else {
				new InvalidPluginException("Could not find a plugin " + pluginName + "! Has it been loaded properly?")
					.printStackTrace();
			}
		}

		for (String commandName : fileConfig.getStringList("other-commands-to-convert")) {
			this.commandsToConvert.add(commandName);
		}
	}

	/**
	 * Creates an {@link InternalConfig}
	 * 
	 * @param verbose whether verbose logging should be enabled or not
	 */
	@Deprecated(forRemoval = true)
	public InternalConfig(boolean verbose) {
		this(new CommandAPIConfig().verboseOutput(verbose));
	}

	/**
	 * Creates an {@link InternalConfig} from a {@link CommandAPIConfig}
	 * 
	 * @param config The configuration to use to set up this internal configuration
	 */
	public InternalConfig(CommandAPIConfig config) {
		this.verboseOutput = config.verboseOutput;
		this.silentLogs = config.silentLogs;
		this.useLatestNMSVersion = config.useLatestNMSVersion;
		this.message_missingExecutorImplementation = config.missingExecutorImplementationMessage;
		this.dispatcherFile = config.dispatcherFile;
		this.pluginsToConvert = new HashMap<>();
		this.skipSenderProxy = new ArrayList<>();
		this.commandsToConvert = new ArrayList<>();
		this.nbtContainerClass = config.nbtContainerClass;
		this.nbtContainerConstructor = config.nbtContainerConstructor;
		this.customNMS = config.customNMS;
	}

	/**
	 * @return Whether verbose output is enabled
	 */
	public boolean hasVerboseOutput() {
		return this.verboseOutput;
	}

	/**
	 * @return Whether silent logs is enabled
	 */
	public boolean hasSilentLogs() {
		return this.silentLogs;
	}

	/**
	 * @return Whether the CommandAPI should use the latest available NMS version
	 */
	public boolean shouldUseLatestNMSVersion() {
		return this.useLatestNMSVersion;
	}

	/**
	 * @return The message to display if a command executor does not have an
	 *         implementation for a given type
	 */
	public String getMissingImplementationMessage() {
		return this.message_missingExecutorImplementation;
	}

	/**
	 * @return The {@link File} which should be used to create a JSON representation
	 *         of Brigadier's command tree
	 */
	public File getDispatcherFile() {
		return this.dispatcherFile;
	}

	/**
	 * @return A set of plugins and a list of commands to convert
	 */
	public Set<Entry<JavaPlugin, String[]>> getPluginsToConvert() {
		return this.pluginsToConvert.entrySet();
	}

	/**
	 * @param plugin A plugin where sender proxying should be skipped
	 * @return Whether sender proxying should be skipped for a given plugin
	 */
	public boolean shouldSkipSenderProxy(Plugin plugin) {
		return this.skipSenderProxy.contains(plugin.getName());
	}

	/**
	 * @param commandName A command where sender proxying should be skipped
	 * @return Whether sender proxying should be skipped for a given command
	 */
	public boolean shouldSkipSenderProxy(String commandName) {
		return this.skipSenderProxy.contains(commandName);
	}

	/**
	 * @return A list of commands to convert to Vanilla-compatible commands
	 */
	public List<String> getCommandsToConvert() {
		return this.commandsToConvert;
	}

	/**
	 * @return The NBT Tag Compound implementation class
	 */
	public Class<?> getNBTContainerClass() {
		return this.nbtContainerClass;
	}

	/**
	 * @return A function that takes in an Object (NMS NBTTagCompound) and returns
	 *         an implementation of an NBT Tag Compound
	 */
	public Function<Object, ?> getNBTContainerConstructor() {
		return this.nbtContainerConstructor;
	}

	public NMS<?> getCustomNMS() {
		return this.customNMS;
	}

}