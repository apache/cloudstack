package com.cloud.utils.db;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class SearchCriteria2<T, K> extends SearchCriteria<K> {
	GenericSearchBuilder<?, K> _sb;
	GenericDao<? extends Serializable, ? extends Serializable> _dao;
	
	protected SearchCriteria2(GenericSearchBuilder<?, K> sb, GenericDao<? extends Serializable, ? extends Serializable> dao) {
	    super(sb);
	    this._sb = sb;
	    this._dao = dao;
    }
	
	static public <T, K> SearchCriteria2<T, K> create(Class<T> entityType) {
		GenericDao<? extends Serializable, ? extends Serializable> dao = (GenericDao<? extends Serializable, ? extends Serializable>)GenericDaoBase.getDao(entityType);
		assert dao != null : "Can not find DAO for " + entityType.getName();
		SearchBuilder<T> sb = (SearchBuilder<T>) dao.createSearchBuilder();
		SearchCriteria2<T, K> sc = new SearchCriteria2(sb, dao);
		return (SearchCriteria2<T, K>) sc;
	}
		
	public void selectField(Object... useless) {
		_sb.selectField(useless);
	}
	
	public void addAnd(Object useless, Op op, Object...values) {
		String uuid = UUID.randomUUID().toString();
		_sb.and(uuid, null, op);
		this.setParameters(uuid, values);
	}
	
	public List<K> list() {
		return (List<K>)_dao.search((SearchCriteria)this, null);
	}
	
	public T getEntity() {
		return (T) _sb.entity(); 
	}
}
