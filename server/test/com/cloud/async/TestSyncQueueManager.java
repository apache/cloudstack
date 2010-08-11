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

import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.apache.log4j.Logger;

import com.cloud.async.SyncQueueItemVO;
import com.cloud.async.SyncQueueManager;
import com.cloud.async.SyncQueueVO;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.testcase.ComponentSetup;
import com.cloud.utils.testcase.ComponentTestCase;

@ComponentSetup(managerName="management-server", setupXml="sync-queue-component.xml")
public class TestSyncQueueManager extends ComponentTestCase {
    public static final Logger s_logger = Logger.getLogger(TestSyncQueueManager.class.getName());
    
    private volatile int count = 0;
    private volatile long expectingCurrent = 1;

	public void leftOverItems() {
		SyncQueueManager mgr = ComponentLocator.getCurrentLocator().getManager(
			SyncQueueManager.class);

		List<SyncQueueItemVO> l = mgr.getActiveQueueItems(1L);
		if(l != null && l.size() > 0) {
			for(SyncQueueItemVO item : l) {
				s_logger.info("Left over item: " + item.toString());
				mgr.purgeItem(item.getId());
			}
		}
	}

	public void dequeueFromOneQueue() {
		final SyncQueueManager mgr = ComponentLocator.getCurrentLocator().getManager(
			SyncQueueManager.class);
		
		final int totalRuns = 5000;
		final SyncQueueVO queue = mgr.queue("vm_instance", 1L, "Async-job", 1);
		for(int i = 1; i < totalRuns; i++)
			mgr.queue("vm_instance", 1L, "Async-job", i+1);
		
		count = 0;
		expectingCurrent = 1;
		Thread thread1 = new Thread(new Runnable() {
				public void run() {
					while(count < totalRuns) {
						SyncQueueItemVO item = mgr.dequeueFromOne(queue.getId(), 1L);
						if(item != null) {
							s_logger.info("Thread 1 process item: " + item.toString());
							
							Assert.assertEquals(expectingCurrent, item.getContentId().longValue());
							expectingCurrent++;
							count++;
							
							mgr.purgeItem(item.getId());
						}
						try {
							Thread.sleep(getRandomMilliseconds(1, 10));
						} catch (InterruptedException e) {
						}
					}
				}
			}
		);
		
		Thread thread2 = new Thread(new Runnable() {
			public void run() {
					while(count < totalRuns) {
						SyncQueueItemVO item = mgr.dequeueFromOne(queue.getId(), 1L);
						if(item != null) {
							s_logger.info("Thread 2 process item: " + item.toString());
							
							Assert.assertEquals(expectingCurrent, item.getContentId().longValue());
							expectingCurrent++;
							count++;
							mgr.purgeItem(item.getId());
						}
						
						try {
							Thread.sleep(getRandomMilliseconds(1, 10));
						} catch (InterruptedException e) {
						}
					}
				}
			}
		); 
		
		thread1.start();
		thread2.start();
		try {
			thread1.join();
		} catch (InterruptedException e) {
		}
		try {
			thread2.join();
		} catch (InterruptedException e) {
		}
		
		Assert.assertEquals(totalRuns, count);
	}
	
	public void dequeueFromAnyQueue() {
		final SyncQueueManager mgr = ComponentLocator.getCurrentLocator().getManager(
			SyncQueueManager.class);

		// simulate 30 queues
		final int queues = 30;
		final int totalRuns = 100;
		final int itemsPerRun = 20;
		for(int q = 1; q <= queues; q++)
			for(int i = 0; i < totalRuns; i++)
				mgr.queue("vm_instance", q, "Async-job", i+1);
		
		count = 0;
		Thread thread1 = new Thread(new Runnable() {
				public void run() {
					while(count < totalRuns*queues) {
						List<SyncQueueItemVO> l = mgr.dequeueFromAny(1L, itemsPerRun);
						if(l != null && l.size() > 0) {
							s_logger.info("Thread 1 get " + l.size() + " dequeued items");
							
							for(SyncQueueItemVO item : l) {
								s_logger.info("Thread 1 process item: " + item.toString());
								count++;
								
								mgr.purgeItem(item.getId());
							}
						}
						try {
							Thread.sleep(getRandomMilliseconds(1, 10));
						} catch (InterruptedException e) {
						}
					}
				}
			}
		);
		
		Thread thread2 = new Thread(new Runnable() {
			public void run() {
					while(count < totalRuns*queues) {
						List<SyncQueueItemVO> l = mgr.dequeueFromAny(1L, itemsPerRun);
						if(l != null && l.size() > 0) {
							s_logger.info("Thread 2 get " + l.size() + " dequeued items");
							
							for(SyncQueueItemVO item : l) {
								s_logger.info("Thread 2 process item: " + item.toString());
								count++;
								mgr.purgeItem(item.getId());
							}
						}
						
						try {
							Thread.sleep(getRandomMilliseconds(1, 10));
						} catch (InterruptedException e) {
						}
					}
				}
			}
		); 
		
		thread1.start();
		thread2.start();
		try {
			thread1.join();
		} catch (InterruptedException e) {
		}
		try {
			thread2.join();
		} catch (InterruptedException e) {
		}
		Assert.assertEquals(queues*totalRuns, count);
	}
	
	public void testPopulateQueueData() {
		final int queues = 30000;
		final int totalRuns = 100;
		
		final SyncQueueManager mgr = ComponentLocator.getCurrentLocator().getManager(
				SyncQueueManager.class);
		for(int q = 1; q <= queues; q++)
			for(int i = 0; i < totalRuns; i++)
				mgr.queue("vm_instance", q, "Async-job", i+1);
	}
}
