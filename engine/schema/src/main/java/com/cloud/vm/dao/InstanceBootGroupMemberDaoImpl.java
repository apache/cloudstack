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

package com.cloud.vm.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMemberVO;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMember;

@Component
public class InstanceBootGroupMemberDaoImpl extends GenericDaoBase<InstanceBootGroupMemberVO, Long> implements InstanceBootGroupMemberDao {

    private final SearchBuilder<InstanceBootGroupMemberVO> bootGroupSearch;
    private final SearchBuilder<InstanceBootGroupMemberVO> bootGroupTypeSearch;
    private final SearchBuilder<InstanceBootGroupMemberVO> memberSearch;

    public InstanceBootGroupMemberDaoImpl() {
        bootGroupSearch = createSearchBuilder();
        bootGroupSearch.and("bootGroupId", bootGroupSearch.entity().getBootGroupId(), SearchCriteria.Op.EQ);
        bootGroupSearch.done();

        bootGroupTypeSearch = createSearchBuilder();
        bootGroupTypeSearch.and("bootGroupId", bootGroupTypeSearch.entity().getBootGroupId(), SearchCriteria.Op.EQ);
        bootGroupTypeSearch.and("memberType", bootGroupTypeSearch.entity().getMemberType(), SearchCriteria.Op.EQ);
        bootGroupTypeSearch.done();

        memberSearch = createSearchBuilder();
        memberSearch.and("memberType", memberSearch.entity().getMemberType(), SearchCriteria.Op.EQ);
        memberSearch.and("memberId", memberSearch.entity().getMemberId(), SearchCriteria.Op.EQ);
        memberSearch.done();
    }

    @Override
    public List<InstanceBootGroupMemberVO> listByBootGroupId(long bootGroupId) {
        SearchCriteria<InstanceBootGroupMemberVO> sc = bootGroupSearch.create();
        sc.setParameters("bootGroupId", bootGroupId);
        return listBy(sc, null);
    }

    @Override
    public int countByBootGroupId(long bootGroupId) {
        SearchCriteria<InstanceBootGroupMemberVO> sc = bootGroupSearch.create();
        sc.setParameters("bootGroupId", bootGroupId);
        return getCount(sc);
    }

    @Override
    public List<InstanceBootGroupMemberVO> listByBootGroupIdAndEqualOrHigherOrder(long bootGroupId, int order) {
        SearchBuilder<InstanceBootGroupMemberVO> bootGroupOrderSearch = createSearchBuilder();
        bootGroupOrderSearch.and("bootGroupId", bootGroupOrderSearch.entity().getBootGroupId(), SearchCriteria.Op.EQ);
        bootGroupOrderSearch.and("bootOrder", bootGroupOrderSearch.entity().getBootOrder(), SearchCriteria.Op.GTEQ);
        bootGroupOrderSearch.done();
        SearchCriteria<InstanceBootGroupMemberVO> sc = bootGroupOrderSearch.create();
        sc.setParameters("bootGroupId", bootGroupId);
        sc.setParameters("bootOrder", order);
        return listBy(sc, null);
    }

    @Override
    public List<InstanceBootGroupMemberVO> listByBootGroupIdAndOrderRange(long bootGroupId, int low, int high) {
        SearchBuilder<InstanceBootGroupMemberVO> bootGroupOrderSearch = createSearchBuilder();
        bootGroupOrderSearch.and("bootGroupId", bootGroupOrderSearch.entity().getBootGroupId(), SearchCriteria.Op.EQ);
        bootGroupOrderSearch.and("low", bootGroupOrderSearch.entity().getBootOrder(), SearchCriteria.Op.GTEQ);
        bootGroupOrderSearch.and("high", bootGroupOrderSearch.entity().getBootOrder(), SearchCriteria.Op.LTEQ);
        bootGroupOrderSearch.done();
        SearchCriteria<InstanceBootGroupMemberVO> sc = bootGroupOrderSearch.create();
        sc.setParameters("bootGroupId", bootGroupId);
        sc.setParameters("low", low);
        sc.setParameters("high", high);
        return listBy(sc, null);
    }

    @Override
    public Pair<List<InstanceBootGroupMemberVO>, Integer> searchAndCountByBootGroupId(long bootGroupId) {
        SearchCriteria<InstanceBootGroupMemberVO> sc = bootGroupSearch.create();
        sc.setParameters("bootGroupId", bootGroupId);
        return searchAndCount(sc, null);
    }

    @Override
    public Pair<List<InstanceBootGroupMemberVO>, Integer> searchAndCountByBootGroupIdAndType(long bootGroupId, InstanceBootGroupMember.MemberType memberType) {
        SearchCriteria<InstanceBootGroupMemberVO> sc = bootGroupTypeSearch.create();
        sc.setParameters("bootGroupId", bootGroupId);
        sc.setParameters("memberType", memberType);
        return searchAndCount(sc, null);
    }

    @Override
    public InstanceBootGroupMemberVO findByMember(InstanceBootGroupMember.MemberType memberType, long memberId) {
        SearchCriteria<InstanceBootGroupMemberVO> sc = memberSearch.create();
        sc.setParameters("memberType", memberType);
        sc.setParameters("memberId", memberId);
        return findOneBy(sc);
    }

    @Override
    public void deleteByBootGroupId(long bootGroupId) {
        SearchCriteria<InstanceBootGroupMemberVO> sc = bootGroupSearch.create();
        sc.setParameters("bootGroupId", bootGroupId);
        expunge(sc);
    }
}
