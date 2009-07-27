package org.mn.z80util.spectrum.profiling;

import java.util.*;

import org.mn.z80util.disassembler.*;
import org.mn.z80util.spectrum.*;

public class ProfileBlock implements Comparable<ProfileBlock> {
	SpectrumULA ula;
	long entryDensity;
	TreeSet<Integer> predecessors, successors;
	Vector<Integer> commandAddresses;
	
	public ProfileBlock(SpectrumULA ula) {
		this.ula=ula;
		entryDensity=0L;
		predecessors=new TreeSet<Integer>();
		commandAddresses=new Vector<Integer>();
		successors=new TreeSet<Integer>();
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
		String retval="\nPROGRAM BLOCK:\n";
		retval+="Density: "+entryDensity+"\n";
		retval+="Predecessors: ";
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
		retval+="\nSuccessors: ";
		for(int i : successors) {
			retval+=Hex.intToHex4(i)+" ";
		}
		retval+="\n\n----------";
		return retval;
	}
}
