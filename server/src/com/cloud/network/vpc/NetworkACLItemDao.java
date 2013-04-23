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

import com.cloud.utils.db.GenericDao;

import java.util.List;

/*
 * Data Access Object for network_acl_item table
 */
public interface NetworkACLItemDao extends GenericDao<NetworkACLItemVO, Long> {

    List<NetworkACLItemVO> listByACLAndNotRevoked(long aclId);

    boolean setStateToAdd(NetworkACLItemVO rule);

    boolean revoke(NetworkACLItemVO rule);

    boolean releasePorts(long ipAddressId, String protocol, int[] ports);

    List<NetworkACLItemVO> listByACL(long aclId);

    List<NetworkACLItemVO> listSystemRules();

    List<NetworkACLItemVO> listByACLTrafficTypeAndNotRevoked(long aclId, NetworkACLItemVO.TrafficType trafficType);
    List<NetworkACLItemVO> listByACLTrafficType(long aclId, NetworkACLItemVO.TrafficType trafficType);
    
    void loadSourceCidrs(NetworkACLItemVO rule);
}
