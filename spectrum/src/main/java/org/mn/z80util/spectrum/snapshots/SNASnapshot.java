package org.mn.z80util.spectrum.snapshots;

import java.io.*;

import javax.swing.*;

import org.apache.log4j.*;
import org.mn.z80util.z80.*;
import org.mn.z80util.spectrum.*;

public class SNASnapshot extends AbstractSpectrumSnapshot {
	private Logger LOG=Logger.getLogger(SNASnapshot.class);
	
	public SNASnapshot(InputStream is) {
		super();
		read(is);
	}
	
	public SNASnapshot(String filename) {
		super();
		try {
			read(new FileInputStream(filename));
		} catch (FileNotFoundException e) {
			LOG.warn("SNA snapshot file "+filename+" not found.");
		}
	}
	
	public SNASnapshot(Z80 z80, SpectrumULA ula) {
		super(z80,ula);
	}

	public void setSNARegisterValues(byte[] sna_header) {
		regs[Z80.I]=sna_header[0];
		regs[Z80.L_ALT]=sna_header[1];
		regs[Z80.H_ALT]=sna_header[2];
		regs[Z80.E_ALT]=sna_header[3];
		regs[Z80.D_ALT]=sna_header[4];
		regs[Z80.C_ALT]=sna_header[5];
		regs[Z80.B_ALT]=sna_header[6];
		regs[Z80.F_ALT]=sna_header[7];
		regs[Z80.A_ALT]=sna_header[8];
		regs[Z80.L]=sna_header[9];
		regs[Z80.H]=sna_header[10];
		regs[Z80.E]=sna_header[11];
		regs[Z80.D]=sna_header[12];
		regs[Z80.C]=sna_header[13];
		regs[Z80.B]=sna_header[14];
		regs[Z80.YL]=sna_header[15];
		regs[Z80.YH]=sna_header[16];
		regs[Z80.XL]=sna_header[17];
		regs[Z80.XH]=sna_header[18];
		int im_iff = ((sna_header[19] & 2) == 0) ? 0 : 3;
		regs[Z80.R]=sna_header[20];
		regs[Z80.F]=sna_header[21];
		regs[Z80.A]=sna_header[22];
		regs[Z80.SPL]=sna_header[23];
		regs[Z80.SPH]=sna_header[24];
		regs[Z80.IM_IFF]=(byte)(im_iff | ((sna_header[25] & 3) << 2));
		border=(sna_header[26] & 7);
	}
	
	public void read(InputStream is) {
		if(SwingUtilities.isEventDispatchThread()) {
			LOG.fatal("\n  Attempted to load SNA snapshot from event dispatch thread.\n" +
					"This is not allowed, because it is a possibly time-consuming task\n" +
					"and not thread safe with main emulator loop thread.");
			System.exit(1);
		}
		
		byte[] sna_header=new byte[27];
		try {
			is.read(sna_header);
		} catch (Exception e) {
			LOG.error("Unable to read SNA header.");
			e.printStackTrace();
			System.exit(1);
		}

		setSNARegisterValues(sna_header);
		
		try {
			is.read(memory,0x4000,0xc000);
		} catch (IOException e) {
			LOG.error("Unable to read SNA snapshot memory contents.");
			e.printStackTrace();
			System.exit(1);
		}
		
		/* Pops the value of program counter from top of the stack. */
		int sp=(regs[Z80.SPL] & 0xff) | ((regs[Z80.SPH] & 0xff) << 8);
		regs[Z80.PCL]=memory[sp];
		regs[Z80.PCH]=memory[sp+1];
		sp+=2;
		regs[Z80.SPL]=(byte)(sp & 0xff);
		regs[Z80.SPH]=(byte)((sp & 0xff00) >> 8);
	}

	public void write(OutputStream os) {
		// TODO: SNA file writing.
	}
}
