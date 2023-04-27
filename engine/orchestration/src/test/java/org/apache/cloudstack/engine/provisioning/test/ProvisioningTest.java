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
import java.util.UUID;

import junit.framework.TestCase;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceManager;
import org.apache.cloudstack.engine.service.api.ProvisioningServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import org.apache.cloudstack.engine.datacenter.entity.api.ClusterEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State;
import org.apache.cloudstack.engine.datacenter.entity.api.HostEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.PodEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.ZoneEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineClusterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineDataCenterVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineHostPodVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineHostVO;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.EngineClusterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.EngineDataCenterDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.EngineHostDao;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.EngineHostPodDao;
import org.apache.cloudstack.engine.service.api.ProvisioningService;

import com.cloud.dc.DataCenter.NetworkType;

import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class ProvisioningTest extends TestCase {

    @Spy
    @InjectMocks
    ProvisioningService service = new ProvisioningServiceImpl();

    @Mock
    DataCenterResourceManager dataCenterResourceManager;

    @Mock
    EngineDataCenterDao dcDao;

    @Mock
    EngineHostPodDao _podDao;

    @Mock
    EngineClusterDao _clusterDao;

    @Mock
    EngineHostDao _hostDao;

    @Mock
    EngineDataCenterVO dataCenterVO;

    @Mock
    EngineHostPodVO podVO;

    @Mock
    EngineClusterVO clusterVO;

    @Mock
    EngineHostVO hostVO;

    @Override
    @Before
    public void setUp() {
        Mockito.when(dataCenterResourceManager.loadDataCenter(any())).thenReturn(dataCenterVO);
        Mockito.when(dataCenterResourceManager.loadPod(any())).thenReturn(podVO);
        Mockito.when(dataCenterResourceManager.loadCluster(any())).thenReturn(clusterVO);
        Mockito.when(dataCenterResourceManager.loadHost(any())).thenReturn(hostVO);
    }

    private void registerAndEnableZone() {
        ZoneEntity zone = service.registerZone("47547648", "lab", "owner", null, new HashMap<String, String>());
        State state = zone.getState();
        System.out.println("state:" + state);
        boolean result = zone.enable();
        System.out.println("result:" + result);

    }

    private void registerAndEnablePod() {
        PodEntity pod = service.registerPod("47547648", "lab", "owner", "8709874074", null, new HashMap<String, String>());
        State state = pod.getState();
        System.out.println("state:" + state);
        boolean result = pod.enable();
        System.out.println("result:" + result);
    }

    private void registerAndEnableCluster() {
        ClusterEntity cluster = service.registerCluster("1265476542", "lab", "owner", null, new HashMap<String, String>());
        State state = cluster.getState();
        System.out.println("state:" + state);
        boolean result = cluster.enable();
        System.out.println("result:" + result);
    }

    private void registerAndEnableHost() {
        HostEntity host = service.registerHost("1265476542", "lab", "owner", null, new HashMap<String, String>());
        State state = host.getState();
        System.out.println("state:" + state);
        boolean result = host.enable();
        System.out.println("result:" + result);
    }

    @Test
    public void testProvisioning() {
        registerAndEnableZone();
        registerAndEnablePod();
        registerAndEnableCluster();
        registerAndEnableHost();
    }

}
