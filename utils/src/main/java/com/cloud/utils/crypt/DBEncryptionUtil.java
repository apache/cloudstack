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

import java.util.Properties;

import org.apache.log4j.Logger;

import com.cloud.utils.db.DbProperties;
import com.cloud.utils.exception.CloudRuntimeException;

public class DBEncryptionUtil {
    public static final Logger s_logger = Logger.getLogger(DBEncryptionUtil.class);
    private static CloudStackEncryptor s_encryptor = null;

    public static String encrypt(String plain) {
        if (!EncryptionSecretKeyChecker.useEncryption() || (plain == null) || plain.isEmpty()) {
            return plain;
        }
        if (s_encryptor == null) {
            initialize();
        }
        return s_encryptor.encrypt(plain);
    }

    public static String decrypt(String encrypted) {
        if (!EncryptionSecretKeyChecker.useEncryption() || (encrypted == null) || encrypted.isEmpty()) {
            return encrypted;
        }
        if (s_encryptor == null) {
            initialize();
        }

        return s_encryptor.decrypt(encrypted);
    }

    protected static void initialize() {
        s_logger.debug("Calling to initialize");
        final Properties dbProps = DbProperties.getDbProperties();

        if (EncryptionSecretKeyChecker.useEncryption()) {
            String dbSecretKey = dbProps.getProperty("db.cloud.encrypt.secret");
            if (dbSecretKey == null || dbSecretKey.isEmpty()) {
                throw new CloudRuntimeException("Empty DB secret key in db.properties");
            }
            String dbEncryptorVersion = dbProps.getProperty("db.cloud.encryptor.version");

            s_encryptor = new CloudStackEncryptor(dbSecretKey, dbEncryptorVersion, DBEncryptionUtil.class);
        } else {
            throw new CloudRuntimeException("Trying to encrypt db values when encryption is not enabled");
        }
        s_logger.debug("initialized");
    }
}
