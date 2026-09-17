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
package org.apache.cloudstack.oauth2.oidc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;

import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A single provider implementation for any OIDC compliant identity provider. Unlike the per vendor
 * providers it is not bound to one registration: the registration is selected by name on every call,
 * so one bean serves any number of registrations of type "oidc".
 */
public class GenericOIDCOAuth2Provider extends AdapterBase implements UserOAuth2Authenticator {

    public static final String OIDC_PROVIDER_TYPE = "oidc";

    private static final String DISCOVERY_PATH = "/.well-known/openid-configuration";
    private static final int CLOCK_SKEW_SECONDS = 60;
    private static final long METADATA_CACHE_MINUTES = 60;
    private static final long VERIFIED_EMAIL_CACHE_SECONDS = 60;
    private static final int HTTP_TIMEOUT_MILLIS = 10000;

    @Inject
    OauthProviderDao oauthProviderDao;

    private CloseableHttpClient httpClient;

    private final Cache<String, OIDCMetadata> metadataCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(METADATA_CACHE_MINUTES, TimeUnit.MINUTES)
                    .maximumSize(64)
                    .build();

    private final Cache<String, String> verifiedEmailCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(VERIFIED_EMAIL_CACHE_SECONDS, TimeUnit.SECONDS)
                    .maximumSize(1024)
                    .build();

    public GenericOIDCOAuth2Provider() {
        this(HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(HTTP_TIMEOUT_MILLIS)
                        .setConnectionRequestTimeout(HTTP_TIMEOUT_MILLIS)
                        .setSocketTimeout(HTTP_TIMEOUT_MILLIS)
                        .build())
                .build());
    }

    public GenericOIDCOAuth2Provider(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String getName() {
        return OIDC_PROVIDER_TYPE;
    }

    @Override
    public String getDescription() {
        return "Generic OpenID Connect Provider Plugin";
    }

    @Override
    public boolean verifyUser(String email, String secretCode) {
        throw new CloudRuntimeException("The generic OIDC provider requires the registered provider name");
    }

    @Override
    public boolean verifyUser(String email, String secretCode, Long domainId) {
        throw new CloudRuntimeException("The generic OIDC provider requires the registered provider name");
    }

    @Override
    public String verifySecretCodeAndFetchEmail(String secretCode) {
        throw new CloudRuntimeException("The generic OIDC provider requires the registered provider name");
    }

    @Override
    public String verifySecretCodeAndFetchEmail(String secretCode, Long domainId) {
        throw new CloudRuntimeException("The generic OIDC provider requires the registered provider name");
    }

    @Override
    public boolean verifyUser(String email, String secretCode, Long domainId, String providerName) {
        if (StringUtils.isAnyEmpty(email, secretCode)) {
            throw new CloudAuthenticationException("Either email or secret code should not be null/empty");
        }

        String verifiedEmail = verifiedEmailCache.asMap().remove(verifiedEmailKey(providerName, secretCode));
        if (verifiedEmail == null) {
            verifiedEmail = resolveEmail(secretCode, domainId, providerName);
        }
        if (StringUtils.isBlank(verifiedEmail) || !email.equals(verifiedEmail)) {
            throw new CloudRuntimeException("Unable to verify the email address with the provided secret");
        }

        return true;
    }

    @Override
    public String verifySecretCodeAndFetchEmail(String secretCode, Long domainId, String providerName) {
        String email = resolveEmail(secretCode, domainId, providerName);
        verifiedEmailCache.put(verifiedEmailKey(providerName, secretCode), email);
        return email;
    }

    protected String resolveEmail(String secretCode, Long domainId, String providerName) {
        OauthProviderVO provider = findRegistration(providerName, domainId);
        OIDCMetadata metadata = getMetadata(provider);
        String idToken = exchangeAuthorizationCode(secretCode, provider, metadata);

        return validateAndExtractEmail(idToken, provider, metadata);
    }

    @Override
    public String getUserEmailAddress() throws CloudRuntimeException {
        return null;
    }

    private String verifiedEmailKey(String providerName, String secretCode) {
        return DigestUtils.sha256Hex(providerName + ":" + secretCode);
    }

    protected OauthProviderVO findRegistration(String providerName, Long domainId) {
        if (StringUtils.isBlank(providerName)) {
            throw new CloudAuthenticationException("The registered provider name is required");
        }
        OauthProviderVO provider = oauthProviderDao.findByProviderAndDomainWithGlobalFallback(providerName, domainId);
        if (provider == null) {
            throw new CloudAuthenticationException(String.format("%s provider is not registered, so user cannot be verified", providerName));
        }
        return provider;
    }

    protected OIDCMetadata getMetadata(OauthProviderVO provider) {
        String issuerUrl = StringUtils.trimToNull(provider.getIssuerUrl());
        if (issuerUrl == null) {
            throw new CloudRuntimeException(String.format(
                    "Provider %s has no issuer URL, so its endpoints and signing keys cannot be discovered", provider.getProvider()));
        }
        return metadataCache.get(issuerUrl, this::discover);
    }

    protected OIDCMetadata discover(String issuerUrl) {
        String document = httpGet(StringUtils.removeEnd(issuerUrl, "/") + DISCOVERY_PATH,
                String.format("Unable to read the OpenID Connect discovery document from %s", issuerUrl));

        JsonObject json = JsonParser.parseString(document).getAsJsonObject();
        String issuer = readString(json, "issuer");
        String tokenEndpoint = readString(json, "token_endpoint");
        String jwksUri = readString(json, "jwks_uri");
        if (StringUtils.isAnyBlank(issuer, tokenEndpoint, jwksUri)) {
            throw new CloudRuntimeException(String.format(
                    "The discovery document at %s is missing the issuer, the token endpoint or the JWKS URI", issuerUrl));
        }
        if (!StringUtils.removeEnd(issuer, "/").equals(StringUtils.removeEnd(issuerUrl, "/"))) {
            throw new CloudRuntimeException(String.format("The discovery document at %s names a different issuer: %s", issuerUrl, issuer));
        }

        return new OIDCMetadata(issuer, readString(json, "authorization_endpoint"), tokenEndpoint, jwksUri);
    }

    protected String exchangeAuthorizationCode(String secretCode, OauthProviderVO provider, OIDCMetadata metadata) {
        String auth = provider.getClientId() + ":" + provider.getSecretKey();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("code", secretCode));
        params.add(new BasicNameValuePair("redirect_uri", provider.getRedirectUri()));

        HttpPost post = new HttpPost(metadata.getTokenEndpoint());
        post.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
        try {
            post.setEntity(new UrlEncodedFormEntity(params));
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable to generate URL parameters: " + e.getMessage());
        }

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            String body = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new CloudRuntimeException(String.format("%s error during token generation: %s", provider.getProvider(), body));
            }

            JsonElement fetchedIdToken = JsonParser.parseString(body).getAsJsonObject().get("id_token");
            if (fetchedIdToken == null) {
                throw new CloudRuntimeException("No id_token found in token");
            }
            return fetchedIdToken.getAsString();
        } catch (IOException e) {
            throw new CloudRuntimeException(String.format("Unable to connect to the %s token endpoint", provider.getProvider()), e);
        }
    }

    protected String validateAndExtractEmail(String idToken, OauthProviderVO provider, OIDCMetadata metadata) {
        JwsJwtCompactConsumer consumer = new JwsJwtCompactConsumer(idToken);

        verifySignature(consumer, metadata, provider);

        JwtClaims claims = consumer.getJwtClaims();
        if (!metadata.getIssuer().equals(claims.getIssuer())) {
            throw new CloudAuthenticationException("Issuer mismatch");
        }
        if (!claims.getAudiences().contains(provider.getClientId())) {
            throw new CloudAuthenticationException("Audience mismatch");
        }
        JwtUtils.validateJwtExpiry(claims, CLOCK_SKEW_SECONDS, true);

        String email = (String) claims.getClaim("email");
        if (StringUtils.isBlank(email)) {
            throw new CloudAuthenticationException("The id_token carries no email claim");
        }
        return email;
    }

    protected void verifySignature(JwsJwtCompactConsumer consumer, OIDCMetadata metadata, OauthProviderVO provider) {
        if (StringUtils.isBlank(metadata.getJwksUri())) {
            throw new CloudAuthenticationException(String.format(
                    "Provider %s has no JWKS endpoint, so the id_token signature cannot be verified", provider.getProvider()));
        }

        JsonWebKeys keys = readJwkSet(metadata.getJwksUri());
        String keyId = consumer.getJwsHeaders().getKeyId();

        JsonWebKey key = StringUtils.isNotBlank(keyId) ? keys.getKey(keyId) : singleKey(keys);
        if (key == null) {
            throw new CloudAuthenticationException("No matching signing key was published by the identity provider");
        }
        if (!consumer.verifySignatureWith(key)) {
            throw new CloudAuthenticationException("The id_token signature is not valid");
        }
    }

    protected JsonWebKeys readJwkSet(String jwksUri) {
        return JwkUtils.readJwkSet(httpGet(jwksUri, String.format("Unable to read the signing keys from %s", jwksUri)));
    }

    private JsonWebKey singleKey(JsonWebKeys keys) {
        List<JsonWebKey> published = keys.getKeys();
        return published != null && published.size() == 1 ? published.get(0) : null;
    }

    protected String httpGet(String url, String failureMessage) {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
            String body = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new CloudRuntimeException(String.format("%s: %s", failureMessage, body));
            }
            return body;
        } catch (IOException e) {
            throw new CloudRuntimeException(failureMessage, e);
        }
    }

    private String readString(JsonObject json, String member) {
        JsonElement element = json.get(member);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    protected static class OIDCMetadata {
        private final String issuer;
        private final String authorizationEndpoint;
        private final String tokenEndpoint;
        private final String jwksUri;

        protected OIDCMetadata(String issuer, String authorizationEndpoint, String tokenEndpoint, String jwksUri) {
            this.issuer = issuer;
            this.authorizationEndpoint = authorizationEndpoint;
            this.tokenEndpoint = tokenEndpoint;
            this.jwksUri = jwksUri;
        }

        public String getIssuer() {
            return issuer;
        }

        public String getAuthorizationEndpoint() {
            return authorizationEndpoint;
        }

        public String getTokenEndpoint() {
            return tokenEndpoint;
        }

        public String getJwksUri() {
            return jwksUri;
        }
    }
}
