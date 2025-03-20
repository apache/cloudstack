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
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.DiskOfferingInfo;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.exception.ResourceAllocationException;
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
import com.cloud.user.UserVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.backup.ImportBackupOfferingCmd;
import org.apache.cloudstack.api.command.admin.backup.UpdateBackupOfferingCmd;
import org.apache.cloudstack.api.command.user.backup.CreateBackupCmd;
import org.apache.cloudstack.api.command.user.backup.CreateBackupScheduleCmd;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.BackupScheduleDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BackupManagerTest {
    @Spy
    @InjectMocks
    BackupManagerImpl backupManager = new BackupManagerImpl();

    @Mock
    BackupOfferingDao backupOfferingDao;

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
    AlertManager alertManager;

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

    private AccountVO account;
    private UserVO user;

    private String[] hostPossibleValues = {"127.0.0.1", "hostname"};
    private String[] datastoresPossibleValues = {"e9804933-8609-4de3-bccc-6278072a496c", "datastore-name"};
    private AutoCloseable closeable;
    private ConfigDepotImpl configDepotImpl;
    private boolean updatedConfigKeyDepot = false;

    @Before
    public void setup() throws Exception {
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
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>("i-2-3-VM", VirtualMachine.State.Running);

        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("127.0.0.1"), Mockito.eq("e9804933-8609-4de3-bccc-6278072a496c"), Mockito.eq(vmNameAndState))).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success"));
        Pair<Boolean, String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success", restoreBackedUpVolume.second());

        verify(backupProvider, times(1)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), any(Pair.class));
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
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>("i-2-3-VM", VirtualMachine.State.Running);
        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("127.0.0.1"), Mockito.eq("datastore-name"), Mockito.eq(vmNameAndState))).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success2"));
        Pair<Boolean, String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success2", restoreBackedUpVolume.second());

        verify(backupProvider, times(2)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), any(Pair.class));
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
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>("i-2-3-VM", VirtualMachine.State.Running);

        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("hostname"), Mockito.eq("e9804933-8609-4de3-bccc-6278072a496c"), Mockito.eq(vmNameAndState))).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success3"));
        Pair<Boolean, String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success3", restoreBackedUpVolume.second());

        verify(backupProvider, times(3)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), any(Pair.class));
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
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>("i-2-3-VM", VirtualMachine.State.Running);

        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("hostname"), Mockito.eq("datastore-name"), Mockito.eq(vmNameAndState))).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success4"));
        Pair<Boolean, String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success4", restoreBackedUpVolume.second());

        verify(backupProvider, times(4)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), any(Pair.class));
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
    public void testConfigureBackupScheduleLimitReached() {
        Long vmId = 1L;
        Long zoneId = 2L;
        Long accountId = 3L;
        Long domainId = 4L;

        CreateBackupScheduleCmd cmd = Mockito.mock(CreateBackupScheduleCmd.class);
        when(cmd.getVmId()).thenReturn(vmId);
        when(cmd.getTimezone()).thenReturn("GMT");
        when(cmd.getIntervalType()).thenReturn(DateUtil.IntervalType.DAILY);
        when(cmd.getMaxBackups()).thenReturn(8);

        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        when(vm.getDataCenterId()).thenReturn(zoneId);
        when(vm.getAccountId()).thenReturn(accountId);

        overrideBackupFrameworkConfigValue();

        Account account = Mockito.mock(Account.class);
        when(accountManager.getAccount(accountId)).thenReturn(account);
        when(account.getDomainId()).thenReturn(domainId);
        Domain domain = Mockito.mock(Domain.class);
        when(domainManager.getDomain(domainId)).thenReturn(domain);
        when(resourceLimitMgr.findCorrectResourceLimitForAccount(account, Resource.ResourceType.backup, null)).thenReturn(10L);
        when(resourceLimitMgr.findCorrectResourceLimitForDomain(domain, Resource.ResourceType.backup, null)).thenReturn(1L);

        InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class,
                () -> backupManager.configureBackupSchedule(cmd));
        Assert.assertEquals(exception.getMessage(), "Max number of backups shouldn't exceed the domain/account level backup limit");
    }

    @Test
    public void testCreateScheduledBackup() throws ResourceAllocationException {
        Long vmId = 1L;
        Long zoneId = 2L;
        Long scheduleId = 3L;
        Long backupOfferingId = 4L;
        Long accountId = 5L;
        Long backupId = 6L;
        Long oldestBackupId = 7L;
        Long newBackupSize = 1000000000L;
        Long oldBackupSize = 400000000L;

        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        when(vm.getId()).thenReturn(vmId);
        when(vm.getDataCenterId()).thenReturn(zoneId);
        when(vm.getBackupOfferingId()).thenReturn(backupOfferingId);
        when(vm.getAccountId()).thenReturn(accountId);

        overrideBackupFrameworkConfigValue();
        BackupOfferingVO offering = Mockito.mock(BackupOfferingVO.class);
        when(backupOfferingDao.findById(backupOfferingId)).thenReturn(offering);
        when(offering.isUserDrivenBackupAllowed()).thenReturn(true);
        when(offering.getProvider()).thenReturn("test");

        Account account = Mockito.mock(Account.class);
        when(accountManager.getAccount(accountId)).thenReturn(account);

        BackupScheduleVO schedule = mock(BackupScheduleVO.class);
        when(schedule.getScheduleType()).thenReturn(DateUtil.IntervalType.DAILY);
        when(schedule.getMaxBackups()).thenReturn(0);
        when(backupScheduleDao.findById(scheduleId)).thenReturn(schedule);
        when(backupScheduleDao.findByVMAndIntervalType(vmId, DateUtil.IntervalType.DAILY)).thenReturn(schedule);

        BackupProvider backupProvider = mock(BackupProvider.class);
        Backup backup = mock(Backup.class);
        when(backup.getId()).thenReturn(backupId);
        when(backup.getSize()).thenReturn(newBackupSize);
        when(backupProvider.getName()).thenReturn("test");
        when(backupProvider.takeBackup(vm)).thenReturn(new Pair<>(true, backup));
        Map<String, BackupProvider> backupProvidersMap = new HashMap<>();
        backupProvidersMap.put(backupProvider.getName().toLowerCase(), backupProvider);
        ReflectionTestUtils.setField(backupManager, "backupProvidersMap", backupProvidersMap);

        BackupVO backupVO = mock(BackupVO.class);
        when(backupVO.getId()).thenReturn(backupId);
        BackupVO oldestBackupVO = mock(BackupVO.class);
        when(oldestBackupVO.getSize()).thenReturn(oldBackupSize);
        when(oldestBackupVO.getId()).thenReturn(oldestBackupId);
        when(oldestBackupVO.getVmId()).thenReturn(vmId);
        when(oldestBackupVO.getBackupOfferingId()).thenReturn(backupOfferingId);
        when(oldestBackupVO.getZoneId()).thenReturn(zoneId);
        when(oldestBackupVO.getAccountId()).thenReturn(accountId);

        when(backupDao.findById(backupId)).thenReturn(backupVO);
        List<BackupVO> backups = new ArrayList<>(List.of(oldestBackupVO));
        when(backupDao.listBackupsByVMandIntervalType(vmId, Backup.Type.DAILY)).thenReturn(backups);
        when(backupDao.findByIdIncludingRemoved(oldestBackupId)).thenReturn(oldestBackupVO);
        when(backupOfferingDao.findByIdIncludingRemoved(backupOfferingId)).thenReturn(offering);
        when(backupProvider.deleteBackup(oldestBackupVO, false)).thenReturn(true);
        when(backupDao.remove(oldestBackupVO.getId())).thenReturn(true);

        CreateBackupCmd cmd = Mockito.mock(CreateBackupCmd.class);
        when(cmd.getVmId()).thenReturn(vmId);
        when(cmd.getScheduleId()).thenReturn(scheduleId);

        try (MockedStatic<ActionEventUtils> ignored = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(),
                    Mockito.anyLong(), Mockito.anyString())).thenReturn(1L);

            Assert.assertEquals(backupManager.createBackup(cmd), true);

            Mockito.verify(resourceLimitMgr, times(1)).incrementResourceCount(accountId, Resource.ResourceType.backup);
            Mockito.verify(resourceLimitMgr, times(1)).incrementResourceCount(accountId, Resource.ResourceType.backup_storage, newBackupSize);
            Mockito.verify(backupDao, times(1)).update(backupVO.getId(), backupVO);

            Mockito.verify(resourceLimitMgr, times(1)).decrementResourceCount(accountId, Resource.ResourceType.backup);
            Mockito.verify(resourceLimitMgr, times(1)).decrementResourceCount(accountId, Resource.ResourceType.backup_storage, oldBackupSize);
            Mockito.verify(backupDao, times(1)).remove(oldestBackupId);
        }
    }

    @Test (expected = ResourceAllocationException.class)
    public void testCreateBackupLimitReached() throws ResourceAllocationException {
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

        BackupScheduleVO schedule = mock(BackupScheduleVO.class);
        when(schedule.getScheduleType()).thenReturn(DateUtil.IntervalType.DAILY);
        when(backupScheduleDao.findById(scheduleId)).thenReturn(schedule);

        Account account = Mockito.mock(Account.class);
        when(account.getId()).thenReturn(accountId);
        when(accountManager.getAccount(accountId)).thenReturn(account);
        Mockito.doThrow(new ResourceAllocationException("", Resource.ResourceType.backup_storage)).when(resourceLimitMgr).checkResourceLimit(account, Resource.ResourceType.backup_storage, 0L);

        CreateBackupCmd cmd = Mockito.mock(CreateBackupCmd.class);
        when(cmd.getVmId()).thenReturn(vmId);
        when(cmd.getScheduleId()).thenReturn(scheduleId);

        backupManager.createBackup(cmd);

        String msg = "Backup storage space resource limit exceeded for account id : " + accountId + ". Failed to create backup";
        Mockito.verify(alertManager, times(1)).sendAlert(AlertManager.AlertType.ALERT_TYPE_UPDATE_RESOURCE_COUNT, 0L, 0L, msg, "Backup storage space resource limit exceeded for account id : " + accountId
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
        Long metricSize = 4 * Resource.ResourceType.bytesToGiB;

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
        when(vmInstanceDao.listByZoneWithBackups(dataCenterId, null)).thenReturn(List.of(vm));
        Backup.Metric metric = new Backup.Metric(metricSize, null);
        Map<VirtualMachine, Backup.Metric> metricMap = new HashMap<>();
        metricMap.put(vm, metric);
        when(backupProvider.getBackupMetrics(Mockito.anyLong(), Mockito.anyList())).thenReturn(metricMap);

        Backup.RestorePoint restorePoint1 = new Backup.RestorePoint(restorePoint1ExternalId, DateUtil.now(), "Root");
        Backup.RestorePoint restorePoint2 = new Backup.RestorePoint("12345", DateUtil.now(), "Root");
        List<Backup.RestorePoint> restorePoints = new ArrayList<>(List.of(restorePoint1, restorePoint2));
        when(backupProvider.listRestorePoints(vm)).thenReturn(restorePoints);

        BackupVO backupInDb1 = new BackupVO();
        backupInDb1.setSize(backup1Size);
        backupInDb1.setExternalId(restorePoint1ExternalId);

        BackupVO backupInDb2 = new BackupVO();
        backupInDb2.setSize(backup2Size);
        backupInDb2.setExternalId(null);
        ReflectionTestUtils.setField(backupInDb2, "id", backup2Id);
        when(backupDao.findById(backup2Id)).thenReturn(backupInDb2);

        when(backupDao.listByVmId(null, vmId)).thenReturn(List.of(backupInDb1, backupInDb2));

        BackupVO newBackupEntry = new BackupVO();
        newBackupEntry.setSize(newBackupSize);
        when(backupProvider.createNewBackupEntryForRestorePoint(restorePoint2, vm, metric)).thenReturn(newBackupEntry);

        try (MockedStatic<ActionEventUtils> ignored = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(),
                    Mockito.anyLong(), Mockito.anyString())).thenReturn(1L);

            try (MockedStatic<UsageEventUtils> ignored2 = Mockito.mockStatic(UsageEventUtils.class)) {

                BackupManagerImpl.BackupSyncTask backupSyncTask = backupManager.new BackupSyncTask(backupManager);
                backupSyncTask.runInContext();

                verify(resourceLimitMgr, times(1)).decrementResourceCount(accountId, Resource.ResourceType.backup_storage, backup1Size);
                verify(resourceLimitMgr, times(1)).incrementResourceCount(accountId, Resource.ResourceType.backup_storage, metricSize);
                Assert.assertEquals(backupInDb1.getSize(), metricSize);

                verify(resourceLimitMgr, times(1)).incrementResourceCount(accountId, Resource.ResourceType.backup);
                verify(resourceLimitMgr, times(1)).incrementResourceCount(accountId, Resource.ResourceType.backup_storage, newBackupSize);

                verify(resourceLimitMgr, times(1)).decrementResourceCount(accountId, Resource.ResourceType.backup);
                verify(resourceLimitMgr, times(1)).decrementResourceCount(accountId, Resource.ResourceType.backup_storage, backup2Size);
            }
        }
    }

    @Test
    public void testGetVmDetailsForBackup() {
        Long vmId = 1L;
        VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(vm.getServiceOfferingId()).thenReturn(1L);
        when(vm.getTemplateId()).thenReturn(1L);
        when(vm.getId()).thenReturn(vmId);

        ServiceOfferingVO serviceOffering = mock(ServiceOfferingVO.class);
        when(serviceOffering.getUuid()).thenReturn("service-offering-uuid");
        when(serviceOfferingDao.findById(1L)).thenReturn(serviceOffering);

        VMTemplateVO template = mock(VMTemplateVO.class);
        when(template.getUuid()).thenReturn("template-uuid");
        when(vmTemplateDao.findById(1L)).thenReturn(template);

        UserVmJoinVO userVmJoinVO = mock(UserVmJoinVO.class);
        when(userVmJoinVO.getNetworkUuid()).thenReturn("mocked-network-uuid");
        List<UserVmJoinVO> userVmJoinVOs = Collections.singletonList(userVmJoinVO);
        when(userVmJoinDao.searchByIds(vmId)).thenReturn(userVmJoinVOs);

        Map<String, String> details = backupManager.getVmDetailsForBackup(vm);

        assertEquals("KVM", details.get(ApiConstants.HYPERVISOR));
        assertEquals("service-offering-uuid", details.get(ApiConstants.SERVICE_OFFERING_ID));
        assertEquals("template-uuid", details.get(ApiConstants.TEMPLATE_ID));
        assertEquals("mocked-network-uuid", details.get(ApiConstants.NETWORK_IDS));
    }

    @Test
    public void testGetDiskOfferingDetailsForBackup() {
        Long vmId = 1L;
        VolumeVO volume = new VolumeVO(Volume.Type.DATADISK, null, 0, 0, 0, 0, null, 1024L, 100L, 1000L, null);
        volume.setDiskOfferingId(1L);
        volume.setSize(1024L);
        volume.setDeviceId(0L);
        volume.setMinIops(100L);
        volume.setMaxIops(200L);
        when(volumeDao.findByInstance(vmId)).thenReturn(Collections.singletonList(volume));

        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOffering.getUuid()).thenReturn("disk-offering-uuid");
        when(diskOfferingDao.findById(1L)).thenReturn(diskOffering);

        Map<String, String> details = backupManager.getDiskOfferingDetailsForBackup(vmId);

        assertEquals("disk-offering-uuid", details.get(ApiConstants.DISK_OFFERING_IDS));
        assertEquals("1024", details.get(ApiConstants.DISK_SIZES));
        assertEquals("100", details.get(ApiConstants.MIN_IOPS));
        assertEquals("200", details.get(ApiConstants.MAX_IOPS));
        assertEquals("0", details.get(ApiConstants.DEVICE_IDS));
    }

    @Test
    public void getDataDiskOfferingListFromBackup() {
        Long size1 = 5L * 1024 * 1024 * 1024;
        Long size2 = 10L * 1024 * 1024 * 1024;
        Backup backup = mock(Backup.class);
        when(backup.getDetail(ApiConstants.DISK_OFFERING_IDS)).thenReturn("root-disk-offering-uuid,disk-offering-uuid-1,disk-offering-uuid-2");
        when(backup.getDetail(ApiConstants.DEVICE_IDS)).thenReturn("0,1,2");
        when(backup.getDetail(ApiConstants.DISK_SIZES)).thenReturn("0," + size1 + "," + size2);
        when(backup.getDetail(ApiConstants.MIN_IOPS)).thenReturn("0,100,200");
        when(backup.getDetail(ApiConstants.MAX_IOPS)).thenReturn("0,300,400");

        DiskOfferingVO rootDiskOffering = mock(DiskOfferingVO.class);

        DiskOfferingVO diskOffering1 = mock(DiskOfferingVO.class);
        when(diskOffering1.getUuid()).thenReturn("disk-offering-uuid-1");
        when(diskOffering1.getState()).thenReturn(DiskOffering.State.Active);
        when(diskOffering1.isCustomizedIops()).thenReturn(true);

        DiskOfferingVO diskOffering2 = mock(DiskOfferingVO.class);
        when(diskOffering2.getUuid()).thenReturn("disk-offering-uuid-2");
        when(diskOffering2.getState()).thenReturn(DiskOffering.State.Active);
        when(diskOffering2.isCustomizedIops()).thenReturn(true);

        when(diskOfferingDao.findByUuid("disk-offering-uuid-1")).thenReturn(diskOffering1);
        when(diskOfferingDao.findByUuid("disk-offering-uuid-2")).thenReturn(diskOffering2);

        List<DiskOfferingInfo> diskOfferingInfoList = backupManager.getDataDiskOfferingListFromBackup(backup);

        assertEquals(2, diskOfferingInfoList.size());
        assertEquals("disk-offering-uuid-1", diskOfferingInfoList.get(0).getDiskOffering().getUuid());
        assertEquals(Long.valueOf(5), diskOfferingInfoList.get(0).getSize());
        assertEquals(Long.valueOf(1), diskOfferingInfoList.get(0).getDeviceId());
        assertEquals(Long.valueOf(100), diskOfferingInfoList.get(0).getMinIops());
        assertEquals(Long.valueOf(300), diskOfferingInfoList.get(0).getMaxIops());

        assertEquals("disk-offering-uuid-2", diskOfferingInfoList.get(1).getDiskOffering().getUuid());
        assertEquals(Long.valueOf(10), diskOfferingInfoList.get(1).getSize());
        assertEquals(Long.valueOf(2), diskOfferingInfoList.get(1).getDeviceId());
        assertEquals(Long.valueOf(200), diskOfferingInfoList.get(1).getMinIops());
        assertEquals(Long.valueOf(400), diskOfferingInfoList.get(1).getMaxIops());
    }

    @Test
    public void getDataDiskOfferingListFromBackupNullIops() {
        Long size = 5L * 1024 * 1024 * 1024;
        Backup backup = mock(Backup.class);
        when(backup.getDetail(ApiConstants.DISK_OFFERING_IDS)).thenReturn("disk-offering-uuid-1");
        when(backup.getDetail(ApiConstants.DEVICE_IDS)).thenReturn("1");
        when(backup.getDetail(ApiConstants.DISK_SIZES)).thenReturn("" + size);
        when(backup.getDetail(ApiConstants.MIN_IOPS)).thenReturn("null");
        when(backup.getDetail(ApiConstants.MAX_IOPS)).thenReturn("null");

        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOffering.getUuid()).thenReturn("disk-offering-uuid-1");
        when(diskOffering.isCustomizedIops()).thenReturn(true);
        when(diskOffering.getState()).thenReturn(DiskOffering.State.Active);

        when(diskOfferingDao.findByUuid("disk-offering-uuid-1")).thenReturn(diskOffering);

        List<DiskOfferingInfo> diskOfferingInfoList = backupManager.getDataDiskOfferingListFromBackup(backup);

        assertEquals(1, diskOfferingInfoList.size());
        assertEquals("disk-offering-uuid-1", diskOfferingInfoList.get(0).getDiskOffering().getUuid());
        assertEquals(Long.valueOf(5), diskOfferingInfoList.get(0).getSize());
        assertEquals(Long.valueOf(1), diskOfferingInfoList.get(0).getDeviceId());
        assertNull(diskOfferingInfoList.get(0).getMinIops());
        assertNull(diskOfferingInfoList.get(0).getMaxIops());
    }

    @Test
    public void testUpdateDiskOfferingSizeFromBackup() {
        Long sizeInBackup = 5L;
        Long sizeInCmd = 2L;
        Backup backup = mock(Backup.class);
        when(backup.getDetail(ApiConstants.DISK_OFFERING_IDS)).thenReturn("disk-offering-uuid-1");
        when(backup.getDetail(ApiConstants.DEVICE_IDS)).thenReturn("1");
        when(backup.getDetail(ApiConstants.DISK_SIZES)).thenReturn("" + sizeInBackup * 1024 * 1024 * 1024);
        when(backup.getDetail(ApiConstants.MIN_IOPS)).thenReturn("null");
        when(backup.getDetail(ApiConstants.MAX_IOPS)).thenReturn("null");

        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOffering.isCustomizedIops()).thenReturn(true);
        when(diskOffering.getState()).thenReturn(DiskOffering.State.Active);

        when(diskOfferingDao.findByUuid("disk-offering-uuid-1")).thenReturn(diskOffering);
        List<DiskOfferingInfo> diskOfferingInfoList = List.of(new DiskOfferingInfo(diskOffering, sizeInCmd, 1L, null, null));

        backupManager.updateDiskOfferingSizeFromBackup(diskOfferingInfoList, backup);

        assertEquals(sizeInBackup, diskOfferingInfoList.get(0).getSize());
    }

    @Test
    public void testGetRootDiskOfferingInfoFromBackup() {
        Long size = 5L * 1024 * 1024 * 1024;
        Backup backup = mock(Backup.class);
        when(backup.getDetail(ApiConstants.DISK_OFFERING_IDS)).thenReturn("root-disk-offering-uuid");
        when(backup.getDetail(ApiConstants.DEVICE_IDS)).thenReturn("0");
        when(backup.getDetail(ApiConstants.DISK_SIZES)).thenReturn("" + size);

        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOffering.getUuid()).thenReturn("root-disk-offering-uuid");
        when(diskOfferingDao.findByUuid("root-disk-offering-uuid")).thenReturn(diskOffering);

        DiskOfferingInfo diskOfferingInfo = backupManager.getRootDiskOfferingInfoFromBackup(backup);

        assertEquals("root-disk-offering-uuid", diskOfferingInfo.getDiskOffering().getUuid());
        assertEquals(Long.valueOf(5), diskOfferingInfo.getSize());
        assertEquals(Long.valueOf(0), diskOfferingInfo.getDeviceId());
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
        List<VolumeVO> volumes = new ArrayList<>();
        VolumeVO volume1 = new VolumeVO(Volume.Type.ROOT, "vol1", 1L, 2L, 3L,
                4L, null, 1024L, 0L, 0L, null);
        volume1.setUuid("uuid1");
        volume1.setPath("path1");
        volume1.setVolumeType(Volume.Type.ROOT);
        volumes.add(volume1);

        VolumeVO volume2 = new VolumeVO(Volume.Type.ROOT, "vol2", 1L, 2L, 3L,
                4L, null, 2048L, 0L, 0L, null);
        volume2.setUuid("uuid2");
        volume2.setPath("path2");
        volume2.setVolumeType(Volume.Type.DATADISK);
        volumes.add(volume2);

        String expectedJson = "[{\"uuid\":\"uuid1\",\"type\":\"ROOT\",\"size\":1024,\"path\":\"path1\"},{\"uuid\":\"uuid2\",\"type\":\"DATADISK\",\"size\":2048,\"path\":\"path2\"}]";
        String actualJson = BackupManagerImpl.createVolumeInfoFromVolumes(volumes);

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
        boolean forced = true;

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(vm);
        when(vm.getDataCenterId()).thenReturn(1L);
        when(vm.getBackupOfferingId()).thenReturn(2L);

        BackupOfferingVO offering = mock(BackupOfferingVO.class);
        when(backupOfferingDao.findById(vm.getBackupOfferingId())).thenReturn(offering);
        when(offering.getProvider()).thenReturn("testbackupprovider");
        when(backupProvider.removeVMFromBackupOffering(vm)).thenReturn(true);

        overrideBackupFrameworkConfigValue();

        try (MockedStatic<UsageEventUtils> ignored = Mockito.mockStatic(UsageEventUtils.class)) {
            boolean result = backupManager.removeVMFromBackupOffering(vmId, forced);

            assertTrue(result);
            verify(vmInstanceDao, times(1)).findByIdIncludingRemoved(vmId);
            verify(backupOfferingDao, times(1)).findById(vm.getBackupOfferingId());
            verify(backupManager, times(1)).getBackupProvider("testbackupprovider");
        }
    }

    private
    void setupRestoreBackupToVMMocks() {

    }

    @Test
    public void testRestoreBackupToVM() throws NoTransitionException {
        Long backupId = 1L;
        Long vmId = 2L;
        Long hostId = 3L;
        Long offeringId = 4L;
        Long poolId = 5L;

        setupRestoreBackupToVMMocks();

        BackupVO backup = mock(BackupVO.class);
        when(backup.getBackupOfferingId()).thenReturn(offeringId);

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
        when(backupProvider.restoreBackupToVM(vm, backup, null, null)).thenReturn(true);

        try (MockedStatic<ActionEventUtils> utils = Mockito.mockStatic(ActionEventUtils.class)) {
            boolean result = backupManager.restoreBackupToVM(backupId, vmId);

            assertTrue(result);
            verify(backupProvider, times(1)).restoreBackupToVM(vm, backup, null, null);
            verify(virtualMachineManager, times(1)).stateTransitTo(vm, VirtualMachine.Event.RestoringRequested, hostId);
            verify(virtualMachineManager, times(1)).stateTransitTo(vm, VirtualMachine.Event.RestoringSuccess, hostId);
        } catch (ResourceUnavailableException e) {
            fail("Test failed due to exception" + e);
        }
    }

    @Test
    public void testUpdateOrphanedBackups() {
        Long vmId = 1L;
        Long zoneId = 2L;
        Long backupId1 = 3L;
        Long backupId2 = 4L;

        VirtualMachine vm = mock(VirtualMachine.class);
        when(vm.getDataCenterId()).thenReturn(1L);
        when(vm.getId()).thenReturn(vmId);
        when(vm.getHostName()).thenReturn("test-vm");
        when(vm.getDataCenterId()).thenReturn(zoneId);

        Backup backup1 = mock(Backup.class);
        Backup backup2 = mock(Backup.class);
        when(backup1.getId()).thenReturn(backupId1);
        when(backup2.getId()).thenReturn(backupId2);
        List<Backup> backups = List.of(backup1, backup2);
        when(backupDao.listByVmId(zoneId, vmId)).thenReturn(backups);

        BackupVO backupVO1 = new BackupVO();
        BackupVO backupVO2 = new BackupVO();
        when(backupDao.findById(backupId1)).thenReturn(backupVO1);
        when(backupDao.findById(backupId2)).thenReturn(backupVO2);

        backupManager.updateOrphanedBackups(vm);

        Assert.assertEquals(null, backupVO1.getVmId());
        Assert.assertEquals("test-vm", backupVO1.getVmName());
        Assert.assertEquals(null, backupVO2.getVmId());
        Assert.assertEquals("test-vm", backupVO2.getVmName());
        for (Backup backup : backups) {
            verify(backupDao).update(Mockito.eq(backup.getId()), any(BackupVO.class));
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
}
