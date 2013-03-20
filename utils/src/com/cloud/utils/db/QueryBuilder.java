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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import com.cloud.utils.Pair;

public class QueryBuilder<S, T> implements MethodInterceptor, SimpleQueryBuilder<S>, SelectQueryBuilder<S, T>, JoinQueryBuilder<S, T> {
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
    
    protected HashMap<Class<?>, Pair<GenericDao<?,?>, Object>> _entities;
    protected ArrayList<Attribute> _specifiedAttrs = new ArrayList<Attribute>();
    protected T _resultSetClass;
    protected ArrayList<Select<S, T>> _selects;
    
    public QueryBuilder(Class<T> resultSetClass, Class<?>... clazzes) {
        _entities = new HashMap<Class<?>, Pair<GenericDao<?,?>, Object>>(clazzes.length);
        for (Class<?> clazz : clazzes) {
            GenericDao<?,?> dao = GenericDaoBase.getDao(clazz);
            Enhancer searchEnhancer = new Enhancer();
            searchEnhancer.setSuperclass(clazz);
            searchEnhancer.setCallback(this);
            Object entity = searchEnhancer.create();
            
            _entities.put(clazz, new Pair<GenericDao<?, ?>, Object>(dao, entity));
        }
    }
    
    protected void clean() {
        _specifiedAttrs = null;
        _entities = null;
    }
    
    /**
     * Constructor for SelectQueryBuilder interface.  Must specify the
     * table to be performing the query on and the result class to place it in.
     * @param entityClass entity class to do the query on.
     * @param resultSetClass result class to put the result set in.
     */
    public QueryBuilder(Class<S> entityClass, Class<T> resultSetClass) {
        _entities = new HashMap<Class<?>, Pair<GenericDao<?,?>, Object>>(1);
        GenericDao<?,?> dao = GenericDaoBase.getDao(entityClass);
        Enhancer searchEnhancer = new Enhancer();
        searchEnhancer.setSuperclass(entityClass);
        searchEnhancer.setCallback(this);
        Object entity = searchEnhancer.create();
        
        _entities.put(entityClass, new Pair<GenericDao<?, ?>, Object>(dao, entity));
    }
    
    @Override
    public SimpleQueryBuilder<S> selectFields(Object... fields) {
        assert _entities != null && _entities.size() == 1 : "Now you've done it....Stop casting interfaces on the QueryBuilder";
        assert _specifiedAttrs.size() > 0 : "You didn't specify any attributes";
   
        if (_selects == null) {
            _selects = new ArrayList<Select<S, T>>(fields.length);
        }
        
        for (Attribute attr : _specifiedAttrs) {
            _selects.add(new Select<S, T>(this, null, attr));
        }
        
        _specifiedAttrs.clear();
        
        return this;
    }
    
    protected void set(GenericDao<?, ?> dao , String name) {
        Attribute attr = dao.getAllAttributes().get(name);
        assert (attr != null) : "Searching for a field that's not there: " + name;
        _specifiedAttrs.add(attr);
    }
    
    
    @Override
    public Object intercept(Object entity, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        Class<?> entityClass = entity.getClass().getSuperclass();
        
        Pair<GenericDao<?,?>, Object> daoInfo = _entities.get(entityClass);
        assert (daoInfo != null) : "You need to specify " + entityClass + " as one of the entities in the Query";
        GenericDao<?,?> dao = daoInfo.first();
        
        String name = method.getName();
        if (name.startsWith("get")) {
            String fieldName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            set(dao, fieldName);
            return null;
        } else if (name.startsWith("is")) {
            String fieldName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
            set(dao, fieldName);
            return null;
        } else {
            assert false : "Perhaps you need to make the method start with get or is?";
        }
        return proxy.invokeSuper(entity, args);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <E> E entity(Class<E> clazz) {
        return (E)_entities.get(clazz).second();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public S entity() {
        return (S)_entities.values().iterator().next().second();
    }

    @Override
    public FirstWhere<S, T> where() {
        return new Where<S, T>(this);
    }

    @Override
    public SimpleQueryBuilder<S> selectAll() {
        return this;
    }
    
    public List<Attribute> getSpecifiedAttributes() {
        return _specifiedAttrs;
    }
    
    public Attribute getSpecifiedAttribute() {
        assert _specifiedAttrs.size() == 1 : "You can only specify one attribute";
        return _specifiedAttrs.get(0);
    }

    @Override
    public Select<S, T> selectColumn(Object column) {
        return null;
    }

    @Override
    public Select<S, T> selectField(Object column) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <J> On<S, J, T> innerJoin(Class<J> entityClazz) {
        // TODO Auto-generated method stub
        return null;
    }
}
