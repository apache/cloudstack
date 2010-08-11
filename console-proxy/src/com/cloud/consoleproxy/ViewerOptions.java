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

package com.cloud.consoleproxy;

public class ViewerOptions {
	  public int preferredEncoding = -1;
	  public int compressLevel = 5;
	  public int jpegQuality = 5;
	  public boolean useCopyRect = false;
	  public boolean requestCursorUpdates = true;
	  public boolean ignoreCursorUpdates = false;

	  public boolean eightBitColors = false;

	  public boolean reverseMouseButtons2And3 = false;
	  public boolean shareDesktop = true;
	  public boolean viewOnly = false;
	  public int scaleCursor = 0;

	  public boolean autoScale = false;
	  public int scalingFactor = 100;
}
