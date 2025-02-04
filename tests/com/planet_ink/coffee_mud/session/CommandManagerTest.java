package com.planet_ink.coffee_mud.session;

import com.planet_ink.coffee_mud.core.Log;
import com.planet_ink.coffee_mud.core.interfaces.MUDCmdProcessor;
import com.planet_ink.coffee_mud.io.interfaces.OutputFormatter;
import com.planet_ink.coffee_mud.session.interfaces.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

class CommandManagerTest {

	private DefaultCommandManager commandManager;
	private CommandManagerContext mockContext;
	private InputParser mockInputParser;
	private OutputFormatter mockOutputFormatter;
	private PlayerAliasExpander mockAliasExpander;
	private CommandTranslator mockCommandTranslator;
	private PlayerCommandMetaFlags mockMetaFlagsProvider;
	private PlayerAliasProvider mockAliasProvider;
	private MUDCmdProcessor mockCmdProcessor;

	@BeforeAll
	static public void setupMocks() {
		MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class);
		Log mockLog = Mockito.mock(Log.class);
		logMockedStatic.when(Log::instance).thenReturn(mockLog);
		logMockedStatic.when(Log::debugChannelOn).thenReturn(false);
	}

	@BeforeEach
	void setUp() {
		mockContext = mock(CommandManagerContext.class);
		mockInputParser = mock(InputParser.class);
		mockOutputFormatter = mock(OutputFormatter.class);
		mockAliasExpander = mock(PlayerAliasExpander.class);
		mockCommandTranslator = mock(CommandTranslator.class);
		mockMetaFlagsProvider = mock(PlayerCommandMetaFlags.class);
		mockAliasProvider = mock(PlayerAliasProvider.class);
		mockCmdProcessor = mock(MUDCmdProcessor.class);

		commandManager = new DefaultCommandManager(mockContext, mockInputParser, mockOutputFormatter,
				mockAliasExpander, mockCommandTranslator, mockMetaFlagsProvider);
	}

	@AfterAll
	static public void cleanupMocks() {
		Log.instance();
		Mockito.clearAllCaches();
	}

	@Test
	void testHandleInput_WithNullInput() {
		commandManager.handleInput(null, mockAliasProvider, mockCmdProcessor);
		verifyNoInteractions(mockContext, mockInputParser, mockOutputFormatter, mockAliasExpander,
				mockCommandTranslator, mockMetaFlagsProvider, mockAliasProvider, mockCmdProcessor);
	}

	@Test
	void testHandleInput_WithEmptyInput() {
		commandManager.handleInput("", mockAliasProvider, mockCmdProcessor);
		verifyNoInteractions(mockContext, mockInputParser, mockOutputFormatter, mockAliasExpander,
				mockCommandTranslator, mockMetaFlagsProvider, mockAliasProvider, mockCmdProcessor);
	}

	@Test
	void testHandleInput_WithNonAliasCommand() {
		List<String> parsedInput = Arrays.asList("look", "around");
		when(mockInputParser.parse("look around")).thenReturn(parsedInput);
		when(mockAliasProvider.getAlias("look")).thenReturn(null);
		when(mockMetaFlagsProvider.getMetaFlags()).thenReturn(0);
		when(mockCommandTranslator.translate(parsedInput)).thenReturn(Collections.singletonList(parsedInput));

		commandManager.handleInput("look around", mockAliasProvider, mockCmdProcessor);

		verify(mockContext).addLastMsg("look around");
		verify(mockContext).setPreviousCmd(parsedInput);
		verify(mockOutputFormatter).rawPrintln("look around");
		verify(mockCmdProcessor).enqueCommand(parsedInput, 0, 0);
	}

	@Test
	void testHandleInput_WithAlias() {
		List<String> parsedInput = new ArrayList<>(Collections.singletonList("l"));
		List<String> expandedCommand = Arrays.asList("look", "around");
		when(mockInputParser.parse("l")).thenReturn(parsedInput);
		when(mockAliasProvider.getAlias("l")).thenReturn("look around");
		when(mockMetaFlagsProvider.getMetaFlags()).thenReturn(0);
		doAnswer(invocation -> {
			List<List<String>> commands = invocation.getArgument(2);
			commands.add(expandedCommand);
			boolean[] doEcho = invocation.getArgument(3);
			doEcho[0] = true;
			return null;
		}).when(mockAliasExpander).expandAlias(eq("look around"), eq(Collections.emptyList()), any(), any());
		when(mockCommandTranslator.translate(expandedCommand)).thenReturn(Collections.singletonList(expandedCommand));

		commandManager.handleInput("l", mockAliasProvider, mockCmdProcessor);

		verify(mockContext).addLastMsg("l");
		verify(mockContext).setPreviousCmd(expandedCommand);
		verify(mockOutputFormatter).rawPrintln("look around");
		verify(mockCmdProcessor).enqueCommand(expandedCommand, 0, 0);
	}

	@Test
	void testHandleInput_WithMultipleCommands() {
		// Setup
		List<String> parsedInput = new ArrayList<>(Collections.singletonList("su"));
		String aliasDefinition = "cast shield;cast blink;cast flameshield";
		List<List<String>> expandedCommands = Arrays.asList(
				Arrays.asList("cast", "shield"),
				Arrays.asList("cast", "blink"),
				Arrays.asList("cast", "flameshield")
		);

		// Mocking
		when(mockInputParser.parse("su")).thenReturn(parsedInput);
		when(mockAliasProvider.getAlias("su")).thenReturn(aliasDefinition);
		when(mockMetaFlagsProvider.getMetaFlags()).thenReturn(0);
		doAnswer(invocation -> {
			List<List<String>> commands = invocation.getArgument(2);
			commands.addAll(expandedCommands);
			boolean[] doEcho = invocation.getArgument(3);
			doEcho[0] = true;
			return null;
		}).when(mockAliasExpander).expandAlias(eq(aliasDefinition), eq(Collections.emptyList()), any(), any());
		when(mockCommandTranslator.translate(any())).thenAnswer(i -> Collections.singletonList(i.getArgument(0)));

		// Execute
		commandManager.handleInput("su", mockAliasProvider, mockCmdProcessor);

		// Verify
		verify(mockContext).addLastMsg("su");
		verify(mockContext, times(3)).setPreviousCmd(any());
		verify(mockOutputFormatter).rawPrintln("cast shield");
		verify(mockOutputFormatter).rawPrintln("cast blink");
		verify(mockOutputFormatter).rawPrintln("cast flameshield");
		verify(mockCmdProcessor).enqueCommand(expandedCommands.get(0), MUDCmdProcessor.METAFLAG_INORDER, 0);
		verify(mockCmdProcessor).enqueCommand(expandedCommands.get(1), MUDCmdProcessor.METAFLAG_INORDER, 0);
		verify(mockCmdProcessor).enqueCommand(expandedCommands.get(2), MUDCmdProcessor.METAFLAG_INORDER, 0);
	}
}
