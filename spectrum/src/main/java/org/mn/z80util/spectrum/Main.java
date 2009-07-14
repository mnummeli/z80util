/*
 * Main.java - Main class for Spectrum emulator.
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

import javax.swing.*;

import org.apache.log4j.*;

/* Z80 related imports */
import org.mn.z80util.spectrum.snapshots.*;
import org.mn.z80util.z80.*;
import org.mn.z80util.z80.qaop.*; // In reserve if one wants to wire the processor differently
import org.mn.z80util.z80.yaze.*;

public class Main {
	private final static String COPYRIGHT_NOTICE=
		"Jeccy - a Sinclair ZX Spectrum 48K emulator\n"+
		"(C) 2009, Mikko Nummelin, <mikko.nummelin@tkk.fi>\n"+
		"Jeccy is free software and comes with ABSOLUTELY NO WARRANTY.\n";
	
	private static Logger LOG=Logger.getLogger(Main.class);
	private static String[] args;
	
	private static SpectrumZ80Clock clock;
	private static SpectrumScreen scr;
	private static SpectrumControls controller;
	
	public static void main( String[] args ) {
		System.out.println(COPYRIGHT_NOTICE);
		Main.args=args;

		/* Creates basic components */
		Z80 z80                 = new YazeBasedZ80Impl();
		// Z80 z80				= new QaopZ80Impl();
		SpectrumULA ula         = new SpectrumULA();
		clock  					= new SpectrumZ80Clock();
		scr  					= new SpectrumScreen();
		controller 				= new SpectrumControls();

		/* Wires the components together */
		z80.setUla(ula);
		ula.setScreen(scr);
		clock.setZ80(z80);
		clock.setUla(ula);
		scr.setUla(ula);
		controller.setUla(ula);
		controller.setZ80(z80);
		controller.setClock(clock);
				
		/* Resets ULA and Z80 processor */
		ula.reset();
		z80.reset();
				
		/* Parses arguments and loads appropriate ROM and snapshots */
		LOG.info("Parsing the arguments");
		String ROMFileName="48.rom", Z80FileName=null, SNAFileName=null;
		for(int i=0;i<Main.args.length;i++) {
			LOG.info("Argument type: "+Main.args[i]);
			if(Main.args[i].equals("-rom")) {
				ROMFileName=Main.args[++i];
				LOG.info("ROM file name: "+ROMFileName);
			} else if(Main.args[i].equals("-z80")) {
				Z80FileName=Main.args[++i];
				LOG.info("Z80 file name: "+Z80FileName);
			} else if(Main.args[i].equals("-sna")) {
				SNAFileName=Main.args[++i];
				LOG.info("SNA file name: "+SNAFileName);
			}
		}
		ula.loadROM(Main.class.getResourceAsStream("/"+ROMFileName));
		if(Z80FileName!=null) {
			AbstractSpectrumSnapshot snsh=
				new Z80Snapshot(Z80FileName);
			snsh.write(z80,ula);
		} else if(SNAFileName!=null) {
			AbstractSpectrumSnapshot snsh=
				new SNASnapshot(SNAFileName);
			snsh.write(z80,ula);
		}

		SwingUtilities.invokeLater(new Runnable() {

			/**
			 * Creates the GUI in the event dispatching thread as recommended
			 * by Sun.
			 */
			public void run() {		
				new GUI(controller, scr);

				/* Starts the machine */
				new Thread(clock, "Spectrum").start();
			}
		});
    }
}
