/*
 * ScreenImpl.java - Spectrum screen implementation.
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
import java.awt.image.*;

import javax.swing.*;

public class SpectrumScreen extends JPanel {

    private SpectrumULA ula;

    public void setUla(SpectrumULA ula) {
        this.ula = ula;
    }
    /**
     * The RGB integer array of Spectrum colors. Change this in order to fix the
     * colors appearance.
     */
    private final int[] spectrumColorsRGB = {
        0x000000, // DARK BLACK
        0x0000cf, // DARK BLUE
        0xcf0000, // DARK RED
        0xcf00cf, // DARK MAGENTA
        0x00cf00, // DARK GREEN
        0x00cfcf, // DARK CYAN
        0xcfcf00, // DARK YELLOW
        0xcfcfcf, // DARK WHITE (e.g. grey)
        0x000000, // BLACK
        0x0000ff, // BRIGHT BLUE
        0xff0000, // BRIGHT RED
        0xff00ff, // BRIGHT MAGENTA
        0x00ff00, // BRIGHT GREEN
        0x00ffff, // BRIGHT CYAN
        0xffff00, // BRIGHT YELLOW
        0xffffff // BRIGHT WHITE
    };
    private Color[] spectrumColors = new Color[16];
    private BufferedImage paperStripe;
    public static final int SCALE = 2;
    public static final int PAPER_WIDTH = 256;
    public static final int PAPER_HEIGHT = 192;
    public static final int BORDER_WIDTH = 48;
    public static final int LOWER_BORDER_WIDTH = 56;
    public static final int WIDTH_S = BORDER_WIDTH + PAPER_WIDTH + BORDER_WIDTH;
    public static final int HEIGHT_S = BORDER_WIDTH + PAPER_HEIGHT + LOWER_BORDER_WIDTH;

    /**
     * Initializes the screen with Spectrum-related measures of paper area and
     * colors.
     */
    public SpectrumScreen() {
        paperStripe = new BufferedImage(SCALE * PAPER_WIDTH, SCALE,
                BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < 16; i++) {
            spectrumColors[i] = new Color(spectrumColorsRGB[i]);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(SCALE * WIDTH_S, SCALE * HEIGHT_S);
    }

    private void setPixel(int x, int colorno) {
        for (int i = 0; i < SCALE; i++) {
            for (int j = 0; j < SCALE; j++) {
                paperStripe.setRGB(SCALE * x + j, i, spectrumColorsRGB[colorno]);
            }
        }
    }

    /**
     * Updates screen row from range 0-311. The highest 48 and lowest 56 rows
     * consist solely of BORDER area. This entry point is for external calls
     * only.
     */
    public void updateRow(int row) {
        repaint(0, SCALE * row, SCALE * WIDTH_S, SCALE);
    }

    public void updateRowSync(Graphics g, int row) {
        int b = ula.getBorder();
        g.setColor(spectrumColors[b]);
        if ((row < BORDER_WIDTH) || (row >= BORDER_WIDTH + PAPER_HEIGHT)) {
            g.fillRect(0, SCALE * row, SCALE * WIDTH_S, SCALE);
        } else {
            g.fillRect(0, SCALE * row, SCALE * BORDER_WIDTH, SCALE);
            g.fillRect(SCALE * (BORDER_WIDTH + PAPER_WIDTH), SCALE * row,
                    SCALE * BORDER_WIDTH, SCALE);
            int n = row - BORDER_WIDTH;
            byte[] memory = ula.getMemory();
            int third = (n & 0xc0) >> 6;
            int characterRow = (n & 0x38) >> 3;
            int pixelRow = n & 0x7;
            int pixBase = 0x4000 | (characterRow << 5) | (pixelRow << 8)
                    | (third << 11);
            int attrBase = 0x5800 | (characterRow << 5) | (third << 8);
            for (int i = 0; i < PAPER_WIDTH; i++) {
                int c = i >> 3;
                byte attrCell = memory[attrBase + c];

                /* INK:   -x---xxx */
                int ink = (attrCell & 7) | ((attrCell & 0x40) >> 3);

                /* PAPER: -xxxx--- */
                int paper = (attrCell & 0x78) >> 3;

                if (((attrCell & 0x80) != 0) & ula.getFlashState()) {
                    /* FLASH: x------- */
                    int tmp = ink;
                    ink = paper;
                    paper = tmp;
                }
                int ind = i & 7;
                if ((memory[pixBase + c] & (0x80 >> ind)) != 0) {
                    setPixel(i, ink);
                } else {
                    setPixel(i, paper);
                }
            }
            g.drawImage(paperStripe, SCALE * BORDER_WIDTH, SCALE * row,
                    SCALE * PAPER_WIDTH, SCALE, this);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Rectangle clipBounds = g.getClipBounds();
        int y1 = clipBounds.y / SCALE;
        int y2;
        y2 = (clipBounds.y + clipBounds.height) / SCALE;
        for (int i = y1; i < y2; i++) {
            updateRowSync(g, i);
        }
    }
}
