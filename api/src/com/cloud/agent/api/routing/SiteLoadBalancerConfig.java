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

// details of site participating in the GLSB service
public class SiteLoadBalancerConfig {
// true if the site is local (GSLB provider receiving GlobalLoadBalancerConfigCommand is in same site)
    boolean local;

    // true if the site needs to be removed from GSLB service
    boolean revoked;

    // service type of the 'site load balanced' service
    String serviceType;

    // public IP corresponding to the site load balanced service
    String servicePublicIp;

    // port corresponding to the site load balanced service
    String port;

    // IP corresponding to the GSLB service provider in the site.
    String gslbProviderIp;

    // Public IP corresponding to the GSLB service provider in the site.
    String gslbProviderPublicIp;

    public SiteLoadBalancerConfig(boolean revoked, String serviceType, String servicePublicIp, String port) {
        this.revoked = revoked;
        this.serviceType = serviceType;
        this.servicePublicIp = servicePublicIp;
        this.port = port;
    }

    public SiteLoadBalancerConfig(String gslbProviderIp, String gslbProviderPublicIP, boolean local, boolean revoked,
                                 String serviceType, String servicePublicIp, String port) {
        this.gslbProviderIp = gslbProviderIp;
        this.gslbProviderPublicIp = gslbProviderPublicIP;
        this.local = local;
        this.revoked = revoked;
        this.serviceType = serviceType;
        this.servicePublicIp = servicePublicIp;
        this.port = port;
    }

    public String getServiceType() {
        return  serviceType;
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

    public String getPublicPort() {
        return port;
    }

    public void setPublicPort(String port) {
        this.port = port;
    }

    public String getGslbProviderPrivateIp() {
        return gslbProviderIp;
    }

    public void setGslbProviderPrivateIp(String privateIp) {
        this.gslbProviderIp = privateIp;
    }

    public String getGslbProviderPublicIp() {
        return gslbProviderPublicIp;
    }

    public void setGslbProviderPublicIp(String publicIp) {
        this.gslbProviderPublicIp = publicIp;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public boolean forRevoke() {
        return revoked;
    }
}