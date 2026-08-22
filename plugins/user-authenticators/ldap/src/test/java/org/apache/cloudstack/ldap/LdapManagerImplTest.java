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
package org.apache.cloudstack.ldap;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.api.command.LinkAccountToLdapCmd;
import org.apache.cloudstack.api.response.LinkAccountToLdapResponse;
import org.apache.cloudstack.ldap.dao.LdapTrustMapDao;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link LdapManagerImpl#linkAccountToLdap}: re-linking an account replaces its own
 * mapping, refuses a group already claimed by another live account, and leaves no mapping
 * persisted if clearing the old one fails.
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapManagerImplTest {

    private static final Long DOMAIN_ID = 1L;
    private static final long ACCOUNT_ID = 24L;
    private static final long OLD_MAPPING_ID = 5L;

    private LdapManagerImpl ldapManager;

    private MockedStatic<LdapConfiguration> ldapConfigurationMockedStatic;

    @Mock
    private LdapTrustMapDao ldapTrustMapDaoMock;

    @Mock
    private DomainDao domainDaoMock;

    @Mock
    private AccountDao accountDaoMock;

    @Mock
    private RoleService roleServiceMock;

    @Before
    public void setup() {
        ldapConfigurationMockedStatic = Mockito.mockStatic(LdapConfiguration.class, Mockito.CALLS_REAL_METHODS);
        when(LdapConfiguration.getBaseDn(DOMAIN_ID)).thenReturn("dc=my,dc=domain,dc=com");

        ldapManager = new LdapManagerImpl();
        ldapManager._ldapTrustMapDao = ldapTrustMapDaoMock;
        ReflectionTestUtils.setField(ldapManager, "domainDao", domainDaoMock);
        ReflectionTestUtils.setField(ldapManager, "accountDao", accountDaoMock);
        when(domainDaoMock.findById(DOMAIN_ID)).thenReturn(new DomainVO());

        AccountVO existingAccount = new AccountVO("jdoe", DOMAIN_ID, null, Account.Type.NORMAL, null, "acct-uuid");
        ReflectionTestUtils.setField(existingAccount, "id", ACCOUNT_ID);
        when(accountDaoMock.findActiveAccount("jdoe", DOMAIN_ID)).thenReturn(existingAccount);
        when(ldapTrustMapDaoMock.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @After
    public void tearDown() {
        ldapConfigurationMockedStatic.close();
    }

    @Test
    public void relinkingAccountReplacesItsOwnExistingMapping() {
        LdapTrustMapVO ownMapping = new LdapTrustMapVO(DOMAIN_ID, LdapManager.LinkType.GROUP, "cn=old,dc=my,dc=domain,dc=com", Account.Type.NORMAL, ACCOUNT_ID);
        ReflectionTestUtils.setField(ownMapping, "id", OLD_MAPPING_ID);
        when(ldapTrustMapDaoMock.findByAccount(DOMAIN_ID, ACCOUNT_ID)).thenReturn(ownMapping);

        LinkAccountToLdapResponse response = ldapManager.linkAccountToLdap(buildCmd("cn=new,dc=my,dc=domain,dc=com"));

        verify(ldapTrustMapDaoMock, times(1)).expunge(Long.valueOf(OLD_MAPPING_ID));
        assertEquals("cn=new,dc=my,dc=domain,dc=com", response.getLdapDomain());
    }

    @Test
    public void firstLinkOfAccountDoesNotExpungeAnything() {
        when(ldapTrustMapDaoMock.findByAccount(DOMAIN_ID, ACCOUNT_ID)).thenReturn(null);

        ldapManager.linkAccountToLdap(buildCmd("cn=first,dc=my,dc=domain,dc=com"));

        verify(ldapTrustMapDaoMock, never()).expunge(any(Long.class));
    }

    @Test
    public void relinkingAccountToItsCurrentGroupDoesNotThrow() {
        LdapTrustMapVO ownMapping = new LdapTrustMapVO(DOMAIN_ID, LdapManager.LinkType.GROUP, "cn=same,dc=my,dc=domain,dc=com", Account.Type.NORMAL, ACCOUNT_ID);
        ReflectionTestUtils.setField(ownMapping, "id", OLD_MAPPING_ID);
        when(ldapTrustMapDaoMock.findGroupInDomain(DOMAIN_ID, "cn=same,dc=my,dc=domain,dc=com")).thenReturn(ownMapping);
        when(ldapTrustMapDaoMock.findByAccount(DOMAIN_ID, ACCOUNT_ID)).thenReturn(ownMapping);

        LinkAccountToLdapResponse response = ldapManager.linkAccountToLdap(buildCmd("cn=same,dc=my,dc=domain,dc=com"));

        assertEquals("cn=same,dc=my,dc=domain,dc=com", response.getLdapDomain());
    }

    @Test
    public void relinkingAccountRefusesGroupClaimedByAnotherLiveAccount() {
        long otherAccountId = 99L;
        LdapTrustMapVO otherMapping = new LdapTrustMapVO(DOMAIN_ID, LdapManager.LinkType.GROUP, "cn=claimed,dc=my,dc=domain,dc=com", Account.Type.NORMAL, otherAccountId);
        when(ldapTrustMapDaoMock.findGroupInDomain(DOMAIN_ID, "cn=claimed,dc=my,dc=domain,dc=com")).thenReturn(otherMapping);
        AccountVO otherAccount = new AccountVO();
        when(accountDaoMock.findByIdIncludingRemoved(otherAccountId)).thenReturn(otherAccount);

        assertThrows(CloudRuntimeException.class, () -> ldapManager.linkAccountToLdap(buildCmd("cn=claimed,dc=my,dc=domain,dc=com")));

        verify(ldapTrustMapDaoMock, never()).persist(any());
    }

    @Test
    public void relinkingAccountAllowsGroupOnceOtherClaimingAccountIsRemoved() {
        long removedAccountId = 99L;
        LdapTrustMapVO otherMapping = new LdapTrustMapVO(DOMAIN_ID, LdapManager.LinkType.GROUP, "cn=stale,dc=my,dc=domain,dc=com", Account.Type.NORMAL, removedAccountId);
        ReflectionTestUtils.setField(otherMapping, "id", OLD_MAPPING_ID);
        when(ldapTrustMapDaoMock.findGroupInDomain(DOMAIN_ID, "cn=stale,dc=my,dc=domain,dc=com")).thenReturn(otherMapping);
        AccountVO removedAccount = new AccountVO();
        ReflectionTestUtils.setField(removedAccount, "removed", new Date());
        when(accountDaoMock.findByIdIncludingRemoved(removedAccountId)).thenReturn(removedAccount);

        LinkAccountToLdapResponse response = ldapManager.linkAccountToLdap(buildCmd("cn=stale,dc=my,dc=domain,dc=com"));

        assertEquals("cn=stale,dc=my,dc=domain,dc=com", response.getLdapDomain());
    }

    @Test
    public void relinkingAccountDoesNotPersistWhenClearingOldMappingFails() {
        LdapTrustMapVO ownMapping = new LdapTrustMapVO(DOMAIN_ID, LdapManager.LinkType.GROUP, "cn=old,dc=my,dc=domain,dc=com", Account.Type.NORMAL, ACCOUNT_ID);
        ReflectionTestUtils.setField(ownMapping, "id", OLD_MAPPING_ID);
        when(ldapTrustMapDaoMock.findByAccount(DOMAIN_ID, ACCOUNT_ID)).thenReturn(ownMapping);
        Mockito.doThrow(new CloudRuntimeException("db blip")).when(ldapTrustMapDaoMock).expunge(Long.valueOf(OLD_MAPPING_ID));

        assertThrows(CloudRuntimeException.class, () -> ldapManager.linkAccountToLdap(buildCmd("cn=new,dc=my,dc=domain,dc=com")));

        verify(ldapTrustMapDaoMock, never()).persist(any());
    }

    private LinkAccountToLdapCmd buildCmd(String ldapDomain) {
        LinkAccountToLdapCmd cmd = new LinkAccountToLdapCmd();
        cmd.roleService = roleServiceMock;
        ReflectionTestUtils.setField(cmd, "domainId", DOMAIN_ID);
        ReflectionTestUtils.setField(cmd, "type", "GROUP");
        ReflectionTestUtils.setField(cmd, "ldapDomain", ldapDomain);
        ReflectionTestUtils.setField(cmd, "accountName", "jdoe");
        ReflectionTestUtils.setField(cmd, "accountType", Account.Type.NORMAL.ordinal());
        return cmd;
    }
}
