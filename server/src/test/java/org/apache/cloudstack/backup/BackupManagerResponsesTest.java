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

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.backup.dao.BackupDetailsDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.InternalBackupJoinDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BackupManagerResponsesTest {
    @Spy
    @InjectMocks
    private BackupManagerImpl backupManager = new BackupManagerImpl();

    @Mock
    private VMInstanceDao vmInstanceDao;
    @Mock
    private AccountDao accountDao;
    @Mock
    private DomainDao domainDao;
    @Mock
    private DataCenterDao dataCenterDao;
    @Mock
    private BackupOfferingDao backupOfferingDao;
    @Mock
    private VMTemplateDao vmTemplateDao;
    @Mock
    private ServiceOfferingDao serviceOfferingDao;
    @Mock
    private NetworkDao networkDao;
    @Mock
    private BackupDetailsDao backupDetailsDao;
    @Mock
    private InternalBackupJoinDao internalBackupJoinDaoMock;

    // Entities
    private BackupVO backup;
    private VMInstanceVO virtualMachine;
    private AccountVO account;
    private DomainVO domain;
    private DataCenterVO zone;
    private BackupOfferingVO backupOffering;
    private VMTemplateVO template;
    private ServiceOfferingVO serviceOffering;
    private NetworkVO network;

    @Before
    public void setup() {
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

        backup = new BackupVO();
        ReflectionTestUtils.setField(backup, "id", backupId);
        ReflectionTestUtils.setField(backup, "uuid", "backup-uuid");
        backup.setVmId(vmId);
        backup.setAccountId(accountId);
        backup.setDomainId(domainId);
        backup.setZoneId(zoneId);
        backup.setBackupOfferingId(backupOfferingId);
        backup.setType("Full");
        backup.setBackupScheduleId(null);

        virtualMachine = new VMInstanceVO(vmId, 0L, "test-vm", "test-vm", VirtualMachine.Type.User,
                0L, Hypervisor.HypervisorType.Simulator, 0L, domainId, accountId, 0L, false);
        virtualMachine.setDataCenterId(zoneId);
        virtualMachine.setBackupOfferingId(vmOfferingId);
        virtualMachine.setTemplateId(templateId);

        account = new AccountVO();
        account.setUuid("account-uuid");
        account.setAccountName("test-account");

        domain = new DomainVO();
        domain.setUuid("domain-uuid");
        domain.setName("test-domain");

        zone = new DataCenterVO(1L, "test-zone", null, null, null, null, null, null, null, null, DataCenter.NetworkType.Advanced, null, null);
        zone.setUuid("zone-uuid");

        backupOffering = new BackupOfferingVO();
        ReflectionTestUtils.setField(backupOffering, "id", backupOfferingId);
        ReflectionTestUtils.setField(backupOffering, "uuid", "offering-uuid");
        backupOffering.setName("test-offering");

        Mockito.when(vmInstanceDao.findByIdIncludingRemoved(vmId)).thenReturn(virtualMachine);
        Mockito.when(accountDao.findByIdIncludingRemoved(accountId)).thenReturn(account);
        Mockito.when(domainDao.findByIdIncludingRemoved(domainId)).thenReturn(domain);
        Mockito.when(dataCenterDao.findByIdIncludingRemoved(zoneId)).thenReturn(zone);
        Mockito.when(backupOfferingDao.findByIdIncludingRemoved(backupOfferingId)).thenReturn(backupOffering);

        template = new VMTemplateVO();
        template.setFormat(Storage.ImageFormat.QCOW2);
        template.setName("template1");
        when(vmTemplateDao.findByUuid(templateUuid)).thenReturn(template);


        serviceOffering = mock(ServiceOfferingVO.class);
        when(serviceOffering.getName()).thenReturn("service-offering1");
        when(serviceOfferingDao.findByUuid(serviceOfferingUuid)).thenReturn(serviceOffering);

        network = new NetworkVO();
        network.setName("network1");
        when(networkDao.findByUuid("network-uuid1")).thenReturn(network);


        Map<String, String> details = new HashMap<>();
        details.put(ApiConstants.TEMPLATE_ID, templateUuid);
        details.put(ApiConstants.SERVICE_OFFERING_ID, serviceOfferingUuid);
        details.put(ApiConstants.NICS, "[{\"networkid\":\"network-uuid1\"}]");
        Mockito.when(backupDetailsDao.listDetailsKeyPairs(backup.getId(), true)).thenReturn(details);
    }

    @Test
    public void createBackupResponseTestBasicResponse() {
        BackupResponse response = backupManager.createBackupResponse(backup, true, true);

        assertFullBackupResponseFields(response);
        Assert.assertNull(response.getBackupPaths());
    }

    @Test
    public void createBackupResponseTestKBOSSResponseAdmin() {
        ReflectionTestUtils.setField(backupOffering, "provider", BackupManagerImpl.KBOSS_BACKUP_PROVIDER);

        InternalBackupJoinVO backupJoin = createBackupJoin();
        Mockito.when(internalBackupJoinDaoMock.listById(Mockito.anyLong())).thenReturn(Collections.singletonList(backupJoin));

        BackupResponse response = backupManager.createBackupResponse(backup, true, true);

        assertFullBackupResponseFields(response);
        Assert.assertFalse(response.getBackupPaths().isEmpty());
        Assert.assertTrue(response.getBackupPaths().containsKey(backupJoin.getVolumeUuid()));
        Assert.assertEquals(backupJoin.getImageStorePath(), response.getBackupPaths().get(backupJoin.getVolumeUuid()).getBackupPath());
        Assert.assertEquals(backupJoin.getVolumeName(), response.getBackupPaths().get(backupJoin.getVolumeUuid()).getVolumeName());
    }

    @Test
    public void createBackupResponseTestKBOSSResponseMultipleEqualNamedVolumes() {
        ReflectionTestUtils.setField(backupOffering, "provider", BackupManagerImpl.KBOSS_BACKUP_PROVIDER);

        int joinsAmount = 3;
        List<InternalBackupJoinVO> backupJoins = getListOfBackupJoins(joinsAmount);
        Mockito.when(internalBackupJoinDaoMock.listById(Mockito.anyLong())).thenReturn(backupJoins);

        BackupResponse response = backupManager.createBackupResponse(backup, true, true);

        assertFullBackupResponseFields(response);
        Assert.assertFalse(response.getBackupPaths().isEmpty());
        Assert.assertEquals(joinsAmount, response.getBackupPaths().size());

        for (InternalBackupJoinVO backupJoin : backupJoins) {
            Assert.assertTrue(response.getBackupPaths().containsKey(backupJoin.getVolumeUuid()));
            Assert.assertEquals(backupJoin.getImageStorePath(), response.getBackupPaths().get(backupJoin.getVolumeUuid()).getBackupPath());
            Assert.assertEquals(backupJoin.getVolumeName(), response.getBackupPaths().get(backupJoin.getVolumeUuid()).getVolumeName());
        }
    }

    @Test
    public void createBackupResponseTestKBOSSResponseNormalUser() {
        ReflectionTestUtils.setField(backupOffering, "provider", BackupManagerImpl.KBOSS_BACKUP_PROVIDER);

        BackupResponse response = backupManager.createBackupResponse(backup, true, false);

        assertFullBackupResponseFields(response);
        Assert.assertNull(response.getBackupPaths());
    }

    @Test
    public void createBackupResponseTestKBOSSResponseNullOffering() {
        Mockito.when(backupOfferingDao.findByIdIncludingRemoved(Mockito.anyLong())).thenReturn(null);

        BackupResponse response = backupManager.createBackupResponse(backup, true, true);

        assertBasicBackupResponseFields(response);
        Assert.assertNull(response.getBackupPaths());
    }

    private void assertFullBackupResponseFields(BackupResponse response) {
        assertBasicBackupResponseFields(response);
        assertBackupResponseOfferingFields(response);
    }

    private void assertBasicBackupResponseFields(BackupResponse response) {
        Assert.assertEquals("backup-uuid", response.getId());
        Assert.assertEquals("test-vm", response.getVmName());
        Assert.assertEquals("account-uuid", response.getAccountId());
        Assert.assertEquals("test-account", response.getAccount());
        Assert.assertEquals("domain-uuid", response.getDomainId());
        Assert.assertEquals("test-domain", response.getDomain());
        Assert.assertEquals("zone-uuid", response.getZoneId());
        Assert.assertEquals("test-zone", response.getZone());
        Assert.assertEquals("MANUAL", response.getIntervalType());
        Assert.assertEquals("{serviceofferingid=service-offering-uuid1, isiso=false, hypervisor=Simulator, " +
                "nics=[{\"networkid\":\"network-uuid1\",\"networkname\":\"network1\"}], serviceofferingname=service-offering1, " +
                "templatename=template1, templateid=template-uuid1}", response.getVmDetails().toString());
        Assert.assertEquals(true, response.getVmOfferingRemoved());
    }

    private void assertBackupResponseOfferingFields(BackupResponse response) {
        Assert.assertEquals("offering-uuid", response.getBackupOfferingId());
        Assert.assertEquals("test-offering", response.getBackupOffering());
    }

    private List<InternalBackupJoinVO> getListOfBackupJoins(int listSize) {
        List<InternalBackupJoinVO> backupJoins = new java.util.ArrayList<>();

        for (int i = 0; i < listSize; i++) {
            backupJoins.add(createBackupJoin());
        }

        return backupJoins;
    }

    private InternalBackupJoinVO createBackupJoin() {
        String backupPath = "/backup/path";
        String volumeName = "ROOT";
        String volumeUuid = UUID.randomUUID().toString();
        InternalBackupJoinVO backupJoin = new InternalBackupJoinVO();
        ReflectionTestUtils.setField(backupJoin, "imageStorePath", backupPath);
        ReflectionTestUtils.setField(backupJoin, "volumeName", volumeName);
        ReflectionTestUtils.setField(backupJoin, "volumeUuid", volumeUuid);

        return backupJoin;
    }
}
