package org.mn.z80util.z80;

public class MockZ80 implements Z80 {

    @Override
    public void NMI() {
        // TODO Auto-generated method stub
    }

    @Override
    public void executeNextCommand() {
        // TODO Auto-generated method stub
    }

    @Override
    public byte getReg(int regno) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public short getRegPair(int regpairno) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getTStates() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void interrupt() {
        // TODO Auto-generated method stub
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub
    }

    @Override
    public void setFlag(int flag, boolean value) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setHaltState(boolean value) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setReg(int regno, byte value) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setRegPair(int regpairno, short value) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setTStates(int value) {
        // TODO Auto-generated method stub
    }
    private AddressBusProvider ula;

    @Override
    public void setUla(AddressBusProvider ula) {
        this.ula = ula;
    }

    @Override
    public boolean testFlag(int flag) {
        // TODO Auto-generated method stub
        return false;
    }
}
