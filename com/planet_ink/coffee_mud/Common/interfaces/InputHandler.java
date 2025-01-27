package com.planet_ink.coffee_mud.Common.interfaces;

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

/**
 * Interface for handling byte-level input operations.
 * Provides methods for reading bytes from input streams.
 */
public interface InputHandler {

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
	 * Resets the byte input stream to a new input source.
	 *
	 * @param inputStream the new InputStream to use for reading bytes
	 * @throws IllegalArgumentException if the input stream is null
	 */
	void resetByteStream(InputStream inputStream);

	/**
	 * Reads a single byte from the input stream with special handling options.
	 *
	 * @param nextByteIs255 If true, forces the next byte to be 255 regardless of actual input
	 * @param fakeInput An object that, if non-null, triggers an InterruptedIOException
	 * @return The byte read from the stream, or 255 if nextByteIs255 is true
	 * @throws IOException If an I/O error occurs
	 * @deprecated This method is maintained for compatibility and may be removed in future versions
	 */
	int readByte(boolean nextByteIs255, Object fakeInput) throws IOException;

	/**
	 * Reads a single byte from the input stream.
	 * <p>This method is designed to read one byte at a time from the input stream.
	 * Unlike typical {@link InputStream#read()} behavior, this method does not return
	 * -1 to indicate the end of the stream. Instead, it throws an {@link InterruptedIOException}.
	 * This behavior is consistent with legacy implementations but differs from standard
	 * expectations for input stream reading.</p>
	 *
	 * <p><b>Compatibility Note:</b> This method currently maintains compatibility with
	 * legacy implementations by converting EOF (-1) into an {@link InterruptedIOException}.
	 * However, future versions may align with standard input stream behavior by returning
	 * -1 for EOF instead of throwing an exception.</p>
	 *
	 * @return The byte read from the stream
	 * @throws IOException If an I/O error occurs
	 * @throws InterruptedIOException If the end of the stream is reached or termination is requested.
	 */
	int readByte() throws IOException;

	/**
	 * Configures debug flag for string input logging.
	 *
	 * @param flag enable or disable string input debugging
	 */
	void setDebugStr(boolean flag);

	/**
	 * Configures debug flag for binary input logging.
	 *
	 * @param flag enable or disable binary input debugging
	 */
	void setDebugBin(boolean flag);
}
