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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;

public class SearchCriteria2<T, K> extends SearchBase<T, K> {
    final HashMap<String, Object[]> _params = new HashMap<String, Object[]>();

    protected SearchCriteria2(Class<T> entityType, Class<K> resultType) {
        super(entityType, resultType);
    }

    @SuppressWarnings("unchecked")
    static public <T, K> SearchCriteria2<T, K> create(Class<T> entityType, Class<K> resultType) {
        GenericDao<T, ? extends Serializable> dao = (GenericDao<T, ? extends Serializable>)GenericDaoBase.getDao(entityType);
        assert dao != null : "Can not find DAO for " + entityType.getName();
        return new SearchCriteria2<T, K>(entityType, resultType);
    }

    static public <T> SearchCriteria2<T, T> create(Class<T> entityType) {
        return create(entityType, entityType);
    }

    public void selectField(Object... useless) {
        assert _entity != null : "SearchBuilder cannot be modified once it has been setup";
        assert _specifiedAttrs.size() > 0 : "You didn't specify any attributes";

        if (_selects == null) {
            _selects = new ArrayList<Select>();
        }

        for (Attribute attr : _specifiedAttrs) {
            Field field = null;
            try {
                field = _resultType.getDeclaredField(attr.field.getName());
                field.setAccessible(true);
            } catch (SecurityException e) {
            } catch (NoSuchFieldException e) {
            }
            _selects.add(new Select(Func.NATIVE, attr, field, null));
        }

        _specifiedAttrs.clear();
    }

    private void setParameters(String conditionName, Object... params) {
        assert _conditions.contains(new Condition(conditionName)) : "Couldn't find " + conditionName;
        _params.put(conditionName, params);
    }

    public void addAnd(Object useless, Op op, Object... values) {
        String uuid = UUID.randomUUID().toString();
        constructCondition(uuid, " AND ", _specifiedAttrs.get(0), op);
        setParameters(uuid, values);
    }

    public List<K> list() {
        finalize();
        SearchCriteria sc1 = create();
        if (isSelectAll()) {
            return (List<K>)_dao.search(sc1, null);
        } else {
            return _dao.customSearch(sc1, null);
        }
    }

    private boolean isSelectAll() {
        return _selects == null || _selects.size() == 0;
    }

    public T getEntity() {
        return _entity;
    }

    public K find() {
        assert isSelectAll() : "find doesn't support select search";
        finalize();
        SearchCriteria sc1 = create();
        return (K)_dao.findOneBy(sc1);
    }

    public Preset and(Object useless, Op op) {
        Condition condition = constructCondition(UUID.randomUUID().toString(), " AND ", _specifiedAttrs.get(0), op);
        return new Preset(this, condition);
    }

    public Preset where(Object useless, Op op) {
        return and(useless, op);
    }

    public Preset left(Object useless, Op op) {
        Condition condition = constructCondition(UUID.randomUUID().toString(), " ( ", _specifiedAttrs.get(0), op);
        return new Preset(this, condition);
    }

    public Preset op(Object useless, Op op) {
        return left(useless, op);
    }

    public Preset openParen(Object useless, Op op) {
        return left(useless, op);
    }

    public Preset or(Object useless, Op op) {
        Condition condition = constructCondition(UUID.randomUUID().toString(), " OR ", _specifiedAttrs.get(0), op);
        return new Preset(this, condition);
    }

    public class Preset {
        SearchCriteria2<T, K> builder;
        Condition condition;

        protected Preset(SearchCriteria2<T, K> builder, Condition condition) {
            this.builder = builder;
            this.condition = condition;
        }

        public SearchCriteria2<T, K> values(Object... params) {
            if (condition.op.getParams() > 0 && condition.op.params != params.length) {
                throw new RuntimeException("The # of parameters set " + params.length + " does not match # of parameters required by " + condition.op);
            }
            condition.setPresets(params);
            return builder;
        }
    }

}
