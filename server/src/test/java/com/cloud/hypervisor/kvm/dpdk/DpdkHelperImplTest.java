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
package com.cloud.hypervisor.kvm.dpdk;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class DpdkHelperImplTest {

    @Mock
    ServiceOfferingDetailsDao serviceOfferingDetailsDao;
    @Mock
    HostDao hostDao;
    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    UserVmDetailsDao userVmDetailsDao;

    @Spy
    @InjectMocks
    private DpdkHelper dpdkHelper = new DpdkHelperImpl();

    @Mock
    VirtualMachineTO vmTO;
    @Mock
    VirtualMachineProfile vmProfile;
    @Mock
    ServiceOfferingDetailsVO dpdkVhostUserModeDetailVO;
    @Mock
    ServiceOfferingDetailsVO dpdkNumaDetailVO;
    @Mock
    ServiceOfferingDetailsVO dpdkHugePagesDetailVO;
    @Mock
    ServiceOffering serviceOffering;
    @Mock
    UserVmDetailVO dpdkNumaVmDetail;
    @Mock
    UserVmDetailVO dpdkHugePagesVmDetail;
    @Mock
    HostVO hostVO;
    @Mock
    VMInstanceVO vmInstanceVO;

    private String dpdkVhostMode = DpdkHelper.VHostUserMode.SERVER.toString();

    private static final String dpdkNumaConf =
            "<cpu mode=\"host-passthrough\">\n" +
                    "  <numa>\n" +
                    "    <cell id=\"0\" cpus=\"0\" memory=\"9437184\" unit=\"KiB\" memAccess=\"shared\"/>\n" +
                    "  </numa>\n" +
                    "</cpu>";
    private static final String dpdkHugePagesConf =
            "<memoryBacking>\n" +
                    "  <hugePages/>\n" +
                    "</memoryBacking>";
    private static String dpdkNumaValue;
    private static String dpdkHugePagesValue;
    private static final Long offeringId = 1L;
    private static final Long hostId = 1L;
    private static final String hostCapabilities = "hvm,snapshot";
    private static final Long vmId = 1L;

    @Before
    public void setup() throws UnsupportedEncodingException {
        dpdkHugePagesValue = URLEncoder.encode(dpdkHugePagesConf, "UTF-8");
        dpdkNumaValue = URLEncoder.encode(dpdkNumaConf, "UTF-8");

        Mockito.when(dpdkVhostUserModeDetailVO.getName()).thenReturn(DpdkHelper.DPDK_VHOST_USER_MODE);
        Mockito.when(dpdkVhostUserModeDetailVO.getValue()).thenReturn(dpdkVhostMode);
        Mockito.lenient().when(dpdkVhostUserModeDetailVO.getResourceId()).thenReturn(offeringId);
        Mockito.when(dpdkNumaDetailVO.getName()).thenReturn(DpdkHelper.DPDK_NUMA);
        Mockito.lenient().when(dpdkNumaDetailVO.getResourceId()).thenReturn(offeringId);
        Mockito.lenient().when(dpdkNumaDetailVO.getValue()).thenReturn(dpdkNumaValue);
        Mockito.when(dpdkHugePagesDetailVO.getName()).thenReturn(DpdkHelper.DPDK_HUGE_PAGES);
        Mockito.lenient().when(dpdkHugePagesDetailVO.getResourceId()).thenReturn(offeringId);
        Mockito.lenient().when(dpdkHugePagesDetailVO.getValue()).thenReturn(dpdkHugePagesValue);

        Mockito.when(serviceOfferingDetailsDao.listDetails(offeringId)).thenReturn(
                Arrays.asList(dpdkNumaDetailVO, dpdkHugePagesDetailVO, dpdkVhostUserModeDetailVO));
        Mockito.when(vmProfile.getServiceOffering()).thenReturn(serviceOffering);
        Mockito.when(serviceOffering.getId()).thenReturn(offeringId);
        Mockito.when(vmProfile.getServiceOffering()).thenReturn(serviceOffering);
        Mockito.when(serviceOffering.getId()).thenReturn(offeringId);

        Mockito.when(hostDao.findById(hostId)).thenReturn(hostVO);
        Mockito.when(hostVO.getCapabilities()).thenReturn(hostCapabilities);

        Mockito.when(vmInstanceDao.findById(vmId)).thenReturn(vmInstanceVO);
        Mockito.when(vmInstanceVO.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vmInstanceVO.getHostId()).thenReturn(hostId);
        Mockito.when(vmInstanceVO.getServiceOfferingId()).thenReturn(offeringId);
        Mockito.when(vmInstanceVO.getId()).thenReturn(vmId);

        Mockito.when(dpdkNumaVmDetail.getName()).thenReturn(DpdkHelper.DPDK_NUMA);
        Mockito.lenient().when(dpdkNumaVmDetail.getValue()).thenReturn(dpdkNumaConf);
        Mockito.when(dpdkHugePagesVmDetail.getName()).thenReturn(DpdkHelper.DPDK_HUGE_PAGES);
        Mockito.lenient().when(dpdkHugePagesVmDetail.getValue()).thenReturn(dpdkHugePagesConf);
        Mockito.when(userVmDetailsDao.listDetails(vmId)).thenReturn(Arrays.asList(dpdkNumaVmDetail, dpdkHugePagesVmDetail));
    }

    @Test
    public void testSetDpdkVhostUserModeValidDetail() {
        Mockito.when(serviceOfferingDetailsDao.findDetail(offeringId, DpdkHelper.DPDK_VHOST_USER_MODE)).
                thenReturn(dpdkVhostUserModeDetailVO);
        dpdkHelper.setDpdkVhostUserMode(vmTO, vmProfile);
        Mockito.verify(vmTO).addExtraConfig(DpdkHelper.DPDK_VHOST_USER_MODE, dpdkVhostMode);
    }

    @Test
    public void testSetDpdkVhostUserModeInvalidDetail() {
        Mockito.lenient().when(dpdkVhostUserModeDetailVO.getValue()).thenReturn("serverrrr");
        Mockito.verify(vmTO, Mockito.never()).addExtraConfig(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testSetDpdkVhostUserModeNotExistingDetail() {
        Mockito.lenient().when(serviceOfferingDetailsDao.listDetails(offeringId)).thenReturn(
                Arrays.asList(dpdkNumaDetailVO, dpdkHugePagesDetailVO));
        Mockito.verify(vmTO, Mockito.never()).addExtraConfig(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testDpdkvHostUserFromValueClient() {
        DpdkHelper.VHostUserMode mode = DpdkHelper.VHostUserMode.fromValue("client");
        Assert.assertEquals(DpdkHelper.VHostUserMode.CLIENT, mode);
    }

    @Test
    public void testDpdkvHostUserFromValueServer() {
        DpdkHelper.VHostUserMode mode = DpdkHelper.VHostUserMode.fromValue("server");
        Assert.assertEquals(DpdkHelper.VHostUserMode.SERVER, mode);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDpdkvHostUserFromValueServerInvalid() {
        DpdkHelper.VHostUserMode.fromValue("serverrrr");
    }

    @Test
    public void testIsHostDpdkEnabledNonDPDKHost() {
        Assert.assertFalse(dpdkHelper.isHostDpdkEnabled(hostId));
    }

    @Test
    public void testIsHostDpdkEnabledDpdkHost() {
        Mockito.when(hostVO.getCapabilities()).thenReturn(hostCapabilities + ",dpdk");
        Assert.assertTrue(dpdkHelper.isHostDpdkEnabled(hostId));
    }

    @Test
    public void testIsVMDPDKEnabledDPDKEnabledVM() {
        Mockito.when(hostVO.getCapabilities()).thenReturn(hostCapabilities + ",dpdk");
        Assert.assertTrue(dpdkHelper.isVMDpdkEnabled(vmId));
    }

    @Test
    public void testIsVMDpdkEnabledGuestType() {
        Mockito.when(vmInstanceVO.getType()).thenReturn(VirtualMachine.Type.SecondaryStorageVm);
        Assert.assertFalse(dpdkHelper.isVMDpdkEnabled(vmId));
        Mockito.verify(dpdkHelper, Mockito.never()).isHostDpdkEnabled(hostId);
    }

    @Test
    public void testIsVMDpdkEnabledGuestTypeMissingConfigurationOnDetails() {
        Mockito.when(userVmDetailsDao.listDetails(vmId)).thenReturn(Arrays.asList(dpdkNumaVmDetail));
        Mockito.when(serviceOfferingDetailsDao.listDetails(offeringId)).thenReturn(new ArrayList<>());
        Assert.assertFalse(dpdkHelper.isVMDpdkEnabled(vmId));
        Mockito.verify(dpdkHelper, Mockito.never()).isHostDpdkEnabled(hostId);
    }

    @Test
    public void testIsVMDpdkEnabledGuestTypeEmptyDetails() {
        Mockito.when(userVmDetailsDao.listDetails(vmId)).thenReturn(new ArrayList<>());
        Mockito.when(serviceOfferingDetailsDao.listDetails(offeringId)).thenReturn(new ArrayList<>());
        Assert.assertFalse(dpdkHelper.isVMDpdkEnabled(vmId));
        Mockito.verify(dpdkHelper, Mockito.never()).isHostDpdkEnabled(hostId);
    }

    @Test
    public void testIsVMDpdkEnabledNonDPDKCapabilityOnHost() {
        Assert.assertFalse(dpdkHelper.isVMDpdkEnabled(vmId));
    }

    @Test
    public void testIsVMDpdkEnabledGuestTypeMissingConfigurationOnVmDetails() {
        Mockito.when(userVmDetailsDao.listDetails(vmId)).thenReturn(Collections.singletonList(dpdkNumaVmDetail));
        Mockito.when(hostVO.getCapabilities()).thenReturn(hostCapabilities + ",dpdk");
        Assert.assertTrue(dpdkHelper.isVMDpdkEnabled(vmId));
    }

    @Test
    public void testIsVMDpdkEnabledGuestTypeEmptyVmDetails() {
        Mockito.when(userVmDetailsDao.listDetails(vmId)).thenReturn(new ArrayList<>());
        Mockito.when(hostVO.getCapabilities()).thenReturn(hostCapabilities + ",dpdk");
        Assert.assertTrue(dpdkHelper.isVMDpdkEnabled(vmId));
    }

    @Test
    public void testIsVMDpdkEnabledGuestTypeMixedConfigurationOnDetails() {
        Mockito.when(userVmDetailsDao.listDetails(vmId)).thenReturn(Collections.singletonList(dpdkNumaVmDetail));
        Mockito.when(serviceOfferingDetailsDao.listDetails(offeringId)).thenReturn(Collections.singletonList(dpdkHugePagesDetailVO));
        Mockito.when(hostVO.getCapabilities()).thenReturn(hostCapabilities + ",dpdk");
        Assert.assertTrue(dpdkHelper.isVMDpdkEnabled(vmId));
    }
}
