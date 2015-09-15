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

import org.springframework.stereotype.Component;

import com.cloud.netapp.PoolVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = {PoolDao.class})
public class PoolDaoImpl extends GenericDaoBase<PoolVO, Long> implements PoolDao {

    protected final SearchBuilder<PoolVO> PoolSearch;

    protected PoolDaoImpl() {

        PoolSearch = createSearchBuilder();
        PoolSearch.and("name", PoolSearch.entity().getName(), SearchCriteria.Op.EQ);
        PoolSearch.done();

    }

    @Override
    public PoolVO findPool(String poolName) {
        SearchCriteria sc = PoolSearch.create();
        sc.setParameters("name", poolName);
        List<PoolVO> poolList = listBy(sc);

        return (poolList.size() > 0 ? poolList.get(0) : null);
    }

    @Override
    public List<PoolVO> listPools() {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
//    public List<NetappStoragePoolVO> listVolumes(String poolName) {
//        SearchCriteria sc = NetappListVolumeSearch.create();
//        sc.setParameters("poolName", poolName);
//        return listBy(sc);
//    }

}
