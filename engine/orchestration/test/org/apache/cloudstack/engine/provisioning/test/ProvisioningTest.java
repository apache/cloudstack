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
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.ClusterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.DataCenterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.HostDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.HostPodDao;
import org.apache.cloudstack.engine.service.api.ProvisioningService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.apache.cloudstack.engine.datacenter.entity.api.db.ClusterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.DataCenterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.HostPodVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.HostVO;

import com.cloud.dc.DataCenter.NetworkType;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/resource/provisioningContext.xml")
public class ProvisioningTest extends TestCase {
	
	@Inject
	ProvisioningService service;
	
	@Inject
	DataCenterDao dcDao;
	
	@Inject
	HostPodDao _podDao;

	@Inject
	ClusterDao _clusterDao;

	@Inject
	HostDao _hostDao;
	
    @Before
	public void setUp() {
    	DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null,  "10.0.0.1/24", 
				null, null, NetworkType.Basic, null, null, true,  true);
		Mockito.when(dcDao.findByUUID(Mockito.anyString())).thenReturn(dc);
		Mockito.when(dcDao.persist((DataCenterVO) Mockito.anyObject())).thenReturn(dc);
		
		HostPodVO pod = new HostPodVO("lab", 123, "10.0.0.1", "10.0.0.1", 24, "test");
		Mockito.when(_podDao.findByUUID(Mockito.anyString())).thenReturn(pod);
		Mockito.when(_podDao.persist((HostPodVO) Mockito.anyObject())).thenReturn(pod);    	    	
		
    	ClusterVO cluster = new ClusterVO();
		Mockito.when(_clusterDao.findByUUID(Mockito.anyString())).thenReturn(cluster);
		Mockito.when(_clusterDao.persist((ClusterVO) Mockito.anyObject())).thenReturn(cluster);
		
		HostVO host = new HostVO("68765876598");
		Mockito.when(_hostDao.findByUUID(Mockito.anyString())).thenReturn(host);
		Mockito.when(_hostDao.persist((HostVO) Mockito.anyObject())).thenReturn(host);    	    	
		
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
		//registerAndEnableZone();
		//registerAndEnablePod();
		//registerAndEnableCluster();
		registerAndEnableHost();
	}


}
