<<<<<<< HEAD
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

=======
>>>>>>> 9e53596ba92eaec1289e97bfc9f441cc3c507002
package org.apache.cloudstack.oauth2.keycloak;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
<<<<<<< HEAD
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.AccessTokenResponse;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
=======
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.AccessTokenResponse;

import javax.inject.Inject;
import java.util.List;
>>>>>>> 9e53596ba92eaec1289e97bfc9f441cc3c507002

public class KeycloakOAuth2Provider extends AdapterBase implements UserOAuth2Authenticator {

    protected String accessToken = null;
    protected String refreshToken = null;

    @Inject
    OauthProviderDao _oauthProviderDao;

    @Override
    public String getName() {
        return "keycloak";
    }

    @Override
    public String getDescription() {
        return "Keycloak OAuth2 Provider Plugin";
    }

    @Override
    public boolean verifyUser(String email, String secretCode) {
        if (StringUtils.isAnyEmpty(email, secretCode)) {
            throw new CloudAuthenticationException("Either email or secret code should not be null/empty");
        }

        OauthProviderVO providerVO = _oauthProviderDao.findByProvider(getName());
        if (providerVO == null) {
            throw new CloudAuthenticationException("Keycloak provider is not registered, so user cannot be verified");
        }

        String verifiedEmail = verifyCodeAndFetchEmail(secretCode);
        if (verifiedEmail == null || !email.equals(verifiedEmail)) {
            throw new CloudRuntimeException("Unable to verify the email address with the provided secret");
        }
        clearAccessAndRefreshTokens();

        return true;
    }

    @Override
    public String verifyCodeAndFetchEmail(String secretCode) {
        OauthProviderVO keycloakProvider = _oauthProviderDao.findByProvider(getName());
        String clientId = keycloakProvider.getClientId();
<<<<<<< HEAD
        String secret = keycloakProvider.getSecretKey();
        String authServerUrl = keycloakProvider.getAuthServerUrl();
        String realm = keycloakProvider.getRealm();

        Keycloak keycloak = Keycloak.getInstance(authServerUrl, realm, clientId, secret, OAuth2Constants.CLIENT_CREDENTIALS);
=======
        String clientSecret = keycloakProvider.getSecretKey();
        String redirectUri = keycloakProvider.getRedirectUri();
        String authServerUrl = keycloakProvider.getAuthenticationUri();

        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(keycloakProvider.getProviderName())
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .code(secretCode)
                .build();

>>>>>>> 9e53596ba92eaec1289e97bfc9f441cc3c507002
        AccessTokenResponse tokenResponse = keycloak.tokenManager().getAccessToken();

        accessToken = tokenResponse.getToken();
        refreshToken = tokenResponse.getRefreshToken();

<<<<<<< HEAD
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(tokenResponse.getSubject());

        Map<String, Object> attributes = new HashMap<>();
        try {
            attributes = userResource.toRepresentation().getAttributes();
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Failed to fetch the email address with the provided secret: %s", e.getMessage()));
        }

        return (String) attributes.get("email");
=======
        List<UserRepresentation> users = keycloak.realm(keycloakProvider.getProviderName()).users().search("", 0, 1);
        if (users.isEmpty()) {
            throw new CloudRuntimeException("No user found with the provided secret");
        }

        return users.get(0).getEmail();
>>>>>>> 9e53596ba92eaec1289e97bfc9f441cc3c507002
    }

    protected void clearAccessAndRefreshTokens() {
        accessToken = null;
        refreshToken = null;
    }

    @Override
    public String getUserEmailAddress() throws CloudRuntimeException {
        return null;
    }
}
<<<<<<< HEAD
=======

>>>>>>> 9e53596ba92eaec1289e97bfc9f441cc3c507002
