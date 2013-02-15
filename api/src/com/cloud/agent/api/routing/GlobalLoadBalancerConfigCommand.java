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

import com.cloud.agent.api.Command;

/**
 * GlobalLoadBalancerConfigCommand used for sending the GSLB configuration to GSLB service provider
 */
public class GlobalLoadBalancerConfigCommand extends Command {

    // FQDN that represents the globally load balanced service
    String domainName;

    // type of the globally load balanced service
    String serviceType;

    // load balancing method to distribute traffic across the sites participating in global service load balancing
    String lbMethod;

    // session persistence type
    String persistenceType;

    // true if global load balancer rule is being deleted
    boolean revoked;

    // list of the site details that are participating in the GSLB service
    SiteLoadBalancer[] siteLoadBalancers;

    public  void GlobalLoadBalancerConfigCommand(SiteLoadBalancer[] slbs,
                                                 String domainName,
                                                 String lbMethod,
                                                 String persistenceType,
                                                 String serviceType,
                                                 boolean revoked) {
        this.domainName = domainName;
        this.serviceType = serviceType;
        this.lbMethod = lbMethod;
        this.persistenceType = persistenceType;
        this.revoked = revoked;
        this.siteLoadBalancers = slbs;
    }

    public SiteLoadBalancer[] getSiteDetails() {
        return siteLoadBalancers;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getLoadBalancerMethod() {
        return lbMethod;
    }

    public String getPersistenceType() {
        return persistenceType;
    }

    public String getDomainName() {
        return domainName;
    }

    public boolean isForRevoke() {
        return revoked;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    // details of site participating in the GLSB service
    public class SiteLoadBalancer {
        // true if the site is local (GSLB provider receiving GlobalLoadBalancerConfigCommand is in same site)
        boolean local;

        // true if the sire needs to be removed from GSLB service
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

        public void SiteLoadBalancer(String gslbProviderIp, String gslbProviderPublicIP, boolean local, boolean revoked,
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

        public String getServicePublicIp() {
            return servicePublicIp;
        }

        public String getPublicPort() {
            return port;
        }

        public String getGslbProviderPrivateIp() {
            return gslbProviderIp;
        }

        public String getGslbProviderPublicIp() {
            return gslbProviderPublicIp;
        }

        public boolean isLocal() {
            return local;
        }

        public boolean forRevoke() {
            return revoked;
        }
    }
}
