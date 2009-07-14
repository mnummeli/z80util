package org.mn.z80util.spectrum.snapshots;

import java.io.*;

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

	public void read(InputStream is) {
		// TODO: SNA file reading.

	}

	public void write(OutputStream os) {
		// TODO: SNA file writing.

	}
}
