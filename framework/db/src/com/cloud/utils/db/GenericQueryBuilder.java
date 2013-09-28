// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.db;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.cloud.utils.db.SearchCriteria.Op;

public class GenericQueryBuilder<T, K> extends SearchBase<GenericQueryBuilder<T, K>, T, K> {
    final HashMap<String, Object[]> _params = new HashMap<String, Object[]>();

    protected GenericQueryBuilder(Class<T> entityType, Class<K> resultType) {
        super(entityType, resultType);
    }

    @SuppressWarnings("unchecked")
    static public <T, K> GenericQueryBuilder<T, K> create(Class<T> entityType, Class<K> resultType) {
        GenericDao<T, ? extends Serializable> dao = (GenericDao<T, ? extends Serializable>)GenericDaoBase.getDao(entityType);
        assert dao != null : "Can not find DAO for " + entityType.getName();
        return new GenericQueryBuilder<T, K>(entityType, resultType);
    }

    public GenericQueryBuilder<T, K> and(Object useless, Op op, Object... values) {
        String uuid = UUID.randomUUID().toString();
        constructCondition(uuid, " AND ", _specifiedAttrs.get(0), op);
        _params.put(uuid, values);
        return this;
    }

    public GenericQueryBuilder<T, K> or(Object useless, Op op, Object... values) {
        String uuid = UUID.randomUUID().toString();
        constructCondition(uuid, " OR ", _specifiedAttrs.get(0), op);
        _params.put(uuid, values);
        return this;
    }

    protected GenericQueryBuilder<T, K> left(Object useless, Op op, Object... values) {
        String uuid = UUID.randomUUID().toString();
        constructCondition(uuid, " ( ", _specifiedAttrs.get(0), op);
        _params.put(uuid, values);
        return this;
    }

    public GenericQueryBuilder<T, K> op(Object useless, Op op, Object... values) {
        return left(useless, op, values);
    }

    @SuppressWarnings("unchecked")
    public List<K> list() {
        finalize();
        if (isSelectAll()) {
            @SuppressWarnings("rawtypes")
            SearchCriteria sc1 = create();
            return (List<K>)_dao.search(sc1, null);
        } else {
            SearchCriteria<K> sc1 = create();
            return _dao.customSearch(sc1, null);
        }
    }

    @Override
    public SearchCriteria<K> create() {
        SearchCriteria<K> sc = super.create();
        sc.setParameters(_params);
        return sc;
    }

    private boolean isSelectAll() {
        return _selects == null || _selects.size() == 0;
    }

    @SuppressWarnings("unchecked")
    public K find() {
        finalize();
        @SuppressWarnings("rawtypes")
        SearchCriteria sc1 = create();
        return (K)_dao.findOneBy(sc1);
    }
}
