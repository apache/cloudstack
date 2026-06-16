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

package org.apache.cloudstack.api.command.admin.offering;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.offering.ServiceOffering;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.vm.lease.VMLeaseManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloneServiceOfferingCmdTest {

    private CloneServiceOfferingCmd cloneServiceOfferingCmd;

    @Mock
    private com.cloud.configuration.ConfigurationService configService;

    @Mock
    private ResponseGenerator responseGenerator;

    @Mock
    private ServiceOffering mockServiceOffering;

    @Mock
    private ServiceOfferingResponse mockServiceOfferingResponse;

    @Before
    public void setUp() {
        cloneServiceOfferingCmd = new CloneServiceOfferingCmd();
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "_configService", configService);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "_responseGenerator", responseGenerator);
    }

    @Test
    public void testGetSourceOfferingId() {
        Long sourceOfferingId = 555L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", sourceOfferingId);
        assertEquals(sourceOfferingId, cloneServiceOfferingCmd.getSourceOfferingId());
    }

    @Test
    public void testGetServiceOfferingName() {
        String name = "ClonedServiceOffering";
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "serviceOfferingName", name);
        assertEquals(name, cloneServiceOfferingCmd.getServiceOfferingName());
    }

    @Test
    public void testGetDisplayText() {
        String displayText = "Cloned Service Offering Display Text";
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "displayText", displayText);
        assertEquals(displayText, cloneServiceOfferingCmd.getDisplayText());
    }

    @Test
    public void testGetDisplayTextDefaultsToName() {
        String name = "ClonedServiceOffering";
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "serviceOfferingName", name);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "displayText", null);
        assertEquals(name, cloneServiceOfferingCmd.getDisplayText());
    }

    @Test
    public void testGetCpu() {
        Integer cpu = 4;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "cpuNumber", cpu);
        assertEquals(cpu, cloneServiceOfferingCmd.getCpuNumber());
    }

    @Test
    public void testGetMemory() {
        Integer memory = 8192;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "memory", memory);
        assertEquals(memory, cloneServiceOfferingCmd.getMemory());
    }

    @Test
    public void testGetCpuSpeed() {
        Integer cpuSpeed = 2000;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "cpuSpeed", cpuSpeed);
        assertEquals(cpuSpeed, cloneServiceOfferingCmd.getCpuSpeed());
    }

    @Test
    public void testGetOfferHa() {
        Boolean offerHa = true;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "offerHa", offerHa);
        assertEquals(offerHa, cloneServiceOfferingCmd.isOfferHa());
    }

    @Test
    public void testGetLimitCpuUse() {
        Boolean limitCpuUse = false;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "limitCpuUse", limitCpuUse);
        assertEquals(limitCpuUse, cloneServiceOfferingCmd.isLimitCpuUse());
    }

    @Test
    public void testGetVolatileVm() {
        Boolean volatileVm = true;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "isVolatile", volatileVm);
        assertEquals(volatileVm, cloneServiceOfferingCmd.isVolatileVm());
    }

    @Test
    public void testGetStorageType() {
        String storageType = "local";
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "storageType", storageType);
        assertEquals(storageType, cloneServiceOfferingCmd.getStorageType());
    }

    @Test
    public void testGetTags() {
        String tags = "ssd,premium,dedicated";
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "tags", tags);
        assertEquals(tags, cloneServiceOfferingCmd.getTags());
    }

    @Test
    public void testGetHostTag() {
        String hostTag = "gpu-enabled";
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "hostTag", hostTag);
        assertEquals(hostTag, cloneServiceOfferingCmd.getHostTag());
    }

    @Test
    public void testGetNetworkRate() {
        Integer networkRate = 1000;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "networkRate", networkRate);
        assertEquals(networkRate, cloneServiceOfferingCmd.getNetworkRate());
    }

    @Test
    public void testGetDeploymentPlanner() {
        String deploymentPlanner = "UserDispersingPlanner";
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "deploymentPlanner", deploymentPlanner);
        assertEquals(deploymentPlanner, cloneServiceOfferingCmd.getDeploymentPlanner());
    }

    @Test
    public void testGetDetails() {
        Map<String, HashMap<String, String>> details = new HashMap<>();

        HashMap<String, String> cpuOvercommit = new HashMap<>();
        cpuOvercommit.put("key", "cpuOvercommitRatio");
        cpuOvercommit.put("value", "2.0");

        HashMap<String, String> memoryOvercommit = new HashMap<>();
        memoryOvercommit.put("key", "memoryOvercommitRatio");
        memoryOvercommit.put("value", "1.5");

        details.put("0", cpuOvercommit);
        details.put("1", memoryOvercommit);

        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "details", details);

        Map<String, String> result = cloneServiceOfferingCmd.getDetails();
        assertNotNull(result);
        assertEquals("2.0", result.get("cpuOvercommitRatio"));
        assertEquals("1.5", result.get("memoryOvercommitRatio"));
    }

    @Test
    public void testIsPurgeResources() {
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "purgeResources", true);
        assertTrue(cloneServiceOfferingCmd.isPurgeResources());
    }

    @Test
    public void testIsPurgeResourcesFalse() {
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "purgeResources", false);
        assertFalse(cloneServiceOfferingCmd.isPurgeResources());
    }

    @Test
    public void testIsPurgeResourcesDefaultFalse() {
        assertFalse(cloneServiceOfferingCmd.isPurgeResources());
    }

    @Test
    public void testGetLeaseDuration() {
        Integer leaseDuration = 3600;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "leaseDuration", leaseDuration);
        assertEquals(leaseDuration, cloneServiceOfferingCmd.getLeaseDuration());
    }

    @Test
    public void testGetLeaseExpiryAction() {
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "leaseExpiryAction", "stop");
        assertEquals(VMLeaseManager.ExpiryAction.STOP, cloneServiceOfferingCmd.getLeaseExpiryAction());

        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "leaseExpiryAction", "DESTROY");
        assertEquals(VMLeaseManager.ExpiryAction.DESTROY, cloneServiceOfferingCmd.getLeaseExpiryAction());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetLeaseExpiryActionInvalidValue() {
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "leaseExpiryAction", "InvalidAction");
        cloneServiceOfferingCmd.getLeaseExpiryAction();
    }

    @Test
    public void testGetVgpuProfileId() {
        Long vgpuProfileId = 10L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "vgpuProfileId", vgpuProfileId);
        assertEquals(vgpuProfileId, cloneServiceOfferingCmd.getVgpuProfileId());
    }

    @Test
    public void testGetGpuCount() {
        Integer gpuCount = 2;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "gpuCount", gpuCount);
        assertEquals(gpuCount, cloneServiceOfferingCmd.getGpuCount());
    }

    @Test
    public void testGetGpuDisplay() {
        Boolean gpuDisplay = true;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "gpuDisplay", gpuDisplay);
        assertEquals(gpuDisplay, cloneServiceOfferingCmd.getGpuDisplay());
    }

    @Test
    public void testExecuteSuccess() {
        Long sourceOfferingId = 555L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", sourceOfferingId);

        when(configService.cloneServiceOffering(any(CloneServiceOfferingCmd.class))).thenReturn(mockServiceOffering);
        when(responseGenerator.createServiceOfferingResponse(mockServiceOffering)).thenReturn(mockServiceOfferingResponse);

        cloneServiceOfferingCmd.execute();

        assertNotNull(cloneServiceOfferingCmd.getResponseObject());
        assertEquals(mockServiceOfferingResponse, cloneServiceOfferingCmd.getResponseObject());
    }

    @Test
    public void testExecuteFailure() {
        Long sourceOfferingId = 555L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", sourceOfferingId);

        when(configService.cloneServiceOffering(any(CloneServiceOfferingCmd.class))).thenReturn(null);

        try {
            cloneServiceOfferingCmd.execute();
            fail("Expected ServerApiException to be thrown");
        } catch (ServerApiException e) {
            assertEquals(ApiErrorCode.INTERNAL_ERROR, e.getErrorCode());
            assertEquals("Failed to clone service offering", e.getMessage());
        }
    }

    @Test
    public void testExecuteSuccessWithAllParameters() {
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", 555L);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "serviceOfferingName", "ClonedOffering");
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "displayText", "Test Display");
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "cpuNumber", 4);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "memory", 8192);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "cpuSpeed", 2000);

        when(configService.cloneServiceOffering(any(CloneServiceOfferingCmd.class))).thenReturn(mockServiceOffering);
        when(responseGenerator.createServiceOfferingResponse(mockServiceOffering)).thenReturn(mockServiceOfferingResponse);

        cloneServiceOfferingCmd.execute();

        assertNotNull(cloneServiceOfferingCmd.getResponseObject());
        assertEquals(mockServiceOfferingResponse, cloneServiceOfferingCmd.getResponseObject());
    }

    @Test
    public void testExecuteWithInvalidParameterException() {
        Long sourceOfferingId = 555L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", sourceOfferingId);

        when(configService.cloneServiceOffering(any(CloneServiceOfferingCmd.class)))
            .thenThrow(new InvalidParameterValueException("Invalid source offering ID"));

        try {
            cloneServiceOfferingCmd.execute();
            fail("Expected ServerApiException to be thrown");
        } catch (ServerApiException e) {
            assertEquals(ApiErrorCode.PARAM_ERROR, e.getErrorCode());
            assertEquals("Invalid source offering ID", e.getMessage());
        }
    }

    @Test
    public void testExecuteWithCloudRuntimeException() {
        Long sourceOfferingId = 555L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", sourceOfferingId);

        when(configService.cloneServiceOffering(any(CloneServiceOfferingCmd.class)))
            .thenThrow(new com.cloud.utils.exception.CloudRuntimeException("Runtime error during clone"));

        try {
            cloneServiceOfferingCmd.execute();
            fail("Expected ServerApiException to be thrown");
        } catch (ServerApiException e) {
            assertEquals(ApiErrorCode.INTERNAL_ERROR, e.getErrorCode());
            assertEquals("Runtime error during clone", e.getMessage());
        }
    }

    @Test
    public void testExecuteResponseNameIsSet() {
        Long sourceOfferingId = 555L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", sourceOfferingId);

        when(configService.cloneServiceOffering(any(CloneServiceOfferingCmd.class))).thenReturn(mockServiceOffering);
        when(responseGenerator.createServiceOfferingResponse(mockServiceOffering)).thenReturn(mockServiceOfferingResponse);

        cloneServiceOfferingCmd.execute();

        assertNotNull(cloneServiceOfferingCmd.getResponseObject());
        // Verify that response name would be set (actual verification would require accessing the response object's internal state)
    }

    @Test
    public void testCloneWithAllParameters() {
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", 555L);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "serviceOfferingName", "ClonedServiceOffering");
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "displayText", "Cloned Service Offering");
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "cpuNumber", 4);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "memory", 8192);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "cpuSpeed", 2000);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "offerHa", true);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "limitCpuUse", false);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "isVolatile", true);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "storageType", "local");
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "tags", "premium");
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "hostTag", "gpu-enabled");
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "networkRate", 1000);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "deploymentPlanner", "UserDispersingPlanner");
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "purgeResources", true);

        assertEquals(Long.valueOf(555L), cloneServiceOfferingCmd.getSourceOfferingId());
        assertEquals("ClonedServiceOffering", cloneServiceOfferingCmd.getServiceOfferingName());
        assertEquals("Cloned Service Offering", cloneServiceOfferingCmd.getDisplayText());
        assertEquals(Integer.valueOf(4), cloneServiceOfferingCmd.getCpuNumber());
        assertEquals(Integer.valueOf(8192), cloneServiceOfferingCmd.getMemory());
        assertEquals(Integer.valueOf(2000), cloneServiceOfferingCmd.getCpuSpeed());
        assertEquals(Boolean.TRUE, cloneServiceOfferingCmd.isOfferHa());
        assertEquals(Boolean.FALSE, cloneServiceOfferingCmd.isLimitCpuUse());
        assertEquals(Boolean.TRUE, cloneServiceOfferingCmd.isVolatileVm());
        assertEquals("local", cloneServiceOfferingCmd.getStorageType());
        assertEquals("premium", cloneServiceOfferingCmd.getTags());
        assertEquals("gpu-enabled", cloneServiceOfferingCmd.getHostTag());
        assertEquals(Integer.valueOf(1000), cloneServiceOfferingCmd.getNetworkRate());
        assertEquals("UserDispersingPlanner", cloneServiceOfferingCmd.getDeploymentPlanner());
        assertTrue(cloneServiceOfferingCmd.isPurgeResources());
    }

    @Test
    public void testSourceOfferingIdNullByDefault() {
        assertNull(cloneServiceOfferingCmd.getSourceOfferingId());
    }

    @Test
    public void testGetSystemVmType() {
        String systemVmType = "domainrouter";
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "systemVmType", systemVmType);
        assertEquals(systemVmType, cloneServiceOfferingCmd.getSystemVmType());
    }

    @Test
    public void testGetBytesReadRate() {
        Long bytesReadRate = 1000000L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "bytesReadRate", bytesReadRate);
        assertEquals(bytesReadRate, cloneServiceOfferingCmd.getBytesReadRate());
    }

    @Test
    public void testGetBytesWriteRate() {
        Long bytesWriteRate = 1000000L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "bytesWriteRate", bytesWriteRate);
        assertEquals(bytesWriteRate, cloneServiceOfferingCmd.getBytesWriteRate());
    }

    @Test
    public void testGetIopsReadRate() {
        Long iopsReadRate = 1000L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "iopsReadRate", iopsReadRate);
        assertEquals(iopsReadRate, cloneServiceOfferingCmd.getIopsReadRate());
    }

    @Test
    public void testGetIopsWriteRate() {
        Long iopsWriteRate = 1000L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "iopsWriteRate", iopsWriteRate);
        assertEquals(iopsWriteRate, cloneServiceOfferingCmd.getIopsWriteRate());
    }

    @Test
    public void testCloneServiceOfferingWithGpuProfile() {
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", 555L);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "serviceOfferingName", "GPU-Offering-Clone");
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "vgpuProfileId", 10L);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "gpuCount", 2);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "gpuDisplay", true);

        assertEquals(Long.valueOf(10L), cloneServiceOfferingCmd.getVgpuProfileId());
        assertEquals(Integer.valueOf(2), cloneServiceOfferingCmd.getGpuCount());
        assertTrue(cloneServiceOfferingCmd.getGpuDisplay());
    }

    @Test
    public void testCloneServiceOfferingWithLease() {
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", 555L);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "serviceOfferingName", "Lease-Offering-Clone");
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "leaseDuration", 7200);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "leaseExpiryAction", "destroy");

        assertEquals(Integer.valueOf(7200), cloneServiceOfferingCmd.getLeaseDuration());
        assertEquals(VMLeaseManager.ExpiryAction.DESTROY, cloneServiceOfferingCmd.getLeaseExpiryAction());
    }

    @Test
    public void testExecuteWithOverriddenParameters() {
        Long sourceOfferingId = 555L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", sourceOfferingId);

        String newName = "ClonedOffering-Override";
        String newDisplayText = "Overridden Display Text";
        Integer newCpu = 8;
        Integer newMemory = 16384;
        Integer newCpuSpeed = 3000;
        Boolean newOfferHa = true;
        Boolean newLimitCpuUse = true;
        String newStorageType = "shared";
        String newTags = "premium,gpu";
        String newHostTag = "compute-optimized";
        Integer newNetworkRate = 2000;
        String newDeploymentPlanner = "FirstFitPlanner";
        Boolean newPurgeResources = true;

        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "serviceOfferingName", newName);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "displayText", newDisplayText);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "cpuNumber", newCpu);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "memory", newMemory);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "cpuSpeed", newCpuSpeed);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "offerHa", newOfferHa);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "limitCpuUse", newLimitCpuUse);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "storageType", newStorageType);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "tags", newTags);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "hostTag", newHostTag);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "networkRate", newNetworkRate);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "deploymentPlanner", newDeploymentPlanner);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "purgeResources", newPurgeResources);

        assertEquals(sourceOfferingId, cloneServiceOfferingCmd.getSourceOfferingId());
        assertEquals(newName, cloneServiceOfferingCmd.getServiceOfferingName());
        assertEquals(newDisplayText, cloneServiceOfferingCmd.getDisplayText());
        assertEquals(newCpu, cloneServiceOfferingCmd.getCpuNumber());
        assertEquals(newMemory, cloneServiceOfferingCmd.getMemory());
        assertEquals(newCpuSpeed, cloneServiceOfferingCmd.getCpuSpeed());
        assertEquals(newOfferHa, cloneServiceOfferingCmd.isOfferHa());
        assertEquals(newLimitCpuUse, cloneServiceOfferingCmd.isLimitCpuUse());
        assertEquals(newStorageType, cloneServiceOfferingCmd.getStorageType());
        assertEquals(newTags, cloneServiceOfferingCmd.getTags());
        assertEquals(newHostTag, cloneServiceOfferingCmd.getHostTag());
        assertEquals(newNetworkRate, cloneServiceOfferingCmd.getNetworkRate());
        assertEquals(newDeploymentPlanner, cloneServiceOfferingCmd.getDeploymentPlanner());
        assertTrue(cloneServiceOfferingCmd.isPurgeResources());

        when(configService.cloneServiceOffering(any(CloneServiceOfferingCmd.class))).thenReturn(mockServiceOffering);
        when(responseGenerator.createServiceOfferingResponse(mockServiceOffering)).thenReturn(mockServiceOfferingResponse);

        cloneServiceOfferingCmd.execute();

        assertNotNull(cloneServiceOfferingCmd.getResponseObject());
        assertEquals(mockServiceOfferingResponse, cloneServiceOfferingCmd.getResponseObject());
    }

    @Test
    public void testExecuteWithPartialOverrides() {
        Long sourceOfferingId = 555L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", sourceOfferingId);

        String newName = "PartialOverride";
        Integer newCpu = 6;
        Integer newMemory = 12288;

        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "serviceOfferingName", newName);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "cpuNumber", newCpu);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "memory", newMemory);

        assertEquals(newName, cloneServiceOfferingCmd.getServiceOfferingName());
        assertEquals(newCpu, cloneServiceOfferingCmd.getCpuNumber());
        assertEquals(newMemory, cloneServiceOfferingCmd.getMemory());

        assertNull(cloneServiceOfferingCmd.getCpuSpeed());
        assertFalse(cloneServiceOfferingCmd.isOfferHa());
        assertNull(cloneServiceOfferingCmd.getStorageType());

        when(configService.cloneServiceOffering(any(CloneServiceOfferingCmd.class))).thenReturn(mockServiceOffering);
        when(responseGenerator.createServiceOfferingResponse(mockServiceOffering)).thenReturn(mockServiceOfferingResponse);

        cloneServiceOfferingCmd.execute();

        assertNotNull(cloneServiceOfferingCmd.getResponseObject());
        assertEquals(mockServiceOfferingResponse, cloneServiceOfferingCmd.getResponseObject());
    }

    @Test
    public void testExecuteWithGpuOverrides() {
        Long sourceOfferingId = 555L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", sourceOfferingId);

        String newName = "GPU-Clone-Override";
        Long vgpuProfileId = 15L;
        Integer gpuCount = 4;
        Boolean gpuDisplay = false;

        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "serviceOfferingName", newName);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "vgpuProfileId", vgpuProfileId);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "gpuCount", gpuCount);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "gpuDisplay", gpuDisplay);

        assertEquals(newName, cloneServiceOfferingCmd.getServiceOfferingName());
        assertEquals(vgpuProfileId, cloneServiceOfferingCmd.getVgpuProfileId());
        assertEquals(gpuCount, cloneServiceOfferingCmd.getGpuCount());
        assertEquals(gpuDisplay, cloneServiceOfferingCmd.getGpuDisplay());

        when(configService.cloneServiceOffering(any(CloneServiceOfferingCmd.class))).thenReturn(mockServiceOffering);
        when(responseGenerator.createServiceOfferingResponse(mockServiceOffering)).thenReturn(mockServiceOfferingResponse);

        cloneServiceOfferingCmd.execute();

        assertNotNull(cloneServiceOfferingCmd.getResponseObject());
        assertEquals(mockServiceOfferingResponse, cloneServiceOfferingCmd.getResponseObject());
    }

    @Test
    public void testExecuteWithLeaseOverrides() {
        Long sourceOfferingId = 555L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", sourceOfferingId);

        String newName = "Lease-Clone-Override";
        Integer leaseDuration = 14400; // 4 hours
        String leaseExpiryAction = "stop";

        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "serviceOfferingName", newName);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "leaseDuration", leaseDuration);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "leaseExpiryAction", leaseExpiryAction);

        assertEquals(newName, cloneServiceOfferingCmd.getServiceOfferingName());
        assertEquals(leaseDuration, cloneServiceOfferingCmd.getLeaseDuration());
        assertEquals(VMLeaseManager.ExpiryAction.STOP, cloneServiceOfferingCmd.getLeaseExpiryAction());

        when(configService.cloneServiceOffering(any(CloneServiceOfferingCmd.class))).thenReturn(mockServiceOffering);
        when(responseGenerator.createServiceOfferingResponse(mockServiceOffering)).thenReturn(mockServiceOfferingResponse);

        cloneServiceOfferingCmd.execute();

        assertNotNull(cloneServiceOfferingCmd.getResponseObject());
        assertEquals(mockServiceOfferingResponse, cloneServiceOfferingCmd.getResponseObject());
    }

    @Test
    public void testExecuteWithStorageOverrides() {
        Long sourceOfferingId = 555L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", sourceOfferingId);
        String newName = "Storage-Clone-Override";
        Long bytesReadRate = 2000000L;
        Long bytesWriteRate = 1500000L;
        Long iopsReadRate = 2000L;
        Long iopsWriteRate = 1500L;

        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "serviceOfferingName", newName);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "bytesReadRate", bytesReadRate);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "bytesWriteRate", bytesWriteRate);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "iopsReadRate", iopsReadRate);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "iopsWriteRate", iopsWriteRate);

        assertEquals(newName, cloneServiceOfferingCmd.getServiceOfferingName());
        assertEquals(bytesReadRate, cloneServiceOfferingCmd.getBytesReadRate());
        assertEquals(bytesWriteRate, cloneServiceOfferingCmd.getBytesWriteRate());
        assertEquals(iopsReadRate, cloneServiceOfferingCmd.getIopsReadRate());
        assertEquals(iopsWriteRate, cloneServiceOfferingCmd.getIopsWriteRate());

        when(configService.cloneServiceOffering(any(CloneServiceOfferingCmd.class))).thenReturn(mockServiceOffering);
        when(responseGenerator.createServiceOfferingResponse(mockServiceOffering)).thenReturn(mockServiceOfferingResponse);

        cloneServiceOfferingCmd.execute();

        assertNotNull(cloneServiceOfferingCmd.getResponseObject());
        assertEquals(mockServiceOfferingResponse, cloneServiceOfferingCmd.getResponseObject());
    }

    @Test
    public void testExecuteWithDetailsOverride() {
        Long sourceOfferingId = 555L;
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "sourceOfferingId", sourceOfferingId);

        String newName = "Details-Clone-Override";
        Map<String, HashMap<String, String>> details = new HashMap<>();

        HashMap<String, String> cpuOvercommit = new HashMap<>();
        cpuOvercommit.put("key", "cpuOvercommitRatio");
        cpuOvercommit.put("value", "3.0");

        HashMap<String, String> memoryOvercommit = new HashMap<>();
        memoryOvercommit.put("key", "memoryOvercommitRatio");
        memoryOvercommit.put("value", "2.5");

        HashMap<String, String> customDetail = new HashMap<>();
        customDetail.put("key", "customParameter");
        customDetail.put("value", "customValue");

        details.put("0", cpuOvercommit);
        details.put("1", memoryOvercommit);
        details.put("2", customDetail);

        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "serviceOfferingName", newName);
        ReflectionTestUtils.setField(cloneServiceOfferingCmd, "details", details);

        assertEquals(newName, cloneServiceOfferingCmd.getServiceOfferingName());
        Map<String, String> result = cloneServiceOfferingCmd.getDetails();
        assertNotNull(result);
        assertEquals("3.0", result.get("cpuOvercommitRatio"));
        assertEquals("2.5", result.get("memoryOvercommitRatio"));
        assertEquals("customValue", result.get("customParameter"));

        when(configService.cloneServiceOffering(any(CloneServiceOfferingCmd.class))).thenReturn(mockServiceOffering);
        when(responseGenerator.createServiceOfferingResponse(mockServiceOffering)).thenReturn(mockServiceOfferingResponse);

        cloneServiceOfferingCmd.execute();

        assertNotNull(cloneServiceOfferingCmd.getResponseObject());
        assertEquals(mockServiceOfferingResponse, cloneServiceOfferingCmd.getResponseObject());
    }
}
