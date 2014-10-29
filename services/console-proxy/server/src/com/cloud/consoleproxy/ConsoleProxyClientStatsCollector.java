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
package com.cloud.consoleproxy;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 * ConsoleProxyClientStatsCollector collects client stats for console proxy agent to report
 */
public class ConsoleProxyClientStatsCollector {

    ArrayList<ConsoleProxyConnection> connections;

    public ConsoleProxyClientStatsCollector() {
    }

    public ConsoleProxyClientStatsCollector(Hashtable<String, ConsoleProxyClient> connMap) {
        setConnections(connMap);
    }

    public String getStatsReport() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    public void getStatsReport(OutputStreamWriter os) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        gson.toJson(this, os);
    }

    private void setConnections(Hashtable<String, ConsoleProxyClient> connMap) {

        ArrayList<ConsoleProxyConnection> conns = new ArrayList<ConsoleProxyConnection>();
        Enumeration<String> e = connMap.keys();
        while (e.hasMoreElements()) {
            synchronized (connMap) {
                String key = e.nextElement();
                ConsoleProxyClient client = connMap.get(key);

                ConsoleProxyConnection conn = new ConsoleProxyConnection();

                conn.id = client.getClientId();
                conn.clientInfo = "";
                conn.host = client.getClientHostAddress();
                conn.port = client.getClientHostPort();
                conn.tag = client.getClientTag();
                conn.createTime = client.getClientCreateTime();
                conn.lastUsedTime = client.getClientLastFrontEndActivityTime();
                conns.add(conn);
            }
        }
        connections = conns;
    }

    public static class ConsoleProxyConnection {
        public int id;
        public String clientInfo;
        public String host;
        public int port;
        public String tag;
        public long createTime;
        public long lastUsedTime;

        public ConsoleProxyConnection() {
        }
    }
}
