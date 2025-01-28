package com.planet_ink.coffee_mud.Common.interfaces;

import java.io.IOException;
import java.io.OutputStream;

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
 * Interface for handling byte-level output operations.
 * Provides methods for writing raw byte arrays to output streams.
 */
public interface OutputHandler {

	/**
	 * Signals a one-way, non-blocking termination request.
	 * Idempotent method that can be called multiple times safely.
	 */
	void requestTermination();

	/**
	 * Checks if termination has been requested.
	 *
	 * @return boolean indicating termination status
	 */
	boolean isTerminationRequested();

	/**
	 * Resets the byte output stream to a new output destination.
	 *
	 * @param outputStream the new OutputStream to use for writing bytes
	 * @throws IllegalArgumentException if the output stream is null
	 */
	void resetByteStream(OutputStream outputStream);

	/**
	 * Writes a raw byte array to the output stream.
	 *
	 * @param bytes The byte array to be written
	 * @throws IOException If an I/O error occurs
	 */
	void rawBytesOut(byte[] bytes) throws IOException;

	/**
	 * Writes characters to the output writer.
	 *
	 * @param chars the characters to write
	 * @throws IOException if an I/O error occurs during writing
	 */
	void rawCharsOut(char[] chars) throws IOException;

	/**
	 * Gets the time when the last write operation started.
	 *
	 * @return the timestamp of the last write start time in milliseconds
	 */
	long getWriteStartTime();

	/**
	 * Gets the time when the last write operation completed.
	 *
	 * @return the timestamp of the last write completion time in milliseconds
	 */
	long getLastWriteTime();

	/**
	 * Resets the last write time to the current system time.
	 */
	void resetLastWriteTime();

	/**
	 * Checks if the output stream is locked up in a write operation.
	 *
	 * @return true if the stream has been writing for more than 10 seconds, false otherwise
	 */
	boolean isLockedUpWriting();

	/**
	 * Configures debug flag for string output logging.
	 *
	 * @param flag enable or disable string output debugging
	 */
	void setDebugStr(boolean flag);

	/**
	 * Configures debug flag for binary output logging.
	 *
	 * @param flag enable or disable binary output debugging
	 */
	void setDebugBin(boolean flag);
}
