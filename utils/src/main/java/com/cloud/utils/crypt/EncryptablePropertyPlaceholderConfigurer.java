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

import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * Spring bean that resolves property placeholders, decrypting any value wrapped as
 * {@code ENC(...)} with the given jasypt {@link StringEncryptor}. Values that are not wrapped
 * are passed through unchanged.
 *
 * This replaces {@code org.jasypt.spring3.properties.EncryptablePropertyPlaceholderConfigurer}
 * (from the jasypt-spring3 artifact), which is not on the classpath and is incompatible with
 * Spring 5, so beans referencing it fail with a ClassNotFoundException.
 */
public class EncryptablePropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";

    private final StringEncryptor encryptor;

    public EncryptablePropertyPlaceholderConfigurer(StringEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    protected String convertPropertyValue(String originalValue) {
        if (originalValue == null) {
            return null;
        }

        String trimmedValue = originalValue.trim();
        if (trimmedValue.startsWith(ENC_PREFIX) && trimmedValue.endsWith(ENC_SUFFIX)) {
            String encryptedValue = trimmedValue.substring(ENC_PREFIX.length(), trimmedValue.length() - ENC_SUFFIX.length());
            return encryptor.decrypt(encryptedValue);
        }

        return originalValue;
    }
}
