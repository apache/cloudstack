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
package com.cloud.network.vpc;

import java.util.List;

import org.apache.cloudstack.api.command.user.network.CreateNetworkACLCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkACLListsCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkACLsCmd;
import org.apache.cloudstack.api.command.user.network.MoveNetworkAclItemCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkACLItemCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkACLListCmd;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.Pair;

public interface NetworkACLService {

    /**
     * Creates Network ACL for the specified VPC
     */
    NetworkACL createNetworkACL(String name, String description, long vpcId, Boolean forDisplay);

    /**
     * Get Network ACL with specified Id
     */
    NetworkACL getNetworkACL(long id);

    /**
     * List NetworkACLs by Id/Name/Network or VPC it belongs to
     */
    Pair<List<? extends NetworkACL>, Integer> listNetworkACLs(ListNetworkACLListsCmd cmd);

    /**
     * Delete specified network ACL. Deletion fails if the list is not empty
     */
    boolean deleteNetworkACL(long id);

    /**
     * Associates ACL with specified Network
     */
    boolean replaceNetworkACL(long aclId, long networkId) throws ResourceUnavailableException;

    /**
     * Applied ACL to associated networks
     */
    boolean applyNetworkACL(long aclId) throws ResourceUnavailableException;

    /**
     * Creates a Network ACL Item within an ACL and applies the ACL to associated networks
     */
    NetworkACLItem createNetworkACLItem(CreateNetworkACLCmd aclItemCmd);

    /**
     * Return ACL item with specified Id
     */
    NetworkACLItem getNetworkACLItem(long ruleId);

    /**
     * Lists Network ACL Items by Id, Network, ACLId, Traffic Type, protocol
     */
    Pair<List<? extends NetworkACLItem>, Integer> listNetworkACLItems(ListNetworkACLsCmd cmd);

    /**
     * Revoke ACL Item with specified Id
     */
    boolean revokeNetworkACLItem(long ruleId);

    /**
     * Updates existing aclItem applies to associated networks
     */
    NetworkACLItem updateNetworkACLItem(UpdateNetworkACLItemCmd updateNetworkACLItemCmd) throws ResourceUnavailableException;

    /**
     * Associates ACL with specified Network
     */
    boolean replaceNetworkACLonPrivateGw(long aclId, long privateGatewayId) throws ResourceUnavailableException;

    NetworkACL updateNetworkACL(UpdateNetworkACLListCmd updateNetworkACLListCmd);

    /**
     * Updates a network item ACL to a new position. This method allows users to inform between which ACLs the given ACL will be placed. Therefore, the 'number' field will be filled out by the system in the best way possible to place the ACL accordingly.
     */
    NetworkACLItem moveNetworkAclRuleToNewPosition(MoveNetworkAclItemCmd moveNetworkAclItemCmd);
}
