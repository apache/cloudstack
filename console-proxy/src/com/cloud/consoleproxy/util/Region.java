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

package com.cloud.consoleproxy.util;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class Region {
	private Rectangle bound;
	private List<Rectangle> rectList;

	public Region() {
		bound = new Rectangle(0, 0, 0, 0);
		rectList = new ArrayList<Rectangle>();
	}
	
	public Region(Rectangle rect) {
		bound = new Rectangle(rect.x, rect.y, rect.width, rect.height);
		rectList = new ArrayList<Rectangle>();
		rectList.add(rect);
	}
	
	public Rectangle getBound() {
		return bound;
	}
	
	public void clearBound() {
		assert(rectList.size() == 0);
		bound.x = bound.y = bound.width = bound.height = 0;
	}
	
	public List<Rectangle> getRectangles() {
		return rectList;
	}
	
	public boolean add(Rectangle rect) {
		if(bound.isEmpty()) {
			assert(rectList.size() == 0);
			bound.x = rect.x;
			bound.y = rect.y;
			bound.width = rect.width;
			bound.height = rect.height;
			
			rectList.add(rect);
			return true;
		}
		
		Rectangle rcInflated = new Rectangle(rect.x - 1, rect.y- 1, rect.width + 2, rect.height + 2);
		if(!bound.intersects(rcInflated))
			return false;

		for(Rectangle r : rectList) {
			if(r.intersects(rcInflated)) {
				if(!r.contains(rect)) {
					enlargeBound(rect);
					rectList.add(rect);
					return true;
				}
			}
		}
		return false;
	}
	
	private void enlargeBound(Rectangle rect) {
		int boundLeft = Math.min(bound.x, rect.x);
		int boundTop = Math.min(bound.y, rect.y);
		int boundRight = Math.max(bound.x + bound.width, rect.x + rect.width);
		int boundBottom = Math.max(bound.y + bound.height, rect.y + rect.height);
		
		bound.x = boundLeft;
		bound.y = boundTop;
		bound.width = boundRight - boundLeft;
		bound.height = boundBottom - boundTop;
	}
}
