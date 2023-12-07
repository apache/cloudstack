//
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
//

package org.apache.cloudstack.oauth2;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.oauth2.api.command.DeleteOAuthProviderCmd;
import org.apache.cloudstack.oauth2.api.command.RegisterOAuthProviderCmd;
import org.apache.cloudstack.oauth2.api.command.UpdateOAuthProviderCmd;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class OAuth2AuthManagerImplTest {

    @Spy
    @InjectMocks
    private OAuth2AuthManagerImpl _authManager;

    @Mock
    OauthProviderDao _oauthProviderDao;

    AutoCloseable closeable;
    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testRegisterOauthProvider() {
        when(_authManager.isOAuthPluginEnabled()).thenReturn(false);
        RegisterOAuthProviderCmd cmd = Mockito.mock(RegisterOAuthProviderCmd.class);
        try {
            _authManager.registerOauthProvider(cmd);
            Assert.fail("Expected CloudRuntimeException was not thrown");
        } catch (CloudRuntimeException e) {
            assertEquals("OAuth is not enabled, please enable to register", e.getMessage());
        }

        // Test when provider is already registered
        when(_authManager.isOAuthPluginEnabled()).thenReturn(true);
        OauthProviderVO providerVO = new OauthProviderVO();
        providerVO.setProvider("testProvider");
        when(_authManager._oauthProviderDao.findByProvider(Mockito.anyString())).thenReturn(providerVO);
        when(cmd.getProvider()).thenReturn("testProvider");

        try {
            _authManager.registerOauthProvider(cmd);
            Assert.fail("Expected CloudRuntimeException was not thrown");
        } catch (CloudRuntimeException e) {
            assertEquals("Provider with the name testProvider is already registered", e.getMessage());
        }

        // Test when provider is github and secret key is not null
        when(cmd.getSecretKey()).thenReturn("testSecretKey");
        providerVO = null;
        when(_authManager._oauthProviderDao.findByProvider(Mockito.anyString())).thenReturn(providerVO);
        OauthProviderVO savedProviderVO = new OauthProviderVO();
        when(cmd.getProvider()).thenReturn("github");
        when(_authManager._oauthProviderDao.persist(Mockito.any(OauthProviderVO.class))).thenReturn(savedProviderVO);
        OauthProviderVO result = _authManager.registerOauthProvider(cmd);
        assertEquals("github", result.getProvider());
        assertEquals("testSecretKey", result.getSecretKey());
    }

    @Test
    public void testUpdateOauthProvider() {
        Long id = 1L;
        String description = "updated description";
        String clientId = "updated client id";
        String redirectUri = "updated redirect uri";
        String secretKey = "updated secret key";

        UpdateOAuthProviderCmd cmd = Mockito.mock(UpdateOAuthProviderCmd.class);
        when(cmd.getId()).thenReturn(id);
        when(cmd.getDescription()).thenReturn(description);
        when(cmd.getClientId()).thenReturn(clientId);
        when(cmd.getRedirectUri()).thenReturn(redirectUri);
        when(cmd.getSecretKey()).thenReturn(secretKey);

        OauthProviderVO providerVO = new OauthProviderVO();
        providerVO.setDescription("old description");
        providerVO.setClientId("old client id");
        providerVO.setRedirectUri("old redirect uri");
        providerVO.setSecretKey("old secret key");

        when(_oauthProviderDao.findById(id)).thenReturn(providerVO);

        OauthProviderVO updatedProviderVO = new OauthProviderVO();
        updatedProviderVO.setDescription(description);
        updatedProviderVO.setClientId(clientId);
        updatedProviderVO.setRedirectUri(redirectUri);
        updatedProviderVO.setSecretKey(secretKey);

        when(_oauthProviderDao.update(id, providerVO)).thenReturn(true);

        OauthProviderVO result = _authManager.updateOauthProvider(cmd);

        assertEquals(description, result.getDescription());
        assertEquals(clientId, result.getClientId());
        assertEquals(redirectUri, result.getRedirectUri());
        assertEquals(secretKey, result.getSecretKey());
    }

    @Test
    public void testListOauthProviders() {
        String uuid = "1234-5678-9101";
        String provider = "testProvider";
        OauthProviderVO providerVO = new OauthProviderVO();
        providerVO.setProvider(provider);
        List<OauthProviderVO> providerList = Collections.singletonList(providerVO);

        // Test when uuid is not null
        when(_oauthProviderDao.findByUuid(uuid)).thenReturn(providerVO);
        List<OauthProviderVO> result = _authManager.listOauthProviders(null, uuid);
        assertEquals(providerList, result);

        // Test when provider is not blank
        when(_oauthProviderDao.findByProvider(provider)).thenReturn(providerVO);
        result = _authManager.listOauthProviders(provider, null);
        assertEquals(providerList, result);

        // Test when both uuid and provider are null
        when(_oauthProviderDao.listAll()).thenReturn(providerList);
        result = _authManager.listOauthProviders(null, null);
        assertEquals(providerList, result);
    }

    @Test
    public void testGetCommands() {
        List<Class<?>> expectedCmdList = new ArrayList<>();
        expectedCmdList.add(RegisterOAuthProviderCmd.class);
        expectedCmdList.add(DeleteOAuthProviderCmd.class);
        expectedCmdList.add(UpdateOAuthProviderCmd.class);

        List<Class<?>> cmdList = _authManager.getCommands();

        assertEquals(expectedCmdList, cmdList);
    }

    @Test
    public void testStart() {
        when(_authManager.isOAuthPluginEnabled()).thenReturn(true);
        doNothing().when(_authManager).initializeUserOAuth2AuthenticationProvidersMap();
        boolean result = _authManager.start();
        assertTrue(result);

        when(_authManager.isOAuthPluginEnabled()).thenReturn(false);
        result = _authManager.start();
        assertTrue(result);
    }

}
