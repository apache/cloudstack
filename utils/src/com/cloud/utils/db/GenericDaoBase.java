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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.naming.ConfigurationException;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.TableGenerator;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;

import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;

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
public abstract class GenericDaoBase<T, ID extends Serializable> implements GenericDao<T, ID> {
    private final static Logger s_logger = Logger.getLogger(GenericDaoBase.class);
    
    protected final static TimeZone s_gmtTimeZone = TimeZone.getTimeZone("GMT");

    protected Class<T> _entityBeanType;
    protected String _table;
    
    protected String _tables;

    protected Field[] _embeddedFields;

    // This is private on purpose.  Everyone should use createPartialSelectSql()
    private final Pair<StringBuilder, Attribute[]> _partialSelectSql;
    protected StringBuilder _discriminatorClause;
    protected Map<String, Object> _discriminatorValues;
    protected String _selectByIdSql;

    protected Field _idField;

    protected List<Pair<String, Attribute[]>> _insertSqls;
    protected Pair<String, Attribute> _removed;
    protected Pair<String, Attribute[]> _removeSql;
    protected List<Pair<String, Attribute[]>> _deleteSqls;
    protected Map<String, Attribute[]> _idAttributes;
    protected Map<String, TableGenerator> _tgs;
    protected final Map<String, Attribute> _allAttributes;
    protected final Map<Pair<String, String>, Attribute> _allColumns;
    protected Enhancer _enhancer;
    protected Factory _factory;
    protected int _timeoutSeconds;

    protected static final String FOR_UPDATE_CLAUSE = " FOR UPDATE ";
    protected static final String SHARE_MODE_CLAUSE = " LOCK IN SHARE MODE";
    protected static final String SELECT_LAST_INSERT_ID_SQL = "SELECT LAST_INSERT_ID()";
    
    protected static final SequenceFetcher s_seqFetcher = SequenceFetcher.getInstance();

    protected static PreparedStatement s_initStmt;
    static {
    	Connection conn = Transaction.getStandaloneConnection();
        try {
            s_initStmt = conn.prepareStatement("SELECT 1");
        } catch (final SQLException e) {
        } finally {
        	try {
				conn.close();
			} catch (SQLException e) {
			}
        }

    }

    protected String _name;
    
    @SuppressWarnings("unchecked")
    protected GenericDaoBase() {
        Type t = getClass().getGenericSuperclass();
        if (t instanceof ParameterizedType) {
            _entityBeanType = (Class<T>)((ParameterizedType)t).getActualTypeArguments()[0];
        } else {
            _entityBeanType = (Class<T>)((ParameterizedType)((Class<?>)t).getGenericSuperclass()).getActualTypeArguments()[0];
            
        }
        _table = DbUtil.getTableName(_entityBeanType);

        final SqlGenerator generator = new SqlGenerator(_entityBeanType);
        _partialSelectSql = generator.buildSelectSql();
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
        
        TableGenerator tg = this.getClass().getAnnotation(TableGenerator.class);
        if (tg != null) {
            _tgs.put(tg.name(), tg);
        }
        tg = this.getClass().getSuperclass().getAnnotation(TableGenerator.class);
        if (tg != null) {
            _tgs.put(tg.name(), tg);
        }
        
        _enhancer = new Enhancer();
        _enhancer.setSuperclass(_entityBeanType);
        _enhancer.setCallback(new UpdateBuilder(_allAttributes));
        _factory = (Factory)_enhancer.create();
        
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
        }
    }

    @Override @DB(txn=false)
    @SuppressWarnings("unchecked")
    public T createForUpdate(final ID id) {
        final T entity = (T)_enhancer.create();
        final Factory factory = (Factory)entity;
        if (id != null) {
            try {
                _idField.set(entity, id);
            } catch (final IllegalArgumentException e) {
            } catch (final IllegalAccessException e) {
            }
        }
        factory.setCallback(0, new UpdateBuilder(_allAttributes));
        return entity;
    }

    @Override @DB(txn=false)
    public T createForUpdate() {
        return createForUpdate(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K> K getNextInSequence(final Class<K> clazz, final String name) {
        final TableGenerator tg = _tgs.get(name);
        assert (tg != null) : "Couldn't find Table generator using " + name;

        return s_seqFetcher.getNextSequence(clazz, tg);
    }

    @Override @DB(txn=false)
    public List<T> lock(final SearchCriteria sc, final Filter filter, final boolean exclusive) {
        return search(sc, filter, exclusive, false);
    }

    @Override @DB(txn=false)
    public T lock(final SearchCriteria sc, final boolean exclusive) {
        final Filter filter = new Filter(1);
        final List<T> beans = search(sc, filter, exclusive, true);
        return beans.isEmpty() ? null : beans.get(0);
    }

    @DB(txn=false)
    protected List<T> search(SearchCriteria sc, final Filter filter, final Boolean lock, final boolean cache) {
        if (_removed != null) {
            if (sc == null) {
                sc = createSearchCriteria();
            }
            sc.addAnd(_removed.second().field.getName(), SearchCriteria.Op.NULL);
        }
        return searchAll(sc, filter, lock, cache);
    }

    public List<T> searchAll(SearchCriteria sc, final Filter filter, final Boolean lock, final boolean cache) {
        String clause = sc != null ? sc.getWhereClause() : null;
        if (clause != null && clause.length() == 0) {
        	clause = null;
        }
        
        final StringBuilder str = createPartialSelectSql(sc, clause != null);
        if (clause != null) {
            str.append(clause);
        }

        Collection<Ternary<SearchCriteria, Attribute, Attribute>> joins = null;
        if (sc != null) {
            joins = sc.getJoins();
            if (joins != null) {
                addJoins(str, joins);
            }
        }
        
        addGroupBy(str, sc);
        addFilter(str, filter);

        final Transaction txn = Transaction.currentTxn();
        if (lock != null) {
            assert (txn.dbTxnStarted() == true) : "As nice as I can here now....how do you lock when there's no DB transaction?  Review your db 101 course from college.";
            str.append(lock ? FOR_UPDATE_CLAUSE : SHARE_MODE_CLAUSE);
        }

        final String sql = str.toString();

        PreparedStatement pstmt = s_initStmt;
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
                i = prepareJoins(i, pstmt, joins);
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("join search statement is " + pstmt.toString());
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
            throw new CloudRuntimeException("DB Exception on: " + pstmt.toString(), e);
        } catch (final Throwable e) {
            throw new CloudRuntimeException("Caught: " + pstmt.toString(), e);
        }
    }

    private int prepareJoins(int i, PreparedStatement pstmt, Collection<Ternary<SearchCriteria, Attribute, Attribute>> joins) throws SQLException {
        int j = i;
        for (Ternary<SearchCriteria, Attribute, Attribute> join : joins) {
            for (final Pair<Attribute, Object> value : join.first().getValues()) {
                prepareAttribute(++j, pstmt, value.first(), value.second());
            }
        }

        for (Ternary<SearchCriteria, Attribute, Attribute> join : joins) {
            if (join.first().getJoins() != null) {
                j = prepareJoins(j, pstmt, join.first().getJoins());
            }
        }

        // return the count of attributes just in case someone wants to prepare attributes after the joins are prepared
        return j;
    }

    public List<Object[]> searchAll(SearchCriteria sc, final Filter filter) {
        String clause = sc != null ? sc.getWhereClause() : null;
        if (clause != null && clause.length() == 0) {
            clause = null;
        }
        
        final StringBuilder str = createPartialSelectSql(sc, clause != null);
        if (clause != null) {
            str.append(clause);
        }

        Collection<Ternary<SearchCriteria, Attribute, Attribute>> joins = null;
        if (sc != null) {
            joins = sc.getJoins();
            if (joins != null) {
                addJoins(str, joins);
            }
        }
        
        addGroupBy(str, sc);
        addFilter(str, filter);

        final String sql = str.toString();

        final Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = s_initStmt;
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
                for (Ternary<SearchCriteria, Attribute, Attribute> join : joins) {
                    for (final Pair<Attribute, Object> value : join.first().getValues()) {
                        prepareAttribute(++i, pstmt, value.first(), value.second());
                    }
                }
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("join search statement is " + pstmt.toString());
                }
            }
            ResultSet rs = pstmt.executeQuery();
            ArrayList<Object[]> results = new ArrayList<Object[]>();
            int size = sc.getSelectCount();
            
            while (rs.next()) {
                Object[] objs = new Object[size];
                for (int j = 0; j < size; j++) {
                    objs[j] = rs.getObject(j + 1);
                }
                results.add(objs);
            }
            
            return results;
        } catch (final SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + pstmt.toString(), e);
        } catch (final Throwable e) {
            throw new CloudRuntimeException("Caught: " + pstmt.toString(), e);
        }
    }
    
    @DB(txn=false)
    protected int addJoinAttributes(int count, PreparedStatement pstmt, List<Ternary<SearchCriteria, Attribute, Attribute>> joins) throws SQLException {
        for (Ternary<SearchCriteria, Attribute, Attribute> join : joins) {
            for (final Pair<Attribute, Object> value : join.first().getValues()) {
                prepareAttribute(++count, pstmt, value.first(), value.second());
            }
        }
        for (Ternary<SearchCriteria, Attribute, Attribute> join : joins) {
            if (join.first().getJoins() != null) {
                count = addJoinAttributes(count, pstmt, joins);
            }
        }
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("join search statement is " + pstmt.toString());
        }
        return count;
    }

    @DB(txn=false)
    protected int update(final ID id, final UpdateBuilder ub) {
    	SearchCriteria sc = createSearchCriteria();
    	sc.addAnd(_idAttributes.get(_table)[0], SearchCriteria.Op.EQ, id);
    	int rowsUpdated = update(ub, sc, null);
    	if (_cache != null) {
            _cache.remove(id);
    	}
    	return rowsUpdated;
    }

    //   @Override
    public int update(final UpdateBuilder ub, final SearchCriteria sc, Integer rows) {
        StringBuilder sql = null;
        PreparedStatement pstmt = s_initStmt;
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
            
            for (final Pair<Attribute, Object> value : sc.getValues()) {
            	prepareAttribute(i++, pstmt, value.first(), value.second());
            }
            
            int result = pstmt.executeUpdate();
            txn.commit();
            ub.clear();
            return result;
        } catch (final SQLException e) {
            final String sqlStr = pstmt.toString();
            throw new CloudRuntimeException("DB Exception on: " + sqlStr, e);
        } catch (final Throwable e) {
            final String sqlStr = pstmt.toString();
            throw new CloudRuntimeException("Caught: " + sqlStr, e);
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
    public Class<T> getEntityBeanType() {
        return _entityBeanType;
    }

    protected T findOneBy(final SearchCriteria sc) {
        Filter filter = new Filter(1);
        List<T> results = searchAll(sc, filter, null, false);
        assert results.size() <= 1 : "Didn't the limiting worked?";
        return results.size() == 0 ? null : results.get(0);
    }

    @DB(txn=false)
    protected T findOneActiveBy(final SearchCriteria sc) {
        if (_removed != null) {
            sc.addAnd(_removed.second().field.getName(), SearchCriteria.Op.NULL);
        }
        return findOneBy(sc);
    }

    @DB(txn=false)
    protected List<T> listActiveBy(final SearchCriteria sc, final Filter filter) {
        if (_removed != null) {
            sc.addAnd(_removed.second().field.getName(), SearchCriteria.Op.NULL);
        }
        return listBy(sc, filter);
    }

    @DB(txn=false)
    protected List<T> listActiveBy(final SearchCriteria sc) {
        return listActiveBy(sc, null);
    }

    @DB(txn=false)
    protected List<T> listBy(final SearchCriteria sc, final Filter filter) {
        return searchAll(sc, filter, null, false);
    }

    @DB(txn=false)
    protected List<T> listBy(final SearchCriteria sc) {
        return listBy(sc, null);
    }

    @Override @DB(txn=false)
    @SuppressWarnings("unchecked")
    public T findById(final ID id) {
        if (_cache != null) {
            final Element element = _cache.get(id);
            return element == null ? lock(id, null) : (T)element.getObjectValue();
        } else {
            return lock(id, null);
        }
    }
    
    @Override @DB(txn=false)
    @SuppressWarnings("unchecked")
    public T findById(final ID id, boolean fresh) {
    	if(!fresh)
    		return findById(id);
    	
        if (_cache != null) {
        	_cache.remove(id);
        }
        return lock(id, null);
    }
    
    @Override
    public T lock(ID id, Boolean lock) {
        String sql = _selectByIdSql;
        if (lock != null) {
            sql += lock ? FOR_UPDATE_CLAUSE : SHARE_MODE_CLAUSE;
        }
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = s_initStmt;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);

            if (_idField.getAnnotation(EmbeddedId.class) == null) {
                pstmt.setObject(1, id);
            }
            
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? toEntityBean(rs, true) : null;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + pstmt.toString(), e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + pstmt.toString(), e);
        }
    }

    @Override @DB(txn=false)
    public T acquire(ID id) {
    	return acquire(id, _timeoutSeconds);
    }
    
    public T acquire(final ID id, int seconds) {
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
    public boolean release(final ID id) {
        final Transaction txn = Transaction.currentTxn();
    	return txn.release(_table + id);
    }

    @Override @DB(txn=false)
    public List<T> listAll() {
        return listAll(null);
    }

    @DB(txn=false)
    protected void addGroupBy(final StringBuilder sql, SearchCriteria sc) {
    	List<Attribute> groupBys = sc.getGroupBy();
    	if(groupBys != null) {
    		sql.append(" GROUP BY ");
    		for(int i = 0; i < groupBys.size(); i++) {
    			Attribute attr = groupBys.get(i);
    			sql.append(attr.table).append(".").append(attr.columnName);
    			if(i < groupBys.size() - 1)
    				sql.append(", ");
    		}
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
    public List<T> listAll(final Filter filter) {
        final StringBuilder sql = createPartialSelectSql(null, false);
        addFilter(sql, filter);

        return executeList(sql.toString());
    }

    protected List<T> executeList(final String sql, final Object... params) {
        final Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = s_initStmt;
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
            throw new CloudRuntimeException("DB Exception on: " + pstmt.toString(), e);
        } catch (final Throwable e) {
            throw new CloudRuntimeException("Caught: " + pstmt.toString(), e);
        }
    }

    @Override @DB(txn=false)
    public List<T> listAllActive() {
        return listAllActive(null);
    }

    @Override @DB(txn=false)
    public List<T> listAllActive(final Filter filter) {
        if (_removed == null) {
            return listAll(filter);
        }

        final StringBuilder sql = createPartialSelectSql(null, true);
        sql.append(_removed.first());
        addFilter(sql, filter);

        return executeList(sql.toString());
    }

    @Override
    public boolean delete(final ID id) {
        final Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = s_initStmt;
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
            final String sqlStr = pstmt.toString();
            throw new CloudRuntimeException("DB Exception on: " + sqlStr, e);
        }
    }

    // FIXME: Does not work for joins.
    public int delete(final SearchCriteria sc) {
        final StringBuilder str = new StringBuilder("DELETE FROM ");
        str.append(_table);
        str.append(" WHERE ");

        if (sc != null && sc.getWhereClause().length() > 0) {
            str.append(sc.getWhereClause());
        }

        final String sql = str.toString();

        final Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = s_initStmt;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            int i = 0;
            for (final Pair<Attribute, Object> value : sc.getValues()) {
            	prepareAttribute(++i, pstmt, value.first(), value.second());
            }
            return pstmt.executeUpdate();
        } catch (final SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + pstmt.toString(), e);
        } catch (final Throwable e) {
            throw new CloudRuntimeException("Caught: " + pstmt.toString(), e);
        }
    }

    @DB(txn=false)
    protected StringBuilder createPartialSelectSql(SearchCriteria sc, final boolean whereClause) {
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
    
    @DB(txn=false)
    protected void addJoins(StringBuilder str, Collection<Ternary<SearchCriteria, Attribute, Attribute>> joins) {
        int fromIndex = str.lastIndexOf("FROM") + 5;
        if (str.lastIndexOf("WHERE") == -1) {
            str.append(" WHERE ");
        } else {
            str.append(" AND ");
        }
        
        for (Ternary<SearchCriteria, Attribute, Attribute> join : joins) {
            str.insert(fromIndex, join.third().table + ", ");
            str.append(join.second().table).append(".").append(join.second().columnName).append("=").append(join.third().table).append(".").append(join.third().columnName);
            String whereClause = join.first().getWhereClause();
            if (whereClause != null && whereClause.length() > 0) {
                str.append(" AND (").append(whereClause).append(") ");
            }
            str.append("AND ");
        }

        str.delete(str.length() - 4, str.length());

        for (Ternary<SearchCriteria, Attribute, Attribute> join : joins) {
            if (join.first().getJoins() != null) {
                addJoins(str, join.first().getJoins());
            }
        }
    }

    @Override @DB(txn=false)
    public List<T> search(final SearchCriteria sc, final Filter filter) {
        return search(sc, filter, null, false);
    }

    @Override @DB(txn=false)
    public boolean update(final ID id, final T entity) {
        assert Enhancer.isEnhanced(entity.getClass()) : "Entity is not generated by this dao";

        final UpdateBuilder ub = getUpdateBuilder(entity);
        final boolean result = update(id, ub) != 0;
        return result;
    }

    @DB(txn=false)
    public int update(final T entity, final SearchCriteria sc, Integer rows) {
        final UpdateBuilder ub = getUpdateBuilder(entity);
        return update(ub, sc, rows);
    }
    
    @DB(txn=false)
    public int update(final T entity, final SearchCriteria sc) {
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
        PreparedStatement pstmt = s_initStmt;
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
            txn.commit();
        } catch (final SQLException e) {
            final String sqlStr = pstmt.toString();
            throw new CloudRuntimeException("DB Exception on: " + sqlStr, e);
        }
        
        return _idField != null ? findById(id) : null;
    }

    @DB(txn=false)
    protected Object generateValue(final Attribute attr) {
        if (attr.is(Attribute.Flag.Created) || attr.is(Attribute.Flag.Removed)) {
            return new Date();
        } else if (attr.is(Attribute.Flag.TableGV)) {
            return null;
            // Not sure what to do here.
        } else if (attr.is(Attribute.Flag.AutoGV)) {
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

            if (length < str.length()) {
                pstmt.setString(j, str.substring(0, column.length()));
            } else {
                pstmt.setString(j, str);
            }
        } else if (attr.field.getType() == Date.class) {
            final Date date = (Date)value;
            if (date == null) {
                pstmt.setObject(j, null);
                return;
            }
            if (attr.is(Attribute.Flag.Date)) {
                pstmt.setString(j, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), date));
            } else if (attr.is(Attribute.Flag.TimeStamp)) {
                pstmt.setString(j, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), date));
            } else if (attr.is(Attribute.Flag.Time)) {
                pstmt.setString(j, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), date));
            }
        } else if (attr.field.getType() == Calendar.class) {
            final Calendar cal = (Calendar)value;
            if (cal == null) {
                pstmt.setObject(j, null);
                return;
            }
            if (attr.is(Attribute.Flag.Date)) {
                pstmt.setString(j, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cal.getTime()));
            } else if (attr.is(Attribute.Flag.TimeStamp)) {
                pstmt.setString(j, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cal.getTime()));
            } else if (attr.is(Attribute.Flag.Time)) {
                pstmt.setString(j, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cal.getTime()));
            }
        } else if (attr.field.getType().isEnum()) {
            final Enumerated enumerated = attr.field.getType().getAnnotation(Enumerated.class);
            final EnumType type = (enumerated == null) ? EnumType.STRING : enumerated.value();
            if (type == EnumType.STRING) {
                pstmt.setString(j, value == null ? null :  value.toString());
            } else if (type == EnumType.ORDINAL) {
                pstmt.setInt(j, value == null ? null : ((Enum)value).ordinal());
            }
        } else if (attr.field.getType() == byte[].class) {
            pstmt.setBytes(j, (byte[])value);
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
        final T entity = (T)_factory.newInstance(new UpdateBuilder(_allAttributes));

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
    }

    @Override
    public void expunge() {
        if (_removed == null) {
            return;
        }
        final StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(_table).append(" WHERE ").append(_removed.first()).append(" IS NOT NULL");
        final Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = s_initStmt;
        try {
            txn.start();
            pstmt = txn.prepareAutoCloseStatement(sql.toString());

            pstmt.executeUpdate();
            txn.commit();
        } catch (final SQLException e) {
            final String sqlStr = pstmt.toString();
            throw new CloudRuntimeException("DB Exception on " + sqlStr, e);
        }
    }
    
    @DB(txn=false)
    protected void setField(final Object entity, final ResultSet rs, ResultSetMetaData meta, final int index) throws SQLException {
        Attribute attr = _allColumns.get(new Pair<String, String>(meta.getTableName(index), meta.getColumnName(index)));
        assert (attr != null) : "How come I can't find " + meta.getCatalogName(index) + "." + meta.getColumnName(index);
        try {
            final Class<?> type = attr.field.getType();
            if (type == String.class) {
                attr.field.set(entity, rs.getString(index));
            } else if (type == int.class) {
                attr.field.set(entity, rs.getInt(index));
            } else if (type == Integer.class) {
                if (rs.getObject(index) == null) {
                    attr.field.set(entity, null);
                } else {
                    attr.field.set(entity, rs.getInt(index));
                }
            } else if (type == long.class) {
                attr.field.setLong(entity, rs.getLong(index));
            } else if (type == Long.class) {
                if (rs.getObject(index) == null) {
                    attr.field.set(entity, null);
                } else {
                    attr.field.set(entity, rs.getLong(index));
                }
            } else if (type == Date.class) {
                final Object data = rs.getDate(index);
                if (data == null) {
                    attr.field.set(entity, null);
                    return;
                }
                if (attr.is(Attribute.Flag.Date)) {
                    attr.field.set(entity, DateUtil.parseDateString(s_gmtTimeZone, rs.getString(index)));
                } else if (attr.is(Attribute.Flag.TimeStamp)) {
            		attr.field.set(entity, DateUtil.parseDateString(s_gmtTimeZone, rs.getString(index)));
                } else if (attr.is(Attribute.Flag.Time)) {
                    attr.field.set(entity, DateUtil.parseDateString(s_gmtTimeZone, rs.getString(index)));
                }
            } else if (type == short.class) {
                attr.field.setShort(entity, rs.getShort(index));
            } else if (type == Short.class) {
                if (rs.getObject(index) == null) {
                    attr.field.set(entity, null);
                } else {
                    attr.field.set(entity, rs.getShort(index));
                }
            } else if (type == boolean.class) {
                attr.field.setBoolean(entity, rs.getBoolean(index));
            } else if (type == Boolean.class) {
                if (rs.getObject(index) == null) {
                    attr.field.set(entity, null);
                } else {
                    attr.field.set(entity, rs.getBoolean(index));
                }
            } else if (type == float.class) {
                attr.field.setFloat(entity, rs.getFloat(index));
            } else if (type == Float.class) {
                if (rs.getObject(index) == null) {
                    attr.field.set(entity, null);
                } else {
                    attr.field.set(entity, rs.getFloat(index));
                }
            } else if (type == double.class) {
                attr.field.setDouble(entity, rs.getDouble(index));
            } else if (type == Double.class) {
                if (rs.getObject(index) == null) {
                    attr.field.set(entity, null);
                } else {
                    attr.field.set(entity, rs.getDouble(index));
                }
            } else if (type == byte.class) {
                attr.field.setByte(entity, rs.getByte(index));
            } else if (type == Byte.class) {
                if (rs.getObject(index) == null) {
                    attr.field.set(entity, null);
                } else {
                    attr.field.set(entity, rs.getByte(index));
                }
            } else if (type == Calendar.class) {
                final Object data = rs.getDate(index);
                if (data == null) {
                    attr.field.set(entity, null);
                    return;
                }
                final Calendar cal = Calendar.getInstance();
                if (attr.is(Attribute.Flag.Date)) {
                    cal.setTime(DateUtil.parseDateString(s_gmtTimeZone, rs.getString(index)));
                } else if (attr.is(Attribute.Flag.TimeStamp)) {
                    cal.setTime(DateUtil.parseDateString(s_gmtTimeZone, rs.getString(index)));
                } else if (attr.is(Attribute.Flag.Time)) {
                    cal.setTime(DateUtil.parseDateString(s_gmtTimeZone, rs.getString(index)));
                }
                attr.field.set(entity, cal);
            } else if (type.isEnum()) {
                final Enumerated enumerated = attr.field.getAnnotation(Enumerated.class);
                final EnumType enumType = (enumerated == null) ? EnumType.STRING : enumerated.value();

                final Enum[] enums =  (Enum[])attr.field.getType().getEnumConstants();
                for (final Enum e : enums) {
                    if ((enumType == EnumType.STRING && e.name().equalsIgnoreCase(rs.getString(index))) ||
                            (enumType == EnumType.ORDINAL && e.ordinal() == rs.getInt(index))) {
                        attr.field.set(entity, e);
                        return;
                    }
                }
            } else if (type == byte[].class) {
                attr.field.set(entity, rs.getBytes(index));
            } else {
                attr.field.set(entity, rs.getObject(index));
            }
        } catch (final IllegalAccessException e) {
            throw new CloudRuntimeException("Yikes! ", e);
        }
    }

    @Override
    public boolean remove(final ID id) {
        if (_removeSql == null) {
            return delete(id);
        }

        final Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = s_initStmt;
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
            final String sqlStr = pstmt.toString();
            throw new CloudRuntimeException("DB Exception on: " + sqlStr, e);
        }
    }

    @Override
    public int remove(SearchCriteria sc) {
        if (_removeSql == null) {
            return delete(sc);
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

        final String value = (String)params.get("lock.timeout");
        _timeoutSeconds = NumbersUtil.parseInt(value, 300);

        createCache(params);
        final boolean load = Boolean.parseBoolean((String)params.get("cache.preload"));
        if (load) {
            listAllActive();
        }

        return true;
    }

    @DB(txn=false)
    public String getName() {
        return _name;
    }
    
    @DB(txn=false)
    public static <T> UpdateBuilder getUpdateBuilder(final T entityObject) {
        final Factory factory = (Factory)entityObject;
        assert(factory != null);
        return (UpdateBuilder)factory.getCallback(0);
    }
    
    @SuppressWarnings("unchecked")
    @Override @DB(txn=false)
    public SearchBuilder<T> createSearchBuilder() {
        final T entity = (T)_enhancer.create();
        final Factory factory = (Factory)entity;
        SearchBuilder<T> builder = new SearchBuilder<T>(entity, _allAttributes);
        factory.setCallback(0, builder);
        return builder;
    }
    
    @Override @DB(txn=false)
    public SearchCriteria createSearchCriteria() {
    	SearchBuilder<T> builder = createSearchBuilder();
    	return builder.create();
    }
}
