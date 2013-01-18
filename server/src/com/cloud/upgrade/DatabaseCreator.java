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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.cloud.utils.PropertiesUtil;

import com.cloud.utils.component.SystemIntegrityChecker;
import com.cloud.utils.db.ScriptRunner;
import com.cloud.utils.db.Transaction;

// Creates the CloudStack Database by using the 4.0 schema and apply
// upgrade steps to it.
public class DatabaseCreator {
    protected static void printHelp(String cmd) {
        System.out.println(
                "DatabaseCreator creates the database schema by removing the \n" +
                "previous schema, creating the schema, and running \n" +
                "through the database updaters.");
        System.out.println("Usage: " + cmd + " [schema files] [database upgrade class]");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            printHelp("DatabaseCreator");
            System.exit(1);
        }

        for (int i = 0; i < args.length; i++) {

            if (args[i].endsWith("sql")) {

                File sqlScript = PropertiesUtil.findConfigFile(args[i]);
                if (sqlScript == null) {
                    System.err.println("Unable to find " + args[i]);
                    printHelp("DatabaseCreator");
                    System.exit(1);
                }

                System.out.println("=============> Processing SQL file at " + sqlScript.getAbsolutePath());

                Connection conn = Transaction.getStandaloneConnection();
                try {

                    ScriptRunner runner = new ScriptRunner(conn, false, true);
                    FileReader reader = null;
                    try {
                        reader = new FileReader(sqlScript);
                    } catch (FileNotFoundException e) {
                        System.err.println("Unable to read " + args[i] + ": " + e.getMessage());
                        System.exit(1);
                    }
                    try {
                        runner.runScript(reader);
                    } catch (IOException e) {
                        System.err.println("Unable to read " + args[i] + ": " + e.getMessage());
                        System.exit(1);
                    } catch (SQLException e) {
                        System.err.println("Unable to execute " + args[i] + ": " + e.getMessage());
                        System.exit(1);
                    }
                } finally {

                    try {
                        conn.close();
                    } catch (SQLException e) {
                        System.err.println("Unable to close DB connection: " + e.getMessage());
                    }
                }

            } else {
                System.out.println("=============> Processing upgrade: " + args[i]);
                Class<?> clazz = null;
                try {
                    clazz = Class.forName(args[i]);
                    if (!SystemIntegrityChecker.class.isAssignableFrom(clazz)) {
                        System.err.println("The class must be of SystemIntegrityChecker: " + clazz.getName());
                        System.exit(1);
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Unable to find " + args[i] + ": " + e.getMessage());
                    System.exit(1);
                }

                //SystemIntegrityChecker checker = (SystemIntegrityChecker)ComponentLocator.inject(clazz);
                //checker.check();
            }
        }
    }
}
