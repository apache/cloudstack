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

package com.cloud.utils.crypt;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

public class EncryptionSecretKeyCheckerTest {
    @Before
    public void setup() {
        EncryptionSecretKeyChecker.initEncryptor("managementkey");
    }

    @After
    public void tearDown() {
        EncryptionSecretKeyChecker.resetEncryptor();
    }

    @Test
    public void decryptPropertyIfNeededTest() {
        String rawValue = "ENC(iYVsCZXiGiC6SzZLMNBvBL93hoUpntxkuRjyaqC8L+JYKXw=)";
        String result = EncryptionSecretKeyChecker.decryptPropertyIfNeeded(rawValue);
        Assert.assertEquals("encthis", result);
    }

    @Test
    public void decryptAnyPropertiesTest() {
        Properties props = new Properties();
        props.setProperty("db.cloud.encrypt.secret", "ENC(iYVsCZXiGiC6SzZLMNBvBL93hoUpntxkuRjyaqC8L+JYKXw=)");
        props.setProperty("other.unencrypted", "somevalue");

        EncryptionSecretKeyChecker.decryptAnyProperties(props);

        Assert.assertEquals("encthis", props.getProperty("db.cloud.encrypt.secret"));
        Assert.assertEquals("somevalue", props.getProperty("other.unencrypted"));
    }
}
