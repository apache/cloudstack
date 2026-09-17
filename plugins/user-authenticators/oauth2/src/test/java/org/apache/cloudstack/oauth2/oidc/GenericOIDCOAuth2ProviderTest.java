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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Collections;

import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.utils.exception.CloudRuntimeException;

public class GenericOIDCOAuth2ProviderTest {

    private static final String REGISTRATION = "corp-idp";
    private static final String ISSUER = "https://idp.example.com";
    private static final String CLIENT_ID = "test-client";

    @Mock
    private OauthProviderDao oauthProviderDao;

    @Mock
    private CloseableHttpClient httpClient;

    private GenericOIDCOAuth2Provider provider;

    private OauthProviderVO registration;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        provider = Mockito.spy(new GenericOIDCOAuth2Provider(httpClient));
        provider.oauthProviderDao = oauthProviderDao;

        registration = new OauthProviderVO();
        registration.setProvider(REGISTRATION);
        registration.setType(GenericOIDCOAuth2Provider.OIDC_PROVIDER_TYPE);
        registration.setClientId(CLIENT_ID);
        registration.setSecretKey("test-secret");
        registration.setRedirectUri("http://localhost/redirect");
        registration.setIssuerUrl(ISSUER);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    private String idToken(String issuer, String audience, String email, long expiresInSeconds) {
        String header = "{\"alg\":\"RS256\",\"kid\":\"key-1\"}";
        String payload = "{"
                + "\"iss\":\"" + issuer + "\","
                + "\"aud\":[\"" + audience + "\"],"
                + (email == null ? "" : "\"email\":\"" + email + "\",")
                + "\"exp\":" + (System.currentTimeMillis() / 1000L + expiresInSeconds) + ","
                + "\"sub\":\"12345\""
                + "}";
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "."
                + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".signature";
    }

    private GenericOIDCOAuth2Provider.OIDCMetadata metadata() {
        return new GenericOIDCOAuth2Provider.OIDCMetadata(ISSUER, ISSUER + "/authorize", ISSUER + "/token", ISSUER + "/jwks");
    }

    @Test
    public void testNameIsTheProviderType() {
        assertEquals(GenericOIDCOAuth2Provider.OIDC_PROVIDER_TYPE, provider.getName());
        assertNull(provider.getUserEmailAddress());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyUserWithoutRegistrationNameIsRejected() {
        provider.verifyUser("user@example.com", "code");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifySecretCodeWithoutRegistrationNameIsRejected() {
        provider.verifySecretCodeAndFetchEmail("code");
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testBlankRegistrationNameIsRejected() {
        provider.findRegistration(" ", null);
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testUnregisteredProviderIsRejected() {
        when(oauthProviderDao.findByProviderAndDomainWithGlobalFallback(REGISTRATION, null)).thenReturn(null);
        provider.findRegistration(REGISTRATION, null);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testRegistrationWithoutIssuerIsRejected() {
        registration.setIssuerUrl(null);
        registration.setAuthorizeUrl(ISSUER + "/authorize");
        registration.setTokenUrl(ISSUER + "/token");
        provider.getMetadata(registration);
    }

    @Test
    public void testDiscoveryReadsTheEndpoints() {
        String document = "{"
                + "\"issuer\":\"" + ISSUER + "\","
                + "\"authorization_endpoint\":\"" + ISSUER + "/authorize\","
                + "\"token_endpoint\":\"" + ISSUER + "/token\","
                + "\"jwks_uri\":\"" + ISSUER + "/jwks\"}";
        doReturn(document).when(provider).httpGet(eq(ISSUER + "/.well-known/openid-configuration"), anyString());

        GenericOIDCOAuth2Provider.OIDCMetadata metadata = provider.discover(ISSUER);

        assertEquals(ISSUER, metadata.getIssuer());
        assertEquals(ISSUER + "/token", metadata.getTokenEndpoint());
        assertEquals(ISSUER + "/jwks", metadata.getJwksUri());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDiscoveryWithoutTokenEndpointIsRejected() {
        doReturn("{\"issuer\":\"" + ISSUER + "\"}").when(provider).httpGet(anyString(), anyString());
        provider.discover(ISSUER);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDiscoveryWithoutJwksIsRejected() {
        doReturn("{\"issuer\":\"" + ISSUER + "\",\"token_endpoint\":\"" + ISSUER + "/token\"}")
                .when(provider).httpGet(anyString(), anyString());
        provider.discover(ISSUER);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDiscoveryNamingAnotherIssuerIsRejected() {
        doReturn("{\"issuer\":\"https://attacker.example.com\",\"token_endpoint\":\"" + ISSUER + "/token\","
                + "\"jwks_uri\":\"" + ISSUER + "/jwks\"}").when(provider).httpGet(anyString(), anyString());
        provider.discover(ISSUER);
    }

    private KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String signedIdToken(KeyPair keys, String keyId, String email) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(ISSUER);
        claims.setAudiences(Collections.singletonList(CLIENT_ID));
        claims.setSubject("12345");
        claims.setExpiryTime(System.currentTimeMillis() / 1000L + 3600);
        claims.setClaim("email", email);
        JwsHeaders headers = new JwsHeaders(SignatureAlgorithm.RS256);
        headers.setKeyId(keyId);
        return new JwsJwtCompactProducer(headers, claims)
                .signWith(JwsUtils.getPrivateKeySignatureProvider(keys.getPrivate(), SignatureAlgorithm.RS256));
    }

    private void publishKey(KeyPair keys, String keyId) {
        JsonWebKey key = JwkUtils.fromRSAPublicKey((RSAPublicKey) keys.getPublic(), "RS256");
        key.setKeyId(keyId);
        doReturn(JwkUtils.jwkSetToJson(new JsonWebKeys(key))).when(provider).httpGet(eq(ISSUER + "/jwks"), anyString());
    }

    @Test
    public void testGenuinelySignedTokenIsAccepted() throws Exception {
        KeyPair keys = rsaKeyPair();
        publishKey(keys, "key-1");

        assertEquals("user@example.com",
                provider.validateAndExtractEmail(signedIdToken(keys, "key-1", "user@example.com"), registration, metadata()));
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testTokenWithAlteredClaimsIsRejected() throws Exception {
        KeyPair keys = rsaKeyPair();
        publishKey(keys, "key-1");
        String[] parts = signedIdToken(keys, "key-1", "user@example.com").split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("user@example.com", "admin@example.com");
        String tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];

        provider.validateAndExtractEmail(tampered, registration, metadata());
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testTokenSignedByAnotherKeyIsRejected() throws Exception {
        publishKey(rsaKeyPair(), "key-1");

        provider.validateAndExtractEmail(signedIdToken(rsaKeyPair(), "key-1", "user@example.com"), registration, metadata());
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testTokenNamingAnUnpublishedKeyIsRejected() throws Exception {
        KeyPair keys = rsaKeyPair();
        publishKey(keys, "key-1");

        provider.validateAndExtractEmail(signedIdToken(keys, "key-2", "user@example.com"), registration, metadata());
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testUnsignedTokenIsRejectedWhenNoJwksIsPublished() {
        GenericOIDCOAuth2Provider.OIDCMetadata noJwks =
                new GenericOIDCOAuth2Provider.OIDCMetadata(ISSUER, null, ISSUER + "/token", null);
        provider.validateAndExtractEmail(idToken(ISSUER, CLIENT_ID, "user@example.com", 3600), registration, noJwks);
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testIssuerMismatchIsRejected() {
        doNothing().when(provider).verifySignature(any(), any(), any());
        provider.validateAndExtractEmail(idToken("https://attacker.example.com", CLIENT_ID, "user@example.com", 3600),
                registration, metadata());
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testAudienceMismatchIsRejected() {
        doNothing().when(provider).verifySignature(any(), any(), any());
        provider.validateAndExtractEmail(idToken(ISSUER, "another-client", "user@example.com", 3600),
                registration, metadata());
    }

    @Test(expected = RuntimeException.class)
    public void testExpiredTokenIsRejected() {
        doNothing().when(provider).verifySignature(any(), any(), any());
        provider.validateAndExtractEmail(idToken(ISSUER, CLIENT_ID, "user@example.com", -3600),
                registration, metadata());
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testTokenWithoutEmailClaimIsRejected() {
        doNothing().when(provider).verifySignature(any(), any(), any());
        provider.validateAndExtractEmail(idToken(ISSUER, CLIENT_ID, null, 3600), registration, metadata());
    }

    @Test
    public void testValidTokenYieldsTheEmailClaim() {
        doNothing().when(provider).verifySignature(any(), any(), any());

        String email = provider.validateAndExtractEmail(idToken(ISSUER, CLIENT_ID, "user@example.com", 3600),
                registration, metadata());

        assertEquals("user@example.com", email);
    }

    /**
     * The provider is a singleton shared by every login, so it must hold no token state between
     * calls: each call has to exchange the authorization code it was given.
     */
    @Test
    public void testEveryCallExchangesItsOwnAuthorizationCode() {
        when(oauthProviderDao.findByProviderAndDomainWithGlobalFallback(REGISTRATION, null)).thenReturn(registration);
        doReturn(metadata()).when(provider).getMetadata(registration);
        doReturn("token-for-first").when(provider).exchangeAuthorizationCode(eq("first-code"), any(), any());
        doReturn("token-for-second").when(provider).exchangeAuthorizationCode(eq("second-code"), any(), any());
        doReturn("first@example.com").when(provider).validateAndExtractEmail(eq("token-for-first"), any(), any());
        doReturn("second@example.com").when(provider).validateAndExtractEmail(eq("token-for-second"), any(), any());

        assertEquals("first@example.com", provider.verifySecretCodeAndFetchEmail("first-code", null, REGISTRATION));
        assertEquals("second@example.com", provider.verifySecretCodeAndFetchEmail("second-code", null, REGISTRATION));

        verify(provider, times(1)).exchangeAuthorizationCode(eq("first-code"), any(), any());
        verify(provider, times(1)).exchangeAuthorizationCode(eq("second-code"), any(), any());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyUserRejectsAnEmailThatDoesNotMatchTheToken() {
        when(oauthProviderDao.findByProviderAndDomainWithGlobalFallback(REGISTRATION, null)).thenReturn(registration);
        doReturn("someone-else@example.com").when(provider).resolveEmail("code", null, REGISTRATION);

        provider.verifyUser("user@example.com", "code", null, REGISTRATION);
    }

    /**
     * The UI resolves the code with verifyOAuthCodeAndGetUser and then logs in with the same code, and an
     * identity provider accepts an authorization code only once, so the login must not redeem it again.
     */
    @Test
    public void testLoginAfterVerificationDoesNotRedeemTheCodeAgain() {
        doReturn("user@example.com").when(provider).resolveEmail("code", null, REGISTRATION);

        assertEquals("user@example.com", provider.verifySecretCodeAndFetchEmail("code", null, REGISTRATION));
        assertTrue(provider.verifyUser("user@example.com", "code", null, REGISTRATION));

        verify(provider, times(1)).resolveEmail("code", null, REGISTRATION);
    }

    @Test
    public void testVerifiedCodeIsServedFromTheCacheOnlyOnce() {
        doReturn("user@example.com").when(provider).resolveEmail("code", null, REGISTRATION);

        provider.verifySecretCodeAndFetchEmail("code", null, REGISTRATION);
        provider.verifyUser("user@example.com", "code", null, REGISTRATION);
        provider.verifyUser("user@example.com", "code", null, REGISTRATION);

        verify(provider, times(2)).resolveEmail("code", null, REGISTRATION);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAnotherCodeIsNeverAnsweredFromTheCache() {
        doReturn("user@example.com").when(provider).resolveEmail("user-code", null, REGISTRATION);
        doThrow(new CloudRuntimeException("invalid_grant")).when(provider).resolveEmail("unrelated-code", null, REGISTRATION);

        provider.verifySecretCodeAndFetchEmail("user-code", null, REGISTRATION);
        provider.verifyUser("user@example.com", "unrelated-code", null, REGISTRATION);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCachedCodeIsScopedToItsRegistration() {
        doReturn("user@example.com").when(provider).resolveEmail("code", null, REGISTRATION);
        doThrow(new CloudRuntimeException("invalid_grant")).when(provider).resolveEmail("code", null, "other-idp");

        provider.verifySecretCodeAndFetchEmail("code", null, REGISTRATION);
        provider.verifyUser("user@example.com", "code", null, "other-idp");
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserRejectsEmptyArguments() {
        provider.verifyUser("", "", null, REGISTRATION);
    }
}
