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

package org.apache.cloudstack.logsws;

import com.cloud.cluster.ManagementServerHostVO;

public class LogsWebSessionWebSocket {

    private ManagementServerHostVO managementServerHost;
    private int port;
    private String path;

    public LogsWebSessionWebSocket(final ManagementServerHostVO managementServerHost, final int port,
               final String path) {
        this.managementServerHost = managementServerHost;
        this.port = port;
        this.path = path;
    }

    public ManagementServerHostVO getManagementServerHost() {
        return managementServerHost;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }
}
