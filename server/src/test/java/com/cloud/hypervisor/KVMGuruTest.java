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
import org.apache.cloudstack.api.ApiConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.UnsupportedEncodingException;
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
    ServiceOffering serviceOffering;
    @Mock
    ServiceOfferingDetailsVO detail1;
    @Mock
    ServiceOfferingDetailsVO detail2;

    private static final long hostId = 1L;
    private static final Long offeringId = 1L;

    private static final String detail1Key = ApiConstants.EXTRA_CONFIG + "-config-1";
    private static final String detail1Value = "value1";
    private static final String detail2Key = "detail2";
    private static final String detail2Value = "value2";

    @Before
    public void setup() throws UnsupportedEncodingException {
        Mockito.when(vmTO.getLimitCpuUse()).thenReturn(true);
        Mockito.when(vmProfile.getVirtualMachine()).thenReturn(vm);
        Mockito.when(vm.getHostId()).thenReturn(hostId);
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);
        Mockito.when(host.getCpus()).thenReturn(3);
        Mockito.when(host.getSpeed()).thenReturn(1995L);
        Mockito.when(vmTO.getMaxSpeed()).thenReturn(500);
        Mockito.when(serviceOffering.getId()).thenReturn(offeringId);
        Mockito.when(vmProfile.getServiceOffering()).thenReturn(serviceOffering);

        Mockito.when(detail1.getName()).thenReturn(detail1Key);
        Mockito.when(detail1.getValue()).thenReturn(detail1Value);
        Mockito.when(detail1.getResourceId()).thenReturn(offeringId);
        Mockito.when(detail2.getName()).thenReturn(detail2Key);
        Mockito.when(detail2.getResourceId()).thenReturn(offeringId);
        Mockito.when(detail2.getValue()).thenReturn(detail2Value);

        Mockito.when(serviceOfferingDetailsDao.listDetails(offeringId)).thenReturn(
                Arrays.asList(detail1, detail2));
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
}