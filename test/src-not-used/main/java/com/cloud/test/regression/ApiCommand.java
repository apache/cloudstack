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
import java.io.InputStream;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.cloud.test.utils.UtilsForTest;

public class ApiCommand {
    public static final Logger s_logger = Logger.getLogger(ApiCommand.class.getName());

    public static enum CommandType {
        HTTP, MYSQL, SCRIPT;
    }

    public static enum ResponseType {
        ERROR, EMPTY;
    }

    private Element xmlCommand;
    private String commandName;
    private String testCaseInfo;
    private boolean isUserCommand;
    private boolean isAsync = false;
    private CommandType commandType;
    private ResponseType responseType;

    private TreeMap<String, String> urlParam;
    private HashMap<String, String> verifyParam = new HashMap<String, String>();;
    private HashMap<String, String> setParam = new HashMap<String, String>();;
    private int responseCode;
    private Element responseBody;

    private String command;
    private String host;
    private boolean list;
    private Element listName;
    private Element listId;
    private boolean required = false;
    private ResultSet result;

    public ApiCommand(Element fstElmnt, HashMap<String, String> param, HashMap<String, String> commands) {
        this.setXmlCommand(fstElmnt);
        this.setCommandName();
        this.setResponseType();
        this.setUserCommand();
        this.setCommandType();
        this.setTestCaseInfo();
        this.setUrlParam(param);
        this.setVerifyParam(param);
        this.setHost("http://" + param.get("hostip"));
        this.setCommand(param);
        String async = commands.get(this.getName());
        if (async != null && async.equals("yes")) {
            this.isAsync = true;

        }
    }

    public Element getXmlCommand() {
        return xmlCommand;
    }

    public void setXmlCommand(Element xmlCommand) {
        this.xmlCommand = xmlCommand;
    }

    // ================FOLLOWING METHODS USE INPUT XML FILE=======================//
    public void setCommandName() {
        NodeList commandName = this.xmlCommand.getElementsByTagName("name");
        Element commandElmnt = (Element)commandName.item(0);
        NodeList commandNm = commandElmnt.getChildNodes();
        this.commandName = (commandNm.item(0).getNodeValue());
    }

    public String getName() {
        return commandName;
    }

    public void setTestCaseInfo() {
        this.testCaseInfo = getElementByName("testcase");
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setResponseType() {
        boolean result = verifyTagValue("error", "true");
        if (result) {
            this.responseType = ResponseType.ERROR;
            return;
        }
        result = verifyTagValue("empty", "true");
        if (result) {
            this.responseType = ResponseType.EMPTY;
        }
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setUserCommand() {
        boolean result = verifyTagValue("usercommand", "true");
        this.isUserCommand = result;
    }

    public void setCommandType() {
        boolean result = verifyTagValue("mysql", "true");
        if (result) {
            this.commandType = CommandType.MYSQL;
            return;
        }
        result = verifyTagValue("script", "true");
        if (result) {
            this.commandType = CommandType.SCRIPT;
            return;
        }
        this.commandType = CommandType.HTTP;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getTestCaseInfo() {
        return testCaseInfo;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setUrlParam(HashMap<String, String> param) {
        this.urlParam = new TreeMap<String, String>();
        NodeList parameterLst = this.xmlCommand.getElementsByTagName("parameters");
        if (parameterLst != null) {
            for (int j = 0; j < parameterLst.getLength(); j++) {
                Element parameterElement = (Element)parameterLst.item(j);
                NodeList itemLst = parameterElement.getElementsByTagName("item");
                for (int k = 0; k < itemLst.getLength(); k++) {
                    Node item = itemLst.item(k);
                    if (item.getNodeType() == Node.ELEMENT_NODE) {
                        Element itemElement = (Element)item;
                        NodeList itemName = itemElement.getElementsByTagName("name");
                        Element itemNameElement = (Element)itemName.item(0);

                        // get value
                        Element itemValueElement = null;
                        if ((itemElement.getElementsByTagName("value") != null) && (itemElement.getElementsByTagName("value").getLength() != 0)) {
                            NodeList itemValue = itemElement.getElementsByTagName("value");
                            itemValueElement = (Element)itemValue.item(0);
                        }

                        Element itemParamElement = null;
                        // getparam
                        if ((itemElement.getElementsByTagName("param") != null) && (itemElement.getElementsByTagName("param").getLength() != 0)) {
                            NodeList itemParam = itemElement.getElementsByTagName("param");
                            itemParamElement = (Element)itemParam.item(0);
                        }

                        if ((itemElement.getAttribute("getparam").equals("true")) && (itemParamElement != null)) {
                            this.urlParam.put(itemNameElement.getTextContent(), param.get(itemParamElement.getTextContent()));
                        } else if (itemValueElement != null) {
                            this.urlParam.put(itemNameElement.getTextContent(), itemValueElement.getTextContent());
                        } else if (itemElement.getAttribute("random").equals("true")) {
                            Random ran = new Random();
                            String randomString = Math.abs(ran.nextInt()) + "-randomName";
                            this.urlParam.put(itemNameElement.getTextContent(), randomString);
                            if ((itemElement.getAttribute("setparam").equals("true")) && (itemParamElement != null)) {
                                param.put(itemParamElement.getTextContent(), randomString);
                            }
                        } else if (itemElement.getAttribute("randomnumber").equals("true")) {
                            Random ran = new Random();
                            Integer randomNumber = Math.abs(ran.nextInt(65535));
                            this.urlParam.put(itemNameElement.getTextContent(), randomNumber.toString());
                            if ((itemElement.getAttribute("setparam").equals("true")) && (itemParamElement != null)) {
                                param.put(itemParamElement.getTextContent(), randomNumber.toString());
                            }
                        }
                    }
                }
            }
        }
    }

    // Set command URL
    public void setCommand(HashMap<String, String> param) {

        if (this.getCommandType() == CommandType.SCRIPT) {
            String temp = "bash xen/" + this.commandName;
            Set<?> c = this.urlParam.entrySet();
            Iterator<?> it = c.iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> me = (Map.Entry<?, ?>)it.next();
                String key = (String)me.getKey();
                String value = (String)me.getValue();
                try {
                    temp = temp + " -" + key + " " + value;
                } catch (Exception ex) {
                    s_logger.error("Unable to set parameter " + key + " for the command " + this.getName());
                }
            }
            this.command = temp;
        } else if (this.getCommandType() == CommandType.MYSQL) {
            String temp = this.commandName + " where ";
            Set<?> c = this.urlParam.entrySet();
            Iterator<?> it = c.iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> me = (Map.Entry<?, ?>)it.next();
                String key = (String)me.getKey();
                String value = (String)me.getValue();
                try {
                    temp = temp + key + "=" + value;
                } catch (Exception ex) {
                    s_logger.error("Unable to set parameter " + key + " for the command " + this.getName());
                }
            }
            this.command = temp;
            s_logger.info("The command is " + this.command);

        } else {
            if ((param.get("apikey") == null) || (param.get("secretkey") == null) || (this.isUserCommand == false)) {
                String temp = this.host + ":8096/?command=" + this.commandName;
                Set<?> c = this.urlParam.entrySet();
                Iterator<?> it = c.iterator();
                while (it.hasNext()) {
                    Map.Entry<?, ?> me = (Map.Entry<?, ?>)it.next();
                    String key = (String)me.getKey();
                    String value = (String)me.getValue();
                    try {
                        temp = temp + "&" + key + "=" + URLEncoder.encode(value, "UTF-8");
                    } catch (Exception ex) {
                        s_logger.error("Unable to set parameter " + key + " for the command " + this.getName());
                    }
                }
                this.command = temp;
            } else if (isUserCommand == true) {
                String apiKey = param.get("apikey");
                String secretKey = param.get("secretkey");

                String temp = "";
                this.urlParam.put("apikey", apiKey);
                this.urlParam.put("command", this.commandName);

                // sort url hash map by key
                Set<?> c = this.urlParam.entrySet();
                Iterator<?> it = c.iterator();
                while (it.hasNext()) {
                    Map.Entry<?, ?> me = (Map.Entry<?, ?>)it.next();
                    String key = (String)me.getKey();
                    String value = (String)me.getValue();
                    try {
                        temp = temp + key + "=" + URLEncoder.encode(value, "UTF-8") + "&";
                    } catch (Exception ex) {
                        s_logger.error("Unable to set parameter " + value + " for the command " + this.getName());
                    }

                }
                temp = temp.substring(0, temp.length() - 1);
                String requestToSign = temp.toLowerCase();
                String signature = UtilsForTest.signRequest(requestToSign, secretKey);
                String encodedSignature = "";
                try {
                    encodedSignature = URLEncoder.encode(signature, "UTF-8");
                } catch (Exception ex) {
                    s_logger.error(ex);
                }
                this.command = this.host + ":8080/client/api/?" + temp + "&signature=" + encodedSignature;
            }
        }
    }

    public void setVerifyParam(HashMap<String, String> param) {
        NodeList returnLst = this.xmlCommand.getElementsByTagName("returnvalue");
        if (returnLst != null) {
            for (int m = 0; m < returnLst.getLength(); m++) {
                Element returnElement = (Element)returnLst.item(m);
                if (returnElement.getAttribute("list").equals("true")) {
                    this.list = true;
                    NodeList elementLst = returnElement.getElementsByTagName("element");
                    this.listId = (Element)elementLst.item(0);
                    NodeList elementName = returnElement.getElementsByTagName("name");
                    this.listName = (Element)elementName.item(0);
                } else {
                    this.list = false;
                }

                NodeList itemLst1 = returnElement.getElementsByTagName("item");
                if (itemLst1 != null) {
                    for (int n = 0; n < itemLst1.getLength(); n++) {
                        Node item = itemLst1.item(n);
                        if (item.getNodeType() == Node.ELEMENT_NODE) {
                            Element itemElement = (Element)item;
                            // get parameter name
                            NodeList itemName = itemElement.getElementsByTagName("name");
                            Element itemNameElement = (Element)itemName.item(0);

                            // Get parameters for future use
                            if (itemElement.getAttribute("setparam").equals("true")) {
                                NodeList itemVariable = itemElement.getElementsByTagName("param");
                                Element itemVariableElement = (Element)itemVariable.item(0);
                                setParam.put(itemVariableElement.getTextContent(), itemNameElement.getTextContent());
                                this.required = true;
                            } else if (itemElement.getAttribute("getparam").equals("true")) {
                                NodeList itemVariable = itemElement.getElementsByTagName("param");
                                Element itemVariableElement = (Element)itemVariable.item(0);
                                this.verifyParam.put(itemNameElement.getTextContent(), param.get(itemVariableElement.getTextContent()));
                            } else if ((itemElement.getElementsByTagName("value") != null) && (itemElement.getElementsByTagName("value").getLength() != 0)) {
                                NodeList itemVariable = itemElement.getElementsByTagName("value");
                                Element itemVariableElement = (Element)itemVariable.item(0);
                                this.verifyParam.put(itemNameElement.getTextContent(), itemVariableElement.getTextContent());
                            } else {
                                this.verifyParam.put(itemNameElement.getTextContent(), "no value");
                            }
                        }
                    }
                }
            }
        }
    }

    public int getResponseCode() {
        return responseCode;
    }

    // Send api command to the server
    public void sendCommand(HttpClient client, Connection conn) {
        if (TestCaseEngine.s_printUrl == true) {
            s_logger.info("url is " + this.command);
        }

        if (this.getCommandType() == CommandType.SCRIPT) {
            try {
                s_logger.info("Executing command " + this.command);
                Runtime rtime = Runtime.getRuntime();
                Process child = rtime.exec(this.command);
                Thread.sleep(10000);
                int retCode = child.waitFor();
                if (retCode != 0) {
                    this.responseCode = retCode;
                } else {
                    this.responseCode = 200;
                }

            } catch (Exception ex) {
                s_logger.error("Unable to execute a command " + this.command, ex);
            }
        } else if (this.getCommandType() == CommandType.MYSQL) {
            try {
                Statement stmt = conn.createStatement();
                this.result = stmt.executeQuery(this.command);
                this.responseCode = 200;
            } catch (Exception ex) {
                this.responseCode = 400;
                s_logger.error("Unable to execute mysql query " + this.command, ex);
            }
        } else {
            HttpMethod method = new GetMethod(this.command);
            try {
                this.responseCode = client.executeMethod(method);

                if (this.responseCode == 200) {
                    InputStream is = method.getResponseBodyAsStream();
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(is);
                    doc.getDocumentElement().normalize();

                    if (!(this.isAsync)) {
                        this.responseBody = doc.getDocumentElement();
                    } else {
                        // get async job result
                        Element jobTag = (Element)doc.getDocumentElement().getElementsByTagName("jobid").item(0);
                        String jobId = jobTag.getTextContent();
                        Element responseBodyAsyncEl = queryAsyncJobResult(jobId);
                        if (responseBodyAsyncEl == null) {
                            s_logger.error("Can't get a async result");
                        } else {
                            this.responseBody = responseBodyAsyncEl;
                            // get status of the job
                            Element jobStatusTag = (Element)responseBodyAsyncEl.getElementsByTagName("jobstatus").item(0);
                            String jobStatus = jobStatusTag.getTextContent();
                            if (!jobStatus.equals("1")) { // Need to modify with different error codes for jobAsync
// results
                                // set fake response code by now
                                this.responseCode = 400;
                            }
                        }
                    }
                }

                if (TestCaseEngine.s_printUrl == true) {
                    s_logger.info("Response code is " + this.responseCode);
                }
            } catch (Exception ex) {
                s_logger.error("Command " + command + " failed with exception " + ex.getMessage());
            } finally {
                method.releaseConnection();
            }
        }
    }

    // verify if response is empty (contains only root element)
    public boolean isEmpty() {
        boolean result = false;
        if (!this.responseBody.hasChildNodes())
            result = true;
        return result;
    }

    // ================FOLLOWING METHODS USE RETURN XML FILE=======================//

    public boolean setParam(HashMap<String, String> param) {
        if ((this.responseBody == null) && (this.commandType == CommandType.HTTP)) {
            s_logger.error("Response body is empty");
            return false;
        }
        Boolean result = true;

        if (this.getCommandType() == CommandType.MYSQL) {
            Set<?> set = this.setParam.entrySet();
            Iterator<?> it = set.iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> me = (Map.Entry<?, ?>)it.next();
                String key = (String)me.getKey();
                String value = (String)me.getValue();
                try {
                    String itemName = null;
                    while (this.result.next()) {
                        itemName = this.result.getString(value);
                    }
                    if (itemName != null) {
                        param.put(key, itemName);
                    } else {
                        s_logger.error("Following return parameter is missing: " + value);
                        result = false;
                    }
                } catch (Exception ex) {
                    s_logger.error("Unable to set parameter " + value, ex);
                }
            }
        } else if (this.getCommandType() == CommandType.HTTP) {
            if (this.list == false) {
                Set<?> set = this.setParam.entrySet();
                Iterator<?> it = set.iterator();

                while (it.hasNext()) {
                    Map.Entry<?, ?> me = (Map.Entry<?, ?>)it.next();
                    String key = (String)me.getKey();
                    String value = (String)me.getValue();
                    // set parameters needed for the future use
                    NodeList itemName = this.responseBody.getElementsByTagName(value);
                    if ((itemName != null) && (itemName.getLength() != 0)) {
                        for (int i = 0; i < itemName.getLength(); i++) {
                            Element itemNameElement = (Element)itemName.item(i);
                            if (itemNameElement.getChildNodes().getLength() <= 1) {
                                param.put(key, itemNameElement.getTextContent());
                                break;
                            }
                        }
                    } else {
                        s_logger.error("Following return parameter is missing: " + value);
                        result = false;
                    }
                }
            } else {
                Set<?> set = this.setParam.entrySet();
                Iterator<?> it = set.iterator();
                NodeList returnLst = this.responseBody.getElementsByTagName(this.listName.getTextContent());
                Node requiredNode = returnLst.item(Integer.parseInt(this.listId.getTextContent()));

                if (requiredNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element fstElmnt = (Element)requiredNode;

                    while (it.hasNext()) {
                        Map.Entry<?, ?> me = (Map.Entry<?, ?>)it.next();
                        String key = (String)me.getKey();
                        String value = (String)me.getValue();
                        NodeList itemName = fstElmnt.getElementsByTagName(value);
                        if ((itemName != null) && (itemName.getLength() != 0)) {
                            Element itemNameElement = (Element)itemName.item(0);
                            if (itemNameElement.getChildNodes().getLength() <= 1) {
                                param.put(key, itemNameElement.getTextContent());
                            }
                        } else {
                            s_logger.error("Following return parameter is missing: " + value);
                            result = false;
                        }
                    }
                }
            }
        }
        return result;
    }

    public String getUrl() {
        return command;
    }

    public boolean verifyParam() {
        boolean result = true;
        if (this.getCommandType() == CommandType.HTTP) {
            if (this.list == false) {
                Set<?> set = verifyParam.entrySet();
                Iterator<?> it = set.iterator();

                while (it.hasNext()) {
                    Map.Entry<?, ?> me = (Map.Entry<?, ?>)it.next();
                    String key = (String)me.getKey();
                    String value = (String)me.getValue();
                    if (value == null) {
                        s_logger.error("Parameter " + key + " is missing in the list of global parameters");
                        return false;
                    }

                    NodeList itemName = this.responseBody.getElementsByTagName(key);
                    if ((itemName.getLength() != 0) && (itemName != null)) {
                        Element itemNameElement = (Element)itemName.item(0);
                        if (itemNameElement.hasChildNodes()) {
                            continue;
                        }
                        if (!(verifyParam.get(key).equals("no value")) && !(itemNameElement.getTextContent().equals(verifyParam.get(key)))) {
                            s_logger.error("Incorrect value for the following tag: " + key + ". Expected value is " + verifyParam.get(key) + " while actual value is " +
                                itemNameElement.getTextContent());
                            result = false;
                        }
                    } else {
                        s_logger.error("Following xml element is missing in the response: " + key);
                        result = false;
                    }
                }
            }
            // for multiple elements
            else {
                Set<?> set = verifyParam.entrySet();
                Iterator<?> it = set.iterator();
                // get list element specified by id
                NodeList returnLst = this.responseBody.getElementsByTagName(this.listName.getTextContent());
                Node requiredNode = returnLst.item(Integer.parseInt(this.listId.getTextContent()));

                if (requiredNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element fstElmnt = (Element)requiredNode;

                    while (it.hasNext()) {
                        Map.Entry<?, ?> me = (Map.Entry<?, ?>)it.next();
                        String key = (String)me.getKey();
                        String value = (String)me.getValue();
                        if (value == null) {
                            s_logger.error("Parameter " + key + " is missing in the list of global parameters");
                            return false;
                        }
                        NodeList itemName = fstElmnt.getElementsByTagName(key);
                        if ((itemName.getLength() != 0) && (itemName != null)) {
                            Element itemNameElement = (Element)itemName.item(0);
                            if (!(verifyParam.get(key).equals("no value")) && !(itemNameElement.getTextContent().equals(verifyParam.get(key)))) {
                                s_logger.error("Incorrect value for the following tag: " + key + ". Expected value is " + verifyParam.get(key) +
                                    " while actual value is " + itemNameElement.getTextContent());
                                result = false;
                            }
                        } else {
                            s_logger.error("Following xml element is missing in the response: " + key);
                            result = false;
                        }
                    }
                }
            }
        } else if (this.getCommandType() == CommandType.MYSQL) {
            Set<?> set = verifyParam.entrySet();
            Iterator<?> it = set.iterator();

            while (it.hasNext()) {
                Map.Entry<?, ?> me = (Map.Entry<?, ?>)it.next();
                String key = (String)me.getKey();
                String value = (String)me.getValue();
                if (value == null) {
                    s_logger.error("Parameter " + key + " is missing in the list of global parameters");
                    return false;
                }

                String itemName = null;
                try {
                    while (this.result.next()) {
                        itemName = this.result.getString(key);
                    }
                } catch (Exception ex) {
                    s_logger.error("Unable to get element from result set " + key);
                }

                if (!(value.equals("no value")) && !(itemName.equals(verifyParam.get(key)))) {
                    s_logger.error("Incorrect value for the following tag: " + key + ". Expected value is " + verifyParam.get(key) + " while actual value is " + itemName);
                    result = false;
                }
            }
        }
        return result;
    }

    public static boolean verifyEvents(String fileName, String level, String host, String account) {
        boolean result = false;
        HashMap<String, Integer> expectedEvents = new HashMap<String, Integer>();
        HashMap<String, Integer> actualEvents = new HashMap<String, Integer>();
        String key = "";

        File file = new File(fileName);
        if (file.exists()) {
            Properties pro = new Properties();
            try {
                // get expected events
                FileInputStream in = new FileInputStream(file);
                pro.load(in);
                Enumeration<?> en = pro.propertyNames();
                while (en.hasMoreElements()) {
                    key = (String)en.nextElement();
                    expectedEvents.put(key, Integer.parseInt(pro.getProperty(key)));
                }

                // get actual events
                String url = host + "/?command=listEvents&account=" + account + "&level=" + level + "&domainid=1&pagesize=100";
                s_logger.info("Getting events with the following url " + url);
                HttpClient client = new HttpClient();
                HttpMethod method = new GetMethod(url);
                int responseCode = client.executeMethod(method);
                if (responseCode == 200) {
                    InputStream is = method.getResponseBodyAsStream();
                    ArrayList<HashMap<String, String>> eventValues = UtilsForTest.parseMulXML(is, new String[] {"event"});

                    for (int i = 0; i < eventValues.size(); i++) {
                        HashMap<String, String> element = eventValues.get(i);
                        if (element.get("level").equals(level)) {
                            if (actualEvents.containsKey(element.get("type")) == true) {
                                actualEvents.put(element.get("type"), actualEvents.get(element.get("type")) + 1);
                            } else {
                                actualEvents.put(element.get("type"), 1);
                            }
                        }
                    }
                }
                method.releaseConnection();

                // compare actual events with expected events

                // compare expected result and actual result
                Iterator<?> iterator = expectedEvents.keySet().iterator();
                Integer expected;
                Integer actual;
                int fail = 0;
                while (iterator.hasNext()) {
                    expected = null;
                    actual = null;
                    String type = iterator.next().toString();
                    expected = expectedEvents.get(type);
                    actual = actualEvents.get(type);
                    if (actual == null) {
                        s_logger.error("Event of type " + type + " and level " + level + " is missing in the listEvents response. Expected number of these events is " +
                            expected);
                        fail++;
                    } else if (expected.compareTo(actual) != 0) {
                        fail++;
                        s_logger.info("Amount of events of  " + type + " type and level " + level + " is incorrect. Expected number of these events is " + expected +
                            ", actual number is " + actual);
                    }
                }
                if (fail == 0) {
                    result = true;
                }
            } catch (Exception ex) {
                s_logger.error(ex);
            }
        } else {
            s_logger.info("File " + fileName + " not found");
        }
        return result;
    }

    public static boolean verifyEvents(HashMap<String, Integer> expectedEvents, String level, String host, String parameters) {
        boolean result = false;
        HashMap<String, Integer> actualEvents = new HashMap<String, Integer>();
        try {
            // get actual events
            String url = host + "/?command=listEvents&" + parameters;
            HttpClient client = new HttpClient();
            HttpMethod method = new GetMethod(url);
            int responseCode = client.executeMethod(method);
            if (responseCode == 200) {
                InputStream is = method.getResponseBodyAsStream();
                ArrayList<HashMap<String, String>> eventValues = UtilsForTest.parseMulXML(is, new String[] {"event"});

                for (int i = 0; i < eventValues.size(); i++) {
                    HashMap<String, String> element = eventValues.get(i);
                    if (element.get("level").equals(level)) {
                        if (actualEvents.containsKey(element.get("type")) == true) {
                            actualEvents.put(element.get("type"), actualEvents.get(element.get("type")) + 1);
                        } else {
                            actualEvents.put(element.get("type"), 1);
                        }
                    }
                }
            }
            method.releaseConnection();
        } catch (Exception ex) {
            s_logger.error(ex);
        }

        // compare actual events with expected events
        Iterator<?> iterator = expectedEvents.keySet().iterator();
        Integer expected;
        Integer actual;
        int fail = 0;
        while (iterator.hasNext()) {
            expected = null;
            actual = null;
            String type = iterator.next().toString();
            expected = expectedEvents.get(type);
            actual = actualEvents.get(type);
            if (actual == null) {
                s_logger.error("Event of type " + type + " and level " + level + " is missing in the listEvents response. Expected number of these events is " + expected);
                fail++;
            } else if (expected.compareTo(actual) != 0) {
                fail++;
                s_logger.info("Amount of events of  " + type + " type and level " + level + " is incorrect. Expected number of these events is " + expected +
                    ", actual number is " + actual);
            }
        }

        if (fail == 0) {
            result = true;
        }

        return result;
    }

    public Element queryAsyncJobResult(String jobId) {
        Element returnBody = null;
        int code = 400;
        String resultUrl = this.host + ":8096/?command=queryAsyncJobResult&jobid=" + jobId;
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(resultUrl);
        while (true) {
            try {
                code = client.executeMethod(method);
                if (code == 200) {
                    InputStream is = method.getResponseBodyAsStream();
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(is);
                    doc.getDocumentElement().normalize();
                    returnBody = doc.getDocumentElement();
                    Element jobStatusTag = (Element)returnBody.getElementsByTagName("jobstatus").item(0);
                    String jobStatus = jobStatusTag.getTextContent();
                    if (jobStatus.equals("0")) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            s_logger.debug("[ignored] interrupted while during async job result query.");
                        }
                    } else {
                        break;
                    }
                    method.releaseConnection();
                } else {
                    s_logger.error("Error during queryJobAsync. Error code is " + code);
                    this.responseCode = code;
                    return null;
                }
            } catch (Exception ex) {
                s_logger.error(ex);
            }
        }
        return returnBody;
    }

    private String getElementByName(String elementName) {
        NodeList commandName = this.xmlCommand.getElementsByTagName(elementName);
        if (commandName.getLength() != 0) {
            Element commandElmnt = (Element)commandName.item(0);
            NodeList commandNm = commandElmnt.getChildNodes();
            return commandNm.item(0).getNodeValue();
        } else {
            return null;
        }
    }

    private boolean verifyTagValue(String elementName, String expectedValue) {
        NodeList tag = this.xmlCommand.getElementsByTagName(elementName);
        if (tag.getLength() != 0) {
            Element commandElmnt = (Element)tag.item(0);
            NodeList commandNm = commandElmnt.getChildNodes();
            if (commandNm.item(0).getNodeValue().equals(expectedValue)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
