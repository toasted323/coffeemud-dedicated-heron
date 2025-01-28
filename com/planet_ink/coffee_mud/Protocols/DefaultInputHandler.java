package com.planet_ink.coffee_mud.Protocols;

import com.planet_ink.coffee_mud.Common.interfaces.InputHandler;
import com.planet_ink.coffee_mud.core.Log;

import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

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
	public static final int TELNET_IAC = 255; // Interpret as Command
	public static final char ESCAPE_CHAR = '\033'; // Escape character (= 27)

	private final AtomicBoolean terminationRequested = new AtomicBoolean(false);

	private InputStream in;
	private final Reader charIn;
	private final CharacterCircularBufferInputStream charInWriter;
	private final int maxBytesPerCharIn;

	private boolean debugStrInput;
	private boolean debugBinInput;

	public DefaultInputHandler(InputStream in, Charset charset, boolean debugStrInput, boolean debugBinInput) {
		if (in == null) {
			throw new IllegalArgumentException("Input stream cannot be null");
		}
		this.in = in;
		this.maxBytesPerCharIn = (int) Math.round(Math.ceil(charset.newEncoder().maxBytesPerChar()));
		this.charInWriter = new CharacterCircularBufferInputStream(this.maxBytesPerCharIn);
		this.charIn = new BufferedReader(new InputStreamReader(this.charInWriter, charset));

		this.debugStrInput = debugStrInput;
		this.debugBinInput = debugBinInput;
	}

	@Override
	public void requestTermination() {
		terminationRequested.compareAndSet(false, true);
	}

	@Override
	public boolean isTerminationRequested() {
		return terminationRequested.get();
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
		if (isTerminationRequested()) {
			Log.warnOut("InputHandler", "readByte: Termination requested");
			throw new InterruptedIOException("Termination requested");
		}

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
	public int readChar(boolean nextByteIs255, StringBuffer fakeInput) throws IOException {
		if (nextByteIs255) {
			return (char) 255;
		}

		if (fakeInput != null && fakeInput.length() > 0) {
			char c = fakeInput.charAt(0);
			fakeInput.deleteCharAt(0);
			return c;
		}

		return readChar();
	}

	@Override
	public int readChar() throws IOException {
		if (isTerminationRequested()) {
			Log.warnOut("InputHandler", "readChar: Termination requested");
			throw new InterruptedIOException("Termination requested");
		}

		// Start by reading a single byte
		int b = readByte();

		// If charset is single-byte, return the byte as a char
		if (maxBytesPerCharIn == 1) {
			return (char) b;
		}

		// Special handling for telnet and escape characters
		if ((b == TELNET_IAC) ||
				((b & 0xff) == TELNET_IAC) ||
				(b == ESCAPE_CHAR)) {
			return (char) b;
		}

		// Write the first byte to the circular buffer
		charInWriter.write(b);

		// Attempt to read a full character
		int maxBytes = this.maxBytesPerCharIn;
		while (!isTerminationRequested() && this.charIn.ready() && this.in.available() > 0 && (--maxBytes >= 0)) {
			try {
				// Try to read a character from the buffer
				return charIn.read();
			} catch (final java.io.InterruptedIOException e) {
				// If interrupted (not enough bytes for a full character),
				// read another byte and add it to the buffer
				b = readByte();
				this.charInWriter.write(b);

				// Continue the loop to try reading again
			}
		}

		// Final attempt to read a character
		return charIn.read();
	}

	@Override
	public void setDebugStr(boolean flag) {
		debugStrInput = flag;
	}

	@Override
	public void setDebugBin(boolean flag) {
		debugBinInput = flag;
	}

	private static class CharacterCircularBufferInputStream extends InputStream {
		private final int[] bytes;
		private int start = 0;
		private int end = 0;

		protected CharacterCircularBufferInputStream(final int maxBytesPerChar) {
			bytes = new int[maxBytesPerChar + 1];
		}

		public void write(int b) {
			bytes[end] = b;
			end = (end + 1) % bytes.length;
		}

		public int read() throws IOException {
			if (start == end) {
				// Buffer is empty, throw exception to signal more input is needed
				throw new InterruptedIOException();
			}
			int b = bytes[start];
			start = (start + 1) % bytes.length;
			return b;
		}
	}
}
