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
package org.apache.cloudstack.userdata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.cloudstack.api.BaseCmd;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.domain.Domain;
import com.cloud.user.User;
import com.cloud.user.UserDataVO;
import com.cloud.user.dao.UserDataDao;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class UserDataManagerImplTest {

    @Mock
    private UserDataDao userDataDao;

    @Spy
    @InjectMocks
    private UserDataManagerImpl userDataManager;

    @Test
    public void testValidateBase64WithoutPadding() {
        // fo should be encoded in base64 either as Zm8 or Zm8=
        String encodedUserdata = "Zm8";
        String encodedUserdataWithPadding = "Zm8=";

        // Verify that we accept both but return the padded version
        assertEquals("validate return the value with padding", encodedUserdataWithPadding, userDataManager.validateUserData(encodedUserdata, BaseCmd.HTTPMethod.GET));
        assertEquals("validate return the value with padding", encodedUserdataWithPadding, userDataManager.validateUserData(encodedUserdataWithPadding, BaseCmd.HTTPMethod.GET));
    }

    @Test
    public void testValidateUrlEncodedBase64() {
        // fo should be encoded in base64 either as Zm8 or Zm8=
        String encodedUserdata = "Zm+8/w8=";
        String urlEncodedUserdata = java.net.URLEncoder.encode(encodedUserdata, StandardCharsets.UTF_8);

        // Verify that we accept both but return the padded version
        assertEquals("validate return the value with padding", encodedUserdata, userDataManager.validateUserData(encodedUserdata, BaseCmd.HTTPMethod.GET));
        assertEquals("validate return the value with padding", encodedUserdata, userDataManager.validateUserData(urlEncodedUserdata, BaseCmd.HTTPMethod.GET));
    }

    @Test
    public void testValidateAndGetUserDataForSystemVMWithBlankUuid() throws IOException {
        // Test with blank UUID should return null
        assertNull("null UUID should return null", userDataManager.validateAndGetUserDataForSystemVM(null));
        assertNull("blank UUID should return null", userDataManager.validateAndGetUserDataForSystemVM(""));
        assertNull("blank UUID should return null", userDataManager.validateAndGetUserDataForSystemVM("   "));
    }

    @Test
    public void testValidateAndGetUserDataForSystemVMNotFound() throws IOException {
        // Test when userDataVo is not found
        String testUuid = "test-uuid-123";
        when(userDataDao.findByUuid(testUuid)).thenReturn(null);

        assertNull("userdata not found should return null", userDataManager.validateAndGetUserDataForSystemVM(testUuid));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testValidateAndGetUserDataForSystemVMInvalidDomain() throws IOException {
        // Test with userDataVo that doesn't belong to ROOT domain
        String testUuid = "test-uuid-123";
        UserDataVO userDataVo = Mockito.mock(UserDataVO.class);
        when(userDataVo.getDomainId()).thenReturn(2L); // Not ROOT domain

        when(userDataDao.findByUuid(testUuid)).thenReturn(userDataVo);
        userDataManager.validateAndGetUserDataForSystemVM(testUuid);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testValidateAndGetUserDataForSystemVMInvalidAccount() throws IOException {
        // Test with userDataVo that doesn't belong to ADMIN account
        String testUuid = "test-uuid-123";
        UserDataVO userDataVo = Mockito.mock(UserDataVO.class);
        when(userDataVo.getDomainId()).thenReturn(Domain.ROOT_DOMAIN);
        when(userDataVo.getAccountId()).thenReturn(3L);
        userDataVo.setUserData("dGVzdCBkYXRh"); // "test data" in base64

        when(userDataDao.findByUuid(testUuid)).thenReturn(userDataVo);
        userDataManager.validateAndGetUserDataForSystemVM(testUuid);
    }

    @Test
    public void testValidateAndGetUserDataForSystemVMValidSystemVMUserData() throws IOException {
        // Test with valid system VM userdata (ROOT domain + ADMIN account)
        String testUuid = "test-uuid-123";
        String originalText = "#!/bin/bash\necho 'Hello World'";
        String base64EncodedUserData = Base64.getEncoder().encodeToString(originalText.getBytes());

        UserDataVO userDataVo = Mockito.mock(UserDataVO.class);
        when(userDataVo.getDomainId()).thenReturn(Domain.ROOT_DOMAIN);
        when(userDataVo.getAccountId()).thenReturn(User.UID_ADMIN);
        when(userDataVo.getUserData()).thenReturn(base64EncodedUserData);

        when(userDataDao.findByUuid(testUuid)).thenReturn(userDataVo);

        String result = userDataManager.validateAndGetUserDataForSystemVM(testUuid);

        // Verify result is not null and is base64 encoded
        assertNotNull("result should not be null", result);
        assertFalse("result should be base64 encoded", result.isEmpty());

        // Verify the result is valid base64
        try {
            Base64.getDecoder().decode(result);
        } catch (IllegalArgumentException e) {
            throw new AssertionError("Result should be valid base64", e);
        }

        // The result should be different from input since it's compressed
        assertNotEquals("compressed result should be different from original", result, base64EncodedUserData);
    }

}
