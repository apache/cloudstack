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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

public class EncryptionSecretKeyChangerTest {
    @Spy
    EncryptionSecretKeyChanger changer = new EncryptionSecretKeyChanger();
    @Mock
    CloudStackEncryptor oldEncryptor;
    @Mock
    CloudStackEncryptor newEncryptor;

    private static final String emtpyString = "";
    private static final String encryptedValue = "encryptedValue";
    private static final String plainText = "plaintext";
    private static final String newEncryptedValue = "newEncryptedValue";

    @Before
    public void setUp() {
        oldEncryptor = Mockito.mock(CloudStackEncryptor.class);
        newEncryptor = Mockito.mock(CloudStackEncryptor.class);

        ReflectionTestUtils.setField(changer, "oldEncryptor", oldEncryptor);
        ReflectionTestUtils.setField(changer, "newEncryptor", newEncryptor);

        Mockito.when(oldEncryptor.decrypt(encryptedValue)).thenReturn(plainText);
        Mockito.when(newEncryptor.encrypt(plainText)).thenReturn(newEncryptedValue);
    }

    @Test
    public void migrateValueTest() {
        String value = changer.migrateValue(encryptedValue);
        Assert.assertEquals(newEncryptedValue, value);

        Mockito.verify(oldEncryptor).decrypt(encryptedValue);
        Mockito.verify(newEncryptor).encrypt(plainText);
    }

    @Test
    public void migrateValueTest2() {
        String value = changer.migrateValue(emtpyString);
        Assert.assertEquals(emtpyString, value);
    }

    @Test
    public void migrateUrlOrPathTest() {
        String path = emtpyString;
        Assert.assertEquals(path, changer.migrateUrlOrPath(path));

        path = String.format("password=%s", encryptedValue);
        Assert.assertEquals(path.replaceAll("password=" + encryptedValue, "password=" + newEncryptedValue), changer.migrateUrlOrPath(path));

        path = String.format("username=user&password=%s", encryptedValue);
        Assert.assertEquals(path.replaceAll("password=" + encryptedValue, "password=" + newEncryptedValue), changer.migrateUrlOrPath(path));

        path = String.format("username=user&password2=%s", encryptedValue);
        Assert.assertEquals(path, changer.migrateUrlOrPath(path));

        path = String.format("username=user&password=%s&add=false", encryptedValue);
        Assert.assertEquals(path.replaceAll("password=" + encryptedValue, "password=" + newEncryptedValue), changer.migrateUrlOrPath(path));
    }
}
