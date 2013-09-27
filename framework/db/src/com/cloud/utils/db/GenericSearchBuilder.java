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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import net.sf.cglib.proxy.Factory;

import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria.SelectType;

/**
 * GenericSearchBuilder is used to build a search based on a VO object
 * a convenience class provided called SearchBuilder that provides
 * exactly that functionality.
 *
 * @param <T> VO object this Search is build for.
 * @param <K> Result object that should contain the results.
 */
public class GenericSearchBuilder<T, K> extends SearchBase<T, K> {
    @SuppressWarnings("unchecked")
    protected GenericSearchBuilder(Class<T> entityType, Class<K> resultType) {
        super(entityType, resultType);
    }
    
    public T entity() {
        return _entity;
    }
    
    protected Attribute getSpecifiedAttribute() {
        if (_entity == null || _specifiedAttrs == null || _specifiedAttrs.size() != 1) {
            throw new RuntimeException("Now now, better specify an attribute or else we can't help you");
        }
        return _specifiedAttrs.get(0);
    }

    public GenericSearchBuilder<T, K> selectField(Object... useless) {
        if (_entity == null) {
            throw new RuntimeException("SearchBuilder cannot be modified once it has been setup");
        }
        if (_specifiedAttrs.size() <= 0) {
            throw new RuntimeException("You didn't specify any attributes");
        }
   
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
        
        return this;
    }
    
    /**
     * Specifies the field to select.
     * 
     * @param fieldName The field name of the result object to put the value of the field selected.  This can be null if you're selecting only one field and the result is not a complex object.
     * @param func function to place.
     * @param useless column to select.  Call this with this.entity() method.
     * @param params parameters to the function.
     * @return a SearchBuilder to build more search parts.
     */
    public GenericSearchBuilder<T, K> select(String fieldName, Func func, Object useless, Object... params) {
        if (_entity == null) {
            throw new RuntimeException("SearchBuilder cannot be modified once it has been setup");
        }
        if (_specifiedAttrs.size() > 1) {
            throw new RuntimeException("You can't specify more than one field to search on");
        }
        if (func.getCount() != -1 && (func.getCount() != (params.length + 1))) {
            throw new RuntimeException("The number of parameters does not match the function param count for " + func);
        }
        
        if (_selects == null) {
            _selects = new ArrayList<Select>();
        }
        
        Field field = null;
        if (fieldName != null) {
            try {
                field = _resultType.getDeclaredField(fieldName);
                field.setAccessible(true);
            } catch (SecurityException e) {
                throw new RuntimeException("Unable to find " + fieldName, e);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Unable to find " + fieldName, e);
            }
        } else {
            if (_selects.size() != 0) {
                throw new RuntimeException(
                        "You're selecting more than one item and yet is not providing a container class to put these items in.  So what do you expect me to do.  Spin magic?");
            }
        }
        
        Select select = new Select(func, _specifiedAttrs.size() == 0 ? null : _specifiedAttrs.get(0), field, params);
        _selects.add(select);
        
        _specifiedAttrs.clear();
        
        return this;
    }
    
    @Override
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
    public GenericSearchBuilder<T, K> and(String name, Object useless, Op op) {
        constructCondition(name, " AND ", _specifiedAttrs.get(0), op);
        return this;
    }
    
    public GenericSearchBuilder<T, K> and(Object useless, Op op, String name) {
        constructCondition(name, " AND ", _specifiedAttrs.get(0), op);
        return this;
    }

    public Preset and(Object useless, Op op) {
        Condition condition = constructCondition(UUID.randomUUID().toString(), " AND ", _specifiedAttrs.get(0), op);
        return new Preset(this, condition);
    }

    public GenericSearchBuilder<T, K> and() {
        constructCondition(null, " AND ", null, null);
        return this;
    }
    
    public GenericSearchBuilder<T, K> where() {
        return and();
    }
    
    public GenericSearchBuilder<T, K> or() {
        constructCondition(null, " OR ", null, null);
        return this;
    }
    
    public GenericSearchBuilder<T, K> where(String name, Object useless, Op op) {
        return and(name, useless, op);
    }
    
    public GenericSearchBuilder<T, K> where(Object useless, Op op, String name) {
        return and(name, useless, op);
    }

    public Preset where(Object useless, Op op) {
        return and(useless, op);
    }

    public GenericSearchBuilder<T, K> left(String name, Object useless, Op op) {
        constructCondition(name, " ( ", _specifiedAttrs.get(0), op);
        return this;
    }
    
    public GenericSearchBuilder<T, K> left(Object useless, Op op, String name) {
        constructCondition(name, " ( ", _specifiedAttrs.get(0), op);
        return this;
    }

    public Preset left(Object useless, Op op) {
        Condition condition = constructCondition(UUID.randomUUID().toString(), " ( ", _specifiedAttrs.get(0), op);
        return new Preset(this, condition);
    }

    public GenericSearchBuilder<T, K> op(Object useless, Op op, String name) {
        return left(useless, op, name);
    }

    public Preset op(Object useless, Op op) {
        return left(useless, op);
    }

    public GenericSearchBuilder<T, K> op(String name, Object useless, Op op) {
        return left(name, useless, op);
    }
    
    public GenericSearchBuilder<T, K> openParen(Object useless, Op op, String name) {
        return left(name, useless, op);
    }

    public GenericSearchBuilder<T, K> openParen(String name, Object useless, Op op) {
        return left(name, useless, op);
    }
    
    public Preset openParen(Object useless, Op op) {
        return left(useless, op);
    }

    public GroupBy<T, K> groupBy(Object... useless) {
        assert _groupBy == null : "Can't do more than one group bys";
        _groupBy = new GroupBy<T, K>(this);
        
        return _groupBy;
    }
    
    protected List<Attribute> getSpecifiedAttributes() {
        return _specifiedAttrs;
    }
    
    /**
     * Adds an OR condition to the SearchBuilder.
     * 
     * @param name param name you will use later to set the values in this search condition.
     * @param useless SearchBuilder.entity().get*() which refers to the field that you're searching on.
     * @param op operation to apply to the field.
     * @return this
     */
    public GenericSearchBuilder<T, K> or(String name, Object useless, Op op) {
        constructCondition(name, " OR ", _specifiedAttrs.get(0), op);
        return this;
    }
    
    public GenericSearchBuilder<T, K> or(Object useless, Op op, String name) {
        constructCondition(name, " OR ", _specifiedAttrs.get(0), op);
        return this;
    }

    public Preset or(Object useless, Op op) {
        Condition condition = constructCondition(UUID.randomUUID().toString(), " OR ", _specifiedAttrs.get(0), op);
        return new Preset(this, condition);
    }

    public GenericSearchBuilder<T, K> join(String name, GenericSearchBuilder<?, ?> builder, Object useless, Object useless2, JoinBuilder.JoinType joinType) {
        assert _entity != null : "SearchBuilder cannot be modified once it has been setup";
        assert _specifiedAttrs.size() == 1 : "You didn't select the attribute.";
        assert builder._entity != null : "SearchBuilder cannot be modified once it has been setup";
        assert builder._specifiedAttrs.size() == 1 : "You didn't select the attribute.";
        assert builder != this : "You can't add yourself, can you?  Really think about it!";
        
        JoinBuilder<GenericSearchBuilder<?, ?>> t = new JoinBuilder<GenericSearchBuilder<?, ?>>(builder, _specifiedAttrs.get(0), builder._specifiedAttrs.get(0), joinType);
        if (_joins == null) {
        	_joins = new HashMap<String, JoinBuilder<GenericSearchBuilder<?, ?>>>();
        }
        _joins.put(name, t);
        
        builder._specifiedAttrs.clear();
        _specifiedAttrs.clear();
        return this;
    }
    
    protected Condition constructCondition(String conditionName, String cond, Attribute attr, Op op) {
        assert _entity != null : "SearchBuilder cannot be modified once it has been setup";
        assert op == null || _specifiedAttrs.size() == 1 : "You didn't select the attribute.";
        assert op != Op.SC : "Call join";
        
        Condition condition = new Condition(conditionName, cond, attr, op);
        _conditions.add(condition);
        _specifiedAttrs.clear();
        return condition;
    }

    /**
     * creates the SearchCriteria so the actual values can be filled in.
     * 
     * @return SearchCriteria
     */
    public SearchCriteria<K> create() {
        if (_entity != null) {
            done();
        }
        return new SearchCriteria<K>(this);
    }
    
    public SearchCriteria<K> create(String name, Object... values) {
        SearchCriteria<K> sc = create();
        sc.setParameters(name, values);
        return sc;
    }
    
    public GenericSearchBuilder<T, K> right() {
        Condition condition = new Condition("rp", " ) ", null, Op.RP);
        _conditions.add(condition);
        return this;
    }
    
    public GenericSearchBuilder<T, K> cp() {
        return right();
    }
    
    public GenericSearchBuilder<T, K> closeParen() {
        return right();
    }
    
    public SelectType getSelectType() {
        return _selectType;
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
        	for (JoinBuilder<GenericSearchBuilder<?, ?>> join : _joins.values()) {
        		join.getT().done();
            }
        }
        
        if (_selects == null || _selects.size() == 0) {
            _selectType = SelectType.Entity;
            assert _entityBeanType.equals(_resultType) : "Expecting " + _entityBeanType + " because you didn't specify any selects but instead got " + _resultType;
            return;
        }
        
        for (Select select : _selects) {
            if (select.field == null) {
                assert (_selects.size() == 1) : "You didn't specify any fields to put the result in but you're specifying more than one select so where should I put the selects?";
                _selectType = SelectType.Single;
                return;
            }
            if (select.func != null) {
                _selectType = SelectType.Result;
                return;
            }
        }
        
        _selectType = SelectType.Fields;
    }
    
    public class Preset {
        GenericSearchBuilder<T, K> builder;
        Condition condition;

        protected Preset(GenericSearchBuilder<T, K> builder, Condition condition) {
            this.builder = builder;
            this.condition = condition;
        }

        public GenericSearchBuilder<T, K> values(Object... params) {
            if (condition.op.getParams() > 0 && condition.op.params != params.length) {
                throw new RuntimeException("The # of parameters set " + params.length + " does not match # of parameters required by " + condition.op);
            }
            condition.setPresets(params);
            return builder;
        }
    }
}
