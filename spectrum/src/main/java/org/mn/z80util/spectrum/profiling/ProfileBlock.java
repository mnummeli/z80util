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
