package com.planet_ink.coffee_mud.session.interfaces;

import com.planet_ink.coffee_mud.core.interfaces.MUDCmdProcessor;

/**
 * The CommandManager interface defines the contract for managing and processing
 * user input commands in the CoffeeMUD system.
 */
public interface CommandManager {

	/**
	 * Handles the input command from the user.
	 *
	 * @param input The raw input string from the user.
	 * @param aliasProvider The provider for player-defined command aliases.
	 * @param cmdProcessor The processor for executing MUD commands.
	 */
	void handleInput(String input, PlayerAliasProvider aliasProvider, MUDCmdProcessor cmdProcessor);

	/**
	 * Retrieves the total processing time in milliseconds for all commands
	 * handled by this manager since its creation or last reset.
	 *
	 * @return The total processing time in milliseconds.
	 */
	long getTotalProcessingTimeMillis();

	/**
	 * Retrieves the start time of the most recent command processing operation.
	 *
	 * @return The processing start time in milliseconds since the epoch.
	 */
	long getProcessingStartTimeMillis();
}
