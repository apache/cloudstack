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
package com.cloud.agent.api.baremetal;

import com.cloud.agent.api.Command;

public class prepareCreateTemplateCommand extends Command {
    String ip;
    String mac;
    String netMask;
    String gateway;
    String dns;
    String template;

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public prepareCreateTemplateCommand(String ip, String mac, String netMask, String gateway, String dns, String template) {
        this.ip = ip;
        this.mac = mac;
        this.netMask = netMask;
        this.gateway = gateway;
        this.dns = dns;
        this.template = template;
    }

    public String getIp() {
        return ip;
    }

    public String getMac() {
        return mac;
    }

    public String getNetMask() {
        return netMask;
    }

    public String getGateWay() {
        return gateway;
    }

    public String getDns() {
        return dns;
    }

    public String getTemplate() {
        return template;
    }
}
