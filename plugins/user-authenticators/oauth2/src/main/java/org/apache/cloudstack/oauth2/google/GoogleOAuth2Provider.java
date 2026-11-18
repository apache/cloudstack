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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

public class GoogleOAuth2Provider extends AdapterBase implements UserOAuth2Authenticator {

    protected String accessToken = null;
    protected String refreshToken = null;

    @Inject
    OauthProviderDao _oauthProviderDao;

    private final Cache<String, String> validatedEmailCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(60, TimeUnit.SECONDS)
                    .maximumSize(1024)
                    .build();

    private String getCacheKey(final String secretCode) {
        return DigestUtils.sha256Hex(secretCode);
    }

    private void addValidatedEmailToCache(final String secretCode, final String email) {
        validatedEmailCache.put(getCacheKey(secretCode), email);
    }

    private String consumeValidatedEmailFromCache(final String secretCode) {
        final String key = getCacheKey(secretCode);
        final String email = validatedEmailCache.getIfPresent(key);

        if (email != null) {
            validatedEmailCache.invalidate(key);
        }

        return email;
    }

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
        return verifyUser(email, secretCode, null);
    }

    @Override
    public String verifySecretCodeAndFetchEmail(String secretCode) {
        return verifySecretCodeAndFetchEmail(secretCode, null);
    }

    @Override
    public String verifySecretCodeAndFetchEmail(String secretCode, Long domainId) {
        return verifyCodeAndFetchEmailInternal(secretCode, domainId, null);
    }

    protected void clearAccessAndRefreshTokens() {
        accessToken = null;
        refreshToken = null;
    }

    @Override
    public String getUserEmailAddress() throws CloudRuntimeException {
        return null;
    }

    @Override
    public boolean verifyUser(String email, String secretCode, Long domainId) {
        if (StringUtils.isAnyEmpty(email, secretCode)) {
            throw new CloudAuthenticationException("Either email or secret code should not be null/empty");
        }

        OauthProviderVO providerVO = _oauthProviderDao.findByProviderAndDomainWithGlobalFallback(getName(), domainId);
        if (providerVO == null) {
            throw new CloudAuthenticationException("Google provider is not registered, so user cannot be verified");
        }

        String verifiedEmail = consumeValidatedEmailFromCache(secretCode);
        if (StringUtils.isEmpty(verifiedEmail)) {
            verifiedEmail = verifyCodeAndFetchEmailInternal(secretCode, domainId, providerVO);
        }
        if (verifiedEmail == null || !email.equals(verifiedEmail)) {
            throw new CloudRuntimeException("Unable to verify the email address with the provided secret");
        }

        return true;
    }

    protected String verifyCodeAndFetchEmailInternal(String secretCode, Long domainId, OauthProviderVO googleProvider) {
        if (googleProvider == null) {
            googleProvider = _oauthProviderDao.findByProviderAndDomainWithGlobalFallback(getName(), domainId);
        }
        String clientId = googleProvider.getClientId();
        String secret = googleProvider.getSecretKey();
        String redirectURI = googleProvider.getRedirectUri();
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

        GoogleTokenResponse tokenResponse;
        try {
            tokenResponse = flow.newTokenRequest(secretCode)
                    .setRedirectUri(redirectURI)
                    .execute();
        } catch (IOException e) {
            throw new CloudRuntimeException("Failed to verify secret code", e);
        }

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setTokenServerEncodedUrl("https://oauth2.googleapis.com/token")
                .setClientAuthentication(new ClientParametersAuthentication(clientId, secret))
                .build()
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken);

        Oauth2 oauth2 = new Oauth2.Builder(httpTransport, jsonFactory, credential).build();
        Userinfo userinfo;
        try {
            userinfo = oauth2.userinfo().get().execute();
        } catch (IOException e) {
            throw new CloudRuntimeException(String.format("Failed to fetch the email address with the provided secret: %s", e.getMessage()));
        }
        String verifiedEmail = userinfo.getEmail();
        addValidatedEmailToCache(secretCode, verifiedEmail);
        return verifiedEmail;
    }
}
