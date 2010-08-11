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

package com.cloud.async;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.Assert;

import org.apache.log4j.Logger;

import com.cloud.async.AsyncJobResult;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.domain.dao.DomainDaoImpl;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressDaoImpl;
import com.cloud.server.ManagementServer;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.testcase.ComponentSetup;
import com.cloud.utils.testcase.ComponentTestCase;

@ComponentSetup(managerName="management-server", setupXml="async-job-component.xml")
public class TestAsyncJobManager extends ComponentTestCase {
    public static final Logger s_logger = Logger.getLogger(TestAsyncJobManager.class.getName());
    
    volatile long s_count = 0;

	public void asyncCall() {
		ManagementServer mgr = (ManagementServer)ComponentLocator.getComponent("management-server");

		long jobId = mgr.rebootVirtualMachineAsync(1, 1);
		s_logger.info("Async-call job id: " + jobId);
		
		while(true) {
			AsyncJobResult result;
			try {
				result = mgr.queryAsyncJobResult(jobId);
			
				if(result.getJobStatus() != AsyncJobResult.STATUS_IN_PROGRESS) {
					s_logger.info("Async-call completed, result: " + result.toString());
					break;
				}
				s_logger.info("Async-call is in progress, progress: " + result.toString());
				
			} catch (PermissionDeniedException e1) {
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public void sequence() {
		final HostDao hostDao = new HostDaoImpl();
		long seq = hostDao.getNextSequence(1);
		s_logger.info("******* seq : " + seq + " ********");
		
		HashMap<Long, Long> hashMap = new HashMap<Long, Long>();
		final Map<Long, Long> map = Collections.synchronizedMap(hashMap);
		
		s_count = 0;
		final long maxCount = 1000000;	// test one million times
		
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				while(s_count < maxCount) {
					s_count++;
					long seq = hostDao.getNextSequence(1);
					Assert.assertTrue(map.put(seq, seq) == null);
				}
			}
		});
		
		Thread t2 = new Thread(new Runnable() {
			public void run() {
				while(s_count < maxCount) {
					s_count++;
					long seq = hostDao.getNextSequence(1);
					Assert.assertTrue(map.put(seq, seq) == null);
				}
			}
		});
		
		t1.start();
		t2.start();
		
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
		}
	}
	
	public void ipAssignment() {
		final IPAddressDao ipAddressDao = new IPAddressDaoImpl();
		
		final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
		//final Map<String, String> map = Collections.synchronizedMap(hashMap);
		
		s_count = 0;
		final long maxCount = 1000000;	// test one million times
		
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				while(s_count < maxCount) {
					s_count++;
					
					Transaction txn = Transaction.open("Alex1");
					try {
						String addr = ipAddressDao.assignIpAddress(1, 0, 1, false);
						String returnStr = map.put(addr, addr);
						if(returnStr != null) {
							System.out.println("addr : " + addr);
						}
						Assert.assertTrue(returnStr == null);
					} finally {
						txn.close();
					}
				}
			}
		});
		
		Thread t2 = new Thread(new Runnable() {
			public void run() {
				while(s_count < maxCount) {
					s_count++;
					
					Transaction txn = Transaction.open("Alex2");
					try {
						String addr = ipAddressDao.assignIpAddress(1, 0, 1, false);
						Assert.assertTrue(map.put(addr, addr) == null);
					} finally {
						txn.close();
					}
				}
			}
		});
		
		t1.start();
		t2.start();
		
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
		}
	}
	
	private long getRandomLockId() {
		return 1L;
		
		/*
		 *  will use in the future test cases
		int i = new Random().nextInt();
		if(i % 2 == 0)
			return 1L;
		return 2L;
		*/
	}
	
	public void tstLocking() {
		
		int testThreads = 20;
		Thread[] threads = new Thread[testThreads];
		
		for(int i = 0; i < testThreads; i++) {
			final int current = i;
			threads[i] = new Thread(new Runnable() {
				public void run() {
					
					final HostDao hostDao = new HostDaoImpl();
					while(true) {
						Transaction txn = Transaction.currentTxn();
						try {
							HostVO host = hostDao.acquire(getRandomLockId(), 10);
							if(host != null) {
								s_logger.info("Thread " + (current + 1) + " acquired lock");
								
								try { Thread.sleep(getRandomMilliseconds(1000, 5000)); } catch (InterruptedException e) {}
								
								s_logger.info("Thread " + (current + 1) + " released lock");
								hostDao.release(host.getId());
								
								try { Thread.sleep(getRandomMilliseconds(1000, 5000)); } catch (InterruptedException e) {}
							} else {
								s_logger.info("Thread " + (current + 1) + " is not able to acquire lock");
							}
						} finally {
							txn.close();
						}
					}
				}
			});
			threads[i].start();
		}
		
		try {
			for(int i = 0; i < testThreads; i++)
				threads[i].join();
		} catch(InterruptedException e) {
		}
	}
	
	public void testDomain() {
		getRandomMilliseconds(1, 100);		
		DomainDao domainDao = new DomainDaoImpl();
		
		DomainVO domain1 = new DomainVO("d1", 2L, 1L);
		domainDao.create(domain1);
		
		DomainVO domain2 = new DomainVO("d2", 2L, 1L);
		domainDao.create(domain2);
		
		DomainVO domain3 = new DomainVO("d3", 2L, 1L);
		domainDao.create(domain3);

		DomainVO domain11 = new DomainVO("d11", 2L, domain1.getId());
		domainDao.create(domain11);
		
		domainDao.remove(domain11.getId());
		
		DomainVO domain12 = new DomainVO("d12", 2L, domain1.getId());
		domainDao.create(domain12);
		
		domainDao.remove(domain3.getId());
		DomainVO domain4 = new DomainVO("d4", 2L, 1L);
		domainDao.create(domain4);
	}
}
