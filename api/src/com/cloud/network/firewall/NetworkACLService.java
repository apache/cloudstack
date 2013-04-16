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
package com.cloud.network.firewall;


import java.util.List;

import com.cloud.network.vpc.NetworkACL;
import org.apache.cloudstack.api.command.user.network.CreateNetworkACLListCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkACLListsCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkACLsCmd;

import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.FirewallRule;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public interface NetworkACLService {
    FirewallRule getNetworkACLItem(long ruleId);
    boolean applyNetworkACLs(long networkId, Account caller) throws ResourceUnavailableException;

    /**
     * @param createNetworkACLCmd
     * @return
     */
    FirewallRule createNetworkACLItem(FirewallRule acl) throws NetworkRuleConflictException;
    /**
     * @param ruleId
     * @param apply
     * @return
     */
    boolean revokeNetworkACL(long ruleId, boolean apply);
    /**
     * @param listNetworkACLsCmd
     * @return
     */
    Pair<List<? extends FirewallRule>, Integer> listNetworkACLItems(ListNetworkACLsCmd cmd);

    NetworkACL createNetworkACL(CreateNetworkACLListCmd cmd);

    NetworkACL getNetworkACL(long id);

    boolean deleteNetworkACL(long id);

    Pair<List<? extends NetworkACL>,Integer> listNetworkACLs(ListNetworkACLListsCmd listNetworkACLListsCmd);
}
