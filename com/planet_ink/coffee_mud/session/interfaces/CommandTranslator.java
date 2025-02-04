package com.planet_ink.coffee_mud.session.interfaces;

import java.util.List;

/**
 * The CommandTranslator interface provides methods to pre-process and translate user commands.
 */
public interface CommandTranslator {
	/**
	 * Accepts user-entered pre-parsed command list, and generates
	 * a list containing one or more translated full command lists.
	 *
	 * @param cmds the command list
	 * @return a list with the translated command list
	 */
	List<List<String>> translate(List<String> cmds);
}