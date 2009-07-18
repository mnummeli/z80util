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
import javax.swing.table.*;

import java.util.*;

import org.apache.log4j.*;
import org.mn.z80util.*;
import org.mn.z80util.disassembler.*;
import org.mn.z80util.z80.*;

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
	private JPanel regsPanel;
	private JLabel[] regLabels;
	private String[] regLabelTexts={"AF = ","BC = ","DE = ","HL = ",
			"AF' = ","BC' = ","DE' = ","HL' =",
			"IX = ","IY = ","SP = ","PC = ","IR = "};
	
	private JTextField[] regFields;
	public JTextField[] getRegFields() {
		return regFields;
	}
	
	private JPanel tablePanel;
	private JTable table;
	private JScrollPane tableScrollPane;
	
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
	
	private Z80 z80;
	public void setZ80(Z80 z80) {
		this.z80=z80;
	}
	
	private DebuggerTableModel debuggerTableModel;
	public DebuggerTableModel getDebuggerTableModel() {
		return debuggerTableModel;
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
		debuggerFrame.setLayout(new BorderLayout());
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
		
		/* Debugger panels */
		regsPanel=new JPanel(new BorderLayout()) {
			public Dimension getPreferredSize() {
				return new Dimension(200,200);
			}
		};
		regsPanel.setBorder(BorderFactory.createTitledBorder("Register values"));
		regLabels=new JLabel[13];
		regFields=new JTextField[13];
		JPanel normalRegsPanel=new JPanel(new GridLayout(4,1));
		normalRegsPanel.setBorder(BorderFactory.createTitledBorder("Normal"));
		JPanel altRegsPanel=new JPanel(new GridLayout(4,1));
		altRegsPanel.setBorder(BorderFactory.createTitledBorder("Alternative"));
		JPanel specRegsPanel=new JPanel(new GridLayout(5,1));
		specRegsPanel.setBorder(BorderFactory.createTitledBorder("Special"));
		JPanel regInfoPanels[] =new JPanel[13];
		for(int i=0;i<13;i++) {
			regLabels[i]=new JLabel(regLabelTexts[i]);
			regFields[i]=new JTextField("xxxx");
			regFields[i].setEditable(false);
			regInfoPanels[i]=new JPanel();
			regInfoPanels[i].add(regLabels[i]);
			regInfoPanels[i].add(regFields[i]);
			if(i<4) {
				normalRegsPanel.add(regInfoPanels[i]);
			} else if((i>=4) && (i<8)) {
				altRegsPanel.add(regInfoPanels[i]);
			} else {
				specRegsPanel.add(regInfoPanels[i]);
			}
		}
		regsPanel.add(normalRegsPanel,BorderLayout.WEST);
		regsPanel.add(altRegsPanel,BorderLayout.EAST);
		regsPanel.add(specRegsPanel,BorderLayout.SOUTH);
		debuggerFrame.add(regsPanel, BorderLayout.WEST);
		
		tablePanel=new JPanel();
		debuggerTableModel=new DebuggerTableModel();
		debuggerTableModel.setCommandListing(new LinkedList<DisasmResult>());
		table=new JTable(debuggerTableModel);
		tableScrollPane=new JScrollPane(table,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		tablePanel.add(tableScrollPane);
		debuggerFrame.add(tablePanel, BorderLayout.CENTER);
		
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
	
	public void updateDebuggerInfo() {
		if(!SwingUtilities.isEventDispatchThread()) {
			LOG.fatal("Attempted to update debugger text fields outside event " +
					"dispatch thread. This is not legal due to Swing concurrency " +
					"issues.");
			System.exit(1);
		}
		
		for(int i=0;i<13;i++) {
			regFields[i].setText(Hex.intToHex4(z80.getRegPair(i)));
		}
	}
}
