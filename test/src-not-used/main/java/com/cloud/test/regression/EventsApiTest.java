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

import java.sql.Statement;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;

import com.cloud.test.regression.ApiCommand.ResponseType;

public class EventsApiTest extends TestCase {
    public static final Logger s_logger = Logger.getLogger(EventsApiTest.class.getName());

    public EventsApiTest() {
        this.setClient();
        this.setParam(new HashMap<String, String>());
    }

    @Override
    public boolean executeTest() {
        int error = 0;
        Element rootElement = this.getInputFile().get(0).getDocumentElement();
        NodeList commandLst = rootElement.getElementsByTagName("command");

        //Analyze each command, send request and build the array list of api commands
        for (int i = 0; i < commandLst.getLength(); i++) {
            Node fstNode = commandLst.item(i);
            Element fstElmnt = (Element)fstNode;

            //!!!check if we need to execute mySql command
            NodeList commandName = fstElmnt.getElementsByTagName("name");
            Element commandElmnt = (Element)commandName.item(0);
            NodeList commandNm = commandElmnt.getChildNodes();
            if (commandNm.item(0).getNodeValue().equals("mysqlupdate")) {
                //establish connection to mysql server and execute an update command
                NodeList mysqlList = fstElmnt.getElementsByTagName("mysqlcommand");
                for (int j = 0; j < mysqlList.getLength(); j++) {
                    Element itemVariableElement = (Element)mysqlList.item(j);

                    s_logger.info("Executing mysql command " + itemVariableElement.getTextContent());
                    try {
                        Statement st = this.getConn().createStatement();
                        st.executeUpdate(itemVariableElement.getTextContent());
                    } catch (Exception ex) {
                        s_logger.error(ex);
                        return false;
                    }
                }
            }

            else if (commandNm.item(0).getNodeValue().equals("agentcommand")) {
                //connect to all the agents and execute agent command
                NodeList commandList = fstElmnt.getElementsByTagName("commandname");
                Element commandElement = (Element)commandList.item(0);
                NodeList ipList = fstElmnt.getElementsByTagName("ip");
                for (int j = 0; j < ipList.getLength(); j++) {
                    Element itemVariableElement = (Element)ipList.item(j);

                    s_logger.info("Attempting to SSH into agent " + itemVariableElement.getTextContent());
                    try {
                        Connection conn = new Connection(itemVariableElement.getTextContent());
                        conn.connect(null, 60000, 60000);

                        s_logger.info("SSHed successfully into agent " + itemVariableElement.getTextContent());

                        boolean isAuthenticated = conn.authenticateWithPassword("root", "password");

                        if (isAuthenticated == false) {
                            s_logger.info("Authentication failed for root with password");
                            return false;
                        }

                        Session sess = conn.openSession();
                        s_logger.info("Executing : " + commandElement.getTextContent());
                        sess.execCommand(commandElement.getTextContent());
                        Thread.sleep(60000);
                        sess.close();
                        conn.close();

                    } catch (Exception ex) {
                        s_logger.error(ex);
                        return false;
                    }
                }
            }

            else {
                //new command
                ApiCommand api = new ApiCommand(fstElmnt, this.getParam(), this.getCommands());

                //send a command
                api.sendCommand(this.getClient(), null);

                //verify the response of the command
                if ((api.getResponseType() == ResponseType.ERROR) && (api.getResponseCode() == 200)) {
                    s_logger.error("Test case " + api.getTestCaseInfo() +
                        " failed. Command that was supposed to fail, passed. The command was sent with the following url " + api.getUrl());
                    error++;
                } else if ((api.getResponseType() != ResponseType.ERROR) && (api.getResponseCode() == 200)) {
                    //verify if response is suppposed to be empty
                    if (api.getResponseType() == ResponseType.EMPTY) {
                        if (api.isEmpty() == true) {
                            s_logger.info("Test case " + api.getTestCaseInfo() + " passed. Empty response was returned as expected. Command was sent with url " +
                                api.getUrl());
                        } else {
                            s_logger.error("Test case " + api.getTestCaseInfo() + " failed. Empty response was expected. Command was sent with url " + api.getUrl());
                        }
                    } else {
                        if (api.isEmpty() != false)
                            s_logger.error("Test case " + api.getTestCaseInfo() + " failed. Non-empty response was expected. Command was sent with url " + api.getUrl());
                        else {
                            //set parameters for the future use
                            if (api.setParam(this.getParam()) == false) {
                                s_logger.error("Exiting the test...Command " + api.getName() +
                                    " didn't return parameters needed for the future use. The command was sent with url " + api.getUrl());
                                return false;
                            } else if (api.getTestCaseInfo() != null) {
                                s_logger.info("Test case " + api.getTestCaseInfo() + " passed. Command was sent with the url " + api.getUrl());
                            }
                        }
                    }
                } else if ((api.getResponseType() != ResponseType.ERROR) && (api.getResponseCode() != 200)) {
                    s_logger.error("Command " + api.getName() + " failed with an error code " + api.getResponseCode() + " . Command was sent with url  " + api.getUrl());
                    if (api.getRequired() == true) {
                        s_logger.info("The command is required for the future use, so exiging");
                        return false;
                    }
                    error++;
                } else if (api.getTestCaseInfo() != null) {
                    s_logger.info("Test case " + api.getTestCaseInfo() + " passed. Command that was supposed to fail, failed. Command was sent with url " + api.getUrl());

                }
            }
        }

        //verify events with userid parameter - test case 97
        HashMap<String, Integer> expectedEvents = new HashMap<String, Integer>();
        expectedEvents.put("VM.START", 1);
        boolean eventResult =
            ApiCommand.verifyEvents(expectedEvents, "INFO", "http://" + this.getParam().get("hostip") + ":8096", "userid=" + this.getParam().get("userid1") +
                "&type=VM.START");
        s_logger.info("Test case 97 - listEvent command verification result is  " + eventResult);

        //verify error events
        eventResult =
            ApiCommand.verifyEvents("../metadata/error_events.properties", "ERROR", "http://" + this.getParam().get("hostip") + ":8096",
                this.getParam().get("erroruseraccount"));
        s_logger.info("listEvent command verification result is  " + eventResult);

        if (error != 0)
            return false;
        else
            return true;
    }
}
