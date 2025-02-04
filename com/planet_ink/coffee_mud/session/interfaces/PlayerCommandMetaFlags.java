package com.planet_ink.coffee_mud.session.interfaces;

import com.planet_ink.coffee_mud.core.interfaces.MUDCmdProcessor;

/**
 * The PlayerCommandMetaFlags interface provides methods to manage meta flags for player commands.
 *
 * @see MUDCmdProcessor
 */
public interface PlayerCommandMetaFlags {
	/**
	 * Retrieves the current meta flags for player commands.
	 *
	 * @return An integer representing the current meta flags.
	 */
	int getMetaFlags();
}