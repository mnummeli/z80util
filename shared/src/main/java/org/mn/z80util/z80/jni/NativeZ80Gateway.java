/*
 * NativeZ80Gateway.java - Z80 processor C++ interface.
 * 
 * This is a special gateway class to provide JNI calls to an undelying
 * C++/C/FORTRAN implementation of Z80 processor. The motivation for this
 * class is to be able to test it against Java Z80 implementations.
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

package org.mn.z80util.z80.jni;

import org.mn.z80util.z80.*;

public class NativeZ80Gateway implements TestZ80 {
	public NativeZ80Gateway() {
		System.loadLibrary("Z80Gateway");
	}
	
	public native void executeNextCommand();
	public native byte getReg(int regno);
	public native short getRegPair(int regpairno);
	public native void reset();
	public native void setHaltState(boolean value);
	public native void setReg(int regno, byte value);
	public native void setRegPair(int regpairno, short value);
	
	AddressBusProvider ula;
	public void setUla(AddressBusProvider ula) {
		this.ula=ula;
	}
}
