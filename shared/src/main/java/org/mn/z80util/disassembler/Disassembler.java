/*
 * Disassembler.java - Z80 disassembler core.
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

public final class Disassembler {

    private static byte[] memory;
    private static int base_address;
    private static int bytes_read;
    private static String command;
    private static boolean ixmode, iymode;
    private static final int HL = 2;
    private static final int SP = 3;
    private static final int H = 4;
    private static final int L = 5;
    private static final int IHL = 6;
    private static final String[] flag = {"NZ", "Z", "NC", "C", "PO", "PE", "P", "M"};
    private static final String[] rp = {"BC", "DE", "HL", "SP"};
    private static final String[] reg = {"B", "C", "D", "E", "H", "L", "(HL)", "A"};
    private static final String[] misc1 = {"RLCA", "RRCA", "RLA", "RRA", "DAA", "CPL", "SCF", "CCF"};
    private static final String[] misc2 = {"ADD", "ADC", "SUB", "SBC", "AND", "XOR", "OR", "CP"};
    private static final String[] misc3 = {"RLC", "RRC", "RL", "RR", "SLA", "SRA", "SLL", "SLR"};
    private static final String[] misc4 = {null, "BIT", "RES", "SET"};
    private static final String[] misc5 = {"LDI", "CPI", "INI", "OUTI",
        "LDD", "CPD", "IND", "OUTD",
        "LDIR", "CPIR", "INIR", "OTIR",
        "LDDR", "CPDR", "INDR", "OTDR"};

    private static int getByteAt(int addr) {
        return memory[addr & 0xffff] & 0xff;
    }

    private static int getWordAt(int addr) {
        return getByteAt(addr) | (getByteAt(addr + 1) << 8);
    }

    private static int fetchByte() {
        int value = getByteAt(base_address + bytes_read);
        bytes_read++;
        return value;
    }

    private static int fetchWord() {
        int value = getWordAt(base_address + bytes_read);
        bytes_read += 2;
        return value;
    }

    private static String getCommandHexDigits() {
        String result = "";
        for (int i = 0; i < 5; i++) {
            if (i < bytes_read) {
                result += Hex.intToHex2(getByteAt(base_address + i)) + " ";
            } else {
                result += "   ";
            }
        }
        return result;
    }

    public static DisasmResult disassemble(byte[] memory, short address) {
        Disassembler.memory = memory;
        Disassembler.base_address = address;
        Disassembler.bytes_read = 0;
        Disassembler.command = "";
        Disassembler.ixmode = Disassembler.iymode = false;

        int i, cmd = 0x00;

        /* 'Semi-infinite' loop for determining IX and IY modes */
        for (i = 0; i < 0x10000; i++) {
            cmd = fetchByte();
            if (cmd == 0xdd) {
                ixmode = true;
                iymode = false;
            } else if (cmd == 0xfd) {
                ixmode = false;
                iymode = true;
            } else {
                break; // Usually breaks out here after 1 round
            }
        }
        if (i == 0x10000) {
            System.err.println("ERROR - only IX and IY prefixes in entire memory!");
            System.exit(1);
        }

        /* Command parsing */
        int p = (cmd & 0xc0) >> 6;
        int q = (cmd & 0x38) >> 3;
        int r = cmd & 0x7;
        if (cmd == 0xcb) {
            byte idx = 0;
            if (ixmode || iymode) {
                idx = (byte) fetchByte();
            }
            cmd = fetchByte();
            p = (cmd & 0xc0) >> 6;
            q = (cmd & 0x38) >> 3;
            r = cmd & 0x7;
            switch (p) {
                case 0:
                    disassemble_cb_p0(q, r, idx);
                    break;
                case 1:
                case 2:
                case 3:
                    disassemble_cb_p123(p, q, r, idx);
                    break;
            }
        } else if (cmd == 0xed) {
            cmd = fetchByte();
            p = (cmd & 0xc0) >> 6;
            q = (cmd & 0x38) >> 3;
            r = cmd & 0x7;
            if (ixmode || iymode) {
                command = "*ILLEGAL ED*, indexing mode.";
            } else {
                switch (p) {
                    case 0:
                    case 3:
                        command = "*ILLEGAL ED*, page " + p;
                        break;
                    case 1:
                        disassemble_ed_p1(q, r);
                        break;
                    case 2:
                        disassemble_ed_p2(q, r);
                        break;
                }
            }
        } else {
            switch (p) {
                case 0:
                    disassemble_p0(q, r);
                    break;
                case 1:
                    disassemble_p1(q, r);
                    break;
                case 2:
                    disassemble_p2(q, r);
                    break;
                case 3:
                    disassemble_p3(q, r);
                    break;
            }
        }

        return new DisasmResult(address, bytes_read, getCommandHexDigits(),
                command);
    }

    private static String addr_from_dis() {
        byte dis = (byte) fetchByte();
        int tmp = base_address + bytes_read + dis;
        return Hex.intToHex4(tmp);
    }

    private static String ind_expr(String s, int idx) {
        if ((idx & 0x80) == 0) {
            return "(" + s + "+" + Hex.intToHex2(idx) + ")";
        } else {
            idx ^= 0xff;
            idx++;
            return "(" + s + "-" + Hex.intToHex2(idx) + ")";
        }
    }

    private static String ind_expr(String s) {
        int tmp = fetchByte();
        return ind_expr(s, tmp);
    }

    private static String rp(int n, boolean use_af) {
        if (ixmode && (n == HL)) {
            return "IX";
        } else if (iymode && (n == HL)) {
            return "IY";
        } else if (use_af && (n == SP)) {
            return "AF";
        } else {
            return rp[n];
        }
    }

    private static String rp(int n) {
        return rp(n, false);
    }

    private static String reg(int n) {
        if (ixmode) {
            if (n == H) {
                return "XH";
            } else if (n == L) {
                return "XL";
            } else if (n == IHL) {
                return ind_expr("IX");
            }
        } else if (iymode) {
            if (n == H) {
                return "YH";
            } else if (n == L) {
                return "YL";
            } else if (n == IHL) {
                return ind_expr("IY");
            }
        }
        return reg[n];
    }

    private static void disassemble_p0(int q, int r) {
        switch (r) {
            case 0:
                if (q == 0) {
                    command = "NOP";
                } else if (q == 1) {
                    command = "EX AF,AF'";
                } else if (q == 2) {
                    command = "DJNZ " + addr_from_dis();
                } else if (q == 3) {
                    command = "JR " + addr_from_dis();
                } else {
                    command = "JR " + flag[q & 3] + "," + addr_from_dis();
                }
                break;
            case 1:
                if ((q & 1) == 0) {
                    command = "LD " + rp((q & 6) >> 1) + ","
                            + Hex.intToHex4(fetchWord());
                } else {
                    command = "ADD " + rp(HL) + "," + rp((q & 6) >> 1);
                }
                break;
            case 2:
                switch (q) {
                    case 0:
                    case 2:
                        command = "LD (" + rp[q & 1] + "),A";
                        break;
                    case 1:
                    case 3:
                        command = "LD A,(" + rp[q & 1] + ")";
                        break;
                    case 4:
                        command = "LD (" + Hex.intToHex4(fetchWord()) + ")," + rp(HL);
                        break;
                    case 5:
                        command = "LD " + rp(HL) + ",(" + Hex.intToHex4(fetchWord()) + ")";
                        break;
                    case 6:
                        command = "LD (" + Hex.intToHex4(fetchWord()) + "),A";
                        break;
                    case 7:
                        command = "LD A,(" + Hex.intToHex4(fetchWord()) + ")";
                        break;
                }
                break;
            case 3:
                if ((q & 1) == 0) {
                    command = "INC " + rp((q & 6) >> 1);
                } else {
                    command = "DEC " + rp((q & 6) >> 1);
                }
                break;
            case 4:
                command = "INC " + reg(q);
                break;
            case 5:
                command = "DEC " + reg(q);
                break;
            case 6:
                command = "LD " + reg(q) + "," + Hex.intToHex2(fetchByte());
                break;
            case 7:
                command = misc1[q];
                break;
        }
    }

    private static void disassemble_p1(int q, int r) {
        if ((q == 6) && (r == 6)) {
            command = "HALT";
            return;
        }
        if ((ixmode || iymode) && (q == IHL)) {
            command = "LD " + reg(q) + "," + reg[r];
        } else if ((ixmode || iymode) && (r == IHL)) {
            command = "LD " + reg[q] + "," + reg(r);
        } else {
            command = "LD " + reg(q) + "," + reg(r);
        }
    }

    private static void disassemble_p2(int q, int r) {
        command = misc2[q] + " A," + reg(r);
    }

    private static void disassemble_p3(int q, int r) {
        switch (r) {
            case 0:
                command = "RET " + flag[q];
                break;
            case 1:
                if ((q & 1) == 0) {
                    command = "POP " + rp((q & 6) >> 1, true);
                } else {
                    switch (q) {
                        case 1:
                            command = "RET";
                            break;
                        case 3:
                            command = "EXX";
                            break;
                        case 5:
                            command = "JP (" + rp(HL) + ")";
                            break;
                        case 7:
                            command = "LD SP," + rp(HL);
                            break;
                    }
                }
                break;
            case 2:
                command = "JP " + flag[q] + "," + Hex.intToHex4(fetchWord());
                break;
            case 3:
                switch (q) {
                    case 0:
                        command = "JP " + Hex.intToHex4(fetchWord());
                        break;
                    case 2:
                        command = "OUT (" + Hex.intToHex2(fetchByte()) + "),A";
                        break;
                    case 3:
                        command = "IN A," + Hex.intToHex2(fetchByte());
                        break;
                    case 4:
                        command = "EX (SP)," + rp(HL);
                        break;
                    case 5:
                        command = "EX DE,HL";
                        break;
                    case 6:
                        command = "DI";
                        break;
                    case 7:
                        command = "EI";
                        break;
                }
                break;
            case 4:
                command = "CALL " + flag[q] + "," + Hex.intToHex4(fetchWord());
                break;
            case 5:
                if ((q & 1) == 0) {
                    command = "PUSH " + rp((q & 6) >> 1, true);
                } else {
                    if (q == 1) {
                        command = "CALL " + Hex.intToHex4(fetchWord());
                    } else {
                        /* The other cases should be already decided */
                        System.err.println("ERROR, prefixes.");
                    }
                }
                break;
            case 6:
                command = misc2[q] + " A," + Hex.intToHex2(fetchByte());
                break;
            case 7:
                command = "RST " + Hex.intToHex2(q << 3);
                break;
        }
    }

    private static void disassemble_cb_p0(int q, int r, int idx) {
        if (ixmode) {
            command = misc3[q] + " " + ind_expr("IX", idx);
            if (r != 6) {
                command += " -> " + reg[r];
            }
        } else if (iymode) {
            command = misc3[q] + " " + ind_expr("IY", idx);
            if (r != 6) {
                command += " -> " + reg[r];
            }
        } else {
            command = misc3[q] + " " + reg[r];
        }
    }

    private static void disassemble_cb_p123(int p, int q, int r, int idx) {
        if (ixmode) {
            command = misc4[p] + " " + q + "," + ind_expr("IX", idx);
            if (r != 6) {
                command += " -> " + reg[r];
            }
        } else if (iymode) {
            command = misc4[p] + " " + q + "," + ind_expr("IY", idx);
            if (r != 6) {
                command += " -> " + reg[r];
            }
        } else {
            command = misc4[p] + " " + q + "," + reg[r];
        }
    }

    private static void disassemble_ed_p1(int q, int r) {
        switch (r) {
            case 0:
                if (q == 6) {
                    command = "IN (BC)";
                } else {
                    command = "IN " + reg[q] + "," + "(BC)";
                }
                break;
            case 1:
                if (q == 6) {
                    command = "OUT (BC)";
                } else {
                    command = "OUT (BC)," + reg[q];
                }
                break;
            case 2:
                if ((q & 1) == 0) {
                    command = "SBC HL," + rp[(q & 6) >> 1];
                } else {
                    command = "ADC HL," + rp[(q & 6) >> 1];
                }
                break;
            case 3:
                if ((q & 1) == 0) {
                    command = "LD (" + Hex.intToHex4(fetchWord()) + ")," + rp[(q & 6) >> 1];
                } else {
                    command = "LD " + rp[(q & 6) >> 1] + ",(" + Hex.intToHex4(fetchWord()) + ")";
                }
                break;
            case 4:
                command = "NEG";
                break;
            case 5:
                if (q == 1) {
                    command = "RETI";
                } else {
                    command = "RETN";
                }
                break;
            case 6:
                if ((q & 2) == 2) {
                    command = "IM " + ((q & 1) + 1);
                } else {
                    command = "IM 0";
                }
                break;
            case 7:
                switch (q) {
                    case 0:
                        command = "LD I,A";
                        break;
                    case 1:
                        command = "LD R,A";
                        break;
                    case 2:
                        command = "LD A,I";
                        break;
                    case 3:
                        command = "LD A,R";
                        break;
                    case 4:
                        command = "RRD";
                        break;
                    case 5:
                        command = "RLD";
                        break;
                    case 6:
                    case 7:
                        command = "NOP";
                        break;
                }
                break;
        }
    }

    private static void disassemble_ed_p2(int q, int r) {
        if ((q < 4) || (r >= 4)) {
            command = "*ILLEGAL ED*, page 2";
        } else {
            command = misc5[(r & 3) | ((q & 3) << 2)];
        }
    }
}
