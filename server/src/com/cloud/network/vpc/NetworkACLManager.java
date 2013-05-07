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
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.user.Account;
import com.cloud.utils.db.DB;
import org.apache.cloudstack.api.command.user.network.CreateNetworkACLListCmd;


public interface NetworkACLManager{

    /**
     * Creates Network ACL for the specified VPC
     * @param name
     * @param description
     * @param vpcId
     * @return
     */
    NetworkACL createNetworkACL(String name, String description, long vpcId);

    /**
     * Fetches Network ACL with specified Id
     * @param id
     * @return
     */
    NetworkACL getNetworkACL(long id);

    /**
     * Applies the items in the ACL to all associated networks
     * @param aclId
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyNetworkACL(long aclId) throws ResourceUnavailableException;

    /**
     * Deletes the specified Network ACL
     * @param id
     * @return
     */
    boolean deleteNetworkACL(NetworkACL acl);

    /**
     * Associates acl with a network and applies the ACLItems
     * @param acl
     * @param network
     * @return
     */
    boolean replaceNetworkACL(NetworkACL acl, NetworkVO network) throws ResourceUnavailableException;

    /**
     * Creates a Network ACL Item within an ACL and applies it to associated networks
     * @param sourcePortStart
     * @param sourcePortEnd
     * @param protocol
     * @param sourceCidrList
     * @param icmpCode
     * @param icmpType
     * @param trafficType
     * @param aclId
     * @param action
     * @param number
     * @return
     */
    NetworkACLItem createNetworkACLItem(Integer sourcePortStart, Integer sourcePortEnd, String protocol,
                                        List<String> sourceCidrList, Integer icmpCode, Integer icmpType,
                                        NetworkACLItem.TrafficType trafficType, Long aclId, String action, Integer number);

    /**
     * Returns Network ACL Item with specified Id
     * @param ruleId
     * @return
     */
    NetworkACLItem getNetworkACLItem(long ruleId);

    /**
     * Revoke ACL Item and apply changes
     * @param ruleId
     * @return
     */
    boolean revokeNetworkACLItem(long ruleId);

    /**
     * Revoke ACL Items for network and remove them in back-end. Db is not updated
     * @param networkId
     * @param userId
     * @param caller
     * @return
     * @throws ResourceUnavailableException
     */
    boolean revokeACLItemsForNetwork(long networkId, long userId, Account caller) throws ResourceUnavailableException;
    
    List<NetworkACLItemVO> listNetworkACLItems(long guestNtwkId);

    boolean applyACLToNetwork(long networkId) throws ResourceUnavailableException;

    NetworkACLItem updateNetworkACLItem(Long id, String protocol, List<String> sourceCidrList, NetworkACLItem.TrafficType trafficType,
                                        String action, Integer number, Integer sourcePortStart, Integer sourcePortEnd,
                                        Integer icmpCode, Integer icmpType) throws ResourceUnavailableException;
}
