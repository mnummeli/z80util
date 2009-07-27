/*
 * SpectrumRunningProfile.java - Spectrum runtime profiling functions.
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

import org.apache.log4j.Logger;
import org.mn.z80util.disassembler.*;
import org.mn.z80util.spectrum.*;
import org.mn.z80util.z80.*;

public class SpectrumRunningProfile {
	private Logger LOG=Logger.getLogger(SpectrumRunningProfile.class);
	
	private ProfileNode[] profilingMap;
	private Vector<ProfileBlock> blockMap;
	int currentPC=-1, previousPC=-1;
	
	private Z80 z80;
	private SpectrumULA ula;
	
	public SpectrumRunningProfile(Z80 z80, SpectrumULA ula) {
		this.z80=z80;
		this.ula=ula;
		profilingMap=new ProfileNode[0x10000];
		previousPC=-1;
	}

	/**
     * Collects profiling data to profilingMap.
     * The following categories are collected:
     * 
     * <ul>
     * 	 <li>command execution density per address</li>
     *   <li>command predecessors, 52h is blacklisted, as it is IM 1
     *   interrupt return command address in Spectrum</li>
     *   <li>command successors, 38h is blacklisted, as it is IM 1
     *   interrupt entry address</li>
     * </ul>
     */
    public void collectProfilingData() {
    	currentPC=z80.getRegPair(Z80.PC) & 0xffff;
		if(profilingMap[currentPC] == null) {
			profilingMap[currentPC]=new ProfileNode();
		}
		profilingMap[currentPC].density++;
		
		/* Blacklist mode 1 interrupts (entry and return) */
		if((previousPC >= 0) && (previousPC != 0x52) &&
				(currentPC != 0x38)) {
			profilingMap[currentPC].addPredecessor(previousPC);
			profilingMap[previousPC].addSuccessor(currentPC);
		}
		previousPC=currentPC;
    }

    /**
     * Iterates the start and end blocks in profile array. The algorithm is:
     * 
     * An existing profile node is a <b>start block node</b> if and only if:
     * <ul>
     * 	 <li>its address is 38h; or</li>
     *   <li>its predecessor count differs from one; or</li>
     *   <li>it has a predecessor with higher or equal address than its own
     *     address; or</li>
     *   <li>it has a predecessor which is an <i>end block node</i></li>
     * </ul>
     * 
     * An existing profile node is an <b>end block node</b> if and only if:
     * <ul>
     *   <li>its address is 52h; or</li>
     *   <li>its successor count differs from one; or</li>
     *   <li>it has a successor with higher or equal address than its own
     *     address; or</li>
     *   <li>it has a successor which is a <i>start block node</i></li>
     * </ul>
     * 
     * As the rules are dependent from each other, one must iterate until
     * a stable state is reached.
     */
	public void findBlockStartsAndEnds() {
		for(int i=0;i<0x10000;i++) {
    		if(profilingMap[i]!=null) {
    			profilingMap[i].startBlock=false;
    			profilingMap[i].endBlock=false;
    		}
    	}
    	
    	boolean hasChanged;
    	do {
    		hasChanged=false;
    		for(int i=0;i<0x10000;i++) {
        		if(profilingMap[i]!=null) {
        			if(!profilingMap[i].startBlock) {
        				if((i==0x38)||
        						(profilingMap[i].predecessors.size()>1)) {
            				profilingMap[i].startBlock=true;
            				hasChanged=true;
            			}
        				for(Object addrObj : profilingMap[i].predecessors) {
            				int addr=((Integer)addrObj).intValue();
            				if((addr >= i) || (profilingMap[addr].endBlock)) {
            					profilingMap[i].startBlock=true;
                				hasChanged=true;
            				}
            			}
        			}
        			
        			if(!profilingMap[i].endBlock) {
        				if((i==0x52)||
        						(profilingMap[i].successors.size()>1)) {
        					profilingMap[i].endBlock=true;
        					hasChanged=true;
        				}
        				for(Object addrObj : profilingMap[i].successors) {
        					int addr=((Integer)addrObj).intValue();
        					if((addr <= i) || (profilingMap[addr].startBlock)) {
        						profilingMap[i].endBlock=true;
        						hasChanged=true;
        					}
        				}
        			}
        		}
        	}
    	} while(hasChanged==true);
	}
	
	public void createBlocks() {
		blockMap=new Vector<ProfileBlock>();
		for(int i=0;i<0x10000;i++) {
			if((profilingMap[i]!=null) &&
					(profilingMap[i].startBlock)) {
				ProfileBlock pb=new ProfileBlock(ula);
				pb.entryDensity=profilingMap[i].density;
				pb.predecessors=profilingMap[i].predecessors;
				int j=i;
				while(!profilingMap[j].endBlock) {
					pb.commandAddresses.add(j);
					j=profilingMap[j].successors.first();
				}
				pb.commandAddresses.add(j);
				pb.successors=profilingMap[j].successors;
				blockMap.add(pb);
			}
		}
	}
	
	public void reportBlocks() {
		for(ProfileBlock pb : blockMap) {
			System.out.println(pb);
		}
	}
}
