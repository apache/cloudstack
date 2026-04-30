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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.cloudstack.dns.dao.NicDnsJoinDao;
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
import org.apache.cloudstack.dns.vo.NicDnsJoinVO;
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
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
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

    @Mock
    AccountManager accountMgr;
    @Mock
    DnsServerDao dnsServerDao;
    @Mock
    DnsZoneDao dnsZoneDao;
    @Mock
    DnsZoneJoinDao dnsZoneJoinDao;
    @Mock
    DnsServerJoinDao dnsServerJoinDao;
    @Mock
    DnsZoneNetworkMapDao dnsZoneNetworkMapDao;
    @Mock
    NetworkDao networkDao;
    @Mock
    DomainDao domainDao;
    @Mock
    NicDao nicDao;
    @Mock
    NicDetailsDao nicDetailsDao;
    @Mock
    NicDnsJoinDao nicDnsJoinDao;
    @Mock
    MessageBus messageBus;
    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    DnsProvider dnsProviderMock;
    @Mock
    Account callerMock;

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

        serverVO = Mockito.spy(
                new DnsServerVO("test-server", "http://pdns:8081", 8081, DnsProviderType.PowerDNS, null,
                        "apikey", false, null, Collections.singletonList("ns1.example.com"), ACCOUNT_ID, DOMAIN_ID));

        Map<String, String> serverDetails = new HashMap<>();
        serverDetails.put("pdsnServerId", "localhost");
        serverVO.setDetails(serverDetails);
        Mockito.lenient().doReturn(SERVER_ID).when(serverVO).getId();

        zoneVO = Mockito.spy(
                new DnsZoneVO("example.com", DnsZone.ZoneType.Public, SERVER_ID, ACCOUNT_ID, DOMAIN_ID, "Test zone"));
        Mockito.lenient().doReturn(ZONE_ID).when(zoneVO).getId();

        when(dnsProviderMock.getProviderType()).thenReturn(DnsProviderType.PowerDNS);
        manager.setDnsProviders(Collections.singletonList(dnsProviderMock));

        doNothing().when(accountMgr).checkAccess(any(Account.class),
                nullable(org.apache.cloudstack.acl.SecurityChecker.AccessType.class), eq(true), any());
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
        manager.provisionDnsZone(ZONE_ID, false);
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
        manager.provisionDnsZone(ZONE_ID, false);
    }

    @Test
    public void testProvisionDnsZoneSuccess() throws Exception {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        when(dnsProviderMock.provisionZone(any(), any())).thenReturn("example.com.");
        when(dnsZoneDao.update(anyLong(), any())).thenReturn(true);
        DnsZone result = manager.provisionDnsZone(ZONE_ID, false);
        assertNotNull(result);
        verify(dnsProviderMock).provisionZone(serverVO, zoneVO);
        verify(dnsZoneDao).update(anyLong(), any());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testProvisionDnsZoneConflictException() throws Exception {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        when(dnsProviderMock.provisionZone(any(), any())).thenThrow(new DnsConflictException("conflict"));
        manager.provisionDnsZone(ZONE_ID, false);
        verify(dnsZoneDao).remove(ZONE_ID);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testProvisionDnsZoneTransportException() throws Exception {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        when(dnsProviderMock.provisionZone(any(), any()))
                .thenThrow(new DnsTransportException("unreachable", new IOException("i/o")));
        manager.provisionDnsZone(ZONE_ID, false);
        verify(dnsZoneDao).remove(ZONE_ID);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteDnsZoneNotFound() {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(null);
        manager.deleteDnsZone(ZONE_ID, false);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDeleteDnsZoneServerMissing() {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(null);
        manager.deleteDnsZone(ZONE_ID, false);
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
        doNothing().when(accountMgr).checkAccess(any(Account.class),
                nullable(org.apache.cloudstack.acl.SecurityChecker.AccessType.class), eq(true), any());

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
        doNothing().when(accountMgr).checkAccess(any(Account.class),
                nullable(org.apache.cloudstack.acl.SecurityChecker.AccessType.class), eq(true), any());
        when(dnsZoneNetworkMapDao.findByZoneId(ZONE_ID)).thenReturn(null);
        when(dnsZoneDao.remove(ZONE_ID)).thenReturn(true);

        try (MockedStatic<Transaction> transactionMock = Mockito.mockStatic(Transaction.class)) {
            transactionMock.when(() -> Transaction.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
                TransactionCallback<Boolean> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });

            boolean res = manager.deleteDnsZone(ZONE_ID, false);
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
        DnsRecord record = new DnsRecord("www.example.com", DnsRecord.RecordType.A,
                Collections.singletonList("1.2.3.4"), 300);
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
        ReflectionTestUtils.setField(manager, "accountDao", Mockito.mock(com.cloud.user.dao.AccountDao.class));
        com.cloud.user.dao.AccountDao accountDaoMock = (com.cloud.user.dao.AccountDao) ReflectionTestUtils
                .getField(manager, "accountDao");
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
        org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd.class);
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(null);
        manager.associateZoneToNetwork(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAssociateZoneToNetworkNetworkNotFound() {
        org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd.class);
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(cmd.getNetworkId()).thenReturn(NETWORK_ID);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        when(networkDao.findById(NETWORK_ID)).thenReturn(null);
        manager.associateZoneToNetwork(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAssociateZoneToNetworkNonSharedNetwork() {
        org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd.class);
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
        org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd.class);
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
        org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.AssociateDnsZoneToNetworkCmd.class);
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
        org.apache.cloudstack.api.command.user.dns.CreateDnsRecordCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.CreateDnsRecordCmd.class);
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
        org.apache.cloudstack.api.command.user.dns.DeleteDnsRecordCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.DeleteDnsRecordCmd.class);
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
    public void testConfigure() throws Exception {
        assertTrue(manager.configure("dnsProviderManagerImpl", Collections.emptyMap()));
        verify(messageBus, times(3)).subscribe(anyString(), any());
    }

    @Test
    public void testAddDnsServerSuccess() throws Exception {
        org.apache.cloudstack.api.command.user.dns.AddDnsServerCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.AddDnsServerCmd.class);
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
        org.apache.cloudstack.api.command.user.dns.ListDnsServersCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.ListDnsServersCmd.class);
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
        org.apache.cloudstack.api.command.user.dns.UpdateDnsServerCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.UpdateDnsServerCmd.class);
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
        org.apache.cloudstack.api.command.user.dns.ListDnsZonesCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.ListDnsZonesCmd.class);
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

    @Test(expected = InvalidParameterValueException.class)
    public void testAddDnsServerAlreadyExists() {
        org.apache.cloudstack.api.command.user.dns.AddDnsServerCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.AddDnsServerCmd.class);
        when(cmd.getUrl()).thenReturn("http://newpdns:8081");
        when(dnsServerDao.findByUrlAndAccount(anyString(), anyLong())).thenReturn(serverVO);
        manager.addDnsServer(cmd);
    }

    @Test
    public void testAddDnsServerNormalUser() throws Exception {
        org.apache.cloudstack.api.command.user.dns.AddDnsServerCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.AddDnsServerCmd.class);
        when(callerMock.getType()).thenReturn(Account.Type.NORMAL);
        when(cmd.getUrl()).thenReturn("http://newpdns:8081");
        when(cmd.getProvider()).thenReturn(DnsProviderType.PowerDNS);
        when(cmd.getNameServers()).thenReturn(Collections.emptyList());
        when(cmd.isPublic()).thenReturn(true);
        when(cmd.getPublicDomainSuffix()).thenReturn("example.com");
        when(dnsServerDao.findByUrlAndAccount(anyString(), anyLong())).thenReturn(null);
        when(dnsProviderMock.validateAndResolveServer(any())).thenReturn("resolved-id");
        when(dnsServerDao.persist(any())).thenReturn(serverVO);
        DnsServer result = manager.addDnsServer(cmd);
        assertNotNull(result);
        verify(dnsServerDao).persist(Mockito.argThat(
                s -> !((DnsServerVO) s).getPublicServer() && ((DnsServerVO) s).getPublicDomainSuffix() == null));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAddDnsServerValidationFailure() throws Exception {
        org.apache.cloudstack.api.command.user.dns.AddDnsServerCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.AddDnsServerCmd.class);
        when(callerMock.getType()).thenReturn(Account.Type.ADMIN);
        when(cmd.getUrl()).thenReturn("http://newpdns:8081");
        when(cmd.getProvider()).thenReturn(DnsProviderType.PowerDNS);
        when(cmd.getNameServers()).thenReturn(Collections.emptyList());
        when(dnsServerDao.findByUrlAndAccount(anyString(), anyLong())).thenReturn(null);
        when(dnsProviderMock.validateAndResolveServer(any())).thenThrow(new CloudRuntimeException("Validation failed"));
        manager.addDnsServer(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateDnsServerUrlDuplicate() {
        org.apache.cloudstack.api.command.user.dns.UpdateDnsServerCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.UpdateDnsServerCmd.class);
        when(cmd.getId()).thenReturn(SERVER_ID);
        when(cmd.getUrl()).thenReturn("http://duplicate:8081");
        DnsServerVO existingServer = mock(DnsServerVO.class);
        when(existingServer.getId()).thenReturn(SERVER_ID + 1); // Different ID implies duplicate

        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        Mockito.doReturn("http://original:8081").when(serverVO).getUrl();
        when(dnsServerDao.findByUrlAndAccount(anyString(), anyLong())).thenReturn(existingServer);

        manager.updateDnsServer(cmd);
    }

    @Test
    public void testUpdateDnsServerUrlValid() throws Exception {
        org.apache.cloudstack.api.command.user.dns.UpdateDnsServerCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.UpdateDnsServerCmd.class);
        when(cmd.getId()).thenReturn(SERVER_ID);
        when(cmd.getUrl()).thenReturn("http://new-url:8081");
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);

        Mockito.doReturn("http://original:8081").when(serverVO).getUrl();
        Mockito.doReturn(DnsProviderType.PowerDNS).when(serverVO).getProviderType();
        when(dnsServerDao.findByUrlAndAccount(anyString(), anyLong())).thenReturn(null);
        doNothing().when(dnsProviderMock).validate(any());
        when(dnsServerDao.update(anyLong(), any())).thenReturn(true);

        DnsServer result = manager.updateDnsServer(cmd);
        assertNotNull(result);
        verify(dnsProviderMock).validate(any()); // Changing URL triggers validationRequired
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateDnsServerValidationException() throws Exception {
        org.apache.cloudstack.api.command.user.dns.UpdateDnsServerCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.UpdateDnsServerCmd.class);
        when(cmd.getId()).thenReturn(SERVER_ID);
        when(cmd.getDnsApiKey()).thenReturn("new-api-key");

        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        Mockito.doReturn("old-api-key").when(serverVO).getDnsApiKey();
        Mockito.doReturn("http://original:8081").when(serverVO).getUrl();
        Mockito.doReturn(DnsProviderType.PowerDNS).when(serverVO).getProviderType();

        Mockito.doThrow(new CloudRuntimeException("Validation failed")).when(dnsProviderMock).validate(any());

        manager.updateDnsServer(cmd);
    }

    @Test
    public void testVmLifecycleSubscriberStateUnchanged() {
        DnsProviderManagerImpl.VmLifecycleSubscriber subscriber = manager.new VmLifecycleSubscriber();
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put(org.apache.cloudstack.api.ApiConstants.OLD_STATE, com.cloud.vm.VirtualMachine.State.Running);
        event.put(org.apache.cloudstack.api.ApiConstants.NEW_STATE, com.cloud.vm.VirtualMachine.State.Running);
        event.put(org.apache.cloudstack.api.ApiConstants.INSTANCE_ID, 10L);

        subscriber.onPublishMessage("sender", "subject", event);
        verify(vmInstanceDao, never()).findByIdIncludingRemoved(anyLong());
    }

    @Test
    public void testVmLifecycleSubscriberRunning() {
        DnsProviderManagerImpl.VmLifecycleSubscriber subscriber = manager.new VmLifecycleSubscriber();
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put(org.apache.cloudstack.api.ApiConstants.OLD_STATE, com.cloud.vm.VirtualMachine.State.Starting);
        event.put(org.apache.cloudstack.api.ApiConstants.NEW_STATE, com.cloud.vm.VirtualMachine.State.Running);
        event.put(org.apache.cloudstack.api.ApiConstants.INSTANCE_ID, 12L);

        // Expect handleVmEvent to be called, which accesses
        // vmInstanceDao.findByIdIncludingRemoved
        when(vmInstanceDao.findById(12L)).thenReturn(null);

        subscriber.onPublishMessage("sender", "subject", event);
        verify(vmInstanceDao, times(1)).findById(12L);
    }

    @Test
    public void testVmLifecycleSubscriberDestroyed() {
        DnsProviderManagerImpl.VmLifecycleSubscriber subscriber = manager.new VmLifecycleSubscriber();
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put(org.apache.cloudstack.api.ApiConstants.OLD_STATE, com.cloud.vm.VirtualMachine.State.Running);
        event.put(org.apache.cloudstack.api.ApiConstants.NEW_STATE, VirtualMachine.State.Destroyed);
        event.put(org.apache.cloudstack.api.ApiConstants.INSTANCE_ID, 15L);
        when(nicDnsJoinDao.listIncludingRemovedByVmId(15L)).thenReturn(null);
        subscriber.onPublishMessage("sender", "subject", event);
        verify(nicDnsJoinDao, times(1)).listIncludingRemovedByVmId(15L);
    }

    @Test
    public void testVmLifecycleSubscriberUnsupportedState() {
        DnsProviderManagerImpl.VmLifecycleSubscriber subscriber = manager.new VmLifecycleSubscriber();
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put(org.apache.cloudstack.api.ApiConstants.OLD_STATE, com.cloud.vm.VirtualMachine.State.Running);
        event.put(org.apache.cloudstack.api.ApiConstants.NEW_STATE, com.cloud.vm.VirtualMachine.State.Starting);
        event.put(org.apache.cloudstack.api.ApiConstants.INSTANCE_ID, 20L);

        subscriber.onPublishMessage("sender", "subject", event);
        verify(vmInstanceDao, never()).findByIdIncludingRemoved(anyLong());
    }

    @Test
    public void testVmLifecycleSubscriberException() {
        DnsProviderManagerImpl.VmLifecycleSubscriber subscriber = manager.new VmLifecycleSubscriber();
        // Passing invalid args to trigger ClassCastException or similar
        subscriber.onPublishMessage("sender", "subject", "not a map");
        // Should not throw exception upstream
        verify(vmInstanceDao, never()).findByIdIncludingRemoved(anyLong());
    }

    @Test
    public void testNicLifecycleSubscriberCreate() {
        DnsProviderManagerImpl.NicLifecycleSubscriber subscriber = manager.new NicLifecycleSubscriber();
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put(org.apache.cloudstack.api.ApiConstants.EVENT_TYPE, com.cloud.event.EventTypes.EVENT_NIC_CREATE);
        event.put(org.apache.cloudstack.api.ApiConstants.NIC_ID, 100L);
        event.put(org.apache.cloudstack.api.ApiConstants.INSTANCE_ID, 200L);

        when(vmInstanceDao.findById(200L)).thenReturn(null); // Short circuits handleNicEvent

        subscriber.onPublishMessage("sender", "subject", event);
        verify(vmInstanceDao, times(1)).findById(200L);
    }

    @Test
    public void testNicLifecycleSubscriberDelete() {
        DnsProviderManagerImpl.NicLifecycleSubscriber subscriber = manager.new NicLifecycleSubscriber();
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put(org.apache.cloudstack.api.ApiConstants.EVENT_TYPE, com.cloud.event.EventTypes.EVENT_NIC_DELETE);
        event.put(org.apache.cloudstack.api.ApiConstants.NIC_ID, 101L);
        event.put(org.apache.cloudstack.api.ApiConstants.INSTANCE_ID, 201L);
        when(nicDnsJoinDao.findByIdIncludingRemoved(101L)).thenReturn(null);
        subscriber.onPublishMessage("sender", "subject", event);
        verify(nicDnsJoinDao, times(1)).findByIdIncludingRemoved(101L);
    }

    @Test
    public void testNicLifecycleSubscriberMissingData() {
        DnsProviderManagerImpl.NicLifecycleSubscriber subscriber = manager.new NicLifecycleSubscriber();
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put(org.apache.cloudstack.api.ApiConstants.EVENT_TYPE, com.cloud.event.EventTypes.EVENT_NIC_CREATE);
        // Missing NIC_ID and INSTANCE_ID

        subscriber.onPublishMessage("sender", "subject", event);
        verify(vmInstanceDao, never()).findById(anyLong());
    }

    @Test
    public void testNicLifecycleSubscriberUnsupportedEvent() {
        DnsProviderManagerImpl.NicLifecycleSubscriber subscriber = manager.new NicLifecycleSubscriber();
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put(org.apache.cloudstack.api.ApiConstants.EVENT_TYPE, "unsupported-event");
        event.put(org.apache.cloudstack.api.ApiConstants.NIC_ID, 102L);
        event.put(org.apache.cloudstack.api.ApiConstants.INSTANCE_ID, 202L);

        subscriber.onPublishMessage("sender", "subject", event);
        verify(vmInstanceDao, never()).findById(anyLong());
    }

    @Test
    public void testNicLifecycleSubscriberException() {
        DnsProviderManagerImpl.NicLifecycleSubscriber subscriber = manager.new NicLifecycleSubscriber();
        subscriber.onPublishMessage("sender", "subject", "not a map");
        // Should catch and not throw
        verify(vmInstanceDao, never()).findById(anyLong());
    }

    @Test
    public void testPrepareDnsRecordUrlNullSubdomain() {
        String result = manager.prepareDnsRecordUrl("myvm", null, "example.com");
        assertEquals("myvm.example.com", result);
    }

    @Test
    public void testPrepareDnsRecordUrlBlankSubdomain() {
        String result = manager.prepareDnsRecordUrl("myvm", "   ", "example.com");
        assertEquals("myvm.example.com", result);
    }

    @Test
    public void testPrepareDnsRecordUrlTrimsSubdomain() {
        String result = manager.prepareDnsRecordUrl("myvm", "  sub  ", "example.com");
        assertEquals("myvm.sub.example.com", result);
    }

    @Test
    public void testCreateDnsRecordAlreadyExistsThrowsCloudRuntimeException() throws Exception {
        org.apache.cloudstack.api.command.user.dns.CreateDnsRecordCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.CreateDnsRecordCmd.class);
        when(cmd.getName()).thenReturn("www");
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(cmd.getType()).thenReturn(DnsRecord.RecordType.A);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(anyLong())).thenReturn(serverVO);
        when(dnsProviderMock.dnsRecordExists(any(), any(), anyString(), anyString())).thenReturn(true);

        boolean threw = false;
        try {
            manager.createDnsRecord(cmd);
        } catch (CloudRuntimeException ex) {
            threw = true;
        }
        assertTrue(threw);
    }

    @Test
    public void testDeleteDnsRecordProviderReturnsNullReturnsFalse() throws Exception {
        org.apache.cloudstack.api.command.user.dns.DeleteDnsRecordCmd cmd = mock(
                org.apache.cloudstack.api.command.user.dns.DeleteDnsRecordCmd.class);
        when(cmd.getDnsZoneId()).thenReturn(ZONE_ID);
        when(cmd.getName()).thenReturn("www");
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(anyLong())).thenReturn(serverVO);
        when(dnsProviderMock.deleteRecord(any(), any(), any())).thenReturn(null);

        boolean result = manager.deleteDnsRecord(cmd);
        assertFalse(result);
    }

    @Test
    public void testSyncDnsRecordsStateNoIpv4AndNoIpv6DeletesBothRecords() throws Exception {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        when(nicDnsJoinDao.listActiveByVmIdZoneAndDnsRecord(anyLong(), anyLong(), anyString()))
                .thenReturn(Collections.emptyList());

        manager.syncDnsRecordsState(1L, "myvm.example.com", ZONE_ID);

        verify(dnsProviderMock, times(2)).deleteRecord(eq(serverVO), eq(zoneVO), any(DnsRecord.class));
        verify(dnsProviderMock, never()).addRecord(any(), any(), any());
    }

    @Test
    public void testSyncDnsRecordsStateOnlyIpv4AddsAAndDeletesAAAA() throws Exception {
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);

        NicDnsJoinVO nic = mock(NicDnsJoinVO.class);
        when(nic.getIp4Address()).thenReturn("10.0.0.1");
        when(nic.getIp6Address()).thenReturn(null);
        when(nicDnsJoinDao.listActiveByVmIdZoneAndDnsRecord(anyLong(), anyLong(), anyString()))
                .thenReturn(Collections.singletonList(nic));

        manager.syncDnsRecordsState(1L, "myvm.example.com", ZONE_ID);

        verify(dnsProviderMock, times(1)).addRecord(eq(serverVO), eq(zoneVO),
                Mockito.argThat(r -> r.getType() == DnsRecord.RecordType.A));
        verify(dnsProviderMock, times(1)).deleteRecord(eq(serverVO), eq(zoneVO),
                Mockito.argThat(r -> r.getType() == DnsRecord.RecordType.AAAA));
    }

    @Test
    public void testHandleVmCreateEventFoundButNoActiveNics() throws DnsProviderException {
        com.cloud.vm.VMInstanceVO instanceMock = mock(com.cloud.vm.VMInstanceVO.class);
        when(vmInstanceDao.findById(30L)).thenReturn(instanceMock);
        when(nicDnsJoinDao.listActiveByVmId(30L)).thenReturn(Collections.emptyList());

        manager.handleVmCreateEvent(30L);

        verify(dnsProviderMock, never()).addRecord(any(), any(), any());
        verify(dnsProviderMock, never()).deleteRecord(any(), any(), any());
    }

    @Test
    public void testHandleVmDestroyEventNicWithNullDnsUrlIsSkipped() throws DnsProviderException {
        NicDnsJoinVO nicMock =
                mock(NicDnsJoinVO.class);
        when(nicMock.getNicDnsName()).thenReturn(null);
        when(nicDnsJoinDao.listIncludingRemovedByVmId(31L))
                .thenReturn(Collections.singletonList(nicMock));

        manager.handleVmDestroyEvent(31L);

        verify(dnsProviderMock, never()).deleteRecord(any(), any(), any());
    }

    @Test
    public void testHandleVmDestroyEventWithValidDnsUrlTriggersCleanup() throws Exception {
        NicDnsJoinVO nicMock =
                mock(NicDnsJoinVO.class);
        when(nicMock.getNicDnsName()).thenReturn("myvm.example.com");
        when(nicMock.getDnsZoneId()).thenReturn(ZONE_ID);
        when(nicDnsJoinDao.listIncludingRemovedByVmId(32L))
                .thenReturn(Collections.singletonList(nicMock));

        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        when(nicDnsJoinDao.listActiveByVmIdZoneAndDnsRecord(eq(32L), eq(ZONE_ID), anyString()))
                .thenReturn(Collections.emptyList());

        try (MockedStatic<com.cloud.utils.db.Transaction> txMock =
                Mockito.mockStatic(com.cloud.utils.db.Transaction.class)) {
            txMock.when(() -> com.cloud.utils.db.Transaction.execute(
                    any(com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn.class)))
                    .thenAnswer(invocation -> {
                        com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn<?> cb =
                                invocation.getArgument(0);
                        try {
                            cb.doInTransactionWithoutResult(null);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    });

            manager.handleVmDestroyEvent(32L);

            verify(nicDetailsDao).removeDetail(nicMock.getId(), org.apache.cloudstack.api.ApiConstants.NIC_DNS_NAME);
            verify(dnsProviderMock, times(2)).deleteRecord(eq(serverVO), eq(zoneVO), any(DnsRecord.class));
        }
    }

    @Test
    public void testHandleNicPlugVmNotRunningExitsEarly() throws DnsProviderException {
        com.cloud.vm.VMInstanceVO instanceMock = mock(com.cloud.vm.VMInstanceVO.class);
        when(instanceMock.getState()).thenReturn(VirtualMachine.State.Destroyed);
        when(vmInstanceDao.findById(33L)).thenReturn(instanceMock);
        manager.handleNicPlug(33L, 500L);
        verify(nicDnsJoinDao, never()).findById(anyLong());
        verify(dnsProviderMock, never()).addRecord(any(), any(), any());
    }

    @Test
    public void testHandleNicUnplugNicHasValidDnsUrlTriggersSyncCleanup() throws Exception {
        NicDnsJoinVO nicMock =
                mock(NicDnsJoinVO.class);
        when(nicMock.getNicDnsName()).thenReturn("myvm.example.com");
        when(nicMock.getDnsZoneId()).thenReturn(ZONE_ID);
        when(nicDnsJoinDao.findByIdIncludingRemoved(600L)).thenReturn(nicMock);

        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);
        when(nicDnsJoinDao.listActiveByVmIdZoneAndDnsRecord(eq(34L), eq(ZONE_ID), anyString()))
                .thenReturn(Collections.emptyList());

        try (MockedStatic<com.cloud.utils.db.Transaction> txMock =
                Mockito.mockStatic(com.cloud.utils.db.Transaction.class)) {
            txMock.when(() -> com.cloud.utils.db.Transaction.execute(
                    any(com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn.class)))
                    .thenAnswer(invocation -> {
                        com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn<?> cb =
                                invocation.getArgument(0);
                        try {
                            cb.doInTransactionWithoutResult(null);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    });

            manager.handleNicUnplug(34L, 600L);

            verify(nicDetailsDao).removeDetail(600L, org.apache.cloudstack.api.ApiConstants.NIC_DNS_NAME);
            verify(dnsProviderMock, times(2)).deleteRecord(eq(serverVO), eq(zoneVO), any(DnsRecord.class));
        }
    }

    @Test
    public void testHandleVmHostnameChangedVmFoundButNoActiveNicsExitsEarly() throws DnsProviderException {
        com.cloud.vm.VMInstanceVO instanceMock = mock(com.cloud.vm.VMInstanceVO.class);
        when(vmInstanceDao.findById(35L)).thenReturn(instanceMock);
        when(nicDnsJoinDao.listActiveByVmId(35L)).thenReturn(Collections.emptyList());

        manager.handleVmHostnameChanged(35L, "newname");

        verify(dnsZoneDao, never()).findById(anyLong());
        verify(dnsProviderMock, never()).addRecord(any(), any(), any());
    }

    @Test
    public void testIsDnsCollisionReturnsTrueForDifferentInstance() {
        NicDnsJoinVO existing =
                mock(NicDnsJoinVO.class);
        when(existing.getInstanceId()).thenReturn(99L);
        when(nicDnsJoinDao.findActiveByDnsRecordAndZone("vm.example.com", ZONE_ID)).thenReturn(existing);

        try (MockedStatic<com.cloud.event.ActionEventUtils> aeMock =
                Mockito.mockStatic(com.cloud.event.ActionEventUtils.class)) {
            aeMock.when(() -> com.cloud.event.ActionEventUtils.onActionEvent(
                    anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyLong(), anyString()))
                    .thenReturn(1L);
            boolean result = (boolean) ReflectionTestUtils.invokeMethod(
                    manager, "isDnsCollision", "vm.example.com", ZONE_ID, 42L);
            assertTrue(result);
        }
    }

    @Test
    public void testIsDnsCollisionReturnsFalseWhenNoExistingRecord() {
        when(nicDnsJoinDao.findActiveByDnsRecordAndZone("vm.example.com", ZONE_ID)).thenReturn(null);
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(
                manager, "isDnsCollision", "vm.example.com", ZONE_ID, 42L);
        assertFalse(result);
    }

    @Test
    public void testIsDnsCollisionReturnsFalseWhenSameInstance() {
        NicDnsJoinVO existing =
                mock(NicDnsJoinVO.class);
        when(existing.getInstanceId()).thenReturn(42L);
        when(nicDnsJoinDao.findActiveByDnsRecordAndZone("vm.example.com", ZONE_ID)).thenReturn(existing);
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(
                manager, "isDnsCollision", "vm.example.com", ZONE_ID, 42L);
        assertFalse(result);
    }

    @Test
    public void testHandleNicPlugRunningVmNicFoundButZoneNullExitsGracefully() throws DnsProviderException {
        com.cloud.vm.VMInstanceVO instanceMock = mock(com.cloud.vm.VMInstanceVO.class);
        when(instanceMock.getState()).thenReturn(com.cloud.vm.VirtualMachine.State.Running);
        when(vmInstanceDao.findById(40L)).thenReturn(instanceMock);

        NicDnsJoinVO nicMock =
                mock(NicDnsJoinVO.class);
        when(nicMock.getDnsZoneId()).thenReturn(ZONE_ID);
        when(nicDnsJoinDao.findById(700L)).thenReturn(nicMock);
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(null); // zone missing → exit after NIC lookup

        manager.handleNicPlug(40L, 700L);

        verify(nicDnsJoinDao, times(1)).findById(700L);
        verify(dnsProviderMock, never()).addRecord(any(), any(), any());
    }

    @Test
    public void testHandleVmHostnameChangedNonEmptyNicsAllZonesMissingSkipsTransactions()
            throws DnsProviderException {
        com.cloud.vm.VMInstanceVO instanceMock = mock(com.cloud.vm.VMInstanceVO.class);
        when(vmInstanceDao.findById(41L)).thenReturn(instanceMock);

        NicDnsJoinVO nicMock =
                mock(NicDnsJoinVO.class);
        when(nicMock.getDnsZoneId()).thenReturn(ZONE_ID);
        when(nicDnsJoinDao.listActiveByVmId(41L)).thenReturn(Collections.singletonList(nicMock));
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(null); // zone null → NIC skipped → empty map

        manager.handleVmHostnameChanged(41L, "newname");

        verify(dnsZoneDao, times(1)).findById(ZONE_ID);
        verify(dnsProviderMock, never()).addRecord(any(), any(), any());
    }

    @Test
    public void testHandleVmCreateEventNonEmptyNicsAllZonesMissingSkipsSync() throws DnsProviderException {
        com.cloud.vm.VMInstanceVO instanceMock = mock(com.cloud.vm.VMInstanceVO.class);
        when(vmInstanceDao.findById(42L)).thenReturn(instanceMock);

        NicDnsJoinVO nicMock =
                mock(NicDnsJoinVO.class);
        when(nicMock.getDnsZoneId()).thenReturn(ZONE_ID);
        when(nicDnsJoinDao.listActiveByVmId(42L)).thenReturn(Collections.singletonList(nicMock));
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(null); // zone null → NIC skipped → empty outer map

        manager.handleVmCreateEvent(42L);

        verify(dnsZoneDao, times(1)).findById(ZONE_ID);
        verify(dnsProviderMock, never()).addRecord(any(), any(), any());
    }

    @Test
    public void testVmRenameSubscriberInvalidPayloadIsSwallowed() {
        DnsProviderManagerImpl.VmRenameActionSubscriber subscriber =
                manager.new VmRenameActionSubscriber();
        subscriber.onPublishMessage("sender", "topic", "not-a-map");
        verify(vmInstanceDao, never()).findById(anyLong());
    }

    @Test
    public void testVmRenameSubscriberMissingInstanceIdSwallowsNpe() {
        DnsProviderManagerImpl.VmRenameActionSubscriber subscriber =
                manager.new VmRenameActionSubscriber();
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put(org.apache.cloudstack.api.ApiConstants.EVENT_TYPE,
                com.cloud.event.EventTypes.EVENT_VM_UPDATE);
        event.put(org.apache.cloudstack.api.ApiConstants.HOST_NAME, "newvm");
        event.put(org.apache.cloudstack.api.ApiConstants.OLD_HOST_NAME, "oldvm");
        // INSTANCE_ID intentionally absent → (long) null → NullPointerException → caught internally
        subscriber.onPublishMessage("sender", "topic", event);
        verify(vmInstanceDao, never()).findById(anyLong());
    }

    // ─── handleVmRunningState ──────────────────────────────────────────────────

    @Test
    public void testHandleVmCreateEventInstanceNullExitsEarly() throws DnsProviderException {
        when(vmInstanceDao.findById(50L)).thenReturn(null);
        manager.handleVmCreateEvent(50L);
        verify(nicDnsJoinDao, never()).listActiveByVmId(anyLong());
    }

    @Test
    public void testHandleVmCreateEventFullSyncNoCollision() throws Exception {
        com.cloud.vm.VMInstanceVO instanceMock = mock(com.cloud.vm.VMInstanceVO.class);
        when(instanceMock.getHostName()).thenReturn("myvm");
        when(vmInstanceDao.findById(51L)).thenReturn(instanceMock);

        NicDnsJoinVO nicMock =
                mock(NicDnsJoinVO.class);
        when(nicMock.getDnsZoneId()).thenReturn(ZONE_ID);
        when(nicMock.getSubDomain()).thenReturn(null);
        when(nicDnsJoinDao.listActiveByVmId(51L)).thenReturn(Collections.singletonList(nicMock));
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);

        // no collision
        when(nicDnsJoinDao.findActiveByDnsRecordAndZone(anyString(), eq(ZONE_ID))).thenReturn(null);
        // sync: no IPs → delete both
        when(nicDnsJoinDao.listActiveByVmIdZoneAndDnsRecord(eq(51L), eq(ZONE_ID), anyString()))
                .thenReturn(Collections.emptyList());

        try (MockedStatic<com.cloud.utils.db.Transaction> txMock =
                Mockito.mockStatic(com.cloud.utils.db.Transaction.class)) {
            txMock.when(() -> com.cloud.utils.db.Transaction.execute(
                    any(com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn.class)))
                    .thenAnswer(invocation -> {
                        com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn<?> cb =
                                invocation.getArgument(0);
                        try { cb.doInTransactionWithoutResult(null); }
                        catch (Exception e) { throw new RuntimeException(e); }
                        return null;
                    });

            manager.handleVmCreateEvent(51L);

            verify(nicDetailsDao).addDetail(anyLong(),
                    eq(org.apache.cloudstack.api.ApiConstants.NIC_DNS_NAME), anyString(), eq(true));
            verify(dnsProviderMock, times(2)).deleteRecord(eq(serverVO), eq(zoneVO), any(DnsRecord.class));
        }
    }

    @Test
    public void testHandleVmCreateEventCollisionSkipsAddDetail() throws Exception {
        com.cloud.vm.VMInstanceVO instanceMock = mock(com.cloud.vm.VMInstanceVO.class);
        when(instanceMock.getHostName()).thenReturn("myvm");
        when(vmInstanceDao.findById(52L)).thenReturn(instanceMock);

        NicDnsJoinVO nicMock =
                mock(NicDnsJoinVO.class);
        when(nicMock.getDnsZoneId()).thenReturn(ZONE_ID);
        when(nicMock.getSubDomain()).thenReturn(null);
        when(nicDnsJoinDao.listActiveByVmId(52L)).thenReturn(Collections.singletonList(nicMock));
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);

        // collision: different instance owns the record
        NicDnsJoinVO colliding =
                mock(NicDnsJoinVO.class);
        when(colliding.getInstanceId()).thenReturn(999L);
        when(nicDnsJoinDao.findActiveByDnsRecordAndZone(anyString(), eq(ZONE_ID))).thenReturn(colliding);

        try (MockedStatic<com.cloud.utils.db.Transaction> txMock =
                Mockito.mockStatic(com.cloud.utils.db.Transaction.class);
             MockedStatic<com.cloud.event.ActionEventUtils> aeMock =
                Mockito.mockStatic(com.cloud.event.ActionEventUtils.class)) {
            aeMock.when(() -> com.cloud.event.ActionEventUtils.onActionEvent(
                    anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyLong(), anyString()))
                    .thenReturn(1L);
            txMock.when(() -> com.cloud.utils.db.Transaction.execute(
                    any(com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn.class)))
                    .thenAnswer(invocation -> {
                        com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn<?> cb =
                                invocation.getArgument(0);
                        try { cb.doInTransactionWithoutResult(null); }
                        catch (Exception e) { throw new RuntimeException(e); }
                        return null;
                    });

            manager.handleVmCreateEvent(52L);

            verify(nicDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), eq(true));
            verify(dnsProviderMock, never()).addRecord(any(), any(), any());
        }
    }

    // ─── handleVmHostnameChanged ───────────────────────────────────────────────

    @Test
    public void testHandleVmHostnameChangedInstanceNullExitsEarly() throws DnsProviderException {
        when(vmInstanceDao.findById(60L)).thenReturn(null);
        manager.handleVmHostnameChanged(60L, "newname");
        verify(nicDnsJoinDao, never()).listActiveByVmId(anyLong());
    }

    @Test
    public void testHandleVmHostnameChangedFqdnUnchangedSkipsNic() throws DnsProviderException {
        com.cloud.vm.VMInstanceVO instanceMock = mock(com.cloud.vm.VMInstanceVO.class);
        when(vmInstanceDao.findById(61L)).thenReturn(instanceMock);

        NicDnsJoinVO nicMock =
                mock(NicDnsJoinVO.class);
        when(nicMock.getDnsZoneId()).thenReturn(ZONE_ID);
        when(nicMock.getSubDomain()).thenReturn(null);
        // old URL already equals the new computed URL → continue (skip)
        when(nicMock.getNicDnsName()).thenReturn("newname.example.com");
        when(nicDnsJoinDao.listActiveByVmId(61L)).thenReturn(Collections.singletonList(nicMock));
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);

        manager.handleVmHostnameChanged(61L, "newname");

        // map stays empty → no Transaction executed
        verify(nicDetailsDao, never()).removeDetail(anyLong(), anyString());
        verify(dnsProviderMock, never()).addRecord(any(), any(), any());
    }

    @Test
    public void testHandleVmHostnameChangedFullRenamePath() throws Exception {
        com.cloud.vm.VMInstanceVO instanceMock = mock(com.cloud.vm.VMInstanceVO.class);
        when(vmInstanceDao.findById(62L)).thenReturn(instanceMock);

        NicDnsJoinVO nicMock =
                mock(NicDnsJoinVO.class);
        when(nicMock.getDnsZoneId()).thenReturn(ZONE_ID);
        when(nicMock.getSubDomain()).thenReturn(null);
        when(nicMock.getNicDnsName()).thenReturn("oldvm.example.com");   // differs from new FQDN
        when(nicDnsJoinDao.listActiveByVmId(62L)).thenReturn(Collections.singletonList(nicMock));
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);

        // no collision for new record
        when(nicDnsJoinDao.findActiveByDnsRecordAndZone(anyString(), eq(ZONE_ID))).thenReturn(null);
        // sync always returns empty → deleteRecord called
        when(nicDnsJoinDao.listActiveByVmIdZoneAndDnsRecord(eq(62L), eq(ZONE_ID), anyString()))
                .thenReturn(Collections.emptyList());

        try (MockedStatic<com.cloud.utils.db.Transaction> txMock =
                Mockito.mockStatic(com.cloud.utils.db.Transaction.class)) {
            txMock.when(() -> com.cloud.utils.db.Transaction.execute(
                    any(com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn.class)))
                    .thenAnswer(invocation -> {
                        com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn<?> cb =
                                invocation.getArgument(0);
                        try { cb.doInTransactionWithoutResult(null); }
                        catch (Exception e) { throw new RuntimeException(e); }
                        return null;
                    });

            manager.handleVmHostnameChanged(62L, "newvm");

            // Tx1: old URL removed from nic_details
            verify(nicDetailsDao).removeDetail(anyLong(),
                    eq(org.apache.cloudstack.api.ApiConstants.NIC_DNS_NAME));
            // Tx2: new URL written to nic_details
            verify(nicDetailsDao).addDetail(anyLong(),
                    eq(org.apache.cloudstack.api.ApiConstants.NIC_DNS_NAME), anyString(), eq(true));
            // deleteRecord called for both old-sync (A+AAAA) and new-sync (A+AAAA) = 4 total
            verify(dnsProviderMock, times(4)).deleteRecord(eq(serverVO), eq(zoneVO), any(DnsRecord.class));
        }
    }

    @Test
    public void testHandleVmHostnameChangedCollisionOnNewUrlSkipsAddDetail() {
        com.cloud.vm.VMInstanceVO instanceMock = mock(com.cloud.vm.VMInstanceVO.class);
        when(vmInstanceDao.findById(63L)).thenReturn(instanceMock);

        NicDnsJoinVO nicMock =
                mock(NicDnsJoinVO.class);
        when(nicMock.getDnsZoneId()).thenReturn(ZONE_ID);
        when(nicMock.getSubDomain()).thenReturn(null);
        when(nicMock.getNicDnsName()).thenReturn("oldvm.example.com");
        when(nicDnsJoinDao.listActiveByVmId(63L)).thenReturn(Collections.singletonList(nicMock));
        when(dnsZoneDao.findById(ZONE_ID)).thenReturn(zoneVO);
        when(dnsServerDao.findById(SERVER_ID)).thenReturn(serverVO);

        // collision on the new FQDN
        NicDnsJoinVO colliding =
                mock(NicDnsJoinVO.class);
        when(colliding.getInstanceId()).thenReturn(999L);
        when(nicDnsJoinDao.findActiveByDnsRecordAndZone(anyString(), eq(ZONE_ID))).thenReturn(colliding);
        when(nicDnsJoinDao.listActiveByVmIdZoneAndDnsRecord(eq(63L), eq(ZONE_ID), anyString()))
                .thenReturn(Collections.emptyList());

        try (MockedStatic<com.cloud.utils.db.Transaction> txMock =
                Mockito.mockStatic(com.cloud.utils.db.Transaction.class);
             MockedStatic<com.cloud.event.ActionEventUtils> aeMock =
                Mockito.mockStatic(com.cloud.event.ActionEventUtils.class)) {
            aeMock.when(() -> com.cloud.event.ActionEventUtils.onActionEvent(
                    anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyLong(), anyString()))
                    .thenReturn(1L);
            txMock.when(() -> com.cloud.utils.db.Transaction.execute(
                    any(com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn.class)))
                    .thenAnswer(invocation -> {
                        com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn<?> cb =
                                invocation.getArgument(0);
                        try { cb.doInTransactionWithoutResult(null); }
                        catch (Exception e) { throw new RuntimeException(e); }
                        return null;
                    });

            manager.handleVmHostnameChanged(63L, "newvm");

            // Tx2 collision → addDetail never called for new URL
            verify(nicDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), eq(true));
        }
    }
}
