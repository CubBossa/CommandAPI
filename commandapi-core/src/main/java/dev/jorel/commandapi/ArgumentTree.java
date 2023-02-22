package dev.jorel.commandapi;

import dev.jorel.commandapi.arguments.Argument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is a base class for arguments, allowing them to behave as tree nodes in
 * a {@link CommandTree}
 */
public class ArgumentTree extends Executable<ArgumentTree> implements ArgumentTreeLike<ArgumentTree, ArgumentTree> {

	final List<ArgumentTree> arguments = new ArrayList<>();
	final Argument<?> argument;
	ArgumentTreeLike<?, ArgumentTree> parent;

	/**
	 * Instantiates an {@link ArgumentTree}. This can only be called if the class
	 * that extends this is an {@link Argument}
	 */
	protected ArgumentTree() {
		if (!(this instanceof Argument<?> argument)) {
			throw new IllegalArgumentException("Implicit inherited constructor must be from Argument");
		}
		this.argument = argument;
	}

	/**
	 * Instantiates an {@link ArgumentTree} with an underlying argument.
	 * 
	 * @param argument the argument to use as the underlying argument for this
	 *                 argument tree
	 */
	public ArgumentTree(final Argument<?> argument) {
		this.argument = argument;
		// Copy the executor in case any executions were defined on the argument
		this.executor = argument.executor;
	}

	@Override
	public ArgumentTreeLike<?, ArgumentTree> getParent() {
		return parent;
	}

	@Override
	public void setParent(ArgumentTreeLike<?, ArgumentTree> parent) {
		this.parent = parent;
	}

	@Override
	public List<ArgumentTree> getArguments() {
		return arguments;
	}

	public ArgumentTree then(final ArgumentTree tree) {
		this.arguments.add(tree);
		tree.setParent(this);
		tree.parent = this;
		return this;
	}

	List<Execution> getExecutions() {
		List<Execution> executions = new ArrayList<>();
		// If this is executable, add its execution
		if (this.executor.hasAnyExecutors()) {
			executions.add(new Execution(Arrays.asList(this.argument), this.executor));
		}
		// Add all executions from all arguments
		for (ArgumentTree tree : arguments) {
			for (Execution execution : tree.getExecutions()) {
				// Prepend this argument to the arguments of the executions
				executions.add(execution.prependedBy(this.argument));
			}
		}
		return executions;
	}
}
