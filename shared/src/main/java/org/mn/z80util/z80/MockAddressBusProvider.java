package org.mn.z80util.z80;

public class MockAddressBusProvider implements AddressBusProvider {

    private byte[] memory = new byte[0x10000];

    @Override
    public byte[] getMemory() {
        return memory;
    }

    @Override
    public byte getByte(short address) {
        return memory[address & 0xffff];
    }

    @Override
    public void setByte(short address, byte value) {
        memory[address & 0xffff] = value;
    }

    @Override
    public short getWord(short address) {
        short low = (short) (getByte(address) & 0xff);
        short high = (short) (getByte((short) (address + 1)) & 0xff);
        return (short) (low | (high << 8));
    }

    @Override
    public void setWord(short address, short value) {
        byte low = (byte) (value & 0xff);
        byte high = (byte) ((value >> 8) & 0xff);
        setByte(address, low);
        setByte((short) (address + 1), high);
    }

    /**
     * @return Always FFh (tied high) as this is a mock ULA.
     */
    @Override
    public byte getIOByte(short address) {
        return (byte) 0xff;
    }

    /**
     * Does nothing as this is a mock ULA.
     */
    @Override
    public void setIOByte(short address, byte value) {
        // do nothing as this is a mock ULA.
    }
}
