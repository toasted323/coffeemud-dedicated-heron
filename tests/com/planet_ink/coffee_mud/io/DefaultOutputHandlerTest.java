package com.planet_ink.coffee_mud.io;

import com.planet_ink.coffee_mud.core.Log;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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

public class DefaultOutputHandlerTest {
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

	@Test
	public void testIsTerminationRequested() {
		final Charset outputCharSet = StandardCharsets.UTF_8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock writeLock = new ReentrantLock();
		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				writeLock,
				false,
				false
		);

		assertFalse(outputHandler.isTerminationRequested());

		outputHandler.requestTermination();

		assertTrue(outputHandler.isTerminationRequested());
	}

	@Test
	public void testRawBytesOut() throws IOException {
		final Charset outputCharSet = StandardCharsets.UTF_8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock writeLock = new ReentrantLock();
		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				writeLock,
				false,
				false
		);

		byte[] bytesToSend = {65, 66, 67}; // ASCII for 'A', 'B', 'C'
		outputHandler.rawBytesOut(bytesToSend);

		byte[] writtenBytes = outputStream.toByteArray();
		assertArrayEquals(bytesToSend, writtenBytes);
	}

	@Test
	public void testRawCharsOut_SingleByteCharset() throws IOException {
		final Charset outputCharSet = StandardCharsets.US_ASCII;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock writeLock = new ReentrantLock();
		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				writeLock,
				false,
				false
		);

		char[] charsToSend = {'A', 'B', 'C'};
		outputHandler.rawCharsOut(charsToSend);

		byte[] writtenBytes = outputStream.toByteArray();
		assertArrayEquals(new byte[]{65, 66, 67}, writtenBytes);
	}

	@Test
	public void testRawBytesOut_AfterTermination() {
		final Charset outputCharSet = StandardCharsets.UTF_8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock writeLock = new ReentrantLock();
		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				writeLock,
				false,
				false
		);

		outputHandler.requestTermination();

		byte[] testBytes = {1, 2, 3};
		assertDoesNotThrow(() -> {
			outputHandler.rawBytesOut(testBytes);
		});
	}

	@Test
	public void testRawBytesOut_WriteLockTimeout() throws InterruptedException {
		final Charset outputCharSet = StandardCharsets.UTF_8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock mockLock = Mockito.mock(ReentrantLock.class);
		when(mockLock.tryLock(10000, TimeUnit.MILLISECONDS)).thenReturn(false);

		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				mockLock,
				false,
				false
		);

		byte[] bytesToSend = {65, 66, 67};

		IOException exception = assertThrows(IOException.class, () -> {
			outputHandler.rawBytesOut(bytesToSend);
		});

		assertEquals("Could not acquire write lock within 10 seconds", exception.getMessage());
	}

	@Test
	public void testRawBytesOut_InterruptedExceptionHandling() throws IOException, InterruptedException {
		final Charset outputCharSet = StandardCharsets.UTF_8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock mockLock = Mockito.mock(ReentrantLock.class);
		when(mockLock.tryLock(10000, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException("Interrupted"));

		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				mockLock,
				false,
				false
		);

		byte[] bytesToSend = {65, 66, 67};

		IOException exception = assertThrows(IOException.class, () -> {
			outputHandler.rawBytesOut(bytesToSend);
		});

		assertEquals("Write operation interrupted", exception.getMessage());
	}

	@Test
	public void testRawCharsOut() throws IOException {
		final Charset outputCharSet = StandardCharsets.UTF_8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock writeLock = new ReentrantLock();
		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				writeLock,
				false,
				false
		);

		char[] charsToSend = {'A', 'B', 'C'};
		outputHandler.rawCharsOut(charsToSend);

		String writtenString = outputStream.toString(outputCharSet.name());
		assertEquals("ABC", writtenString);
	}

	@Test
	public void testRawCharsOut_UTF8MultibyteIcon() throws IOException {
		final Charset outputCharSet = StandardCharsets.UTF_8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock writeLock = new ReentrantLock();
		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				writeLock,
				false,
				false
		);

		String emojiString = "😀"; // Grinning Face emoji
		outputHandler.rawCharsOut(emojiString.toCharArray());

		String writtenString = outputStream.toString(outputCharSet.name());
		assertEquals(emojiString, writtenString);
	}


	@Test
	public void testRawCharsOut_UTF8MultibyteChinese() throws IOException {
		final Charset outputCharSet = StandardCharsets.UTF_8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock writeLock = new ReentrantLock();
		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				writeLock,
				false,
				false
		);

		char[] charsToSend = {'中'};
		outputHandler.rawCharsOut(charsToSend);

		String writtenString = outputStream.toString(outputCharSet.name());
		assertEquals("中", writtenString);
	}

	@Test
	public void testRawCharsOut_UTF8MultibyteConsecutiveChinese() throws IOException {
		final Charset outputCharSet = StandardCharsets.UTF_8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock writeLock = new ReentrantLock();
		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				writeLock,
				false,
				false
		);

		char[] charsToSend = {'中', '国'};
		outputHandler.rawCharsOut(charsToSend);

		String writtenString = outputStream.toString(outputCharSet.name());
		assertEquals("中国", writtenString);
	}

	@Test
	public void testRawCharsOut_UTF8MultibyteConsecutiveGermanUmlauts() throws IOException {
		final Charset outputCharSet = StandardCharsets.UTF_8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock writeLock = new ReentrantLock();
		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				writeLock,
				false,
				false
		);

		char[] charsToSend = {'ä', 'ö', 'ü'};
		outputHandler.rawCharsOut(charsToSend);

		String writtenString = outputStream.toString(outputCharSet.name());
		assertEquals("äöü", writtenString);
	}

	@Test
	public void testRawCharsOut_AfterTermination() {
		final Charset outputCharSet = StandardCharsets.UTF_8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock writeLock = new ReentrantLock();
		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				writeLock,
				false,
				false
		);

		outputHandler.requestTermination();

		char[] testChars = {'X', 'Y', 'Z'};
		assertDoesNotThrow(() -> {
			outputHandler.rawCharsOut(testChars);
		});
	}

	@Test
	public void testRawCharsOut_WriteLockTimeout() throws InterruptedException {
		final Charset outputCharSet = StandardCharsets.UTF_8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock mockLock = Mockito.mock(ReentrantLock.class);
		when(mockLock.tryLock(10000, TimeUnit.MILLISECONDS)).thenReturn(false);

		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				mockLock,
				false,
				false
		);

		char[] charsToSend = {'A', 'B', 'C'};

		IOException exception = assertThrows(IOException.class, () -> {
			outputHandler.rawCharsOut(charsToSend);
		});

		assertEquals("Could not acquire write lock within 10 seconds", exception.getMessage());
	}

	@Test
	public void testRawCharsOut_InterruptedExceptionHandling() throws IOException, InterruptedException {
		final Charset outputCharSet = StandardCharsets.UTF_8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock mockLock = Mockito.mock(ReentrantLock.class);
		when(mockLock.tryLock(10000, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException("Interrupted"));

		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
				outputCharSet,
				mockLock,
				false,
				false
		);

		char[] charsToSend = {'A', 'B', 'C'};

		IOException exception = assertThrows(IOException.class, () -> {
			outputHandler.rawCharsOut(charsToSend);
		});

		assertEquals("Write operation interrupted", exception.getMessage());
	}

	@Test
	public void testShutdown_NormalOperation() throws IOException {
		OutputStream mockOutputStream = Mockito.mock(OutputStream.class);
		PrintWriter mockPrintWriter = Mockito.mock(PrintWriter.class);
		ReentrantLock mockLock = Mockito.mock(ReentrantLock.class);

		DefaultOutputHandler handler = new DefaultOutputHandler(mockOutputStream, StandardCharsets.UTF_8, mockLock, false, false);
		handler.shutdown();

		assertTrue(handler.isTerminationRequested());
		Mockito.verify(mockOutputStream).close();
		Mockito.verify(mockLock).isHeldByCurrentThread();
	}

	@Test
	public void testShutdown_ExceptionHandling() throws IOException {
		OutputStream mockOutputStream = Mockito.mock(OutputStream.class);
		PrintWriter mockPrintWriter = Mockito.mock(PrintWriter.class);
		ReentrantLock mockLock = Mockito.mock(ReentrantLock.class);

		Mockito.doThrow(new IOException("Test exception")).when(mockOutputStream).close();

		DefaultOutputHandler handler = new DefaultOutputHandler(mockOutputStream, StandardCharsets.UTF_8, mockLock, false, false);

		IOException exception = assertThrows(IOException.class, () -> handler.shutdown());
		assertTrue(exception.getMessage().contains("Test exception"));
		assertTrue(handler.isTerminationRequested());
	}

	@Test
	public void testShutdown_WriteLockHandling() throws IOException {
		OutputStream mockOutputStream = Mockito.mock(OutputStream.class);
		PrintWriter mockPrintWriter = Mockito.mock(PrintWriter.class);
		ReentrantLock mockLock = Mockito.mock(ReentrantLock.class);

		Mockito.when(mockLock.isHeldByCurrentThread()).thenReturn(true);
		Mockito.doThrow(new IllegalMonitorStateException("Test exception")).when(mockLock).unlock();

		DefaultOutputHandler handler = new DefaultOutputHandler(mockOutputStream, StandardCharsets.UTF_8, mockLock, false, false);

		IOException exception = assertThrows(IOException.class, () -> handler.shutdown());
		assertTrue(exception.getMessage().contains("Error while releasing write lock"));
		assertTrue(handler.isTerminationRequested());
	}
}
