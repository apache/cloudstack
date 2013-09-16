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
/**
 *
 */
package org.apache.cloudstack.engine.provisioning.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.engine.datacenter.entity.api.ClusterEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State;
import org.apache.cloudstack.engine.datacenter.entity.api.HostEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.PodEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.ZoneEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.EngineClusterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.EngineDataCenterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.EngineHostDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.EngineHostPodDao;
import org.apache.cloudstack.engine.service.api.ProvisioningService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineClusterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineDataCenterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineHostPodVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineHostVO;

import com.cloud.dc.DataCenter.NetworkType;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/resource/provisioningContext.xml")
public class ProvisioningTest extends TestCase {

	@Inject
	ProvisioningService service;

	@Inject
	EngineDataCenterDao dcDao;

	@Inject
	EngineHostPodDao _podDao;

	@Inject
	EngineClusterDao _clusterDao;

	@Inject
	EngineHostDao _hostDao;

    @Before
	public void setUp() {
	EngineDataCenterVO dc = new EngineDataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null,  "10.0.0.1/24",
				null, null, NetworkType.Basic, null, null, true,  true, null, null);
		Mockito.when(dcDao.findByUuid(Mockito.anyString())).thenReturn(dc);
		Mockito.when(dcDao.persist((EngineDataCenterVO) Mockito.anyObject())).thenReturn(dc);

		EngineHostPodVO pod = new EngineHostPodVO("lab", 123, "10.0.0.1", "10.0.0.1", 24, "test");
		Mockito.when(_podDao.findByUuid(Mockito.anyString())).thenReturn(pod);
		Mockito.when(_podDao.persist((EngineHostPodVO) Mockito.anyObject())).thenReturn(pod);

	EngineClusterVO cluster = new EngineClusterVO();
		Mockito.when(_clusterDao.findByUuid(Mockito.anyString())).thenReturn(cluster);
		Mockito.when(_clusterDao.persist((EngineClusterVO) Mockito.anyObject())).thenReturn(cluster);

		EngineHostVO host = new EngineHostVO("68765876598");
		Mockito.when(_hostDao.findByUuid(Mockito.anyString())).thenReturn(host);
		Mockito.when(_hostDao.persist((EngineHostVO) Mockito.anyObject())).thenReturn(host);

    }

	private void registerAndEnableZone() {
		ZoneEntity zone = service.registerZone("47547648", "lab","owner", null, new HashMap<String, String>());
		State state = zone.getState();
		System.out.println("state:"+state);
		boolean result = zone.enable();
		System.out.println("result:"+result);

	}

	private void registerAndEnablePod() {
		PodEntity pod = service.registerPod("47547648", "lab","owner", "8709874074", null, new HashMap<String, String>());
		State state = pod.getState();
		System.out.println("state:"+state);
		boolean result = pod.enable();
		System.out.println("result:"+result);
	}

	private void registerAndEnableCluster() {
		ClusterEntity cluster = service.registerCluster("1265476542", "lab","owner", null, new HashMap<String, String>());
		State state = cluster.getState();
		System.out.println("state:"+state);
		boolean result = cluster.enable();
		System.out.println("result:"+result);
	}

	private void registerAndEnableHost() {
		HostEntity host = service.registerHost("1265476542", "lab","owner", null, new HashMap<String, String>());
		State state = host.getState();
		System.out.println("state:"+state);
		boolean result = host.enable();
		System.out.println("result:"+result);
	}

	@Test
	public void testProvisioning() {
		registerAndEnableZone();
		registerAndEnablePod();
		registerAndEnableCluster();
		registerAndEnableHost();
	}


}
