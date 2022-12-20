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
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.ReflectUtil;
import com.cloud.utils.db.Encrypt;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.persistence.Column;
import javax.persistence.Table;

/*
 * EncryptionSecretKeyChanger updates Management Secret Key / DB Secret Key or both.
 * DB secret key is validated against the key in db.properties
 * db.properties is updated with values encrypted using new MS secret key
 * server.properties is updated with values encrypted using new MS secret key
 * DB data migrated using new DB secret key
 */
public class EncryptionSecretKeyChanger {

    private CloudStackEncryptor oldEncryptor;
    private CloudStackEncryptor newEncryptor;
    private static final String keyFile = "/etc/cloudstack/management/key";
    private static final String envNewManagementKey = "CLOUD_SECRET_KEY_NEW";
    private final Gson gson = new Gson();

    private static final Options options = initializeOptions();
    private static final HelpFormatter helper = initializeHelper();
    private static final String cmdLineSyntax = "cloudstack-migrate-databases";
    private static final int width = 100;
    private static final String header = "Options:";
    private static final String footer = "\nExamples:\n" +
            "  " + cmdLineSyntax + " -m password -d password -n newmgmtkey -v V2 \n" +
            "       Migrate cloudstack properties (db.properties and server.properties) \n" +
            "       with new management key and encryptor V2.\n" +
            "  " + cmdLineSyntax + " -m password -d password -n newmgmtkey -e newdbkey \n" +
            "       Migrate cloudstack properties and databases with new management key and database secret key.\n" +
            "  " + cmdLineSyntax + " -m password -d password -n newmgmtkey -e newdbkey -s -v V2 \n" +
            "       Migrate cloudstack properties with new keys and encryptor V2, but skip database migration.\n" +
            "  " + cmdLineSyntax + " -m password -d password -l -f \n" +
            "       Migrate cloudstack properties with new management key (load from $CLOUD_SECRET_KEY_NEW),\n" +
            "       and migrate database with old db key.\n" +
            "\nReturn codes:\n" +
            "  0 - Succeed to change keys and/or migrate databases \n" +
            "  1 - Fail to parse the command line arguments \n" +
            "  2 - Fail to validate parameters \n" +
            "  3 - Fail to migrate database";
    private static final String oldMSKeyOption = "oldMSKey";
    private static final String oldDBKeyOption = "oldDBKey";
    private static final String newMSKeyOption = "newMSKey";
    private static final String newDBKeyOption = "newDBKey";
    private static final String encryptorVersionOption = "version";
    private static final String loadNewMsKeyFromEnvFlag = "load-new-management-key-from-env";
    private static final String forceDatabaseMigrationFlag = "force-database-migration";
    private static final String skipDatabaseMigrationFlag = "skip-database-migration";
    private static final String helpFlag = "help";

    public static void main(String[] args) {
        if (args.length == 0 || StringUtils.equalsAny(args[0], "-h", "--help")) {
            helper.printHelp(width, cmdLineSyntax, header, options, footer, true);
            System.exit(0);
        }

        CommandLine cmdLine = null;
        CommandLineParser parser = new DefaultParser();
        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            helper.printHelp(width, cmdLineSyntax, header, options, footer, true);
            System.exit(1);
        }

        String oldMSKey = cmdLine.getOptionValue(oldMSKeyOption);
        String oldDBKey = cmdLine.getOptionValue(oldDBKeyOption);
        String newMSKey = cmdLine.getOptionValue(newMSKeyOption);
        String newDBKey = cmdLine.getOptionValue(newDBKeyOption);
        String newEncryptorVersion = cmdLine.getOptionValue(encryptorVersionOption);
        boolean loadNewMsKeyFromEnv = cmdLine.hasOption(loadNewMsKeyFromEnvFlag);
        boolean forced = cmdLine.hasOption(forceDatabaseMigrationFlag);
        boolean skipped = cmdLine.hasOption(skipDatabaseMigrationFlag);

        if (!validateParameters(oldMSKey, oldDBKey, newMSKey, newDBKey, newEncryptorVersion, loadNewMsKeyFromEnv)) {
            helper.printHelp(width, cmdLineSyntax, header, options, footer, true);
            System.exit(2);
        }

        System.out.println("Started database migration at " + new Date());
        if (!migrateEverything(oldMSKey, oldDBKey, newMSKey, newDBKey, newEncryptorVersion, loadNewMsKeyFromEnv, forced, skipped)) {
            System.out.println("Got error during database migration at " + new Date());
            System.exit(3);
        }
        System.out.println("Finished database migration at " + new Date());
    }

    private static Options initializeOptions() {
        Options options = new Options();

        Option oldMSKey = Option.builder("m").longOpt(oldMSKeyOption).argName(oldMSKeyOption).required(true).hasArg().desc("(required) Current Mgmt Secret Key").build();
        Option oldDBKey = Option.builder("d").longOpt(oldDBKeyOption).argName(oldDBKeyOption).required(true).hasArg().desc("(required) Current DB Secret Key").build();
        Option newMSKey = Option.builder("n").longOpt(newMSKeyOption).argName(newMSKeyOption).required(false).hasArg().desc("New Mgmt Secret Key").build();
        Option newDBKey = Option.builder("e").longOpt(newDBKeyOption).argName(newDBKeyOption).required(false).hasArg().desc("New DB Secret Key").build();
        Option encryptorVersion = Option.builder("v").longOpt(encryptorVersionOption).argName(encryptorVersionOption).required(false).hasArg().desc("New DB Encryptor Version. Options are V1, V2.").build();

        Option loadNewMsKeyFromEnv = Option.builder("l").longOpt(loadNewMsKeyFromEnvFlag).desc("Load new management key from environment variable " + envNewManagementKey).build();
        Option forceDatabaseMigration = Option.builder("f").longOpt(forceDatabaseMigrationFlag).desc("Force database migration even if DB Secret key is not changed").build();
        Option skipDatabaseMigration = Option.builder("s").longOpt(skipDatabaseMigrationFlag).desc("Skip database migration even if DB Secret key is changed").build();
        Option help = Option.builder("h").longOpt(helpFlag).desc("Show help message").build();

        options.addOption(oldMSKey);
        options.addOption(oldDBKey);
        options.addOption(newMSKey);
        options.addOption(newDBKey);
        options.addOption(encryptorVersion);
        options.addOption(loadNewMsKeyFromEnv);
        options.addOption(forceDatabaseMigration);
        options.addOption(skipDatabaseMigration);
        options.addOption(help);

        return options;
    }

    private static HelpFormatter initializeHelper() {
        HelpFormatter helper = new HelpFormatter();

        helper.setOptionComparator((o1, o2) -> {
            if (o1.isRequired() && !o2.isRequired()) {
                return -1;
            }
            if (!o1.isRequired() && o2.isRequired()) {
                return 1;
            }
            if (o1.hasArg() && !o2.hasArg()) {
                return -1;
            }
            if (!o1.hasArg() && o2.hasArg()) {
                return 1;
            }
            return o1.getOpt().compareTo(o2.getOpt());
        });

        return helper;
    }

    private static boolean validateParameters(String oldMSKey, String oldDBKey, String newMSKey, String newDBKey,
                                              String newEncryptorVersion, boolean loadNewMsKeyFromEnv) {

        if (oldMSKey == null || oldDBKey == null) {
            System.out.println("Existing Management secret key or DB secret key is not provided");
            return false;
        }

        if (loadNewMsKeyFromEnv) {
            if (StringUtils.isNotEmpty(newMSKey)) {
                System.out.println("The new management key has already been set. Please check if it is set twice.");
                return false;
            }
            newMSKey = System.getenv(envNewManagementKey);
            if (StringUtils.isEmpty(newMSKey)) {
                System.out.println("Environment variable " + envNewManagementKey + " is not set or empty");
                return false;
            }
        }

        if (newMSKey == null && newDBKey == null) {
            System.out.println("New Management secret key and DB secret are both not provided");
            return false;
        }

        if (newEncryptorVersion != null) {
            try {
                CloudStackEncryptor.EncryptorVersion.fromString(newEncryptorVersion);
            } catch (CloudRuntimeException ex) {
                System.out.println(ex.getMessage());
                return false;
            }
        }

        return true;
    }

    private static boolean migrateEverything(String oldMSKey, String oldDBKey, String newMSKey, String newDBKey,
                                             String newEncryptorVersion, boolean loadNewMsKeyFromEnv,
                                             boolean forced, boolean skipped) {

        final File dbPropsFile = PropertiesUtil.findConfigFile("db.properties");
        final Properties dbProps = new Properties();
        EncryptionSecretKeyChanger keyChanger = new EncryptionSecretKeyChanger();
        PropertiesConfiguration backupDBProps = null;

        System.out.println("Parsing db.properties file");
        try(FileInputStream db_prop_fstream = new FileInputStream(dbPropsFile)) {
            dbProps.load(db_prop_fstream);
            backupDBProps = new PropertiesConfiguration(dbPropsFile);
        } catch (FileNotFoundException e) {
            System.out.println("db.properties file not found while reading DB secret key: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.out.println("Error while reading DB secret key from db.properties: " + e.getMessage());
            return false;
        } catch (ConfigurationException e) {
            System.out.println("Error while getting configurations from db.properties: " + e.getMessage());
            return false;
        }

        try {
            EncryptionSecretKeyChecker.initEncryptor(oldMSKey);
            EncryptionSecretKeyChecker.decryptAnyProperties(dbProps);
        } catch (CloudRuntimeException e) {
            System.out.println("Error: Incorrect Management Secret Key");
            return false;
        }
        String dbSecretKey = dbProps.getProperty("db.cloud.encrypt.secret");

        if (!oldDBKey.equals(dbSecretKey)) {
            System.out.println("Error: Incorrect DB Secret Key");
            return false;
        }

        System.out.println("DB Secret key provided matched the key in db.properties");
        final String encryptionType = dbProps.getProperty("db.cloud.encryption.type");
        final String oldEncryptorVersion = dbProps.getProperty("db.cloud.encryptor.version");

        // validate old and new encryptor versions
        try {
            CloudStackEncryptor.EncryptorVersion.fromString(oldEncryptorVersion);
            CloudStackEncryptor.EncryptorVersion.fromString(newEncryptorVersion);
        } catch (CloudRuntimeException ex) {
            System.out.println(ex.getMessage());
            return false;
        }

        if (loadNewMsKeyFromEnv) {
            newMSKey = System.getenv(envNewManagementKey);
        }
        if (newMSKey == null) {
            newMSKey = oldMSKey;
            System.out.println("New Management Secret Key is not provided. Skipping migrating db.properties");
        } else {
            if (newEncryptorVersion == null && oldEncryptorVersion != null) {
                newEncryptorVersion = oldEncryptorVersion;
            }
            if (!keyChanger.migrateProperties(dbPropsFile, dbProps, newMSKey, (newDBKey != null ? newDBKey : oldDBKey), newEncryptorVersion)) {
                System.out.println("Failed to update db.properties");
                return false;
            }
            if (!keyChanger.migrateServerProperties(newMSKey)) {
                System.out.println("Failed to update server.properties");
                return false;
            }
            //db.properties updated successfully
            if (encryptionType.equals("file")) {
                //update key file with new MS key
                try (FileWriter fwriter = new FileWriter(keyFile);
                     BufferedWriter bwriter = new BufferedWriter(fwriter))
                {
                    bwriter.write(newMSKey);
                } catch (IOException e) {
                    System.out.printf("Please update the file %s manually. Failed to write new secret to file with error %s%n", keyFile, e.getMessage());
                    return false;
                }
            }
        }

        boolean success = false;
        if ((newDBKey == null || newDBKey.equals(oldDBKey)) && !forced) {
            System.out.println("No change in DB Secret Key. Skipping Data Migration");
            return true;
        } else if (skipped) {
            System.out.println("Skipping Data Migration as '-s' or '--skip-database-migration' is passed");
            return true;
        } else {
            EncryptionSecretKeyChecker.initEncryptor(newMSKey);
            try {
                success = keyChanger.migrateData(oldDBKey, newDBKey != null ? newDBKey : oldDBKey, oldEncryptorVersion,
                        newEncryptorVersion);
            } catch (Exception e) {
                System.out.println("Error during data migration");
                e.printStackTrace();
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
                     BufferedWriter bwriter = new BufferedWriter(fwriter))
                {
                    bwriter.write(oldMSKey);
                } catch (IOException e) {
                    System.out.println("Failed to revert to old secret to file. Please update the file manually");
                }
            }
            return false;
        }

        return true;
    }

    private boolean migrateServerProperties(String newMSKey) {
        System.out.println("Migrating server.properties..");
        final File serverPropsFile = PropertiesUtil.findConfigFile("server.properties");
        final Properties serverProps = new Properties();
        PropertiesConfiguration newServerProps;

        try(FileInputStream server_prop_fstream = new FileInputStream(serverPropsFile)) {
            serverProps.load(server_prop_fstream);
            newServerProps = new PropertiesConfiguration(serverPropsFile);
        } catch (FileNotFoundException e) {
            System.out.println("server.properties file not found: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.out.println("Error while reading server.properties: " + e.getMessage());
            return false;
        } catch (ConfigurationException e) {
            System.out.println("Error while getting configurations from server.properties: " + e.getMessage());
            return false;
        }

        try {
            EncryptionSecretKeyChecker.decryptAnyProperties(serverProps);
        } catch (CloudRuntimeException e) {
            System.out.println(e.getMessage());
            return false;
        }

        CloudStackEncryptor msEncryptor = new CloudStackEncryptor(newMSKey, null, getClass());

        try {
            String encryptionType = serverProps.getProperty("password.encryption.type");
            if (StringUtils.isEmpty(encryptionType) || encryptionType.equalsIgnoreCase("none")) {
                System.out.println("Skipping server.properties as password.encryption.type is " + encryptionType);
                return true;
            }
            String keystorePassword = serverProps.getProperty("https.keystore.password");
            if (StringUtils.isNotEmpty(keystorePassword)) {
                newServerProps.setProperty("https.keystore.password", "ENC(" + msEncryptor.encrypt(keystorePassword) + ")");
            }
            newServerProps.save(serverPropsFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        System.out.println("Migrating server.properties Done.");
        return true;
    }

    private boolean migrateProperties(File dbPropsFile, Properties dbProps, String newMSKey, String newDBKey, String newEncryptorVersion) {
        System.out.println("Migrating db.properties..");
        CloudStackEncryptor msEncryptor = new CloudStackEncryptor(newMSKey, null, getClass());

        try {
            PropertiesConfiguration newDBProps = new PropertiesConfiguration(dbPropsFile);
            if (StringUtils.isNotEmpty(newDBKey)) {
                newDBProps.setProperty("db.cloud.encrypt.secret", "ENC(" + msEncryptor.encrypt(newDBKey) + ")");
            }
            String prop = dbProps.getProperty("db.cloud.password");
            if (StringUtils.isNotEmpty(prop)) {
                newDBProps.setProperty("db.cloud.password", "ENC(" + msEncryptor.encrypt(prop) + ")");
            }
            prop = dbProps.getProperty("db.usage.password");
            if (StringUtils.isNotEmpty(prop)) {
                newDBProps.setProperty("db.usage.password", "ENC(" + msEncryptor.encrypt(prop) + ")");
            }
            if (newEncryptorVersion != null) {
                newDBProps.setProperty("db.cloud.encryptor.version", newEncryptorVersion);
            }
            newDBProps.save(dbPropsFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        System.out.println("Migrating db.properties Done.");
        return true;
    }

    private boolean migrateData(String oldDBKey, String newDBKey, String oldEncryptorVersion, String newEncryptorVersion) throws SQLException {
        System.out.println("Begin Data migration");
        oldEncryptor = new CloudStackEncryptor(oldDBKey, oldEncryptorVersion, getClass());
        newEncryptor = new CloudStackEncryptor(newDBKey, newEncryptorVersion, getClass());
        System.out.println("Initialised Encryptors");

        TransactionLegacy txn = TransactionLegacy.open("Migrate");
        txn.start();
        try {
            Connection conn;
            try {
                conn = txn.getConnection();
            } catch (SQLException e) {
                System.out.println("Unable to migrate encrypted data in the database due to: " + e.getMessage());
                throw new CloudRuntimeException("Unable to migrate encrypted data in the database", e);
            }

            // migrate values in configuration
            migrateConfigValues(conn);

            // migrate resource details values
            migrateHostDetails(conn);
            migrateClusterDetails(conn);
            migrateImageStoreDetails(conn);
            migrateStoragePoolDetails(conn);
            migrateScaleIOStoragePoolDetails(conn);
            migrateUserVmDetails(conn);

            // migrate other encrypted fields
            migrateTemplateDeployAsIsDetails(conn);
            migrateImageStoreUrlForCifs(conn);
            migrateStoragePoolPathForSMB(conn);

            // migrate columns with annotation @Encrypt
            migrateEncryptedTableColumns(conn);

            txn.commit();
        } finally {
            txn.close();
        }
        System.out.println("End Data migration");
        return true;
    }

    protected String migrateValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        String decryptVal = oldEncryptor.decrypt(value);
        return newEncryptor.encrypt(decryptVal);
    }

    protected String migrateUrlOrPath(String urlOrPath) {
        if (StringUtils.isEmpty(urlOrPath)) {
            return urlOrPath;
        }
        String[] properties = urlOrPath.split("&");
        for (String property : properties) {
            if (property.startsWith("password=")) {
                String password = property.substring(property.indexOf("=") + 1);
                password = migrateValue(password);
                return urlOrPath.replaceAll(property, "password=" + password);
            }
        }
        return urlOrPath;
    }

    private void migrateConfigValues(Connection conn) {
        System.out.println("Begin migrate config values");

        String tableName = "configuration";
        String selectSql = "SELECT name, value FROM configuration WHERE category IN ('Hidden', 'Secure')";
        String updateSql = "UPDATE configuration SET value=? WHERE name=?";
        migrateValueAndUpdateDatabaseByName(conn, tableName, selectSql, updateSql);

        System.out.println("End migrate config values");
    }


    private void migrateValueAndUpdateDatabaseById(Connection conn, String tableName, String selectSql, String updateSql, boolean isUrlOrPath) {
        try( PreparedStatement select_pstmt = conn.prepareStatement(selectSql);
             ResultSet rs = select_pstmt.executeQuery();
             PreparedStatement update_pstmt = conn.prepareStatement(updateSql)
        ) {
            while (rs.next()) {
                long id = rs.getLong(1);
                String value = rs.getString(2);
                if (StringUtils.isEmpty(value)) {
                    continue;
                }
                String encryptedValue = isUrlOrPath ? migrateUrlOrPath(value) : migrateValue(value);
                update_pstmt.setBytes(1, encryptedValue.getBytes(StandardCharsets.UTF_8));
                update_pstmt.setLong(2, id);
                update_pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throwCloudRuntimeException(String.format("Unable to update %s values", tableName), e);
        }
    }

    private void migrateValueAndUpdateDatabaseByName(Connection conn, String tableName, String selectSql, String updateSql) {
        try(PreparedStatement select_pstmt = conn.prepareStatement(selectSql);
            ResultSet rs = select_pstmt.executeQuery();
            PreparedStatement update_pstmt = conn.prepareStatement(updateSql)
        ) {
            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                if (StringUtils.isEmpty(value)) {
                    continue;
                }
                String encryptedValue = migrateValue(value);
                update_pstmt.setBytes(1, encryptedValue.getBytes(StandardCharsets.UTF_8));
                update_pstmt.setString(2, name);
                update_pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throwCloudRuntimeException(String.format("Unable to update %s values", tableName), e);
        }
    }

    private void migrateHostDetails(Connection conn) {
        System.out.println("Begin migrate host details");
        migrateDetails(conn, "host_details", "password");
        System.out.println("End migrate host details");
    }

    private void migrateClusterDetails(Connection conn) {
        System.out.println("Begin migrate cluster details");
        migrateDetails(conn, "cluster_details", "password");
        System.out.println("End migrate cluster details");
    }

    private void migrateImageStoreDetails(Connection conn) {
        System.out.println("Begin migrate image store details");
        migrateDetails(conn, "image_store_details", "key", "secretkey");
        System.out.println("End migrate image store details");
    }

    private void migrateStoragePoolDetails(Connection conn) {
        System.out.println("Begin migrate storage pool details");
        migrateDetails(conn, "storage_pool_details", "password");
        System.out.println("End migrate storage pool details");
    }

    private void migrateScaleIOStoragePoolDetails(Connection conn) {
        System.out.println("Begin migrate storage pool details for ScaleIO");
        migrateDetails(conn, "storage_pool_details", "powerflex.gw.username", "powerflex.gw.password");
        System.out.println("End migrate storage pool details for ScaleIO");
    }

    private void migrateUserVmDetails(Connection conn) {
        System.out.println("Begin migrate user vm details");
        migrateDetails(conn, "user_vm_details", "password");
        System.out.println("End migrate user vm details");
    }

    private void migrateDetails(Connection conn, String tableName, String... detailNames) {
        String convertedDetails = Arrays.stream(detailNames).map(detail -> "'" + detail + "'").collect(Collectors.joining(", "));
        String selectSql = String.format("SELECT id, value FROM %s WHERE name IN (%s)", tableName, convertedDetails);
        String updateSql = String.format("UPDATE %s SET value=? WHERE id=?", tableName);
        migrateValueAndUpdateDatabaseById(conn, tableName, selectSql, updateSql, false);
    }

    private void migrateTemplateDeployAsIsDetails(Connection conn) throws SQLException {
        System.out.println("Begin migrate user vm deploy_as_is details");
        if (!ifTableExists(conn.getMetaData(), "user_vm_deploy_as_is_details")) {
            System.out.printf("Skipped as table %s does not exist\n", "user_vm_deploy_as_is_details");
            return;
        }
        if (!ifTableExists(conn.getMetaData(), "template_deploy_as_is_details")) {
            System.out.printf("Skipped as table %s does not exist\n", "template_deploy_as_is_details");
            return;
        }
        String sql_template_deploy_as_is_details = "SELECT template_deploy_as_is_details.value " +
                "FROM template_deploy_as_is_details JOIN vm_instance " +
                "WHERE template_deploy_as_is_details.template_id = vm_instance.vm_template_id " +
                "vm_instance.id = %s AND template_deploy_as_is_details.name = '%s' LIMIT 1";
        try (PreparedStatement sel_pstmt = conn.prepareStatement("SELECT id, vm_id, name, value FROM user_vm_deploy_as_is_details");
             ResultSet rs = sel_pstmt.executeQuery();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE user_vm_deploy_as_is_details SET value=? WHERE id=?")
        ) {
            while (rs.next()) {
                long id = rs.getLong(1);
                long vmId = rs.getLong(2);
                String name = rs.getString(3);
                String value = rs.getString(4);
                if (StringUtils.isEmpty(value)) {
                    continue;
                }
                String key = name.startsWith("property-") ? name : "property-" + name;

                try (PreparedStatement pstmt_template_deploy_as_is = conn.prepareStatement(String.format(sql_template_deploy_as_is_details, vmId, key));
                    ResultSet rs_template_deploy_as_is = pstmt_template_deploy_as_is.executeQuery()) {
                    if (rs_template_deploy_as_is.next()) {
                        String template_deploy_as_is_detail_value = rs_template_deploy_as_is.getString(1);
                        OVFPropertyTO property = gson.fromJson(template_deploy_as_is_detail_value, OVFPropertyTO.class);
                        if (property != null && property.isPassword()) {
                            String encryptedValue = migrateValue(value);
                            pstmt.setBytes(1, encryptedValue.getBytes(StandardCharsets.UTF_8));
                            pstmt.setLong(2, id);
                            pstmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException | JsonSyntaxException e) {
            throwCloudRuntimeException("Unable to update user_vm_deploy_as_is_details values", e);
        }
        System.out.println("End migrate user vm deploy_as_is details");
    }

    private void migrateImageStoreUrlForCifs(Connection conn) {
        System.out.println("Begin migrate image store url if protocol is cifs");

        String tableName = "image_store";
        String fieldName = "url";
        if (getCountOfTable(conn, tableName) == 0) {
            System.out.printf("Skipped table %s as there is no data in the table\n", tableName);
            return;
        }
        String selectSql = String.format("SELECT id, `%s` FROM %s WHERE protocol = 'cifs'", fieldName, tableName);
        String updateSql = String.format("UPDATE %s SET `%s`=? WHERE id=?", tableName, fieldName);
        migrateValueAndUpdateDatabaseById(conn, tableName, selectSql, updateSql, true);

        System.out.println("End migrate image store url if protocol is cifs");
    }

    private void migrateStoragePoolPathForSMB(Connection conn) {
        System.out.println("Begin migrate storage pool path if type is SMB");

        String tableName = "storage_pool";
        String fieldName = "path";
        if (getCountOfTable(conn, tableName) == 0) {
            System.out.printf("Skipped table %s as there is no data in the table\n", tableName);
            return;
        }
        String selectSql = String.format("SELECT id, `%s` FROM %s WHERE pool_type = 'SMB'", fieldName, tableName);
        String updateSql = String.format("UPDATE %s SET `%s`=? WHERE id=?", tableName, fieldName);
        migrateValueAndUpdateDatabaseById(conn, tableName, selectSql, updateSql, true);

        System.out.println("End migrate storage pool path if type is SMB");
    }

    private void migrateDatabaseField(Connection conn, String tableName, String fieldName) {
        System.out.printf("Begin migrate table %s field %s\n", tableName, fieldName);

        String selectSql = String.format("SELECT id, `%s` FROM %s", fieldName, tableName);
        String updateSql = String.format("UPDATE %s SET `%s`=? WHERE id=?", tableName, fieldName);
        migrateValueAndUpdateDatabaseById(conn, tableName, selectSql, updateSql, false);

        System.out.printf("Done migrating database field %s.%s\n", tableName, fieldName);
    }

    protected static Map<String, Set<String>> findEncryptedTableColumns() {
        Map<String, Set<String>> tableCols = new HashMap<>();
        Set<Class<?>> vos = ReflectUtil.getClassesWithAnnotation(Table.class, new String[]{"com", "org"});
        vos.forEach( vo -> {
            Table tableAnnotation = vo.getAnnotation(Table.class);
            if (tableAnnotation == null || (tableAnnotation.name() != null && tableAnnotation.name().endsWith("_view"))) {
                return;
            }
            for (Field field : vo.getDeclaredFields()) {
                if (field.isAnnotationPresent(Encrypt.class)) {
                    Set<String> encryptedColumns = tableCols.getOrDefault(tableAnnotation.name(), new HashSet<>());
                    String columnName = field.getName();
                    if (field.isAnnotationPresent(Column.class)) {
                        Column columnAnnotation = field.getAnnotation(Column.class);
                        columnName = columnAnnotation.name();
                    }
                    encryptedColumns.add(columnName);
                    tableCols.put(tableAnnotation.name(), encryptedColumns);
                }
            }
        });
        return tableCols;
    }

    private void migrateEncryptedTableColumns(Connection conn) throws SQLException {
        Map<String, Set<String>> encryptedTableCols = findEncryptedTableColumns();
        DatabaseMetaData metadata = conn.getMetaData();
        encryptedTableCols.forEach((table, columns) -> {
            if (!ifTableExists(metadata, table)) {
                System.out.printf("Skipped table %s as it does not exist\n", table);
                return;
            }
            if (getCountOfTable(conn, table) == 0) {
                System.out.printf("Skipped table %s as there is no data in the table\n", table);
                return;
            }
            columns.forEach(column -> {
                if (!ifTableColumnExists(metadata, table, column)) {
                    System.out.printf("Skipped column %s in table %s as it does not exist\n", column, table);
                    return;
                }
                migrateDatabaseField(conn, table, column);
            });
        });
    }

    private boolean ifTableExists(DatabaseMetaData metadata, String table) {
        try {
            ResultSet rs = metadata.getTables(null, null, table, null);
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            throwCloudRuntimeException(String.format("Unable to get table %s", table), e);
        }
        return false;
    }

    private boolean ifTableColumnExists(DatabaseMetaData metadata, String table, String column) {
        try {
            ResultSet rs = metadata.getColumns(null, null, table, column);
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            throwCloudRuntimeException(String.format("Unable to get column %s in table %s", column, table), e);
        }
        return false;
    }

    private int getCountOfTable(Connection conn, String table) {
        try (PreparedStatement pstmt = conn.prepareStatement(String.format("SELECT count(*) FROM %s", table));
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throwCloudRuntimeException(String.format("Unable to get count of records in table %s", table), e);
        }
        return 0;
    }

    private static void throwCloudRuntimeException(String msg, Exception e) {
        System.out.println(msg + " due to: " + e.getMessage());
        throw new CloudRuntimeException(msg, e);
    }
}
