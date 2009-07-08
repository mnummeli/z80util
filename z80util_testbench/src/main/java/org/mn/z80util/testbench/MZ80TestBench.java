/*
 * MZ80TestBench.java - A Z80 test bench.
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

package org.mn.z80util.testbench;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.mn.z80util.*;
import org.mn.z80util.disassembler.*;
import org.mn.z80util.z80.*;
import org.springframework.context.*;
import org.springframework.context.support.*;

/**
 * This is an utility for comparing performance of two different Z80
 * implementations. The tester loops through the command set, on each
 * round creating a random initial configuration and performing the
 * same command on each processor implementations to both. The register
 * set is compared and if differences are found, they are reported in
 * detail.
 * 
 * @author Mikko Nummelin <mikko.nummelin@tkk.fi>
 *
 */
public class MZ80TestBench implements Runnable {
	private final static String COPYRIGHT_NOTICE=
	"MZ80TestBench - a Z80 processor testbench.\n"+
	"(C) 2009, Mikko Nummelin, <mikko.nummelin@tkk.fi>\n"+
	"MZ80TestBench is free software and comes with ABSOLUTELY NO WARRANTY.";
	
	private final String[] registerName={"B","C","D","E","H","L","F","A",
			"B'","C'","D'","E'","H'","L'","F'","A'",
			"XH","XL","YH","YL","SPH","SPL","PCH","PCL","I","R","IM_IFF"};
	
	/* The tester GUI */
	
	private static MZ80TestBench testBench;
	private JFrame GUI;

	private JPanel leftPanel;
	private JPanel firstProcessorPanel;
	private JLabel firstProcessorStatus;
	private JPanel secondProcessorPanel;
	private JLabel secondProcessorStatus;
	private JPanel progressBarPanel;
	private JProgressBar progressBar;
	private JLabel statusMessage;
	private String msgString;
	private JLabel executedCommand;
	private JPanel actionPanel;
	private JButton okCancelButton;

	private JPanel imagePanel;
	private ImageIcon img;
	
	boolean fail_this=false;
	
	/* Please remember to run this in event dispatch thread, even if you
	 * change the program. And NEVER place Swing components in Spring application
	 * context via XML or otherwise. */
	private void createAndShowGUI() {
		
		// See above
		if(!SwingUtilities.isEventDispatchThread()) {
			System.err.println("Attempting to construct the GUI from outside "+
					"of event dispatch thread! This is an error. Please check "+
					"your code modifications.");
			System.exit(1);
		}
		
		/* Initializes the GUI frame */
		GUI=new JFrame("Mikko's Z80 Testbench - (C) Mikko Nummelin, 2009");
		GUI.setLayout(new BorderLayout());
		GUI.setIconImage(LogoFactory.createLogo());
		GUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		leftPanel=new JPanel();
		leftPanel.setLayout(new GridLayout(3,1));
		
		// The first processor information
		firstProcessorPanel=new JPanel();
		firstProcessorPanel.setLayout(new GridLayout(2,1));
		firstProcessorPanel.setBorder(BorderFactory.createTitledBorder("Processor 1"));
		firstProcessorPanel.add(new JLabel(processor1.getClass().getName()));
		firstProcessorStatus=new JLabel("-");
		firstProcessorPanel.add(firstProcessorStatus);
		leftPanel.add(firstProcessorPanel);
		
		// The second processor information
		secondProcessorPanel=new JPanel();
		secondProcessorPanel.setLayout(new GridLayout(2,1));
		secondProcessorPanel.setBorder(BorderFactory.createTitledBorder("Processor 2"));
		secondProcessorPanel.add(new JLabel(processor2.getClass().getName()));
		secondProcessorStatus=new JLabel("-");
		secondProcessorPanel.add(secondProcessorStatus);
		leftPanel.add(secondProcessorPanel);
		
		// The progress bar panel
		progressBarPanel=new JPanel();
		progressBarPanel.setLayout(new GridLayout(3,1));
		progressBarPanel.setBorder(BorderFactory.createTitledBorder("Progress"));
		progressBar=new JProgressBar(0,0x6bf);
		progressBarPanel.add(progressBar);
		statusMessage=new JLabel("-");
		progressBarPanel.add(statusMessage);
		executedCommand=new JLabel("-");
		progressBarPanel.add(executedCommand);
		leftPanel.add(progressBarPanel);
		
		GUI.add(leftPanel, BorderLayout.WEST);
		
		// The action button panel
		actionPanel=new JPanel();
		okCancelButton=new JButton("Cancel");
		okCancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		actionPanel.add(okCancelButton);
		GUI.add(actionPanel, BorderLayout.SOUTH);

		// The image panel
		imagePanel=new JPanel();
		java.net.URL imgURL = getClass().getResource("/Z80-pinout.png");
		img=new ImageIcon(imgURL);
		imagePanel.add(new JLabel(img));
		GUI.add(imagePanel, BorderLayout.EAST);

		GUI.pack();
		GUI.setResizable(false);
		GUI.setVisible(true);
	}

	/*
	 * Increasing/decreasing this value renders the tests slower/faster,
	 * but tradeoff is that they become more/less accurate.
	 */
	private int sameCommandRounds;
	public void setSameCommandRounds(int sameCommandRounds) {
		this.sameCommandRounds=sameCommandRounds;
	}
	
	private boolean wantGui;
	public void setWantGui(boolean wantGui) {
		this.wantGui=wantGui;
	}

	private AddressBusProvider ula;
	public void setUla(AddressBusProvider ula) {
		this.ula=ula;
	}
	
	private Z80 processor1;
	public void setProcessor1(Z80 processor1) {
		this.processor1=processor1;
	}
	
	private Z80 processor2;
	public void setProcessor2(Z80 processor2) {
		this.processor2=processor2;
	}
	
	private DisasmResult dar;
	private int cmdno;
	
	/* Very important that these are volatile. */
	private volatile byte[] result1, result2;
	
	/**
	 * Creates simple snapshot from a Z80 system.
	 * @param z80	The processor
	 * @param ula	The ULA (containing memory)
	 * @return	The snapshot
	 */
	private synchronized byte[] createSnap(Z80 z80, AddressBusProvider ula) {
		byte[] snap=new byte[0x10000+27];
		for(int j=0;j<0xffff;j++) {
			snap[j]=ula.getByte((short)j);
		}
		for(int j=0;j<27;j++) {
			snap[j+0x10000]=z80.getReg(j);
		}
		return snap;
	}
	
	/**
	 * Applies simple snapshot to a Z80 system.
	 * 
	 * @see createSnap
	 */
	private void applySnap(byte[] snap, Z80 z80, AddressBusProvider ula) {
		for(int j=0;j<0xffff;j++) {
			ula.setByte((short)j,snap[j]);
		}
		for(int j=0;j<27;j++) {
			z80.setReg(j,snap[j+0x10000]);
		}
	}
	
	private final String flagBinary(int flags) {
		String result="";
		for(int i=7;i>=0;i--) {
			result += ((flags & (1<<i))!=0) ? '*' : ' ';
		}
		return result;
	}
	
	private TestResultSet createResult() {
		int m=-1,r=-1;
		for(int i=0;i<0x10000;i++) {
			if(result1[i]!=result2[i]) {
				m=i;
				break;
			}
		}
		
		for(int i=0; i<27; i++) {
			if(i==Z80.R) continue;
			
			/*
			 * In case of 'BIT' instructions, undocumented and unknown flags
			 * are masked out, as their values may depend on processor internal
			 * registers and thus vary between legitimate Z80 cores.
			 */
			if((cmdno>=0x300) && (cmdno<0x5ff) && ((cmdno & 0300) == 0100) &&
					(i==6)) {
				result1[i+0x10000] &= 0x53;
				result2[i+0x10000] &= 0x53;
			}
			
			/* To avoid contribution of IFF2 to flags in LD A,I  */
			if((cmdno==0x657) && (i==6)){
				result1[i+0x10000] &= 0xfb;
				result2[i+0x10000] &= 0xfb;
			}
			
			/* Unknown flags are masked away from INI, OUTI and similar
			 * instructions */
			if(((cmdno & 0x6e6)==0x6a2) && (i==6)) {
				result1[i+0x10000] &= 0x43;
				result2[i+0x10000] &= 0x43;
			}
			
			if(result1[i+0x10000]!=result2[i+0x10000]) {
				r=i;
				break;
			}
		}
		
		return new TestResultSet(m,r);
	}
	
	private volatile TestResultSet trs;
	private void reportResultsToGUI() {
		trs=createResult();
		if(trs.differingMemoryAddress >= 0) {
			fail_this=true;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					statusMessage.setText("Memory contents are different.");
					executedCommand.setText("Command:"+dar.getHexDigits()+dar.getCommand());
					firstProcessorStatus.setText("("+Hex.intToHex4(trs.differingMemoryAddress)+
							")="+Hex.intToHex2(result1[trs.differingMemoryAddress]&0xff));
					secondProcessorStatus.setText("("+Hex.intToHex4(trs.differingMemoryAddress)+
							")="+Hex.intToHex2(result2[trs.differingMemoryAddress]&0xff));
				}
			});
		} else if(trs.differingRegisterNumber >= 0) {
			fail_this=true;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					executedCommand.setText("Command: "+dar.getHexDigits()+dar.getCommand());
					int i=trs.differingRegisterNumber;
					int c1=result1[trs.differingRegisterNumber+0x10000] & 0xff;
					int c2=result2[trs.differingRegisterNumber+0x10000] & 0xff;
					if(i==6) {
						String flagsStr;
						statusMessage.setText("Flag contents are different.");
						flagsStr="SZ5H3PNC";
						String msg="Flags raised: ";
						for(int j=7;j>=0;j--) {
							msg+=((c1&(1<<j))!=0) ? flagsStr.charAt(7-j) : '-';
						}
						firstProcessorStatus.setText(msg);
						msg="Flags raised: ";
						for(int j=7;j>=0;j--) {
							msg+=((c2&(1<<j))!=0) ? flagsStr.charAt(7-j) : '-';
						}
						secondProcessorStatus.setText(msg);
					} else if(i==26) {
						statusMessage.setText("Interrupt flag contents are different.");
						firstProcessorStatus.setText("IFF1="+((c1&1)!=0)+", IFF2="+
								((c1&2)!=0)+", IM="+((c1&0xc)>>2));
						secondProcessorStatus.setText("IFF1="+((c2&1)!=0)+", IFF2="+
								((c2&2)!=0)+", IM="+((c2&0xc)>>2));
					} else {
						statusMessage.setText("Register contents are different.");
						firstProcessorStatus.setText(registerName[i]+"="+Hex.intToHex2(c1));
						secondProcessorStatus.setText(registerName[i]+"="+Hex.intToHex2(c2));
					}
				}
			});
		}
		
		if(fail_this) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					progressBar.setForeground(Color.RED);
					okCancelButton.setText("OK");
				}
			});
		}
	}
	
	private void reportResults() {
		trs=createResult();
		if(trs.differingMemoryAddress >= 0) {
			fail_this=true;
			int i=trs.differingMemoryAddress;
			System.out.println();
			System.out.println("When applying command: [ "+dar.getHexDigits()+"] "+
					dar.getCommand());
			System.out.println("Difference at memory address: "+Hex.intToHex4(i));
			System.out.println("Value on "+processor1+" is: "+
					Hex.intToHex2(result1[i]&0xff));
			System.out.println("Value on "+processor2+" is: "+
					Hex.intToHex2(result2[i]&0xff));
		}
		
		if(trs.differingRegisterNumber >= 0) {
			fail_this=true;
			int i=trs.differingRegisterNumber;	
			System.out.println();
			System.out.println("When applying command: [ "+dar.getHexDigits()+"] "+
					dar.getCommand());
			
			/* Flags */
			if(i==6) {
				System.out.format("%50s: %8s\n", new Object[] {"Difference on flags", "SZ5H3PNC"});
				System.out.format("%50s: %8s\n", new Object[] {processor1,flagBinary(result1[i+0x10000]&0xff)});
				System.out.format("%50s: %8s\n", new Object[] {processor2,flagBinary(result2[i+0x10000]&0xff)});

			/* Interrupts */
			} else if(i==26) {
				System.out.format("%50s: %8s\n", new Object[] {"Difference on interrupt flags", "----IM21"});
				System.out.format("%50s: %8s\n", new Object[] {processor1,flagBinary(result1[i+0x10000]&0xff)});
				System.out.format("%50s: %8s\n", new Object[] {processor2,flagBinary(result2[i+0x10000]&0xff)});
				
			/* Registers */
			} else {
				System.out.println("Difference on register: "+registerName[i]);
				System.out.println("Value on "+processor1+" is: "+
						Hex.intToHex2(result1[i+0x10000]&0xff));
				System.out.println("Value on "+processor2+" is: "+
						Hex.intToHex2(result2[i+0x10000]&0xff));
			}
		}
		
		if(fail_this) {
			System.exit(1);
		}
	}
	
	public void run() {

		/*
		 * 0x00-0xff	Normal commands.
		 * 0x100-0x1ff	IX-prefixed commands.
		 * 0x200-0x2ff	IY-prefixed commands.
		 * 0x300-0x3ff	CBh-prefixed commands.
		 * 0x400-0x4ff	IX/CBh
		 * 0x500-0x5ff	IY/CBh
		 * 0x600-0x6ff	EDh-prefixed commands.
		 */
		for(cmdno=0x00; cmdno<0x6c0; cmdno++) {
			commandselect: {
			/* Omissions */
			switch(cmdno) {
			case 0xcb:
			case 0xdd:
			case 0xed:
			case 0xfd:
			case 0x1cb:
			case 0x1dd:
			case 0x1ed:
			case 0x1fd:
			case 0x2cb:
			case 0x2dd:
			case 0x2ed:
			case 0x2fd:
			case 0x65f: // to avoid messing with R-register
			case 0x677:
			case 0x67f:
				break commandselect;
			}

			/* To avoid illegal EDh zeroth page */
			if((cmdno>=0x600) && (cmdno<0x640)) {
				break commandselect;
			}

			/* To avoid illegal commands on EDh second page */
			if((cmdno>=0x680) && (cmdno<0x6c0) && (((cmdno & 7)>=4) ||
					(((cmdno & 0070)>>3)<4))) {
				break commandselect;
			}

			for(int j=0; j < sameCommandRounds; j++) {
				byte[] memory=ula.getMemory();
				Random rand=new Random(System.nanoTime());
				rand.nextBytes(memory);
				processor1.reset();
				processor1.setRegPair(Z80.PC,(short)0x8000);
				processor1.setRegPair(Z80.SP,(short)0x7ffe);

				/* Sets up the command to be tested. */
				if(cmdno<0x100) {
					ula.setByte((short)0x8000,(byte)cmdno);
				} else if(cmdno<0x200) {
					ula.setByte((short)0x8000,(byte)0xdd);
					ula.setByte((short)0x8001,(byte)cmdno);
				} else if(cmdno<0x300) {
					ula.setByte((short)0x8000,(byte)0xfd);
					ula.setByte((short)0x8001,(byte)cmdno);
				} else if(cmdno<0x400) {
					ula.setByte((short)0x8000,(byte)0xcb);
					ula.setByte((short)0x8001,(byte)cmdno);
				} else if(cmdno<0x500) {
					ula.setByte((short)0x8000,(byte)0xdd);
					ula.setByte((short)0x8001,(byte)0xcb);
					ula.setByte((short)0x8003,(byte)cmdno);
				} else if(cmdno<0x600) {
					ula.setByte((short)0x8000,(byte)0xfd);
					ula.setByte((short)0x8001,(byte)0xcb);
					ula.setByte((short)0x8003,(byte)cmdno);
				} else {
					ula.setByte((short)0x8000,(byte)0xed);
					ula.setByte((short)0x8001,(byte)cmdno);
				}

				dar=Disassembler.disassemble(ula.getMemory(),(short)0x8000);

				/* Takes snapshot as this setup is to be applied to the other
				 * processor also. */
				byte[] initialBackup=createSnap(processor1,ula);

				/* Executes the command on both processors and collects results. */
				processor1.setHaltState(false);
				processor1.executeNextCommand();
				result1=createSnap(processor1,ula);
				applySnap(initialBackup,processor2,ula);
				processor2.setHaltState(false);
				processor2.executeNextCommand();
				result2=createSnap(processor2,ula);
				
				if(wantGui) {
					reportResultsToGUI();
				} else {
					reportResults();
				}
				
				if(fail_this) {
					return;
				}

			} // for j=...
		} // commandselect

		switch((cmdno & 0x700)>>8) {
		case 0:
			msgString="Testing normal commands";
			break;
		case 1:
			msgString="Testing IX-prefixed commands.";
			break;
		case 2:
			msgString="Testing IY-prefixed commands.";
			break;
		case 3:
			msgString="Testing CBh-prefixed commands.";
			break;
		case 4:
			msgString="Testing IX-CBh-prefixed commands.";
			break;
		case 5:
			msgString="Testing IY-CBh-prefixed commands.";
			break;
		case 6:
			msgString="Testing EDh-prefixed commands.";
			break;
		}
		
		if(wantGui) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					progressBar.setValue(cmdno);
					statusMessage.setText(msgString);
				}
			});
		} else if((cmdno & 7)==0) {
			if((cmdno & 0xff)==0) {
				System.out.println();
				System.out.println(msgString);
			}
			System.out.print(".");
		}
		} // for cmdno=...

		if(wantGui) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					statusMessage.setText("Processors are compatible with each other.");
					okCancelButton.setText("OK");
				}
			});
		} else {
			System.out.println("\nOK.");
		}
	}
	
	/**
	 * The main method. Loads a Spring Framework application context and
	 * testbench bean from it.
	 * 
	 * @see spring-testbench.xml
	 */
	public static void main(String[] args) {
		System.out.println(COPYRIGHT_NOTICE);
		ApplicationContext context =
			new ClassPathXmlApplicationContext(new String[] {"spring-testbench.xml"});
		testBench = (MZ80TestBench) context.getBean("testbench");

		// The correct way is to initialize the GUI in the event dispatch thread.
		if(testBench.wantGui) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					testBench.createAndShowGUI();
					new Thread(testBench,"Z80-Testbench-with-GUI").start();
				}
			});
		} else {
			new Thread(testBench,"Z80-Testbench-without-GUI").start();
		}
	}
	
	class TestResultSet {
		/*
		 * Possibly differing memory address and register number. Set to -1
		 * if there was no difference in such category.
		 */
		int differingMemoryAddress, differingRegisterNumber;
		
		TestResultSet(int m, int r) {
			differingMemoryAddress=m;
			differingRegisterNumber=r;
		}
	}
}
