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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;

/**
 * SearchBuilder is meant as a static query construct.  Often times in DAO code,
 * we write static sql that just assumes the database table does not change.
 * But if it does change, we're left with a bunch of strings that we have to
 * change by hand.  SearchBuilder is meant to replace that.  It provides load-time
 * checking for the fields within a VO object.
 * 
 * SearchBuilder is really meant to replace the static SQL strings and it should
 * be used as such.  The following is an example of how to use SearchBuilder.
 * You should not the following things.
 *   1. SearchBuilder is declared as final because it should not change after load time.
 *   2. search.entity().getHostId() allows you to declare which field you are searching
 *      on.  By doing this, we take advantage of the compiler to check that we
 *      are not searching on obsolete fields.  You should try to use this
 *      as much as possible as oppose to the addAndByField() and addOrByField() methods.
 *   3. Note that the same SearchBuilder is used to create multiple SearchCriteria.
 *      This is equivalent to clearing the parameters on PreparedStatement.  The
 *      multiple SearchCriteria does not interfere with each other.
 *   4. Note that the same field (getHostId()) was specified with two param
 *      names.  This is basically allowing you to reuse the same field in two
 *      parts of the search.
 * 
 * {@code
 * final SearchBuilder<UserVmVO> search = _userVmDao.createSearchBuilder();
 * final String param1 = "param1";
 * final String param2 = "param2";
 * search.addAnd(param1, search.entity().getHostId(), SearchCriteria.Op.NEQ);
 * search.addAnd(param2, search.entity().getHostId(), SearchCriteria.op.GT);
 * ...
 * SearchCriteria sc = search.create();
 * sc.setParameters(param1, 3);
 * sc.setParameters(param2, 1);
 * 
 * ...
 * 
 * SearchCriteria sc2 = search.create();
 * sc2.setParameters(param1, 4);
 * sc2.setParameters(param2, 1);
 * }
 * 
 * @param <T> VO object.
 */
public class SearchBuilder<T> implements MethodInterceptor {
    protected ArrayList<Condition> _conditions;
    protected Map<String, Attribute> _attrs;
    protected HashMap<String, Ternary<SearchBuilder<?>, Attribute, Attribute>> _joins;
    protected ArrayList<Pair<Func, Attribute[]>> _selects;
    protected ArrayList<Attribute> _groupBys;
    
    protected T _entity;
    protected ArrayList<Attribute> _specifiedAttrs;
    
    public SearchBuilder(T entity, Map<String, Attribute> attrs) {
    	_attrs = attrs;
    	_entity = entity;
    	_conditions = new ArrayList<Condition>();
    	_joins = null;
    	_specifiedAttrs = new ArrayList<Attribute>();
    }
    
    public T entity() {
    	return _entity;
    }

    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        String name = method.getName();
        if (name.startsWith("get")) {
            String fieldName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            set(fieldName);
        } else if (name.startsWith("is")) {
            String fieldName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
            set(fieldName);
        } else {
        	assert false : "Perhaps you need to make the method start with get or is?";
        }
        return methodProxy.invokeSuper(object, args);
    }
    
    protected void set(String name) {
        Attribute attr = _attrs.get(name);
        assert (attr != null) : "Searching for a field that's not there: " + name;
        _specifiedAttrs.add(attr);
    }
   
    /**
     * Adds an AND condition to the SearchBuilder.
     * 
     * @param name param name you will use later to set the values in this search condition.
     * @param useless SearchBuilder.entity().get*() which refers to the field that you're searching on.
     * @param op operation to apply to the field.
     * @return this
     */
    public SearchBuilder<T> and(String name, Object useless, Op op) {
        constructCondition(name, " AND ", _specifiedAttrs.get(0), op);
        return this;
    }
    
    public SearchBuilder<T> left(Op logic, String name, Object useless, Op op) {
        assert (logic == null || logic == Op.OR || logic == Op.AND) : "You can only use logic operators for open paren";
        constructCondition(name, (logic == null ? "" : logic.toString()) + " ( ", _specifiedAttrs.get(0), op);
        return this;
    }
    
    public SearchBuilder<T> selectField(Object useless) {
        return select(Func.NATIVE, useless);
    }
    
    public SearchBuilder<T> op(Op logic, String name, Object useless, Op op) {
        return left(logic, name, useless, op);
    }
    
    public SearchBuilder<T> openParen(Op logic, String name, Object useless, Op op) {
        return left(logic, name, useless, op);
    }
    
    public SearchBuilder<T> select(Func func, Object... useless) {
        assert _entity != null : "SearchBuilder cannot be modified once it has been setup";
        
        Pair<Func, Attribute[]> pair = new Pair<Func, Attribute[]>(func, _specifiedAttrs.toArray(new Attribute[_specifiedAttrs.size()]));
        if (_selects == null) {
            _selects = new ArrayList<Pair<Func, Attribute[]>>();
        }
        _selects.add(pair);
        
        _specifiedAttrs.clear();
        
        return this;
    }
    
    public SearchBuilder<T> groupBy(Object... useless) {
    	if(_groupBys == null) {
    		_groupBys = new ArrayList<Attribute>();
    	}
    	
    	Attribute[] attrs = _specifiedAttrs.toArray(new Attribute[_specifiedAttrs.size()]);
    	for(Attribute attr : attrs)
    		_groupBys.add(attr);
    	
        _specifiedAttrs.clear();
        return this;
    }
    
    /**
     * Adds an OR condition to the SearchBuilder.
     * 
     * @param name param name you will use later to set the values in this search condition.
     * @param useless SearchBuilder.entity().get*() which refers to the field that you're searching on.
     * @param op operation to apply to the field.
     * @return this
     */
    public SearchBuilder<T> or(String name, Object useless, Op op) {
    	constructCondition(name, " OR ", _specifiedAttrs.get(0), op);
        return this;
    }
    
    public SearchBuilder<T> join(String name, SearchBuilder<?> builder, Object useless, Object useless2) {
        assert _entity != null : "SearchBuilder cannot be modified once it has been setup";
        assert _specifiedAttrs.size() == 1 : "You didn't select the attribute.";
        assert builder._entity != null : "SearchBuilder cannot be modified once it has been setup";
        assert builder._specifiedAttrs.size() == 1 : "You didn't select the attribute.";
        assert builder != this : "You can't add yourself, can you?  Really think about it!";
        
        Ternary<SearchBuilder<?>, Attribute, Attribute> t = new Ternary<SearchBuilder<?>, Attribute, Attribute>(builder, _specifiedAttrs.get(0), builder._specifiedAttrs.get(0));
        if (_joins == null) {
            _joins = new HashMap<String, Ternary<SearchBuilder<?>, Attribute, Attribute>>();
        }
        _joins.put(name, t);
        
        builder._specifiedAttrs.clear();
        _specifiedAttrs.clear();
        return this;
    }
    
    protected void constructCondition(String conditionName, String cond, Attribute attr, Op op) {
        assert _entity != null : "SearchBuilder cannot be modified once it has been setup";
        assert _specifiedAttrs.size() == 1 : "You didn't select the attribute.";
        assert op != Op.SC : "Call join";
        
        Condition condition = new Condition(conditionName, cond, attr, op);
        _conditions.add(condition);
        _specifiedAttrs.clear();
    }

    /**
     * creates the SearchCriteria so the actual values can be filled in.
     * 
     * @return SearchCriteria
     */
    public SearchCriteria create() {
    	if (_entity != null) {
    		done();
    	}
        return new SearchCriteria(this);
    }
    
    public SearchCriteria create(String name, Object... values) {
        SearchCriteria sc = create();
        sc.setParameters(name, values);
        return sc;
    }
    
    public SearchBuilder<T> right() {
        Condition condition = new Condition("rp", " ) ", null, Op.RP);
        _conditions.add(condition);
        return this;
    }
    
    
    public SearchBuilder<T> cp() {
        return right();
    }
    
    public SearchBuilder<T> closeParen() {
        return right();
    }
    
    /**
     * Marks the SearchBuilder as completed in building the search conditions.
     */
    public synchronized void done() {
    	if (_entity != null) {
	    	Factory factory = (Factory)_entity;
	    	factory.setCallback(0, null);
	    	_entity = null;
    	}
    	if (_joins != null) {
    	    for (Ternary<SearchBuilder<?>, Attribute, Attribute> join : _joins.values()) {
    	        join.first().done();
    	    }
    	}
    }
    
    protected static class Condition {
        protected final String name;
        protected final String cond;
        protected final Op op;
        protected final Attribute attr;
        
        protected Condition(String name) {
            this(name, null, null, null);
        }
        
        public Condition(String name, String cond, Attribute attr, Op op) {
            this.name = name;
            this.attr = attr;
            this.cond = cond;
            this.op = op;
        }
        
        public void toSql(StringBuilder sql, Object[] params, int count) {
            if (count > 0) {
                sql.append(cond);
            }
            
            if (op == Op.SC) {
                sql.append(" (").append(((SearchCriteria)params[0]).getWhereClause()).append(") ");
                return;
            }
            
            if (attr == null) {
                return;
            }
            
            sql.append(attr.table).append(".").append(attr.columnName).append(op.toString());
            if (op.getParams() == -1) {
                for (int i = 0; i < params.length; i++) {
                    sql.insert(sql.length() - 2, "?,");
                }
                sql.delete(sql.length() - 3, sql.length() - 2); // remove the last ,
            } else if (op  == Op.EQ && (params == null || params.length == 0 || params[0] == null)) {
            	sql.delete(sql.length() - 4, sql.length());
            	sql.append(" IS NULL ");
            } else if (op == Op.NEQ && (params == null || params.length == 0 || params[0] == null)) {
            	sql.delete(sql.length() - 5, sql.length());
            	sql.append(" IS NOT NULL ");
            } else {
            	assert((op.getParams() == 0 && params == null) || (params.length == op.getParams())) : "Problem with condition: " + name;
            }
        }
        
        @Override
        public int hashCode() {
            return name.hashCode();
        }
        
        @Override
        public boolean equals(Object obj) {
        	if (!(obj instanceof Condition)) {
        		return false;
        	}
        	
            Condition condition = (Condition)obj;
            return name.equals(condition.name);
        }
    }
}
