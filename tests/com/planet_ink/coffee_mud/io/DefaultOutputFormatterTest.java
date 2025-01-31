package com.planet_ink.coffee_mud.io;

import com.planet_ink.coffee_mud.core.Log;
import com.planet_ink.coffee_mud.io.interfaces.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class DefaultOutputFormatterTest {

	@Mock
	private OutputHandler outputHandler;

	@Mock
	private IOExceptionHandler exceptionHandler;

	private DefaultOutputFormatter formatter;

	private SnoopManager snoopManager;

	@Mock
	private OutputTranslator translator;

	@BeforeAll
	static public void setupMockLog() {
		MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class);
		Log mockLog = Mockito.mock(Log.class);
		logMockedStatic.when(Log::instance).thenReturn(mockLog);
		logMockedStatic.when(Log::debugChannelOn).thenReturn(false);
	}

	@AfterAll
	static public void cleanupMockLog() {
		Log.instance();
		Mockito.clearAllCaches();
	}

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		outputHandler = mock(OutputHandler.class);
		exceptionHandler = mock(IOExceptionHandler.class);
		snoopManager = mock(SnoopManager.class);
		final OutputFormattingContext formattingContext = mock(OutputFormattingContext.class);
		final BlockingInputProvider inputProvider = mock(BlockingInputProvider.class);
		translator = mock(OutputTranslator.class);

		formatter = new DefaultOutputFormatter(outputHandler, formattingContext, snoopManager, exceptionHandler);
		formatter.setTranslator(translator);
		formatter.setBlockingInputProvider(inputProvider);
	}

	@Test
	public void testRawCharsOut() throws IOException {
		// Test with normal char array
		char[] normalChars = "Hello".toCharArray();
		formatter.rawCharsOut(normalChars);
		verify(outputHandler).rawCharsOut(normalChars);

		// Test with empty char array
		char[] emptyChars = new char[0];
		formatter.rawCharsOut(emptyChars);
		verify(outputHandler).rawCharsOut(emptyChars);

		// Test with null
		formatter.rawCharsOut((char[]) null);
		verify(outputHandler, never()).rawCharsOut(null);
	}

	@Test
	public void testRawCharsOut_WithException() throws IOException {
		char[] testChars = "Test".toCharArray();
		doThrow(new IOException("Test exception")).when(outputHandler).rawCharsOut(any(char[].class));
		formatter.rawCharsOut(testChars);
		verify(exceptionHandler).handleException(any(IOException.class));
	}

	@Test
	public void testRawOut_WithNormalString() throws IOException {
		formatter.rawOut("Hello");
		verify(outputHandler).rawCharsOut("Hello".toCharArray());
	}

	@Test
	public void testRawOut_WithEmptyString() throws IOException {
		formatter.rawOut("");
		verify(outputHandler).rawCharsOut(new char[0]);
	}

	@Test
	public void testRawOut_WithNull() throws IOException {
		formatter.rawOut(null);
		verify(outputHandler, never()).rawCharsOut(any());
	}


	@Test
	public void testOnlyPrint_WithNormalString() throws IOException {
		formatter.onlyPrint("Hello");
		verify(snoopManager).snoopSupportPrint("Hello", false);
		verify(outputHandler).rawCharsOut("Hello".toCharArray());
	}

	@Test
	public void testOnlyPrint_WithEmptyString() throws IOException {
		formatter.onlyPrint("");
		verify(snoopManager).snoopSupportPrint("", false);
		verify(outputHandler).rawCharsOut(new char[0]);
	}

	@Test
	public void testOnlyPrint_WithNull() throws IOException {
		formatter.onlyPrint(null);
		verify(snoopManager, never()).snoopSupportPrint(any(), anyBoolean());
		verify(outputHandler, never()).rawCharsOut(any());
	}

	@Test
	public void testOnlyPrint_SpamPrevention() throws IOException {
		String spamMsg = "Spam\n\r";
		when(translator.translate(spamMsg)).thenReturn(spamMsg);
		formatter.onlyPrint(spamMsg, false);
		formatter.onlyPrint(spamMsg, false);
		formatter.onlyPrint(spamMsg, false);
		verify(outputHandler, times(1)).rawCharsOut(spamMsg.toCharArray());
	}
}
