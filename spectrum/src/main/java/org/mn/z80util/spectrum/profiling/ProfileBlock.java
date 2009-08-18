/*
 * ProfileBlock.java - Spectrum runtime profiler block.
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


package org.mn.z80util.spectrum.profiling;

import java.util.*;

import org.mn.z80util.disassembler.*;
import org.mn.z80util.spectrum.*;

public class ProfileBlock implements Comparable<ProfileBlock> {
	SpectrumULA ula;
	SpectrumRunningProfile profile;
	long entryDensity;
	TreeSet<Integer> predecessors, successors;
	
	/**
	 * Another set of predecessors and successors, which should be in some
	 * cases a translation of predecessor and successor addresses into
	 * their order numbers in a program block array.
	 */
	TreeSet<Integer> predecessorNumbers, successorNumbers;
	
	Vector<Integer> commandAddresses;
	
	public ProfileBlock(SpectrumULA ula, SpectrumRunningProfile profile) {
		this.ula=ula;
		this.profile=profile;
		entryDensity=0L;
		predecessors=new TreeSet<Integer>();
		commandAddresses=new Vector<Integer>();
		successors=new TreeSet<Integer>();
	}
	
	public int getFirstCommandAddress() {
		return commandAddresses.firstElement() & 0xffff;
	}
	
	public int getLastCommandAddress() {
		return commandAddresses.lastElement() & 0xffff;
	}

	public int compareTo(ProfileBlock pb) {
		if (this.entryDensity < pb.entryDensity) {
			return 1;
		} else if (this.entryDensity == pb.entryDensity) {
			return 0;
		} else {
			return -1;
		}
	}
	
	/**
	 * A helper function for determining whether this profile block is
	 * probably innermost in a loop structure. Requires that profile blocks
	 * are sorted according to their frequencies/densities in descending order
	 * and that block order number of this block is provided.
	 * 
	 * @param orderno	Order number of this profile block
	 * @return			True if this block has no successors or predecessors
	 * 					with lower order number than this block has, false
	 * 					otherwise
	 */
	public boolean isInnermostNode(int orderno) {
		for(int i : predecessorNumbers) {
			if (i<orderno) {
				return false;
			}
		}
		for(int i : successorNumbers) {
			if (i<orderno) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * A pretty printer of the frequency value, distinguishes between Hz and kHz.
	 * 
	 * @param frequency	Frequency number
	 */
	private String frequencyString(double frequency) {
		int roundedFrequency=(int)(frequency+.5);
		if(roundedFrequency>=1000) {
			return roundedFrequency/1000 + " kHz";
		} else {
			return roundedFrequency+" Hz";
		}
	}
	
	public String toString() {
		byte[] memory=ula.getMemory();
		String retval="Number of entries: "+entryDensity+"\n";
		double frequency=(double)entryDensity/profile.getProfilingTimeInSeconds();
		retval+="Frequency of entries: "+frequencyString(frequency)+"\n";
		retval+="Binary logarithm of density: "+Math.log(frequency)/Math.log(2)+"\n";
		if(predecessorNumbers != null) {
			retval+="Predecessors numbers: ";
			for(int i : predecessorNumbers) {
				retval+=i+" ";
			}
		}
		retval+="\nPredecessors: ";
		for(int i : predecessors) {
			retval+=Hex.intToHex4(i)+" ";
		}
		retval+="\n\n";
		for(int i : commandAddresses) {
			DisasmResult dar=Disassembler.disassemble(memory,(short)i);
			retval+=String.format("%-4s : %-14s %s\n",
					Hex.intToHex4(i & 0xffff),
					dar.getHexDigits(), dar.getCommand());
		}
		if(successorNumbers != null) {
			retval+="\nSuccessor numbers: ";
			for(int i : successorNumbers) {
				retval+=i+" ";
			}
		}
		retval+="\nSuccessors: ";
		for(int i : successors) {
			retval+=Hex.intToHex4(i)+" ";
		}
		retval+="\n\n----------\n";
		return retval;
	}
}
