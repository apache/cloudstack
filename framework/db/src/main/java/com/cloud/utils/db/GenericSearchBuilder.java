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

import java.util.UUID;

import com.cloud.utils.db.SearchCriteria.Op;

/**
 * GenericSearchBuilder is used to build a search based on a VO object.  It
 * can select the result into a native type, the entity object, or a composite
 * object depending on what's needed.
 *
 * The way to use GenericSearchBuilder is to use it to build a search at load
 * time so it should be declared at class constructions.  It allows queries to
 * be constructed completely in Java and parameters have String tokens that
 * can be replaced during runtime with SearchCriteria.  Because
 * GenericSearchBuilder is created at load time and SearchCriteria is used
 * at runtime, the search query creation and the parameter value setting are
 * separated in the code.  While that's tougher on the coder to maintain, what
 * you gain is that all string constructions are done at load time rather than
 * runtime and, more importantly, the proper construction can be checked when
 * components are being loaded.  However, if you prefer to just construct
 * the entire search at runtime, you can use GenericQueryBuilder.
 *
 * <code>
 * // To specify the GenericSearchBuilder, you should do this at load time.
 * // Note that in the following search, it selects a func COUNT to be the
 * // return result so for the second parameterized type is long.  It also
 * // presets the type in the search and declares created to be set during
 * // runtime.  Note the entity object itself must have came from search and
 * // it uses the getters of the object to retrieve the field used in the search.
 *
 * GenericSearchBuilder<HostVO, Long> CountSearch = _hostDao.createSearchBuilder(Long.class);
 * HostVO entity = CountSearch.entity();
 * CountSearch.select(null, FUNC.COUNT, null, null).where(entity.getType(), Op.EQ).value(Host.Type.Routing);
 * CountSearch.and(entity.getCreated(), Op.LT, "create_date").done();
 *
 * // Later in the code during runtime
 * SearchCriteria<Long> sc = CountSearch.create();
 * sc.setParameter("create_date", new Date());
 * Long count = _hostDao.customizedSearch(sc, null);
 * </code>
 *
 * @see GenericQueryBuilder for runtime construction of search query
 * @see SearchBuilder for returning VO objects itself
 *
 * @param <T> VO object this Search is build for.
 * @param <K> Result object that should contain the results.
 */
public class GenericSearchBuilder<T, K> extends SearchBase<GenericSearchBuilder<T, K>, T, K> {
    protected GenericSearchBuilder(Class<T> entityType, Class<K> resultType) {
        super(entityType, resultType);
    }

    /**
     * Adds an AND condition to the SearchBuilder.
     *
     * @param name param name you will use later to set the values in this search condition.
     * @param field SearchBuilder.entity().get*() which refers to the field that you're searching on.
     * @param op operation to apply to the field.
     * @return this
     */
    public GenericSearchBuilder<T, K> and(String name, Object field, Op op) {
        constructCondition(name, " AND ", _specifiedAttrs.get(0), op);
        return this;
    }

    /**
     * Adds an AND condition.  Some prefer this method because it looks like
     * the actual SQL query.
     *
     * @param field field of entity object
     * @param op operator of the search condition
     * @param name param name used to later to set parameter value
     * @return this
     */
    public GenericSearchBuilder<T, K> and(Object field, Op op, String name) {
        constructCondition(name, " AND ", _specifiedAttrs.get(0), op);
        return this;
    }

    /**
     * Adds an AND condition but allows for a preset value to be set for this conditio.
     *
     * @param field field of the entity object
     * @param op operator of the search condition
     * @return Preset which allows you to set the values
     */
    public Preset and(Object field, Op op) {
        Condition condition = constructCondition(UUID.randomUUID().toString(), " AND ", _specifiedAttrs.get(0), op);
        return new Preset(this, condition);
    }

    /**
     * Starts the search
     *
     * @param field field of the entity object
     * @param op operator
     * @param name param name to refer to the value later.
     * @return this
     */
    public GenericSearchBuilder<T, K> where(Object field, Op op, String name) {
        return and(name, field, op);
    }

    /**
     * Starts the search but the value is already set during construction.
     *
     * @param field field of the entity object
     * @param op operator of the search condition
     * @return Preset which allows you to set the values
     */
    public Preset where(Object field, Op op) {
        return and(field, op);
    }

    protected GenericSearchBuilder<T, K> left(Object field, Op op, String name) {
        constructCondition(name, " ( ", _specifiedAttrs.get(0), op);
        return this;
    }

    protected Preset left(Object field, Op op) {
        Condition condition = constructCondition(UUID.randomUUID().toString(), " ( ", _specifiedAttrs.get(0), op);
        return new Preset(this, condition);
    }

    /**
     * Adds an condition that starts with open parenthesis.  Use cp() to close
     * the parenthesis.
     *
     * @param field field of the entity object
     * @param op operator
     * @param name parameter name used to set the value later
     * @return this
     */
    public GenericSearchBuilder<T, K> op(Object field, Op op, String name) {
        return left(field, op, name);
    }

    public Preset op(Object field, Op op) {
        return left(field, op);
    }

    /**
     * Adds an condition that starts with open parenthesis.  Use cp() to close
     * the parenthesis.
     *
     * @param name parameter name used to set the parameter value later.
     * @param field field of the entity object
     * @param op operator
     * @return this
     */
    public GenericSearchBuilder<T, K> op(String name, Object field, Op op) {
        return left(field, op, name);
    }

    /**
     * Adds an OR condition to the SearchBuilder.
     *
     * @param name param name you will use later to set the values in this search condition.
     * @param field SearchBuilder.entity().get*() which refers to the field that you're searching on.
     * @param op operation to apply to the field.
     * @return this
     */
    public GenericSearchBuilder<T, K> or(String name, Object field, Op op) {
        constructCondition(name, " OR ", _specifiedAttrs.get(0), op);
        return this;
    }

    /**
     * Adds an OR condition
     *
     * @param field field of the entity object
     * @param op operator
     * @param name parameter name
     * @return this
     */
    public GenericSearchBuilder<T, K> or(Object field, Op op, String name) {
        constructCondition(name, " OR ", _specifiedAttrs.get(0), op);
        return this;
    }

    /**
     * Adds an OR condition but the values can be preset
     *
     * @param field field of the entity object
     * @param op operator
     * @return Preset
     */
    public Preset or(Object field, Op op) {
        Condition condition = constructCondition(UUID.randomUUID().toString(), " OR ", _specifiedAttrs.get(0), op);
        return new Preset(this, condition);
    }

    /**
     * Convenience method to create the search criteria and set a
     * parameter in the search.
     *
     * @param name parameter name set during construction
     * @param values values to be inserted for that parameter
     * @return SearchCriteria
     */
    public SearchCriteria<K> create(String name, Object... values) {
        SearchCriteria<K> sc = create();
        sc.setParameters(name, values);
        return sc;
    }

    /**
     * Marks the SearchBuilder as completed in building the search conditions.
     */
    public synchronized void done() {
        super.finalize();
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
