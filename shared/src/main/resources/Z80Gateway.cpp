/*
 * Z80Gateway.cpp - Generic Z80 snapshot gateway C++ interface for JNI.
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

#include "org_mn_z80util_z80_jni_NativeZ80Gateway.h"
#include "NativeMNZ80.h"

NativeMNZ80 *z80;

/**
 * Executes next command from processor
 */
JNIEXPORT void JNICALL Java_org_mn_z80util_z80_jni_NativeZ80Gateway_executeNextCommand
  (JNIEnv *envP, jobject thisP) {
}

/**
 * Gets register contents.
 * @see org.mn.z80util.z80.TestZ80
 * @param regNo Register number.
 * @return Register content.
 */
JNIEXPORT jbyte JNICALL Java_org_mn_z80util_z80_jni_NativeZ80Gateway_getReg
  (JNIEnv *envP, jobject thisP, jint regNo) {
}

/**
 * Gets register pair contents.
 * @see org.mn.z80util.z80.TestZ80
 * @param regPairNo Register pair number.
 * @return Register pair contents.
 */
JNIEXPORT jshort JNICALL Java_org_mn_z80util_z80_jni_NativeZ80Gateway_getRegPair
  (JNIEnv *envP, jobject thisP, jint regPairNo) {
}

/**
 * Resets the processor.
 */
JNIEXPORT void JNICALL Java_org_mn_z80util_z80_jni_NativeZ80Gateway_reset
  (JNIEnv *envP, jobject thisP) {
		cout << "--> reset.";

	  // Initializes singleton object.
	  z80=NativeMNZ80::getProcessor();
	  z80->reset();
}

/**
 * Sets or releases processor halt state.
 * @param haltState <code>true</code> if going to halting, <code>false</code> if releasing
 */
JNIEXPORT void JNICALL Java_org_mn_z80util_z80_jni_NativeZ80Gateway_setHaltState
  (JNIEnv *envP, jobject thisP, jboolean haltState) {
}

/**
 * Sets register contents.
 * @see org.mn.z80util.z80.TestZ80
 * @param regNo Register number.
 * @param value New value.
 */
JNIEXPORT void JNICALL Java_org_mn_z80util_z80_jni_NativeZ80Gateway_setReg
  (JNIEnv *envP, jobject thisP, jint regNo, jbyte value) {
}

/**
 * Sets register pair contents.
 * @see org.mn.z80util.z80.TestZ80
 * @param regPairNo Register pair number.
 * @param value New value.
 */
JNIEXPORT void JNICALL Java_org_mn_z80util_z80_jni_NativeZ80Gateway_setRegPair
  (JNIEnv *envP, jobject thisP, jint regNo, jshort value) {
}
