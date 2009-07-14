package org.mn.z80util.spectrum.snapshots;

import java.io.*;
import javax.swing.*;

import org.apache.log4j.*;

import org.mn.z80util.disassembler.*;
import org.mn.z80util.z80.*;

public class Z80Snapshot extends AbstractSpectrumSnapshot {
	private Logger LOG=Logger.getLogger(Z80Snapshot.class);
	private boolean isCompressed=false, isVersion2=false;
	
	public Z80Snapshot(InputStream is) {
		super();
		read(is);
	}
	
	public Z80Snapshot(String filename) {
		super();
		try {
			read(new FileInputStream(filename));
		} catch (FileNotFoundException e) {
			LOG.warn("Z80 snapshot file "+filename+" not found.");
		}
	}
	
	public void setV1RegisterValues(byte[] v1_header) {
		regs[Z80.A]=v1_header[0];
		regs[Z80.F]=v1_header[1];
		regs[Z80.C]=v1_header[2];
		regs[Z80.B]=v1_header[3];
		regs[Z80.L]=v1_header[4];
		regs[Z80.H]=v1_header[5];
		regs[Z80.PCL]=v1_header[6];
		regs[Z80.PCH]=v1_header[7];
		regs[Z80.SPL]=v1_header[8];
		regs[Z80.SPH]=v1_header[9];
		regs[Z80.I]=v1_header[10];
		regs[Z80.R]=(byte)(v1_header[11] & 0x7f);
		byte b=v1_header[12];
		if(b==0xff) {
			b=1;
		}
		isCompressed=((b & 0x20) != 0);
		regs[Z80.R] |= ((b & 1) << 7);
		border = ((b & 0xe) >> 1);
		regs[Z80.E]=v1_header[13];
		regs[Z80.D]=v1_header[14];
		regs[Z80.C_ALT]=v1_header[15];
		regs[Z80.B_ALT]=v1_header[16];
		regs[Z80.E_ALT]=v1_header[17];
		regs[Z80.D_ALT]=v1_header[18];
		regs[Z80.L_ALT]=v1_header[19];
		regs[Z80.H_ALT]=v1_header[20];
		regs[Z80.A_ALT]=v1_header[21];
		regs[Z80.F_ALT]=v1_header[22];
		regs[Z80.YL]=v1_header[23];
		regs[Z80.YH]=v1_header[24];
		regs[Z80.XL]=v1_header[25];
		regs[Z80.XH]=v1_header[26];
		regs[Z80.IM_IFF]=(byte)((v1_header[27] & 1) |			// IFF1
								((v1_header[28] & 1) << 1) |	// IFF2
								((v1_header[29] & 3) << 2));	// IM
	}

	/**
	 * Ported from Szeredi Miklos' Spectemu.
	 * 
	 * @param is				Input stream where data is read
	 * @param startAddr			Start address of target memory area
	 * @param length			Length of target memory area
	 * @param hasEndSignature	true if end signature 00 ED ED 00 is expected
	 */
	private void loadCompressedBlock(InputStream is, int startAddr,
			int length, boolean hasEndSignature) throws IOException {
		int j, p, end, times, ch;
		boolean last_ed;

		p = startAddr;
		end = startAddr+length;
		last_ed = false;

		while(p < end) {
			ch=is.read();

			if(ch != 0xED) {
				last_ed = false;
				memory[p++] = (byte)ch;
			} else if(last_ed) {
				last_ed = false;
				p--;
				times=is.read();
				if(times == 0) break;
				ch=is.read();
				if(p + times > end) {
					LOG.warn("Repeat parameter too large in snapshot.");
					times = (int) ((long) end - (long) p);
				}
				for(j = 0; j < times; j++) memory[p++] = (byte)ch;
			} else {
				last_ed = true;
				memory[p++] = (byte)0xED;
			}
		}

		if(hasEndSignature) {
			byte[] supposed_ending=new byte[4];
			is.read(supposed_ending);
			if(supposed_ending[0] != 0 || supposed_ending[1] != 0xED ||
					supposed_ending[2] != 0xED || supposed_ending[3] != 0)
				LOG.warn("Illegal ending of snapshot.");
		}
	}
	
	private void loadZ80Version2(InputStream is) {

		/* Loops through pages */
		while(true) {
			byte header[]=new byte[3];
			int hdrlen=0;
			try {
				hdrlen=is.read(header);
			} catch(IOException e) {
				LOG.error("Unable to read Z80 V2 block header length.");
				e.printStackTrace();
				System.exit(1);
			}
			if(hdrlen<3) {
				break;
			}

			int length = (header[0] & 0xff) | ((header[1] & 0xff) << 8);
			int pageno = header[2] & 0xff;
			LOG.info("Attempting to load Z80 V2 page: "+pageno+", length: "+length);

			/*
			 * If ever extending to other machines than Spectrum 48K, this hack
			 * should be replaced.
			 */
			int startAddr = ((pageno & 4) << 13) | ((pageno & 1) << 14) |
				((pageno & 8) << 11);
			LOG.info("Z80 V2 page start address is: "+Hex.intToHex4(startAddr));

			if(startAddr==0x0000) {
				LOG.warn("ROM block!");
			}

			if(length==0x10000) {
				LOG.info("Noncompressed block.");
				try {
					is.read(memory, startAddr, startAddr+0x4000);
				} catch(IOException e) {
					LOG.error("Unable to read Z80 V2 noncompressed block.");
					e.printStackTrace();
					System.exit(1);
				}
			} else {
				LOG.info("Compressed block.");
				try {
					loadCompressedBlock(is, startAddr, 0x4000, false);
				} catch (IOException e) {
					LOG.error("Unable to read Z80 V2 compressed block.");
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
	}

	public void read(InputStream is) {

		if(SwingUtilities.isEventDispatchThread()) {
			LOG.fatal("\n  Attempted to load Z80 snapshot from event dispatch thread.\n" +
					"This is not allowed because it is a possibly time-consuming task\n" +
					"and not thread safe with main emulator loop thread.");
			System.exit(1);
		}
		
		byte[] v1_header=new byte[30];
		try {
			is.read(v1_header);
		} catch (Exception e) {
			LOG.error("Unable to read Z80 V1 header.");
			e.printStackTrace();
			System.exit(1);
		}
		setV1RegisterValues(v1_header);
		if((regs[Z80.PC]==0) && (regs[Z80.PC+1]==0)) {
			isVersion2=true;
			LOG.info("Z80 file is of V2 format.");
			byte[] lenbytes=new byte[2];
			try {
				is.read(lenbytes);
			} catch (Exception e) {
				LOG.error("Unable to read Z80 V2/3 header length bytes.");
				e.printStackTrace();
				System.exit(1);
			}
			int length=(lenbytes[0] & 0xff) | ((lenbytes[1] & 0xff) << 8);
			byte[] v23_header=new byte[length];
			try {
				is.read(v23_header);
			} catch (Exception e) {
				LOG.error("Unable to read Z80 V2/3 extended header.");
				e.printStackTrace();
				System.exit(1);
			}
			regs[Z80.PCL]=v23_header[0];
			regs[Z80.PCH]=v23_header[1];
			loadZ80Version2(is);
		} else {
			LOG.info("Z80 file is of V1 format.");
			if(!isCompressed) {
				LOG.info("Z80 file is not compressed.");
				try {
					is.read(memory,0x4000,0xc000);
				} catch (IOException e) {
					LOG.error("Unable to read uncompressed Z80 snapshot memory contents.");
					e.printStackTrace();
					System.exit(1);
				}
			} else {
				LOG.info("Z80 file is compressed.");
				try {
					loadCompressedBlock(is, 0x4000, 0xc000, true);
				} catch (IOException e) {
					LOG.error("Unable to read compressed Z80 snapshot memory contents.");
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
	}

	public void write(OutputStream os) {

	}
}
