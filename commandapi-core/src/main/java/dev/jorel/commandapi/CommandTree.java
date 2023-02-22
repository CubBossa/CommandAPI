package dev.jorel.commandapi;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the root node for creating a command as a tree
 */
public class CommandTree extends ExecutableCommand<CommandTree> implements ArgumentTreeLike<CommandTree, ArgumentTree> {

	private final List<ArgumentTree> arguments = new ArrayList<>();

	/**
	 * Creates a main root node for a command tree with a given command name
	 * 
	 * @param commandName The name of the command to create
	 */
	public CommandTree(final String commandName) {
		super(commandName);
	}


	@Override
	public ArgumentTreeLike<?, CommandTree> getParent() {
		return null; // a command node cannot have a parent
	}

	@Override
	public void setParent(ArgumentTreeLike<?, CommandTree> parent) {
		// a command node cannot have a parent
	}

	@Override
	public List<ArgumentTree> getArguments() {
		return arguments;
	}

	public CommandTree then(final ArgumentTree tree) {
		this.arguments.add(tree);
		tree.parent = this;
		return this;
	}

	/**
	 * Registers the command
	 */
	public void register() {
		List<Execution> executions = new ArrayList<>();
		if (this.executor.hasAnyExecutors()) {
			executions.add(new Execution(new ArrayList<>(), this.executor));
		}
		for (ArgumentTree tree : arguments) {
			executions.addAll(tree.getExecutions());
		}
		for (Execution execution : executions) {
			execution.register(this.meta);
		}
	}

}
