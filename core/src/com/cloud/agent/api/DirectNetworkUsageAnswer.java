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

import java.util.HashMap;
import java.util.Map;

public class DirectNetworkUsageAnswer extends Answer {

    Map<String, long[]> ipBytesSentAndReceived;

    protected DirectNetworkUsageAnswer() {
    }

    public DirectNetworkUsageAnswer(Command command) {
        super(command);
        this.ipBytesSentAndReceived = new HashMap<String, long[]>();
    }

    public DirectNetworkUsageAnswer(Command command, Exception e) {
        super(command, e);
        this.ipBytesSentAndReceived = null;
    }

    public void put(String ip, long[] bytesSentAndReceived) {
        this.ipBytesSentAndReceived.put(ip, bytesSentAndReceived);
    }

    public long[] get(String ip) {
        long[] entry = ipBytesSentAndReceived.get(ip);
        if (entry == null) {
            ipBytesSentAndReceived.put(ip, new long[] {0, 0});
            return ipBytesSentAndReceived.get(ip);
        } else {
            return entry;
        }
    }

    public Map<String, long[]> getIpBytesSentAndReceived() {
        return ipBytesSentAndReceived;
    }
}
