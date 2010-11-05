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
package com.cloud.network.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import javax.ejb.Local;
import javax.persistence.TableGenerator;

import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.NetworkAccountDaoImpl;
import com.cloud.network.NetworkAccountVO;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SequenceFetcher;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

@Local(value=NetworkConfigurationDao.class) @DB(txn=false)
public class NetworkConfigurationDaoImpl extends GenericDaoBase<NetworkConfigurationVO, Long> implements NetworkConfigurationDao {
    final SearchBuilder<NetworkConfigurationVO> ProfileSearch;
    final SearchBuilder<NetworkConfigurationVO> AccountSearch;
    final SearchBuilder<NetworkConfigurationVO> OfferingSearch;
    final SearchBuilder<NetworkConfigurationVO> RelatedConfigSearch;
    final SearchBuilder<NetworkConfigurationVO> RelatedConfigsSearch;
    
    NetworkAccountDaoImpl _accountsDao = new NetworkAccountDaoImpl();
    final TableGenerator _tgMacAddress;
    Random _rand = new Random(System.currentTimeMillis());
    long _prefix = 0x2;
    
    protected NetworkConfigurationDaoImpl() {
        super();
        
        ProfileSearch = createSearchBuilder();
        ProfileSearch.and("trafficType", ProfileSearch.entity().getTrafficType(), SearchCriteria.Op.EQ);
        ProfileSearch.and("cidr", ProfileSearch.entity().getCidr(), SearchCriteria.Op.EQ);
        ProfileSearch.and("broadcastType", ProfileSearch.entity().getBroadcastDomainType(), SearchCriteria.Op.EQ);
        ProfileSearch.and("offering", ProfileSearch.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        ProfileSearch.and("datacenter", ProfileSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ProfileSearch.done();
        
        AccountSearch = createSearchBuilder();
        AccountSearch.and("offering", AccountSearch.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        SearchBuilder<NetworkAccountVO> join = _accountsDao.createSearchBuilder();
        join.and("account", join.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.join("accounts", join, AccountSearch.entity().getId(), join.entity().getNetworkConfigurationId(), JoinBuilder.JoinType.INNER);
        AccountSearch.and("datacenter", AccountSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AccountSearch.done();
    
        OfferingSearch = createSearchBuilder();
        OfferingSearch.and("guesttype", OfferingSearch.entity().getGuestType(), SearchCriteria.Op.EQ);
        OfferingSearch.and("datacenter", OfferingSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        
        RelatedConfigSearch = createSearchBuilder();
        RelatedConfigSearch.and("offering", RelatedConfigSearch.entity().getNetworkOfferingId(), SearchCriteria.Op.EQ);
        RelatedConfigSearch.and("datacenter", RelatedConfigSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        SearchBuilder<NetworkAccountVO> join2 = _accountsDao.createSearchBuilder();
        join2.and("account", join2.entity().getAccountId(), SearchCriteria.Op.EQ);
        RelatedConfigSearch.join("account", join2, join2.entity().getNetworkConfigurationId(), RelatedConfigSearch.entity().getId(), JoinType.INNER);
        RelatedConfigSearch.done();
        
        RelatedConfigsSearch = createSearchBuilder();
        RelatedConfigsSearch.and("related", RelatedConfigsSearch.entity().getRelated(), SearchCriteria.Op.EQ);
        RelatedConfigsSearch.done();
        
        _tgMacAddress = _tgs.get("macAddress");
    }
    
    public List<NetworkConfigurationVO> findBy(TrafficType trafficType, Mode mode, BroadcastDomainType broadcastType, long networkOfferingId, long dataCenterId) {
        SearchCriteria<NetworkConfigurationVO> sc = ProfileSearch.create();
        sc.setParameters("trafficType", trafficType);
        sc.setParameters("broadcastType", broadcastType);
        sc.setParameters("offering", networkOfferingId);
        sc.setParameters("datacenter", dataCenterId);
        
        return search(sc, null);
    }
    
    @Override
    public List<NetworkConfigurationVO> listBy(long accountId) {
        SearchCriteria<NetworkConfigurationVO> sc = AccountSearch.create();
        sc.setParameters("account", accountId);
        sc.setJoinParameters("accounts", "account", accountId);
        
        return listBy(sc);
    }
    
    @Override
    public List<NetworkConfigurationVO> listBy(long accountId, long offeringId, long dataCenterId) {
        SearchCriteria<NetworkConfigurationVO> sc = AccountSearch.create();
        sc.setParameters("offering", offeringId);
        sc.setJoinParameters("accounts", "account", accountId);
        sc.setParameters("datacenter", dataCenterId);
        
        return listBy(sc);
    }
    
    @Override @DB
    public NetworkConfigurationVO persist(NetworkConfigurationVO config) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        config = super.persist(config);
        addAccountToNetworkConfiguration(config.getId(), config.getAccountId(), true);
        try {
            PreparedStatement pstmt = txn.prepareAutoCloseStatement("INSERT INTO op_network_configurations (id) VALUES(?)");
            pstmt.setLong(1, config.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Problem inserting into the op_network_configurations");
        }
        txn.commit();
        return config;
    }
    
    @Override
    public void addAccountToNetworkConfiguration(long configurationId, long accountId) {
        addAccountToNetworkConfiguration(configurationId, accountId, false);
    }
    
    protected void addAccountToNetworkConfiguration(long configurationId, long accountId, boolean isOwner) {
        NetworkAccountVO account = new NetworkAccountVO(configurationId, accountId, isOwner);
        _accountsDao.persist(account);
    }
    
    @Override
    public SearchBuilder<NetworkAccountVO> createSearchBuilderForAccount() {
        return _accountsDao.createSearchBuilder();
    }
    
    @Override
    public List<NetworkConfigurationVO> getNetworkConfigurationsForOffering(long offeringId, long dataCenterId, long accountId) {
        SearchCriteria<NetworkConfigurationVO> sc = RelatedConfigSearch.create();
        sc.setParameters("offering", offeringId);
        sc.setParameters("dc", dataCenterId);
        sc.setJoinParameters("account", "account", accountId);
        return search(sc, null);
    }
    
    @Override
    public List<NetworkConfigurationVO> getRelatedNetworkConfigurations(long related) {
        SearchCriteria<NetworkConfigurationVO> sc = RelatedConfigsSearch.create();
        sc.setParameters("related", related);
        return search(sc, null);
    }
    
    @Override
    public String getNextAvailableMacAddress(long networkConfigId) {
        SequenceFetcher fetch = SequenceFetcher.getInstance();
        
        long seq = fetch.getNextSequence(Long.class, _tgMacAddress, networkConfigId);
        seq = seq | _prefix | ((_rand.nextInt(Short.MAX_VALUE) << 16) & 0x00000000ffff0000l);
        return NetUtils.long2Mac(seq);
    }
}
