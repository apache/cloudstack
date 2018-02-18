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

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ServerResource;

public abstract class CitrixConsoleProxyLoadCommandWrapper<T extends Command, A extends Answer, R extends ServerResource> extends CommandWrapper<Command, Answer, ServerResource> {

    private static final Logger s_logger = Logger.getLogger(CitrixConsoleProxyLoadCommandWrapper.class);

    protected Answer executeProxyLoadScan(final Command cmd, final long proxyVmId, final String proxyVmName, final String proxyManagementIp, final int cmdPort) {
        String result = null;

        final StringBuffer sb = new StringBuffer();
        sb.append("http://").append(proxyManagementIp).append(":" + cmdPort).append("/cmd/getstatus");

        boolean success = true;
        try {
            final URL url = new URL(sb.toString());
            final URLConnection conn = url.openConnection();

            // setting TIMEOUTs to avoid possible waiting until death situations
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            final InputStream is = conn.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
            final StringBuilder sb2 = new StringBuilder();
            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    sb2.append(line + "\n");
                }
                result = sb2.toString();
            } catch (final IOException e) {
                success = false;
            } finally {
                try {
                    is.close();
                } catch (final IOException e) {
                    s_logger.warn("Exception when closing , console proxy address : " + proxyManagementIp);
                    success = false;
                }
            }
        } catch (final IOException e) {
            s_logger.warn("Unable to open console proxy command port url, console proxy address : " + proxyManagementIp);
            success = false;
        }

        return new ConsoleProxyLoadAnswer(cmd, proxyVmId, proxyVmName, success, result);
    }
}
