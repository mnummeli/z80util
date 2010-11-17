/*
 * ALU.java
 * 
 * (C) 2009, Mikko Nummelin
 * based on YAZE, originally (C) 1995, Frank D. Cringle.
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
package org.mn.z80util.z80.yaze;

public class YazeBasedALU {

    /* Parity table */
    private final int[] partab = new int[0x100];

    /**
     * Constructor initializes parity table.
     */
    public YazeBasedALU() {
        for (int i = 0; i < 0x100; i++) {
            int tmp = 4;
            for (int j = 0; j < 8; j++) {
                if ((i & (1 << j)) != 0) {
                    tmp ^= 4;
                }
            }
            partab[i] = tmp;
        }
    }
    /* Accumulator and flags */
    private int af = 0x0000, af_alt = 0x0000;
    private final int C = 0x01, N = 0x02, H = 0x10, Z = 0x40;

    public int af() {
        return af;
    }

    public void af(int af) {
        this.af = af & 0xffff;
    }

    public int af_alt() {
        return af_alt;
    }

    public void af_alt(int af) {
        this.af_alt = af & 0xffff;
    }

    public int f() {
        return af & 0xff;
    }

    public void f(int f) {
        this.af = (this.af & ~0xff) | (f & 0xff);
    }

    public int f_alt() {
        return af_alt & 0xff;
    }

    public void f_alt(int f) {
        this.af_alt = (this.af_alt & ~0xff) | (f & 0xff);
    }

    public int a() {
        return (af & 0xff00) >> 8;
    }

    public void a(int a) {
        this.af = (this.af & ~0xff00) | ((a & 0xff) << 8);
    }

    public int a_alt() {
        return (af_alt & 0xff00) >> 8;
    }

    public void a_alt(int a) {
        this.af_alt = (this.af_alt & ~0xff00) | ((a & 0xff) << 8);
    }

    /* Private helper functions */
    private int v(boolean b) {
        return b ? 1 : 0;
    }

    private int ldig(int v) {
        return v & 0xf;
    }

    private int hdig(int v) {
        return (v >> 4) & 0xf;
    }

    private boolean TSTFLAG(int flag) {
        return (f() & flag) != 0;
    }

    private void SETFLAG(int flag, boolean value) {
        if (value) {
            af |= flag;
        } else {
            af &= ~flag;
        }
    }

    private void SETFLAG(int flag, int value) {
        SETFLAG(flag, value != 0);
    }

    private int parity(int value) {
        return partab[value & 0xff];
    }

    /* Arithmetic operations involving flags */
    public void ex_af() {
        int tmp = af;
        af = af_alt;
        af_alt = tmp;
    }

    public int inc8(int value) {
        int tmp = (value + 1) & 0xff;
        af = (af & ~0xfe) | (tmp & 0xa8)
                | (v((tmp & 0xff) == 0) << 6)
                | (v((tmp & 0xf) == 0) << 4)
                | (v(tmp == 0x80) << 2);
        return tmp & 0xff;
    }

    public int dec8(int value) {
        int tmp = (value - 1) & 0xff;
        af = (af & ~0xfe) | (tmp & 0xa8)
                | (v((tmp & 0xff) == 0) << 6)
                | (v((tmp & 0xf) == 0xf) << 4)
                | (v(tmp == 0x7f) << 2) | 2;
        return tmp & 0xff;
    }

    public void rlca() {
        af = ((af >> 7) & 0x0128) | ((af << 1) & ~0x1ff)
                | (af & 0xc4) | ((af >> 15) & 1);
    }

    public void rla() {
        af = ((af << 8) & 0x0100) | ((af >> 7) & 0x28)
                | ((af << 1) & ~0x01ff) | (af & 0xc4) | ((af >> 15) & 1);
    }

    public void rrca() {
        int tmp = a();
        int sum = tmp >> 1;
        af = ((tmp & 1) << 15) | (sum << 8) | (sum & 0x28)
                | (af & 0xc4) | (tmp & 1);
    }

    public void rra() {
        int tmp = a();
        int sum = tmp >> 1;
        af = ((af & 1) << 15) | (sum << 8) | (sum & 0x28)
                | (af & 0xc4) | (tmp & 1);
    }

    public int add16(int hl, int value) {
        hl &= 0xffff;
        value &= 0xffff;
        int sum = hl + value;
        int cbits = (hl ^ value ^ sum) >> 8;
        af = (af & ~0x3b) | ((sum >> 8) & 0x28)
                | (cbits & 0x10) | ((cbits >> 8) & 1);
        return sum;
    }

    public void daa() {
        int acu = a();
        int temp = ldig(acu);
        int cbits = v(TSTFLAG(C));
        if (TSTFLAG(N)) {	/* last operation was a subtract */
            boolean hd = (cbits != 0) || (acu > 0x99);
            if (TSTFLAG(H) || (temp > 9)) { /* adjust low digit */
                if (temp > 5) {
                    SETFLAG(H, 0);
                }
                acu -= 6;
                acu &= 0xff;
            }
            if (hd) /* adjust high digit */ {
                acu -= 0x160;
            }
        } else {			/* last operation was an add */
            if (TSTFLAG(H) || (temp > 9)) { /* adjust low digit */
                SETFLAG(H, (temp > 9));
                acu += 6;
            }
            if ((cbits != 0) || ((acu & 0x1f0) > 0x90)) /* adjust high digit */ {
                acu += 0x60;
            }
        }
        cbits |= (acu >> 8) & 1;
        acu &= 0xff;
        af = (acu << 8) | (acu & 0xa8) | (v(acu == 0) << 6)
                | (af & 0x12) | partab[acu] | cbits;
    }

    public void cpl() {
        af = (~af & ~0xff) | (af & 0xc5) | ((~af >> 8) & 0x28) | 0x12;
    }

    public void scf() {
        af = (af & ~0x3b) | ((af >> 8) & 0x28) | 1;
    }

    public void ccf() {
        af = (af & ~0x3b) | ((af >> 8) & 0x28) | ((af & 1) << 4) | (~af & 1);
    }

    public void cmd8(int cmd, int value) {
        value &= 0xff;
        switch (cmd) {
            case 0:
                add8(value);
                break;
            case 1:
                adc8(value);
                break;
            case 2:
                sub8(value);
                break;
            case 3:
                sbc8(value);
                break;
            case 4:
                and8(value);
                break;
            case 5:
                xor8(value);
                break;
            case 6:
                or8(value);
                break;
            case 7:
                cp8(value);
                break;
        }
    }

    public void add8(int value) {
        value &= 0xff;
        int acu = a();
        int sum = acu + value;
        int cbits = acu ^ value ^ sum;
        af = ((sum & 0xff) << 8) | (sum & 0xa8)
                | (v((sum & 0xff) == 0) << 6) | (cbits & 0x10)
                | (((cbits >> 6) ^ (cbits >> 5)) & 4)
                | ((cbits >> 8) & 1);
    }

    public void adc8(int value) {
        value &= 0xff;
        int acu = a();
        int sum = acu + value + v(TSTFLAG(C));
        int cbits = acu ^ value ^ sum;
        af = ((sum & 0xff) << 8) | (sum & 0xa8)
                | (v((sum & 0xff) == 0) << 6) | (cbits & 0x10)
                | (((cbits >> 6) ^ (cbits >> 5)) & 4)
                | ((cbits >> 8) & 1);
    }

    public void sub8(int value) {
        value &= 0xff;
        int acu = a();
        int sum = acu - value;
        int cbits = acu ^ value ^ sum;
        af = ((sum & 0xff) << 8) | (sum & 0xa8)
                | (v((sum & 0xff) == 0) << 6) | (cbits & 0x10)
                | (((cbits >> 6) ^ (cbits >> 5)) & 4) | 2
                | ((cbits >> 8) & 1);
    }

    public void sbc8(int value) {
        value &= 0xff;
        int acu = a();
        int sum = acu - value - v(TSTFLAG(C));
        int cbits = acu ^ value ^ sum;
        af = ((sum & 0xff) << 8) | (sum & 0xa8)
                | (v((sum & 0xff) == 0) << 6) | (cbits & 0x10)
                | (((cbits >> 6) ^ (cbits >> 5)) & 4) | 2
                | ((cbits >> 8) & 1);
    }

    public void and8(int value) {
        value &= 0xff;
        int sum = ((af >> 8) & value) & 0xff;
        af = (sum << 8) | (sum & 0xa8)
                | (v(sum == 0) << 6) | 0x10 | partab[sum];
    }

    public void xor8(int value) {
        value &= 0xff;
        int sum = ((af >> 8) ^ value) & 0xff;
        af = (sum << 8) | (sum & 0xa8)
                | (v(sum == 0) << 6) | partab[sum];
    }

    public void or8(int value) {
        value &= 0xff;
        int sum = ((af >> 8) | value) & 0xff;
        af = (sum << 8) | (sum & 0xa8)
                | (v(sum == 0) << 6) | partab[sum];
    }

    public void cp8(int value) {
        value &= 0xff;
        af = (af & ~0x28) | (value & 0x28);
        int acu = a();
        int sum = acu - value;
        int cbits = acu ^ value ^ sum;
        af = (af & ~0xff) | (sum & 0x80)
                | (v((sum & 0xff) == 0) << 6) | (value & 0x28)
                | (((cbits >> 6) ^ (cbits >> 5)) & 4) | 2
                | (cbits & 0x10) | ((cbits >> 8) & 1);
    }

    private void cbshflg1(int value, boolean cbits) {
        value &= 0xff;
        af = (af & ~0xff) | (value & 0xa8)
                | (v((value & 0xff) == 0) << 6)
                | parity(value) | v(cbits);
    }

    public int cb(int op, int acu) {
        int temp = (acu &= 0xff), cbits;
        switch (op & 0xc0) {
            case 0x00:		/* shift/rotate */
                switch (op & 0x38) {
                    case 0x00:	/* RLC */
                        temp = (acu << 1) | (acu >> 7);
                        cbits = temp & 1;
                        cbshflg1(temp, cbits != 0);
                        break;
                    case 0x08:	/* RRC */
                        temp = (acu >> 1) | (acu << 7);
                        cbits = temp & 0x80;
                        cbshflg1(temp, cbits != 0);
                        break;
                    case 0x10:	/* RL */
                        temp = (acu << 1) | v(TSTFLAG(C));
                        cbits = acu & 0x80;
                        cbshflg1(temp, cbits != 0);
                        break;
                    case 0x18:	/* RR */
                        temp = (acu >> 1) | (v(TSTFLAG(C)) << 7);
                        cbits = acu & 1;
                        cbshflg1(temp, cbits != 0);
                        break;
                    case 0x20:	/* SLA */
                        temp = acu << 1;
                        cbits = acu & 0x80;
                        cbshflg1(temp, cbits != 0);
                        break;
                    case 0x28:	/* SRA */
                        temp = (acu >> 1) | (acu & 0x80);
                        cbits = acu & 1;
                        cbshflg1(temp, cbits != 0);
                        break;
                    case 0x30:	/* SLL */
                        temp = (acu << 1) | 1;
                        cbits = acu & 0x80;
                        cbshflg1(temp, cbits != 0);
                        break;
                    case 0x38:	/* SRL */
                        temp = acu >> 1;
                        cbits = acu & 1;
                        cbshflg1(temp, cbits != 0);
                        break;
                }
                break;
            case 0x40:		/* BIT */
                if ((acu & (1 << ((op >> 3) & 7))) != 0) {
                    af = (af & ~0xfe) | 0x10
                            | (v((op & 0x38) == 0x38) << 7);
                } else {
                    af = (af & ~0xfe) | 0x54;
                }
                if ((op & 7) != 6) {
                    af |= (acu & 0x28);
                }
                break;
            case 0x80:		/* RES */
                temp = acu & ~(1 << ((op >> 3) & 7));
                break;
            case 0xc0:		/* SET */
                temp = acu | (1 << ((op >> 3) & 7));
                break;
        }

        return temp;
    }

    /**
     * This should be called ONLY when performing IN operation with memory
     * pointed by BC.
     *
     * @param value	Byte from I/O address pointed by BC in Z80.
     */
    public void in_ibc(int value) {
        af = (af & ~0xfe) | (value & 0xa8) | (v((value & 0xff) == 0) << 6)
                | parity(value);
    }

    public int sbc16(int hl, int value) {
        hl &= 0xffff;
        value &= 0xffff;
        int sum = hl - value - v(TSTFLAG(C));
        int cbits = (hl ^ value ^ sum) >> 8;
        af = (af & ~0xff) | ((sum >> 8) & 0xa8)
                | (v((sum & 0xffff) == 0) << 6)
                | (((cbits >> 6) ^ (cbits >> 5)) & 4)
                | (cbits & 0x10) | 2 | ((cbits >> 8) & 1);
        return sum;
    }

    public int adc16(int hl, int value) {
        hl &= 0xffff;
        value &= 0xffff;
        int sum = hl + value + v(TSTFLAG(C));
        int cbits = (hl ^ value ^ sum) >> 8;
        af = (af & ~0xff) | ((sum >> 8) & 0xa8)
                | (v((sum & 0xffff) == 0) << 6)
                | (((cbits >> 6) ^ (cbits >> 5)) & 4)
                | (cbits & 0x10) | ((cbits >> 8) & 1);
        return sum;
    }

    public void neg() {
        int temp = a();
        af = (-(af & 0xff00) & 0xff00);
        af |= ((af >> 8) & 0xa8) | (v((af & 0xff00) == 0) << 6)
                | (v((temp & 0x0f) != 0) << 4) | (v(temp == 0x80) << 2)
                | 2 | v(temp != 0);
    }

    /**
     * This should be called ONLY, when assigning register A the value of
     * either I or R register.
     *
     * @param ir	Value of I or R register.
     * @param iff2	Value of IFF2 interrupt flip-flop.
     */
    public void ld_a_ir(int ir, boolean iff2) {
        ir &= 0xff;
        af = (af & 0x01) | ((ir & 0xff) << 8) | (ir & 0xa8)
                | (v((ir & 0xff) == 0) << 6) | v(iff2) << 2;
    }

    /**
     * Performs right rotation of BCD values.
     *
     * @param ihl	Previous value of (hl)
     * @return		New value of (hl)
     */
    public int rrd(int ihl) {
        int temp = ihl & 0xff;
        int acu = a();
        ihl = hdig(temp) | (ldig(acu) << 4);
        acu = (acu & 0xf0) | ldig(temp);
        af = (acu << 8) | (acu & 0xa8) | (v((acu & 0xff) == 0) << 6)
                | partab[acu] | (af & 1);
        return ihl;
    }

    /**
     * Performs left rotation of BCD values.
     *
     * @param ihl	Previous value of (hl)
     * @return		New value of (hl)
     */
    public int rld(int ihl) {
        int temp = ihl & 0xff;
        int acu = a();
        ihl = (ldig(temp) << 4) | ldig(acu);
        acu = (acu & 0xf0) | hdig(temp);
        af = (acu << 8) | (acu & 0xa8) | (v((acu & 0xff) == 0) << 6)
                | partab[acu] | (af & 1);
        return ihl;
    }

    /**
     * Performs flags in LDI/LDD and single step LDIR/LDDR. Note that the flag
     * adjustment should be done BEFORE incrementing/decrementing HL or
     * decrementing BC.
     *
     * @param ihl	Value of (HL)
     * @param bc	Value of BC
     */
    public void ldi_ldd(int ihl, int bc) {
        ihl &= 0xff;
        bc &= 0xffff;
        ihl += a();
        af = (af & ~0x3e) | (ihl & 8) | ((ihl & 2) << 4)
                | (v((--bc & 0xffff) != 0) << 2);
    }

    public void cpi_cpd(int ihl, int bc) {
        ihl &= 0xff;
        bc &= 0xffff;
        int acu = a();
        int sum = acu - ihl;
        int cbits = acu ^ ihl ^ sum;
        af = (af & ~0xfe) | (sum & 0x80) | (v((sum & 0xff) == 0) << 6)
                | (((sum - ((cbits & 16) >> 4)) & 2) << 4) | (cbits & 16)
                | ((sum - ((cbits >> 4) & 1)) & 8)
                | v((--bc & 0xffff) != 0) << 2 | 2;
        if ((sum & 15) == 8 && (cbits & 16) != 0) {
            af &= ~8;
        }
    }

    public void ini_ind(int bc) {
        SETFLAG(N, 1);
        SETFLAG(Z, (bc & 0xff00) == 0x100);
    }

    public void outi_outd(int b) {
        b &= 0xff;
        SETFLAG(N, 1);
        SETFLAG(Z, b == 1);
    }
}
