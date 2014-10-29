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

package org.apache.cloudstack.network.contrail.management;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.ServerSocket;

import org.apache.commons.configuration.PropertiesConfiguration;

public class TestDbSetup {

    public static int findFreePort() throws Exception {

        int port;
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        return port;
    }

    public static void startMysqlServer(int port, String startMysqlScript) throws Exception {

        try {
            String cwd = new java.io.File(".").getCanonicalPath();
            Runtime r = Runtime.getRuntime();
            String script = startMysqlScript;
            if (script == null) {
                script = "test/resources/mysql_db_start.sh " + port;
            }
            Process process = r.exec("sh " + cwd + "/" + script);
            process.waitFor();
            System.out.println("new sql server instance launched on port: " + port);
        } catch (Exception e) {

            String cause = e.getMessage();
            if (cause.equals("sh: not found"))
                System.out.println("No sh interpreter found.");
            throw e;
        }
    }

    public static void stopMysqlServer(int port, String stopMysqlScript) throws Exception {

        try {
            Runtime r = Runtime.getRuntime();
            String script = stopMysqlScript;
            if (script == null) {
                script = "test/resources/mysql_db_stop.sh " + port;
            }
            Process process = r.exec("sh " + script);
            process.waitFor();
            System.out.println("sql server instance running at port " + port + " stopped");
        } catch (Exception e) {

            String cause = e.getMessage();
            if (cause.equals("sh: not found"))
                System.out.println("No sh interpreter found.");
            throw e;
        }
    }

    /* this is required for deploying db with new set of sql server parameters */
    public static void copyDbPropertiesFile() throws Exception {
        Runtime.getRuntime().exec("cp ../../../utils/conf/db.properties ../../../utils/conf/db.properties.override");
    }

    public static void updateSqlPort(int port, String propertyFileOverride) throws Exception {

        PropertiesConfiguration config = new PropertiesConfiguration(propertyFileOverride);
        System.out.println("File: " + propertyFileOverride + "; old: db.properties port: " + config.getProperty("db.cloud.port") + ", new port: " + port);
        config.setProperty("db.cloud.port", "" + port);
        config.setProperty("db.cloud.username", System.getProperty("user.name"));
        config.setProperty("db.cloud.password", "");

        config.setProperty("db.usage.port", "" + port);
        config.setProperty("db.usage.username", System.getProperty("user.name"));
        config.setProperty("db.usage.password", "");

        config.setProperty("db.awsapi.port", "" + port);
        config.setProperty("db.awsapi.username", System.getProperty("user.name"));
        config.setProperty("db.awsapi.password", "");

        config.setProperty("db.simulator.port", "" + port);
        config.setProperty("db.simulator.username", System.getProperty("user.name"));
        config.setProperty("db.simulator.password", "");

        config.save();
    }

    public static void initCloudstackDb() throws Exception {
        try {
            File dir = new File("../../../");
            Runtime r = Runtime.getRuntime();
            Process process = r.exec("mvn -P developer -pl developer -Ddeploydb ", null, dir);
            dumpProcessOutput(process);
            process.waitFor();
        } catch (Exception e) {
            String cause = e.getMessage();
            System.out.println("e: " + cause);
            throw e;
        }
    }

    public static void dumpProcessOutput(Process p) throws Exception {

        BufferedReader istream = null;
        istream = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = null;
        while ((line = istream.readLine()) != null) {
            System.out.println(line);
        }
    }

    public static int init(String startScript) throws Exception {
        int port = TestDbSetup.findFreePort();
        TestDbSetup.startMysqlServer(port, startScript);
        copyDbPropertiesFile();
        /* both of these files needs to have mysql port, username password details */
        TestDbSetup.updateSqlPort(port, "db.properties"); /* for cloudstack runtime */
        TestDbSetup.updateSqlPort(port, "../../../utils/conf/db.properties.override"); /* for deploying db */
        TestDbSetup.initCloudstackDb();
        return port;
    }

    public static void destroy(int port, String stopScript) throws Exception {
        TestDbSetup.stopMysqlServer(port, stopScript);
    }
}
