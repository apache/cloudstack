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

package org.apache.cloudstack.logsws.command;

import com.cloud.agent.api.Answer;

public class GetLogsSessionWebSocketAnswer  extends Answer {
    int port;
    String path;
    boolean ssl;

    public GetLogsSessionWebSocketAnswer(GetLogsSessionWebSocketCommand cmd, int port, String path, boolean useSsl) {
        super(cmd, true, "success");
        this.port = port;
        this.path = path;
        this.ssl = useSsl;
    }

    public GetLogsSessionWebSocketAnswer(GetLogsSessionWebSocketCommand cmd, String error) {
        super(null, false, error);
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public boolean isSsl() {
        return ssl;
    }
}
