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
package com.cloud.test.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

public class SubmitCert {
    public static String url = "Action=SetCertificate";
    public static String secretKey;
    public static String apiKey;
    public static String host;
    public static String port;
    public static String command;
    public static String accessPoint;
    public static String signatureMethod;
    public static String fileName = "tool.properties";
    public static String certFileName;
    public static String cert;
    public static final Logger s_logger = Logger.getLogger(SubmitCert.class.getName());

    public static void main(String[] args) {
        // Parameters
        List<String> argsList = Arrays.asList(args);
        Iterator<String> iter = argsList.iterator();
        while (iter.hasNext()) {
            String arg = iter.next();

            if (arg.equals("-c")) {
                certFileName = iter.next();
            }

            if (arg.equals("-s")) {
                secretKey = iter.next();
            }

            if (arg.equals("-a")) {
                apiKey = iter.next();
            }

            if (arg.equals("-action")) {
                url = "Action=" + iter.next();
            }
        }

        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("conf/tool.properties"));
        } catch (IOException ex) {
            s_logger.error("Error reading from conf/tool.properties", ex);
            System.exit(2);
        }

        host = prop.getProperty("host");
        port = prop.getProperty("port");

        if (url.equals("Action=SetCertificate") && certFileName == null) {
            s_logger.error("Please set path to certificate (including file name) with -c option");
            System.exit(1);
        }

        if (secretKey == null) {
            s_logger.error("Please set secretkey  with -s option");
            System.exit(1);
        }

        if (apiKey == null) {
            s_logger.error("Please set apikey with -a option");
            System.exit(1);
        }

        if (host == null) {
            s_logger.error("Please set host in tool.properties file");
            System.exit(1);
        }

        if (port == null) {
            s_logger.error("Please set port in tool.properties file");
            System.exit(1);
        }

        TreeMap<String, String> param = new TreeMap<String, String>();

        String req = "GET\n" + host + ":" + prop.getProperty("port") + "\n/" + prop.getProperty("accesspoint") + "\n";
        String temp = "";

        if (certFileName != null) {
            cert = readCert(certFileName);
            param.put("cert", cert);
        }

        param.put("AWSAccessKeyId", apiKey);
        param.put("Expires", prop.getProperty("expires"));
        param.put("SignatureMethod", prop.getProperty("signaturemethod"));
        param.put("SignatureVersion", "2");
        param.put("Version", prop.getProperty("version"));

        StringTokenizer str1 = new StringTokenizer(url, "&");
        while (str1.hasMoreTokens()) {
            String newEl = str1.nextToken();
            StringTokenizer str2 = new StringTokenizer(newEl, "=");
            String name = str2.nextToken();
            String value = str2.nextToken();
            param.put(name, value);
        }

        //sort url hash map by key
        Set c = param.entrySet();
        Iterator it = c.iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry)it.next();
            String key = (String)me.getKey();
            String value = (String)me.getValue();
            try {
                temp = temp + key + "=" + URLEncoder.encode(value, "UTF-8") + "&";
            } catch (Exception ex) {
                s_logger.error("Unable to set parameter " + value + " for the command " + param.get("command"), ex);
            }

        }
        temp = temp.substring(0, temp.length() - 1);
        String requestToSign = req + temp;
        String signature = UtilsForTest.signRequest(requestToSign, secretKey);
        String encodedSignature = "";
        try {
            encodedSignature = URLEncoder.encode(signature, "UTF-8");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String url = "http://" + host + ":" + prop.getProperty("port") + "/" + prop.getProperty("accesspoint") + "?" + temp + "&Signature=" + encodedSignature;
        s_logger.info("Sending request with url:  " + url + "\n");
        sendRequest(url);
    }

    public static String readCert(String filePath) {
        try {
            StringBuffer fileData = new StringBuffer(1000);
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            char[] buf = new char[1024];
            int numRead = 0;
            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
            reader.close();
            return fileData.toString();
        } catch (Exception ex) {
            s_logger.error(ex);
            return null;
        }
    }

    public static void sendRequest(String url) {
        try {
            HttpClient client = new HttpClient();
            HttpMethod method = new GetMethod(url);
            int responseCode = client.executeMethod(method);
            String is = method.getResponseBodyAsString();
            s_logger.info("Response code " + responseCode + ": " + is);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
