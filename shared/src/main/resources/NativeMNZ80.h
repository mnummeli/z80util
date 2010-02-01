/*
 * NativeMNZ80.h - An example C++ implementation of Z80 processor.
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

#include <iostream>
#include <stdlib.h>
#include <time.h>

/* Register pair indices - Do NOT change the order! */
#define BC	0
#define DE	1
#define HL	2
#define	AF	3
#define BC_ALT	4
#define	DE_ALT	5
#define HL_ALT	6
#define AF_ALT	7
#define IX	8
#define	IY	9
#define SP	10
#define PC	11
#define IR	12

/* Register indices - Do NOT change the order! */
#define B	0
#define C	1
#define D	2
#define E	3
#define H	4
#define L	5
#define F	6
#define A	7
#define B_ALT	8
#define C_ALT	9
#define D_ALT	10
#define E_ALT	11
#define H_ALT	12
#define L_ALT	13
#define F_ALT	14
#define A_ALT	15
#define XH	16
#define XL	17
#define YH	18
#define YL	19
#define SPH	20
#define SPL	21
#define PCH	22
#define PCL	23
#define I	24
#define R	25

/* Halt state, interrupt mode and iff 1 and 2 */
#define IM_IFF	26	// ---HIM21

#define REGS_COUNT	27

typedef unsigned char	u8;
typedef char		s8;
typedef unsigned short	u16;
typedef short		s16;

class NativeMNZ80 {
public:
	static NativeMNZ80 *getProcessor();
	void reset();
private:
	static NativeMNZ80 *processorInstance;
	
	union {
		u8	single[REGS_COUNT];
		u16	pair[13];
	} regs;
};
