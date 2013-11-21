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

package org.apache.cloudstack.region.gslb;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = ("global_load_balancer_lb_rule_map"))
public class GlobalLoadBalancerLbRuleMapVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "lb_rule_id")
    private long loadBalancerId;

    @Column(name = "gslb_rule_id")
    private long gslbLoadBalancerId;

    @Column(name = "weight")
    private long weight;

    @Column(name = "revoke")
    private boolean revoke = false;

    public GlobalLoadBalancerLbRuleMapVO() {
        this.weight = 1;
    }

    public GlobalLoadBalancerLbRuleMapVO(long loadBalancerId, long gslbLoadBalancerId, long weight) {
        this.loadBalancerId = loadBalancerId;
        this.gslbLoadBalancerId = gslbLoadBalancerId;
        this.revoke = false;
        this.weight = weight;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getLoadBalancerId() {
        return loadBalancerId;
    }

    public long getGslbLoadBalancerId() {
        return gslbLoadBalancerId;
    }

    public void setLoadBalancerId(long loadBalancerId) {
        this.loadBalancerId = loadBalancerId;
    }

    public void setGslbLoadBalancerId(long gslbLoadBalancerId) {
        this.gslbLoadBalancerId = gslbLoadBalancerId;
    }

    public boolean isRevoke() {
        return revoke;
    }

    public void setRevoke(boolean revoke) {
        this.revoke = revoke;
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }

    public long getWeight() {
        return weight;
    }
}
