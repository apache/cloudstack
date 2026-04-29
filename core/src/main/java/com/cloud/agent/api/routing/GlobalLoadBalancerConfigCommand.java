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

import java.util.List;

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

    // unique Id given per gslb rule, that is uniquely represents glsb rule on all participant sites
    long gslbId;

    // true if global load balancer rule is being deleted
    boolean revoked;

    // list of the site details that are participating in the GSLB service
    List<SiteLoadBalancerConfig> siteLoadBalancers;

    public GlobalLoadBalancerConfigCommand(String domainName, String lbMethod, String persistenceType, String serviceType, long gslbId, boolean revoked) {
        this.domainName = domainName;
        this.serviceType = serviceType;
        this.lbMethod = lbMethod;
        this.persistenceType = persistenceType;
        this.gslbId = gslbId;
        this.revoked = revoked;
    }

    public List<SiteLoadBalancerConfig> getSiteDetails() {
        return siteLoadBalancers;
    }

    public void setSiteLoadBalancers(List<SiteLoadBalancerConfig> siteLoadBalancers) {
        this.siteLoadBalancers = siteLoadBalancers;
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

    public long getGslbId() {
        return this.gslbId;
    }

    public String getDomainName() {
        return domainName;
    }

    public boolean isForRevoke() {
        return revoked;
    }

    public void setForRevoke(boolean revoke) {
        this.revoked = revoke;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
