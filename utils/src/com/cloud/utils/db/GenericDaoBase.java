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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.ConfigurationException;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.EntityExistsException;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.NoOp;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;

import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.SearchCriteria.SelectType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;

/**
 *  GenericDaoBase is a simple way to implement DAOs.  It DOES NOT
 *  support the full EJB3 spec.  It borrows some of the annotations from
 *  the EJB3 spec to produce a set of SQLs so developers don't have to
 *  copy and paste the same code over and over again.  Of course,
 *  GenericDaoBase is completely at the mercy of the annotations you add
 *  to your entity bean.  If GenericDaoBase does not fit your needs, then
 *  don't extend from it.
 * 
 *  GenericDaoBase attempts to achieve the following:
 *    1. If you use _allFieldsStr in your SQL statement and use to() to convert
 *       the result to the entity bean, you don't ever have to worry about
 *       missing fields because its automatically taken from the entity bean's
 *       annotations.
 *    2. You don't have to rewrite the same insert and select query strings
 *       in all of your DAOs.
 *    3. You don't have to match the '?' (you know what I'm talking about) to
 *       the fields in the insert statement as that's taken care of for you.
 * 
 *  GenericDaoBase looks at the following annotations:
 *    1. Table - just name
 *    2. Column - just name
 *    3. GeneratedValue - any field with this annotation is not inserted.
 *    4. SequenceGenerator - sequence generator
 *    5. Id
 *    6. SecondaryTable
 * 
 *  Sometime later, I might look into injecting the SQLs as needed but right
 *  now we have to construct them at construction time.  The good thing is that
 *  the DAOs are suppose to be one per jvm so the time is all during the
 *  initial load.
 * 
 **/
@DB
public abstract class GenericDaoBase<T, ID extends Serializable> extends ComponentLifecycleBase implements GenericDao<T, ID> {
    private final static Logger s_logger = Logger.getLogger(GenericDaoBase.class);

    protected final static TimeZone s_gmtTimeZone = TimeZone.getTimeZone("GMT");

    protected final static Map<Class<?>, GenericDao<?, ? extends Serializable>> s_daoMaps = new ConcurrentHashMap<Class<?>, GenericDao<?, ? extends Serializable>>(71);

    protected Class<T> _entityBeanType;
    protected String _table;

    protected String _tables;

    protected Field[] _embeddedFields;

    // This is private on purpose.  Everyone should use createPartialSelectSql()
    private Pair<StringBuilder, Attribute[]> _partialSelectSql;
    private Pair<StringBuilder, Attribute[]> _partialQueryCacheSelectSql;
    protected StringBuilder _discriminatorClause;
    protected Map<String, Object> _discriminatorValues;
    protected String _selectByIdSql;
    protected String _count;

    protected Field _idField;

    protected List<Pair<String, Attribute[]>> _insertSqls;
    protected Pair<String, Attribute> _removed;
    protected Pair<String, Attribute[]> _removeSql;
    protected List<Pair<String, Attribute[]>> _deleteSqls;
    protected Map<String, Attribute[]> _idAttributes;
    protected Map<String, TableGenerator> _tgs;
    protected Map<String, Attribute> _allAttributes;
    protected List<Attribute> _ecAttributes;
    protected Map<Pair<String, String>, Attribute> _allColumns;
    protected Enhancer _enhancer;
    protected Factory _factory;
    protected Enhancer _searchEnhancer;
    protected int _timeoutSeconds;

    protected final static CallbackFilter s_callbackFilter = new UpdateFilter();

    protected static final String FOR_UPDATE_CLAUSE = " FOR UPDATE ";
    protected static final String SHARE_MODE_CLAUSE = " LOCK IN SHARE MODE";
    protected static final String SELECT_LAST_INSERT_ID_SQL = "SELECT LAST_INSERT_ID()";

    protected static final SequenceFetcher s_seqFetcher = SequenceFetcher.getInstance();

    public static <J> GenericDao<? extends J, ? extends Serializable> getDao(Class<J> entityType) {
        @SuppressWarnings("unchecked")
        GenericDao<? extends J, ? extends Serializable> dao = (GenericDao<? extends J, ? extends Serializable>)s_daoMaps.get(entityType);
        assert dao != null : "Unable to find DAO for " + entityType + ".  Are you sure you waited for the DAO to be initialized before asking for it?";
        return dao;
    }

    @Override
    @SuppressWarnings("unchecked") @DB(txn=false)
    public <J> GenericSearchBuilder<T, J> createSearchBuilder(Class<J> resultType) {
        final T entity = (T)_searchEnhancer.create();
        final Factory factory = (Factory)entity;
        GenericSearchBuilder<T, J> builder = new GenericSearchBuilder<T, J>(entity, resultType, _allAttributes);
        factory.setCallback(0, builder);
        return builder;
    }

    public Map<String, Attribute> getAllAttributes() {
        return _allAttributes;
    }
    
    @SuppressWarnings("unchecked")
    protected GenericDaoBase() {
        Type t = getClass().getGenericSuperclass();
        if (t instanceof ParameterizedType) {
            _entityBeanType = (Class<T>)((ParameterizedType)t).getActualTypeArguments()[0];
        } else if (((Class<?>)t).getGenericSuperclass() instanceof ParameterizedType) {
            _entityBeanType = (Class<T>)((ParameterizedType)((Class<?>)t).getGenericSuperclass()).getActualTypeArguments()[0];
        } else {
            _entityBeanType = (Class<T>)((ParameterizedType)
                    ( (Class<?>)((Class<?>)t).getGenericSuperclass()).getGenericSuperclass()).getActualTypeArguments()[0];
        }

/*        
        s_daoMaps.put(_entityBeanType, ComponentContext.getComponent(this.getClass()));
        Class<?>[] interphaces = _entityBeanType.getInterfaces();
        if (interphaces != null) {
            for (Class<?> interphace : interphaces) {
                s_daoMaps.put(interphace, ComponentContext.getComponent(this.getClass()));
            }
        }
*/  
        _table = DbUtil.getTableName(_entityBeanType);

        final SqlGenerator generator = new SqlGenerator(_entityBeanType);
        _partialSelectSql = generator.buildSelectSql(false);
        _count = generator.buildCountSql();
        _partialQueryCacheSelectSql = generator.buildSelectSql(true);
        _embeddedFields = generator.getEmbeddedFields();
        _insertSqls = generator.buildInsertSqls();
        final Pair<StringBuilder, Map<String, Object>> dc = generator.buildDiscriminatorClause();
        _discriminatorClause = dc.first().length() == 0 ? null : dc.first();
        _discriminatorValues = dc.second();

        _idAttributes = generator.getIdAttributes();
        _idField = _idAttributes.get(_table).length > 0 ? _idAttributes.get(_table)[0].field : null;

        _tables = generator.buildTableReferences();

        _allAttributes = generator.getAllAttributes();
        _allColumns = generator.getAllColumns();

        _selectByIdSql = buildSelectByIdSql(createPartialSelectSql(null, true));
        _removeSql = generator.buildRemoveSql();
        _deleteSqls = generator.buildDeleteSqls();
        _removed = generator.getRemovedAttribute();
        _tgs = generator.getTableGenerators();
        _ecAttributes = generator.getElementCollectionAttributes();

        TableGenerator tg = this.getClass().getAnnotation(TableGenerator.class);
        if (tg != null) {
            _tgs.put(tg.name(), tg);
        }
        tg = this.getClass().getSuperclass().getAnnotation(TableGenerator.class);
        if (tg != null) {
            _tgs.put(tg.name(), tg);
        }

        Callback[] callbacks = new Callback[] { NoOp.INSTANCE, new UpdateBuilder(this) };

        _enhancer = new Enhancer();
        _enhancer.setSuperclass(_entityBeanType);
        _enhancer.setCallbackFilter(s_callbackFilter);
        _enhancer.setCallbacks(callbacks);
        _factory = (Factory)_enhancer.create();

        _searchEnhancer = new Enhancer();
        _searchEnhancer.setSuperclass(_entityBeanType);
        _searchEnhancer.setCallback(new UpdateBuilder(this));

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Select SQL: " + _partialSelectSql.first().toString());
            s_logger.trace("Remove SQL: " + (_removeSql != null ? _removeSql.first() : "No remove sql"));
            s_logger.trace("Select by Id SQL: " + _selectByIdSql);
            s_logger.trace("Table References: " + _tables);
            s_logger.trace("Insert SQLs:");
            for (final Pair<String, Attribute[]> insertSql : _insertSqls) {
                s_logger.trace(insertSql.first());
            }

            s_logger.trace("Delete SQLs");
            for (final Pair<String, Attribute[]> deletSql : _deleteSqls) {
                s_logger.trace(deletSql.first());
            }

            s_logger.trace("Collection SQLs");
            for (Attribute attr : _ecAttributes) {
                EcInfo info = (EcInfo)attr.attache;
                s_logger.trace(info.insertSql);
                s_logger.trace(info.selectSql);
            }
        }
        
        setRunLevel(ComponentLifecycle.RUN_LEVEL_SYSTEM);
    }

    @Override @DB(txn=false)
    @SuppressWarnings("unchecked")
    public T createForUpdate(final ID id) {
        final T entity = (T)_factory.newInstance(new Callback[] {NoOp.INSTANCE, new UpdateBuilder(this)});
        if (id != null) {
            try {
                _idField.set(entity, id);
            } catch (final IllegalArgumentException e) {
            } catch (final IllegalAccessException e) {
            }
        }
        return entity;
    }

    @Override @DB(txn=false)
    public T createForUpdate() {
        return createForUpdate(null);
    }

    @Override @DB(txn=false)
    public <K> K getNextInSequence(final Class<K> clazz, final String name) {
        final TableGenerator tg = _tgs.get(name);
        assert (tg != null) : "Couldn't find Table generator using " + name;

        return s_seqFetcher.getNextSequence(clazz, tg);
    }

    @Override @DB(txn=false)
    public <K> K getRandomlyIncreasingNextInSequence(final Class<K> clazz, final String name) {
        final TableGenerator tg = _tgs.get(name);
        assert (tg != null) : "Couldn't find Table generator using " + name;

        return s_seqFetcher.getRandomNextSequence(clazz, tg);
    }

    @Override @DB(txn=false)
    public List<T> lockRows(final SearchCriteria<T> sc, final Filter filter, final boolean exclusive) {
        return search(sc, filter, exclusive, false);
    }

    @Override @DB(txn=false)
    public T lockOneRandomRow(final SearchCriteria<T> sc, final boolean exclusive) {
        final Filter filter = new Filter(1);
        final List<T> beans = search(sc, filter, exclusive, true);
        return beans.isEmpty() ? null : beans.get(0);
    }

    @DB(txn=false)
    protected List<T> search(SearchCriteria<T> sc, final Filter filter, final Boolean lock, final boolean cache) {
        if (_removed != null) {
            if (sc == null) {
                sc = createSearchCriteria();
            }
            sc.addAnd(_removed.second().field.getName(), SearchCriteria.Op.NULL);
        }
        return searchIncludingRemoved(sc, filter, lock, cache);
    }

    @DB(txn=false)
    protected List<T> search(SearchCriteria<T> sc, final Filter filter, final Boolean lock, final boolean cache, final boolean enable_query_cache) {
        if (_removed != null) {
            if (sc == null) {
                sc = createSearchCriteria();
            }
            sc.addAnd(_removed.second().field.getName(), SearchCriteria.Op.NULL);
        }
        return searchIncludingRemoved(sc, filter, lock, cache, enable_query_cache);
    }

    @Override
    public List<T> searchIncludingRemoved(SearchCriteria<T> sc, final Filter filter, final Boolean lock, final boolean cache) {
        return searchIncludingRemoved(sc, filter, lock, cache, false) ;
    }

    @Override
    public List<T> searchIncludingRemoved(SearchCriteria<T> sc, final Filter filter, final Boolean lock, 
            final boolean cache, final boolean enable_query_cache) {
        String clause = sc != null ? sc.getWhereClause() : null;
        if (clause != null && clause.length() == 0) {
            clause = null;
        }

        final StringBuilder str = createPartialSelectSql(sc, clause != null, enable_query_cache);
        if (clause != null) {
            str.append(clause);
        }

        Collection<JoinBuilder<SearchCriteria<?>>> joins = null;
        if (sc != null) {
            joins = sc.getJoins();
            if (joins != null) {
                addJoins(str, joins);
            }
        }

        List<Object> groupByValues = addGroupBy(str, sc);
        addFilter(str, filter);

        final Transaction txn = Transaction.currentTxn();
        if (lock != null) {
            assert (txn.dbTxnStarted() == true) : "As nice as I can here now....how do you lock when there's no DB transaction?  Review your db 101 course from college.";
            str.append(lock ? FOR_UPDATE_CLAUSE : SHARE_MODE_CLAUSE);
        }

        final String sql = str.toString();

        PreparedStatement pstmt = null;
        final List<T> result = new ArrayList<T>();
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            int i = 0;
            if (clause != null) {
                for (final Pair<Attribute, Object> value : sc.getValues()) {
                    prepareAttribute(++i, pstmt, value.first(), value.second());
                }
            }

            if (joins != null) {
                i = addJoinAttributes(i, pstmt, joins);
            }

            if (groupByValues != null) {
                for (Object value : groupByValues) {
                    pstmt.setObject(i++, value);
                }
            }

            if (s_logger.isDebugEnabled() && lock != null) {
                txn.registerLock(pstmt.toString());
            }
            final ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(toEntityBean(rs, cache));
            }
            return result;
        } catch (final SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + pstmt, e);
        } catch (final Throwable e) {
            throw new CloudRuntimeException("Caught: " + pstmt, e);
        }
    }

    @Override @SuppressWarnings("unchecked")
    public <M> List<M> customSearchIncludingRemoved(SearchCriteria<M> sc, final Filter filter) {
        String clause = sc != null ? sc.getWhereClause() : null;
        if (clause != null && clause.length() == 0) {
            clause = null;
        }

        final StringBuilder str = createPartialSelectSql(sc, clause != null);
        if (clause != null) {
            str.append(clause);
        }

        Collection<JoinBuilder<SearchCriteria<?>>> joins = null;
        if (sc != null) {
            joins = sc.getJoins();
            if (joins != null) {
                addJoins(str, joins);
            }
        }

        List<Object> groupByValues = addGroupBy(str, sc);
        addFilter(str, filter);

        final String sql = str.toString();

        final Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            int i = 0;
            if (clause != null) {
                for (final Pair<Attribute, Object> value : sc.getValues()) {
                    prepareAttribute(++i, pstmt, value.first(), value.second());
                }
            }

            if (joins != null) {
                i = addJoinAttributes(i, pstmt, joins);
            }

            if (groupByValues != null) {
                for (Object value : groupByValues) {
                    pstmt.setObject(i++, value);
                }
            }

            ResultSet rs = pstmt.executeQuery();
            SelectType st = sc.getSelectType();
            ArrayList<M> results = new ArrayList<M>();
            List<Field> fields = sc.getSelectFields();
            while (rs.next()) {
                if (st == SelectType.Entity) {
                    results.add((M)toEntityBean(rs, false));
                } else if (st == SelectType.Fields || st == SelectType.Result) {
                    M m = sc.getResultType().newInstance();
                    for (int j = 1; j <= fields.size(); j++) {
                        setField(m, fields.get(j - 1), rs, j);
                    }
                    results.add(m);
                } else if (st == SelectType.Single) {
                    results.add(getObject(sc.getResultType(), rs, 1));
                }
            }

            return results;
        } catch (final SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + pstmt, e);
        } catch (final Throwable e) {
            throw new CloudRuntimeException("Caught: " + pstmt, e);
        }
    }

    @Override @DB(txn=false)
    public <M> List<M> customSearch(SearchCriteria<M> sc, final Filter filter) {
        if (_removed != null) {
            sc.addAnd(_removed.second().field.getName(), SearchCriteria.Op.NULL);
        }

        return customSearchIncludingRemoved(sc, filter);
    }

    @DB(txn=false)
    protected void setField(Object entity, Field field, ResultSet rs, int index) throws SQLException {
        try {
            final Class<?> type = field.getType();
            if (type == String.class) {
                byte[] bytes = rs.getBytes(index);
                if(bytes != null) {
                    try {
                        Encrypt encrypt = field.getAnnotation(Encrypt.class);
                        if (encrypt != null && encrypt.encrypt()){
                            field.set(entity, DBEncryptionUtil.decrypt(new String(bytes, "UTF-8")));
                        } else {
                            field.set(entity, new String(bytes, "UTF-8"));
                        }
                    } catch (IllegalArgumentException e) {
                        assert(false);
                        throw new CloudRuntimeException("IllegalArgumentException when converting UTF-8 data");
                    } catch (UnsupportedEncodingException e) {
                        assert(false);
                        throw new CloudRuntimeException("UnsupportedEncodingException when converting UTF-8 data");
                    }
                } else {
                    field.set(entity, null);
                }
            } else if (type == long.class) {
                field.setLong(entity, rs.getLong(index));
            } else if (type == Long.class) {
                if (rs.getObject(index) == null) {
                    field.set(entity, null);
                } else {
                    field.set(entity, rs.getLong(index));
                }
            } else if (type.isEnum()) {
                final Enumerated enumerated = field.getAnnotation(Enumerated.class);
                final EnumType enumType = (enumerated == null) ? EnumType.STRING : enumerated.value();

                final Enum<?>[] enums =  (Enum<?>[])field.getType().getEnumConstants();
                for (final Enum<?> e : enums) {
                    if ((enumType == EnumType.STRING && e.name().equalsIgnoreCase(rs.getString(index))) ||
                            (enumType == EnumType.ORDINAL && e.ordinal() == rs.getInt(index))) {
                        field.set(entity, e);
                        return;
                    }
                }
            } else if (type == int.class) {
                field.set(entity, rs.getInt(index));
            } else if (type == Integer.class) {
                if (rs.getObject(index) == null) {
                    field.set(entity, null);
                } else {
                    field.set(entity, rs.getInt(index));
                }
            } else if (type == Date.class) {
                final Object data = rs.getDate(index);
                if (data == null) {
                    field.set(entity, null);
                    return;
                }
                field.set(entity, DateUtil.parseDateString(s_gmtTimeZone, rs.getString(index)));
            } else if (type == Calendar.class) {
                final Object data = rs.getDate(index);
                if (data == null) {
                    field.set(entity, null);
                    return;
                }
                final Calendar cal = Calendar.getInstance();
                cal.setTime(DateUtil.parseDateString(s_gmtTimeZone, rs.getString(index)));
                field.set(entity, cal);
            } else if (type == boolean.class) {
                field.setBoolean(entity, rs.getBoolean(index));
            } else if (type == Boolean.class) {
                if (rs.getObject(index) == null) {
                    field.set(entity, null);
                } else {
                    field.set(entity, rs.getBoolean(index));
                }
            } else if (type == URI.class) {
                try {
                    String str = rs.getString(index);
                    field.set(entity, str == null ? null : new URI(str));
                } catch (URISyntaxException e) {
                    throw new CloudRuntimeException("Invalid URI: " + rs.getString(index), e);
                }
            } else if (type == URL.class) {
                try {
                    String str = rs.getString(index);
                    field.set(entity, str != null ? new URL(str) : null);
                } catch (MalformedURLException e) {
                    throw new CloudRuntimeException("Invalid URL: " + rs.getString(index), e);
                }
            } else if (type == Ip.class) {
                final Enumerated enumerated = field.getAnnotation(Enumerated.class);
                final EnumType enumType = (enumerated == null) ? EnumType.STRING : enumerated.value();

                Ip ip = null;
                if (enumType == EnumType.STRING) {
                    String s = rs.getString(index);
                    ip = s == null ? null : new Ip(NetUtils.ip2Long(s));
                } else {
                    ip = new Ip(rs.getLong(index));
                }
                field.set(entity, ip);
            } else if (type == short.class) {
                field.setShort(entity, rs.getShort(index));
            } else if (type == Short.class) {
                if (rs.getObject(index) == null) {
                    field.set(entity, null);
                } else {
                    field.set(entity, rs.getShort(index));
                }
            } else if (type == float.class) {
                field.setFloat(entity, rs.getFloat(index));
            } else if (type == Float.class) {
                if (rs.getObject(index) == null) {
                    field.set(entity, null);
                } else {
                    field.set(entity, rs.getFloat(index));
                }
            } else if (type == double.class) {
                field.setDouble(entity, rs.getDouble(index));
            } else if (type == Double.class) {
                if (rs.getObject(index) == null) {
                    field.set(entity, null);
                } else {
                    field.set(entity, rs.getDouble(index));
                }
            } else if (type == byte.class) {
                field.setByte(entity, rs.getByte(index));
            } else if (type == Byte.class) {
                if (rs.getObject(index) == null) {
                    field.set(entity, null);
                } else {
                    field.set(entity, rs.getByte(index));
                }
            } else if (type == byte[].class) {
                field.set(entity, rs.getBytes(index));
            } else {
                field.set(entity, rs.getObject(index));
            }
        } catch (final IllegalAccessException e) {
            throw new CloudRuntimeException("Yikes! ", e);
        }
    }

    @DB(txn=false) @SuppressWarnings("unchecked")
    protected <M> M getObject(Class<M> type, ResultSet rs, int index) throws SQLException {
        if (type == String.class) {
            byte[] bytes = rs.getBytes(index);
            if(bytes != null) {
                try {
                    return (M)new String(bytes, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new CloudRuntimeException("UnsupportedEncodingException exception while converting UTF-8 data");
                }
            } else {
                return null;
            }
        } else if (type == int.class) {
            return (M)new Integer(rs.getInt(index));
        } else if (type == Integer.class) {
            if (rs.getObject(index) == null) {
                return null;
            } else {
                return (M)new Integer(rs.getInt(index));
            }
        } else if (type == long.class) {
            return (M)new Long(rs.getLong(index));
        } else if (type == Long.class) {
            if (rs.getObject(index) == null) {
                return null;
            } else {
                return (M)new Long(rs.getLong(index));
            }
        } else if (type == Date.class) {
            final Object data = rs.getDate(index);
            if (data == null) {
                return null;
            } else {
                return (M)DateUtil.parseDateString(s_gmtTimeZone, rs.getString(index));
            }
        } else if (type == short.class) {
            return (M)new Short(rs.getShort(index));
        } else if (type == Short.class) {
            if (rs.getObject(index) == null) {
                return null;
            } else {
                return (M)new Short(rs.getShort(index));
            }
        } else if (type == boolean.class) {
            return (M)new Boolean(rs.getBoolean(index));
        } else if (type == Boolean.class) {
            if (rs.getObject(index) == null) {
                return null;
            } else {
                return (M)new Boolean(rs.getBoolean(index));
            }
        } else if (type == float.class) {
            return (M)new Float(rs.getFloat(index));
        } else if (type == Float.class) {
            if (rs.getObject(index) == null) {
                return null;
            } else {
                return (M)new Float(rs.getFloat(index));
            }
        } else if (type == double.class) {
            return (M)new Double(rs.getDouble(index));
        } else if (type == Double.class) {
            if (rs.getObject(index) == null) {
                return null;
            } else {
                return (M)new Double(rs.getDouble(index));
            }
        } else if (type == byte.class) {
            return (M)new Byte(rs.getByte(index));
        } else if (type == Byte.class) {
            if (rs.getObject(index) == null) {
                return null;
            } else {
                return (M)new Byte(rs.getByte(index));
            }
        } else if (type == Calendar.class) {
            final Object data = rs.getDate(index);
            if (data == null) {
                return null;
            } else {
                final Calendar cal = Calendar.getInstance();
                cal.setTime(DateUtil.parseDateString(s_gmtTimeZone, rs.getString(index)));
                return (M)cal;
            }
        } else if (type == byte[].class) {
            return (M)rs.getBytes(index);
        } else {
            return (M)rs.getObject(index);
        }
    }

    @DB(txn=false)
    protected int addJoinAttributes(int count, PreparedStatement pstmt, Collection<JoinBuilder<SearchCriteria<?>>> joins) throws SQLException {
        for (JoinBuilder<SearchCriteria<?>> join : joins) {
            for (final Pair<Attribute, Object> value : join.getT().getValues()) {
                prepareAttribute(++count, pstmt, value.first(), value.second());
            }
        }

        for (JoinBuilder<SearchCriteria<?>> join : joins) {
            if (join.getT().getJoins() != null) {
                count = addJoinAttributes(count, pstmt, join.getT().getJoins());
            }
        }

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("join search statement is " + pstmt);
        }
        return count;
    }

    protected int update(ID id, UpdateBuilder ub, T entity) {
        if (_cache != null) {
            _cache.remove(id);
        }
        SearchCriteria<T> sc = createSearchCriteria();
        sc.addAnd(_idAttributes.get(_table)[0], SearchCriteria.Op.EQ, id);
        Transaction txn = Transaction.currentTxn();
        txn.start();

        try {
            if (ub.getCollectionChanges() != null) {
                insertElementCollection(entity, _idAttributes.get(_table)[0], id, ub.getCollectionChanges());
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to persist element collection", e);
        }

        int rowsUpdated = update(ub, sc, null);

        txn.commit();

        return rowsUpdated;
    }

    public int update(UpdateBuilder ub, final SearchCriteria<?> sc, Integer rows) {
        StringBuilder sql = null;
        PreparedStatement pstmt = null;
        final Transaction txn = Transaction.currentTxn();
        try {
            final String searchClause = sc.getWhereClause();

            sql = ub.toSql(_tables);
            if (sql == null) {
                return  0;
            }

            sql.append(searchClause);

            if (rows != null) {
                sql.append(" LIMIT ").append(rows);
            }

            txn.start();
            pstmt = txn.prepareAutoCloseStatement(sql.toString());

            Collection<Ternary<Attribute, Boolean, Object>> changes = ub.getChanges();

            int i = 1;
            for (final Ternary<Attribute, Boolean, Object> value : changes) {
                prepareAttribute(i++, pstmt, value.first(), value.third());
            }

            for (Pair<Attribute, Object> value : sc.getValues()) {
                prepareAttribute(i++, pstmt, value.first(), value.second());
            }

            int result = pstmt.executeUpdate();
            txn.commit();
            ub.clear();
            return result;
        } catch (final SQLException e) {
            if (e.getSQLState().equals("23000") && e.getErrorCode() == 1062) {
                throw new EntityExistsException("Entity already exists ", e);
            }
            throw new CloudRuntimeException("DB Exception on: " + pstmt, e);
        }
    }

    @DB(txn=false)
    protected Attribute findAttributeByFieldName(String name) {
        return _allAttributes.get(name);
    }

    @DB(txn=false)
    protected String buildSelectByIdSql(final StringBuilder sql) {
        if (_idField == null) {
            return null;
        }

        if (_idField.getAnnotation(EmbeddedId.class) == null) {
            sql.append(_table).append(".").append(DbUtil.getColumnName(_idField, null)).append(" = ? ");
        } else {
            final Class<?> clazz = _idField.getClass();
            final AttributeOverride[] overrides = DbUtil.getAttributeOverrides(_idField);
            for (final Field field : clazz.getDeclaredFields()) {
                sql.append(_table).append(".").append(DbUtil.getColumnName(field, overrides)).append(" = ? AND ");
            }
            sql.delete(sql.length() - 4, sql.length());
        }

        return sql.toString();
    }

    @DB(txn=false)
    @Override
    public Class<T> getEntityBeanType() {
        return _entityBeanType;
    }

    @DB(txn=false)
    protected T findOneIncludingRemovedBy(final SearchCriteria<T> sc) {
        Filter filter = new Filter(1);
        List<T> results = searchIncludingRemoved(sc, filter, null, false);
        assert results.size() <= 1 : "Didn't the limiting worked?";
        return results.size() == 0 ? null : results.get(0);
    }

    @Override
    @DB(txn=false)
    public T findOneBy(final SearchCriteria<T> sc) {
        if (_removed != null) {
            sc.addAnd(_removed.second().field.getName(), SearchCriteria.Op.NULL);
        }
        return findOneIncludingRemovedBy(sc);
    }

    @DB(txn=false)
    protected List<T> listBy(final SearchCriteria<T> sc, final Filter filter) {
        if (_removed != null) {
            sc.addAnd(_removed.second().field.getName(), SearchCriteria.Op.NULL);
        }
        return listIncludingRemovedBy(sc, filter);
    }

    @DB(txn=false)
    protected List<T> listBy(final SearchCriteria<T> sc, final Filter filter, final boolean enable_query_cache) {
        if (_removed != null) {
            sc.addAnd(_removed.second().field.getName(), SearchCriteria.Op.NULL);
        }
        return listIncludingRemovedBy(sc, filter, enable_query_cache);
    }

    @DB(txn=false)
    protected List<T> listBy(final SearchCriteria<T> sc) {
        return listBy(sc, null);
    }

    @DB(txn=false)
    protected List<T> listIncludingRemovedBy(final SearchCriteria<T> sc, final Filter filter, final boolean enable_query_cache) {
        return searchIncludingRemoved(sc, filter, null, false, enable_query_cache);
    }

    @DB(txn=false)
    protected List<T> listIncludingRemovedBy(final SearchCriteria<T> sc, final Filter filter) {
        return searchIncludingRemoved(sc, filter, null, false);
    }

    @DB(txn=false)
    protected List<T> listIncludingRemovedBy(final SearchCriteria<T> sc) {
        return listIncludingRemovedBy(sc, null);
    }

    @Override @DB(txn=false)
    @SuppressWarnings("unchecked")
    public T findById(final ID id) {
        if (_cache != null) {
            final Element element = _cache.get(id);
            return element == null ? lockRow(id, null) : (T)element.getObjectValue();
        } else {
            return lockRow(id, null);
        }
    }

    @Override @DB(txn=false)
    @SuppressWarnings("unchecked")
    public T findByUuid(final String uuid) {
        SearchCriteria<T> sc = createSearchCriteria();
        sc.addAnd("uuid", SearchCriteria.Op.EQ, uuid);
        return findOneBy(sc);
    }

    @Override @DB(txn=false)
    @SuppressWarnings("unchecked")
    public T findByUuidIncludingRemoved(final String uuid) {
        SearchCriteria<T> sc = createSearchCriteria();
        sc.addAnd("uuid", SearchCriteria.Op.EQ, uuid);
        return findOneIncludingRemovedBy(sc);
    }

    @Override @DB(txn=false)
    public T findByIdIncludingRemoved(ID id) {
        return findById(id, true, null);
    }

    @Override @DB(txn=false)
    public T findById(final ID id, boolean fresh) {
        if(!fresh) {
            return findById(id);
        }

        if (_cache != null) {
            _cache.remove(id);
        }
        return lockRow(id, null);
    }

    @Override @DB(txn=false)
    public T lockRow(ID id, Boolean lock) {
        return findById(id, false, lock);
    }

    protected T findById(ID id, boolean removed, Boolean lock) {
        StringBuilder sql = new StringBuilder(_selectByIdSql);
        if (!removed && _removed != null) {
            sql.append(" AND ").append(_removed.first());
        }
        if (lock != null) {
            sql.append(lock ? FOR_UPDATE_CLAUSE : SHARE_MODE_CLAUSE);
        }
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());

            if (_idField.getAnnotation(EmbeddedId.class) == null) {
                prepareAttribute(1, pstmt, _idAttributes.get(_table)[0], id);
            }

            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? toEntityBean(rs, true) : null;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + pstmt, e);
        }
    }

    @Override @DB(txn=false)
    public T acquireInLockTable(ID id) {
        return acquireInLockTable(id, _timeoutSeconds);
    }

    @Override
    public T acquireInLockTable(final ID id, int seconds) {
        Transaction txn = Transaction.currentTxn();
        T t = null;
        boolean locked  = false;
        try {
            if (!txn.lock(_table + id.toString(), seconds)) {
                return null;
            }

            locked = true;
            t = findById(id);
            return t;
        } finally {
            if (t == null && locked) {
                txn.release(_table + id.toString());
            }
        }
    }

    @Override
    public boolean releaseFromLockTable(final ID id) {
        final Transaction txn = Transaction.currentTxn();
        return txn.release(_table + id);
    }

    @Override @DB(txn=false)
    public boolean lockInLockTable(final String id) {
        return lockInLockTable(id, _timeoutSeconds);
    }

    @Override
    public boolean lockInLockTable(final String id, int seconds) {
        Transaction txn = Transaction.currentTxn();
        return txn.lock(_table + id, seconds);
    }

    @Override
    public boolean unlockFromLockTable(final String id) {
        final Transaction txn = Transaction.currentTxn();
        return txn.release(_table + id);
    }

    @Override @DB(txn=false)
    public List<T> listAllIncludingRemoved() {
        return listAllIncludingRemoved(null);
    }

    @DB(txn=false)
    protected List<Object> addGroupBy(final StringBuilder sql, SearchCriteria<?> sc) {
        Pair<GroupBy<?, ?>, List<Object>> groupBys = sc.getGroupBy();
        if (groupBys != null) {
            groupBys.first().toSql(sql);
            return groupBys.second();
        } else {
            return null;
        }
    }

    @DB(txn=false)
    protected void addFilter(final StringBuilder sql, final Filter filter) {
        if (filter != null) {
            if (filter.getOrderBy() != null) {
                sql.append(filter.getOrderBy());
            }
            if (filter.getOffset() != null) {
                sql.append(" LIMIT ");
                sql.append(filter.getOffset());
                if (filter.getLimit() != null) {
                    sql.append(", ").append(filter.getLimit());
                }
            }
        }
    }

    @Override @DB(txn=false)
    public List<T> listAllIncludingRemoved(final Filter filter) {
        final StringBuilder sql = createPartialSelectSql(null, false);
        addFilter(sql, filter);

        return executeList(sql.toString());
    }

    protected List<T> executeList(final String sql, final Object... params) {
        final Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        final List<T> result = new ArrayList<T>();
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            int i = 0;
            for (final Object param : params) {
                pstmt.setObject(++i, param);
            }

            final ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(toEntityBean(rs, true));
            }
            return result;
        } catch (final SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + pstmt, e);
        } catch (final Throwable e) {
            throw new CloudRuntimeException("Caught: " + pstmt, e);
        }
    }

    @Override @DB(txn=false)
    public List<T> listAll() {
        return listAll(null);
    }

    @Override @DB(txn=false)
    public List<T> listAll(final Filter filter) {
        if (_removed == null) {
            return listAllIncludingRemoved(filter);
        }

        final StringBuilder sql = createPartialSelectSql(null, true);
        sql.append(_removed.first());
        addFilter(sql, filter);

        return executeList(sql.toString());
    }

    @Override
    public boolean expunge(final ID id) {
        final Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        String sql = null;
        try {
            txn.start();
            for (final Pair<String, Attribute[]> deletSql : _deleteSqls) {
                sql = deletSql.first();
                final Attribute[] attrs = deletSql.second();

                pstmt = txn.prepareAutoCloseStatement(sql);

                for (int i = 0; i < attrs.length; i++) {
                    prepareAttribute(i + 1, pstmt, attrs[i], id);
                }
                pstmt.executeUpdate();
            }

            txn.commit();
            if (_cache != null) {
                _cache.remove(id);
            }
            return true;
        } catch (final SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + pstmt, e);
        }
    }

    // FIXME: Does not work for joins.
    @Override
    public int expunge(final SearchCriteria<T> sc) {
        final StringBuilder str = new StringBuilder("DELETE FROM ");
        str.append(_table);
        str.append(" WHERE ");

        if (sc != null && sc.getWhereClause().length() > 0) {
            str.append(sc.getWhereClause());
        }

        final String sql = str.toString();

        final Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            int i = 0;
            for (final Pair<Attribute, Object> value : sc.getValues()) {
                prepareAttribute(++i, pstmt, value.first(), value.second());
            }
            return pstmt.executeUpdate();
        } catch (final SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + pstmt, e);
        } catch (final Throwable e) {
            throw new CloudRuntimeException("Caught: " + pstmt, e);
        }
    }

    @DB(txn=false)
    protected StringBuilder createPartialSelectSql(SearchCriteria<?> sc, final boolean whereClause, final boolean enable_query_cache) {
        StringBuilder sql = new StringBuilder(enable_query_cache ? _partialQueryCacheSelectSql.first() : _partialSelectSql.first());
        if (sc != null && !sc.isSelectAll()) {
            sql.delete(7, sql.indexOf(" FROM"));
            sc.getSelect(sql, 7);
        }

        if (!whereClause) {
            sql.delete(sql.length() - (_discriminatorClause == null ? 6 : 4), sql.length());
        }

        return sql;
    }

    @DB(txn=false)
    protected StringBuilder createPartialSelectSql(SearchCriteria<?> sc, final boolean whereClause) {
        StringBuilder sql = new StringBuilder(_partialSelectSql.first());
        if (sc != null && !sc.isSelectAll()) {
            sql.delete(7, sql.indexOf(" FROM"));
            sc.getSelect(sql, 7);
        }

        if (!whereClause) {
            sql.delete(sql.length() - (_discriminatorClause == null ? 6 : 4), sql.length());
        }

        return sql;
    }


    @DB(txn = false)
    protected void addJoins(StringBuilder str, Collection<JoinBuilder<SearchCriteria<?>>> joins) {
        int fromIndex = str.lastIndexOf("WHERE");
        if (fromIndex == -1) {
            fromIndex = str.length();
            str.append(" WHERE ");
        } else {
            str.append(" AND ");
        }

        for (JoinBuilder<SearchCriteria<?>> join : joins) {
            StringBuilder onClause = new StringBuilder();
            onClause.append(" ").append(join.getType().getName()).append(" ").append(join.getSecondAttribute().table)
            .append(" ON ").append(join.getFirstAttribute().table).append(".").append(join.getFirstAttribute().columnName)
            .append("=").append(join.getSecondAttribute().table).append(".").append(join.getSecondAttribute().columnName)
            .append(" ");
            str.insert(fromIndex, onClause);
            String whereClause = join.getT().getWhereClause();
            if ((whereClause != null) && !"".equals(whereClause)) {
                str.append(" (").append(whereClause).append(") AND");
            }
            fromIndex += onClause.length();
        }

        str.delete(str.length() - 4, str.length());

        for (JoinBuilder<SearchCriteria<?>> join : joins) {
            if (join.getT().getJoins() != null) {
                addJoins(str, join.getT().getJoins());
            }
        }
    }

    @Override @DB(txn=false)
    public List<T> search(final SearchCriteria<T> sc, final Filter filter) {
        return search(sc, filter, null, false);
    }

    @Override @DB(txn=false)
    public Pair<List<T>, Integer> searchAndCount(final SearchCriteria<T> sc, final Filter filter) {
        List<T> objects = search(sc, filter, null, false);
        Integer count = getCount(sc);
        return new Pair<List<T>, Integer>(objects, count);
    }

    @Override @DB(txn=false)
    public List<T> search(final SearchCriteria<T> sc, final Filter filter, final boolean enable_query_cache) {
        return search(sc, filter, null, false, enable_query_cache);
    }

    @Override @DB(txn=false)
    public boolean update(ID id, T entity) {
        assert Enhancer.isEnhanced(entity.getClass()) : "Entity is not generated by this dao";

        UpdateBuilder ub = getUpdateBuilder(entity);
        boolean result = update(id, ub, entity) != 0;
        return result;
    }

    @DB(txn=false)
    public int update(final T entity, final SearchCriteria<T> sc, Integer rows) {
        final UpdateBuilder ub = getUpdateBuilder(entity);
        return update(ub, sc, rows);
    }

    @DB(txn=false)
    public int update(final T entity, final SearchCriteria<T> sc) {
        final UpdateBuilder ub = getUpdateBuilder(entity);
        return update(ub, sc, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T persist(final T entity) {
        if (Enhancer.isEnhanced(entity.getClass())) {
            if (_idField != null) {
                ID id;
                try {
                    id = (ID)_idField.get(entity);
                } catch (IllegalAccessException e) {
                    throw new CloudRuntimeException("How can it be illegal access...come on", e);
                }
                update(id, entity);
                return entity;
            }

            assert false : "Can't call persit if you don't have primary key";
        }

        ID id = null;
        final Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        String sql = null;
        try {
            txn.start();
            for (final Pair<String, Attribute[]> pair : _insertSqls) {
                sql = pair.first();
                final Attribute[] attrs = pair.second();

                pstmt = txn.prepareAutoCloseStatement(sql, Statement.RETURN_GENERATED_KEYS);

                int index = 1;
                index = prepareAttributes(pstmt, entity, attrs, index);

                pstmt.executeUpdate();

                final ResultSet rs = pstmt.getGeneratedKeys();
                if (id == null) {
                    if (rs != null && rs.next()) {
                        id = (ID)rs.getObject(1);
                    }
                    try {
                        if (_idField != null) {
                            if (id != null) {
                                _idField.set(entity, id);
                            } else {
                                id = (ID)_idField.get(entity);
                            }
                        }
                    } catch (final IllegalAccessException e) {
                        throw new CloudRuntimeException("Yikes! ", e);
                    }
                }
            }

            if (_ecAttributes != null && _ecAttributes.size() > 0) {
                HashMap<Attribute, Object> ecAttributes = new HashMap<Attribute, Object>();
                for (Attribute attr : _ecAttributes) {
                    Object ec = attr.field.get(entity);
                    if (ec != null) {
                        ecAttributes.put(attr, ec);
                    }
                }

                insertElementCollection(entity, _idAttributes.get(_table)[0], id, ecAttributes);
            }
            txn.commit();
        } catch (final SQLException e) {
            if (e.getSQLState().equals("23000") && e.getErrorCode() == 1062) {
                throw new EntityExistsException("Entity already exists: ", e);
            } else {
                throw new CloudRuntimeException("DB Exception on: " + pstmt, e);
            }
        } catch (IllegalArgumentException e) {
            throw new CloudRuntimeException("Problem with getting the ec attribute ", e);
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException("Problem with getting the ec attribute ", e);
        }

        return _idField != null ? findByIdIncludingRemoved(id) : null;
    }

    protected void insertElementCollection(T entity, Attribute idAttribute, ID id, Map<Attribute, Object> ecAttributes) throws SQLException {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (Map.Entry<Attribute, Object> entry : ecAttributes.entrySet()) {
            Attribute attr = entry.getKey();
            Object obj = entry.getValue();

            EcInfo ec = (EcInfo)attr.attache;
            Enumeration en = null;
            if (ec.rawClass == null) {
                en = Collections.enumeration(Arrays.asList((Object[])obj));
            } else {
                en = Collections.enumeration((Collection)obj);
            }
            PreparedStatement pstmt = txn.prepareAutoCloseStatement(ec.clearSql);
            prepareAttribute(1, pstmt, idAttribute, id);
            pstmt.executeUpdate();

            while (en.hasMoreElements()) {
                pstmt = txn.prepareAutoCloseStatement(ec.insertSql);
                if (ec.targetClass == Date.class) {
                    pstmt.setString(1, DateUtil.getDateDisplayString(s_gmtTimeZone, (Date)en.nextElement()));
                } else {
                    pstmt.setObject(1, en.nextElement());
                }
                prepareAttribute(2, pstmt, idAttribute, id);
                pstmt.executeUpdate();
            }
        }
        txn.commit();
    }

    @DB(txn=false)
    protected Object generateValue(final Attribute attr) {
        if (attr.is(Attribute.Flag.Created) || attr.is(Attribute.Flag.Removed)) {
            return new Date();
        } else if (attr.is(Attribute.Flag.TableGV)) {
            return null;
            // Not sure what to do here.
        } else if (attr.is(Attribute.Flag.AutoGV)) {
            if (attr.columnName.equals(GenericDao.XID_COLUMN)) {
                return UUID.randomUUID().toString();
            }
            assert (false) : "Auto generation is not supported.";
            return null;
        } else if (attr.is(Attribute.Flag.SequenceGV)) {
            assert (false) : "Sequence generation is not supported.";
            return null;
        } else if (attr.is(Attribute.Flag.DC)) {
            return _discriminatorValues.get(attr.columnName);
        } else {
            assert (false) : "Attribute can't be auto generated: " + attr.columnName;
            return null;
        }
    }

    @DB(txn=false)
    protected void prepareAttribute(final int j, final PreparedStatement pstmt, final Attribute attr, Object value) throws SQLException {
        if (attr.is(Attribute.Flag.DaoGenerated) && value == null) {
            value = generateValue(attr);
            if (attr.field == null) {
                pstmt.setObject(j, value);
                return;
            }
        }
        if (attr.field.getType() == String.class) {
            final String str = (String)value;
            if (str == null) {
                pstmt.setString(j, null);
                return;
            }
            final Column column = attr.field.getAnnotation(Column.class);
            final int length = column != null ? column.length() : 255;

            // to support generic localization, utilize MySql UTF-8 support
            if (length < str.length()) {
                try {
                    if (attr.is(Attribute.Flag.Encrypted)){
                        pstmt.setBytes(j, DBEncryptionUtil.encrypt(str.substring(0, column.length())).getBytes("UTF-8"));
                    } else {
                        pstmt.setBytes(j, str.substring(0, column.length()).getBytes("UTF-8"));
                    }
                } catch (UnsupportedEncodingException e) {
                    // no-way it can't support UTF-8 encoding
                    assert(false);
                    throw new CloudRuntimeException("UnsupportedEncodingException when saving string as UTF-8 data");
                }
            } else {
                try {
                    if (attr.is(Attribute.Flag.Encrypted)){
                        pstmt.setBytes(j, DBEncryptionUtil.encrypt(str).getBytes("UTF-8"));
                    } else {
                        pstmt.setBytes(j, str.getBytes("UTF-8"));
                    }
                } catch (UnsupportedEncodingException e) {
                    // no-way it can't support UTF-8 encoding
                    assert(false);
                    throw new CloudRuntimeException("UnsupportedEncodingException when saving string as UTF-8 data");
                }
            }
        } else if (attr.field.getType() == Date.class) {
            final Date date = (Date)value;
            if (date == null) {
                pstmt.setObject(j, null);
                return;
            }
            if (attr.is(Attribute.Flag.Date)) {
                pstmt.setString(j, DateUtil.getDateDisplayString(s_gmtTimeZone, date));
            } else if (attr.is(Attribute.Flag.TimeStamp)) {
                pstmt.setString(j, DateUtil.getDateDisplayString(s_gmtTimeZone, date));
            } else if (attr.is(Attribute.Flag.Time)) {
                pstmt.setString(j, DateUtil.getDateDisplayString(s_gmtTimeZone, date));
            }
        } else if (attr.field.getType() == Calendar.class) {
            final Calendar cal = (Calendar)value;
            if (cal == null) {
                pstmt.setObject(j, null);
                return;
            }
            if (attr.is(Attribute.Flag.Date)) {
                pstmt.setString(j, DateUtil.getDateDisplayString(s_gmtTimeZone, cal.getTime()));
            } else if (attr.is(Attribute.Flag.TimeStamp)) {
                pstmt.setString(j, DateUtil.getDateDisplayString(s_gmtTimeZone, cal.getTime()));
            } else if (attr.is(Attribute.Flag.Time)) {
                pstmt.setString(j, DateUtil.getDateDisplayString(s_gmtTimeZone, cal.getTime()));
            }
        } else if (attr.field.getType().isEnum()) {
            final Enumerated enumerated = attr.field.getAnnotation(Enumerated.class);
            final EnumType type = (enumerated == null) ? EnumType.STRING : enumerated.value();
            if (type == EnumType.STRING) {
                pstmt.setString(j, value == null ? null :  value.toString());
            } else if (type == EnumType.ORDINAL) {
                pstmt.setInt(j, value == null ? null : ((Enum<?>)value).ordinal());
            }
        } else if (attr.field.getType() == URI.class) {
            pstmt.setString(j, value == null ? null : value.toString());
        } else if (attr.field.getType() == URL.class) {
            pstmt.setURL(j, (URL)value);
        } else if (attr.field.getType() == byte[].class) {
            pstmt.setBytes(j, (byte[])value);
        } else if (attr.field.getType() == Ip.class) {
            final Enumerated enumerated = attr.field.getAnnotation(Enumerated.class);
            final EnumType type = (enumerated == null) ? EnumType.ORDINAL : enumerated.value();
            if (type == EnumType.STRING) {
                pstmt.setString(j, value == null ? null : value.toString());
            } else if (type == EnumType.ORDINAL) {
                pstmt.setLong(j, value == null ? null : (value instanceof Ip) ? ((Ip)value).longValue() : NetUtils.ip2Long((String)value));
            }
        } else {
            pstmt.setObject(j, value);
        }
    }

    @DB(txn=false)
    protected int prepareAttributes(final PreparedStatement pstmt, final Object entity, final Attribute[] attrs, final int index) throws SQLException {
        int j = 0;
        for (int i = 0; i < attrs.length; i++) {
            j = i + index;
            try {
                prepareAttribute(j, pstmt, attrs[i], attrs[i].field != null ? attrs[i].field.get(entity) : null);
            } catch (final IllegalArgumentException e) {
                throw new CloudRuntimeException("IllegalArgumentException", e);
            } catch (final IllegalAccessException e) {
                throw new CloudRuntimeException("IllegalArgumentException", e);
            }
        }

        return j;
    }

    @SuppressWarnings("unchecked") @DB(txn=false)
    protected T toEntityBean(final ResultSet result, final boolean cache) throws SQLException {
        final T entity = (T)_factory.newInstance(new Callback[] {NoOp.INSTANCE, new UpdateBuilder(this)});

        toEntityBean(result, entity);

        if (cache && _cache != null) {
            try {
                _cache.put(new Element(_idField.get(entity), entity));
            } catch (final Exception e) {
                s_logger.debug("Can't put it in the cache", e);
            }
        }

        return entity;
    }

    @DB(txn=false)
    protected T toVO(ResultSet result, boolean cache) throws SQLException {
        T entity;
        try {
            entity = _entityBeanType.newInstance();
        } catch (InstantiationException e1) {
            throw new CloudRuntimeException("Unable to instantiate entity", e1);
        } catch (IllegalAccessException e1) {
            throw new CloudRuntimeException("Illegal Access", e1);
        }
        toEntityBean(result, entity);
        if (cache && _cache != null) {
            try {
                _cache.put(new Element(_idField.get(entity), entity));
            } catch (final Exception e) {
                s_logger.debug("Can't put it in the cache", e);
            }
        }

        return entity;
    }

    @DB(txn=false)
    protected void toEntityBean(final ResultSet result, final T entity) throws SQLException {
        ResultSetMetaData meta = result.getMetaData();
        for (int index = 1, max = meta.getColumnCount(); index <= max; index++) {
            setField(entity, result, meta, index);
        }
        for (Attribute attr : _ecAttributes) {
            loadCollection(entity, attr);
        }
    }

    @DB(txn = true)
    @SuppressWarnings("unchecked")
    protected void loadCollection(T entity, Attribute attr)  {
        EcInfo ec = (EcInfo)attr.attache;

        Transaction txn = Transaction.currentTxn();
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareStatement(ec.selectSql);
            pstmt.setObject(1, _idField.get(entity));
            rs = pstmt.executeQuery();
            ArrayList lst = new ArrayList();
            if (ec.targetClass == Integer.class) {
                while (rs.next()) {
                    lst.add(rs.getInt(1));
                }
            } else if (ec.targetClass == Long.class) {
                while (rs.next()) {
                    lst.add(rs.getLong(1));
                }
            } else if (ec.targetClass == String.class) {
                while (rs.next()) {
                    lst.add(rs.getString(1));
                }
            } else if (ec.targetClass == Short.class) {
                while (rs.next()) {
                    lst.add(rs.getShort(1));
                }
            } else if (ec.targetClass == Date.class) {
                while (rs.next()) {
                    lst.add(DateUtil.parseDateString(s_gmtTimeZone, rs.getString(1)));
                }
            } else if (ec.targetClass == Boolean.class) {
                while (rs.next()) {
                    lst.add(rs.getBoolean(1));
                }
            } else {
                assert (false) : "You'll need to add more classeses";
            }

            if (ec.rawClass == null) {
                Object[] array = (Object[])Array.newInstance(ec.targetClass);
                lst.toArray(array);
                try {
                    attr.field.set(entity, array);
                } catch (IllegalArgumentException e) {
                    throw new CloudRuntimeException("Come on we screen for this stuff, don't we?", e);
                } catch (IllegalAccessException e) {
                    throw new CloudRuntimeException("Come on we screen for this stuff, don't we?", e);
                }
            } else {
                try {
                    Collection coll = (Collection)ec.rawClass.newInstance();
                    coll.addAll(lst);
                    attr.field.set(entity, coll);
                } catch (IllegalAccessException e) {
                    throw new CloudRuntimeException("Come on we screen for this stuff, don't we?", e);
                } catch (InstantiationException e) {
                    throw new CloudRuntimeException("Never should happen", e);
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Error executing " + pstmt, e);
        } catch (IllegalArgumentException e) {
            throw new CloudRuntimeException("Error executing " + pstmt, e);
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException("Error executing " + pstmt, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
                s_logger.error("Why are we getting an exception at close? ", e);
            }
        }
    }

    @Override
    public void expunge() {
        if (_removed == null) {
            return;
        }
        final StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(_table).append(" WHERE ").append(_removed.first()).append(" IS NOT NULL");
        final Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            txn.start();
            pstmt = txn.prepareAutoCloseStatement(sql.toString());

            pstmt.executeUpdate();
            txn.commit();
        } catch (final SQLException e) {
            throw new CloudRuntimeException("DB Exception on " + pstmt, e);
        }
    }

    @DB(txn=false)
    protected void setField(final Object entity, final ResultSet rs, ResultSetMetaData meta, final int index) throws SQLException {
        Attribute attr = _allColumns.get(new Pair<String, String>(meta.getTableName(index), meta.getColumnName(index)));
        if ( attr == null ){
            // work around for mysql bug to return original table name instead of view name in db view case
            Table tbl = entity.getClass().getSuperclass().getAnnotation(Table.class);
            if ( tbl != null ){
                attr = _allColumns.get(new Pair<String, String>(tbl.name(), meta.getColumnLabel(index)));
            }
        }
        assert (attr != null) : "How come I can't find " + meta.getCatalogName(index) + "." + meta.getColumnName(index);
        setField(entity, attr.field, rs, index);
    }

    @Override
    public boolean remove(final ID id) {
        if (_removeSql == null) {
            return expunge(id);
        }

        final Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {

            txn.start();
            pstmt = txn.prepareAutoCloseStatement(_removeSql.first());
            final Attribute[] attrs = _removeSql.second();
            prepareAttribute(1, pstmt, attrs[attrs.length - 1], null);
            for (int i = 0; i < attrs.length - 1; i++) {
                prepareAttribute(i + 2, pstmt, attrs[i], id);
            }

            final int result = pstmt.executeUpdate();
            txn.commit();
            if (_cache != null) {
                _cache.remove(id);
            }
            return result > 0;
        } catch (final SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + pstmt, e);
        }
    }

    @Override
    public int remove(SearchCriteria<T> sc) {
        if (_removeSql == null) {
            return expunge(sc);
        }

        T vo = createForUpdate();
        UpdateBuilder ub = getUpdateBuilder(vo);

        ub.set(vo, _removed.second(), new Date());
        return update(ub, sc, null);
    }

    protected Cache _cache;
    @DB(txn=false)
    protected void createCache(final Map<String, ? extends Object> params) {
        final String value = (String)params.get("cache.size");

        if (value != null) {
            final CacheManager cm = CacheManager.create();
            final int maxElements = NumbersUtil.parseInt(value, 0);
            final int live = NumbersUtil.parseInt((String)params.get("cache.time.to.live"), 300);
            final int idle = NumbersUtil.parseInt((String)params.get("cache.time.to.idle"), 300);
            _cache = new Cache(getName(), maxElements, false, live == -1, live == -1 ? Integer.MAX_VALUE : live, idle);
            cm.addCache(_cache);
            s_logger.info("Cache created: " + _cache.toString());
        } else {
            _cache = null;
        }
    }

    @Override @DB(txn=false)
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        Class<?> daoInterface = null;
        for(Class<?> intf : this.getClass().getInterfaces()) {
        	if(GenericDao.class.isAssignableFrom(intf)) {
        		daoInterface = intf;
        		break;
        	}
        }
 
        if(daoInterface != null) {
        	s_logger.info("Register dao interface in GenericDaoBase entity-DAO map. " + daoInterface.getName());
	        s_daoMaps.put(_entityBeanType, (GenericDao<?, ? extends Serializable>) ComponentContext.getComponent(daoInterface));
	        Class<?>[] interphaces = _entityBeanType.getInterfaces();
	        if (interphaces != null) {
	            for (Class<?> interphace : interphaces) {
	                s_daoMaps.put(interphace,  (GenericDao<?, ? extends Serializable>) ComponentContext.getComponent(daoInterface));
	            }
	        }
	    }
       
        final String value = (String)params.get("lock.timeout");
        _timeoutSeconds = NumbersUtil.parseInt(value, 300);

        createCache(params);
        final boolean load = Boolean.parseBoolean((String)params.get("cache.preload"));
        if (load) {
            listAll();
        }

        return true;
    }

    @DB(txn=false)
    public static <T> UpdateBuilder getUpdateBuilder(final T entityObject) {
        final Factory factory = (Factory)entityObject;
        assert(factory != null);
        return (UpdateBuilder)factory.getCallback(1);
    }

    @SuppressWarnings("unchecked")
    @Override @DB(txn=false)
    public SearchBuilder<T> createSearchBuilder() {
        final T entity = (T)_searchEnhancer.create();
        final Factory factory = (Factory)entity;
        SearchBuilder<T> builder = new SearchBuilder<T>(entity, _allAttributes);
        factory.setCallback(0, builder);
        return builder;
    }

    @Override @DB(txn=false)
    public SearchCriteria<T> createSearchCriteria() {
        SearchBuilder<T> builder = createSearchBuilder();
        return builder.create();
    }

    @Override @DB(txn=false)
    public <K> SearchCriteria2 createSearchCriteria2(Class<K> resultType) {
        final T entity = (T)_searchEnhancer.create();
        final Factory factory = (Factory)entity;
        SearchCriteria2 sc = new SearchCriteria2(entity, resultType, _allAttributes, this);
        factory.setCallback(0, sc);
        return sc;
    }

    @Override @DB(txn=false)
    public SearchCriteria2 createSearchCriteria2() {
        final T entity = (T)_searchEnhancer.create();
        final Factory factory = (Factory)entity;
        SearchCriteria2 sc = new SearchCriteria2(entity, entity.getClass(), _allAttributes, this);
        factory.setCallback(0, sc);
        return sc;
    }

    @Override
    public int getRegionId(){
    	return Transaction.s_region_id;
    }

    public Integer getCount(SearchCriteria<T> sc) {
        String clause = sc != null ? sc.getWhereClause() : null;
        if (clause != null && clause.length() == 0) {
            clause = null;
        }

        final StringBuilder str = createCountSelect(sc, clause != null);
        if (clause != null) {
            str.append(clause);
        }

        Collection<JoinBuilder<SearchCriteria<?>>> joins = null;
        if (sc != null) {
            joins = sc.getJoins();
            if (joins != null) {
                addJoins(str, joins);
            }
        }

        // we have to disable group by in getting count, since count for groupBy clause will be different.
        //List<Object> groupByValues = addGroupBy(str, sc);
        final Transaction txn = Transaction.currentTxn();
        final String sql = str.toString();

        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            int i = 0;
            if (clause != null) {
                for (final Pair<Attribute, Object> value : sc.getValues()) {
                    prepareAttribute(++i, pstmt, value.first(), value.second());
                }
            }

            if (joins != null) {
                i = addJoinAttributes(i, pstmt, joins);
            }

            /*
            if (groupByValues != null) {
                for (Object value : groupByValues) {
                    pstmt.setObject(i++, value);
                }
            }
             */

            final ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (final SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + pstmt, e);
        } catch (final Throwable e) {
            throw new CloudRuntimeException("Caught: " + pstmt, e);
        }
    }

    @DB(txn=false)
    protected StringBuilder createCountSelect(SearchCriteria<?> sc, final boolean whereClause) {
        StringBuilder sql = new StringBuilder(_count);

        if (!whereClause) {
            sql.delete(sql.length() - (_discriminatorClause == null ? 6 : 4), sql.length());
        }

        return sql;
    }
}
