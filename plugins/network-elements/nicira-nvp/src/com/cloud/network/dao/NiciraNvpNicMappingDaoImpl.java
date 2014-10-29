//
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
//

package com.cloud.network.dao;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.network.NiciraNvpNicMappingVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value = NiciraNvpNicMappingDao.class)
public class NiciraNvpNicMappingDaoImpl extends GenericDaoBase<NiciraNvpNicMappingVO, Long> implements NiciraNvpNicMappingDao {

    protected final SearchBuilder<NiciraNvpNicMappingVO> nicSearch;

    public NiciraNvpNicMappingDaoImpl() {
        nicSearch = createSearchBuilder();
        nicSearch.and("nicUuid", nicSearch.entity().getNicUuid(), Op.EQ);
        nicSearch.done();
    }

    @Override
    public NiciraNvpNicMappingVO findByNicUuid(final String nicUuid) {
        SearchCriteria<NiciraNvpNicMappingVO> sc = nicSearch.create();
        sc.setParameters("nicUuid", nicUuid);
        return findOneBy(sc);
    }

}
