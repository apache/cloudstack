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

package com.cloud.utils;

public class Profiler {
	private Long startTickInMs;
	private Long stopTickInMs;
	
	public Profiler() {
		startTickInMs = null;
		stopTickInMs = null;
	}
	
	public long start() {
		startTickInMs = System.currentTimeMillis();
		return startTickInMs.longValue();
	}
	
	public long stop() {
		stopTickInMs = System.currentTimeMillis();
		return stopTickInMs.longValue();
	}
	
	public long getDuration() {
	    if(startTickInMs != null &&  stopTickInMs != null)
	        return stopTickInMs.longValue() - startTickInMs.longValue();
	    
	    return -1;
	}
	
	public boolean isStarted() {
	    return startTickInMs != null;
	}
	
	public boolean isStopped() {
	    return stopTickInMs != null;
	}
	
	public String toString() {
	    if(startTickInMs == null)
	        return "Not Started";
	    
	    if(stopTickInMs == null)
	        return "Started but not stopped";
	    
	    return "Done. Duration: " + getDuration() + "ms";
	}
}
