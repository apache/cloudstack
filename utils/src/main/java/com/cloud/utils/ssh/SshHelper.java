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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import org.apache.log4j.Logger;
import org.joda.time.Duration;

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;
import com.cloud.utils.Pair;

public class SshHelper {
    private static final int DEFAULT_CONNECT_TIMEOUT = 180000;
    private static final int DEFAULT_KEX_TIMEOUT = 60000;

    private static final Logger s_logger = Logger.getLogger(SshHelper.class);

    public static Pair<Boolean, String> sshExecute(String host, int port, String user, File pemKeyFile, String password, String command) throws Exception {

        return sshExecute(host, port, user, pemKeyFile, password, command, DEFAULT_CONNECT_TIMEOUT, DEFAULT_KEX_TIMEOUT, 120000);
    }

    public static void scpTo(String host, int port, String user, File pemKeyFile, String password, String remoteTargetDirectory, String localFile, String fileMode)
            throws Exception {

        scpTo(host, port, user, pemKeyFile, password, remoteTargetDirectory, localFile, fileMode, DEFAULT_CONNECT_TIMEOUT, DEFAULT_KEX_TIMEOUT);
    }

    public static void scpTo(String host, int port, String user, File pemKeyFile, String password, String remoteTargetDirectory, byte[] data, String remoteFileName,
            String fileMode) throws Exception {

        scpTo(host, port, user, pemKeyFile, password, remoteTargetDirectory, data, remoteFileName, fileMode, DEFAULT_CONNECT_TIMEOUT, DEFAULT_KEX_TIMEOUT);
    }

    public static void scpFrom(String host, int port, String user, File permKeyFile, String localTargetDirectory, String remoteTargetFile) throws Exception {
        com.trilead.ssh2.Connection conn = null;
        com.trilead.ssh2.SCPClient scpClient = null;

        try {
            conn = new com.trilead.ssh2.Connection(host, port);
            conn.connect(null, DEFAULT_CONNECT_TIMEOUT, DEFAULT_KEX_TIMEOUT);

            if (!conn.authenticateWithPublicKey(user, permKeyFile, null)) {
                String msg = "Failed to authentication SSH user " + user + " on host " + host;
                s_logger.error(msg);
                throw new Exception(msg);
            }
            scpClient = conn.createSCPClient();

            scpClient.get(remoteTargetFile, localTargetDirectory);

        } finally {
            if (conn != null) {
                conn.close();
            }
        }
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

    public static Pair<Boolean, String> sshExecute(String host, int port, String user, File pemKeyFile, String password, String command, Duration connectTimeout,
            Duration kexTimeout, Duration waitTime) throws Exception {
        return sshExecute(host, port, user, pemKeyFile, password, command, (int)connectTimeout.getMillis(), (int)kexTimeout.getMillis(), (int)waitTime.getMillis());
    }

    public static Pair<Boolean, String> sshExecute(String host, int port, String user, File pemKeyFile, String password, String command, int connectTimeoutInMs, int kexTimeoutInMs,
            int waitResultTimeoutInMs) throws Exception {

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
            sess = openConnectionSession(conn);

            sess.execCommand(command);

            InputStream stdout = sess.getStdout();
            InputStream stderr = sess.getStderr();

            byte[] buffer = new byte[8192];
            StringBuffer sbResult = new StringBuffer();

            int currentReadBytes = 0;
            while (true) {
                throwSshExceptionIfStdoutOrStdeerIsNull(stdout, stderr);

                if ((stdout.available() == 0) && (stderr.available() == 0)) {
                    int conditions = sess.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.EOF | ChannelCondition.EXIT_STATUS,
                            waitResultTimeoutInMs);

                    throwSshExceptionIfConditionsTimeout(conditions);

                    if ((conditions & ChannelCondition.EXIT_STATUS) != 0) {
                        break;
                    }

                    if (canEndTheSshConnection(waitResultTimeoutInMs, sess, conditions)) {
                        break;
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

            if (StringUtils.isBlank(result)) {
                try {
                    result = IOUtils.toString(stdout, StandardCharsets.UTF_8);
                }
                catch (IOException e) {
                    s_logger.error("Couldn't get content of input stream due to: " + e.getMessage());
                    return new Pair<Boolean, String>(false, result);
                }
            }

            if (sess.getExitStatus() == null) {
                //Exit status is NOT available. Returning failure result.
                s_logger.error(String.format("SSH execution of command %s has no exit status set. Result output: %s", command, result));
                return new Pair<Boolean, String>(false, result);
            }

            if (sess.getExitStatus() != null && sess.getExitStatus().intValue() != 0) {
                s_logger.error(String.format("SSH execution of command %s has an error status code in return. Result output: %s", command, result));
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


    protected static Session openConnectionSession(Connection conn) throws IOException, InterruptedException {
        Session sess = conn.openSession();
        return sess;
    }

    /**
     * Handles the SSH connection in case of timeout or exit. If the session ends with a timeout
     * condition, it throws an exception; if the channel reaches an end of file condition, but it
     * does not have an exit status, it returns true to break the loop; otherwise, it returns
     * false.
     */
    protected static boolean canEndTheSshConnection(int waitResultTimeoutInMs, com.trilead.ssh2.Session sess, int conditions) throws SshException {
        if (isChannelConditionEof(conditions)) {
            int newConditions = sess.waitForCondition(ChannelCondition.EXIT_STATUS, waitResultTimeoutInMs);
            throwSshExceptionIfConditionsTimeout(newConditions);
            if ((newConditions & ChannelCondition.EXIT_STATUS) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * It throws a {@link SshException} if the channel condition is {@link ChannelCondition#TIMEOUT}
     */
    protected static void throwSshExceptionIfConditionsTimeout(int conditions) throws SshException {
        if ((conditions & ChannelCondition.TIMEOUT) != 0) {
            String msg = "Timed out in waiting for SSH execution exit status";
            s_logger.error(msg);
            throw new SshException(msg);
        }
    }

    /**
     * Checks if the channel condition mask is of {@link ChannelCondition#EOF} and not
     * {@link ChannelCondition#STDERR_DATA} or {@link ChannelCondition#STDOUT_DATA}.
     */
    protected static boolean isChannelConditionEof(int conditions) {
        if ((conditions & ChannelCondition.EOF) != 0) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the SSH session {@link com.trilead.ssh2.Session#getStdout()} or
     * {@link com.trilead.ssh2.Session#getStderr()} is null.
     */
    protected static void throwSshExceptionIfStdoutOrStdeerIsNull(InputStream stdout, InputStream stderr) throws SshException {
        if (stdout == null || stderr == null) {
            String msg = "Stdout or Stderr of SSH session is null";
            s_logger.error(msg);
            throw new SshException(msg);
        }
    }
}
