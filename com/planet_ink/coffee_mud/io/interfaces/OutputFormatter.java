package com.planet_ink.coffee_mud.io.interfaces;

import com.planet_ink.coffee_mud.Common.interfaces.Session;
import com.planet_ink.coffee_mud.MOBS.interfaces.MOB;
import com.planet_ink.coffee_mud.core.interfaces.Environmental;
import com.planet_ink.coffee_mud.core.interfaces.Physical;

import java.io.IOException;

public interface OutputFormatter {

	/**
	 * Sets the translator for this IOManager.
	 *
	 * @param translator The translator to use for message translation.
	 */
	void setTranslator(OutputTranslator translator);

	/**
	 * Sets the BlockingInputProvider for this OutputFormatter.
	 * The BlockingInputProvider is used to handle blocking input operations.
	 *
	 * @param provider The BlockingInputProvider to be used by this OutputFormatter
	 */
	void setBlockingInputProvider(BlockingInputProvider provider);

	/**
	 * Sets the session for this OutputFormatter.
	 *
	 * @param session The session to associate with this OutputFormatter.
	 */
	void setSession(Session session);

	/**
	 * Sets the MOB (Mobile Object) for this OutputFormatter.
	 *
	 * @param mob The MOB to associate with this OutputFormatter.
	 */
	void setMob(MOB mob);

	/**
	 * Outputs raw characters to the underlying output stream.
	 *
	 * @param chars The array of characters to output.
	 */
	void rawCharsOut(char[] chars);

	/**
	 * Outputs a raw string message to the underlying output stream.
	 *
	 * @param msg The raw message to output.
	 */
	void rawOut(String msg);

	/**
	 * Prints a message to the output stream.
	 *
	 * @param msg The message to print.
	 */
	void onlyPrint(final String msg);

	/**
	 * Prints a message to the output stream.
	 *
	 * @param msg     The message to print.
	 * @param noCache If true, the message will not be cached.
	 */
	void onlyPrint(String msg, boolean noCache);

	/**
	 * Resets the spam stack.
	 */
	void resetSpamStack();


	/**
	 * maximum number of last messages stored
	 */
	int MAX_PREVMSGS = 100;

	/*
	 * Raw Print Methods
	 */

	/**
	 * Prints a raw message to the output stream.
	 *
	 * @param msg The raw message to print.
	 */
	void rawPrint(String msg);

	/**
	 * Prints a raw message followed by a newline to the output stream.
	 *
	 * @param msg The raw message to print.
	 */
	void rawPrintln(String msg);

	/**
	 * Safely prints a raw message to the output stream, applying MXP safety filters.
	 * This method protects MXP clients by escaping special characters.
	 * It does not perform word-wrapping, text filtering, or ('\','n') to '\n' translations.
	 *
	 * @param msg The raw message to print safely.
	 */
	void safeRawPrint(String msg);

	/**
	 * Safely prints a raw message followed by a newline to the output stream, applying MXP safety filters.
	 * This method protects MXP clients by escaping special characters.
	 * It does not perform word-wrapping, text filtering, or ('\','n') to '\n' translations.
	 *
	 * @param msg The raw message to print safely.
	 */
	void safeRawPrintln(String msg);

	/*
	 * Standard Print Methods
	 */

	/**
	 * Prints a message to the output stream, applying any necessary filters.
	 *
	 * @param msg The message to print.
	 */
	void print(String msg);

	/**
	 * Prints a message followed by a newline to the output stream.
	 *
	 * @param msg The message to print.
	 */
	void println(String msg);

	/**
	 * Prints a standard message to the output stream.
	 *
	 * @param msg The standard message to print.
	 */
	void stdPrint(String msg);

	/**
	 * Prints a standard message followed by a newline to the output stream.
	 *
	 * @param msg The standard message to print.
	 */
	void stdPrintln(String msg);

	/*
	 * Contextual Print Methods
	 */

	/**
	 * Prints a message to the output stream, considering source, target, and tool.
	 *
	 * @param src The source of the message.
	 * @param trg The target of the message.
	 * @param tol The tool used in the message.
	 * @param msg The message to print.
	 */
	void print(Physical src, Environmental trg, Environmental tol, String msg);

	/**
	 * Prints a message followed by a newline to the output stream, considering source, target, and tool.
	 *
	 * @param src    The source of the message.
	 * @param target The target of the message.
	 * @param tool   The tool used in the message.
	 * @param msg    The message to print.
	 */
	void println(Physical src, Environmental target, Environmental tool, String msg);

	/**
	 * Prints a standard message to the output stream, considering source, target, and tool.
	 *
	 * @param src The source of the message.
	 * @param trg The target of the message.
	 * @param tol The tool used in the message.
	 * @param msg The standard message to print.
	 */
	void stdPrint(Physical src, Environmental trg, Environmental tol, String msg);

	/**
	 * Prints a standard message followed by a newline to the output stream, considering source, target, and tool.
	 *
	 * @param src    The source of the message.
	 * @param target The target of the message.
	 * @param tool   The tool used in the message.
	 * @param msg    The standard message to print.
	 */
	void stdPrintln(Physical src, Environmental target, Environmental tool, String msg);

	/*
	 * Wrapless Print Methods
	 */

	/**
	 * Prints a message without wrapping to the output stream.
	 *
	 * @param msg The message to print without wrapping.
	 */
	void wraplessPrint(String msg);

	/**
	 * Prints a message without wrapping, followed by a newline to the output stream.
	 *
	 * @param msg The message to print without wrapping.
	 */
	void wraplessPrintln(String msg);

	/*
	 * Color-Only Print Methods
	 */

	/**
	 * Prints a color-only message to the output stream.
	 *
	 * @param msg The color-only message to print.
	 */
	void colorOnlyPrint(String msg);

	/**
	 * Prints a color-only message to the output stream, with caching option.
	 *
	 * @param msg     The color-only message to print.
	 * @param noCache If true, the message will not be cached.
	 */
	void colorOnlyPrint(String msg, boolean noCache);

	/**
	 * Prints a color-only message followed by a newline to the output stream.
	 *
	 * @param msg The color-only message to print.
	 */
	void colorOnlyPrintln(String msg);

	/**
	 * Prints a color-only message followed by a newline to the output stream, with caching option.
	 *
	 * @param msg     The color-only message to print.
	 * @param noCache If true, the message will not be cached.
	 */
	void colorOnlyPrintln(String msg, boolean noCache);


	/**
	 * Adds a session filter to the OutputFormatter.
	 *
	 * @param filter The SessionFilter to add.
	 * @return true if the filter was successfully added, false otherwise.
	 */
	boolean addSessionFilter(final Session.SessionFilter filter);

	/**
	 * Sets whether a prompt is needed after the next output.
	 *
	 * @param value true if a prompt is needed, false otherwise.
	 */
	void setNeedPrompt(boolean value);

	/**
	 * Checks if a prompt is needed after the next output.
	 *
	 * @return true if a prompt is needed, false otherwise.
	 */
	boolean getNeedPrompt();

	/**
	 * Closes the output handler and releases any associated resources.
	 *
	 * @throws IOException If an error occurs during closing.
	 */
	void shutdown() throws IOException;
}
