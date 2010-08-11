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

package com.cloud.utils.db;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.utils.Profiler;

//
// Wrapper class for global database lock to reduce contention for database connections from within process
//
// Example of using dynamic named locks
//
//		GlobalLock lock = GlobalLock.getInternLock("some table name" + rowId);
//		
//		if(lock.lock()) {
//			try {
//				do something
//			} finally {
//				lock.unlock();
//			}
//		}
//		lock.releaseRef();
//
//
// 		GlobalLock.Auto lock = GlobalLock.getAutoInternLock("some table name" + rowId);
//		if(lock.lock()) {
//			try {
//				do something
//			} finally {
//				lock.unlock();
//			}
//		}
//
public class GlobalLock {
    protected final static Logger s_logger = Logger.getLogger(GlobalLock.class);

	private String name;
	private volatile int lockCount = 0;
	private Thread ownerThread = null;
	
	private int referenceCount = 0;
	private long holdingStartTick = 0;
	
	private static Map<String, GlobalLock> s_lockMap = new HashMap<String, GlobalLock>();
	
	private GlobalLock(String name) {
		this.name = name;
	}
	
	public int addRef() {
		synchronized(this) {
			referenceCount++;
			return referenceCount;
		}
	}
	
	public int releaseRef() {
		boolean releaseInternLock = false;
		int refCount;
		synchronized(this) {
			referenceCount--;
			refCount = referenceCount;
			
			if(referenceCount < 0)
				s_logger.warn("Unmatched Global lock " + name + " reference usage detected, check your code!");
			
			if(referenceCount == 0)
				releaseInternLock = true;
		}
		
		if(releaseInternLock)
			releaseInternLock(name);
		return refCount;
	}
	
	public static GlobalLock.Auto getAutoInternLock(String name) {
		return new GlobalLock.Auto(getInternLock(name));
	}
	
	public static GlobalLock getInternLock(String name) {
		synchronized(s_lockMap) {
			if(s_lockMap.containsKey(name)) {
				GlobalLock lock = s_lockMap.get(name);
				lock.addRef();
				return lock;
			} else {
				GlobalLock lock = new GlobalLock(name);
				lock.addRef();
				s_lockMap.put(name, lock);
				return lock;
			}
		}
	}
	
	private static void releaseInternLock(String name) {
		synchronized(s_lockMap) {
			s_lockMap.remove(name);
		}
	}
	
	public boolean lock(int timeoutSeconds) {
		int remainingMilliSeconds = timeoutSeconds*1000;
		Profiler profiler = new Profiler();
		boolean interrupted = false;
		try {
			while(true) {
				synchronized(this) {
					if(ownerThread != null && ownerThread == Thread.currentThread()) {
						s_logger.warn("Global lock re-entrance detected");
						
						lockCount++;
	
						if(s_logger.isTraceEnabled())
							s_logger.trace("lock " + name + " is acquired, lock count :" + lockCount);
						return true;
					}
					
					if(ownerThread != null) {
						profiler.start();
						try {
							wait(((long)timeoutSeconds)*1000L);
						} catch (InterruptedException e) {
							interrupted = true;
						}
						profiler.stop();
						
						remainingMilliSeconds -= profiler.getDuration();
						if(remainingMilliSeconds < 0)
							return false;
						
						continue;
					} else {
						// we will discount the time that has been spent in previous waiting
						if(DbUtil.getGlobalLock(name, remainingMilliSeconds / 1000)) {
							lockCount++;
							ownerThread = Thread.currentThread();
							holdingStartTick = System.currentTimeMillis();
							
							// keep the lock in the intern map when we got the lock from database
							addRef();
							
							if(s_logger.isTraceEnabled())
								s_logger.trace("lock " + name + " is acquired, lock count :" + lockCount);
							return true;
						}
						return false;
					}
				}
			}
		} finally {
			if(interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}
	
	public boolean unlock() {
		synchronized(this) {
			if(ownerThread != null && ownerThread == Thread.currentThread()) {
				lockCount--;
				if(lockCount == 0) {
					ownerThread = null;
					DbUtil.releaseGlobalLock(name);
					
					if(s_logger.isTraceEnabled())
						s_logger.trace("lock " + name + " is returned to free state, total holding time :" + 
							(System.currentTimeMillis() - holdingStartTick));
					holdingStartTick = 0;
					
					// release holding position in intern map when we released the DB connection
					releaseRef();
					notifyAll();
				}
				
				if(s_logger.isTraceEnabled())
					s_logger.trace("lock " + name + " is released, lock count :" + lockCount);
				return true;
			}
			return false;
		}
	}
	
	public String getName() {
		return name;
	}
	
	public static class Auto {
		private GlobalLock lock;
		
		public Auto(GlobalLock lock) {
			this.lock = lock;
		}
		
		protected void finalize() throws Throwable {
		    try {
		    	lock.releaseRef();
		    } finally {
		        super.finalize();
		    }
		}
		
		public boolean lock(int timeoutSeconds) {
			return lock.lock(timeoutSeconds); 
		}
		
		public boolean unlock() {
			return lock.unlock();
		}
	}
}
