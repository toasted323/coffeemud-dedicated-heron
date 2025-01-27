package com.planet_ink.coffee_mud.Protocols;

import com.planet_ink.coffee_mud.core.Log;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
	public void testRawBytesOut() throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock writeLock = new ReentrantLock();
		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
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
	public void testRawBytesOut_WriteLockTimeout() throws InterruptedException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock mockLock = Mockito.mock(ReentrantLock.class);
		when(mockLock.tryLock(10000, TimeUnit.MILLISECONDS)).thenReturn(false);

		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
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
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ReentrantLock mockLock = Mockito.mock(ReentrantLock.class);
		when(mockLock.tryLock(10000, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException("Interrupted"));

		DefaultOutputHandler outputHandler = new DefaultOutputHandler(
				outputStream,
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
}
