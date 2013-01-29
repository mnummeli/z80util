/*
 * DisasmResult.java - Z80 disassembler helper class.
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
package org.mn.z80util.disassembler;

public final class DisasmResult implements Comparable<DisasmResult> {

    public DisasmResult(int startAddr, int bytesRead, String hexDigits,
            String command) {
        this.startAddr = startAddr;
        this.bytesRead = bytesRead;
        this.hexDigits = hexDigits;
        this.command = command;
    }
    private int startAddr;

    public int getStartAddr() {
        return startAddr;
    }
    private int bytesRead;

    public int getBytesRead() {
        return bytesRead;
    }
    private String hexDigits;

    public String getHexDigits() {
        return hexDigits;
    }
    private String command;

    public String getCommand() {
        return command;
    }

    @Override
    public int compareTo(DisasmResult dar) {
        if (this.startAddr < dar.getStartAddr()) {
            return -1;
        } else if (this.startAddr == dar.getStartAddr()) {
            return 0;
        } else {
            return 1;
        }
    }
}
