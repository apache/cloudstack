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
package org.apache.cloudstack.backup.dao;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupOfferingVO;
import org.apache.cloudstack.backup.BackupVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class BackupDaoImplTest {
    @Spy
    @InjectMocks
    private BackupDaoImpl backupDao;

    @Mock
    BackupDetailsDao backupDetailsDao;

    @Mock
    SearchBuilder<BackupVO> backupSearch;

    @Mock
    VMInstanceDao vmInstanceDao;

    @Mock
    AccountDao accountDao;

    @Mock
    DomainDao domainDao;

    @Mock
    DataCenterDao dataCenterDao;

    @Mock
    BackupOfferingDao backupOfferingDao;

    @Mock
    private VMTemplateDao templateDao;

    @Test
    public void testLoadDetails() {
        Long backupId = 1L;
        BackupVO backup = new BackupVO();
        ReflectionTestUtils.setField(backup, "id", backupId);
        Map<String, String> details = new HashMap<>();
        details.put("key1", "value1");
        details.put("key2", "value2");

        Mockito.when(backupDetailsDao.listDetailsKeyPairs(backupId)).thenReturn(details);

        backupDao.loadDetails(backup);

        Assert.assertEquals(details, backup.getDetails());
        Mockito.verify(backupDetailsDao).listDetailsKeyPairs(backupId);
    }

    @Test
    public void testSaveDetails() {
        Long backupId = 1L;
        BackupVO backup = new BackupVO();
        ReflectionTestUtils.setField(backup, "id", backupId);
        Map<String, String> details = new HashMap<>();
        details.put("key1", "value1");
        details.put("key2", "value2");
        backup.setDetails(details);

        backupDao.saveDetails(backup);

        Mockito.verify(backupDetailsDao).saveDetails(Mockito.anyList());
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

        BackupVO backup = new BackupVO();
        ReflectionTestUtils.setField(backup, "id", backupId);
        ReflectionTestUtils.setField(backup, "uuid", "backup-uuid");
        backup.setVmId(vmId);
        backup.setAccountId(accountId);
        backup.setDomainId(domainId);
        backup.setZoneId(zoneId);
        backup.setBackupOfferingId(backupOfferingId);
        backup.setType("Full");
        backup.setBackupIntervalType((short) Backup.Type.MANUAL.ordinal());

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
        when(templateDao.findById(templateId)).thenReturn(template);
        Map<String, String> details = new HashMap<>();

        Mockito.when(backupDetailsDao.listDetailsKeyPairs(backup.getId(), true)).thenReturn(details);

        BackupResponse response = backupDao.newBackupResponse(backup, true);

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
        Assert.assertEquals("{isiso=false, hypervisor=Simulator, templatename=template1, templateid=template-uuid1}", response.getVmDetails().toString());
        Assert.assertEquals(true, response.getVmOfferingRemoved());
    }
}
