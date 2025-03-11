package org.apache.cloudstack.backup.dao;

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
        Long offeringId = 5L;
        Long backupId = 6L;
        BackupVO backup = new BackupVO();
        ReflectionTestUtils.setField(backup, "id", backupId);
        ReflectionTestUtils.setField(backup, "uuid", "backup-uuid");
        backup.setVmId(vmId);
        backup.setAccountId(accountId);
        backup.setDomainId(domainId);
        backup.setZoneId(zoneId);
        backup.setBackupOfferingId(offeringId);
        backup.setType("Full");
        backup.setBackupIntervalType((short) Backup.Type.MANUAL.ordinal());

        VMInstanceVO vm = new VMInstanceVO(vmId, 0L, "test-vm", "test-vm", VirtualMachine.Type.User,
                0L, Hypervisor.HypervisorType.Simulator, 0L, domainId, accountId, 0L, false);
        vm.setDataCenterId(zoneId);

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
        Mockito.when(backupOfferingDao.findByIdIncludingRemoved(offeringId)).thenReturn(offering);

        BackupResponse response = backupDao.newBackupResponse(backup, false);

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
    }
}