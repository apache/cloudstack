/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.image.db;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;


import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.storage.ScopeType;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class ImageStoreDaoImpl extends GenericDaoBase<ImageStoreVO, Long> implements ImageStoreDao {

    private static final Logger s_logger = Logger.getLogger(ImageStoreDaoImpl.class);
    private SearchBuilder<ImageStoreVO> nameSearch;
    private SearchBuilder<ImageStoreVO> providerSearch;
    private SearchBuilder<ImageStoreVO> scopeSearch;


    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        nameSearch = createSearchBuilder();
        nameSearch.and("name", nameSearch.entity().getName(), SearchCriteria.Op.EQ);
        nameSearch.done();

        providerSearch = createSearchBuilder();
        providerSearch.and("providerName", providerSearch.entity().getProviderName(), SearchCriteria.Op.EQ);
        providerSearch.done();

        scopeSearch = createSearchBuilder();
        scopeSearch.and("scope", scopeSearch.entity().getScope(), SearchCriteria.Op.EQ);
        scopeSearch.done();

        return true;
    }

    @Override
    public ImageStoreVO findByName(String name) {
        SearchCriteria<ImageStoreVO> sc = nameSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    @Override
    public List<ImageStoreVO> findByProvider(String provider) {
        SearchCriteria<ImageStoreVO> sc = providerSearch.create();
        sc.setParameters("providerName", provider);
        return listBy(sc);
    }

    @Override
    public List<ImageStoreVO> findByScope(ScopeType scope) {
        SearchCriteria<ImageStoreVO> sc = scopeSearch.create();
        sc.setParameters("scope", scope);
        return listBy(sc);
    }


}
