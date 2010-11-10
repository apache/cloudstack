/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.utils.db;

import java.lang.reflect.Field;

import javax.persistence.Column;

import com.cloud.utils.Pair;
import com.cloud.utils.ReflectUtil;

/**
 *  Filter is used to filter the results returned by searches and lists.
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
    
    public Filter(long limit, boolean random) {
    	_orderBy = (random ? " ORDER BY RAND() " : "") + "LIMIT " + limit;  
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
        assert(pair != null) : "Can't find field " + field + " in " + clazz.getName();
        clazz = pair.first();
        f = pair.second();
        
        Column column = f.getAnnotation(Column.class);
        String name = column != null ? column.name() : field;
        
        StringBuilder order = new StringBuilder();
        if (column.table() == null || column.table().length() == 0) {
            order.append(DbUtil.getTableName(clazz));
        } else {
            order.append(column.table());
        }
        order.append(".").append(name).append(ascending ? " ASC " : " DESC ");
        
        if (_orderBy == null) {
            _orderBy = order.insert(0, " ORDER BY ").toString();
        } else {
            _orderBy = order.insert(0, _orderBy).toString();
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
