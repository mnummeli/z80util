/*
 * SpectrumControls.java - Spectrum emulator keyboard and GUI controller class.
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

import org.apache.log4j.*;

import org.mn.z80util.spectrum.snapshots.Snapshots;

public class SpectrumControls implements KeyListener, ActionListener {
private Logger LOG=Logger.getLogger(SpectrumControls.class);
	
	private SpectrumULA ula;
	public void setUla(SpectrumULA ula) {
		this.ula=ula;
	}
	
	private SpectrumZ80Clock clock;
	public void setClock(SpectrumZ80Clock clock) {
		this.clock=clock;
	}
	
	private JFrame parentFrame;
	public void setParentFrame(JFrame parentFrame) {
		this.parentFrame=parentFrame;
	}

	public void keyPressed(KeyEvent e) {
		LOG.trace("Key pressed.");
		processKeyEvent(e, true);
	}
 
	public void keyReleased(KeyEvent e) {
		LOG.trace("Key released.");
		processKeyEvent(e, false);
	}

	/**
	 * Processes key presses and releases and sends them to the ULA.
	 * From Spectrum configuration, note that:
	 * 
	 * <ul>
	 *   <li>Left SHIFT stands for CAPS SHIFT.</li>
	 *   <li>Right SHIFT stands for BREAK SPACE.</li>
	 *   <li>BACKSPACE stands for ERASE, i.e. 0 CAPS SHIFT and 0 combined.</li>
	 *   <li>COMMA, PERIOD, MINUS, ALT and ALT-GR stand for SYMBOL SHIFT.</li>
	 * </ul>
	 * 
	 * @param e	The key event
	 * @param eventType	true if key pressed, false if key released.
	 */
	private void processKeyEvent(KeyEvent e, boolean eventType) {
		LOG.trace(e);
		switch(e.getKeyCode()) {
		case KeyEvent.VK_SHIFT:
			if(e.getKeyLocation()==KeyEvent.KEY_LOCATION_LEFT) {
				ula.setKeyData(0,0,eventType); // CAPS SHIFT
			} else if(e.getKeyLocation()==KeyEvent.KEY_LOCATION_RIGHT) {
				ula.setKeyData(7,0,eventType); // BREAK SPACE
			}
			break;
		case KeyEvent.VK_Z:
			ula.setKeyData(0,1,eventType);
			break;
		case KeyEvent.VK_X:
			ula.setKeyData(0,2,eventType);
			break;
		case KeyEvent.VK_C:
			ula.setKeyData(0,3,eventType);
			break;
		case KeyEvent.VK_V:
			ula.setKeyData(0,4,eventType);
			break;
		case KeyEvent.VK_A:
			ula.setKeyData(1,0,eventType);
			break;
		case KeyEvent.VK_S:
			ula.setKeyData(1,1,eventType);
			break;
		case KeyEvent.VK_D:
			ula.setKeyData(1,2,eventType);
			break;
		case KeyEvent.VK_F:
			ula.setKeyData(1,3,eventType);
			break;
		case KeyEvent.VK_G:
			ula.setKeyData(1,4,eventType);
			break;
		case KeyEvent.VK_Q:
			ula.setKeyData(2,0,eventType);
			break;
		case KeyEvent.VK_W:
			ula.setKeyData(2,1,eventType);
			break;
		case KeyEvent.VK_E:
			ula.setKeyData(2,2,eventType);
			break;
		case KeyEvent.VK_R:
			ula.setKeyData(2,3,eventType);
			break;
		case KeyEvent.VK_T:
			ula.setKeyData(2,4,eventType);
			break;
		case KeyEvent.VK_1:
			ula.setKeyData(3,0,eventType);
			break;
		case KeyEvent.VK_2:
			ula.setKeyData(3,1,eventType);
			break;
		case KeyEvent.VK_3:
			ula.setKeyData(3,2,eventType);
			break;
		case KeyEvent.VK_4:
			ula.setKeyData(3,3,eventType);
			break;
		case KeyEvent.VK_5:
			ula.setKeyData(3,4,eventType);
			break;
		case KeyEvent.VK_0:
			ula.setKeyData(4,0,eventType);
			break;
		case KeyEvent.VK_9:
			ula.setKeyData(4,1,eventType);
			break;
		case KeyEvent.VK_8:
			ula.setKeyData(4,2,eventType);
			break;
		case KeyEvent.VK_7:
			ula.setKeyData(4,3,eventType);
			break;
		case KeyEvent.VK_6:
			ula.setKeyData(4,4,eventType);
			break;
		case KeyEvent.VK_P:
			ula.setKeyData(5,0,eventType);
			break;
		case KeyEvent.VK_O:
			ula.setKeyData(5,1,eventType);
			break;
		case KeyEvent.VK_I:
			ula.setKeyData(5,2,eventType);
			break;
		case KeyEvent.VK_U:
			ula.setKeyData(5,3,eventType);
			break;
		case KeyEvent.VK_Y:
			ula.setKeyData(5,4,eventType);
			break;
		case KeyEvent.VK_ENTER:
			ula.setKeyData(6,0,eventType);
			break;
		case KeyEvent.VK_L:
			ula.setKeyData(6,1,eventType);
			break;
		case KeyEvent.VK_K:
			ula.setKeyData(6,2,eventType);
			break;
		case KeyEvent.VK_J:
			ula.setKeyData(6,3,eventType);
			break;
		case KeyEvent.VK_H:
			ula.setKeyData(6,4,eventType);
			break;
		case KeyEvent.VK_SPACE:
			ula.setKeyData(7,0,eventType);	// BREAK SPACE
			break;
		case KeyEvent.VK_COMMA:
		case KeyEvent.VK_PERIOD:
		case KeyEvent.VK_MINUS:
		case KeyEvent.VK_ALT:
		case KeyEvent.VK_ALT_GRAPH:
			ula.setKeyData(7,1,eventType);	// SYMBOL SHIFT
			break;
		case KeyEvent.VK_M:
			ula.setKeyData(7,2,eventType);
			break;
		case KeyEvent.VK_N:
			ula.setKeyData(7,3,eventType);
			break;
		case KeyEvent.VK_B:
			ula.setKeyData(7,4,eventType);
			break;
		case KeyEvent.VK_BACK_SPACE:
			ula.setKeyData(0,0,eventType);	// ERASE
			ula.setKeyData(4,0,eventType);
			break;
			
		/* Function keys */
		case KeyEvent.VK_F1:
			showAboutMessage();
			break;
		case KeyEvent.VK_F2:
			handleSaveDialog();
			synchronized(clock) {
				clock.notifyAll();
			}
			break;
		case KeyEvent.VK_F3:
			handleLoadDialog();
			synchronized(clock) {
				clock.notifyAll();
			}
			break;
		case KeyEvent.VK_F5:
			if(eventType) {
				clock.reset();
				synchronized(clock) {
					clock.notifyAll();
				}
			}
			break;
		case KeyEvent.VK_F10:
			System.exit(0);
		case KeyEvent.VK_F11:
			if(eventType) {
				clock.stepMode();
				synchronized(clock) {
					clock.notifyAll();
				}
			}
			break;
		case KeyEvent.VK_F12:
			if(eventType) {
				clock.runMode();
				synchronized(clock) {
					clock.notifyAll();
				}
			}
			break;
		}
	}

	public void keyTyped(KeyEvent e) {
		/* Do nothing */
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equalsIgnoreCase("Load")) {
			handleLoadDialog();
			synchronized(clock) {
				clock.notifyAll();
			}
		} else if(e.getActionCommand().equalsIgnoreCase("Save")) {
			handleSaveDialog();
			synchronized(clock) {
				clock.notifyAll();
			}
		} else if(e.getActionCommand().equalsIgnoreCase("Step")) {
			clock.stepMode();
			synchronized(clock) {
				clock.notifyAll();
			}
		} else if(e.getActionCommand().equalsIgnoreCase("Continue")) {
			clock.runMode();
			synchronized(clock) {
				clock.notifyAll();
			}
		} else if (e.getActionCommand().equalsIgnoreCase("Exit")) {
			System.exit(0);
		} else if (e.getActionCommand().equalsIgnoreCase("About")) {
			showAboutMessage();
		}
	}
	
	private void handleLoadDialog() {
		JFileChooser chooser = new JFileChooser();
	    int returnVal = chooser.showOpenDialog(null);
	    if(returnVal == JFileChooser.APPROVE_OPTION) {
	    	try {
	    		File f=chooser.getSelectedFile();
	    		String ftype=Snapshots.fileType(f.getName());
	    		
	    		/*
	    		 * The actual snapshot loading will take place in the main
	    		 * thread (SpectrumZ80Clock), not here in event dispatch thread.
	    		 */
	    		if(ftype.equals("z80")) {
	    			clock.setSnapshotImportFileType(SpectrumZ80Clock.Z80_FILE);
	    			clock.setSnapshotImportFile(new FileInputStream(f));
	    		} else if(ftype.equals("sna")) {
	    			clock.setSnapshotImportFileType(SpectrumZ80Clock.SNA_FILE);
	    			clock.setSnapshotImportFile(new FileInputStream(f));
	    		}
			} catch (FileNotFoundException fnfe) {
				LOG.warn("Unable to load selected file");
			}
	    }
	}
	
	private void handleSaveDialog() {
		JFileChooser chooser = new JFileChooser();
	    int returnVal = chooser.showSaveDialog(null);
	    if(returnVal == JFileChooser.APPROVE_OPTION) {
	    	try {
	    		File f=chooser.getSelectedFile();
	    		String ftype=Snapshots.fileType(f.getName());
	    		if(ftype.equals("z80")) {
	    			clock.setSnapshotExportFileType(SpectrumZ80Clock.Z80_FILE);
	    			clock.setSnapshotExportFile(new FileOutputStream(f));
	    		} else if(ftype.equals("sna")) {
	    			clock.setSnapshotExportFileType(SpectrumZ80Clock.SNA_FILE);
	    			clock.setSnapshotExportFile(new FileOutputStream(f));
	    		} else {
	    			LOG.warn("Attempted to save into unknown file type "+ftype+".");
	    		}
			} catch (FileNotFoundException fnfe) {
				LOG.warn("Unable to open selected file");
			}
	    }
	}
	
	private void showAboutMessage() {
		JOptionPane.showMessageDialog(parentFrame,
				"Jeccy - Spectrum emulator - (C) 2009, Mikko Nummelin\n"+
				"This program is free software and comes with ABSOLUTELY NO WARRANTY.",
				"About",
				JOptionPane.INFORMATION_MESSAGE);
	}
}
