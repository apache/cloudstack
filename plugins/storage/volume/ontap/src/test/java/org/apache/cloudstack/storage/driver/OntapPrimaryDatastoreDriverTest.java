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
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.model.Igroup;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.service.UnifiedNASStrategy;
import org.apache.cloudstack.storage.service.UnifiedSANStrategy;
import org.apache.cloudstack.storage.service.model.AccessGroup;
import org.apache.cloudstack.storage.service.model.CloudStackVolume;
import org.apache.cloudstack.storage.service.model.ProtocolType;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import org.apache.cloudstack.storage.utils.OntapStorageUtils;
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

import static com.cloud.agent.api.to.DataObjectType.TEMPLATE;
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
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
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
    private VolumeDao volumeDao;

    @Mock
    private VolumeDetailsDao volumeDetailsDao;

    @Mock
    private VMTemplatePoolDao vmTemplatePoolDao;

    @Mock
    private VMTemplateStoragePoolVO templatePoolRef;

    @Mock
    private DataStore dataStore;

    @Mock
    private VolumeInfo volumeInfo;

    @Mock
    private TemplateInfo templateInfo;

    @Mock
    private StoragePoolVO storagePool;

    @Mock
    private VolumeVO volumeVO;

    @Mock
    private Host host;

    @Mock
    private UnifiedSANStrategy sanStrategy;

    @Mock
    private UnifiedNASStrategy nasStrategy;

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
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.ISCSI.name());
        storagePoolDetails.put(OntapStorageConstants.SVM_NAME, "svm1");
    }

    @Test
    void testGetCapabilities() {
        Map<String, String> capabilities = driver.getCapabilities();

        assertNotNull(capabilities);
        // With SIS clone approach, driver advertises storage system snapshot capability
        // so StorageSystemSnapshotStrategy handles snapshot backup to secondary storage
        assertEquals(Boolean.TRUE.toString(), capabilities.get("STORAGE_SYSTEM_SNAPSHOT"));
        assertEquals(Boolean.TRUE.toString(), capabilities.get("CAN_CREATE_VOLUME_FROM_SNAPSHOT"));
        assertEquals(Boolean.TRUE.toString(), capabilities.get("CAN_REVERT_VOLUME_TO_SNAPSHOT"));
        assertEquals(Boolean.TRUE.toString(), capabilities.get("CAN_CREATE_VOLUME_FROM_VOLUME"));
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
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getName()).thenReturn("test-volume");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getName()).thenReturn("vol1");
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(storagePool.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);

        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(100L);

        Lun mockLun = new Lun();
        mockLun.setName("/vol/vol1/lun1");
        mockLun.setUuid("lun-uuid-123");
        CloudStackVolume responseVolume = new CloudStackVolume();
        responseVolume.setLun(mockLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any()))
                    .thenReturn(sanStrategy);
            when(sanStrategy.createCloudStackVolume(any())).thenReturn(responseVolume);

            // Execute
            driver.createAsync(dataStore, volumeInfo, createCallback);

            // Verify
            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());

            CreateCmdResult result = resultCaptor.getValue();
            assertNotNull(result);
            assertTrue(result.isSuccess());

            verify(volumeDetailsDao).addDetail(eq(100L), eq(OntapStorageConstants.LUN_DOT_UUID), eq("lun-uuid-123"), eq(false));
            verify(volumeDetailsDao).addDetail(eq(100L), eq(OntapStorageConstants.LUN_DOT_NAME), eq("/vol/vol1/lun1"), eq(false));
            verify(volumeVO).setFormat(Storage.ImageFormat.QCOW2);
            verify(volumeDao).update(eq(100L), any(VolumeVO.class));
        }
    }

    @Test
    void testCreateAsync_VolumeWithNFS_Success() {
        // Setup
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());

        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getName()).thenReturn("test-volume");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(storagePool.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(100L);

        CloudStackVolume mockCloudStackVolume = new CloudStackVolume();

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);

            when(sanStrategy.createCloudStackVolume(any())).thenReturn(mockCloudStackVolume);

            // Execute
            driver.createAsync(dataStore, volumeInfo, createCallback);

            // Verify
            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());

            CreateCmdResult result = resultCaptor.getValue();
            assertNotNull(result);
            assertTrue(result.isSuccess());
            verify(volumeVO).setFormat(Storage.ImageFormat.QCOW2);
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

        VolumeDetailVO lunNameDetail = new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_NAME, "/vol/vol1/lun1", false);
        VolumeDetailVO lunUuidDetail = new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_UUID, "lun-uuid-123", false);

        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_NAME)).thenReturn(lunNameDetail);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_UUID)).thenReturn(lunUuidDetail);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
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
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());

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
    void testDeleteAsync_Template_DeletesCacheLun() {
        when(dataStore.getId()).thenReturn(1L);
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getId()).thenReturn(50L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getName()).thenReturn("vol1");
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);
        when(templatePoolRef.getLocalDownloadPath()).thenReturn("template-lun-uuid");

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails)).thenReturn(sanStrategy);

            driver.deleteAsync(dataStore, templateInfo, commandCallback);

            ArgumentCaptor<CommandResult> resultCaptor = ArgumentCaptor.forClass(CommandResult.class);
            verify(commandCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());

            ArgumentCaptor<CloudStackVolume> requestCaptor = ArgumentCaptor.forClass(CloudStackVolume.class);
            verify(sanStrategy).deleteCloudStackVolume(requestCaptor.capture());
            assertEquals("template-lun-uuid", requestCaptor.getValue().getLun().getUuid());
        }
    }

    @Test
    void testDeleteAsync_Template_NoCachedLun_SucceedsWithoutCallingOntap() {
        when(dataStore.getId()).thenReturn(1L);
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getId()).thenReturn(50L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(null);

        driver.deleteAsync(dataStore, templateInfo, commandCallback);

        ArgumentCaptor<CommandResult> resultCaptor = ArgumentCaptor.forClass(CommandResult.class);
        verify(commandCallback).complete(resultCaptor.capture());
        assertTrue(resultCaptor.getValue().isSuccess());
        verify(sanStrategy, never()).deleteCloudStackVolume(any());
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

        when(host.getName()).thenReturn("host1");
        when(host.getUuid()).thenReturn("host-uuid-1");

        VolumeDetailVO lunNameDetail = new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_NAME, "/vol/vol1/lun1", false);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_NAME)).thenReturn(lunNameDetail);

        // Mock AccessGroup with existing igroup
        AccessGroup existingAccessGroup = new AccessGroup();
        Igroup existingIgroup = new Igroup();
        existingIgroup.setName("igroup1");
        existingAccessGroup.setIgroup(existingIgroup);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            utilityMock.when(() -> OntapStorageUtils.getIgroupName(anyString(), anyString()))
                    .thenReturn("igroup1");

            when(sanStrategy.getAccessGroup(any())).thenReturn(existingAccessGroup);
            when(sanStrategy.ensureLunMapped(anyString(), anyString(), anyString())).thenReturn("0");

            // Execute
            boolean result = driver.grantAccess(volumeInfo, host, dataStore);

            // Verify
            assertTrue(result);
            verify(volumeDao).update(eq(100L), any(VolumeVO.class));
            verify(sanStrategy).getAccessGroup(any());
            verify(sanStrategy).ensureLunMapped(anyString(), anyString(), anyString());
            verify(sanStrategy, never()).validateInitiatorInAccessGroup(anyString(), anyString(), any(Igroup.class));
        }
    }

    @Test
    void testGrantAccess_IgroupNotFound_CreatesNewIgroup() {
        // Setup - use HostVO mock since production code casts Host to HostVO
        HostVO hostVO = mock(HostVO.class);
        when(hostVO.getName()).thenReturn("host1");
        when(hostVO.getUuid()).thenReturn("host-uuid-1");

        when(dataStore.getId()).thenReturn(1L);
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

        VolumeDetailVO lunNameDetail = new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_NAME, "/vol/vol1/lun1", false);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_NAME)).thenReturn(lunNameDetail);

        // Mock getAccessGroup returning null (igroup doesn't exist)
        AccessGroup createdAccessGroup = new AccessGroup();
        Igroup createdIgroup = new Igroup();
        createdIgroup.setName("igroup1");
        createdAccessGroup.setIgroup(createdIgroup);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            utilityMock.when(() -> OntapStorageUtils.getIgroupName(anyString(), anyString()))
                    .thenReturn("igroup1");

            when(sanStrategy.getAccessGroup(any())).thenReturn(null);
            when(sanStrategy.createAccessGroup(any())).thenReturn(createdAccessGroup);
            when(sanStrategy.ensureLunMapped(anyString(), anyString(), anyString())).thenReturn("0");

            // Execute
            boolean result = driver.grantAccess(volumeInfo, hostVO, dataStore);

            // Verify
            assertTrue(result);
            verify(sanStrategy).getAccessGroup(any());
            verify(sanStrategy).createAccessGroup(any());
            verify(sanStrategy).ensureLunMapped(anyString(), anyString(), anyString());
            verify(volumeDao).update(eq(100L), any(VolumeVO.class));
        }
    }

    @Test
    void testRevokeAccess_NFSVolume_SkipsRevoke() {
        // Setup - NFS volumes have no LUN mapping, so revokeAccess is a no-op
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());
        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);

        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getName()).thenReturn("test-volume");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getScope()).thenReturn(ScopeType.CLUSTER);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(host.getName()).thenReturn("host1");

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);

            // Execute - NFS has no iSCSI protocol, so revokeAccessForVolume does nothing
            driver.revokeAccess(volumeInfo, host, dataStore);

            // Verify - no LUN unmap operations for NFS
            verify(sanStrategy, never()).disableLogicalAccess(any());
        }
    }

    @Test
    void testRevokeAccess_ISCSIVolume_Success() {
        // Setup
        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);

        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(100L);
        when(volumeVO.getName()).thenReturn("test-volume");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getScope()).thenReturn(ScopeType.CLUSTER);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        when(host.getStorageUrl()).thenReturn("iqn.1993-08.org.debian:01:host1");
        when(host.getName()).thenReturn("host1");
        when(host.getUuid()).thenReturn("host-uuid-1");

        VolumeDetailVO lunNameDetail = new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_NAME, "/vol/vol1/lun1", false);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_NAME)).thenReturn(lunNameDetail);

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

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            utilityMock.when(() -> OntapStorageUtils.getIgroupName(anyString(), anyString()))
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
                any(Igroup.class)
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
            verify(sanStrategy).validateInitiatorInAccessGroup(anyString(), anyString(), any(Igroup.class));
            verify(sanStrategy).disableLogicalAccess(any());
        }
    }

    @Test
    void testGetDataObjectSizeIncludingHypervisorSnapshotReserve_NoReserveAdded() {
        when(templateInfo.getSize()).thenReturn(5368709120L);

        assertEquals(5368709120L, driver.getDataObjectSizeIncludingHypervisorSnapshotReserve(templateInfo, storagePool));
    }

    @Test
    void testGetBytesRequiredForTemplate_AlreadyCached_ReturnsZero() {
        when(storagePool.getId()).thenReturn(1L);
        when(templateInfo.getId()).thenReturn(50L);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);

        assertEquals(0L, driver.getBytesRequiredForTemplate(templateInfo, storagePool));
    }

    @Test
    void testGetBytesRequiredForTemplate_NotCached_ReturnsVirtualSize() {
        when(storagePool.getId()).thenReturn(1L);
        when(templateInfo.getId()).thenReturn(50L);
        when(templateInfo.getSize()).thenReturn(5368709120L);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(null);

        assertEquals(5368709120L, driver.getBytesRequiredForTemplate(templateInfo, storagePool));
    }

    @Test
    void testCreateAsync_TemplateWithISCSI_CreatesLunAndRecordsCloneSource() {
        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getId()).thenReturn(50L);
        when(templateInfo.getSize()).thenReturn(5368709120L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getName()).thenReturn("vol1");
        when(storagePool.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);
        when(templatePoolRef.getId()).thenReturn(7L);

        Lun templateLun = new Lun();
        templateLun.setName("/vol/vol1/cs_tmpl_50");
        templateLun.setUuid("template-lun-uuid");
        CloudStackVolume created = new CloudStackVolume();
        created.setLun(templateLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.createCloudStackVolume(any())).thenReturn(created);

            driver.createAsync(dataStore, templateInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());

            // local_download_path carries the clone source; install_path is left for grantAccess
            verify(templatePoolRef).setLocalDownloadPath("template-lun-uuid");
            verify(templatePoolRef).setTemplateSize(5368709120L);
            verify(templatePoolRef, never()).setInstallPath(any());
            verify(vmTemplatePoolDao).update(eq(7L), any(VMTemplateStoragePoolVO.class));
        }
    }

    @Test
    void testCreateAsync_TemplateWithISCSI_UnknownSize_Fails() {
        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getId()).thenReturn(50L);
        when(templateInfo.getSize()).thenReturn(0L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);

        driver.createAsync(dataStore, templateInfo, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
    }

    @Test
    void testCreateAsync_TemplateWithNFS_IsMetadataOnly() {
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());

        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getUuid()).thenReturn("template-uuid");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        driver.createAsync(dataStore, templateInfo, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback).complete(resultCaptor.capture());
        assertTrue(resultCaptor.getValue().isSuccess());
        verify(vmTemplatePoolDao, never()).update(any(Long.class), any(VMTemplateStoragePoolVO.class));
    }

    @Test
    void testCreateAsync_VolumeClonedFromTemplate_ClonesWithoutGrowing() {
        stubVolumeCloneFromTemplate(5368709120L, 5368709120L);

        Lun clonedLun = new Lun();
        clonedLun.setName("/vol/vol1/test_volume");
        clonedLun.setUuid("cloned-lun-uuid");
        CloudStackVolume cloned = new CloudStackVolume();
        cloned.setLun(clonedLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.cloneCloudStackVolume(any())).thenReturn(cloned);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());

            ArgumentCaptor<CloudStackVolume> requestCaptor = ArgumentCaptor.forClass(CloudStackVolume.class);
            verify(sanStrategy).cloneCloudStackVolume(requestCaptor.capture());
            assertEquals("template-lun-uuid", requestCaptor.getValue().getLun().getClone().getSource().getUuid());
            verify(sanStrategy, never()).createCloudStackVolume(any());
            verify(sanStrategy, never()).resizeCloudStackVolume(any(), anyLong());
            verify(volumeDetailsDao).addDetail(eq(100L), eq(OntapStorageConstants.LUN_DOT_UUID), eq("cloned-lun-uuid"), eq(false));
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromTemplate_GrowsWhenOfferingIsLarger() {
        stubVolumeCloneFromTemplate(5368709120L, 21474836480L);

        Lun clonedLun = new Lun();
        clonedLun.setName("/vol/vol1/test_volume");
        clonedLun.setUuid("cloned-lun-uuid");
        CloudStackVolume cloned = new CloudStackVolume();
        cloned.setLun(clonedLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.cloneCloudStackVolume(any())).thenReturn(cloned);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            verify(sanStrategy).resizeCloudStackVolume(eq(cloned), eq(21474836480L));
        }
    }

    /**
     * Sets up a volume create that the orchestrator has marked as a clone of a cached template.
     */
    private void stubVolumeCloneFromTemplate(long templateSize, long volumeSize) {
        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getName()).thenReturn("test-volume");
        when(volumeInfo.getSize()).thenReturn(volumeSize);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        lenient().when(storagePool.getName()).thenReturn("vol1");
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.Iscsi);
        when(storagePool.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(100L);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.CLONE_OF_TEMPLATE))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.CLONE_OF_TEMPLATE, "50", false));

        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);
        lenient().when(templatePoolRef.getLocalDownloadPath()).thenReturn("template-lun-uuid");
        when(templatePoolRef.getTemplateSize()).thenReturn(templateSize);
    }

    @Test
    void testGrantAccess_Template_WritesInstallPathAndRefreshesStoreTarget() {
        PrimaryDataStore primaryDataStore = mock(PrimaryDataStore.class);
        Map<String, String> dataStoreDetails = new HashMap<>();
        dataStoreDetails.put(PrimaryDataStore.MANAGED_STORE_TARGET, "stale-value");

        when(primaryDataStore.getId()).thenReturn(1L);
        when(primaryDataStore.getDetails()).thenReturn(dataStoreDetails);
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getId()).thenReturn(50L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getName()).thenReturn("vol1");
        when(storagePool.getScope()).thenReturn(ScopeType.CLUSTER);
        when(storagePool.getPath()).thenReturn("iqn.1992-08.com.netapp:sn.123456");
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);
        when(templatePoolRef.getId()).thenReturn(7L);

        when(host.getName()).thenReturn("host1");
        when(host.getUuid()).thenReturn("host-uuid-1");

        AccessGroup existingAccessGroup = new AccessGroup();
        Igroup existingIgroup = new Igroup();
        existingIgroup.setName("igroup1");
        existingAccessGroup.setIgroup(existingIgroup);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails)).thenReturn(sanStrategy);
            utilityMock.when(() -> OntapStorageUtils.getIgroupName(anyString(), anyString())).thenReturn("igroup1");

            when(sanStrategy.getAccessGroup(any())).thenReturn(existingAccessGroup);
            when(sanStrategy.ensureLunMapped(eq("svm1"), eq("/vol/vol1/cs_tmpl_50"), eq("igroup1"))).thenReturn("3");

            assertTrue(driver.grantAccess(templateInfo, host, primaryDataStore));

            String expectedPath = "/iqn.1992-08.com.netapp:sn.123456/3";
            verify(templatePoolRef).setInstallPath(expectedPath);
            verify(vmTemplatePoolDao).update(eq(7L), any(VMTemplateStoragePoolVO.class));

            ArgumentCaptor<Map<String, String>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(primaryDataStore).setDetails(detailsCaptor.capture());
            assertEquals(expectedPath, detailsCaptor.getValue().get(PrimaryDataStore.MANAGED_STORE_TARGET));
        }
    }

    @Test
    void testGrantAccess_TemplateOnNFS_SkipsMapping() {
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());

        when(dataStore.getId()).thenReturn(1L);
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getUuid()).thenReturn("template-uuid");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getScope()).thenReturn(ScopeType.CLUSTER);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        assertTrue(driver.grantAccess(templateInfo, host, dataStore));
        verify(vmTemplatePoolDao, never()).update(any(Long.class), any(VMTemplateStoragePoolVO.class));
    }

    @Test
    void testCreateAsync_VolumeClonedFromTemplateNFS_ClonesFile() {
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());
        stubVolumeCloneFromTemplate(5368709120L, 5368709120L);
        when(volumeInfo.getUuid()).thenReturn("volume-uuid");
        when(templatePoolRef.getInstallPath()).thenReturn("template-uuid");

        CloudStackVolume cloned = new CloudStackVolume();

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(nasStrategy);
            when(nasStrategy.cloneCloudStackVolume(any())).thenReturn(cloned);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());

            ArgumentCaptor<CloudStackVolume> requestCaptor = ArgumentCaptor.forClass(CloudStackVolume.class);
            verify(nasStrategy).cloneCloudStackVolume(requestCaptor.capture());
            assertEquals("template-uuid", requestCaptor.getValue().getFile().getPath());
            assertEquals("volume-uuid", requestCaptor.getValue().getDestinationPath());
            verify(nasStrategy, never()).resizeCloudStackVolume(any(), anyLong());
        }
    }

    @Test
    void testDeleteAsync_Template_NFS_DeletesCachedFile() {
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());
        storagePoolDetails.put(OntapStorageConstants.VOLUME_UUID, "flexvol-uuid");

        when(dataStore.getId()).thenReturn(1L);
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getId()).thenReturn(50L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);
        when(templatePoolRef.getInstallPath()).thenReturn("template-uuid");

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails)).thenReturn(nasStrategy);

            driver.deleteAsync(dataStore, templateInfo, commandCallback);

            ArgumentCaptor<CommandResult> resultCaptor = ArgumentCaptor.forClass(CommandResult.class);
            verify(commandCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(nasStrategy).deleteFileByPath("flexvol-uuid", "template-uuid");
        }
    }

    @Test
    void testRevokeAccess_Template_UnmapsCacheLun() {
        when(dataStore.getId()).thenReturn(1L);
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getId()).thenReturn(50L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getName()).thenReturn("vol1");
        when(storagePool.getScope()).thenReturn(ScopeType.CLUSTER);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        when(host.getStorageUrl()).thenReturn("iqn.1993-08.org.debian:01:host1");
        when(host.getName()).thenReturn("host1");
        when(host.getUuid()).thenReturn("host-uuid-1");

        Lun templateLun = new Lun();
        templateLun.setName("/vol/vol1/cs_tmpl_50");
        templateLun.setUuid("template-lun-uuid");
        CloudStackVolume cachedTemplate = new CloudStackVolume();
        cachedTemplate.setLun(templateLun);

        Igroup igroup = mock(Igroup.class);
        when(igroup.getName()).thenReturn("igroup1");
        when(igroup.getUuid()).thenReturn("igroup-uuid-123");
        AccessGroup accessGroup = new AccessGroup();
        accessGroup.setIgroup(igroup);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails)).thenReturn(sanStrategy);
            utilityMock.when(() -> OntapStorageUtils.getIgroupName(anyString(), anyString())).thenReturn("igroup1");

            when(sanStrategy.getCloudStackVolume(argThat(map ->
                    map != null && "/vol/vol1/cs_tmpl_50".equals(map.get("name")))))
                    .thenReturn(cachedTemplate);
            when(sanStrategy.getAccessGroup(any())).thenReturn(accessGroup);
            when(sanStrategy.validateInitiatorInAccessGroup(anyString(), anyString(), any(Igroup.class))).thenReturn(true);

            driver.revokeAccess(templateInfo, host, dataStore);

            verify(sanStrategy).disableLogicalAccess(argThat(map ->
                    map != null && "template-lun-uuid".equals(map.get("lun.uuid"))
                            && "igroup-uuid-123".equals(map.get("igroup.uuid"))));
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
