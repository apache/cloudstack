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
package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import java.io.File;

import com.cloud.utils.Pair;
import com.cloud.utils.ssh.SshHelper;


/**
 * This class is used to wrap the calls to several static methods. By doing so, we make easier to mock this class
 * and the methods wrapped here.
 */
public class XenServerUtilitiesHelper {

    public static final int TIMEOUT = 10000;
    public static final String SCRIPT_CMD_PATH = "sh /opt/cloud/bin/";

    public Pair<Boolean, String> executeSshWrapper(final String hostIp, final int port, final String username, final File pemFile, final String hostPasswd, final String command) throws Exception {
        final Pair<Boolean, String> result = SshHelper.sshExecute(hostIp, port, username, pemFile, hostPasswd, command, 60000, 60000, TIMEOUT);
        return result;
    }

    public String buildCommandLine(final String scriptPath, final String script, final String username, final String newPassword) {
        final StringBuilder cmdLine = new StringBuilder();
        cmdLine.append(scriptPath).append(script).append(' ').append(username).append(' ').append(newPassword);

        return cmdLine.toString();
    }
}