//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.oauth2.google;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GoogleOAuth2Provider extends AdapterBase implements UserOAuth2Authenticator {

    protected String accessToken = null;
    protected String refreshToken = null;

    @Inject
    OauthProviderDao _oauthProviderDao;

    @Override
    public String getName() {
        return "google";
    }

    @Override
    public String getDescription() {
        return "Google OAuth2 Provider Plugin";
    }

    @Override
    public boolean verifyUser(String email, String secretCode) {
        if (StringUtils.isAnyEmpty(email, secretCode)) {
            throw new CloudAuthenticationException("Either email or secret code should not be null/empty");
        }

        OauthProviderVO providerVO = _oauthProviderDao.findByProvider(getName());
        if (providerVO == null) {
            throw new CloudAuthenticationException("Google provider is not registered, so user cannot be verified");
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
        OauthProviderVO githubProvider = _oauthProviderDao.findByProvider(getName());
        String clientId = githubProvider.getClientId();
        String secret = githubProvider.getSecretKey();
        String redirectURI = githubProvider.getRedirectUri();
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setWeb(new GoogleClientSecrets.Details()
                        .setClientId(clientId)
                        .setClientSecret(secret));

        NetHttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        List<String> scopes = Arrays.asList(
                                "https://www.googleapis.com/auth/userinfo.profile",
                                "https://www.googleapis.com/auth/userinfo.email");
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, clientSecrets, scopes)
                .build();

        if (StringUtils.isAnyEmpty(accessToken, refreshToken)) {
            GoogleTokenResponse tokenResponse = null;
            try {
                tokenResponse = flow.newTokenRequest(secretCode)
                        .setRedirectUri(redirectURI)
                        .execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            accessToken = tokenResponse.getAccessToken();
            refreshToken = tokenResponse.getRefreshToken();
        }

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setClientSecrets(clientSecrets)
                .build()
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken);

        Oauth2 oauth2 = new Oauth2.Builder(httpTransport, jsonFactory, credential).build();
        Userinfo userinfo = null;
        try {
            userinfo = oauth2.userinfo().get().execute();
        } catch (IOException e) {
            throw new CloudRuntimeException(String.format("Failed to fetch the email address with the provided secret: %s" + e.getMessage()));
        }
        return userinfo.getEmail();
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
