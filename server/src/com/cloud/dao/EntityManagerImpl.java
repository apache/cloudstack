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
package com.cloud.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.utils.component.Manager;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value=EntityManager.class)
@SuppressWarnings("unchecked")
public class EntityManagerImpl implements EntityManager, Manager {
    String _name;
    
    @Override
    public <T, K extends Serializable> T findById(Class<T> entityType, K id) {
        GenericDao<? extends T, K> dao = (GenericDao<? extends T, K>)GenericDaoBase.getDao(entityType);
        return dao.findById(id);
    }

    @Override
    public <T> T findByXid(Class<T> entityType, String xid) {
        return null;
    }

    @Override
    public <T> List<? extends T> list(Class<T> entityType) {
        GenericDao<? extends T, ? extends Serializable> dao = GenericDaoBase.getDao(entityType);
        return dao.listAll();
    }

    @Override
    public <T> T persist(T t) {
        GenericDao<T, ? extends Serializable> dao = (GenericDao<T, ? extends Serializable>)GenericDaoBase.getDao((Class<T>)t.getClass());
        return dao.persist(t);
    }

    @Override
    public <T> SearchBuilder<T> createSearchBuilder(Class<T> entityType) {
        GenericDao<T, ? extends Serializable> dao = (GenericDao<T, ? extends Serializable>)GenericDaoBase.getDao(entityType);
        return dao.createSearchBuilder();
    }

    @Override
    public <T, K> GenericSearchBuilder<T, K> createGenericSearchBuilder(Class<T> entityType, Class<K> resultType) {
        GenericDao<T, ? extends Serializable> dao = (GenericDao<T, ? extends Serializable>)GenericDaoBase.getDao(entityType);
        return dao.createSearchBuilder((Class<K>)resultType.getClass());
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public <T, K> List<K> search(Class<T> entityType, SearchCriteria<K> sc) {
        GenericDao<T, ? extends Serializable> dao = (GenericDao<T, ? extends Serializable>)GenericDaoBase.getDao(entityType);
        return dao.customSearch(sc, null);
    }

    @Override
    public <T, K extends Serializable> void remove(Class<T> entityType, K id) {
        GenericDao<T, K> dao = (GenericDao<T, K>)GenericDaoBase.getDao(entityType);
        dao.remove(id);
    }

}
