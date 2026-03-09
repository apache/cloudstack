/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.service;

import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.exception.CloudRuntimeException;
import feign.FeignException;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.storage.feign.client.SANFeignClient;
import org.apache.cloudstack.storage.feign.model.Igroup;
import org.apache.cloudstack.storage.feign.model.Initiator;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.feign.model.LunMap;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.cloudstack.storage.utils.Utility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedSANStrategyTest {

    @Mock
    private SANFeignClient sanFeignClient;

    @Mock
    private OntapStorage ontapStorage;

    @Mock
    private PrimaryDataStoreInfo primaryDataStoreInfo;

    @Mock
    private Scope scope;

    private UnifiedSANStrategy unifiedSANStrategy;
    private String authHeader;

    @BeforeEach
    void setUp() {
        lenient().when(ontapStorage.getStorageIP()).thenReturn("192.168.1.100");
        lenient().when(ontapStorage.getUsername()).thenReturn("admin");
        lenient().when(ontapStorage.getPassword()).thenReturn("password");
        lenient().when(ontapStorage.getSvmName()).thenReturn("svm1");

        unifiedSANStrategy = new UnifiedSANStrategy(ontapStorage);

        // Use reflection to inject the mock SANFeignClient (field is in parent StorageStrategy class)
        try {
            java.lang.reflect.Field sanFeignClientField = StorageStrategy.class.getDeclaredField("sanFeignClient");
            sanFeignClientField.setAccessible(true);
            sanFeignClientField.set(unifiedSANStrategy, sanFeignClient);

            // Also inject the storage field from parent class to ensure proper mocking
            java.lang.reflect.Field storageField = StorageStrategy.class.getDeclaredField("storage");
            storageField.setAccessible(true);
            storageField.set(unifiedSANStrategy, ontapStorage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        authHeader = "Basic YWRtaW46cGFzc3dvcmQ="; // Base64 encoded admin:password
    }

    @Test
    void testCreateCloudStackVolume_Success() {
        // Setup
        Lun lun = new Lun();
        lun.setName("/vol/vol1/lun1");

        CloudStackVolume request = new CloudStackVolume();
        request.setLun(lun);

        Lun createdLun = new Lun();
        createdLun.setName("/vol/vol1/lun1");
        createdLun.setUuid("lun-uuid-123");

        OntapResponse<Lun> response = new OntapResponse<>();
        response.setRecords(List.of(createdLun));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.createLun(eq(authHeader), eq(true), any(Lun.class)))
                    .thenReturn(response);

            // Execute
            CloudStackVolume result = unifiedSANStrategy.createCloudStackVolume(request);

            // Verify
            assertNotNull(result);
            assertNotNull(result.getLun());
            assertEquals("lun-uuid-123", result.getLun().getUuid());
            assertEquals("/vol/vol1/lun1", result.getLun().getName());

            verify(sanFeignClient).createLun(eq(authHeader), eq(true), any(Lun.class));
        }
    }

    @Test
    void testCreateCloudStackVolume_NullRequest_ThrowsException() {
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.createCloudStackVolume(null));
    }

    @Test
    void testCreateCloudStackVolume_FeignException_ThrowsCloudRuntimeException() {
        // Setup
        Lun lun = new Lun();
        lun.setName("/vol/vol1/lun1");
        CloudStackVolume request = new CloudStackVolume();
        request.setLun(lun);

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);
        when(feignException.getMessage()).thenReturn("Internal server error");

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.createLun(eq(authHeader), eq(true), any(Lun.class)))
                    .thenThrow(feignException);

            // Execute & Verify
            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.createCloudStackVolume(request));
        }
    }

    @Test
    void testDeleteCloudStackVolume_Success() {
        // Setup
        Lun lun = new Lun();
        lun.setName("/vol/vol1/lun1");
        lun.setUuid("lun-uuid-123");
        CloudStackVolume request = new CloudStackVolume();
        request.setLun(lun);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            doNothing().when(sanFeignClient).deleteLun(eq(authHeader), eq("lun-uuid-123"), anyMap());

            // Execute
            unifiedSANStrategy.deleteCloudStackVolume(request);

            // Verify
            verify(sanFeignClient).deleteLun(eq(authHeader), eq("lun-uuid-123"), anyMap());
        }
    }

    @Test
    void testDeleteCloudStackVolume_NotFound_SkipsDeletion() {
        // Setup
        Lun lun = new Lun();
        lun.setName("/vol/vol1/lun1");
        lun.setUuid("lun-uuid-123");
        CloudStackVolume request = new CloudStackVolume();
        request.setLun(lun);

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(404);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            doThrow(feignException).when(sanFeignClient).deleteLun(eq(authHeader), eq("lun-uuid-123"), anyMap());

            // Execute - should not throw exception
            assertDoesNotThrow(() -> unifiedSANStrategy.deleteCloudStackVolume(request));
        }
    }

    @Test
    void testGetCloudStackVolume_Success() {
        // Setup
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.NAME, "/vol/vol1/lun1");

        Lun lun = new Lun();
        lun.setName("/vol/vol1/lun1");
        lun.setUuid("lun-uuid-123");

        OntapResponse<Lun> response = new OntapResponse<>();
        response.setRecords(List.of(lun));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getLunResponse(eq(authHeader), anyMap())).thenReturn(response);

            // Execute
            CloudStackVolume result = unifiedSANStrategy.getCloudStackVolume(values);

            // Verify
            assertNotNull(result);
            assertNotNull(result.getLun());
            assertEquals("lun-uuid-123", result.getLun().getUuid());
            assertEquals("/vol/vol1/lun1", result.getLun().getName());
        }
    }

    @Test
    void testGetCloudStackVolume_NotFound_ReturnsNull() {
        // Setup
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.NAME, "/vol/vol1/lun1");

        OntapResponse<Lun> response = new OntapResponse<>();
        response.setRecords(new ArrayList<>());

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getLunResponse(eq(authHeader), anyMap())).thenReturn(response);

            // Execute
            CloudStackVolume result = unifiedSANStrategy.getCloudStackVolume(values);

            // Verify
            assertNull(result);
        }
    }

    @Test
    void testCreateAccessGroup_Success() {
        // Setup
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        accessGroup.setScope(scope);

        Map<String, String> details = new HashMap<>();
        details.put(Constants.SVM_NAME, "svm1");
        details.put(Constants.PROTOCOL, ProtocolType.ISCSI.name());

        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");
        lenient().when(primaryDataStoreInfo.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);

        List<HostVO> hosts = new ArrayList<>();
        HostVO host1 = mock(HostVO.class);
        when(host1.getStorageUrl()).thenReturn("iqn.1993-08.org.debian:01:host1");
        hosts.add(host1);
        accessGroup.setHostsToConnect(hosts);

        Igroup createdIgroup = new Igroup();
        createdIgroup.setName("igroup1");

        OntapResponse<Igroup> response = new OntapResponse<>();
        response.setRecords(List.of(createdIgroup));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            when(sanFeignClient.createIgroup(eq(authHeader), eq(true), any(Igroup.class)))
                    .thenReturn(response);

            // Execute
            AccessGroup result = unifiedSANStrategy.createAccessGroup(accessGroup);

            // Verify
            assertNotNull(result);
            assertNotNull(result.getIgroup());
            assertEquals("igroup1", result.getIgroup().getName());

            ArgumentCaptor<Igroup> igroupCaptor = ArgumentCaptor.forClass(Igroup.class);
            verify(sanFeignClient).createIgroup(eq(authHeader), eq(true), igroupCaptor.capture());

            Igroup capturedIgroup = igroupCaptor.getValue();
            assertEquals("igroup1", capturedIgroup.getName());
            assertNotNull(capturedIgroup.getInitiators());
            assertEquals(1, capturedIgroup.getInitiators().size());
        }
    }

    @Test
    void testCreateAccessGroup_AlreadyExists_ReturnsSuccessfully() {
        // Setup
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        accessGroup.setScope(scope);

        Map<String, String> details = new HashMap<>();
        details.put(Constants.SVM_NAME, "svm1");
        details.put(Constants.PROTOCOL, ProtocolType.ISCSI.name());

        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");
        lenient().when(primaryDataStoreInfo.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);

        List<HostVO> hosts = new ArrayList<>();
        HostVO host1 = mock(HostVO.class);
        when(host1.getStorageUrl()).thenReturn("iqn.1993-08.org.debian:01:host1");
        hosts.add(host1);
        accessGroup.setHostsToConnect(hosts);

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(409);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            when(sanFeignClient.createIgroup(eq(authHeader), eq(true), any(Igroup.class)))
                    .thenThrow(feignException);

            // Execute
            AccessGroup result = unifiedSANStrategy.createAccessGroup(accessGroup);

            // Verify - should not throw exception
            assertNotNull(result);
        }
    }

    @Test
    void testDeleteAccessGroup_Success() {
        // Setup
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);

        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");
        when(primaryDataStoreInfo.getClusterId()).thenReturn(10L);

        Igroup igroup = new Igroup();
        igroup.setName("igroup1");
        // Use reflection to set UUID since there's no setter
        try {
            java.lang.reflect.Field uuidField = Igroup.class.getDeclaredField("uuid");
            uuidField.setAccessible(true);
            uuidField.set(igroup, "igroup-uuid-123");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OntapResponse<Igroup> response = new OntapResponse<>();
        response.setRecords(List.of(igroup));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenReturn(response);
            doNothing().when(sanFeignClient).deleteIgroup(eq(authHeader), eq("igroup-uuid-123"));

            // Execute
            unifiedSANStrategy.deleteAccessGroup(accessGroup);

            // Verify
            verify(sanFeignClient).deleteIgroup(eq(authHeader), eq("igroup-uuid-123"));
        }
    }

    @Test
    void testDeleteAccessGroup_NotFound_SkipsDeletion() {
        // Setup
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);

        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(404);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenThrow(feignException);

            // Execute - should not throw exception
            assertDoesNotThrow(() -> unifiedSANStrategy.deleteAccessGroup(accessGroup));
        }
    }

    @Test
    void testGetAccessGroup_Success() {
        // Setup
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.NAME, "igroup1");

        Igroup igroup = new Igroup();
        igroup.setName("igroup1");

        OntapResponse<Igroup> response = new OntapResponse<>();
        response.setRecords(List.of(igroup));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenReturn(response);

            // Execute
            AccessGroup result = unifiedSANStrategy.getAccessGroup(values);

            // Verify
            assertNotNull(result);
            assertNotNull(result.getIgroup());
            assertEquals("igroup1", result.getIgroup().getName());
        }
    }

    @Test
    void testEnableLogicalAccess_Success() {
        // Setup
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.LUN_DOT_NAME, "/vol/vol1/lun1");
        values.put(Constants.IGROUP_DOT_NAME, "igroup1");

        LunMap lunMap = new LunMap();
        lunMap.setLogicalUnitNumber(0);

        OntapResponse<LunMap> response = new OntapResponse<>();
        response.setRecords(List.of(lunMap));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.createLunMap(eq(authHeader), eq(true), any(LunMap.class)))
                    .thenReturn(new OntapResponse<>());
            when(sanFeignClient.getLunMapResponse(eq(authHeader), anyMap())).thenReturn(response);

            // Execute
            Map<String, String> result = unifiedSANStrategy.enableLogicalAccess(values);

            // Verify
            assertNotNull(result);
            assertTrue(result.containsKey(Constants.LOGICAL_UNIT_NUMBER));
            assertEquals("0", result.get(Constants.LOGICAL_UNIT_NUMBER));

            verify(sanFeignClient).createLunMap(eq(authHeader), eq(true), any(LunMap.class));
        }
    }

    @Test
    void testEnableLogicalAccess_AlreadyMapped_ReturnsLunNumber() {
        // Setup
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.LUN_DOT_NAME, "/vol/vol1/lun1");
        values.put(Constants.IGROUP_DOT_NAME, "igroup1");

        LunMap lunMap = new LunMap();
        lunMap.setLogicalUnitNumber(5);

        OntapResponse<LunMap> response = new OntapResponse<>();
        response.setRecords(List.of(lunMap));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            Exception exception = new RuntimeException("LUN already mapped to this group");
            doThrow(exception).when(sanFeignClient).createLunMap(eq(authHeader), eq(true), any(LunMap.class));
            when(sanFeignClient.getLunMapResponse(eq(authHeader), anyMap())).thenReturn(response);

            // Execute
            Map<String, String> result = unifiedSANStrategy.enableLogicalAccess(values);

            // Verify
            assertNotNull(result);
            assertEquals("5", result.get(Constants.LOGICAL_UNIT_NUMBER));
        }
    }

    @Test
    void testDisableLogicalAccess_Success() {
        // Setup
        Map<String, String> values = new HashMap<>();
        values.put(Constants.LUN_DOT_UUID, "lun-uuid-123");
        values.put(Constants.IGROUP_DOT_UUID, "igroup-uuid-123");

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            doNothing().when(sanFeignClient).deleteLunMap(eq(authHeader), eq("lun-uuid-123"), eq("igroup-uuid-123"));

            // Execute
            unifiedSANStrategy.disableLogicalAccess(values);

            // Verify
            verify(sanFeignClient).deleteLunMap(eq(authHeader), eq("lun-uuid-123"), eq("igroup-uuid-123"));
        }
    }

    @Test
    void testDisableLogicalAccess_NotFound_SkipsDeletion() {
        // Setup
        Map<String, String> values = new HashMap<>();
        values.put(Constants.LUN_DOT_UUID, "lun-uuid-123");
        values.put(Constants.IGROUP_DOT_UUID, "igroup-uuid-123");

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(404);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            doThrow(feignException).when(sanFeignClient).deleteLunMap(eq(authHeader), eq("lun-uuid-123"), eq("igroup-uuid-123"));

            // Execute - should not throw exception
            assertDoesNotThrow(() -> unifiedSANStrategy.disableLogicalAccess(values));
        }
    }

    @Test
    void testGetLogicalAccess_Success() {
        // Setup
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.LUN_DOT_NAME, "/vol/vol1/lun1");
        values.put(Constants.IGROUP_DOT_NAME, "igroup1");

        LunMap lunMap = new LunMap();
        lunMap.setLogicalUnitNumber(3);

        OntapResponse<LunMap> response = new OntapResponse<>();
        response.setRecords(List.of(lunMap));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getLunMapResponse(eq(authHeader), anyMap())).thenReturn(response);

            // Execute
            Map<String, String> result = unifiedSANStrategy.getLogicalAccess(values);

            // Verify
            assertNotNull(result);
            assertEquals("3", result.get(Constants.LOGICAL_UNIT_NUMBER));
        }
    }

    @Test
    void testGetLogicalAccess_NotFound_ReturnsNull() {
        // Setup
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.LUN_DOT_NAME, "/vol/vol1/lun1");
        values.put(Constants.IGROUP_DOT_NAME, "igroup1");

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getLunMapResponse(eq(authHeader), anyMap()))
                    .thenThrow(new RuntimeException("Not found"));

            // Execute
            Map<String, String> result = unifiedSANStrategy.getLogicalAccess(values);

            // Verify
            assertNull(result);
        }
    }

    @Test
    void testEnsureLunMapped_ExistingMapping_ReturnsLunNumber() {
        // Setup
        String svmName = "svm1";
        String lunName = "/vol/vol1/lun1";
        String accessGroupName = "igroup1";

        LunMap lunMap = new LunMap();
        lunMap.setLogicalUnitNumber(2);

        OntapResponse<LunMap> response = new OntapResponse<>();
        response.setRecords(List.of(lunMap));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getLunMapResponse(eq(authHeader), anyMap())).thenReturn(response);

            // Execute
            String result = unifiedSANStrategy.ensureLunMapped(svmName, lunName, accessGroupName);

            // Verify
            assertEquals("2", result);
            verify(sanFeignClient, never()).createLunMap(any(), anyBoolean(), any());
        }
    }

    @Test
    void testEnsureLunMapped_CreatesNewMapping_ReturnsLunNumber() {
        // Setup
        String svmName = "svm1";
        String lunName = "/vol/vol1/lun1";
        String accessGroupName = "igroup1";

        LunMap lunMap = new LunMap();
        lunMap.setLogicalUnitNumber(4);

        OntapResponse<LunMap> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        OntapResponse<LunMap> response = new OntapResponse<>();
        response.setRecords(List.of(lunMap));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            // First call returns empty (no existing mapping), second call returns the new mapping
            when(sanFeignClient.getLunMapResponse(eq(authHeader), anyMap()))
                    .thenReturn(emptyResponse)  // First call - no records
                    .thenReturn(response);      // Second call after creation

            when(sanFeignClient.createLunMap(eq(authHeader), eq(true), any(LunMap.class)))
                    .thenReturn(new OntapResponse<>());

            // Execute
            String result = unifiedSANStrategy.ensureLunMapped(svmName, lunName, accessGroupName);

            // Verify
            assertEquals("4", result);
            verify(sanFeignClient).createLunMap(eq(authHeader), eq(true), any(LunMap.class));
        }
    }

    @Test
    void testValidateInitiatorInAccessGroup_InitiatorFound_ReturnsTrue() {
        // Setup
        String hostInitiator = "iqn.1993-08.org.debian:01:host1";
        String svmName = "svm1";
        String accessGroupName = "igroup1";

        Initiator initiator = new Initiator();
        initiator.setName(hostInitiator);

        Igroup igroup = new Igroup();
        igroup.setName(accessGroupName);
        igroup.setInitiators(List.of(initiator));

        OntapResponse<Igroup> response = new OntapResponse<>();
        response.setRecords(List.of(igroup));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenReturn(response);

            // Execute
            boolean result = unifiedSANStrategy.validateInitiatorInAccessGroup(hostInitiator, svmName, accessGroupName);

            // Verify
            assertTrue(result);
        }
    }

    @Test
    void testValidateInitiatorInAccessGroup_InitiatorNotFound_ReturnsFalse() {
        // Setup
        String hostInitiator = "iqn.1993-08.org.debian:01:host1";
        String svmName = "svm1";
        String accessGroupName = "igroup1";

        Initiator differentInitiator = new Initiator();
        differentInitiator.setName("iqn.1993-08.org.debian:01:host2");

        Igroup igroup = new Igroup();
        igroup.setName(accessGroupName);
        igroup.setInitiators(List.of(differentInitiator));

        OntapResponse<Igroup> response = new OntapResponse<>();
        response.setRecords(List.of(igroup));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenReturn(response);

            // Execute
            boolean result = unifiedSANStrategy.validateInitiatorInAccessGroup(hostInitiator, svmName, accessGroupName);

            // Verify
            assertFalse(result);
        }
    }

    @Test
    void testValidateInitiatorInAccessGroup_EmptyInitiator_ReturnsFalse() {
        boolean result = unifiedSANStrategy.validateInitiatorInAccessGroup("", "svm1", "igroup1");
        assertFalse(result);

        result = unifiedSANStrategy.validateInitiatorInAccessGroup(null, "svm1", "igroup1");
        assertFalse(result);
    }

    @Test
    void testValidateInitiatorInAccessGroup_IgroupNotFound_ReturnsFalse() {
        // Setup
        String hostInitiator = "iqn.1993-08.org.debian:01:host1";

        OntapResponse<Igroup> response = new OntapResponse<>();
        response.setRecords(new ArrayList<>());

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenReturn(response);

            // Execute
            boolean result = unifiedSANStrategy.validateInitiatorInAccessGroup(hostInitiator, "svm1", "igroup1");

            // Verify
            assertFalse(result);
        }
    }

    @Test
    void testCopyCloudStackVolume_NullRequest_DoesNotThrow() {
        // copyCloudStackVolume is not yet implemented (no-op), so it should not throw
        assertDoesNotThrow(() -> unifiedSANStrategy.copyCloudStackVolume(null));
    }

    @Test
    void testCopyCloudStackVolume_NullLun_DoesNotThrow() {
        // copyCloudStackVolume is not yet implemented (no-op), so it should not throw
        CloudStackVolume request = new CloudStackVolume();
        request.setLun(null);

        assertDoesNotThrow(() -> unifiedSANStrategy.copyCloudStackVolume(request));
    }

    @Test
    void testCopyCloudStackVolume_ValidRequest_DoesNotThrow() {
        // copyCloudStackVolume is not yet implemented (no-op), so it should not throw
        Lun lun = new Lun();
        lun.setName("/vol/vol1/lun1");
        CloudStackVolume request = new CloudStackVolume();
        request.setLun(lun);

        assertDoesNotThrow(() -> unifiedSANStrategy.copyCloudStackVolume(request));
    }

    @Test
    void testSetOntapStorage() {
        OntapStorage newStorage = mock(OntapStorage.class);
        assertDoesNotThrow(() -> unifiedSANStrategy.setOntapStorage(newStorage));
    }

    @Test
    void testUpdateCloudStackVolume_ReturnsNull() {
        CloudStackVolume request = new CloudStackVolume();
        CloudStackVolume result = unifiedSANStrategy.updateCloudStackVolume(request);
        assertNull(result);
    }

    @Test
    void testUpdateAccessGroup_ReturnsNull() {
        AccessGroup accessGroup = new AccessGroup();
        AccessGroup result = unifiedSANStrategy.updateAccessGroup(accessGroup);
        assertNull(result);
    }

    @Test
    void testCreateAccessGroup_NullPrimaryDataStoreInfo_ThrowsException() {
        AccessGroup accessGroup = new AccessGroup();
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.createAccessGroup(accessGroup));
    }

    @Test
    void testCreateAccessGroup_NullDetails_ThrowsException() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        when(primaryDataStoreInfo.getDetails()).thenReturn(null);

        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.createAccessGroup(accessGroup));
    }

    @Test
    void testCreateAccessGroup_EmptyDetails_ThrowsException() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        when(primaryDataStoreInfo.getDetails()).thenReturn(new HashMap<>());

        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.createAccessGroup(accessGroup));
    }

    @Test
    void testCreateAccessGroup_NullHostsToConnect_ThrowsException() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        accessGroup.setScope(scope);

//        when(scope.getScopeType()).thenReturn(com.cloud.storage.ScopeType.CLUSTER);

        Map<String, String> details = new HashMap<>();
        details.put(Constants.SVM_NAME, "svm1");
        details.put(Constants.PROTOCOL, ProtocolType.ISCSI.name());

        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");
        lenient().when(primaryDataStoreInfo.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);

        accessGroup.setHostsToConnect(null);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.createAccessGroup(accessGroup));
        }
    }

    @Test
    void testCreateAccessGroup_EmptyHostsToConnect_ThrowsException() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        accessGroup.setScope(scope);

        Map<String, String> details = new HashMap<>();
        details.put(Constants.SVM_NAME, "svm1");
        details.put(Constants.PROTOCOL, ProtocolType.ISCSI.name());

        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");
        lenient().when(primaryDataStoreInfo.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);

        accessGroup.setHostsToConnect(new ArrayList<>());

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.createAccessGroup(accessGroup));
        }
    }

    @Test
    void testCreateAccessGroup_HostWithoutIQN_ThrowsException() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        accessGroup.setScope(scope);

        when(scope.getScopeType()).thenReturn(com.cloud.storage.ScopeType.CLUSTER);

        Map<String, String> details = new HashMap<>();
        details.put(Constants.SVM_NAME, "svm1");
        details.put(Constants.PROTOCOL, ProtocolType.ISCSI.name());

        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");
        lenient().when(primaryDataStoreInfo.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);

        List<HostVO> hosts = new ArrayList<>();
        HostVO host1 = mock(HostVO.class);
        when(host1.getStorageUrl()).thenReturn("invalid-storage-url");
        hosts.add(host1);
        accessGroup.setHostsToConnect(hosts);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.createAccessGroup(accessGroup));
        }
    }

    @Test
    void testCreateAccessGroup_HostWithNullStorageUrl_ThrowsException() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        accessGroup.setScope(scope);

        when(scope.getScopeType()).thenReturn(com.cloud.storage.ScopeType.CLUSTER);

        Map<String, String> details = new HashMap<>();
        details.put(Constants.SVM_NAME, "svm1");
        details.put(Constants.PROTOCOL, ProtocolType.ISCSI.name());

        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");
        lenient().when(primaryDataStoreInfo.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);

        List<HostVO> hosts = new ArrayList<>();
        HostVO host1 = mock(HostVO.class);
        when(host1.getStorageUrl()).thenReturn(null);
        hosts.add(host1);
        accessGroup.setHostsToConnect(hosts);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.createAccessGroup(accessGroup));
        }
    }

    @Test
    void testCreateAccessGroup_FeignExceptionNon409_ThrowsException() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        accessGroup.setScope(scope);

        Map<String, String> details = new HashMap<>();
        details.put(Constants.SVM_NAME, "svm1");
        details.put(Constants.PROTOCOL, ProtocolType.ISCSI.name());

        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");
        lenient().when(primaryDataStoreInfo.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);

        List<HostVO> hosts = new ArrayList<>();
        HostVO host1 = mock(HostVO.class);
        when(host1.getStorageUrl()).thenReturn("iqn.1993-08.org.debian:01:host1");
        hosts.add(host1);
        accessGroup.setHostsToConnect(hosts);

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);
        when(feignException.getMessage()).thenReturn("Internal server error");

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            when(sanFeignClient.createIgroup(eq(authHeader), eq(true), any(Igroup.class)))
                    .thenThrow(feignException);

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.createAccessGroup(accessGroup));
        }
    }

    @Test
    void testCreateAccessGroup_EmptyResponseRecords_ThrowsException() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        accessGroup.setScope(scope);

        Map<String, String> details = new HashMap<>();
        details.put(Constants.SVM_NAME, "svm1");
        details.put(Constants.PROTOCOL, ProtocolType.ISCSI.name());

        when(primaryDataStoreInfo.getDetails()).thenReturn(details);
        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");
        lenient().when(primaryDataStoreInfo.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);

        List<HostVO> hosts = new ArrayList<>();
        HostVO host1 = mock(HostVO.class);
        when(host1.getStorageUrl()).thenReturn("iqn.1993-08.org.debian:01:host1");
        hosts.add(host1);
        accessGroup.setHostsToConnect(hosts);

        OntapResponse<Igroup> response = new OntapResponse<>();
        response.setRecords(new ArrayList<>());

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            when(sanFeignClient.createIgroup(eq(authHeader), eq(true), any(Igroup.class)))
                    .thenReturn(response);

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.createAccessGroup(accessGroup));
        }
    }

    @Test
    void testDeleteAccessGroup_NullAccessGroup_ThrowsException() {
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.deleteAccessGroup(null));
    }

    @Test
    void testDeleteAccessGroup_NullPrimaryDataStoreInfo_ThrowsException() {
        AccessGroup accessGroup = new AccessGroup();
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.deleteAccessGroup(accessGroup));
    }

    @Test
    void testDeleteAccessGroup_EmptyIgroupUuid_ThrowsException() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");

        Igroup igroup = new Igroup();
        igroup.setName("igroup1");
        // UUID is null

        OntapResponse<Igroup> response = new OntapResponse<>();
        response.setRecords(List.of(igroup));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenReturn(response);

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.deleteAccessGroup(accessGroup));
        }
    }

    @Test
    void testDeleteAccessGroup_FeignExceptionNon404_ThrowsException() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);
        when(feignException.getMessage()).thenReturn("Internal server error");

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenThrow(feignException);

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.deleteAccessGroup(accessGroup));
        }
    }

    @Test
    void testGetAccessGroup_NullValues_ThrowsException() {
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.getAccessGroup(null));
    }

    @Test
    void testGetAccessGroup_EmptyValues_ThrowsException() {
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.getAccessGroup(new HashMap<>()));
    }

    @Test
    void testGetAccessGroup_NullSvmName_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.NAME, "igroup1");
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.getAccessGroup(values));
    }

    @Test
    void testGetAccessGroup_NullIgroupName_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.getAccessGroup(values));
    }

    @Test
    void testGetAccessGroup_FeignExceptionNon404_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.NAME, "igroup1");

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);
        when(feignException.getMessage()).thenReturn("Internal server error");

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenThrow(feignException);

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.getAccessGroup(values));
        }
    }

    @Test
    void testGetCloudStackVolume_NullValues_ThrowsException() {
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.getCloudStackVolume(null));
    }

    @Test
    void testGetCloudStackVolume_EmptyValues_ThrowsException() {
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.getCloudStackVolume(new HashMap<>()));
    }

    @Test
    void testGetCloudStackVolume_NullSvmName_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.NAME, "/vol/vol1/lun1");
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.getCloudStackVolume(values));
    }

    @Test
    void testGetCloudStackVolume_NullLunName_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.getCloudStackVolume(values));
    }

    @Test
    void testGetCloudStackVolume_FeignExceptionNon404_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.NAME, "/vol/vol1/lun1");

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);
        when(feignException.getMessage()).thenReturn("Internal server error");

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getLunResponse(eq(authHeader), anyMap())).thenThrow(feignException);

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.getCloudStackVolume(values));
        }
    }

    @Test
    void testEnableLogicalAccess_NullValues_ThrowsException() {
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.enableLogicalAccess(null));
    }

    @Test
    void testEnableLogicalAccess_MissingSvmName_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.LUN_DOT_NAME, "/vol/vol1/lun1");
        values.put(Constants.IGROUP_DOT_NAME, "igroup1");
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.enableLogicalAccess(values));
    }

    @Test
    void testEnableLogicalAccess_MissingLunName_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.IGROUP_DOT_NAME, "igroup1");
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.enableLogicalAccess(values));
    }

    @Test
    void testEnableLogicalAccess_MissingIgroupName_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.LUN_DOT_NAME, "/vol/vol1/lun1");
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.enableLogicalAccess(values));
    }

    @Test
    void testEnableLogicalAccess_FetchLunMapFails_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.LUN_DOT_NAME, "/vol/vol1/lun1");
        values.put(Constants.IGROUP_DOT_NAME, "igroup1");

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.createLunMap(eq(authHeader), eq(true), any(LunMap.class)))
                    .thenReturn(new OntapResponse<>());
            when(sanFeignClient.getLunMapResponse(eq(authHeader), anyMap()))
                    .thenThrow(new RuntimeException("Failed to fetch LunMap"));

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.enableLogicalAccess(values));
        }
    }

    @Test
    void testDisableLogicalAccess_NullValues_ThrowsException() {
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.disableLogicalAccess(null));
    }

    @Test
    void testDisableLogicalAccess_MissingLunUuid_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.IGROUP_DOT_UUID, "igroup-uuid-123");
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.disableLogicalAccess(values));
    }

    @Test
    void testDisableLogicalAccess_MissingIgroupUuid_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.LUN_DOT_UUID, "lun-uuid-123");
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.disableLogicalAccess(values));
    }

    @Test
    void testDisableLogicalAccess_FeignExceptionNon404_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.LUN_DOT_UUID, "lun-uuid-123");
        values.put(Constants.IGROUP_DOT_UUID, "igroup-uuid-123");

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);
        when(feignException.getMessage()).thenReturn("Internal server error");

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            doThrow(feignException).when(sanFeignClient).deleteLunMap(eq(authHeader), eq("lun-uuid-123"), eq("igroup-uuid-123"));

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.disableLogicalAccess(values));
        }
    }

    @Test
    void testGetLogicalAccess_NullValues_ThrowsException() {
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.getLogicalAccess(null));
    }

    @Test
    void testGetLogicalAccess_MissingSvmName_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.LUN_DOT_NAME, "/vol/vol1/lun1");
        values.put(Constants.IGROUP_DOT_NAME, "igroup1");
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.getLogicalAccess(values));
    }

    @Test
    void testEnsureLunMapped_CreateNewMapping_Success() {
        String svmName = "svm1";
        String lunName = "/vol/vol1/lun1";
        String accessGroupName = "igroup1";

        LunMap lunMap = new LunMap();
        lunMap.setLogicalUnitNumber(4);

        OntapResponse<LunMap> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        OntapResponse<LunMap> response = new OntapResponse<>();
        response.setRecords(List.of(lunMap));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            // First call returns empty (no existing mapping)
            when(sanFeignClient.getLunMapResponse(eq(authHeader), anyMap()))
                    .thenReturn(emptyResponse)  // First call - check existing
                    .thenReturn(response);      // Second call - after creation

            when(sanFeignClient.createLunMap(eq(authHeader), eq(true), any(LunMap.class)))
                    .thenReturn(new OntapResponse<>());

            String result = unifiedSANStrategy.ensureLunMapped(svmName, lunName, accessGroupName);

            assertEquals("4", result);
            verify(sanFeignClient).createLunMap(eq(authHeader), eq(true), any(LunMap.class));
        }
    }

    @Test
    void testEnsureLunMapped_FailedToMap_ThrowsException() {
        String svmName = "svm1";
        String lunName = "/vol/vol1/lun1";
        String accessGroupName = "igroup1";

        OntapResponse<LunMap> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getLunMapResponse(eq(authHeader), anyMap()))
                    .thenReturn(emptyResponse)  // First call - no existing
                    .thenReturn(emptyResponse); // Second call - still empty after creation

            when(sanFeignClient.createLunMap(eq(authHeader), eq(true), any(LunMap.class)))
                    .thenReturn(new OntapResponse<>());

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.ensureLunMapped(svmName, lunName, accessGroupName));
        }
    }

    @Test
    void testValidateInitiatorInAccessGroup_NullIgroupInitiators_ReturnsFalse() {
        String hostInitiator = "iqn.1993-08.org.debian:01:host1";
        String svmName = "svm1";
        String accessGroupName = "igroup1";

        Igroup igroup = new Igroup();
        igroup.setName(accessGroupName);
        igroup.setInitiators(null);

        OntapResponse<Igroup> response = new OntapResponse<>();
        response.setRecords(List.of(igroup));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenReturn(response);

            // Execute
            boolean result = unifiedSANStrategy.validateInitiatorInAccessGroup(hostInitiator, svmName, accessGroupName);

            // Verify
            assertFalse(result);
        }
    }

    // ============= Additional Test Cases for Complete Coverage =============

    @Test
    void testCreateCloudStackVolume_NullLun_ThrowsException() {
        CloudStackVolume request = new CloudStackVolume();
        request.setLun(null);

        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.createCloudStackVolume(request));
    }

    @Test
    void testCreateCloudStackVolume_EmptyResponse_ThrowsException() {
        Lun lun = new Lun();
        lun.setName("/vol/vol1/lun1");
        CloudStackVolume request = new CloudStackVolume();
        request.setLun(lun);

        OntapResponse<Lun> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.createLun(eq(authHeader), eq(true), any(Lun.class)))
                    .thenReturn(emptyResponse);

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.createCloudStackVolume(request));
        }
    }

    @Test
    void testCreateCloudStackVolume_NullResponse_ThrowsException() {
        Lun lun = new Lun();
        lun.setName("/vol/vol1/lun1");
        CloudStackVolume request = new CloudStackVolume();
        request.setLun(lun);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.createLun(eq(authHeader), eq(true), any(Lun.class)))
                    .thenReturn(null);

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.createCloudStackVolume(request));
        }
    }

    @Test
    void testDeleteCloudStackVolume_NullRequest_ThrowsException() {
        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.deleteCloudStackVolume(null));
    }

    @Test
    void testDeleteCloudStackVolume_NullLun_ThrowsException() {
        CloudStackVolume request = new CloudStackVolume();
        request.setLun(null);

        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.deleteCloudStackVolume(request));
    }

    @Test
    void testDeleteCloudStackVolume_FeignException_ThrowsCloudRuntimeException() {
        Lun lun = new Lun();
        lun.setName("/vol/vol1/lun1");
        lun.setUuid("lun-uuid-123");
        CloudStackVolume request = new CloudStackVolume();
        request.setLun(lun);

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);
        when(feignException.getMessage()).thenReturn("Internal server error");

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            doThrow(feignException).when(sanFeignClient).deleteLun(eq(authHeader), eq("lun-uuid-123"), anyMap());

            assertThrows(CloudRuntimeException.class,
                () -> unifiedSANStrategy.deleteCloudStackVolume(request));
        }
    }

    @Test
    void testGetCloudStackVolume_FeignException404_ReturnsNull() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.NAME, "/vol/vol1/lun1");

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(404);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getLunResponse(eq(authHeader), anyMap())).thenThrow(feignException);

            CloudStackVolume result = unifiedSANStrategy.getCloudStackVolume(values);

            assertNull(result);
        }
    }

    @Test
    void testGetCloudStackVolume_EmptyResponse_ReturnsNull() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.NAME, "/vol/vol1/lun1");

        OntapResponse<Lun> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getLunResponse(eq(authHeader), anyMap())).thenReturn(emptyResponse);

            CloudStackVolume result = unifiedSANStrategy.getCloudStackVolume(values);

            assertNull(result);
        }
    }

    @Test
    void testGetAccessGroup_FeignException404_ReturnsNull() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.NAME, "igroup1");

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(404);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenThrow(feignException);

            AccessGroup result = unifiedSANStrategy.getAccessGroup(values);

            assertNull(result);
        }
    }

    @Test
    void testGetAccessGroup_EmptyResponse_ReturnsNull() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.NAME, "igroup1");

        OntapResponse<Igroup> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenReturn(emptyResponse);

            AccessGroup result = unifiedSANStrategy.getAccessGroup(values);

            assertNull(result);
        }
    }

    @Test
    void testEnableLogicalAccess_EmptySvmName_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "");
        values.put(Constants.LUN_DOT_NAME, "/vol/vol1/lun1");
        values.put(Constants.IGROUP_DOT_NAME, "igroup1");

        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.enableLogicalAccess(values));
    }

    @Test
    void testEnableLogicalAccess_EmptyLunName_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.LUN_DOT_NAME, "");
        values.put(Constants.IGROUP_DOT_NAME, "igroup1");

        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.enableLogicalAccess(values));
    }

    @Test
    void testEnableLogicalAccess_EmptyIgroupName_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.LUN_DOT_NAME, "/vol/vol1/lun1");
        values.put(Constants.IGROUP_DOT_NAME, "");

        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.enableLogicalAccess(values));
    }

    @Test
    void testDisableLogicalAccess_EmptyLunUuid_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.LUN_DOT_UUID, "");
        values.put(Constants.IGROUP_DOT_UUID, "igroup-uuid-123");

        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.disableLogicalAccess(values));
    }

    @Test
    void testDisableLogicalAccess_EmptyIgroupUuid_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.LUN_DOT_UUID, "lun-uuid-123");
        values.put(Constants.IGROUP_DOT_UUID, "");

        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.disableLogicalAccess(values));
    }

    @Test
    void testDisableLogicalAccess_FeignException404_SkipsDeletion() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.LUN_DOT_UUID, "lun-uuid-123");
        values.put(Constants.IGROUP_DOT_UUID, "igroup-uuid-123");

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(404);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            doThrow(feignException).when(sanFeignClient).deleteLunMap(eq(authHeader), eq("lun-uuid-123"), eq("igroup-uuid-123"));

            // Should not throw exception for 404
            assertDoesNotThrow(() -> unifiedSANStrategy.disableLogicalAccess(values));
        }
    }

    @Test
    void testGetLogicalAccess_MissingLunName_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.IGROUP_DOT_NAME, "igroup1");

        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.getLogicalAccess(values));
    }

    @Test
    void testGetLogicalAccess_MissingIgroupName_ThrowsException() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.LUN_DOT_NAME, "/vol/vol1/lun1");

        assertThrows(CloudRuntimeException.class,
            () -> unifiedSANStrategy.getLogicalAccess(values));
    }

    @Test
    void testGetLogicalAccess_EmptyResponse_ReturnsNull() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.LUN_DOT_NAME, "/vol/vol1/lun1");
        values.put(Constants.IGROUP_DOT_NAME, "igroup1");

        OntapResponse<LunMap> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getLunMapResponse(eq(authHeader), anyMap())).thenReturn(emptyResponse);

            Map<String, String> result = unifiedSANStrategy.getLogicalAccess(values);

            assertNull(result);
        }
    }

    @Test
    void testGetLogicalAccess_ExceptionThrown_ReturnsNull() {
        Map<String, String> values = new HashMap<>();
        values.put(Constants.SVM_DOT_NAME, "svm1");
        values.put(Constants.LUN_DOT_NAME, "/vol/vol1/lun1");
        values.put(Constants.IGROUP_DOT_NAME, "igroup1");

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getLunMapResponse(eq(authHeader), anyMap()))
                    .thenThrow(new RuntimeException("Connection failed"));

            Map<String, String> result = unifiedSANStrategy.getLogicalAccess(values);

            assertNull(result);
        }
    }

    @Test
    void testValidateInitiatorInAccessGroup_NullInitiator_ReturnsFalse() {
        boolean result = unifiedSANStrategy.validateInitiatorInAccessGroup(null, "svm1", "igroup1");
        assertFalse(result);
    }

    @Test
    void testValidateInitiatorInAccessGroup_AccessGroupNotFound_ReturnsFalse() {
        String hostInitiator = "iqn.1993-08.org.debian:01:host1";
        String svmName = "svm1";
        String accessGroupName = "igroup1";

        OntapResponse<Igroup> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenReturn(emptyResponse);

            boolean result = unifiedSANStrategy.validateInitiatorInAccessGroup(hostInitiator, svmName, accessGroupName);

            assertFalse(result);
        }
    }

    @Test
    void testDeleteAccessGroup_FeignException404_SkipsDeletion() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");

        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(404);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenThrow(feignException);

            // Should not throw exception for 404
            assertDoesNotThrow(() -> unifiedSANStrategy.deleteAccessGroup(accessGroup));
        }
    }

    @Test
    void testDeleteAccessGroup_NotFoundInResponse_SkipsDeletion() {
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setPrimaryDataStoreInfo(primaryDataStoreInfo);
        when(primaryDataStoreInfo.getUuid()).thenReturn("pool-uuid-123");

        OntapResponse<Igroup> emptyResponse = new OntapResponse<>();
        emptyResponse.setRecords(new ArrayList<>());

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);
            utilityMock.when(() -> Utility.getIgroupName("svm1", "pool-uuid-123"))
                    .thenReturn("igroup1");

            when(sanFeignClient.getIgroupResponse(eq(authHeader), anyMap())).thenReturn(emptyResponse);

            // Should not throw exception when not found
            assertDoesNotThrow(() -> unifiedSANStrategy.deleteAccessGroup(accessGroup));
        }
    }

    @Test
    void testEnsureLunMapped_ExistingMapping_ReturnsExistingNumber() {
        // Setup
        String svmName = "svm1";
        String lunName = "/vol/vol1/lun1";
        String accessGroupName = "igroup1";

        LunMap lunMap = new LunMap();
        lunMap.setLogicalUnitNumber(3);

        OntapResponse<LunMap> response = new OntapResponse<>();
        response.setRecords(List.of(lunMap));

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.generateAuthHeader("admin", "password"))
                    .thenReturn(authHeader);

            when(sanFeignClient.getLunMapResponse(eq(authHeader), anyMap())).thenReturn(response);

            // Execute
            String result = unifiedSANStrategy.ensureLunMapped(svmName, lunName, accessGroupName);

            // Verify
            assertEquals("3", result);
            // Verify createLunMap was NOT called
            verify(sanFeignClient, never()).createLunMap(any(), anyBoolean(), any(LunMap.class));
        }
    }
}
