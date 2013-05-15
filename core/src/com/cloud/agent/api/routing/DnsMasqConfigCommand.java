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
package com.cloud.agent.api.routing;

import com.cloud.agent.api.to.DnsmasqTO;

import java.util.List;

public class DnsMasqConfigCommand extends NetworkElementCommand {
    String domain;
    String dns1;
    String dns2;
    String internal_dns1;
    String internal_dns2;
    List<DnsmasqTO> dnsmasqTOs;

    public DnsMasqConfigCommand(String domain, List<DnsmasqTO> dnsmasqTOs, String dns1, String dns2, String internal_dns1, String internal_dns2) {
        this.domain = domain;
        this.dnsmasqTOs = dnsmasqTOs;
        this.dns1= dns1;
        this.dns2= dns2;
        this.internal_dns1 = internal_dns1;
        this.internal_dns2 = internal_dns2;

    }

    public List<DnsmasqTO> getIps() {
        return  dnsmasqTOs;
    }

    public  String getDomain() {
        return domain;
    }

    public String getDns1() {
        return dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public String getInternal_dns1() {
        return internal_dns1;
    }

    public String getInternal_dns2() {
        return internal_dns2;
    }

}
