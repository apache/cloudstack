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

package com.cloud.network;

import java.util.List;

import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.to.PortForwardingRuleTO;

public interface LoadBalancerConfigurator {
    public final static int ADD = 0;
    public final static int REMOVE = 1;
    public final static int STATS = 2;

    public String[] generateConfiguration(List<PortForwardingRuleTO> fwRules);

    public String[] generateConfiguration(LoadBalancerConfigCommand lbCmd);

    public String[][] generateFwRules(LoadBalancerConfigCommand lbCmd);
}
