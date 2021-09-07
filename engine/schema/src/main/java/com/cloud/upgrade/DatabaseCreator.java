/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.upgrade;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.SystemIntegrityChecker;
import com.cloud.utils.db.ScriptRunner;
import com.cloud.utils.db.TransactionLegacy;

// Creates the CloudStack Database by using the 4.0 schema and apply
// upgrade steps to it.
public class DatabaseCreator {

    protected static void printHelp(String cmd) {
        System.out.println("\nDatabaseCreator creates the database schema by removing the \n" + "previous schema, creating the schema, and running \n"
            + "through the database updaters.");
        System.out.println("Usage: " + cmd + " [options] [db.properties file] [schema.sql files] [database upgrade class]\nOptions:" +
            "\n   --database=a,b comma separate databases to initialize, use the db name in db.properties defined as db.xyz.host, xyz should be passed" +
            "\n   --rootpassword=password, by default it will try with an empty password" + "\n   --dry or -d, this would not run any process, just does a dry run" +
            "\n   --verbose or -v to print running sql commands, by default it won't print them" + "\n   --help or -h for help");
    }

    private static boolean fileExists(String file) {
        File f = new File(file);
        if (!f.exists())
            System.out.println("========> WARNING: Provided file does not exist: " + file);
        return f.exists();
    }

    private static void runScript(Connection conn, Reader reader, String filename, boolean verbosity) {
        ScriptRunner runner = new ScriptRunner(conn, false, true, verbosity);
        try {
            runner.runScript(reader);
        } catch (IOException e) {
            System.err.println("Unable to read " + filename + ": " + e.getMessage());
            System.exit(1);
        } catch (SQLException e) {
            System.err.println("Unable to execute " + filename + ": " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runQuery(String host, String port, String rootPassword, String query, boolean dryRun) {
        System.out.println("============> Running query: " + query);
        try (Connection conn = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/", host, port), "root", rootPassword);
             Statement stmt = conn.createStatement();){
             if (!dryRun)
                stmt.executeUpdate(query);
        } catch (SQLException e) {
            System.out.println("SQL exception in trying initDB: " + e);
            System.exit(1);
        }
    }

    private static void initDB(String dbPropsFile, String rootPassword, String[] databases, boolean dryRun) {
        Properties dbProperties = new Properties();
        try(FileInputStream f_stream = new FileInputStream(new File(dbPropsFile));) {
            dbProperties.load(f_stream);
        } catch (IOException e) {
            System.out.println("IOError: unable to load/read db properties file: " + e);
            System.exit(1);
        }

        for (String database : databases) {
            String host = dbProperties.getProperty(String.format("db.%s.host", database));
            String port = dbProperties.getProperty(String.format("db.%s.port", database));
            String username = dbProperties.getProperty(String.format("db.%s.username", database));
            String password = dbProperties.getProperty(String.format("db.%s.password", database));
            String dbName = dbProperties.getProperty(String.format("db.%s.name", database));
            System.out.println(String.format("========> Initializing database=%s with host=%s port=%s username=%s password=%s", dbName, host, port, username, password));

            List<String> queries = new ArrayList<String>();
            queries.add(String.format("drop database if exists `%s`", dbName));
            queries.add(String.format("create database `%s`", dbName));
            queries.add(String.format("CREATE USER IF NOT EXISTS %s@`localhost` identified by '%s'", username, password));
            queries.add(String.format("CREATE USER IF NOT EXISTS %s@`%%` identified by '%s'", username, password));
            queries.add(String.format("GRANT ALL ON %s.* to '%s'@`localhost`", dbName, username));
            queries.add(String.format("GRANT ALL ON %s.* to '%s'@`%%`", dbName, username));

            for (String query : queries) {
                runQuery(host, port, rootPassword, query, dryRun);
            }
        }
    }

    public static void main(String[] args) {

        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(new String[] {"/com/cloud/upgrade/databaseCreatorContext.xml"});
        appContext.getBean(ComponentContext.class);

        String dbPropsFile = "";
        List<String> sqlFiles = new ArrayList<String>();
        List<String> upgradeClasses = new ArrayList<String>();
        String[] databases = new String[] {};
        String rootPassword = "";
        boolean verbosity = false;
        boolean dryRun = false;

        // Process opts
        for (String arg : args) {
            if (arg.equals("--help") || arg.equals("-h")) {
                printHelp("DatabaseCreator");
                System.exit(0);
            } else if (arg.equals("--verbose") || arg.equals("-v")) {
                verbosity = true;
            } else if (arg.equals("--dry") || arg.equals("-d")) {
                dryRun = true;
            } else if (arg.startsWith("--rootpassword=")) {
                rootPassword = arg.substring(arg.lastIndexOf("=") + 1, arg.length());
            } else if (arg.startsWith("--database=")) {
                databases = arg.substring(arg.lastIndexOf("=") + 1, arg.length()).split(",");
            } else if (arg.endsWith(".sql")) {
                sqlFiles.add(arg);
            } else if (arg.endsWith(".sql.override")) {
                if (fileExists(arg)) {
                    int index = arg.lastIndexOf(".override");
                    String fileToOverride = arg.substring(0, index);
                    sqlFiles.remove(fileToOverride);
                    sqlFiles.add(arg);
                }
            } else if (arg.endsWith(".properties")) {
                if (!dbPropsFile.endsWith("properties.override") && fileExists(arg))
                    dbPropsFile = arg;
            } else if (arg.endsWith("properties.override")) {
                if (fileExists(arg))
                    dbPropsFile = arg;
            } else {
                upgradeClasses.add(arg);
            }
        }

        if ((dbPropsFile.isEmpty()) || (sqlFiles.size() == 0) && upgradeClasses.size() == 0) {
            printHelp("DatabaseCreator");
            System.exit(1);
        }

        try {
            TransactionLegacy.initDataSource(dbPropsFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        initDB(dbPropsFile, rootPassword, databases, dryRun);

        // Process sql files
        for (String sqlFile : sqlFiles) {
            File sqlScript = PropertiesUtil.findConfigFile(sqlFile);
            if (sqlScript == null) {
                System.err.println("Unable to find " + sqlFile);
                printHelp("DatabaseCreator");
                System.exit(1);
            }

            System.out.println("========> Processing SQL file at " + sqlScript.getAbsolutePath());

            try(Connection conn = TransactionLegacy.getStandaloneConnection();
                FileReader reader = new FileReader(sqlScript);
            ) {
                if (!dryRun)
                    runScript(conn, reader, sqlFile, verbosity);
            }catch (SQLException e)
            {
                System.err.println("Sql Exception:" + e.getMessage());
                System.exit(1);
            }
            catch (IOException e)
            {
                System.err.println("File IO Exception : " + e.getMessage());
                System.exit(1);
            }

        }

        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
        try {
            // Process db upgrade classes
            for (String upgradeClass : upgradeClasses) {
                System.out.println("========> Processing upgrade: " + upgradeClass);
                Class<?> clazz = null;
                try {
                    clazz = Class.forName(upgradeClass);
                    if (!SystemIntegrityChecker.class.isAssignableFrom(clazz)) {
                        System.err.println("The class must be of SystemIntegrityChecker: " + clazz.getName());
                        System.exit(1);
                    }
                    SystemIntegrityChecker checker = (SystemIntegrityChecker)clazz.newInstance();
                    checker.check();
                } catch (ClassNotFoundException e) {
                    System.err.println("Unable to find " + upgradeClass + ": " + e.getMessage());
                    System.exit(1);
                } catch (InstantiationException e) {
                    System.err.println("Unable to instantiate " + upgradeClass + ": " + e.getMessage());
                    System.exit(1);
                } catch (IllegalAccessException e) {
                    System.err.println("Unable to access " + upgradeClass + ": " + e.getMessage());
                    System.exit(1);
                }

            }
        } finally {
            txn.close();
        }
        System.exit(0);
    }
}
