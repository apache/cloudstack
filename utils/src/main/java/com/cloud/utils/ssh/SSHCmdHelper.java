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

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Session;
import org.apache.cloudstack.utils.security.KeyStoreUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.InputStream;

public class SSHCmdHelper {
    protected static Logger LOGGER = LogManager.getLogger(SSHCmdHelper.class);
    private static final int DEFAULT_CONNECT_TIMEOUT = 180000;
    private static final int DEFAULT_KEX_TIMEOUT = 60000;

    public static class SSHCmdResult {
        private int returnCode = -1;
        private String stdOut;
        private String stdErr;

        public SSHCmdResult(final int returnCode, final String stdOut, final String stdErr) {
            this.returnCode = returnCode;
            this.stdOut = stdOut;
            this.stdErr = stdErr;
        }

        @Override
        public String toString() {
            return String.format("SSH cmd result: return code=%d, stdout=%s, stderr=%s",
                    getReturnCode(), getStdOut().split("-----BEGIN")[0], getStdErr());
        }

        public boolean isSuccess() {
            return returnCode == 0;
        }

        public int getReturnCode() {
            return returnCode;
        }

        public void setReturnCode(int returnCode) {
            this.returnCode = returnCode;
        }

        public String getStdOut() {
            return stdOut;
        }

        public String getStdErr() {
            return stdErr;
        }
    }

    public static com.trilead.ssh2.Connection acquireAuthorizedConnection(String ip, String username, String password) {
        return acquireAuthorizedConnection(ip, 22, username, password);
    }

    public static com.trilead.ssh2.Connection acquireAuthorizedConnection(String ip, int port, String username, String password) {
        return acquireAuthorizedConnection(ip, 22, username, password, null);
    }

    public static boolean acquireAuthorizedConnectionWithPublicKey(final com.trilead.ssh2.Connection sshConnection, final String username, final String privateKey) {
        if (StringUtils.isNotBlank(privateKey)) {
            try {
                if (!sshConnection.authenticateWithPublicKey(username, privateKey.toCharArray(), null)) {
                    LOGGER.warn("Failed to authenticate with ssh key");
                    return false;
                }
                return true;
            } catch (IOException e) {
                LOGGER.warn("An exception occurred when authenticate with ssh key");
                return false;
            }
        }
        return false;
    }

    public static com.trilead.ssh2.Connection acquireAuthorizedConnection(String ip, int port, String username, String password, String privateKey) {
        com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(ip, port);
        try {
            sshConnection.connect(null, DEFAULT_CONNECT_TIMEOUT, DEFAULT_KEX_TIMEOUT);
            if (acquireAuthorizedConnectionWithPublicKey(sshConnection, username, privateKey)) {
                return sshConnection;
            };
            sshConnection = new com.trilead.ssh2.Connection(ip, port);
            sshConnection.connect(null, DEFAULT_CONNECT_TIMEOUT, DEFAULT_KEX_TIMEOUT);
            if (!sshConnection.authenticateWithPassword(username, password)) {
                String[] methods = sshConnection.getRemainingAuthMethods(username);
                StringBuffer mStr = new StringBuffer();
                for (int i = 0; i < methods.length; i++) {
                    mStr.append(methods[i]);
                }
                LOGGER.warn("SSH authorizes failed, support authorized methods are " + mStr);
                return null;
            }
            return sshConnection;
        } catch (IOException e) {
            LOGGER.warn("Get SSH connection failed", e);
            return null;
        }
    }

    public static void releaseSshConnection(com.trilead.ssh2.Connection sshConnection) {
        if (sshConnection != null) {
            sshConnection.close();
        }
    }

    public static boolean sshExecuteCmd(com.trilead.ssh2.Connection sshConnection, String cmd, int nTimes) {
        for (int i = 0; i < nTimes; i++) {
            try {
                final SSHCmdResult result = sshExecuteCmdOneShot(sshConnection, cmd);
                if (result.isSuccess()) {
                    return true;
                }
            } catch (SshException ignored) {
                continue;
            }
        }
        return false;
    }

    public static SSHCmdResult sshExecuteCmdWithResult(com.trilead.ssh2.Connection sshConnection, String cmd, int nTimes) {
        for (int i = 0; i < nTimes; i++) {
            try {
                final SSHCmdResult result = sshExecuteCmdOneShot(sshConnection, cmd);
                if (result.isSuccess()) {
                    return result;
                }
            } catch (SshException ignored) {
                continue;
            }
        }
        return new SSHCmdResult(-1, null, null);
    }

    public static boolean sshExecuteCmd(com.trilead.ssh2.Connection sshConnection, String cmd) {
        return sshExecuteCmd(sshConnection, cmd, 3);
    }

    public static SSHCmdResult sshExecuteCmdWithResult(com.trilead.ssh2.Connection sshConnection, String cmd) {
        return sshExecuteCmdWithResult(sshConnection, cmd, 3);
    }

    public static SSHCmdResult sshExecuteCmdOneShot(com.trilead.ssh2.Connection sshConnection, String cmd) throws SshException {
        LOGGER.debug("Executing cmd: " + cmd.split(KeyStoreUtils.KS_FILENAME)[0]);
        Session sshSession = null;
        try {
            sshSession = sshConnection.openSession();
            // There is a bug in Trilead library, wait a second before
            // starting a shell and executing commands, from http://spci.st.ewi.tudelft.nl/chiron/xref/nl/tudelft/swerl/util/SSHConnection.html
            Thread.sleep(1000);

            if (sshSession == null) {
                throw new SshException("Cannot open ssh session");
            }

            sshSession.execCommand(cmd);

            InputStream stdout = sshSession.getStdout();
            InputStream stderr = sshSession.getStderr();

            byte[] buffer = new byte[8192];
            StringBuffer sbStdoutResult = new StringBuffer();
            StringBuffer sbStdErrResult = new StringBuffer();

            int currentReadBytes = 0;
            while (true) {
                if (stdout == null || stderr == null) {
                    throw new SshException("stdout or stderr of ssh session is null");
                }
                if ((stdout.available() == 0) && (stderr.available() == 0)) {
                    int conditions = sshSession.waitForCondition(ChannelCondition.STDOUT_DATA
                                | ChannelCondition.STDERR_DATA | ChannelCondition.EOF | ChannelCondition.EXIT_STATUS,
                                120000);

                    if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                        String msg = "Timed out in waiting SSH execution result";
                        LOGGER.error(msg);
                        throw new Exception(msg);
                    }

                    if ((conditions & ChannelCondition.EXIT_STATUS) != 0) {
                        if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
                            break;
                        }
                    }

                    if ((conditions & ChannelCondition.EOF) != 0) {
                        if ((conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
                            break;
                        }
                    }
                }

                while (stdout.available() > 0) {
                    currentReadBytes = stdout.read(buffer);
                    sbStdoutResult.append(new String(buffer, 0, currentReadBytes));
                }

                while (stderr.available() > 0) {
                    currentReadBytes = stderr.read(buffer);
                    sbStdErrResult.append(new String(buffer, 0, currentReadBytes));
                }
            }

            final SSHCmdResult result = new SSHCmdResult(-1, sbStdoutResult.toString(), sbStdErrResult.toString());
            if (!StringUtils.isAllEmpty(result.getStdOut(), result.getStdErr())) {
                LOGGER.debug("SSH command: " + cmd.split(KeyStoreUtils.KS_FILENAME)[0] + "\nSSH command output:" + result.getStdOut().split("-----BEGIN")[0] + "\n" + result.getStdErr());
            }

            // exit status delivery might get delayed
            for(int i = 0 ; i<10 ; i++ ) {
                Integer status = sshSession.getExitStatus();
                if( status != null ) {
                    result.setReturnCode(status);
                    return result;
                }
                Thread.sleep(100);
            }
            return result;
        } catch (Exception e) {
            LOGGER.debug("SSH execution failed", e);
            throw new SshException("SSH execution failed " + e.getMessage());
        } finally {
            if (sshSession != null)
                sshSession.close();
        }
    }
}
