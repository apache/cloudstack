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
package com.cloud.network.security;

import java.util.List;

import com.cloud.api.commands.AuthorizeSecurityGroupIngressCmd;
import com.cloud.api.commands.AuthorizeSecurityGroupEgressCmd;
import com.cloud.api.commands.CreateSecurityGroupCmd;
import com.cloud.api.commands.DeleteSecurityGroupCmd;
import com.cloud.api.commands.ListSecurityGroupsCmd;
import com.cloud.api.commands.RevokeSecurityGroupIngressCmd;
import com.cloud.api.commands.RevokeSecurityGroupEgressCmd;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceInUseException;

public interface SecurityGroupService {
    /**
     * Create a network group with the given name and description
     * @param command the command specifying the name and description
     * @return the created security group if successful, null otherwise
     */
    public SecurityGroup createSecurityGroup(CreateSecurityGroupCmd command) throws PermissionDeniedException, InvalidParameterValueException;
    boolean revokeSecurityGroupIngress(RevokeSecurityGroupIngressCmd cmd);
    boolean revokeSecurityGroupEgress(RevokeSecurityGroupEgressCmd cmd);
    
    boolean deleteSecurityGroup(DeleteSecurityGroupCmd cmd) throws ResourceInUseException;

    /**
     * Search for security groups and associated ingress rules for the given account, domain, group name, and/or keyword.
     * The search terms are specified in the search criteria.
     * @return the list of security groups and associated ingress rules
     */
    public List<? extends SecurityGroupRules> searchForSecurityGroupRules(ListSecurityGroupsCmd cmd) throws PermissionDeniedException, InvalidParameterValueException;

    public List<? extends SecurityRule> authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressCmd cmd);
    
    public List<? extends SecurityRule> authorizeSecurityGroupEgress(AuthorizeSecurityGroupEgressCmd cmd);

}
