/*
 * ClockImpl.java - Spectrum ticker implementation.
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

import org.apache.log4j.*;
import org.mn.z80util.disassembler.*;
import org.mn.z80util.z80.*;

public class SpectrumZ80Clock implements Runnable {
    private static Logger LOG=Logger.getLogger(SpectrumZ80Clock.class);
    
    private int interrupts, screenLine;
    private long startTime;
    private boolean approveUpdate;

    private SpectrumULA ula;
    public void setUla(SpectrumULA ula) {
        this.ula=ula;
    }

    private Z80 z80;
    public void setZ80(Z80 z80) {
        this.z80=z80;
    }
    
    private StatusPanel statusPanel;
    public void setStatusPanel(StatusPanel statusPanel) {
    	this.statusPanel=statusPanel;
    }

    private boolean paused=false;
    private boolean stepMode=false;
    public void stepMode() {
        stepMode=true;
        paused=false;
    }

    public void runMode() {
        stepMode=false;
        paused=false;
        statusPanel.setStatusPanelString("Running.");
        statusPanel.repaint();
    }
    
    public void reset() {
    	stepMode=false;
    	paused=false;
    	statusPanel.setStatusPanelString("Running.");
        statusPanel.repaint();
    	z80.reset();
    }

    /* Processor loop routines */
    
    /**
     * A single processor step. Should be as fast as possible if not
     * in stepping mode.
     */
    private void processorStep() {
    	if(stepMode) {
    		paused=true;
    		byte[] memory=ula.getMemory();
    		short pc=z80.getRegPair(Z80.PC);
    		DisasmResult dar=Disassembler.disassemble(memory,pc);
    		String cmdString=Hex.intToHex4(pc & 0xffff)+" "+dar.getHexDigits()+
    			dar.getCommand();
            LOG.debug(cmdString);
            statusPanel.setStatusPanelString("Stepping, next command: "+cmdString);
            statusPanel.repaint();
    	}

    	while(paused) {
            synchronized(this) {
                try {
                    wait();
                    ula.markScreenDirty();
                } catch (InterruptedException e) {
                    /* Do nothing */
                }
            }
        }
    	z80.executeNextCommand();
    }
    
    /**
     * Processor frame of approximately 224 T-States. In this period,
     * one screen line is drawn.
     */
    private void processorFrame() {
    	int ts=z80.getTStates();
    	for(z80.setTStates(ts+224);z80.getTStates()>0;) {
    		processorStep();
    	}
    	
    	if(approveUpdate) {
    		ula.updateRow(screenLine);
    	}
    }
    
    /**
     * Delays the interrupt period to be at least 1/50th of a second.
     */
    private void delay() {
    	long delayTime=(20000000L+startTime-System.nanoTime())/1000000L;
        if(delayTime>0) {
        	approveUpdate=true;
        	try {
        		Thread.sleep(delayTime);
            } catch (InterruptedException e) {
                LOG.warn("Main loop delay interrupted.");
            }
        } else {
        	approveUpdate=false;
        }
    }
    
    /**
     * An interrupt period of 1/50th of a second.
     */
    private void processorInterruptPeriod() {
    	startTime=System.nanoTime();
    	
    	/* Processing period begins */

        z80.interrupt();
        if((interrupts%25)==0) {
        	ula.changeFlashState();
        	LOG.debug("Flash state changed to: "+ula.getFlashState());
            ula.markScreenDirty();
        }

        for(screenLine=0; screenLine<312; screenLine++) {
        	processorFrame();
        }
        
        /* Processing period ends */
        
        delay();
    }

    /**
     * The main Z80 processing loop.
     */
    public void run() {
        LOG.info("Starting the machine.");
        ula.markScreenDirty();
        statusPanel.setStatusPanelString("Running.");
        statusPanel.repaint();
        for(interrupts=0; true; interrupts++) {
        	processorInterruptPeriod();
        }
    }
}
