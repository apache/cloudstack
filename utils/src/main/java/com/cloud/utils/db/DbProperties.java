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

package com.cloud.utils.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.crypt.EncryptionSecretKeyChecker;

public class DbProperties {

    private static final Logger log = Logger.getLogger(DbProperties.class);

    private static Properties properties = new Properties();
    private static boolean loaded = false;

    protected static Properties wrapEncryption(Properties dbProps) throws IOException {
        EncryptionSecretKeyChecker checker = new EncryptionSecretKeyChecker();
        checker.check(dbProps);

        if (EncryptionSecretKeyChecker.useEncryption()) {
            return dbProps;
        } else {
            EncryptableProperties encrProps = new EncryptableProperties(EncryptionSecretKeyChecker.getEncryptor());
            encrProps.putAll(dbProps);
            return encrProps;
        }
    }

    public synchronized static Properties getDbProperties() {
        if (!loaded) {
            Properties dbProps = new Properties();
            InputStream is = null;
            try {
                File props = PropertiesUtil.findConfigFile("db.properties");
                if (props != null && props.exists()) {
                    is = new FileInputStream(props);
                }

                if (is == null) {
                    is = PropertiesUtil.openStreamFromURL("db.properties");
                }

                if (is == null) {
                    System.err.println("Failed to find db.properties");
                    log.error("Failed to find db.properties");
                }

                if (is != null) {
                    dbProps.load(is);
                }

                EncryptionSecretKeyChecker checker = new EncryptionSecretKeyChecker();
                checker.check(dbProps);

                if (EncryptionSecretKeyChecker.useEncryption()) {
                    StandardPBEStringEncryptor encryptor = EncryptionSecretKeyChecker.getEncryptor();
                    EncryptableProperties encrDbProps = new EncryptableProperties(encryptor);
                    encrDbProps.putAll(dbProps);
                    dbProps = encrDbProps;
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load db.properties", e);
            } finally {
                IOUtils.closeQuietly(is);
            }

            properties = dbProps;
            loaded = true;
        }

        return properties;
    }

    public synchronized static Properties setDbProperties(Properties props) throws IOException {
        if (loaded) {
            throw new IllegalStateException("DbProperties has already been loaded");
        }
        properties = wrapEncryption(props);
        loaded = true;
        return properties;
    }
}
