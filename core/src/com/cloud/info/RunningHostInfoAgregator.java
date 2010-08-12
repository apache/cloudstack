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

package com.cloud.info;

import java.util.HashMap;
import java.util.Map;

import com.cloud.host.Host;

public class RunningHostInfoAgregator {
	
	public static class ZoneHostInfo {
		public static int COMPUTING_HOST_MASK = 1;
		public static int ROUTING_HOST_MASK = 2;
		public static int STORAGE_HOST_MASK = 4;
		public static int ALL_HOST_MASK = COMPUTING_HOST_MASK | ROUTING_HOST_MASK | STORAGE_HOST_MASK;
		
		private long dcId;
		
		// (1 << 0) : at least one computing host is running in the zone
		// (1 << 1) : at least one routing host is running in the zone
		// (1 << 2) : at least one storage host is running in the zone
		private int flags = 0;
		
		public long getDcId() {
			return dcId;
		}
		
		public void setDcId(long dcId) {
			this.dcId = dcId;
		}
		
		public void setFlag(int flagMask) {
			flags |= flagMask;  
		}
		
		public int getFlags() {
			return flags; 
		}
	}
	
	private Map<Long, ZoneHostInfo> zoneHostInfoMap = new HashMap<Long, ZoneHostInfo>();
	
	public RunningHostInfoAgregator() {
	}
	
	public void aggregate(RunningHostCountInfo countInfo) {
		if(countInfo.getCount() > 0) {
			ZoneHostInfo zoneInfo = getZoneHostInfo(countInfo.getDcId());
			
			Host.Type type = Enum.valueOf(Host.Type.class, countInfo.getHostType());
			if(type == Host.Type.Routing) {
				zoneInfo.setFlag(ZoneHostInfo.COMPUTING_HOST_MASK);
				zoneInfo.setFlag(ZoneHostInfo.ROUTING_HOST_MASK);
			} else if(type == Host.Type.Storage || type == Host.Type.SecondaryStorage) {
				zoneInfo.setFlag(ZoneHostInfo.STORAGE_HOST_MASK);
			}
		}
	}
	
	public Map<Long, ZoneHostInfo> getZoneHostInfoMap() {
		return zoneHostInfoMap;
	}
	
	private ZoneHostInfo getZoneHostInfo(long dcId) {
		if(zoneHostInfoMap.containsKey(dcId))
			return zoneHostInfoMap.get(dcId);
		
		ZoneHostInfo info = new ZoneHostInfo();
		info.setDcId(dcId);
		zoneHostInfoMap.put(dcId, info);
		return info;
	}
}
