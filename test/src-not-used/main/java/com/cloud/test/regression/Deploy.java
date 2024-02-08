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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Deploy extends TestCase {

    public Deploy() {
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
            api.sendCommand(this.getClient(), null);

            //verify the response of the command
            if (api.getResponseCode() != 200) {
                error++;
                logger.error("The command " + api.getUrl() + " failed");
            } else {
                logger.info("The command " + api.getUrl() + " passsed");
            }
        }
        if (error != 0)
            return false;
        else
            return true;
    }

    public static void main(String[] args) {

        List<String> argsList = Arrays.asList(args);
        Iterator<String> iter = argsList.iterator();
        String host = null;
        String file = null;

        while (iter.hasNext()) {
            String arg = iter.next();
            // management server host
            if (arg.equals("-h")) {
                host = iter.next();
            }
            if (arg.equals("-f")) {
                file = iter.next();
            }
        }

        Deploy deploy = new Deploy();

        ArrayList<String> inputFile = new ArrayList<String>();
        inputFile.add(file);
        deploy.setInputFile(inputFile);
        deploy.setTestCaseName("Management server deployment");
        deploy.getParam().put("hostip", host);
        deploy.getParam().put("apicommands", "../metadata/func/commands");
        deploy.setCommands();

        logger.info("Starting deployment against host " + host);

        boolean result = deploy.executeTest();
        if (result == false) {
            logger.error("DEPLOYMENT FAILED");
            System.exit(1);
        } else {
            logger.info("DEPLOYMENT IS SUCCESSFUL");
        }

    }

}
