package org.mn.z80util.spectrum.snapshots;

import java.io.*;

import org.apache.log4j.*;
import org.mn.z80util.z80.*;

public class Z80Snapshot extends AbstractSpectrumSnapshot {
	private Logger LOG=Logger.getLogger(Z80Snapshot.class);
	
	public Z80Snapshot(InputStream is) {
		super();
		read(is);
	}
	
	public Z80Snapshot(String filename) {
		super();
		try {
			read(new FileInputStream(filename));
		} catch (FileNotFoundException e) {
			LOG.warn("SNA snapshot file "+filename+" not found.");
		}
	}

	public void read(InputStream is) {
		// TODO: SNA file reading.

	}

	public void write(OutputStream os) {
		// TODO: SNA file writing.

	}
}
