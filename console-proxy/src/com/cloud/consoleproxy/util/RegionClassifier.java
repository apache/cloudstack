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

public class RegionClassifier {
	private List<Region> regionList;
	
	public RegionClassifier() {
		regionList = new ArrayList<Region>();
	}
	
	public void add(Rectangle rect) {
		// quickly identify that if we need a new region
		boolean newRegion = true;
		Rectangle rcInflated = new Rectangle(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2);
		for(Region region : regionList) {
			if(region.getBound().intersects(rcInflated)) {
				newRegion = false;
				break;
			}
		}
		
		if(newRegion) {
			regionList.add(new Region(rect));
		} else {
			for(Region region : regionList) {
				if(region.add(rect))
					return;
			}
			regionList.add(new Region(rect));
		}
	}
	
	public List<Region> getRegionList() {
		return regionList;
	}
	
	public void clear() {
		regionList.clear();
	}
}
