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

package org.apache.cloudstack.dns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.api.command.user.dns.CreateDnsZoneCmd;
import org.apache.cloudstack.api.command.user.dns.DeleteDnsServerCmd;

import org.apache.cloudstack.api.command.user.dns.DisassociateDnsZoneFromNetworkCmd;
import org.apache.cloudstack.api.command.user.dns.ListDnsRecordsCmd;
import org.apache.cloudstack.api.command.user.dns.UpdateDnsZoneCmd;
import org.apache.cloudstack.api.response.DnsRecordResponse;
import org.apache.cloudstack.api.response.DnsServerResponse;
import org.apache.cloudstack.api.response.DnsZoneNetworkMapResponse;
import org.apache.cloudstack.api.response.DnsZoneResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.dns.dao.DnsServerDao;
import org.apache.cloudstack.dns.dao.DnsServerJoinDao;
import org.apache.cloudstack.dns.dao.DnsZoneDao;
import org.apache.cloudstack.dns.dao.DnsZoneJoinDao;
import org.apache.cloudstack.dns.dao.DnsZoneNetworkMapDao;
import org.apache.cloudstack.dns.exception.DnsConflictException;
import org.apache.cloudstack.dns.exception.DnsNotFoundException;
import org.apache.cloudstack.dns.exception.DnsProviderException;
import org.apache.cloudstack.dns.exception.DnsTransportException;
import org.apache.cloudstack.dns.vo.DnsServerJoinVO;
import org.apache.cloudstack.dns.vo.DnsServerVO;
import org.apache.cloudstack.dns.vo.DnsZoneJoinVO;
import org.apache.cloudstack.dns.vo.DnsZoneNetworkMapVO;
import org.apache.cloudstack.dns.vo.DnsZoneVO;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicDetailVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class DnsProviderManagerImplTest {

    private static final long ACCOUNT_ID = 1L;
    private static final long DOMAIN_ID = 10L;
    private static final long SERVER_ID = 100L;
    private static final long ZONE_ID = 200L;
    private static final long NETWORK_ID = 300L;

    @InjectMocks
    DnsProviderManagerImpl manager;

    @Mock AccountManager accountMgr;
    @Mock DnsServerDao dnsServerDao;
    @Mock DnsZoneDao dnsZoneDao;
    @Mock DnsZoneJoinDao dnsZoneJoinDao;
    @Mock DnsServerJoinDao dnsServerJoinDao;
    @Mock DnsZoneNetworkMapDao dnsZoneNetworkMapDao;
    @Mock NetworkDao networkDao;
    @Mock DomainDao domainDao;
    @Mock NicDao nicDao;
    @Mock NicDetailsDao nicDetailsDao;
    @Mock MessageBus messageBus;
    @Mock VMInstanceDao vmInstanceDao;
    @Mock DnsProvider dnsProviderMock;
    @Mock Account callerMock;

    private MockedStatic<CallContext> callContextMocked;
    private CallContext callContextMock;

    // Support VOs
    private DnsServerVO serverVO;
    private DnsZoneVO zoneVO;

    @Before
    public void setUp() throws Exception {
        callContextMocked = Mockito.mockStatic(CallContext.class);
        callContextMock = mock(CallContext.class);
        callContextMocked.when(CallContext::current).thenReturn(callContextMock);
        when(callContextMock.getCallingAccount()).thenReturn(callerMock);
        when(callerMock.getId()).thenReturn(ACCOUNT_ID);
        when(callerMock.getDomainId()).thenReturn(DOMAIN_ID);

        serverVO = Mockito.spy(new DnsServerVO("test-server", "http://pdns:8081", 8081, "localhost",
                DnsProviderType.PowerDNS, null, "apikey", false, null,
                Collections.singletonList("ns1.example.com"), ACCOUNT_ID, DOMAIN_ID));
        Mockito.lenient().doReturn(SERVER_ID).when(serverVO).getId();

        zoneVO = Mockito.spy(new DnsZoneVO("example.com", DnsZone.ZoneType.Public, SERVER_ID, ACCOUNT_ID, DOMAIN_ID, "Test zone"));
        Mockito.lenient().doReturn(ZONE_ID).when(zoneVO).getId();

        when(dnsProviderMock.getProviderType()).thenReturn(DnsProviderType.PowerDNS);
        manager.setDnsProviders(Collections.singletonList(dnsProviderMock));

        doNothing().when(accountMgr).checkAccess(any(Account.class), nullable(org.apache.cloudstack.acl.SecurityChecker.AccessType.class), eq(true), any());
    }

    @After
    public void tearDown() {
        callContextMocked.close();
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetProviderByTypeNull() {
        // Setting providers to empty to force lookup failure
        manager.setDnsProviders(Collections.emptyList());
        // Trigger via provisionDnsZone which calls getProviderByType
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        manager.provisionDnsZone(ZONE_ID);
    }

    @Test
    public void testListProviderNamesReturnsList() {
        List<String> names = manager.listProviderNames();
        assertEquals(1, names.size());
        assertEquals("PowerDNS", names.get(0));
    }

    @Test
    public void testListProviderNamesWithNullProviders() {
        manager.setDnsProviders(null);
        List<String> names = manager.listProviderNames();
        assertTrue(names.isEmpty());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAllocateDnsZoneBlankName() {
        CreateDnsZoneCmd cmd = mock(CreateDnsZoneCmd.class);
        when(cmd.getName()).thenReturn("  ");
        manager.allocateDnsZone(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAllocateDnsZoneServerNotFound() {
        CreateDnsZoneCmd cmd = mock(CreateDnsZoneCmd.class);
        when(cmd.getName()).thenReturn("example.com");
        when(cmd.getDnsServerId()).thenReturn(SERVER_ID);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(null);
        manager.allocateDnsZone(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAllocateDnsZoneAlreadyExists() {
        CreateDnsZoneCmd cmd = mock(CreateDnsZoneCmd.class);
        when(cmd.getName()).thenReturn("example.com");
        when(cmd.getDnsServerId()).thenReturn(SERVER_ID);
        when(cmd.getType()).thenReturn(DnsZone.ZoneType.Public);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        Mockito.doReturn(SERVER_ID).when(serverVO).getId();
        Mockito.doReturn(ACCOUNT_ID).when(serverVO).getAccountId();
        when(dnsZoneDao.findByNameServerAndType(anyString(), anyLong(), any())).thenReturn(zoneVO);
        manager.allocateDnsZone(cmd);
    }

    @Test
    public void testAllocateDnsZoneOwnerSuccess() {
        CreateDnsZoneCmd cmd = mock(CreateDnsZoneCmd.class);
        when(cmd.getName()).thenReturn("example.com");
        when(cmd.getDnsServerId()).thenReturn(SERVER_ID);
        when(cmd.getType()).thenReturn(DnsZone.ZoneType.Public);
        when(cmd.getDescription()).thenReturn("desc");
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        Mockito.doReturn(SERVER_ID).when(serverVO).getId();
        Mockito.doReturn(ACCOUNT_ID).when(serverVO).getAccountId();
        when(dnsZoneDao.findByNameServerAndType(anyString(), anyLong(), any())).thenReturn(null);
        when(dnsZoneDao.persist(any(DnsZoneVO.class))).thenReturn(zoneVO);
        DnsZone result = manager.allocateDnsZone(cmd);
        assertNotNull(result);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testAllocateDnsZoneNonOwnerPrivateServer() {
        CreateDnsZoneCmd cmd = mock(CreateDnsZoneCmd.class);
        when(cmd.getName()).thenReturn("tenant.com");
        when(cmd.getDnsServerId()).thenReturn(SERVER_ID);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        Mockito.doReturn(ACCOUNT_ID + 99).when(serverVO).getAccountId(); // different owner

        manager.allocateDnsZone(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testProvisionDnsZoneNotFound() {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(null);
        manager.provisionDnsZone(ZONE_ID);
    }

    @Test
    public void testProvisionDnsZoneSuccess() throws Exception {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        when(dnsProviderMock.provisionZone(any(), any())).thenReturn("example.com.");
        when(dnsZoneDao.update(anyLong(), any())).thenReturn(true);
        DnsZone result = manager.provisionDnsZone(ZONE_ID);
        assertNotNull(result);
        verify(dnsProviderMock).provisionZone(serverVO, zoneVO);
        verify(dnsZoneDao).update(anyLong(), any());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testProvisionDnsZoneConflictException() throws Exception {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        when(dnsProviderMock.provisionZone(any(), any())).thenThrow(new DnsConflictException("conflict"));
        manager.provisionDnsZone(ZONE_ID);
        verify(dnsZoneDao).remove(ZONE_ID);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testProvisionDnsZoneTransportException() throws Exception {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        when(dnsProviderMock.provisionZone(any(), any())).thenThrow(new DnsTransportException("unreachable", new IOException("i/o")));
        manager.provisionDnsZone(ZONE_ID);
        verify(dnsZoneDao).remove(ZONE_ID);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteDnsZoneNotFound() {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(null);
        manager.deleteDnsZone(ZONE_ID);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDeleteDnsZoneServerMissing() {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(null);
        manager.deleteDnsZone(ZONE_ID);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateDnsZoneNotFound() {
        UpdateDnsZoneCmd cmd = mock(UpdateDnsZoneCmd.class);
        when(cmd.getId()).thenReturn(ZONE_ID);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(null);
        manager.updateDnsZone(cmd);
    }

    @Test
    public void testUpdateDnsZoneNoChange() {
        UpdateDnsZoneCmd cmd = mock(UpdateDnsZoneCmd.class);
        when(cmd.getId()).thenReturn(ZONE_ID);
        when(cmd.getDescription()).thenReturn(null);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        DnsZone result = manager.updateDnsZone(cmd);
        assertNotNull(result);
        verify(dnsZoneDao, never()).update(anyLong(), any());
    }

    @Test
    public void testUpdateDnsZoneWithDescription() throws Exception {
        UpdateDnsZoneCmd cmd = mock(UpdateDnsZoneCmd.class);
        when(cmd.getId()).thenReturn(ZONE_ID);
        when(cmd.getDescription()).thenReturn("Updated description");
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        doNothing().when(dnsProviderMock).updateZone(any(), any());
        when(dnsZoneDao.update(anyLong(), any())).thenReturn(true);
        DnsZone result = manager.updateDnsZone(cmd);
        assertNotNull(result);
        verify(dnsProviderMock).updateZone(serverVO, zoneVO);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testUpdateDnsZoneServerMissing() {
        UpdateDnsZoneCmd cmd = mock(UpdateDnsZoneCmd.class);
        when(cmd.getId()).thenReturn(ZONE_ID);
        when(cmd.getDescription()).thenReturn("New description");
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(null);
        manager.updateDnsZone(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteDnsServerNotFound() {
        DeleteDnsServerCmd cmd = mock(DeleteDnsServerCmd.class);
        when(cmd.getId()).thenReturn(SERVER_ID);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(null);
        manager.deleteDnsServer(cmd);
    }

    @Test
    public void testDeleteDnsServerWithCleanup() throws Exception {
        DeleteDnsServerCmd cmd = mock(DeleteDnsServerCmd.class);
        when(cmd.getId()).thenReturn(SERVER_ID);
        when(cmd.getCleanup()).thenReturn(true);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        doNothing().when(accountMgr).checkAccess(any(Account.class), nullable(org.apache.cloudstack.acl.SecurityChecker.AccessType.class), eq(true), any());

        List<DnsZoneVO> zones = Collections.singletonList(zoneVO);
        when(dnsZoneDao.findDnsZonesByServerId(SERVER_ID)).thenReturn(zones);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsZoneNetworkMapDao.findByZoneId(ZONE_ID)).thenReturn(null);
        when(dnsServerDao.remove(SERVER_ID)).thenReturn(true);
        when(dnsZoneDao.remove(ZONE_ID)).thenReturn(true);

        try (MockedStatic<Transaction> transactionMock = Mockito.mockStatic(Transaction.class)) {
            transactionMock.when(() -> Transaction.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
                TransactionCallback<Boolean> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });

            boolean res = manager.deleteDnsServer(cmd);
            assertTrue(res);
            verify(dnsServerDao).remove(SERVER_ID);
            verify(dnsProviderMock).deleteZone(any(), any());
        }
    }

    @Test
    public void testDeleteDnsZoneSuccess() throws Exception {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(anyLong())).thenReturn(serverVO);
        doNothing().when(accountMgr).checkAccess(any(Account.class), nullable(org.apache.cloudstack.acl.SecurityChecker.AccessType.class), eq(true), any());
        when(dnsZoneNetworkMapDao.findByZoneId(ZONE_ID)).thenReturn(null);
        when(dnsZoneDao.remove(ZONE_ID)).thenReturn(true);

        try (MockedStatic<Transaction> transactionMock = Mockito.mockStatic(Transaction.class)) {
            transactionMock.when(() -> Transaction.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
                TransactionCallback<Boolean> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });

            boolean res = manager.deleteDnsZone(ZONE_ID);
            assertTrue(res);
            verify(dnsZoneDao).remove(ZONE_ID);
            verify(dnsProviderMock).deleteZone(any(), any());
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListDnsRecordsZoneNotFound() {
        ListDnsRecordsCmd cmd = mock(ListDnsRecordsCmd.class);
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(null);
        manager.listDnsRecords(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testListDnsRecordsServerMissing() {
        ListDnsRecordsCmd cmd = mock(ListDnsRecordsCmd.class);
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(null);
        manager.listDnsRecords(cmd);
    }

    @Test
    public void testListDnsRecordsSuccess() throws Exception {
        ListDnsRecordsCmd cmd = mock(ListDnsRecordsCmd.class);
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        DnsRecord record = new DnsRecord("www.example.com", DnsRecord.RecordType.A, Collections.singletonList("1.2.3.4"), 300);
        when(dnsProviderMock.listRecords(any(), any())).thenReturn(Collections.singletonList(record));
        ListResponse<DnsRecordResponse> result = manager.listDnsRecords(cmd);
        assertNotNull(result);
        assertEquals(1, result.getCount().intValue());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testListDnsRecordsZoneNotFoundInProvider() throws Exception {
        ListDnsRecordsCmd cmd = mock(ListDnsRecordsCmd.class);
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        when(dnsProviderMock.listRecords(any(), any())).thenThrow(new DnsNotFoundException("not found"));
        manager.listDnsRecords(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDisassociateZoneNoMappingFound() {
        DisassociateDnsZoneFromNetworkCmd cmd = mock(DisassociateDnsZoneFromNetworkCmd.class);
        when(cmd.getNetworkId()).thenReturn(NETWORK_ID);
        when(dnsZoneNetworkMapDao.findByNetworkId(NETWORK_ID)).thenReturn(null);
        manager.disassociateZoneFromNetwork(cmd);
    }

    @Test
    public void testDisassociateZoneOrphanedMapping() {
        DisassociateDnsZoneFromNetworkCmd cmd = mock(DisassociateDnsZoneFromNetworkCmd.class);
        when(cmd.getNetworkId()).thenReturn(NETWORK_ID);
        DnsZoneNetworkMapVO mapping = mock(DnsZoneNetworkMapVO.class);
        when(mapping.getDnsZoneId()).thenReturn(ZONE_ID);
        when(mapping.getId()).thenReturn(500L);
        when(dnsZoneNetworkMapDao.findByNetworkId(NETWORK_ID)).thenReturn(mapping);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(null); // zone missing (orphan)
        when(dnsZoneNetworkMapDao.remove(500L)).thenReturn(true);
        boolean result = manager.disassociateZoneFromNetwork(cmd);
        assertTrue(result);
    }

    @Test
    public void testDisassociateZoneSuccess() {
        DisassociateDnsZoneFromNetworkCmd cmd = mock(DisassociateDnsZoneFromNetworkCmd.class);
        when(cmd.getNetworkId()).thenReturn(NETWORK_ID);
        DnsZoneNetworkMapVO mapping = mock(DnsZoneNetworkMapVO.class);
        when(mapping.getDnsZoneId()).thenReturn(ZONE_ID);
        when(mapping.getId()).thenReturn(500L);
        when(dnsZoneNetworkMapDao.findByNetworkId(NETWORK_ID)).thenReturn(mapping);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);

        when(dnsZoneNetworkMapDao.remove(500L)).thenReturn(true);
        boolean result = manager.disassociateZoneFromNetwork(cmd);
        assertTrue(result);
        verify(dnsZoneNetworkMapDao).remove(500L);
    }

    @Test
    public void testCreateDnsRecordResponse() {
        DnsRecord record = new DnsRecord("www.example.com", DnsRecord.RecordType.A, Arrays.asList("1.2.3.4"), 300);
        DnsRecordResponse response = manager.createDnsRecordResponse(record);
        assertNotNull(response);
    }

    @Test
    public void testCreateDnsServerResponseFromJoinVO() {
        DnsServerJoinVO join = mock(DnsServerJoinVO.class);
        when(join.getUuid()).thenReturn("uuid-1");
        when(join.getName()).thenReturn("pdns");
        when(join.getUrl()).thenReturn("http://pdns:8081");
        when(join.getPort()).thenReturn(8081);
        when(join.getProviderType()).thenReturn(DnsProviderType.PowerDNS.toString());
        when(join.isPublicServer()).thenReturn(false);
        when(join.getNameServers()).thenReturn(Collections.emptyList());
        when(join.getPublicDomainSuffix()).thenReturn(null);
        when(join.getAccountName()).thenReturn("admin");
        when(join.getDomainUuid()).thenReturn("domain-uuid");
        when(join.getDomainName()).thenReturn("ROOT");
        when(join.getState()).thenReturn(DnsServer.State.Enabled);
        DnsServerResponse response = manager.createDnsServerResponse(join);
        assertNotNull(response);
    }

    @Test
    public void testCreateDnsZoneResponseFromJoinVO() {
        DnsZoneJoinVO join = mock(DnsZoneJoinVO.class);
        when(join.getUuid()).thenReturn("zone-uuid");
        when(join.getName()).thenReturn("example.com");
        when(join.getDnsServerUuid()).thenReturn("server-uuid");
        when(join.getAccountName()).thenReturn("admin");
        when(join.getDomainUuid()).thenReturn("domain-uuid");
        when(join.getDomainName()).thenReturn("ROOT");
        when(join.getDnsServerName()).thenReturn("pdns");
        when(join.getDnsServerAccountName()).thenReturn("admin");
        when(join.getState()).thenReturn(DnsZone.State.Active);
        when(join.getDescription()).thenReturn("Test zone");
        DnsZoneResponse response = manager.createDnsZoneResponse(join);
        assertNotNull(response);
    }

    @Test
    public void testCheckDnsServerPermissionOwner() {
        // owner has same accountId as server
        when(callerMock.getId()).thenReturn(ACCOUNT_ID);
        Mockito.doReturn(ACCOUNT_ID).when(serverVO).getAccountId();
        // Should not throw
        manager.checkDnsServerPermission(callerMock, serverVO);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckDnsServerPermissionNonOwnerPrivate() {
        when(callerMock.getId()).thenReturn(ACCOUNT_ID + 1);
        Mockito.doReturn(ACCOUNT_ID).when(serverVO).getAccountId();
        Mockito.doReturn(false).when(serverVO).getPublicServer();
        manager.checkDnsServerPermission(callerMock, serverVO);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckDnsServerPermissionNonOwnerPublicOutsideDomain() {
        AccountVO serverOwner = mock(AccountVO.class);
        when(callerMock.getId()).thenReturn(ACCOUNT_ID + 1);
        Mockito.doReturn(ACCOUNT_ID).when(serverVO).getAccountId();
        Mockito.doReturn(true).when(serverVO).getPublicServer();
        when(serverOwner.getDomainId()).thenReturn(20L);
        when(callerMock.getDomainId()).thenReturn(DOMAIN_ID);
        ReflectionTestUtils.setField(manager, "accountDao",
                Mockito.mock(com.cloud.user.dao.AccountDao.class));
        com.cloud.user.dao.AccountDao accountDaoMock = (com.cloud.user.dao.AccountDao) ReflectionTestUtils.getField(manager, "accountDao");
        when(accountDaoMock.findByIdIncludingRemoved(ACCOUNT_ID)).thenReturn(serverOwner);
        when(domainDao.isChildDomain(20L, DOMAIN_ID)).thenReturn(false);
        manager.checkDnsServerPermission(callerMock, serverVO);
    }

    @Test
    public void testCheckDnsZonePermissionOwner() {
        when(callerMock.getId()).thenReturn(ACCOUNT_ID);
        Mockito.doReturn(ACCOUNT_ID).when(zoneVO).getAccountId();
        // Should not throw
        manager.checkDnsZonePermission(callerMock, zoneVO);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckDnsZonePermissionNonOwner() {
        when(callerMock.getId()).thenReturn(ACCOUNT_ID + 1);
        Mockito.doReturn(ACCOUNT_ID).when(zoneVO).getAccountId();
        manager.checkDnsZonePermission(callerMock, zoneVO);
    }

    @Test
    public void testAddDnsRecordForVMNoNetworkMapping() throws DnsProviderException {
        Network network = mock(Network.class);
        NicVO nic = mock(NicVO.class);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(dnsZoneNetworkMapDao.findByNetworkId(anyLong())).thenReturn(null);
        when(network.getId()).thenReturn(NETWORK_ID);
        manager.addDnsRecordForVM(vm, network, nic);
        verify(dnsProviderMock, never()).addRecord(any(), any(), any());
    }

    @Test
    public void testAddDnsRecordForVMInactiveZone() {
        Network network = mock(Network.class);
        NicVO nic = mock(NicVO.class);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        DnsZoneNetworkMapVO mapping = mock(DnsZoneNetworkMapVO.class);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(dnsZoneNetworkMapDao.findByNetworkId(NETWORK_ID)).thenReturn(mapping);
        when(mapping.getDnsZoneId()).thenReturn(ZONE_ID);
        DnsZoneVO inactiveZone = Mockito.spy(new DnsZoneVO("ex.com", DnsZone.ZoneType.Public, SERVER_ID, ACCOUNT_ID, DOMAIN_ID, ""));
        // state defaults to Inactive
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(inactiveZone);
        manager.addDnsRecordForVM(vm, network, nic);
        verify(dnsServerDao, never()).findById(anyLong());
    }

    @Test
    public void testAddDnsRecordForVMServerMissing() {
        Network network = mock(Network.class);
        NicVO nic = mock(NicVO.class);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        DnsZoneNetworkMapVO mapping = mock(DnsZoneNetworkMapVO.class);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(dnsZoneNetworkMapDao.findByNetworkId(NETWORK_ID)).thenReturn(mapping);
        when(mapping.getDnsZoneId()).thenReturn(ZONE_ID);
        DnsZoneVO activeZone = Mockito.spy(new DnsZoneVO("ex.com", DnsZone.ZoneType.Public, SERVER_ID, ACCOUNT_ID, DOMAIN_ID, ""));
        activeZone.setState(DnsZone.State.Active);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(activeZone);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(null);
        when(vm.getInstanceName()).thenReturn("vm-1");
        manager.addDnsRecordForVM(vm, network, nic);
        verify(nicDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), eq(true));
    }

    @Test
    public void testDeleteDnsRecordForVMNoNicDetail() {
        Network network = mock(Network.class);
        NicVO nic = mock(NicVO.class);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(nic.getId()).thenReturn(50L);
        when(vm.getInstanceName()).thenReturn("vm-1");
        when(nicDetailsDao.findDetail(50L, "nicdnsrecord")).thenReturn(null);
        manager.deleteDnsRecordForVM(vm, network, nic);
        verify(dnsZoneNetworkMapDao, never()).findByNetworkId(anyLong());
    }

    @Test
    public void testDeleteDnsRecordForVMNicDetailBlankValue() {
        Network network = mock(Network.class);
        NicVO nic = mock(NicVO.class);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        NicDetailVO detail = mock(NicDetailVO.class);
        when(nic.getId()).thenReturn(50L);
        when(vm.getInstanceName()).thenReturn("vm-1");
        when(nicDetailsDao.findDetail(50L, "nicdnsrecord")).thenReturn(detail);
        when(detail.getValue()).thenReturn("  ");
        manager.deleteDnsRecordForVM(vm, network, nic);
        verify(dnsZoneNetworkMapDao, never()).findByNetworkId(anyLong());
    }

    @Test
    public void testProcessEventForDnsRecordAdd() throws Exception {
        Network network = mock(Network.class);
        NicVO nic = mock(NicVO.class);
        VMInstanceVO vm = mock(VMInstanceVO.class);

        when(dnsZoneNetworkMapDao.findByNetworkId(anyLong())).thenReturn(null);
        when(network.getId()).thenReturn(NETWORK_ID);
        manager.processEventForDnsRecord(vm, network, nic, true);
        // addDnsRecordForVM was called → returns early because no mapping
        verify(dnsZoneNetworkMapDao, times(1)).findByNetworkId(NETWORK_ID);
    }

    @Test
    public void testProcessEventForDnsRecordDelete() {
        Network network = mock(Network.class);
        NicVO nic = mock(NicVO.class);
        VMInstanceVO vm = mock(VMInstanceVO.class);

        when(nic.getId()).thenReturn(50L);
        when(vm.getInstanceName()).thenReturn("vm-1");
        when(nicDetailsDao.findDetail(50L, "nicdnsrecord")).thenReturn(null);
        manager.processEventForDnsRecord(vm, network, nic, false);
        verify(nicDetailsDao, times(1)).findDetail(50L, "nicdnsrecord");
    }

    @Test
    public void testGetCommandsReturnsNonEmptyList() {
        List<Class<?>> commands = manager.getCommands();
        assertNotNull(commands);
        assertFalse(commands.isEmpty());
        assertTrue(commands.size() > 5);
    }

    @Test
    public void testStartWithNoProviders() {
        manager.setDnsProviders(Collections.emptyList());
        assertTrue(manager.start());
    }

    @Test
    public void testStartWithProviders() {
        assertTrue(manager.start());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAssociateZoneToNetworkZoneNotFound() {
        org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd cmd =
                mock(org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd.class);
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(null);
        manager.associateZoneToNetwork(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAssociateZoneToNetworkNetworkNotFound() {
        org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd cmd =
                mock(org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd.class);
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(cmd.getNetworkId()).thenReturn(NETWORK_ID);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        when(networkDao.findById(NETWORK_ID)).thenReturn(null);
        manager.associateZoneToNetwork(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAssociateZoneToNetworkNonSharedNetwork() {
        org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd cmd =
                mock(org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd.class);
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(cmd.getNetworkId()).thenReturn(NETWORK_ID);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        NetworkVO network = mock(NetworkVO.class);
        when(network.getGuestType()).thenReturn(NetworkVO.GuestType.Isolated);
        when(networkDao.findById(NETWORK_ID)).thenReturn(network);
        manager.associateZoneToNetwork(cmd);
    }

    @Test
    public void testAssociateZoneToNetworkSuccess() {
        org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd cmd =
                mock(org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd.class);
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(cmd.getNetworkId()).thenReturn(NETWORK_ID);

        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        Mockito.doReturn("zone-uuid").when(zoneVO).getUuid();
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        NetworkVO network = mock(NetworkVO.class);
        when(network.getGuestType()).thenReturn(NetworkVO.GuestType.Shared);

        when(networkDao.findById(NETWORK_ID)).thenReturn(network);
        DnsZoneNetworkMapVO savedMapping = mock(DnsZoneNetworkMapVO.class);
        when(dnsZoneNetworkMapDao.persist(any(DnsZoneNetworkMapVO.class))).thenReturn(savedMapping);
        DnsZoneNetworkMapResponse response = manager.associateZoneToNetwork(cmd);
        assertNotNull(response);
        verify(dnsZoneNetworkMapDao).persist(any(DnsZoneNetworkMapVO.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAssociateZoneToNetworkAlreadyAssociated() {
        org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd cmd =
                mock(org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd.class);
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(cmd.getNetworkId()).thenReturn(NETWORK_ID);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        NetworkVO network = mock(NetworkVO.class);
        when(network.getGuestType()).thenReturn(NetworkVO.GuestType.Shared);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(networkDao.findById(NETWORK_ID)).thenReturn(network);
        when(dnsZoneNetworkMapDao.findByNetworkId(NETWORK_ID)).thenReturn(mock(DnsZoneNetworkMapVO.class));
        manager.associateZoneToNetwork(cmd);
    }

    @Test
    public void testCreateDnsRecordSuccess() throws Exception {
        org.apache.cloudstack.api.command.user.dns.CreateDnsRecordCmd cmd = mock(org.apache.cloudstack.api.command.user.dns.CreateDnsRecordCmd.class);
        when(cmd.getName()).thenReturn("www");
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(cmd.getType()).thenReturn(DnsRecord.RecordType.A);
        when(cmd.getContents()).thenReturn(Collections.singletonList("1.2.3.4"));
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(anyLong())).thenReturn(serverVO);
        when(dnsProviderMock.addRecord(any(), any(), any())).thenReturn("www.example.com");

        DnsRecordResponse res = manager.createDnsRecord(cmd);
        assertNotNull(res);
        verify(dnsProviderMock).addRecord(any(), any(), any());
    }

    @Test
    public void testDeleteDnsRecordSuccess() throws Exception {
        org.apache.cloudstack.api.command.user.dns.DeleteDnsRecordCmd cmd = mock(org.apache.cloudstack.api.command.user.dns.DeleteDnsRecordCmd.class);
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(cmd.getName()).thenReturn("www");
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(anyLong())).thenReturn(serverVO);
        when(dnsProviderMock.deleteRecord(any(), any(), any())).thenReturn("www.example.com");

        boolean res = manager.deleteDnsRecord(cmd);
        assertTrue(res);
        verify(dnsProviderMock).deleteRecord(any(), any(), any());
    }

    @Test
    public void testDeleteDnsRecordForVMSuccess() throws Exception {
        Network network = mock(Network.class);
        NicVO nic = mock(NicVO.class);
        when(nic.getIPv4Address()).thenReturn("1.2.3.4");
        VMInstanceVO vm = mock(VMInstanceVO.class);
        NicDetailVO detail = mock(NicDetailVO.class);
        when(nic.getId()).thenReturn(50L);
        when(vm.getInstanceName()).thenReturn("vm-1");
        when(nicDetailsDao.findDetail(50L, "nicdnsrecord")).thenReturn(detail);
        when(detail.getValue()).thenReturn("vm-1.ex.com");

        DnsZoneNetworkMapVO mapping = mock(DnsZoneNetworkMapVO.class);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(dnsZoneNetworkMapDao.findByNetworkId(NETWORK_ID)).thenReturn(mapping);
        when(mapping.getDnsZoneId()).thenReturn(ZONE_ID);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(anyLong())).thenReturn(serverVO);
        when(dnsProviderMock.deleteRecord(any(), any(), any())).thenReturn("vm-1.ex.com");

        manager.deleteDnsRecordForVM(vm, network, nic);
        verify(dnsProviderMock).deleteRecord(any(), any(), any());
        verify(nicDetailsDao).removeDetail(50L, "nicdnsrecord");
    }

    @Test
    public void testConfigure() throws Exception {
        assertTrue(manager.configure("dnsProviderManagerImpl", Collections.emptyMap()));
        verify(messageBus, times(3)).subscribe(anyString(), any());
    }

    @Test
    public void testHandleVmEventAndNicEvent() throws Exception {
        VMInstanceVO vm = mock(VMInstanceVO.class);
        NicVO nic = mock(NicVO.class);
        NetworkVO network = mock(NetworkVO.class);
        when(network.getId()).thenReturn(NETWORK_ID);

        when(vmInstanceDao.findById(10L)).thenReturn(vm);
        when(nicDao.findByIdIncludingRemoved(50L)).thenReturn(nic);
        when(nic.getNetworkId()).thenReturn(NETWORK_ID);
        when(networkDao.findById(NETWORK_ID)).thenReturn(network);
        when(network.getGuestType()).thenReturn(Network.GuestType.Shared);
        when(dnsZoneNetworkMapDao.findByNetworkId(NETWORK_ID)).thenReturn(null);

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(manager, "handleNicEvent", 50L, 10L, true);
        verify(dnsZoneNetworkMapDao, times(1)).findByNetworkId(NETWORK_ID);

        when(vmInstanceDao.findByIdIncludingRemoved(10L)).thenReturn(vm);
        when(vm.getId()).thenReturn(10L);
        when(nicDao.listByVmIdIncludingRemoved(10L)).thenReturn(Collections.singletonList(nic));

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(manager, "handleVmEvent", 10L, true);
        verify(dnsZoneNetworkMapDao, times(2)).findByNetworkId(NETWORK_ID);
    }

    @Test
    public void testAddDnsServerSuccess() throws Exception {
        org.apache.cloudstack.api.command.user.dns.AddDnsServerCmd cmd = mock(org.apache.cloudstack.api.command.user.dns.AddDnsServerCmd.class);
        when(callerMock.getType()).thenReturn(Account.Type.ADMIN);
        when(cmd.getUrl()).thenReturn("http://newpdns:8081");
        when(cmd.getProvider()).thenReturn(DnsProviderType.PowerDNS);
        when(dnsServerDao.findByUrlAndAccount(anyString(), anyLong())).thenReturn(null);
        when(dnsProviderMock.validateAndResolveServer(any())).thenReturn("resolved-id");
        when(dnsServerDao.persist(any())).thenReturn(serverVO);
        DnsServer result = manager.addDnsServer(cmd);
        assertNotNull(result);
        verify(dnsServerDao).persist(any());
    }

    @Test
    public void testListDnsServers() {
        org.apache.cloudstack.api.command.user.dns.ListDnsServersCmd cmd = mock(org.apache.cloudstack.api.command.user.dns.ListDnsServersCmd.class);
        when(domainDao.getDomainParentIds(anyLong())).thenReturn(Collections.emptySet());
        List<DnsServerVO> servers = Collections.singletonList(serverVO);
        com.cloud.utils.Pair<List<DnsServerVO>, Integer> searchPair = new com.cloud.utils.Pair<>(servers, 1);
        when(dnsServerDao.searchDnsServer(any(), anyLong(), any(), any(), any(), any())).thenReturn(searchPair);

        DnsServerJoinVO joinVO = mock(DnsServerJoinVO.class);
        when(joinVO.getProviderType()).thenReturn(DnsProviderType.PowerDNS.toString());
        when(joinVO.getState()).thenReturn(DnsServer.State.Enabled);
        when(dnsServerJoinDao.listByUuids(any())).thenReturn(Collections.singletonList(joinVO));

        ListResponse<DnsServerResponse> res = manager.listDnsServers(cmd);
        assertEquals(1, res.getCount().intValue());
    }

    @Test
    public void testUpdateDnsServer() throws Exception {
        org.apache.cloudstack.api.command.user.dns.UpdateDnsServerCmd cmd = mock(org.apache.cloudstack.api.command.user.dns.UpdateDnsServerCmd.class);
        when(cmd.getId()).thenReturn(SERVER_ID);
        when(cmd.getName()).thenReturn("updated-name");
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        when(dnsServerDao.update(eq(SERVER_ID), any())).thenReturn(true);
        DnsServer res = manager.updateDnsServer(cmd);
        assertNotNull(res);
        verify(dnsServerDao).update(eq(SERVER_ID), any());
    }

    @Test
    public void testListDnsZones() {
        org.apache.cloudstack.api.command.user.dns.ListDnsZonesCmd cmd = mock(org.apache.cloudstack.api.command.user.dns.ListDnsZonesCmd.class);
        when(cmd.getId()).thenReturn(null);
        when(dnsServerDao.listDnsServerIdsByAccountId(anyLong())).thenReturn(Collections.emptyList());
        List<DnsZoneVO> zones = Collections.singletonList(zoneVO);
        com.cloud.utils.Pair<List<DnsZoneVO>, Integer> searchPair = new com.cloud.utils.Pair<>(zones, 1);
        when(dnsZoneDao.searchZones(any(), anyLong(), any(), any(), any(), any())).thenReturn(searchPair);

        DnsZoneJoinVO joinVO = mock(DnsZoneJoinVO.class);
        when(dnsZoneJoinDao.listByUuids(any())).thenReturn(Collections.singletonList(joinVO));

        ListResponse<DnsZoneResponse> res = manager.listDnsZones(cmd);
        assertEquals(1, res.getCount().intValue());
    }
}
