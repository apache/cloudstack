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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.SearchBuilder.Condition;

/**
 * SearchCriteria is a way for the caller to the dao code to do quick
 * searches without having to add methods to the dao code.  Obviously,
 * this should only be used when the search is not considered to be
 * performance critical or is limited in size.  If you need to do
 * big joins or high performance searches, it is much better to
 * add a specific method to the dao.
 */
public class SearchCriteria {
    public enum Op {
        GT(" > ? ", 1),
        GTEQ(" >= ? ", 1),
        LT(" < ? ", 1),
        LTEQ(" <= ? ", 1),
        EQ(" = ? ", 1),
        NEQ(" != ? ", 1),
        BETWEEN(" BETWEEN ? AND ? ", 2),
        NBETWEEN(" NOT BETWEEN ? AND ? ", 2),
        IN(" IN () ", -1),
        LIKE(" LIKE ? ", 1),
        NLIKE(" NOT LIKE ? ", 1),
        NIN(" NOT IN () ", -1),
        NULL(" IS NULL ", 0),
        NNULL(" IS NOT NULL ", 0),
        SC(" () ", 1),
        TEXT("  () ", 1),
        RP("", 0),
        AND(" AND ", 0),
        OR(" OR ", 0),
        NOT(" NOT ", 0);
        
        private final String op;
        int params;
        Op(String op, int params) {
            this.op = op;
            this.params = params;
        }
        
        @Override
		public String toString() {
            return op;
        }
        
        public int getParams() {
            return params;
        }
    }
    
    public enum Func {
        NATIVE("@", 1),
        MAX("MAX(@)", 1),
        MIN("MIN(@)", 1),
        FIRST("FIRST(@)", 1),
        LAST("LAST(@)", 1),
        SUM("SUM(@)", 1),
        COUNT("COUNT(@)", 1),
        DISTINCT("DISTINCT(@)", 1);
        
        private String func;
        private int count;
        
        Func(String func, int params) {
            this.func = func;
            this.count = params;
        }
        
        @Override
        public String toString() {
            return func;
        }
        
        public int getCount() {
            return count;
        }
    }
    
    public enum SelectType {
        Field,
        All,
        Function
    }

    private final Map<String, Attribute> _attrs;
    private final ArrayList<Condition> _conditions;
    private ArrayList<Condition> _additionals = null;
    private final HashMap<String, Object[]> _params = new HashMap<String, Object[]>();
    private int _counter;
    private HashMap<String, Ternary<SearchCriteria, Attribute, Attribute>> _joins;
    private final ArrayList<Pair<Func, Attribute[]>> _selects;
    private final ArrayList<Attribute> _groupBys;
    
    protected SearchCriteria(SearchBuilder<?> sb) {
    	this._attrs = sb._attrs;
        this._conditions = sb._conditions;
        this._additionals = new ArrayList<Condition>();
        this._counter = 0;
        this._joins = null;
        if (sb._joins != null) {
            _joins = new HashMap<String, Ternary<SearchCriteria, Attribute, Attribute>>(sb._joins.size());
            for (Map.Entry<String, Ternary<SearchBuilder<?>, Attribute, Attribute>> entry : sb._joins.entrySet()) {
                Ternary<SearchBuilder<?>, Attribute, Attribute> value = entry.getValue();
                _joins.put(entry.getKey(), new Ternary<SearchCriteria, Attribute, Attribute>(value.first().create(), value.second(), value.third()));
            }
        }
        _selects = sb._selects;
        _groupBys = sb._groupBys;
    }
    
    public SelectType getSelect(StringBuilder str, int insertAt) {
        if (_selects == null || _selects.size() == 0) {
            return SelectType.All;
        }
        
        boolean selectOnly = true;
        for (Pair<Func, Attribute[]> select : _selects) {
            String func = select.first().toString() + ",";
            if (select.second().length == 0) {
                func = func.replace("@", "*");
            } else {
                for (Attribute attr : select.second()) {
                    func = func.replaceFirst("@", attr.table + "." + attr.columnName);
                }
            }
            str.insert(insertAt, func);
            insertAt += func.length();
            if (select.first() != Func.NATIVE) {
                selectOnly = false;
            }
        }
        
        str.delete(insertAt - 1, insertAt);
        return selectOnly ? SelectType.Field : SelectType.Function;
    }
    
    public int getSelectCount() {
        return _selects == null ? 0 : _selects.size();
    }
    
    public void setParameters(String conditionName, Object... params) {
        assert _conditions.contains(new Condition(conditionName)) || _additionals.contains(new Condition(conditionName)) : "Couldn't find " + conditionName;
        _params.put(conditionName, params);
    }
    
    public boolean isSelectAll() {
        return _selects == null || _selects.size() == 0;
    }
    
    public void setJoinParameters(String joinName, String conditionName, Object... params) {
        Ternary<SearchCriteria, Attribute, Attribute> join = _joins.get(joinName);
        assert (join != null) : "Incorrect join name specified: " + joinName;
        join.first().setParameters(conditionName, params);
    }
    
    public void addJoinAnd(String joinName, String field, Op op, Object... values) {
        Ternary<SearchCriteria, Attribute, Attribute> join = _joins.get(joinName);
        assert (join != null) : "Incorrect join name specified: " + joinName;
        join.first().addAnd(field, op, values);
    }
    
    public void addJoinOr(String joinName, String field, Op op, Object... values) {
        Ternary<SearchCriteria, Attribute, Attribute> join = _joins.get(joinName);
        assert (join != null) : "Incorrect join name specified: " + joinName;
        join.first().addOr(field, op, values);
    }
    
    public SearchCriteria getJoin(String joinName) {
        return _joins.get(joinName).first();
    }
    
    public List<Attribute> getGroupBy() {
    	return _groupBys;
    }
    
    public void addAnd(String field, Op op, Object... values) {
        String name = Integer.toString(_counter++);
        addCondition(name, " AND ", field, op);
        setParameters(name, values);
    }
    
    public void addAnd(Attribute attr, Op op, Object... values) {
        String name = Integer.toString(_counter++);
        addCondition(name, " AND ", attr, op);
        setParameters(name, values);
    }
    
    public void addOr(String field, Op op, Object... values) {
        String name = Integer.toString(_counter++);
        addCondition(name, " OR ", field, op);
        setParameters(name, values);
    }
    
    public void addOr(Attribute attr, Op op, Object... values) {
        String name = Integer.toString(_counter++);
        addCondition(name, " OR ", attr, op);
        setParameters(name, values);
    }
    
    protected void addCondition(String conditionName, String cond, String fieldName, Op op) {
    	Attribute attr = _attrs.get(fieldName);
    	assert attr != null : "Unable to find field: " + fieldName;
    	addCondition(conditionName, cond, attr, op);
    }
    
    protected void addCondition(String conditionName, String cond, Attribute attr, Op op) {
        Condition condition = new Condition(conditionName, /*(_conditions.size() + _additionals.size()) == 0 ? "" : */cond, attr, op);
        _additionals.add(condition);
    }
    
    public String getWhereClause() {
        StringBuilder sql = new StringBuilder();
        int i = 0;
        for (Condition condition : _conditions) {
            Object[] params = _params.get(condition.name);
            if ((condition.op.params == 0) || (params != null)) {
                condition.toSql(sql, params, i++);
            }
        }
        
        for (Condition condition : _additionals) {
            Object[] params = _params.get(condition.name);
            if ((condition.op.params == 0) || (params != null)) {
                condition.toSql(sql, params, i++);
            }
        }
        
        return sql.toString();
    }
    
    public List<Pair<Attribute, Object>> getValues() {
        ArrayList<Pair<Attribute, Object>> params = new ArrayList<Pair<Attribute, Object>>(_params.size());
        for (Condition condition : _conditions) {
            Object[] objs = _params.get(condition.name);
            if ((condition.op.params == 0) || (objs != null)) {
                getParams(params, condition, objs);
            }
        }
        
        for (Condition condition : _additionals) {
            Object[] objs = _params.get(condition.name);
            if ((condition.op.params == 0) || (objs != null)) {
                getParams(params, condition, objs);
            }
        }
        
        return params;
    }
    
    public Collection<Ternary<SearchCriteria, Attribute, Attribute>> getJoins() {
        return _joins != null ? _joins.values() : null;
    }
    
    private void getParams(ArrayList<Pair<Attribute, Object>> params, Condition condition, Object[] objs) {
        //Object[] objs = _params.get(condition.name);
        if (condition.op == Op.SC) {
            assert (objs != null && objs.length > 0) : " Where's your search criteria object? " + condition.name;
            params.addAll(((SearchCriteria)objs[0]).getValues());
            return;
        }
        
        if (objs != null && objs.length > 0) {
            for (Object obj : objs) {
                if ((condition.op != Op.EQ && condition.op != Op.NEQ) || (obj != null)) {
                	params.add(new Pair<Attribute, Object>(condition.attr, obj));
                }
            }
        }
    }
}
