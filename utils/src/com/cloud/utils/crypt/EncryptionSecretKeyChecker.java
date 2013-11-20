// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.crypt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

import com.cloud.utils.db.DbProperties;
import com.cloud.utils.exception.CloudRuntimeException;

public class EncryptionSecretKeyChecker {

    private static final Logger s_logger = Logger.getLogger(EncryptionSecretKeyChecker.class);

    // Two possible locations with the new packaging naming
    private static final String s_altKeyFile = "/etc/cloudstack/management/key";
    private static final String s_keyFile = "/etc/cloudstack/management/key";
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
            File keyFile = new File(s_keyFile);
            if (!keyFile.exists()) {
                keyFile = new File(s_altKeyFile);
            }
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(keyFile));
                secretKey = in.readLine();
                //Check for null or empty secret key
            } catch (FileNotFoundException e) {
                throw new CloudRuntimeException("File containing secret key not found: " + s_keyFile, e);
            } catch (IOException e) {
                throw new CloudRuntimeException("Error while reading secret key from: " + s_keyFile, e);
            } finally {
                IOUtils.closeQuietly(in);
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
            ServerSocket serverSocket = null;
            int port = 8097;
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException ioex) {
                throw new CloudRuntimeException("Error initializing secret key reciever", ioex);
            }
            s_logger.info("Waiting for admin to send secret key on port " + port);
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                throw new CloudRuntimeException("Accept failed on " + port);
            }
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;
            if ((inputLine = in.readLine()) != null) {
                secretKey = inputLine;
            }
            out.close();
            in.close();
            clientSocket.close();
            serverSocket.close();
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
