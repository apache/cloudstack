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

import java.lang.reflect.Field;

import javax.persistence.Column;

import com.cloud.utils.Pair;
import com.cloud.utils.ReflectUtil;

/**
 *  Try to use static initialization to help you in finding incorrect
 *  field names being passed in early.
 *
 *  Something like the following:
 *  protected static final Filter s_NameFilter = new Filter(VMInstanceVO, name, true, null, null);
 *
 *  Filter nameFilter = new Filter(s_nameFilter);
 *
 */
public class Filter {
    Long _offset;
    Long _limit;
    String _orderBy;

    /**
     * @param clazz the VO object type
     * @param field name of the field
     * @param offset
     * @param limit
     */
    public Filter(Class<?> clazz, String field, boolean ascending, Long offset, Long limit) {
        _offset = offset;
        _limit = limit;

        addOrderBy(clazz, field, ascending);
    }

    public Filter(long limit) {
        _orderBy = " ORDER BY RAND() LIMIT " + limit;
    }

    /**
     * Note that this copy constructor does not copy offset and limit.
     * @param that filter
     */
    public Filter(Filter that) {
        this._orderBy = that._orderBy;
        this._limit = null;
        that._limit = null;
    }

    public void addOrderBy(Class<?> clazz, String field, boolean ascending) {
        if (field == null) {
            return;
        }
        Field f;
        Pair<Class<?>, Field> pair = ReflectUtil.getAnyField(clazz, field);
        assert (pair != null) : "Can't find field " + field + " in " + clazz.getName();
        clazz = pair.first();
        f = pair.second();

        Column column = f.getAnnotation(Column.class);
        String name = column != null ? column.name() : field;

        StringBuilder order = new StringBuilder();
        if (column == null || column.table() == null || column.table().length() == 0) {
            order.append(DbUtil.getTableName(clazz));
        } else {
            order.append(column.table());
        }
        order.append(".").append(name).append(ascending ? " ASC " : " DESC ");

        if (_orderBy == null) {
            _orderBy = order.insert(0, " ORDER BY ").toString();
        } else {
            _orderBy = order.insert(0, _orderBy + ", ").toString();
        }
    }

    public String getOrderBy() {
        return _orderBy;
    }

    public void setOffset(Long offset) {
        _offset = offset;
    }

    public Long getOffset() {
        return _offset;
    }

    public Long getLimit() {
        return _limit;
    }

    public void setLimit(Long limit) {
        _limit = limit;
    }
}
