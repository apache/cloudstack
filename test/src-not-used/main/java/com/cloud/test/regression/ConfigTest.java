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

import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;

import com.cloud.test.regression.ApiCommand.ResponseType;

public class ConfigTest extends TestCase {

    public ConfigTest() {
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

            //new command
            ApiCommand api = new ApiCommand(fstElmnt, this.getParam(), this.getCommands());

            if (api.getName().equals("rebootManagementServer")) {

                logger.info("Attempting to SSH into management server " + this.getParam().get("hostip"));
                try {
                    Connection conn = new Connection(this.getParam().get("hostip"));
                    conn.connect(null, 60000, 60000);

                    logger.info("SSHed successfully into management server " + this.getParam().get("hostip"));

                    boolean isAuthenticated = conn.authenticateWithPassword("root", "password");

                    if (isAuthenticated == false) {
                        logger.info("Authentication failed for root with password");
                        return false;
                    }

                    String restartCommand = "service cloud-management restart; service cloud-usage restart";
                    Session sess = conn.openSession();
                    logger.info("Executing : " + restartCommand);
                    sess.execCommand(restartCommand);
                    Thread.sleep(120000);
                    sess.close();
                    conn.close();

                } catch (Exception ex) {
                    logger.error(ex);
                    return false;
                }
            } else {
                //send a command
                api.sendCommand(this.getClient(), null);

                //verify the response of the command
                if ((api.getResponseType() == ResponseType.ERROR) && (api.getResponseCode() == 200) && (api.getTestCaseInfo() != null)) {
                    logger.error("Test case " + api.getTestCaseInfo() +
                        "failed. Command that was supposed to fail, passed. The command was sent with the following url " + api.getUrl());
                    error++;
                } else if ((api.getResponseType() != ResponseType.ERROR) && (api.getResponseCode() == 200)) {
                    //set parameters for the future use
                    if (api.setParam(this.getParam()) == false) {
                        logger.error("Exiting the test...Command " + api.getName() +
                            " didn't return parameters needed for the future use. The command was sent with url " + api.getUrl());
                        return false;
                    } else {
                        //verify parameters
                        if (api.verifyParam() == false) {
                            logger.error("Command " + api.getName() + " failed. Verification for returned parameters failed. Command was sent with url " + api.getUrl());
                            error++;
                        } else if (api.getTestCaseInfo() != null) {
                            logger.info("Test case " + api.getTestCaseInfo() + " passed. Command was sent with the url " + api.getUrl());
                        }
                    }
                } else if ((api.getResponseType() != ResponseType.ERROR) && (api.getResponseCode() != 200)) {
                    logger.error("Command " + api.getName() + " failed with an error code " + api.getResponseCode() + " . Command was sent with url  " + api.getUrl() +
                        " Required: " + api.getRequired());
                    if (api.getRequired() == true) {
                        logger.info("The command is required for the future use, so exiging");
                        return false;
                    }
                    error++;
                } else if (api.getTestCaseInfo() != null) {
                    logger.info("Test case " + api.getTestCaseInfo() + " passed. Command that was supposed to fail, failed - test passed. Command was sent with url " +
                        api.getUrl());
                }
            }
        }
        if (error != 0)
            return false;
        else
            return true;
    }
}
