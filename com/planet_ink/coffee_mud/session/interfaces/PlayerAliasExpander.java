package com.planet_ink.coffee_mud.session.interfaces;

import java.util.List;

/**
 * The PlayerAliasExpander interface provides methods to expand aliases into executable commands.
 */
public interface PlayerAliasExpander {
	/**
	 * Expands a raw alias definition into a list of executable commands.
	 *
	 * @param rawAliasDefinition The raw alias definition to expand.
	 * @param parsedInput The parsed input arguments.
	 * @param executableCommands The list to populate with expanded commands.
	 * @param doEcho An array with a single boolean element, indicating whether to echo the commands.
	 */
	void expandAlias(String rawAliasDefinition, List<String> parsedInput,
					 List<List<String>> executableCommands, boolean[] doEcho);
}