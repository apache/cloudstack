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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.jasypt.encryption.StringEncryptor;
import org.junit.Assert;
import org.junit.Test;

public class EncryptablePropertyPlaceholderConfigurerTest {

    @Test
    public void convertPropertyValueDecryptsWrappedValue() {
        StringEncryptor encryptor = mock(StringEncryptor.class);
        when(encryptor.decrypt("bmb2VaFdQb")).thenReturn("secret");
        EncryptablePropertyPlaceholderConfigurer configurer = new EncryptablePropertyPlaceholderConfigurer(encryptor);

        String result = configurer.convertPropertyValue("ENC(bmb2VaFdQb)");

        Assert.assertEquals("secret", result);
    }

    @Test
    public void convertPropertyValueTrimsSurroundingWhitespaceBeforeDecrypting() {
        StringEncryptor encryptor = mock(StringEncryptor.class);
        when(encryptor.decrypt("bmb2VaFdQb")).thenReturn("secret");
        EncryptablePropertyPlaceholderConfigurer configurer = new EncryptablePropertyPlaceholderConfigurer(encryptor);

        String result = configurer.convertPropertyValue("  ENC(bmb2VaFdQb)  ");

        Assert.assertEquals("secret", result);
    }

    @Test
    public void convertPropertyValuePassesThroughPlainValues() {
        StringEncryptor encryptor = mock(StringEncryptor.class);
        EncryptablePropertyPlaceholderConfigurer configurer = new EncryptablePropertyPlaceholderConfigurer(encryptor);

        String result = configurer.convertPropertyValue("guest");

        Assert.assertEquals("guest", result);
        verifyNoInteractions(encryptor);
    }

    @Test
    public void convertPropertyValueHandlesNull() {
        StringEncryptor encryptor = mock(StringEncryptor.class);
        EncryptablePropertyPlaceholderConfigurer configurer = new EncryptablePropertyPlaceholderConfigurer(encryptor);

        Assert.assertNull(configurer.convertPropertyValue(null));
        verifyNoInteractions(encryptor);
    }
}
