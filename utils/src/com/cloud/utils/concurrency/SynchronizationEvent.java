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
package com.cloud.utils.concurrency;

import org.apache.log4j.Logger;

public class SynchronizationEvent {
    protected final static Logger s_logger = Logger.getLogger(SynchronizationEvent.class);
	
	private boolean signalled;
	
	public SynchronizationEvent() {
		signalled = false;
	}
	
	public SynchronizationEvent(boolean signalled) {
		this.signalled = signalled;
	}
	
	public void setEvent() {
		synchronized(this) {
			signalled = true;
			notifyAll();
		}
	}
	
	public void resetEvent() {
		synchronized(this) {
			signalled = false;
		}		
	}
	
	public boolean waitEvent() {
		synchronized(this) {
			if(signalled)
				return true;
			
			while(true) {
				try {
					wait();
					assert(signalled);
					return signalled;
				} catch (InterruptedException e) {
					s_logger.debug("unexpected awaken signal in wait()");
				}
			}
		}
	}
	
	public boolean waitEvent(long timeOutMiliseconds) {
		synchronized(this) {
			if(signalled)
				return true;
			
			try {
				wait(timeOutMiliseconds);
				return signalled;
			} catch (InterruptedException e) {
				// TODO, we don't honor time out semantics when the waiting thread is interrupted
				s_logger.debug("unexpected awaken signal in wait(...)");
				return false;
			}
		}
	}
	
	public boolean isSignalled() {
		synchronized(this) {
			return signalled;
		}
	}
}
