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
import org.apache.cloudstack.api.command.LinkDomainToLdapCmd;
import org.apache.cloudstack.api.response.LinkDomainToLdapResponse;
import org.apache.cloudstack.ldap.dao.LdapTrustMapDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
 * Regression tests: re-linking a domain to LDAP must replace its existing mapping
 * instead of failing on the domain_id/account_id unique key, must not silently steal
 * a group still claimed by a live account, and must not persist a new mapping if
 * clearing the old one fails.
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapManagerImplTest {

    private static final long DOMAIN_ID = 1L;
    private static final long OLD_MAPPING_ID = 5L;

    private LdapManagerImpl ldapManager;

    @Mock
    private LdapTrustMapDao ldapTrustMapDaoMock;

    @Mock
    private LdapConfiguration ldapConfigurationMock;

    @Mock
    private DomainDao domainDaoMock;

    @Mock
    private AccountDao accountDaoMock;

    @Before
    public void setup() {
        ldapManager = new LdapManagerImpl();
        ldapManager._ldapTrustMapDao = ldapTrustMapDaoMock;
        ReflectionTestUtils.setField(ldapManager, "_ldapConfiguration", ldapConfigurationMock);
        ReflectionTestUtils.setField(ldapManager, "domainDao", domainDaoMock);
        ReflectionTestUtils.setField(ldapManager, "accountDao", accountDaoMock);
        when(ldapConfigurationMock.getBaseDn(DOMAIN_ID)).thenReturn("dc=my,dc=domain,dc=com");
        when(domainDaoMock.findById(DOMAIN_ID)).thenReturn(new DomainVO());
    }

    @Test
    public void relinkingDomainReplacesExistingMapping() {
        LdapTrustMapVO oldMapping = new LdapTrustMapVO(DOMAIN_ID, LdapManager.LinkType.GROUP, "cn=old,dc=my,dc=domain,dc=com", Account.Type.NORMAL, 0);
        ReflectionTestUtils.setField(oldMapping, "id", OLD_MAPPING_ID);
        when(ldapTrustMapDaoMock.findByDomainId(DOMAIN_ID)).thenReturn(oldMapping);
        when(ldapTrustMapDaoMock.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LinkDomainToLdapResponse response = ldapManager.linkDomainToLdap(buildCmd("cn=new,dc=my,dc=domain,dc=com"));

        verify(ldapTrustMapDaoMock, times(1)).expunge(Long.valueOf(OLD_MAPPING_ID));
        assertEquals("cn=new,dc=my,dc=domain,dc=com", response.getLdapDomain());
    }

    @Test
    public void firstLinkOfDomainDoesNotExpungeAnything() {
        when(ldapTrustMapDaoMock.findByDomainId(DOMAIN_ID)).thenReturn(null);
        when(ldapTrustMapDaoMock.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ldapManager.linkDomainToLdap(buildCmd("cn=first,dc=my,dc=domain,dc=com"));

        verify(ldapTrustMapDaoMock, never()).expunge(any(Long.class));
    }

    @Test
    public void linkingDomainRefusesGroupClaimedByLiveAccount() {
        long liveAccountId = 42L;
        LdapTrustMapVO accountMapping = new LdapTrustMapVO(DOMAIN_ID, LdapManager.LinkType.GROUP, "cn=claimed,dc=my,dc=domain,dc=com", Account.Type.NORMAL, liveAccountId);
        when(ldapTrustMapDaoMock.findGroupInDomain(DOMAIN_ID, "cn=claimed,dc=my,dc=domain,dc=com")).thenReturn(accountMapping);
        AccountVO liveAccount = new AccountVO();
        when(accountDaoMock.findByIdIncludingRemoved(liveAccountId)).thenReturn(liveAccount);

        assertThrows(CloudRuntimeException.class, () -> ldapManager.linkDomainToLdap(buildCmd("cn=claimed,dc=my,dc=domain,dc=com")));

        verify(ldapTrustMapDaoMock, never()).persist(any());
    }

    @Test
    public void linkingDomainAllowsGroupOnceClaimingAccountIsRemoved() {
        long removedAccountId = 42L;
        LdapTrustMapVO accountMapping = new LdapTrustMapVO(DOMAIN_ID, LdapManager.LinkType.GROUP, "cn=stale,dc=my,dc=domain,dc=com", Account.Type.NORMAL, removedAccountId);
        when(ldapTrustMapDaoMock.findGroupInDomain(DOMAIN_ID, "cn=stale,dc=my,dc=domain,dc=com")).thenReturn(accountMapping);
        AccountVO removedAccount = new AccountVO();
        ReflectionTestUtils.setField(removedAccount, "removed", new Date());
        when(accountDaoMock.findByIdIncludingRemoved(removedAccountId)).thenReturn(removedAccount);
        when(ldapTrustMapDaoMock.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LinkDomainToLdapResponse response = ldapManager.linkDomainToLdap(buildCmd("cn=stale,dc=my,dc=domain,dc=com"));

        assertEquals("cn=stale,dc=my,dc=domain,dc=com", response.getLdapDomain());
    }

    @Test
    public void linkingDomainDoesNotPersistWhenClearingOldMappingFails() {
        LdapTrustMapVO oldMapping = new LdapTrustMapVO(DOMAIN_ID, LdapManager.LinkType.GROUP, "cn=old,dc=my,dc=domain,dc=com", Account.Type.NORMAL, 0);
        ReflectionTestUtils.setField(oldMapping, "id", OLD_MAPPING_ID);
        when(ldapTrustMapDaoMock.findByDomainId(DOMAIN_ID)).thenReturn(oldMapping);
        Mockito.doThrow(new CloudRuntimeException("db blip")).when(ldapTrustMapDaoMock).expunge(Long.valueOf(OLD_MAPPING_ID));

        assertThrows(CloudRuntimeException.class, () -> ldapManager.linkDomainToLdap(buildCmd("cn=new,dc=my,dc=domain,dc=com")));

        verify(ldapTrustMapDaoMock, never()).persist(any());
    }

    private LinkDomainToLdapCmd buildCmd(String ldapDomain) {
        LinkDomainToLdapCmd cmd = new LinkDomainToLdapCmd();
        ReflectionTestUtils.setField(cmd, "domainId", DOMAIN_ID);
        ReflectionTestUtils.setField(cmd, "type", "GROUP");
        ReflectionTestUtils.setField(cmd, "ldapDomain", ldapDomain);
        ReflectionTestUtils.setField(cmd, "accountType", Account.Type.NORMAL.ordinal());
        return cmd;
    }
}
