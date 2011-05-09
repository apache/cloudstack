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

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.dao.FirewallRulesCidrsDaoImpl;
import com.cloud.network.dao.FirewallRulesDaoImpl;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value=PortForwardingRulesDao.class)
public class PortForwardingRulesDaoImpl extends GenericDaoBase<PortForwardingRuleVO, Long> implements PortForwardingRulesDao {
    private static final Logger s_logger = Logger.getLogger(PortForwardingRulesDaoImpl.class);

    protected final SearchBuilder<PortForwardingRuleVO> AllFieldsSearch;
    protected final SearchBuilder<PortForwardingRuleVO> ApplicationSearch;
    protected final SearchBuilder<PortForwardingRuleVO> ActiveRulesSearch;
    protected final SearchBuilder<PortForwardingRuleVO> AllRulesSearchByVM;
    protected final SearchBuilder<PortForwardingRuleVO> ActiveRulesSearchByAccount;

    protected final FirewallRulesCidrsDaoImpl _portForwardingRulesCidrsDao = ComponentLocator.inject(FirewallRulesCidrsDaoImpl.class);
    
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
    

    public void saveSourceCidrs(PortForwardingRuleVO portForwardingRule) {
        List<String> cidrlist = portForwardingRule.getSourceCidrList();
        if (cidrlist == null) {
            return;
        }
        _portForwardingRulesCidrsDao.persist(portForwardingRule.getId(), cidrlist);
    }
    

    public void loadSourceCidrs(PortForwardingRuleVO portForwardingRule){
        List<String> sourceCidrs = _portForwardingRulesCidrsDao.getSourceCidrs(portForwardingRule.getId());
        portForwardingRule.setSourceCidrList(sourceCidrs);
     }    

    

    @Override @DB
    public PortForwardingRuleVO persist(PortForwardingRuleVO portForwardingRule) {        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        PortForwardingRuleVO dbfirewallRule = super.persist(portForwardingRule);
        
        saveSourceCidrs(portForwardingRule);
        loadSourceCidrs(dbfirewallRule);
        
        txn.commit();
     
        return dbfirewallRule;
    }
    
    
    @Override @DB
    public boolean update(Long portForwardingRuleId, PortForwardingRuleVO portForwardingRule) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        boolean persisted = super.update(portForwardingRuleId, portForwardingRule);
        if (!persisted) {
            return persisted;
        }
        
        saveSourceCidrs(portForwardingRule);
        txn.commit();
     
        return persisted;
    }
    
}
