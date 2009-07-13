/*
 * AbstractSpectrumSnapshot.java - Provides a base, along with interface
 * Snapshot, to load and write Spectrum snapshots, first capturing the
 * important information here.
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

package org.mn.z80util.spectrum.snapshots;

import org.mn.z80util.spectrum.*;
import org.mn.z80util.z80.*;

public abstract class AbstractSpectrumSnapshot implements Snapshot {
	protected Z80 z80;
	protected SpectrumULA ula;
	
	protected byte[] memory;
	protected byte[] regs;
	protected int border;
	
	public AbstractSpectrumSnapshot() {
		memory=new byte[0x10000];
		regs=new byte[27];
	}

	public AbstractSpectrumSnapshot(Z80 z80, SpectrumULA ula) {
		this();
		this.z80=z80;
		this.ula=ula;
		read(z80,ula);
	}
	
	public void read(Z80 z80, SpectrumULA ula) {
		this.z80=z80;
		this.ula=ula;
		for(int i=0;i<0x10000;i++) {
			memory[i]=ula.getByte((short)i);
		}
		for(int i=0;i<27;i++) {
			regs[i]=z80.getReg(i);
		}
		border=ula.getBorder();
	}
	
	public void write(Z80 z80, SpectrumULA ula) {
		this.z80=z80;
		this.ula=ula;
		for(int i=0;i<0x10000;i++) {
			ula.setByte((short)i,memory[i]);
		}
		for(int i=0;i<27;i++) {
			z80.setReg(i,regs[i]);
		}
		ula.setBorder(border);
	}
	
	public void write() {
		if((this.z80 != null) && (this.ula != null)) {
			write(this.z80, this.ula);
		}
	}
}
