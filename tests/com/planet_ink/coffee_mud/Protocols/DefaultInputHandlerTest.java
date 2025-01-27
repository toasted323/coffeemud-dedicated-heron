package com.planet_ink.coffee_mud.Protocols;

import com.planet_ink.coffee_mud.core.Log;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
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
		inputStream = new ByteArrayInputStream(new byte[]{});
		inputHandler = new DefaultInputHandler(
			inputStream,
			false,
			false
		);
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


}
