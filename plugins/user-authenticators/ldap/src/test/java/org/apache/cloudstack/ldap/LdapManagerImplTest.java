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

import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.AccountManager;
import org.apache.cloudstack.api.command.LdapTestConfigurationCmd;
import org.apache.cloudstack.ldap.dao.LdapConfigurationDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.naming.NamingException;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link LdapManagerImpl#testConnection} and {@link LdapManagerImpl#addConfiguration}:
 * testing a connection binds to the LDAP server (defaulting the port to 389 when omitted) but
 * never persists a configuration, whether the bind succeeds or fails; adding a configuration
 * still binds before persisting, and does not persist when the bind fails.
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapManagerImplTest {

    private static final long DOMAIN_ID = 1L;

    private LdapManagerImpl ldapManager;

    @Mock
    private LdapConfigurationDao ldapConfigurationDao;

    @Mock
    private LdapContextFactory ldapContextFactory;

    @Mock
    private DomainDao domainDao;

    @Mock
    private AccountManager accountManager;

    @Before
    public void setup() {
        ldapManager = new LdapManagerImpl(ldapConfigurationDao, ldapContextFactory, null, null);
        ReflectionTestUtils.setField(ldapManager, "domainDao", domainDao);
        ReflectionTestUtils.setField(ldapManager, "accountManager", accountManager);
    }

    @Test
    public void testConnectionDoesNotPersistOnSuccess() throws Exception {
        LdapTestConfigurationCmd cmd = buildCmd("ldap.example.com", 389, DOMAIN_ID);

        ldapManager.testConnection(cmd);

        verify(ldapContextFactory).createBindContext("ldap://ldap.example.com:389", DOMAIN_ID);
        verify(ldapConfigurationDao, never()).persist(any());
    }

    @Test
    public void testConnectionDefaultsPortWhenNotGiven() throws Exception {
        LdapTestConfigurationCmd cmd = buildCmd("ldap.example.com", 0, DOMAIN_ID);

        ldapManager.testConnection(cmd);

        verify(ldapContextFactory).createBindContext("ldap://ldap.example.com:389", DOMAIN_ID);
    }

    @Test
    public void testConnectionThrowsOnBindFailure() throws Exception {
        LdapTestConfigurationCmd cmd = buildCmd("ldap.example.com", 389, DOMAIN_ID);
        doThrow(new NamingException("bind failed")).when(ldapContextFactory).createBindContext(any(), anyLong());

        assertThrows(InvalidParameterValueException.class, () -> ldapManager.testConnection(cmd));

        verify(ldapConfigurationDao, never()).persist(any());
    }

    @Test
    public void addConfigurationStillBindsBeforePersisting() throws Exception {
        when(ldapConfigurationDao.find("ldap.example.com", 389, DOMAIN_ID)).thenReturn(null);
        when(ldapConfigurationDao.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ldapManager.addConfiguration("ldap.example.com", 389, DOMAIN_ID);

        verify(ldapContextFactory).createBindContext("ldap://ldap.example.com:389", DOMAIN_ID);
        verify(ldapConfigurationDao).persist(any());
    }

    @Test
    public void addConfigurationDoesNotPersistOnBindFailure() throws Exception {
        when(ldapConfigurationDao.find("ldap.example.com", 389, DOMAIN_ID)).thenReturn(null);
        doThrow(new NamingException("bind failed")).when(ldapContextFactory).createBindContext(any(), anyLong());

        assertThrows(InvalidParameterValueException.class, () -> ldapManager.addConfiguration("ldap.example.com", 389, DOMAIN_ID));

        verify(ldapConfigurationDao, never()).persist(any());
    }

    private LdapTestConfigurationCmd buildCmd(String hostname, int port, long domainId) {
        LdapTestConfigurationCmd cmd = new LdapTestConfigurationCmd();
        ReflectionTestUtils.setField(cmd, "hostname", hostname);
        ReflectionTestUtils.setField(cmd, "port", port);
        ReflectionTestUtils.setField(cmd, "domainId", domainId);
        return cmd;
    }
}
