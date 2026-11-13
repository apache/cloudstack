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
package org.apache.cloudstack.api.command;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link LdapTestConfigurationCmd}: a successful bind yields a success response, a failed
 * bind propagates as an error rather than a response reporting success=false.
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapTestConfigurationCmdTest {

    @Mock
    private LdapManager ldapManager;

    private LdapTestConfigurationCmd cmd;

    @Before
    public void setUp() {
        cmd = new LdapTestConfigurationCmd(ldapManager);
        ReflectionTestUtils.setField(cmd, "hostname", "ldap.example.com");
        ReflectionTestUtils.setField(cmd, "port", 389);
        ReflectionTestUtils.setField(cmd, "domainId", 1L);
    }

    @Test
    public void executeReturnsSuccessResponseWhenBindSucceeds() {
        cmd.execute();

        verify(ldapManager).testConnection(cmd);
        SuccessResponse response = (SuccessResponse) cmd.getResponseObject();
        assertTrue(response.getSuccess());
        assertEquals("ldaptestconfigurationresponse", response.getResponseName());
        assertEquals("Successfully connected to the LDAP server", response.getDisplayText());
    }

    @Test
    public void executePropagatesBindFailure() {
        doThrow(new InvalidParameterValueException("Unable to bind to the given LDAP server")).when(ldapManager).testConnection(cmd);

        InvalidParameterValueException e = assertThrows(InvalidParameterValueException.class, () -> cmd.execute());

        assertEquals("Unable to bind to the given LDAP server", e.getMessage());
        assertNull(cmd.getResponseObject());
    }

    @Test
    public void gettersExposeParameters() {
        assertEquals("ldap.example.com", cmd.getHostname());
        assertEquals(389, cmd.getPort());
        assertEquals(Long.valueOf(1L), cmd.getDomainId());
        assertEquals(Account.ACCOUNT_ID_SYSTEM, cmd.getEntityOwnerId());
    }
}
