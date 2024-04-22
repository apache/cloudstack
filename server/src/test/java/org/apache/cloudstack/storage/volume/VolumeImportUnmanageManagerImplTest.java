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
package org.apache.cloudstack.storage.volume;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.GetVolumesOnStorageAnswer;
import com.cloud.agent.api.GetVolumesOnStorageCommand;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.admin.volume.ImportVolumeCmd;
import org.apache.cloudstack.api.command.admin.volume.ListVolumesForImportCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VolumeForImportResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.apache.cloudstack.storage.volume.VolumeImportUnmanageManagerImpl.DEFAULT_DISK_OFFERING_UNIQUE_NAME;
import static org.apache.cloudstack.storage.volume.VolumeImportUnmanageManagerImpl.DISK_OFFERING_UNIQUE_NAME_SUFFIX_LOCAL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VolumeImportUnmanageManagerImplTest {

    @Spy
    @InjectMocks
    VolumeImportUnmanageManagerImpl volumeImportUnmanageManager;

    @Mock
    private AccountManager accountMgr;
    @Mock
    private AgentManager agentManager;
    @Mock
    private HostDao hostDao;
    @Mock
    private DiskOfferingDao diskOfferingDao;
    @Mock
    private ResourceLimitService resourceLimitService;
    @Mock
    private ResponseGenerator responseGenerator;
    @Mock
    private VolumeDao volumeDao;
    @Mock
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Mock
    private StoragePoolHostDao storagePoolHostDao;
    @Mock
    private ConfigurationManager configMgr;
    @Mock
    private DataCenterDao dcDao;
    @Mock
    private VolumeOrchestrationService volumeManager;
    @Mock
    private VMTemplatePoolDao templatePoolDao;
    @Mock
    private VolumeApiService volumeApiService;
    @Mock
    private SnapshotDataStoreDao snapshotDataStoreDao;

    @Mock
    StoragePoolVO storagePoolVO;
    @Mock
    VolumeVO volumeVO;
    @Mock
    DiskProfile diskProfile;
    @Mock
    HostVO hostVO;
    @Mock
    StoragePoolHostVO storagePoolHostVO;
    @Mock
    DiskOfferingVO diskOfferingVO;
    @Mock
    DataCenterVO dataCenterVO;

    final static long accountId = 10L;
    final static long zoneId = 11L;
    final static long clusterId = 11L;
    final static long hostId = 13L;
    final static long poolId = 100L;
    final static boolean isLocal = true;
    final static long volumeId = 101L;
    final static String volumeName = "import volume";
    final static long diskOfferingId = 120L;
    final static String localPath = "/mnt/localPath";

    private static String path = "path";
    private static String name = "name";
    private static String fullPath = "fullPath";
    private static String format = "qcow2";
    private static long size = 100000L;
    private static long virtualSize = 20000000L;
    private static String encryptFormat = "LUKS";
    private static Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
    private static String BACKING_FILE = "backing file";
    private static String BACKING_FILE_FORMAT = "qcow2";
    private static String storagePoolUuid = "pool-uuid";
    private static String storagePoolName = "pool-name";
    private static Storage.StoragePoolType storagePoolType = Storage.StoragePoolType.NetworkFilesystem;

    AccountVO account;

    @Before
    public void setUp() {
        CallContext.unregister();
        account = new AccountVO("admin", 1L, "", Account.Type.ADMIN, "uuid");
        account.setId(accountId);
        UserVO user = new UserVO(1, "admin", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        when(accountMgr.finalizeOwner(any(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(account);

        when(primaryDataStoreDao.findById(poolId)).thenReturn(storagePoolVO);
        when(storagePoolVO.getId()).thenReturn(poolId);
        when(storagePoolVO.getDataCenterId()).thenReturn(zoneId);
        when(storagePoolVO.isLocal()).thenReturn(isLocal);
        when(storagePoolVO.getHypervisor()).thenReturn(hypervisorType);
        when(storagePoolVO.getUuid()).thenReturn(storagePoolUuid);
        when(storagePoolVO.getName()).thenReturn(storagePoolName);
        when(storagePoolVO.getPoolType()).thenReturn(storagePoolType);
        when(storagePoolVO.getStatus()).thenReturn(StoragePoolStatus.Up);

        when(volumeDao.findById(volumeId)).thenReturn(volumeVO);
        when(volumeVO.getId()).thenReturn(volumeId);
        when(volumeVO.getAccountId()).thenReturn(accountId);
        when(volumeVO.getSize()).thenReturn(virtualSize);
        when(volumeVO.getDataCenterId()).thenReturn(zoneId);
        when(volumeVO.getName()).thenReturn(volumeName);

        when(hostVO.getHypervisorType()).thenReturn(hypervisorType);
        when(hostVO.getId()).thenReturn(hostId);
        when(hostDao.findById(hostId)).thenReturn(hostVO);

        when(storagePoolHostVO.getLocalPath()).thenReturn(localPath);
        when(storagePoolHostDao.findByPoolHost(poolId, hostId)).thenReturn(storagePoolHostVO);
        when(storagePoolHostVO.getHostId()).thenReturn(hostId);

        when(dcDao.findById(zoneId)).thenReturn(dataCenterVO);
    }

    @Test
    public void testListVolumesForImport() {
        ListVolumesForImportCmd cmd = mock(ListVolumesForImportCmd.class);
        when(cmd.getPath()).thenReturn(path);
        when(cmd.getStorageId()).thenReturn(poolId);

        when(volumeDao.findByPoolIdAndPath(poolId, path)).thenReturn(null);
        when(templatePoolDao.findByPoolPath(poolId, path)).thenReturn(null);
        when(snapshotDataStoreDao.listByStoreAndInstallPaths(eq(poolId), eq(DataStoreRole.Primary), any())).thenReturn(null);

        VolumeOnStorageTO volumeOnStorageTO = new VolumeOnStorageTO(hypervisorType, path, name, fullPath,
                format, size, virtualSize);
        volumeOnStorageTO.setQemuEncryptFormat(encryptFormat);
        List<VolumeOnStorageTO> volumesOnStorageTO = new ArrayList<>();
        volumesOnStorageTO.add(volumeOnStorageTO);
        doReturn(volumesOnStorageTO).when(volumeImportUnmanageManager).listVolumesForImportInternal(storagePoolVO, path, null);

        ListResponse<VolumeForImportResponse> listResponses = volumeImportUnmanageManager.listVolumesForImport(cmd);
        Assert.assertEquals(1, listResponses.getResponses().size());
        VolumeForImportResponse response = listResponses.getResponses().get(0);

        Assert.assertEquals(path, response.getPath());
        Assert.assertEquals(name, response.getName());
        Assert.assertEquals(fullPath, response.getFullPath());
        Assert.assertEquals(format, response.getFormat());
        Assert.assertEquals(size, response.getSize());
        Assert.assertEquals(virtualSize, response.getVirtualSize());
        Assert.assertEquals(encryptFormat, response.getQemuEncryptFormat());
        Assert.assertEquals(storagePoolType.name(), response.getStoragePoolType());
        Assert.assertEquals(storagePoolName, response.getStoragePoolName());
        Assert.assertEquals(storagePoolUuid, response.getStoragePoolId());
    }

    @Test
    public void testImportVolumeAllGood() throws ResourceAllocationException {
        ImportVolumeCmd cmd = mock(ImportVolumeCmd.class);
        when(cmd.getPath()).thenReturn(path);
        when(cmd.getStorageId()).thenReturn(poolId);
        when(cmd.getDiskOfferingId()).thenReturn(diskOfferingId);
        when(volumeDao.findByPoolIdAndPath(poolId, path)).thenReturn(null);
        when(templatePoolDao.findByPoolPath(poolId, path)).thenReturn(null);

        VolumeOnStorageTO volumeOnStorageTO = new VolumeOnStorageTO(hypervisorType, path, name, fullPath,
                format, size, virtualSize);
        volumeOnStorageTO.setQemuEncryptFormat(encryptFormat);
        List<VolumeOnStorageTO> volumesOnStorageTO = new ArrayList<>();
        volumesOnStorageTO.add(volumeOnStorageTO);

        doReturn(volumesOnStorageTO).when(volumeImportUnmanageManager).listVolumesForImportInternal(storagePoolVO, path, null);

        doNothing().when(volumeImportUnmanageManager).checkIfVolumeIsLocked(volumeOnStorageTO);
        doNothing().when(volumeImportUnmanageManager).checkIfVolumeIsEncrypted(volumeOnStorageTO);
        doNothing().when(volumeImportUnmanageManager).checkIfVolumeHasBackingFile(volumeOnStorageTO);

        doNothing().when(resourceLimitService).checkResourceLimit(account, Resource.ResourceType.volume);
        doNothing().when(resourceLimitService).checkResourceLimit(account, Resource.ResourceType.primary_storage, virtualSize);

        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOffering.isCustomized()).thenReturn(true);
        doReturn(diskOffering).when(volumeImportUnmanageManager).getOrCreateDiskOffering(account, diskOfferingId, zoneId, isLocal);
        doNothing().when(volumeApiService).validateCustomDiskOfferingSizeRange(anyLong());
        doReturn(true).when(volumeApiService).doesTargetStorageSupportDiskOffering(any(), isNull());
        doReturn(diskProfile).when(volumeManager).importVolume(any(), anyString(), any(), eq(virtualSize), isNull(), isNull(), anyLong(),
                any(), isNull(), isNull(), any(), isNull(), anyLong(), anyString(), isNull());
        when(diskProfile.getVolumeId()).thenReturn(volumeId);
        when(volumeDao.findById(volumeId)).thenReturn(volumeVO);

        doNothing().when(resourceLimitService).incrementResourceCount(accountId, Resource.ResourceType.volume);
        doNothing().when(resourceLimitService).incrementResourceCount(accountId, Resource.ResourceType.primary_storage, virtualSize);

        VolumeResponse response = mock(VolumeResponse.class);
        doReturn(response).when(responseGenerator).createVolumeResponse(ResponseObject.ResponseView.Full, volumeVO);
        try (MockedStatic<UsageEventUtils> ignored = Mockito.mockStatic(UsageEventUtils.class);
             MockedStatic<ActionEventUtils> ignoredtoo = Mockito.mockStatic(ActionEventUtils.class)) {
            VolumeResponse result = volumeImportUnmanageManager.importVolume(cmd);
            Assert.assertEquals(response, result);
        }
    }

    @Test
    public void testListVolumesForImportInternal() {
        Pair<HostVO, String> hostAndLocalPath = mock(Pair.class);
        doReturn(hostAndLocalPath).when(volumeImportUnmanageManager).findHostAndLocalPathForVolumeImport(storagePoolVO);
        when(hostAndLocalPath.first()).thenReturn(hostVO);

        VolumeOnStorageTO volumeOnStorageTO = new VolumeOnStorageTO(hypervisorType, path, name, fullPath,
                format, size, virtualSize);
        volumeOnStorageTO.setQemuEncryptFormat(encryptFormat);
        List<VolumeOnStorageTO> volumesOnStorageTO = new ArrayList<>();
        volumesOnStorageTO.add(volumeOnStorageTO);
        GetVolumesOnStorageAnswer answer = mock(GetVolumesOnStorageAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(answer.getVolumes()).thenReturn(volumesOnStorageTO);
        doReturn(answer).when(agentManager).easySend(eq(hostId), any(GetVolumesOnStorageCommand.class));

        List<VolumeOnStorageTO> result = volumeImportUnmanageManager.listVolumesForImportInternal(storagePoolVO, path, null);
        Assert.assertEquals(volumesOnStorageTO, result);
    }

    @Test
    public void testCheckIfVolumeIsLocked() {
        try {
            VolumeOnStorageTO volumeOnStorageTO = new VolumeOnStorageTO(hypervisorType, path, name, fullPath,
                    format, size, virtualSize);
            volumeOnStorageTO.addDetail(VolumeOnStorageTO.Detail.IS_LOCKED, "true");
            volumeImportUnmanageManager.checkIfVolumeIsLocked(volumeOnStorageTO);
            Assert.fail("It should fail as the volume is locked");
        } catch (CloudRuntimeException ex) {
            Assert.assertEquals("Locked volume cannot be imported or unmanaged.", ex.getMessage());
            verify(volumeImportUnmanageManager).logFailureAndThrowException("Locked volume cannot be imported or unmanaged.");
        }
    }

    @Test
    public void testCheckIfVolumeIsEncrypted() {
        try {
            VolumeOnStorageTO volumeOnStorageTO = new VolumeOnStorageTO(hypervisorType, path, name, fullPath,
                    format, size, virtualSize);
            volumeOnStorageTO.addDetail(VolumeOnStorageTO.Detail.IS_ENCRYPTED, "true");
            volumeImportUnmanageManager.checkIfVolumeIsEncrypted(volumeOnStorageTO);
            Assert.fail("It should fail as the volume is encrypted");
        } catch (CloudRuntimeException ex) {
            Assert.assertEquals("Encrypted volume cannot be imported or unmanaged.", ex.getMessage());
            verify(volumeImportUnmanageManager).logFailureAndThrowException("Encrypted volume cannot be imported or unmanaged.");
        }
    }

    @Test
    public void testCheckIfVolumeHasBackingFile() {
        try {
            VolumeOnStorageTO volumeOnStorageTO = new VolumeOnStorageTO(hypervisorType, path, name, fullPath,
                    format, size, virtualSize);
            volumeOnStorageTO.addDetail(VolumeOnStorageTO.Detail.BACKING_FILE, BACKING_FILE);
            volumeOnStorageTO.addDetail(VolumeOnStorageTO.Detail.BACKING_FILE_FORMAT, BACKING_FILE_FORMAT);
            volumeImportUnmanageManager.checkIfVolumeHasBackingFile(volumeOnStorageTO);
            Assert.fail("It should fail as the volume has backing file");
        } catch (CloudRuntimeException ex) {
            Assert.assertEquals("Volume with backing file cannot be imported or unmanaged.", ex.getMessage());
            verify(volumeImportUnmanageManager).logFailureAndThrowException("Volume with backing file cannot be imported or unmanaged.");
        }
    }

    @Test
    public void testUnmanageVolume() {
        when(volumeVO.getState()).thenReturn(Volume.State.Ready);
        when(volumeVO.getPoolId()).thenReturn(poolId);
        when(volumeVO.getInstanceId()).thenReturn(null);
        when(volumeVO.getPath()).thenReturn(path);
        VolumeOnStorageTO volumeOnStorageTO = new VolumeOnStorageTO(hypervisorType, path, name, fullPath,
                format, size, virtualSize);
        doReturn(volumeOnStorageTO).when(volumeImportUnmanageManager).getVolumeOnStorageAndCheck(storagePoolVO, path);
        doNothing().when(resourceLimitService).decrementResourceCount(accountId, Resource.ResourceType.volume);
        doNothing().when(resourceLimitService).decrementResourceCount(accountId, Resource.ResourceType.primary_storage, virtualSize);

        try (MockedStatic<UsageEventUtils> ignored = Mockito.mockStatic(UsageEventUtils.class);
             MockedStatic<ActionEventUtils> ignoredtoo = Mockito.mockStatic(ActionEventUtils.class)) {
            volumeImportUnmanageManager.unmanageVolume(volumeId);
        }

        verify(resourceLimitService).decrementResourceCount(volumeVO.getAccountId(), Resource.ResourceType.volume);
        verify(resourceLimitService).decrementResourceCount(volumeVO.getAccountId(), Resource.ResourceType.primary_storage, virtualSize);
        verify(volumeDao).update(eq(volumeId), any());
    }

    @Test
    public void testUnmanageVolumeNotExist() {
        try {
            when(volumeDao.findById(volumeId)).thenReturn(null);
            volumeImportUnmanageManager.unmanageVolume(volumeId);
            Assert.fail("it should fail");
        } catch (CloudRuntimeException ex) {
            verify(volumeImportUnmanageManager).logFailureAndThrowException(String.format("Volume (ID: %s) does not exist", volumeId));
        }
    }

    @Test
    public void testUnmanageVolumeNotReady() {
        try {
            when(volumeVO.getState()).thenReturn(Volume.State.Allocated);
            volumeImportUnmanageManager.unmanageVolume(volumeId);
            Assert.fail("it should fail");
        } catch (CloudRuntimeException ex) {
            verify(volumeImportUnmanageManager).logFailureAndThrowException(String.format("Volume (ID: %s) is not ready", volumeId));
        }
    }


    @Test
    public void testUnmanageVolumeEncrypted() {
        try {
            when(volumeVO.getState()).thenReturn(Volume.State.Ready);
            when(volumeVO.getEncryptFormat()).thenReturn(encryptFormat);
            volumeImportUnmanageManager.unmanageVolume(volumeId);
            Assert.fail("it should fail");
        } catch (CloudRuntimeException ex) {
            verify(volumeImportUnmanageManager).logFailureAndThrowException(String.format("Volume (ID: %s) is encrypted", volumeId));
        }
    }

    @Test
    public void testUnmanageVolumeAttached() {
        try {
            when(volumeVO.getState()).thenReturn(Volume.State.Ready);
            when(volumeVO.getAttached()).thenReturn(new Date());
            volumeImportUnmanageManager.unmanageVolume(volumeId);
            Assert.fail("it should fail");
        } catch (CloudRuntimeException ex) {
            verify(volumeImportUnmanageManager).logFailureAndThrowException(String.format("Volume (ID: %s) is attached to VM (ID: %s)", volumeId, volumeVO.getInstanceId()));
        }
    }

    @Test
    public void testCheckIfPoolAvailableNotExist() {
        try {
            when(primaryDataStoreDao.findById(poolId)).thenReturn(null);
            volumeImportUnmanageManager.checkIfPoolAvailable(poolId);
            Assert.fail("it should fail");
        } catch (CloudRuntimeException ex) {
            verify(volumeImportUnmanageManager).logFailureAndThrowException(String.format("Storage pool (ID: %s) does not exist", poolId));
        }
    }

    @Test
    public void testCheckIfPoolAvailableInMaintenance() {
        try {
            when(primaryDataStoreDao.findById(poolId)).thenReturn(storagePoolVO);
            when(storagePoolVO.isInMaintenance()).thenReturn(true);
            volumeImportUnmanageManager.checkIfPoolAvailable(poolId);
            Assert.fail("it should fail");
        } catch (CloudRuntimeException ex) {
            verify(volumeImportUnmanageManager).logFailureAndThrowException(String.format("Storage pool (name: %s) is in maintenance", storagePoolName));
        }
    }

    @Test
    public void testCheckIfPoolAvailableDisabled() {
        try {
            when(primaryDataStoreDao.findById(poolId)).thenReturn(storagePoolVO);
            when(storagePoolVO.isInMaintenance()).thenReturn(false);
            when(storagePoolVO.getStatus()).thenReturn(StoragePoolStatus.Disabled);
            volumeImportUnmanageManager.checkIfPoolAvailable(poolId);
            Assert.fail("it should fail");
        } catch (CloudRuntimeException ex) {
            verify(volumeImportUnmanageManager).logFailureAndThrowException(String.format("Storage pool (ID: %s) is not Up: %s", storagePoolName, StoragePoolStatus.Disabled));
        }
    }

    @Test
    public void testFindHostAndLocalPathForVolumeImportZoneScope() {
        when(storagePoolVO.getScope()).thenReturn(ScopeType.ZONE);
        List<HostVO> hosts = new ArrayList<>();
        hosts.add(hostVO);
        when(hostDao.listAllHostsUpByZoneAndHypervisor(zoneId, hypervisorType)).thenReturn(hosts);

        Pair<HostVO, String> result = volumeImportUnmanageManager.findHostAndLocalPathForVolumeImport(storagePoolVO);
        Assert.assertNotNull(result);
        Assert.assertEquals(hostVO, result.first());
        Assert.assertEquals(localPath, result.second());
    }

    @Test
    public void testFindHostAndLocalPathForVolumeImportClusterScope() {
        when(storagePoolVO.getScope()).thenReturn(ScopeType.CLUSTER);
        when(storagePoolVO.getClusterId()).thenReturn(clusterId);

        List<HostVO> hosts = new ArrayList<>();
        hosts.add(hostVO);
        when(hostDao.findHypervisorHostInCluster(clusterId)).thenReturn(hosts);

        Pair<HostVO, String> result = volumeImportUnmanageManager.findHostAndLocalPathForVolumeImport(storagePoolVO);
        Assert.assertNotNull(result);
        Assert.assertEquals(hostVO, result.first());
        Assert.assertEquals(localPath, result.second());
    }

    @Test
    public void testFindHostAndLocalPathForVolumeImportLocalHost() {
        when(storagePoolVO.getScope()).thenReturn(ScopeType.HOST);

        List<StoragePoolHostVO> storagePoolHostVOs = new ArrayList<>();
        storagePoolHostVOs.add(storagePoolHostVO);
        when(storagePoolHostDao.listByPoolId(poolId)).thenReturn(storagePoolHostVOs);

        Pair<HostVO, String> result = volumeImportUnmanageManager.findHostAndLocalPathForVolumeImport(storagePoolVO);
        Assert.assertNotNull(result);
        Assert.assertEquals(hostVO, result.first());
        Assert.assertEquals(localPath, result.second());
    }

    @Test
    public void testGetOrCreateDiskOfferingAllGood() {
        when(diskOfferingDao.findById(diskOfferingId)).thenReturn(diskOfferingVO);
        when(diskOfferingVO.getState()).thenReturn(DiskOffering.State.Active);
        when(diskOfferingVO.isUseLocalStorage()).thenReturn(isLocal);
        when(diskOfferingVO.getEncrypt()).thenReturn(false);
        doNothing().when(configMgr).checkDiskOfferingAccess(account, diskOfferingVO, dataCenterVO);

        DiskOfferingVO result = volumeImportUnmanageManager.getOrCreateDiskOffering(account, diskOfferingId, zoneId, isLocal);
        Assert.assertEquals(diskOfferingVO, result);
    }

    @Test
    public void testGetOrCreateDiskOfferingNotExist() {
        try {
            when(diskOfferingDao.findById(diskOfferingId)).thenReturn(null);
            volumeImportUnmanageManager.getOrCreateDiskOffering(account, diskOfferingId, zoneId, isLocal);
            Assert.fail("it should fail");
        } catch (CloudRuntimeException ex) {
            verify(volumeImportUnmanageManager).logFailureAndThrowException(String.format("Disk offering %s does not exist", diskOfferingId));
        }
    }

    @Test
    public void testGetOrCreateDiskOfferingNotActive() {
        try {
            when(diskOfferingDao.findById(diskOfferingId)).thenReturn(diskOfferingVO);
            when(diskOfferingVO.getState()).thenReturn(DiskOffering.State.Inactive);

            volumeImportUnmanageManager.getOrCreateDiskOffering(account, diskOfferingId, zoneId, isLocal);
            Assert.fail("it should fail");
        } catch (CloudRuntimeException ex) {
            verify(volumeImportUnmanageManager).logFailureAndThrowException(String.format("Disk offering with ID %s is not active", diskOfferingId));
        }
    }

    @Test
    public void testGetOrCreateDiskOfferingNotLocal() {
        try {
            when(diskOfferingDao.findById(diskOfferingId)).thenReturn(diskOfferingVO);
            when(diskOfferingVO.getState()).thenReturn(DiskOffering.State.Active);
            when(diskOfferingVO.isUseLocalStorage()).thenReturn(!isLocal);

            volumeImportUnmanageManager.getOrCreateDiskOffering(account, diskOfferingId, zoneId, isLocal);
            Assert.fail("it should fail");
        } catch (CloudRuntimeException ex) {
            verify(volumeImportUnmanageManager).logFailureAndThrowException(String.format("Disk offering with ID %s should use %s storage", diskOfferingId, isLocal ? "local" : "shared"));
        }
    }

    @Test
    public void testGetOrCreateDiskOfferingForVolumeEncryption() {
        try {
            when(diskOfferingDao.findById(diskOfferingId)).thenReturn(diskOfferingVO);
            when(diskOfferingVO.getState()).thenReturn(DiskOffering.State.Active);
            when(diskOfferingVO.isUseLocalStorage()).thenReturn(isLocal);
            when(diskOfferingVO.getEncrypt()).thenReturn(true);

            volumeImportUnmanageManager.getOrCreateDiskOffering(account, diskOfferingId, zoneId, isLocal);
            Assert.fail("it should fail");
        } catch (CloudRuntimeException ex) {
            verify(volumeImportUnmanageManager).logFailureAndThrowException(String.format("Disk offering with ID %s should not support volume encryption", diskOfferingId));
        }
    }

    @Test
    public void testGetOrCreateDiskOfferingNoPermission() {
        try {
            when(diskOfferingDao.findById(diskOfferingId)).thenReturn(diskOfferingVO);
            when(diskOfferingVO.getState()).thenReturn(DiskOffering.State.Active);
            when(diskOfferingVO.isUseLocalStorage()).thenReturn(isLocal);
            doThrow(PermissionDeniedException.class).when(configMgr).checkDiskOfferingAccess(account, diskOfferingVO, dataCenterVO);

            volumeImportUnmanageManager.getOrCreateDiskOffering(account, diskOfferingId, zoneId, isLocal);
            Assert.fail("it should fail");
        } catch (CloudRuntimeException ex) {
            verify(volumeImportUnmanageManager).logFailureAndThrowException(String.format("Disk offering with ID %s is not accessible by owner %s", diskOfferingId, account));
        }
    }

    @Test
    public void testGetOrCreateDefaultDiskOfferingIdForVolumeImportExist() {
        String uniqueName = DEFAULT_DISK_OFFERING_UNIQUE_NAME + (isLocal ? DISK_OFFERING_UNIQUE_NAME_SUFFIX_LOCAL : "");
        when(diskOfferingDao.findByUniqueName(uniqueName)).thenReturn(diskOfferingVO);

        DiskOfferingVO result = volumeImportUnmanageManager.getOrCreateDiskOffering(account, null, zoneId, isLocal);
        Assert.assertEquals(diskOfferingVO, result);
    }

    @Test
    public void testGetOrCreateDefaultDiskOfferingIdForVolumeImportNotExist() {
        String uniqueName = DEFAULT_DISK_OFFERING_UNIQUE_NAME + (isLocal ? DISK_OFFERING_UNIQUE_NAME_SUFFIX_LOCAL : "");
        when(diskOfferingDao.findByUniqueName(uniqueName)).thenReturn(null);
        when(diskOfferingDao.persistDefaultDiskOffering(any())).thenReturn(diskOfferingVO);

        try (
                MockedConstruction<DiskOfferingVO> diskOfferingVOMockedConstruction = Mockito.mockConstruction(DiskOfferingVO.class);
        ) {
            DiskOfferingVO result = volumeImportUnmanageManager.getOrCreateDiskOffering(account, null, zoneId, isLocal);
            Assert.assertEquals(diskOfferingVO, result);

            DiskOfferingVO diskOfferingVOMock = diskOfferingVOMockedConstruction.constructed().get(0);
            verify(diskOfferingVOMock).setUseLocalStorage(isLocal);
            verify(diskOfferingVOMock).setUniqueName(uniqueName);
        }
    }

    @Test
    public void testLogFailureAndThrowException() {
        String message = "error message";
        try {
            volumeImportUnmanageManager.logFailureAndThrowException(message);
        } catch (CloudRuntimeException ex) {
            Assert.assertEquals(message, ex.getMessage());
        }
    }
}
