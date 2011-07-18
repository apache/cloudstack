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

import java.util.HashMap;

import com.cloud.utils.db.SearchCriteria.Op;

public class Condition<T, K> {
    Where<T, K> _where;
    Attribute _attr;
    String _as;
    SearchCriteria.Op _op;
    String _paramName;
    
    protected Condition(Where<T, K> where, Attribute attr, String as) {
        assert (where != null) : "What am I going to return to the user when Where is null?";
        assert (attr != null) : "What's the point of giving me a null attribute?";
        _where = where;
        _attr = attr;
        _as = as;
    }
    
    protected NextWhere<T, K> set(Op op, String paramName) {
        _op = op;
        _paramName = paramName;
        Where<T, K> where = _where;
        _where = null;
        return where;
    }
    
    public NextWhere<T, K> eq(String paramName) {
        return set(Op.EQ, paramName);
    }
    
    public NextWhere<T, K> lt(String paramName) {
        return set(Op.LT, paramName);
    }
    
    public NextWhere<T, K> lteq(String paramName) {
        return set(Op.LTEQ, paramName);
    }
    
    public NextWhere<T, K> gt(String paramName) {
        return set(Op.GT, paramName);
    }
    
    public NextWhere<T, K> isNull() {
        return set(Op.NULL, null);
    }
    
    public NextWhere<T, K> isNotNull() {
        return set(Op.NNULL, null);
    }
    
    public NextWhere<T, K> in(String paramName) {
        _op = Op.IN;
        _paramName = paramName;
        return _where;
    }
    
    protected String getParamName() {
        assert (_paramName instanceof String) : "Well, how can we get back a parameter name if it was not assigned one?";
        return _paramName;
    }
    
    @Override
    public boolean equals(Object obj) {
        return _paramName.equals(obj);
    }
    
    @Override
    public int hashCode() {
        return _paramName.hashCode();
    }
    
    public void toSql(StringBuilder builder, HashMap<String, Object[]> values) {
        if (_as != null) {
            builder.append(_as);
        } else {
            builder.append(_attr.table);
        }
        builder.append(".").append(_attr.columnName);
    }
   
}
