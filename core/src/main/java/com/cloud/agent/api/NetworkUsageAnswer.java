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

package com.cloud.agent.api;

import com.cloud.agent.api.LogLevel.Log4jLevel;

@LogLevel(Log4jLevel.Debug)
public class NetworkUsageAnswer extends Answer {
    String routerName;
    Long bytesSent;
    Long bytesReceived;

    protected NetworkUsageAnswer() {
    }

    public NetworkUsageAnswer(NetworkUsageCommand cmd, String details, Long bytesSent, Long bytesReceived) {
        super(cmd, true, details);
        this.bytesReceived = bytesReceived;
        this.bytesSent = bytesSent;
        routerName = cmd.getDomRName();
    }

    public NetworkUsageAnswer(Command command, Exception e) {
        super(command, e);
    }

    public void setBytesReceived(Long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public Long getBytesReceived() {
        return bytesReceived;
    }

    public void setBytesSent(Long bytesSent) {
        this.bytesSent = bytesSent;
    }

    public Long getBytesSent() {
        return bytesSent;
    }

    public String getRouterName() {
        return routerName;
    }
}
