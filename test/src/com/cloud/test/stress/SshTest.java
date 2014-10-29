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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;

public class SshTest {

    public static final Logger s_logger = Logger.getLogger(SshTest.class.getName());
    public static String host = "";
    public static String password = "password";
    public static String url = "http://google.com";

    public static void main(String[] args) {

        // Parameters
        List<String> argsList = Arrays.asList(args);
        Iterator<String> iter = argsList.iterator();
        while (iter.hasNext()) {
            String arg = iter.next();
            if (arg.equals("-h")) {
                host = iter.next();
            }
            if (arg.equals("-p")) {
                password = iter.next();
            }

            if (arg.equals("-u")) {
                url = iter.next();
            }
        }

        if (host == null || host.equals("")) {
            s_logger.info("Did not receive a host back from test, ignoring ssh test");
            System.exit(2);
        }

        if (password == null) {
            s_logger.info("Did not receive a password back from test, ignoring ssh test");
            System.exit(2);
        }

        try {
            s_logger.info("Attempting to SSH into host " + host);
            Connection conn = new Connection(host);
            conn.connect(null, 60000, 60000);

            s_logger.info("User + ssHed successfully into host " + host);

            boolean isAuthenticated = conn.authenticateWithPassword("root", password);

            if (isAuthenticated == false) {
                s_logger.info("Authentication failed for root with password" + password);
                System.exit(2);
            }

            String linuxCommand = "wget " + url;
            Session sess = conn.openSession();
            sess.execCommand(linuxCommand);
            sess.close();
            conn.close();

        } catch (Exception e) {
            s_logger.error("SSH test fail with error", e);
            System.exit(2);
        }
    }

}
