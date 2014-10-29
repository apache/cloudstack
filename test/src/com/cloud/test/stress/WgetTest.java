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
package com.cloud.test.stress;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;

public class WgetTest {

    public static final int MAX_RETRY_LINUX = 1;
    public static final Logger s_logger = Logger.getLogger(WgetTest.class.getName());
    public static String host = "";
    public static String password = "rs-ccb35ea5";

    public static void main(String[] args) {

        // Parameters
        List<String> argsList = Arrays.asList(args);
        Iterator<String> iter = argsList.iterator();
        while (iter.hasNext()) {
            String arg = iter.next();
            // host
            if (arg.equals("-h")) {
                host = iter.next();
            }
            //password

            if (arg.equals("-p")) {
                password = iter.next();
            }

        }

        int i = 0;
        if (host == null || host.equals("")) {
            s_logger.info("Did not receive a host back from test, ignoring ssh test");
            System.exit(2);
        }

        if (password == null) {
            s_logger.info("Did not receive a password back from test, ignoring ssh test");
            System.exit(2);
        }
        int retry = 0;

        try {
            if (retry > 0) {
                s_logger.info("Retry attempt : " + retry + " ...sleeping 120 seconds before next attempt");
                Thread.sleep(120000);
            }

            s_logger.info("Attempting to SSH into linux host " + host + " with retry attempt: " + retry);

            Connection conn = new Connection(host);
            conn.connect(null, 60000, 60000);

            s_logger.info("User + ssHed successfully into linux host " + host);

            boolean isAuthenticated = conn.authenticateWithPassword("root", password);

            if (isAuthenticated == false) {
                s_logger.info("Authentication failed for root with password" + password);
                System.exit(2);
            }

            boolean success = false;
            String linuxCommand = null;

            if (i % 10 == 0)
                linuxCommand = "rm -rf *; wget http://192.168.1.250/dump.bin && ls -al dump.bin";
            else
                linuxCommand = "wget http://192.168.1.250/dump.bin && ls -al dump.bin";

            Session sess = conn.openSession();
            sess.execCommand(linuxCommand);

            InputStream stdout = sess.getStdout();
            InputStream stderr = sess.getStderr();

            byte[] buffer = new byte[8192];
            while (true) {
                if ((stdout.available() == 0) && (stderr.available() == 0)) {
                    int conditions = sess.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.EOF, 120000);

                    if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                        s_logger.info("Timeout while waiting for data from peer.");
                        System.exit(2);
                    }

                    if ((conditions & ChannelCondition.EOF) != 0) {
                        if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
                            break;
                        }
                    }
                }

                while (stdout.available() > 0) {
                    success = true;
                    int len = stdout.read(buffer);
                    if (len > 0) // this check is somewhat paranoid
                        s_logger.info(new String(buffer, 0, len));
                }

                while (stderr.available() > 0) {
                    /* int len = */stderr.read(buffer);
                }
            }

            sess.close();
            conn.close();

            if (!success) {
                retry++;
                if (retry == MAX_RETRY_LINUX) {
                    System.exit(2);
                }
            }
        } catch (Exception e) {
            retry++;
            s_logger.error("SSH Linux Network test fail with error");
            if (retry == MAX_RETRY_LINUX) {
                s_logger.error("Ssh test failed");
                System.exit(2);
            }
        }
    }

}
