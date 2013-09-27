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

public abstract class SearchBase<T, K> {

    final Map<String, Attribute> _attrs;
    final Class<T> _entityBeanType;
    final Class<K> _resultType;
    final GenericDaoBase<? extends T, ? extends Serializable> _dao;

    final ArrayList<Condition> _conditions;
    final ArrayList<Attribute> _specifiedAttrs;

    protected HashMap<String, JoinBuilder<SearchBase<?, ?>>> _joins;
    protected ArrayList<Select> _selects;
    protected GroupBy<? extends SearchBase<T, K>, T, K> _groupBy = null;
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

    public SelectType getSelectType() {
        return _selectType;
    }

    protected void set(String name) {
        Attribute attr = _attrs.get(name);
        assert (attr != null) : "Searching for a field that's not there: " + name;
        _specifiedAttrs.add(attr);
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
     * Marks the SearchBuilder as completed in building the search conditions.
     */
    @Override
    public synchronized void finalize() {
        if (_entity != null) {
            Factory factory = (Factory)_entity;
            factory.setCallback(0, null);
            _entity = null;
        }

        if (_joins != null) {
            for (JoinBuilder<SearchBase<?, ?>> join : _joins.values()) {
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