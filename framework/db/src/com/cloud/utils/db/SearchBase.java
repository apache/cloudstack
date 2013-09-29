// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.utils.db;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Transient;

import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria.SelectType;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * SearchBase contains the methods that are used to build up search
 * queries.  While this class is public it's not really meant for public
 * consumption.  Unfortunately, it has to be public for methods to be mocked.
 * 
 * @see GenericSearchBuilder
 * @see GenericQueryBuilder
 *
 * @param <J> Child class that inherited from SearchBase
 * @param <T> Entity Type to perform the searches on
 * @param <K> Type to place the search results.  This can be a native type,
 *            composite object, or the entity type itself.
 */
public abstract class SearchBase<J extends SearchBase<?, T, K>, T, K> {

    final Map<String, Attribute> _attrs;
    final Class<T> _entityBeanType;
    final Class<K> _resultType;
    final GenericDaoBase<? extends T, ? extends Serializable> _dao;

    final ArrayList<Condition> _conditions;
    final ArrayList<Attribute> _specifiedAttrs;

    protected HashMap<String, JoinBuilder<SearchBase<?, ?, ?>>> _joins;
    protected ArrayList<Select> _selects;
    protected GroupBy<J, T, K> _groupBy = null;
    protected SelectType _selectType;
    T _entity;

    SearchBase(Class<T> entityType, Class<K> resultType) {
        _dao = (GenericDaoBase<? extends T, ? extends Serializable>)GenericDaoBase.getDao(entityType);
        if (_dao == null) {
            throw new CloudRuntimeException("Unable to find DAO for " + entityType);
        }

        _entityBeanType = entityType;
        _resultType = resultType;
        _attrs = _dao.getAllAttributes();

        _entity = _dao.createSearchEntity(new Interceptor());
        _conditions = new ArrayList<Condition>();
        _joins = null;
        _specifiedAttrs = new ArrayList<Attribute>();
    }

    /**
     * Specifies how the search query should be grouped
     * 
     * @param fields fields of the entity object that should be grouped on.  The order is important.
     * @return GroupBy object to perform more operations on.
     * @see GroupBy
     */
    @SuppressWarnings("unchecked")
    public GroupBy<J, T, K> groupBy(Object... fields) {
        assert _groupBy == null : "Can't do more than one group bys";
        _groupBy = new GroupBy<J, T, K>((J)this);
        return _groupBy;
    }

    /**
     * Specifies what to select in the search.
     * 
     * @param fieldName The field name of the result object to put the value of the field selected.  This can be null if you're selecting only one field and the result is not a complex object.
     * @param func function to place.
     * @param field column to select.  Call this with this.entity() method.
     * @param params parameters to the function.
     * @return itself to build more search parts.
     */
    @SuppressWarnings("unchecked")
    public J select(String fieldName, Func func, Object field, Object... params) {
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

        Field declaredField = null;
        if (fieldName != null) {
            try {
                declaredField = _resultType.getDeclaredField(fieldName);
                declaredField.setAccessible(true);
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

        Select select = new Select(func, _specifiedAttrs.size() == 0 ? null : _specifiedAttrs.get(0), declaredField, params);
        _selects.add(select);

        _specifiedAttrs.clear();

        return (J)this;
    }

    /**
     * Select fields from the entity object to be selected in the search query.
     * 
     * @param fields fields from the entity object
     * @return itself
     */
    @SuppressWarnings("unchecked")
    public J selectFields(Object... fields) {
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

        return (J)this;
    }

    /**
     * joins this search with another search
     * 
     * @param name name given to the other search.  used for setJoinParameters.
     * @param builder The other search
     * @param joinField1 field of the first table used to perform the join
     * @param joinField2 field of the second table used to perform the join
     * @param joinType type of join
     * @return itself
     */
    @SuppressWarnings("unchecked")
    public J join(String name, SearchBase<?, ?, ?> builder, Object joinField1, Object joinField2, JoinBuilder.JoinType joinType) {
        assert _entity != null : "SearchBuilder cannot be modified once it has been setup";
        assert _specifiedAttrs.size() == 1 : "You didn't select the attribute.";
        assert builder._entity != null : "SearchBuilder cannot be modified once it has been setup";
        assert builder._specifiedAttrs.size() == 1 : "You didn't select the attribute.";
        assert builder != this : "You can't add yourself, can you?  Really think about it!";

        JoinBuilder<SearchBase<?, ?, ?>> t = new JoinBuilder<SearchBase<?, ?, ?>>(builder, _specifiedAttrs.get(0), builder._specifiedAttrs.get(0), joinType);
        if (_joins == null) {
            _joins = new HashMap<String, JoinBuilder<SearchBase<?, ?, ?>>>();
        }
        _joins.put(name, t);

        builder._specifiedAttrs.clear();
        _specifiedAttrs.clear();
        return (J)this;
    }

    public SelectType getSelectType() {
        return _selectType;
    }

    protected void set(String name) {
        Attribute attr = _attrs.get(name);
        assert (attr != null) : "Searching for a field that's not there: " + name;
        _specifiedAttrs.add(attr);
    }

    /**
     * @return entity object.  This allows the caller to use the entity return
     * to specify the field to be selected in many of the search parameters.
     */
    public T entity() {
        return _entity;
    }

    protected Attribute getSpecifiedAttribute() {
        if (_entity == null || _specifiedAttrs == null || _specifiedAttrs.size() != 1) {
            throw new RuntimeException("Now now, better specify an attribute or else we can't help you");
        }
        return _specifiedAttrs.get(0);
    }

    protected List<Attribute> getSpecifiedAttributes() {
        return _specifiedAttrs;
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
            finalize();
        }
        return new SearchCriteria<K>(this);
    }

    /**
     * Adds an OR condition to the search.  Normally you should use this to
     * perform an 'OR' with a big conditional in parenthesis.  For example,
     * 
     * search.or().op(entity.getId(), Op.Eq, "abc").cp()
     * 
     * The above fragment produces something similar to
     * 
     * "OR (id = $abc) where abc is the token to be replaced by a value later.
     * 
     * @return this
     */
    @SuppressWarnings("unchecked")
    public J or() {
        constructCondition(null, " OR ", null, null);
        return (J)this;
    }

    /**
     * Adds an AND condition to the search.  Normally you should use this to
     * perform an 'AND' with a big conditional in parenthesis.  For example,
     * 
     * search.and().op(entity.getId(), Op.Eq, "abc").cp()
     * 
     * The above fragment produces something similar to
     * 
     * "AND (id = $abc) where abc is the token to be replaced by a value later.
     * 
     * @return this
     */
    @SuppressWarnings("unchecked")
    public J and() {
        constructCondition(null, " AND ", null, null);
        return (J)this;
    }

    /**
     * Closes a parenthesis that's started by op()
     * @return this
     */
    @SuppressWarnings("unchecked")
    public J cp() {
        Condition condition = new Condition(null, " ) ", null, Op.RP);
        _conditions.add(condition);
        return (J)this;
    }

    /**
     * Writes an open parenthesis into the search
     * @return this
     */
    @SuppressWarnings("unchecked")
    public J op() {
        Condition condition = new Condition(null, " ( ", null, Op.RP);
        _conditions.add(condition);
        return (J)this;
    }

    /**
     * Marks the SearchBuilder as completed in building the search conditions.
     */
    @Override
    protected synchronized void finalize() {
        if (_entity != null) {
            Factory factory = (Factory)_entity;
            factory.setCallback(0, null);
            _entity = null;
        }

        if (_joins != null) {
            for (JoinBuilder<SearchBase<?, ?, ?>> join : _joins.values()) {
                join.getT().finalize();
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

    protected static class Condition {
        protected final String name;
        protected final String cond;
        protected final Op op;
        protected final Attribute attr;
        protected Object[] presets;

        protected Condition(String name) {
            this(name, null, null, null);
        }

        public Condition(String name, String cond, Attribute attr, Op op) {
            this.name = name;
            this.attr = attr;
            this.cond = cond;
            this.op = op;
            this.presets = null;
        }

        public boolean isPreset() {
            return presets != null;
        }

        public void setPresets(Object... presets) {
            this.presets = presets;
        }

        public Object[] getPresets() {
            return presets;
        }

        public void toSql(StringBuilder sql, Object[] params, int count) {
            if (count > 0) {
                sql.append(cond);
            }

            if (op == null) {
                return;
            }

            if (op == Op.SC) {
                sql.append(" (").append(((SearchCriteria<?>)params[0]).getWhereClause()).append(") ");
                return;
            }

            if (attr == null) {
                return;
            }

            sql.append(attr.table).append(".").append(attr.columnName).append(op.toString());
            if (op == Op.IN && params.length == 1) {
                sql.delete(sql.length() - op.toString().length(), sql.length());
                sql.append("=?");
            } else if (op == Op.NIN && params.length == 1) {
                sql.delete(sql.length() - op.toString().length(), sql.length());
                sql.append("!=?");
            } else if (op.getParams() == -1) {
                for (int i = 0; i < params.length; i++) {
                    sql.insert(sql.length() - 2, "?,");
                }
                sql.delete(sql.length() - 3, sql.length() - 2); // remove the last ,
            } else if (op == Op.EQ && (params == null || params.length == 0 || params[0] == null)) {
                sql.delete(sql.length() - 4, sql.length());
                sql.append(" IS NULL ");
            } else if (op == Op.NEQ && (params == null || params.length == 0 || params[0] == null)) {
                sql.delete(sql.length() - 5, sql.length());
                sql.append(" IS NOT NULL ");
            } else {
                if ((op.getParams() != 0 || params != null) && (params.length != op.getParams())) {
                    throw new RuntimeException("Problem with condition: " + name);
                }
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

    protected static class Select {
        public Func func;
        public Attribute attr;
        public Object[] params;
        public Field field;

        protected Select() {
        }

        public Select(Func func, Attribute attr, Field field, Object[] params) {
            this.func = func;
            this.attr = attr;
            this.params = params;
            this.field = field;
        }
    }

    protected class Interceptor implements MethodInterceptor {
        @Override
        public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            String name = method.getName();
            if (method.getAnnotation(Transient.class) == null) {
                if (name.startsWith("get")) {
                    String fieldName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    set(fieldName);
                    return null;
                } else if (name.startsWith("is")) {
                    String fieldName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
                    set(fieldName);
                    return null;
                } else {
                    Column ann = method.getAnnotation(Column.class);
                    if (ann != null) {
                        String colName = ann.name();
                        for (Map.Entry<String, Attribute> attr : _attrs.entrySet()) {
                            if (colName.equals(attr.getValue().columnName)) {
                                set(attr.getKey());
                                return null;
                            }
                        }
                    }
                    throw new RuntimeException("Perhaps you need to make the method start with get or is: " + method);
                }
            }
            return methodProxy.invokeSuper(object, args);
        }

    }
}