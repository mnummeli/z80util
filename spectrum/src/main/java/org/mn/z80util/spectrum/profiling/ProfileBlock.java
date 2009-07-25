package org.mn.z80util.spectrum.profiling;

import java.util.*;

import org.mn.z80util.disassembler.Hex;

public class ProfileBlock implements Comparable<ProfileBlock> {
	long maxDensity=0L;
	
	Vector<Integer> commandAddresses=new Vector<Integer>();
	
	TreeSet<Integer> predecessorsAddr=new TreeSet<Integer>(),
		successorsAddr=new TreeSet<Integer>();

	TreeSet<Integer> predecessorsInd=new TreeSet<Integer>(),
	successorsInd=new TreeSet<Integer>();
	
	void addCommandAddress(int address) {
		commandAddresses.add(address);
	}

	public int compareTo(ProfileBlock pb) {
		if (this.maxDensity < pb.maxDensity) {
			return 1;
		} else if (this.maxDensity == pb.maxDensity) {
			return 0;
		} else {
			return -1;
		}
	}
	
	public String toString() {
		String retval="BLOCK:\nPredecessors: ";
		for(Integer i : predecessorsAddr) {
			retval+=Hex.intToHex4(i)+" ";
		}
		retval+="Commands:\n";
		for(Integer i : commandAddresses) {
			retval+=(Hex.intToHex4(i)+"\n");
		}
		retval+="Successors: ";
		for(Integer i : successorsAddr) {
			retval+=Hex.intToHex4(i)+" ";
		}
		return retval+"\n\n";
	}
}
