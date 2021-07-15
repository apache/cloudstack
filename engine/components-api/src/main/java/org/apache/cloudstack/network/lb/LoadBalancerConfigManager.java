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
package org.apache.cloudstack.network.lb;

import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

public interface LoadBalancerConfigManager extends Configurable {

    static final String DefaultLbSSLConfigurationCK = "default.lb.ssl.configuration";

    static final ConfigKey<String> DefaultLbSSLConfiguration = new ConfigKey<>("Advanced", String.class,
            DefaultLbSSLConfigurationCK, "none",
            "Default value of load balancer ssl configuration, could be 'none', 'old' or 'intermediate'",
            true, ConfigKey.Scope.Global);

    List<? extends LoadBalancerConfig> getNetworkLbConfigs(Long networkId);

    List<? extends LoadBalancerConfig> getVpcLbConfigs(Long vpcId);

    List<? extends LoadBalancerConfig> getRuleLbConfigs(Long ruleId);
}
