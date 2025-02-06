package com.planet_ink.coffee_mud.session.interfaces;

import java.io.IOException;
import java.util.List;

/**
 * The SyncModalDialogManager interface defines methods for handling modal dialogs
 * in the CoffeeMUD system using "blocking" input handlers.
 *
 * Note: The term "Sync" in CoffeeMUD jargon refers to methods that use blocking input,
 * typically implemented with longer polling intervals. Despite the name, this does not
 * imply true synchronous behavior in the conventional sense of programming.
 * The underlying implementation still uses polling, governed by thread timing and socket timeouts.
 */
public interface SyncModalDialogManager {

	/**
	 * Prompts the user with a message and waits for input.
	 *
	 * @param message The prompt message to display to the user.
	 * @return The user's input as a String.
	 * @throws IOException If an I/O error occurs during reading.
	 */
	String prompt(String message) throws IOException;

	/**
	 * Prompts the user with a message and waits for input with a timeout.
	 *
	 * @param message The prompt message to display to the user.
	 * @param maxTime The maximum time to wait for input in milliseconds.
	 * @return The user's input as a String, or an empty string if the timeout is reached.
	 * @throws IOException If an I/O error occurs during reading.
	 */
	String prompt(String message, long maxTime) throws IOException;

	/**
	 * Prompts the user with a message and provides a default value.
	 *
	 * @param message The prompt message to display to the user.
	 * @param defaultValue The default value to use if the user provides no input.
	 * @return The user's input as a String, or the default value if no input is provided.
	 * @throws IOException If an I/O error occurs during reading.
	 */
	String prompt(String message, String defaultValue) throws IOException;

	/**
	 * Prompts the user with a message, provides a default value, and waits for input with a timeout.
	 *
	 * @param message The prompt message to display to the user.
	 * @param defaultValue The default value to use if the user provides no input.
	 * @param maxTime The maximum time to wait for input in milliseconds.
	 * @return The user's input as a String, the default value if no input is provided, or the default value if the timeout is reached.
	 * @throws IOException If an I/O error occurs during reading.
	 */
	String prompt(String message, String defaultValue, long maxTime) throws IOException;

	/**
	 * Prompts the user for confirmation with a yes/no question.
	 *
	 * @param message The confirmation message to display to the user.
	 * @param defaultValue The default value ("Y" or "N") to use if the user provides no input.
	 * @return true if the user confirms (answers "Y"), false otherwise.
	 * @throws IOException If an I/O error occurs during reading.
	 */
	boolean confirm(String message, String defaultValue) throws IOException;

	/**
	 * Prompts the user for confirmation with a yes/no question and a timeout.
	 *
	 * @param message The confirmation message to display to the user.
	 * @param defaultValue The default value ("Y" or "N") to use if the user provides no input or if the timeout is reached.
	 * @param maxTime The maximum time to wait for input in milliseconds.
	 * @return true if the user confirms (answers "Y"), false otherwise.
	 * @throws IOException If an I/O error occurs during reading.
	 */
	boolean confirm(String message, String defaultValue, long maxTime) throws IOException;

	/**
	 * Prompts the user to choose from a list of options.
	 *
	 * @param message The prompt message to display to the user.
	 * @param choices A string containing the available choices.
	 * @param defaultValue The default choice to use if the user provides no input.
	 * @return The user's chosen option as a String.
	 * @throws IOException If an I/O error occurs during reading.
	 */
	String choose(String message, String choices, String defaultValue) throws IOException;

	/**
	 * Prompts the user to choose from a list of options with a timeout.
	 *
	 * @param message The prompt message to display to the user.
	 * @param choices A string containing the available choices.
	 * @param defaultValue The default choice to use if the user provides no input or if the timeout is reached.
	 * @param maxTime The maximum time to wait for input in milliseconds.
	 * @return The user's chosen option as a String.
	 * @throws IOException If an I/O error occurs during reading.
	 */
	String choose(String message, String choices, String defaultValue, long maxTime) throws IOException;

	/**
	 * Prompts the user to choose from a list of options with a timeout and additional parameter output.
	 *
	 * @param message The prompt message to display to the user.
	 * @param choices A string containing the available choices.
	 * @param defaultValue The default choice to use if the user provides no input or if the timeout is reached.
	 * @param maxTime The maximum time to wait for input in milliseconds.
	 * @param paramsOut A list to store any additional parameters provided with the choice.
	 * @return The user's chosen option as a String.
	 * @throws IOException If an I/O error occurs during reading.
	 */
	String choose(String message, String choices, String defaultValue, long maxTime, List<String> paramsOut) throws IOException;
}
