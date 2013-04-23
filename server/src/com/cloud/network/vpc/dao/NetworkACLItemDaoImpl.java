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

import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItem.State;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;
import java.util.List;

@Component
@Local(value = NetworkACLItemDao.class)
@DB(txn = false)
public class NetworkACLItemDaoImpl extends GenericDaoBase<NetworkACLItemVO, Long> implements NetworkACLItemDao {

    protected final SearchBuilder<NetworkACLItemVO> AllFieldsSearch;
    protected final SearchBuilder<NetworkACLItemVO> NotRevokedSearch;
    protected final SearchBuilder<NetworkACLItemVO> ReleaseSearch;
    protected SearchBuilder<NetworkACLItemVO> VmSearch;
    protected final SearchBuilder<NetworkACLItemVO> SystemRuleSearch;
    protected final GenericSearchBuilder<NetworkACLItemVO, Long> RulesByIpCount;

    @Inject protected FirewallRulesCidrsDao _firewallRulesCidrsDao;
    @Inject ResourceTagDao _tagsDao;
    @Inject IPAddressDao _ipDao;

    protected NetworkACLItemDaoImpl() {
        super();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("protocol", AllFieldsSearch.entity().getProtocol(), Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("aclId", AllFieldsSearch.entity().getACLId(), Op.EQ);
        AllFieldsSearch.and("trafficType", AllFieldsSearch.entity().getTrafficType(), Op.EQ);
        AllFieldsSearch.done();

        NotRevokedSearch = createSearchBuilder();
        NotRevokedSearch.and("state", NotRevokedSearch.entity().getState(), Op.NEQ);
        NotRevokedSearch.and("protocol", NotRevokedSearch.entity().getProtocol(), Op.EQ);
        NotRevokedSearch.and("sourcePortStart", NotRevokedSearch.entity().getSourcePortStart(), Op.EQ);
        NotRevokedSearch.and("sourcePortEnd", NotRevokedSearch.entity().getSourcePortEnd(), Op.EQ);
        NotRevokedSearch.and("aclId", NotRevokedSearch.entity().getACLId(), Op.EQ);
        NotRevokedSearch.and("trafficType", NotRevokedSearch.entity().getTrafficType(), Op.EQ);
        NotRevokedSearch.done();

        ReleaseSearch = createSearchBuilder();
        ReleaseSearch.and("protocol", ReleaseSearch.entity().getProtocol(), Op.EQ);
        ReleaseSearch.and("ports", ReleaseSearch.entity().getSourcePortStart(), Op.IN);
        ReleaseSearch.done();

        SystemRuleSearch = createSearchBuilder();
        SystemRuleSearch.and("type", SystemRuleSearch.entity().getType(), Op.EQ);
        SystemRuleSearch.done();

        RulesByIpCount = createSearchBuilder(Long.class);
        RulesByIpCount.select(null, Func.COUNT, RulesByIpCount.entity().getId());
        RulesByIpCount.done();
    }


    @Override
    public List<NetworkACLItemVO> listByACLAndNotRevoked(long aclId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean releasePorts(long ipAddressId, String protocol, int[] ports) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<NetworkACLItemVO> listByACL(long aclId) {
        SearchCriteria<NetworkACLItemVO> sc = AllFieldsSearch.create();
        sc.setParameters("aclId", aclId);

        return listBy(sc);
    }

    @Override
    public List<NetworkACLItemVO> listSystemRules() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<NetworkACLItemVO> listByACLTrafficTypeAndNotRevoked(long aclId, NetworkACLItem.TrafficType trafficType) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<NetworkACLItemVO> listByACLTrafficType(long aclId, NetworkACLItem.TrafficType trafficType) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void loadSourceCidrs(NetworkACLItemVO rule) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
