/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.persist;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import com.cloud.bridge.util.QueryHelper;

/**
 * @author Kelven Yang, John Zucker
 * Provide methods for getting, saving, deleting or updating state per session or, in a given session, returnin a List in
 * response to queryEntities for a particular instantation of the EntityDao generic class, as defined here.
 * Any instantation of EntityDao passes in the class for which it is instantiating.  For example a new instance of SBucketDao 
 * passes in com.cloud.bridge.model.SBucket as its clazz.
 */

public class EntityDao<T> {
	private Class<?> clazz;
	
	public EntityDao(Class<?> clazz) {
		this.clazz = clazz;
		
		// Note : beginTransaction can be called multiple times
		PersistContext.beginTransaction();
	}
	
	@SuppressWarnings("unchecked")
	public T get(Serializable id) {
		Session session = PersistContext.getSession();
		return (T)session.get(clazz, id);
	}
	
	public T save(T entity) {
		Session session = PersistContext.getSession();
		session.saveOrUpdate(entity);
		return entity;
	}
	
	public T update(T entity) {
		Session session = PersistContext.getSession();
		session.saveOrUpdate(entity);
		return entity;
	}
	
	public void delete(T entity) {
		Session session = PersistContext.getSession();
		session.delete(entity);
	}
	
	public T queryEntity(String hql, Object[] params) {
		Session session = PersistContext.getSession();
		Query query = session.createQuery(hql);
		query.setMaxResults(1);
		QueryHelper.bindParameters(query, params);
		return (T)query.uniqueResult();
	}
	
	public List<T> queryEntities(String hql, Object[] params) {
		Session session = PersistContext.getSession();
		Query query = session.createQuery(hql);
		QueryHelper.bindParameters(query, params);
		
		return (List<T>)query.list();
	}
	
	public List<T> queryEntities(String hql, int offset, int limit, Object[] params) {
		Session session = PersistContext.getSession();
		Query query = session.createQuery(hql);
		QueryHelper.bindParameters(query, params);
		query.setFirstResult(offset);
		query.setMaxResults(limit);
		return (List<T>)query.list();
	}
	
	public int executeUpdate(String hql, Object[] params) {
		Session session = PersistContext.getSession();
		Query query = session.createQuery(hql);
		QueryHelper.bindParameters(query, params);

		return query.executeUpdate();
	}
}
