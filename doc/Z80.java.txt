/*
 * Z80.java - Z80 processor interface.
 * 
 * (C) 2009, Mikko Nummelin <mikko.nummelin@tkk.fi>
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package org.mn.z80util.z80;

public interface Z80 {

	/*
	 * Register pair indices. Register pair getters and setters must
	 * conform to these.
	 */

	/**
	 * Byte counter.
	 */
	public static final int BC=0;

	/**
	 * Destination.
	 */
	public static final int DE=1;

	/**
	 * High-low.
	 */
	public static final int HL=2;

	/**
	 * Accumulator-flags.
	 */
	public static final int AF=3;

	/**
	 * Swap register pair of byte counter.
	 */
	public static final int BC_ALT=4;

	/**
	 * Swap register pair of destination.
	 */
	public static final int DE_ALT=5;

	/** 
	 * Swap register pair of high-low.
	 */
	public static final int HL_ALT=6;

	/** 
	 * Swap register pair of accumulator-flags.
	 */
	public static final int AF_ALT=7;

	/**
	 * Normal index register pair.
	 */
	public static final int IX=8;

	/**
	 * System index register pair.
	 */
	public static final int IY=9;

	/**
	 * Stack pointer.
	 */
	public static final int SP=10;

	/**
	 * Program counter.
	 */
	public static final int PC=11;

	/**
	 * Interrupt-refresh register pair. If interrupt mode is 2, then I is used
	 * as higher byte when determining the interrupt vector. R has usually no
	 * significant meaning in emulators.
	 */
	public static final int IR=12;

	/*
	 * Register indices. Register getters and setters must conform to these.
	 * Note that they are essentially in the same order as their register pair
	 * counterparts, only exception is AF where A, the higher byte, is listed
	 * after flag register F, the lower byte.
	 */
	public static final int B=0;
	public static final int C=1;
	public static final int D=2;
	public static final int E=3;
	public static final int H=4;
	public static final int L=5;
	public static final int F=6;
	public static final int A=7;
	public static final int B_ALT=8;
	public static final int C_ALT=9;
	public static final int D_ALT=10;
	public static final int E_ALT=11;
	public static final int H_ALT=12;
	public static final int L_ALT=13;
	public static final int F_ALT=14;
	public static final int A_ALT=15;
	public static final int XH=16;
	public static final int XL=17;
	public static final int YH=18;
	public static final int YL=19;
	public static final int SPH=20;
	public static final int SPL=21;
	public static final int PCH=22;
	public static final int PCL=23;
	public static final int I=24;
	public static final int R=25;
	
	/**
	 * Interrupt related extra flags. Explanation of bytes:
	 * 
	 * 0: IFF1 (interrupts enabled ?)
	 * 1: IFF2 (backup of IFF1 in NMI)
	 * 2-3: Interrupt mode: 0, 1 or 2
	 */
	public static final int IM_IFF=26;

	/*
	 * Flag masks in F register.
	 */

	/**
	 * Carry flag. Usually set in a byte unsigned overflows, for example
	 * <code>85h+76h=FBh</code> does not set this, but
	 * <code>10h-20h=-10h=F0h</code> or <code>c0h+55h=15h</code> do.
	 */
	public static final int CF  = 0x01;

	/**
	 * Add-subtract flag. Usually set when addition and increasing commands
	 * are performed and reset when subtraction, decreasing or comparison
	 * commands are performed.
	 */
	public static final int NF  = 0x02;

	/**
	 * Parity-oVerflow-flag. In addition and subtraction this is set in
	 * signed overflows, for example when two positive (7-bit 0) numbers
	 * are added to produce a negative (7-bit 1) number. In logical
	 * commands this is set if the number of 1-bits in the result is even.
	 */
	public static final int PVF = 0x04;

	/**
	 * An unofficial flag usually holding a copy of 3-bit of the result.
	 */
	public static final int B3F = 0x08;

	/**
	 * This flag is set if there is an unsigned overflow from 3-bit in
	 * 8-bit arithmetic operations and from 11-bit in 16-bit arithmetic.
	 * It is used only in DAA command when decimal adjusting results of
	 * additions and subtractions.
	 */
	public static final int HF  = 0x10;

	/**
	 * An unofficial flag usually holding a copy of 3-bit of the result.
	 */
	public static final int B5F = 0x20;

	/**
	 * This flag is set if the previous arithmetic or logical operation
	 * results in zero or if a comparison by CP, CPI or CPIR succeeds.
	 */
	public static final int ZF  = 0x40;

	/**
	 * An <i>official</i> flag usually holding a copy of 7-bit of the result,
	 * which conforms to the sign bit in two's complement system. Used mostly
	 * in <code>JP PO</code>, <code>JP PE</code> etc. commands.
	 */
	public static final int SF  = 0x80;

	/**
	 * Sets the ULA unit, could be used by Spring Framework. Note that as this
	 * interface does not require a memory nor peripherals, those should be
	 * implemented inside the ULA.
	 * 
	 * @param ula	The new ULA associated with this Z80 processor.
	 */
	public void setUla(AddressBusProvider ula);

	/**
	 * Executes next Z80 command and updates PC register accordingly.
	 */
	public void executeNextCommand();

	/**
	 * Resets the processor, fills registers with random, except PC to 0000h.
	 */
	public void reset();

	/**
	 * Signals a maskable interrupt.
	 */
	public void interrupt();

	/**
	 * Signals a non-maskable interrupt. The following procedure should be
	 * executed:
	 * 
	 * <code>
	 * iff2 := iff1;
	 * iff1 := false;
	 * PUSH(PC);
	 * PC := 0x0066;
	 * </code>
	 * 
	 * When RETN possibly occurs later in the program, the following procedure
	 * should be executed:
	 * 
	 * <code>
	 * iff1 := iff2;
	 * POP(PC);
	 * </code>
	 */
	public void NMI();

	/**
	 * Sets register a value.
	 * @param regno	Number of register affected.
	 * @param value	New byte value for register affected.
	 */
	public void setReg(int regno, byte value);

	/**
	 * Gets value of a register
	 * @param regno	Number of register affected.
	 * @return		Byte value of register.
	 */
	public byte getReg(int regno);

	/**
	 * Sets register pair a value. Note that AF and AF_ALT register pairs are
	 * special, as their register indices are big-endian while other register
	 * pairs are little-endian.
	 * 
	 * @param regpairno	Number of register pair affected.
	 * @param value		New short value for register pair affected.
	 */
	public void setRegPair(int regpairno, short value);

	/**
	 * Gets value of a register pair. Note that AF and AF_ALT register pairs
	 * are special, as their register indices are big-endian while other
	 * register pairs are little-endian.
	 * 
	 * @param regpairno	Number of register pair affected.
	 * @return			Short value of register pair.
	 */
	public short getRegPair(int regpairno);

	/**
	 * Convenience method for setting a flag in F register.
	 * @param flag	Flag index
	 * @param value	New value
	 */
	public void setFlag(int flag, boolean value);

	/**
	 * Convenience method for testing a flag in F register.
	 * @param flag	Flag index
	 * @return		Value of the flag.
	 */
	public boolean testFlag(int flag);

	/**
	 * Sets the processor halt state.
	 * @param value	If set to <code>true</code>, the processor will reset
	 *  its number of available T-states to zero and will resume only
	 *  after next interrupt or forcing the processor out of halt
	 *  state by calling this method by <code>false</code>.
	 */
	public void setHaltState(boolean value);

	/**
	 * Sets the value of available T-states before next consideration of
	 * interrupt. There are approximately 3.5 million T-states per second
	 * in an old 3.5MHz Z80-processor.
	 * 
	 * @param value	New amount of T-states.
	 */
	public void setTStates(int value);
	
	/**
	 * Gets the value of available T-states. Useful for testing.
	 * @return	Amount of T-states left.
	 */
	public int getTStates();
}

