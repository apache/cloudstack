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
package org.apache.cloudstack.engine.provisioning.test;


import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.ClusterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.DataCenterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.HostDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.HostPodDao;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;




public class ChildTestConfiguration {
	
	@Bean
	public DataCenterDao dataCenterDao() {
		return Mockito.mock(DataCenterDao.class);
	}
	
	@Bean
	public HostPodDao hostPodDao() {
		return Mockito.mock(HostPodDao.class);
	}
	
	@Bean
	public ClusterDao clusterDao() {
		return Mockito.mock(ClusterDao.class);
	}

	@Bean
	public HostDao hostDao() {
		return Mockito.mock(HostDao.class);
	}
}
