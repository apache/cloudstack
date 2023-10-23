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

package org.apache.cloudstack.quota.activationrule.presetvariables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.RoleVO;
import org.apache.cloudstack.acl.dao.RoleDao;
import org.apache.cloudstack.backup.BackupOfferingVO;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.VmTemplateDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.usage.UsageTypes;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostTagsDao;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolTagsDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@RunWith(MockitoJUnitRunner.class)
public class PresetVariableHelperTest {

    @Mock
    AccountDao accountDaoMock;

    @Mock
    DataCenterDao dataCenterDaoMock;

    @Mock
    DiskOfferingDao diskOfferingDaoMock;

    @Mock
    DomainDao domainDaoMock;

    @Mock
    GuestOSDao guestOsDaoMock;

    @Mock
    HostDao hostDaoMock;

    @Mock
    HostTagsDao hostTagsDaoMock;

    @Mock
    ImageStoreDao imageStoreDaoMock;

    @Mock
    NetworkOfferingDao networkOfferingDaoMock;

    @Mock
    PrimaryDataStoreDao primaryStorageDaoMock;

    @Mock
    ResourceTagDao resourceTagDaoMock;

    @Mock
    RoleDao roleDaoMock;

    @Mock
    ServiceOfferingDao serviceOfferingDaoMock;

    @Mock
    SnapshotDao snapshotDaoMock;

    @Mock
    SnapshotDataStoreDao snapshotDataStoreDaoMock;

    @Mock
    StoragePoolTagsDao storagePoolTagsDaoMock;

    @Mock
    UsageDao usageDaoMock;

    @Mock
    VMInstanceDao vmInstanceDaoMock;

    @Mock
    VMSnapshotDao vmSnapshotDaoMock;

    @Mock
    VmTemplateDao vmTemplateDaoMock;

    @Mock
    VolumeDao volumeDaoMock;

    @Mock
    UserVmDetailsDao userVmDetailsDaoMock;

    @InjectMocks
    PresetVariableHelper presetVariableHelperSpy = Mockito.spy(PresetVariableHelper.class);

    @Mock
    UsageVO usageVoMock;

    @Mock
    VMInstanceVO vmInstanceVoMock;

    @Mock
    ServiceOfferingVO serviceOfferingVoMock;

    @Mock
    BackupOfferingDao backupOfferingDaoMock;

    List<Integer> runningAndAllocatedVmUsageTypes = Arrays.asList(UsageTypes.RUNNING_VM, UsageTypes.ALLOCATED_VM);
    List<Integer> templateAndIsoUsageTypes = Arrays.asList(UsageTypes.TEMPLATE, UsageTypes.ISO);

    private Account getAccountForTests() {
        Account account = new Account();
        account.setId("account_id");
        account.setName("account_name");
        return account;
    }

    private Domain getDomainForTests() {
        Domain domain = new Domain();
        domain.setId("domain_id");
        domain.setName("domain_name");
        domain.setPath("domain_path");
        return domain;
    }

    private Value getValueForTests() {
        Value value = new Value();
        value.setId("value_id");
        value.setName("value_name");
        value.setOsName("value_os_name");
        value.setComputeOffering(getComputeOfferingForTests());
        value.setTags(Collections.singletonMap("tag1", "value1"));
        value.setTemplate(getGenericPresetVariableForTests());
        value.setDiskOffering(getGenericPresetVariableForTests());
        value.setProvisioningType(ProvisioningType.THIN);
        value.setStorage(getStorageForTests());
        value.setSize(ByteScaleUtils.GiB);
        value.setSnapshotType(Snapshot.Type.HOURLY);
        value.setTag("tag_test");
        value.setVmSnapshotType(VMSnapshot.Type.Disk);
        value.setComputingResources(getComputingResourcesForTests());
        return value;
    }

    private ComputingResources getComputingResourcesForTests() {
        ComputingResources computingResources = new ComputingResources();
        computingResources.setCpuNumber(1);
        computingResources.setCpuSpeed(1000);
        computingResources.setMemory(512);
        return computingResources;
    }

    private ComputeOffering getComputeOfferingForTests() {
        ComputeOffering computeOffering = new ComputeOffering();
        computeOffering.setId("compute_offering_id");
        computeOffering.setName("compute_offering_name");
        computeOffering.setCustomized(false);
        return computeOffering;
    }

    private Host getHostForTests() {
        Host host = new Host();
        host.setId("host_id");
        host.setName("host_name");
        host.setTags(Arrays.asList("tag1", "tag2"));
        return host;
    }

    private Storage getStorageForTests() {
        Storage storage = new Storage();
        storage.setId("storage_id");
        storage.setName("storage_name");
        storage.setTags(Arrays.asList("tag1", "tag2"));
        storage.setScope(ScopeType.ZONE);
        return storage;
    }

    private Set<Map.Entry<Integer, QuotaTypes>> getQuotaTypesForTests(Integer... typesToRemove) {
        Map<Integer, QuotaTypes> quotaTypesMap = new LinkedHashMap<>(QuotaTypes.listQuotaTypes());

        if (ArrayUtils.isNotEmpty(typesToRemove)) {
            for (Integer type : typesToRemove) {
                quotaTypesMap.remove(type);
            }
        }

        return quotaTypesMap.entrySet();
    }

    private List<UserVmDetailVO> getVmDetailsForTests() {
        List<UserVmDetailVO> details = new LinkedList<>();
        details.add(new UserVmDetailVO(1l, "test_with_value", "277", false));
        details.add(new UserVmDetailVO(1l, "test_with_invalid_value", "invalid", false));
        details.add(new UserVmDetailVO(1l, "test_with_null", null, false));
        return details;
    }

    private void assertPresetVariableIdAndName(GenericPresetVariable expected, GenericPresetVariable result) {
        Assert.assertEquals(expected.getId(), result.getId());
        Assert.assertEquals(expected.getName(), result.getName());
    }

    private void validateFieldNamesToIncludeInToString(List<String> expected, GenericPresetVariable resultObject) {
        List<String> result = new ArrayList<>(resultObject.fieldNamesToIncludeInToString);
        Collections.sort(expected);
        Collections.sort(result);
        Assert.assertEquals(expected, result);
    }

    private BackupOffering getBackupOfferingForTests() {
        BackupOffering backupOffering = new BackupOffering();
        backupOffering.setId("backup_offering_id");
        backupOffering.setName("backup_offering_name");
        backupOffering.setExternalId("backup_offering_external_id");
        return backupOffering;
    }

    private void mockMethodValidateIfObjectIsNull() {
        Mockito.doNothing().when(presetVariableHelperSpy).validateIfObjectIsNull(Mockito.any(), Mockito.anyLong(), Mockito.anyString());
    }

    private GenericPresetVariable getGenericPresetVariableForTests() {
        GenericPresetVariable gpv = new GenericPresetVariable();
        gpv.setId("test_id");
        gpv.setName("test_name");
        return gpv;
    }

    @Test
    public void getPresetVariablesTestSetFieldsAndReturnObject() {
        PresetVariables expected = new PresetVariables();
        expected.setAccount(getAccountForTests());
        expected.setDomain(getDomainForTests());
        expected.setValue(new Value());
        expected.setZone(getGenericPresetVariableForTests());

        Mockito.doReturn(expected.getAccount()).when(presetVariableHelperSpy).getPresetVariableAccount(Mockito.anyLong());
        Mockito.doNothing().when(presetVariableHelperSpy).setPresetVariableProject(Mockito.any());
        Mockito.doReturn(expected.getDomain()).when(presetVariableHelperSpy).getPresetVariableDomain(Mockito.anyLong());
        Mockito.doReturn(expected.getValue()).when(presetVariableHelperSpy).getPresetVariableValue(Mockito.any(UsageVO.class));
        Mockito.doReturn(expected.getZone()).when(presetVariableHelperSpy).getPresetVariableZone(Mockito.anyLong());

        PresetVariables result = presetVariableHelperSpy.getPresetVariables(usageVoMock);

        Assert.assertEquals(expected.getAccount(), result.getAccount());
        Assert.assertEquals(expected.getDomain(), result.getDomain());
        Assert.assertEquals(expected.getValue(), result.getValue());
        Assert.assertEquals(expected.getZone(), result.getZone());
    }

    @Test
    public void setPresetVariableProjectTestAccountWithRoleDoNotSetAsProject() {
        PresetVariables result = new PresetVariables();
        result.setAccount(new Account());
        result.getAccount().setRole(new Role());

        presetVariableHelperSpy.setPresetVariableProject(result);

        Assert.assertNull(result.getProject());
    }

    @Test
    public void setPresetVariableProjectTestAccountWithoutRoleSetAsProject() {
        PresetVariables result = new PresetVariables();
        Account account = getAccountForTests();
        result.setAccount(account);

        presetVariableHelperSpy.setPresetVariableProject(result);

        Assert.assertNotNull(result.getProject());
        assertPresetVariableIdAndName(account, result.getProject());
        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name"), result.getProject());
    }

    @Test
    public void getPresetVariableAccountTestSetValuesAndReturnObject() {
        AccountVO accountVoMock = Mockito.mock(AccountVO.class);
        Mockito.doReturn(accountVoMock).when(accountDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        mockMethodValidateIfObjectIsNull();
        Mockito.doNothing().when(presetVariableHelperSpy).setPresetVariableRoleInAccountIfAccountIsNotAProject(Mockito.any(), Mockito.anyLong(), Mockito.any(Account.class));

        Account account = getAccountForTests();
        Mockito.doReturn(account.getId()).when(accountVoMock).getUuid();
        Mockito.doReturn(account.getName()).when(accountVoMock).getName();

        Account result = presetVariableHelperSpy.getPresetVariableAccount(1l);

        assertPresetVariableIdAndName(account, result);
        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name"), result);
    }

    @Test
    public void setPresetVariableRoleInAccountIfAccountIsNotAProjectTestAllCases() {
        Mockito.doReturn(new Role()).when(presetVariableHelperSpy).getPresetVariableRole(Mockito.anyLong());


        for (com.cloud.user.Account.Type type : com.cloud.user.Account.Type.values()) {
            Account account = new Account();
            presetVariableHelperSpy.setPresetVariableRoleInAccountIfAccountIsNotAProject(type, 1L, account);

            if (com.cloud.user.Account.Type.PROJECT == type) {
                Assert.assertNull(account.getRole());
            } else {
                Assert.assertNotNull(account.getRole());
            }
      }
    }

    @Test
    public void getPresetVariableRoleTestSetValuesAndReturnObject() {
        RoleVO roleVoMock = Mockito.mock(RoleVO.class);
        Mockito.doReturn(roleVoMock).when(roleDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        mockMethodValidateIfObjectIsNull();

        Arrays.asList(RoleType.values()).forEach(roleType -> {
            Role role = new Role();
            role.setId("test_id");
            role.setName("test_name");
            role.setType(roleType);

            Mockito.doReturn(role.getId()).when(roleVoMock).getUuid();
            Mockito.doReturn(role.getName()).when(roleVoMock).getName();
            Mockito.doReturn(role.getType()).when(roleVoMock).getRoleType();

            Role result = presetVariableHelperSpy.getPresetVariableRole(1l);

            assertPresetVariableIdAndName(role, result);
            Assert.assertEquals(role.getType(), result.getType());

            validateFieldNamesToIncludeInToString(Arrays.asList("id", "name", "type"), result);
        });
    }

    @Test
    public void getPresetVariableDomainTestSetValuesAndReturnObject() {
        DomainVO domainVoMock = Mockito.mock(DomainVO.class);
        Mockito.doReturn(domainVoMock).when(domainDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        mockMethodValidateIfObjectIsNull();

        Domain domain = getDomainForTests();
        Mockito.doReturn(domain.getId()).when(domainVoMock).getUuid();
        Mockito.doReturn(domain.getName()).when(domainVoMock).getName();
        Mockito.doReturn(domain.getPath()).when(domainVoMock).getPath();

        Domain result = presetVariableHelperSpy.getPresetVariableDomain(1l);

        assertPresetVariableIdAndName(domain, result);
        Assert.assertEquals(domain.getPath(), result.getPath());

        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name", "path"), result);
    }

    @Test
    public void getPresetVariableZoneTestSetValuesAndReturnObject() {
        DataCenterVO dataCenterVoMock = Mockito.mock(DataCenterVO.class);
        Mockito.doReturn(dataCenterVoMock).when(dataCenterDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        mockMethodValidateIfObjectIsNull();

        GenericPresetVariable expected = getGenericPresetVariableForTests();
        Mockito.doReturn(expected.getId()).when(dataCenterVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(dataCenterVoMock).getName();

        GenericPresetVariable result = presetVariableHelperSpy.getPresetVariableZone(1l);

        assertPresetVariableIdAndName(expected, result);
        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name"), result);
    }

    @Test
    public void getPresetVariableValueTestSetFieldsAndReturnObject() {
        List<Resource> resources = Arrays.asList(new Resource(), new Resource());

        Mockito.doReturn(resources).when(presetVariableHelperSpy).getPresetVariableAccountResources(Mockito.any(UsageVO.class), Mockito.anyLong(), Mockito.anyInt());
        Mockito.doNothing().when(presetVariableHelperSpy).loadPresetVariableValueForRunningAndAllocatedVm(Mockito.any(UsageVO.class), Mockito.any(Value.class));
        Mockito.doNothing().when(presetVariableHelperSpy).loadPresetVariableValueForVolume(Mockito.any(UsageVO.class), Mockito.any(Value.class));
        Mockito.doNothing().when(presetVariableHelperSpy).loadPresetVariableValueForTemplateAndIso(Mockito.any(UsageVO.class), Mockito.any(Value.class));
        Mockito.doNothing().when(presetVariableHelperSpy).loadPresetVariableValueForSnapshot(Mockito.any(UsageVO.class), Mockito.any(Value.class));
        Mockito.doNothing().when(presetVariableHelperSpy).loadPresetVariableValueForNetworkOffering(Mockito.any(UsageVO.class), Mockito.any(Value.class));
        Mockito.doNothing().when(presetVariableHelperSpy).loadPresetVariableValueForVmSnapshot(Mockito.any(UsageVO.class), Mockito.any(Value.class));
        Mockito.doNothing().when(presetVariableHelperSpy).loadPresetVariableValueForBackup(Mockito.any(UsageVO.class), Mockito.any(Value.class));

        Value result = presetVariableHelperSpy.getPresetVariableValue(usageVoMock);

        Assert.assertEquals(resources, result.getAccountResources());
        validateFieldNamesToIncludeInToString(Arrays.asList("accountResources"), result);
    }

    @Test
    public void getPresetVariableAccountResourcesTestSetFieldsAndReturnObject() {
        List<Pair<String, String>> expected = Arrays.asList(new Pair<>("zoneId1", "domainId2"), new Pair<>("zoneId3", "domainId4"));
        Mockito.doReturn(new Date()).when(usageVoMock).getStartDate();
        Mockito.doReturn(new Date()).when(usageVoMock).getEndDate();
        Mockito.doReturn(expected).when(usageDaoMock).listAccountResourcesInThePeriod(Mockito.anyLong(), Mockito.anyInt(), Mockito.any(Date.class), Mockito.any(Date.class));

        List<Resource> result = presetVariableHelperSpy.getPresetVariableAccountResources(usageVoMock, 1l, 0);

        for (int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(expected.get(i).first(), result.get(i).getZoneId());
            Assert.assertEquals(expected.get(i).second(), result.get(i).getDomainId());
        }
    }

    @Test
    public void loadPresetVariableValueForRunningAndAllocatedVmTestRecordIsNotARunningNorAnAllocatedVmDoNothing() {
        getQuotaTypesForTests(runningAndAllocatedVmUsageTypes.toArray(new Integer[0])).forEach(type -> {
            Mockito.doReturn(type.getKey()).when(usageVoMock).getUsageType();
            presetVariableHelperSpy.loadPresetVariableValueForRunningAndAllocatedVm(usageVoMock, null);
        });

        Mockito.verifyNoInteractions(vmInstanceDaoMock);
    }

    @Test
    public void loadPresetVariableValueForRunningAndAllocatedVmTestRecordIsRunningOrAllocatedVmSetFields() {
        Value expected = getValueForTests();

        Mockito.doReturn(vmInstanceVoMock).when(vmInstanceDaoMock).findByIdIncludingRemoved(Mockito.anyLong());

        mockMethodValidateIfObjectIsNull();

        Mockito.doNothing().when(presetVariableHelperSpy).setPresetVariableHostInValueIfUsageTypeIsRunningVm(Mockito.any(Value.class), Mockito.anyInt(),
                Mockito.any(VMInstanceVO.class));

        Mockito.doReturn(expected.getId()).when(vmInstanceVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(vmInstanceVoMock).getHostName();
        Mockito.doReturn(expected.getOsName()).when(presetVariableHelperSpy).getPresetVariableValueOsName(Mockito.anyLong());
        Mockito.doNothing().when(presetVariableHelperSpy).setPresetVariableValueServiceOfferingAndComputingResources(Mockito.any(), Mockito.anyInt(), Mockito.any());
        Mockito.doReturn(expected.getTags()).when(presetVariableHelperSpy).getPresetVariableValueResourceTags(Mockito.anyLong(), Mockito.any(ResourceObjectType.class));
        Mockito.doReturn(expected.getTemplate()).when(presetVariableHelperSpy).getPresetVariableValueTemplate(Mockito.anyLong());

        runningAndAllocatedVmUsageTypes.forEach(type -> {
            Mockito.doReturn(type).when(usageVoMock).getUsageType();

            Value result = new Value();
            presetVariableHelperSpy.loadPresetVariableValueForRunningAndAllocatedVm(usageVoMock, result);

            assertPresetVariableIdAndName(expected, result);
            Assert.assertEquals(expected.getOsName(), result.getOsName());
            Assert.assertEquals(expected.getTags(), result.getTags());
            Assert.assertEquals(expected.getTemplate(), result.getTemplate());

            validateFieldNamesToIncludeInToString(Arrays.asList("id", "name", "osName", "tags", "template"), result);
        });

        Mockito.verify(presetVariableHelperSpy, Mockito.times(runningAndAllocatedVmUsageTypes.size())).getPresetVariableValueResourceTags(Mockito.anyLong(),
                Mockito.eq(ResourceObjectType.UserVm));
    }

    @Test
    public void setPresetVariableHostInValueIfUsageTypeIsRunningVmTestQuotaTypeDifferentFromRunningVmDoNothing() {
        getQuotaTypesForTests(UsageTypes.RUNNING_VM).forEach(type -> {
            Value result = new Value();

            presetVariableHelperSpy.setPresetVariableHostInValueIfUsageTypeIsRunningVm(result, type.getKey(), vmInstanceVoMock);

            Assert.assertNull(result.getHost());
        });
    }

    @Test
    public void setPresetVariableHostInValueIfUsageTypeIsRunningVmTestQuotaTypeIsRunningVmSetHost() {
        Value result = new Value();
        Host expected = getHostForTests();

        Mockito.doReturn(expected).when(presetVariableHelperSpy).getPresetVariableValueHost(Mockito.anyLong());
        presetVariableHelperSpy.setPresetVariableHostInValueIfUsageTypeIsRunningVm(result, UsageTypes.RUNNING_VM, vmInstanceVoMock);

        Assert.assertNotNull(result.getHost());

        assertPresetVariableIdAndName(expected, result.getHost());
        Assert.assertEquals(expected.getTags(), result.getHost().getTags());
        validateFieldNamesToIncludeInToString(Arrays.asList("host"), result);
    }

    @Test
    public void getPresetVariableValueHostTestSetFieldsAndReturnObject() {
        Host expected = getHostForTests();
        HostVO hostVoMock = Mockito.mock(HostVO.class);

        Mockito.doReturn(hostVoMock).when(hostDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        mockMethodValidateIfObjectIsNull();
        Mockito.doReturn(expected.getId()).when(hostVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(hostVoMock).getName();
        Mockito.doReturn(expected.getTags()).when(hostTagsDaoMock).getHostTags(Mockito.anyLong());

        Host result = presetVariableHelperSpy.getPresetVariableValueHost(1l);

        assertPresetVariableIdAndName(expected, result);
        Assert.assertEquals(expected.getTags(), result.getTags());
        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name", "tags"), result);
    }

    @Test
    public void getPresetVariableValueOsNameTestReturnDisplayName() {
        GuestOSVO guestOsVoMock = Mockito.mock(GuestOSVO.class);
        Mockito.doReturn(guestOsVoMock).when(guestOsDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        mockMethodValidateIfObjectIsNull();

        String expected = "os_display_name";
        Mockito.doReturn(expected).when(guestOsVoMock).getDisplayName();

        String result = presetVariableHelperSpy.getPresetVariableValueOsName(1l);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getPresetVariableValueComputeOfferingTestSetFieldsAndReturnObject() {
        ComputeOffering expected = getComputeOfferingForTests();
        Mockito.doReturn(expected.getId()).when(serviceOfferingVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(serviceOfferingVoMock).getName();
        Mockito.doReturn(expected.isCustomized()).when(serviceOfferingVoMock).isDynamic();

        ComputeOffering result = presetVariableHelperSpy.getPresetVariableValueComputeOffering(serviceOfferingVoMock);

        assertPresetVariableIdAndName(expected, result);
        Assert.assertEquals(expected.isCustomized(), result.isCustomized());
        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name", "customized"), result);
    }

    @Test
    public void getPresetVariableValueTemplateTestSetValuesAndReturnObject() {
        VMTemplateVO vmTemplateVoMock = Mockito.mock(VMTemplateVO.class);
        Mockito.doReturn(vmTemplateVoMock).when(vmTemplateDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        mockMethodValidateIfObjectIsNull();

        GenericPresetVariable expected = getGenericPresetVariableForTests();
        Mockito.doReturn(expected.getId()).when(vmTemplateVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(vmTemplateVoMock).getName();

        GenericPresetVariable result = presetVariableHelperSpy.getPresetVariableValueTemplate(1l);

        assertPresetVariableIdAndName(expected, result);
        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name"), result);
    }

    @Test
    public void getPresetVariableValueResourceTagsTestAllCases() {
        List<ResourceTag> listExpected = new ArrayList<>();
        listExpected.add(new ResourceTagVO("tag1", "value2", 0, 0, 0, null, null, null));
        listExpected.add(new ResourceTagVO("tag3", "value4", 0, 0, 0, null, null, null));

        Mockito.doReturn(listExpected).when(resourceTagDaoMock).listBy(Mockito.anyLong(), Mockito.any(ResourceObjectType.class));

        Arrays.asList(ResourceObjectType.values()).forEach(type -> {
            Map<String, String> result = presetVariableHelperSpy.getPresetVariableValueResourceTags(1l, type);

            for (ResourceTag expected: listExpected) {
                Assert.assertEquals(expected.getValue(), result.get(expected.getKey()));
            }
        });
    }

    @Test
    public void loadPresetVariableValueForVolumeTestRecordIsNotAVolumeDoNothing() {
        getQuotaTypesForTests(UsageTypes.VOLUME).forEach(type -> {
            Mockito.doReturn(type.getKey()).when(usageVoMock).getUsageType();
            presetVariableHelperSpy.loadPresetVariableValueForVolume(usageVoMock, null);
        });

        Mockito.verifyNoInteractions(volumeDaoMock);
    }

    @Test
    public void loadPresetVariableValueForVolumeTestRecordIsVolumeAndHasStorageSetFields() {
        Value expected = getValueForTests();

        VolumeVO volumeVoMock = Mockito.mock(VolumeVO.class);
        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        Mockito.doReturn(1l).when(volumeVoMock).getPoolId();

        mockMethodValidateIfObjectIsNull();

        Mockito.doReturn(expected.getId()).when(volumeVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(volumeVoMock).getName();
        Mockito.doReturn(expected.getDiskOffering()).when(presetVariableHelperSpy).getPresetVariableValueDiskOffering(Mockito.anyLong());
        Mockito.doReturn(expected.getProvisioningType()).when(volumeVoMock).getProvisioningType();
        Mockito.doReturn(expected.getStorage()).when(presetVariableHelperSpy).getPresetVariableValueStorage(Mockito.anyLong(), Mockito.anyInt());
        Mockito.doReturn(expected.getTags()).when(presetVariableHelperSpy).getPresetVariableValueResourceTags(Mockito.anyLong(), Mockito.any(ResourceObjectType.class));
        Mockito.doReturn(expected.getSize()).when(volumeVoMock).getSize();

        Mockito.doReturn(UsageTypes.VOLUME).when(usageVoMock).getUsageType();

        Value result = new Value();
        presetVariableHelperSpy.loadPresetVariableValueForVolume(usageVoMock, result);

        Long expectedSize = ByteScaleUtils.bytesToMebibytes(expected.getSize());

        assertPresetVariableIdAndName(expected, result);
        Assert.assertEquals(expected.getDiskOffering(), result.getDiskOffering());
        Assert.assertEquals(expected.getProvisioningType(), result.getProvisioningType());
        Assert.assertEquals(expected.getStorage(), result.getStorage());
        Assert.assertEquals(expected.getTags(), result.getTags());
        Assert.assertEquals(expectedSize, result.getSize());

        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name", "diskOffering", "provisioningType", "storage", "tags", "size"), result);

        Mockito.verify(presetVariableHelperSpy).getPresetVariableValueResourceTags(Mockito.anyLong(), Mockito.eq(ResourceObjectType.Volume));
    }

    @Test
    public void loadPresetVariableValueForVolumeTestRecordIsVolumeAndDoesNotHaveStorageSetFields() {
        Value expected = getValueForTests();

        VolumeVO volumeVoMock = Mockito.mock(VolumeVO.class);
        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        Mockito.doReturn(null).when(volumeVoMock).getPoolId();

        mockMethodValidateIfObjectIsNull();

        Mockito.doReturn(expected.getId()).when(volumeVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(volumeVoMock).getName();
        Mockito.doReturn(expected.getDiskOffering()).when(presetVariableHelperSpy).getPresetVariableValueDiskOffering(Mockito.anyLong());
        Mockito.doReturn(expected.getProvisioningType()).when(volumeVoMock).getProvisioningType();
        Mockito.doReturn(expected.getTags()).when(presetVariableHelperSpy).getPresetVariableValueResourceTags(Mockito.anyLong(), Mockito.any(ResourceObjectType.class));
        Mockito.doReturn(expected.getSize()).when(volumeVoMock).getSize();

        Mockito.doReturn(UsageTypes.VOLUME).when(usageVoMock).getUsageType();

        Value result = new Value();
        presetVariableHelperSpy.loadPresetVariableValueForVolume(usageVoMock, result);

        Long expectedSize = ByteScaleUtils.bytesToMebibytes(expected.getSize());

        assertPresetVariableIdAndName(expected, result);
        Assert.assertEquals(expected.getDiskOffering(), result.getDiskOffering());
        Assert.assertEquals(expected.getProvisioningType(), result.getProvisioningType());
        Assert.assertEquals(null, result.getStorage());
        Assert.assertEquals(expected.getTags(), result.getTags());
        Assert.assertEquals(expectedSize, result.getSize());

        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name", "diskOffering", "provisioningType", "tags", "size"), result);

        Mockito.verify(presetVariableHelperSpy).getPresetVariableValueResourceTags(Mockito.anyLong(), Mockito.eq(ResourceObjectType.Volume));
    }

    @Test
    public void getPresetVariableValueDiskOfferingTestSetValuesAndReturnObject() {
        DiskOfferingVO diskOfferingVoMock = Mockito.mock(DiskOfferingVO.class);
        Mockito.doReturn(diskOfferingVoMock).when(diskOfferingDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        mockMethodValidateIfObjectIsNull();

        GenericPresetVariable expected = getGenericPresetVariableForTests();
        Mockito.doReturn(expected.getId()).when(diskOfferingVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(diskOfferingVoMock).getName();

        GenericPresetVariable result = presetVariableHelperSpy.getPresetVariableValueDiskOffering(1l);

        assertPresetVariableIdAndName(expected, result);
        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name"), result);
    }

    @Test
    public void getPresetVariableValueStorageTestGetSecondaryStorageForSnapshot() {
        Storage expected = getStorageForTests();
        Mockito.doReturn(expected).when(presetVariableHelperSpy).getSecondaryStorageForSnapshot(Mockito.anyLong(), Mockito.anyInt());

        Storage result = presetVariableHelperSpy.getPresetVariableValueStorage(1l, 2);

        Assert.assertEquals(expected, result);
        Mockito.verify(primaryStorageDaoMock, Mockito.never()).findByIdIncludingRemoved(Mockito.anyLong());
    }

    @Test
    public void getPresetVariableValueStorageTestGetSecondaryStorageForSnapshotReturnsNull() {
        Storage expected = getStorageForTests();
        Mockito.doReturn(null).when(presetVariableHelperSpy).getSecondaryStorageForSnapshot(Mockito.anyLong(), Mockito.anyInt());

        StoragePoolVO storagePoolVoMock = Mockito.mock(StoragePoolVO.class);
        Mockito.doReturn(storagePoolVoMock).when(primaryStorageDaoMock).findByIdIncludingRemoved(Mockito.anyLong());

        Mockito.doReturn(expected.getId()).when(storagePoolVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(storagePoolVoMock).getName();
        Mockito.doReturn(expected.getScope()).when(storagePoolVoMock).getScope();
        Mockito.doReturn(expected.getTags()).when(storagePoolTagsDaoMock).getStoragePoolTags(Mockito.anyLong());

        Storage result = presetVariableHelperSpy.getPresetVariableValueStorage(1l, 2);

        assertPresetVariableIdAndName(expected, result);
        Assert.assertEquals(expected.getScope(), result.getScope());
        Assert.assertEquals(expected.getTags(), result.getTags());

        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name", "scope", "tags"), result);
    }

    @Test
    public void getSecondaryStorageForSnapshotTestAllTypesAndDoNotBackupSnapshotReturnNull() {
        presetVariableHelperSpy.backupSnapshotAfterTakingSnapshot = false;
        getQuotaTypesForTests().forEach(type -> {
            Storage result = presetVariableHelperSpy.getSecondaryStorageForSnapshot(1l, type.getKey());
            Assert.assertNull(result);
        });
    }

    @Test
    public void getSecondaryStorageForSnapshotTestAllTypesExceptSnapshotAndBackupSnapshotReturnNull() {
        presetVariableHelperSpy.backupSnapshotAfterTakingSnapshot = true;
        getQuotaTypesForTests(UsageTypes.SNAPSHOT).forEach(type -> {
            Storage result = presetVariableHelperSpy.getSecondaryStorageForSnapshot(1l, type.getKey());
            Assert.assertNull(result);
        });
    }

    @Test
    public void getSecondaryStorageForSnapshotTestRecordIsSnapshotAndBackupSnapshotSetFieldsAndReturnObject() {
        Storage expected = getStorageForTests();

        ImageStoreVO imageStoreVoMock = Mockito.mock(ImageStoreVO.class);
        Mockito.doReturn(imageStoreVoMock).when(imageStoreDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        mockMethodValidateIfObjectIsNull();

        Mockito.doReturn(expected.getId()).when(imageStoreVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(imageStoreVoMock).getName();
        presetVariableHelperSpy.backupSnapshotAfterTakingSnapshot = true;

        Storage result = presetVariableHelperSpy.getSecondaryStorageForSnapshot(1l, UsageTypes.SNAPSHOT);

        assertPresetVariableIdAndName(expected, result);
        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name"), result);
    }

    @Test
    public void loadPresetVariableValueForTemplateAndIsoTestRecordIsNotAtemplateNorAnIsoDoNothing() {
        getQuotaTypesForTests(templateAndIsoUsageTypes.toArray(new Integer[templateAndIsoUsageTypes.size()])).forEach(type -> {
            Mockito.doReturn(type.getKey()).when(usageVoMock).getUsageType();
            presetVariableHelperSpy.loadPresetVariableValueForTemplateAndIso(usageVoMock, null);
        });

        Mockito.verifyNoInteractions(vmTemplateDaoMock);
    }


    @Test
    public void loadPresetVariableValueForTemplateAndIsoTestRecordIsVolumeSetFields() {
        Value expected = getValueForTests();

        VMTemplateVO vmTemplateVoMock = Mockito.mock(VMTemplateVO.class);
        Mockito.doReturn(vmTemplateVoMock).when(vmTemplateDaoMock).findByIdIncludingRemoved(Mockito.anyLong());

        mockMethodValidateIfObjectIsNull();

        Mockito.doReturn(expected.getId()).when(vmTemplateVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(vmTemplateVoMock).getName();
        Mockito.doReturn(expected.getOsName()).when(presetVariableHelperSpy).getPresetVariableValueOsName(Mockito.anyLong());
        Mockito.doReturn(expected.getTags()).when(presetVariableHelperSpy).getPresetVariableValueResourceTags(Mockito.anyLong(), Mockito.any(ResourceObjectType.class));
        Mockito.doReturn(expected.getSize()).when(vmTemplateVoMock).getSize();

        templateAndIsoUsageTypes.forEach(type -> {
            Mockito.doReturn(type).when(usageVoMock).getUsageType();

            Value result = new Value();
            presetVariableHelperSpy.loadPresetVariableValueForTemplateAndIso(usageVoMock, result);

            Long expectedSize = ByteScaleUtils.bytesToMebibytes(expected.getSize());

            assertPresetVariableIdAndName(expected, result);
            Assert.assertEquals(expected.getOsName(), result.getOsName());
            Assert.assertEquals(expected.getTags(), result.getTags());
            Assert.assertEquals(expectedSize, result.getSize());

            validateFieldNamesToIncludeInToString(Arrays.asList("id", "name", "osName", "tags", "size"), result);
        });

        Mockito.verify(presetVariableHelperSpy).getPresetVariableValueResourceTags(Mockito.anyLong(), Mockito.eq(ResourceObjectType.Template));
        Mockito.verify(presetVariableHelperSpy).getPresetVariableValueResourceTags(Mockito.anyLong(), Mockito.eq(ResourceObjectType.ISO));
    }


    @Test
    public void loadPresetVariableValueForSnapshotTestRecordIsNotASnapshotDoNothing() {
        getQuotaTypesForTests(UsageTypes.SNAPSHOT).forEach(type -> {
            Mockito.doReturn(type.getKey()).when(usageVoMock).getUsageType();
            presetVariableHelperSpy.loadPresetVariableValueForSnapshot(usageVoMock, null);
        });

        Mockito.verifyNoInteractions(snapshotDaoMock);
    }


    @Test
    public void loadPresetVariableValueForSnapshotTestRecordIsSnapshotSetFields() {
        Value expected = getValueForTests();

        SnapshotVO snapshotVoMock = Mockito.mock(SnapshotVO.class);
        Mockito.doReturn(snapshotVoMock).when(snapshotDaoMock).findByIdIncludingRemoved(Mockito.anyLong());

        mockMethodValidateIfObjectIsNull();

        Mockito.doReturn(expected.getId()).when(snapshotVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(snapshotVoMock).getName();
        Mockito.doReturn(expected.getSize()).when(snapshotVoMock).getSize();
        Mockito.doReturn((short) 3).when(snapshotVoMock).getSnapshotType();
        Mockito.doReturn(1l).when(presetVariableHelperSpy).getSnapshotDataStoreId(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(expected.getStorage()).when(presetVariableHelperSpy).getPresetVariableValueStorage(Mockito.anyLong(), Mockito.anyInt());
        Mockito.doReturn(expected.getTags()).when(presetVariableHelperSpy).getPresetVariableValueResourceTags(Mockito.anyLong(), Mockito.any(ResourceObjectType.class));

        Mockito.doReturn(UsageTypes.SNAPSHOT).when(usageVoMock).getUsageType();

        Value result = new Value();
        presetVariableHelperSpy.loadPresetVariableValueForSnapshot(usageVoMock, result);

        Long expectedSize = ByteScaleUtils.bytesToMebibytes(expected.getSize());

        assertPresetVariableIdAndName(expected, result);
        Assert.assertEquals(expected.getSnapshotType(), result.getSnapshotType());
        Assert.assertEquals(expected.getStorage(), result.getStorage());
        Assert.assertEquals(expected.getTags(), result.getTags());
        Assert.assertEquals(expectedSize, result.getSize());

        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name", "snapshotType", "storage", "tags", "size"), result);

        Mockito.verify(presetVariableHelperSpy).getPresetVariableValueResourceTags(Mockito.anyLong(), Mockito.eq(ResourceObjectType.Snapshot));
    }


    @Test
    public void getSnapshotDataStoreIdTestDoNotBackupSnapshotToSecondaryRetrievePrimaryStorage() {
        SnapshotDataStoreVO snapshotDataStoreVoMock = Mockito.mock(SnapshotDataStoreVO.class);

        Long expected = 1l;
        Mockito.doReturn(snapshotDataStoreVoMock).when(snapshotDataStoreDaoMock).findOneBySnapshotAndDatastoreRole(Mockito.anyLong(), Mockito.any(DataStoreRole.class));
        Mockito.doReturn(expected).when(snapshotDataStoreVoMock).getDataStoreId();
        presetVariableHelperSpy.backupSnapshotAfterTakingSnapshot = false;

        Long result = presetVariableHelperSpy.getSnapshotDataStoreId(1l, 1l);

        Assert.assertEquals(expected, result);

        Arrays.asList(DataStoreRole.values()).forEach(role -> {
            if (role == DataStoreRole.Primary) {
                Mockito.verify(snapshotDataStoreDaoMock).findOneBySnapshotAndDatastoreRole(Mockito.anyLong(), Mockito.eq(role));
            } else {
                Mockito.verify(snapshotDataStoreDaoMock, Mockito.never()).findOneBySnapshotAndDatastoreRole(Mockito.anyLong(), Mockito.eq(role));
            }
        });
    }

    @Test
    public void getSnapshotDataStoreIdTestBackupSnapshotToSecondaryRetrieveSecondaryStorage() {
        SnapshotDataStoreVO snapshotDataStoreVoMock = Mockito.mock(SnapshotDataStoreVO.class);

        Long expected = 2l;
        ImageStoreVO imageStore = Mockito.mock(ImageStoreVO.class);
        Mockito.when(imageStoreDaoMock.findById(Mockito.anyLong())).thenReturn(imageStore);
        Mockito.when(imageStore.getDataCenterId()).thenReturn(1L);
        Mockito.when(snapshotDataStoreDaoMock.listReadyBySnapshot(Mockito.anyLong(), Mockito.any(DataStoreRole.class))).thenReturn(List.of(snapshotDataStoreVoMock));
        Mockito.doReturn(expected).when(snapshotDataStoreVoMock).getDataStoreId();
        presetVariableHelperSpy.backupSnapshotAfterTakingSnapshot = true;

        Long result = presetVariableHelperSpy.getSnapshotDataStoreId(2l, 1L);

        Assert.assertEquals(expected, result);

        Arrays.asList(DataStoreRole.values()).forEach(role -> {
            if (role == DataStoreRole.Image) {
                Mockito.verify(snapshotDataStoreDaoMock).listReadyBySnapshot(Mockito.anyLong(), Mockito.eq(role));
            } else {
                Mockito.verify(snapshotDataStoreDaoMock, Mockito.never()).listReadyBySnapshot(Mockito.anyLong(), Mockito.eq(role));
            }
        });
    }

    @Test
    public void loadPresetVariableValueForNetworkOfferingTestRecordIsNotASnapshotDoNothing() {
        getQuotaTypesForTests(UsageTypes.NETWORK_OFFERING).forEach(type -> {
            Mockito.doReturn(type.getKey()).when(usageVoMock).getUsageType();
            presetVariableHelperSpy.loadPresetVariableValueForNetworkOffering(usageVoMock, null);
        });

        Mockito.verifyNoInteractions(networkOfferingDaoMock);
    }

    @Test
    public void loadPresetVariableValueForNetworkOfferingTestRecordIsSnapshotSetFields() {
        Value expected = getValueForTests();

        NetworkOfferingVO networkOfferingVoMock = Mockito.mock(NetworkOfferingVO.class);
        Mockito.doReturn(networkOfferingVoMock).when(networkOfferingDaoMock).findByIdIncludingRemoved(Mockito.anyLong());

        mockMethodValidateIfObjectIsNull();

        Mockito.doReturn(expected.getId()).when(networkOfferingVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(networkOfferingVoMock).getName();
        Mockito.doReturn(expected.getTag()).when(networkOfferingVoMock).getTags();

        Mockito.doReturn(UsageTypes.NETWORK_OFFERING).when(usageVoMock).getUsageType();

        Value result = new Value();
        presetVariableHelperSpy.loadPresetVariableValueForNetworkOffering(usageVoMock, result);

        assertPresetVariableIdAndName(expected, result);
        Assert.assertEquals(expected.getTag(), result.getTag());

        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name", "tag"), result);
    }

    @Test
    public void loadPresetVariableValueForVmSnapshotTestRecordIsNotAVmSnapshotDoNothing() {
        getQuotaTypesForTests(UsageTypes.VM_SNAPSHOT).forEach(type -> {
            Mockito.doReturn(type.getKey()).when(usageVoMock).getUsageType();
            presetVariableHelperSpy.loadPresetVariableValueForVmSnapshot(usageVoMock, null);
        });

        Mockito.verifyNoInteractions(vmSnapshotDaoMock);
    }

    @Test
    public void loadPresetVariableValueForVmSnapshotTestRecordIsVmSnapshotSetFields() {
        Value expected = getValueForTests();

        VMSnapshotVO vmSnapshotVoMock = Mockito.mock(VMSnapshotVO.class);
        Mockito.doReturn(vmSnapshotVoMock).when(vmSnapshotDaoMock).findByIdIncludingRemoved(Mockito.anyLong());

        mockMethodValidateIfObjectIsNull();

        Mockito.doReturn(expected.getId()).when(vmSnapshotVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(vmSnapshotVoMock).getName();
        Mockito.doReturn(expected.getTags()).when(presetVariableHelperSpy).getPresetVariableValueResourceTags(Mockito.anyLong(), Mockito.any(ResourceObjectType.class));
        Mockito.doReturn(expected.getVmSnapshotType()).when(vmSnapshotVoMock).getType();

        Mockito.doReturn(UsageTypes.VM_SNAPSHOT).when(usageVoMock).getUsageType();

        Value result = new Value();
        presetVariableHelperSpy.loadPresetVariableValueForVmSnapshot(usageVoMock, result);

        assertPresetVariableIdAndName(expected, result);
        Assert.assertEquals(expected.getTags(), result.getTags());
        Assert.assertEquals(expected.getVmSnapshotType(), result.getVmSnapshotType());

        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name", "tags", "vmSnapshotType"), result);

        Mockito.verify(presetVariableHelperSpy).getPresetVariableValueResourceTags(Mockito.anyLong(), Mockito.eq(ResourceObjectType.VMSnapshot));
    }

    @Test (expected = CloudRuntimeException.class)
    public void validateIfObjectIsNullTestObjectIsNullThrowException() {
        presetVariableHelperSpy.validateIfObjectIsNull(null, null, null);
    }

    @Test
    public void validateIfObjectIsNullTestObjectIsNotNullDoNothing() {
        presetVariableHelperSpy.validateIfObjectIsNull(new Object(), null, null);
    }

    @Test
    public void setPresetVariableValueServiceOfferingAndComputingResourcesTestSetComputingResourcesOnlyToRunningVm() {
        Value expected = getValueForTests();

        Mockito.doReturn(serviceOfferingVoMock).when(serviceOfferingDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        mockMethodValidateIfObjectIsNull();

        Mockito.doReturn(expected.getComputeOffering()).when(presetVariableHelperSpy).getPresetVariableValueComputeOffering(Mockito.any());
        Mockito.doReturn(expected.getComputingResources()).when(presetVariableHelperSpy).getPresetVariableValueComputingResource(Mockito.any(), Mockito.any());

        QuotaTypes.listQuotaTypes().forEach((typeInt, value) -> {

            Value result = new Value();
            presetVariableHelperSpy.setPresetVariableValueServiceOfferingAndComputingResources(result, typeInt, vmInstanceVoMock);

            Assert.assertEquals(expected.getComputeOffering(), result.getComputeOffering());

            if (typeInt == UsageTypes.RUNNING_VM) {
                Assert.assertEquals(expected.getComputingResources(), result.getComputingResources());
                validateFieldNamesToIncludeInToString(Arrays.asList("computeOffering", "computingResources"), result);
            } else {
                validateFieldNamesToIncludeInToString(Arrays.asList("computeOffering"), result);
            }
        });
    }

    @Test
    public void getDetailByNameTestKeyDoesNotExistsReturnsDefaultValue() {
        int expected = 874;
        int result = presetVariableHelperSpy.getDetailByName(getVmDetailsForTests(), "detail_that_does_not_exist", expected);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getDetailByNameTestValueIsNullReturnsDefaultValue() {
        int expected = 749;
        int result = presetVariableHelperSpy.getDetailByName(getVmDetailsForTests(), "test_with_null", expected);
        Assert.assertEquals(expected, result);
    }

    @Test(expected = NumberFormatException.class)
    public void getDetailByNameTestValueIsInvalidThrowsNumberFormatException() {
        presetVariableHelperSpy.getDetailByName(getVmDetailsForTests(), "test_with_invalid_value", 0);
    }

    @Test
    public void getDetailByNameTestReturnsValue() {
        int expected = Integer.valueOf(getVmDetailsForTests().get(0).getValue());
        int result = presetVariableHelperSpy.getDetailByName(getVmDetailsForTests(), "test_with_value", expected);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void getPresetVariableValueComputingResourceTestServiceOfferingIsNotDynamicReturnsServiceOfferingValues() {
        ComputingResources expected = getComputingResourcesForTests();

        Mockito.doReturn(expected.getCpuNumber()).when(serviceOfferingVoMock).getCpu();
        Mockito.doReturn(expected.getCpuSpeed()).when(serviceOfferingVoMock).getSpeed();
        Mockito.doReturn(expected.getMemory()).when(serviceOfferingVoMock).getRamSize();
        Mockito.doReturn(false).when(serviceOfferingVoMock).isDynamic();

        ComputingResources result = presetVariableHelperSpy.getPresetVariableValueComputingResource(vmInstanceVoMock, serviceOfferingVoMock);

        Assert.assertEquals(expected.toString(), result.toString());
        Mockito.verify(userVmDetailsDaoMock, Mockito.never()).listDetails(Mockito.anyLong());
    }

    @Test
    public void getPresetVariableValueComputingResourceTestServiceOfferingIsDynamicReturnsVmDetails() {
        ComputingResources expected = getComputingResourcesForTests();

        Mockito.doReturn(1).when(serviceOfferingVoMock).getCpu();
        Mockito.doReturn(2).when(serviceOfferingVoMock).getSpeed();
        Mockito.doReturn(3).when(serviceOfferingVoMock).getRamSize();
        Mockito.doReturn(true).when(serviceOfferingVoMock).isDynamic();

        Mockito.doReturn(expected.getMemory(), expected.getCpuNumber(), expected.getCpuSpeed()).when(presetVariableHelperSpy).getDetailByName(Mockito.anyList(),
                Mockito.anyString(), Mockito.anyInt());

        ComputingResources result = presetVariableHelperSpy.getPresetVariableValueComputingResource(vmInstanceVoMock, serviceOfferingVoMock);

        Assert.assertEquals(expected.toString(), result.toString());
        Mockito.verify(userVmDetailsDaoMock).listDetails(Mockito.anyLong());
    }

    @Test
    public void loadPresetVariableValueForBackupTestRecordIsNotABackupDoNothing() {
        getQuotaTypesForTests(QuotaTypes.BACKUP).forEach(type -> {
            Mockito.doReturn(type.getKey()).when(usageVoMock).getUsageType();
            presetVariableHelperSpy.loadPresetVariableValueForBackup(usageVoMock, null);
        });

        Mockito.verifyNoInteractions(backupOfferingDaoMock);
    }

    @Test
    public void loadPresetVariableValueForBackupTestRecordIsBackupSetAllFields() {
        Value expected = getValueForTests();

        mockMethodValidateIfObjectIsNull();

        Mockito.doReturn(expected.getSize()).when(usageVoMock).getSize();
        Mockito.doReturn(expected.getVirtualSize()).when(usageVoMock).getVirtualSize();
        Mockito.doReturn(expected.getBackupOffering()).when(presetVariableHelperSpy).getPresetVariableValueBackupOffering(Mockito.anyLong());

        Mockito.doReturn(QuotaTypes.BACKUP).when(usageVoMock).getUsageType();

        Value result = new Value();
        presetVariableHelperSpy.loadPresetVariableValueForBackup(usageVoMock, result);

        Assert.assertEquals(expected.getSize(), result.getSize());
        Assert.assertEquals(expected.getVirtualSize(), result.getVirtualSize());
        Assert.assertEquals(expected.getBackupOffering(), result.getBackupOffering());

        validateFieldNamesToIncludeInToString(Arrays.asList("size", "virtualSize", "backupOffering"), result);

        Mockito.verify(presetVariableHelperSpy).getPresetVariableValueBackupOffering(Mockito.anyLong());
    }

    @Test
    public void getPresetVariableValueBackupOfferingTestSetValuesAndReturnObject() {
        BackupOfferingVO backupOfferingVoMock = Mockito.mock(BackupOfferingVO.class);
        Mockito.doReturn(backupOfferingVoMock).when(backupOfferingDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        mockMethodValidateIfObjectIsNull();

        BackupOffering expected = getBackupOfferingForTests();
        Mockito.doReturn(expected.getId()).when(backupOfferingVoMock).getUuid();
        Mockito.doReturn(expected.getName()).when(backupOfferingVoMock).getName();
        Mockito.doReturn(expected.getExternalId()).when(backupOfferingVoMock).getExternalId();

        BackupOffering result = presetVariableHelperSpy.getPresetVariableValueBackupOffering(1l);

        assertPresetVariableIdAndName(expected, result);
        Assert.assertEquals(expected.getExternalId(), result.getExternalId());
        validateFieldNamesToIncludeInToString(Arrays.asList("id", "name", "externalId"), result);
    }

    @Test
    public void testGetSnapshotImageStoreRefNull() {
        SnapshotDataStoreVO ref1 = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(ref1.getDataStoreId()).thenReturn(1L);
        Mockito.when(snapshotDataStoreDaoMock.listReadyBySnapshot(Mockito.anyLong(), Mockito.any(DataStoreRole.class))).thenReturn(List.of(ref1));
        ImageStoreVO store = Mockito.mock(ImageStoreVO.class);
        Mockito.when(store.getDataCenterId()).thenReturn(2L);
        Mockito.when(imageStoreDaoMock.findById(1L)).thenReturn(store);
        Assert.assertNull(presetVariableHelperSpy.getSnapshotImageStoreRef(1L, 1L));
    }

    @Test
    public void testGetSnapshotImageStoreRefNotNull() {
        SnapshotDataStoreVO ref1 = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(ref1.getDataStoreId()).thenReturn(1L);
        Mockito.when(snapshotDataStoreDaoMock.listReadyBySnapshot(Mockito.anyLong(), Mockito.any(DataStoreRole.class))).thenReturn(List.of(ref1));
        ImageStoreVO store = Mockito.mock(ImageStoreVO.class);
        Mockito.when(store.getDataCenterId()).thenReturn(1L);
        Mockito.when(imageStoreDaoMock.findById(1L)).thenReturn(store);
        Assert.assertNotNull(presetVariableHelperSpy.getSnapshotImageStoreRef(1L, 1L));
    }
}
