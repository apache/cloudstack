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
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.model.FileInfo;
import org.apache.cloudstack.storage.feign.model.Igroup;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.feign.model.VolumeSpace;
import org.apache.cloudstack.storage.feign.model.Svm;
import org.apache.cloudstack.storage.feign.model.VolumeQosPolicy;
import org.apache.cloudstack.storage.service.StorageStrategy;
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
import java.util.List;
import java.util.Map;

import static com.cloud.agent.api.to.DataObjectType.SNAPSHOT;
import static com.cloud.agent.api.to.DataObjectType.TEMPLATE;
import static com.cloud.agent.api.to.DataObjectType.VOLUME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private SnapshotDetailsDao snapshotDetailsDao;

    @Mock
    private SnapshotDao snapshotDao;

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
        storagePoolDetails.put(OntapStorageConstants.IS_AFF, "true");
        lenient().when(volumeInfo.getName()).thenReturn("test-volume");
        lenient().when(volumeInfo.getSize()).thenReturn(1073741824L);
        lenient().when(storagePool.getName()).thenReturn("vol1");
        lenient().when(storagePool.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);
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
        assertEquals(Boolean.TRUE.toString(), capabilities.get("CAN_CREATE_TEMPLATE_FROM_SNAPSHOT"));
        assertEquals(Boolean.FALSE.toString(), capabilities.get("CAN_DIRECT_ATTACH_SNAPSHOT"));
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
        lenient().when(storagePool.getId()).thenReturn(1L);
        lenient().when(storagePool.getName()).thenReturn("vol1");

        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.OntapiSCSI);
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
            verify(volumeVO).setFormat(Storage.ImageFormat.RAW);
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
                    .thenReturn(nasStrategy);

            when(nasStrategy.createCloudStackVolume(any())).thenReturn(mockCloudStackVolume);

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
    void testCreateAsync_UnsupportedHypervisor_FailsWithError() {
        // Use NFS so LUN OS-type resolution does not fail earlier in getOSTypeFromHypervisor;
        // the failure under test is image-format resolution for non-KVM hypervisors.
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());

        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getName()).thenReturn("test-volume");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(storagePool.getHypervisor()).thenReturn(Hypervisor.HypervisorType.VMware);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);

        CloudStackVolume mockCloudStackVolume = new CloudStackVolume();

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(nasStrategy);
            when(nasStrategy.createCloudStackVolume(any())).thenReturn(mockCloudStackVolume);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            assertTrue(resultCaptor.getValue().getResult().contains("Unsupported hypervisor [VMware]"));
        }
    }

    @Test
    void testCreateAsync_KvmUnsupportedProtocol_FailsWithError() {
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, "FC");

        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getName()).thenReturn("test-volume");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        driver.createAsync(dataStore, volumeInfo, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
        assertTrue(resultCaptor.getValue().getResult().contains("Unsupported protocol [FC]"));
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
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.QOS_POLICY_UUID)).thenReturn(null);

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
            verify(volumeDao, never()).remove(anyLong());
            verify(volumeDetailsDao, never()).removeDetails(anyLong());
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
    void testDeleteAsync_Template_ResolvesLunByNameWhenUuidMissing() {
        when(dataStore.getId()).thenReturn(1L);
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getId()).thenReturn(50L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getName()).thenReturn("vol1");
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);
        when(templatePoolRef.getLocalDownloadPath()).thenReturn(null);

        Lun templateLun = new Lun();
        templateLun.setName("/vol/vol1/cs_tmpl_50");
        templateLun.setUuid("resolved-lun-uuid");
        CloudStackVolume existing = new CloudStackVolume();
        existing.setLun(templateLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails)).thenReturn(sanStrategy);
            when(sanStrategy.getCloudStackVolume(argThat(map ->
                    map != null && "/vol/vol1/cs_tmpl_50".equals(map.get("name")))))
                    .thenReturn(existing);
            doNothing().when(sanStrategy).deleteCloudStackVolume(any());

            driver.deleteAsync(dataStore, templateInfo, commandCallback);

            ArgumentCaptor<CommandResult> resultCaptor = ArgumentCaptor.forClass(CommandResult.class);
            verify(commandCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());

            ArgumentCaptor<CloudStackVolume> deleteCaptor = ArgumentCaptor.forClass(CloudStackVolume.class);
            verify(sanStrategy).deleteCloudStackVolume(deleteCaptor.capture());
            assertEquals("resolved-lun-uuid", deleteCaptor.getValue().getLun().getUuid());
            assertEquals("/vol/vol1/cs_tmpl_50", deleteCaptor.getValue().getLun().getName());
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
            verify(volumeInfo, never()).getVolume();
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
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);
        when(templatePoolRef.getState()).thenReturn(ObjectInDataStoreStateMachine.State.Ready);
        when(templatePoolRef.getLocalDownloadPath()).thenReturn("template-lun-uuid");

        assertEquals(0L, driver.getBytesRequiredForTemplate(templateInfo, storagePool));
    }

    @Test
    void testGetBytesRequiredForTemplate_NotCached_ReturnsVirtualSize() {
        when(storagePool.getId()).thenReturn(1L);
        when(templateInfo.getId()).thenReturn(50L);
        when(templateInfo.getSize()).thenReturn(5368709120L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(null);

        assertEquals(5368709120L, driver.getBytesRequiredForTemplate(templateInfo, storagePool));
    }

    @Test
    void testGetBytesRequiredForTemplate_SpoolRefNotReady_ReturnsVirtualSize() {
        when(storagePool.getId()).thenReturn(1L);
        when(templateInfo.getId()).thenReturn(50L);
        when(templateInfo.getSize()).thenReturn(5368709120L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);
        when(templatePoolRef.getState()).thenReturn(ObjectInDataStoreStateMachine.State.Allocated);

        assertEquals(5368709120L, driver.getBytesRequiredForTemplate(templateInfo, storagePool));
    }

    @Test
    void testGetBytesRequiredForTemplate_ReadyWithoutBackendIdentity_ReturnsVirtualSize() {
        when(storagePool.getId()).thenReturn(1L);
        when(templateInfo.getId()).thenReturn(50L);
        when(templateInfo.getSize()).thenReturn(5368709120L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);
        when(templatePoolRef.getState()).thenReturn(ObjectInDataStoreStateMachine.State.Ready);
        when(templatePoolRef.getLocalDownloadPath()).thenReturn(null);

        assertEquals(5368709120L, driver.getBytesRequiredForTemplate(templateInfo, storagePool));
    }

    @Test
    void testGetBytesRequiredForTemplate_NfsCached_ReturnsZero() {
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());
        when(storagePool.getId()).thenReturn(1L);
        when(templateInfo.getId()).thenReturn(50L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);
        when(templatePoolRef.getState()).thenReturn(ObjectInDataStoreStateMachine.State.Ready);
        when(templatePoolRef.getInstallPath()).thenReturn("/mnt/pool/template-uuid");

        assertEquals(0L, driver.getBytesRequiredForTemplate(templateInfo, storagePool));
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
            when(sanStrategy.createTemplateCache(any(), any(), any(), anyLong())).thenReturn(created);

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
    void testCreateAsync_TemplateWithISCSI_RollsBackLunWhenDbUpdateFails() {
        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getId()).thenReturn(50L);
        when(templateInfo.getSize()).thenReturn(5368709120L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);
        when(templatePoolRef.getId()).thenReturn(7L);
        doThrow(new RuntimeException("db update failed")).when(vmTemplatePoolDao)
                .update(eq(7L), any(VMTemplateStoragePoolVO.class));

        Lun templateLun = new Lun();
        templateLun.setName("/vol/vol1/cs_tmpl_50");
        templateLun.setUuid("template-lun-uuid");
        CloudStackVolume created = new CloudStackVolume();
        created.setLun(templateLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.createTemplateCache(any(), any(), any(), anyLong())).thenReturn(created);
            doNothing().when(sanStrategy).deleteCloudStackVolume(any());

            driver.createAsync(dataStore, templateInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());

            ArgumentCaptor<CloudStackVolume> deleteCaptor = ArgumentCaptor.forClass(CloudStackVolume.class);
            verify(sanStrategy).deleteCloudStackVolume(deleteCaptor.capture());
            assertEquals("template-lun-uuid", deleteCaptor.getValue().getLun().getUuid());
            assertEquals("/vol/vol1/cs_tmpl_50", deleteCaptor.getValue().getLun().getName());
        }
    }

    @Test
    void testCreateAsync_TemplateWithISCSI_UnknownSize_Fails() {
        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getSize()).thenReturn(0L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.createTemplateCache(any(), any(), any(), anyLong()))
                    .thenThrow(new CloudRuntimeException("Unknown virtual size for template [50]"));

            driver.createAsync(dataStore, templateInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
        }
    }

    @Test
    void testCreateAsync_TemplateWithNFS_IsMetadataOnly() {
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());

        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getUuid()).thenReturn("template-uuid");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(nasStrategy);
            when(nasStrategy.createTemplateCache(any(), any(), any(), anyLong())).thenReturn(null);

            driver.createAsync(dataStore, templateInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(vmTemplatePoolDao, never()).update(any(Long.class), any(VMTemplateStoragePoolVO.class));
        }
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
            assertEquals("/vol/vol1/cs_tmpl_50", requestCaptor.getValue().getLun().getClone().getSource().getName());
            assertEquals("template-lun-uuid", requestCaptor.getValue().getLun().getClone().getSource().getUuid());
            assertSame(volumeInfo, requestCaptor.getValue().getVolumeInfo());
            verify(sanStrategy, never()).createCloudStackVolume(any());
            verify(sanStrategy, never()).resizeCloudStackVolume(any(), anyLong());
            verify(sanStrategy, never()).updateCloudStackVolume(any());
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

    @Test
    void testCreateAsync_VolumeClonedFromSnapshot_IscsiSuccessWithoutGrow() {
        stubVolumeCloneFromSnapshot(5368709120L, 5368709120L, ProtocolType.ISCSI.name());

        Lun clonedLun = new Lun();
        clonedLun.setName("/vol/vol1/test_volume");
        clonedLun.setUuid("snap-cloned-lun-uuid");
        CloudStackVolume cloned = new CloudStackVolume();
        cloned.setLun(clonedLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.cloneCloudStackVolume(any()))
                    .thenReturn(cloned);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());

            ArgumentCaptor<CloudStackVolume> requestCaptor = ArgumentCaptor.forClass(CloudStackVolume.class);
            verify(sanStrategy).cloneCloudStackVolume(requestCaptor.capture());
            Lun lunRequest = requestCaptor.getValue().getLun();
            assertEquals("/vol/vol1/.snapshot/snap_cs200/source_lun", lunRequest.getClone().getSource().getName());
            assertNull(lunRequest.getClone().getSource().getUuid());
            assertEquals("/vol/vol1/test_volume", lunRequest.getName());
            assertEquals("svm1", lunRequest.getSvm().getName());
            assertSame(volumeInfo, requestCaptor.getValue().getVolumeInfo());
            verify(sanStrategy, never()).createCloudStackVolume(any());
            verify(sanStrategy, never()).resizeCloudStackVolume(any(), anyLong());
            verify(volumeDetailsDao).addDetail(eq(100L), eq(OntapStorageConstants.LUN_DOT_UUID), eq("snap-cloned-lun-uuid"), eq(false));
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromSnapshot_GrowsWhenOfferingIsLarger() {
        stubVolumeCloneFromSnapshot(5368709120L, 21474836480L, ProtocolType.ISCSI.name());

        Lun clonedLun = new Lun();
        clonedLun.setName("/vol/vol1/test_volume");
        clonedLun.setUuid("snap-cloned-lun-uuid");
        CloudStackVolume cloned = new CloudStackVolume();
        cloned.setLun(clonedLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.cloneCloudStackVolume(any()))
                    .thenReturn(cloned);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            verify(sanStrategy).resizeCloudStackVolume(eq(cloned), eq(21474836480L));
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromSnapshot_ResizeFails_DeletesOrphanedClone() {
        stubVolumeCloneFromSnapshot(5368709120L, 21474836480L, ProtocolType.ISCSI.name());

        Lun clonedLun = new Lun();
        clonedLun.setName("/vol/vol1/test_volume");
        clonedLun.setUuid("snap-cloned-lun-uuid");
        CloudStackVolume cloned = new CloudStackVolume();
        cloned.setLun(clonedLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.cloneCloudStackVolume(any())).thenReturn(cloned);
            doThrow(new CloudRuntimeException("resize failed"))
                    .when(sanStrategy).resizeCloudStackVolume(eq(cloned), eq(21474836480L));
            doNothing().when(sanStrategy).deleteCloudStackVolume(eq(cloned));

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            verify(sanStrategy).deleteCloudStackVolume(eq(cloned));
            verify(volumeDetailsDao, never()).addDetail(eq(100L), eq(OntapStorageConstants.LUN_DOT_UUID), anyString(), anyBoolean());
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromSnapshot_NfsSuccess() {
        stubVolumeCloneFromSnapshot(5368709120L, 5368709120L, ProtocolType.NFS3.name());
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(volumeInfo.getUuid()).thenReturn("new-volume-uuid");

        CloudStackVolume cloned = new CloudStackVolume();
        FileInfo file = new FileInfo();
        file.setPath("new-volume-uuid");
        cloned.setFile(file);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(nasStrategy);
            when(nasStrategy.cloneCloudStackVolume(any()))
                    .thenReturn(cloned);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());

            ArgumentCaptor<CloudStackVolume> requestCaptor = ArgumentCaptor.forClass(CloudStackVolume.class);
            verify(nasStrategy).cloneCloudStackVolume(requestCaptor.capture());
            CloudStackVolume request = requestCaptor.getValue();
            assertEquals("source-file-uuid", request.getFile().getPath());
            assertEquals("new-volume-uuid", request.getDestinationPath());
            assertEquals("snap_cs200", request.getSnapshotName());
            assertEquals("1", request.getDatastoreId());
            verify(nasStrategy, never()).createCloudStackVolume(any());
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromSnapshot_PoolMismatch_Fails() {
        stubVolumeCloneFromSnapshot(5368709120L, 5368709120L, ProtocolType.ISCSI.name());
        when(snapshotDetailsDao.findDetail(200L, OntapStorageConstants.PRIMARY_POOL_ID))
                .thenReturn(new SnapshotDetailsVO(200L, OntapStorageConstants.PRIMARY_POOL_ID, "999", false));

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            verify(sanStrategy, never()).cloneCloudStackVolume(any());
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromSnapshot_MissingDetail_Fails() {
        stubVolumeCloneFromSnapshot(5368709120L, 5368709120L, ProtocolType.ISCSI.name());
        when(snapshotDetailsDao.findDetail(200L, OntapStorageConstants.ONTAP_SNAP_NAME)).thenReturn(null);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            verify(sanStrategy, never()).cloneCloudStackVolume(any());
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromSnapshot_ProtocolMismatch_Fails() {
        stubVolumeCloneFromSnapshot(5368709120L, 5368709120L, ProtocolType.ISCSI.name());
        // Pool is iSCSI (default stub details) but snapshot was taken on NFS.
        when(snapshotDetailsDao.findDetail(200L, OntapStorageConstants.PROTOCOL))
                .thenReturn(new SnapshotDetailsVO(200L, OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name(), false));

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            verify(sanStrategy, never()).cloneCloudStackVolume(any());
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromSnapshot_NullStrategyResult_Fails() {
        stubVolumeCloneFromSnapshot(5368709120L, 5368709120L, ProtocolType.ISCSI.name());

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.cloneCloudStackVolume(any()))
                    .thenReturn(null);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromSnapshot_StrategyThrows_Fails() {
        stubVolumeCloneFromSnapshot(5368709120L, 5368709120L, ProtocolType.ISCSI.name());

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.cloneCloudStackVolume(any()))
                    .thenThrow(new CloudRuntimeException("ONTAP clone failed"));

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            verify(sanStrategy, never()).resizeCloudStackVolume(any(), anyLong());
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromSnapshot_PrefersSnapshotOverTemplate() {
        // Corner: when both details are present, the snapshot clone wins over the template clone.
        stubVolumeCloneFromSnapshot(5368709120L, 5368709120L, ProtocolType.ISCSI.name());
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.CLONE_OF_TEMPLATE))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.CLONE_OF_TEMPLATE, "50", false));

        Lun clonedLun = new Lun();
        clonedLun.setName("/vol/vol1/test_volume");
        clonedLun.setUuid("snap-cloned-lun-uuid");
        CloudStackVolume cloned = new CloudStackVolume();
        cloned.setLun(clonedLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.cloneCloudStackVolume(any()))
                    .thenReturn(cloned);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CloudStackVolume> requestCaptor = ArgumentCaptor.forClass(CloudStackVolume.class);
            verify(sanStrategy).cloneCloudStackVolume(requestCaptor.capture());
            assertEquals("/vol/vol1/.snapshot/snap_cs200/source_lun",
                    requestCaptor.getValue().getLun().getClone().getSource().getName());
            verify(vmTemplatePoolDao, never()).findByPoolTemplate(anyLong(), anyLong(), any());
            verify(sanStrategy, never()).createCloudStackVolume(any());
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromSnapshot_NfsGrowsWhenOfferingIsLarger() {
        stubVolumeCloneFromSnapshot(5368709120L, 21474836480L, ProtocolType.NFS3.name());
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(volumeInfo.getUuid()).thenReturn("new-volume-uuid");

        CloudStackVolume cloned = new CloudStackVolume();
        FileInfo file = new FileInfo();
        file.setPath("new-volume-uuid");
        cloned.setFile(file);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(nasStrategy);
            when(nasStrategy.cloneCloudStackVolume(any()))
                    .thenReturn(cloned);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            verify(nasStrategy).resizeCloudStackVolume(eq(cloned), eq(21474836480L));
            verify(sanStrategy, never()).cloneCloudStackVolume(any());
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromSnapshot_SkipsGrowWhenSnapshotSizeUnknown() {
        stubVolumeCloneFromSnapshot(0L, 21474836480L, ProtocolType.ISCSI.name());

        Lun clonedLun = new Lun();
        clonedLun.setName("/vol/vol1/test_volume");
        clonedLun.setUuid("snap-cloned-lun-uuid");
        CloudStackVolume cloned = new CloudStackVolume();
        cloned.setLun(clonedLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.cloneCloudStackVolume(any()))
                    .thenReturn(cloned);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            verify(sanStrategy, never()).resizeCloudStackVolume(any(), anyLong());
            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
        }
    }

    /**
     * Sets up a volume create that the orchestrator has marked as a clone of a CloudStack snapshot.
     */
    private void stubVolumeCloneFromSnapshot(long snapshotSize, long volumeSize, String protocol) {
        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getName()).thenReturn("test-volume");
        lenient().when(volumeInfo.getSize()).thenReturn(volumeSize);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        // getId is only used after snapshot_details validation succeeds; lenient for early-fail tests.
        lenient().when(storagePool.getId()).thenReturn(1L);
        lenient().when(storagePool.getName()).thenReturn("vol1");
        lenient().when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.OntapiSCSI);
        lenient().when(storagePool.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        lenient().when(volumeVO.getId()).thenReturn(100L);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.CLONE_OF_SNAPSHOT))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.CLONE_OF_SNAPSHOT, "200", false));

        String volumePath = ProtocolType.NFS3.name().equalsIgnoreCase(protocol)
                ? "source-file-uuid"
                : "/vol/vol1/source_lun";
        if (!ProtocolType.NFS3.name().equalsIgnoreCase(protocol)) {
            storagePoolDetails.put(OntapStorageConstants.VOLUME_NAME, "vol1");
        }
        // Lenient so early-fail tests can override/null individual keys without STRICT_STUBS noise.
        lenient().when(snapshotDetailsDao.findDetail(200L, OntapStorageConstants.ONTAP_SNAP_NAME))
                .thenReturn(new SnapshotDetailsVO(200L, OntapStorageConstants.ONTAP_SNAP_NAME, "snap_cs200", false));
        lenient().when(snapshotDetailsDao.findDetail(200L, OntapStorageConstants.VOLUME_PATH))
                .thenReturn(new SnapshotDetailsVO(200L, OntapStorageConstants.VOLUME_PATH, volumePath, false));
        lenient().when(snapshotDetailsDao.findDetail(200L, OntapStorageConstants.PRIMARY_POOL_ID))
                .thenReturn(new SnapshotDetailsVO(200L, OntapStorageConstants.PRIMARY_POOL_ID, "1", false));
        lenient().when(snapshotDetailsDao.findDetail(200L, OntapStorageConstants.PROTOCOL))
                .thenReturn(new SnapshotDetailsVO(200L, OntapStorageConstants.PROTOCOL, protocol, false));

        SnapshotVO snapshotVO = mock(SnapshotVO.class);
        lenient().when(snapshotDao.findById(200L)).thenReturn(snapshotVO);
        lenient().when(snapshotVO.getSize()).thenReturn(snapshotSize);
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
        lenient().when(volumeInfo.getSize()).thenReturn(volumeSize);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        lenient().when(storagePool.getName()).thenReturn("vol1");
        lenient().when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.OntapiSCSI);
        lenient().when(storagePool.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);
        lenient().when(storagePool.getCapacityIops()).thenReturn(null);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        lenient().when(volumeVO.getId()).thenReturn(100L);
        // createAsync checks cloneOfSnapshot before cloneOfTemplate; under STRICT_STUBS an
        // unstubbed alternate key on the same method is treated as an argument mismatch.
        lenient().when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.CLONE_OF_SNAPSHOT)).thenReturn(null);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.CLONE_OF_TEMPLATE))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.CLONE_OF_TEMPLATE, "50", false));

        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(templatePoolRef);
        lenient().when(templatePoolRef.getLocalDownloadPath()).thenReturn("template-lun-uuid");
        lenient().when(templatePoolRef.getTemplateSize()).thenReturn(templateSize);
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
    void testGrantAccess_Template_UnmapsWhenInstallPathUpdateFails() {
        PrimaryDataStore primaryDataStore = mock(PrimaryDataStore.class);

        when(primaryDataStore.getId()).thenReturn(1L);
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
        doThrow(new RuntimeException("install path update failed")).when(vmTemplatePoolDao)
                .update(eq(7L), any(VMTemplateStoragePoolVO.class));

        when(host.getName()).thenReturn("host1");
        when(host.getUuid()).thenReturn("host-uuid-1");
        when(host.getStorageUrl()).thenReturn("iqn.1993-08.org.debian:01:host1");

        AccessGroup existingAccessGroup = new AccessGroup();
        Igroup existingIgroup = mock(Igroup.class);
        when(existingIgroup.getName()).thenReturn("igroup1");
        when(existingIgroup.getUuid()).thenReturn("igroup-uuid-123");
        existingAccessGroup.setIgroup(existingIgroup);

        Lun templateLun = new Lun();
        templateLun.setName("/vol/vol1/cs_tmpl_50");
        templateLun.setUuid("template-lun-uuid");
        CloudStackVolume cachedTemplate = new CloudStackVolume();
        cachedTemplate.setLun(templateLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails)).thenReturn(sanStrategy);
            utilityMock.when(() -> OntapStorageUtils.getIgroupName(anyString(), anyString())).thenReturn("igroup1");

            when(sanStrategy.getAccessGroup(any())).thenReturn(existingAccessGroup);
            when(sanStrategy.ensureLunMapped(eq("svm1"), eq("/vol/vol1/cs_tmpl_50"), eq("igroup1"))).thenReturn("3");
            when(sanStrategy.getCloudStackVolume(argThat(map ->
                    map != null && "/vol/vol1/cs_tmpl_50".equals(map.get("name")))))
                    .thenReturn(cachedTemplate);
            when(sanStrategy.validateInitiatorInAccessGroup(anyString(), anyString(), any(Igroup.class))).thenReturn(true);
            doNothing().when(sanStrategy).disableLogicalAccess(any());

            assertThrows(CloudRuntimeException.class,
                    () -> driver.grantAccess(templateInfo, host, primaryDataStore));

            verify(sanStrategy).disableLogicalAccess(argThat(map ->
                    map != null && "template-lun-uuid".equals(map.get("lun.uuid"))
                            && "igroup-uuid-123".equals(map.get("igroup.uuid"))));
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
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
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
            verify(nasStrategy, never()).updateCloudStackVolume(any());
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromTemplate_RootWithIops_SetsQosOnLunCreate() {
        stubVolumeCloneFromTemplate(5368709120L, 5368709120L);
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.ROOT);
        when(volumeInfo.getMinIops()).thenReturn(100L);
        when(volumeInfo.getMaxIops()).thenReturn(200L);

        Lun clonedLun = new Lun();
        clonedLun.setName("/vol/vol1/test_volume");
        clonedLun.setUuid("cloned-lun-uuid");
        CloudStackVolume cloned = new CloudStackVolume();
        cloned.setLun(clonedLun);
        VolumeQosPolicy qosPolicy = qosPolicy("qos-root-uuid", "cs_100_to_200_iops_svm1");

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            stubQosCreateMocks(utilityMock, sanStrategy, cloned, qosPolicy);
            when(sanStrategy.cloneCloudStackVolume(any())).thenReturn(cloned);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(sanStrategy).createVolumeQosPolicy(eq("cs_100_to_200_iops_svm1"), eq(100L), eq(200L));
            verify(sanStrategy).cloneCloudStackVolume(argThat(request ->
                    request.getLun() != null && request.getLun().getQosPolicy() != null
                            && "qos-root-uuid".equals(request.getLun().getQosPolicy().getUuid())));
            verify(sanStrategy, never()).updateCloudStackVolume(any());
            verify(volumeDetailsDao).addDetail(eq(100L), eq(OntapStorageConstants.QOS_POLICY_UUID),
                    eq("qos-root-uuid"), eq(false));
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromTemplateNFS_RootWithIops_AttachesQosToFile() {
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());
        storagePoolDetails.put(OntapStorageConstants.VOLUME_UUID, "flex-uuid");
        stubVolumeCloneFromTemplate(5368709120L, 5368709120L);
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(volumeInfo.getUuid()).thenReturn("volume-uuid");
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.ROOT);
        when(volumeInfo.getMinIops()).thenReturn(100L);
        when(volumeInfo.getMaxIops()).thenReturn(200L);
        when(templatePoolRef.getInstallPath()).thenReturn("template-uuid");

        CloudStackVolume cloned = new CloudStackVolume();
        VolumeQosPolicy qosPolicy = lookedUpQosPolicy("qos-nfs-root-uuid", "cs_100_to_200_iops_svm1");

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            stubQosCreateMocks(utilityMock, nasStrategy, cloned, qosPolicy);
            when(nasStrategy.cloneCloudStackVolume(any())).thenReturn(cloned);
            when(nasStrategy.updateCloudStackVolume(any())).thenReturn(cloned);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(nasStrategy).createVolumeQosPolicy(eq("cs_100_to_200_iops_svm1"), eq(100L), eq(200L));
            verify(nasStrategy, never()).resizeCloudStackVolume(any(), anyLong());
            verify(nasStrategy).updateCloudStackVolume(argThat(request ->
                    "1".equals(request.getDatastoreId())
                            && "flex-uuid".equals(request.getFlexVolumeUuid())
                            && request.getVolumeInfo() == volumeInfo
                            && request.getFile() != null
                            && isQosPolicyReference(request.getFile().getQosPolicy(),
                                    "qos-nfs-root-uuid", "cs_100_to_200_iops_svm1")));
            verify(volumeDetailsDao).addDetail(eq(100L), eq(OntapStorageConstants.QOS_POLICY_UUID),
                    eq("qos-nfs-root-uuid"), eq(false));
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
    void testDeleteAsync_Template_NFS_FailsWhenFlexVolUuidMissing() {
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());
        storagePoolDetails.remove(OntapStorageConstants.VOLUME_UUID);

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
            assertFalse(resultCaptor.getValue().isSuccess());
            assertTrue(resultCaptor.getValue().getResult().contains("volumeUUID"));
            verify(nasStrategy, never()).deleteFileByPath(any(), any());
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
    void testGetUsedIops_SumsMinIopsIncludingCreatingVolumes() {
        VolumeVO readyVolume = mock(VolumeVO.class);
        VolumeVO creatingVolume = mock(VolumeVO.class);
        VolumeVO volumeWithoutMinIops = mock(VolumeVO.class);

        when(storagePool.getId()).thenReturn(1L);
        when(readyVolume.getMinIops()).thenReturn(700L);
        when(creatingVolume.getMinIops()).thenReturn(300L);
        when(volumeWithoutMinIops.getMinIops()).thenReturn(null);
        when(volumeDao.findNonDestroyedVolumesByPoolId(1L, null))
                .thenReturn(List.of(readyVolume, creatingVolume, volumeWithoutMinIops));

        assertEquals(1000L, driver.getUsedIops(storagePool));
    }

    @Test
    void testResize_IopsUpdate_AttachesQosPolicy() {
        long currentSize = 4L * 1024 * 1024 * 1024;
        stubResizeVolumeRecord(currentSize);
        when(volumeInfo.getpayload()).thenReturn(
                new ResizeVolumePayload(currentSize, 0L, 5000L, null, false, null, null, true));
        when(storagePool.getCapacityIops()).thenReturn(null);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.QOS_POLICY_UUID)).thenReturn(null);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_UUID))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_UUID, "lun-uuid-123", false));

        VolumeQosPolicy qosPolicy = lookedUpQosPolicy("qos-uuid", "cs_0_to_5000_iops_svm1");

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            stubQosCreateMocks(utilityMock, sanStrategy, iscsiCloudStackVolume(), qosPolicy);

            driver.resize(volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(sanStrategy).resizeCloudStackVolume(argThat(request ->
                    request.getLun() != null
                            && "lun-uuid-123".equals(request.getLun().getUuid())
                            && isQosPolicyReference(request.getLun().getQosPolicy(), "qos-uuid", "cs_0_to_5000_iops_svm1")),
                    eq(currentSize));
            verify(sanStrategy, never()).updateCloudStackVolume(any());
            verify(volumeDetailsDao).addDetail(100L, OntapStorageConstants.QOS_POLICY_UUID, "qos-uuid", false);
        }
    }

    @Test
    void testResize_IopsUpdate_NfsAttachesQosPolicy() {
        long currentSize = 4L * 1024 * 1024 * 1024;
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());
        storagePoolDetails.put(OntapStorageConstants.VOLUME_UUID, "flex-uuid");
        stubResizeVolumeRecord(currentSize);
        when(volumeInfo.getpayload()).thenReturn(
                new ResizeVolumePayload(currentSize, 100L, 200L, null, false, null, null, true));
        when(storagePool.getCapacityIops()).thenReturn(null);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.QOS_POLICY_UUID)).thenReturn(null);

        VolumeQosPolicy qosPolicy = lookedUpQosPolicy("qos-uuid", "cs_100_to_200_iops_svm1");

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            stubQosCreateMocks(utilityMock, nasStrategy, nfsCloudStackVolume(), qosPolicy);

            driver.resize(volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(nasStrategy).resizeCloudStackVolume(argThat(request ->
                    "flex-uuid".equals(request.getFlexVolumeUuid())
                            && request.getFile() != null
                            && isQosPolicyReference(request.getFile().getQosPolicy(), "qos-uuid", "cs_100_to_200_iops_svm1")),
                    eq(currentSize));
            verify(nasStrategy, never()).updateCloudStackVolume(any());
            verify(volumeDetailsDao).addDetail(100L, OntapStorageConstants.QOS_POLICY_UUID, "qos-uuid", false);
        }
    }

    @Test
    void testResize_SameQosPolicy_SkipsAttach() {
        long currentSize = 4L * 1024 * 1024 * 1024;
        stubResizeVolumeRecord(currentSize);
        when(volumeInfo.getpayload()).thenReturn(
                new ResizeVolumePayload(currentSize, 0L, 3333L, null, false, null, null, true));
        when(storagePool.getCapacityIops()).thenReturn(null);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.QOS_POLICY_UUID))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.QOS_POLICY_UUID, "qos-uuid", false));

        VolumeQosPolicy qosPolicy = qosPolicy("qos-uuid", "cs_0_to_3333_iops_svm1");

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            stubQosCreateMocks(utilityMock, sanStrategy, iscsiCloudStackVolume(), qosPolicy);

            driver.resize(volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(sanStrategy).resizeCloudStackVolume(argThat(request -> request.getLun() == null), eq(currentSize));
            verify(sanStrategy, never()).updateCloudStackVolume(any());
        }
    }

    @Test
    void testResize_ClearIops_DetachesQosPolicy() {
        long currentSize = 4L * 1024 * 1024 * 1024;
        stubResizeVolumeRecord(currentSize);
        when(volumeInfo.getpayload()).thenReturn(
                new ResizeVolumePayload(currentSize, 0L, 0L, null, false, null, null, true));
        when(storagePool.getCapacityIops()).thenReturn(null);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.QOS_POLICY_UUID))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.QOS_POLICY_UUID, "qos-uuid", false));
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_UUID))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_UUID, "lun-uuid-123", false));

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any()))
                    .thenReturn(sanStrategy);

            driver.resize(volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(sanStrategy).resizeCloudStackVolume(argThat(request ->
                    request.getLun() != null
                            && isQosPolicyReference(request.getLun().getQosPolicy(), null, OntapStorageConstants.QOS_POLICY_NONE)),
                    eq(currentSize));
            verify(sanStrategy, never()).updateCloudStackVolume(any());
            verify(sanStrategy).deleteVolumeQosPolicy("qos-uuid");
        }
    }

    @Test
    void testResize_SwitchIops_OldPolicyDeleteFails_StillSucceeds() {
        long currentSize = 4L * 1024 * 1024 * 1024;
        stubResizeVolumeRecord(currentSize);
        when(volumeInfo.getpayload()).thenReturn(
                new ResizeVolumePayload(currentSize, 0L, 5000L, null, false, null, null, true));
        when(storagePool.getCapacityIops()).thenReturn(null);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.QOS_POLICY_UUID))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.QOS_POLICY_UUID, "old-qos-uuid", false));
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_UUID))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_UUID, "lun-uuid-123", false));

        VolumeQosPolicy qosPolicy = lookedUpQosPolicy("new-qos-uuid", "cs_0_to_5000_iops_svm1");

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            stubQosCreateMocks(utilityMock, sanStrategy, iscsiCloudStackVolume(), qosPolicy);
            doThrow(new CloudRuntimeException("ONTAP QoS delete failed")).when(sanStrategy)
                    .deleteVolumeQosPolicy("old-qos-uuid");

            driver.resize(volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(sanStrategy).resizeCloudStackVolume(argThat(request ->
                    request.getLun() != null
                            && isQosPolicyReference(request.getLun().getQosPolicy(), "new-qos-uuid", "cs_0_to_5000_iops_svm1")),
                    eq(currentSize));
            verify(volumeDetailsDao).addDetail(100L, OntapStorageConstants.QOS_POLICY_UUID, "new-qos-uuid", false);
            verify(sanStrategy).deleteVolumeQosPolicy("old-qos-uuid");
            verify(sanStrategy, never()).deleteVolumeQosPolicy("new-qos-uuid");
        }
    }

    @Test
    void testResize_ClearIops_OldPolicyDeleteFails_StillSucceeds() {
        long currentSize = 4L * 1024 * 1024 * 1024;
        stubResizeVolumeRecord(currentSize);
        when(volumeInfo.getpayload()).thenReturn(
                new ResizeVolumePayload(currentSize, 0L, 0L, null, false, null, null, true));
        when(storagePool.getCapacityIops()).thenReturn(null);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.QOS_POLICY_UUID))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.QOS_POLICY_UUID, "qos-uuid", false));
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_UUID))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_UUID, "lun-uuid-123", false));

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any()))
                    .thenReturn(sanStrategy);
            doThrow(new CloudRuntimeException("ONTAP QoS delete failed")).when(sanStrategy)
                    .deleteVolumeQosPolicy("qos-uuid");

            driver.resize(volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(sanStrategy).deleteVolumeQosPolicy("qos-uuid");
        }
    }

    @Test
    void testResize_MinIopsBeyondPoolCapacity_Fails() {
        long currentSize = 4L * 1024 * 1024 * 1024;
        VolumeVO otherVolume = mock(VolumeVO.class);
        when(otherVolume.getId()).thenReturn(50L);
        when(otherVolume.getMinIops()).thenReturn(800L);

        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getDataStore()).thenReturn(dataStore);
        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getpayload()).thenReturn(
                new ResizeVolumePayload(currentSize, 300L, 1000L, null, false, null, null, true));
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(100L);
        when(volumeVO.getSize()).thenReturn(currentSize);
        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getName()).thenReturn("ontap-pool");
        when(storagePool.getCapacityIops()).thenReturn(1000L);
        when(volumeDao.findNonDestroyedVolumesByPoolId(1L, null)).thenReturn(List.of(otherVolume, volumeVO));

        driver.resize(volumeInfo, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
        assertTrue(resultCaptor.getValue().getResult().contains(
                "requested total of 1100 IOPS exceeds the pool IOPS capacity of 1000"));
        verify(sanStrategy, never()).resizeCloudStackVolume(any(), anyLong());
        verify(sanStrategy, never()).updateCloudStackVolume(any());
    }

    @Test
    void testCreateAsync_MinIopsBeyondPoolCapacity_FailsWithCapacityDetails() {
        VolumeVO allocatedVolume = mock(VolumeVO.class);
        when(allocatedVolume.getId()).thenReturn(50L);
        when(allocatedVolume.getMinIops()).thenReturn(800L);

        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getName()).thenReturn("test-volume");
        when(volumeInfo.getMinIops()).thenReturn(300L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getName()).thenReturn("ontap-pool");
        when(storagePool.getCapacityIops()).thenReturn(1000L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeDao.findNonDestroyedVolumesByPoolId(1L, null)).thenReturn(List.of(allocatedVolume));

        driver.createAsync(dataStore, volumeInfo, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
        assertTrue(resultCaptor.getValue().getResult().contains(
                "storage pool ontap-pool: requested total of 1100 IOPS exceeds the pool IOPS capacity of 1000"));
        verify(sanStrategy, never()).createCloudStackVolume(any());
    }

    @Test
    void testCreateAsync_OtherCreatingVolumeCountsAgainstCapacity() {
        VolumeVO readyVolume = mock(VolumeVO.class);
        when(readyVolume.getId()).thenReturn(50L);
        when(readyVolume.getMinIops()).thenReturn(500L);

        VolumeVO otherCreatingVolume = mock(VolumeVO.class);
        when(otherCreatingVolume.getId()).thenReturn(60L);
        when(otherCreatingVolume.getMinIops()).thenReturn(300L);

        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getName()).thenReturn("test-volume");
        when(volumeInfo.getMinIops()).thenReturn(300L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getName()).thenReturn("ontap-pool");
        when(storagePool.getCapacityIops()).thenReturn(1000L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeDao.findNonDestroyedVolumesByPoolId(1L, null))
                .thenReturn(List.of(readyVolume, otherCreatingVolume));

        driver.createAsync(dataStore, volumeInfo, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
        assertTrue(resultCaptor.getValue().getResult().contains(
                "storage pool ontap-pool: requested total of 1100 IOPS exceeds the pool IOPS capacity of 1000"));
        verify(sanStrategy, never()).createCloudStackVolume(any());
    }

    @Test
    void testCreateAsync_DoesNotDoubleCountSelfWhenAlreadyCreating() {
        VolumeVO readyVolume = mock(VolumeVO.class);
        when(readyVolume.getId()).thenReturn(50L);
        when(readyVolume.getMinIops()).thenReturn(500L);

        VolumeVO selfCreatingVolume = mock(VolumeVO.class);
        when(selfCreatingVolume.getId()).thenReturn(100L);

        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getName()).thenReturn("test-volume");
        when(volumeInfo.getMinIops()).thenReturn(300L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        lenient().when(storagePool.getName()).thenReturn("vol1");
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.OntapiSCSI);
        when(storagePool.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(storagePool.getCapacityIops()).thenReturn(1000L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(100L);
        when(volumeDao.findNonDestroyedVolumesByPoolId(1L, null))
                .thenReturn(List.of(readyVolume, selfCreatingVolume));

        Lun mockLun = new Lun();
        mockLun.setName("/vol/vol1/lun1");
        mockLun.setUuid("lun-uuid-123");
        CloudStackVolume cloudStackVolume = new CloudStackVolume();
        cloudStackVolume.setLun(mockLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any()))
                    .thenReturn(sanStrategy);
            when(sanStrategy.createCloudStackVolume(any())).thenReturn(cloudStackVolume);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(sanStrategy).createCloudStackVolume(any());
        }
    }

    @Test
    void testCreateAsync_DataDiskFixedIops_CreatesAndPersistsQosPolicy() {
        stubIscsiVolumeCreate();
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        when(volumeInfo.getMinIops()).thenReturn(100L);
        when(volumeInfo.getMaxIops()).thenReturn(200L);

        VolumeQosPolicy qosPolicy = qosPolicy("qos-uuid", "cs_100_to_200_iops_svm1");
        CloudStackVolume cloudStackVolume = iscsiCloudStackVolume();

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            stubQosCreateMocks(utilityMock, sanStrategy, cloudStackVolume, qosPolicy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(sanStrategy).createVolumeQosPolicy(eq("cs_100_to_200_iops_svm1"), eq(100L), eq(200L));
            verify(sanStrategy).createCloudStackVolume(argThat(request ->
                    request.getLun() != null && request.getLun().getQosPolicy() != null
                            && "qos-uuid".equals(request.getLun().getQosPolicy().getUuid())));
            verify(volumeDetailsDao).addDetail(eq(100L), eq(OntapStorageConstants.QOS_POLICY_UUID),
                    eq("qos-uuid"), eq(false));
            verify(sanStrategy, never()).isAff();
        }
    }

    @Test
    void testCreateAsync_FasPoolWithMinIops_FailsWithoutCallingOntap() {
        stubIscsiVolumeCreate();
        storagePoolDetails.put(OntapStorageConstants.IS_AFF, "false");
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        when(volumeInfo.getMinIops()).thenReturn(100L);
        when(volumeInfo.getMaxIops()).thenReturn(200L);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any()))
                    .thenReturn(sanStrategy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            assertTrue(resultCaptor.getValue().getResult().contains(OntapStorageConstants.MIN_IOPS_NOT_SUPPORTED_ON_FAS));
            verify(sanStrategy, never()).isAff();
            verify(sanStrategy, never()).createVolumeQosPolicy(any(), any(), any());
        }
    }

    @Test
    void testCreateAsync_FasPoolWithMaxIopsOnly_CreatesQosPolicyWithoutNodeCall() {
        stubIscsiVolumeCreate();
        storagePoolDetails.put(OntapStorageConstants.IS_AFF, "false");
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        when(volumeInfo.getMinIops()).thenReturn(0L);
        when(volumeInfo.getMaxIops()).thenReturn(200L);

        VolumeQosPolicy qosPolicy = qosPolicy("qos-uuid", "cs_0_to_200_iops_svm1");
        CloudStackVolume cloudStackVolume = iscsiCloudStackVolume();

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            stubQosCreateMocks(utilityMock, sanStrategy, cloudStackVolume, qosPolicy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(sanStrategy, never()).isAff();
            verify(sanStrategy).createVolumeQosPolicy(eq("cs_0_to_200_iops_svm1"), eq(0L), eq(200L));
        }
    }

    @Test
    void testCreateAsync_MissingAffDetail_FetchesFromNodesAndPersists() {
        stubIscsiVolumeCreate();
        storagePoolDetails.remove(OntapStorageConstants.IS_AFF);
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        when(volumeInfo.getMinIops()).thenReturn(100L);
        when(volumeInfo.getMaxIops()).thenReturn(200L);
        when(sanStrategy.isAff()).thenReturn(true);

        VolumeQosPolicy qosPolicy = qosPolicy("qos-uuid", "cs_100_to_200_iops_svm1");
        CloudStackVolume cloudStackVolume = iscsiCloudStackVolume();

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            stubQosCreateMocks(utilityMock, sanStrategy, cloudStackVolume, qosPolicy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(sanStrategy).isAff();
            verify(storagePoolDetailsDao).addDetail(1L, OntapStorageConstants.IS_AFF, "true", false);
            verify(sanStrategy).createVolumeQosPolicy(eq("cs_100_to_200_iops_svm1"), eq(100L), eq(200L));
        }
    }

    @Test
    void testCreateAsync_MissingAffDetailOnFas_PersistsFalseAndRejectsMinIops() {
        stubIscsiVolumeCreate();
        storagePoolDetails.remove(OntapStorageConstants.IS_AFF);
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        when(volumeInfo.getMinIops()).thenReturn(100L);
        when(volumeInfo.getMaxIops()).thenReturn(200L);
        when(sanStrategy.isAff()).thenReturn(false);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any()))
                    .thenReturn(sanStrategy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            assertTrue(resultCaptor.getValue().getResult().contains(OntapStorageConstants.MIN_IOPS_NOT_SUPPORTED_ON_FAS));
            verify(sanStrategy).isAff();
            verify(storagePoolDetailsDao).addDetail(1L, OntapStorageConstants.IS_AFF, "false", false);
            verify(sanStrategy, never()).createVolumeQosPolicy(any(), any(), any());
        }
    }

    @Test
    void testCreateAsync_MissingAffDetail_IsAffThrows_StillCreatesWithMinIops() {
        stubIscsiVolumeCreate();
        storagePoolDetails.remove(OntapStorageConstants.IS_AFF);
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        when(volumeInfo.getMinIops()).thenReturn(100L);
        when(volumeInfo.getMaxIops()).thenReturn(200L);
        when(sanStrategy.isAff()).thenThrow(new CloudRuntimeException(
                "Unable to determine whether the ONTAP cluster is AFF or FAS"));

        VolumeQosPolicy qosPolicy = qosPolicy("qos-uuid", "cs_100_to_200_iops_svm1");
        CloudStackVolume cloudStackVolume = iscsiCloudStackVolume();

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            stubQosCreateMocks(utilityMock, sanStrategy, cloudStackVolume, qosPolicy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(sanStrategy).isAff();
            verify(storagePoolDetailsDao, never()).addDetail(eq(1L), eq(OntapStorageConstants.IS_AFF),
                    anyString(), eq(false));
            verify(sanStrategy).createVolumeQosPolicy(eq("cs_100_to_200_iops_svm1"), eq(100L), eq(200L));
            verify(sanStrategy).createCloudStackVolume(any());
        }
    }

    @Test
    void testCreateAsync_RootDiskCustomIops_CreatesAndPersistsQosPolicy() {
        stubIscsiVolumeCreate();
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.ROOT);
        when(volumeInfo.getMinIops()).thenReturn(111L);
        when(volumeInfo.getMaxIops()).thenReturn(999L);

        VolumeQosPolicy qosPolicy = qosPolicy("qos-root-uuid", "cs_111_to_999_iops_svm1");
        CloudStackVolume cloudStackVolume = iscsiCloudStackVolume();

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            stubQosCreateMocks(utilityMock, sanStrategy, cloudStackVolume, qosPolicy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(sanStrategy).createVolumeQosPolicy(eq("cs_111_to_999_iops_svm1"), eq(111L), eq(999L));
            verify(volumeDetailsDao).addDetail(eq(100L), eq(OntapStorageConstants.QOS_POLICY_UUID),
                    eq("qos-root-uuid"), eq(false));
        }
    }

    @Test
    void testCreateAsync_NfsDataDiskWithIops_CreatesQosPolicy() {
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());
        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getName()).thenReturn("test-volume");
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        when(volumeInfo.getMinIops()).thenReturn(100L);
        when(volumeInfo.getMaxIops()).thenReturn(200L);
        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(storagePool.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);
        lenient().when(storagePool.getCapacityIops()).thenReturn(null);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(100L);

        VolumeQosPolicy qosPolicy = qosPolicy("qos-nfs-uuid", "cs_100_to_200_iops_svm1");
        CloudStackVolume cloudStackVolume = new CloudStackVolume();

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            stubQosCreateMocks(utilityMock, nasStrategy, cloudStackVolume, qosPolicy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(nasStrategy).createVolumeQosPolicy(eq("cs_100_to_200_iops_svm1"), eq(100L), eq(200L));
            verify(nasStrategy).createCloudStackVolume(argThat(request ->
                    request.getFile() != null && request.getFile().getQosPolicy() != null
                            && "qos-nfs-uuid".equals(request.getFile().getQosPolicy().getUuid())));
        }
    }

    @Test
    void testCreateAsync_MinIopsGreaterThanMaxIops_Fails() {
        stubIscsiVolumeCreate();
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        when(volumeInfo.getMinIops()).thenReturn(200L);
        when(volumeInfo.getMaxIops()).thenReturn(100L);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any()))
                    .thenReturn(sanStrategy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            assertTrue(resultCaptor.getValue().getResult().contains(
                    "Minimum IOPS cannot be greater than maximum IOPS"));
            verify(sanStrategy, never()).createVolumeQosPolicy(any(), any(), any());
            verify(sanStrategy, never()).createCloudStackVolume(any());
        }
    }

    @Test
    void testCreateAsync_NoIops_DoesNotCreateQosPolicy() {
        stubIscsiVolumeCreate();
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        CloudStackVolume cloudStackVolume = iscsiCloudStackVolume();

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any()))
                    .thenReturn(sanStrategy);
            when(sanStrategy.createCloudStackVolume(any())).thenReturn(cloudStackVolume);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            verify(sanStrategy, never()).createVolumeQosPolicy(any(), any(), any());
            verify(sanStrategy).createCloudStackVolume(argThat(request ->
                    request.getLun() != null && request.getLun().getQosPolicy() == null));
        }
    }

    @Test
    void testCreateAsync_SwapVolumeWithIops_DoesNotCreateQosPolicy() {
        stubIscsiVolumeCreate();
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.SWAP);
        when(volumeInfo.getMinIops()).thenReturn(100L);
        CloudStackVolume cloudStackVolume = iscsiCloudStackVolume();

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any()))
                    .thenReturn(sanStrategy);
            when(sanStrategy.createCloudStackVolume(any())).thenReturn(cloudStackVolume);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            verify(sanStrategy, never()).createVolumeQosPolicy(any(), any(), any());
        }
    }

    @Test
    void testCreateAsync_QosCreateThenLunCreateFails_DeletesUnusedPolicy() {
        stubIscsiVolumeCreate();
        when(volumeInfo.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        when(volumeInfo.getMinIops()).thenReturn(100L);
        when(volumeInfo.getMaxIops()).thenReturn(200L);

        VolumeQosPolicy qosPolicy = qosPolicy("qos-uuid", "cs_100_to_200_iops_svm1");

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any()))
                    .thenReturn(sanStrategy);
            when(sanStrategy.createVolumeQosPolicy(nullable(String.class), nullable(Long.class), nullable(Long.class)))
                    .thenReturn(qosPolicy);
            when(sanStrategy.createCloudStackVolume(any())).thenThrow(new CloudRuntimeException(
                    "Failed to create Lun: {\"error\":{\"code\":\"8454269\"}}"));

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            verify(sanStrategy).deleteVolumeQosPolicy("qos-uuid");
        }
    }

    @Test
    void testDeleteAsync_DeletesUnusedQosPolicy() {
        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        VolumeDetailVO lunNameDetail = new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_NAME, "/vol/vol1/lun1", false);
        VolumeDetailVO lunUuidDetail = new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_UUID, "lun-uuid-123", false);
        VolumeDetailVO qosDetail = new VolumeDetailVO(100L, OntapStorageConstants.QOS_POLICY_UUID, "qos-uuid", false);

        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_NAME)).thenReturn(lunNameDetail);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_UUID)).thenReturn(lunUuidDetail);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.QOS_POLICY_UUID)).thenReturn(qosDetail);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            doNothing().when(sanStrategy).deleteCloudStackVolume(any());

            driver.deleteAsync(dataStore, volumeInfo, commandCallback);

            ArgumentCaptor<CommandResult> resultCaptor = ArgumentCaptor.forClass(CommandResult.class);
            verify(commandCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(volumeDetailsDao, never()).removeDetail(100L, OntapStorageConstants.QOS_POLICY_UUID);
            verify(sanStrategy).deleteVolumeQosPolicy("qos-uuid");
        }
    }

    @Test
    void testDeleteAsync_QosPolicyDeleteFails_VolumeDeleteStillSucceeds() {
        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        VolumeDetailVO lunNameDetail = new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_NAME, "/vol/vol1/lun1", false);
        VolumeDetailVO lunUuidDetail = new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_UUID, "lun-uuid-123", false);
        VolumeDetailVO qosDetail = new VolumeDetailVO(100L, OntapStorageConstants.QOS_POLICY_UUID, "qos-uuid", false);

        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_NAME)).thenReturn(lunNameDetail);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_UUID)).thenReturn(lunUuidDetail);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.QOS_POLICY_UUID)).thenReturn(qosDetail);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            doNothing().when(sanStrategy).deleteCloudStackVolume(any());
            doThrow(new CloudRuntimeException("ONTAP QoS delete failed")).when(sanStrategy)
                    .deleteVolumeQosPolicy("qos-uuid");

            driver.deleteAsync(dataStore, volumeInfo, commandCallback);

            ArgumentCaptor<CommandResult> resultCaptor = ArgumentCaptor.forClass(CommandResult.class);
            verify(commandCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(volumeDetailsDao, never()).removeDetail(100L, OntapStorageConstants.QOS_POLICY_UUID);
            verify(sanStrategy).deleteVolumeQosPolicy("qos-uuid");
        }
    }

    private void stubIscsiVolumeCreate() {
        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getName()).thenReturn("test-volume");
        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        lenient().when(storagePool.getId()).thenReturn(1L);
        lenient().when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.OntapiSCSI);
        lenient().when(storagePool.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);
        lenient().when(storagePool.getCapacityIops()).thenReturn(null);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        lenient().when(volumeVO.getId()).thenReturn(100L);
    }

    private CloudStackVolume iscsiCloudStackVolume() {
        Lun mockLun = new Lun();
        mockLun.setName("/vol/vol1/lun1");
        mockLun.setUuid("lun-uuid-123");
        CloudStackVolume volume = new CloudStackVolume();
        volume.setLun(mockLun);
        return volume;
    }

    private CloudStackVolume nfsCloudStackVolume() {
        FileInfo fileInfo = new FileInfo();
        CloudStackVolume volume = new CloudStackVolume();
        volume.setFlexVolumeUuid("flex-uuid");
        volume.setFile(fileInfo);
        volume.setVolumeInfo(volumeInfo);
        return volume;
    }

    private VolumeQosPolicy qosPolicy(String uuid, String name) {
        VolumeQosPolicy policy = new VolumeQosPolicy();
        policy.setUuid(uuid);
        policy.setName(name);
        return policy;
    }

    /**
     * Policy as returned by an ONTAP lookup: name and uuid plus fields that must not be sent on a resize.
     */
    private VolumeQosPolicy lookedUpQosPolicy(String uuid, String name) {
        VolumeQosPolicy policy = qosPolicy(uuid, name);
        Svm svm = new Svm();
        svm.setName("svm1");
        policy.setSvm(svm);
        VolumeQosPolicy.Fixed fixed = new VolumeQosPolicy.Fixed();
        fixed.setMinThroughputIops(100L);
        fixed.setMaxThroughputIops(5000L);
        policy.setFixed(fixed);
        policy.setObjectCount(2);
        return policy;
    }

    private boolean isQosPolicyReference(VolumeQosPolicy sent, String uuid, String name) {
        return sent != null
                && name.equals(sent.getName())
                && (uuid == null ? sent.getUuid() == null : uuid.equals(sent.getUuid()))
                && sent.getSvm() == null
                && sent.getFixed() == null
                && sent.getObjectCount() == null;
    }

    private void stubQosCreateMocks(MockedStatic<OntapStorageUtils> utilityMock,
                                    StorageStrategy strategy,
                                    CloudStackVolume cloudStackVolume, VolumeQosPolicy qosPolicy) {
        utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any()))
                .thenReturn(strategy);
        when(strategy.createVolumeQosPolicy(nullable(String.class), nullable(Long.class), nullable(Long.class)))
                .thenReturn(qosPolicy);
        lenient().when(strategy.createCloudStackVolume(any())).thenReturn(cloudStackVolume);
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

    @Test
    void testGetBytesRequiredForTemplate_NullArgs_ReturnsZero() {
        assertEquals(0L, driver.getBytesRequiredForTemplate(null, storagePool));
        assertEquals(0L, driver.getBytesRequiredForTemplate(templateInfo, null));
    }

    @Test
    void testCreateAsync_Template_StrategyThrows_Fails() {
        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getId()).thenReturn(50L);
        when(templateInfo.getSize()).thenReturn(5368709120L);
        when(templateInfo.getName()).thenReturn("tmpl");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.createTemplateCache(any(), any(), any(), anyLong()))
                    .thenThrow(new RuntimeException("unexpected"));

            driver.createAsync(dataStore, templateInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromTemplate_NullCloneResult_Fails() {
        stubVolumeCloneFromTemplate(5368709120L, 5368709120L);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.cloneCloudStackVolume(any())).thenReturn(null);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromTemplateNFS_MissingInstallPath_Fails() {
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());
        stubVolumeCloneFromTemplate(5368709120L, 5368709120L);
        when(templatePoolRef.getInstallPath()).thenReturn(null);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(nasStrategy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            verify(nasStrategy, never()).cloneCloudStackVolume(any());
        }
    }

    @Test
    void testDeleteAsync_Template_NoSpoolRef_Succeeds() {
        when(dataStore.getId()).thenReturn(1L);
        when(templateInfo.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getId()).thenReturn(50L);

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(null);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);

            driver.deleteAsync(dataStore, templateInfo, commandCallback);

            ArgumentCaptor<CommandResult> resultCaptor = ArgumentCaptor.forClass(CommandResult.class);
            verify(commandCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
            verify(sanStrategy, never()).deleteCloudStackVolume(any());
        }
    }

    @Test
    void testGrantAccess_Template_CreatesIgroupWhenMissing() {
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

        HostVO hostVo = mock(HostVO.class);
        when(hostVo.getName()).thenReturn("host1");
        when(hostVo.getUuid()).thenReturn("host-uuid-1");

        AccessGroup createdAccessGroup = new AccessGroup();
        Igroup createdIgroup = new Igroup();
        createdIgroup.setName("igroup1");
        createdAccessGroup.setIgroup(createdIgroup);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails)).thenReturn(sanStrategy);
            utilityMock.when(() -> OntapStorageUtils.getIgroupName(anyString(), anyString())).thenReturn("igroup1");

            when(sanStrategy.getAccessGroup(any())).thenReturn(null);
            when(sanStrategy.createAccessGroup(any())).thenReturn(createdAccessGroup);
            when(sanStrategy.ensureLunMapped(eq("svm1"), eq("/vol/vol1/cs_tmpl_50"), eq("igroup1"))).thenReturn("3");

            assertTrue(driver.grantAccess(templateInfo, hostVo, primaryDataStore));

            verify(sanStrategy).createAccessGroup(any());
            verify(templatePoolRef).setInstallPath("/iqn.1992-08.com.netapp:sn.123456/3");
        }
    }

    @Test
    void testCreateAsync_VolumeClonedFromTemplate_MissingSpoolRef_Fails() {
        when(dataStore.getId()).thenReturn(1L);
        when(dataStore.getName()).thenReturn("ontap-pool");
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getName()).thenReturn("test-volume");

        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        lenient().when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.CLONE_OF_SNAPSHOT)).thenReturn(null);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.CLONE_OF_TEMPLATE))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.CLONE_OF_TEMPLATE, "50", false));
        when(vmTemplatePoolDao.findByPoolTemplate(1L, 50L, null)).thenReturn(null);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);

            driver.createAsync(dataStore, volumeInfo, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            verify(sanStrategy, never()).cloneCloudStackVolume(any());
        }
    }

    // =========================================================================
    // resize() tests
    // =========================================================================

    private void stubResizeVolumeRecord(long currentSize) {
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getDataStore()).thenReturn(dataStore);
        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(100L);
        when(volumeVO.getSize()).thenReturn(currentSize);
        lenient().when(volumeVO.getPath()).thenReturn("/vol/vol1/lun1");
        when(volumeDao.update(100L, volumeVO)).thenReturn(true);
        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
    }

    private DataObject stubResizeCommon(long currentSize, long newSize) {
        ResizeVolumePayload payload = new ResizeVolumePayload(newSize, null, null, null, false, "i-2-VM", null, false);
        DataObject data = volumeInfo;
        when(data.getType()).thenReturn(VOLUME);
        when(volumeInfo.getDataStore()).thenReturn(dataStore);
        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getpayload()).thenReturn(payload);
        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        // storagePool.getName() is only needed for the shrink-error message path
        lenient().when(storagePool.getName()).thenReturn("test-pool");
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(volumeVO);
        when(volumeVO.getSize()).thenReturn(currentSize);
        // volumeVO.getId() is only needed in the success path (volumeDao.update call)
        lenient().when(volumeVO.getId()).thenReturn(100L);
        lenient().when(volumeVO.getPath()).thenReturn("/vol/vol1/lun1");
        lenient().when(volumeDao.update(100L, volumeVO)).thenReturn(true);
        return data;
    }

    @Test
    void testResize_iSCSI_Success() {
        long currentSize = 10737418240L; // 10 GB
        long newSize = 21474836480L;     // 20 GB
        DataObject data = stubResizeCommon(currentSize, newSize);
        assertEquals(VOLUME, data.getType());

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            doNothing().when(sanStrategy).resizeCloudStackVolume(any(), eq(newSize));

            driver.resize(data, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());

            // Driver only wires VolumeInfo; LUN UUID lookup is inside UnifiedSANStrategy
            ArgumentCaptor<CloudStackVolume> volumeCaptor = ArgumentCaptor.forClass(CloudStackVolume.class);
            verify(sanStrategy).resizeCloudStackVolume(volumeCaptor.capture(), eq(newSize));
            assertEquals(volumeInfo, volumeCaptor.getValue().getVolumeInfo());
            assertNull(volumeCaptor.getValue().getLun());
            verify(volumeDetailsDao, never()).findDetail(anyLong(), eq(OntapStorageConstants.LUN_DOT_UUID));

            verify(volumeVO).setSize(newSize);
            verify(volumeDao).update(eq(100L), any(VolumeVO.class));
        }
    }

    @Test
    void testResize_NFS_Success() {
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, ProtocolType.NFS3.name());
        long currentSize = 10737418240L;
        long newSize = 21474836480L;
        DataObject data = stubResizeCommon(currentSize, newSize);
        assertEquals(VOLUME, data.getType());

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(nasStrategy);
            doNothing().when(nasStrategy).resizeCloudStackVolume(any(), eq(newSize));

            driver.resize(data, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());

            ArgumentCaptor<CloudStackVolume> volumeCaptor = ArgumentCaptor.forClass(CloudStackVolume.class);
            verify(nasStrategy).resizeCloudStackVolume(volumeCaptor.capture(), eq(newSize));
            assertEquals(volumeInfo, volumeCaptor.getValue().getVolumeInfo());
            // NFS has no LUN – driver must not look up a LUN UUID
            verify(volumeDetailsDao, never()).findDetail(anyLong(), eq(OntapStorageConstants.LUN_DOT_UUID));
            verify(volumeVO).setSize(newSize);
            verify(volumeDao).update(eq(100L), any(VolumeVO.class));
        }
    }

    @Test
    void testResize_NullPayload_Fails() {
        DataObject data = volumeInfo;
        when(data.getType()).thenReturn(VOLUME);
        assertEquals(VOLUME, data.getType());
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getpayload()).thenReturn(null);

        driver.resize(data, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
        assertTrue(resultCaptor.getValue().getResult().contains("Invalid resize payload"));
    }

    @Test
    void testResize_NullData_FailsAndCompletesCallbackOnce() {
        driver.resize(null, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback, times(1)).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
        assertTrue(resultCaptor.getValue().getResult().contains("Expected a VOLUME DataObject"));
    }

    @Test
    void testResize_NonVolumeDataType_FailsAndCompletesCallbackOnce() {
        DataObject data = templateInfo;
        when(data.getType()).thenReturn(TEMPLATE);
        when(templateInfo.getId()).thenReturn(50L);

        driver.resize(data, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback, times(1)).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
        assertTrue(resultCaptor.getValue().getResult().contains("Expected a VOLUME DataObject but received TEMPLATE"));
    }

    @Test
    void testResize_SnapshotDataType_FailsAndCompletesCallbackOnce() {
        DataObject data = mock(DataObject.class);
        when(data.getType()).thenReturn(SNAPSHOT);
        when(data.getId()).thenReturn(75L);

        driver.resize(data, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback, times(1)).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
        assertTrue(resultCaptor.getValue().getResult().contains("Expected a VOLUME DataObject but received SNAPSHOT"));
    }

    @Test
    void testResize_VolumeTypeWithoutVolumeDetails_FailsWithoutInternalTypeName() {
        DataObject data = mock(DataObject.class);
        when(data.getType()).thenReturn(VOLUME);

        driver.resize(data, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback, times(1)).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
        String message = resultCaptor.getValue().getResult();
        assertTrue(message.contains("Invalid DataObjectType (VOLUME) passed to resize"));
        assertFalse(message.contains("VolumeInfo"));
    }

    @Test
    void testResize_NullNewSize_Fails() {
        // payload.newSize is null
        ResizeVolumePayload payload = new ResizeVolumePayload(null, null, null, null, false, "i-2-VM", null, false);
        DataObject data = volumeInfo;
        when(data.getType()).thenReturn(VOLUME);
        assertEquals(VOLUME, data.getType());
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getpayload()).thenReturn(payload);

        driver.resize(data, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
        assertTrue(resultCaptor.getValue().getResult().contains("Invalid resize payload"));
    }

    @Test
    void testResize_StoragePoolNotFound_Fails() {
        ResizeVolumePayload payload = new ResizeVolumePayload(21474836480L, null, null, null, false, "i-2-VM", null, false);
        DataObject data = volumeInfo;
        when(data.getType()).thenReturn(VOLUME);
        assertEquals(VOLUME, data.getType());
        when(volumeInfo.getDataStore()).thenReturn(dataStore);
        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getpayload()).thenReturn(payload);
        when(storagePoolDao.findById(1L)).thenReturn(null);

        driver.resize(data, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
        assertTrue(resultCaptor.getValue().getResult().contains("Storage pool not found"));
    }

    @Test
    void testResize_MissingDataStore_Fails() {
        ResizeVolumePayload payload = new ResizeVolumePayload(21474836480L, null, null, null, false, "none", null, false);
        DataObject data = volumeInfo;
        when(data.getType()).thenReturn(VOLUME);
        assertEquals(VOLUME, data.getType());
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getpayload()).thenReturn(payload);
        when(volumeInfo.getDataStore()).thenReturn(null);

        driver.resize(data, createCallback);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(createCallback).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
        assertTrue(resultCaptor.getValue().getResult().contains("Data store not found"));
    }

    @Test
    void testResize_VolumeVONotFound_Fails() {
        long newSize = 21474836480L;
        ResizeVolumePayload payload = new ResizeVolumePayload(newSize, null, null, null, false, "i-2-VM", null, false);
        DataObject data = volumeInfo;
        when(data.getType()).thenReturn(VOLUME);
        assertEquals(VOLUME, data.getType());
        when(volumeInfo.getDataStore()).thenReturn(dataStore);
        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getpayload()).thenReturn(payload);
        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(null);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);

            driver.resize(data, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            assertTrue(resultCaptor.getValue().getResult().contains("Volume not found"));
        }
    }

    @Test
    void testResize_MissingStoragePoolDetails_Fails() {
        long currentSize = 10737418240L;
        long newSize = 21474836480L;
        DataObject data = stubResizeCommon(currentSize, newSize);
        assertEquals(VOLUME, data.getType());
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(null);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(null))
                    .thenThrow(new CloudRuntimeException("Storage pool details are missing"));

            driver.resize(data, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback, times(1)).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            assertTrue(resultCaptor.getValue().getResult().contains("Storage pool details are missing"));
            verify(volumeVO, never()).setSize(anyLong());
        }
    }

    @Test
    void testResize_ShrinkAttempt_Fails() {
        long currentSize = 21474836480L; // 20 GB
        long newSize = 10737418240L;     // 10 GB – smaller than current
        DataObject data = stubResizeCommon(currentSize, newSize);
        assertEquals(VOLUME, data.getType());

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);

            driver.resize(data, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            assertTrue(resultCaptor.getValue().getResult().contains("Unable to shrink volume"));
        }
    }

    private static VolumeVO temporarySnapshotCopyVolume() {
        return new VolumeVO(Volume.Type.DATADISK, "ROOT-5_20260924.TMP", 1L, 1L, 2L, 0L,
                Storage.ProvisioningType.THIN, 5368709120L, 0L, 0L, "");
    }

    private SnapshotInfo stubSnapshotToVolumeCopy(VolumeVO destVolume) {
        SnapshotInfo snapshotInfo = mock(SnapshotInfo.class);
        lenient().when(snapshotInfo.getType()).thenReturn(SNAPSHOT);
        lenient().when(snapshotInfo.getDataStore()).thenReturn(dataStore);
        lenient().when(snapshotInfo.getId()).thenReturn(200L);
        lenient().when(volumeInfo.getType()).thenReturn(VOLUME);
        lenient().when(volumeInfo.getDataStore()).thenReturn(dataStore);
        lenient().when(volumeInfo.getId()).thenReturn(100L);
        // VolumeObject.getName() reads the VolumeVO, which copyAsync renames before cloning.
        lenient().when(volumeInfo.getName()).thenReturn("cs_tmp_snap_200_100");
        lenient().when(dataStore.getId()).thenReturn(1L);
        lenient().when(dataStore.getRole()).thenReturn(DataStoreRole.Primary);
        lenient().when(volumeDao.findById(100L)).thenReturn(destVolume);
        return snapshotInfo;
    }

    private void stubSnapshotCloneSource(String protocol) {
        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        lenient().when(storagePool.getId()).thenReturn(1L);
        lenient().when(storagePool.getName()).thenReturn("vol1");
        lenient().when(storagePool.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);
        storagePoolDetails.put(OntapStorageConstants.PROTOCOL, protocol);
        storagePoolDetails.put(OntapStorageConstants.VOLUME_NAME, "vol1");
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        lenient().when(volumeInfo.getSize()).thenReturn(5368709120L);

        String volumePath = ProtocolType.NFS3.name().equals(protocol) ? "source-file-uuid" : "/vol/vol1/source_lun";
        lenient().when(snapshotDetailsDao.findDetail(200L, OntapStorageConstants.ONTAP_SNAP_NAME))
                .thenReturn(new SnapshotDetailsVO(200L, OntapStorageConstants.ONTAP_SNAP_NAME, "snap_cs200", false));
        lenient().when(snapshotDetailsDao.findDetail(200L, OntapStorageConstants.VOLUME_PATH))
                .thenReturn(new SnapshotDetailsVO(200L, OntapStorageConstants.VOLUME_PATH, volumePath, false));
        lenient().when(snapshotDetailsDao.findDetail(200L, OntapStorageConstants.PRIMARY_POOL_ID))
                .thenReturn(new SnapshotDetailsVO(200L, OntapStorageConstants.PRIMARY_POOL_ID, "1", false));
        lenient().when(snapshotDetailsDao.findDetail(200L, OntapStorageConstants.PROTOCOL))
                .thenReturn(new SnapshotDetailsVO(200L, OntapStorageConstants.PROTOCOL, protocol, false));
        SnapshotVO snapshotVO = mock(SnapshotVO.class);
        lenient().when(snapshotDao.findById(200L)).thenReturn(snapshotVO);
        lenient().when(snapshotVO.getSize()).thenReturn(5368709120L);
    }

    @Test
    void testCanCopy_TemporarySnapshotCopyOnSamePool_ReturnsTrue() {
        SnapshotInfo snapshotInfo = stubSnapshotToVolumeCopy(temporarySnapshotCopyVolume());

        assertTrue(driver.canCopy(snapshotInfo, volumeInfo));
    }

    @Test
    void testCanCopy_CreateVolumeFromSnapshot_ReturnsFalse() {
        VolumeVO userVolume = temporarySnapshotCopyVolume();
        userVolume.setDiskOfferingId(5L);
        userVolume.setState(Volume.State.Creating);
        SnapshotInfo snapshotInfo = stubSnapshotToVolumeCopy(userVolume);

        assertFalse(driver.canCopy(snapshotInfo, volumeInfo));
    }

    @Test
    void testCanCopy_AllocatedVolumeWithDiskOffering_ReturnsFalse() {
        VolumeVO userVolume = temporarySnapshotCopyVolume();
        userVolume.setDiskOfferingId(5L);
        SnapshotInfo snapshotInfo = stubSnapshotToVolumeCopy(userVolume);

        assertFalse(driver.canCopy(snapshotInfo, volumeInfo));
    }

    @Test
    void testCanCopy_DifferentPool_ReturnsFalse() {
        SnapshotInfo snapshotInfo = stubSnapshotToVolumeCopy(temporarySnapshotCopyVolume());
        DataStore otherStore = mock(DataStore.class);
        when(otherStore.getId()).thenReturn(2L);
        when(volumeInfo.getDataStore()).thenReturn(otherStore);

        assertFalse(driver.canCopy(snapshotInfo, volumeInfo));
        verify(volumeDao, never()).findById(anyLong());
    }

    @Test
    void testCanCopy_TemplateCacheCopies_ReturnFalse() {
        TemplateInfo templateOnPrimary = mock(TemplateInfo.class);
        lenient().when(templateInfo.getType()).thenReturn(TEMPLATE);
        lenient().when(templateInfo.getDataStore()).thenReturn(dataStore);
        lenient().when(templateOnPrimary.getType()).thenReturn(TEMPLATE);
        lenient().when(templateOnPrimary.getDataStore()).thenReturn(dataStore);
        lenient().when(volumeInfo.getType()).thenReturn(VOLUME);
        lenient().when(volumeInfo.getDataStore()).thenReturn(dataStore);

        assertFalse(driver.canCopy(templateInfo, templateOnPrimary));
        assertFalse(driver.canCopy(templateOnPrimary, volumeInfo));
        verify(volumeDao, never()).findById(anyLong());
    }

    @Test
    void testCanCopy_VolumeMigration_ReturnsFalse() {
        VolumeInfo destVolumeInfo = mock(VolumeInfo.class);
        DataStore destStore = mock(DataStore.class);
        lenient().when(volumeInfo.getType()).thenReturn(VOLUME);
        lenient().when(volumeInfo.getDataStore()).thenReturn(dataStore);
        lenient().when(destVolumeInfo.getType()).thenReturn(VOLUME);
        lenient().when(destVolumeInfo.getDataStore()).thenReturn(destStore);

        assertFalse(driver.canCopy(volumeInfo, destVolumeInfo));
        verify(volumeDao, never()).findById(anyLong());
    }

    @Test
    void testCopyAsync_VolumeMigration_Throws() {
        VolumeInfo destVolumeInfo = mock(VolumeInfo.class);
        DataStore destStore = mock(DataStore.class);
        lenient().when(volumeInfo.getType()).thenReturn(VOLUME);
        lenient().when(volumeInfo.getDataStore()).thenReturn(dataStore);
        lenient().when(destVolumeInfo.getType()).thenReturn(VOLUME);
        lenient().when(destVolumeInfo.getDataStore()).thenReturn(destStore);

        assertThrows(UnsupportedOperationException.class,
                () -> driver.copyAsync(volumeInfo, destVolumeInfo, null, null));
        verify(sanStrategy, never()).cloneCloudStackVolume(any());
        verify(nasStrategy, never()).cloneCloudStackVolume(any());
    }

    @Test
    void testCopyAsync_UnsupportedPair_Throws() {
        lenient().when(templateInfo.getType()).thenReturn(TEMPLATE);
        lenient().when(templateInfo.getDataStore()).thenReturn(dataStore);
        lenient().when(volumeInfo.getType()).thenReturn(VOLUME);
        lenient().when(volumeInfo.getDataStore()).thenReturn(dataStore);

        assertThrows(UnsupportedOperationException.class, () -> driver.copyAsync(templateInfo, volumeInfo, null, null));
    }

    @Test
    void testCopyAsync_IscsiTemporarySnapshotCopy_ClonesLunAndRecordsIdentity() {
        VolumeVO dbVolume = temporarySnapshotCopyVolume();
        VolumeVO inMemoryVolume = temporarySnapshotCopyVolume();
        SnapshotInfo snapshotInfo = stubSnapshotToVolumeCopy(dbVolume);
        when(volumeInfo.getVolume()).thenReturn(inMemoryVolume);
        stubSnapshotCloneSource(ProtocolType.ISCSI.name());
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.OntapiSCSI);

        Lun clonedLun = new Lun();
        clonedLun.setName("/vol/vol1/cs_tmp_snap_200_100");
        clonedLun.setUuid("tmp-lun-uuid");
        CloudStackVolume cloned = new CloudStackVolume();
        cloned.setLun(clonedLun);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.cloneCloudStackVolume(any())).thenReturn(cloned);

            driver.copyAsync(snapshotInfo, volumeInfo, null, null);

            ArgumentCaptor<CloudStackVolume> requestCaptor = ArgumentCaptor.forClass(CloudStackVolume.class);
            verify(sanStrategy).cloneCloudStackVolume(requestCaptor.capture());
            assertEquals("/vol/vol1/.snapshot/snap_cs200/source_lun",
                    requestCaptor.getValue().getLun().getClone().getSource().getName());
            assertEquals("/vol/vol1/cs_tmp_snap_200_100", requestCaptor.getValue().getLun().getName());
            assertSame(volumeInfo, requestCaptor.getValue().getVolumeInfo());

            assertEquals("cs_tmp_snap_200_100", dbVolume.getName());
            assertEquals("cs_tmp_snap_200_100", inMemoryVolume.getName());
            assertEquals(Storage.ImageFormat.RAW, dbVolume.getFormat());
            assertEquals("tmp-lun-uuid", dbVolume.getFolder());
            verify(volumeDetailsDao).addDetail(eq(100L), eq(OntapStorageConstants.LUN_DOT_UUID), eq("tmp-lun-uuid"), eq(false));
            verify(volumeDetailsDao).addDetail(eq(100L), eq(OntapStorageConstants.LUN_DOT_NAME), eq("/vol/vol1/cs_tmp_snap_200_100"), eq(false));
            verify(volumeDao).update(anyLong(), eq(dbVolume));
            verify(sanStrategy, never()).resizeCloudStackVolume(any(), anyLong());
        }
    }

    @Test
    void testResize_StrategyThrows_Fails() {
        long currentSize = 10737418240L;
        long newSize = 21474836480L;
        DataObject data = stubResizeCommon(currentSize, newSize);
        assertEquals(VOLUME, data.getType());

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            doThrow(new com.cloud.utils.exception.CloudRuntimeException("ONTAP resize failed"))
                    .when(sanStrategy).resizeCloudStackVolume(any(), eq(newSize));

            driver.resize(data, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            assertTrue(resultCaptor.getValue().getResult().contains("ONTAP resize failed"));
            // volumeVO size must NOT be updated on failure
            verify(volumeVO, never()).setSize(anyLong());
        }
    }

    @Test
    void testCopyAsync_NfsTemporarySnapshotCopy_SetsFilePath() {
        VolumeVO dbVolume = temporarySnapshotCopyVolume();
        SnapshotInfo snapshotInfo = stubSnapshotToVolumeCopy(dbVolume);
        when(volumeInfo.getVolume()).thenReturn(temporarySnapshotCopyVolume());
        when(volumeInfo.getUuid()).thenReturn("tmp-volume-uuid");
        stubSnapshotCloneSource(ProtocolType.NFS3.name());
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);

        CloudStackVolume cloned = new CloudStackVolume();
        FileInfo file = new FileInfo();
        file.setPath("tmp-volume-uuid");
        cloned.setFile(file);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(nasStrategy);
            when(nasStrategy.cloneCloudStackVolume(any())).thenReturn(cloned);

            driver.copyAsync(snapshotInfo, volumeInfo, null, null);

            ArgumentCaptor<CloudStackVolume> requestCaptor = ArgumentCaptor.forClass(CloudStackVolume.class);
            verify(nasStrategy).cloneCloudStackVolume(requestCaptor.capture());
            assertEquals("source-file-uuid", requestCaptor.getValue().getFile().getPath());
            assertEquals("tmp-volume-uuid", requestCaptor.getValue().getDestinationPath());
            assertEquals("snap_cs200", requestCaptor.getValue().getSnapshotName());

            assertEquals("tmp-volume-uuid", dbVolume.getPath());
            assertEquals(Storage.ImageFormat.QCOW2, dbVolume.getFormat());
            verify(volumeDao).update(anyLong(), eq(dbVolume));
        }
    }

    @Test
    void testResize_EqualSize_InvokesStrategyAndSucceeds() {
        long size = 10737418240L;
        DataObject data = stubResizeCommon(size, size);
        assertEquals(VOLUME, data.getType());

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);

            driver.resize(data, createCallback);

            verify(sanStrategy).resizeCloudStackVolume(any(), eq(size));
            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback).complete(resultCaptor.capture());
            assertTrue(resultCaptor.getValue().isSuccess());
        }
    }

    @Test
    void testCopyAsync_CloneFailsWithoutCallback_Throws() {
        SnapshotInfo snapshotInfo = stubSnapshotToVolumeCopy(temporarySnapshotCopyVolume());
        when(volumeInfo.getVolume()).thenReturn(temporarySnapshotCopyVolume());
        stubSnapshotCloneSource(ProtocolType.ISCSI.name());

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.cloneCloudStackVolume(any()))
                    .thenThrow(new CloudRuntimeException("clone failed"));

            CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                    () -> driver.copyAsync(snapshotInfo, volumeInfo, null, null));
            assertTrue(ex.getMessage().contains("clone failed"));
            verify(volumeDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), anyBoolean());
        }
    }

    @Test
    void testResize_DatabaseUpdateFailure_ReturnsFailure() {
        long currentSize = 10737418240L;
        long newSize = 21474836480L;
        DataObject data = stubResizeCommon(currentSize, newSize);
        assertEquals(VOLUME, data.getType());
        when(volumeDao.update(100L, volumeVO)).thenReturn(false);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);

            driver.resize(data, createCallback);

            ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
            verify(createCallback, times(1)).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
            assertTrue(resultCaptor.getValue().getResult().contains("Failed to update volume"));
        }
    }

    // =========================================================================
    // getUsedBytes() tests
    // =========================================================================

    @Test
    void testGetUsedBytes_NullPool_ThrowsException() {
        InvalidParameterValueException ex = assertThrows(InvalidParameterValueException.class,
                () -> driver.getUsedBytes(null));
        assertTrue(ex.getMessage().contains("storagePool is null, ensure the pool exists and is fully initialised before querying used bytes"));
    }

    @Test
    void testGetUsedBytes_NoFlexVolUuid_ThrowsException() {
        // VOLUME_UUID key is absent from pool details
        storagePoolDetails.remove(OntapStorageConstants.VOLUME_UUID);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class, () -> driver.getUsedBytes(storagePool));
        assertTrue(ex.getMessage().contains("FlexVolume UUID not found in pool details for pool 1"));
    }

    @Test
    void testGetUsedBytes_BlankFlexVolUuid_ThrowsException() {
        storagePoolDetails.put(OntapStorageConstants.VOLUME_UUID, "   ");
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class, () -> driver.getUsedBytes(storagePool));
        assertTrue(ex.getMessage().contains("FlexVolume UUID not found in pool details for pool 1"));
    }

    @Test
    void testGetUsedBytes_NullPoolDetails_ThrowsException() {
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(null);

        CloudRuntimeException ex = assertThrows(CloudRuntimeException.class, () -> driver.getUsedBytes(storagePool));
        assertTrue(ex.getMessage().contains("FlexVolume UUID not found in pool details for pool 1"));
    }

    @Test
    void testGetUsedBytes_Success_ReturnsUsedBytes() {
        storagePoolDetails.put(OntapStorageConstants.VOLUME_UUID, "flexvol-uuid-123");
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        var flexVol = new org.apache.cloudstack.storage.feign.model.Volume();
        VolumeSpace space = new VolumeSpace();
        space.setUsed(10737418240L); // 10 GB
        flexVol.setSpace(space);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            Map<String, Object> queryParams = Map.of(OntapStorageConstants.FIELDS, OntapStorageConstants.SPACE_USED);
            when(sanStrategy.getStorageVolume(eq("flexvol-uuid-123"), eq(queryParams))).thenReturn(flexVol);

            assertEquals(10737418240L, driver.getUsedBytes(storagePool));
            verify(sanStrategy).getStorageVolume(eq("flexvol-uuid-123"), eq(queryParams));
        }
    }

    @Test
    void testGetUsedBytes_FlexVolNotFound_ThrowsException() {
        storagePoolDetails.put(OntapStorageConstants.VOLUME_UUID, "flexvol-uuid-123");
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            when(sanStrategy.getStorageVolume(eq("flexvol-uuid-123"), any())).thenReturn(null);

            CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                    () -> driver.getUsedBytes(storagePool));
            assertTrue(ex.getMessage().contains("was not found on ONTAP"));
        }
    }

    @Test
    void testCopyAsync_CloneFailsWithCallback_CompletesWithFailure() {
        SnapshotInfo snapshotInfo = stubSnapshotToVolumeCopy(temporarySnapshotCopyVolume());
        when(volumeInfo.getVolume()).thenReturn(temporarySnapshotCopyVolume());
        stubSnapshotCloneSource(ProtocolType.ISCSI.name());
        @SuppressWarnings("unchecked")
        AsyncCompletionCallback<CopyCommandResult> copyCallback = mock(AsyncCompletionCallback.class);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(any())).thenReturn(sanStrategy);
            when(sanStrategy.cloneCloudStackVolume(any()))
                    .thenThrow(new CloudRuntimeException("clone failed"));

            driver.copyAsync(snapshotInfo, volumeInfo, null, copyCallback);

            ArgumentCaptor<CopyCommandResult> resultCaptor = ArgumentCaptor.forClass(CopyCommandResult.class);
            verify(copyCallback).complete(resultCaptor.capture());
            assertFalse(resultCaptor.getValue().isSuccess());
        }
    }

    @Test
    void testDeleteAsync_TemporarySnapshotCopy_NullCallbackRemovesRecord() {
        VolumeVO tempVolume = temporarySnapshotCopyVolume();
        tempVolume.setName("cs_tmp_snap_200_100");
        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeDao.findById(100L)).thenReturn(tempVolume);
        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_NAME))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_NAME, "/vol/vol1/cs_tmp_snap_200_100", false));
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_UUID))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_UUID, "tmp-lun-uuid", false));
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.QOS_POLICY_UUID)).thenReturn(null);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails)).thenReturn(sanStrategy);

            driver.deleteAsync(dataStore, volumeInfo, null);

            verify(sanStrategy).deleteCloudStackVolume(any(CloudStackVolume.class));
            verify(volumeDetailsDao).removeDetails(100L);
            verify(volumeDao).remove(100L);
        }
    }

    @Test
    void testGetUsedBytes_FlexVolNullSpace_ThrowsException() {
        storagePoolDetails.put(OntapStorageConstants.VOLUME_UUID, "flexvol-uuid-123");
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        var flexVol = new org.apache.cloudstack.storage.feign.model.Volume();
        // space is intentionally left null

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            when(sanStrategy.getStorageVolume(eq("flexvol-uuid-123"), any())).thenReturn(flexVol);

            CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                    () -> driver.getUsedBytes(storagePool));
            assertTrue(ex.getMessage().contains("no space information"));
        }
    }

    @Test
    void testGetUsedBytes_OntapException_PropagatesException() {
        storagePoolDetails.put(OntapStorageConstants.VOLUME_UUID, "flexvol-uuid-123");
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            when(sanStrategy.getStorageVolume(eq("flexvol-uuid-123"), any()))
                    .thenThrow(new com.cloud.utils.exception.CloudRuntimeException("ONTAP unreachable"));

            CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                    () -> driver.getUsedBytes(storagePool));
            assertEquals("ONTAP unreachable", ex.getMessage());
        }
    }

    @Test
    void testGetUsedBytes_UnexpectedException_IsWrapped() {
        storagePoolDetails.put(OntapStorageConstants.VOLUME_UUID, "flexvol-uuid-123");
        when(storagePool.getId()).thenReturn(1L);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails))
                    .thenReturn(sanStrategy);
            when(sanStrategy.getStorageVolume(eq("flexvol-uuid-123"), any()))
                    .thenThrow(new IllegalStateException("invalid ONTAP response"));

            CloudRuntimeException ex = assertThrows(CloudRuntimeException.class,
                    () -> driver.getUsedBytes(storagePool));
            assertTrue(ex.getMessage().contains("Could not read used space"));
            assertTrue(ex.getMessage().contains("invalid ONTAP response"));
            assertTrue(ex.getCause() instanceof IllegalStateException);
        }
    }

    @Test
    void testGrantAccess_TemporarySnapshotCopy_SyncsIscsiPathOnInMemoryVolume() {
        String iscsiPath = "/iqn.1992-08.com.netapp:sn.123456/0";
        VolumeVO dbVolume = mock(VolumeVO.class);
        when(dbVolume.getId()).thenReturn(100L);
        when(dbVolume.getState()).thenReturn(Volume.State.Allocated);
        when(dbVolume.getDiskOfferingId()).thenReturn(0L);
        when(dbVolume.getPath()).thenReturn(iscsiPath);
        when(dbVolume.get_iScsiName()).thenReturn(iscsiPath);
        VolumeVO inMemoryVolume = temporarySnapshotCopyVolume();

        when(dataStore.getId()).thenReturn(1L);
        when(volumeInfo.getType()).thenReturn(VOLUME);
        when(volumeInfo.getId()).thenReturn(100L);
        when(volumeInfo.getVolume()).thenReturn(inMemoryVolume);
        when(storagePoolDao.findById(1L)).thenReturn(storagePool);
        when(storagePool.getId()).thenReturn(1L);
        when(storagePool.getScope()).thenReturn(ScopeType.CLUSTER);
        when(storagePool.getPath()).thenReturn("iqn.1992-08.com.netapp:sn.123456");
        when(storagePool.getPoolType()).thenReturn(Storage.StoragePoolType.OntapiSCSI);
        when(storagePoolDetailsDao.listDetailsKeyPairs(1L)).thenReturn(storagePoolDetails);
        when(volumeDao.findById(100L)).thenReturn(dbVolume);
        when(host.getUuid()).thenReturn("host-uuid-1");
        when(volumeDetailsDao.findDetail(100L, OntapStorageConstants.LUN_DOT_NAME))
                .thenReturn(new VolumeDetailVO(100L, OntapStorageConstants.LUN_DOT_NAME, "/vol/vol1/cs_tmp_snap_200_100", false));

        AccessGroup existingAccessGroup = new AccessGroup();
        Igroup existingIgroup = new Igroup();
        existingIgroup.setName("igroup1");
        existingAccessGroup.setIgroup(existingIgroup);

        try (MockedStatic<OntapStorageUtils> utilityMock = mockStatic(OntapStorageUtils.class, CALLS_REAL_METHODS)) {
            utilityMock.when(() -> OntapStorageUtils.getStrategyByStoragePoolDetails(storagePoolDetails)).thenReturn(sanStrategy);
            utilityMock.when(() -> OntapStorageUtils.getIgroupName(anyString(), anyString())).thenReturn("igroup1");
            when(sanStrategy.getAccessGroup(any())).thenReturn(existingAccessGroup);
            when(sanStrategy.ensureLunMapped(anyString(), anyString(), anyString())).thenReturn("0");

            assertTrue(driver.grantAccess(volumeInfo, host, dataStore));

            assertEquals(iscsiPath, inMemoryVolume.getPath());
            assertEquals(iscsiPath, inMemoryVolume.get_iScsiName());
        }
    }
}
