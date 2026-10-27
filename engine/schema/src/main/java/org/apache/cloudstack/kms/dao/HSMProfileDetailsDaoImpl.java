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

package org.apache.cloudstack.kms.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import org.apache.cloudstack.kms.HSMProfileDetailsVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HSMProfileDetailsDaoImpl extends GenericDaoBase<HSMProfileDetailsVO, Long> implements HSMProfileDetailsDao {

    protected SearchBuilder<HSMProfileDetailsVO> ProfileSearch;
    protected SearchBuilder<HSMProfileDetailsVO> DetailSearch;

    public HSMProfileDetailsDaoImpl() {
        super();

        ProfileSearch = createSearchBuilder();
        ProfileSearch.and("profileId", ProfileSearch.entity().getResourceId(), Op.EQ);
        ProfileSearch.done();

        DetailSearch = createSearchBuilder();
        DetailSearch.and("profileId", DetailSearch.entity().getResourceId(), Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), Op.EQ);
        DetailSearch.done();
    }

    @Override
    public List<HSMProfileDetailsVO> listByProfileId(long profileId) {
        SearchCriteria<HSMProfileDetailsVO> sc = ProfileSearch.create();
        sc.setParameters("profileId", profileId);
        return listBy(sc);
    }

    @Override
    public void persist(long profileId, String name, String value) {
        HSMProfileDetailsVO vo = new HSMProfileDetailsVO(profileId, name, value);
        persist(vo);
    }

    @Override
    public HSMProfileDetailsVO findDetail(long profileId, String name) {
        SearchCriteria<HSMProfileDetailsVO> sc = DetailSearch.create();
        sc.setParameters("profileId", profileId);
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    @Override
    public void deleteDetails(long profileId) {
        SearchCriteria<HSMProfileDetailsVO> sc = ProfileSearch.create();
        sc.setParameters("profileId", profileId);
        remove(sc);
    }
}
