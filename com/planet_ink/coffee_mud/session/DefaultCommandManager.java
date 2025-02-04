package com.planet_ink.coffee_mud.session;

import com.planet_ink.coffee_mud.core.interfaces.MUDCmdProcessor;
import com.planet_ink.coffee_mud.io.interfaces.OutputFormatter;
import com.planet_ink.coffee_mud.session.interfaces.*;

import java.util.ArrayList;
import java.util.List;

public class DefaultCommandManager implements CommandManager {
	private final CommandManagerContext context;
	private final InputParser inputParser;
	private final OutputFormatter outputFormatter;
	private final PlayerAliasExpander aliasExpander;
	private final CommandTranslator commandTranslator;
	private final PlayerCommandMetaFlags metaFlagsProvider;

	private long totalProcessingTimeMillis = 0;
	private long processingStartTimeMillis = 0;
	private long processingEndTimeMillis = 0;

	public DefaultCommandManager(CommandManagerContext context,
								 InputParser inputParser,
								 OutputFormatter outputFormatter,
								 PlayerAliasExpander aliasExpander,
								 CommandTranslator commandTranslator,
								 PlayerCommandMetaFlags metaFlagsProvider) {
		this.context = context;
		this.inputParser = inputParser;
		this.outputFormatter = outputFormatter;
		this.aliasExpander = aliasExpander;
		this.commandTranslator = commandTranslator;
		this.metaFlagsProvider = metaFlagsProvider;
	}

	public void handleInput(String input, PlayerAliasProvider aliasProvider, MUDCmdProcessor cmdProcessor) {
		if (input == null || input.isEmpty()) {
			return;
		}

		// Parse the input
		List<String> parsedInput = inputParser.parse(input);
		if (parsedInput.isEmpty()) {
			return;
		}

		// Add to I/O msg log
		context.addLastMsg(input);

		// Handle aliases
		String firstWord = parsedInput.get(0);
		String aliasDefinition = aliasProvider.getAlias(firstWord);
		List<List<String>> executableCommands = new ArrayList<>();
		boolean[] doEcho = new boolean[1];

		if (aliasDefinition != null && !aliasDefinition.isEmpty()) {
			parsedInput.remove(0);
			aliasExpander.expandAlias(aliasDefinition, parsedInput, executableCommands, doEcho);
		}
		else {
			executableCommands.add(parsedInput);
			doEcho[0] = true;
		}

		// Set meta flags
		int metaFlags = metaFlagsProvider.getMetaFlags();
		if (executableCommands.size() > 1) {
			metaFlags |= MUDCmdProcessor.METAFLAG_INORDER;
		}

		// Store current mob actions
		final double curActions = cmdProcessor.actions();
		cmdProcessor.setActions(0.0);

		// Process commands
		for (List<String> command : executableCommands) {
			// Add command to history
			context.setPreviousCmd(command);

			// Timing start
			processingStartTimeMillis = System.currentTimeMillis();

			// Echo command if needed
			if (doEcho[0]) {
				outputFormatter.rawPrintln(String.join(" ", command));
			}

			// Translate command
			List<List<String>> translatedCommands = commandTranslator.translate(command);

			// Enqueue commands for execution
			for (List<String> translatedCommand : translatedCommands) {
				cmdProcessor.enqueCommand(translatedCommand, metaFlags, 0);
			}

			// Update timing
			processingEndTimeMillis = System.currentTimeMillis();
			totalProcessingTimeMillis += (processingEndTimeMillis - processingStartTimeMillis);
		}

		// Restore mob actions
		cmdProcessor.setActions(curActions);
	}

	@Override
	public long getTotalProcessingTimeMillis() {
		return totalProcessingTimeMillis;
	}

	@Override
	public long getProcessingStartTimeMillis() {
		return processingStartTimeMillis;
	}
}
