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
package com.cloud.network.security.dao;

import java.util.List;

import com.cloud.network.security.SecurityGroupRulesVO;
import com.cloud.utils.db.GenericDao;

public interface SecurityGroupRulesDao extends GenericDao<SecurityGroupRulesVO, Long> {
    /**
     * List a security group and associated ingress rules
     * @return the list of ingress rules associated with the security group (and security group info)
     */
    List<SecurityGroupRulesVO> listSecurityGroupRules(long accountId, String groupName);

    /**
     * List security groups and associated ingress rules
     * @return the list of security groups with associated ingress rules
     */
    List<SecurityGroupRulesVO> listSecurityGroupRules(long accountId);

    /**
     * List all security groups and associated ingress rules
     * @return the list of security groups with associated ingress rules
     */
    List<SecurityGroupRulesVO> listSecurityGroupRules();

    /**
     * List all security rules belonging to the specific group
     * @return the security group with associated ingress rules
     */
    List<SecurityGroupRulesVO> listSecurityRulesByGroupId(long groupId);
}
