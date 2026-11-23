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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;

import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.oauth2.OAuth2AuthManager;
import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static final long VERIFIED_EMAIL_CACHE_SECONDS = 300;
    private static final int HTTP_TIMEOUT_MILLIS = 10000;
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

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

    private final Cache<String, JsonWebKeys> jwksCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(METADATA_CACHE_MINUTES, TimeUnit.MINUTES)
                    .maximumSize(64)
                    .build();

    protected Logger logger = LogManager.getLogger(getClass());

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

        OauthProviderVO registration = findRegistration(providerName, domainId);
        String verifiedEmail = verifiedEmailCache.asMap().remove(verifiedEmailKey(registration, secretCode));
        if (verifiedEmail == null) {
            verifiedEmail = resolveEmail(registration, secretCode);
        }
        if (StringUtils.isBlank(verifiedEmail) || !email.equals(verifiedEmail)) {
            throw new CloudRuntimeException("Unable to verify the email address with the provided secret");
        }

        return true;
    }

    @Override
    public String verifySecretCodeAndFetchEmail(String secretCode, Long domainId, String providerName) {
        OauthProviderVO registration = findRegistration(providerName, domainId);
        String email = resolveEmail(registration, secretCode);
        verifiedEmailCache.put(verifiedEmailKey(registration, secretCode), email);
        return email;
    }

    protected String resolveEmail(OauthProviderVO registration, String secretCode) {
        OIDCMetadata metadata = getMetadata(registration);
        String idToken = exchangeAuthorizationCode(secretCode, registration, metadata);

        return validateAndExtractEmail(idToken, registration, metadata);
    }

    /**
     * Validate an issuer URL at provider registration: confirm it uses https (or loopback), that its
     * discovery document is reachable, and that the document names the same issuer, so a bad issuer fails
     * at registration instead of silently at the first login.
     */
    public void validateIssuer(String issuerUrl) {
        if (StringUtils.isBlank(issuerUrl)) {
            throw new CloudRuntimeException("An issuer URL is required for an oidc provider");
        }
        discover(StringUtils.trim(issuerUrl));
    }

    @Override
    public String getUserEmailAddress() throws CloudRuntimeException {
        return null;
    }

    private String verifiedEmailKey(OauthProviderVO registration, String secretCode) {
        return DigestUtils.sha256Hex(registration.getUuid() + ":" + secretCode);
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
        requireHttps(issuerUrl, "issuer URL");
        String document = httpGet(StringUtils.removeEnd(issuerUrl, "/") + DISCOVERY_PATH,
                String.format("Unable to read the OpenID Connect discovery document from %s", issuerUrl));

        JsonObject json = asJsonObject(document,
                String.format("The discovery document at %s is not a JSON object", issuerUrl));
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
        requireHttps(tokenEndpoint, "token endpoint");
        requireHttps(jwksUri, "JWKS URI");

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
            String body = readBody(response);
            if (response.getStatusLine().getStatusCode() != 200) {
                logger.warn("{} rejected the token exchange with HTTP {}", provider.getProvider(),
                        response.getStatusLine().getStatusCode());
                throw new CloudRuntimeException(String.format("The %s identity provider rejected the token exchange", provider.getProvider()));
            }

            JsonObject json = asJsonObject(body, "The token endpoint response is not a JSON object");
            JsonElement fetchedIdToken = json.get("id_token");
            if (fetchedIdToken == null || !fetchedIdToken.isJsonPrimitive()) {
                throw new CloudRuntimeException("No id_token found in the token endpoint response");
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
        validateAudience(claims, provider.getClientId());
        validateTokenTime(claims);

        Object emailClaim = claims.getClaim("email");
        String email = emailClaim instanceof String ? (String) emailClaim : null;
        if (StringUtils.isBlank(email)) {
            throw new CloudAuthenticationException("The id_token carries no email claim");
        }
        Boolean emailVerified = emailVerifiedClaim(claims);
        if (Boolean.FALSE.equals(emailVerified)) {
            throw new CloudAuthenticationException("The identity provider has marked the email address in the id_token as not verified");
        }
        if (emailVerified == null && isVerifiedEmailRequired(provider)) {
            throw new CloudAuthenticationException(String.format(
                    "The identity provider did not assert that the email address is verified; set %s to false for this domain to accept it",
                    OAuth2AuthManager.OIDCRequireVerifiedEmail.key()));
        }
        return email;
    }

    /**
     * Validate the audience per OIDC core 3.1.3.7: the registered client must be an audience, and when the
     * token lists more than one audience the authorized party (azp) must be present and must be this client.
     * If azp is present at all it must match, so a token minted for another relying party on the same issuer
     * cannot be replayed here.
     */
    private void validateAudience(JwtClaims claims, String clientId) {
        List<String> audiences = claims.getAudiences();
        if (audiences == null || !audiences.contains(clientId)) {
            throw new CloudAuthenticationException("Audience mismatch");
        }
        Object azp = claims.getClaim("azp");
        if (audiences.size() > 1 && azp == null) {
            throw new CloudAuthenticationException("The id_token has multiple audiences but no authorized party (azp)");
        }
        if (azp != null && !clientId.equals(azp)) {
            throw new CloudAuthenticationException("The authorized party (azp) does not match the registered client");
        }
    }

    private void validateTokenTime(JwtClaims claims) {
        try {
            JwtUtils.validateJwtExpiry(claims, CLOCK_SKEW_SECONDS, true);
        } catch (CloudAuthenticationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CloudAuthenticationException("The id_token has expired or carries no expiry");
        }
        Long notBefore = claims.getNotBefore();
        if (notBefore != null && notBefore > (System.currentTimeMillis() / 1000L) + CLOCK_SKEW_SECONDS) {
            throw new CloudAuthenticationException("The id_token is not valid yet");
        }
    }

    private Boolean emailVerifiedClaim(JwtClaims claims) {
        Object verified = claims.getClaim("email_verified");
        if (verified instanceof Boolean) {
            return (Boolean) verified;
        }
        if (verified instanceof String) {
            String value = ((String) verified).trim();
            if ("true".equalsIgnoreCase(value)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(value)) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    protected boolean isVerifiedEmailRequired(OauthProviderVO provider) {
        return Boolean.TRUE.equals(
                OAuth2AuthManager.OIDCRequireVerifiedEmail.valueInScope(ConfigKey.Scope.Domain, provider.getDomainId(), false));
    }

    protected void verifySignature(JwsJwtCompactConsumer consumer, OIDCMetadata metadata, OauthProviderVO provider) {
        if (StringUtils.isBlank(metadata.getJwksUri())) {
            throw new CloudAuthenticationException(String.format(
                    "Provider %s has no JWKS endpoint, so the id_token signature cannot be verified", provider.getProvider()));
        }

        String jwksUri = metadata.getJwksUri();
        String keyId = consumer.getJwsHeaders().getKeyId();

        JsonWebKeys keys = jwksCache.get(jwksUri, this::readJwkSet);
        JsonWebKey key = selectKey(keys, keyId);
        if (key == null && StringUtils.isNotBlank(keyId)) {
            keys = readJwkSet(jwksUri);
            jwksCache.put(jwksUri, keys);
            key = selectKey(keys, keyId);
        }
        if (key == null) {
            throw new CloudAuthenticationException("No matching signing key was published by the identity provider");
        }
        if (!consumer.verifySignatureWith(key, resolveAlgorithm(consumer, key))) {
            throw new CloudAuthenticationException("The id_token signature is not valid");
        }
    }

    private JsonWebKey selectKey(JsonWebKeys keys, String keyId) {
        return StringUtils.isNotBlank(keyId) ? keys.getKey(keyId) : singleKey(keys);
    }

    private SignatureAlgorithm resolveAlgorithm(JwsJwtCompactConsumer consumer, JsonWebKey key) {
        SignatureAlgorithm algorithm = key.getAlgorithm() != null
                ? SignatureAlgorithm.getAlgorithm(key.getAlgorithm())
                : consumer.getJwsHeaders().getSignatureAlgorithm();
        if (algorithm == null) {
            throw new CloudAuthenticationException("The id_token does not name a signature algorithm");
        }
        if (!isAsymmetricSignature(algorithm)) {
            throw new CloudAuthenticationException("Unsupported id_token signature algorithm: " + algorithm.getJwaName());
        }
        return algorithm;
    }

    /**
     * Only asymmetric signatures (RSA, RSA-PSS, ECDSA) are accepted for an id_token verified against a
     * published JWKS. Rejecting HMAC (HS*) and "none" closes the RS256 to HS256 key-confusion attack where
     * a token is HMAC-signed with the public key as the secret.
     */
    private boolean isAsymmetricSignature(SignatureAlgorithm algorithm) {
        String name = algorithm.getJwaName();
        return name != null && (name.startsWith("RS") || name.startsWith("PS") || name.startsWith("ES"));
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
            String body = readBody(response);
            if (response.getStatusLine().getStatusCode() != 200) {
                logger.warn("{} (HTTP {})", failureMessage, response.getStatusLine().getStatusCode());
                throw new CloudRuntimeException(failureMessage);
            }
            return body;
        } catch (IOException e) {
            throw new CloudRuntimeException(failureMessage, e);
        }
    }

    /**
     * Read a response body with a hard size cap so a hostile or misbehaving identity provider cannot
     * exhaust management-server memory by streaming an unbounded body.
     */
    private String readBody(CloseableHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return "";
        }
        if (entity.getContentLength() > MAX_RESPONSE_BYTES) {
            throw new CloudRuntimeException("The identity provider response is too large");
        }
        byte[] buffer = new byte[MAX_RESPONSE_BYTES + 1];
        int total = 0;
        try (InputStream in = entity.getContent()) {
            int read;
            while (total <= MAX_RESPONSE_BYTES && (read = in.read(buffer, total, buffer.length - total)) != -1) {
                total += read;
            }
        }
        if (total > MAX_RESPONSE_BYTES) {
            throw new CloudRuntimeException("The identity provider response is too large");
        }
        return new String(buffer, 0, total, StandardCharsets.UTF_8);
    }

    private JsonObject asJsonObject(String body, String failureMessage) {
        try {
            JsonElement element = JsonParser.parseString(body);
            if (!element.isJsonObject()) {
                throw new CloudRuntimeException(failureMessage);
            }
            return element.getAsJsonObject();
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CloudRuntimeException(failureMessage);
        }
    }

    /**
     * Require that an OIDC endpoint uses https, so the client secret is never POSTed in cleartext and a
     * tampered discovery document cannot redirect the management server to an internal http target. Loopback
     * is allowed for development.
     */
    private void requireHttps(String url, String what) {
        if (StringUtils.isBlank(url) || url.toLowerCase().startsWith("https://") || !isHttpsRequired()) {
            return;
        }
        if (url.toLowerCase().startsWith("http://")) {
            String host;
            try {
                host = URI.create(url).getHost();
            } catch (IllegalArgumentException e) {
                throw new CloudRuntimeException(String.format("The %s is not a valid URL: %s", what, url));
            }
            if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host) || "[::1]".equals(host)) {
                return;
            }
        }
        throw new CloudRuntimeException(String.format("The %s must use https: %s (set %s to false to allow http)",
                what, url, OAuth2AuthManager.OIDCRequireHttps.key()));
    }

    protected boolean isHttpsRequired() {
        return Boolean.TRUE.equals(OAuth2AuthManager.OIDCRequireHttps.value());
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
