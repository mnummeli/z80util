package org.mn.z80util.disassembler;

import junit.framework.*;

public class HexTest extends TestCase {
	public void testIntToHex() {
		assertEquals("0401",Hex.intToHex4(1025));
		assertEquals("2000",Hex.intToHex4(8192));
		assertEquals("BFFF",Hex.intToHex4(49151));
	}
}
