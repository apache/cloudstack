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
package org.apache.cloudstack.backup;

import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.alert.AlertManager;
import com.cloud.capacity.CapacityVO;
import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.network.NetworkService;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.DiskOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDiskInfo;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.Gson;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.backup.ImportBackupOfferingCmd;
import org.apache.cloudstack.api.command.admin.backup.UpdateBackupOfferingCmd;
import org.apache.cloudstack.api.command.user.backup.CreateBackupCmd;
import org.apache.cloudstack.api.command.user.backup.CreateBackupScheduleCmd;
import org.apache.cloudstack.api.command.user.backup.DeleteBackupScheduleCmd;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupDetailsDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.BackupScheduleDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;

@RunWith(MockitoJUnitRunner.class)
public class BackupManagerTest {
    @Spy
    @InjectMocks
    BackupManagerImpl backupManager = new BackupManagerImpl();

    @Mock
    BackupOfferingDao backupOfferingDao;

    @Mock
    BackupDetailsDao backupDetailsDao;

    @Mock
    BackupProvider backupProvider;

    @Mock
    VirtualMachineManager virtualMachineManager;

    @Mock
    VolumeApiService volumeApiService;

    @Mock
    VolumeDao volumeDao;

    @Mock
    VMInstanceDao vmInstanceDao;

    @Mock
    AccountManager accountManager;

    @Mock
    DomainManager domainManager;

    @Mock
    ResourceLimitService resourceLimitMgr;

    @Mock
    BackupScheduleDao backupScheduleDao;

    @Mock
    BackupDao backupDao;

    @Mock
    DataCenterDao dataCenterDao;

    @Mock
    private AlertManager alertManagerMock;

    @Mock
    private Domain domainMock;

    @Mock
    private VMInstanceVO vmInstanceVOMock;

    @Mock
    private CreateBackupScheduleCmd createBackupScheduleCmdMock;

    @Mock
    private BackupOfferingVO backupOfferingVOMock;

    @Mock
    private AsyncJobVO asyncJobVOMock;

    @Mock
    private BackupScheduleVO backupScheduleVOMock;

    @Mock
    private AccountVO accountVOMock;

    @Mock
    private CallContext callContextMock;

    @Mock
    private DeleteBackupScheduleCmd deleteBackupScheduleCmdMock;

    @Mock
    DiskOfferingDao diskOfferingDao;

    @Mock
    ServiceOfferingDao serviceOfferingDao;

    @Mock
    VMTemplateDao vmTemplateDao;

    @Mock
    UserVmJoinDao userVmJoinDao;

    @Mock
    PrimaryDataStoreDao primaryDataStoreDao;

    @Mock
    HostDao hostDao;

    @Mock
    private NetworkDao networkDao;

    @Mock
    private NetworkService networkService;

    @Mock
    private VMInstanceDetailsDao vmInstanceDetailsDao;

    @Mock
    AccountDao accountDao;

    @Mock
    DomainDao domainDao;

    private Gson gson;

    private String[] hostPossibleValues = {"127.0.0.1", "hostname"};
    private String[] datastoresPossibleValues = {"e9804933-8609-4de3-bccc-6278072a496c", "datastore-name"};
    private AutoCloseable closeable;
    private ConfigDepotImpl configDepotImpl;
    private boolean updatedConfigKeyDepot = false;

    @Before
    public void setup() throws Exception {
        gson = new Gson();

        closeable = MockitoAnnotations.openMocks(this);
        when(backupOfferingDao.findById(null)).thenReturn(null);
        when(backupOfferingDao.findById(123l)).thenReturn(null);

        BackupOfferingVO offering = Mockito.spy(BackupOfferingVO.class);
        when(offering.getName()).thenCallRealMethod();
        when(offering.getDescription()).thenCallRealMethod();
        when(offering.isUserDrivenBackupAllowed()).thenCallRealMethod();

        BackupOfferingVO offeringUpdate = Mockito.spy(BackupOfferingVO.class);

        when(backupOfferingDao.findById(1234l)).thenReturn(offering);
        when(backupOfferingDao.createForUpdate(1234l)).thenReturn(offeringUpdate);
        when(backupOfferingDao.update(1234l, offeringUpdate)).thenAnswer(answer -> {
            offering.setName("New name");
            offering.setDescription("New description");
            offering.setUserDrivenBackupAllowed(true);
            return true;
        });

        backupProvider = mock(BackupProvider.class);
        when(backupProvider.getName()).thenReturn("testbackupprovider");
        Map<String, BackupProvider> backupProvidersMap = new HashMap<>();
        backupProvidersMap.put(backupProvider.getName().toLowerCase(), backupProvider);
        ReflectionTestUtils.setField(backupManager, "backupProvidersMap", backupProvidersMap);

        Account account = mock(Account.class);
        User user = mock(User.class);
        CallContext.register(user, account);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
        if (updatedConfigKeyDepot) {
            ReflectionTestUtils.setField(BackupManager.BackupFrameworkEnabled, "s_depot", configDepotImpl);
        }
        CallContext.unregister();
    }

    private void overrideBackupFrameworkConfigValue() {
        ConfigKey configKey = BackupManager.BackupFrameworkEnabled;
        this.configDepotImpl = (ConfigDepotImpl) ReflectionTestUtils.getField(configKey, "s_depot");
        ConfigDepotImpl configDepot = Mockito.mock(ConfigDepotImpl.class);
        Mockito.when(configDepot.getConfigStringValue(Mockito.eq(BackupManager.BackupFrameworkEnabled.key()),
                Mockito.eq(ConfigKey.Scope.Global), Mockito.isNull())).thenReturn("true");
        Mockito.when(configDepot.getConfigStringValue(Mockito.eq(BackupManager.BackupFrameworkEnabled.key()),
                Mockito.eq(ConfigKey.Scope.Zone), Mockito.anyLong())).thenReturn("true");
        Mockito.when(configDepot.getConfigStringValue(Mockito.eq(BackupManager.BackupProviderPlugin.key()),
                Mockito.eq(ConfigKey.Scope.Zone), Mockito.anyLong())).thenReturn("testbackupprovider");
        ReflectionTestUtils.setField(configKey, "s_depot", configDepot);
        updatedConfigKeyDepot = true;
    }

    @Test
    public void testExceptionWhenUpdateWithNullId() {
        try {
            Long id = null;

            UpdateBackupOfferingCmd cmd = Mockito.spy(UpdateBackupOfferingCmd.class);
            when(cmd.getId()).thenReturn(id);

            backupManager.updateBackupOffering(cmd);
        } catch (InvalidParameterValueException e) {
            assertEquals("Unable to find Backup Offering with id: [null].", e.getMessage());
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testExceptionWhenUpdateWithNonExistentId() {
        Long id = 123l;

        UpdateBackupOfferingCmd cmd = Mockito.spy(UpdateBackupOfferingCmd.class);
        when(cmd.getId()).thenReturn(id);

        backupManager.updateBackupOffering(cmd);
    }

    @Test(expected = ServerApiException.class)
    public void testExceptionWhenUpdateWithoutChanges() {
        UpdateBackupOfferingCmd cmd = Mockito.spy(UpdateBackupOfferingCmd.class);
        when(cmd.getName()).thenReturn(null);
        when(cmd.getDescription()).thenReturn(null);
        when(cmd.getAllowUserDrivenBackups()).thenReturn(null);

        Mockito.doCallRealMethod().when(cmd).execute();

        cmd.execute();
    }

    @Test
    public void testUpdateBackupOfferingSuccess() {
        Long id = 1234l;

        UpdateBackupOfferingCmd cmd = Mockito.spy(UpdateBackupOfferingCmd.class);
        when(cmd.getId()).thenReturn(id);
        when(cmd.getName()).thenReturn("New name");
        when(cmd.getDescription()).thenReturn("New description");
        when(cmd.getAllowUserDrivenBackups()).thenReturn(true);

        BackupOffering updated = backupManager.updateBackupOffering(cmd);
        assertEquals("New name", updated.getName());
        assertEquals("New description", updated.getDescription());
        assertEquals(true, updated.isUserDrivenBackupAllowed());
    }

    @Test
    public void restoreBackedUpVolumeTestHostIpAndDatastoreUuid() {
        BackupVO backupVO = new BackupVO();
        VMInstanceVO vm = mock(VMInstanceVO.class);
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";
        String vmName = "i-2-3-VM";
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Mockito.when(vm.getName()).thenReturn(vmName);
        Mockito.when(vm.getState()).thenReturn(vmState);
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>(vmName, vmState);

        Backup.VolumeInfo volumeInfo = mock(Backup.VolumeInfo.class);
        when(volumeInfo.getUuid()).thenReturn(volumeUuid);

        doReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success"))
            .when(backupProvider).restoreBackedUpVolume(any(Backup.class), any(Backup.VolumeInfo.class),
                any(String.class), any(String.class), any(Pair.class));

        Pair<Boolean, String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeInfo, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success", restoreBackedUpVolume.second());

        verify(backupProvider, atLeastOnce()).restoreBackedUpVolume(any(Backup.class), any(Backup.VolumeInfo.class),
            any(String.class), any(String.class), any(Pair.class));
    }

    @Test
    public void restoreBackedUpVolumeTestHostIpAndDatastoreName() {
        BackupVO backupVO = new BackupVO();
        VMInstanceVO vm = mock(VMInstanceVO.class);
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";
        String vmName = "i-2-3-VM";
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Mockito.when(vm.getName()).thenReturn(vmName);
        Mockito.when(vm.getState()).thenReturn(vmState);
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>(vmName, vmState);

        Backup.VolumeInfo volumeInfo = mock(Backup.VolumeInfo.class);
        when(volumeInfo.getUuid()).thenReturn(volumeUuid);

        doReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success2"))
            .when(backupProvider).restoreBackedUpVolume(any(Backup.class), any(Backup.VolumeInfo.class),
                any(String.class), any(String.class), any(Pair.class));

        Pair<Boolean, String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeInfo, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success2", restoreBackedUpVolume.second());

        verify(backupProvider, atLeastOnce()).restoreBackedUpVolume(any(Backup.class), any(Backup.VolumeInfo.class),
            any(String.class), any(String.class), any(Pair.class));
    }

    @Test
    public void restoreBackedUpVolumeTestHostNameAndDatastoreUuid() {
        BackupVO backupVO = new BackupVO();
        VMInstanceVO vm = mock(VMInstanceVO.class);
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";
        String vmName = "i-2-3-VM";
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Mockito.when(vm.getName()).thenReturn(vmName);
        Mockito.when(vm.getState()).thenReturn(vmState);
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>(vmName, vmState);

        Backup.VolumeInfo volumeInfo = mock(Backup.VolumeInfo.class);
        when(volumeInfo.getUuid()).thenReturn(volumeUuid);

        doReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success3"))
            .when(backupProvider).restoreBackedUpVolume(any(Backup.class), any(Backup.VolumeInfo.class),
                any(String.class), any(String.class), any(Pair.class));

        Pair<Boolean, String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeInfo, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success3", restoreBackedUpVolume.second());

        verify(backupProvider, atLeastOnce()).restoreBackedUpVolume(any(Backup.class), any(Backup.VolumeInfo.class),
            any(String.class), any(String.class), any(Pair.class));
    }

    @Test
    public void restoreBackedUpVolumeTestHostAndDatastoreName() {
        BackupVO backupVO = new BackupVO();
        VMInstanceVO vm = mock(VMInstanceVO.class);
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";
        String vmName = "i-2-3-VM";
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Mockito.when(vm.getName()).thenReturn(vmName);
        Mockito.when(vm.getState()).thenReturn(vmState);
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>(vmName, vmState);

        Backup.VolumeInfo volumeInfo = mock(Backup.VolumeInfo.class);
        when(volumeInfo.getUuid()).thenReturn(volumeUuid);

        doReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success4"))
            .when(backupProvider).restoreBackedUpVolume(any(Backup.class), any(Backup.VolumeInfo.class),
                any(String.class), any(String.class), any(Pair.class));

        Pair<Boolean, String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeInfo, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success4", restoreBackedUpVolume.second());

        verify(backupProvider, atLeastOnce()).restoreBackedUpVolume(any(Backup.class), any(Backup.VolumeInfo.class),
            any(String.class), any(String.class), any(Pair.class));
    }

    @Test
    public void tryRestoreVMTestRestoreSucceeded() throws NoTransitionException {
        BackupOffering offering = mock(BackupOffering.class);
        VolumeVO volumeVO = mock(VolumeVO.class);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        BackupVO backup = mock(BackupVO.class);

        try (MockedStatic<ActionEventUtils> utils = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onStartedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.eq(true), Mockito.eq(0))).thenReturn(1L);
            Mockito.when(ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.eq(0))).thenReturn(2L);

            Mockito.when(volumeDao.findIncludingRemovedByInstanceAndType(1L, null)).thenReturn(Collections.singletonList(volumeVO));
            Mockito.when(virtualMachineManager.stateTransitTo(Mockito.eq(vm), Mockito.eq(VirtualMachine.Event.RestoringRequested), Mockito.any())).thenReturn(true);
            Mockito.when(volumeApiService.stateTransitTo(Mockito.eq(volumeVO), Mockito.eq(Volume.Event.RestoreRequested))).thenReturn(true);

            Mockito.when(vm.getId()).thenReturn(1L);
            Mockito.when(offering.getProvider()).thenReturn("veeam");
            Mockito.doReturn(backupProvider).when(backupManager).getBackupProvider("veeam");
            Mockito.when(backupProvider.restoreVMFromBackup(vm, backup)).thenReturn(true);

            backupManager.tryRestoreVM(backup, vm, offering, "Nothing to write here.");
        }
    }

    @Test
    public void tryRestoreVMTestRestoreFails() throws NoTransitionException {
        BackupOffering offering = mock(BackupOffering.class);
        VolumeVO volumeVO = mock(VolumeVO.class);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        BackupVO backup = mock(BackupVO.class);

        try (MockedStatic<ActionEventUtils> utils = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onStartedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.eq(true), Mockito.eq(0))).thenReturn(1L);
            Mockito.when(ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.eq(0))).thenReturn(2L);

            Mockito.when(volumeDao.findIncludingRemovedByInstanceAndType(1L, null)).thenReturn(Collections.singletonList(volumeVO));
            Mockito.when(virtualMachineManager.stateTransitTo(Mockito.eq(vm), Mockito.eq(VirtualMachine.Event.RestoringRequested), Mockito.any())).thenReturn(true);
            Mockito.when(volumeApiService.stateTransitTo(Mockito.eq(volumeVO), Mockito.eq(Volume.Event.RestoreRequested))).thenReturn(true);
            Mockito.when(virtualMachineManager.stateTransitTo(Mockito.eq(vm), Mockito.eq(VirtualMachine.Event.RestoringFailed), Mockito.any())).thenReturn(true);
            Mockito.when(volumeApiService.stateTransitTo(Mockito.eq(volumeVO), Mockito.eq(Volume.Event.RestoreFailed))).thenReturn(true);

            Mockito.when(vm.getId()).thenReturn(1L);
            Mockito.when(offering.getProvider()).thenReturn("veeam");
            Mockito.doReturn(backupProvider).when(backupManager).getBackupProvider("veeam");
            Mockito.when(backupProvider.restoreVMFromBackup(vm, backup)).thenReturn(false);
            try {
                backupManager.tryRestoreVM(backup, vm, offering, "Checking message error.");
                fail("An exception is needed.");
            } catch (CloudRuntimeException e) {
                assertEquals("Error restoring VM from backup [Checking message error.].", e.getMessage());
            }
        }
    }

    @Test
    public void testConfigureBackupSchedule() {
        Long vmId = 1L;
        Long zoneId = 2L;
        Long accountId = 3L;
        Long domainId = 4L;
        Long backupOfferingId = 5L;

        CreateBackupScheduleCmd cmd = Mockito.mock(CreateBackupScheduleCmd.class);
        when(cmd.getVmId()).thenReturn(vmId);
        when(cmd.getTimezone()).thenReturn("GMT");
        when(cmd.getIntervalType()).thenReturn(DateUtil.IntervalType.DAILY);
        when(cmd.getMaxBackups()).thenReturn(8);
        when(cmd.getSchedule()).thenReturn("00:00:00");
        when(cmd.getQuiesceVM()).thenReturn(null);

        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        when(vm.getDataCenterId()).thenReturn(zoneId);
        when(vm.getAccountId()).thenReturn(accountId);
        when(vm.getBackupOfferingId()).thenReturn(backupOfferingId);

        overrideBackupFrameworkConfigValue();

        Account account = Mockito.mock(Account.class);
        when(accountManager.getAccount(accountId)).thenReturn(account);
        when(account.getDomainId()).thenReturn(domainId);
        Domain domain = Mockito.mock(Domain.class);
        when(domainManager.getDomain(domainId)).thenReturn(domain);
        when(resourceLimitMgr.findCorrectResourceLimitForAccount(account, Resource.ResourceType.backup, null)).thenReturn(8L);
        when(resourceLimitMgr.findCorrectResourceLimitForDomain(domain, Resource.ResourceType.backup, null)).thenReturn(8L);

        BackupOfferingVO offering = Mockito.mock(BackupOfferingVO.class);
        when(backupOfferingDao.findById(backupOfferingId)).thenReturn(offering);
        when(offering.isUserDrivenBackupAllowed()).thenReturn(true);
        when(offering.getProvider()).thenReturn("test");

        BackupScheduleVO schedule = mock(BackupScheduleVO.class);
        when(backupScheduleDao.findByVMAndIntervalType(vmId, DateUtil.IntervalType.DAILY)).thenReturn(schedule);

        backupManager.configureBackupSchedule(cmd);

        verify(schedule, times(1)).setScheduleType((short) DateUtil.IntervalType.DAILY.ordinal());
        verify(schedule, times(1)).setSchedule("00:00:00");
        verify(schedule, times(1)).setTimezone(TimeZone.getTimeZone("GMT").getID());
        verify(schedule, times(1)).setMaxBackups(8);
    }

    @Test
    public void configureBackupScheduleTestEnsureLimitCheckIsPerformed() {
        long vmId = 1L;
        long zoneId = 2L;
        long accountId = 3L;
        long domainId = 4L;
        long backupOfferingId = 5L;

        when(createBackupScheduleCmdMock.getVmId()).thenReturn(vmId);
        when(createBackupScheduleCmdMock.getTimezone()).thenReturn("GMT");
        when(createBackupScheduleCmdMock.getIntervalType()).thenReturn(DateUtil.IntervalType.DAILY);
        when(createBackupScheduleCmdMock.getMaxBackups()).thenReturn(8);

        when(vmInstanceDao.findById(vmId)).thenReturn(vmInstanceVOMock);
        when(vmInstanceVOMock.getDataCenterId()).thenReturn(zoneId);
        when(vmInstanceVOMock.getAccountId()).thenReturn(accountId);
        when(vmInstanceVOMock.getBackupOfferingId()).thenReturn(backupOfferingId);

        when(backupOfferingDao.findById(backupOfferingId)).thenReturn(backupOfferingVOMock);
        when(backupOfferingVOMock.isUserDrivenBackupAllowed()).thenReturn(true);

        overrideBackupFrameworkConfigValue();

        when(accountManager.getAccount(accountId)).thenReturn(accountVOMock);
        when(accountVOMock.getDomainId()).thenReturn(domainId);
        when(domainManager.getDomain(domainId)).thenReturn(domainMock);
        when(resourceLimitMgr.findCorrectResourceLimitForAccount(accountVOMock, Resource.ResourceType.backup, null)).thenReturn(10L);
        when(resourceLimitMgr.findCorrectResourceLimitForDomain(domainMock, Resource.ResourceType.backup, null)).thenReturn(1L);

        InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class,
                () -> backupManager.configureBackupSchedule(createBackupScheduleCmdMock));
        Assert.assertEquals("'maxbackups' should not exceed the domain/account backup limit.", exception.getMessage());
    }

    @Test
    public void createBackupTestCreateScheduledBackup() throws ResourceAllocationException {
        Long vmId = 1L;
        Long zoneId = 2L;
        Long scheduleId = 3L;
        Long backupOfferingId = 4L;
        Long accountId = 5L;
        Long backupId = 6L;
        Long oldestBackupId = 7L;
        Long newBackupSize = 1000000000L;
        Long oldBackupSize = 400000000L;

        when(vmInstanceDao.findById(vmId)).thenReturn(vmInstanceVOMock);
        when(vmInstanceVOMock.getDataCenterId()).thenReturn(zoneId);
        when(vmInstanceVOMock.getBackupOfferingId()).thenReturn(backupOfferingId);
        when(vmInstanceVOMock.getAccountId()).thenReturn(accountId);

        overrideBackupFrameworkConfigValue();
        when(backupOfferingDao.findById(backupOfferingId)).thenReturn(backupOfferingVOMock);
        when(backupOfferingVOMock.isUserDrivenBackupAllowed()).thenReturn(true);
        when(backupOfferingVOMock.getProvider()).thenReturn("testbackupprovider");

        Mockito.doReturn(scheduleId).when(backupManager).getBackupScheduleId(asyncJobVOMock);

        when(accountManager.getAccount(accountId)).thenReturn(accountVOMock);

        BackupScheduleVO schedule = mock(BackupScheduleVO.class);
        when(backupScheduleDao.findById(scheduleId)).thenReturn(schedule);
        when(schedule.getMaxBackups()).thenReturn(2);

        VolumeVO volume = mock(VolumeVO.class);
        when(volumeDao.findByInstance(vmId)).thenReturn(List.of(volume));
        when(volume.getState()).thenReturn(Volume.State.Ready);
        when(volumeApiService.getVolumePhysicalSize(null, null, null)).thenReturn(newBackupSize);

        BackupProvider backupProvider = mock(BackupProvider.class);
        Backup backup = mock(Backup.class);
        when(backup.getId()).thenReturn(backupId);
        when(backup.getSize()).thenReturn(newBackupSize);
        when(backupProvider.getName()).thenReturn("testbackupprovider");
        when(backupProvider.takeBackup(vmInstanceVOMock, null)).thenReturn(new Pair<>(true, backup));
        Map<String, BackupProvider> backupProvidersMap = new HashMap<>();
        backupProvidersMap.put(backupProvider.getName().toLowerCase(), backupProvider);
        ReflectionTestUtils.setField(backupManager, "backupProvidersMap", backupProvidersMap);

        BackupVO backupVO = mock(BackupVO.class);
        when(backupVO.getId()).thenReturn(backupId);
        BackupVO oldestBackupVO = mock(BackupVO.class);

        when(backupDao.findById(backupId)).thenReturn(backupVO);
        List<BackupVO> backups = new ArrayList<>(List.of(oldestBackupVO));
        when(backupDao.listBySchedule(scheduleId)).thenReturn(backups);

        CreateBackupCmd cmd = Mockito.mock(CreateBackupCmd.class);
        when(cmd.getVmId()).thenReturn(vmId);
        when(cmd.getName()).thenReturn("new-backup1");
        when(cmd.getQuiesceVM()).thenReturn(null);

        try (MockedStatic<ActionEventUtils> ignored = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(),
                    Mockito.anyLong(), Mockito.anyString())).thenReturn(1L);

            assertTrue(backupManager.createBackup(cmd, asyncJobVOMock));

            Mockito.verify(resourceLimitMgr, times(1)).checkResourceLimit(accountVOMock, Resource.ResourceType.backup);
            Mockito.verify(resourceLimitMgr, times(1)).checkResourceLimit(accountVOMock, Resource.ResourceType.backup_storage, newBackupSize);

            Mockito.verify(resourceLimitMgr, times(1)).incrementResourceCount(accountId, Resource.ResourceType.backup);
            Mockito.verify(resourceLimitMgr, times(1)).incrementResourceCount(accountId, Resource.ResourceType.backup_storage, newBackupSize);
            Mockito.verify(backupDao, times(1)).update(backupVO.getId(), backupVO);
            Mockito.verify(backupManager, times(1)).deleteOldestBackupFromScheduleIfRequired(vmId, scheduleId);
        }
    }

    @Test(expected = ResourceAllocationException.class)
    public void createBackupTestResourceLimitReached() throws ResourceAllocationException {
        Long vmId = 1L;
        Long zoneId = 2L;
        Long scheduleId = 3L;
        Long backupOfferingId = 4L;
        Long accountId = 5L;

        when(vmInstanceDao.findById(vmId)).thenReturn(vmInstanceVOMock);
        when(vmInstanceVOMock.getDataCenterId()).thenReturn(zoneId);
        when(vmInstanceVOMock.getBackupOfferingId()).thenReturn(backupOfferingId);
        when(vmInstanceVOMock.getAccountId()).thenReturn(accountId);

        overrideBackupFrameworkConfigValue();
        BackupOfferingVO offering = Mockito.mock(BackupOfferingVO.class);
        when(backupOfferingDao.findById(backupOfferingId)).thenReturn(offering);
        when(offering.isUserDrivenBackupAllowed()).thenReturn(true);
        when(offering.getProvider()).thenReturn("testbackupprovider");

        Account account = Mockito.mock(Account.class);
        when(accountManager.getAccount(accountId)).thenReturn(account);
        Mockito.doThrow(new ResourceAllocationException("", Resource.ResourceType.backup)).when(resourceLimitMgr).checkResourceLimit(account, Resource.ResourceType.backup);

        CreateBackupCmd cmd = Mockito.mock(CreateBackupCmd.class);
        when(cmd.getVmId()).thenReturn(vmId);
        when(cmd.getQuiesceVM()).thenReturn(null);

        String jobParams = "{}";
        when(asyncJobVOMock.getCmdInfo()).thenReturn(jobParams);
        when(asyncJobVOMock.getId()).thenReturn(1L);

        backupManager.createBackup(cmd, asyncJobVOMock);

        String msg = "Backup storage space resource limit exceeded for account id : " + accountId + ". Failed to create backup";
        Mockito.verify(alertManagerMock, times(1)).sendAlert(AlertManager.AlertType.ALERT_TYPE_UPDATE_RESOURCE_COUNT, 0L, 0L, msg, "Backup resource limit exceeded for account id : " + accountId
                + ". Failed to create backups; please use updateResourceLimit to increase the limit");
    }

    @Test (expected = ResourceAllocationException.class)
    public void testCreateBackupStorageLimitReached() throws ResourceAllocationException {
        Long vmId = 1L;
        Long zoneId = 2L;
        Long scheduleId = 3L;
        Long backupOfferingId = 4L;
        Long accountId = 5L;

        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        when(vm.getDataCenterId()).thenReturn(zoneId);
        when(vm.getBackupOfferingId()).thenReturn(backupOfferingId);
        when(vm.getAccountId()).thenReturn(accountId);

        overrideBackupFrameworkConfigValue();
        BackupOfferingVO offering = Mockito.mock(BackupOfferingVO.class);
        when(backupOfferingDao.findById(backupOfferingId)).thenReturn(offering);
        when(offering.isUserDrivenBackupAllowed()).thenReturn(true);
        when(offering.getProvider()).thenReturn("testbackupprovider");

        Mockito.doReturn(scheduleId).when(backupManager).getBackupScheduleId(asyncJobVOMock);

        Account account = Mockito.mock(Account.class);
        when(accountManager.getAccount(accountId)).thenReturn(account);
        Mockito.doThrow(new ResourceAllocationException("", Resource.ResourceType.backup_storage)).when(resourceLimitMgr).checkResourceLimit(account, Resource.ResourceType.backup_storage, 0L);

        CreateBackupCmd cmd = Mockito.mock(CreateBackupCmd.class);
        when(cmd.getVmId()).thenReturn(vmId);
        when(cmd.getQuiesceVM()).thenReturn(null);

        backupManager.createBackup(cmd, asyncJobVOMock);

        String msg = "Backup storage space resource limit exceeded for account id : " + accountId + ". Failed to create backup";
        Mockito.verify(alertManagerMock, times(1)).sendAlert(AlertManager.AlertType.ALERT_TYPE_UPDATE_RESOURCE_COUNT, 0L, 0L, msg, "Backup storage space resource limit exceeded for account id : " + accountId
                + ". Failed to create backups; please use updateResourceLimit to increase the limit");
    }

    @Test
    public void testBackupSyncTask() {
        Long dataCenterId = 1L;
        Long vmId = 2L;
        Long accountId = 3L;
        Long backup2Id = 4L;
        String restorePoint1ExternalId = "1234";
        Long backup1Size = 1 * Resource.ResourceType.bytesToGiB;
        Long backup2Size = 2 * Resource.ResourceType.bytesToGiB;
        Long newBackupSize = 3 * Resource.ResourceType.bytesToGiB;
        Long restorePointSize = 4 * Resource.ResourceType.bytesToGiB;

        overrideBackupFrameworkConfigValue();

        DataCenterVO dataCenter = mock(DataCenterVO.class);
        when(dataCenter.getId()).thenReturn(dataCenterId);
        when(dataCenterDao.listAllZones()).thenReturn(List.of(dataCenter));

        BackupProvider backupProvider = mock(BackupProvider.class);
        when(backupProvider.getName()).thenReturn("testbackupprovider");
        backupManager.setBackupProviders(List.of(backupProvider));
        backupManager.start();

        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        when(vm.getId()).thenReturn(vmId);
        when(vm.getAccountId()).thenReturn(accountId);
        List<Long> vmIds = List.of(vmId);
        when(backupDao.listVmIdsWithBackupsInZone(dataCenterId)).thenReturn(vmIds);
        when(vmInstanceDao.listByZoneAndBackupOffering(dataCenterId, null)).thenReturn(List.of(vm));

        Backup.RestorePoint restorePoint1 = new Backup.RestorePoint(restorePoint1ExternalId, DateUtil.now(), "Full", restorePointSize, 0L);
        Backup.RestorePoint restorePoint2 = new Backup.RestorePoint("12345", DateUtil.now(), "Full", restorePointSize, 0L);
        List<Backup.RestorePoint> restorePoints = new ArrayList<>(List.of(restorePoint1, restorePoint2));
        when(backupProvider.listRestorePoints(vm)).thenReturn(restorePoints);

        BackupVO backupInDb1 = new BackupVO();
        backupInDb1.setSize(backup1Size);
        backupInDb1.setAccountId(accountId);
        backupInDb1.setExternalId(restorePoint1ExternalId);

        BackupVO backupInDb2 = new BackupVO();
        backupInDb2.setSize(backup2Size);
        backupInDb2.setAccountId(accountId);
        backupInDb2.setExternalId(null);
        ReflectionTestUtils.setField(backupInDb2, "id", backup2Id);
        when(backupDao.findById(backup2Id)).thenReturn(backupInDb2);

        when(backupDao.listByVmId(null, vmId)).thenReturn(List.of(backupInDb1, backupInDb2));

        BackupVO newBackupEntry = new BackupVO();
        newBackupEntry.setSize(newBackupSize);
        when(backupProvider.createNewBackupEntryForRestorePoint(restorePoint2, vm)).thenReturn(newBackupEntry);

        try (MockedStatic<ActionEventUtils> ignored = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(),
                    Mockito.anyLong(), Mockito.anyString())).thenReturn(1L);

            try (MockedStatic<UsageEventUtils> ignored2 = Mockito.mockStatic(UsageEventUtils.class)) {

                BackupManagerImpl.BackupSyncTask backupSyncTask = backupManager.new BackupSyncTask(backupManager);
                backupSyncTask.runInContext();

                verify(resourceLimitMgr, times(1)).decrementResourceCount(accountId, Resource.ResourceType.backup_storage, backup1Size);
                verify(resourceLimitMgr, times(1)).incrementResourceCount(accountId, Resource.ResourceType.backup_storage, restorePointSize);
                Assert.assertEquals(backupInDb1.getSize(), restorePointSize);

                verify(resourceLimitMgr, times(1)).incrementResourceCount(accountId, Resource.ResourceType.backup);
                verify(resourceLimitMgr, times(1)).incrementResourceCount(accountId, Resource.ResourceType.backup_storage, newBackupSize);

                verify(resourceLimitMgr, times(1)).decrementResourceCount(accountId, Resource.ResourceType.backup);
                verify(resourceLimitMgr, times(1)).decrementResourceCount(accountId, Resource.ResourceType.backup_storage, backup2Size);
            }
        }
    }

    @Test
    public void checkCallerAccessToBackupScheduleVmTestExecuteAccessCheckMethods() {
        long vmId = 1L;
        long dataCenterId = 2L;

        try (MockedStatic<CallContext> mockedCallContext = Mockito.mockStatic(CallContext.class)) {
            Mockito.when(vmInstanceDao.findById(vmId)).thenReturn(vmInstanceVOMock);
            Mockito.when(vmInstanceVOMock.getDataCenterId()).thenReturn(dataCenterId);
            Mockito.when(backupManager.isDisabled(dataCenterId)).thenReturn(false);

            mockedCallContext.when(CallContext::current).thenReturn(callContextMock);
            Mockito.when(callContextMock.getCallingAccount()).thenReturn(accountVOMock);
            Mockito.doNothing().when(accountManager).checkAccess(accountVOMock, null, true, vmInstanceVOMock);
            backupManager.checkCallerAccessToBackupScheduleVm(vmId);

            verify(accountManager, times(1)).checkAccess(accountVOMock, null, true, vmInstanceVOMock);
        }
    }

    @Test
    public void deleteAllVmBackupSchedulesTestReturnSuccessWhenAllSchedulesAreDeleted() {
        long vmId = 1L;
        List<BackupScheduleVO> backupSchedules = List.of(Mockito.mock(BackupScheduleVO.class), Mockito.mock(BackupScheduleVO.class));
        Mockito.when(backupScheduleDao.listByVM(vmId)).thenReturn(backupSchedules);
        Mockito.when(backupSchedules.get(0).getId()).thenReturn(2L);
        Mockito.when(backupSchedules.get(1).getId()).thenReturn(3L);
        Mockito.when(backupScheduleDao.remove(Mockito.anyLong())).thenReturn(true);

        boolean success = backupManager.deleteAllVmBackupSchedules(vmId);
        assertTrue(success);
        Mockito.verify(backupScheduleDao, times(2)).remove(Mockito.anyLong());
    }

    @Test
    public void deleteAllVmBackupSchedulesTestReturnFalseWhenAnyDeletionFails() {
        long vmId = 1L;
        List<BackupScheduleVO> backupSchedules = List.of(Mockito.mock(BackupScheduleVO.class), Mockito.mock(BackupScheduleVO.class));
        Mockito.when(backupScheduleDao.listByVM(vmId)).thenReturn(backupSchedules);
        Mockito.when(backupSchedules.get(0).getId()).thenReturn(2L);
        Mockito.when(backupSchedules.get(1).getId()).thenReturn(3L);
        Mockito.when(backupScheduleDao.remove(2L)).thenReturn(true);
        Mockito.when(backupScheduleDao.remove(3L)).thenReturn(false);

        boolean success = backupManager.deleteAllVmBackupSchedules(vmId);
        assertFalse(success);
        Mockito.verify(backupScheduleDao, times(2)).remove(Mockito.anyLong());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void deleteBackupScheduleTestThrowExceptionWhenVmIdAndScheduleIdAreNull() {
        when(deleteBackupScheduleCmdMock.getVmId()).thenReturn(null);
        when(deleteBackupScheduleCmdMock.getId()).thenReturn(null);

        backupManager.deleteBackupSchedule(deleteBackupScheduleCmdMock);
    }

    @Test
    public void deleteBackupScheduleTestDeleteVmSchedulesWhenVmIdIsSpecified() {
        long vmId = 1L;

        when(deleteBackupScheduleCmdMock.getId()).thenReturn(null);
        when(deleteBackupScheduleCmdMock.getVmId()).thenReturn(vmId);
        Mockito.doNothing().when(backupManager).checkCallerAccessToBackupScheduleVm(vmId);
        Mockito.doReturn(true).when(backupManager).deleteAllVmBackupSchedules(vmId);

        boolean success = backupManager.deleteBackupSchedule(deleteBackupScheduleCmdMock);
        assertTrue(success);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void deleteBackupScheduleTestThrowExceptionWhenSpecificScheduleIsNotFound() {
        long id = 1L;
        when(deleteBackupScheduleCmdMock.getId()).thenReturn(id);
        backupManager.deleteBackupSchedule(deleteBackupScheduleCmdMock);
    }

    @Test
    public void deleteBackupScheduleTestDeleteSpecificScheduleWhenItsIdIsSpecified() {
        long id = 1L;
        long vmId = 2L;
        when(deleteBackupScheduleCmdMock.getId()).thenReturn(id);
        when(deleteBackupScheduleCmdMock.getVmId()).thenReturn(null);
        when(backupScheduleDao.findById(id)).thenReturn(backupScheduleVOMock);
        when(backupScheduleVOMock.getVmId()).thenReturn(vmId);
        Mockito.doNothing().when(backupManager).checkCallerAccessToBackupScheduleVm(vmId);
        when(backupScheduleVOMock.getId()).thenReturn(id);
        when(backupScheduleDao.remove(id)).thenReturn(true);

        boolean success = backupManager.deleteBackupSchedule(deleteBackupScheduleCmdMock);
        assertTrue(success);
    }

    @Test
    public void testGetBackupDetailsFromVM() {
        Long vmId = 1L;
        VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getServiceOfferingId()).thenReturn(1L);
        when(vm.getTemplateId()).thenReturn(2L);
        when(vm.getId()).thenReturn(vmId);

        ServiceOfferingVO serviceOffering = mock(ServiceOfferingVO.class);
        when(serviceOffering.getUuid()).thenReturn("service-offering-uuid");
        when(serviceOfferingDao.findById(1L)).thenReturn(serviceOffering);
        VMTemplateVO template = mock(VMTemplateVO.class);
        when(template.getUuid()).thenReturn("template-uuid");
        when(vmTemplateDao.findById(2L)).thenReturn(template);

        VMInstanceDetailVO vmInstanceDetail = mock(VMInstanceDetailVO.class);
        when(vmInstanceDetail.getName()).thenReturn("mocked-detail-name");
        when(vmInstanceDetail.getValue()).thenReturn("mocked-detail-value");
        List<VMInstanceDetailVO> vmDetails = Collections.singletonList(vmInstanceDetail);
        when(vmInstanceDetailsDao.listDetails(vmId)).thenReturn(vmDetails);

        UserVmJoinVO userVmJoinVO = mock(UserVmJoinVO.class);
        when(userVmJoinVO.getNetworkUuid()).thenReturn("mocked-network-uuid");
        List<UserVmJoinVO> userVmJoinVOs = Collections.singletonList(userVmJoinVO);
        when(userVmJoinDao.searchByIds(vmId)).thenReturn(userVmJoinVOs);

        Map<String, String> details = backupManager.getBackupDetailsFromVM(vm);

        assertEquals("service-offering-uuid", details.get(ApiConstants.SERVICE_OFFERING_ID));
        assertEquals("[{\"networkid\":\"mocked-network-uuid\"}]", details.get(ApiConstants.NICS));
        assertEquals("{\"mocked-detail-name\":\"mocked-detail-value\"}", details.get(ApiConstants.VM_SETTINGS));
    }

    @Test
    public void getDataDiskInfoListFromBackup() {
        Long size1 = 5L * 1024 * 1024 * 1024;
        Long size2 = 10L * 1024 * 1024 * 1024;
        Backup backup = mock(Backup.class);

        Backup.VolumeInfo volumeInfo0 = mock(Backup.VolumeInfo.class);
        when(volumeInfo0.getType()).thenReturn(Volume.Type.ROOT);
        Backup.VolumeInfo volumeInfo1 = mock(Backup.VolumeInfo.class);
        when(volumeInfo1.getDiskOfferingId()).thenReturn("disk-offering-uuid-1");
        when(volumeInfo1.getSize()).thenReturn(size1);
        when(volumeInfo1.getMinIops()).thenReturn(100L);
        when(volumeInfo1.getMaxIops()).thenReturn(300L);
        when(volumeInfo1.getType()).thenReturn(Volume.Type.DATADISK);
        when(volumeInfo1.getDeviceId()).thenReturn(1L);
        Backup.VolumeInfo volumeInfo2 = mock(Backup.VolumeInfo.class);
        when(volumeInfo2.getDiskOfferingId()).thenReturn("disk-offering-uuid-2");
        when(volumeInfo2.getSize()).thenReturn(size2);
        when(volumeInfo2.getMinIops()).thenReturn(200L);
        when(volumeInfo2.getMaxIops()).thenReturn(400L);
        when(volumeInfo2.getType()).thenReturn(Volume.Type.DATADISK);
        when(volumeInfo2.getDeviceId()).thenReturn(2L);
        when(backup.getBackedUpVolumes()).thenReturn(List.of(volumeInfo0, volumeInfo1, volumeInfo2));

        DiskOfferingVO diskOffering1 = mock(DiskOfferingVO.class);
        when(diskOffering1.getUuid()).thenReturn("disk-offering-uuid-1");
        when(diskOffering1.getState()).thenReturn(DiskOffering.State.Active);

        DiskOfferingVO diskOffering2 = mock(DiskOfferingVO.class);
        when(diskOffering2.getUuid()).thenReturn("disk-offering-uuid-2");
        when(diskOffering2.getState()).thenReturn(DiskOffering.State.Active);

        when(diskOfferingDao.findByUuid("disk-offering-uuid-1")).thenReturn(diskOffering1);
        when(diskOfferingDao.findByUuid("disk-offering-uuid-2")).thenReturn(diskOffering2);

        List<VmDiskInfo> vmDiskInfoList = backupManager.getDataDiskInfoListFromBackup(backup);

        assertEquals(2, vmDiskInfoList.size());
        assertEquals("disk-offering-uuid-1", vmDiskInfoList.get(0).getDiskOffering().getUuid());
        assertEquals(Long.valueOf(5), vmDiskInfoList.get(0).getSize());
        assertEquals(Long.valueOf(1), vmDiskInfoList.get(0).getDeviceId());
        assertEquals(Long.valueOf(100), vmDiskInfoList.get(0).getMinIops());
        assertEquals(Long.valueOf(300), vmDiskInfoList.get(0).getMaxIops());

        assertEquals("disk-offering-uuid-2", vmDiskInfoList.get(1).getDiskOffering().getUuid());
        assertEquals(Long.valueOf(10), vmDiskInfoList.get(1).getSize());
        assertEquals(Long.valueOf(2), vmDiskInfoList.get(1).getDeviceId());
        assertEquals(Long.valueOf(200), vmDiskInfoList.get(1).getMinIops());
        assertEquals(Long.valueOf(400), vmDiskInfoList.get(1).getMaxIops());
    }

    @Test
    public void getDataDiskInfoListFromBackupNullIops() {
        Long size = 5L * 1024 * 1024 * 1024;
        Backup backup = mock(Backup.class);
        Backup.VolumeInfo volumeInfo1 = mock(Backup.VolumeInfo.class);
        when(volumeInfo1.getDiskOfferingId()).thenReturn("disk-offering-uuid-1");
        when(volumeInfo1.getSize()).thenReturn(size);
        when(volumeInfo1.getMinIops()).thenReturn(null);
        when(volumeInfo1.getMaxIops()).thenReturn(null);
        when(volumeInfo1.getType()).thenReturn(Volume.Type.DATADISK);
        when(volumeInfo1.getDeviceId()).thenReturn(1L);
        when(backup.getBackedUpVolumes()).thenReturn(List.of(volumeInfo1));

        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOffering.getUuid()).thenReturn("disk-offering-uuid-1");
        when(diskOffering.getState()).thenReturn(DiskOffering.State.Active);

        when(diskOfferingDao.findByUuid("disk-offering-uuid-1")).thenReturn(diskOffering);

        List<VmDiskInfo> vmDiskInfoList = backupManager.getDataDiskInfoListFromBackup(backup);

        assertEquals(1, vmDiskInfoList.size());
        assertEquals("disk-offering-uuid-1", vmDiskInfoList.get(0).getDiskOffering().getUuid());
        assertEquals(Long.valueOf(5), vmDiskInfoList.get(0).getSize());
        assertEquals(Long.valueOf(1), vmDiskInfoList.get(0).getDeviceId());
        assertNull(vmDiskInfoList.get(0).getMinIops());
        assertNull(vmDiskInfoList.get(0).getMaxIops());
    }

    @Test (expected = InvalidParameterValueException.class)
    public void testCheckVmDisksSizeAgainstBackup() {
        Long sizeInBackup = 5L * 1024 * 1024 * 1024;
        Long sizeInCmd = 2L;
        Backup backup = mock(Backup.class);
        Backup.VolumeInfo volumeInfo = mock(Backup.VolumeInfo.class);
        when(volumeInfo.getDiskOfferingId()).thenReturn("disk-offering-uuid-1");
        when(volumeInfo.getSize()).thenReturn(sizeInBackup);
        when(volumeInfo.getType()).thenReturn(Volume.Type.DATADISK);
        when(backup.getBackedUpVolumes()).thenReturn(List.of(volumeInfo));

        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOffering.getState()).thenReturn(DiskOffering.State.Active);
        when(diskOfferingDao.findByUuid("disk-offering-uuid-1")).thenReturn(diskOffering);
        List<VmDiskInfo> vmDiskInfoList = List.of(new VmDiskInfo(diskOffering, sizeInCmd, 1L, null, null));

        backupManager.checkVmDisksSizeAgainstBackup(vmDiskInfoList, backup);
    }

    @Test
    public void testGetRootDiskInfoFromBackup() {
        Long size = 5L * 1024 * 1024 * 1024;
        Backup backup = mock(Backup.class);
        Backup.VolumeInfo volumeInfo = mock(Backup.VolumeInfo.class);
        when(volumeInfo.getDiskOfferingId()).thenReturn("root-disk-offering-uuid");
        when(volumeInfo.getSize()).thenReturn(size);
        when(volumeInfo.getType()).thenReturn(Volume.Type.ROOT);
        when(backup.getBackedUpVolumes()).thenReturn(List.of(volumeInfo));

        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOffering.getUuid()).thenReturn("root-disk-offering-uuid");
        when(diskOfferingDao.findByUuid("root-disk-offering-uuid")).thenReturn(diskOffering);

        VmDiskInfo VmDiskInfo = backupManager.getRootDiskInfoFromBackup(backup);

        assertEquals("root-disk-offering-uuid", VmDiskInfo.getDiskOffering().getUuid());
        assertEquals(Long.valueOf(5), VmDiskInfo.getSize());
        assertEquals(null, VmDiskInfo.getDeviceId());
    }

    @Test
    public void testImportBackupOffering() {
        ImportBackupOfferingCmd cmd = Mockito.mock(ImportBackupOfferingCmd.class);
        when(cmd.getZoneId()).thenReturn(1L);
        when(cmd.getExternalId()).thenReturn("external-id");
        when(cmd.getName()).thenReturn("Test Offering");
        when(cmd.getDescription()).thenReturn("Test Description");
        when(cmd.getUserDrivenBackups()).thenReturn(true);

        overrideBackupFrameworkConfigValue();

        when(backupOfferingDao.findByExternalId("external-id", 1L)).thenReturn(null);
        when(backupOfferingDao.findByName("Test Offering", 1L)).thenReturn(null);

        BackupOfferingVO offering = new BackupOfferingVO(1L, "external-id", "testbackupprovider", "Test Offering", "Test Description", true);
        when(backupOfferingDao.persist(any(BackupOfferingVO.class))).thenReturn(offering);
        when(backupProvider.isValidProviderOffering(cmd.getZoneId(), cmd.getExternalId())).thenReturn(true);

        BackupOffering result = backupManager.importBackupOffering(cmd);

        assertEquals("Test Offering", result.getName());
        assertEquals("Test Description", result.getDescription());
        assertEquals(true, result.isUserDrivenBackupAllowed());
        assertEquals("external-id", result.getExternalId());
        assertEquals("testbackupprovider", result.getProvider());
    }

    @Test
    public void testCreateVolumeInfoFromVolumes() {
        Long diskOfferingId = 5L;
        DiskOfferingVO diskOffering = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(diskOffering.getUuid()).thenReturn("disk-offering-uuid");
        Mockito.when(diskOfferingDao.findById(diskOfferingId)).thenReturn(diskOffering);

        List<VolumeVO> volumes = new ArrayList<>();
        VolumeVO volume1 = new VolumeVO(Volume.Type.ROOT, "vol1", 1L, 2L, 3L,
                diskOfferingId, null, 1024L, null, null, null);
        volume1.setUuid("uuid1");
        volume1.setPath("path1");
        volume1.setDeviceId(0L);
        volume1.setVolumeType(Volume.Type.ROOT);
        volumes.add(volume1);

        VolumeVO volume2 = new VolumeVO(Volume.Type.ROOT, "vol2", 1L, 2L, 3L,
                diskOfferingId, null, 2048L, 1000L, 2000L, null);
        volume2.setUuid("uuid2");
        volume2.setPath("path2");
        volume2.setDeviceId(1L);
        volume2.setVolumeType(Volume.Type.DATADISK);
        volumes.add(volume2);

        String expectedJson = "[{\"uuid\":\"uuid1\",\"type\":\"ROOT\",\"size\":1024,\"path\":\"path1\",\"deviceId\":0,\"diskOfferingId\":\"disk-offering-uuid\"},{\"uuid\":\"uuid2\",\"type\":\"DATADISK\",\"size\":2048,\"path\":\"path2\",\"deviceId\":1,\"diskOfferingId\":\"disk-offering-uuid\",\"minIops\":1000,\"maxIops\":2000}]";
        String actualJson = backupManager.createVolumeInfoFromVolumes(new ArrayList<>(volumes));

        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testAssignVMToBackupOffering() {
        Long vmId = 1L;
        Long offeringId = 2L;

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getId()).thenReturn(vmId);
        BackupOfferingVO offering = mock(BackupOfferingVO.class);

        overrideBackupFrameworkConfigValue();

        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        when(backupOfferingDao.findById(offeringId)).thenReturn(offering);
        when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        when(vm.getDataCenterId()).thenReturn(1L);
        when(vm.getBackupOfferingId()).thenReturn(null);
        when(offering.getProvider()).thenReturn("testbackupprovider");
        when(backupProvider.assignVMToBackupOffering(vm, offering)).thenReturn(true);
        when(vmInstanceDao.update(1L, vm)).thenReturn(true);

        try (MockedStatic<UsageEventUtils> ignored2 = Mockito.mockStatic(UsageEventUtils.class)) {
            boolean result = backupManager.assignVMToBackupOffering(vmId, offeringId);

            assertTrue(result);
            verify(vmInstanceDao, times(1)).findById(vmId);
            verify(backupOfferingDao, times(1)).findById(offeringId);
            verify(backupManager, times(1)).getBackupProvider("testbackupprovider");
        }
    }

    @Test
    public void testRemoveVMFromBackupOffering() {
        Long vmId = 1L;
        Long accountId = 2L;
        Long zoneId = 3L;
        Long offeringId = 4L;
        Long backupScheduleId = 5L;
        String vmHostName = "vm1";
        String vmUuid = "uuid1";
        String resourceName = "Backup-" + vmHostName + "-" + vmUuid;

        boolean forced = true;

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getId()).thenReturn(vmId);
        when(vm.getDataCenterId()).thenReturn(1L);
        when(vm.getBackupOfferingId()).thenReturn(offeringId);
        when(vm.getAccountId()).thenReturn(accountId);
        when(vm.getDataCenterId()).thenReturn(zoneId);
        when(vm.getHostName()).thenReturn(vmHostName);
        when(vm.getUuid()).thenReturn(vmUuid);
        when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        when(vmInstanceDao.update(vmId, vm)).thenReturn(true);

        BackupOfferingVO offering = mock(BackupOfferingVO.class);
        when(backupOfferingDao.findById(vm.getBackupOfferingId())).thenReturn(offering);
        when(offering.getProvider()).thenReturn("testbackupprovider");
        when(backupProvider.removeVMFromBackupOffering(vm)).thenReturn(true);
        when(backupProvider.willDeleteBackupsOnOfferingRemoval()).thenReturn(true);
        when(backupDao.listByVmId(null, vmId)).thenReturn(new ArrayList<>());

        BackupScheduleVO backupSchedule = new BackupScheduleVO();
        ReflectionTestUtils.setField(backupSchedule, "id", backupScheduleId);
        when(backupScheduleDao.listByVM(vmId)).thenReturn(List.of(backupSchedule));

        overrideBackupFrameworkConfigValue();

        try (MockedStatic<UsageEventUtils> usageEventUtilsMocked = Mockito.mockStatic(UsageEventUtils.class)) {
            boolean result = backupManager.removeVMFromBackupOffering(vmId, forced);

            assertTrue(result);
            verify(vmInstanceDao, times(1)).findByIdIncludingRemoved(vmId);
            verify(backupOfferingDao, times(1)).findById(vm.getBackupOfferingId());
            verify(backupManager, times(1)).getBackupProvider("testbackupprovider");
            verify(backupScheduleDao, times(1)).remove(backupScheduleId);
            usageEventUtilsMocked.verify(() -> UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_OFFERING_REMOVED_AND_BACKUPS_DELETED, accountId, zoneId, vmId, resourceName,
                    offeringId, null, null, Backup.class.getSimpleName(), vmUuid));
        }
    }

    @Test
    public void testDeleteBackupScheduleByVmId() {
        Long vmId = 1L;
        Long scheduleId = 2L;
        DeleteBackupScheduleCmd cmd = new DeleteBackupScheduleCmd();
        ReflectionTestUtils.setField(cmd, "vmId", vmId);

        overrideBackupFrameworkConfigValue();

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        BackupScheduleVO schedule = mock(BackupScheduleVO.class);
        when(schedule.getId()).thenReturn(scheduleId);
        when(backupScheduleDao.listByVM(vmId)).thenReturn(List.of(schedule));
        when(backupScheduleDao.remove(scheduleId)).thenReturn(true);

        boolean result = backupManager.deleteBackupSchedule(cmd);
        assertTrue(result);
    }

    @Test
    public void testRestoreBackupToVM() throws NoTransitionException {
        Long backupId = 1L;
        Long vmId = 2L;
        Long hostId = 3L;
        Long offeringId = 4L;
        Long poolId = 5L;

        BackupVO backup = mock(BackupVO.class);
        when(backup.getBackupOfferingId()).thenReturn(offeringId);
        when(backup.getStatus()).thenReturn(Backup.Status.BackedUp);

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        when(vm.getId()).thenReturn(vmId);
        when(vm.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(vm.getHostId()).thenReturn(hostId);

        BackupOfferingVO offering = mock(BackupOfferingVO.class);
        BackupProvider backupProvider = mock(BackupProvider.class);
        when(backupProvider.supportsInstanceFromBackup()).thenReturn(true);

        overrideBackupFrameworkConfigValue();

        when(backupDao.findById(backupId)).thenReturn(backup);
        when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        when(backupOfferingDao.findByIdIncludingRemoved(offeringId)).thenReturn(offering);
        when(offering.getProvider()).thenReturn("testbackupprovider");
        when(backupManager.getBackupProvider("testbackupprovider")).thenReturn(backupProvider);
        when(virtualMachineManager.stateTransitTo(vm, VirtualMachine.Event.RestoringRequested, hostId)).thenReturn(true);
        when(virtualMachineManager.stateTransitTo(vm, VirtualMachine.Event.RestoringSuccess, hostId)).thenReturn(true);

        VolumeVO rootVolume = mock(VolumeVO.class);
        when(rootVolume.getPoolId()).thenReturn(poolId);
        HostVO host = mock(HostVO.class);
        when(hostDao.findById(hostId)).thenReturn(host);
        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(volumeDao.findIncludingRemovedByInstanceAndType(vmId, Volume.Type.ROOT)).thenReturn(List.of(rootVolume));
        when(primaryDataStoreDao.findById(poolId)).thenReturn(pool);
        when(rootVolume.getPoolId()).thenReturn(poolId);
        when(volumeDao.findIncludingRemovedByInstanceAndType(vmId, Volume.Type.ROOT)).thenReturn(List.of(rootVolume));
        when(primaryDataStoreDao.findById(poolId)).thenReturn(pool);
        when(backupProvider.restoreBackupToVM(vm, backup, null, null)).thenReturn(new Pair<>(true, null));

        try (MockedStatic<ActionEventUtils> utils = Mockito.mockStatic(ActionEventUtils.class)) {
            boolean result = backupManager.restoreBackupToVM(backupId, vmId);

            assertTrue(result);
            verify(backupProvider, times(1)).restoreBackupToVM(vm, backup, null, null);
            verify(virtualMachineManager, times(1)).stateTransitTo(vm, VirtualMachine.Event.RestoringRequested, hostId);
            verify(virtualMachineManager, times(1)).stateTransitTo(vm, VirtualMachine.Event.RestoringSuccess, hostId);
        } catch (CloudRuntimeException e) {
            fail("Test failed due to exception" + e);
        }
    }

    @Test
    public void testRestoreBackupToVMException() throws NoTransitionException {
        Long backupId = 1L;
        Long vmId = 2L;
        Long hostId = 3L;
        Long offeringId = 4L;
        Long poolId = 5L;

        BackupVO backup = mock(BackupVO.class);
        when(backup.getBackupOfferingId()).thenReturn(offeringId);
        when(backup.getStatus()).thenReturn(Backup.Status.BackedUp);

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        when(vm.getId()).thenReturn(vmId);
        when(vm.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(vm.getHostId()).thenReturn(hostId);

        BackupOfferingVO offering = mock(BackupOfferingVO.class);
        BackupProvider backupProvider = mock(BackupProvider.class);
        when(backupProvider.supportsInstanceFromBackup()).thenReturn(true);

        overrideBackupFrameworkConfigValue();

        when(backupDao.findById(backupId)).thenReturn(backup);
        when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        when(backupOfferingDao.findByIdIncludingRemoved(offeringId)).thenReturn(offering);
        when(offering.getProvider()).thenReturn("testbackupprovider");
        when(backupManager.getBackupProvider("testbackupprovider")).thenReturn(backupProvider);
        when(virtualMachineManager.stateTransitTo(vm, VirtualMachine.Event.RestoringRequested, hostId)).thenReturn(true);
        when(virtualMachineManager.stateTransitTo(vm, VirtualMachine.Event.RestoringFailed, hostId)).thenReturn(true);

        VolumeVO rootVolume = mock(VolumeVO.class);
        when(rootVolume.getPoolId()).thenReturn(poolId);
        HostVO host = mock(HostVO.class);
        when(hostDao.findById(hostId)).thenReturn(host);
        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(volumeDao.findIncludingRemovedByInstanceAndType(vmId, Volume.Type.ROOT)).thenReturn(List.of(rootVolume));
        when(primaryDataStoreDao.findById(poolId)).thenReturn(pool);
        when(rootVolume.getPoolId()).thenReturn(poolId);
        when(volumeDao.findIncludingRemovedByInstanceAndType(vmId, Volume.Type.ROOT)).thenReturn(List.of(rootVolume));
        when(primaryDataStoreDao.findById(poolId)).thenReturn(pool);
        when(backupProvider.restoreBackupToVM(vm, backup, null, null)).thenReturn(new Pair<>(false, null));

        try (MockedStatic<ActionEventUtils> utils = Mockito.mockStatic(ActionEventUtils.class)) {
            CloudRuntimeException exception = Assert.assertThrows(CloudRuntimeException.class,
                    () -> backupManager.restoreBackupToVM(backupId, vmId));

            verify(backupProvider, times(1)).restoreBackupToVM(vm, backup, null, null);
            verify(virtualMachineManager, times(1)).stateTransitTo(vm, VirtualMachine.Event.RestoringRequested, hostId);
            verify(virtualMachineManager, times(1)).stateTransitTo(vm, VirtualMachine.Event.RestoringFailed, hostId);
        }
    }

    @Test
    public void testGetBackupStorageUsedStats() {
        Long zoneId = 1L;
        overrideBackupFrameworkConfigValue();
        when(backupManager.getBackupProvider(zoneId)).thenReturn(backupProvider);
        when(backupProvider.getBackupStorageStats(zoneId)).thenReturn(new Pair<>(100L, 200L));

        CapacityVO capacity = backupManager.getBackupStorageUsedStats(zoneId);

        Assert.assertNotNull(capacity);
        Assert.assertEquals(Optional.ofNullable(Long.valueOf(100)), Optional.ofNullable(capacity.getUsedCapacity()));
        Assert.assertEquals(Optional.ofNullable(Long.valueOf(200)), Optional.ofNullable(capacity.getTotalCapacity()));
        Assert.assertEquals(CapacityVO.CAPACITY_TYPE_BACKUP_STORAGE, capacity.getCapacityType());
    }

    @Test
    public void testCheckAndRemoveBackupOfferingBeforeExpunge() {
        Long vmId = 1L;
        Long zoneId = 2L;
        Long offeringId = 3L;
        String vmUuid = "uuid1";
        String instanceName = "i-2-1-VM";
        String backupExternalId = "backup-external-id";

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getId()).thenReturn(vmId);
        when(vm.getUuid()).thenReturn(vmUuid);
        when(vm.getBackupOfferingId()).thenReturn(offeringId);
        when(vm.getInstanceName()).thenReturn(instanceName);
        when(vm.getBackupExternalId()).thenReturn(backupExternalId);
        when(vm.getDataCenterId()).thenReturn(zoneId);
        Backup backup = mock(Backup.class);
        when(backupDao.listByVmIdAndOffering(zoneId, vmId, offeringId)).thenReturn(List.of(backup));

        CloudRuntimeException exception = Assert.assertThrows(CloudRuntimeException.class,
                () -> backupManager.checkAndRemoveBackupOfferingBeforeExpunge(vm));
        Assert.assertEquals("This Instance [uuid: uuid1, name: i-2-1-VM] has a "
                        + "Backup Offering [id: 3, external id: backup-external-id] with 1 backups. Please, remove the backup offering "
                        + "before proceeding to VM exclusion!", exception.getMessage());
    }

    @Test
    public void testGetIpToNetworkMapFromBackup() {
        Long networkId1 = 1L;
        Long networkId2 = 2L;
        String networkUuid1 = "network-uuid-1";
        String networkUuid2 = "network-uuid-2";
        String ip1 = "10.1.1.1";
        String ip2 = "10.1.1.2";
        String ipv61 = "2001:db8::1";
        String ipv62 = "2001:db8::2";
        String mac1 = "00:11:22:33:44:55";
        String mac2 = "00:11:22:33:44:56";

        // Test case 1: Missing network information
        Backup backup1 = mock(Backup.class);
        List<Long> networkIds1 = new ArrayList<>();
        try {
            backupManager.getIpToNetworkMapFromBackup(backup1, true, networkIds1);
            fail("Expected CloudRuntimeException for missing network information");
        } catch (CloudRuntimeException e) {
            assertEquals("Backup doesn't contain network information. Please specify at least one valid network while creating instance", e.getMessage());
        }

        // Test case 2: IP preservation enabled with IP information
        Backup backup2 = mock(Backup.class);
        String nicsJson = String.format("[{\"networkid\":\"%s\",\"ipaddress\":\"%s\",\"ip6address\":\"%s\",\"macaddress\":\"%s\"}," +
                        "{\"networkid\":\"%s\",\"ipaddress\":\"%s\",\"ip6address\":\"%s\",\"macaddress\":\"%s\"}]",
                        networkUuid1, ip1, ipv61, mac1, networkUuid2, ip2, ipv62, mac2);
        when(backup2.getDetail(ApiConstants.NICS)).thenReturn(nicsJson);

        NetworkVO network1 = mock(NetworkVO.class);
        NetworkVO network2 = mock(NetworkVO.class);
        when(networkDao.findByUuid(networkUuid1)).thenReturn(network1);
        when(networkDao.findByUuid(networkUuid2)).thenReturn(network2);
        when(network1.getId()).thenReturn(networkId1);
        when(network2.getId()).thenReturn(networkId2);

        Network.IpAddresses ipAddresses1 = mock(Network.IpAddresses.class);
        Network.IpAddresses ipAddresses2 = mock(Network.IpAddresses.class);
        when(networkService.getIpAddressesFromIps(ip1, ipv61, mac1)).thenReturn(ipAddresses1);
        when(networkService.getIpAddressesFromIps(ip2, ipv62, mac2)).thenReturn(ipAddresses2);

        List<Long> networkIds2 = new ArrayList<>();
        Map<Long, Network.IpAddresses> result2 = backupManager.getIpToNetworkMapFromBackup(backup2, true, networkIds2);

        assertEquals(2, result2.size());
        assertEquals(ipAddresses1, result2.get(networkId1));
        assertEquals(ipAddresses2, result2.get(networkId2));
        assertEquals(2, networkIds2.size());
        assertTrue(networkIds2.contains(networkId1));
        assertTrue(networkIds2.contains(networkId2));

        // Test case 3: IP preservation enabled but missing IP information
        Backup backup3 = mock(Backup.class);
        nicsJson = String.format("[{\"networkid\":\"%s\"}]", networkUuid1);
        when(backup3.getDetail(ApiConstants.NICS)).thenReturn(nicsJson);

        List<Long> networkIds3 = new ArrayList<>();
        Map<Long, Network.IpAddresses> result3 = backupManager.getIpToNetworkMapFromBackup(backup3, true, networkIds3);

        assertEquals(1, result3.size());
        assertNull(result3.get(networkId1));
        assertEquals(1, networkIds3.size());
        assertTrue(networkIds3.contains(networkId1));

        // Test case 4: IP preservation disabled
        Backup backup4 = mock(Backup.class);
        nicsJson = String.format("[{\"networkid\":\"%s\"}]", networkUuid1);
        when(backup4.getDetail(ApiConstants.NICS)).thenReturn(nicsJson);

        List<Long> networkIds4 = new ArrayList<>();
        Map<Long, Network.IpAddresses> result4 = backupManager.getIpToNetworkMapFromBackup(backup4, false, networkIds4);

        assertEquals(1, result4.size());
        assertNull(result4.get(networkId1));
        assertEquals(1, networkIds4.size());
        assertTrue(networkIds4.contains(networkId1));
    }

    @Test
    public void testDeleteBackupVmNotFound() {
        Long backupId = 1L;
        Long vmId = 2L;
        Long zoneId = 3L;
        Long accountId = 4L;
        Long backupOfferingId = 5L;
        String vmHostName = "vm1";
        String vmUuid = "uuid1";
        String resourceName = "Backup-" + vmHostName + "-" + vmUuid;

        BackupVO backup = mock(BackupVO.class);
        when(backup.getId()).thenReturn(backupId);
        when(backup.getVmId()).thenReturn(vmId);
        when(backup.getZoneId()).thenReturn(zoneId);
        when(backup.getAccountId()).thenReturn(accountId);
        when(backup.getBackupOfferingId()).thenReturn(backupOfferingId);
        when(backup.getSize()).thenReturn(100L);

        overrideBackupFrameworkConfigValue();

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getId()).thenReturn(vmId);
        when(vm.getAccountId()).thenReturn(accountId);
        when(vm.getBackupOfferingId()).thenReturn(10L);
        when(vm.getDataCenterId()).thenReturn(zoneId);
        when(vm.getHostName()).thenReturn(vmHostName);
        when(vm.getUuid()).thenReturn(vmUuid);
        when(backupDao.findByIdIncludingRemoved(backupId)).thenReturn(backup);
        when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        when(backupDao.listByVmIdAndOffering(zoneId, vmId, backupOfferingId)).thenReturn(new ArrayList<>());

        BackupOfferingVO offering = mock(BackupOfferingVO.class);
        when(backupOfferingDao.findByIdIncludingRemoved(backupOfferingId)).thenReturn(offering);

        when(backupProvider.deleteBackup(backup, false)).thenReturn(true);

        when(backupDao.remove(backupId)).thenReturn(true);

        try (MockedStatic<UsageEventUtils> usageEventUtilsMocked = Mockito.mockStatic(UsageEventUtils.class)) {
            boolean result = backupManager.deleteBackup(backupId, false);

            assertTrue(result);
            verify(backupProvider).deleteBackup(backup, false);
            verify(resourceLimitMgr).decrementResourceCount(accountId, Resource.ResourceType.backup);
            verify(resourceLimitMgr).decrementResourceCount(accountId, Resource.ResourceType.backup_storage, backup.getSize());
            verify(backupDao).remove(backupId);
            usageEventUtilsMocked.verify(() -> UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_OFFERING_REMOVED_AND_BACKUPS_DELETED, accountId, zoneId, vmId, resourceName,
                    backupOfferingId, null, null, Backup.class.getSimpleName(), vmUuid));
        }
    }

    @Test
    public void testNewBackupResponse() {
        Long vmId = 1L;
        Long accountId = 2L;
        Long domainId = 3L;
        Long zoneId = 4L;
        Long vmOfferingId = 5L;
        Long backupOfferingId = 6L;
        Long backupId = 7L;
        Long templateId = 8L;
        String templateUuid = "template-uuid1";
        String serviceOfferingUuid = "service-offering-uuid1";

        BackupVO backup = new BackupVO();
        ReflectionTestUtils.setField(backup, "id", backupId);
        ReflectionTestUtils.setField(backup, "uuid", "backup-uuid");
        backup.setVmId(vmId);
        backup.setAccountId(accountId);
        backup.setDomainId(domainId);
        backup.setZoneId(zoneId);
        backup.setBackupOfferingId(backupOfferingId);
        backup.setType("Full");
        backup.setBackupScheduleId(null);

        VMInstanceVO vm = new VMInstanceVO(vmId, 0L, "test-vm", "test-vm", VirtualMachine.Type.User,
                0L, Hypervisor.HypervisorType.Simulator, 0L, domainId, accountId, 0L, false);
        vm.setDataCenterId(zoneId);
        vm.setBackupOfferingId(vmOfferingId);
        vm.setTemplateId(templateId);

        AccountVO account = new AccountVO();
        account.setUuid("account-uuid");
        account.setAccountName("test-account");

        DomainVO domain = new DomainVO();
        domain.setUuid("domain-uuid");
        domain.setName("test-domain");

        DataCenterVO zone = new DataCenterVO(1L, "test-zone", null, null, null, null, null, null, null, null, DataCenter.NetworkType.Advanced, null, null);
        zone.setUuid("zone-uuid");

        BackupOfferingVO offering = Mockito.mock(BackupOfferingVO.class);
        Mockito.when(offering.getUuid()).thenReturn("offering-uuid");
        Mockito.when(offering.getName()).thenReturn("test-offering");

        Mockito.when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        Mockito.when(accountDao.findByIdIncludingRemoved(accountId)).thenReturn(account);
        Mockito.when(domainDao.findByIdIncludingRemoved(domainId)).thenReturn(domain);
        Mockito.when(dataCenterDao.findByIdIncludingRemoved(zoneId)).thenReturn(zone);
        Mockito.when(backupOfferingDao.findByIdIncludingRemoved(backupOfferingId)).thenReturn(offering);

        VMTemplateVO template = mock(VMTemplateVO.class);
        when(template.getFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        when(template.getUuid()).thenReturn(templateUuid);
        when(template.getName()).thenReturn("template1");
        when(vmTemplateDao.findByUuid(templateUuid)).thenReturn(template);
        Map<String, String> details = new HashMap<>();
        details.put(ApiConstants.TEMPLATE_ID, templateUuid);

        ServiceOfferingVO serviceOffering = mock(ServiceOfferingVO.class);
        when(serviceOffering.getUuid()).thenReturn(serviceOfferingUuid);
        when(serviceOffering.getName()).thenReturn("service-offering1");
        when(serviceOfferingDao.findByUuid(serviceOfferingUuid)).thenReturn(serviceOffering);
        details.put(ApiConstants.SERVICE_OFFERING_ID, serviceOfferingUuid);

        NetworkVO network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("network1");
        when(networkDao.findByUuid("network-uuid1")).thenReturn(network);
        details.put(ApiConstants.NICS, "[{\"networkid\":\"network-uuid1\"}]");

        Mockito.when(backupDetailsDao.listDetailsKeyPairs(backup.getId(), true)).thenReturn(details);

        BackupResponse response = backupManager.createBackupResponse(backup, true);

        Assert.assertEquals("backup-uuid", response.getId());
        Assert.assertEquals("test-vm", response.getVmName());
        Assert.assertEquals("account-uuid", response.getAccountId());
        Assert.assertEquals("test-account", response.getAccount());
        Assert.assertEquals("domain-uuid", response.getDomainId());
        Assert.assertEquals("test-domain", response.getDomain());
        Assert.assertEquals("zone-uuid", response.getZoneId());
        Assert.assertEquals("test-zone", response.getZone());
        Assert.assertEquals("offering-uuid", response.getBackupOfferingId());
        Assert.assertEquals("test-offering", response.getBackupOffering());
        Assert.assertEquals("MANUAL", response.getIntervalType());
        Assert.assertEquals("{serviceofferingid=service-offering-uuid1, isiso=false, hypervisor=Simulator, " +
                "nics=[{\"networkid\":\"network-uuid1\",\"networkname\":\"network1\"}], serviceofferingname=service-offering1, " +
                "templatename=template1, templateid=template-uuid1}", response.getVmDetails().toString());
        Assert.assertEquals(true, response.getVmOfferingRemoved());
    }

    @Test
    public void validateAndGetDefaultBackupRetentionIfRequiredTestReturnZeroAsDefaultValue() {
        int retention = backupManager.validateAndGetDefaultBackupRetentionIfRequired(null, backupOfferingVOMock, null);
        assertEquals(0, retention);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAndGetDefaultBackupRetentionIfRequiredTestThrowExceptionWhenBackupOfferingProviderIsVeeam() {
        Mockito.when(backupOfferingVOMock.getProvider()).thenReturn("veeam");
        backupManager.validateAndGetDefaultBackupRetentionIfRequired(1, backupOfferingVOMock, vmInstanceVOMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAndGetDefaultBackupRetentionIfRequiredTestThrowExceptionWhenMaxBackupsIsLessThanZero() {
        backupManager.validateAndGetDefaultBackupRetentionIfRequired(-1, backupOfferingVOMock, vmInstanceVOMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAndGetDefaultBackupRetentionIfRequiredTestThrowExceptionWhenMaxBackupsExceedsAccountLimit() {
        int maxBackups = 6;
        long accountId = 1L;
        long accountLimit = 5L;
        long domainId = 10L;
        long domainLimit = -1L;

        when(vmInstanceVOMock.getAccountId()).thenReturn(accountId);
        when(accountManager.getAccount(accountId)).thenReturn(accountVOMock);
        when(resourceLimitMgr.findCorrectResourceLimitForAccount(accountVOMock, Resource.ResourceType.backup, null)).thenReturn(accountLimit);
        when(accountVOMock.getDomainId()).thenReturn(domainId);
        when(domainManager.getDomain(domainId)).thenReturn(domainMock);
        when(resourceLimitMgr.findCorrectResourceLimitForDomain(domainMock, Resource.ResourceType.backup, null)).thenReturn(domainLimit);
        when(accountVOMock.getId()).thenReturn(accountId);
        when(accountManager.isRootAdmin(accountId)).thenReturn(false);

        backupManager.validateAndGetDefaultBackupRetentionIfRequired(maxBackups, backupOfferingVOMock, vmInstanceVOMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAndGetDefaultBackupRetentionIfRequiredTestThrowExceptionWhenMaxBackupsExceedsDomainLimit() {
        int maxBackups = 6;
        long accountId = 1L;
        long accountLimit = -1L;
        long domainId = 10L;
        long domainLimit = 5L;

        when(vmInstanceVOMock.getAccountId()).thenReturn(accountId);
        when(accountManager.getAccount(accountId)).thenReturn(accountVOMock);
        when(resourceLimitMgr.findCorrectResourceLimitForAccount(accountVOMock, Resource.ResourceType.backup, null)).thenReturn(accountLimit);
        when(accountVOMock.getDomainId()).thenReturn(domainId);
        when(domainManager.getDomain(domainId)).thenReturn(domainMock);
        when(resourceLimitMgr.findCorrectResourceLimitForDomain(domainMock, Resource.ResourceType.backup, null)).thenReturn(domainLimit);
        when(accountVOMock.getId()).thenReturn(accountId);
        when(accountManager.isRootAdmin(accountId)).thenReturn(false);

        backupManager.validateAndGetDefaultBackupRetentionIfRequired(maxBackups, backupOfferingVOMock, vmInstanceVOMock);
    }

    @Test
    public void validateAndGetDefaultBackupRetentionIfRequiredTestIgnoreLimitCheckWhenAccountIsRootAdmin() {
        int maxBackups = 6;
        long accountId = 1L;
        long accountLimit = 5L;
        long domainId = 10L;
        long domainLimit = 5L;

        when(vmInstanceVOMock.getAccountId()).thenReturn(accountId);
        when(accountManager.getAccount(accountId)).thenReturn(accountVOMock);
        when(resourceLimitMgr.findCorrectResourceLimitForAccount(accountVOMock, Resource.ResourceType.backup, null)).thenReturn(accountLimit);
        when(accountVOMock.getDomainId()).thenReturn(domainId);
        when(domainManager.getDomain(domainId)).thenReturn(domainMock);
        when(resourceLimitMgr.findCorrectResourceLimitForDomain(domainMock, Resource.ResourceType.backup, null)).thenReturn(domainLimit);
        when(accountVOMock.getId()).thenReturn(accountId);
        when(accountManager.isRootAdmin(accountId)).thenReturn(true);

        int retention = backupManager.validateAndGetDefaultBackupRetentionIfRequired(maxBackups, backupOfferingVOMock, vmInstanceVOMock);
        assertEquals(maxBackups, retention);
    }

    @Test
    public void getBackupScheduleTestReturnNullWhenBackupIsManual() {
        String jobParams = "{}";
        when(asyncJobVOMock.getCmdInfo()).thenReturn(jobParams);
        when(asyncJobVOMock.getId()).thenReturn(1L);

        Long backupScheduleId = backupManager.getBackupScheduleId(asyncJobVOMock);
        assertNull(backupScheduleId);
    }

    @Test
    public void getBackupScheduleTestReturnBackupScheduleIdWhenBackupIsScheduled() {
        Map<String, String> params = Map.of(
                ApiConstants.SCHEDULE_ID, "100"
        );
        String jobParams = gson.toJson(params);
        when(asyncJobVOMock.getCmdInfo()).thenReturn(jobParams);
        when(asyncJobVOMock.getId()).thenReturn(1L);

        Long backupScheduleId = backupManager.getBackupScheduleId(asyncJobVOMock);
        assertEquals(Long.valueOf("100"), backupScheduleId);
    }

    @Test
    public void getBackupScheduleTestReturnNullWhenSpecifiedBackupScheduleIdIsNotALongValue() {
        Map<String, String> params = Map.of(
                ApiConstants.SCHEDULE_ID, "InvalidValue"
        );
        String jobParams = gson.toJson(params);
        when(asyncJobVOMock.getCmdInfo()).thenReturn(jobParams);
        when(asyncJobVOMock.getId()).thenReturn(1L);

        Long backupScheduleId = backupManager.getBackupScheduleId(asyncJobVOMock);
        assertNull(backupScheduleId);
    }

    @Test
    public void deleteOldestBackupFromScheduleIfRequiredTestSkipDeletionWhenBackupScheduleIsNotFound() {
        backupManager.deleteOldestBackupFromScheduleIfRequired(1L, 1L);
        Mockito.verify(backupManager, Mockito.never()).deleteExcessBackups(Mockito.anyList(), Mockito.anyInt(), Mockito.anyLong());
    }

    @Test
    public void deleteOldestBackupFromScheduleIfRequiredTestSkipDeletionWhenRetentionIsEqualToZero() {
        Mockito.when(backupScheduleDao.findById(1L)).thenReturn(backupScheduleVOMock);
        Mockito.when(backupScheduleVOMock.getMaxBackups()).thenReturn(0);
        backupManager.deleteOldestBackupFromScheduleIfRequired(1L, 1L);
        Mockito.verify(backupManager, Mockito.never()).deleteExcessBackups(Mockito.anyList(), Mockito.anyInt(), Mockito.anyLong());
    }

    @Test
    public void deleteOldestBackupFromScheduleIfRequiredTestSkipDeletionWhenAmountOfBackupsToBeDeletedIsLessThanOne() {
        List<BackupVO> backups = List.of(Mockito.mock(BackupVO.class), Mockito.mock(BackupVO.class));
        Mockito.when(backupScheduleDao.findById(1L)).thenReturn(backupScheduleVOMock);
        Mockito.when(backupScheduleVOMock.getMaxBackups()).thenReturn(2);
        Mockito.when(backupDao.listBySchedule(1L)).thenReturn(backups);
        backupManager.deleteOldestBackupFromScheduleIfRequired(1L, 1L);
        Mockito.verify(backupManager, Mockito.never()).deleteExcessBackups(Mockito.anyList(), Mockito.anyInt(), Mockito.anyLong());
    }

    @Test
    public void deleteOldestBackupFromScheduleIfRequiredTestDeleteBackupsWhenRequired() {
        List<BackupVO> backups = List.of(Mockito.mock(BackupVO.class), Mockito.mock(BackupVO.class));
        Mockito.when(backupScheduleDao.findById(1L)).thenReturn(backupScheduleVOMock);
        Mockito.when(backupScheduleVOMock.getMaxBackups()).thenReturn(1);
        Mockito.when(backupDao.listBySchedule(1L)).thenReturn(backups);
        Mockito.doNothing().when(backupManager).deleteExcessBackups(Mockito.anyList(), Mockito.anyInt(), Mockito.anyLong());
        backupManager.deleteOldestBackupFromScheduleIfRequired(1L, 1L);
        Mockito.verify(backupManager).deleteExcessBackups(Mockito.anyList(), Mockito.anyInt(), Mockito.anyLong());
    }

    @Test
    public void deleteExcessBackupsTestEnsureBackupsAreDeletedWhenMethodIsCalled() {
        try (MockedStatic<ActionEventUtils> actionEventUtils = Mockito.mockStatic(ActionEventUtils.class)) {
            List<BackupVO> backups = List.of(Mockito.mock(BackupVO.class),
                    Mockito.mock(BackupVO.class),
                    Mockito.mock(BackupVO.class));

            Mockito.when(backups.get(0).getId()).thenReturn(1L);
            Mockito.when(backups.get(1).getId()).thenReturn(2L);
            Mockito.when(backups.get(0).getAccountId()).thenReturn(1L);
            Mockito.when(backups.get(1).getAccountId()).thenReturn(2L);
            Mockito.doReturn(true).when(backupManager).deleteBackup(Mockito.anyLong(), Mockito.eq(false));

            actionEventUtils.when(() -> ActionEventUtils.onStartedActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.anyBoolean(), Mockito.anyInt())).thenReturn(1L);
            actionEventUtils.when(() -> ActionEventUtils.onCompletedActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyInt())).thenReturn(2L);

            backupManager.deleteExcessBackups(backups, 2, 1L);
            Mockito.verify(backupManager, times(2)).deleteBackup(Mockito.anyLong(), Mockito.eq(false));
        }
    }

    @Test
    public void sendExceededBackupLimitAlertTestSendAlertForBackupResourceType() {
        String accountUuid = UUID.randomUUID().toString();
        String expectedMessage = "Failed to create backup: backup resource limit exceeded for account with ID: " + accountUuid + ".";
        String expectedAlertDetails = expectedMessage + " Please, use the 'updateResourceLimit' API to increase the backup limit.";

        backupManager.sendExceededBackupLimitAlert(accountUuid, Resource.ResourceType.backup);
        verify(alertManagerMock).sendAlert(
                AlertManager.AlertType.ALERT_TYPE_UPDATE_RESOURCE_COUNT,
                0L,
                0L,
                expectedMessage,
                expectedAlertDetails
        );
    }

    @Test
    public void sendExceededBackupLimitAlertTestSendAlertForBackupStorageResourceType() {
        String accountUuid = UUID.randomUUID().toString();
        String expectedMessage = "Failed to create backup: backup storage space resource limit exceeded for account with ID: " + accountUuid + ".";
        String expectedAlertDetails = expectedMessage + " Please, use the 'updateResourceLimit' API to increase the backup limit.";

        backupManager.sendExceededBackupLimitAlert(accountUuid, Resource.ResourceType.backup_storage);
        verify(alertManagerMock).sendAlert(
                AlertManager.AlertType.ALERT_TYPE_UPDATE_RESOURCE_COUNT,
                0L,
                0L,
                expectedMessage,
                expectedAlertDetails
        );
    }

    @Test
    public void testCanCreateInstanceFromBackupAcrossZonesSuccess() {
        Long backupId = 1L;
        Long backupOfferingId = 2L;
        String providerName = "testbackupprovider";

        BackupVO backup = mock(BackupVO.class);
        when(backup.getBackupOfferingId()).thenReturn(backupOfferingId);

        BackupOfferingVO offering = mock(BackupOfferingVO.class);
        when(offering.getProvider()).thenReturn(providerName);

        BackupProvider backupProvider = mock(BackupProvider.class);
        when(backupProvider.crossZoneInstanceCreationEnabled(offering)).thenReturn(true);

        when(backupDao.findById(backupId)).thenReturn(backup);
        when(backupOfferingDao.findByIdIncludingRemoved(backupOfferingId)).thenReturn(offering);
        when(backupManager.getBackupProvider(providerName)).thenReturn(backupProvider);

        Boolean result = backupManager.canCreateInstanceFromBackupAcrossZones(backupId);

        assertTrue(result);
        verify(backupDao, times(1)).findById(backupId);
        verify(backupOfferingDao, times(1)).findByIdIncludingRemoved(backupOfferingId);
        verify(backupManager, times(1)).getBackupProvider(providerName);
        verify(backupProvider, times(1)).crossZoneInstanceCreationEnabled(offering);
    }

    @Test
    public void testCanCreateInstanceFromBackupAcrossZonesFalse() {
        Long backupId = 1L;
        Long backupOfferingId = 2L;
        String providerName = "testbackupprovider";

        BackupVO backup = mock(BackupVO.class);
        when(backup.getBackupOfferingId()).thenReturn(backupOfferingId);

        BackupOfferingVO offering = mock(BackupOfferingVO.class);
        when(offering.getProvider()).thenReturn(providerName);

        BackupProvider backupProvider = mock(BackupProvider.class);
        when(backupProvider.crossZoneInstanceCreationEnabled(offering)).thenReturn(false);

        when(backupDao.findById(backupId)).thenReturn(backup);
        when(backupOfferingDao.findByIdIncludingRemoved(backupOfferingId)).thenReturn(offering);
        when(backupManager.getBackupProvider(providerName)).thenReturn(backupProvider);

        Boolean result = backupManager.canCreateInstanceFromBackupAcrossZones(backupId);

        assertFalse(result);
        verify(backupDao, times(1)).findById(backupId);
        verify(backupOfferingDao, times(1)).findByIdIncludingRemoved(backupOfferingId);
        verify(backupManager, times(1)).getBackupProvider(providerName);
        verify(backupProvider, times(1)).crossZoneInstanceCreationEnabled(offering);
    }

    @Test
    public void testCanCreateInstanceFromBackupAcrossZonesOfferingNotFound() {
        Long backupId = 1L;
        Long backupOfferingId = 2L;

        BackupVO backup = mock(BackupVO.class);
        when(backup.getBackupOfferingId()).thenReturn(backupOfferingId);

        when(backupDao.findById(backupId)).thenReturn(backup);
        when(backupOfferingDao.findByIdIncludingRemoved(backupOfferingId)).thenReturn(null);

        CloudRuntimeException exception = Assert.assertThrows(CloudRuntimeException.class,
                () -> backupManager.canCreateInstanceFromBackupAcrossZones(backupId));

        assertEquals("Failed to find backup offering", exception.getMessage());
        verify(backupDao, times(1)).findById(backupId);
        verify(backupOfferingDao, times(1)).findByIdIncludingRemoved(backupOfferingId);
        verify(backupManager, never()).getBackupProvider(any(String.class));
    }

    @Test
    public void testRestoreBackupSuccess() throws NoTransitionException {
        Long backupId = 1L;
        Long vmId = 2L;
        Long zoneId = 3L;
        Long accountId = 4L;
        Long domainId = 5L;
        Long userId = 6L;
        Long offeringId = 7L;
        String vmInstanceName = "test-vm";
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;

        BackupVO backup = mock(BackupVO.class);
        when(backup.getVmId()).thenReturn(vmId);
        when(backup.getZoneId()).thenReturn(zoneId);
        when(backup.getStatus()).thenReturn(Backup.Status.BackedUp);
        when(backup.getBackupOfferingId()).thenReturn(offeringId);
        Backup.VolumeInfo volumeInfo = new Backup.VolumeInfo("uuid", "path", Volume.Type.ROOT, 1024L, 0L, "disk-offering-uuid", 1000L, 2000L);
        when(backup.getBackedUpVolumes()).thenReturn(List.of(volumeInfo));
        when(backup.getUuid()).thenReturn("backup-uuid");

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getId()).thenReturn(vmId);
        when(vm.getDataCenterId()).thenReturn(zoneId);
        when(vm.getDomainId()).thenReturn(domainId);
        when(vm.getAccountId()).thenReturn(accountId);
        when(vm.getUserId()).thenReturn(userId);
        when(vm.getInstanceName()).thenReturn(vmInstanceName);
        when(vm.getHypervisorType()).thenReturn(hypervisorType);
        when(vm.getState()).thenReturn(VirtualMachine.State.Stopped);
        when(vm.getRemoved()).thenReturn(null);
        when(vm.getBackupOfferingId()).thenReturn(offeringId);

        BackupOfferingVO offering = mock(BackupOfferingVO.class);
        when(offering.getProvider()).thenReturn("testbackupprovider");

        VolumeVO volume = mock(VolumeVO.class);
        when(volumeDao.findByInstance(vmId)).thenReturn(Collections.singletonList(volume));

        BackupProvider backupProvider = mock(BackupProvider.class);
        when(backupProvider.restoreVMFromBackup(vm, backup)).thenReturn(true);

        when(backupDao.findById(backupId)).thenReturn(backup);
        when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        when(backupOfferingDao.findByIdIncludingRemoved(offeringId)).thenReturn(offering);
        when(backupManager.getBackupProvider("testbackupprovider")).thenReturn(backupProvider);
        doReturn(true).when(backupManager).importRestoredVM(zoneId, domainId, accountId, userId, vmInstanceName, hypervisorType, backup);
        doNothing().when(backupManager).validateBackupForZone(any());
        when(virtualMachineManager.stateTransitTo(any(), any(), any())).thenReturn(true);

        try (MockedStatic<ActionEventUtils> utils = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onStartedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.eq(true), Mockito.eq(0))).thenReturn(1L);

            boolean result = backupManager.restoreBackup(backupId);

            assertTrue(result);
            verify(backupDao, times(1)).findById(backupId);
            verify(vmInstanceDao, times(1)).findByIdIncludingRemoved(vmId);
            verify(backupOfferingDao, times(2)).findByIdIncludingRemoved(offeringId);
            verify(backupProvider, times(1)).restoreVMFromBackup(vm, backup);
            verify(backupManager, times(1)).importRestoredVM(zoneId, domainId, accountId, userId, vmInstanceName, hypervisorType, backup);
        }
    }

    @Test
    public void testRestoreBackupBackupNotFound() {
        Long backupId = 1L;

        when(backupDao.findById(backupId)).thenReturn(null);

        CloudRuntimeException exception = Assert.assertThrows(CloudRuntimeException.class,
                () -> backupManager.restoreBackup(backupId));

        assertEquals("Backup " + backupId + " does not exist", exception.getMessage());
        verify(backupDao, times(1)).findById(backupId);
        verify(vmInstanceDao, never()).findByIdIncludingRemoved(any());
    }

    @Test
    public void testRestoreBackupBackupNotBackedUp() {
        Long backupId = 1L;

        BackupVO backup = mock(BackupVO.class);
        when(backup.getStatus()).thenReturn(Backup.Status.BackingUp);

        when(backupDao.findById(backupId)).thenReturn(backup);

        CloudRuntimeException exception = Assert.assertThrows(CloudRuntimeException.class,
                () -> backupManager.restoreBackup(backupId));

        assertEquals("Backup should be in BackedUp state", exception.getMessage());
        verify(backupDao, times(1)).findById(backupId);
        verify(vmInstanceDao, never()).findByIdIncludingRemoved(any());
    }

    @Test
    public void testRestoreBackupVmExpunging() {
        Long backupId = 1L;
        Long vmId = 2L;
        Long zoneId = 3L;

        BackupVO backup = mock(BackupVO.class);
        when(backup.getVmId()).thenReturn(vmId);
        when(backup.getZoneId()).thenReturn(zoneId);
        when(backup.getStatus()).thenReturn(Backup.Status.BackedUp);

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getState()).thenReturn(VirtualMachine.State.Expunging);

        when(backupDao.findById(backupId)).thenReturn(backup);
        when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        doNothing().when(backupManager).validateBackupForZone(any());

        CloudRuntimeException exception = Assert.assertThrows(CloudRuntimeException.class,
                () -> backupManager.restoreBackup(backupId));

        assertEquals("The Instance from which the backup was taken could not be found.", exception.getMessage());
        verify(backupDao, times(1)).findById(backupId);
        verify(vmInstanceDao, times(1)).findByIdIncludingRemoved(vmId);
    }

    @Test
    public void testRestoreBackupVmNotStopped() {
        Long backupId = 1L;
        Long vmId = 2L;
        Long zoneId = 3L;

        BackupVO backup = mock(BackupVO.class);
        when(backup.getVmId()).thenReturn(vmId);
        when(backup.getZoneId()).thenReturn(zoneId);
        when(backup.getStatus()).thenReturn(Backup.Status.BackedUp);

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        when(vm.getRemoved()).thenReturn(null);

        when(backupDao.findById(backupId)).thenReturn(backup);
        when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        doNothing().when(backupManager).validateBackupForZone(any());

        CloudRuntimeException exception = Assert.assertThrows(CloudRuntimeException.class,
                () -> backupManager.restoreBackup(backupId));

        assertEquals("Existing VM should be stopped before being restored from backup", exception.getMessage());
        verify(backupDao, times(1)).findById(backupId);
        verify(vmInstanceDao, times(1)).findByIdIncludingRemoved(vmId);
    }

    @Test
    public void testRestoreBackupVolumeMismatch() {
        Long backupId = 1L;
        Long vmId = 2L;
        Long zoneId = 3L;

        BackupVO backup = mock(BackupVO.class);
        when(backup.getVmId()).thenReturn(vmId);
        when(backup.getZoneId()).thenReturn(zoneId);
        when(backup.getStatus()).thenReturn(Backup.Status.BackedUp);
        when(backup.getBackedUpVolumes()).thenReturn(Collections.emptyList());

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getId()).thenReturn(vmId);
        when(vm.getState()).thenReturn(VirtualMachine.State.Destroyed);
        when(vm.getRemoved()).thenReturn(null);
        when(vm.getBackupVolumeList()).thenReturn(Collections.emptyList());

        VolumeVO volume = mock(VolumeVO.class);
        when(volumeDao.findByInstance(vmId)).thenReturn(Collections.singletonList(volume));

        when(backupDao.findById(backupId)).thenReturn(backup);
        when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        doNothing().when(backupManager).validateBackupForZone(any());

        try (MockedStatic<ActionEventUtils> utils = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onStartedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.eq(true), Mockito.eq(0))).thenReturn(1L);
            CloudRuntimeException exception = Assert.assertThrows(CloudRuntimeException.class,
                    () -> backupManager.restoreBackup(backupId));

            assertEquals("Unable to restore VM with the current backup as the backup has different number of disks as the VM", exception.getMessage());
        }
        verify(backupDao, times(1)).findById(backupId);
        verify(vmInstanceDao, times(1)).findByIdIncludingRemoved(vmId);
        verify(volumeDao, times(1)).findByInstance(vmId);
    }
}
