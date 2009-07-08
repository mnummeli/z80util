/*
 * StatusPanel.java - Spectrum emulator status panel (next command).
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


public class StatusPanel extends JPanel {	
	
	private volatile String statusPanelString="";
	public void setStatusPanelString(String s) {
		statusPanelString=s;
	}

	public Dimension getPreferredSize() {
		return new Dimension(SpectrumScreen.SCALE*SpectrumScreen.WIDTH,
				20*SpectrumScreen.SCALE);
	}
	
	protected void paintComponent(Graphics g) {
		g.setColor(Color.WHITE);
		g.fillRect(0,0,getWidth(),getHeight());
		g.setColor(Color.BLACK);
		g.setFont(new Font("Monospaced",Font.PLAIN,8*SpectrumScreen.SCALE));
		g.drawString(statusPanelString, 12*SpectrumScreen.SCALE,12*SpectrumScreen.SCALE);
	}
}
