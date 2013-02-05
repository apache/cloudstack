// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.usage;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import com.cloud.cluster.agentlb.dao.HostTransferMapDao;
import com.cloud.cluster.agentlb.dao.HostTransferMapDaoImpl;
import com.cloud.dc.dao.*;
import com.cloud.service.dao.ServiceOfferingDaoImpl;
import com.cloud.vm.dao.*;
import com.cloud.network.dao.*;
import com.cloud.host.dao.*;

import com.cloud.utils.crypt.EncryptionSecretKeyChecker;  
import com.cloud.vm.dao.VMInstanceDaoImpl; 
import com.cloud.vm.dao.UserVmDaoImpl;
import com.cloud.event.dao.EventDaoImpl; 
import com.cloud.user.dao.UserStatisticsDaoImpl; 
import com.cloud.network.dao.IPAddressDaoImpl; 
import com.cloud.domain.dao.DomainDaoImpl; 
import com.cloud.user.dao.AccountDaoImpl; 
import com.cloud.user.dao.UserAccountDaoImpl; 
import com.cloud.configuration.dao.ConfigurationDaoImpl; 
import com.cloud.alert.dao.AlertDaoImpl;
import com.cloud.event.dao.UsageEventDaoImpl; 
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.event.dao.EventDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.dao.*;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.alert.dao.AlertDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.tags.dao.*;

@Configuration
public class UsageServerComponentConfig {

	@Bean
	public HostTransferMapDao HostTransferDao() {
		return new HostTransferMapDaoImpl();
	}

	@Bean
	public ClusterDao ClusterDao() {
		return new ClusterDaoImpl();
	}

	@Bean
	public HostPodDao HostPodDao() {
		return new HostPodDaoImpl();
	}
	
	@Bean
	public UserVmDetailsDao UserVmDetailsDao() {
		return new UserVmDetailsDaoImpl();
	}
	
	@Bean
	public VlanDaoImpl VlanDaoImpl() {
		return new VlanDaoImpl();
	}
	
	@Bean
	public PodVlanMapDao PodVlanMapDao() {
		return new PodVlanMapDaoImpl();
	}
	
	@Bean
	public AccountVlanMapDao AccountVlanMapDao() {
		return new AccountVlanMapDaoImpl();
	}

	@Bean
	public EncryptionSecretKeyChecker EncryptionSecretKeyChecker() {
		return new EncryptionSecretKeyChecker();
	}
	
	@Bean
	public VMInstanceDao VmInstanceDao() {
		return new VMInstanceDaoImpl();
	}

	@Bean
	public UserVmDao UserVmDao() {
		return new UserVmDaoImpl();
	}

	@Bean
	public ServiceOfferingDao ServiceOfferingDao() {
		return new ServiceOfferingDaoImpl();
	}
	
	@Bean
	public EventDao EventDao() {
		return new EventDaoImpl();
	}

	@Bean
	public UserStatisticsDao UserStatisticsDao() {
		return new UserStatisticsDaoImpl();
	}

	@Bean
	public IPAddressDao IPAddressDao() {
		return new IPAddressDaoImpl();
	}
	
	@Bean
	public DomainDao DomainDao() {
		return new DomainDaoImpl();
	}

	@Bean
	public AccountDao AccountDao() {
		return new AccountDaoImpl();
	}

	@Bean
	public UserAccountDao UserAccountDao() {
		return new UserAccountDaoImpl();
	}

	@Bean
	public ConfigurationDao ConfigurationDao() {
		return new ConfigurationDaoImpl();
	}

	@Bean
	public AlertDao AlertDao() {
		return new AlertDaoImpl();
	}

	@Bean
	public UsageEventDao UsageEventDao() {
		return new UsageEventDaoImpl();
	}

	@Bean
	public ResourceTagsDaoImpl ResourceTagsDaoImpl() {
		return new ResourceTagsDaoImpl();
	}

	@Bean
	public NicDao NicDao() {
		return new NicDaoImpl();
	}

	@Bean
	public HostDao HostDao() {
		return new HostDaoImpl();
	}

	@Bean
	public HostDetailsDao HostDetailsDao() {
		return new HostDetailsDaoImpl();
	}

	@Bean
	public HostTagsDao HostTagsDao() {
		return new HostTagsDaoImpl();
	}
}
