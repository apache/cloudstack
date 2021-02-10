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
package org.apache.cloudstack.storage.datastore.db;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class ImageStoreDaoImpl extends GenericDaoBase<ImageStoreVO, Long> implements ImageStoreDao {
    private SearchBuilder<ImageStoreVO> nameSearch;
    private SearchBuilder<ImageStoreVO> providerSearch;
    private SearchBuilder<ImageStoreVO> regionSearch;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        nameSearch = createSearchBuilder();
        nameSearch.and("name", nameSearch.entity().getName(), SearchCriteria.Op.EQ);
        nameSearch.and("role", nameSearch.entity().getRole(), SearchCriteria.Op.EQ);
        nameSearch.done();

        providerSearch = createSearchBuilder();
        providerSearch.and("providerName", providerSearch.entity().getProviderName(), SearchCriteria.Op.EQ);
        providerSearch.and("role", providerSearch.entity().getRole(), SearchCriteria.Op.EQ);
        providerSearch.done();

        regionSearch = createSearchBuilder();
        regionSearch.and("scope", regionSearch.entity().getScope(), SearchCriteria.Op.EQ);
        regionSearch.and("role", regionSearch.entity().getRole(), SearchCriteria.Op.EQ);
        regionSearch.done();

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
        sc.setParameters("role", DataStoreRole.Image);
        return listBy(sc);
    }

    @Override
    public List<ImageStoreVO> findByZone(ZoneScope scope, Boolean readonly) {
        SearchCriteria<ImageStoreVO> sc = createSearchCriteria();
        sc.addAnd("role", SearchCriteria.Op.EQ, DataStoreRole.Image);
        if (readonly != null) {
            sc.addAnd("readonly", SearchCriteria.Op.EQ, readonly);
        }
        if (scope.getScopeId() != null) {
            SearchCriteria<ImageStoreVO> scc = createSearchCriteria();
            scc.addOr("scope", SearchCriteria.Op.EQ, ScopeType.REGION);
            scc.addOr("dcId", SearchCriteria.Op.EQ, scope.getScopeId());
            sc.addAnd("scope", SearchCriteria.Op.SC, scc);
        }
        // we should return all image stores if cross-zone scope is passed
        // (scopeId = null)
        return listBy(sc);
    }

    @Override
    public List<ImageStoreVO> findRegionImageStores() {
        SearchCriteria<ImageStoreVO> sc = regionSearch.create();
        sc.setParameters("scope", ScopeType.REGION);
        sc.setParameters("role", DataStoreRole.Image);
        return listBy(sc);
    }

    @Override
    public List<ImageStoreVO> findImageCacheByScope(ZoneScope scope) {
        SearchCriteria<ImageStoreVO> sc = createSearchCriteria();
        sc.addAnd("role", SearchCriteria.Op.EQ, DataStoreRole.ImageCache);
        if (scope.getScopeId() != null) {
            sc.addAnd("scope", SearchCriteria.Op.EQ, ScopeType.ZONE);
            sc.addAnd("dcId", SearchCriteria.Op.EQ, scope.getScopeId());
        }
        return listBy(sc);
    }

    @Override
    public Integer countAllImageStores() {
        SearchCriteria<ImageStoreVO> sc = createSearchCriteria();
        sc.addAnd("role", SearchCriteria.Op.EQ, DataStoreRole.Image);
        return getCount(sc);
    }

    @Override
    public List<ImageStoreVO> listImageStores() {
        SearchCriteria<ImageStoreVO> sc = createSearchCriteria();
        sc.addAnd("role", SearchCriteria.Op.EQ, DataStoreRole.Image);
        return listBy(sc);
    }

    @Override
    public List<ImageStoreVO> listImageCacheStores() {
        SearchCriteria<ImageStoreVO> sc = createSearchCriteria();
        sc.addAnd("role", SearchCriteria.Op.EQ, DataStoreRole.ImageCache);
        return listBy(sc);
    }

    @Override
    public List<ImageStoreVO> listStoresByZoneId(long zoneId) {
        SearchCriteria<ImageStoreVO> sc = createSearchCriteria();
        sc.addAnd("dcId", SearchCriteria.Op.EQ, zoneId);
        return listBy(sc);
    }
}
