/*
 * Hex.java - hexadecimal utils.
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

public final class Hex {
	private static final char[] hexdigits=
	{'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	public static String intToHex2(int n) {
		return ""+hexdigits[(n&0xf0)>>4]+
			hexdigits[n&0xf];
	}
	
	public static String intToHex4(int n) {
		return ""+hexdigits[(n&0xf000)>>12]+
			hexdigits[(n&0xf00)>>8]+
			hexdigits[(n&0xf0)>>4]+
			hexdigits[n&0xf];
	}
	
	public static int hexDigitToInt(char c) {
		if((c>='0')&&(c<='9')) {
			return c-'0';
		} else if((c>='A')&&(c<='F')) {
			return c-'A'+10;
		} else if((c>='a')&&(c<='f')) {
			return c-'a'+10;
		} else {
			return -1; /* Error */
		}
	}
	
	public static int hexToInt(String s) {
		int sum=0;
		for(int i=0;i<s.length();i++) {
			int hdi=hexDigitToInt(s.charAt(i));
			if(hdi<0) {
				return -1; /* Error */
			}
			int shift=s.length()-i-1;
			sum+=hdi<<(shift<<2);
		}
		return sum;
	}
}
