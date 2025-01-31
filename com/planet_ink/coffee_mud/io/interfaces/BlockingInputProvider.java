package com.planet_ink.coffee_mud.io.interfaces;

import java.io.IOException;

public interface BlockingInputProvider {
	String blockingIn(long maxTime, boolean filter) throws IOException;
}
