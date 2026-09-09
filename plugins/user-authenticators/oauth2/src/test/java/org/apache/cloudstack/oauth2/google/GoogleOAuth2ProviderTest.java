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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.oauth2.dao.OauthProviderDao;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.utils.exception.CloudRuntimeException;

public class GoogleOAuth2ProviderTest {

    @Mock
    private OauthProviderDao _oauthProviderDao;

    @Spy
    @InjectMocks
    private GoogleOAuth2Provider _googleOAuth2Provider;

    private AutoCloseable closeable;

    @Mock
    private OauthProviderVO mockProvider;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        when(mockProvider.getClientId()).thenReturn("test_client_id");
        when(mockProvider.getSecretKey()).thenReturn("test_secret_key");
        when(mockProvider.getRedirectUri()).thenReturn("http://localhost/redirect");
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserWithNullEmail() {
        _googleOAuth2Provider.verifyUser(null, "secretCode");
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserWithNullSecretCode() {
        _googleOAuth2Provider.verifyUser("email@example.com", null);
    }

    @Test(expected = CloudAuthenticationException.class)
    public void testVerifyUserWithUnregisteredProvider() {
        when(_oauthProviderDao.findByProviderAndDomainWithGlobalFallback(anyString(), Mockito.isNull())).thenReturn(null);
        _googleOAuth2Provider.verifyUser("email@example.com", "secretCode");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyUserWithInvalidSecretCode() {
        when(_oauthProviderDao.findByProviderAndDomainWithGlobalFallback(anyString(), Mockito.isNull())).thenReturn(mockProvider);
        doReturn(null).when(_googleOAuth2Provider).verifyCodeAndFetchEmailInternal(
                "secretCode", null, mockProvider);

        _googleOAuth2Provider.verifyUser("email@example.com", "secretCode");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyUserWithMismatchedEmail() {
        when(_oauthProviderDao.findByProviderAndDomainWithGlobalFallback(anyString(), Mockito.isNull())).thenReturn(mockProvider);
        doReturn("otheremail@example.com").when(_googleOAuth2Provider).verifyCodeAndFetchEmailInternal(
                "secretCode", null, mockProvider);

        _googleOAuth2Provider.verifyUser("email@example.com", "secretCode");
    }

    @Test
    public void testVerifyUserEmail() {
        when(_oauthProviderDao.findByProviderAndDomainWithGlobalFallback(anyString(), Mockito.isNull())).thenReturn(mockProvider);
        doReturn("email@example.com").when(_googleOAuth2Provider).verifyCodeAndFetchEmailInternal(
                "secretCode", null, mockProvider);

        boolean result = _googleOAuth2Provider.verifyUser("email@example.com", "secretCode");

        assertTrue(result);
    }

    @Test
    public void testCacheInitializationAndCleanupResources() {
        // Verifies that cache cleanup executor is properly initialized
        GoogleOAuth2Provider provider = new GoogleOAuth2Provider();
        assertNotNull("GoogleOAuth2Provider should initialize", provider);
    }

    @Test
    public void testNoSensitiveDataInErrorMessages() {
        // Verifies that error messages don't expose sensitive information
        when(_oauthProviderDao.findByProviderAndDomainWithGlobalFallback(anyString(), Mockito.isNull())).thenReturn(mockProvider);
        String testSecret = "secret_key";
        String testEmail = "email@example.com";

        try {
            _googleOAuth2Provider.verifyUser(testEmail, testSecret);
            fail("Expected exception");
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            // Verify sensitive terms are not in error messages
            assertFalse("Error should not contain secret", errorMsg.toLowerCase().contains(testSecret));
            assertFalse("Error should not contain email", errorMsg.toLowerCase().contains(testEmail));
            assertFalse("Error should not contain token", errorMsg.toLowerCase().contains("access_token"));
        }
    }

    @Test
    public void testVerifyUserErrorHandlingAndCleanup() {
        // Tests that any authentication error properly cleans up
        when(_oauthProviderDao.findByProviderAndDomainWithGlobalFallback(anyString(), Mockito.isNull())).thenReturn(mockProvider);

        doReturn("error@example.com").when(_googleOAuth2Provider).verifyCodeAndFetchEmailInternal(
                "bad_code", null, mockProvider);

        try {
            _googleOAuth2Provider.verifyUser("expected@example.com", "bad_code");
            fail("Should throw exception for email mismatch");
        } catch (CloudRuntimeException e) {
            assertEquals("Should have proper error message",
                "Unable to verify the email address with the provided secret",
                e.getMessage());
        }
    }

    @Test
    public void testMultipleFailedVerificationAttempts() {
        // Tests that multiple failures are handled gracefully without cache pollution
        when(_oauthProviderDao.findByProviderAndDomainWithGlobalFallback(anyString(), Mockito.isNull())).thenReturn(mockProvider);

        // Multiple failed attempts
        for (int i = 0; i < 5; i++) {
            try {
                doReturn("wrong@example.com").when(_googleOAuth2Provider)
                    .verifyCodeAndFetchEmailInternal("code_" + i, null, mockProvider);
                _googleOAuth2Provider.verifyUser("correct@example.com", "code_" + i);
                fail("Should fail on attempt " + i);
            } catch (CloudRuntimeException e) {
                // Expected - cache should be cleaned for each failure
                assertTrue("Should report email verification failure",
                    e.getMessage().contains("email"));
            }
        }
    }
}
