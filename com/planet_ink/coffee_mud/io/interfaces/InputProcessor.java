package com.planet_ink.coffee_mud.io.interfaces;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * The InputProcessor interface defines methods for handling various types of input
 * in the CoffeeMUD system, including blocking, non-blocking, and line-based input.
 */
public interface InputProcessor {
	/**
	 * Reads a complete line of input, potentially blocking until a full line is available.
	 * This method uses nonBlockingIn internally, which returns control depending on the configured
	 * socket timeout, allowing for periodic checks and processing.
	 *
	 * @return The complete line of input, or null if the session is killed or disconnected.
	 * @throws IOException If an I/O error occurs during reading.
	 */
	String readlineContinue() throws IOException;

	/**
	 * Attempts to read a single character or process special sequences with minimal blocking.
	 * This method will return control to depending on the configured socket timeout,
	 * even if no input is available.
	 *
	 * @param appendInputFlag If true, appends the input to an internal buffer.
	 * @return 0 for line end, 1 for continuation, -1 for no input (including timeout), or the character read.
	 * @throws IOException If an I/O error occurs during reading.
	 */
	int nonBlockingIn(boolean appendInputFlag) throws IOException;

	/**
	 * Blocks until a complete line of input is available or a timeout occurs.
	 * This method provides controlled blocking with a timeout, suitable for
	 * scenarios requiring responsiveness while still waiting for complete input.
	 * It uses nonBlockingIn internally, which returns control depending on the configured socket timeout.
	 *
	 * @param maxTime The maximum time to wait in milliseconds. A value <= 0 means wait indefinitely.
	 * @param filter If true, applies input filtering and MXP processing.
	 * @return The complete line of input, or null if the specified timeout is reached.
	 * @throws IOException If an I/O error occurs during reading.
	 * @throws InterruptedIOException If the method is interrupted.
	 */
	String blockingIn(long maxTime, boolean filter) throws IOException;

	/**
	 * Displays a prompt message to the user without waiting for input.
	 * This method is typically used to show a message before requesting input.
	 *
	 * @param msg The prompt message to display.
	 */
	void promptPrint(final String msg);

	/**
	 * Displays the current prompt to the user.
	 * This method is typically used to show the standard prompt or command line indicator.
	 */
	void showPrompt();
}
