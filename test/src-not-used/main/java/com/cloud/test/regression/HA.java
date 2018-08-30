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

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.cloud.test.regression.ApiCommand.ResponseType;

public class HA extends TestCase {

    public static final Logger s_logger = Logger.getLogger(HA.class.getName());

    public HA() {
        this.setClient();
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

            //verify the response parameters
            if ((api.getResponseCode() != 200) && (api.getRequired() == true)) {
                s_logger.error("Exiting the test....Command " + api.getName() + " required for the future run, failed with an error code " + api.getResponseCode() +
                    ". Command was sent with the url " + api.getUrl());
                return false;
            } else if ((api.getResponseCode() != 200) && (api.getResponseType() != ResponseType.ERROR)) {
                error++;
                s_logger.error("Command " + api.getTestCaseInfo() + " failed with an error code " + api.getResponseCode() + " . Command was sent with url  " +
                    api.getUrl());
            } else if ((api.getResponseCode() == 200) && (api.getResponseType() == ResponseType.ERROR)) {
                error++;
                s_logger.error("Command " + api.getTestCaseInfo() + " which was supposed to failed, passed. The command was sent with url  " + api.getUrl());
            } else {
                //set parameters for the future use
                if (api.setParam(this.getParam()) == false) {
                    s_logger.error("Exiting the test...Command " + api.getName() + " didn't return parameters needed for the future use. Command was sent with url " +
                        api.getUrl());
                    return false;
                }
                s_logger.info("Command " + api.getTestCaseInfo() + " passed");
            }
        }

        if (error != 0)
            return false;
        else
            return true;
    }
}
