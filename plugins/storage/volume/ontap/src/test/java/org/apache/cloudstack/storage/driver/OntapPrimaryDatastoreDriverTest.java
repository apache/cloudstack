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
package org.apache.cloudstack.storage.driver;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.service.UnifiedSANStrategy;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.cloudstack.storage.utils.Utility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static com.cloud.agent.api.to.DataObjectType.VOLUME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OntapPrimaryDatastoreDriverTest {

    @Mock
    private StoragePoolDetailsDao storagePoolDetailsDao;

    @Mock
    private PrimaryDataStoreDao storagePoolDao;

    @Mock
    private VMInstanceDao vmDao;

    @Mock
    private VolumeDao volumeDao;

    @Mock
    private VolumeDetailsDao volumeDetailsDao;

    @Mock
    private DataStore dataStore;

    @Mock
    private VolumeInfo volumeInfo;

    @Mock
    private StoragePoolVO storagePool;

    @Mock
    private VolumeVO volumeVO;

    @Mock
    private Host host;

    @Mock
    private UnifiedSANStrategy sanStrategy;

    @Mock
    private AsyncCompletionCallback<CreateCmdResult> createCallback;

    @Mock
    private AsyncCompletionCallback<CommandResult> commandCallback;

    @InjectMocks
    private OntapPrimaryDatastoreDriver driver;

    private Map<String, String> storagePoolDetails;

    @BeforeEach
    void setUp() {
        storagePoolDetails = new HashMap<>();
        storagePoolDetails.put(Constants.PROTOCOL, ProtocolType.ISCSI.name());
        storagePoolDetails.put(Constants.SVM_NAME, "svm1");
    }

    @Test
    void testGetCapabilities() {
        Map<String, String> capabilities = driver.getCapabilities();

        assertNotNull(capabilities);
        assertEquals(Boolean.FALSE.toString(), capabilities.get("STORAGE_SYSTEM_SNAPSHOT"));
        assertEquals(Boolean.FALSE.toString(), capabilities.get("CAN_CREATE_VOLUME_FROM_SNAPSHOT"));
    }

    @Test
    void testCreateAsync_NullDataObject_ThrowsException() {
        assertThrows(InvalidParameterValueException.class,
            () -> driver.createAsync(dataStore, null, createCallback));
    }

    @Test
    void testCreateAsync_NullDataStore_ThrowsException() {
        assertThrows(InvalidParameterValueException.class,
            () -> driver.createAsync(null, volumeInfo, createCallback));
    }

    @Test
    void testCreateAsync_NullCallback_ThrowsException() {
        assertThrows(InvalidParameterValueException.class,
            () -> driver.createAsync(dataStore, volumeInfo, null));
    }

    @Test
    void testCreateAsync_VolumeWithISCSI_Success() {
        // Setup
        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getUuid()).thenReturn("pool-uuid-123");
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getName()).thenReturn("test-volume");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(storagePool.getPath()).thenReturn("iqn.1992-08.com.netapp:sn.123456");

        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(100L);

        Lun mockLun = new Lun();
        mockLun.setName("/vol/vol1/lun1");
        mockLun.setUuid("lun-uuid-123");
        CloudStackVolume mockCloudStackVolume = new CloudStackVolume();
        mockCloudStackVolume.setLun(mockLun);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            utilityMock.when(() -> Utility.createCloudStackVolumeRequestByProtocol(
                    any(), any(), any())).thenReturn(mockCloudStackVolume);
            utilityMock.when(() -> Utility.getIgroupName(anyString(), anyString()))
                    .thenReturn("igroup1");

            when(sanStrategy.createCloudStackVolume(any())).thenReturn(mockCloudStackVolume);
            when(sanStrategy.ensureLunMapped(anyString(), anyString(), anyString())).thenReturn("0");

            // Execute
            driver.createAsync(dataStore, volumeInfo, createCallback);

            // Verify
            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());

            CreateCmdResult result = resultCaptor.getValue();
            assertNotNull(result);
            assertTrue(result.isSuccess());

            verify(volumeDetailsDao).addDetail(eq(100L), eq(Constants.LUN_DOT_UUID), eq("lun-uuid-123"), eq(false));
            verify(volumeDetailsDao).addDetail(eq(100L), eq(Constants.LUN_DOT_NAME), eq("/vol/vol1/lun1"), eq(false));
            verify(volumeDao).update(eq(100L), any(VolumeVO.class));
        }
    }

    @Test
    void testCreateAsync_VolumeWithNFS_Success() {
        // Setup
        storagePoolDetails.put(Constants.PROTOCOL, ProtocolType.NFS3.name());

        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getUuid()).thenReturn("pool-uuid-123");
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getName()).thenReturn("test-volume");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(100L);

        CloudStackVolume mockCloudStackVolume = new CloudStackVolume();

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            utilityMock.when(() -> Utility.createCloudStackVolumeRequestByProtocol(
                    any(), any(), any())).thenReturn(mockCloudStackVolume);

            when(sanStrategy.createCloudStackVolume(any())).thenReturn(mockCloudStackVolume);

            // Execute
            driver.createAsync(dataStore, volumeInfo, createCallback);

            // Verify
            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());

            CreateCmdResult result = resultCaptor.getValue();
            assertNotNull(result);
            assertTrue(result.isSuccess());
            verify(volumeDao).update(eq(100L), any(VolumeVO.class));
        }
    }

    @Test
    void testDeleteAsync_NullStore_ThrowsException() {
        ArgumentCaptor<CommandResult> resultCaptor = ArgumentCaptor.forClass(CommandResult.class);

        driver.deleteAsync(null, volumeInfo, commandCallback);

        verify(commandCallback).complete(resultCaptor.capture());
        CommandResult result = resultCaptor.getValue();
        assertFalse(result.isSuccess());
        assertTrue(result.getResult().contains("store or data is null"));
    }

    @Test
    void testDeleteAsync_ISCSIVolume_Success() {
        // Setup
        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        VolumeDetailVO lunNameDetail = new VolumeDetailVO(100L, Constants.LUN_DOT_NAME, "/vol/vol1/lun1", false);
        VolumeDetailVO lunUuidDetail = new VolumeDetailVO(100L, Constants.LUN_DOT_UUID, "lun-uuid-123", false);

        when(volumeDetailsDao.findDetail(100L, Constants.LUN_DOT_NAME)).thenReturn(lunNameDetail);
        when(volumeDetailsDao.findDetail(100L, Constants.LUN_DOT_UUID)).thenReturn(lunUuidDetail);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);

            doNothing().when(sanStrategy).deleteCloudStackVolume(any());

            // Execute
            driver.deleteAsync(dataStore, volumeInfo, commandCallback);

            // Verify
            ArgumentCaptor<CommandResult> resultCaptor = ArgumentCaptor.forClass(CommandResult.class);
            verify(commandCallback).complete(resultCaptor.capture());

            CommandResult result = resultCaptor.getValue();
            assertNotNull(result);
            assertTrue(result.isSuccess());
            verify(sanStrategy).deleteCloudStackVolume(any(CloudStackVolume.class));
        }
    }

    @Test
    void testDeleteAsync_NFSVolume_Success() {
        // Setup
        storagePoolDetails.put(Constants.PROTOCOL, ProtocolType.NFS3.name());

        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getType()).thenReturn(VOLUME);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        // Execute
        driver.deleteAsync(dataStore, volumeInfo, commandCallback);

        // Verify
        ArgumentCaptor<CommandResult> resultCaptor = ArgumentCaptor.forClass(CommandResult.class);
        verify(commandCallback).complete(resultCaptor.capture());

        CommandResult result = resultCaptor.getValue();
        assertNotNull(result);
        // NFS deletion doesn't fail, handled by hypervisor
    }

    @Test
    void testGrantAccess_NullParameters_ThrowsException() {
        assertThrows(CloudRuntimeException.class,
            () -> driver.grantAccess(null, host, dataStore));

        assertThrows(CloudRuntimeException.class,
            () -> driver.grantAccess(volumeInfo, null, dataStore));

        assertThrows(CloudRuntimeException.class,
            () -> driver.grantAccess(volumeInfo, host, null));
    }

    @Test
    void testGrantAccess_ClusterScope_Success() {
        // Setup
        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getUuid()).thenReturn("pool-uuid-123");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getScope()).thenReturn(ScopeType.CLUSTER);
        when(storagePool.getPath()).thenReturn("iqn.1992-08.com.netapp:sn.123456");
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);

        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(100L);

        when(host.getStorageUrl()).thenReturn("iqn.1993-08.org.debian:01:host1");

        VolumeDetailVO lunNameDetail = new VolumeDetailVO(100L, Constants.LUN_DOT_NAME, "/vol/vol1/lun1", false);
        when(volumeDetailsDao.findDetail(100L, Constants.LUN_DOT_NAME)).thenReturn(lunNameDetail);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            utilityMock.when(() -> Utility.getIgroupName(anyString(), anyString()))
                    .thenReturn("igroup1");

            when(sanStrategy.validateInitiatorInAccessGroup(anyString(), anyString(), anyString()))
                    .thenReturn(true);
            when(sanStrategy.ensureLunMapped(anyString(), anyString(), anyString())).thenReturn("0");

            // Execute
            boolean result = driver.grantAccess(volumeInfo, host, dataStore);

            // Verify
            assertTrue(result);
            verify(volumeDao).update(eq(100L), any(VolumeVO.class));
            verify(sanStrategy).validateInitiatorInAccessGroup(anyString(), anyString(), anyString());
            verify(sanStrategy).ensureLunMapped(anyString(), anyString(), anyString());
        }
    }

    @Test
    void testGrantAccess_InitiatorNotInIgroup_ThrowsException() {
        // Setup
        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getUuid()).thenReturn("pool-uuid-123");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getScope()).thenReturn(ScopeType.CLUSTER);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(100L);

        when(host.getStorageUrl()).thenReturn("iqn.1993-08.org.debian:01:host1");

        VolumeDetailVO lunNameDetail = new VolumeDetailVO(100L, Constants.LUN_DOT_NAME, "/vol/vol1/lun1", false);
        when(volumeDetailsDao.findDetail(100L, Constants.LUN_DOT_NAME)).thenReturn(lunNameDetail);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            utilityMock.when(() -> Utility.getIgroupName(anyString(), anyString()))
                    .thenReturn("igroup1");

            when(sanStrategy.validateInitiatorInAccessGroup(anyString(), anyString(), anyString()))
                    .thenReturn(false);

            // Execute & Verify
            CloudRuntimeException exception = assertThrows(CloudRuntimeException.class,
                () -> driver.grantAccess(volumeInfo, host, dataStore));

            assertTrue(exception.getMessage().contains("is not present in iGroup"));
        }
    }

    @Test
    void testRevokeAccess_VolumeAttachedToRunningVM_SkipsRevoke() {
        // Setup
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);

        VolumeVO mockVolume = mock(VolumeVO.class);
        when(mockVolume.getInstanceId()).thenReturn(200L);
        when(volumeDao.findById(100L)).thenReturn(mockVolume);

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        when(vm.getInstanceName()).thenReturn("i-2-100-VM");
        when(vmDao.findById(200L)).thenReturn(vm);

        // Execute
        driver.revokeAccess(volumeInfo, host, dataStore);

        // Verify - should skip revoke for running VM
        verify(storagePoolDao, never()).findById(anyLong());
    }

    @Test
    void testRevokeAccess_ISCSIVolume_Success() {
        // Setup
        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);

        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(100L);
        when(volumeVO.getInstanceId()).thenReturn(null);
        when(volumeVO.getName()).thenReturn("test-volume");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getScope()).thenReturn(ScopeType.CLUSTER);
        when(storagePool.getUuid()).thenReturn("pool-uuid-123");
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        when(host.getStorageUrl()).thenReturn("iqn.1993-08.org.debian:01:host1");
        when(host.getName()).thenReturn("host1");

        VolumeDetailVO lunNameDetail = new VolumeDetailVO(100L, Constants.LUN_DOT_NAME, "/vol/vol1/lun1", false);
        when(volumeDetailsDao.findDetail(100L, Constants.LUN_DOT_NAME)).thenReturn(lunNameDetail);

        Lun mockLun = new Lun();
        mockLun.setName("/vol/vol1/lun1");
        mockLun.setUuid("lun-uuid-123");
        CloudStackVolume mockCloudStackVolume = new CloudStackVolume();
        mockCloudStackVolume.setLun(mockLun);

        org.apache.cloudstack.storage.feign.model.Igroup mockIgroup = mock(org.apache.cloudstack.storage.feign.model.Igroup.class);
        when(mockIgroup.getName()).thenReturn("igroup1");
        when(mockIgroup.getUuid()).thenReturn("igroup-uuid-123");
        AccessGroup mockAccessGroup = new AccessGroup();
        mockAccessGroup.setIgroup(mockIgroup);

        try (MockedStatic<Utility> utilityMock = mockStatic(Utility.class)) {
            utilityMock.when(() -> Utility.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            utilityMock.when(() -> Utility.getIgroupName(anyString(), anyString()))
                    .thenReturn("igroup1");

            // Mock the methods called by getCloudStackVolumeByName and getAccessGroupByName
            when(sanStrategy.getCloudStackVolume(argThat(map ->
                map != null &&
                "/vol/vol1/lun1".equals(map.get("name")) &&
                "svm1".equals(map.get("svm.name"))
            ))).thenReturn(mockCloudStackVolume);

            when(sanStrategy.getAccessGroup(argThat(map ->
                map != null &&
                "igroup1".equals(map.get("name")) &&
                "svm1".equals(map.get("svm.name"))
            ))).thenReturn(mockAccessGroup);

            when(sanStrategy.validateInitiatorInAccessGroup(
                eq("iqn.1993-08.org.debian:01:host1"),
                eq("svm1"),
                eq("igroup1")
            )).thenReturn(true);

            doNothing().when(sanStrategy).disableLogicalAccess(argThat(map ->
                map != null &&
                "lun-uuid-123".equals(map.get("lun.uuid")) &&
                "igroup-uuid-123".equals(map.get("igroup.uuid"))
            ));

            // Execute
            driver.revokeAccess(volumeInfo, host, dataStore);

            // Verify
            verify(sanStrategy).getCloudStackVolume(any());
            verify(sanStrategy).getAccessGroup(any());
            verify(sanStrategy).validateInitiatorInAccessGroup(anyString(), anyString(), anyString());
            verify(sanStrategy).disableLogicalAccess(any());
        }
    }

    @Test
    void testCanHostAccessStoragePool_ReturnsTrue() {
        assertTrue(driver.canHostAccessStoragePool(host, storagePool));
    }

    @Test
    void testIsVmInfoNeeded_ReturnsTrue() {
        assertTrue(driver.isVmInfoNeeded());
    }

    @Test
    void testIsStorageSupportHA_ReturnsTrue() {
        assertTrue(driver.isStorageSupportHA(Storage.StoragePoolType.NetworkFilesystem));
    }

    @Test
    void testGetChapInfo_ReturnsNull() {
        assertNull(driver.getChapInfo(volumeInfo));
    }

    @Test
    void testCanProvideStorageStats_ReturnsFalse() {
        assertFalse(driver.canProvideStorageStats());
    }

    @Test
    void testCanProvideVolumeStats_ReturnsFalse() {
        assertFalse(driver.canProvideVolumeStats());
    }
}
