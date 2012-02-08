/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.network.security.dao;

import java.util.List;

import com.cloud.network.security.SecurityGroupRulesVO;
import com.cloud.utils.db.GenericDao;

public interface SecurityGroupRulesDao extends GenericDao<SecurityGroupRulesVO, Long> {
	/**
	 * List a security group and associated ingress rules
	 * @param accountId the account id of the owner of the security group
	 * @param groupName the name of the group for which to list rules
	 * @return the list of ingress rules associated with the security group (and security group info)
	 */
	List<SecurityGroupRulesVO> listSecurityGroupRules(long accountId, String groupName);

	/**
	 * List security groups and associated ingress rules
	 * @param accountId the id of the account for which to list groups and associated rules
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
    List<SecurityGroupRulesVO> listSecurityRulesByGroupIds(Long[] groupId);
}
