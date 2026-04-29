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
package org.apache.cloudstack.framework.jobs;

import org.apache.cloudstack.framework.jobs.impl.AsyncJobManagerImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith (MockitoJUnitRunner.class)
public class AsyncJobManagerTest {

    @Spy
    AsyncJobManagerImpl asyncJobManager;

    String input = "\"haprovider\":\"kvmhaprovider\"},\"outofbandmanagement\":{\"powerstate\":\"On\",\"enabled\":true,\"driver\":\"redfish\",\"address\":\"oob-address.com\",\"port\":\"80\",\"username\":\"root\",\"password\":\"password\"},\"resourcestate\":\"PrepareForMaintenance\",\"hahost\":false";
    String expected = "\"haprovider\":\"kvmhaprovider\"},\"outofbandmanagement\":{\"powerstate\":\"On\",\"enabled\":true,\"driver\":\"redfish\",\"address\":\"oob-address.com\",\"port\":\"80\",\"username\":\"root\",\"password\":\"p*****\"},\"resourcestate\":\"PrepareForMaintenance\",\"hahost\":false";
    String obfuscatedInput = "\"haprovider\":\"kvmhaprovider\"},\"outofbandmanagement\":{\"powerstate\":\"On\",\"enabled\":true,\"driver\":\"redfish\",\"address\":\"oob-address.com\",\"port\":\"80\",\"username\":\"root\",\"password\":\"p***\"},\"resourcestate\":\"PrepareForMaintenance\",\"hahost\":false";
    String noPassword = "\"haprovider\":\"kvmhaprovider\"},\"outofbandmanagement\":{\"powerstate\":\"On\",\"enabled\":true,\"driver\":\"redfish\",\"address\":\"oob-address.com\",\"port\":\"80\",\"username\":\"root\"},\"resourcestate\":\"PrepareForMaintenance\",\"hahost\":false";

    String inputNoBraces = "\"password\":\"password\"\",\"action\":\"OFF\"";
    String expectedNoBraces = "\"password\":\"p*****\",\"action\":\"OFF\"";

    @Test
    public void obfuscatePasswordTest() {
        String result = asyncJobManager.obfuscatePassword(input, true);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void obfuscatePasswordTestNoBraces() {
        String result = asyncJobManager.obfuscatePassword(inputNoBraces, true);
        Assert.assertEquals(expectedNoBraces, result);
    }

    @Test
    public void obfuscatePasswordTestHidePasswordFalse() {
        String result = asyncJobManager.obfuscatePassword(input, false);
        Assert.assertEquals(input, result);
    }

    @Test
    public void obfuscatePasswordTestObfuscatedInput() {
        String result = asyncJobManager.obfuscatePassword(obfuscatedInput, true);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void obfuscatePasswordTestHidePasswordFalseObfuscatedInput() {
        String result = asyncJobManager.obfuscatePassword(obfuscatedInput, false);
        Assert.assertEquals(obfuscatedInput, result);
    }

    @Test
    public void obfuscatePasswordTestNoPassword() {
        String result = asyncJobManager.obfuscatePassword(noPassword, true);
        Assert.assertEquals(noPassword, result);
    }

    @Test
    public void obfuscatePasswordTestHidePasswordNoPassword() {
        String result = asyncJobManager.obfuscatePassword(noPassword, false);
        Assert.assertEquals(noPassword, result);
    }

}
