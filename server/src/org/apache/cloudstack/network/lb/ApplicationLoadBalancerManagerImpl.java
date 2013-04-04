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

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.network.NetworkModel;
import com.cloud.network.rules.ApplicationLoadBalancerRule;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;

@Component
@Local(value = { ApplicationLoadBalancerService.class })
public class ApplicationLoadBalancerManagerImpl extends ManagerBase implements ApplicationLoadBalancerService {
    private static final Logger s_logger = Logger.getLogger(ApplicationLoadBalancerManagerImpl.class);
    
    @Inject NetworkModel _networkModel;
    
    @Override
    public ApplicationLoadBalancerRule createApplicationLoadBalancer(String name, String description, Scheme scheme, long sourceIpNetworkId, String sourceIp,
            int sourcePort, int instancePort, String algorithm, long networkId, long lbOwnerId) throws InsufficientAddressCapacityException,
            NetworkRuleConflictException {
        
        return null;
    }

    @Override
    public boolean deleteApplicationLoadBalancer(long id) {
        return true;
    }

    @Override
    public Pair<List<? extends ApplicationLoadBalancerRule>, Integer> listApplicationLoadBalancers() {
        return null;
    }

}
