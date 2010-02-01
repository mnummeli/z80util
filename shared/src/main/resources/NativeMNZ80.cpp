/*
 * NativeMNZ80.cpp - An example C++ implementation of Z80 processor,
 * attempted to optimize for clarity, not speed.
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

#include "NativeMNZ80.h"

/**
 * A singleton method to initialize processor instance
 */
NativeMNZ80 *NativeMNZ80::getProcessor() {
	if(processorInstance==NULL) {
		processorInstance=new NativeMNZ80;
	}
	return processorInstance;
}

/**
 * Fills registers with random numbers, sets halt state to FALSE and program
 * counter to zero.
 */
void NativeMNZ80::reset() {
	srand(time(NULL));
	for(int i=0; i < REGS_COUNT; i++) {
		regs.single[i]=(rand()<<8)/RAND_MAX;
	}
	regs.single[IM_IFF]=0x00; // Halt state is included in bit 4
	regs.pair[PC]=0x0000;
}
