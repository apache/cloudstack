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

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkVO;

public interface NetworkACLManager {

    /**
     * Creates Network ACL for the specified VPC
     */
    NetworkACL createNetworkACL(String name, String description, long vpcId, Boolean forDisplay);

    /**
     * Fetches Network ACL with specified Id
     */
    NetworkACL getNetworkACL(long id);

    /**
     * Applies the items in the ACL to all associated networks
     */
    boolean applyNetworkACL(long aclId) throws ResourceUnavailableException;

    /**
     * Deletes the specified Network ACL
     */
    boolean deleteNetworkACL(NetworkACL acl);

    /**
     * Associates ACL with a network and applies the ACLItems
     */
    boolean replaceNetworkACL(NetworkACL acl, NetworkVO network) throws ResourceUnavailableException;

    /**
     * Creates a Network ACL Item within an ACL and applies it to associated networks
     */
    NetworkACLItem createNetworkACLItem(NetworkACLItemVO networkACLItemVO);

    /**
     * Returns Network ACL Item with specified Id
     */
    NetworkACLItem getNetworkACLItem(long ruleId);

    /**
     * Revoke ACL Item and apply changes
     */
    boolean revokeNetworkACLItem(long ruleId);

    /**
     * Revoke ACL Items for network and remove them in back-end. Db is not updated
     */
    boolean revokeACLItemsForNetwork(long networkId) throws ResourceUnavailableException;

    /**
     * List network ACL items by network
     */
    List<NetworkACLItemVO> listNetworkACLItems(long guestNtwkId);

    /**
     * Applies associated ACL to specified network
     */
    boolean applyACLToNetwork(long networkId) throws ResourceUnavailableException;

    /**
     * Updates and existing network ACL Item
     */
    NetworkACLItem updateNetworkACLItem(NetworkACLItemVO networkACLItemVO) throws ResourceUnavailableException;

    /**
     * Associates ACL with a network and applies the ACLItems
     */
    boolean replaceNetworkACLForPrivateGw(NetworkACL acl, PrivateGateway gateway) throws ResourceUnavailableException;

    boolean revokeACLItemsForPrivateGw(PrivateGateway gateway) throws ResourceUnavailableException;

    boolean applyACLToPrivateGw(PrivateGateway gateway) throws ResourceUnavailableException;

    boolean reorderAclRules(VpcVO vpc, List<? extends Network> networks, List<? extends NetworkACLItem> networkACLItems);
}
