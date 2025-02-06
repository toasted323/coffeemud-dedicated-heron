package com.planet_ink.coffee_mud.session.interfaces;

import java.util.List;

public interface StringParsingUtility {
	List<String> parseCommas(String str, boolean ignoreQuotes);
	boolean containsIgnoreCase(List<String> list, String str);
	List<String> cleanParameterList(String rest);
}