package com.planet_ink.coffee_mud.Protocols;

import com.planet_ink.coffee_mud.Common.interfaces.OutputHandler;
import com.planet_ink.coffee_mud.core.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

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

public class DefaultOutputHandler implements OutputHandler {
	private final AtomicBoolean terminationRequested = new AtomicBoolean(false);
	private static final long WRITE_LOCK_TIMEOUT_MS = 10000;
	private final ReentrantLock writeLock;
	private Thread writeThread;
	private long writeStartTime;
	private long lastWriteTime;

	private OutputStream out;

	private boolean debugStrOutput;
	private boolean debugBinOutput;

	public DefaultOutputHandler(
			OutputStream out,
			ReentrantLock writeLock,
			boolean debugStrOutput,
			boolean debugBinOutput
	) {
		if (out == null) {
			throw new IllegalArgumentException("Output stream cannot be null");
		}
		if (writeLock == null) {
			throw new IllegalArgumentException("Write lock cannot be null");
		}

		this.out = out;
		this.writeLock = writeLock;
		this.debugStrOutput = debugStrOutput;
		this.debugBinOutput = debugBinOutput;
	}

	@Override
	public void requestTermination() {
		terminationRequested.compareAndSet(false, true);
	}

	@Override
	public boolean isTerminationRequested() {
		return terminationRequested.get();
	}

	@Override
	public void resetByteStream(OutputStream outputStream) {
		if (outputStream == null) {
			throw new IllegalArgumentException("Output stream cannot be null");
		}

		//TODO check termination requested

		try {
			if (this.out != null && this.out != outputStream) {
				this.out.flush();
			}
		} catch (IOException e) {
			Log.errOut("OutputHandler", "resetByteStream: Error flushing previous output stream: " + e.getMessage());
		}

		//TODO thread safety
		this.out = outputStream;
	}

	@Override
	public void rawBytesOut(byte[] bytes) throws IOException {
		if (isTerminationRequested()) {
			Log.warnOut("OutputHandler", "rawBytesOut: Termination requested, skipping write");
			return;
		}

		if (bytes == null || bytes.length == 0) {
			Log.warnOut("OutputHandler", "rawBytesOut: Attempting to write empty or null byte array");
			return;
		}

		if (out == null) {
			Log.warnOut("OutputHandler", "rawBytesOut: Output stream is not available");
			throw new IOException("Output stream is not available");
		}

		try {
			if (writeLock.tryLock(WRITE_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
				try {
					writeThread = Thread.currentThread();
					writeStartTime = System.currentTimeMillis();
					if (Log.debugChannelOn()) {
						if (debugBinOutput) {
							final StringBuilder binStr = new StringBuilder("OUTPUT (Binary): '");
							for (final byte c : bytes)
								binStr.append(String.format("%02X ", c & 0xFF));
							Log.debugOut(binStr.toString() + "'");
						}
						if (debugStrOutput) {
							final StringBuilder strStr = new StringBuilder("OUTPUT (Printable): '");
							for (final byte c : bytes)
								strStr.append(((c < 32) || (c > 127))
										? "%" + String.format("%02X", c & 0xFF)
										: ("" + (char) c));
							Log.debugOut(strStr.toString() + "'");
						}
					}
					out.write(bytes);
					out.flush();
				} finally {
					writeThread = null;
					writeStartTime = 0;
					lastWriteTime = System.currentTimeMillis();
					writeLock.unlock();
				}
			}
			else {
				Log.errOut("OutputHandler", "rawBytesOut: Could not acquire write lock within " +
						(WRITE_LOCK_TIMEOUT_MS / 1000) + "-second timeout");
				throw new IOException("Could not acquire write lock within " +
						(WRITE_LOCK_TIMEOUT_MS / 1000) + " seconds");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Log.errOut("OutputHandler", "rawBytesOut: Write operation interrupted: " + e.getMessage());
			throw new IOException("Write operation interrupted", e);
		}
	}

	@Override
	public void setDebugStr(boolean flag) {
		debugStrOutput = flag;
	}

	@Override
	public void setDebugBin(boolean flag) {
		debugBinOutput = flag;
	}
}
