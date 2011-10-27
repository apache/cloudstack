package com.cloud.utils.db;

import java.util.List;

import com.cloud.utils.db.SearchCriteria.Op;

public interface SearchCriteriaService<T, K> {
	public void selectField(Object... useless);
	public void addAnd(Object useless, Op op, Object...values);
	public List<K> list();
	public T getEntity();
	public <K> K find();
}
