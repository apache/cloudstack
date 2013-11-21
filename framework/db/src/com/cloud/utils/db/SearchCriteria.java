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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.utils.Pair;
import com.cloud.utils.db.SearchBase.Condition;
import com.cloud.utils.db.SearchBase.Select;

/**
 * big joins or high performance searches, it is much better to
 */
public class SearchCriteria<K> {
    public enum Op {
        GT(" > ? ", 1), GTEQ(" >= ? ", 1), LT(" < ? ", 1), LTEQ(" <= ? ", 1), EQ(" = ? ", 1), NEQ(" != ? ", 1), BETWEEN(" BETWEEN ? AND ? ", 2), NBETWEEN(
                " NOT BETWEEN ? AND ? ",
                2), IN(" IN () ", -1), NOTIN(" NOT IN () ", -1), LIKE(" LIKE ? ", 1), NLIKE(" NOT LIKE ? ", 1), NIN(" NOT IN () ", -1), NULL(" IS NULL ", 0), NNULL(
                " IS NOT NULL ",
                0), SC(" () ", 1), TEXT("  () ", 1), RP("", 0), AND(" AND ", 0), OR(" OR ", 0), NOT(" NOT ", 0);

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
        NATIVE("@", 1), MAX("MAX(@)", 1), MIN("MIN(@)", 1), FIRST("FIRST(@)", 1), LAST("LAST(@)", 1), SUM("SUM(@)", 1), COUNT("COUNT(@)", 1), DISTINCT("DISTINCT(@)", 1);

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
        Fields, Entity, Single, Result
    }

    private final Map<String, Attribute> _attrs;
    private final ArrayList<Condition> _conditions;
    private ArrayList<Condition> _additionals = null;
    private HashMap<String, Object[]> _params = new HashMap<String, Object[]>();
    private int _counter;
    private HashMap<String, JoinBuilder<SearchCriteria<?>>> _joins;
    private final ArrayList<Select> _selects;
    private final GroupBy<? extends SearchBase<?, ?, K>, ?, K> _groupBy;
    private final List<Object> _groupByValues;
    private final Class<K> _resultType;
    private final SelectType _selectType;

    protected SearchCriteria(SearchBase<?, ?, K> sb) {
        this._attrs = sb._attrs;
        this._conditions = sb._conditions;
        this._additionals = new ArrayList<Condition>();
        this._counter = 0;
        this._joins = null;
        if (sb._joins != null) {
            _joins = new HashMap<String, JoinBuilder<SearchCriteria<?>>>(sb._joins.size());
            for (Map.Entry<String, JoinBuilder<SearchBase<?, ?, ?>>> entry : sb._joins.entrySet()) {
                JoinBuilder<SearchBase<?, ?, ?>> value = entry.getValue();
                _joins.put(entry.getKey(),
                    new JoinBuilder<SearchCriteria<?>>(value.getT().create(), value.getFirstAttribute(), value.getSecondAttribute(), value.getType()));
            }
        }
        _selects = sb._selects;
        _groupBy = sb._groupBy;
        if (_groupBy != null) {
            _groupByValues = new ArrayList<Object>();
        } else {
            _groupByValues = null;
        }
        _resultType = sb._resultType;
        _selectType = sb._selectType;
    }

    protected void setParameters(HashMap<String, Object[]> parameters) {
        _params = parameters;
    }

    public SelectType getSelectType() {
        return _selectType;
    }

    public void getSelect(StringBuilder str, int insertAt) {
        if (_selects == null || _selects.size() == 0) {
            return;
        }

        for (Select select : _selects) {
            String func = select.func.toString() + ",";
            if (select.attr == null) {
                func = func.replace("@", "*");
            } else {
                func = func.replace("@", select.attr.table + "." + select.attr.columnName);
            }
            str.insert(insertAt, func);
            insertAt += func.length();
            if (select.field == null) {
                break;
            }
        }

        str.delete(insertAt - 1, insertAt);
    }

    public List<Field> getSelectFields() {
        List<Field> fields = new ArrayList<Field>(_selects.size());
        for (Select select : _selects) {
            fields.add(select.field);
        }

        return fields;
    }

    public void setParameters(String conditionName, Object... params) {
        assert _conditions.contains(new Condition(conditionName)) || _additionals.contains(new Condition(conditionName)) : "Couldn't find " + conditionName;
        _params.put(conditionName, params);
    }

    public boolean isSelectAll() {
        return _selects == null || _selects.size() == 0;
    }

    protected JoinBuilder<SearchCriteria<?>> findJoin(Map<String, JoinBuilder<SearchCriteria<?>>> jbmap, String joinName) {
        JoinBuilder<SearchCriteria<?>> jb = jbmap.get(joinName);
        if (jb != null) {
            return jb;
        }

        for (JoinBuilder<SearchCriteria<?>> j2 : jbmap.values()) {
            SearchCriteria<?> sc = j2.getT();
            if (sc._joins != null)
                jb = findJoin(sc._joins, joinName);
            if (jb != null) {
                return jb;
            }
        }

        assert (false) : "Unable to find a join by the name " + joinName;
        return null;
    }

    public void setJoinParameters(String joinName, String conditionName, Object... params) {
        JoinBuilder<SearchCriteria<?>> join = findJoin(_joins, joinName);
        assert (join != null) : "Incorrect join name specified: " + joinName;
        join.getT().setParameters(conditionName, params);

    }

    public SearchCriteria<?> getJoin(String joinName) {
        return _joins.get(joinName).getT();
    }

    public Pair<GroupBy<?, ?, ?>, List<Object>> getGroupBy() {
        return _groupBy == null ? null : new Pair<GroupBy<?, ?, ?>, List<Object>>(_groupBy, _groupByValues);
    }

    public void setGroupByValues(Object... values) {
        for (Object value : values) {
            _groupByValues.add(value);
        }
    }

    public Class<K> getResultType() {
        return _resultType;
    }

    @Deprecated
    public void addAnd(String field, Op op, Object... values) {
        String name = Integer.toString(_counter++);
        addCondition(name, " AND ", field, op);
        setParameters(name, values);
    }

    @Deprecated
    public void addAnd(Attribute attr, Op op, Object... values) {
        String name = Integer.toString(_counter++);
        addCondition(name, " AND ", attr, op);
        setParameters(name, values);
    }

    @Deprecated
    public void addOr(String field, Op op, Object... values) {
        String name = Integer.toString(_counter++);
        addCondition(name, " OR ", field, op);
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
            if (condition.isPreset()) {
                _params.put(condition.name, condition.presets);
            }
            Object[] params = _params.get(condition.name);
            if ((condition.op == null || condition.op.params == 0) || (params != null)) {
                condition.toSql(sql, params, i++);
            }
        }

        for (Condition condition : _additionals) {
            if (condition.isPreset()) {
                _params.put(condition.name, condition.presets);
            }
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
            if (condition.op != null && condition.op.params != 0 && objs != null) {
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

    public Collection<JoinBuilder<SearchCriteria<?>>> getJoins() {
        return _joins != null ? _joins.values() : null;
    }

    private void getParams(ArrayList<Pair<Attribute, Object>> params, Condition condition, Object[] objs) {
        if (condition.op == Op.SC) {
            assert (objs != null && objs.length > 0) : " Where's your search criteria object? " + condition.name;
            params.addAll(((SearchCriteria<?>)objs[0]).getValues());
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

    public Pair<String, ArrayList<Object>> toSql() {
        StringBuilder sql = new StringBuilder();

        return new Pair<String, ArrayList<Object>>(sql.toString(), null);
    }
}
