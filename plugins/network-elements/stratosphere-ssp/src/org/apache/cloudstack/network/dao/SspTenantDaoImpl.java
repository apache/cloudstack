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
package org.apache.cloudstack.network.dao;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(SspTenantDao.class)
public class SspTenantDaoImpl extends GenericDaoBase<SspTenantVO, Long>
        implements SspTenantDao {

    private final SearchBuilder<SspTenantVO> zoneSearch;

    public SspTenantDaoImpl(){
        zoneSearch = createSearchBuilder();
        zoneSearch.and("zoneId", zoneSearch.entity().getZoneId(), Op.EQ);
        zoneSearch.done();
    }

    @Override
    public String findUuidByZone(long zoneId) {
        SearchCriteria<SspTenantVO> sc = zoneSearch.create();
        sc.setParameters("zoneId", zoneId);
        SspTenantVO ret = findOneBy(sc);
        if(ret != null){
            return ret.getUuid();
        }
        return null;
    }
}
