/*
 * DebuggerFrame.java - The GUI for integrated Z80 disassembler and debugger.
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
import javax.swing.*;

public class DebuggerFrame extends JFrame {
	private JPanel controlPanel;
	private JButton stepButton, continueButton;
	private JTextField startAddressField, endAddressField;
	private JScrollPane scrollPane;
	private JTextArea textArea;

	public DebuggerFrame() {
		setTitle("Debugger");
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		controlPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
		stepButton=new JButton("Step");
		// TODO: stepButton.addActionListener(menuControls);
		continueButton=new JButton("Continue");
		// TODO: continueButton.addActionListener(menuControls);
		startAddressField=new JTextField(4);
		endAddressField=new JTextField(4);
		controlPanel.add(stepButton);
		controlPanel.add(continueButton);
		controlPanel.add(new JLabel("Start address:"));
		controlPanel.add(startAddressField);
		controlPanel.add(new JLabel("End address:"));
		controlPanel.add(endAddressField);
		
		textArea=new JTextArea("Lorem ipsum.");
		textArea.setEditable(false);
		scrollPane=new JScrollPane(textArea,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		add(controlPanel,BorderLayout.NORTH);
		add(scrollPane,BorderLayout.SOUTH);
		setResizable(false);
		pack();
	}
}
