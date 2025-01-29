package com.planet_ink.coffee_mud.Protocols;

import com.planet_ink.coffee_mud.Common.interfaces.OutputHandler;
import com.planet_ink.coffee_mud.core.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
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
	private Charset outputCharset;
	private PrintWriter charOut;

	private boolean debugStrOutput;
	private boolean debugBinOutput;

	public DefaultOutputHandler(
			OutputStream out,
			Charset outputCharSet,
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

		this.writeLock = writeLock;

		this.out = out;
		this.outputCharset = outputCharSet;
		this.charOut =  new PrintWriter(new OutputStreamWriter(out, outputCharSet));

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
		this.charOut = new PrintWriter(new OutputStreamWriter(outputStream, this.outputCharset));
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
					//TODO add method name to debug logging
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
	public void rawCharsOut(char[] chars) throws IOException {
		if (isTerminationRequested()) {
			Log.warnOut("OutputHandler", "rawCharsOut: Termination requested, skipping write");
			return;
		}

		if (chars == null || chars.length == 0) {
			Log.warnOut("OutputHandler", "rawCharsOut: Attempting to write empty or null chars array");
			return;
		}

		if (charOut == null) {
			Log.warnOut("OutputHandler", "rawCharsOut: Output stream is not available");
			throw new IOException("Output stream is not available");
		}

		try {
			if (writeLock.tryLock(WRITE_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
				try {
					if (Log.debugChannelOn()) {
						if (debugBinOutput) {
							final StringBuilder binStr = new StringBuilder("rawCharsOut: OUTPUT (Binary): '");
							for (final char c : chars)
								binStr.append((c & 0xff)).append(" ");
							Log.debugOut(binStr.toString() + "'");
						}
						if (debugStrOutput) {
							final StringBuilder strStr = new StringBuilder("rawCharsOut: OUTPUT (Printable): '");
							for (final char c : chars)
								strStr.append(((c < 32) || (c > 127))
										? "%" + String.format("%02X", (int) c)
										: ("" + c));
							Log.debugOut(strStr.toString() + "'");
						}
					}

					charOut.write(chars);
					// Note: checkError() implicitly flushes the stream
					if (charOut.checkError()) {
						Log.errOut("OutputHandler", "rawCharsOut: Error detected when writing to charOut");
						throw new IOException("Error detected when writing to charOut");
					}
				} finally {
					writeThread=null;
					writeStartTime=0;
					lastWriteTime=System.currentTimeMillis();
					writeLock.unlock();
				}
			}
			else {
				Log.errOut("OutputHandler", "rawCharsOut: Could not acquire write lock within " +
						(WRITE_LOCK_TIMEOUT_MS / 1000) + "-second timeout");
				throw new IOException("Could not acquire write lock within " +
						(WRITE_LOCK_TIMEOUT_MS / 1000) + " seconds");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Log.errOut("OutputHandler", "rawCharsOut: Write operation interrupted: " + e.getMessage());
			throw new IOException("Write operation interrupted", e);
		}
	}

	@Override
	public long getWriteStartTime() {
		return writeStartTime;
	}

	@Override
	public void shutdown() throws IOException {
		List<IOException> exceptions = new ArrayList<>();

		requestTermination();

		if (out != null) {
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				exceptions.add(e);
				Log.errOut("OutputHandler", "shutdown: Error while closing output stream: " + e.getMessage());
			} finally {
				out = null;
				charOut = null;
			}
		}
		// Closing the underlying output stream automatically closes charOut.

		if (writeLock.isHeldByCurrentThread()) {
			try {
				writeLock.unlock();
			} catch (IllegalMonitorStateException e) {
				IOException ioException = new IOException("Error while releasing write lock: " + e.getMessage(), e);
				exceptions.add(ioException);
				Log.errOut("OutputHandler", "shutdown: " + ioException.getMessage());
			}
		}

		writeThread = null;
		writeStartTime = 0;
		lastWriteTime = System.currentTimeMillis();

		if (!exceptions.isEmpty()) {
			StringBuilder errorMessage = new StringBuilder("Errors occurred during shutdown:");
			for (IOException e : exceptions) {
				errorMessage.append("\n").append(e.getMessage());
			}
			throw new IOException(errorMessage.toString(), exceptions.get(0));
		}
	}

	@Override
	public long getLastWriteTime() {
		return lastWriteTime;
	}

	@Override
	public boolean isLockedUpWriting() {
		final long time = writeStartTime;
		if (time == 0)
			return false;
		return ((System.currentTimeMillis() - time) > WRITE_LOCK_TIMEOUT_MS);
	}

	@Override
	public void resetLastWriteTime() {
		lastWriteTime = System.currentTimeMillis();
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
