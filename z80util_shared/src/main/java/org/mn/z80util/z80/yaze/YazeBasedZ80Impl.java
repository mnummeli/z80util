/*
 * Z80Impl.java - Z80 processor implementation, which uses an ALU based on
 * YAZE.
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

package org.mn.z80util.z80.yaze;

import java.util.*;

import org.apache.log4j.*;

import org.mn.z80util.z80.*;
import org.mn.z80util.disassembler.*;

public class YazeBasedZ80Impl implements Z80 {
	Logger LOG=Logger.getLogger(YazeBasedZ80Impl.class);
	
	/* YAZE based arithmetic-logical unit, which also hosts AF and AF' */
	private YazeBasedALU alu=null;

	/* Z80 command parsing helpers */
	private boolean ixmode=false, iymode=false;
	private byte command;
	
	public YazeBasedZ80Impl() {
		this.alu=new YazeBasedALU();
	}
	
	public synchronized void reset() {
		Random rand=new Random(System.nanoTime());
		rand.nextBytes(regs);
		alu.af(rand.nextInt());
		haltState=false;
		setReg(IM_IFF,(byte)0x00);
		
		setRegPair(PC,(short)0x0000);
	}
	
	private AddressBusProvider ula;
	public void setUla(AddressBusProvider ula) {
		this.ula=ula;
	}

	/* T-states and halt */
	
	private int ts;
	public int getTStates() {
		return ts;
	}
	
	public void setTStates(int value) {
		ts=value;
	}
	
	private boolean haltState=false;
	public void setHaltState(boolean value) {
		haltState=value;
		LOG.debug("Processor halt state set to: "+haltState);
	}

	/* Interrupt routines and helpers */
	
	public void NMI() {
		LOG.trace("Generating non-maskable interrupt.");
		haltState=false;
		regs[IM_IFF]=(byte)((regs[IM_IFF] &~ 1) | ((regs[IM_IFF] & 1)<<1));
		push(PC);
		setRegPair(PC,(short)0x0066);	
	}

	public void interrupt() {
		haltState=false;
		if((getReg(IM_IFF) & 1) != 0) {
			setReg(IM_IFF, (byte)(getReg(IM_IFF) & ~3));
			push(PC);
			switch(getReg(IM_IFF)>>2) {
			case 0:
				LOG.trace("Generating interrupt of mode 0.");
				setRegPair(PC,(short)0x0038);
				break;
			case 1:
				LOG.trace("Generating interrupt of mode 1.");
				setRegPair(PC,(short)0x0038);
				break;
			case 2:
				LOG.trace("Generating interrupt of mode 2.");
				int iv=(getReg(I)<<8)|0xff;
				LOG.trace("Interrupt vector pointer is: "+iv);
				int jumpAddress=ula.getWord((short)iv);
				LOG.trace("Jump address is: "+jumpAddress);
				setRegPair(PC,(short)jumpAddress);
				break;
			}
		}
	}
	
	/* Register accessors and helpers */
	
	private byte[] regs=new byte[23];
	
	/**
	 * Gets register. Note that A, F and their alternatives reside in the ALU.
	 */
	public byte getReg(int regno) {
		if(regno==F) {
			return (byte)alu.f();
		} else if(regno==A) {
			return (byte)alu.a();
		} else if(regno==F_ALT) {
			return (byte)alu.f_alt();
		} else if(regno==A_ALT) {
			return (byte)alu.a_alt();
		} else if(regno<6) {
			return regs[regno];
		} else if((regno>=8) && (regno<14)) {
			return regs[regno-2];
		} else {
			return regs[regno-4];
		}
	}
	
	public void setReg(int regno, byte value) {
		if(regno==F) {
			alu.f((byte)value);
		} else if(regno==A) {
			alu.a((byte)value);
		} else if(regno==F_ALT) {
			alu.f_alt((byte)value);
		} else if(regno==A_ALT) {
			alu.a_alt((byte)value);
		} else if(regno<6) {
			regs[regno]=value;
		} else if((regno>=8) && (regno<14)) {
			regs[regno-2]=value;
		} else {
			regs[regno-4]=value;
		}
	}

	public short getRegPair(int regpairno) {
		int regno=regpairno<<1;
		short tmp;
		short high=(short)(getReg(regno)&0xff);
		short low=(short)(getReg((short)(regno+1))&0xff);
		if((regpairno==AF)||(regpairno==AF_ALT)) {
			tmp=high;
			high=low;
			low=tmp;
		}
		return (short)(low|(high<<8));
	}
	
	public void setRegPair(int regpairno, short value) {
		int regno=regpairno<<1;
		byte tmp;
		byte low=(byte)(value & 0xff);
		byte high=(byte)((value>>8) & 0xff);
		if((regpairno==AF)||(regpairno==AF_ALT)) {
			tmp=high;
			high=low;
			low=tmp;
		}
		setReg(regno,high);
		setReg((short)(regno+1),low);
	}

	public void setFlag(int flag, boolean value) {
		if(value) {
			alu.f(alu.f() | flag);
		} else {
			alu.f(alu.f() & ~flag);
		}
	}
	
	public boolean testFlag(int flag) {
		return ((alu.f() & flag)!=0);
	}
	
	/**
	 * Executes next command from memory, pointed by register pair PC.
	 */
	public synchronized void executeNextCommand() {

		if(haltState) {
			ts=0;
			return;
		}
		
		ts-=4;
		ixmode=iymode=false;
		command=fetchByte();
		
		/* IX and IY modes */
		while((command==(byte)0xdd)||(command==(byte)0xfd)) {
			ts-=4;
			if(command==(byte)0xdd) {
				ixmode=true;
				iymode=false;
			} else {
				ixmode=false;
				iymode=true;
			}
			command=fetchByte();
		}
		
		/* CBh and EDh extensions */
		if((command & 0xff) == 0xcb) {
			CBh();
			return;
		} else if((command & 0xff) == 0xed) {
			EDh();
			return;
		}

		/* Main fork, mostly octal notation to highlight some symmetries. */
		
		/* NOP: 00000000 */
		if((command & 0377)==0000) {
			// do nothing

		/* EX AF,AF': 00001000 */
		} else if((command & 0377)==0010) {
			alu.ex_af();

		/* DJNZ dis: 00010000 */
		} else if((command & 0377)==0020) {
			byte tmp=fetchByte();
			setReg(B,(byte)(getReg(B)-1));
			if(getReg(B)!=0) {
				rjump(tmp);
				ts-=9;
			} else {
				ts-=4;
			}

		/* JR dis: 00011000 */
		} else if((command & 0377)==0030) {
			byte tmp=fetchByte();
			rjump(tmp);
			ts-=8;

		/* JR <cond>,dis: 001CC000 */
		} else if((command & 0347)==0040) {
			byte tmp=fetchByte();
			if(flagCond((command & 0030)>>3)) {
				rjump(tmp);
				ts-=8;
			} else {
				ts-=3;
			}
			
		/* LD <rp>,NN: 00RP0001 */
		} else if((command & 0317)==0001) {
			short tmp=fetchWord();
			setRegPair(rp1((command & 0060)>>4),tmp);
			ts-=6;

		/* ADD HL,<rp>: 00RP1001 */
		} else if((command & 0317)==0011) {
			int target=rp1(HL), source=rp1((command & 0060)>>4);
			setRegPair(target, (short)alu.add16(getRegPair(target),
					getRegPair(source)));
			ts-=7;

		/* LD (BC/DE),A: 000R0010 */
		} else if((command & 0357)==0002) {
			int rp=(command & 0020)>>4;
			ula.setByte(getRegPair(rp),(byte)alu.a());
			ts-=3;
		
		/* LD A,(BC/DE): 000R1010 */
		} else if((command & 0357)==0012) {
			int rp=(command & 0020)>>4;
			alu.a(ula.getByte(getRegPair(rp)));
			ts-=3;
		
		/* LD (NN),HL: 00100010 */
		} else if((command & 0377)==0042) {
			short tmp=fetchWord();
			ula.setWord(tmp,getRegPair(rp1(HL)));
			ts-=12;
		
		/* LD HL,(NN): 00101010 */
		} else if((command & 0377)==0052) {
			short tmp=fetchWord();
			setRegPair(rp1(HL),ula.getWord(tmp));
			ts-=12;
			
		/* LD (NN), A: 00110010 */
		} else if((command & 0377)==0062) {
			short tmp=fetchWord();
			ula.setByte(tmp,getReg(A));
			ts-=9;
		
		/* LD A, (NN): 00111010 */
		} else if((command & 0377)==0072) {
			short tmp=fetchWord();
			setReg(A,ula.getByte(tmp));
			ts-=9;
			
		/* INC <rp>: 00RP0011 */
		} else if((command & 0317)==0003) {
			int rp=rp1((command & 0060)>>4);
			short tmp=getRegPair(rp);
			setRegPair(rp,++tmp);
			ts-=2;
		
		/* DEC <rp>: 00RP1011 */
		} else if((command & 0317)==0013) {
			int rp=rp1((command & 0060)>>4);
			short tmp=getRegPair(rp);
			setRegPair(rp,--tmp);
			ts-=2;

		/* INC <reg>: 00RRR100 */
		} else if((command & 0307)==0004) {
			int reg=reg1((command & 0070)>>3);
			if(reg!=6) {
				setReg(reg,(byte)alu.inc8((int)getReg(reg)));
			} else {
				short addr;
				if(ixmode) {
					addr=(short)(getRegPair(IX)+fetchByte());
					ts-=15;
				} else if(iymode) {
					addr=(short)(getRegPair(IY)+fetchByte());
					ts-=15;
				} else {
					addr=getRegPair(HL);
					ts-=7;
				}
				ula.setByte(addr,(byte)alu.inc8((int)ula.getByte(addr)));
			}

		/* DEC <reg>: 00RRR101 */
		} else if((command & 0307)==0005) {
			int reg=reg1((command & 0070)>>3);
			if(reg!=6) {
				setReg(reg,(byte)alu.dec8((int)getReg(reg)));
			} else {
				short addr;
				if(ixmode) {
					addr=(short)(getRegPair(IX)+fetchByte());
					ts-=15;
				} else if(iymode) {
					addr=(short)(getRegPair(IY)+fetchByte());
					ts-=15;
				} else {
					addr=getRegPair(HL);
					ts-=7;
				}
				ula.setByte(addr,(byte)alu.dec8((int)ula.getByte(addr)));
			}
	
		/* LD <reg>,N: 00RRR110 */
		} else if((command & 0307) == 0006) {
			int reg=reg1((command & 0070)>>3);
			if(reg!=6) {
				setReg(reg,fetchByte());
				ts-=3;
			} else {
				short addr;
				if(ixmode) {
					addr=(short)(getRegPair(IX)+fetchByte());
					ts-=11;
				} else if(iymode) {
					addr=(short)(getRegPair(IY)+fetchByte());
					ts-=11;
				} else {
					addr=(short)(getRegPair(HL));
					ts-=6;
				}
				ula.setByte(addr,fetchByte());
			}
		
		/* RLCA: 00000111 */
		} else if((command & 0377) == 0007) {
			alu.rlca();
			
		/* RRCA: 00001111 */
		} else if((command & 0377) == 0017) {
			alu.rrca();
		
		/* RLA: 00010111 */
		} else if((command & 0377) == 0027) {
			alu.rla();
			
		/* RRA: 00011111 */
		} else if((command & 0377) == 0037) {
			alu.rra();
			
		/* DAA: 00100111 */
		} else if((command & 0377) == 0047) {
			alu.daa();
			
		/* CPL: 00101111 */
		} else if((command & 0377) == 0057) {
			alu.cpl();
			
		/* SCF: 00110111 */
		} else if((command & 0377) == 0067) {
			alu.scf();
			
		/* CCF: 00111111 */
		} else if((command & 0377) == 0077) {
			alu.ccf();

		/* HALT: 01110110 */
		} else if((command & 0377) == 0166) {
			setHaltState(true);
			
		/* LD Q,R: 01QQQRRR */
		} else if((command & 0300) == 0100) {
			int q=reg1((command & 0070)>>3);
			int r=reg1(command & 0007);
			if((q!=6) && (r!=6)) {
				setReg(q,getReg(r));
			} else {
				short addr;
				if(ixmode) {
					addr=(short)(getRegPair(IX)+fetchByte());
					ts-=11;
				} else if(iymode) {
					addr=(short)(getRegPair(IY)+fetchByte());
					ts-=11;
				} else {
					addr=(short)(getRegPair(HL));
					ts-=3;
				}
				if(q==6) {
					ula.setByte(addr, getReg(command & 0007));
				} else {
					setReg((command & 0070)>>3, ula.getByte(addr));
				}
			}

		/* CMD A,<reg>: 10CMDRRR */
		} else if((command & 0300) == 0200) {
			int reg=reg1(command & 0007), tmp;
			if(reg==6) {
				short addr;
				if(ixmode) {
					addr=(short)(getRegPair(IX)+fetchByte());
					ts-=11;
				} else if(iymode) {
					addr=(short)(getRegPair(IY)+fetchByte());
					ts-=11;
				} else {
					addr=(short)(getRegPair(HL));
					ts-=3;
				}
				tmp=ula.getByte(addr);
			} else {
				tmp=getReg(reg);
			}
			int cmd=(command & 0070)>>3;
			alu.cmd8(cmd, tmp);
		
		/* RET <cond>: 11CCC000 */
		} else if((command & 0307)==0300) {
			if(flagCond((command & 0070)>>3)) {
				pop(PC);
				ts-=7;
			} else {
				ts-=1;
			}

		/* POP <rp>: 11RP0001 */
		} else if((command & 0317)==0301) {
			pop(rp2((command & 0060)>>4));
			ts-=6;
			
		/* RET: 11001001 */
		} else if((command & 0377)==0311) {
			pop(PC);
			ts-=6;

		/* EXX: 11011001 */
		} else if((command & 0377)==0331) {
			ex(BC,BC_ALT);
			ex(DE,DE_ALT);
			ex(HL,HL_ALT);
			
		/* JP (HL): 11101001 */
		} else if((command & 0377)==0351) {
			setRegPair(PC,getRegPair(rp1(HL)));
			
		/* LD SP,HL: 11111001 */
		} else if((command & 0377)==0371) {
			setRegPair(SP,getRegPair(rp1(HL)));
			ts-=2;

		/* JP <cond>, NN: 11CCC010 */
		} else if((command & 0307)==0302) {
			short addr=fetchWord();
			if(flagCond((command & 0070)>>3)) {
				setRegPair(PC,addr);
			}
			ts-=6;
		
		/* JP NN: 11000011 */
		} else if((command & 0377)==0303) {
			setRegPair(PC,fetchWord());
			ts-=6;
		
		/* OUT (N), A: 11010011 */
		} else if((command & 0377)==0323) {
			int high = (alu.a()&0xff)<<8;
			int low  = fetchByte() & 0xff;
			short addr = (short)(high | low);
			ula.setIOByte(addr,getReg(A));
			ts-=7;
		
		/* IN A, (N): 11011011 */
		} else if((command & 0377)==0333) {
			int high = (alu.a()&0xff)<<8;
			int low  = fetchByte() & 0xff;
			short addr = (short)(high | low);
			setReg(A,ula.getIOByte(addr));
			ts-=7;
			
		/* EX (SP),HL: 11100011 */
		} else if((command & 0377)==0343) {
			short sp=getRegPair(SP);
			short hl=getRegPair(rp1(HL));
			short isp=ula.getWord(sp);
			ula.setWord(sp,hl);
			setRegPair(rp1(HL),isp);
			ts-=15;
			
		/* EX DE,HL: 11101011 */
		} else if((command & 0377)==0353) {
			ex(DE,HL);
		
		/* DI: 11110011 */
		} else if((command & 0377)==0363) {
			setReg(IM_IFF, (byte)(getReg(IM_IFF) & ~3));
			
		/* EI: 11111011 */
		} else if((command & 0377)==0373) {
			setReg(IM_IFF, (byte)(getReg(IM_IFF) | 3));

		/* CALL <cond>, NN: 11CCC100 */
		} else if((command & 0307)==0304) {
			short addr=fetchWord();
			if(flagCond((command & 0070)>>3)) {
				push(PC);
				setRegPair(PC,addr);
				ts-=13;
			} else {
				ts-=6;
			}
			
		/* PUSH <rp>: 11RP0101 */
		} else if((command & 0317)==0305) {
			push(rp2((command & 0060)>>4));
			ts-=7;
			
		/* CALL NN: 11001101 */
		} else if((command & 0377)==0315) {
			short tmp=fetchWord();
			push(PC);
			setRegPair(PC,tmp);
			ts-=13;
			
		/* CMD A,N: 11CMD110 */
		} else if((command & 0307)==0306) {
			int tmp=fetchByte();
			int cmd=(command & 0070)>>3;
			alu.cmd8(cmd,tmp);
			ts-=3;
			
		/* RST 8*N: 11NNN111 */
		} else if((command & 0307)==0307) {
			push(PC);
			setRegPair(PC,(short)(command & 0070));
			ts-=7;
		}
 	}
	
	private void CBh() {
		ts-=4;
		byte dis=0x00;
		if(ixmode || iymode) {
			dis=fetchByte();
		}

		command=fetchByte();
		int reg=command & 0007;
		int tmp;
		short addr=0x0000;
		if(ixmode || iymode || (reg==6)) {
			if(ixmode) {
				addr=(short)(getRegPair(IX)+dis);
				ts-=8;
			} else if(iymode) {
				addr=(short)(getRegPair(IY)+dis);
				ts-=8;
			} else {
				addr=getRegPair(HL);
				ts-=4;
			}
			tmp=ula.getByte(addr);
		} else {
			tmp=getReg(reg);
		}
		
		tmp=alu.cb(command,tmp);
		
		/* Bit only set flags, others didn't but do something else */
		if((command & 0300)!=0100) {
			if(reg!=6) {
				setReg(reg,(byte)tmp);
			}
			if(ixmode || iymode || (reg==6)) {
				ula.setByte(addr,(byte)tmp);
				ts-=3;
			}
		}
	}
	
	private void EDh() {
		ts-=4;
		command=fetchByte();
		if(ixmode || iymode) {
			LOG.warn("IX and IY modes with prefix EDh are not supported.");
			ixmode=iymode=false;
		}
		
		/* IN <reg>, (C): 01RRR000 */
		if((command & 0307) == 0100) {
			int reg=(command & 0070)>>3;
			byte tmp=ula.getIOByte(getRegPair(BC));
			alu.in_ibc(tmp & 0xff);
			if(reg!=6) {
				setReg(reg,tmp);
			}
			ts-=4;

		/* OUT (C), <reg>: 01RRR001 */
		} else if((command & 0307) == 0101) {
			int reg=(command & 0070)>>3;
			if(reg!=6) {
				ula.setIOByte(getRegPair(BC),getReg(reg));
			} else {
				ula.setIOByte(getRegPair(BC),(byte)0x00);
			}
			ts-=4;
			
		/* SBC HL, <rp>: 01RP0010 */
		} else if((command & 0317) == 0102) {
			int source=rp1((command & 0060)>>4);
			setRegPair(HL,(short)alu.sbc16(getRegPair(HL),
					getRegPair(source)));
			ts-=7;
			
		/* ADC HL, <rp>: 01RP1010 */
		} else if((command & 0317) == 0112) {
			int source=rp1((command & 0060)>>4);
			setRegPair(HL,(short)alu.adc16(getRegPair(HL),
					getRegPair(source)));
			ts-=7;
			
		/* LD (NN), <rp>: 01RP0011 */
		} else if((command & 0317) == 0103) {
			ula.setWord(fetchWord(),getRegPair(rp1((command & 0060)>>4)));
			ts-=12;
		
		/* LD <rp>, (NN): 01RP1011 */
		} else if((command & 0317) == 0113) {
			setRegPair(rp1((command & 0060)>>4),ula.getWord(fetchWord()));
			ts-=12;

		/* NEG: 01---100 */
		} else if((command & 0307) == 0104) {
			alu.neg();
			
		/* RETN / RETI: 01--X101 */
		} else if((command & 0307) == 0105) {
			int im_iff=getReg(IM_IFF);
			im_iff = (im_iff &~ 1) | ((im_iff & 2)>>1);
			setReg(IM_IFF, (byte)im_iff);
			pop(PC);
			ts-=6;
		
		/* IM X: 01-XX110 */
		} else if((command & 0307) == 0106) {
			int im_iff=getReg(IM_IFF);
			switch((command & 0030)>>3) {
			case 0:
			case 1:
				im_iff &= 3;
				break;
			case 2:
				im_iff = (byte)((im_iff & 3) | 4);
				break;
			case 3:
				im_iff = (byte)((im_iff & 3) | 8);
				break;
			}
			setReg(IM_IFF, (byte)im_iff);
			
		/* LD I, A: 01000111 */
		} else if((command & 0377) == 0107) {
			setReg(I,getReg(A));
			ts-=1;
			
		/* LD R, A: 01001111 */
		} else if((command & 0377) == 0117) {
			setReg(R, getReg(A));
			ts-=1;

		/* LD A, I: 01010111 */
		} else if((command & 0377) == 0127) {
			int tmp=getReg(I);
			alu.ld_a_ir(tmp, (getReg(IM_IFF) & 2)!=0);
			ts-=1;

		/* LD A, R: 01011111 */
		} else if((command & 0377) == 0137) {
			int tmp=getReg(R);
			alu.ld_a_ir(tmp, (getReg(IM_IFF) & 2)!=0);
			ts-=1;
			
		/* RRD: 01100111 */
		} else if((command & 0377) == 0147) {
			short addr=getRegPair(HL);
			int tmp=ula.getByte(addr);
			tmp=alu.rrd(tmp);
			ula.setByte(addr,(byte)tmp);
			ts-=10;
	
		/* RLD: 01101111 */
		} else if((command & 0377) == 0157) {
			short addr=getRegPair(HL);
			int tmp=ula.getByte(addr);
			tmp=alu.rld(tmp);
			ula.setByte(addr,(byte)tmp);
			ts-=10;

		/* LD(I/D)(R): 101RD000 */
		} else if((command & 0347) == 0240) {
			short bc = getRegPair(BC);
			short de = getRegPair(DE);
			short hl = getRegPair(HL);

			byte ihl=ula.getByte(hl);
			alu.ldi_ldd(ihl & 0xff, bc & 0xffff);
			ula.setByte(de, ihl);
			bc--;
			
			if((command & 0010)==0) {
				de++; hl++;
			} else {
				de--; hl--;
			}
			
			setRegPair(BC, bc);
			setRegPair(DE, de);
			setRegPair(HL, hl);
			ts-=8;
			
			/* Repeat and P/V flag on. */
			if(((command & 0020)!=0) && ((alu.f() & 4)!=0)) {
				setRegPair(PC,(short)(getRegPair(PC)-2));
				ts-=5;
			}
			
		/* CP(I/D)(R): 101RD001 */
		} else if((command & 0347) == 0241) {
			short bc = getRegPair(BC);
			short hl = getRegPair(HL);
			
			byte ihl=ula.getByte(hl);
			alu.cpi_cpd(ihl & 0xff, bc & 0xffff);
			bc--;
			
			if((command & 0010)==0) {
				hl++;
			} else {
				hl--;
			}
			
			setRegPair(BC, bc);
			setRegPair(HL, hl);
			ts-=8;
			
			/* Repeat and P/V flag on and Z flag off. */
			if(((command & 0020)!=0) && ((alu.f() & 4)!=0) &&
					((alu.f() & 0x40)==0)) {
				setRegPair(PC,(short)(getRegPair(PC)-2));
				ts-=5;
			}

		/* IN(I/D)(R): 101RD010 */
		} else if((command & 0347) == 0242) {
			short bc = getRegPair(BC);
			short hl = getRegPair(HL);
			
			ula.setByte(hl,ula.getIOByte(bc));
			alu.ini_ind(bc);
			bc-=0x100;

			if((command & 0010)==0) {
				hl++;
			} else {
				hl--;
			}

			setRegPair(BC, bc);
			setRegPair(HL, hl);
			ts-=8;
			
			/* Repeat and Z flag off. */
			if(((command & 0020)!=0) && ((alu.f() & 0x40)==0)) {
				setRegPair(PC,(short)(getRegPair(PC)-2));
				ts-=5;
			}

		/* OT(I/D)(R): 101RD011 */
		} else if((command & 0347) == 0243) {
			byte b = getReg(B);
			short hl = getRegPair(HL);
			
			ula.setIOByte(getRegPair(BC),ula.getByte(hl));
			alu.outi_outd(b);
			b--;
			
			if((command & 0010)==0) {
				hl++;
			} else {
				hl--;
			}
			
			setReg(B, b);
			setRegPair(HL, hl);
			ts-=8;
			
			/* Repeat and Z flag off. */
			if(((command & 0020)!=0) && ((alu.f() & 0x40)==0)) {
				setRegPair(PC,(short)(getRegPair(PC)-2));
				ts-=5;
			}

		} else {
			LOG.warn("Illegal EDh prefix command "+Hex.intToHex2(command)+
					" near "+Hex.intToHex4(getRegPair(PC))+".");
		}
	}
	
	private byte fetchByte() {
		short tmp=getRegPair(PC);
		setRegPair(PC,(short)(tmp+1));
		return ula.getByte(tmp);
	}
	
	private short fetchWord() {
		short tmp=getRegPair(PC);
		setRegPair(PC,(short)(tmp+2));
		return ula.getWord(tmp);
	}
	
	private void push(int regpairno) {
		short tmp=getRegPair(SP);
		tmp-=2;
		setRegPair(SP,tmp);
		ula.setWord(tmp,getRegPair(regpairno));
	}
	
	private void pop(int regpairno) {
		short tmp=getRegPair(SP);
		setRegPair(regpairno,ula.getWord(tmp));
		tmp+=2;
		setRegPair(SP,tmp);
	}
	
	private void ex(int rp1, int rp2) {
		short tmp=getRegPair(rp1);
		setRegPair(rp1,getRegPair(rp2));
		setRegPair(rp2,tmp);
	}
	
	private void rjump(byte dis) {
		setRegPair(PC,(short)(getRegPair(PC)+dis));
	}
	
	private boolean flagCond(int condType) {
		boolean cond=false;
		switch(condType) {
		case 0:
			cond=!testFlag(ZF);
			break;
		case 1:
			cond=testFlag(ZF);
			break;
		case 2:
			cond=!testFlag(CF);
			break;
		case 3:
			cond=testFlag(CF);
			break;
		case 4:
			cond=!testFlag(PVF);
			break;
		case 5:
			cond=testFlag(PVF);
			break;
		case 6:
			cond=!testFlag(SF);
			break;
		case 7:
			cond=testFlag(SF);
			break;
		}
		return cond;
	}
	
	private int reg1(int reg) {
		if((reg==H) & ixmode) {
			return XH;
		} else if ((reg==L) & ixmode) {
			return XL;
		} else if ((reg==H) & iymode) {
			return YH;
		} else if ((reg==L) & iymode) {
			return YL;
		} else {
			return reg;
		}
	}
	
	private int rp1(int rp) {
		if(rp==AF) {
			return SP;
		} else if((rp==HL) && ixmode) {
			return IX;
		} else if((rp==HL) && iymode) {
			return IY;
		} else {
			return rp;
		}
	}
	
	private int rp2(int rp) {
		if((rp==HL) && ixmode) {
			return IX;
		} else if((rp==HL) && iymode) {
			return IY;
		} else {
			return rp;
		}
	}
}
