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

import java.util.ArrayList;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.log4j.NDC;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;

public class GuestNetwork implements Runnable {
    protected Logger logger = LogManager.getLogger(getClass());

    private String publicIp;
    private ArrayList<VirtualMachine> virtualMachines;
    private int retryNum;

    public GuestNetwork(String publicIp, int retryNum) {
        this.publicIp = publicIp;
        this.retryNum = retryNum;
    }

    public ArrayList<VirtualMachine> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(ArrayList<VirtualMachine> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    @Override
    public void run() {
        NDC.push("Following thread has started" + Thread.currentThread().getName());
        int retry = 0;

        //Start copying files between machines in the network
        logger.info("The size of the array is " + this.virtualMachines.size());
        while (true) {
            try {
                if (retry > 0) {
                    logger.info("Retry attempt : " + retry + " ...sleeping 120 seconds before next attempt");
                    Thread.sleep(120000);
                }
                for (VirtualMachine vm : this.virtualMachines) {

                    logger.info("Attempting to SSH into linux host " + this.publicIp + " with retry attempt: " + retry);
                    Connection conn = new Connection(this.publicIp);
                    conn.connect(null, 600000, 600000);

                    logger.info("SSHed successfully into linux host " + this.publicIp);

                    boolean isAuthenticated = conn.authenticateWithPassword("root", "password");

                    if (isAuthenticated == false) {
                        logger.info("Authentication failed");
                    }
                    //execute copy command
                    Session sess = conn.openSession();
                    String fileName;
                    Random ran = new Random();
                    fileName = Math.abs(ran.nextInt()) + "-file";
                    String copyCommand = new String("./scpScript " + vm.getPrivateIp() + " " + fileName);
                    logger.info("Executing " + copyCommand);
                    sess.execCommand(copyCommand);
                    Thread.sleep(120000);
                    sess.close();

                    //execute wget command
                    sess = conn.openSession();
                    String downloadCommand =
                        new String("wget http://172.16.0.220/scripts/checkDiskSpace.sh; chmod +x *sh; ./checkDiskSpace.sh; rm -rf checkDiskSpace.sh");
                    logger.info("Executing " + downloadCommand);
                    sess.execCommand(downloadCommand);
                    Thread.sleep(120000);
                    sess.close();

                    //close the connection
                    conn.close();
                }
            } catch (Exception ex) {
                logger.error(ex);
                retry++;
                if (retry == retryNum) {
                    logger.info("Performance Guest Network test failed with error " + ex.getMessage());
                }
            }
        }

    }
}
