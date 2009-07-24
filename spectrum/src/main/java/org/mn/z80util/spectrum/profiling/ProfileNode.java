/*
 * ProfileNode.java - Spectrum runtime profiler node.
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

public class ProfileNode {
	boolean startBlock=false, endBlock=false;
	
	long density=0L;
	TreeSet<Integer> predecessors=new TreeSet<Integer>(),
		successors=new TreeSet<Integer>();
	
	void addPredecessor(int address) {
		predecessors.add(address);
	}
	
	void addSuccessor(int address) {
		successors.add(address);
	}
}
