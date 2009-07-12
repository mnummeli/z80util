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

public class GUI extends JFrame {
	private DebuggerFrame debugger;
	private JCheckBoxMenuItem debuggerItem;
	
	Logger LOG=Logger.getLogger(GUI.class);
	
	public GUI(SpectrumControls controller, SpectrumScreen scr) {
		LOG.info("Creating the GUI.");
		setTitle("Spectrum emulator - (C) 2009, Mikko Nummelin");
		
		debugger=new DebuggerFrame();
		
		setLayout(new BorderLayout());
		setIconImage(LogoFactory.createLogo());
		JMenuBar menubar=new JMenuBar();
		setJMenuBar(menubar);
		
		/* The File menu */
		JMenu fileMenu=new JMenu("File");
		JMenuItem openItem=new JMenuItem("Open");
		openItem.addActionListener(controller);
		fileMenu.add(openItem);
		JMenuItem saveItem=new JMenuItem("Save");
		saveItem.addActionListener(controller);
		fileMenu.add(saveItem);
		fileMenu.add(new JSeparator());
		JMenuItem exitItem=new JMenuItem("Exit");
		exitItem.addActionListener(controller);
		fileMenu.add(exitItem);
		menubar.add(fileMenu);
		
		/* The View menu */
		JMenu viewMenu=new JMenu("View");
		debuggerItem=new JCheckBoxMenuItem("Debugger");
		debuggerItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(debuggerItem.getState()) {
					debugger.setVisible(true);
				} else {
					debugger.setVisible(false);
				}
			}
		});
		viewMenu.add(debuggerItem);
		menubar.add(viewMenu);
		
		/* The Help menu */
		JMenu helpMenu=new JMenu("Help");
		JMenuItem aboutItem=new JMenuItem("About");
		aboutItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(GUI.this,
						"Jeccy - Spectrum emulator - (C) 2009, Mikko Nummelin\n"+
						"This program is free software and comes with ABSOLUTELY NO WARRANTY.",
						"About",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		helpMenu.add(aboutItem);
		menubar.add(helpMenu);
		
		add(scr,BorderLayout.CENTER);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addKeyListener(controller);

		pack();
		setResizable(false);
		setVisible(true);		
	}
}
