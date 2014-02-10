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
package com.cloud.netapp.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.netapp.LunVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = {LunDao.class})
public class LunDaoImpl extends GenericDaoBase<LunVO, Long> implements LunDao {
    private static final Logger s_logger = Logger.getLogger(PoolDaoImpl.class);

    protected final SearchBuilder<LunVO> LunSearch;
    protected final SearchBuilder<LunVO> LunNameSearch;

    protected LunDaoImpl() {

        LunSearch = createSearchBuilder();
        LunSearch.and("volumeId", LunSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        LunSearch.done();

        LunNameSearch = createSearchBuilder();
        LunNameSearch.and("name", LunNameSearch.entity().getLunName(), SearchCriteria.Op.EQ);
        LunNameSearch.done();

    }

    @Override
    public List<LunVO> listLunsByVolId(Long volId) {
        Filter searchFilter = new Filter(LunVO.class, "id", Boolean.TRUE, Long.valueOf(0), Long.valueOf(10000));

        SearchCriteria sc = LunSearch.create();
        sc.setParameters("volumeId", volId);
        List<LunVO> lunList = listBy(sc, searchFilter);

        return lunList;
    }

    @Override
    public LunVO findByName(String name) {
        SearchCriteria sc = LunNameSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }
}
