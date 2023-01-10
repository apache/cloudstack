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

import com.cloud.test.regression.ApiCommand.ResponseType;

public class VMApiTest extends TestCase {

    public VMApiTest() {
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

            //send a command
            api.sendCommand(this.getClient(), this.getConn());

            //verify the response of the command
            if ((api.getResponseType() == ResponseType.ERROR) && (api.getResponseCode() == 200)) {
                logger.error("Test case " + api.getTestCaseInfo() + " failed. Command that was supposed to fail, passed. The command was sent with the following url " +
                    api.getUrl());
                error++;
            } else if ((api.getResponseType() != ResponseType.ERROR) && (api.getResponseCode() == 200)) {
                //set parameters for the future use
                if (api.setParam(this.getParam()) == false) {
                    logger.error("Exiting the test...Command " + api.getName() + " didn't return parameters needed for the future use. The command was sent with url " +
                        api.getUrl());
                    return false;
                }
                //verify parameters
                if (api.verifyParam() == false) {
                    logger.error("Test " + api.getTestCaseInfo() + " failed. Verification for returned parameters failed. The command was sent with url " +
                        api.getUrl());
                    error++;
                } else {
                    logger.info("Test " + api.getTestCaseInfo() + " passed");
                }
            } else if ((api.getResponseType() != ResponseType.ERROR) && (api.getResponseCode() != 200)) {
                logger.error("Test case  " + api.getTestCaseInfo() + " failed with an error code " + api.getResponseCode() + " . Command was sent with url  " +
                    api.getUrl());
                if (api.getRequired() == true) {
                    logger.info("The command is required for the future use, so exiging");
                    return false;
                }
                error++;
            } else if (api.getTestCaseInfo() != null) {
                logger.info("Test case " + api.getTestCaseInfo() + " passed");

            }
        }
        if (error != 0)
            return false;
        else
            return true;
    }
}
