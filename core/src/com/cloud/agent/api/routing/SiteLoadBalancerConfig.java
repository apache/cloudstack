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

package com.cloud.agent.api.routing;

// details of site participating in the GLSB service, represents configuration load balancer rule and the zone
// in which the rule is configured
public class SiteLoadBalancerConfig {

    // true if the site details are local to the zone receiving 'GlobalLoadBalancerConfigCommand'
    Boolean local;

    // true if the site needs to be removed from GSLB service
    Boolean revoked;

    // service type of the 'site load balanced' service
    String serviceType;

    // public IP corresponding to the site load balanced service
    String servicePublicIp;

    // port corresponding to the site load balanced service
    String servicePort;

    // Private IP corresponding to the GSLB service provider in the site.
    String gslbProviderPrivateIp;

    // Public IP corresponding to the GSLB service provider in the site.
    String gslbProviderPublicIp;

    // zone id in which site is located
    Long dataCenterId;

    // wight corresponding to this site
    Long weight = 1l;

    public SiteLoadBalancerConfig(Boolean revoked, String serviceType, String servicePublicIp, String servicePort, Long dataCenterId) {
        this.revoked = revoked;
        this.serviceType = serviceType;
        this.servicePublicIp = servicePublicIp;
        this.servicePort = servicePort;
        this.dataCenterId = dataCenterId;
    }

    public SiteLoadBalancerConfig(String gslbProviderPublicIP, String gslbProviderPrivateIp, Boolean local, Boolean revoked, String serviceType, String servicePublicIp,
            String port, Long dataCenterId) {
        this(revoked, serviceType, servicePublicIp, port, dataCenterId);
        this.gslbProviderPrivateIp = gslbProviderPrivateIp;
        this.gslbProviderPublicIp = gslbProviderPublicIP;
        this.local = local;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getServicePublicIp() {
        return servicePublicIp;
    }

    public void SetServicePublicIp(String servicePublicIp) {
        this.servicePublicIp = servicePublicIp;
    }

    public String getServicePort() {
        return servicePort;
    }

    public void setServicePort(String port) {
        this.servicePort = port;
    }

    public String getGslbProviderPrivateIp() {
        return gslbProviderPrivateIp;
    }

    public void setGslbProviderPrivateIp(String privateIp) {
        this.gslbProviderPrivateIp = privateIp;
    }

    public String getGslbProviderPublicIp() {
        return gslbProviderPublicIp;
    }

    public Long getDataCenterId() {
        return dataCenterId;
    }

    public void setGslbProviderPublicIp(String publicIp) {
        this.gslbProviderPublicIp = publicIp;
    }

    public Boolean isLocal() {
        return local;
    }

    public void setLocal(Boolean local) {
        this.local = local;
    }

    public Boolean forRevoke() {
        return revoked;
    }

    public void setWeight(Long weight) {
        assert (weight >= 1 && weight <= 100);
        this.weight = weight;
    }

    public Long getWeight() {
        return weight;
    }

}