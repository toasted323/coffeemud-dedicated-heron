package com.planet_ink.coffee_mud.io;

import com.planet_ink.coffee_mud.core.Log;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/*
   Copyright 2025 github.com/toasted323
   Copyright 2005-2024 Bo Zimmerman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

public class DefaultInputHandlerTest {
	private ByteArrayInputStream inputStream;
	private DefaultInputHandler inputHandler;

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
		final Charset inputCharSet = StandardCharsets.UTF_8;
		inputStream = new ByteArrayInputStream(new byte[]{});
		inputHandler = new DefaultInputHandler(
				inputStream,
				inputCharSet,
				false,
				false
		);
	}

	@Test
	public void testIsTerminationRequested() {
		assertFalse(inputHandler.isTerminationRequested());

		inputHandler.requestTermination();

		assertTrue(inputHandler.isTerminationRequested());
	}

	@Test
	public void testReadByte_NormalReading() throws IOException {
		byte[] testData = {65, 66, 67}; // ASCII for 'A', 'B', 'C'
		inputStream = new ByteArrayInputStream(testData);
		inputHandler.resetByteStream(inputStream);

		assertEquals(65, inputHandler.readByte());
		assertEquals(66, inputHandler.readByte());
		assertEquals(67, inputHandler.readByte());
	}

	@Test
	public void testReadByte_AfterTermination() {
		inputHandler.requestTermination();

		assertThrows(InterruptedIOException.class, () -> {
			inputHandler.readByte();
		});
	}

	@Test
	public void testReadByte_WithFakeInputFlag() throws IOException {
		// Captures existing behavior of handling nextByteIs255 and fakeInput

		Object fakeInput = new Object();
		IOException exception = assertThrows(InterruptedIOException.class, () -> {
			inputHandler.readByte(false, fakeInput);
		});
	}

	@Test
	public void testReadByte_WithNextByteIs255AndFakeInputFlag() throws IOException {
		// Captures existing behavior of handling nextByteIs255 and fakeInput

		Object fakeInput = new Object();
		assertDoesNotThrow(() -> {
			inputHandler.readByte(true, fakeInput);
		});
	}

	@Test
	public void testReadByte_EndOfStream() {
		inputStream = new ByteArrayInputStream(new byte[0]);
		inputHandler.resetByteStream(inputStream);

		assertThrows(InterruptedIOException.class, () -> {
			inputHandler.readByte();
		});
	}

	@Test
	public void testReadChar_SingleByteCharset() throws IOException {
		byte[] testData = {65, 66, 67}; // ASCII for 'A', 'B', 'C'
		inputStream = new ByteArrayInputStream(testData);
		inputHandler = new DefaultInputHandler(inputStream, StandardCharsets.US_ASCII, false, false);

		assertEquals('A', inputHandler.readChar());
		assertEquals('B', inputHandler.readChar());
		assertEquals('C', inputHandler.readChar());
	}

	// FIXME
	@Disabled("Known issue: Incorrect input processing of multibyte character input")
	@Test
	public void testReadChar_UTF8MultibyteIcon() throws IOException {
		byte[] testData = {(byte) 0xF0, (byte) 0x9F, (byte) 0x98, (byte) 0x80}; // UTF-8 for '😀' (Grinning Face emoji)
		inputStream = new ByteArrayInputStream(testData);
		inputHandler = new DefaultInputHandler(inputStream, StandardCharsets.UTF_8, false, false);

		assertEquals("😀".codePointAt(0), inputHandler.readChar());
	}

	// FIXME
	@Disabled("Known issue: Incorrect input processing of multibyte character input")
	@Test
	public void testReadChar_UTF8MultibyteChinese() throws IOException {
		byte[] testData = {(byte) 0xE4, (byte) 0xB8, (byte) 0xAD}; // UTF-8 for '中'
		inputStream = new ByteArrayInputStream(testData);
		inputHandler = new DefaultInputHandler(inputStream, StandardCharsets.UTF_8, false, false);

		assertEquals('中', inputHandler.readChar());
	}

	// FIXME
	@Disabled("Known issue: Incorrect input processing of multibyte character input")
	@Test
	public void testReadChar_UTF8MultibyteConsecutiveChinese() throws IOException {
		byte[] testData = {
				(byte) 0xE4, (byte) 0xB8, (byte) 0xAD, // 中
				(byte) 0xE5, (byte) 0x9B, (byte) 0xBD  // 国
		};
		inputStream = new ByteArrayInputStream(testData);
		inputHandler = new DefaultInputHandler(inputStream, StandardCharsets.UTF_8, false, false);

		assertEquals('中', inputHandler.readChar());
		assertEquals('国', inputHandler.readChar());
	}

	// FIXME
	@Disabled("Known issue: Incorrect input processing of multibyte character input")
	@Test
	public void testReadChar_UTF8MultibyteGermanUmlaut() throws IOException {
		byte[] testData = {
				(byte) 0xC3, (byte) 0xA4 // ä
		};
		inputStream = new ByteArrayInputStream(testData);
		inputHandler = new DefaultInputHandler(inputStream, StandardCharsets.UTF_8, false, false);

		assertEquals('ä', inputHandler.readChar());
	}

	// FIXME
	@Disabled("Known issue: Incorrect input processing of multibyte character input")
	@Test
	public void testReadChar_UTF8MultibyteConsecutiveGermanUmlauts() throws IOException {
		byte[] testData = {
				(byte) 0xC3, (byte) 0xA4, // ä
				(byte) 0xC3, (byte) 0xB6, // ö
				(byte) 0xC3, (byte) 0xBC  // ü
		};
		inputStream = new ByteArrayInputStream(testData);
		inputHandler = new DefaultInputHandler(inputStream, StandardCharsets.UTF_8, false, false);

		assertEquals('ä', inputHandler.readChar());
		assertEquals('ö', inputHandler.readChar());
		assertEquals('ü', inputHandler.readChar());
	}

	@Test
	public void testReadChar_TelnetIAC() throws IOException {
		byte[] testData = {(byte) 255, 65}; // TELNET_IAC followed by 'A'
		inputStream = new ByteArrayInputStream(testData);
		inputHandler = new DefaultInputHandler(inputStream, StandardCharsets.UTF_8, false, false);

		assertEquals(255, inputHandler.readChar());
		assertEquals('A', inputHandler.readChar());
	}

	@Test
	public void testReadChar_EscapeChar() throws IOException {
		byte[] testData = {27, 65}; // ESCAPE_CHAR followed by 'A'
		inputStream = new ByteArrayInputStream(testData);
		inputHandler = new DefaultInputHandler(inputStream, StandardCharsets.UTF_8, false, false);

		assertEquals(27, inputHandler.readChar());
		assertEquals('A', inputHandler.readChar());
	}

	@Test
	public void testReadChar_AfterTermination() {
		inputHandler.requestTermination();

		assertThrows(InterruptedIOException.class, () -> {
			inputHandler.readByte();
		});
	}

	@Test
	public void testReadChar_WithFakeInput() throws IOException {
		StringBuffer fakeInput = new StringBuffer("test");
		int result = inputHandler.readChar(false, fakeInput);
		assertEquals('t', (char) result);
		assertEquals("est", fakeInput.toString());
	}

	@Test
	public void testReadChar_WithNextByteIs255Flag() throws IOException {
		int result = assertDoesNotThrow(() -> inputHandler.readChar(true, null));
		assertEquals(255, result);
	}

	@Test
	public void testShutdown_NormalOperation() throws IOException {
		InputStream mockInputStream = Mockito.mock(InputStream.class);
		inputHandler = new DefaultInputHandler(mockInputStream, StandardCharsets.UTF_8, false, false);

		inputHandler.shutdown();

		assertTrue(inputHandler.isTerminationRequested());

		Mockito.verify(mockInputStream).close();
	}

	@Test
	public void testShutdown_ExceptionHandling() throws IOException {
		InputStream mockInputStream = Mockito.mock(InputStream.class);
		Mockito.doThrow(new IOException("Test exception")).when(mockInputStream).close();
		inputHandler = new DefaultInputHandler(mockInputStream, StandardCharsets.UTF_8, false, false);

		assertThrows(IOException.class, () -> inputHandler.shutdown());

		assertTrue(inputHandler.isTerminationRequested());
	}

	@Test
	public void testShutdown_MultipleInvocations() throws IOException {
		InputStream mockInputStream = Mockito.mock(InputStream.class);
		inputHandler = new DefaultInputHandler(mockInputStream, StandardCharsets.UTF_8, false, false);

		inputHandler.shutdown();
		inputHandler.shutdown();

		Mockito.verify(mockInputStream, Mockito.times(1)).close();
	}

}
