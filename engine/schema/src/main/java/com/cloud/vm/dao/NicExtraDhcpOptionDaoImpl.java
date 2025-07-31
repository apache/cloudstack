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

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.NicExtraDhcpOption;
import com.cloud.vm.NicExtraDhcpOptionVO;

@Component
public class NicExtraDhcpOptionDaoImpl  extends GenericDaoBase<NicExtraDhcpOptionVO, Long> implements NicExtraDhcpOptionDao {
    private SearchBuilder<NicExtraDhcpOptionVO> AllFieldsSearch;

    protected NicExtraDhcpOptionDaoImpl() {
        super();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("nic_id", AllFieldsSearch.entity().getNicId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("code", AllFieldsSearch.entity().getCode(), SearchCriteria.Op.IN);

        AllFieldsSearch.done();
    }

    @DB()
    @Override
    public List<NicExtraDhcpOptionVO> listByNicId(long nicId) {
        SearchCriteria<NicExtraDhcpOptionVO> sc = AllFieldsSearch.create();
        sc.setParameters("nic_id", nicId);

        return listBy(sc);
    }

    @DB()
    @Override
    public void saveExtraDhcpOptions(List<NicExtraDhcpOptionVO> extraDhcpOptions) {
        if (extraDhcpOptions.isEmpty()) {
            return;
        }

        extraDhcpOptions
                .stream()
                .map(NicExtraDhcpOption::getNicId)
                .distinct()
                .forEach(this::removeByNicId);

        extraDhcpOptions.stream()
                .forEach(this::persist);
    }

    public void removeByNicId(long nicId) {
        SearchCriteria<NicExtraDhcpOptionVO> sc = AllFieldsSearch.create();
        sc.setParameters("nic_id", nicId);
        expunge(sc);
    }

    @Override
    public int expungeByNicList(List<Long> nicIds, Long batchSize) {
        if (CollectionUtils.isEmpty(nicIds)) {
            return 0;
        }
        SearchBuilder<NicExtraDhcpOptionVO> sb = createSearchBuilder();
        sb.and("nicIds", sb.entity().getNicId(), SearchCriteria.Op.IN);
        SearchCriteria<NicExtraDhcpOptionVO> sc = sb.create();
        sc.setParameters("nicIds", nicIds.toArray());
        return batchExpunge(sc, batchSize);
    }
}
