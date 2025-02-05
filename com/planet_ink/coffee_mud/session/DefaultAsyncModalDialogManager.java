package com.planet_ink.coffee_mud.session;

import com.planet_ink.coffee_mud.Common.interfaces.Session;
import com.planet_ink.coffee_mud.core.CMLib;
import com.planet_ink.coffee_mud.core.Log;
import com.planet_ink.coffee_mud.session.interfaces.AsyncModalDialogManager;

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

public class DefaultAsyncModalDialogManager implements AsyncModalDialogManager {
	private volatile Session.InputCallback activeCallback;
	private final String groupName;
	private final String objectId;

	public DefaultAsyncModalDialogManager(String groupName) {
		this.groupName = groupName;
		this.objectId = Integer.toHexString(System.identityHashCode(this));
		Log.infoOut("AsyncModalDialog", "constructor: " + objectId + ": Created new AsyncModalDialog");
	}

	@Override
	public void startDialog(Session.InputCallback callback) {
		if (callback != null) {
			if (this.activeCallback != null) {
				Log.infoOut("AsyncModalDialog", "startDialog: " + objectId + ": Timing out previous callback");
				this.activeCallback.timedOut();
			}
			this.activeCallback = callback;
			Log.infoOut("AsyncModalDialog", "startDialog: " + objectId + ": Starting new dialog");
			callback.showPrompt();
		}
	}

	@Override
	public boolean isDialogActive() {
		return activeCallback != null;
	}

	@Override
	public boolean isDialogWaitingForInput() {
		return activeCallback != null && activeCallback.waitForInput();
	}

	@Override
	public void processInput(String input) {
		if (!isDialogActive()) {
			return;
		}

		Log.debugOut("AsyncModalDialog", "processInput: " + objectId + ": Checking timeout");
		boolean timedOut = activeCallback.isTimedOut();

		if (timedOut) {
			Log.debugOut("AsyncModalDialog", "checkTimeout: " + objectId + ": Dialog timed out");
			activeCallback.timedOut();

			Log.debugOut("AsyncModalDialog", "processInput: " + objectId + ": Check if still waiting for input");
			if (!isDialogWaitingForInput()) {
				cancelDialog();
			}
			return;
		}

		if (input != null && activeCallback != null) {
			Log.debugOut("AsyncModalDialog", "processInput: " + objectId + ": Processing input: " + input);
			activeCallback.setInput(input);
			if (!activeCallback.waitForInput()) {
				Log.debugOut("AsyncModalDialog", "processInput: " + objectId + ": Executing callback");
				executeCallback();
			}
		}
	}

	private void executeCallback() {
		CMLib.threads().executeRunnable(groupName, new Runnable() {
			@Override
			public void run() {
				try {
					activeCallback.callBack();
				} catch (Throwable t) {
					Log.errOut("AsyncModalDialog", "executeCallback: " + objectId + ": Error in callback execution: " + t.getMessage());
				} finally {
					Log.debugOut("AsyncModalDialog", "executeCallback: " + objectId + ": Callback execution completed");
					if (!isDialogWaitingForInput()) {
						cancelDialog();
					}
				}
			}
		});
	}

	@Override
	public void cancelDialog() {
		if (activeCallback != null) {
			Log.infoOut("AsyncModalDialog", "cancelDialog: " + objectId + ": Cancelling dialog");
			activeCallback.timedOut();
			activeCallback = null;
		}
	}
}



