package com.planet_ink.coffee_mud.session.interfaces;

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

/**
 * The AsyncModalDialogManager interface defines methods for handling modal dialogs
 * in the CoffeeMUD system using "non-blocking" input handlers.
 *
 * Note: The term "Async" in CoffeeMUD jargon refers to methods that use non-blocking input,
 * typically implemented with shorter polling intervals. This does not imply true asynchronous
 * behavior in the conventional sense of concurrent programming. The underlying implementation
 * still uses polling, governed by thread timing and socket timeouts, but allows for more
 * frequent checks and processing during input operations.
 */
public interface AsyncModalDialogManager {
	/**
	 * Starts an asynchronous modal dialog.
	 * @param callback The InputCallback to handle the dialog interaction.
	 */
	void startDialog(Session.InputCallback callback);

	/**
	 * Checks if there's currently an active dialog.
	 * @return true if a dialog is active, false otherwise.
	 */
	boolean isDialogActive();

	/**
	 * Checks if a dialog is active and is currently waiting for input.
	 * @return true if a dialog is active and waiting for input, false otherwise.
	 */
	boolean isDialogWaitingForInput();

	/**
	 * Processes the provided input for the current dialog, if any.
	 * @param input The input string to process. If null, it will check for timeouts.
	 */
	void processInput(String input);

	/**
	 * Forcibly ends the current dialog, if any.
	 */
	void cancelDialog();
}


