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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.properties.EncryptableProperties;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

/*
 * EncryptionSecretKeyChanger updates Management Secret Key / DB Secret Key or both.
 * DB secret key is validated against the key in db.properties
 * db.properties is updated with values encrypted using new MS secret key
 * DB data migrated using new DB secret key
 */
public class EncryptionSecretKeyChanger {

    private StandardPBEStringEncryptor oldEncryptor = new StandardPBEStringEncryptor();
    private StandardPBEStringEncryptor newEncryptor = new StandardPBEStringEncryptor();
    private static final String keyFile = "/etc/cloudstack/management/key";

    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        Iterator<String> iter = argsList.iterator();
        String oldMSKey = null;
        String oldDBKey = null;
        String newMSKey = null;
        String newDBKey = null;

        //Parse command-line args
        while (iter.hasNext()) {
            String arg = iter.next();
            // Old MS Key
            if (arg.equals("-m")) {
                oldMSKey = iter.next();
            }
            // Old DB Key
            if (arg.equals("-d")) {
                oldDBKey = iter.next();
            }
            // New MS Key
            if (arg.equals("-n")) {
                newMSKey = iter.next();
            }
            // New DB Key
            if (arg.equals("-e")) {
                newDBKey = iter.next();
            }
        }

        if (oldMSKey == null || oldDBKey == null) {
            System.out.println("Existing MS secret key or DB secret key is not provided");
            usage();
            return;
        }

        if (newMSKey == null && newDBKey == null) {
            System.out.println("New MS secret key and DB secret are both not provided");
            usage();
            return;
        }

        final File dbPropsFile = PropertiesUtil.findConfigFile("db.properties");
        final Properties dbProps;
        EncryptionSecretKeyChanger keyChanger = new EncryptionSecretKeyChanger();
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        keyChanger.initEncryptor(encryptor, oldMSKey);
        dbProps = new EncryptableProperties(encryptor);
        PropertiesConfiguration backupDBProps = null;

        System.out.println("Parsing db.properties file");
        try(FileInputStream db_prop_fstream = new FileInputStream(dbPropsFile);) {
            dbProps.load(db_prop_fstream);
            backupDBProps = new PropertiesConfiguration(dbPropsFile);
        } catch (FileNotFoundException e) {
            System.out.println("db.properties file not found while reading DB secret key" + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error while reading DB secret key from db.properties" + e.getMessage());
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

        String dbSecretKey = null;
        try {
            dbSecretKey = dbProps.getProperty("db.cloud.encrypt.secret");
        } catch (EncryptionOperationNotPossibleException e) {
            System.out.println("Failed to decrypt existing DB secret key from db.properties. " + e.getMessage());
            return;
        }

        if (!oldDBKey.equals(dbSecretKey)) {
            System.out.println("Incorrect MS Secret Key or DB Secret Key");
            return;
        }

        System.out.println("Secret key provided matched the key in db.properties");
        final String encryptionType = dbProps.getProperty("db.cloud.encryption.type");

        if (newMSKey == null) {
            System.out.println("No change in MS Key. Skipping migrating db.properties");
        } else {
            if (!keyChanger.migrateProperties(dbPropsFile, dbProps, newMSKey, newDBKey)) {
                System.out.println("Failed to update db.properties");
                return;
            } else {
                //db.properties updated successfully
                if (encryptionType.equals("file")) {
                    //update key file with new MS key
                    try (FileWriter fwriter = new FileWriter(keyFile);
                         BufferedWriter bwriter = new BufferedWriter(fwriter);)
                    {
                        bwriter.write(newMSKey);
                    } catch (IOException e) {
                        System.out.println("Failed to write new secret to file. Please update the file manually");
                    }
                }
            }
        }

        boolean success = false;
        if (newDBKey == null || newDBKey.equals(oldDBKey)) {
            System.out.println("No change in DB Secret Key. Skipping Data Migration");
        } else {
            EncryptionSecretKeyChecker.initEncryptorForMigration(oldMSKey);
            try {
                success = keyChanger.migrateData(oldDBKey, newDBKey);
            } catch (Exception e) {
                System.out.println("Error during data migration");
                e.printStackTrace();
                success = false;
            }
        }

        if (success) {
            System.out.println("Successfully updated secret key(s)");
        } else {
            System.out.println("Data Migration failed. Reverting db.properties");
            //revert db.properties
            try {
                backupDBProps.save();
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
            if (encryptionType.equals("file")) {
                //revert secret key in file
                try (FileWriter fwriter = new FileWriter(keyFile);
                     BufferedWriter bwriter = new BufferedWriter(fwriter);)
                {
                    bwriter.write(oldMSKey);
                } catch (IOException e) {
                    System.out.println("Failed to revert to old secret to file. Please update the file manually");
                }
            }
        }
    }

    private boolean migrateProperties(File dbPropsFile, Properties dbProps, String newMSKey, String newDBKey) {
        System.out.println("Migrating db.properties..");
        StandardPBEStringEncryptor msEncryptor = new StandardPBEStringEncryptor();
        ;
        initEncryptor(msEncryptor, newMSKey);

        try {
            PropertiesConfiguration newDBProps = new PropertiesConfiguration(dbPropsFile);
            if (newDBKey != null && !newDBKey.isEmpty()) {
                newDBProps.setProperty("db.cloud.encrypt.secret", "ENC(" + msEncryptor.encrypt(newDBKey) + ")");
            }
            String prop = dbProps.getProperty("db.cloud.password");
            if (prop != null && !prop.isEmpty()) {
                newDBProps.setProperty("db.cloud.password", "ENC(" + msEncryptor.encrypt(prop) + ")");
            }
            prop = dbProps.getProperty("db.usage.password");
            if (prop != null && !prop.isEmpty()) {
                newDBProps.setProperty("db.usage.password", "ENC(" + msEncryptor.encrypt(prop) + ")");
            }
            newDBProps.save(dbPropsFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        System.out.println("Migrating db.properties Done.");
        return true;
    }

    private boolean migrateData(String oldDBKey, String newDBKey) {
        System.out.println("Begin Data migration");
        initEncryptor(oldEncryptor, oldDBKey);
        initEncryptor(newEncryptor, newDBKey);
        System.out.println("Initialised Encryptors");

        TransactionLegacy txn = TransactionLegacy.open("Migrate");
        txn.start();
        try {
            Connection conn;
            try {
                conn = txn.getConnection();
            } catch (SQLException e) {
                throw new CloudRuntimeException("Unable to migrate encrypted data in the database", e);
            }

            migrateConfigValues(conn);
            migrateHostDetails(conn);
            migrateVNCPassword(conn);
            migrateUserCredentials(conn);

            txn.commit();
        } finally {
            txn.close();
        }
        System.out.println("End Data migration");
        return true;
    }

    private void initEncryptor(StandardPBEStringEncryptor encryptor, String secretKey) {
        encryptor.setAlgorithm("PBEWithMD5AndDES");
        SimpleStringPBEConfig stringConfig = new SimpleStringPBEConfig();
        stringConfig.setPassword(secretKey);
        encryptor.setConfig(stringConfig);
    }

    private String migrateValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String decryptVal = oldEncryptor.decrypt(value);
        return newEncryptor.encrypt(decryptVal);
    }

    private void migrateConfigValues(Connection conn) {
        System.out.println("Begin migrate config values");
        try(PreparedStatement select_pstmt = conn.prepareStatement("select name, value from configuration where category in ('Hidden', 'Secure')");
            ResultSet rs = select_pstmt.executeQuery();
            PreparedStatement update_pstmt = conn.prepareStatement("update configuration set value=? where name=?");
        ) {
            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                if (value == null || value.isEmpty()) {
                    continue;
                }
                String encryptedValue = migrateValue(value);
                update_pstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                update_pstmt.setString(2, name);
                update_pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update configuration values ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable to update configuration values ", e);
        }
        System.out.println("End migrate config values");
    }

    private void migrateHostDetails(Connection conn) {
        System.out.println("Begin migrate host details");

        try( PreparedStatement sel_pstmt = conn.prepareStatement("select id, value from host_details where name = 'password'");
        ResultSet rs = sel_pstmt.executeQuery();
        PreparedStatement pstmt = conn.prepareStatement("update host_details set value=? where id=?");
        ) {
            while (rs.next()) {
                long id = rs.getLong(1);
                String value = rs.getString(2);
                if (value == null || value.isEmpty()) {
                    continue;
                }
                String encryptedValue = migrateValue(value);
                pstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable update host_details values ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable update host_details values ", e);
        }
        System.out.println("End migrate host details");
    }

    private void migrateVNCPassword(Connection conn) {
        System.out.println("Begin migrate VNC password");
        try(PreparedStatement  select_pstmt = conn.prepareStatement("select id, vnc_password from vm_instance");
        ResultSet rs = select_pstmt.executeQuery();
        PreparedStatement pstmt = conn.prepareStatement("update vm_instance set vnc_password=? where id=?");
        ) {
            while (rs.next()) {
                long id = rs.getLong(1);
                String value = rs.getString(2);
                if (value == null || value.isEmpty()) {
                    continue;
                }
                String encryptedValue = migrateValue(value);

                pstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable update vm_instance vnc_password ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable update vm_instance vnc_password ", e);
        }
        System.out.println("End migrate VNC password");
    }

    private void migrateUserCredentials(Connection conn) {
        System.out.println("Begin migrate user credentials");
        try(PreparedStatement select_pstmt = conn.prepareStatement("select id, secret_key from user");
        ResultSet rs = select_pstmt.executeQuery();
        PreparedStatement pstmt = conn.prepareStatement("update user set secret_key=? where id=?");
        ) {
            while (rs.next()) {
                long id = rs.getLong(1);
                String secretKey = rs.getString(2);
                if (secretKey == null || secretKey.isEmpty()) {
                    continue;
                }
                String encryptedSecretKey = migrateValue(secretKey);
                pstmt.setBytes(1, encryptedSecretKey.getBytes("UTF-8"));
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable update user secret key ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable update user secret key ", e);
        }
        System.out.println("End migrate user credentials");
    }

    private static void usage() {
        System.out.println("Usage: \tEncryptionSecretKeyChanger \n" + "\t\t-m <Mgmt Secret Key> \n" + "\t\t-d <DB Secret Key> \n" + "\t\t-n [New Mgmt Secret Key] \n"
            + "\t\t-e [New DB Secret Key]");
    }
}
