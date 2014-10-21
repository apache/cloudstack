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

public class CheckS2SVpnConnectionsAnswer extends Answer {
    Map<String, Boolean> ipToConnected;
    Map<String, String> ipToDetail;
    String details;

    protected CheckS2SVpnConnectionsAnswer() {
        ipToConnected = new HashMap<String, Boolean>();
        ipToDetail = new HashMap<String, String>();
    }

    public CheckS2SVpnConnectionsAnswer(CheckS2SVpnConnectionsCommand cmd, boolean result, String details) {
        super(cmd, result, details);
        ipToConnected = new HashMap<String, Boolean>();
        ipToDetail = new HashMap<String, String>();
        this.details = details;
        if (result) {
            parseDetails(details);
        }
    }

    protected void parseDetails(String details) {
        String[] lines = details.split("&");
        for (String line : lines) {
            String[] words = line.split(":");
            if (words.length != 3) {
                //Not something we can parse
                return;
            }
            String ip = words[0];
            boolean connected = words[1].equals("0");
            String detail = words[2];
            ipToConnected.put(ip, connected);
            ipToDetail.put(ip, detail);
        }
    }

    public boolean isConnected(String ip) {
        if (this.getResult()) {
            Boolean status = ipToConnected.get(ip);

            if (status != null) {
                return status;
            }

        }
        return false;
    }

    public String getDetail(String ip) {
        if (this.getResult()) {
            return ipToDetail.get(ip);
        }
        return null;
    }
}
