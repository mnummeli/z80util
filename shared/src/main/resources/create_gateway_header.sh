#!/bin/sh

# This script should be called from the source tree. It generates an
# appropriate JNI header according to compiled NativeZ80Gateway.
#
# (C) 2010, Mikko Nummelin <mikko.nummelin@tkk.fi>
# 
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330,
# Boston, MA 02111-1307, USA.

javah -classpath ../../../target/classes org.mn.z80util.z80.jni.NativeZ80Gateway
