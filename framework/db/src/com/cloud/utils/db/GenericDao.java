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
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.utils.Pair;

/**
 * a uniform method for persisting and finding db entities.
 **/
public interface GenericDao<T, ID extends Serializable> {

    /**
     */
    static final String REMOVED_COLUMN = "removed";

    /**
     * This column can be used if the table wants to track creation time.
     */
    static final String CREATED_COLUMN = "created";

    /**
     */
    static final String XID_COLUMN = "xid";

    /**
     * Look for an entity bean using the database id.  Does not lock the row.
     * @param id database unique id for the entity bean.
     * @return entity bean.
     **/
    T findById(ID id);

    T findByIdIncludingRemoved(ID id);

    T findById(ID id, boolean fresh);

    // Finds one unique VO using uuid
    T findByUuid(String uuid);

    // Finds one unique VO using uuid including removed entities
    T findByUuidIncludingRemoved(String uuid);

    /**
     * @return VO object ready to be used for update.  It won't have any fields filled in.
     */
    T createForUpdate();

    SearchBuilder<T> createSearchBuilder();

    <K> GenericSearchBuilder<T, K> createSearchBuilder(Class<K> clazz);

    T createForUpdate(ID id);

    /**
     * Returns a SearchCriteria object that can be used to build search conditions.
     *
     * @return SearchCriteria
     */
    SearchCriteria<T> createSearchCriteria();

    /**
     * lock the rows that matched the search criteria and filter.  This method needs
     * to be called within a transaction.
     *
     * @param sc SearchCriteria containing the different search conditions
     * @param filter Filter containing limits etc
     * @param exclusive exclusive or share lock
     * @return List<T> list of entity beans
     */
    List<T> lockRows(SearchCriteria<T> sc, Filter filter, boolean exclusive);

    /**
     * lock 1 of the return set.  This method needs to be run within a
     * transaction or else it's useless.
     * @param sc
     * @param exclusive
     * @return T if found and locked.  null if not.
     */
    T lockOneRandomRow(SearchCriteria<T> sc, boolean exclusive);

    /**
     * Find and lock the row for update.
     * @param id id
     * @param exclusive is this a read share lock or exclusive lock?
     * @return T
     */
    T lockRow(ID id, Boolean exclusive);

    /**
     * Acquires a database wide lock on the id of the entity.  This ensures
     * that only one is being used.  The timeout is the configured default.
     *
     * @param id id of the entity to acquire an lock on.
     * @return object if acquired; null if not.  If null, you need to call findById to see if it is actually not found.
     */
    T acquireInLockTable(ID id);

    /**
     * Acquires a database wide lock on the id of the entity.  This ensures
     * that only one is being used.  The timeout is the configured default.
     *
     * @param id id of the entity to acquire an lock on.
     * @param seconds time to wait for the lock.
     * @return entity if the lock is acquired; null if not.
     */
    T acquireInLockTable(ID id, int seconds);

    /**
     * releases the lock acquired in the acquire method call.
     * @param id id of the entity to release the lock on.
     * @return true if it is released.  false if not or not found.
     */
    boolean releaseFromLockTable(final ID id);

    boolean update(ID id, T entity);

    int update(T entity, SearchCriteria<T> sc);

    /**
     * Look for all active rows.
     * @return list of entity beans.
     */
    List<T> listAll();

    /**
     * Look for all active rows.
     * @param filter filter to limit the results
     * @return list of entity beans.
     */
    List<T> listAll(Filter filter);

    /**
     * Search for the entity beans
     * @param sc
     * @param filter
     * @return list of entity beans.
     */
    List<T> search(SearchCriteria<T> sc, Filter filter);

    /**
     * Search for the entity beans using the sql SQL_CACHE option
     * @param sc
     * @param filter
     * @param enableQueryCache
     * @return list of entity beans.
     */
    List<T> search(SearchCriteria<T> sc, Filter filter, final boolean enableQueryCache);

    List<T> searchIncludingRemoved(SearchCriteria<T> sc, final Filter filter, final Boolean lock, final boolean cache);

    List<T> searchIncludingRemoved(SearchCriteria<T> sc, final Filter filter, final Boolean lock, final boolean cache, final boolean enableQueryCache);

    /**
     * Customized search with SearchCritiria
     * @param sc
     * @param filter
     * @return list of entity beans.
     */
    public <M> List<M> customSearchIncludingRemoved(SearchCriteria<M> sc, Filter filter);

    /**
     * Retrieves the entire table.
     * @return collection of entity beans.
     **/
    List<T> listAllIncludingRemoved();

    /**
     * Retrieves the entire table.
     * @param filter filter to limit the returns.
     * @return collection of entity beans.
     **/
    List<T> listAllIncludingRemoved(Filter filter);

    /**
     * Persist the entity bean.  The id field of the entity is updated with
     * the new id.
     * @param entity the bean to persist.
     * @return The persisted version of the object.  A null is returned if
     * there's no primary key specified in the VO object.
     **/
    T persist(T entity);

    /**
     * remove the entity bean.  This will call delete automatically if
     * the entity bean does not have a removed field.
     * @param id
     * @return true if removed.
     */
    boolean remove(ID id);

    /**
     * Remove based on the search criteria.  This will delete if the VO object
     * does not have a REMOVED column.
     * @param sc search criteria to match
     * @return rows removed.
     */
    int remove(SearchCriteria<T> sc);

    /**
     * Expunge actually delete the row even if it's REMOVED.
     * @param id
     * @return true if removed.
     */
    boolean expunge(ID id);

    /**
     * remove the entity bean specified by the search criteria
     * @param sc
     * @return number of rows deleted
     */
    int expunge(final SearchCriteria<T> sc);

    /**
     * expunge the removed rows.
     */
    void expunge();

    public <K> K getNextInSequence(Class<K> clazz, String name);

    /**
     * Configure.
     * @param name name of the dao.
     * @param params params if any are specified.
     * @return true if config is good.  false if not.
     */
    boolean configure(String name, Map<String, Object> params) throws ConfigurationException;

    <M> List<M> customSearch(SearchCriteria<M> sc, Filter filter);

    boolean lockInLockTable(String id);

    boolean lockInLockTable(String id, int seconds);

    boolean unlockFromLockTable(String id);

    public <K> K getRandomlyIncreasingNextInSequence(Class<K> clazz, String name);

    public T findOneBy(final SearchCriteria<T> sc);

    /**
     * @return
     */
    Class<T> getEntityBeanType();

    /**
     * @param sc
     * @param filter
     * @return
     */
    Pair<List<T>, Integer> searchAndCount(SearchCriteria<T> sc, Filter filter);

    /**
     * @param sc
     * @param filter
     * @return
     */
    Pair<List<T>, Integer> searchAndDistinctCount(final SearchCriteria<T> sc, final Filter filter);

    Map<String, Attribute> getAllAttributes();
}
