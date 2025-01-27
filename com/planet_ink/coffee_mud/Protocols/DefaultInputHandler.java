package com.planet_ink.coffee_mud.Protocols;

import com.planet_ink.coffee_mud.Common.interfaces.InputHandler;
import com.planet_ink.coffee_mud.core.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

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

public class DefaultInputHandler implements InputHandler {
	private InputStream in;
	private boolean debugStrInput;
	private boolean debugBinInput;

	public DefaultInputHandler(InputStream in, boolean debugStrInput, boolean debugBinInput) {
		if (in == null) {
			throw new IllegalArgumentException("Input stream cannot be null");
		}
		this.in = in;
		this.debugStrInput = debugStrInput;
		this.debugBinInput = debugBinInput;
	}

	private boolean isByteStreamAvailable() {
		try {
			return in.available() >= 0;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public void resetByteStream(InputStream inputStream) {
		if (inputStream == null) {
			throw new IllegalArgumentException("Input stream cannot be null");
		}
		this.in = inputStream;
	}

	@Override
	public int readByte(boolean nextByteIs255, Object fakeInput) throws IOException {
		if (nextByteIs255) {
			return 255;
		}
		if (fakeInput != null) {
			throw new InterruptedIOException(".");
		}
		return readByte();
	}

	@Override
	public int readByte() throws IOException {
		if (!isByteStreamAvailable()) {
			Log.warnOut("InputHandler", "readByte: Input stream is not available");
			throw new IOException("Input stream is not available");
		}

		int read;
		try {
			read = in.read();
		} catch (IOException e) {
			Log.errOut("InputHandler", "readByte: Error reading byte: " + e.getMessage());
			throw e;
		}

		// Original implementation turned -1 into an exception
		if (read == -1) {
			Log.warnOut("InputHandler", "readByte: End of input stream");
			throw new InterruptedIOException("End of input stream");
		}
		// NOTE: Original callers of readByte expect a return value of -1 to indicate EOF.
		// However, staying consistent with the original implementation we convert -1 into an InterruptedIOException.
		//
		// This is maintaining the original expectation gap between callers and the implementation readByte.

		if (Log.debugChannelOn() && debugBinInput) {
			// TODO realign with original implementation, i.e. append to 'debugBinInputBuf' and log buffer via tick
			Log.debugOut("INPUT (Binary): '" + (read & 0xFF) + "'");
		}

		return read;
	}

	@Override
	public void setDebugStr(boolean flag) {
		debugStrInput = flag;
	}

	@Override
	public void setDebugBin(boolean flag) {
		debugBinInput = flag;
	}
}
