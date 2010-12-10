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

import java.util.ArrayList;
import java.util.List;

import com.cloud.utils.Pair;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;

public class GroupBy<T, R> {
    GenericSearchBuilder<T, R> _builder;
    List<Pair<Func, Attribute>> _groupBys;
    Having _having;
    
    public GroupBy(GenericSearchBuilder<T, R> builder) {
        _builder = builder;
        _groupBys = new ArrayList<Pair<Func, Attribute>>();
        _having = null;
        for (Attribute attr : _builder.getSpecifiedAttributes()) {
            _groupBys.add(new Pair<Func, Attribute>(null, attr));
        }
        _builder.getSpecifiedAttributes().clear();
    }
    
    public GroupBy<T, R> group(Object useless) {
        _groupBys.add(new Pair<Func, Attribute>(null, _builder.getSpecifiedAttributes().get(0)));
        _builder.getSpecifiedAttributes().clear();
        return this; 
    }
    
    public GroupBy<T, R> group(Func func, Object useless) {
        _groupBys.add(new Pair<Func, Attribute>(func, _builder.getSpecifiedAttributes().get(0)));
        _builder.getSpecifiedAttributes().clear();
        return this;
    }
    
    public GenericSearchBuilder<T, R> having(Func func, Object obj, Op op, Object value) {
        assert(_having == null) : "You can only specify one having in a group by";
        List<Attribute> attrs = _builder.getSpecifiedAttributes();
        assert attrs.size() == 1 : "You didn't specified an attribute";
        
        _having = new Having(func, attrs.get(0), op, value);
        _builder.getSpecifiedAttributes().clear();
        return _builder;
    }
    
    public void toSql(StringBuilder builder) {
        builder.append(" GROUP BY ");
        for (Pair<Func, Attribute> groupBy : _groupBys) {
            if (groupBy.first() != null) {
                String func = groupBy.first().toString();
                func.replaceFirst("@", groupBy.second().table + "." + groupBy.second().columnName);
                builder.append(func);
            } else {
                builder.append(groupBy.second().table + "." + groupBy.second().columnName);
            }
            
            builder.append(", ");
        }
        
        builder.delete(builder.length() - 2, builder.length());
        if (_having != null) {
            _having.toSql(builder);
        }
    }
    
    protected class Having {
        public Func func;
        public Attribute attr;
        public Op op;
        public Object value;
        
        public Having(Func func, Attribute attr, Op op, Object value) {
            this.func = func;
            this.attr = attr;
            this.op = op;
            this.value = value;
        }
        
        public void toSql(StringBuilder builder) {
            if (func != null) {
                String f = func.toString();
                f.replaceFirst("@", attr.toString());
                builder.append(func);
            } else {
                builder.append(attr.toString());
            }
            
            builder.append(op.toString());
        }
    }
}
