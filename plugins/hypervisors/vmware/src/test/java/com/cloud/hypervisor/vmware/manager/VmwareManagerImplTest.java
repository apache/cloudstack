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

package com.cloud.hypervisor.vmware.manager;

import java.util.Collections;
import java.util.Map;

import org.apache.cloudstack.api.command.admin.zone.UpdateVmwareDcCmd;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.vmware.VmwareDatacenter;
import com.cloud.hypervisor.vmware.VmwareDatacenterVO;
import com.cloud.hypervisor.vmware.VmwareDatacenterZoneMapVO;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterDao;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterZoneMapDao;

@RunWith(MockitoJUnitRunner.class)
public class VmwareManagerImplTest {

    @Spy
    @InjectMocks
    private VmwareManagerImpl vmwareManager;

    @Mock
    private UpdateVmwareDcCmd updateVmwareDcCmd;
    @Mock
    private VmwareDatacenterDao vmwareDcDao;
    @Mock
    private VmwareDatacenterZoneMapDao vmwareDatacenterZoneMapDao;
    @Mock
    private ClusterDao clusterDao;
    @Mock
    private ClusterDetailsDao clusterDetailsDao;
    @Mock
    private HostDao hostDao;
    @Mock
    private HostDetailsDao hostDetailsDao;
    @Mock
    private Map<String, String> clusterDetails;
    @Mock
    private Map<String, String> hostDetails;

    @Before
    public void beforeTest() {
        VmwareDatacenterZoneMapVO vmwareDatacenterZoneMap = new VmwareDatacenterZoneMapVO();
        vmwareDatacenterZoneMap.setZoneId(1);
        vmwareDatacenterZoneMap.setVmwareDcId(1);
        VmwareDatacenterVO vmwareDatacenterVO = new VmwareDatacenterVO(1, "some-guid", "some-name", "10.1.1.1", "username", "password");

        Mockito.doReturn(vmwareDatacenterZoneMap).when(vmwareDatacenterZoneMapDao).findByZoneId(Mockito.anyLong());
        Mockito.doReturn(vmwareDatacenterVO).when(vmwareDcDao).findById(Mockito.anyLong());
        Mockito.doReturn(1L).when(updateVmwareDcCmd).getZoneId();
    }

    @Test
    public void updateVmwareDatacenterNoUpdate() {
        VmwareDatacenter vmwareDatacenter = vmwareManager.updateVmwareDatacenter(updateVmwareDcCmd);
        Assert.assertNull(vmwareDatacenter);
    }

    @Test
    public void updateVmwareDatacenterNormalUpdate() {
        Mockito.doReturn("some-new-username").when(updateVmwareDcCmd).getUsername();
        Mockito.doReturn("some-new-password").when(updateVmwareDcCmd).getPassword();
        Mockito.doReturn("some-new-vcenter-address").when(updateVmwareDcCmd).getVcenter();
        Mockito.doReturn(true).when(updateVmwareDcCmd).isRecursive();
        Mockito.doReturn(true).when(vmwareDcDao).update(Mockito.anyLong(), Mockito.any(VmwareDatacenterVO.class));
        Mockito.doReturn(Collections.singletonList(new ClusterVO(1, 1, "some-cluster"))).when(clusterDao).listByDcHyType(Mockito.anyLong(), Mockito.anyString());
        Mockito.doReturn(clusterDetails).when(clusterDetailsDao).findDetails(Mockito.anyLong());

        final HostVO host = new HostVO("someGuid");
        host.setDataCenterId(1);
        host.setHypervisorType(Hypervisor.HypervisorType.VMware);
        Mockito.doReturn(Collections.singletonList(host)).when(hostDao).listAllHostsByZoneAndHypervisorType(Mockito.anyLong(), Mockito.any());
        Mockito.lenient().doReturn(hostDetails).when(hostDetailsDao).findDetails(Mockito.anyLong());
        Mockito.doReturn("some-old-guid").when(hostDetails).get("guid");
        Mockito.doReturn(hostDetails).when(hostDetailsDao).findDetails(Mockito.anyLong());
        Mockito.doReturn(null).when(vmwareManager).importVsphereStoragePoliciesInternal(Mockito.anyLong(), Mockito.anyLong());

        final VmwareDatacenter vmwareDatacenter = vmwareManager.updateVmwareDatacenter(updateVmwareDcCmd);

        Assert.assertEquals(vmwareDatacenter.getUser(), updateVmwareDcCmd.getUsername());
        Assert.assertEquals(vmwareDatacenter.getPassword(), updateVmwareDcCmd.getPassword());
        Mockito.verify(clusterDetails, Mockito.times(2)).put(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(clusterDetailsDao, Mockito.times(1)).persist(Mockito.anyLong(), Mockito.anyMapOf(String.class, String.class));
        Mockito.verify(hostDetails, Mockito.times(3)).put(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(hostDetailsDao, Mockito.times(1)).persist(Mockito.anyLong(), Mockito.anyMapOf(String.class, String.class));
    }
}
