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
package com.cloud.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import net.sf.ehcache.Cache;

@SuppressWarnings("unchecked")
public class EntityManagerImpl extends ManagerBase implements EntityManager {
    String _name;
    Cache _cache;

    @Override
    public <T, K extends Serializable> T findById(Class<T> entityType, K id) {
        GenericDao<? extends T, K> dao = (GenericDao<? extends T, K>)GenericDaoBase.getDao(entityType);
        return dao.findById(id);
    }

    @Override
    public <T, K extends Serializable> T findByIdIncludingRemoved(Class<T> entityType, K id) {
        GenericDao<? extends T, K> dao = (GenericDao<? extends T, K>)GenericDaoBase.getDao(entityType);
        return dao.findByIdIncludingRemoved(id);
    }

    @Override
    public <T> T findByUuid(Class<T> entityType, String uuid) {
        // Finds and returns a unique VO using uuid, null if entity not found in db
        GenericDao<? extends T, String> dao = (GenericDao<? extends T, String>)GenericDaoBase.getDao(entityType);
        return dao.findByUuid(uuid);
    }

    @Override
    public <T> T findByUuidIncludingRemoved(Class<T> entityType, String uuid) {
        // Finds and returns a unique VO using uuid, null if entity not found in db
        GenericDao<? extends T, String> dao = (GenericDao<? extends T, String>)GenericDaoBase.getDao(entityType);
        return dao.findByUuidIncludingRemoved(uuid);
    }

    @Override
    public <T> T findByXId(Class<T> entityType, String xid) {
        return null;
    }

    @Override
    public <T> List<? extends T> list(Class<T> entityType) {
        GenericDao<? extends T, ? extends Serializable> dao = GenericDaoBase.getDao(entityType);
        return dao.listAll();
    }

    public <T> T persist(T t) {
        GenericDao<T, ? extends Serializable> dao = (GenericDao<T, ? extends Serializable>)GenericDaoBase.getDao((Class<T>)t.getClass());
        return dao.persist(t);
    }

    public <T> SearchBuilder<T> createSearchBuilder(Class<T> entityType) {
        GenericDao<T, ? extends Serializable> dao = (GenericDao<T, ? extends Serializable>)GenericDaoBase.getDao(entityType);
        return dao.createSearchBuilder();
    }

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

    public <T, K> List<K> search(Class<T> entityType, SearchCriteria<K> sc) {
        GenericDao<T, ? extends Serializable> dao = (GenericDao<T, ? extends Serializable>)GenericDaoBase.getDao(entityType);
        return dao.customSearch(sc, null);
    }

    @Override
    public <T, K extends Serializable> void remove(Class<T> entityType, K id) {
        GenericDao<T, K> dao = (GenericDao<T, K>)GenericDaoBase.getDao(entityType);
        dao.remove(id);
    }

    @Override
    public <T> boolean validEntityType(Class<T> entityType) {
        GenericDao<T, ? extends Serializable> dao = (GenericDao<T, ? extends Serializable>)GenericDaoBase.getDao(entityType);
        return dao != null;
    }
}
