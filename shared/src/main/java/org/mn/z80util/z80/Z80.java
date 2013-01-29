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

public interface Z80 extends TestZ80 {

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
     * Convenience method for setting a flag in F register.
     *
     * @param flag	Flag index
     * @param value	New value
     */
    public void setFlag(int flag, boolean value);

    /**
     * Convenience method for testing a flag in F register.
     *
     * @param flag	Flag index
     * @return	Value of the flag.
     */
    public boolean testFlag(int flag);

    /**
     * Sets the processor halt state.
     *
     * @param value	If set to <code>true</code>, the processor will reset its
     * number of available T-states to zero and will resume only after next
     * interrupt or forcing the processor out of halt state by calling this
     * method by <code>false</code>.
     */
    @Override
    public void setHaltState(boolean value);

    /**
     * Sets the value of available T-states before next consideration of
     * interrupt. There are approximately 3.5 million T-states per second in an
     * old 3.5MHz Z80-processor.
     *
     * @param value	New amount of T-states.
     */
    public void setTStates(int value);

    /**
     * Gets the value of available T-states. Useful for testing.
     *
     * @return	Amount of T-states left.
     */
    public int getTStates();
}
