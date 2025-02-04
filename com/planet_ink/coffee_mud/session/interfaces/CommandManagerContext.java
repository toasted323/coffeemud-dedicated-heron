package com.planet_ink.coffee_mud.session.interfaces;

import java.util.Enumeration;
import java.util.List;

/**
 * The CommandManagerContext interface provides methods to manage command history
 * and recent messages for a user session in CoffeeMUD.
 */
public interface CommandManagerContext {
	/**
	 * Adds a message to the recent message list.
	 * This method is used to track raw input messages as they are received,
	 * before any processing and output messages right before they are sent.
	 *
	 * @param msg The raw message to add to the recent message list.
	 */
	void addLastMsg(String msg);

	/**
	 * Retrieves the list of recent raw input and output messages for the current session.
	 *
	 * @return A list of recent raw message strings.
	 */
	List<String> getLastMsgs();

	/**
	 * Retrieves the most recent command entered by the user in this session.
	 *
	 * Commands are stored after alias expansion but before preprocessing,
	 * representing what would be echoed back to the user.
	 *
	 * @return A list of strings representing the components of the last command after alias expansion.
	 */
	List<String> getPreviousCMD();

	/**
	 * Sets the most recent command entered by the user in this session.
	 *
	 * Commands are stored after alias expansion but before preprocessing,
	 * representing what would be echoed back to the user.
	 *
	 * @param command A list of strings representing the components of the command to set as the previous command.
	 */
	void setPreviousCmd(List<String> command);

	/**
	 * Retrieves the entire command history for the current session.
	 * This history contains commands after alias expansion but before preprocessing,
	 * representing what would have been echoed back to the user.
	 *
	 * @return An Enumeration of List<String>, where each List represents a command after alias expansion.
	 */
	Enumeration<List<String>> getHistory();
}
