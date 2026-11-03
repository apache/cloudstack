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

package org.apache.cloudstack.api.response;


import org.junit.Assert;
import org.junit.Test;

public class LoginCmdResponseTest {

    @Test
    public void testAllGettersAndSetters() {
        LoginCmdResponse response = new LoginCmdResponse();

        response.setUsername("user1");
        response.setUserId("100");
        response.setDomainId("200");
        response.setTimeout(3600);
        response.setAccount("account1");
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setType("admin");
        response.setTimeZone("UTC");
        response.setTimeZoneOffset("+00:00");
        response.setRegistered("true");
        response.setSessionKey("session-key");
        response.set2FAenabled("true");
        response.set2FAverfied("false");
        response.setProviderFor2FA("totp");
        response.setIssuerFor2FA("cloudstack");
        response.setManagementServerId("ms-1");

        Assert.assertEquals("user1", response.getUsername());
        Assert.assertEquals("100", response.getUserId());
        Assert.assertEquals("200", response.getDomainId());
        Assert.assertEquals(Integer.valueOf(3600), response.getTimeout());
        Assert.assertEquals("account1", response.getAccount());
        Assert.assertEquals("John", response.getFirstName());
        Assert.assertEquals("Doe", response.getLastName());
        Assert.assertEquals("admin", response.getType());
        Assert.assertEquals("UTC", response.getTimeZone());
        Assert.assertEquals("+00:00", response.getTimeZoneOffset());
        Assert.assertEquals("true", response.getRegistered());
        Assert.assertEquals("session-key", response.getSessionKey());
        Assert.assertEquals("true", response.is2FAenabled());
        Assert.assertEquals("false", response.is2FAverfied());
        Assert.assertEquals("totp", response.getProviderFor2FA());
        Assert.assertEquals("cloudstack", response.getIssuerFor2FA());
        Assert.assertEquals("ms-1", response.getManagementServerId());
    }

    @Test
    public void testPasswordChangeRequired_True() {
        LoginCmdResponse response = new LoginCmdResponse();
        response.setPasswordChangeRequired(true);
        Assert.assertTrue(response.getPasswordChangeRequired());
    }

    @Test
    public void testPasswordChangeRequired_False() {
        LoginCmdResponse response = new LoginCmdResponse();
        response.setPasswordChangeRequired(false);
        Assert.assertFalse(response.getPasswordChangeRequired());
    }

    @Test
    public void testPasswordChangeRequired_Null() {
        LoginCmdResponse response = new LoginCmdResponse();
        response.setPasswordChangeRequired(null);
        Assert.assertNull("Boolean.parseBoolean(null) should return null", response.getPasswordChangeRequired());
    }
}
