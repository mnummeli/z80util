package org.mn.z80util.z80;

import org.mn.z80util.z80.qaop.*;
import org.mn.z80util.z80.yaze.*;
import junit.framework.*;

public class MultiplyTest extends TestCase {
	private AddressBusProvider ula;
	private Z80 qaopImpl, yazeImpl;

	public void setUp() {
		ula=new MockAddressBusProvider();
		loadMultiplyProgramIntoMemory(ula);
		qaopImpl=new QaopZ80Impl();
		qaopImpl.setUla(ula);
		yazeImpl=new YazeBasedZ80Impl();
		yazeImpl.setUla(ula);
	}

	private void loadMultiplyProgramIntoMemory(AddressBusProvider ula) {
		// do nothing
	}

	public void testQaopMultiply() {
		qaopImpl.reset();
		assertTrue(true);
	}

	public void testYazeMultiply() {
		yazeImpl.reset();
		assertFalse(false);
	}
}
