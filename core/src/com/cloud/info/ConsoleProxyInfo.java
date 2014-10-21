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

package com.cloud.info;

public class ConsoleProxyInfo {

    private boolean sslEnabled;
    private String proxyAddress;
    private int proxyPort;
    private String proxyImageUrl;
    private int proxyUrlPort = 8000;

    public ConsoleProxyInfo(int proxyUrlPort) {
        this.proxyUrlPort = proxyUrlPort;
    }

    public ConsoleProxyInfo(boolean sslEnabled, String proxyIpAddress, int port, int proxyUrlPort, String consoleProxyUrlDomain) {
        this.sslEnabled = sslEnabled;

        if (sslEnabled) {
            StringBuffer sb = new StringBuffer();
            if (consoleProxyUrlDomain.startsWith("*")) {
                sb.append(proxyIpAddress);
                for (int i = 0; i < proxyIpAddress.length(); i++)
                    if (sb.charAt(i) == '.')
                        sb.setCharAt(i, '-');
                sb.append(consoleProxyUrlDomain.substring(1));//skip the *
            } else {
                //LB address
                sb.append(consoleProxyUrlDomain);
            }
            proxyAddress = sb.toString();
            proxyPort = port;
            this.proxyUrlPort = proxyUrlPort;

            proxyImageUrl = "https://" + proxyAddress;
            if (proxyUrlPort != 443)
                proxyImageUrl += ":" + this.proxyUrlPort;
        } else {
            proxyAddress = proxyIpAddress;
            proxyPort = port;
            this.proxyUrlPort = proxyUrlPort;

            proxyImageUrl = "http://" + proxyAddress;
            if (proxyUrlPort != 80)
                proxyImageUrl += ":" + proxyUrlPort;
        }
    }

    public String getProxyAddress() {
        return proxyAddress;
    }

    public void setProxyAddress(String proxyAddress) {
        this.proxyAddress = proxyAddress;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyImageUrl() {
        return proxyImageUrl;
    }

    public void setProxyImageUrl(String proxyImageUrl) {
        this.proxyImageUrl = proxyImageUrl;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }
}
