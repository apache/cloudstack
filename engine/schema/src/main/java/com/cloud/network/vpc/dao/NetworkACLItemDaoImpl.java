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
package com.cloud.network.vpc.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.network.vpc.NetworkACLItem.State;
import com.cloud.network.vpc.NetworkACLItemCidrsDao;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.collect.Lists;

@DB()
@Component
public class NetworkACLItemDaoImpl extends GenericDaoBase<NetworkACLItemVO, Long> implements NetworkACLItemDao {

    protected final SearchBuilder<NetworkACLItemVO> AllFieldsSearch;
    protected final SearchBuilder<NetworkACLItemVO> NotRevokedSearch;
    protected final SearchBuilder<NetworkACLItemVO> ReleaseSearch;
    protected final GenericSearchBuilder<NetworkACLItemVO, Integer> MaxNumberSearch;

    @Inject
    protected NetworkACLItemCidrsDao _networkACLItemCidrsDao;

    protected NetworkACLItemDaoImpl() {
        super();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("protocol", AllFieldsSearch.entity().getProtocol(), Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("aclId", AllFieldsSearch.entity().getAclId(), Op.EQ);
        AllFieldsSearch.and("trafficType", AllFieldsSearch.entity().getTrafficType(), Op.EQ);
        AllFieldsSearch.and("number", AllFieldsSearch.entity().getNumber(), Op.EQ);
        AllFieldsSearch.and("action", AllFieldsSearch.entity().getAction(), Op.EQ);
        AllFieldsSearch.done();

        NotRevokedSearch = createSearchBuilder();
        NotRevokedSearch.and("state", NotRevokedSearch.entity().getState(), Op.NEQ);
        NotRevokedSearch.and("protocol", NotRevokedSearch.entity().getProtocol(), Op.EQ);
        NotRevokedSearch.and("sourcePortStart", NotRevokedSearch.entity().getSourcePortStart(), Op.EQ);
        NotRevokedSearch.and("sourcePortEnd", NotRevokedSearch.entity().getSourcePortEnd(), Op.EQ);
        NotRevokedSearch.and("aclId", NotRevokedSearch.entity().getAclId(), Op.EQ);
        NotRevokedSearch.and("trafficType", NotRevokedSearch.entity().getTrafficType(), Op.EQ);
        NotRevokedSearch.done();

        ReleaseSearch = createSearchBuilder();
        ReleaseSearch.and("protocol", ReleaseSearch.entity().getProtocol(), Op.EQ);
        ReleaseSearch.and("ports", ReleaseSearch.entity().getSourcePortStart(), Op.IN);
        ReleaseSearch.done();

        MaxNumberSearch = createSearchBuilder(Integer.class);
        MaxNumberSearch.select(null, SearchCriteria.Func.MAX, MaxNumberSearch.entity().getNumber());
        MaxNumberSearch.and("aclId", MaxNumberSearch.entity().getAclId(), Op.EQ);
        MaxNumberSearch.done();
    }

    @Override
    public NetworkACLItemVO findById(Long id) {
        NetworkACLItemVO item = super.findById(id);
        loadCidrs(item);
        return item;
    }

    @Override
    public boolean update(Long id, NetworkACLItemVO item) {
        boolean result = super.update(id, item);
        _networkACLItemCidrsDao.updateCidrs(item.getId(), item.getSourceCidrList());
        return result;
    }

    @Override
    public boolean setStateToAdd(NetworkACLItemVO rule) {
        SearchCriteria<NetworkACLItemVO> sc = AllFieldsSearch.create();
        sc.setParameters("id", rule.getId());
        sc.setParameters("state", State.Staged);

        rule.setState(State.Add);

        return update(rule, sc) > 0;
    }

    @Override
    public boolean revoke(NetworkACLItemVO rule) {
        rule.setState(State.Revoke);
        return update(rule.getId(), rule);
    }

    @Override
    public List<NetworkACLItemVO> listByACL(Long aclId) {
        if (aclId == null) {
            return Lists.newArrayList();
        }

        SearchCriteria<NetworkACLItemVO> sc = AllFieldsSearch.create();
        sc.setParameters("aclId", aclId);
        List<NetworkACLItemVO> list = listBy(sc);
        for (NetworkACLItemVO item : list) {
            loadCidrs(item);
        }
        return list;
    }

    @Override
    public int getMaxNumberByACL(long aclId) {
        SearchCriteria<Integer> sc = MaxNumberSearch.create();
        sc.setParameters("aclId", aclId);
        Integer max = customSearch(sc, null).get(0);
        return (max == null) ? 0 : max;
    }

    @Override
    public NetworkACLItemVO findByAclAndNumber(long aclId, int number) {
        SearchCriteria<NetworkACLItemVO> sc = AllFieldsSearch.create();
        sc.setParameters("aclId", aclId);
        sc.setParameters("number", number);
        NetworkACLItemVO vo = findOneBy(sc);
        if (vo != null) {
            loadCidrs(vo);
        }
        return vo;
    }

    @Override
    @DB
    public NetworkACLItemVO persist(NetworkACLItemVO networkAclItem) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();

        NetworkACLItemVO dbNetworkACLItem = super.persist(networkAclItem);
        saveCidrs(networkAclItem, networkAclItem.getSourceCidrList());
        loadCidrs(dbNetworkACLItem);

        txn.commit();
        return dbNetworkACLItem;
    }

    public void saveCidrs(NetworkACLItemVO networkACLItem, List<String> cidrList) {
        if (cidrList == null) {
            return;
        }
        _networkACLItemCidrsDao.persist(networkACLItem.getId(), cidrList);
    }

    @Override
    public void loadCidrs(NetworkACLItemVO item) {
        List<String> cidrs = _networkACLItemCidrsDao.getCidrs(item.getId());
        item.setSourceCidrList(cidrs);
    }

    private String sqlUpdateNumberFieldNetworkItem = "UPDATE network_acl_item SET number = ? where id =?";

    @Override
    public void updateNumberFieldNetworkItem(long networkItemId, int newNumberValue) {
        try (TransactionLegacy txn = TransactionLegacy.currentTxn();
                PreparedStatement pstmt = txn.prepareAutoCloseStatement(sqlUpdateNumberFieldNetworkItem)) {
            pstmt.setLong(1, newNumberValue);
            pstmt.setLong(2, networkItemId);
            pstmt.executeUpdate();
            txn.commit();
        } catch (SQLException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
