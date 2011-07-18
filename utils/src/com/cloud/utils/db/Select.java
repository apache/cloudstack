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

public class Select<S, T> {
    QueryBuilder<S,T> _builder;
    Class<T> _clazz;
    Attribute _attr;
    String _as;
    Field _field;
    
    protected Select(QueryBuilder<S, T> builder, Class<T> clazz, Attribute attr) {
        _builder = builder;
        _clazz = clazz;
        _attr = attr;
    }
    
    public QueryBuilder<S, T> into(String fieldName) {
        if (fieldName != null) {
            try {
                _field = _clazz.getDeclaredField(fieldName);
                _field.setAccessible(true);
            } catch (SecurityException e) {
                throw new RuntimeException("Unable to find " + fieldName + " in " + _clazz.getName(), e);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Unable to find " + fieldName + " in " + _clazz.getName(), e);
            }
        }
        return _builder;
    }
    
    public QueryBuilder<S, T> as(String as) {
        _as = as;
        return _builder;
    }
}
