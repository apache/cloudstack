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
package com.cloud.ha;

import java.lang.reflect.Field;
import java.util.Arrays;

import javax.inject.Inject;

import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.ha.dao.HighAvailabilityDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ManagementServer;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.user.AccountManager;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class HighAvailabilityManagerImplTest {
    @Mock
    HighAvailabilityDao _haDao;
    @Mock
    VMInstanceDao _instanceDao;
    @Mock
    HostDao _hostDao;
    @Mock
    DataCenterDao _dcDao;
    @Mock
    HostPodDao _podDao;
    @Mock
    ClusterDetailsDao _clusterDetailsDao;

    @Mock
    ServiceOfferingDao _serviceOfferingDao;

    @Mock
    ManagedContext _managedContext;

    @Mock
    AgentManager _agentMgr;
    @Mock
    AlertManager _alertMgr;
    @Mock
    StorageManager _storageMgr;
    @Mock
    GuestOSDao _guestOSDao;
    @Mock
    GuestOSCategoryDao _guestOSCategoryDao;
    @Mock
    VirtualMachineManager _itMgr;
    @Mock
    AccountManager _accountMgr;
    @Mock
    ResourceManager _resourceMgr;
    @Mock
    ManagementServer _msServer;
    @Mock
    ConfigurationDao _configDao;
    @Mock
    VolumeOrchestrationService volumeMgr;

    @Mock
    HostVO hostVO;

    HighAvailabilityManagerImpl highAvailabilityManager;

    @Before
    public void setup() throws IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        highAvailabilityManager = new HighAvailabilityManagerImpl();
        for (Field injectField : HighAvailabilityManagerImpl.class
                .getDeclaredFields()) {
            if (injectField.isAnnotationPresent(Inject.class)) {
                injectField.setAccessible(true);
                injectField.set(highAvailabilityManager, this.getClass()
                        .getDeclaredField(injectField.getName()).get(this));
            }
        }
    }

    @Test
    public void scheduleRestartForVmsOnHost() {
        Mockito.when(hostVO.getType()).thenReturn(Host.Type.Routing);
        Mockito.when(hostVO.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(_instanceDao.listByHostId(42l)).thenReturn(
                Arrays.asList(Mockito.mock(VMInstanceVO.class)));
        Mockito.when(_podDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(HostPodVO.class));
        Mockito.when(_dcDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(DataCenterVO.class));
        highAvailabilityManager.scheduleRestartForVmsOnHost(hostVO, true);
    }
}
