/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.oauth2.api.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.oauth2.OAuth2AuthManager;
import org.apache.cloudstack.oauth2.api.response.OauthProviderResponse;
import org.apache.cloudstack.oauth2.oidc.GenericOIDCOAuth2Provider;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class RegisterOAuthProviderCmdTest {

    private OAuth2AuthManager _oauth2mgr;
    private RegisterOAuthProviderCmd _cmd;

    @Before
    public void setUp() {
        _oauth2mgr = mock(OAuth2AuthManager.class);
        _cmd = new RegisterOAuthProviderCmd();
        _cmd._oauth2mgr = _oauth2mgr;
    }

    @Test
    public void testExecute() throws ServerApiException {
        OauthProviderVO provider = mock(OauthProviderVO.class);
        when(provider.getDomainId()).thenReturn(null);
        when(provider.getUuid()).thenReturn("test-uuid");
        when(provider.getProvider()).thenReturn("github");
        when(provider.getDescription()).thenReturn("test");
        when(provider.getClientId()).thenReturn("client-id");
        when(provider.getSecretKey()).thenReturn("secret-key");
        when(provider.getRedirectUri()).thenReturn("http://localhost");
        when(provider.isEnabled()).thenReturn(true);
        when(_oauth2mgr.registerOauthProvider(any(RegisterOAuthProviderCmd.class))).thenReturn(provider);
        UserOAuth2Authenticator authenticator = mock(UserOAuth2Authenticator.class);
        when(authenticator.getName()).thenReturn("github");
        when(_oauth2mgr.listUserOAuth2AuthenticationProviders()).thenReturn(Collections.singletonList(authenticator));

        _cmd.execute();
        assertEquals(ApiConstants.OAUTH_PROVIDER, ((OauthProviderResponse)_cmd.getResponseObject()).getObjectName());
    }

    @Test
    public void testExecuteSetsDisabledProviderResponseWhenProviderIsDisabled() throws ServerApiException {
        OauthProviderVO provider = mock(OauthProviderVO.class);
        when(provider.getDomainId()).thenReturn(null);
        when(provider.getUuid()).thenReturn("test-uuid");
        when(provider.getProvider()).thenReturn("github");
        when(provider.getDescription()).thenReturn("test");
        when(provider.getClientId()).thenReturn("client-id");
        when(provider.getSecretKey()).thenReturn("secret-key");
        when(provider.getRedirectUri()).thenReturn("http://localhost");
        when(provider.isEnabled()).thenReturn(false);
        when(_oauth2mgr.registerOauthProvider(any(RegisterOAuthProviderCmd.class))).thenReturn(provider);
        UserOAuth2Authenticator authenticator = mock(UserOAuth2Authenticator.class);
        when(authenticator.getName()).thenReturn("github");
        when(_oauth2mgr.listUserOAuth2AuthenticationProviders()).thenReturn(Collections.singletonList(authenticator));

        _cmd.execute();
        OauthProviderResponse response = (OauthProviderResponse)_cmd.getResponseObject();
        assertEquals(ApiConstants.OAUTH_PROVIDER, response.getObjectName());
        assertFalse(response.getEnabled());
    }

    @Test
    public void testOidcProviderIsReportedEnabledByType() throws Exception {
        FieldUtils.writeField(_cmd, "type", GenericOIDCOAuth2Provider.OIDC_PROVIDER_TYPE, true);
        FieldUtils.writeField(_cmd, "issuerUrl", "https://idp.example.com", true);

        OauthProviderVO provider = mock(OauthProviderVO.class);
        when(provider.getDomainId()).thenReturn(null);
        when(provider.getUuid()).thenReturn("test-uuid");
        when(provider.getProvider()).thenReturn("mycorp");
        when(provider.getType()).thenReturn(GenericOIDCOAuth2Provider.OIDC_PROVIDER_TYPE);
        when(provider.getDescription()).thenReturn("test");
        when(provider.getClientId()).thenReturn("client-id");
        when(provider.getSecretKey()).thenReturn("secret-key");
        when(provider.getRedirectUri()).thenReturn("http://localhost");
        when(provider.isEnabled()).thenReturn(true);
        when(_oauth2mgr.registerOauthProvider(any(RegisterOAuthProviderCmd.class))).thenReturn(provider);
        UserOAuth2Authenticator authenticator = mock(UserOAuth2Authenticator.class);
        when(authenticator.getName()).thenReturn(GenericOIDCOAuth2Provider.OIDC_PROVIDER_TYPE);
        when(_oauth2mgr.listUserOAuth2AuthenticationProviders()).thenReturn(Collections.singletonList(authenticator));

        try (MockedStatic<OAuth2AuthManager> mocked = Mockito.mockStatic(OAuth2AuthManager.class)) {
            mocked.when(() -> OAuth2AuthManager.isPluginEnabledForDomain(null)).thenReturn(true);
            _cmd.execute();
        }

        OauthProviderResponse response = (OauthProviderResponse) _cmd.getResponseObject();
        assertEquals(ApiConstants.OAUTH_PROVIDER, response.getObjectName());
        assertTrue(response.getEnabled());
    }
}
