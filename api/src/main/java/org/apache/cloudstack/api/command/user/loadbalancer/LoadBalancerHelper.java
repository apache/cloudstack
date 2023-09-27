
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
package org.apache.cloudstack.api.command.user.loadbalancer;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.OwnedBy;
import com.cloud.utils.db.EntityManager;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.network.lb.LoadBalancerConfig;

public interface LoadBalancerHelper {

    static LoadBalancerConfig findConfigById(EntityManager entityMgr, Long id){
        LoadBalancerConfig config = entityMgr.findById(LoadBalancerConfig.class, id);
        if (config == null) {
            throw new InvalidParameterValueException("Unable to find load balancer config: " + id);
        }
        return config;
    }

    static String getSyncObjType(EntityManager entityMgr, Long id){
        LoadBalancerConfig config = findConfigById(entityMgr, id);
        return getSyncObjType(config.getNetworkId(), config.getVpcId());
    }

    static String getSyncObjType(Long networkId, Long vpcId){
        if (networkId != null) {
            return BaseAsyncCmd.networkSyncObject;
        } else if (vpcId != null) {
            return BaseAsyncCmd.vpcSyncObject;
        }
        return null;
    }

    static Long getSyncObjId(EntityManager entityMgr, Long id){
        LoadBalancerConfig config = findConfigById(entityMgr, id);
        return getSyncObjId(config.getNetworkId(), config.getVpcId());
    }

    // Cause vpcId might be null, this method might return null
    static Long getSyncObjId(Long networkId, Long vpcId){
        if (networkId != null)
            return networkId;
        return vpcId;
    }

    static <T extends OwnedBy> long getEntityOwnerId(EntityManager entityMgr , Class<T> entityType, Long id){
        T t = entityMgr.findById(entityType, id);
        if (t != null) {
            return t.getAccountId();
        }
        throw new InvalidParameterValueException("Unable to find the entity owner");
    }

    static long getEntityOwnerId(EntityManager entityMgr, Long id) {
        LoadBalancerConfig config = findConfigById(entityMgr, id);
        if (config.getNetworkId() != null) {
            return LoadBalancerHelper.getEntityOwnerId(entityMgr, Network.class, config.getNetworkId());
        } else if (config.getVpcId() != null) {
            return LoadBalancerHelper.getEntityOwnerId(entityMgr, Vpc.class, config.getVpcId());
        } else if (config.getLoadBalancerId() != null) {
            return LoadBalancerHelper.getEntityOwnerId(entityMgr, FirewallRule.class, config.getLoadBalancerId());
        }
        throw new InvalidParameterValueException("Unable to find the entity owner");
    }
}
