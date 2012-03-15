package com.cloud.gate.persist;

import org.apache.log4j.Logger;

import com.cloud.bridge.persist.PersistContext;
import com.cloud.gate.testcase.BaseTestCase;

public class PersitTestCase extends BaseTestCase {
    protected final static Logger logger = Logger.getLogger(PersitTestCase.class);
    
    public void testNamedLock() {
    	Thread t1 = new Thread(new Runnable() {
    		public void run() {
    			for(int i = 0; i < 10; i++) {
    				if(PersistContext.acquireNamedLock("TestLock", 3)) {
    					logger.info("Thread 1 acquired lock");
    					try {
							Thread.currentThread().sleep(BaseTestCase.getRandomMilliseconds(5000, 10000));
						} catch (InterruptedException e) {
						}
    					logger.info("Thread 1 to release lock");
    					PersistContext.releaseNamedLock("TestLock");
    				} else {
    					logger.info("Thread 1 is unable to acquire lock");
    				}
    			}
    		}
    	});
    	
    	Thread t2 = new Thread(new Runnable() {
    		public void run() {
    			for(int i = 0; i < 10; i++) {
    				if(PersistContext.acquireNamedLock("TestLock", 3)) {
    					logger.info("Thread 2 acquired lock");
    					try {
							Thread.currentThread().sleep(BaseTestCase.getRandomMilliseconds(1000, 5000));
						} catch (InterruptedException e) {
						}
    					logger.info("Thread 2 to release lock");
    					PersistContext.releaseNamedLock("TestLock");
    				} else {
    					logger.info("Thread 2 is unable to acquire lock");
    				}
    			}
    		}
    	});
    	
    	t1.start();
    	t2.start();
    	
    	try {
	    	t1.join();
	    	t2.join();
    	} catch(InterruptedException e) {
    	}
    }
}
