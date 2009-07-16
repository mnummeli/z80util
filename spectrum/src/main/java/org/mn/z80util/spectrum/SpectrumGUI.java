/*
 * GUI.java - The GUI for Spectrum emulator.
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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.*;
import org.mn.z80util.*;

public class SpectrumGUI {
	Logger LOG=Logger.getLogger(SpectrumGUI.class);
	
	/* The GUI components (main) */
	private JFrame GUIFrame;
	private JMenuBar GUIFrameMenuBar;
	private JMenu fileMenu, actionMenu, viewMenu, helpMenu;
	private JMenuItem loadItem, saveItem, exitItem, stepItem, continueItem,
		debuggerItem, aboutItem;
	
	/* The GUI components (debugger) */
	private JFrame debuggerFrame;
	private JPanel stepControlPanel, regsPanel, switchableRegsPanel, uniqueRegsPanel,
		assemblyListPanel, rangesPanel;
	private JButton stepButton, continueButton;
	private JTextField af, bc, de, hl, af_alt, bc_alt, de_alt, hl_alt,
		ix, iy, sp, pc, ir, im_iff;
	private JTable assemblyListTable;
	private JLabel startLabel, endLabel;
	private JTextField startAddr, endAddr;
	
	/* The event listener, which is also the main Spectrum controller for
	 * keyboard and menu options. */
	private SpectrumControls controller;
	public void setController(SpectrumControls controller) {
		this.controller=controller;
	}
	
	/* The screen */
	private SpectrumScreen screen;
	public void setScreen(SpectrumScreen screen) {
		this.screen=screen;
	}

	public void createAndShowGUI() {
		LOG.info("Creating the GUI.");

		/* Creation of the main GUI frame */
		GUIFrame=new JFrame();
		GUIFrame.setTitle("Jeccy - Spectrum emulator - (C) 2009, Mikko Nummelin");
		GUIFrame.setIconImage(LogoFactory.createLogo());
		GUIFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		/* Creation of the debugger GUI frame */
		debuggerFrame=new JFrame();
		debuggerFrame.setTitle("Z80 disassembler/debugger - (C) 2009, Mikko Nummelin");
		debuggerFrame.setIconImage(LogoFactory.createLogo());
		debuggerFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		
		/* Menu bar */
		GUIFrameMenuBar=new JMenuBar();
		GUIFrame.setJMenuBar(GUIFrameMenuBar);
		
		/* File menu */
		fileMenu=new JMenu("File");
		loadItem=new JMenuItem("Load", KeyEvent.VK_O);
		loadItem.addActionListener(controller);
		fileMenu.add(loadItem);
		saveItem=new JMenuItem("Save", KeyEvent.VK_S);
		saveItem.addActionListener(controller);
		fileMenu.add(saveItem);
		fileMenu.add(new JSeparator());
		exitItem=new JMenuItem("Exit", KeyEvent.VK_X);
		exitItem.addActionListener(controller);
		fileMenu.add(exitItem);
		GUIFrameMenuBar.add(fileMenu);
		
		/* Action menu */
		actionMenu=new JMenu("Action");
		stepItem=new JMenuItem("Step", KeyEvent.VK_T);
		stepItem.addActionListener(controller);
		actionMenu.add(stepItem);
		continueItem=new JMenuItem("Continue", KeyEvent.VK_C);
		continueItem.addActionListener(controller);
		actionMenu.add(continueItem);
		GUIFrameMenuBar.add(actionMenu);
		
		/* View menu */
		viewMenu=new JMenu("View");
		debuggerItem=new JMenuItem("Debugger", KeyEvent.VK_D);
		debuggerItem.addActionListener(controller);
		viewMenu.add(debuggerItem);
		GUIFrameMenuBar.add(viewMenu);
		
		/* Help menu */
		helpMenu=new JMenu("Help");
		aboutItem=new JMenuItem("About", KeyEvent.VK_A);
		aboutItem.addActionListener(controller);
		helpMenu.add(aboutItem);
		GUIFrameMenuBar.add(helpMenu);
		
		/* Debugger */
		stepControlPanel=new JPanel();
		stepButton=new JButton("Step");
		stepButton.addActionListener(controller);
		continueButton=new JButton("Continue");
		continueButton.addActionListener(controller);
		stepControlPanel.add(stepButton);
		stepControlPanel.add(continueButton);
		debuggerFrame.add(stepControlPanel);

		regsPanel=new JPanel();
		switchableRegsPanel=new JPanel(new GridLayout(4,2));
		af=new JTextField(); af_alt=new JTextField();
		switchableRegsPanel.add(af);
		switchableRegsPanel.add(af_alt);
		bc=new JTextField(); bc_alt=new JTextField();
		switchableRegsPanel.add(bc);
		switchableRegsPanel.add(bc_alt);
		de=new JTextField(); de_alt=new JTextField();
		switchableRegsPanel.add(de);
		switchableRegsPanel.add(de_alt);
		hl=new JTextField(); hl_alt=new JTextField();
		switchableRegsPanel.add(hl);
		switchableRegsPanel.add(hl_alt);
		regsPanel.add(switchableRegsPanel);
		
		uniqueRegsPanel=new JPanel();
		uniqueRegsPanel.setLayout(new GridLayout(6,1));
		ix=new JTextField();
		uniqueRegsPanel.add(ix);
		iy=new JTextField();
		uniqueRegsPanel.add(iy);
		sp=new JTextField();
		uniqueRegsPanel.add(sp);
		pc=new JTextField();
		uniqueRegsPanel.add(pc);
		ir=new JTextField();
		uniqueRegsPanel.add(ir);
		im_iff=new JTextField();
		uniqueRegsPanel.add(im_iff);
		regsPanel.add(uniqueRegsPanel);
		debuggerFrame.add(regsPanel);
		
		/* Finishing the setup and setting the main frame, but NOT debugger
		 * frame, visible */
		GUIFrame.add(screen);
		controller.setParentFrame(GUIFrame);
		controller.setDebuggerFrame(debuggerFrame);
		GUIFrame.addKeyListener(controller);
		GUIFrame.pack();
		GUIFrame.setResizable(false);
		GUIFrame.setVisible(true);
	}
}
