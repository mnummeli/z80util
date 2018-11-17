/*
 * SpectrumRunningProfile.java - Spectrum runtime profiling functions.
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
package org.mn.z80util.spectrum.profiling;

import java.io.*;
import java.util.*;
import javax.swing.*;

import org.apache.log4j.Logger;
import org.mn.z80util.spectrum.*;
import org.mn.z80util.spectrum.snapshots.Snapshots;
import org.mn.z80util.z80.*;

public class SpectrumRunningProfile {

    private Logger LOG = Logger.getLogger(SpectrumRunningProfile.class);
    private ProfileNode[] profilingMap;
    private Vector<ProfileBlock> blockMap;
    private int currentPC = -1, previousPC = -1;
    private int interruptsAtStart, interruptsAtEnd;
    private Z80 z80;
    private SpectrumULA ula;
    private SpectrumZ80Clock clock;

    public SpectrumRunningProfile(Z80 z80, SpectrumULA ula,
            SpectrumZ80Clock clock) {
        this.z80 = z80;
        this.ula = ula;
        this.clock = clock;
        profilingMap = new ProfileNode[0x10000];
        previousPC = -1;
        this.interruptsAtStart = clock.getInterrupts();
    }

    public double getProfilingTimeInSeconds() {
        return (double) (interruptsAtEnd - interruptsAtStart) / 50;
    }

    /**
     * Collects profiling data to profilingMap. The following categories are
     * collected:
     *
     * <ul> <li>command execution density per address</li> <li>command
     * predecessors, 52h is blacklisted, as it is IM 1 interrupt return command
     * address in Spectrum</li> <li>command successors, 38h is blacklisted, as
     * it is IM 1 interrupt entry address</li> </ul>
     */
    public void collectProfilingData() {
        currentPC = z80.getRegPair(Z80.PC) & 0xffff;
        if (profilingMap[currentPC] == null) {
            profilingMap[currentPC] = new ProfileNode();
        }
        profilingMap[currentPC].density++;

        /* Blacklist mode 1 interrupts (entry and return) */
        if ((previousPC >= 0) && (previousPC != 0x52)
                && (currentPC != 0x38)) {
            profilingMap[currentPC].addPredecessor(previousPC);
            profilingMap[previousPC].addSuccessor(currentPC);
        }
        previousPC = currentPC;
    }

    /**
     * Iterates the start and end blocks in profile array. The algorithm is:
     *
     * An existing profile node is a <b>start block node</b> if and only if:
     * <ul> <li>its address is 38h; or</li> <li>its predecessor count differs
     * from one; or</li> <li>it has a predecessor with higher or equal address
     * than its own address; or</li> <li>it has a predecessor which is an <i>end
     * block node</i></li> </ul>
     *
     * An existing profile node is an <b>end block node</b> if and only if: <ul>
     * <li>its address is 52h; or</li> <li>its successor count differs from one;
     * or</li> <li>it has a successor with lesser or equal address than its own
     * address; or</li> <li>it has a successor which is a <i>start block
     * node</i></li> </ul>
     *
     * As the rules are dependent from each other, one must iterate until a
     * stable state is reached.
     */
    public void findBlockStartsAndEnds() {
        interruptsAtEnd = clock.getInterrupts();

        LOG.info("Constructing profiling block start and endpoints.");

        for (int i = 0; i < 0x10000; i++) {
            if (profilingMap[i] != null) {
                profilingMap[i].startBlock = false;
                profilingMap[i].endBlock = false;
            }
        }

        boolean hasChanged;
        do {
            hasChanged = false;
            for (int i = 0; i < 0x10000; i++) {
                if (profilingMap[i] != null) {
                    if (!profilingMap[i].startBlock) {
                        if ((i == 0x38)
                                || (profilingMap[i].predecessors.size() > 1)) {
                            profilingMap[i].startBlock = true;
                            hasChanged = true;
                        }
                        for (Object addrObj : profilingMap[i].predecessors) {
                            int addr = ((Integer) addrObj).intValue();
                            if ((addr >= i) || (profilingMap[addr].endBlock)) {
                                profilingMap[i].startBlock = true;
                                hasChanged = true;
                            }
                        }
                    }

                    if (!profilingMap[i].endBlock) {
                        if ((i == 0x52)
                                || (profilingMap[i].successors.size() > 1)) {
                            profilingMap[i].endBlock = true;
                            hasChanged = true;
                        }
                        for (Object addrObj : profilingMap[i].successors) {
                            int addr = ((Integer) addrObj).intValue();
                            if ((addr <= i) || (profilingMap[addr].startBlock)) {
                                profilingMap[i].endBlock = true;
                                hasChanged = true;
                            }
                        }
                    }
                }
            }
        } while (hasChanged == true);
    }

    /**
     * Creates the profiling blocks according to previously marked start and end
     * addresses.
     */
    public void createBlocks() {
        LOG.info("Collecting profiling blocks.");
        int count = 0;

        blockMap = new Vector<ProfileBlock>();
        for (int i = 0; i < 0x10000; i++) {
            if ((profilingMap[i] != null)
                    && (profilingMap[i].startBlock)) {
                ProfileBlock pb = new ProfileBlock(ula, this);
                pb.entryDensity = profilingMap[i].density;
                pb.predecessors = profilingMap[i].predecessors;
                int j = i;
                while (!profilingMap[j].endBlock) {
                    pb.commandAddresses.add(j);
                    j = profilingMap[j].successors.first();
                }
                pb.commandAddresses.add(j);
                pb.successors = profilingMap[j].successors;
                blockMap.add(pb);
                count++;
            }
        }

        LOG.info(count + " profiling blocks added.");
        Collections.sort(blockMap);
        LOG.info("Profiling information sorted according to entry frequency.");
    }

    /**
     * When program blocks are ordered by createBlocks, map the predecessor
     * addresses into the block order values.
     *
     * @param address	Address of predecessor block last command
     * @return	The order number in program blocks array, -1 in case of error.
     */
    public int predecessorCorrespondsTo(int address) {
        for (int i = 0; i < blockMap.size(); i++) {
            ProfileBlock pb = blockMap.elementAt(i);
            if ((pb != null) && (pb.getLastCommandAddress() == address)) {
                return i;
            }
        }
        return -1; /* Error */
    }

    /**
     * When program blocks are ordered by createBlocks, map the successor
     * addresses into the block order values.
     *
     * @param address	Address of successor block last command
     * @return	The order number in program blocks array, -1 in case of error.
     */
    public int successorCorrespondsTo(int address) {
        for (int i = 0; i < blockMap.size(); i++) {
            ProfileBlock pb = blockMap.elementAt(i);
            if ((pb != null) && (pb.getFirstCommandAddress() == address)) {
                return i;
            }
        }
        return -1; /* Error */
    }

    /**
     * Translates predecessor and successor values into corresponding order
     * numbers in the program block array.
     */
    public void translatePredecessorsAndSuccessors() {
        for (ProfileBlock pb : blockMap) {
            pb.predecessorNumbers = new TreeSet<Integer>();
            pb.successorNumbers = new TreeSet<Integer>();
            for (int i : pb.predecessors) {
                pb.predecessorNumbers.add(predecessorCorrespondsTo(i));
            }
            for (int i : pb.successors) {
                pb.successorNumbers.add(successorCorrespondsTo(i));
            }
        }
    }

    private void saveBlocksAsText(PrintStream out) {
        out.println("Interrupts at start: " + interruptsAtStart);
        out.println("Interrupts at end: " + interruptsAtEnd);
        out.println("Estimated profiling time in seconds: "
                + getProfilingTimeInSeconds() + "\n");
        for (int i = 0; i < blockMap.size(); i++) {
            ProfileBlock pb = blockMap.elementAt(i);
            if (pb.isInnermostNode(i)) {
                out.println("INNERMOST PROGRAM BLOCK: " + i);
            } else {
                out.println("PROGRAM BLOCK: " + i);
            }
            out.println(pb);
        }
    }

    private void saveBlocksAsSVGFlowChart(PrintStream out) {
        int imageheight = blockMap.size();
        out.println("<svg width=\"20cm\" height=\"22cm\" viewBox=\"0 -10 "
                + 10 * imageheight + " " + 10 * imageheight + "\">");
        for (int i = 0; i < blockMap.size(); i++) {
            ProfileBlock pb = blockMap.elementAt(i);
            if (pb.isInnermostNode(i)) {
                out.println("<line x1=\"0\" y1=\"" + 10 * i + "\" x2=\""
                        + 10 * imageheight + "\" y2=\"" + 10 * i
                        + "\" stroke=\"purple\"/>");
            }
            for (int j : pb.successorNumbers) {
                if (i == j) {
                    /* Tight loop (circle) */
                    out.println("<circle cx=\"" + 5 * imageheight + "\" cy=\"" + 10 * i
                            + "\" r=\"4\" fill=\"none\" stroke=\"black\"/>");
                } else {
                    int cy_pre = 10 * i, cy_succ = 10 * j;
                    int max = j > i ? j : i;
                    int cx_edge = j > i ? 5 * (imageheight - j) : 5 * (imageheight + max);
                    out.println("<path d=\"M " + 5 * imageheight + " " + cy_pre + " C "
                            + cx_edge + " " + cy_pre + " " + cx_edge + " " + cy_succ + " "
                            + 5 * imageheight + " " + cy_succ
                            + "\" fill=\"none\" stroke=\"black\"/>");
                }
            }
        }
        out.println("</svg>");
    }

    /**
     * Saves the profiling blocks to disk.
     */
    public void saveBlocks() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFileChooser chooser = new JFileChooser();
                int returnVal = chooser.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        File f = chooser.getSelectedFile();
                        String ftype = Snapshots.fileType(f.getName());
                        PrintStream out = new PrintStream(f);
                        if (ftype.equals("svg")) {
                            LOG.info("Saving SVG diagram.");
                            saveBlocksAsSVGFlowChart(out);
                        } else if (ftype.equals("xml")) {
                            LOG.warn("Plain XML output not implemented.");
                        } else {
                            LOG.info("Defaulting to plain text output.");
                            saveBlocksAsText(out);
                        }
                    } catch (IOException e) {
                        LOG.warn("Unable to write profile file.");
                    }
                }
            }
        });
    }
}
