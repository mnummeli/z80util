/*
 * Snapshots.java - Z80 and Spectrum snapshot utilities.
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

package org.mn.z80util.spectrum;

import java.io.*;

import org.apache.log4j.Logger;

import org.mn.z80util.disassembler.*;
import org.mn.z80util.z80.*;

public final class Snapshots {
	private static Logger LOG=Logger.getLogger(Snapshots.class);
	
	private static byte[] memory;
	private static int memptr;
	private static boolean isCompressed, isVersion2, prev_ed;
	
	public static String fileType(String fileName) {
		String[] tokens=fileName.split("[/\\.]");
		return tokens[tokens.length-1].toLowerCase();
	}
	
	public static void loadROM(InputStream is, Z80 z80, SpectrumULA ula) {
		memory=ula.getMemory();
		try {
			is.read(memory,0,0x4000);
		} catch (NullPointerException npexc) {
			LOG.error("ROM file not found.");
			npexc.printStackTrace();
			System.exit(1);
		} catch (IOException ioexc) {
			LOG.error("Unable to load ROM.");
			ioexc.printStackTrace();
			System.exit(1);
		}
		LOG.info("ROM successfully loaded.");
	}

	private static void failZ80(Exception e) {
		LOG.error("Unable to read snapshot.");
		e.printStackTrace();
		System.exit(1);
	}
	
	private static void failWriteZ80(Exception e) {
		LOG.error("Unable to write snapshot.");
		e.printStackTrace();
		System.exit(1);
	}
	
	private static void loadZ80Header(InputStream is, Z80 z80, SpectrumULA ula) {
		byte[] header=new byte[30];
		memory=ula.getMemory();
		isCompressed=false;
		try {
			is.read(header);
		} catch (IOException e) {
			failZ80(e);
		}
		z80.setReg(Z80.A,header[0]);
		z80.setReg(Z80.F,header[1]);
		z80.setReg(Z80.C,header[2]);
		z80.setReg(Z80.B,header[3]);
		z80.setReg(Z80.L,header[4]);
		z80.setReg(Z80.H,header[5]);
		z80.setReg(Z80.PCL,header[6]);
		z80.setReg(Z80.PCH,header[7]);
		z80.setReg(Z80.SPL,header[8]);
		z80.setReg(Z80.SPH,header[9]);
		z80.setReg(Z80.I,header[10]);
		z80.setReg(Z80.R,(byte)(header[11]&0x7f));
		byte b12=header[12];
		if(b12==0xff) { b12=1; }
		if((b12&0x1)!=0) {
			byte r=z80.getReg(Z80.R);
			z80.setReg(Z80.R,(byte)(r|0x80));
		}
		ula.setBorder((b12&0xe)>>1);
		if(isCompressed = ((b12 & 0x20) != 0)) {
			LOG.info("Z80 file is compressed.");
		}
		z80.setReg(Z80.E,header[13]);
		z80.setReg(Z80.D,header[14]);
		z80.setReg(Z80.C_ALT,header[15]);
		z80.setReg(Z80.B_ALT,header[16]);
		z80.setReg(Z80.E_ALT,header[17]);
		z80.setReg(Z80.D_ALT,header[18]);
		z80.setReg(Z80.L_ALT,header[19]);
		z80.setReg(Z80.H_ALT,header[20]);
		z80.setReg(Z80.A_ALT,header[21]);
		z80.setReg(Z80.F_ALT,header[22]);
		z80.setReg(Z80.YL,header[23]);
		z80.setReg(Z80.YH,header[24]);
		z80.setReg(Z80.XL,header[25]);
		z80.setReg(Z80.XH,header[26]);
		z80.setReg(Z80.IM_IFF,(byte)(header[27] | (header[28] << 1) |
				(header[29] <<2 )));
		/*
		 * header[29] other bits (omitted) are:
		 * 2: Issue 2
		 * 3: Double interrupt frequency
		 * 4-5: 1=High video sync, 3=Low video sync
		 * 6-7: joystick, 0=Cursor, 1=Kempston
		 */
		
		if(z80.getRegPair(Z80.PC)==0x0000) {
			LOG.info("Type 2 Z80 file detected, attempting to parse relevant part of it.");
			try {
				byte[] len=new byte[2];
				is.read(len);
				int length=(len[0] & 0xff)|((len[1] & 0xff)<<8);
				LOG.info("Extra header length is: "+length);
				byte[] extraheader=new byte[length];
				is.read(extraheader);
				z80.setReg(Z80.PCL,extraheader[0]);
				z80.setReg(Z80.PCH,extraheader[1]);
				LOG.info("PC = "+
						Hex.intToHex4(z80.getRegPair(Z80.PC)));
				isVersion2=true;
			} catch (IOException e) {
				failZ80(e);
			}
		}
	}

	/**
	 * The Z80 compressed block algorithm. Two EDh values in a row followed by
	 * count and byte means count times byte written to the memory.
	 */
	private static void loadZ80CompressedBlock(InputStream is) {
		try {
			LOG.debug("Memory pointer is: "+Hex.intToHex4(memptr));
			
			byte[] repeat_params=new byte[2];
			
			/* Base case, the byte is just read into memory. */
			is.read(memory,memptr,1);
			LOG.debug("Byte "+Hex.intToHex2(memory[memptr])+" -> "+
					Hex.intToHex4(memptr));
			
			/* In case of current and previous EDh, enter the special routine. */
			if(((memory[memptr]&0xff)==0xed) && prev_ed) {

				/* The first EDh written must be reverted. */
				memptr--;
				is.read(repeat_params);
				int counter=(repeat_params[0]&0xff);
				LOG.debug("Bytes "+Hex.intToHex2(repeat_params[1])+" x "+
						counter+" -> "+Hex.intToHex4(memptr));
				
				for(int i=0;i<counter;i++) {
					memory[memptr++]=repeat_params[1];
				}
				 
				/* The last repeat increment must be reverted and previous EDh
				 * property assumed false. */
				memptr--;
				prev_ed=false;

			} else {
				
				/* Set the previous EDh property if necessary. */
				prev_ed = ((memory[memptr]&0xff)==0xed);
			}
		} catch(IOException e) {
			failZ80(e);
		}
	}
	
	private static void checkFinalSignature(InputStream is) {
		byte[] final_signature=new byte[4];

		try {
			is.read(final_signature);
		} catch (IOException e) {
			failZ80(e);
		}

		if(((final_signature[0]&0xff)!=0x00)||
				((final_signature[1]&0xff)!=0xed)||
				((final_signature[2]&0xff)!=0xed)||
				((final_signature[3]&0xff)!=0x00)) {
			LOG.warn("Malformed Z80 file, end signature not OK.");
		}
	}
	
	private static void loadZ80Compressed(InputStream is) {
		prev_ed=false;
		for(memptr=0x4000; memptr<0x10000; memptr++) {
			LOG.debug("Memory pointer before loading compressed block is: "+
					Hex.intToHex4(memptr));
			loadZ80CompressedBlock(is);
		}
		LOG.debug("Memory pointer after leaving compressed loading loop is: "+
				Hex.intToHex4(memptr));
		checkFinalSignature(is);
	}
	
	private static void loadZ80Version2(InputStream is) {

		/* Loops through pages */
		while(true) {
			byte header[]=new byte[3];
			int hdrlen=0;
			try {
				hdrlen=is.read(header);
			} catch(IOException e) {
				failZ80(e);	
			}
			if(hdrlen<3) {
				break;
			}

			int length = (header[0] & 0xff) | ((header[1] & 0xff) << 8);
			int pageno = header[2] & 0xff;
			LOG.info("Attempting to load page: "+pageno+", length: "+length);

			/*
			 * If ever extending to other machines than Spectrum 48K, this hack
			 * should be replaced.
			 */
			int startAddr = ((pageno & 4) << 13) | ((pageno & 1) << 14) |
				((pageno & 8) << 11);
			LOG.info("Page start address is: "+Hex.intToHex4(startAddr));

			if(startAddr==0x0000) {
				LOG.warn("ROM block!");
			}

			if(length==0x10000) {
				LOG.info("Noncompressed block.");
				try {
					is.read(memory, startAddr, startAddr+0x4000);
				} catch(IOException e) {
					failZ80(e);	
				}
			} else {
				LOG.info("Compressed block.");
				for(memptr=startAddr; memptr < startAddr+0x4000; memptr++) {
					LOG.debug("Memory pointer before loading compressed block is: "+
						Hex.intToHex4(memptr));
					loadZ80CompressedBlock(is);
				}
				LOG.debug("Memory pointer after leaving compressed loading loop is: "+
						Hex.intToHex4(memptr));				
			}
		}
	}
	
	private static void loadZ80Contents(InputStream is) {
		if(isCompressed) {
			loadZ80Compressed(is);
		} else if(isVersion2) {
			loadZ80Version2(is);
		} else {
			try {
				is.read(memory,0x4000,0xc000);
			} catch (IOException e) {
				failZ80(e);
			}
		}
	}
	
	/**
	 * Loads Z80 snapshot file into memory and processor.
	 * @param is	Snapshot input stream
	 */
	public static void loadZ80(InputStream is, Z80 z80, SpectrumULA ula) {
		synchronized(z80) {
			z80.reset();
			ula.clearKeyData();
			
			loadZ80Header(is,z80,ula);
			loadZ80Contents(is);

			try {
				is.close();
			} catch (IOException e) {
				failZ80(e);
			}

			ula.markScreenDirty();
			LOG.info("Z80 snapshot loaded successfully.");
		}
	}
	
	public static void loadZ80(String filename, Z80 z80, SpectrumULA ula) {
		try {
			loadZ80(new FileInputStream(filename), z80, ula);
		} catch (FileNotFoundException e) {
			failZ80(e);
		}
	}

	private static void loadSNAHeader(InputStream is, Z80 z80, SpectrumULA ula) {
		byte[] header=new byte[27];
		memory=ula.getMemory();
		try {
			is.read(header);
		} catch (IOException e) {
			failZ80(e);
		}
		z80.setReg(Z80.I,header[0]);
		z80.setReg(Z80.L_ALT,header[1]);
		z80.setReg(Z80.H_ALT,header[2]);
		z80.setReg(Z80.E_ALT,header[3]);
		z80.setReg(Z80.D_ALT,header[4]);
		z80.setReg(Z80.C_ALT,header[5]);
		z80.setReg(Z80.B_ALT,header[6]);
		z80.setReg(Z80.F_ALT,header[7]);
		z80.setReg(Z80.A_ALT,header[8]);
		z80.setReg(Z80.L,header[9]);
		z80.setReg(Z80.H,header[10]);
		z80.setReg(Z80.E,header[11]);
		z80.setReg(Z80.D,header[12]);
		z80.setReg(Z80.C,header[13]);
		z80.setReg(Z80.B,header[14]);
		z80.setReg(Z80.YL,header[15]);
		z80.setReg(Z80.YH,header[16]);
		z80.setReg(Z80.XL,header[17]);
		z80.setReg(Z80.XH,header[18]);
		int im_iff;
		im_iff = (header[19]!=0) ? 2 : 0;
		z80.setReg(Z80.R,header[20]);
		z80.setReg(Z80.F,header[21]);
		z80.setReg(Z80.A,header[22]);
		z80.setReg(Z80.SPL,header[23]);
		z80.setReg(Z80.SPH,header[24]);
		im_iff=(im_iff & ~0xc) | (header[25]<<2);
		z80.setReg(Z80.IM_IFF,(byte)(im_iff & 0xf));
		ula.setBorder(header[26]);
	}

	/**
	 * Loads Z80 snapshot file into memory and processor.
	 * @param is	Snapshot input stream
	 */
	public static void loadSNA(InputStream is, Z80 z80, SpectrumULA ula) {
		synchronized(z80) {
			z80.reset();
			ula.clearKeyData();
		
			loadSNAHeader(is,z80,ula);
			try {
				is.read(memory,0x4000,0xc000);
				is.close();
			} catch (IOException e) {
				failZ80(e);
			}

			/* Performs 'RETN' */
			int im_iff = z80.getReg(Z80.IM_IFF);
			im_iff = (im_iff &~ 1) | ((im_iff & 2)>>1);
			z80.setReg(Z80.IM_IFF, (byte)im_iff);
			short tmp=z80.getRegPair(Z80.SP);
			z80.setRegPair(Z80.PC,ula.getWord(tmp));
			z80.setRegPair(Z80.SP,(short)(tmp+2));
			
			ula.markScreenDirty();
			LOG.info("Z80 (SNA) snapshot loaded successfully.");
			LOG.info("Loaded snapshot should have PC="+
					Hex.intToHex4(z80.getRegPair(Z80.PC))+
					", SP="+Hex.intToHex4(z80.getRegPair(Z80.SP)));
		}
	}
	
	public static void loadSNA(String filename, Z80 z80, SpectrumULA ula) {
		try {
			loadSNA(new FileInputStream(filename), z80, ula);
		} catch (FileNotFoundException e) {
			failZ80(e);
		}
	}
	
	/* TODO: should be implemented after testing SNA save & load */
	public static void saveZ80Header(OutputStream os, Z80 z80, SpectrumULA ula) {
		LOG.info("Not implemented.");
	}
	
	/* TODO: should be implemented after testing SNA save & load */
	public static void saveZ80Contents(OutputStream os) {
		LOG.info("Not implemented.");
	}
	
	public static void saveZ80(OutputStream os, Z80 z80, SpectrumULA ula) {
		synchronized(z80) {
			saveZ80Header(os,z80,ula);
			saveZ80Contents(os);

			try {
				os.close();
			} catch (IOException e) {
				failWriteZ80(e);
			}
			LOG.info("Z80 snapshot saved successfully.");
		}
	}
	
	private static void saveSNAHeader(OutputStream os, Z80 z80, SpectrumULA ula) {
		byte[] header=new byte[27];
		memory=ula.getMemory();
		
		/* To ensure that correct address passes as SP. */
		short sp=(short)z80.getRegPair(Z80.SP);
		z80.setRegPair(Z80.SP,(short)(sp-2));
		
		header[0]=z80.getReg(Z80.I);
		header[1]=z80.getReg(Z80.L_ALT);
		header[2]=z80.getReg(Z80.H_ALT);
		header[3]=z80.getReg(Z80.E_ALT);
		header[4]=z80.getReg(Z80.D_ALT);
		header[5]=z80.getReg(Z80.C_ALT);
		header[6]=z80.getReg(Z80.B_ALT);
		header[7]=z80.getReg(Z80.F_ALT);
		header[8]=z80.getReg(Z80.A_ALT);
		header[9]=z80.getReg(Z80.L);
		header[10]=z80.getReg(Z80.H);
		header[11]=z80.getReg(Z80.E);
		header[12]=z80.getReg(Z80.D);
		header[13]=z80.getReg(Z80.C);
		header[14]=z80.getReg(Z80.B);
		header[15]=z80.getReg(Z80.YL);
		header[16]=z80.getReg(Z80.YH);
		header[17]=z80.getReg(Z80.XL);
		header[18]=z80.getReg(Z80.XH);
		header[19]=(byte)(z80.getReg(Z80.IM_IFF) & 2);
		header[20]=z80.getReg(Z80.R);
		header[21]=z80.getReg(Z80.F);
		header[22]=z80.getReg(Z80.A);
		header[23]=z80.getReg(Z80.SPL);
		header[24]=z80.getReg(Z80.SPH);
		header[25]=(byte)(z80.getReg(Z80.IM_IFF) & 3);
		header[26]=(byte)ula.getBorder();
		try {
			os.write(header);
		} catch (IOException e) {
			failWriteZ80(e);
		}
	}
	
	public static void saveSNA(OutputStream os, Z80 z80, SpectrumULA ula) {
		synchronized(z80) {
			saveSNAHeader(os,z80,ula);
			try {
				os.write(memory,0x4000,0xc000);
				os.close();
			} catch (IOException e) {
				failWriteZ80(e);
			}

			/* Stores current value of PC into top of stack. */
			short sp=z80.getRegPair(Z80.SP), pc=z80.getRegPair(Z80.PC);
			ula.setWord(sp,pc);
			z80.setRegPair(Z80.SP,(short)sp);

			LOG.info("Z80 (SNA) snapshot saved successfully.");
			LOG.info("Saved snapshot should have PC="+
					Hex.intToHex4(z80.getRegPair(Z80.PC))+
					", SP="+Hex.intToHex4(z80.getRegPair(Z80.SP)));
		}
	}
}
