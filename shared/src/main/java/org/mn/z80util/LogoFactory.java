package org.mn.z80util;

import java.awt.image.*;

public class LogoFactory {
	/**
	 * Creates simple logo for the application.
	 * @return	Logo image
	 */
	public static BufferedImage createLogo() {
		BufferedImage logo=new BufferedImage(16,8,
				BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<16;i++) {
			for(int j=0;j<8;j++) {
				switch(i>>1) {
				case 0:
				case 1:
					logo.setRGB(i,j,0x000000);
					break;
				case 2:
					logo.setRGB(i,j,0xff0000);
					break;
				case 3:
					logo.setRGB(i,j,0xffff00);
					break;
				case 4:
					logo.setRGB(i,j,0x00ff00);
					break;
				case 5:
					logo.setRGB(i,j,0x00ffff);
					break;
				case 6:
				case 7:
					logo.setRGB(i,j,0x00003f);
					break;
				}
			}
		}
		return logo;
	}
}
