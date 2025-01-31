package com.planet_ink.coffee_mud.io.interfaces;

/**
 * This interface provides methods to access output formatting information
 * for a session, including terminal dimensions and text wrapping settings.
 */
public interface OutputFormattingContext {

	/**
	 * Gets the current text wrapping width.
	 * @return The number of characters at which to wrap text, or -1 if wrapping is disabled.
	 */
	int getWrap();

	/**
	 * Gets the current page break setting.
	 * @return The number of lines to display before pausing for user input, or -1 if page breaks are disabled.
	 */
	int getPageBreak();

	/**
	 * Gets the current terminal height.
	 * @return The number of rows in the terminal window, or -1 if unknown.
	 */
	int getTerminalHeight();

	/**
	 * Gets the current terminal width.
	 * @return The number of columns in the terminal window, or -1 if unknown.
	 */
	int getTerminalWidth();

	/**
	 * Adds a message to the list of last messages.
	 * This method is used to keep track of recent output for features like replay.
	 * @param msg The message to add to the last messages list.
	 */
	public void addLastMsg(String msg);
}
