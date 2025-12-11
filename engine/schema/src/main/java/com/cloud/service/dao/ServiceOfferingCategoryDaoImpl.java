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
package com.cloud.service.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloud.service.ServiceOfferingCategoryVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.DB;

@Component
@DB()
public class ServiceOfferingCategoryDaoImpl extends GenericDaoBase<ServiceOfferingCategoryVO, Long> implements ServiceOfferingCategoryDao {

    protected final SearchBuilder<ServiceOfferingCategoryVO> NameSearch;

    protected ServiceOfferingCategoryDaoImpl() {
        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameSearch.done();
    }

    @Override
    public ServiceOfferingCategoryVO findByName(String name) {
        SearchCriteria<ServiceOfferingCategoryVO> sc = NameSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    @Override
    public List<ServiceOfferingCategoryVO> listAll() {
        return listAll(null);
    }
}