package org.mn.z80util.spectrum;

import java.awt.event.*;
import javax.swing.*;
import java.io.*;

import org.apache.log4j.*;

public class SpectrumController implements KeyListener, ActionListener {
	private Logger LOG=Logger.getLogger(SpectrumKeyboard.class);
	
	private SpectrumZ80Clock clock;
	public void setClock(SpectrumZ80Clock clock) {
		this.clock=clock;
	}
	
	/* KeyListener, Spectrum keyboard */

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
		
		SpectrumULA ula=clock.getZ80().getUla();
		
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
		case KeyEvent.VK_F1:
			if(eventType) {
				clock.stepMode();
				synchronized(clock) {
					clock.notifyAll();
				}
			}
			break;
		case KeyEvent.VK_F2:
			if(eventType) {
				clock.runMode();
				synchronized(clock) {
					clock.notifyAll();
				}
			}
			break;
		case KeyEvent.VK_F3:
			if(eventType) {
				clock.reset();
				synchronized(clock) {
					clock.notifyAll();
				}
			}
			break;
		case KeyEvent.VK_ESCAPE:
		case KeyEvent.VK_F10:
			System.exit(0);
		case KeyEvent.VK_BACK_SPACE:
			ula.setKeyData(0,0,eventType);	// ERASE
			ula.setKeyData(4,0,eventType);
			break;
		}
	}

	public void keyTyped(KeyEvent e) {
		/* Do nothing */
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
