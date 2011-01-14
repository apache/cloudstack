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

package com.cloud.agent.resource.computing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.libvirt.Connect;

import com.cloud.utils.script.Script;

public class KVMHAMonitor extends KVMHABase implements Runnable{
	private static final Logger s_logger = Logger.getLogger(KVMHAMonitor.class);
	private Map<String, NfsStoragePool> _storagePool = new HashMap<String, NfsStoragePool>();
	
	private String _hostIP; /*private ip address*/

		
	public KVMHAMonitor(NfsStoragePool pool, String host, String scriptPath) {
		if (pool != null) {
			this._storagePool.put(pool._poolUUID, pool);
		}	
		this._hostIP = host;
		this._heartBeatPath = scriptPath;
	}
	
	public void addStoragePool(NfsStoragePool pool) {
		synchronized (_storagePool) {
			this._storagePool.put(pool._poolUUID, pool);
		}
	}
	
	public void removeStoragePool(NfsStoragePool pool) {
		synchronized (_storagePool) {
			this._storagePool.remove(pool._poolUUID);
		}
	}
	
	public List<NfsStoragePool> getStoragePools() {
		synchronized (_storagePool) {
			return new ArrayList<NfsStoragePool>(_storagePool.values());
		}
	}
	
	private class Monitor implements Runnable {
		
		@Override
		public void run() {
			synchronized (_storagePool) {
				for (NfsStoragePool primaryStoragePool : _storagePool.values()) {
					
					Script cmd = new Script(_heartBeatPath, _heartBeatUpdateTimeout, s_logger);
					cmd.add("-i", primaryStoragePool._poolIp);
					cmd.add("-p", primaryStoragePool._poolMountSourcePath);
					cmd.add("-m", primaryStoragePool._mountDestPath);
					cmd.add("-h", _hostIP);
					String result = cmd.execute();
					if (result != null) {
						s_logger.debug("write heartbeat failed: " + result);
					}
				}
			}
			
		}		
	}

	@Override
	public void run() {
		//s_logger.addAppender(new org.apache.log4j.ConsoleAppender(new org.apache.log4j.PatternLayout(), "System.out"));
		while (true) {
			Thread monitorThread = new Thread(new Monitor());
			monitorThread.start();
			try {
				monitorThread.join();
			} catch (InterruptedException e) {
				
			}
			
			try {
				Thread.sleep(_heartBeatUpdateFreq);
			} catch (InterruptedException e) {
				
			}
		}
	}
	
}
