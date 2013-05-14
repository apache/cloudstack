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
package com.cloud.network.rules.dao;

import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesCidrsDaoImpl;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.PortForwardingRuleVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.dao.NicSecondaryIpVO;

@Component
@Local(value=PortForwardingRulesDao.class)
public class PortForwardingRulesDaoImpl extends GenericDaoBase<PortForwardingRuleVO, Long> implements PortForwardingRulesDao {

    protected final SearchBuilder<PortForwardingRuleVO> AllFieldsSearch;
    protected final SearchBuilder<PortForwardingRuleVO> ApplicationSearch;
    protected final SearchBuilder<PortForwardingRuleVO> ActiveRulesSearch;
    protected final SearchBuilder<PortForwardingRuleVO> AllRulesSearchByVM;
    protected final SearchBuilder<PortForwardingRuleVO> ActiveRulesSearchByAccount;

    @Inject protected FirewallRulesCidrsDao _portForwardingRulesCidrsDao;
    
    protected PortForwardingRulesDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("ipId", AllFieldsSearch.entity().getSourceIpAddressId(), Op.EQ);
        AllFieldsSearch.and("protocol", AllFieldsSearch.entity().getProtocol(), Op.EQ);
        AllFieldsSearch.and("networkId", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.and("vmId", AllFieldsSearch.entity().getVirtualMachineId(), Op.EQ);
        AllFieldsSearch.and("purpose", AllFieldsSearch.entity().getPurpose(), Op.EQ);
        AllFieldsSearch.and("dstIp", AllFieldsSearch.entity().getDestinationIpAddress(), Op.EQ);
        AllFieldsSearch.done();
        
        ApplicationSearch = createSearchBuilder();
        ApplicationSearch.and("ipId", ApplicationSearch.entity().getSourceIpAddressId(), Op.EQ);
        ApplicationSearch.and("state", ApplicationSearch.entity().getState(), Op.NEQ);
        ApplicationSearch.and("purpose", ApplicationSearch.entity().getPurpose(), Op.EQ);
        ApplicationSearch.done();
        
        
        ActiveRulesSearch = createSearchBuilder();
        ActiveRulesSearch.and("ipId", ActiveRulesSearch.entity().getSourceIpAddressId(), Op.EQ);
        ActiveRulesSearch.and("networkId", ActiveRulesSearch.entity().getNetworkId(), Op.EQ);
        ActiveRulesSearch.and("state", ActiveRulesSearch.entity().getState(), Op.NEQ);
        ActiveRulesSearch.and("purpose", ActiveRulesSearch.entity().getPurpose(), Op.EQ);
        ActiveRulesSearch.done();
        
        AllRulesSearchByVM = createSearchBuilder();
        AllRulesSearchByVM.and("vmId", AllRulesSearchByVM.entity().getVirtualMachineId(), Op.EQ);
        AllRulesSearchByVM.and("purpose", AllRulesSearchByVM.entity().getPurpose(), Op.EQ);
        AllRulesSearchByVM.done();
        
        ActiveRulesSearchByAccount = createSearchBuilder();
        ActiveRulesSearchByAccount.and("accountId", ActiveRulesSearchByAccount.entity().getAccountId(), Op.EQ);
        ActiveRulesSearchByAccount.and("state", ActiveRulesSearchByAccount.entity().getState(), Op.NEQ);
        ActiveRulesSearchByAccount.and("purpose", ActiveRulesSearchByAccount.entity().getPurpose(), Op.EQ);
        ActiveRulesSearchByAccount.done();
    }

    @Override
    public List<PortForwardingRuleVO> listForApplication(long ipId) {
        SearchCriteria<PortForwardingRuleVO> sc = ApplicationSearch.create();
        sc.setParameters("ipId", ipId);
        sc.setParameters("state", State.Staged);
        sc.setParameters("purpose", Purpose.PortForwarding);
        
        return listBy(sc, null);
    }
    
    @Override
    public List<PortForwardingRuleVO> listByVm(Long vmId) {
    	SearchCriteria<PortForwardingRuleVO> sc = AllRulesSearchByVM.create();
    	sc.setParameters("vmId", vmId);
    	sc.setParameters("purpose", Purpose.PortForwarding);
    	
    	return listBy(sc, null);
    }

    @Override
    public List<PortForwardingRuleVO> listByIpAndNotRevoked(long ipId) {
        SearchCriteria<PortForwardingRuleVO> sc = ActiveRulesSearch.create();
        sc.setParameters("ipId", ipId);
        sc.setParameters("state", State.Revoke);
        sc.setParameters("purpose", Purpose.PortForwarding);
        
        return listBy(sc, null);
    }
    
    @Override
    public List<PortForwardingRuleVO> listByNetworkAndNotRevoked(long networkId) {
        SearchCriteria<PortForwardingRuleVO> sc = ActiveRulesSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("state", State.Revoke);
        sc.setParameters("purpose", Purpose.PortForwarding);
        
        return listBy(sc, null);
    }
    
    @Override
    public List<PortForwardingRuleVO> listByIp(long ipId) {
        SearchCriteria<PortForwardingRuleVO> sc = AllFieldsSearch.create();
        sc.setParameters("ipId", ipId);
        sc.setParameters("purpose", Purpose.PortForwarding);
        
        return listBy(sc, null);
    }
    
    @Override
    public List<PortForwardingRuleVO> listByNetwork(long networkId) {
        SearchCriteria<PortForwardingRuleVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("purpose", Purpose.PortForwarding);
        
        return listBy(sc);
    }

    @Override
    public List<PortForwardingRuleVO> listByAccount(long accountId) {
        SearchCriteria<PortForwardingRuleVO> sc = ActiveRulesSearchByAccount.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("state", State.Revoke);
        sc.setParameters("purpose", Purpose.PortForwarding);
        
        return listBy(sc);
    }
    @Override
    public List<PortForwardingRuleVO> listByDestIpAddr(String ip4Address) {
        SearchCriteria<PortForwardingRuleVO> sc = AllFieldsSearch.create();
        sc.setParameters("dstIp", ip4Address);
        return listBy(sc);
    }
  
}
