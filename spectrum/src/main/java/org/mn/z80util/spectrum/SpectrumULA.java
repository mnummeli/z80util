/*
 * ULAImpl.java - Spectrum ULA implementation.
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

package org.mn.z80util.spectrum;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.log4j.*;

import org.mn.z80util.z80.*;

public class SpectrumULA implements AddressBusProvider {
	Logger LOG=Logger.getLogger(SpectrumULA.class);
	
	private SpectrumScreen scr;
	public void setScreen(SpectrumScreen scr) {
		this.scr=scr;
	}
	
	private boolean flashState=false;
	private int border=0;
	private byte[] memory=new byte[0x10000];
	private byte[] keys=new byte[8];
	private boolean[] screenLineUpdateRequest=new boolean[296];

	public void changeFlashState() {
		flashState=!flashState;
	}
	
	public boolean getFlashState() {
		return flashState;
	}

	public int getBorder() {
		return border & 7;
	}

	public byte getByte(short address) {
		return memory[address & 0xffff];
	}

	public byte getIOByte(short address) {
		LOG.trace("Requesting IO byte from address: "+address);
		byte retval=(byte)0xff;
		if((address&0x1)==0) {
			/* Keyboard request, process keyboard rows. */
			for(int i=0;i<8;i++) {
				if((address&(0x100<<i))==0) {
					/* Row number i is asked for and AND:ed to the result */
					retval &= keys[i];
				}
			}
		} else {
			/* To avoid false signals from currently not present
			 * Kempston joystick. */
			retval=(byte)0x00;
		}
		return retval;
	}

	public byte[] getMemory() {
		return memory;
	}

	public short getWord(short address) {
		short low=(short)(getByte(address)&0xff);
		short high=(short)(getByte((short)(address+1))&0xff);
		return (short)(low|(high<<8));
	}

	/**
	 * Loads Spectrum ROM
	 * 
	 * @param is	Input stream where the ROM is in uncompressed plain
	 * 				format.
	 */
	public void loadROM(InputStream is) {
		try {
			is.read(memory,0,0x4000);
		} catch (NullPointerException npexc) {
			LOG.error("ROM file not found.");
			npexc.printStackTrace();
			System.exit(1);
		} catch (IOException ioexc) {
			LOG.error("Unable to load ROM.");
			ioexc.printStackTrace();
			System.exit(1);
		}
		LOG.info("ROM successfully loaded.");
	}
	
	public void markScreenDirty() {
		for(int i=0;i<296;i++) {
			screenLineUpdateRequest[i]=true;
		}
	}

	public void reset() {
		Random rand=new Random(System.nanoTime());
		rand.nextBytes(memory);
		clearKeyData();
		markScreenDirty();
	}

	public void setBorder(int value) {
		border=value & 7;
		markScreenDirty();
	}

	public void setByte(short address, byte value) {
		if((address & 0xc000)==0) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("Attempted to write into ROM address: "+address);
			}
		} else {
			/* Contended (screen) memory trap */
			if((address>=0x4000)&&(address<0x5800)) {
				/* Pixel rows */
				int characterRow=(address&0x00e0)>>5;
				int pixelRow=(address&0x0700)>>8;
				int third=(address&0x1800)>>11;
				screenLineUpdateRequest[48+pixelRow+8*characterRow+64*third]=true;
			} else if((address>=0x5800)&&(address<0x5b00)) {
				/* Attribute rows */
				int characterRow=(address&0x00e0)>>5;
				int third=(address&0x0300)>>8;
				for(int i=0;i<8;i++) {
					screenLineUpdateRequest[48+8*characterRow+64*third+i]=true;
				}
			}
			memory[address & 0xffff]=value;
		}
	}

	public void setFlashState(boolean flashState) {
		this.flashState=flashState;
	}

	public void setIOByte(short address, byte value) {
		if((address & 0x1)==0) {
			setBorder(value);
		}
	}

	/**
	 * Initializes keyboard array
	 */
	public void clearKeyData() {
		for(int i=0;i<8;i++) {
			keys[i]=(byte)0xff;
		}
	}

	public void setKeyData(int row, int column, boolean eventType) {
		if(eventType) {
			/* Key press event */
			keys[row] &= (byte)~(0x1<<column);
		} else {
			/* Key release event */
			keys[row] |= (byte)(0x1<<column);
		}
	}

	public void setWord(short address, short value) {
		byte low=(byte)(value & 0xff);
		byte high=(byte)((value>>8) & 0xff);
		setByte(address,low);
		setByte((short)(address+1),high);
	}

	public void updateRow(int row) {
		int tmpRow=row-16;
		if((scr!=null) && (tmpRow>=0) && screenLineUpdateRequest[tmpRow]) {
			scr.updateRow(tmpRow);
			screenLineUpdateRequest[tmpRow]=false;
		}
	}
}
