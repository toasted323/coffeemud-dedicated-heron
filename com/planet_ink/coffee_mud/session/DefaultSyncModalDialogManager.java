package com.planet_ink.coffee_mud.session;

import com.planet_ink.coffee_mud.io.interfaces.InputProcessor;
import com.planet_ink.coffee_mud.session.interfaces.StringParsingUtility;
import com.planet_ink.coffee_mud.session.interfaces.SyncModalDialogManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultSyncModalDialogManager implements SyncModalDialogManager {
	private final InputProcessor inputProcessor;
	private final StringParsingUtility stringParser;

	public DefaultSyncModalDialogManager(InputProcessor inputProcessor, StringParsingUtility stringParser) {
		this.inputProcessor = inputProcessor;
		this.stringParser = stringParser;
	}

	@Override
	public String prompt(String message) throws IOException {
		return prompt(message, -1);
	}

	@Override
	public String prompt(String message, long maxTime) throws IOException {
		inputProcessor.promptPrint(message);
		String input = inputProcessor.blockingIn(maxTime, true);
		if (input == null)
			return "";
		if ((input.length() > 0) && (input.charAt(input.length() - 1) == '\\'))
			return input.substring(0, input.length() - 1);
		return input;
	}

	@Override
	public String prompt(String message, String defaultValue) throws IOException {
		return prompt(message, defaultValue, -1);
	}

	@Override
	public String prompt(String message, String defaultValue, long maxTime) throws IOException {
		String msg = prompt(message, maxTime).trim();
		if (msg.equals(""))
			return defaultValue;
		return msg;
	}

	@Override
	public boolean confirm(String message, String defaultValue) throws IOException {
		return confirm(message, defaultValue, -1);
	}

	@Override
	public boolean confirm(String message, String defaultValue, long maxTime) throws IOException {
		if (defaultValue.toUpperCase().startsWith("T"))
			defaultValue = "Y";
		String yn = choose(message, "YN", defaultValue, maxTime);
		return yn.equals("Y");
	}

	@Override
	public String choose(String message, String choices, String defaultValue) throws IOException {
		return choose(message, choices, defaultValue, -1);
	}

	@Override
	public String choose(String message, String choices, String defaultValue, long maxTime) throws IOException {
		return choose(message, choices, defaultValue, maxTime, null);
	}

	@Override
	public String choose(String message, String choices, String defaultValue, long maxTime, List<String> paramsOut) throws IOException {
		String yn = "";
		String rest = null;
		final List<String> choiceList;
		final boolean oneChar = choices.indexOf(',') < 0;
		if (!oneChar)
			choiceList = stringParser.parseCommas(choices, true);
		else {
			choiceList = new ArrayList<>();
			for (final char c : choices.toCharArray())
				choiceList.add("" + c);
		}
		while ((yn.equals("") || (!stringParser.containsIgnoreCase(choiceList, yn)))) {
			inputProcessor.promptPrint(message);
			yn = inputProcessor.blockingIn(maxTime, true);
			if (yn == null)
				return defaultValue.toUpperCase();
			yn = yn.trim();
			if (yn.equals(""))
				return defaultValue.toUpperCase();
			if ((yn.length() > 1) && (oneChar)) {
				if (paramsOut != null)
					rest = yn.substring(1).trim();
				yn = yn.substring(0, 1).toUpperCase();
			}
			else if (oneChar)
				yn = yn.toUpperCase();
		}
		if ((rest != null) && (paramsOut != null) && (rest.length() > 0))
			paramsOut.addAll(stringParser.cleanParameterList(rest));
		return yn;
	}
}
