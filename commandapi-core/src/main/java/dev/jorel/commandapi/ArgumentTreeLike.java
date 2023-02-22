package dev.jorel.commandapi;

import java.util.List;

public interface ArgumentTreeLike<S extends ArgumentTreeLike<?, ?>, T extends ArgumentTreeLike<?, ?>> {

	/**
	 * @return a list of all child nodes of this tree node.
	 */
	List<T> getArguments();

	/**
	 * Create a child branch on this node and sets the parent for the child node.
	 *
	 * @param tree The child branch
	 * @return this tree node
	 */
	S then(T tree);

	/**
	 * Retrieve the overlaying tree for this subtree. A tree has only one parent instance, so
	 * setting one tree instance as child of multiple other trees will only set the last
	 * parent tree as parent instance. Different parents should have different child instances of
	 * the same tree.
	 *
	 * @return the parent of this tree structure or null if this is a root node.
	 */
	ArgumentTreeLike<?, S> getParent();

	/**
	 * Sets a parent for this tree node. The method will be called ny {@link #then(ArgumentTreeLike)}
	 * and must not be called by the user.
	 *
	 * @param parent The new parent node of this node.
	 */
	void setParent(ArgumentTreeLike<?, S> parent);
}
