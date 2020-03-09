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
package org.apache.cloudstack.saml;

import com.cloud.user.DomainManager;
import com.cloud.user.dao.UserDao;
import org.apache.cloudstack.framework.security.keystore.KeystoreDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class SAML2AuthManagerImplTest {

    @Mock
    private KeystoreDao _ksDao;

    @Mock
    private SAMLTokenDao _samlTokenDao;

    @Mock
    private UserDao _userDao;

    @Mock
    DomainManager _domainMgr;

    @InjectMocks
    @Spy
    SAML2AuthManagerImpl saml2AuthManager = new SAML2AuthManagerImpl();

    @Before
    public void setUp() {
        doReturn(true).when(saml2AuthManager).isSAMLPluginEnabled();
        doReturn(true).when(saml2AuthManager).initSP();
    }

    @Test
    public void testStart() {
        when(saml2AuthManager.getSAMLIdentityProviderMetadataURL()).thenReturn("file://does/not/exist");
        boolean started = saml2AuthManager.start();
        assertFalse("saml2authmanager should not start as the file doesnt exist", started);

        when(saml2AuthManager.getSAMLIdentityProviderMetadataURL()).thenReturn(" ");
        started = saml2AuthManager.start();
        assertFalse("saml2authmanager should not start as the file doesnt exist", started);

        when(saml2AuthManager.getSAMLIdentityProviderMetadataURL()).thenReturn("");
        started = saml2AuthManager.start();
        assertFalse("saml2authmanager should not start as the file doesnt exist", started);

    }
}
