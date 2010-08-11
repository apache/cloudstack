/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.console;

import java.awt.Dimension;
import java.io.IOException;

public interface RfbViewer {
	boolean isProxy();
	boolean hasClientConnection();
	
	RfbProto getRfb();
	Dimension getScreenSize();
	Dimension getFrameSize();
	
	int getScalingFactor();
	int getCursorScaleFactor();
	boolean ignoreCursorUpdate();
	int getDeferCursorUpdateTimeout();
	int getDeferScreenUpdateTimeout();
	int getDeferUpdateRequestTimeout();
	
	int setPixelFormat(RfbProto rfb) throws IOException;
	
	void onInputEnabled(boolean enable);
	
	void onFramebufferSizeChange(int w, int h);
	void onFramebufferUpdate(int x, int y, int w, int h);
	void onFramebufferCursorMove(int x, int y);
	void onFramebufferCursorShapeChange(int encodingType,
		    int xhot, int yhot, int width, int height, byte[] cursorData);
	
	void onDesktopResize();
	void onFrameResize(Dimension newSize);
	void onDisconnectMessage();
	void onBellMessage();
	
	void onPreProtocolProcess(byte[] bs) throws IOException;
	boolean onPostFrameBufferUpdateProcess(boolean cursorPosReceived) throws IOException;
	void onProtocolProcessException(IOException e);
}
