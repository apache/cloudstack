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
package com.cloud.test.longrun;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class BuildGuestNetwork {

    protected Logger logger = LogManager.getLogger(getClass());
    private static final int ApiPort = 8096;
    private static final int DeveloperPort = 8080;
    private static final String ApiUrl = "/client/api";
    private static int numVM = 1;
    private static long zoneId = -1L;
    private static long templateId = 3;
    private static long serviceOfferingId = 1;

    public static void main(String[] args) {

        List<String> argsList = Arrays.asList(args);
        Iterator<String> iter = argsList.iterator();
        String host = "http://localhost";
        int numThreads = 1;

        while (iter.hasNext()) {
            String arg = iter.next();
            if (arg.equals("-h")) {
                host = "http://" + iter.next();
            }
            if (arg.equals("-t")) {
                numThreads = Integer.parseInt(iter.next());
            }
            if (arg.equals("-n")) {
                numVM = Integer.parseInt(iter.next());
            }
            if (arg.equals("-z")) {
                zoneId = Integer.parseInt(iter.next());
            }

            if (arg.equals("-e")) {
                templateId = Integer.parseInt(iter.next());
            }

            if (arg.equals("-s")) {
                serviceOfferingId = Integer.parseInt(iter.next());
            }
        }

        final String server = host + ":" + ApiPort + "/";
        final String developerServer = host + ":" + DeveloperPort + ApiUrl;
        logger.info("Starting test in " + numThreads + " thread(s). Each thread is launching " + numVM + " VMs");

        for (int i = 0; i < numThreads; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        String username = null;
                        String singlePrivateIp = null;
                        Random ran = new Random();
                        username = Math.abs(ran.nextInt()) + "-user";

                        //Create User
                        User myUser = new User(username, username, server, developerServer);
                        try {
                            myUser.launchUser();
                            myUser.registerUser();
                        } catch (Exception e) {
                            logger.warn("Error code: ", e);
                        }

                        if (myUser.getUserId() != null) {
                            logger.info("User " + myUser.getUserName() + " was created successfully, starting VM creation");
                            //create VMs for the user
                            for (int i = 0; i < numVM; i++) {
                                //Create a new VM, add it to the list of user's VMs
                                VirtualMachine myVM = new VirtualMachine(myUser.getUserId());
                                myVM.deployVM(zoneId, serviceOfferingId, templateId, myUser.getDeveloperServer(), myUser.getApiKey(), myUser.getSecretKey());
                                myUser.getVirtualMachines().add(myVM);
                                singlePrivateIp = myVM.getPrivateIp();

                                if (singlePrivateIp != null) {
                                    logger.info("VM with private Ip " + singlePrivateIp + " was successfully created");
                                } else {
                                    logger.info("Problems with VM creation for a user" + myUser.getUserName());
                                    logger.info("Deployment failed");
                                    break;
                                }
                            }

                            logger.info("Deployment done..." + numVM + " VMs were created.");
                        }

                    } catch (Exception e) {
                        logger.error(e);
                    }
                }
            }).start();

        }
    }

}
