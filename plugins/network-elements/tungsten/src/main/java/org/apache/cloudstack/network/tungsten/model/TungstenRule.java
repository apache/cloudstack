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
package org.apache.cloudstack.network.tungsten.model;

public class TungstenRule {
    private final String ruleId;
    private final String action;
    private final String direction;
    private final String protocol;
    private final String srcIpPrefix;
    private final int srcIpPrefixLen;
    private final int srcStartPort;
    private final int srcEndPort;
    private final String dstIpPrefix;
    private final int dstIpPrefixLen;
    private final int dstStartPort;
    private final int dstEndPort;

    public TungstenRule(final String ruleId, final String action, final String direction, final String protocol,
        final String srcIpPrefix, final int srcIpPrefixLen, final int srcStartPort, final int srcEndPort,
        final String dstIpPrefix, final int dstIpPrefixLen, final int dstStartPort, final int dstEndPort) {
        this.ruleId = ruleId;
        this.action = action;
        this.direction = direction;
        this.protocol = protocol;
        this.srcIpPrefix = srcIpPrefix;
        this.srcIpPrefixLen = srcIpPrefixLen;
        this.srcStartPort = srcStartPort;
        this.srcEndPort = srcEndPort;
        this.dstIpPrefix = dstIpPrefix;
        this.dstIpPrefixLen = dstIpPrefixLen;
        this.dstStartPort = dstStartPort;
        this.dstEndPort = dstEndPort;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getAction() {
        return action;
    }

    public String getDirection() {
        return direction;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getSrcIpPrefix() {
        return srcIpPrefix;
    }

    public int getSrcIpPrefixLen() {
        return srcIpPrefixLen;
    }

    public int getSrcStartPort() {
        return srcStartPort;
    }

    public int getSrcEndPort() {
        return srcEndPort;
    }

    public String getDstIpPrefix() {
        return dstIpPrefix;
    }

    public int getDstIpPrefixLen() {
        return dstIpPrefixLen;
    }

    public int getDstStartPort() {
        return dstStartPort;
    }

    public int getDstEndPort() {
        return dstEndPort;
    }
}
