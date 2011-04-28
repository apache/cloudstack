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

package com.cloud.vm;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cloud.cluster.StackMaid;
import com.cloud.utils.Pair;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;

//
// TODO: simple load scanner, to minimize code changes required in console proxy manager and SSVM, we still leave most of work at handler
//
public class SystemVmLoadScanner<T> {
	public enum AfterScanAction { nop, expand, shrink }
	
    private static final Logger s_logger = Logger.getLogger(SystemVmLoadScanner.class);

    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3;   // 3 seconds
    
	private final SystemVmLoadScanHandler<T> _scanHandler;
    private final ScheduledExecutorService _capacityScanScheduler;
    private final GlobalLock _capacityScanLock;
	
	public SystemVmLoadScanner(SystemVmLoadScanHandler<T> scanHandler) {
		_scanHandler = scanHandler;
		_capacityScanScheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory(scanHandler.getScanHandlerName()));
		_capacityScanLock = GlobalLock.getInternLock(scanHandler.getScanHandlerName() + ".scan.lock");
	}
	
	public void initScan(long startupDelayMs, long scanIntervalMs) {
        _capacityScanScheduler.scheduleAtFixedRate(getCapacityScanTask(), startupDelayMs, scanIntervalMs, TimeUnit.MILLISECONDS);
	}
	
	public void stop() {
        _capacityScanScheduler.shutdownNow();

        try {
            _capacityScanScheduler.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }

        _capacityScanLock.releaseRef();
	}
	
    private Runnable getCapacityScanTask() {
        return new Runnable() {

            @Override
            public void run() {
                Transaction txn = Transaction.open(Transaction.CLOUD_DB);
                try {
                    reallyRun();
                } catch (Throwable e) {
                    s_logger.warn("Unexpected exception " + e.getMessage(), e);
                } finally {
                    StackMaid.current().exitCleanup();
                    txn.close();
                }
            }

            private void reallyRun() {
            	loadScan();
            }
        };
    }
    
    private void loadScan() {
    	if(!_scanHandler.canScan()) {
    		return;
    	}
    	
        if (!_capacityScanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Capacity scan lock is used by others, skip and wait for my turn");
            }
            return;
        }
        
        try {
	    	_scanHandler.onScanStart();
	
	    	T[] pools = _scanHandler.getScannablePools();
	    	for(T p : pools) {
	    		if(_scanHandler.isPoolReadyForScan(p)) {
	    			Pair<AfterScanAction, Object> actionInfo = _scanHandler.scanPool(p);
	    			
	    			switch(actionInfo.first()) {
	    			case nop:
	    				break;
	    				
	    			case expand:
	    				_scanHandler.expandPool(p, actionInfo.second());
	    				break;
	    				
	    			case shrink:
	    				_scanHandler.shrinkPool(p, actionInfo.second());
	    				break;
	    			}
	    		}
	    	}
	    	
	    	_scanHandler.onScanEnd();
    	
        } finally {
        	_capacityScanLock.unlock();
        }
    }
}

