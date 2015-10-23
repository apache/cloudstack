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
package com.cloud.test.utils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;

public class TestClient {
    private static long sleepTime = 180000L; // default 0
    private static boolean cleanUp = true;
    public static final Logger s_logger = Logger.getLogger(TestClient.class.getName());
    private static boolean repeat = true;
    private static int numOfUsers = 0;
    private static String[] users = null;
    private static boolean internet = true;

    private static final int MAX_RETRY_LINUX = 5;
    private static final int MAX_RETRY_WIN = 10;

    public static void main(String[] args) {
        String host = "http://localhost";
        String port = "8080";
        String testUrl = "/client/test";
        int numThreads = 1;

        try {
            // Parameters
            List<String> argsList = Arrays.asList(args);
            Iterator<String> iter = argsList.iterator();
            while (iter.hasNext()) {
                String arg = iter.next();
                // host
                if (arg.equals("-h")) {
                    host = "http://" + iter.next();
                }

                if (arg.equals("-p")) {
                    port = iter.next();
                }

                if (arg.equals("-t")) {
                    numThreads = Integer.parseInt(iter.next());
                }

                if (arg.equals("-s")) {
                    sleepTime = Long.parseLong(iter.next());
                }

                if (arg.equals("-c")) {
                    cleanUp = Boolean.parseBoolean(iter.next());
                    if (!cleanUp)
                        sleepTime = 0L; // no need to wait if we don't ever cleanup
                }

                if (arg.equals("-r")) {
                    repeat = Boolean.parseBoolean(iter.next());
                }

                if (arg.equals("-u")) {
                    numOfUsers = Integer.parseInt(iter.next());
                }

                if (arg.equals("-i")) {
                    internet = Boolean.parseBoolean(iter.next());
                }
            }

            final String server = host + ":" + port + testUrl;
            s_logger.info("Starting test against server: " + server + " with " + numThreads + " thread(s)");
            if (cleanUp)
                s_logger.info("Clean up is enabled, each test will wait " + sleepTime + " ms before cleaning up");

            if (numOfUsers > 0) {
                s_logger.info("Pre-generating users for test of size : " + numOfUsers);
                users = new String[numOfUsers];
                Random ran = new Random();
                for (int i = 0; i < numOfUsers; i++) {
                    users[i] = Math.abs(ran.nextInt()) + "-user";
                }
            }

            for (int i = 0; i < numThreads; i++) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        do {
                            String username = null;
                            try {
                                long now = System.currentTimeMillis();
                                Random ran = new Random();
                                if (users != null) {
                                    username = users[Math.abs(ran.nextInt()) % numOfUsers];
                                } else {
                                    username = Math.abs(ran.nextInt()) + "-user";
                                }
                                NDC.push(username);

                                String url = server + "?email=" + username + "&password=" + username + "&command=deploy";
                                s_logger.info("Launching test for user: " + username + " with url: " + url);
                                HttpClient client = new HttpClient();
                                HttpMethod method = new GetMethod(url);
                                int responseCode = client.executeMethod(method);
                                boolean success = false;
                                String reason = null;
                                if (responseCode == 200) {
                                    if (internet) {
                                        s_logger.info("Deploy successful...waiting 5 minute before SSH tests");
                                        Thread.sleep(300000L);  // Wait 60 seconds so the linux VM can boot up.

                                        s_logger.info("Begin Linux SSH test");
                                        reason = sshTest(method.getResponseHeader("linuxIP").getValue());

                                        if (reason == null) {
                                            s_logger.info("Linux SSH test successful");
                                            s_logger.info("Begin Windows SSH test");
                                            reason = sshWinTest(method.getResponseHeader("windowsIP").getValue());
                                        }
                                    }
                                    if (reason == null) {
                                        if (internet) {
                                            s_logger.info("Windows SSH test successful");
                                        } else {
                                            s_logger.info("deploy test successful....now cleaning up");
                                            if (cleanUp) {
                                                s_logger.info("Waiting " + sleepTime + " ms before cleaning up vms");
                                                Thread.sleep(sleepTime);
                                            } else {
                                                success = true;
                                            }
                                        }
                                        if (users == null) {
                                            s_logger.info("Sending cleanup command");
                                            url = server + "?email=" + username + "&password=" + username + "&command=cleanup";
                                        } else {
                                            s_logger.info("Sending stop DomR / destroy VM command");
                                            url = server + "?email=" + username + "&password=" + username + "&command=stopDomR";
                                        }
                                        method = new GetMethod(url);
                                        responseCode = client.executeMethod(method);
                                        if (responseCode == 200) {
                                            success = true;
                                        } else {
                                            reason = method.getStatusText();
                                        }
                                    } else {
                                        // Just stop but don't destroy the VMs/Routers
                                        s_logger.info("SSH test failed with reason '" + reason + "', stopping VMs");
                                        url = server + "?email=" + username + "&password=" + username + "&command=stop";
                                        responseCode = client.executeMethod(new GetMethod(url));
                                    }
                                } else {
                                    // Just stop but don't destroy the VMs/Routers
                                    reason = method.getStatusText();
                                    s_logger.info("Deploy test failed with reason '" + reason + "', stopping VMs");
                                    url = server + "?email=" + username + "&password=" + username + "&command=stop";
                                    client.executeMethod(new GetMethod(url));
                                }

                                if (success) {
                                    s_logger.info("***** Completed test for user : " + username + " in " + ((System.currentTimeMillis() - now) / 1000L) + " seconds");
                                } else {
                                    s_logger.info("##### FAILED test for user : " + username + " in " + ((System.currentTimeMillis() - now) / 1000L) +
                                        " seconds with reason : " + reason);
                                }
                            } catch (Exception e) {
                                s_logger.warn("Error in thread", e);
                                try {
                                    HttpClient client = new HttpClient();
                                    String url = server + "?email=" + username + "&password=" + username + "&command=stop";
                                    client.executeMethod(new GetMethod(url));
                                } catch (Exception e1) {
                                    s_logger.info("[ignored]"
                                            + "error while executing last resort stop attampt: " + e1.getLocalizedMessage());
                                }
                            } finally {
                                NDC.clear();
                            }
                        } while (repeat);
                    }
                }).start();
            }
        } catch (Exception e) {
            s_logger.error(e);
        }
    }

    private static String sshWinTest(String host) {
        if (host == null) {
            s_logger.info("Did not receive a host back from test, ignoring win ssh test");
            return null;
        }

        // We will retry 5 times before quitting
        int retry = 0;

        while (true) {
            try {
                if (retry > 0) {
                    s_logger.info("Retry attempt : " + retry + " ...sleeping 300 seconds before next attempt");
                    Thread.sleep(300000);
                }

                s_logger.info("Attempting to SSH into windows host " + host + " with retry attempt: " + retry);

                Connection conn = new Connection(host);
                conn.connect(null, 60000, 60000);

                s_logger.info("SSHed successfully into windows host " + host);
                boolean success = false;
                boolean isAuthenticated = conn.authenticateWithPassword("vmops", "vmops");
                if (isAuthenticated == false) {
                    return "Authentication failed";
                }
                SCPClient scp = new SCPClient(conn);

                scp.put("wget.exe", "");

                Session sess = conn.openSession();
                s_logger.info("Executing : wget http://172.16.0.220/dump.bin");
                sess.execCommand("wget http://172.16.0.220/dump.bin && dir dump.bin");

                InputStream stdout = sess.getStdout();
                InputStream stderr = sess.getStderr();

                byte[] buffer = new byte[8192];
                while (true) {
                    if ((stdout.available() == 0) && (stderr.available() == 0)) {
                        int conditions = sess.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.EOF, 120000);

                        if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                            s_logger.info("Timeout while waiting for data from peer.");
                            return null;
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
                        int len = stderr.read(buffer);
                    }
                }
                sess.close();
                conn.close();

                if (success) {
                    return null;
                } else {
                    retry++;
                    if (retry == MAX_RETRY_WIN) {
                        return "SSH Windows Network test fail";
                    }
                }
            } catch (Exception e) {
                retry++;
                if (retry == MAX_RETRY_WIN) {
                    return "SSH Windows Network test fail with error " + e.getMessage();
                }
            }
        }
    }

    private static String sshTest(String host) {
        if (host == null) {
            s_logger.info("Did not receive a host back from test, ignoring ssh test");
            return null;
        }

        // We will retry 5 times before quitting
        int retry = 0;

        while (true) {
            try {
                if (retry > 0) {
                    s_logger.info("Retry attempt : " + retry + " ...sleeping 120 seconds before next attempt");
                    Thread.sleep(120000);
                }

                s_logger.info("Attempting to SSH into linux host " + host + " with retry attempt: " + retry);

                Connection conn = new Connection(host);
                conn.connect(null, 60000, 60000);

                s_logger.info("SSHed successfully into linux host " + host);

                boolean isAuthenticated = conn.authenticateWithPassword("root", "password");

                if (isAuthenticated == false) {
                    return "Authentication failed";
                }
                boolean success = false;
                Session sess = conn.openSession();
                s_logger.info("Executing : wget http://172.16.0.220/dump.bin");
                sess.execCommand("wget http://172.16.0.220/dump.bin && ls -al dump.bin");

                InputStream stdout = sess.getStdout();
                InputStream stderr = sess.getStderr();

                byte[] buffer = new byte[8192];
                while (true) {
                    if ((stdout.available() == 0) && (stderr.available() == 0)) {
                        int conditions = sess.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.EOF, 120000);

                        if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                            s_logger.info("Timeout while waiting for data from peer.");
                            return null;
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
                        int len = stderr.read(buffer);
                    }
                }

                sess.close();
                conn.close();

                if (success) {
                    return null;
                } else {
                    retry++;
                    if (retry == MAX_RETRY_LINUX) {
                        return "SSH Linux Network test fail";
                    }
                }
            } catch (Exception e) {
                retry++;
                if (retry == MAX_RETRY_LINUX) {
                    return "SSH Linux Network test fail with error " + e.getMessage();
                }
            }
        }
    }
}
