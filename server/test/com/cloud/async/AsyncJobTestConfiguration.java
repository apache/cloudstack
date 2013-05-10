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

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloud.api.ApiDispatcher;
import com.cloud.async.dao.AsyncJobDao;
import com.cloud.async.dao.AsyncJobDaoImpl;
import com.cloud.async.dao.AsyncJobJoinMapDao;
import com.cloud.async.dao.AsyncJobJoinMapDaoImpl;
import com.cloud.async.dao.AsyncJobJournalDao;
import com.cloud.async.dao.AsyncJobJournalDaoImpl;
import com.cloud.async.dao.SyncQueueDao;
import com.cloud.async.dao.SyncQueueDaoImpl;
import com.cloud.async.dao.SyncQueueItemDao;
import com.cloud.async.dao.SyncQueueItemDaoImpl;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dao.EntityManager;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.vm.VirtualMachineManager;

@Configuration
public class AsyncJobTestConfiguration {

	@Bean
	public ClusterManager clusterManager() {
		return Mockito.mock(ClusterManager.class);
	}
	
	@Bean
	public AsyncJobDao asyncJobDao() {
		return new AsyncJobDaoImpl();
	}
	
	@Bean
	public AccountManager accountManager() {
		return Mockito.mock(AccountManager.class);
	}
	
	@Bean
	public ApiDispatcher apiDispatcher() {
		return Mockito.mock(ApiDispatcher.class);
	}
	
	@Bean
	public EntityManager entityManager() {
		return Mockito.mock(EntityManager.class);
	}
	
	@Bean
	public SyncQueueManager syncQueueManager() {
		return new SyncQueueManagerImpl();
	}
	
	@Bean
	public AccountDao accountDao() {
		return Mockito.mock(AccountDao.class);
	}
	
	@Bean
	public SyncQueueDao syncQueueDao() {
		return new SyncQueueDaoImpl();
	}
	
	@Bean
	public SyncQueueItemDao syncQueueItemDao() {
		return new SyncQueueItemDaoImpl();
	}
	
	@Bean 
	public ConfigurationDao configurationDao() {
		return Mockito.mock(ConfigurationDao.class);
	}
	
	@Bean
	public ConfigurationManager configurationManager() {
		return Mockito.mock(ConfigurationManager.class);
	}
	
	@Bean
	public VirtualMachineManager virtualMachineManager() {
		return Mockito.mock(VirtualMachineManager.class);
	}
	
	@Bean
	public AsyncJobJournalDao asyncJobJournalDao() {
		return new AsyncJobJournalDaoImpl();
	}
	
	@Bean
	public AsyncJobJoinMapDao asyncJobJoinMapDao() {
		return new AsyncJobJoinMapDaoImpl();
	}
}
