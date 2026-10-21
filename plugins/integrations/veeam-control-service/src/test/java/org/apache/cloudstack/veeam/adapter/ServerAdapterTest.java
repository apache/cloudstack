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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.command.user.vm.RemoveNicFromVMCmd;
import org.apache.cloudstack.api.command.user.volume.DestroyVolumeCmd;
import org.apache.cloudstack.api.response.ExceptionResponse;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.backup.BackupVO;
import org.apache.cloudstack.backup.KVMBackupExportService;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.ImageTransferDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.veeam.api.dto.DataCenter;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.Nic;
import org.apache.cloudstack.veeam.api.dto.ResourceAction;
import org.apache.cloudstack.veeam.api.dto.StorageDomain;
import org.apache.cloudstack.veeam.api.dto.Host;
import org.apache.cloudstack.veeam.api.dto.OvfXmlUtil;
import org.apache.cloudstack.veeam.api.dto.Statistic;
import org.apache.cloudstack.veeam.api.dto.Tag;
import org.apache.cloudstack.veeam.api.dto.Vm;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.api.ApiSerializerHelper;
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
import com.cloud.host.HostStats;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.server.StatsCollector;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
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
    @Mock StatsCollector statsCollector;
    @Mock NetworkDao networkDao;
    @Mock UserVmDao userVmDao;
    @Mock UserVmJoinDao userVmJoinDao;
    @Mock VMInstanceDetailsDao vmInstanceDetailsDao;
    @Mock VolumeDao volumeDao;
    @Mock VolumeJoinDao volumeJoinDao;
    // kept minimal: only mocks used directly by tests
    @Mock com.cloud.storage.VolumeApiService volumeApiService;
    @Mock PrimaryDataStoreDao primaryDataStoreDao;
    @Mock ImageTransferDao imageTransferDao;
    @Mock ServiceOfferingDao serviceOfferingDao;
    @Mock VMTemplateDao templateDao;
    @Mock VMTemplateZoneDao templateZoneDao;
    @Mock UserVmManager userVmManager;
    @Mock NicDao nicDao;
    @Mock AsyncJobDao asyncJobDao;
    @Mock AsyncJobJoinDao asyncJobJoinDao;
    @Mock VMSnapshotDao vmSnapshotDao;
    @Mock BackupDao backupDao;
    @Mock NetworkModel networkModel;
    @Mock ProjectManager projectManager;
    @Mock DomainDao domainDao;
    @Mock
    KVMBackupExportService kvmBackupExportService;

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
    public void testGetDetailsForInstanceCreation_WorkerVm_AddsCpuMode() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        when(offering.isCustomized()).thenReturn(false);
        Vm request = mock(Vm.class);
        when(request.isWorkerVm()).thenReturn(true);

        Map<String, String> result = ServerAdapter.getDetailsForInstanceCreation(request, offering, null);

        assertEquals("host-passthrough", result.get(VmDetailConstants.GUEST_CPU_MODE));
    }

    @Test
    public void testGetDetailsForInstanceCreation_NotWorkerVm_NoCpuMode() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        when(offering.isCustomized()).thenReturn(false);
        Vm request = mock(Vm.class);
        when(request.isWorkerVm()).thenReturn(false);

        Map<String, String> result = ServerAdapter.getDetailsForInstanceCreation(request, offering, null);

        assertFalse(result.containsKey(VmDetailConstants.GUEST_CPU_MODE));
    }

    @Test
    public void testGetDetailsForInstanceCreation_CustomizedOffering_AddsDetails() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        when(offering.isCustomized()).thenReturn(true);
        when(offering.getCpu()).thenReturn(4);
        when(offering.getRamSize()).thenReturn(2048);
        when(offering.getSpeed()).thenReturn(null);

        Vm request = mock(Vm.class);
        when(request.isWorkerVm()).thenReturn(false);

        Map<String, String> result = ServerAdapter.getDetailsForInstanceCreation(request, offering, null);

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

        Vm request = mock(Vm.class);
        when(request.isWorkerVm()).thenReturn(false);

        Map<String, String> result = ServerAdapter.getDetailsForInstanceCreation(request, offering, null);

        assertFalse(result.containsKey(VmDetailConstants.CPU_SPEED));
    }

    @Test
    public void testGetDetailsForInstanceCreation_SkipsBiosAndUefiKeys() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        when(offering.isCustomized()).thenReturn(false);

        Vm request = mock(Vm.class);
        when(request.isWorkerVm()).thenReturn(false);

        Map<String, String> existingDetails = new HashMap<>();
        existingDetails.put("BIOS", "bios_value");
        existingDetails.put("UEFI", "uefi_value");
        existingDetails.put("custom_key", "custom_value");

        Map<String, String> result = ServerAdapter.getDetailsForInstanceCreation(request, offering, existingDetails);

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

        Vm request = mock(Vm.class);
        when(request.isWorkerVm()).thenReturn(false);

        Map<String, String> existingDetails = new HashMap<>();
        existingDetails.put(VmDetailConstants.CPU_SPEED, "3000");

        Map<String, String> result = ServerAdapter.getDetailsForInstanceCreation(request, offering, existingDetails);

        assertEquals("3000", result.get(VmDetailConstants.CPU_SPEED));
    }

    @Test
    public void testGetValidatedInstanceNicDetails_NullVm_ReturnsNullPair() {
        NetworkVO network = mock(NetworkVO.class);

        Pair<String, String> result = serverAdapter.getValidatedInstanceNicDetails(null, network);

        assertNull(result.first());
        assertNull(result.second());
    }

    @Test
    public void testGetValidatedInstanceNicDetails_NullNetwork_ReturnsNullPair() {
        Pair<String, String> result = serverAdapter.getValidatedInstanceNicDetails("config", null);

        assertNull(result.first());
        assertNull(result.second());
    }

    @Test
    public void testGetValidatedInstanceNicDetails_NoRestoreConfig_ReturnsNullPair() {
        NetworkVO network = mock(NetworkVO.class);

        Pair<String, String> result = serverAdapter.getValidatedInstanceNicDetails(null, network);

        assertNull(result.first());
        assertNull(result.second());
    }

    @Test
    public void testGetValidatedInstanceNicDetails_BlankRestoreConfig_ReturnsNullPair() {
        NetworkVO network = mock(NetworkVO.class);

        Pair<String, String> result = serverAdapter.getValidatedInstanceNicDetails("   ", network);

        assertNull(result.first());
        assertNull(result.second());
    }

    @Test
    public void testGetValidatedInstanceNicDetails_BlankMacAndIpFromConfig_ReturnsNullPair() {
        NetworkVO network = mock(NetworkVO.class);
        when(network.getUuid()).thenReturn("network-uuid");

        try (MockedStatic<OvfXmlUtil> ovfXmlUtil = Mockito.mockStatic(OvfXmlUtil.class)) {
            ovfXmlUtil.when(() -> OvfXmlUtil.getVmNicDetailFromStoredConfig(eq("restore-xml"), eq("network-uuid"), any(Logger.class)))
                    .thenReturn(new Pair<>("  ", "\t"));

            Pair<String, String> result = serverAdapter.getValidatedInstanceNicDetails("restore-xml", network);

            assertNull(result.first());
            assertNull(result.second());
        }
    }

    @Test
    public void testGetValidatedInstanceNicDetails_NoConflicts_ReturnsMacAndIp() {
        NetworkVO network = mock(NetworkVO.class);
        when(network.getId()).thenReturn(30L);
        when(network.getUuid()).thenReturn("network-uuid");

        when(nicDao.findByNetworkIdAndMacAddress(30L, "02:00:00:00:00:01")).thenReturn(null);
        when(nicDao.findNonPlaceHolderByIp4AddressAndNetworkId("10.0.0.10", 30L)).thenReturn(null);

        try (MockedStatic<OvfXmlUtil> ovfXmlUtil = Mockito.mockStatic(OvfXmlUtil.class)) {
            ovfXmlUtil.when(() -> OvfXmlUtil.getVmNicDetailFromStoredConfig(eq("restore-xml"), eq("network-uuid"), any(Logger.class)))
                    .thenReturn(new Pair<>("02:00:00:00:00:01", "10.0.0.10"));

            Pair<String, String> result = serverAdapter.getValidatedInstanceNicDetails("restore-xml", network);

            assertEquals("02:00:00:00:00:01", result.first());
            assertEquals("10.0.0.10", result.second());
        }
    }

    @Test
    public void testGetValidatedInstanceNicDetails_MacConflictWithSameIp_ClearsBoth() {
        NetworkVO network = mock(NetworkVO.class);
        when(network.getId()).thenReturn(31L);
        when(network.getUuid()).thenReturn("network-uuid");

        NicVO conflictingNic = mock(NicVO.class);
        when(conflictingNic.getIPv4Address()).thenReturn("10.0.0.11");
        when(nicDao.findByNetworkIdAndMacAddress(31L, "02:00:00:00:00:02")).thenReturn(conflictingNic);

        try (MockedStatic<OvfXmlUtil> ovfXmlUtil = Mockito.mockStatic(OvfXmlUtil.class)) {
            ovfXmlUtil.when(() -> OvfXmlUtil.getVmNicDetailFromStoredConfig(eq("restore-xml"), eq("network-uuid"), any(Logger.class)))
                    .thenReturn(new Pair<>("02:00:00:00:00:02", "10.0.0.11"));

            Pair<String, String> result = serverAdapter.getValidatedInstanceNicDetails("restore-xml", network);

            assertNull(result.first());
            assertNull(result.second());
        }
    }

    @Test
    public void testGetValidatedInstanceNicDetails_MacConflictWithDifferentIp_ClearsOnlyMac() {
        NetworkVO network = mock(NetworkVO.class);
        when(network.getId()).thenReturn(32L);
        when(network.getUuid()).thenReturn("network-uuid");

        NicVO conflictingNic = mock(NicVO.class);
        when(conflictingNic.getIPv4Address()).thenReturn("10.0.0.99");
        when(nicDao.findByNetworkIdAndMacAddress(32L, "02:00:00:00:00:03")).thenReturn(conflictingNic);
        when(nicDao.findNonPlaceHolderByIp4AddressAndNetworkId("10.0.0.12", 32L)).thenReturn(null);

        try (MockedStatic<OvfXmlUtil> ovfXmlUtil = Mockito.mockStatic(OvfXmlUtil.class)) {
            ovfXmlUtil.when(() -> OvfXmlUtil.getVmNicDetailFromStoredConfig(eq("restore-xml"), eq("network-uuid"), any(Logger.class)))
                    .thenReturn(new Pair<>("02:00:00:00:00:03", "10.0.0.12"));

            Pair<String, String> result = serverAdapter.getValidatedInstanceNicDetails("restore-xml", network);

            assertNull(result.first());
            assertEquals("10.0.0.12", result.second());
        }
    }

    @Test
    public void testGetValidatedInstanceNicDetails_IpConflict_ClearsIpAndMac() {
        NetworkVO network = mock(NetworkVO.class);
        when(network.getId()).thenReturn(33L);
        when(network.getUuid()).thenReturn("network-uuid");

        when(nicDao.findByNetworkIdAndMacAddress(33L, "02:00:00:00:00:04")).thenReturn(null);
        NicVO conflictingIpNic = mock(NicVO.class);
        when(conflictingIpNic.getIPv4Address()).thenReturn("10.0.0.13");
        when(nicDao.findNonPlaceHolderByIp4AddressAndNetworkId("10.0.0.13", 33L)).thenReturn(conflictingIpNic);

        try (MockedStatic<OvfXmlUtil> ovfXmlUtil = Mockito.mockStatic(OvfXmlUtil.class)) {
            ovfXmlUtil.when(() -> OvfXmlUtil.getVmNicDetailFromStoredConfig(eq("restore-xml"), eq("network-uuid"), any(Logger.class)))
                    .thenReturn(new Pair<>("02:00:00:00:00:04", "10.0.0.13"));

            Pair<String, String> result = serverAdapter.getValidatedInstanceNicDetails("restore-xml", network);

            assertNull(result.first());
            assertNull(result.second());
        }
    }


    @Test
    public void testGetDummyTags_ContainsRootTag() {
        Map<String, Tag> tags = ServerAdapter.getDummyTags();
        assertNotNull(tags);
        assertFalse(tags.isEmpty());
    }


    @Test
    public void testGetTemplateForInstanceCreation_NullUuid_ReturnsNull() {
        assertNull(serverAdapter.getTemplateForInstanceCreation(null, null, mock(DataCenterVO.class), mock(Account.class)));
        verify(templateDao, never()).findByUuid(any());
    }

    @Test
    public void testGetTemplateForInstanceCreation_BlankUuid_ReturnsNull() {
        assertNull(serverAdapter.getTemplateForInstanceCreation("   ", null, mock(DataCenterVO.class), mock(Account.class)));
        verify(templateDao, never()).findByUuid(any());
    }

    @Test
    public void testGetTemplateForInstanceCreation_TemplateNotFound_ReturnsNull() {
        ServerAdapter spyAdapter = spy(serverAdapter);
        when(templateDao.findByUuid("missing-uuid")).thenReturn(null);
        assertNull(spyAdapter.getTemplateForInstanceCreation("missing-uuid", null, mock(DataCenterVO.class),
                mock(Account.class)));
        verify(spyAdapter, never()).isTemplateUsableForInstanceCreation(any(), any(), any());
    }

    @Test
    public void testGetTemplateForInstanceCreation_TemplateNotUsable_ReturnsNull() {
        ServerAdapter spyAdapter = spy(serverAdapter);
        VMTemplateVO template = mock(VMTemplateVO.class);
        DataCenterVO zone = mock(DataCenterVO.class);
        Account owner = mock(Account.class);
        when(templateDao.findByUuid("valid-uuid")).thenReturn(template);
        doReturn(false).when(spyAdapter).isTemplateUsableForInstanceCreation(template, zone, owner);
        assertNull(spyAdapter.getTemplateForInstanceCreation("valid-uuid", null, zone, owner));
    }

    @Test
    public void testGetTemplateForInstanceCreation_TemplateUsable_ReturnsTemplate() {
        ServerAdapter spyAdapter = spy(serverAdapter);
        VMTemplateVO template = mock(VMTemplateVO.class);
        DataCenterVO zone = mock(DataCenterVO.class);
        Account owner = mock(Account.class);
        when(templateDao.findByUuid("valid-uuid")).thenReturn(template);
        doReturn(true).when(spyAdapter).isTemplateUsableForInstanceCreation(template, zone, owner);
        assertEquals(template, spyAdapter.getTemplateForInstanceCreation("valid-uuid", null, zone, owner));
    }

    private VMTemplateVO mockTemplateForUsabilityCheck(VirtualMachineTemplate.State state) {
        VMTemplateVO template = mock(VMTemplateVO.class);
        when(template.getState()).thenReturn(state);
        Mockito.lenient().when(template.getId()).thenReturn(5L);
        return template;
    }

    private DataCenterVO mockZoneForUsabilityCheck() {
        DataCenterVO zone = mock(DataCenterVO.class);
        Mockito.lenient().when(zone.getId()).thenReturn(1L);
        return zone;
    }

    @Test
    public void testIsTemplateUsableForInstanceCreation_ActiveInZoneAndAccessible_ReturnsTrue() {
        VMTemplateVO template = mockTemplateForUsabilityCheck(VirtualMachineTemplate.State.Active);
        DataCenterVO zone = mockZoneForUsabilityCheck();
        Account owner = mock(Account.class);
        when(templateZoneDao.listByZoneTemplate(1L, 5L)).thenReturn(List.of(mock(VMTemplateZoneVO.class)));

        assertTrue(serverAdapter.isTemplateUsableForInstanceCreation(template, zone, owner));
        verify(accountService).checkAccess(owner, SecurityChecker.AccessType.UseEntry, false, template);
    }

    @Test
    public void testIsTemplateUsableForInstanceCreation_NotActive_ReturnsFalse() {
        for (VirtualMachineTemplate.State state : new VirtualMachineTemplate.State[] {
                VirtualMachineTemplate.State.Inactive, VirtualMachineTemplate.State.NotUploaded,
                VirtualMachineTemplate.State.UploadInProgress, VirtualMachineTemplate.State.UploadError,
                VirtualMachineTemplate.State.UploadAbandoned}) {
            VMTemplateVO template = mockTemplateForUsabilityCheck(state);
            assertFalse("Expected not usable for state " + state, serverAdapter.isTemplateUsableForInstanceCreation(
                    template, mockZoneForUsabilityCheck(), mock(Account.class)));
        }
        verify(templateZoneDao, never()).listByZoneTemplate(any(), Mockito.anyLong());
        verify(accountService, never()).checkAccess(any(Account.class), any(SecurityChecker.AccessType.class),
                Mockito.anyBoolean(), any(VMTemplateVO.class));
    }

    @Test
    public void testIsTemplateUsableForInstanceCreation_NotInZone_ReturnsFalse() {
        VMTemplateVO template = mockTemplateForUsabilityCheck(VirtualMachineTemplate.State.Active);
        DataCenterVO zone = mockZoneForUsabilityCheck();
        when(templateZoneDao.listByZoneTemplate(1L, 5L)).thenReturn(Collections.emptyList());

        assertFalse(serverAdapter.isTemplateUsableForInstanceCreation(template, zone, mock(Account.class)));
        verify(accountService, never()).checkAccess(any(Account.class), any(SecurityChecker.AccessType.class),
                Mockito.anyBoolean(), any(VMTemplateVO.class));
    }

    @Test
    public void testIsTemplateUsableForInstanceCreation_NullZoneRefs_ReturnsFalse() {
        VMTemplateVO template = mockTemplateForUsabilityCheck(VirtualMachineTemplate.State.Active);
        DataCenterVO zone = mockZoneForUsabilityCheck();
        when(templateZoneDao.listByZoneTemplate(1L, 5L)).thenReturn(null);

        assertFalse(serverAdapter.isTemplateUsableForInstanceCreation(template, zone, mock(Account.class)));
    }

    @Test
    public void testIsTemplateUsableForInstanceCreation_NoPermission_ReturnsFalse() {
        VMTemplateVO template = mockTemplateForUsabilityCheck(VirtualMachineTemplate.State.Active);
        DataCenterVO zone = mockZoneForUsabilityCheck();
        Account owner = mock(Account.class);
        when(templateZoneDao.listByZoneTemplate(1L, 5L)).thenReturn(List.of(mock(VMTemplateZoneVO.class)));
        doThrow(new PermissionDeniedException("denied")).when(accountService)
                .checkAccess(owner, SecurityChecker.AccessType.UseEntry, false, template);

        assertFalse(serverAdapter.isTemplateUsableForInstanceCreation(template, zone, owner));
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
    public void testWaitForJobCompletion_JobNotFound_ReturnsNull() {
        when(asyncJobDao.findByIdIncludingRemoved(99L)).thenReturn(null);
        assertNull(serverAdapter.waitForJobCompletion(99L, 60000L));
        verify(asyncJobDao).findByIdIncludingRemoved(99L);
    }

    @Test
    public void testWaitForJobCompletion_JobDone_ReturnsJob() {
        for (AsyncJobVO.Status status : new AsyncJobVO.Status[] {AsyncJobVO.Status.SUCCEEDED,
                AsyncJobVO.Status.FAILED, AsyncJobVO.Status.CANCELLED}) {
            AsyncJobVO job = mock(AsyncJobVO.class);
            when(job.getStatus()).thenReturn(status);
            when(asyncJobDao.findByIdIncludingRemoved(1L)).thenReturn(job);
            assertEquals(job, serverAdapter.waitForJobCompletion(1L, 60000L));
        }
    }

    @Test
    public void testWaitForJobCompletion_DeadlinePassed_ThrowsWithBackgroundCaveat() {
        AsyncJobVO job = mock(AsyncJobVO.class);
        when(job.getStatus()).thenReturn(AsyncJobVO.Status.IN_PROGRESS);
        when(job.getUuid()).thenReturn("job-uuid");
        when(asyncJobDao.findByIdIncludingRemoved(3L)).thenReturn(job);

        AsyncJobFailedException e = assertThrows(AsyncJobFailedException.class, () -> serverAdapter.waitForJobCompletion(3L, 0L));
        assertEquals("job-uuid", e.getJobUuid());
        assertTrue(e.getMessage().contains("Timed out waiting for job job-uuid to complete after 0s"));
        assertTrue(e.getMessage().contains("may still be running in the background"));
    }

    @Test
    public void testWaitForJobCompletion_Interrupted_ThrowsAndKeepsInterruptFlag() {
        AsyncJobVO job = mock(AsyncJobVO.class);
        when(job.getStatus()).thenReturn(AsyncJobVO.Status.IN_PROGRESS);
        when(job.getUuid()).thenReturn("job-uuid");
        when(asyncJobDao.findByIdIncludingRemoved(4L)).thenReturn(job);

        Thread.currentThread().interrupt();
        try {
            AsyncJobFailedException e = assertThrows(AsyncJobFailedException.class, () -> serverAdapter.waitForJobCompletion(4L, 60000L));
            assertTrue(e.getMessage().contains("Interrupted while waiting for job job-uuid"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testGetJobWaitTimeoutMillis_PositiveValueIsUsed() {
        assertEquals(120000L, serverAdapter.getJobWaitTimeoutMillis(120));
    }

    @Test
    public void testGetJobWaitTimeoutMillis_ZeroNegativeOrNullFallsBackToDefault() {
        for (Integer value : new Integer[] {0, -1, null}) {
            assertEquals("Expected default for " + value, 3600000L, serverAdapter.getJobWaitTimeoutMillis(value));
        }
    }

    @Test
    public void testWaitForJobSuccess_NullJobJoinVO_Returns() {
        serverAdapter.waitForJobCompletion(null);
    }

    @Test
    public void testWaitForJobSuccess_JobSucceeded_Returns() {
        AsyncJobJoinVO jobVO = mock(AsyncJobJoinVO.class);
        when(jobVO.getId()).thenReturn(5L);

        AsyncJobVO job = mock(AsyncJobVO.class);
        when(job.getStatus()).thenReturn(AsyncJobVO.Status.SUCCEEDED);
        when(asyncJobDao.findByIdIncludingRemoved(5L)).thenReturn(job);

        serverAdapter.waitForJobCompletion(jobVO);

        verify(asyncJobDao).findByIdIncludingRemoved(5L);
    }

    @Test
    public void testWaitForJobSuccess_JobFailed_ThrowsWithElapsedAndErrorText() {
        AsyncJobJoinVO jobVO = mock(AsyncJobJoinVO.class);
        when(jobVO.getId()).thenReturn(6L);
        when(jobVO.getUuid()).thenReturn("job-uuid");

        AsyncJobVO job = mock(AsyncJobVO.class);
        when(job.getStatus()).thenReturn(AsyncJobVO.Status.FAILED);
        when(job.getResult()).thenReturn("Unable to destroy VM");
        when(asyncJobDao.findByIdIncludingRemoved(6L)).thenReturn(job);

        AsyncJobFailedException e = assertThrows(AsyncJobFailedException.class, () -> serverAdapter.waitForJobCompletion(jobVO));
        assertEquals("job-uuid", e.getJobUuid());
        assertEquals("Job job-uuid failed after 0s: Unable to destroy VM", e.getMessage());
    }

    @Test
    public void testWaitForJobSuccess_JobCancelled_Throws() {
        AsyncJobJoinVO jobVO = mock(AsyncJobJoinVO.class);
        when(jobVO.getId()).thenReturn(7L);
        when(jobVO.getUuid()).thenReturn("job-uuid");

        AsyncJobVO job = mock(AsyncJobVO.class);
        when(job.getStatus()).thenReturn(AsyncJobVO.Status.CANCELLED);
        when(asyncJobDao.findByIdIncludingRemoved(7L)).thenReturn(job);

        AsyncJobFailedException e = assertThrows(AsyncJobFailedException.class, () -> serverAdapter.waitForJobCompletion(jobVO));
        assertEquals("Job job-uuid cancelled after 0s: no error details available", e.getMessage());
    }

    @Test
    public void testGetJobErrorText_ExtractsErrorTextFromSerializedExceptionResponse() {
        ExceptionResponse response = new ExceptionResponse();
        response.setErrorText("Host is unreachable");
        AsyncJobVO job = mock(AsyncJobVO.class);
        when(job.getResult()).thenReturn(ApiSerializerHelper.toSerializedString(response));

        assertEquals("Host is unreachable", ServerAdapter.getJobErrorText(job));
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
        DataCenterVO zone = mock(DataCenterVO.class);

        assertEquals(offering, serverAdapter.getServiceOfferingFromRequest(zone, account, "uuid4", 2, 1024));
    }

    @Test
    public void testGetServiceOfferingFromRequest_Customized_ValidParams_SetsAndReturnsOffering() {
        ServiceOfferingVO offering = mock(ServiceOfferingVO.class);
        Account account = mock(Account.class);
        when(serviceOfferingDao.findByUuid("uuid5")).thenReturn(offering);
        doNothing().when(accountService).checkAccess(eq(account), eq(offering), any());
        when(offering.isCustomized()).thenReturn(true);
        doNothing().when(userVmManager).validateCustomParameters(eq(offering), any());
        DataCenterVO zone = mock(DataCenterVO.class);

        ServiceOfferingVO result = serverAdapter.getServiceOfferingFromRequest(zone, account, "uuid5", 2, 1024);

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
        when(dataCenterJoinDao.listByIds(anyList(), any())).thenReturn(List.of(vo));

        List<DataCenter> result = serverAdapter.listAllDataCenters(0L, 10L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testListAllDataCenters_EmptyList_ReturnsEmpty() {
        when(dataCenterJoinDao.listByIds(anyList(), any())).thenReturn(Collections.emptyList());

        assertTrue(serverAdapter.listAllDataCenters(0L, 10L).isEmpty());
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testListStorageDomainsByDcId_DataCenterNotFound_Throws() {
        when(dataCenterDao.findByUuid("dc-uuid")).thenReturn(null);
        serverAdapter.listStorageDomainsByDcId("dc-uuid", null, 0L, 10L);
    }

    @Test
    public void testListStorageDomainsByDcId_Found_ReturnsList() {
        DataCenterVO dcVO = mock(DataCenterVO.class);
        when(dcVO.getId()).thenReturn(1L);
        when(dataCenterDao.findByUuid("dc-uuid")).thenReturn(dcVO);
        StoragePoolJoinVO poolVO = mock(StoragePoolJoinVO.class);
        when(storagePoolJoinDao.listAvailableByZonesHypervisorAndType(eq(List.of(1L)), eq(Hypervisor.HypervisorType.KVM), any(), eq("data%"), any())).thenReturn(List.of(poolVO));

        assertEquals(1, serverAdapter.listStorageDomainsByDcId("dc-uuid", "data%", 0L, 10L).size());
    }

    @Test
    public void testListAllStorageDomains_UsesCompatibleDataCenters() {
        when(kvmBackupExportService.listCompatibleDataCenterIds()).thenReturn(List.of(1L, 2L));
        StoragePoolJoinVO poolVO = mock(StoragePoolJoinVO.class);
        when(poolVO.getUuid()).thenReturn("pool-uuid");
        when(storagePoolJoinDao.listAvailableByZonesHypervisorAndType(eq(List.of(1L, 2L)), eq(Hypervisor.HypervisorType.KVM), any(), isNull(), any())).thenReturn(List.of(poolVO));

        List<StorageDomain> result = serverAdapter.listAllStorageDomains(null, 0L, 10L);

        assertEquals(1, result.size());
        assertEquals("pool-uuid", result.get(0).getId());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetStorageDomain_NotFound_Throws() {
        when(primaryDataStoreDao.findByUuid("pool-uuid")).thenReturn(null);
        serverAdapter.getStorageDomain("pool-uuid");
    }

    @Test
    public void testGetStorageDomain_Found_ReturnsDomain() {
        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(primaryDataStoreDao.findByUuid("pool-uuid")).thenReturn(pool);
        StoragePoolJoinVO poolVO = mock(StoragePoolJoinVO.class);
        when(poolVO.getUuid()).thenReturn("pool-uuid");
        when(storagePoolJoinDao.newStoragePoolView(pool)).thenReturn(List.of(poolVO));

        assertEquals("pool-uuid", serverAdapter.getStorageDomain("pool-uuid").getId());
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
        when(clusterDao.listByZonesAndHypervisorType(anyList(), any(), any())).thenReturn(Collections.emptyList());
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
        when(hostJoinDao.listAvailableRoutingByZonesAndHypervisor(anyList(), any(), any())).thenReturn(List.of(hostVO));

        List<Host> hosts = serverAdapter.listAllHosts(false, 0L, 10L);

        assertEquals(1, hosts.size());
        assertNull(hosts.get(0).getStatistics());
        verify(statsCollector, never()).getHostStats(anyLong());
    }

    @Test
    public void testListAllHosts_WithStatistics_IncludesStatistics() {
        HostJoinVO hostVO = mock(HostJoinVO.class);
        when(hostVO.getId()).thenReturn(7L);
        when(hostVO.getStatus()).thenReturn(Status.Up);
        HostStats hostStats = mock(HostStats.class);
        when(hostStats.getTotalMemoryKBs()).thenReturn(1024D);
        when(statsCollector.getHostStats(7L)).thenReturn(hostStats);
        when(hostJoinDao.listAvailableRoutingByZonesAndHypervisor(anyList(), any(), any())).thenReturn(List.of(hostVO));

        List<Host> hosts = serverAdapter.listAllHosts(true, 0L, 10L);

        assertEquals(1, hosts.size());
        assertEquals(4, hosts.get(0).getStatistics().getItems().size());
        verify(statsCollector).getHostStats(7L);
    }


    @Test(expected = InvalidParameterValueException.class)
    public void testGetHost_NotFound_Throws() {
        when(hostJoinDao.findByUuid("host-uuid")).thenReturn(null);
        serverAdapter.getHost("host-uuid", false);
    }

    @Test
    public void testGetHost_Found_ReturnsHost() {
        HostJoinVO vo = mock(HostJoinVO.class);
        when(hostJoinDao.findByUuid("host-uuid")).thenReturn(vo);

        Host host = serverAdapter.getHost("host-uuid", false);

        assertNotNull(host);
        assertNull(host.getStatistics());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListHostStatistics_NotFound_Throws() {
        when(hostJoinDao.findByUuid("host-uuid")).thenReturn(null);
        serverAdapter.listHostStatistics("host-uuid");
    }

    @Test
    public void testListHostStatistics_NoStats_ReturnsTotalMemoryOnly() {
        HostJoinVO vo = mock(HostJoinVO.class);
        when(vo.getId()).thenReturn(7L);
        when(vo.getTotalMemory()).thenReturn(2048L);
        when(hostJoinDao.findByUuid("host-uuid")).thenReturn(vo);
        when(statsCollector.getHostStats(7L)).thenReturn(null);

        List<Statistic> statistics = serverAdapter.listHostStatistics("host-uuid");

        assertEquals(1, statistics.size());
        assertEquals("memory.total", statistics.get(0).getName());
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


    @Test
    public void testInstanceExists() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(mock(UserVmVO.class));
        when(userVmDao.findByUuid("vm-missing")).thenReturn(null);
        assertTrue(serverAdapter.instanceExists("vm-uuid"));
        assertFalse(serverAdapter.instanceExists("vm-missing"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteInstance_VmNotFound_Throws() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.deleteInstance("vm-uuid", true, false);
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
        serverAdapter.deleteDisk("vol-uuid", false);
    }

    private ServerAdapter spyAdapterForDiskDeletion(AsyncJobJoinVO jobVo) throws Exception {
        VolumeVO vo = mock(VolumeVO.class);
        when(vo.getUuid()).thenReturn("vol-uuid");
        when(volumeDao.findByUuid("vol-uuid")).thenReturn(vo);
        when(asyncJobJoinDao.findById(7L)).thenReturn(jobVo);
        ServerAdapter adapter = Mockito.spy(serverAdapter);
        doReturn(new ApiServerService.AsyncCmdResult(null, null, null, 7L))
                .when(adapter).processAsyncCmdWithContext(any(), any());
        return adapter;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteDisk_Sync_WaitsForJobAndReturnsAction() throws Exception {
        AsyncJobJoinVO jobVo = mock(AsyncJobJoinVO.class);
        when(jobVo.getUuid()).thenReturn("job-uuid");
        ServerAdapter adapter = spyAdapterForDiskDeletion(jobVo);
        doNothing().when(adapter).waitForJobCompletion(any(AsyncJobJoinVO.class));

        ResourceAction action;
        try (MockedStatic<ComponentContext> ignored = Mockito.mockStatic(ComponentContext.class)) {
            action = adapter.deleteDisk("vol-uuid", false);
        }

        ArgumentCaptor<BaseAsyncCmd> cmdCaptor = ArgumentCaptor.forClass(BaseAsyncCmd.class);
        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(adapter).processAsyncCmdWithContext(cmdCaptor.capture(), paramsCaptor.capture());
        assertEquals(DestroyVolumeCmd.class, cmdCaptor.getValue().getClass());
        assertEquals("vol-uuid", paramsCaptor.getValue().get(ApiConstants.ID));
        assertEquals("true", paramsCaptor.getValue().get(ApiConstants.EXPUNGE));
        verify(adapter).waitForJobCompletion(jobVo);
        assertEquals("complete", action.getStatus());
        assertEquals("job-uuid", action.getJob().getId());
    }

    @Test
    public void testDeleteDisk_Async_DoesNotWaitForJob() throws Exception {
        AsyncJobJoinVO jobVo = mock(AsyncJobJoinVO.class);
        when(jobVo.getUuid()).thenReturn("job-uuid");
        ServerAdapter adapter = spyAdapterForDiskDeletion(jobVo);

        try (MockedStatic<ComponentContext> ignored = Mockito.mockStatic(ComponentContext.class)) {
            assertEquals("job-uuid", adapter.deleteDisk("vol-uuid", true).getJob().getId());
        }

        verify(adapter, Mockito.never()).waitForJobCompletion(any(AsyncJobJoinVO.class));
    }

    @Test
    public void testDeleteDisk_JobFailure_ThrowsWithJobFailureCause() throws Exception {
        AsyncJobJoinVO jobVo = mock(AsyncJobJoinVO.class);
        ServerAdapter adapter = spyAdapterForDiskDeletion(jobVo);
        Mockito.doThrow(new AsyncJobFailedException("job-uuid", "volume in use"))
                .when(adapter).waitForJobCompletion(any(AsyncJobJoinVO.class));

        try (MockedStatic<ComponentContext> ignored = Mockito.mockStatic(ComponentContext.class)) {
            CloudRuntimeException e = assertThrows(CloudRuntimeException.class, () -> adapter.deleteDisk("vol-uuid", false));
            assertTrue(e.getCause() instanceof AsyncJobFailedException);
        }
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

    @Test(expected = InvalidParameterValueException.class)
    public void testGetInstanceNic_VmNotFound_Throws() {
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(null);
        serverAdapter.getInstanceNic("vm-uuid", "nic-uuid");
    }

    @Test
    public void testGetInstanceNic_NicNotFoundOrOfAnotherVm_Throws() {
        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getId()).thenReturn(10L);
        when(vm.getUuid()).thenReturn("vm-uuid");
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(vm);
        NicVO otherVmNic = mock(NicVO.class);
        when(otherVmNic.getInstanceId()).thenReturn(11L);
        when(nicDao.findByUuid("other-nic")).thenReturn(otherVmNic);

        InvalidParameterValueException e = assertThrows(InvalidParameterValueException.class,
                () -> serverAdapter.getInstanceNic("vm-uuid", "missing-nic"));
        assertEquals("NIC with ID missing-nic not found for VM with ID vm-uuid", e.getMessage());
        assertThrows(InvalidParameterValueException.class, () -> serverAdapter.getInstanceNic("vm-uuid", "other-nic"));
    }

    @Test
    public void testGetInstanceNic_ReturnsNic() {
        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getId()).thenReturn(10L);
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(vm);
        NicVO nic = mock(NicVO.class);
        when(nic.getInstanceId()).thenReturn(10L);
        when(nic.getUuid()).thenReturn("nic-uuid");
        when(nicDao.findByUuid("nic-uuid")).thenReturn(nic);

        Nic result = serverAdapter.getInstanceNic("vm-uuid", "nic-uuid");

        assertEquals("nic-uuid", result.getId());
        assertTrue(result.getHref().endsWith("/api/vms/vm-uuid/nics/nic-uuid"));
    }

    @Test
    public void testDetachInstanceNic_RemovesNicFromVm() {
        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getId()).thenReturn(10L);
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(vm);
        NicVO nic = mock(NicVO.class);
        when(nic.getId()).thenReturn(20L);
        when(nic.getInstanceId()).thenReturn(10L);
        when(nicDao.findByUuid("nic-uuid")).thenReturn(nic);

        try (MockedStatic<ComponentContext> ignored = Mockito.mockStatic(ComponentContext.class)) {
            serverAdapter.detachInstanceNic("vm-uuid", "nic-uuid");
        }

        ArgumentCaptor<RemoveNicFromVMCmd> captor = ArgumentCaptor.forClass(RemoveNicFromVMCmd.class);
        verify(userVmManager).removeNicFromVirtualMachine(captor.capture());
        assertEquals(Long.valueOf(10L), captor.getValue().getVmId());
        assertEquals(Long.valueOf(20L), captor.getValue().getNicId());
    }

    @Test
    public void testDetachInstanceNic_NicNotFound_ThrowsWithoutRemoving() {
        UserVmVO vm = mock(UserVmVO.class);
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(vm);

        assertThrows(InvalidParameterValueException.class, () -> serverAdapter.detachInstanceNic("vm-uuid", "nic-uuid"));
        verify(userVmManager, Mockito.never()).removeNicFromVirtualMachine(any());
    }

    @Test
    public void testDetachInstanceNic_RemoveFailure_ThrowsCloudRuntimeException() {
        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getId()).thenReturn(10L);
        when(userVmDao.findByUuid("vm-uuid")).thenReturn(vm);
        NicVO nic = mock(NicVO.class);
        when(nic.getInstanceId()).thenReturn(10L);
        when(nicDao.findByUuid("nic-uuid")).thenReturn(nic);
        when(userVmManager.removeNicFromVirtualMachine(any()))
                .thenThrow(new InvalidParameterValueException("Unable to remove nic from VM, it is the default nic"));

        try (MockedStatic<ComponentContext> ignored = Mockito.mockStatic(ComponentContext.class)) {
            CloudRuntimeException e = assertThrows(CloudRuntimeException.class,
                    () -> serverAdapter.detachInstanceNic("vm-uuid", "nic-uuid"));
            assertEquals(CloudRuntimeException.class, e.getClass());
            assertEquals("Failed to remove NIC: Unable to remove nic from VM, it is the default nic", e.getMessage());
        }
    }
}
