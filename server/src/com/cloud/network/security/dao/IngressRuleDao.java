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

import com.cloud.network.security.IngressRuleVO;
import com.cloud.utils.db.GenericDao;

public interface IngressRuleDao extends GenericDao<IngressRuleVO, Long> {
    List<IngressRuleVO> listBySecurityGroupId(long networkGroupId);
    List<IngressRuleVO> listByAllowedSecurityGroupId(long networkGroupId);
    IngressRuleVO findByProtoPortsAndCidr(long networkGroupId, String proto, int startPort, int endPort, String cidr);
    IngressRuleVO findByProtoPortsAndGroup(String proto, int startPort, int endPort, String networkGroup);
    IngressRuleVO findByProtoPortsAndAllowedGroupId(long networkGroupId, String proto, int startPort, int endPort, Long allowedGroupId);
    int deleteBySecurityGroup(long securityGroupId);
	int deleteByPortProtoAndGroup(long securityGroupId, String protocol, int startPort,int endPort, Long id);
	int deleteByPortProtoAndCidr(long securityGroupId, String protocol, int startPort,int endPort, String cidr);

}
