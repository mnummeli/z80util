package org.mn.z80util.z80;

public interface AddressBusProvider {
	byte[] getMemory();
	void setByte(short address, byte value);
	byte getByte(short address);
	void setWord(short address, short value);
	short getWord(short address);
	void setIOByte(short address, byte value);
	byte getIOByte(short address);
}
