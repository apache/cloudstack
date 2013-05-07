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

import com.cloud.async.dao.AsyncJobJoinMapDao;
import com.cloud.async.dao.AsyncJobJoinMapDaoImpl;
import com.cloud.async.dao.SyncQueueDao;
import com.cloud.async.dao.SyncQueueDaoImpl;
import com.cloud.async.dao.SyncQueueItemDao;
import com.cloud.async.dao.SyncQueueItemDaoImpl;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ConfigurationDao;

@Configuration
public class SyncQueueTestConfiguration {

	@Bean
	public SyncQueueManager syncQueueManager() {
		return new SyncQueueManagerImpl();
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
	public AsyncJobJoinMapDao joinMapDao() {
		return new AsyncJobJoinMapDaoImpl();
	}
}

