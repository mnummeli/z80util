package org.mn.z80util.spectrum.profiling;

import java.util.*;

import org.mn.z80util.disassembler.*;
import org.mn.z80util.spectrum.*;

public class ProfileBlock implements Comparable<ProfileBlock> {
	SpectrumULA ula;
	long entryDensity;
	TreeSet<Integer> predecessors, successors;
	
	/**
	 * Another set of predecessors and successors, which should be in some
	 * cases a translation of predecessor and successor addresses into
	 * their order numbers in a program block array.
	 */
	TreeSet<Integer> predecessorNumbers, successorNumbers;
	
	Vector<Integer> commandAddresses;
	
	public ProfileBlock(SpectrumULA ula) {
		this.ula=ula;
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
	
	public String toString() {
		byte[] memory=ula.getMemory();
		String retval="Density: "+entryDensity+"\n";
		retval+="Logarithm of density: "+Math.log(entryDensity)+"\n";
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
			retval+=String.format("%-4s : %-14s %-20s\n",
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
