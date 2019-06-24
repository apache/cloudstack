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


import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

public class SspCredentialDaoImpl extends GenericDaoBase<SspCredentialVO, Long> implements SspCredentialDao {
    private final SearchBuilder<SspCredentialVO> PnetSearch;

    public SspCredentialDaoImpl() {
        PnetSearch = createSearchBuilder();
        PnetSearch.and("dataCenterId", PnetSearch.entity().getZoneId(), Op.EQ);
        PnetSearch.done();
    }

    @Override
    public SspCredentialVO findByZone(long zoneId) {
        SearchCriteria<SspCredentialVO> sc = PnetSearch.create();
        sc.setParameters("dataCenterId", zoneId);
        return findOneBy(sc);
    }
}
