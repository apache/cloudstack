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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

import com.cloud.utils.db.DbProperties;
import com.cloud.utils.exception.CloudRuntimeException;

public class EncryptionSecretKeyChecker {

    private static final Logger s_logger = Logger.getLogger(EncryptionSecretKeyChecker.class);

    // Two possible locations with the new packaging naming
    private static final String s_altKeyFile = "key";
    private static final String s_keyFile = "key";
    private static final String s_envKey = "CLOUD_SECRET_KEY";
    private static StandardPBEStringEncryptor s_encryptor = new StandardPBEStringEncryptor();
    private static boolean s_useEncryption = false;

    @PostConstruct
    public void init() {
        /* This will call DbProperties, which will call this to initialize the encryption. Yep,
         * round about and annoying */
        DbProperties.getDbProperties();
    }

    public void check(Properties dbProps) throws IOException {
        String encryptionType = dbProps.getProperty("db.cloud.encryption.type");

        s_logger.debug("Encryption Type: " + encryptionType);

        if (encryptionType == null || encryptionType.equals("none")) {
            return;
        }

        if (s_useEncryption) {
            s_logger.warn("Encryption already enabled, is check() called twice?");
            return;
        }

        s_encryptor.setAlgorithm("PBEWithMD5AndDES");
        String secretKey = null;

        SimpleStringPBEConfig stringConfig = new SimpleStringPBEConfig();

        if (encryptionType.equals("file")) {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(s_keyFile);
            if (is == null) {
              is = this.getClass().getClassLoader().getResourceAsStream(s_altKeyFile);
            }
            if(is == null) {  //This is means we are not able to load key file from the classpath.
              throw new CloudRuntimeException(s_keyFile + " File containing secret key not found in the classpath: ");
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(is));) {
                secretKey = in.readLine();
                //Check for null or empty secret key
            } catch (IOException e) {
                throw new CloudRuntimeException("Error while reading secret key from: " + s_keyFile, e);
            }

            if (secretKey == null || secretKey.isEmpty()) {
                throw new CloudRuntimeException("Secret key is null or empty in file " + s_keyFile);
            }

        } else if (encryptionType.equals("env")) {
            secretKey = System.getenv(s_envKey);
            if (secretKey == null || secretKey.isEmpty()) {
                throw new CloudRuntimeException("Environment variable " + s_envKey + " is not set or empty");
            }
        } else if (encryptionType.equals("web")) {
            int port = 8097;
            try (ServerSocket serverSocket = new ServerSocket(port);) {
                s_logger.info("Waiting for admin to send secret key on port " + port);
                try (
                        Socket clientSocket = serverSocket.accept();
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    ) {
                    String inputLine;
                    if ((inputLine = in.readLine()) != null) {
                        secretKey = inputLine;
                    }
                } catch (IOException e) {
                    throw new CloudRuntimeException("Accept failed on " + port);
                }
            } catch (IOException ioex) {
                throw new CloudRuntimeException("Error initializing secret key reciever", ioex);
            }
        } else {
            throw new CloudRuntimeException("Invalid encryption type: " + encryptionType);
        }

        stringConfig.setPassword(secretKey);
        s_encryptor.setConfig(stringConfig);
        s_useEncryption = true;
    }

    public static StandardPBEStringEncryptor getEncryptor() {
        return s_encryptor;
    }

    public static boolean useEncryption() {
        return s_useEncryption;
    }

    //Initialize encryptor for migration during secret key change
    public static void initEncryptorForMigration(String secretKey) {
        s_encryptor.setAlgorithm("PBEWithMD5AndDES");
        SimpleStringPBEConfig stringConfig = new SimpleStringPBEConfig();
        stringConfig.setPassword(secretKey);
        s_encryptor.setConfig(stringConfig);
        s_useEncryption = true;
    }
}
