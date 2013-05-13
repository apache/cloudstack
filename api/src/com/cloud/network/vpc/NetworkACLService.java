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


import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.Pair;
import org.apache.cloudstack.api.command.user.network.CreateNetworkACLCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkACLsCmd;

import java.util.List;

public interface NetworkACLService {
    /**
     * Creates Network ACL for the specified VPC
     * @param name
     * @param description
     * @param vpcId
     * @return
     */
    NetworkACL createNetworkACL(String name, String description, long vpcId);

    /**
     * Get Network ACL with specified Id
     * @param id
     * @return
     */
    NetworkACL getNetworkACL(long id);

    /**
     * List NetworkACLs by Id/Name/Network or Vpc it belongs to
     * @param id
     * @param name
     * @param networkId
     * @param vpcId
     * @return
     */
    Pair<List<? extends NetworkACL>,Integer> listNetworkACLs(Long id, String name, Long networkId, Long vpcId);

    /**
     * Delete specified network ACL. Deletion fails if the list is not empty
     * @param id
     * @return
     */
    boolean deleteNetworkACL(long id);

    /**
     * Associates ACL with specified Network
     * @param aclId
     * @param networkId
     * @return
     * @throws ResourceUnavailableException
     */
    boolean replaceNetworkACL(long aclId, long networkId) throws ResourceUnavailableException;

    /**
     * Applied ACL to associated networks
     * @param aclId
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyNetworkACL(long aclId) throws ResourceUnavailableException;

    /**
     * Creates a Network ACL Item within an ACL and applies the ACL to associated networks
     * @param createNetworkACLCmd
     * @return
     */
    NetworkACLItem createNetworkACLItem(CreateNetworkACLCmd aclItemCmd);

    /**
     * Return ACL item with specified Id
     * @param ruleId
     * @return
     */
    NetworkACLItem getNetworkACLItem(long ruleId);

    /**
     * Lists Network ACL Items by Id, Network, ACLId, Traffic Type, protocol
     * @param listNetworkACLsCmd
     * @return
     */
    Pair<List<? extends NetworkACLItem>, Integer> listNetworkACLItems(ListNetworkACLsCmd cmd);

    /**
     * Revoked ACL Item with specified Id
     * @param ruleId
     * @param apply
     * @return
     */
    boolean revokeNetworkACLItem(long ruleId);

    /**
     * Updates existing aclItem applies to associated networks
     * @param id
     * @param protocol
     * @param sourceCidrList
     * @param trafficType
     * @param action
     * @param number
     * @param sourcePortStart
     * @param sourcePortEnd
     * @param icmpCode
     * @param icmpType
     * @return
     * @throws ResourceUnavailableException
     */
    NetworkACLItem updateNetworkACLItem(Long id, String protocol, List<String> sourceCidrList, NetworkACLItem.TrafficType trafficType,
                                        String action, Integer number, Integer sourcePortStart, Integer sourcePortEnd,
                                        Integer icmpCode, Integer icmpType) throws ResourceUnavailableException;

    /**
     * Associates ACL with specified Network
     * @param aclId
     * @param privateGatewayId
     * @return
     * @throws ResourceUnavailableException
     */
    boolean replaceNetworkACLonPrivateGw(long aclId, long privateGatewayId) throws ResourceUnavailableException;

}
