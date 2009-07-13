/*
 * Snapshot.java - Generic Z80 snapshot interface.
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

package org.mn.z80util.z80;

import java.io.*;

public interface Snapshot {
	
	/**
	 * Reads the snapshot contents from specified input stream.
	 * The possibly empty snapshot must be obtained in platform-specific
	 * manner.
	 * 
	 * @param is	The input stream.
	 */
	void read(InputStream is);

	/**
	 * Reads the snapshot contents from specified processor configuration.
	 * 
	 * @param z80	The source processor
	 * @param ula	The source memory provider
	 */
	void read(Z80 z80, AddressBusProvider ula);
	
	/**
	 * Writes the snapshot to specified output stream.
	 * The snapshot must be obtained in platform-specific manner.
	 * 
	 * @param os	The output stream.
	 */
	void write(OutputStream os);
	
	/**
	 * Writes the snapshot to specified processor configuration.
	 * 
	 * @param z80	The target processor
	 * @param ula	The target memory provider
	 */
	void write(Z80 z80, AddressBusProvider ula);
	
	/**
	 * Writes the snapshot to default processor configuration,
	 * whatever that is and if such exists.
	 */
	void write();
}
