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
package org.apache.cloudstack.vm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ServerApiException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.service.ServiceOfferingVO;
import com.cloud.vm.VmDetailConstants;

public class VmwareCbtMigrationOfferingValidationTest {

    @Test
    public void testRejectsServiceOfferingWithTooFewCpus() {
        VmwareCbtPreflightInfo preflightInfo = createPreflightInfo(3, 0, 4096);
        ServiceOfferingVO serviceOffering = createServiceOffering(1, 1000, 4096, Collections.emptyMap());

        try {
            VmwareCbtMigrationManagerImpl.validateSelectedServiceOfferingResourcesForSourceVm(
                    preflightInfo, serviceOffering, Collections.emptyMap());
            Assert.fail("Expected service offering validation to fail");
        } catch (ServerApiException e) {
            Assert.assertTrue(e.getDescription().contains("The requested CPU number (1) is less than the source VM CPU number (3)"));
        }
    }

    @Test
    public void testRejectsServiceOfferingWithTooLittleMemory() {
        VmwareCbtPreflightInfo preflightInfo = createPreflightInfo(2, 0, 4096);
        ServiceOfferingVO serviceOffering = createServiceOffering(2, 1000, 1024, Collections.emptyMap());

        try {
            VmwareCbtMigrationManagerImpl.validateSelectedServiceOfferingResourcesForSourceVm(
                    preflightInfo, serviceOffering, Collections.emptyMap());
            Assert.fail("Expected service offering validation to fail");
        } catch (ServerApiException e) {
            Assert.assertTrue(e.getDescription().contains("The requested Memory (1024) is less than the source VM Memory (4096)"));
        }
    }

    @Test
    public void testAcceptsServiceOfferingWithSufficientCpuAndMemory() {
        VmwareCbtPreflightInfo preflightInfo = createPreflightInfo(2, 0, 4096);
        ServiceOfferingVO serviceOffering = createServiceOffering(4, 1000, 8192, Collections.emptyMap());

        VmwareCbtMigrationManagerImpl.validateSelectedServiceOfferingResourcesForSourceVm(
                preflightInfo, serviceOffering, Collections.emptyMap());
    }

    @Test
    public void testUsesCallerDetailsForCustomizedServiceOffering() {
        VmwareCbtPreflightInfo preflightInfo = createPreflightInfo(3, 0, 4096);
        Map<String, String> offeringDetails = new HashMap<>();
        offeringDetails.put(ApiConstants.MIN_CPU_NUMBER, "1");
        offeringDetails.put(ApiConstants.MIN_MEMORY, "1024");
        ServiceOfferingVO serviceOffering = createServiceOffering(null, 1000, null, offeringDetails);
        Map<String, String> callerDetails = new HashMap<>();
        callerDetails.put(VmDetailConstants.CPU_NUMBER, "4");
        callerDetails.put(VmDetailConstants.MEMORY, "8192");

        VmwareCbtMigrationManagerImpl.validateSelectedServiceOfferingResourcesForSourceVm(
                preflightInfo, serviceOffering, callerDetails);
    }

    @Test
    public void testFallsBackToOfferingMinimumsWhenCallerDetailsAreMissing() {
        Map<String, String> offeringDetails = new HashMap<>();
        offeringDetails.put(ApiConstants.MIN_CPU_NUMBER, "2");
        offeringDetails.put(ApiConstants.MIN_MEMORY, "2048");
        ServiceOfferingVO serviceOffering = createServiceOffering(null, 1000, null, offeringDetails);

        VmwareCbtMigrationManagerImpl.VmwareCbtOfferingResources resources =
                VmwareCbtMigrationManagerImpl.resolveRequestedOfferingResources(serviceOffering, Collections.emptyMap());

        Assert.assertEquals(Integer.valueOf(2), resources.getCpuNumber());
        Assert.assertEquals(Integer.valueOf(1000), resources.getCpuSpeed());
        Assert.assertEquals(Integer.valueOf(2048), resources.getMemoryMb());
    }

    @Test
    public void testDetectsWindowsSourceVmFromVmwareGuestOsName() {
        VmwareCbtPreflightInfo preflightInfo = createPreflightInfo("windows9Server64Guest", "Microsoft Windows Server 2016 (64-bit)");

        Assert.assertTrue(VmwareCbtMigrationManagerImpl.isWindowsSourceVm(preflightInfo));
    }

    @Test
    public void testDoesNotRequireWindowsConversionSupportForLinuxSourceVm() {
        VmwareCbtPreflightInfo preflightInfo = createPreflightInfo("ubuntu64Guest", "Ubuntu Linux (64-bit)");

        Assert.assertFalse(VmwareCbtMigrationManagerImpl.isWindowsSourceVm(preflightInfo));
    }

    private VmwareCbtPreflightInfo createPreflightInfo(Integer cpuCores, Integer cpuSpeed, Integer memoryMb) {
        return new VmwareCbtPreflightInfo("source-vm", "vm-1", true, true, false, 0,
                Collections.emptyList(), cpuCores, cpuSpeed, memoryMb);
    }

    private VmwareCbtPreflightInfo createPreflightInfo(String operatingSystemId, String operatingSystem) {
        return new VmwareCbtPreflightInfo("source-vm", "vm-1", true, true, false, 0,
                Collections.emptyList(), 2, 1000, 2048, operatingSystemId, operatingSystem);
    }

    private ServiceOfferingVO createServiceOffering(Integer cpu, Integer cpuSpeed, Integer memory,
                                                    Map<String, String> details) {
        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOffering.getCpu()).thenReturn(cpu);
        Mockito.when(serviceOffering.getSpeed()).thenReturn(cpuSpeed);
        Mockito.when(serviceOffering.getRamSize()).thenReturn(memory);
        Mockito.when(serviceOffering.getDetails()).thenReturn(details);
        Mockito.when(serviceOffering.getUuid()).thenReturn("service-offering-uuid");
        return serviceOffering;
    }
}
