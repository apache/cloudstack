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
package com.cloud.hypervisor;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
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
public class KVMGuruTest {

    @Mock
    HostDao hostDao;
    @Mock
    ServiceOfferingDetailsDao serviceOfferingDetailsDao;

    @Spy
    @InjectMocks
    private KVMGuru guru = new KVMGuru();

    @Mock
    VirtualMachineTO vmTO;
    @Mock
    VirtualMachineProfile vmProfile;
    @Mock
    VirtualMachine vm;
    @Mock
    HostVO host;
    @Mock
    ServiceOfferingDetailsVO dpdkVhostUserModeDetailVO;
    @Mock
    ServiceOfferingDetailsVO dpdkNumaDetailVO;
    @Mock
    ServiceOfferingDetailsVO dpdkHugePagesDetailVO;
    @Mock
    ServiceOffering serviceOffering;

    private static final long hostId = 1L;
    private static final Long offeringId = 1L;

    private String dpdkVhostMode = KVMGuru.DPDKvHostUserMode.SERVER.toString();

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

    @Before
    public void setup() throws UnsupportedEncodingException {
        dpdkHugePagesValue = URLEncoder.encode(dpdkHugePagesConf, "UTF-8");
        dpdkNumaValue = URLEncoder.encode(dpdkNumaConf, "UTF-8");

        Mockito.when(vmTO.getLimitCpuUse()).thenReturn(true);
        Mockito.when(vmProfile.getVirtualMachine()).thenReturn(vm);
        Mockito.when(vm.getHostId()).thenReturn(hostId);
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);
        Mockito.when(host.getCpus()).thenReturn(3);
        Mockito.when(host.getSpeed()).thenReturn(1995l);
        Mockito.when(vmTO.getMaxSpeed()).thenReturn(500);
        Mockito.when(serviceOffering.getId()).thenReturn(offeringId);
        Mockito.when(vmProfile.getServiceOffering()).thenReturn(serviceOffering);

        Mockito.when(dpdkVhostUserModeDetailVO.getName()).thenReturn(KVMGuru.DPDK_VHOST_USER_MODE);
        Mockito.when(dpdkVhostUserModeDetailVO.getValue()).thenReturn(dpdkVhostMode);
        Mockito.when(dpdkVhostUserModeDetailVO.getResourceId()).thenReturn(offeringId);
        Mockito.when(dpdkNumaDetailVO.getName()).thenReturn(KVMGuru.DPDK_NUMA);
        Mockito.when(dpdkNumaDetailVO.getResourceId()).thenReturn(offeringId);
        Mockito.when(dpdkNumaDetailVO.getValue()).thenReturn(dpdkNumaValue);
        Mockito.when(dpdkHugePagesDetailVO.getName()).thenReturn(KVMGuru.DPDK_HUGE_PAGES);
        Mockito.when(dpdkHugePagesDetailVO.getResourceId()).thenReturn(offeringId);
        Mockito.when(dpdkHugePagesDetailVO.getValue()).thenReturn(dpdkHugePagesValue);

        Mockito.when(serviceOfferingDetailsDao.listDetails(offeringId)).thenReturn(
                Arrays.asList(dpdkNumaDetailVO, dpdkHugePagesDetailVO, dpdkVhostUserModeDetailVO));
    }

    @Test
    public void testSetVmQuotaPercentage() {
        guru.setVmQuotaPercentage(vmTO, vmProfile);
        Mockito.verify(vmTO).setCpuQuotaPercentage(Mockito.anyDouble());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testSetVmQuotaPercentageNullHost() {
        Mockito.when(hostDao.findById(hostId)).thenReturn(null);
        guru.setVmQuotaPercentage(vmTO, vmProfile);
    }

    @Test
    public void testSetVmQuotaPercentageZeroDivision() {
        Mockito.when(host.getSpeed()).thenReturn(0l);
        guru.setVmQuotaPercentage(vmTO, vmProfile);
        Mockito.verify(vmTO, Mockito.never()).setCpuQuotaPercentage(Mockito.anyDouble());
    }

    @Test
    public void testSetVmQuotaPercentageNotCPULimit() {
        Mockito.when(vmTO.getLimitCpuUse()).thenReturn(false);
        guru.setVmQuotaPercentage(vmTO, vmProfile);
        Mockito.verify(vmProfile, Mockito.never()).getVirtualMachine();
        Mockito.verify(vmTO, Mockito.never()).setCpuQuotaPercentage(Mockito.anyDouble());
    }

    @Test
    public void testSetVmQuotaPercentageOverProvision() {
        Mockito.when(vmTO.getMaxSpeed()).thenReturn(3000);
        guru.setVmQuotaPercentage(vmTO, vmProfile);
        Mockito.verify(vmTO).setCpuQuotaPercentage(1d);
    }

    @Test
    public void testSetDpdkVhostUserModeValidDetail() {
        guru.setDpdkVhostUserMode(vmTO, serviceOffering, dpdkVhostUserModeDetailVO);
        Mockito.verify(vmTO).addExtraConfig(KVMGuru.DPDK_VHOST_USER_MODE, dpdkVhostMode);
    }

    @Test
    public void testSetDpdkVhostUserModeInvalidDetail() {
        Mockito.when(dpdkVhostUserModeDetailVO.getValue()).thenReturn("serverrrr");
        Mockito.verify(vmTO, Mockito.never()).addExtraConfig(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testAddServiceOfferingExtraConfigurationDpdkDetails() {
        guru.addServiceOfferingExtraConfiguration(vmTO, vmProfile);
        Mockito.verify(vmTO).addExtraConfig(KVMGuru.DPDK_NUMA, dpdkNumaValue);
        Mockito.verify(vmTO).addExtraConfig(KVMGuru.DPDK_HUGE_PAGES, dpdkHugePagesValue);
        Mockito.verify(vmTO).addExtraConfig(KVMGuru.DPDK_VHOST_USER_MODE, dpdkVhostMode);
    }

    @Test
    public void testAddServiceOfferingExtraConfigurationEmptyDetails() {
        Mockito.when(serviceOfferingDetailsDao.listDetails(offeringId)).thenReturn(null);
        guru.addServiceOfferingExtraConfiguration(vmTO, vmProfile);
        Mockito.verify(vmTO, Mockito.never()).addExtraConfig(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testDPDKvHostUserFromValueClient() {
        KVMGuru.DPDKvHostUserMode mode = KVMGuru.DPDKvHostUserMode.fromValue("client");
        Assert.assertEquals(KVMGuru.DPDKvHostUserMode.CLIENT, mode);
    }

    @Test
    public void testDPDKvHostUserFromValueServer() {
        KVMGuru.DPDKvHostUserMode mode = KVMGuru.DPDKvHostUserMode.fromValue("server");
        Assert.assertEquals(KVMGuru.DPDKvHostUserMode.SERVER, mode);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDPDKvHostUserFromValueServerInvalid() {
        KVMGuru.DPDKvHostUserMode.fromValue("serverrrr");
    }
}