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
package com.cloud.async;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.inject.Inject;
import junit.framework.TestCase;

import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.messagebus.TopicConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.async.AsyncJobJournalVO;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobMonitor;
import com.cloud.async.dao.AsyncJobJoinMapDao;
import com.cloud.async.dao.AsyncJobJournalDao;
import com.cloud.cluster.ClusterManager;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.utils.Predicate;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.Transaction;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/AsyncJobTestContext.xml")
public class TestAsyncJobManager extends TestCase {

    @Inject AsyncJobManager asyncMgr;
    @Inject ClusterManager clusterMgr;
    @Inject MessageBus messageBus;
    @Inject AsyncJobMonitor jobMonitor;
    @Inject AsyncJobJournalDao journalDao;
    @Inject AsyncJobJoinMapDao joinMapDao;
    @Inject AccountManager accountMgr;
    
    @Before                                                  
    public void setUp() {
    	ComponentContext.initComponentsLifeCycle();
    	Mockito.when(clusterMgr.getManagementNodeId()).thenReturn(1L);
    	
    	AccountVO account = new AccountVO();
    	Mockito.when(accountMgr.getSystemAccount()).thenReturn(account);
    	UserVO user = new UserVO();
    	Mockito.when(accountMgr.getSystemUser()).thenReturn(user);
    	
    	Transaction.open("dummy");
    	
		// drop constraint check in order to do single table test
		Statement stat = null;
		try {
			stat = Transaction.currentTxn().getConnection().createStatement();
			stat.execute("SET foreign_key_checks = 0;");
		} catch (SQLException e) {
		} finally {
			if(stat != null) {
				try {
					stat.close();
				} catch (SQLException e) {
				}
			}
		}
    }                                                        
                                                             
    @After                                                   
    public void tearDown() {                                 
    	Transaction.currentTxn().close();                    
    }        
    
    @Test
    public void testJobJournal() {
    	AsyncJobJournalVO journal = new AsyncJobJournalVO();
    	journal.setJobId(1L);
    	journal.setJournalType(AsyncJob.JournalType.SUCCESS);
    	journal.setJournalText("Journal record 1");
    	
    	journalDao.persist(journal);
    	
    	AsyncJobJournalVO journal2 = new AsyncJobJournalVO();
    	journal2.setJobId(1L);
    	journal2.setJournalType(AsyncJob.JournalType.SUCCESS);
    	journal2.setJournalText("Journal record 2");
    	
    	journalDao.persist(journal2);
    	
    	List<AsyncJobJournalVO> l = journalDao.getJobJournal(1L);
    	Assert.assertTrue(l.size() == 2);
    	journal = l.get(0);
    	Assert.assertTrue(journal.getJournalText().equals("Journal record 1"));
    	
    	journal2 = l.get(1);
    	Assert.assertTrue(journal2.getJournalText().equals("Journal record 2"));
    	
    	journalDao.expunge(journal.getId());
    	journalDao.expunge(journal2.getId());
    }
    
    @Test
    public void testJoinMapDao() {
    	joinMapDao.joinJob(2, 1, 100, null, null, null);
    	joinMapDao.joinJob(3, 1, 100, null, null, null);
  
    	AsyncJobJoinMapVO record = joinMapDao.getJoinRecord(2, 1);
    	Assert.assertTrue(record != null);
    	Assert.assertTrue(record.getJoinMsid() == 100);
    	Assert.assertTrue(record.getJoinStatus() == AsyncJobConstants.STATUS_IN_PROGRESS);
    	
    	joinMapDao.completeJoin(1, AsyncJobConstants.STATUS_SUCCEEDED, "Done", 101);
    	
    	record = joinMapDao.getJoinRecord(2, 1);
    	Assert.assertTrue(record != null);
    	Assert.assertTrue(record.getJoinMsid() == 100);
    	Assert.assertTrue(record.getJoinStatus() == AsyncJobConstants.STATUS_SUCCEEDED);
    	Assert.assertTrue(record.getJoinResult().equals("Done"));
    	Assert.assertTrue(record.getCompleteMsid() == 101);
    	
    	record = joinMapDao.getJoinRecord(3, 1);
    	Assert.assertTrue(record != null);
    	Assert.assertTrue(record.getJoinMsid() == 100);
    	Assert.assertTrue(record.getJoinStatus() == AsyncJobConstants.STATUS_SUCCEEDED);
    	Assert.assertTrue(record.getJoinResult().equals("Done"));
    	Assert.assertTrue(record.getCompleteMsid() == 101);
    	
    	joinMapDao.disjoinJob(2, 1);
    	joinMapDao.disjoinJob(3, 1);
    }
    
    @Test
    public void testPseudoJob() {
    	AsyncJob job = asyncMgr.getPseudoJob();
    	Assert.assertTrue(job.getInstanceType().equals(AsyncJobConstants.PSEUDO_JOB_INSTANCE_TYPE));
    	Assert.assertTrue(job.getInstanceId().longValue() == Thread.currentThread().getId());
    }
    
    @Test
    public void testWaitAndCheck() {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				for(int i = 0; i < 2; i++) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
					System.out.println("Publish wakeup message");
					messageBus.publish(null, "VM", PublishScope.GLOBAL, null);
				}
			}
		});
		thread.start();
    
		jobMonitor.registerActiveTask(1, 1, false);
		
    	asyncMgr.waitAndCheck(new String[] {"VM"}, 5000L, 10000L, new Predicate() {
    		public boolean checkCondition() {
    			System.out.println("Check condition to exit");
    			messageBus.publish(null, TopicConstants.JOB_HEARTBEAT, PublishScope.LOCAL, 1L);
    			return false;
    		}
    	});
    	
		jobMonitor.unregisterActiveTask(1);
    	
    	try {
    		thread.join();
    	} catch(InterruptedException e) {
    	}
    }
}
