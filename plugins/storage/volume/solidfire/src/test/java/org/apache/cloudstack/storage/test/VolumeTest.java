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
package org.apache.cloudstack.storage.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;

import com.cloud.agent.AgentManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Cluster.ClusterType;
import com.cloud.org.Managed.ManagedState;
import com.cloud.resource.ResourceState;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/resource/storageContext.xml")
public class VolumeTest {
    @Inject
    HostDao hostDao;
    @Inject
    HostPodDao podDao;
    @Inject
    ClusterDao clusterDao;
    @Inject
    DataCenterDao dcDao;
    @Inject
    PrimaryDataStoreDao primaryStoreDao;
    // @Inject
    // PrimaryDataStoreProviderManager primaryDataStoreProviderMgr;
    @Inject
    AgentManager agentMgr;
    Long dcId;
    Long clusterId;

    @Before
    public void setUp() {
        // create data center
        DataCenterVO dc =
            new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null, "10.0.0.1/24", null, null, NetworkType.Basic, null, null, true,
                true, null, null);
        dc = dcDao.persist(dc);
        dcId = dc.getId();
        // create pod

        HostPodVO pod = new HostPodVO(UUID.randomUUID().toString(), dc.getId(), "192.168.56.1", "192.168.56.0/24", 8, "test");
        pod = podDao.persist(pod);
        // create xen cluster
        ClusterVO cluster = new ClusterVO(dc.getId(), pod.getId(), "devcloud cluster");
        cluster.setHypervisorType(HypervisorType.XenServer.toString());
        cluster.setClusterType(ClusterType.CloudManaged);
        cluster.setManagedState(ManagedState.Managed);
        cluster = clusterDao.persist(cluster);
        clusterId = cluster.getId();
        // create xen host

        HostVO host = new HostVO(UUID.randomUUID().toString());
        host.setName("devcloud xen host");
        host.setType(Host.Type.Routing);
        host.setPrivateIpAddress("192.168.56.2");
        host.setDataCenterId(dc.getId());
        host.setVersion("6.0.1");
        host.setAvailable(true);
        host.setSetup(true);
        host.setLastPinged(0);
        host.setResourceState(ResourceState.Enabled);
        host.setClusterId(cluster.getId());

        host = hostDao.persist(host);
        List<HostVO> results = new ArrayList<HostVO>();
        results.add(host);
        Mockito.when(hostDao.listAll()).thenReturn(results);
        Mockito.when(hostDao.findHypervisorHostInCluster(Matchers.anyLong())).thenReturn(results);
        // CreateObjectAnswer createVolumeFromImageAnswer = new
        // CreateObjectAnswer(null,UUID.randomUUID().toString(), null);

        // Mockito.when(primaryStoreDao.findById(Mockito.anyLong())).thenReturn(primaryStore);
    }

    private PrimaryDataStoreInfo createPrimaryDataStore() {
        try {
            // primaryDataStoreProviderMgr.configure("primary data store mgr",
            // new HashMap<String, Object>());
            // PrimaryDataStoreProvider provider =
            // primaryDataStoreProviderMgr.getDataStoreProvider("Solidfre Primary Data Store Provider");
            Map<String, String> params = new HashMap<String, String>();
            params.put("url", "nfs://test/test");
            params.put("dcId", dcId.toString());
            params.put("clusterId", clusterId.toString());
            params.put("name", "my primary data store");
            // PrimaryDataStoreInfo primaryDataStoreInfo =
            // provider.registerDataStore(params);
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Test
    public void createPrimaryDataStoreTest() {
        createPrimaryDataStore();
    }
}
