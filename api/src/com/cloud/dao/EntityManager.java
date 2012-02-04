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

import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

/**
 * Generic Entity Manager to retrieve database objects.
 *
 */
public interface EntityManager {
    /**
     * Finds an entity by its id.
     * @param <T> class of the entity you're trying to find.
     * @param <K> class of the id that the entity uses.
     * @param entityType Type of the entity.
     * @param id id value
     * @return T if found; null if not.
     */
    public <T, K extends Serializable> T findById(Class<T> entityType, K id);
    
    /**
     * Finds an entity by external id which is always String
     * @param <T> entity class
     * @param entityType type of entity you're looking for.
     * @param xid external id
     * @return T if found, null if not.
     */
    public <T> T findByXid(Class<T> entityType, String xid);
    
    /**
     * Lists all entities.  Use this method at your own risk.
     * @param <T> entity class
     * @param entityType type of entity you're looking for.
     * @return List<T>
     */
    public <T> List<? extends T> list(Class<T> entityType);
    
    /**
     * Persists the entity.
     * @param <T> entity class
     * @param t entity
     * @return persisted entity.  Only use this after persisting.
     */
    public <T> T persist(T t);
    
    public <T> SearchBuilder<T> createSearchBuilder(Class<T> entityType);
    
    public <T, K> GenericSearchBuilder<T, K> createGenericSearchBuilder(Class<T> entityType, Class<K> resultType);
    
    public <T, K> List<K> search(Class<T> entityType, SearchCriteria<K> sc);
    
    public <T, K extends Serializable> void remove(Class<T> entityType, K id);
}

