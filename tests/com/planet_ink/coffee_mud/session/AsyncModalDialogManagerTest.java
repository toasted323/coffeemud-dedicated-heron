package com.planet_ink.coffee_mud.session;

import com.planet_ink.coffee_mud.Libraries.interfaces.ThreadEngine;
import com.planet_ink.coffee_mud.core.CMLib;
import com.planet_ink.coffee_mud.core.Log;
import com.planet_ink.coffee_mud.session.interfaces.AsyncModalDialogManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.planet_ink.coffee_mud.Common.interfaces.Session;

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


class AsyncModalDialogManagerTest {

	private AsyncModalDialogManager dialogManager;

	private static MockedStatic<CMLib> cmLibMockedStatic;

	@Mock
	private static ThreadEngine mockThreadEngine;

	@Mock
	private Session.InputCallback mockCallback;

	@BeforeAll
	static void setupMocks() {
		MockitoAnnotations.openMocks(AsyncModalDialogManagerTest.class);

		// Log
		MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class);
		Log mockLog = Mockito.mock(Log.class);
		logMockedStatic.when(Log::instance).thenReturn(mockLog);
		logMockedStatic.when(Log::debugChannelOn).thenReturn(false);

		// CMLib
		cmLibMockedStatic = Mockito.mockStatic(CMLib.class);
		cmLibMockedStatic.when(CMLib::threads).thenReturn(mockThreadEngine);
	}


	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		dialogManager = new DefaultAsyncModalDialogManager("testGroup");
		reset(mockThreadEngine);
		cmLibMockedStatic.reset();
		cmLibMockedStatic.when(CMLib::threads).thenReturn(mockThreadEngine);
	}

	@AfterAll
	static void tearDownMocks() {
		Log.instance();

		if (cmLibMockedStatic != null) {
			cmLibMockedStatic.close();
		}
		Mockito.clearAllCaches();
	}

	@Test
	void testStartDialog() {
		dialogManager.startDialog(mockCallback);
		assertTrue(dialogManager.isDialogActive());
		verify(mockCallback).showPrompt();
	}

	@Test
	void testCancelDialog() {
		dialogManager.startDialog(mockCallback);
		assertTrue(dialogManager.isDialogActive());
		dialogManager.cancelDialog();
		assertFalse(dialogManager.isDialogActive());
	}

	@Test
	void testProcessInput() {
		doAnswer(invocation -> {
			Runnable runnable = invocation.getArgument(1);
			runnable.run();
			return null;
		}).when(mockThreadEngine).executeRunnable(anyString(), any(Runnable.class));

		when(mockCallback.waitForInput()).thenReturn(false);

		dialogManager.startDialog(mockCallback);
		assertTrue(dialogManager.isDialogActive(), "Dialog should be active after starting");

		dialogManager.processInput("test input");

		verify(mockCallback).setInput("test input");
		verify(mockCallback).callBack();
		verify(mockThreadEngine).executeRunnable(eq("testGroup"), any(Runnable.class));

		assertFalse(dialogManager.isDialogActive(), "Dialog should not be active after processing input");
	}

	@Test
	void testIsDialogActive() {
		assertFalse(dialogManager.isDialogActive());
		dialogManager.startDialog(mockCallback);
		assertTrue(dialogManager.isDialogActive());
	}
}
