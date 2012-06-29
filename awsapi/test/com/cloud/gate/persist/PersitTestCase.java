// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
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
