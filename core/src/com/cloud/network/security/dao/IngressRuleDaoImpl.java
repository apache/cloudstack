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

import com.cloud.network.security.IngressRuleVO;
import com.cloud.network.security.NetworkGroupVO;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={IngressRuleDao.class})
public class IngressRuleDaoImpl extends GenericDaoBase<IngressRuleVO, Long> implements IngressRuleDao {
	
	@Inject NetworkGroupDao _networkGroupDao;
	
    protected SearchBuilder<IngressRuleVO> networkGroupIdSearch;
    protected SearchBuilder<IngressRuleVO> allowedNetworkGroupIdSearch;
    protected SearchBuilder<IngressRuleVO> protoPortsAndCidrSearch;
    protected SearchBuilder<IngressRuleVO> protoPortsAndNetworkGroupNameSearch;
    protected SearchBuilder<IngressRuleVO> protoPortsAndNetworkGroupIdSearch;



    protected IngressRuleDaoImpl() {
        networkGroupIdSearch  = createSearchBuilder();
        networkGroupIdSearch.and("networkGroupId", networkGroupIdSearch.entity().getNetworkGroupId(), SearchCriteria.Op.EQ);
        networkGroupIdSearch.done();
        
        allowedNetworkGroupIdSearch  = createSearchBuilder();
        allowedNetworkGroupIdSearch.and("allowedNetworkId", allowedNetworkGroupIdSearch.entity().getAllowedNetworkId(), SearchCriteria.Op.EQ);
        allowedNetworkGroupIdSearch.done();
        
        protoPortsAndCidrSearch = createSearchBuilder();
        protoPortsAndCidrSearch.and("networkGroupId", protoPortsAndCidrSearch.entity().getNetworkGroupId(), SearchCriteria.Op.EQ);
        protoPortsAndCidrSearch.and("proto", protoPortsAndCidrSearch.entity().getProtocol(), SearchCriteria.Op.EQ);
        protoPortsAndCidrSearch.and("startPort", protoPortsAndCidrSearch.entity().getStartPort(), SearchCriteria.Op.EQ);
        protoPortsAndCidrSearch.and("endPort", protoPortsAndCidrSearch.entity().getEndPort(), SearchCriteria.Op.EQ);
        protoPortsAndCidrSearch.and("cidr", protoPortsAndCidrSearch.entity().getAllowedSourceIpCidr(), SearchCriteria.Op.EQ);
        protoPortsAndCidrSearch.done();
        
        protoPortsAndNetworkGroupIdSearch = createSearchBuilder();
        protoPortsAndNetworkGroupIdSearch.and("networkGroupId", protoPortsAndNetworkGroupIdSearch.entity().getNetworkGroupId(), SearchCriteria.Op.EQ);
        protoPortsAndNetworkGroupIdSearch.and("proto", protoPortsAndNetworkGroupIdSearch.entity().getProtocol(), SearchCriteria.Op.EQ);
        protoPortsAndNetworkGroupIdSearch.and("startPort", protoPortsAndNetworkGroupIdSearch.entity().getStartPort(), SearchCriteria.Op.EQ);
        protoPortsAndNetworkGroupIdSearch.and("endPort", protoPortsAndNetworkGroupIdSearch.entity().getEndPort(), SearchCriteria.Op.EQ);        
        protoPortsAndNetworkGroupIdSearch.and("allowedNetworkId", protoPortsAndNetworkGroupIdSearch.entity().getAllowedNetworkId(), SearchCriteria.Op.EQ);

    }

    public List<IngressRuleVO> listByNetworkGroupId(long networkGroupId) {
        SearchCriteria sc = networkGroupIdSearch.create();
        sc.setParameters("networkGroupId", networkGroupId);
        return listActiveBy(sc);
    }

    public int deleteByNetworkGroup(long networkGroupId) {
        SearchCriteria sc = networkGroupIdSearch.create();
        sc.setParameters("networkGroupId", networkGroupId);
        return delete(sc);
    }

	@Override
	public List<IngressRuleVO> listByAllowedNetworkGroupId(long networkGroupId) {
		 SearchCriteria sc = allowedNetworkGroupIdSearch.create();
		 sc.setParameters("allowedNetworkId", networkGroupId);
		 return listActiveBy(sc);
	}

	@Override
	public IngressRuleVO findByProtoPortsAndCidr(long networkGroupId, String proto, int startPort,
			int endPort, String cidr) {
		SearchCriteria sc = protoPortsAndCidrSearch.create();
		sc.setParameters("networkGroupId", networkGroupId);
		sc.setParameters("proto", proto);
		sc.setParameters("startPort", startPort);
		sc.setParameters("endPort", endPort);
		sc.setParameters("cidr", cidr);
		return findOneBy(sc);
	}

	@Override
	public IngressRuleVO findByProtoPortsAndGroup(String proto, int startPort,
			int endPort, String networkGroup) {
		SearchCriteria sc = protoPortsAndNetworkGroupNameSearch.create();
		sc.setParameters("proto", proto);
		sc.setParameters("startPort", startPort);
		sc.setParameters("endPort", endPort);
		sc.setJoinParameters("groupName", "groupName", networkGroup);
		return findOneBy(sc);
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		protoPortsAndNetworkGroupNameSearch = createSearchBuilder();
        protoPortsAndNetworkGroupNameSearch.and("proto", protoPortsAndNetworkGroupNameSearch.entity().getProtocol(), SearchCriteria.Op.EQ);
        protoPortsAndNetworkGroupNameSearch.and("startPort", protoPortsAndNetworkGroupNameSearch.entity().getStartPort(), SearchCriteria.Op.EQ);
        protoPortsAndNetworkGroupNameSearch.and("endPort", protoPortsAndNetworkGroupNameSearch.entity().getEndPort(), SearchCriteria.Op.EQ);
        SearchBuilder<NetworkGroupVO> ngSb = _networkGroupDao.createSearchBuilder();
        ngSb.and("groupName", ngSb.entity().getName(), SearchCriteria.Op.EQ);
        protoPortsAndNetworkGroupNameSearch.join("groupName", ngSb, protoPortsAndNetworkGroupNameSearch.entity().getAllowedNetworkId(), ngSb.entity().getId());
        protoPortsAndNetworkGroupNameSearch.done();
		return super.configure(name, params);
	}

	@Override
	public int deleteByPortProtoAndGroup(long networkGroupId, String protocol, int startPort, int endPort, Long allowedGroupId) {
		SearchCriteria sc = protoPortsAndNetworkGroupIdSearch.create();
		sc.setParameters("networkGroupId", networkGroupId);
		sc.setParameters("proto", protocol);
		sc.setParameters("startPort", startPort);
		sc.setParameters("endPort", endPort);
		sc.setParameters("allowedNetworkId", allowedGroupId);
		
        return delete(sc);
		
	}

	@Override
	public int deleteByPortProtoAndCidr(long networkGroupId, String protocol, int startPort, int endPort, String cidr) {
		SearchCriteria sc = protoPortsAndCidrSearch.create();
		sc.setParameters("networkGroupId", networkGroupId);
		sc.setParameters("proto", protocol);
		sc.setParameters("startPort", startPort);
		sc.setParameters("endPort", endPort);
		sc.setParameters("cidr", cidr);
		
		return delete(sc);
	}

	@Override
	public IngressRuleVO findByProtoPortsAndAllowedGroupId(long networkGroupId, String proto,
			int startPort, int endPort, Long allowedGroupId) {
		SearchCriteria sc = protoPortsAndNetworkGroupIdSearch.create();
		sc.addAnd("networkGroupId", SearchCriteria.Op.EQ, networkGroupId);
		sc.setParameters("proto", proto);
		sc.setParameters("startPort", startPort);
		sc.setParameters("endPort", endPort);
		sc.setParameters("allowedNetworkId", allowedGroupId);
		
        return findOneBy(sc);
	}
}
