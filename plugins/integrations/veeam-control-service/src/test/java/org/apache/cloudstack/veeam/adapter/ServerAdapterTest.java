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

package org.apache.cloudstack.veeam.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.backup.BackupVO;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.ImageTransferDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.veeam.api.dto.DataCenter;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.Tag;
import org.apache.cloudstack.veeam.api.dto.Vm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.api.query.dao.AsyncJobJoinDao;
import com.cloud.api.query.dao.DataCenterJoinDao;
import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.dao.StoragePoolJoinDao;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.dao.VolumeJoinDao;
import com.cloud.api.query.vo.AsyncJobJoinVO;
import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.StoragePoolJoinVO;
import com.cloud.api.query.vo.VolumeJoinVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@RunWith(MockitoJUnitRunner.class)
public class ServerAdapterTest {

    @InjectMocks
    ServerAdapter serverAdapter;

    @Mock AccountService accountService;
    @Mock DataCenterDao dataCenterDao;
    @Mock DataCenterJoinDao dataCenterJoinDao;
    @Mock StoragePoolJoinDao storagePoolJoinDao;
    @Mock ClusterDao clusterDao;
    @Mock HostJoinDao hostJoinDao;
    @Mock NetworkDao networkDao;
    @Mock UserVmDao userVmDao;
    @Mock UserVmJoinDao userVmJoinDao;
    @Mock VolumeDao volumeDao;
    @Mock VolumeJoinDao volumeJoinDao;
    // kept minimal: only mocks used directly by tests
    @Mock com.cloud.storage.VolumeApiService volumeApiService;
    @Mock PrimaryDataStoreDao primaryDataStoreDao;
    @Mock ImageTransferDao imageTransferDao;
    @Mock ServiceOfferingDao serviceOfferingDao;
    @Mock VMTemplateDao templateDao;
    @Mock UserVmManager userVmManager;
    @Mock AsyncJobDao asyncJobDao;
    @Mock AsyncJobJoinDao asyncJobJoinDao;
    @Mock VMSnapshotDao vmSnapshotDao;
    @Mock BackupDao backupDao;
    @Mock NetworkModel networkModel;
    @Mock ProjectManager projectManager;
    @Mock DomainDao domainDao;

    @Before
    public void setupCallContext() {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
    }

    @After
    public void cleanupCallContext() {
        CallContext.unregister();
    }



    @Test
    public void testGetProvisionedSizeInGb_ExactlyOneGB() {
        long gb = 1024L * 1024L * 1024L;
        assertEquals(1L, ServerAdapter.getProvisionedSizeInGb(String.valueOf(gb)));
    }

    @Test
    public void testGetProvisionedSizeInGb_MultipleGB() {
        long gb = 1024L * 1024L * 1024L;
        assertEquals(5L, ServerAdapter.getProvisionedSizeInGb(String.valueOf(5 * gb)));
    }

    @Test
    public void testGetProvisionedSizeInGb_LessThanOneGB_RoundsUpToOne() {
        assertEquals(1L, ServerAdapter.getProvisionedSizeInGb("512"));
    }

    @Test
    public void testGetProvisionedSizeInGb_NotExactGB_RoundsUp() {
        long gb = 1024L * 1024L * 1024L;
        assertEquals(2L, ServerAdapter.getProvisionedSizeInGb(String.valueOf(gb + gb / 2)));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetProvisionedSizeInGb_InvalidString_Throws() {
        ServerAdapter.getProvisionedSizeInGb("not-a-number");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetProvisionedSizeInGb_Zero_Throws() {
        ServerAdapter.getProvisionedSizeInGb("0");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetProvisionedSizeInGb_Negative_Throws() {
        ServerAdapter.getProvisionedSizeInGb("-1073741824");
    }


    @Test
    public void testGetDetailsForInstanceCreation_WithUserdata_AddsCpuMode() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        when(offering.isCustomized()).thenReturn(false);

        Map<String, String> result = ServerAdapter.getDetailsForInstanceCreation("#!/bin/bash", offering, null);

        assertEquals("host-passthrough", result.get(VmDetailConstants.GUEST_CPU_MODE));
    }

    @Test
    public void testGetDetailsForInstanceCreation_NoUserdata_NoCpuMode() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        when(offering.isCustomized()).thenReturn(false);

        Map<String, String> result = ServerAdapter.getDetailsForInstanceCreation(null, offering, null);

        assertFalse(result.containsKey(VmDetailConstants.GUEST_CPU_MODE));
    }

    @Test
    public void testGetDetailsForInstanceCreation_CustomizedOffering_AddsDetails() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        when(offering.isCustomized()).thenReturn(true);
        when(offering.getCpu()).thenReturn(4);
        when(offering.getRamSize()).thenReturn(2048);
        when(offering.getSpeed()).thenReturn(null);

        Map<String, String> result = ServerAdapter.getDetailsForInstanceCreation(null, offering, null);

        assertEquals("4", result.get(VmDetailConstants.CPU_NUMBER));
        assertEquals("2048", result.get(VmDetailConstants.MEMORY));
        assertEquals("1000", result.get(VmDetailConstants.CPU_SPEED));
    }

    @Test
    public void testGetDetailsForInstanceCreation_CustomizedOffering_WithSpeed_DoesNotAddDefaultCpuSpeed() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        when(offering.isCustomized()).thenReturn(true);
        when(offering.getCpu()).thenReturn(2);
        when(offering.getRamSize()).thenReturn(1024);
        when(offering.getSpeed()).thenReturn(2000);

        Map<String, String> result = ServerAdapter.getDetailsForInstanceCreation(null, offering, null);

        assertFalse(result.containsKey(VmDetailConstants.CPU_SPEED));
    }

    @Test
    public void testGetDetailsForInstanceCreation_SkipsBiosAndUefiKeys() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        when(offering.isCustomized()).thenReturn(false);

        Map<String, String> existingDetails = new HashMap<>();
        existingDetails.put("BIOS", "bios_value");
        existingDetails.put("UEFI", "uefi_value");
        existingDetails.put("custom_key", "custom_value");

        Map<String, String> result = ServerAdapter.getDetailsForInstanceCreation(null, offering, existingDetails);

        assertFalse(result.containsKey("BIOS"));
        assertFalse(result.containsKey("UEFI"));
        assertEquals("custom_value", result.get("custom_key"));
    }

    @Test
    public void testGetDetailsForInstanceCreation_PreservesExistingCpuSpeed() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        when(offering.isCustomized()).thenReturn(true);
        when(offering.getCpu()).thenReturn(2);
        when(offering.getRamSize()).thenReturn(1024);
        when(offering.getSpeed()).thenReturn(null);

        Map<String, String> existingDetails = new HashMap<>();
        existingDetails.put(VmDetailConstants.CPU_SPEED, "3000");

        Map<String, String> result = ServerAdapter.getDetailsForInstanceCreation(null, offering, existingDetails);

        assertEquals("3000", result.get(VmDetailConstants.CPU_SPEED));
    }


    @Test
    public void testGetDummyTags_ContainsRootTag() {
        Map<String, Tag> tags = ServerAdapter.getDummyTags();
        assertNotNull(tags);
        assertFalse(tags.isEmpty());
    }


    @Test
    public void testGetTemplateForInstanceCreation_NullUuid_ReturnsNull() {
        assertNull(serverAdapter.getTemplateForInstanceCreation(null));
    }

    @Test
    public void testGetTemplateForInstanceCreation_BlankUuid_ReturnsNull() {
        assertNull(serverAdapter.getTemplateForInstanceCreation("   "));
    }

    @Test
    public void testGetTemplateForInstanceCreation_TemplateNotFound_ReturnsNull() {
        when(templateDao.findByUuid("missing-uuid")).thenReturn(null);
        assertNull(serverAdapter.getTemplateForInstanceCreation("missing-uuid"));
    }

    @Test
    public void testGetTemplateForInstanceCreation_TemplateFound_ReturnsTemplate() {
        VMTemplateVO template = mock(VMTemplateVO.class);
        when(templateDao.findByUuid("valid-uuid")).thenReturn(template);
        assertEquals(template, serverAdapter.getTemplateForInstanceCreation("valid-uuid"));
    }


    @Test
    public void testGetZoneById_NullId_ReturnsNull() {
        assertNull(serverAdapter.getZoneById(null));
    }

    @Test
    public void testGetZoneById_ReturnsVoFromDao() {
        DataCenterJoinVO vo = mock(DataCenterJoinVO.class);
        when(dataCenterJoinDao.findById(1L)).thenReturn(vo);
        assertEquals(vo, serverAdapter.getZoneById(1L));
    }

    @Test
    public void testGetHostById_NullId_ReturnsNull() {
        assertNull(serverAdapter.getHostById(null));
    }

    @Test
    public void testGetHostById_ReturnsVoFromDao() {
        HostJoinVO vo = mock(HostJoinVO.class);
        when(hostJoinDao.findById(2L)).thenReturn(vo);
        assertEquals(vo, serverAdapter.getHostById(2L));
    }

    @Test
    public void testGetVolumeById_NullId_ReturnsNull() {
        assertNull(serverAdapter.getVolumeById(null));
    }

    @Test
    public void testGetVolumeById_ReturnsVoFromDao() {
        VolumeJoinVO vo = mock(VolumeJoinVO.class);
        when(volumeJoinDao.findById(3L)).thenReturn(vo);
        assertEquals(vo, serverAdapter.getVolumeById(3L));
    }

    @Test
    public void testGetNetworkById_NullId_ReturnsNull() {
        assertNull(serverAdapter.getNetworkById(null));
    }

    @Test
    public void testGetNetworkById_ReturnsVoFromDao() {
        NetworkVO vo = mock(NetworkVO.class);
        when(networkDao.findById(4L)).thenReturn(vo);
        assertEquals(vo, serverAdapter.getNetworkById(4L));
    }


    @Test
    public void testWaitForJobCompletion_JobNotFound_Returns() {
        when(asyncJobDao.findById(99L)).thenReturn(null);
        serverAdapter.waitForJobCompletion(99L);
        verify(asyncJobDao).findById(99L);
    }

    @Test
    public void testWaitForJobCompletion_JobAlreadySucceeded_Returns() {
        AsyncJobVO job = mock(AsyncJobVO.class);
        when(job.getStatus()).thenReturn(AsyncJobVO.Status.SUCCEEDED);
        when(asyncJobDao.findById(1L)).thenReturn(job);
        serverAdapter.waitForJobCompletion(1L);
    }

    @Test
    public void testWaitForJobCompletion_JobAlreadyFailed_Returns() {
        AsyncJobVO job = mock(AsyncJobVO.class);
        when(job.getStatus()).thenReturn(AsyncJobVO.Status.FAILED);
        when(asyncJobDao.findById(2L)).thenReturn(job);
        serverAdapter.waitForJobCompletion(2L);
    }


    @Test
    public void testWaitForJobCompletion_NullJobJoinVO_Returns() {
        AsyncJobJoinVO job = null;
        serverAdapter.waitForJobCompletion(job);
    }

    @Test
    public void testWaitForJobCompletion_CompletedJobJoinVO_DelegatesById() {
        AsyncJobJoinVO jobVO = mock(AsyncJobJoinVO.class);
        when(jobVO.getStatus()).thenReturn(AsyncJobVO.Status.SUCCEEDED.ordinal());
        when(jobVO.getId()).thenReturn(5L);

        AsyncJobVO job = mock(AsyncJobVO.class);
        when(job.getStatus()).thenReturn(AsyncJobVO.Status.SUCCEEDED);
        when(asyncJobDao.findById(5L)).thenReturn(job);

        serverAdapter.waitForJobCompletion(jobVO);

        verify(asyncJobDao).findById(5L);
    }


    @Test
    public void testGetOwnerDetailsForInstanceCreation_NullAccount_ReturnsAllNulls() {
        Ternary<Long, String, Long> result = serverAdapter.getOwnerDetailsForInstanceCreation(null);
        assertNull(result.first());
        assertNull(result.second());
        assertNull(result.third());
    }

    @Test
    public void testGetOwnerDetailsForInstanceCreation_NormalAccount_ReturnsDomainAndName() {
        Account account = mock(Account.class);
        when(account.getType()).thenReturn(Account.Type.NORMAL);
        when(account.getDomainId()).thenReturn(2L);
        when(account.getAccountName()).thenReturn("myaccount");

        Ternary<Long, String, Long> result = serverAdapter.getOwnerDetailsForInstanceCreation(account);

        assertEquals(Long.valueOf(2L), result.first());
        assertEquals("myaccount", result.second());
        assertNull(result.third());
    }

    @Test
    public void testGetOwnerDetailsForInstanceCreation_ProjectAccount_ProjectNotFound_ReturnsAllNulls() {
        Account account = mock(Account.class);
        when(account.getType()).thenReturn(Account.Type.PROJECT);
        when(account.getId()).thenReturn(10L);
        when(projectManager.findByProjectAccountId(10L)).thenReturn(null);

        Ternary<Long, String, Long> result = serverAdapter.getOwnerDetailsForInstanceCreation(account);

        assertNull(result.first());
        assertNull(result.second());
        assertNull(result.third());
    }

    @Test
    public void testGetOwnerDetailsForInstanceCreation_ProjectAccount_ProjectFound_ReturnsProjectId() {
        Account account = mock(Account.class);
        when(account.getType()).thenReturn(Account.Type.PROJECT);
        when(account.getId()).thenReturn(10L);
        when(account.getDomainId()).thenReturn(1L);

        Project project = mock(Project.class);
        when(project.getId()).thenReturn(5L);
        when(projectManager.findByProjectAccountId(10L)).thenReturn(project);

        Ternary<Long, String, Long> result = serverAdapter.getOwnerDetailsForInstanceCreation(account);

        assertEquals(Long.valueOf(1L), result.first());
        assertNull(result.second());
        assertEquals(Long.valueOf(5L), result.third());
    }


    @Test
    public void testGetServiceOfferingFromRequest_BlankUuid_ReturnsNull() {
        assertNull(serverAdapter.getServiceOfferingFromRequest(null, null, "", 2, 1024));
        assertNull(serverAdapter.getServiceOfferingFromRequest(null, null, null, 2, 1024));
    }

    @Test
    public void testGetServiceOfferingFromRequest_OfferingNotFound_ReturnsNull() {
        when(serviceOfferingDao.findByUuid("uuid1")).thenReturn(null);
        assertNull(serverAdapter.getServiceOfferingFromRequest(null, null, "uuid1", 2, 1024));
    }

    @Test
    public void testGetServiceOfferingFromRequest_AccessDenied_ReturnsNull() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        Account account = mock(Account.class);
        when(serviceOfferingDao.findByUuid("uuid2")).thenReturn(offering);
        doThrow(new PermissionDeniedException("denied"))
                .when(accountService).checkAccess(eq(account), eq(offering), any());

        assertNull(serverAdapter.getServiceOfferingFromRequest(null, account, "uuid2", 2, 1024));
    }

    @Test
    public void testGetServiceOfferingFromRequest_NotCustomized_CpuMemoryMismatch_ReturnsNull() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        Account account = mock(Account.class);
        when(serviceOfferingDao.findByUuid("uuid3")).thenReturn(offering);
        doNothing().when(accountService).checkAccess(eq(account), eq(offering), any());
        when(offering.isCustomized()).thenReturn(false);
        when(offering.getCpu()).thenReturn(4);

        assertNull(serverAdapter.getServiceOfferingFromRequest(null, account, "uuid3", 2, 1024));
    }

    @Test
    public void testGetServiceOfferingFromRequest_NotCustomized_CpuMemoryMatch_ReturnsOffering() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        Account account = mock(Account.class);
        when(serviceOfferingDao.findByUuid("uuid4")).thenReturn(offering);
        doNothing().when(accountService).checkAccess(eq(account), eq(offering), any());
        when(offering.isCustomized()).thenReturn(false);
        when(offering.getCpu()).thenReturn(2);
        when(offering.getRamSize()).thenReturn(1024);

        assertEquals(offering, serverAdapter.getServiceOfferingFromRequest(null, account, "uuid4", 2, 1024));
    }

    @Test
    public void testGetServiceOfferingFromRequest_Customized_ValidParams_SetsAndReturnsOffering() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        Account account = mock(Account.class);
        when(serviceOfferingDao.findByUuid("uuid5")).thenReturn(offering);
        doNothing().when(accountService).checkAccess(eq(account), eq(offering), any());
        when(offering.isCustomized()).thenReturn(true);
        doNothing().when(userVmManager).validateCustomParameters(eq(offering), any());

        ServiceOfferingVO result = serverAdapter.getServiceOfferingFromRequest(null, account, "uuid5", 2, 1024);

        assertEquals(offering, result);
        verify(offering).setCpu(2);
        verify(offering).setRamSize(1024);
    }

    @Test
    public void testGetServiceOfferingFromRequest_Customized_InvalidParams_ReturnsNull() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        Account account = mock(Account.class);
        when(serviceOfferingDao.findByUuid("uuid6")).thenReturn(offering);
        doNothing().when(accountService).checkAccess(eq(account), eq(offering), any());
        when(offering.isCustomized()).thenReturn(true);
        doThrow(new InvalidParameterValueException("invalid"))
                .when(userVmManager).validateCustomParameters(eq(offering), any());

        assertNull(serverAdapter.getServiceOfferingFromRequest(null, account, "uuid6", 2, 1024));
    }


    @Test
    public void testAccountCannotAccessNetwork_CanAccess_ReturnsFalse() {
        NetworkVO network = mock(NetworkVO.class);
        Account account = mock(Account.class);
        when(accountService.getActiveAccountById(1L)).thenReturn(account);
        doNothing().when(networkModel).checkNetworkPermissions(account, network);

        assertFalse(serverAdapter.accountCannotAccessNetwork(network, 1L));
    }

    @Test
    public void testAccountCannotAccessNetwork_CannotAccess_ReturnsTrue() {
        NetworkVO network = mock(NetworkVO.class);
        Account account = mock(Account.class);
        when(accountService.getActiveAccountById(1L)).thenReturn(account);
        doThrow(new CloudRuntimeException("Access denied"))
                .when(networkModel).checkNetworkPermissions(account, network);

        assertTrue(serverAdapter.accountCannotAccessNetwork(network, 1L));
    }


    @Test
    public void testValidateInstanceStorage_SupportedStorageType_NoException() {
        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getId()).thenReturn(1L);
        VolumeVO vol = mock(VolumeVO.class);
        when(vol.getPoolId()).thenReturn(10L);
        when(volumeDao.findUsableVolumesForInstance(1L)).thenReturn(List.of(vol));
        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        when(primaryDataStoreDao.listByIds(anyList())).thenReturn(List.of(pool));

        serverAdapter.validateInstanceStorage(vm);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateInstanceStorage_UnsupportedStorageType_Throws() {
        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getId()).thenReturn(2L);
        VolumeVO vol = mock(VolumeVO.class);
        when(vol.getPoolId()).thenReturn(20L);
        when(volumeDao.findUsableVolumesForInstance(2L)).thenReturn(List.of(vol));
        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.getPoolType()).thenReturn(Storage.StoragePoolType.RBD);
        when(pool.getName()).thenReturn("ceph-pool");
        when(primaryDataStoreDao.listByIds(anyList())).thenReturn(List.of(pool));

        serverAdapter.validateInstanceStorage(vm);
    }

    @Test
    public void testValidateInstanceStorage_NoVolumes_NoException() {
        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getId()).thenReturn(3L);
        when(volumeDao.findUsableVolumesForInstance(3L)).thenReturn(Collections.emptyList());
        when(primaryDataStoreDao.listByIds(anyList())).thenReturn(Collections.emptyList());

        serverAdapter.validateInstanceStorage(vm);
    }


    @Test
    public void testGetBackupDisks_NullVolumeInfos_ReturnsEmptyList() {
        BackupVO backup = mock(BackupVO.class);
        when(backup.getBackedUpVolumes()).thenReturn(null);

        assertTrue(serverAdapter.getBackupDisks(backup).isEmpty());
    }

    @Test
    public void testGetBackupDisks_EmptyVolumeInfos_ReturnsEmptyList() {
        BackupVO backup = mock(BackupVO.class);
        when(backup.getBackedUpVolumes()).thenReturn(Collections.emptyList());

        assertTrue(serverAdapter.getBackupDisks(backup).isEmpty());
    }


    @Test
    public void testGetResourceOwnerFiltersWithDomainIds_NullDomainPath_ReturnsNullDomainIds() {
        ServerAdapter spyAdapter = spy(serverAdapter);
        doReturn(new Pair<>(List.of(1L, 2L), (String) null)).when(spyAdapter).getResourceOwnerFilters();

        Pair<List<Long>, List<Long>> result = spyAdapter.getResourceOwnerFiltersWithDomainIds();

        assertEquals(List.of(1L, 2L), result.first());
        assertNull(result.second());
    }

    @Test
    public void testGetResourceOwnerFiltersWithDomainIds_WithDomainPath_ReturnsDomainIds() {
        ServerAdapter spyAdapter = spy(serverAdapter);
        doReturn(new Pair<>(List.of(1L), "ROOT/subdomain")).when(spyAdapter).getResourceOwnerFilters();
        when(domainDao.getDomainChildrenIds("ROOT/subdomain")).thenReturn(List.of(10L, 11L));

        Pair<List<Long>, List<Long>> result = spyAdapter.getResourceOwnerFiltersWithDomainIds();

        assertEquals(List.of(10L, 11L), result.second());
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testGetDataCenter_NotFound_Throws() {
        when(dataCenterJoinDao.findByUuid("dc-uuid")).thenReturn(null);
        serverAdapter.getDataCenter("dc-uuid");
    }

    @Test
    public void testGetDataCenter_Found_ReturnsDataCenter() {
        DataCenterJoinVO vo = mock(DataCenterJoinVO.class);
        when(dataCenterJoinDao.findByUuid("dc-uuid")).thenReturn(vo);

        DataCenter result = serverAdapter.getDataCenter("dc-uuid");

        assertNotNull(result);
    }


    @Test
    public void testListAllDataCenters_ReturnsConvertedList() {
        DataCenterJoinVO vo = mock(DataCenterJoinVO.class);
        when(dataCenterJoinDao.listAll(any())).thenReturn(List.of(vo));

        List<DataCenter> result = serverAdapter.listAllDataCenters(0L, 10L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testListAllDataCenters_EmptyList_ReturnsEmpty() {
        when(dataCenterJoinDao.listAll(any())).thenReturn(Collections.emptyList());

        assertTrue(serverAdapter.listAllDataCenters(0L, 10L).isEmpty());
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testListStorageDomainsByDcId_DataCenterNotFound_Throws() {
        when(dataCenterDao.findByUuid("dc-uuid")).thenReturn(null);
        serverAdapter.listStorageDomainsByDcId("dc-uuid", 0L, 10L);
    }

    @Test
    public void testListStorageDomainsByDcId_Found_ReturnsList() {
        DataCenterVO dcVO = mock(DataCenterVO.class);
        when(dcVO.getId()).thenReturn(1L);
        when(dataCenterDao.findByUuid("dc-uuid")).thenReturn(dcVO);
        StoragePoolJoinVO poolVO = mock(StoragePoolJoinVO.class);
        when(storagePoolJoinDao.listByZoneHypervisorAndType(eq(1L), eq(Hypervisor.HypervisorType.KVM), any(), any())).thenReturn(List.of(poolVO));

        assertNotNull(serverAdapter.listStorageDomainsByDcId("dc-uuid", 0L, 10L));
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testListNetworksByDcId_DataCenterNotFound_Throws() {
        when(dataCenterJoinDao.findByUuid("dc-uuid")).thenReturn(null);
        serverAdapter.listNetworksByDcId("dc-uuid", 0L, 10L);
    }

    @Test
    public void testListNetworksByDcId_Found_ReturnsEmptyListWhenNoNetworks() {
        DataCenterJoinVO dcVO = mock(DataCenterJoinVO.class);
        when(dcVO.getId()).thenReturn(1L);
        when(dataCenterJoinDao.findByUuid("dc-uuid")).thenReturn(dcVO);
        when(networkDao.listByZoneAndTrafficType(eq(1L), eq(Networks.TrafficType.Guest), any()))
                .thenReturn(Collections.emptyList());
        assertTrue(serverAdapter.listNetworksByDcId("dc-uuid", 0L, 10L).isEmpty());
    }


    @Test
    public void testListAllClusters_ReturnsEmptyListWhenNoClusters() {
        when(clusterDao.listByHypervisorType(any(), any())).thenReturn(Collections.emptyList());
        assertTrue(serverAdapter.listAllClusters(0L, 10L).isEmpty());
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testGetCluster_NotFound_Throws() {
        when(clusterDao.findByUuid("cl-uuid")).thenReturn(null);
        serverAdapter.getCluster("cl-uuid");
    }


    @Test
    public void testListAllHosts_ReturnsList() {
        HostJoinVO hostVO = mock(HostJoinVO.class);
        when(hostJoinDao.listRoutingHostsByHypervisor(any(), any())).thenReturn(List.of(hostVO));

        assertEquals(1, serverAdapter.listAllHosts(0L, 10L).size());
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testGetHost_NotFound_Throws() {
        when(hostJoinDao.findByUuid("host-uuid")).thenReturn(null);
        serverAdapter.getHost("host-uuid");
    }

    @Test
    public void testGetHost_Found_ReturnsHost() {
        HostJoinVO vo = mock(HostJoinVO.class);
        when(hostJoinDao.findByUuid("host-uuid")).thenReturn(vo);

        assertNotNull(serverAdapter.getHost("host-uuid"));
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testGetVnicProfile_NotFound_Throws() {
        when(networkDao.findByUuid("net-uuid")).thenReturn(null);
        serverAdapter.getVnicProfile("net-uuid");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testGetInstance_NotFound_Throws() {
        when(userVmJoinDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.getInstance("vm-uuid", false, false, false, false);
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteInstance_VmNotFound_Throws() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.deleteInstance("vm-uuid", false);
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testStartInstance_VmNotFound_Throws() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.startInstance("vm-uuid", false);
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testStopInstance_VmNotFound_Throws() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.stopInstance("vm-uuid", false);
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testShutdownInstance_VmNotFound_Throws() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.shutdownInstance("vm-uuid", false);
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testGetDisk_NotFound_Throws() {
        when(volumeDao.findByUuid("vol-uuid")).thenReturn(null);
        serverAdapter.getDisk("vol-uuid");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteDisk_NotFound_Throws() {
        when(volumeDao.findByUuid("vol-uuid")).thenReturn(null);
        serverAdapter.deleteDisk("vol-uuid");
    }

    @Test
    public void testDeleteDisk_Found_DeletesVolume() {
        VolumeVO vo = mock(VolumeVO.class);
        when(vo.getId()).thenReturn(10L);
        when(volumeDao.findByUuid("vol-uuid")).thenReturn(vo);
        Account sysAccount = mock(Account.class);
        when(accountService.getSystemAccount()).thenReturn(sysAccount);

        serverAdapter.deleteDisk("vol-uuid");

        verify(volumeApiService).deleteVolume(10L, sysAccount);
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testCopyDisk_AlwaysThrows() {
        serverAdapter.copyDisk("any-uuid");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testReduceDisk_AlwaysThrows() {
        serverAdapter.reduceDisk("any-uuid");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testListNicsByInstanceUuid_VmNotFound_Throws() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.listNicsByInstanceUuid("vm-uuid");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testListDiskAttachmentsByInstanceUuid_VmNotFound_Throws() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.listDiskAttachmentsByInstanceUuid("vm-uuid");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testListSnapshotsByInstanceUuid_VmNotFound_Throws() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.listSnapshotsByInstanceUuid("vm-uuid");
    }

    @Test
    public void testListSnapshotsByInstanceUuid_Found_ReturnsEmptyList() {
        UserVmVO vmVO = mock(UserVmVO.class);
        when(vmVO.getId()).thenReturn(1L);
        when(vmVO.getUuid()).thenReturn("vm-uuid");
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(vmVO);
        when(vmSnapshotDao.findByVm(1L)).thenReturn(Collections.emptyList());

        assertTrue(serverAdapter.listSnapshotsByInstanceUuid("vm-uuid").isEmpty());
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testGetSnapshot_NotFound_Throws() {
        when(vmSnapshotDao.findByUuid("snap-uuid")).thenReturn(null);
        serverAdapter.getSnapshot("snap-uuid");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteSnapshot_NotFound_Throws() {
        when(vmSnapshotDao.findByUuid("snap-uuid")).thenReturn(null);
        serverAdapter.deleteSnapshot("snap-uuid", false);
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testRevertInstanceToSnapshot_NotFound_Throws() {
        when(vmSnapshotDao.findByUuid("snap-uuid")).thenReturn(null);
        serverAdapter.revertInstanceToSnapshot("snap-uuid", false);
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testListBackupsByInstanceUuid_VmNotFound_Throws() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.listBackupsByInstanceUuid("vm-uuid");
    }

    @Test
    public void testListBackupsByInstanceUuid_Found_ReturnsEmptyList() {
        UserVmVO vmVO = mock(UserVmVO.class);
        when(vmVO.getId()).thenReturn(1L);
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(vmVO);
        when(backupDao.searchByVmIds(anyList())).thenReturn(Collections.emptyList());

        assertTrue(serverAdapter.listBackupsByInstanceUuid("vm-uuid").isEmpty());
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testGetBackup_NotFound_Throws() {
        when(backupDao.findByUuidIncludingRemoved("backup-uuid")).thenReturn(null);
        serverAdapter.getBackup("backup-uuid");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testFinalizeBackup_VmNotFound_Throws() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.finalizeBackup("vm-uuid", "backup-uuid");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testFinalizeBackup_BackupNotFound_Throws() {
        UserVmVO vmVO = mock(UserVmVO.class);
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(vmVO);
        when(backupDao.findByUuid("backup-uuid")).thenReturn(null);
        serverAdapter.finalizeBackup("vm-uuid", "backup-uuid");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testListCheckpointsByInstanceUuid_VmNotFound_Throws() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.listCheckpointsByInstanceUuid("vm-uuid");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteCheckpoint_VmNotFound_Throws() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.deleteCheckpoint("vm-uuid", "chk-001");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testGetJob_NotFound_Throws() {
        when(asyncJobJoinDao.findByUuidIncludingRemoved("job-uuid")).thenReturn(null);
        serverAdapter.getJob("job-uuid");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testGetImageTransfer_NotFound_Throws() {
        when(imageTransferDao.findByUuidIncludingRemoved("transfer-uuid")).thenReturn(null);
        serverAdapter.getImageTransfer("transfer-uuid");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testCancelImageTransfer_NotFound_Throws() {
        when(imageTransferDao.findByUuid("transfer-uuid")).thenReturn(null);
        serverAdapter.cancelImageTransfer("transfer-uuid");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testFinalizeImageTransfer_NotFound_Throws() {
        when(imageTransferDao.findByUuid("transfer-uuid")).thenReturn(null);
        serverAdapter.finalizeImageTransfer("transfer-uuid");
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateInstance_VmNotFound_Throws() {
        when(userVmJoinDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.updateInstance("vm-uuid", new Vm());
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateDisk_NotFound_Throws() {
        when(volumeDao.findByUuid("vol-uuid")).thenReturn(null);
        serverAdapter.updateDisk("vol-uuid", new Disk());
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testListDisksByBackupUuid_AlwaysThrows() {
        serverAdapter.listDisksByBackupUuid("backup-uuid");
    }
}
