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
package org.apache.cloudstack.affinity.dao;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.affinity.AffinityGroupVMMapVO;
import org.apache.cloudstack.affinity.AffinityGroupVO;
import org.springframework.stereotype.Component;

import com.cloud.host.HostTagVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.Transaction;

@Component
@Local(value = { AffinityGroupVMMapDao.class })
public class AffinityGroupVMMapDaoImpl extends GenericDaoBase<AffinityGroupVMMapVO, Long> implements
        AffinityGroupVMMapDao {
    private SearchBuilder<AffinityGroupVMMapVO> ListByVmId;
    private SearchBuilder<AffinityGroupVMMapVO> ListByVmIdGroupId;
    protected GenericSearchBuilder<AffinityGroupVMMapVO, Long> CountSGForVm;
    private GenericSearchBuilder<AffinityGroupVMMapVO, Long> ListVmIdByAffinityGroup;
    private SearchBuilder<AffinityGroupVMMapVO> ListByAffinityGroup;
    private SearchBuilder<AffinityGroupVMMapVO> ListByVmIdType;

    @Inject
    protected AffinityGroupDao _affinityGroupDao;

    public AffinityGroupVMMapDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        ListVmIdByAffinityGroup = createSearchBuilder(Long.class);
        ListVmIdByAffinityGroup.and("affinityGroupId", ListVmIdByAffinityGroup.entity().getAffinityGroupId(),
                SearchCriteria.Op.EQ);
        ListVmIdByAffinityGroup.selectField(ListVmIdByAffinityGroup.entity().getInstanceId());
        ListVmIdByAffinityGroup.done();

        ListByAffinityGroup = createSearchBuilder();
        ListByAffinityGroup.and("affinityGroupId", ListByAffinityGroup.entity().getAffinityGroupId(),
                SearchCriteria.Op.EQ);
        ListByAffinityGroup.done();

        ListByVmId  = createSearchBuilder();
        ListByVmId.and("instanceId", ListByVmId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByVmId.done();

        ListByVmIdGroupId  = createSearchBuilder();
        ListByVmIdGroupId.and("instanceId", ListByVmIdGroupId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.and("affinityGroupId", ListByVmIdGroupId.entity().getAffinityGroupId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.done();

        ListByVmIdType = createSearchBuilder();
        ListByVmIdType.and("instanceId", ListByVmIdType.entity().getInstanceId(), SearchCriteria.Op.EQ);
        SearchBuilder<AffinityGroupVO> groupSearch = _affinityGroupDao.createSearchBuilder();
        groupSearch.and("type", groupSearch.entity().getType(), SearchCriteria.Op.EQ);
        ListByVmIdType.join("groupSearch", groupSearch, ListByVmIdType.entity().getAffinityGroupId(), groupSearch
                .entity().getId(), JoinType.INNER);
        ListByVmIdType.done();

        CountSGForVm = createSearchBuilder(Long.class);
        CountSGForVm.select(null, Func.COUNT, null);
        CountSGForVm.and("vmId", CountSGForVm.entity().getInstanceId(), SearchCriteria.Op.EQ);
        CountSGForVm.done();
    }

    @Override
    public List<AffinityGroupVMMapVO> listByAffinityGroup(long affinityGroupId) {
        SearchCriteria<AffinityGroupVMMapVO> sc = ListByAffinityGroup.create();
        sc.setParameters("affinityGroupId", affinityGroupId);
        return listBy(sc);
    }

    @Override
    public List<AffinityGroupVMMapVO> listByInstanceId(long vmId) {
        SearchCriteria<AffinityGroupVMMapVO> sc = ListByVmId.create();
        sc.setParameters("instanceId", vmId);
        return listBy(sc);
    }

    @Override
    public Pair<List<AffinityGroupVMMapVO>, Integer> listByInstanceId(long instanceId, Filter filter) {
        SearchCriteria<AffinityGroupVMMapVO> sc = ListByVmId.create();
        sc.setParameters("instanceId", instanceId);
        return this.searchAndCount(sc, filter);
    }

    @Override
    public int deleteVM(long instanceId) {
        SearchCriteria<AffinityGroupVMMapVO> sc = ListByVmId.create();
        sc.setParameters("instanceId", instanceId);
        return super.expunge(sc);
    }

    @Override
    public List<Long> listVmIdsByAffinityGroup(long affinityGroupId) {
        SearchCriteria<Long> sc = ListVmIdByAffinityGroup.create();
        sc.setParameters("affinityGroupId", affinityGroupId);
        return customSearchIncludingRemoved(sc, null);
    }

	@Override
    public AffinityGroupVMMapVO findByVmIdGroupId(long instanceId, long affinityGroupId) {
        SearchCriteria<AffinityGroupVMMapVO> sc = ListByVmIdGroupId.create();
        sc.setParameters("affinityGroupId", affinityGroupId);
        sc.setParameters("instanceId", instanceId);
		return findOneIncludingRemovedBy(sc);
	}

	@Override
    public long countAffinityGroupsForVm(long instanceId) {
		SearchCriteria<Long> sc = CountSGForVm.create();
    	sc.setParameters("vmId", instanceId);
        return customSearch(sc, null).get(0);
	}

    @Override
    public AffinityGroupVMMapVO findByVmIdType(long instanceId, String type) {
        SearchCriteria<AffinityGroupVMMapVO> sc = ListByVmIdType.create();
        sc.setParameters("instanceId", instanceId);
        sc.setJoinParameters("groupSearch", "type", type);
        return customSearch(sc, null).get(0);
    }

    @Override
    public void updateMap(Long vmId, List<Long> affinityGroupIds) {
        Transaction txn = Transaction.currentTxn();
        txn.start();

        SearchCriteria<AffinityGroupVMMapVO> sc = createSearchCriteria();
        sc.addAnd("instanceId", SearchCriteria.Op.EQ, vmId);
        expunge(sc);

        for (Long groupId : affinityGroupIds) {
            AffinityGroupVMMapVO vo = new AffinityGroupVMMapVO(groupId, vmId);
            persist(vo);
        }

        txn.commit();

    }
}
