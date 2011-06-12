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

import com.cloud.utils.db.SearchCriteria.Op;

public class Condition<T, K> {
    Where<T, K> _where;
    Attribute _attr;
    String _as;
    SearchCriteria.Op _op;
    Object _compareTo;
    
    protected Condition(Where<T, K> where, Attribute attr, String as) {
        assert (where != null) : "What am I going to return to the user when Where is null?";
        assert (attr != null) : "What's the point of giving me a null attribute?";
        _where = where;
        _attr = attr;
        _as = as;
    }
    
    public Where<T, K> eq(String paramName) {
        _op = Op.EQ;
        _compareTo = paramName;
        return _where;
    }
    
    public Where<T, K> lt(String paramName) {
        _op = Op.LT;
        _compareTo = paramName;
        return _where;
    }
    
    public Where<T, K> lteq(String paramName) {
        _op = Op.LTEQ;
        _compareTo = paramName;
        return _where;
    }
    
    public Where<T, K> gt(String paramName) {
        _op = Op.GT;
        _compareTo = paramName;
        return _where;
    }
    
    public Where<T, K> isNull() {
        _op = Op.NULL;
        return _where;
    }
    
    public Where<T, K> isNotNull() {
        _op = Op.NNULL;
        return _where;
    }
    
    public Where<T, K> in(String paramName) {
        _op = Op.IN;
        _compareTo = paramName;
        return _where;
    }
    
    protected String getParamName() {
        assert (_compareTo instanceof String) : "Well, how can we get back a parameter name if it was not assigned one?";
        return (String)_compareTo;
    }
    
    @Override
    public boolean equals(Object obj) {
        return _compareTo.equals(obj);
    }
    
    @Override
    public int hashCode() {
        return _compareTo.hashCode();
    }
    
    @Override
    public String toString() {
        // FIXME: fix this
        return null;
    }
}
