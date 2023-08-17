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

import java.nio.charset.StandardCharsets;

import org.apache.cloudstack.api.BaseCmd;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserDataManagerImplTest {

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

}
