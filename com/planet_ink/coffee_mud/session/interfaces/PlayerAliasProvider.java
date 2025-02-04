package com.planet_ink.coffee_mud.session.interfaces;

/**
 * The PlayerAliasProvider interface provides methods to manage command aliases for a player.
 */
public interface PlayerAliasProvider {
	/**
	 * Returns the string array set of defined alias commands for this player.
	 *
	 * @return the string array set of defined alias commands.
	 */
	public String[] getAliasNames();

	/**
	 * Returns the definition of the given alias command for this player.
	 *
	 * @param named the alias command to get the definition of
	 * @return the command(s) to execute when the command is entered.
	 */
	public String getAlias(String named);

	/**
	 * Adds a new alias command for this player, undefined at first.
	 *
	 * @param named the name of the alias command to add
	 */
	public void addAliasName(String named);

	/**
	 * Removes an old alias command for this player.
	 *
	 * @param named the name of the alias command to delete
	 */
	public void delAliasName(String named);

	/**
	 * Modifies the commands executed by an existing alias command.
	 *
	 * @param named the alias command to modify
	 * @param value the new command(s) to execute
	 */
	public void setAlias(String named, String value);
}
