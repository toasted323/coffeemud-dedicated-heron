package com.planet_ink.coffee_mud.io.interfaces;

import com.planet_ink.coffee_mud.Common.interfaces.Session;

public interface SnoopManager {

	/**
	 * Adds or removes a session from the list of snooping sessions.
	 * @param session The session to add or remove
	 * @param onOff True to add, false to remove
	 */
	void setBeingSnoopedBy(Session session, boolean onOff);

	/**
	 * Checks if a specific session is snooping on this session.
	 * @param S The session to check
	 * @return True if S is snooping, false otherwise. If S is null, returns true if there are no snoops.
	 */
	boolean isBeingSnoopedBy(Session S);

	/**
	 * Modifies the snoop suspension stack.
	 * @param change The amount to change the suspension stack by
	 * @return The new value of the snoop suspension stack
	 */
	int snoopSuspension(int change);

	/**
	 * Sends a message to all snooping sessions.
	 * @param msg The message to send
	 * @param noCache Whether to bypass caching
	 */
	void snoopSupportPrint(String msg, boolean noCache);
}

