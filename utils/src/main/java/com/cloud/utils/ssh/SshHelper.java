//
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
//

package com.cloud.utils.ssh;

import java.io.File;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.trilead.ssh2.ChannelCondition;

import com.cloud.utils.Pair;

public class SshHelper {
    private static final int DEFAULT_CONNECT_TIMEOUT = 60000;
    private static final int DEFAULT_KEX_TIMEOUT = 60000;

    private static final Logger s_logger = Logger.getLogger(SshHelper.class);

    public static Pair<Boolean, String> sshExecute(String host, int port, String user, File pemKeyFile, String password, String command) throws Exception {

        return sshExecute(host, port, user, pemKeyFile, password, command, 60000, 60000, 120000);
    }

    public static void scpTo(String host, int port, String user, File pemKeyFile, String password, String remoteTargetDirectory, String localFile, String fileMode)
        throws Exception {

        scpTo(host, port, user, pemKeyFile, password, remoteTargetDirectory, localFile, fileMode, DEFAULT_CONNECT_TIMEOUT, DEFAULT_KEX_TIMEOUT);
    }

    public static void scpTo(String host, int port, String user, File pemKeyFile, String password, String remoteTargetDirectory, byte[] data, String remoteFileName,
        String fileMode) throws Exception {

        scpTo(host, port, user, pemKeyFile, password, remoteTargetDirectory, data, remoteFileName, fileMode, DEFAULT_CONNECT_TIMEOUT, DEFAULT_KEX_TIMEOUT);
    }

    public static void scpTo(String host, int port, String user, File pemKeyFile, String password, String remoteTargetDirectory, String localFile, String fileMode,
        int connectTimeoutInMs, int kexTimeoutInMs) throws Exception {

        com.trilead.ssh2.Connection conn = null;
        com.trilead.ssh2.SCPClient scpClient = null;

        try {
            conn = new com.trilead.ssh2.Connection(host, port);
            conn.connect(null, connectTimeoutInMs, kexTimeoutInMs);

            if (pemKeyFile == null) {
                if (!conn.authenticateWithPassword(user, password)) {
                    String msg = "Failed to authentication SSH user " + user + " on host " + host;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
            } else {
                if (!conn.authenticateWithPublicKey(user, pemKeyFile, password)) {
                    String msg = "Failed to authentication SSH user " + user + " on host " + host;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
            }

            scpClient = conn.createSCPClient();

            if (fileMode != null)
                scpClient.put(localFile, remoteTargetDirectory, fileMode);
            else
                scpClient.put(localFile, remoteTargetDirectory);
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    public static void scpTo(String host, int port, String user, File pemKeyFile, String password, String remoteTargetDirectory, byte[] data, String remoteFileName,
        String fileMode, int connectTimeoutInMs, int kexTimeoutInMs) throws Exception {

        com.trilead.ssh2.Connection conn = null;
        com.trilead.ssh2.SCPClient scpClient = null;

        try {
            conn = new com.trilead.ssh2.Connection(host, port);
            conn.connect(null, connectTimeoutInMs, kexTimeoutInMs);

            if (pemKeyFile == null) {
                if (!conn.authenticateWithPassword(user, password)) {
                    String msg = "Failed to authentication SSH user " + user + " on host " + host;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
            } else {
                if (!conn.authenticateWithPublicKey(user, pemKeyFile, password)) {
                    String msg = "Failed to authentication SSH user " + user + " on host " + host;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
            }

            scpClient = conn.createSCPClient();
            if (fileMode != null)
                scpClient.put(data, remoteFileName, remoteTargetDirectory, fileMode);
            else
                scpClient.put(data, remoteFileName, remoteTargetDirectory);
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    public static Pair<Boolean, String> sshExecute(String host, int port, String user, File pemKeyFile, String password, String command, int connectTimeoutInMs,
        int kexTimeoutInMs, int waitResultTimeoutInMs) throws Exception {

        com.trilead.ssh2.Connection conn = null;
        com.trilead.ssh2.Session sess = null;
        try {
            conn = new com.trilead.ssh2.Connection(host, port);
            conn.connect(null, connectTimeoutInMs, kexTimeoutInMs);

            if (pemKeyFile == null) {
                if (!conn.authenticateWithPassword(user, password)) {
                    String msg = "Failed to authentication SSH user " + user + " on host " + host;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
            } else {
                if (!conn.authenticateWithPublicKey(user, pemKeyFile, password)) {
                    String msg = "Failed to authentication SSH user " + user + " on host " + host;
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
            }
            sess = conn.openSession();

            sess.execCommand(command);

            InputStream stdout = sess.getStdout();
            InputStream stderr = sess.getStderr();

            byte[] buffer = new byte[8192];
            StringBuffer sbResult = new StringBuffer();

            int currentReadBytes = 0;
            while (true) {
                if ((stdout.available() == 0) && (stderr.available() == 0)) {
                    int conditions =
                        sess.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.EOF | ChannelCondition.EXIT_STATUS,
                            waitResultTimeoutInMs);

                    if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                        String msg = "Timed out in waiting SSH execution result";
                        s_logger.error(msg);
                        throw new Exception(msg);
                    }

                    if ((conditions & ChannelCondition.EXIT_STATUS) != 0) {
                        if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
                            break;
                        }
                    }
                }

                while (stdout.available() > 0) {
                    currentReadBytes = stdout.read(buffer);
                    sbResult.append(new String(buffer, 0, currentReadBytes));
                }

                while (stderr.available() > 0) {
                    currentReadBytes = stderr.read(buffer);
                    sbResult.append(new String(buffer, 0, currentReadBytes));
                }
            }

            String result = sbResult.toString();

            if (sess.getExitStatus() == null) {
                //Exit status is NOT available. Returning failure result.
                return new Pair<Boolean, String>(false, result);
            }

            if (sess.getExitStatus() != null && sess.getExitStatus().intValue() != 0) {
                s_logger.error("SSH execution of command " + command + " has an error status code in return. result output: " + result);
                return new Pair<Boolean, String>(false, result);
            }

            return new Pair<Boolean, String>(true, result);
        } finally {
            if (sess != null)
                sess.close();

            if (conn != null)
                conn.close();
        }
    }
}
