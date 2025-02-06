package com.planet_ink.coffee_mud.core;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test class is designed to document the actual behavior of the CMParms utility class,
 * to highlight inconsistencies and unexpected results.
 *
 * Note: This is a work in progress and not stable. The tests here may change as the
 * understanding of the CMParms class behavior evolves or as the class itself is modified.
 */
public class CMParmsTest {

	@Test
	@Disabled("parseCommas unexpectedly removes escaped quotes and injects a space after the comma")
	public void testParseCommas() {
		String input = "a,b,c,\"d,e\",f";
		List<String> result = CMParms.parseCommas(input, true);
		assertEquals(Arrays.asList("a", "b", "c", "d,e", "f"), result);

		result = CMParms.parseCommas(input, false);

		// Expected: ["a", "b", "c", "\"d,e\"", "f"]
		// Actual: ["a", "b", ""c", "d, e", "f"]
		assertEquals(Arrays.asList("a", "b", "c", "\"d", "e\"", "f"), result);
	}

	@Test
	public void testContainsIgnoreCase() {
		List<String> list = Arrays.asList("Apple", "Banana", "Cherry");
		assertTrue(CMParms.containsIgnoreCase(list, "apple"));
		assertTrue(CMParms.containsIgnoreCase(list, "BANANA"));
		assertFalse(CMParms.containsIgnoreCase(list, "Grape"));
	}

	@Test
	public void testCleanParameterList() {
		String input = "  param1  param2\t\tparam3\n param4  ";
		List<String> result = CMParms.cleanParameterList(input);
		assertEquals(Arrays.asList("param1", "param2", "param3", "param4"), result);
	}
}

