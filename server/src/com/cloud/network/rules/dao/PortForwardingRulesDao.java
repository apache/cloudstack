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
package com.cloud.network.rules.dao;

import java.util.List;

import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.utils.db.GenericDao;

public interface PortForwardingRulesDao extends GenericDao<PortForwardingRuleVO, Long> {
    List<PortForwardingRuleVO> listForApplication(long ipId);
    
    /**
     * Find all port forwarding rules for the ip address that have not been revoked.
     * 
     * @param ip ip address 
     * @return List of PortForwardingRuleVO
     */
    List<PortForwardingRuleVO> listByIpAndNotRevoked(long ipId);
    
    List<PortForwardingRuleVO> listByNetworkAndNotRevoked(long networkId);
    
    List<PortForwardingRuleVO> listByIp(long ipId);

    List<PortForwardingRuleVO> listByVm(Long vmId);
    
    List<PortForwardingRuleVO> listByNetwork(long networkId);
    
    List<PortForwardingRuleVO> listByAccount(long accountId);
    
    void loadSourceCidrs(PortForwardingRuleVO portForwardingRule);
    
    void saveSourceCidrs(PortForwardingRuleVO portForwardingRule);
}
