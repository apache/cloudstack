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

import com.cloud.agent.api.to.DhcpTO;

import java.util.List;

public class DnsMasqConfigCommand extends NetworkElementCommand {
    String domain;
    String dns1;
    String dns2;
    String internal_dns1;
    String internal_dns2;
    List<DhcpTO> dhcpTOs;
    boolean useExternal_dns;
    String domain_suffix;
    boolean dns;

    public DnsMasqConfigCommand(String domain, List<DhcpTO> dhcpTOs, String dns1, String dns2, String internal_dns1, String internal_dns2) {
        this.domain = domain;
        this.dhcpTOs = dhcpTOs;
        this.dns1= dns1;
        this.dns2= dns2;
        this.internal_dns1 = internal_dns1;
        this.internal_dns2 = internal_dns2;

    }

    public List<DhcpTO> getIps() {
        return dhcpTOs;
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

    public void setUseExternalDns(boolean useExternal_dns) {
        this.useExternal_dns = useExternal_dns;
    }

    public void setDomainSuffix(String domain_suffix) {
        this.domain_suffix = domain_suffix;
    }

    public void setIfDnsProvided(boolean dns) {
        this.dns =dns;
    }

    public String getDomainSuffix() {
        return this.domain_suffix;
    }

    public boolean getUseExternalDns() {
        return useExternal_dns;
    }

    public boolean isDnsProvided() {
        return dns;
    }


}
