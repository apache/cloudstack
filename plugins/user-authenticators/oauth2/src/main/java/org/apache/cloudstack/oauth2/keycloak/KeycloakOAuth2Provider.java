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
package org.apache.cloudstack.oauth2.keycloak;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;

import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class KeycloakOAuth2Provider extends AdapterBase implements UserOAuth2Authenticator {

    @Inject
    OauthProviderDao oauthProviderDao;

    private CloseableHttpClient httpClient;

    public KeycloakOAuth2Provider() {
        this(HttpClientBuilder.create().build());
    }

    public KeycloakOAuth2Provider(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

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

        OauthProviderVO providerVO = oauthProviderDao.findByProvider(getName());
        if (providerVO == null) {
            throw new CloudAuthenticationException("Keycloak provider is not registered, so user cannot be verified");
        }

        String verifiedEmail = verifyCodeAndFetchEmail(secretCode);
        if (StringUtils.isBlank(verifiedEmail) || !email.equals(verifiedEmail)) {
            throw new CloudRuntimeException("Unable to verify the email address with the provided secret");
        }

        return true;
    }

    @Override
    public String verifyCodeAndFetchEmail(String secretCode) {
        OauthProviderVO provider = oauthProviderDao.findByProvider(getName());

        String auth = provider.getClientId() + ":" + provider.getSecretKey();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("code", secretCode));
        params.add(new BasicNameValuePair("redirect_uri", provider.getRedirectUri()));

        HttpPost post = new HttpPost(provider.getTokenUrl());
        post.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);

        try {
            post.setEntity(new UrlEncodedFormEntity(params));
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable to generating URL parameters: " + e.getMessage());
        }

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            String body = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new CloudRuntimeException("Keycloak error during token generation: " + body);
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String idToken = json.get("id_token").getAsString();

            return validateIdTokenAndGetEmail(idToken, provider);
        } catch (IOException e) {
            throw new CloudRuntimeException("Unable to connect to Keycloak server", e);
        }
    }

    @Override
    public String getUserEmailAddress() throws CloudRuntimeException {
        return null;
    }

    private String validateIdTokenAndGetEmail(String idTokenStr, OauthProviderVO provider) {
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idTokenStr);
        JwtClaims claims = jwtConsumer.getJwtToken().getClaims();

        if (!claims.getAudiences().contains(provider.getClientId())) {
            throw new CloudAuthenticationException("Audience mismatch");
        }

        return (String) claims.getClaim("email");
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

}
