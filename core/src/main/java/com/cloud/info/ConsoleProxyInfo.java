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

import org.apache.commons.lang3.StringUtils;

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
        this.proxyPort = port;
        this.proxyUrlPort = proxyUrlPort;
        this.proxyAddress = this.formatProxyAddress(consoleProxyUrlDomain, proxyIpAddress);

        if (sslEnabled) {
            proxyImageUrl = "https://" + proxyAddress;
            if (proxyUrlPort != 443) {
                proxyImageUrl += ":" + this.proxyUrlPort;
            }

        } else {
            proxyImageUrl = "http://" + proxyAddress;
            if (proxyUrlPort != 80) {
                proxyImageUrl += ":" + proxyUrlPort;
            }
        }
    }

    private String formatProxyAddress(String consoleProxyUrlDomain, String proxyIpAddress) {
        StringBuffer sb = new StringBuffer();
        // Domain in format *.example.com, proxy IP is 1.2.3.4 --> 1-2-3-4.example.com
        if (consoleProxyUrlDomain.startsWith("*")) {
            sb.append(proxyIpAddress.replaceAll("\\.", "-"));
            sb.append(consoleProxyUrlDomain.substring(1)); // skip the *

        // Otherwise we assume a valid domain if config not blank
        } else if (StringUtils.isNotBlank(consoleProxyUrlDomain)) {
            sb.append(consoleProxyUrlDomain);

        // Blank config, we use the proxy IP
        } else {
            sb.append(proxyIpAddress);
        }
        return sb.toString();
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
