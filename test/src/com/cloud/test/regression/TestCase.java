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
package com.cloud.test.regression;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

public abstract class TestCase {

    public static Logger s_logger = Logger.getLogger(TestCase.class.getName());
    private Connection conn;
    private ArrayList<Document> inputFile = new ArrayList<Document>();
    private HttpClient client;
    private String testCaseName;
    private HashMap<String, String> param = new HashMap<String, String>();
    private HashMap<String, String> commands = new HashMap<String, String>();

    public HashMap<String, String> getParam() {
        return param;
    }

    public void setParam(HashMap<String, String> param) {
        this.param = param;
    }

    public HashMap<String, String> getCommands() {
        return commands;
    }

    public void setCommands() {
        File asyncCommands = null;
        if (param.get("apicommands") == null) {
            s_logger.info("Unable to get the list of commands, exiting");
            System.exit(1);
        } else {
            asyncCommands = new File(param.get("apicommands"));
        }
        try {
            Properties pro = new Properties();
            FileInputStream in = new FileInputStream(asyncCommands);
            pro.load(in);
            Enumeration<?> en = pro.propertyNames();
            while (en.hasMoreElements()) {
                String key = (String)en.nextElement();
                commands.put(key, pro.getProperty(key));
            }
        } catch (Exception ex) {
            s_logger.info("Unable to find the file " + param.get("apicommands") + " due to following exception " + ex);
        }

    }

    public Connection getConn() {
        return conn;
    }

    public void setConn(String dbPassword) {
        this.conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.conn = DriverManager.getConnection("jdbc:mysql://" + param.get("db") + "/cloud", "root", dbPassword);
            if (!this.conn.isValid(0)) {
                s_logger.error("Connection to DB failed to establish");
            }

        } catch (Exception ex) {
            s_logger.error(ex);
        }
    }

    public void setInputFile(ArrayList<String> fileNameInput) {
        for (String fileName : fileNameInput) {
            File file = new File(fileName);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document doc = null;
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.parse(file);
                doc.getDocumentElement().normalize();
            } catch (Exception ex) {
                s_logger.error("Unable to load " + fileName + " due to ", ex);
            }
            this.inputFile.add(doc);
        }
    }

    public ArrayList<Document> getInputFile() {
        return inputFile;
    }

    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    public String getTestCaseName() {
        return this.testCaseName;
    }

    public void setClient() {
        HttpClient client = new HttpClient();
        this.client = client;
    }

    public HttpClient getClient() {
        return this.client;
    }

    //abstract methods
    public abstract boolean executeTest();

}
