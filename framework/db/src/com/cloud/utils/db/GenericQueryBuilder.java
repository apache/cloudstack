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

/**
 * GenericQueryBuilder builds a search query during runtime.  It allows the
 * search query to be built completely in Java rather than part SQL fragments
 * and part entity field like HQL or JPQL.  This class is different from
 * GenericSearchBuilder in that it is used for building queries during runtime
 * where GenericSearchBuilder expects the query to be built during load time
 * and parameterized values to be set during runtime.
 *
 * GenericQueryBuilder allows results to be a native type, the entity bean,
 * and a composite type.  If you are just retrieving the entity bean, there
 * is a simpler class called QueryBuilder that you can use.  The usage
 * is approximately the same.
 *
 * <code>
 * // Note that in the following search, it selects a func COUNT to be the
 * // return result so for the second parameterized type is long.
 * // Note the entity object itself must have came from search and
 * // it uses the getters of the object to retrieve the field used in the search.
 *
 * GenericQueryBuilder<HostVO, Long> sc = GenericQueryBuilder.create(HostVO.class, Long.class);
 * HostVO entity = CountSearch.entity();
 * sc.select(null, FUNC.COUNT, null, null).where(entity.getType(), Op.EQ, Host.Type.Routing);
 * sc.and(entity.getCreated(), Op.LT, new Date());
 * Long count = sc.find();
 *
 * </code> *
 *
 * @see GenericSearchBuilder
 * @see QueryBuilder
 *
 * @param <T> Entity object to perform the search on
 * @param <K> Result object
 */
public class GenericQueryBuilder<T, K> extends SearchBase<GenericQueryBuilder<T, K>, T, K> {
    final HashMap<String, Object[]> _params = new HashMap<String, Object[]>();

    protected GenericQueryBuilder(Class<T> entityType, Class<K> resultType) {
        super(entityType, resultType);
    }

    /**
     * Creator method for GenericQueryBuilder.
     * @param entityType Entity to search on
     * @param resultType Result to return
     * @return GenericQueryBuilder
     */
    @SuppressWarnings("unchecked")
    static public <T, K> GenericQueryBuilder<T, K> create(Class<T> entityType, Class<K> resultType) {
        GenericDao<T, ? extends Serializable> dao = (GenericDao<T, ? extends Serializable>)GenericDaoBase.getDao(entityType);
        assert dao != null : "Can not find DAO for " + entityType.getName();
        return new GenericQueryBuilder<T, K>(entityType, resultType);
    }

    /**
     * Adds AND search condition
     *
     * @param field the field of the entity to perform the search on.
     * @param op operator
     * @param values parameterized values
     * @return this
     */
    public GenericQueryBuilder<T, K> and(Object field, Op op, Object... values) {
        String uuid = UUID.randomUUID().toString();
        constructCondition(uuid, " AND ", _specifiedAttrs.get(0), op);
        _params.put(uuid, values);
        return this;
    }

    /**
     * Adds OR search condition
     *
     * @param field the field of the entity to perform the search on.
     * @param op operator
     * @param values parameterized values
     * @return this
     */
    public GenericQueryBuilder<T, K> or(Object field, Op op, Object... values) {
        String uuid = UUID.randomUUID().toString();
        constructCondition(uuid, " OR ", _specifiedAttrs.get(0), op);
        _params.put(uuid, values);
        return this;
    }

    protected GenericQueryBuilder<T, K> left(Object field, Op op, Object... values) {
        String uuid = UUID.randomUUID().toString();
        constructCondition(uuid, " ( ", _specifiedAttrs.get(0), op);
        _params.put(uuid, values);
        return this;
    }

    /**
     * Adds search condition that starts with an open parenthesis.  Call cp()
     * to close the parenthesis.
     *
     * @param field the field of the entity to perform the search on.
     * @param op operator
     * @param values parameterized values
     * @return this
     */
    public GenericQueryBuilder<T, K> op(Object field, Op op, Object... values) {
        return left(field, op, values);
    }

    /**
     * If the query is supposed to return a list, use this.
     * @return List of result objects
     */
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

    /**
     * Creates a SearchCriteria to be used with dao objects.
     */
    @Override
    public SearchCriteria<K> create() {
        SearchCriteria<K> sc = super.create();
        sc.setParameters(_params);
        return sc;
    }

    private boolean isSelectAll() {
        return _selects == null || _selects.size() == 0;
    }

    /**
     * Convenience method to find the result so the result won't be a list.
     * @return result as specified.
     */
    @SuppressWarnings("unchecked")
    public K find() {
        finalize();
        if (isSelectAll()) {
            @SuppressWarnings("rawtypes")
            SearchCriteria sc1 = create();
            return (K)_dao.findOneBy(sc1);
        } else {
            List<K> lst = list();
            return lst.get(0);
        }
    }
}
