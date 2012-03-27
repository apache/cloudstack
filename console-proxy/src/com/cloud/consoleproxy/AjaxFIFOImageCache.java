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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.consoleproxy.util.Logger;

public class AjaxFIFOImageCache {
	private static final Logger s_logger = Logger.getLogger(AjaxFIFOImageCache.class);
	
	private List<Integer> fifoQueue;
	private Map<Integer, byte[]> cache;
	private int cacheSize;
	private int nextKey = 1;
	
	public AjaxFIFOImageCache(int cacheSize) {
		this.cacheSize = cacheSize;
		fifoQueue = new ArrayList<Integer>();
		cache = new HashMap<Integer, byte[]>();
	}
	
	public synchronized void clear() {
		fifoQueue.clear();
		cache.clear();
	}
	
	public synchronized int putImage(byte[] image) {
		while(cache.size() >= cacheSize) {
			Integer keyToRemove = fifoQueue.remove(0);
			cache.remove(keyToRemove);
			
			if(s_logger.isTraceEnabled())
				s_logger.trace("Remove image from cache, key: " + keyToRemove);
		}
		
		int key = getNextKey();
		
		if(s_logger.isTraceEnabled())
			s_logger.trace("Add image to cache, key: " + key);
		
		cache.put(key, image);
		fifoQueue.add(key);
		return key;
	}
	
	public synchronized byte[] getImage(int key) {
		if(cache.containsKey(key)) {
			if(s_logger.isTraceEnabled())
				s_logger.trace("Retrieve image from cache, key: " + key);
			
			return cache.get(key);
		}
		
		if(s_logger.isTraceEnabled())
			s_logger.trace("Image is no long in cache, key: " + key);
		return null;
	}
	
	public synchronized int getNextKey() {
		return nextKey++;
	}
}
