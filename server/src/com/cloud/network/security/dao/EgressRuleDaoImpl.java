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
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.network.security.EgressRuleVO;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={EgressRuleDao.class})
public class EgressRuleDaoImpl extends GenericDaoBase<EgressRuleVO, Long> implements EgressRuleDao {
	
	@Inject SecurityGroupDao _securityGroupDao;
	
    protected SearchBuilder<EgressRuleVO> securityGroupIdSearch;
    protected SearchBuilder<EgressRuleVO> allowedSecurityGroupIdSearch;
    protected SearchBuilder<EgressRuleVO> protoPortsAndCidrSearch;
    protected SearchBuilder<EgressRuleVO> protoPortsAndSecurityGroupNameSearch;
    protected SearchBuilder<EgressRuleVO> protoPortsAndSecurityGroupIdSearch;



    protected EgressRuleDaoImpl() {
        securityGroupIdSearch  = createSearchBuilder();
        securityGroupIdSearch.and("securityGroupId", securityGroupIdSearch.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        securityGroupIdSearch.done();
        
        allowedSecurityGroupIdSearch  = createSearchBuilder();
        allowedSecurityGroupIdSearch.and("allowedNetworkId", allowedSecurityGroupIdSearch.entity().getAllowedNetworkId(), SearchCriteria.Op.EQ);
        allowedSecurityGroupIdSearch.done();
        
        protoPortsAndCidrSearch = createSearchBuilder();
        protoPortsAndCidrSearch.and("securityGroupId", protoPortsAndCidrSearch.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        protoPortsAndCidrSearch.and("proto", protoPortsAndCidrSearch.entity().getProtocol(), SearchCriteria.Op.EQ);
        protoPortsAndCidrSearch.and("startPort", protoPortsAndCidrSearch.entity().getStartPort(), SearchCriteria.Op.EQ);
        protoPortsAndCidrSearch.and("endPort", protoPortsAndCidrSearch.entity().getEndPort(), SearchCriteria.Op.EQ);
        protoPortsAndCidrSearch.and("cidr", protoPortsAndCidrSearch.entity().getAllowedDestinationIpCidr(), SearchCriteria.Op.EQ);
        protoPortsAndCidrSearch.done();
        
        protoPortsAndSecurityGroupIdSearch = createSearchBuilder();
        protoPortsAndSecurityGroupIdSearch.and("securityGroupId", protoPortsAndSecurityGroupIdSearch.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        protoPortsAndSecurityGroupIdSearch.and("proto", protoPortsAndSecurityGroupIdSearch.entity().getProtocol(), SearchCriteria.Op.EQ);
        protoPortsAndSecurityGroupIdSearch.and("startPort", protoPortsAndSecurityGroupIdSearch.entity().getStartPort(), SearchCriteria.Op.EQ);
        protoPortsAndSecurityGroupIdSearch.and("endPort", protoPortsAndSecurityGroupIdSearch.entity().getEndPort(), SearchCriteria.Op.EQ);        
        protoPortsAndSecurityGroupIdSearch.and("allowedNetworkId", protoPortsAndSecurityGroupIdSearch.entity().getAllowedNetworkId(), SearchCriteria.Op.EQ);

    }

    public List<EgressRuleVO> listBySecurityGroupId(long securityGroupId) {
        SearchCriteria<EgressRuleVO> sc = securityGroupIdSearch.create();
        sc.setParameters("securityGroupId", securityGroupId);
        return listBy(sc);
    }

    public int deleteBySecurityGroup(long securityGroupId) {
        SearchCriteria<EgressRuleVO> sc = securityGroupIdSearch.create();
        sc.setParameters("securityGroupId", securityGroupId);
        return expunge(sc);
    }

	@Override
	public List<EgressRuleVO> listByAllowedSecurityGroupId(long securityGroupId) {
		 SearchCriteria<EgressRuleVO> sc = allowedSecurityGroupIdSearch.create();
		 sc.setParameters("allowedNetworkId", securityGroupId);
		 return listBy(sc);
	}

	@Override
	public EgressRuleVO findByProtoPortsAndCidr(long securityGroupId, String proto, int startPort,
			int endPort, String cidr) {
		SearchCriteria<EgressRuleVO> sc = protoPortsAndCidrSearch.create();
		sc.setParameters("securityGroupId", securityGroupId);
		sc.setParameters("proto", proto);
		sc.setParameters("startPort", startPort);
		sc.setParameters("endPort", endPort);
		sc.setParameters("cidr", cidr);
		return findOneIncludingRemovedBy(sc);
	}

	@Override
	public EgressRuleVO findByProtoPortsAndGroup(String proto, int startPort,
			int endPort, String securityGroup) {
		SearchCriteria<EgressRuleVO> sc = protoPortsAndSecurityGroupNameSearch.create();
		sc.setParameters("proto", proto);
		sc.setParameters("startPort", startPort);
		sc.setParameters("endPort", endPort);
		sc.setJoinParameters("groupName", "groupName", securityGroup);
		return findOneIncludingRemovedBy(sc);
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		protoPortsAndSecurityGroupNameSearch = createSearchBuilder();
        protoPortsAndSecurityGroupNameSearch.and("proto", protoPortsAndSecurityGroupNameSearch.entity().getProtocol(), SearchCriteria.Op.EQ);
        protoPortsAndSecurityGroupNameSearch.and("startPort", protoPortsAndSecurityGroupNameSearch.entity().getStartPort(), SearchCriteria.Op.EQ);
        protoPortsAndSecurityGroupNameSearch.and("endPort", protoPortsAndSecurityGroupNameSearch.entity().getEndPort(), SearchCriteria.Op.EQ);
        SearchBuilder<SecurityGroupVO> ngSb = _securityGroupDao.createSearchBuilder();
        ngSb.and("groupName", ngSb.entity().getName(), SearchCriteria.Op.EQ);
        protoPortsAndSecurityGroupNameSearch.join("groupName", ngSb, protoPortsAndSecurityGroupNameSearch.entity().getAllowedNetworkId(), ngSb.entity().getId(), JoinBuilder.JoinType.INNER);
        protoPortsAndSecurityGroupNameSearch.done();
		return super.configure(name, params);
	}

	@Override
	public int deleteByPortProtoAndGroup(long securityGroupId, String protocol, int startPort, int endPort, Long allowedGroupId) {
		SearchCriteria<EgressRuleVO> sc = protoPortsAndSecurityGroupIdSearch.create();
		sc.setParameters("securityGroupId", securityGroupId);
		sc.setParameters("proto", protocol);
		sc.setParameters("startPort", startPort);
		sc.setParameters("endPort", endPort);
		sc.setParameters("allowedNetworkId", allowedGroupId);
		
        return expunge(sc);
		
	}

	@Override
	public int deleteByPortProtoAndCidr(long securityGroupId, String protocol, int startPort, int endPort, String cidr) {
		SearchCriteria<EgressRuleVO> sc = protoPortsAndCidrSearch.create();
		sc.setParameters("securityGroupId", securityGroupId);
		sc.setParameters("proto", protocol);
		sc.setParameters("startPort", startPort);
		sc.setParameters("endPort", endPort);
		sc.setParameters("cidr", cidr);
		
		return expunge(sc);
	}

	@Override
	public EgressRuleVO findByProtoPortsAndAllowedGroupId(long securityGroupId, String proto,
			int startPort, int endPort, Long allowedGroupId) {
		SearchCriteria<EgressRuleVO> sc = protoPortsAndSecurityGroupIdSearch.create();
		sc.addAnd("securityGroupId", SearchCriteria.Op.EQ, securityGroupId);
		sc.setParameters("proto", proto);
		sc.setParameters("startPort", startPort);
		sc.setParameters("endPort", endPort);
		sc.setParameters("allowedNetworkId", allowedGroupId);
		
        return findOneIncludingRemovedBy(sc);
	}
}
