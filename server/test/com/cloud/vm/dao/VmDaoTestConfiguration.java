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
package com.cloud.vm.dao;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.apache.cloudstack.vm.jobs.VmWorkJobDao;
import org.apache.cloudstack.vm.jobs.VmWorkJobDaoImpl;

import com.cloud.cluster.agentlb.dao.HostTransferMapDao;
import com.cloud.cluster.agentlb.dao.HostTransferMapDaoImpl;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.ClusterDaoImpl;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.host.dao.HostTagsDao;
import com.cloud.tags.dao.ResourceTagDao;

@Configuration
public class VmDaoTestConfiguration {
	
	@Bean
	public VMInstanceDao instanceDao() {
		return new VMInstanceDaoImpl();
	}
	
	@Bean
	public HostDao hostDao() {
		return new HostDaoImpl();
	}
	
	@Bean
	public HostDetailsDao hostDetailsDao() {
		return Mockito.mock(HostDetailsDao.class);
	}

	@Bean
	public HostTagsDao hostTagsDao() {
		return Mockito.mock(HostTagsDao.class);
	}
	
	@Bean
	public HostTransferMapDao hostTransferMapDao() {
		return new HostTransferMapDaoImpl();
	}

	@Bean
	public ClusterDao clusterDao() {
		return new ClusterDaoImpl();
	}
	
	@Bean
	public HostPodDao hostPodDao() {
		return Mockito.mock(HostPodDao.class);
	}
	
	@Bean
	public ResourceTagDao resourceTagDao() {
		return Mockito.mock(ResourceTagDao.class);
	}
	
	@Bean
	public NicDao nicDao() {
		return new NicDaoImpl();
	}
	
	@Bean 
	public UserVmDao userVmDao() {
		return new UserVmDaoImpl();
	}
	
	@Bean
	public UserVmDetailsDao userVmDetailsDao() {
		return Mockito.mock(UserVmDetailsDao.class);
	}
	
	@Bean
	public VmWorkJobDao vmWorkJobDao() {
		return new VmWorkJobDaoImpl();
	}
}
