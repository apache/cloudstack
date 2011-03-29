package com.cloud.vm;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cloud.maid.StackMaid;
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
		_capacityScanScheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory(scanHandler.getClass().getSimpleName()));
		_capacityScanLock = GlobalLock.getInternLock(scanHandler.getClass().getSimpleName() + ".scan.lock");
	}
	
	public void initScan(long startupDelayMs, long scanIntervalMs) {
        _capacityScanScheduler.scheduleAtFixedRate(getCapacityScanTask(), startupDelayMs, scanIntervalMs, TimeUnit.MILLISECONDS);
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
	    			switch(_scanHandler.scanPool(p)) {
	    			case nop:
	    				break;
	    				
	    			case expand:
	    				_scanHandler.expandPool(p);
	    				break;
	    				
	    			case shrink:
	    				_scanHandler.shrinkPool(p);
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

