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
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.vm.VirtualMachineProfile;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class DPDKHelperImplTest {

    @Mock
    ServiceOfferingDetailsDao serviceOfferingDetailsDao;

    @Spy
    @InjectMocks
    private DPDKHelper dpdkHelper = new DPDKHelperImpl();

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

    private String dpdkVhostMode = DPDKHelper.VHostUserMode.SERVER.toString();

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

    @Before
    public void setup() throws UnsupportedEncodingException {
        dpdkHugePagesValue = URLEncoder.encode(dpdkHugePagesConf, "UTF-8");
        dpdkNumaValue = URLEncoder.encode(dpdkNumaConf, "UTF-8");

        Mockito.when(dpdkVhostUserModeDetailVO.getName()).thenReturn(DPDKHelper.DPDK_VHOST_USER_MODE);
        Mockito.when(dpdkVhostUserModeDetailVO.getValue()).thenReturn(dpdkVhostMode);
        Mockito.when(dpdkVhostUserModeDetailVO.getResourceId()).thenReturn(offeringId);
        Mockito.when(dpdkNumaDetailVO.getName()).thenReturn(DPDKHelper.DPDK_NUMA);
        Mockito.when(dpdkNumaDetailVO.getResourceId()).thenReturn(offeringId);
        Mockito.when(dpdkNumaDetailVO.getValue()).thenReturn(dpdkNumaValue);
        Mockito.when(dpdkHugePagesDetailVO.getName()).thenReturn(DPDKHelper.DPDK_HUGE_PAGES);
        Mockito.when(dpdkHugePagesDetailVO.getResourceId()).thenReturn(offeringId);
        Mockito.when(dpdkHugePagesDetailVO.getValue()).thenReturn(dpdkHugePagesValue);

        Mockito.when(serviceOfferingDetailsDao.listDetails(offeringId)).thenReturn(
                Arrays.asList(dpdkNumaDetailVO, dpdkHugePagesDetailVO, dpdkVhostUserModeDetailVO));
        Mockito.when(vmProfile.getServiceOffering()).thenReturn(serviceOffering);
        Mockito.when(serviceOffering.getId()).thenReturn(offeringId);
    }

    @Test
    public void testSetDpdkVhostUserModeValidDetail() {
        Mockito.when(serviceOfferingDetailsDao.findDetail(offeringId, DPDKHelper.DPDK_VHOST_USER_MODE)).
                thenReturn(dpdkVhostUserModeDetailVO);
        dpdkHelper.setDpdkVhostUserMode(vmTO, vmProfile);
        Mockito.verify(vmTO).addExtraConfig(DPDKHelper.DPDK_VHOST_USER_MODE, dpdkVhostMode);
    }

    @Test
    public void testSetDpdkVhostUserModeInvalidDetail() {
        Mockito.when(dpdkVhostUserModeDetailVO.getValue()).thenReturn("serverrrr");
        Mockito.verify(vmTO, Mockito.never()).addExtraConfig(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testSetDpdkVhostUserModeNotExistingDetail() {
        Mockito.when(serviceOfferingDetailsDao.listDetails(offeringId)).thenReturn(
                Arrays.asList(dpdkNumaDetailVO, dpdkHugePagesDetailVO));
        Mockito.verify(vmTO, Mockito.never()).addExtraConfig(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testDPDKvHostUserFromValueClient() {
        DPDKHelper.VHostUserMode mode = DPDKHelper.VHostUserMode.fromValue("client");
        Assert.assertEquals(DPDKHelper.VHostUserMode.CLIENT, mode);
    }

    @Test
    public void testDPDKvHostUserFromValueServer() {
        DPDKHelper.VHostUserMode mode = DPDKHelper.VHostUserMode.fromValue("server");
        Assert.assertEquals(DPDKHelper.VHostUserMode.SERVER, mode);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDPDKvHostUserFromValueServerInvalid() {
        DPDKHelper.VHostUserMode.fromValue("serverrrr");
    }
}
