/*
 * MenuControls.java - Spectrum emulator GUI menu controller.
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

import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import org.mn.z80util.z80.*;

public class MenuControls implements ActionListener {
	private Logger LOG=Logger.getLogger(MenuControls.class);
	
	private SpectrumULA ula;
	public void setUla(SpectrumULA ula) {
		this.ula=ula;
	}

	private SpectrumZ80Clock clock;
	public void setClock(SpectrumZ80Clock clock) {
		this.clock=clock;
	}
	
	private Z80 z80;
	public void setZ80(Z80 z80) {
		this.z80=z80;
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equalsIgnoreCase("Open")) {
			JFileChooser chooser = new JFileChooser();
		    int returnVal = chooser.showOpenDialog(null);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
		    	try {
		    		File f=chooser.getSelectedFile();
		    		String ftype=Snapshots.fileType(f.getName());
		    		if(ftype.equals("z80")) {
		    			LOG.info("Loading Z80 file "+f.getName());
		    			Snapshots.loadZ80(new FileInputStream(f),z80,ula);
		    		} else if(ftype.equals("sna")) {
		    			LOG.info("Loading SNA file "+f.getName());
		    			Snapshots.loadSNA(new FileInputStream(f),z80,ula);
		    		}
				} catch (FileNotFoundException fnfe) {
					LOG.warn("Unable to open selected file");
				}
		    }
		} else if(e.getActionCommand().equalsIgnoreCase("Save")) {
			JFileChooser chooser = new JFileChooser();
		    int returnVal = chooser.showSaveDialog(null);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
		    	try {
		    		File f=chooser.getSelectedFile();
		    		String ftype=Snapshots.fileType(f.getName());
		    		if(ftype.equals("z80")) {
		    			LOG.info("Saving Z80 file "+f.getName());
		    			Snapshots.saveZ80(new FileOutputStream(f),z80,ula);
		    		} else if(ftype.equals("sna")) {
		    			LOG.info("Saving SNA file "+f.getName());
		    			Snapshots.saveSNA(new FileOutputStream(f),z80,ula);
		    		} else {
		    			LOG.warn("Attempted to save into unknown file type "+ftype+".");
		    		}
				} catch (FileNotFoundException fnfe) {
					LOG.warn("Unable to open selected file");
				}
		    }
		} else if (e.getActionCommand().equalsIgnoreCase("Exit")) {
			System.exit(0);
		}
	}
}
