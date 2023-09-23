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
package com.cloud.user;

import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MockUsageEventDao implements UsageEventDao{

    List<UsageEventVO> persistedItems;

    public MockUsageEventDao() {
        persistedItems = new ArrayList<UsageEventVO>();
    }

    @Override
    public UsageEventVO findById(Long id) {
        return null;
    }

    @Override
    public UsageEventVO findByIdIncludingRemoved(Long id) {
        return null;
    }

    @Override
    public UsageEventVO findById(Long id, boolean fresh) {
        return null;
    }

    @Override
    public UsageEventVO findByUuid(String uuid) {
        return null;
    }

    @Override
    public UsageEventVO findByUuidIncludingRemoved(String uuid) {
        return null;
    }

    @Override
    public UsageEventVO createForUpdate() {
        return null;
    }

    @Override
    public SearchBuilder<UsageEventVO> createSearchBuilder() {
        return null;
    }

    @Override
    public <K> GenericSearchBuilder<UsageEventVO, K> createSearchBuilder(
            Class<K> clazz) {
        return null;
    }

    @Override
    public UsageEventVO createForUpdate(Long id) {
        return null;
    }

    @Override
    public SearchCriteria<UsageEventVO> createSearchCriteria() {
        return null;
    }

    @Override
    public List<UsageEventVO> lockRows(SearchCriteria<UsageEventVO> sc,
            Filter filter, boolean exclusive) {
        return null;
    }

    @Override
    public UsageEventVO lockOneRandomRow(SearchCriteria<UsageEventVO> sc,
            boolean exclusive) {
        return null;
    }

    @Override
    public UsageEventVO lockRow(Long id, Boolean exclusive) {
        return null;
    }

    @Override
    public UsageEventVO acquireInLockTable(Long id) {
        return null;
    }

    @Override
    public UsageEventVO acquireInLockTable(Long id, int seconds) {
        return null;
    }

    @Override
    public boolean releaseFromLockTable(Long id) {
        return false;
    }

    @Override
    public boolean update(Long id, UsageEventVO entity) {
        return false;
    }

    @Override
    public int update(UsageEventVO entity, SearchCriteria<UsageEventVO> sc) {
        return 0;
    }

    @Override
    public List<UsageEventVO> listAll() {
        return persistedItems;
    }

    @Override
    public List<UsageEventVO> listAll(Filter filter) {
        return null;
    }

    @Override
    public List<UsageEventVO> search(SearchCriteria<UsageEventVO> sc,
            Filter filter) {
        return null;
    }

    @Override
    public List<UsageEventVO> search(SearchCriteria<UsageEventVO> sc,
            Filter filter, boolean enableQueryCache) {
        return null;
    }

    @Override
    public List<UsageEventVO> searchIncludingRemoved(
            SearchCriteria<UsageEventVO> sc, Filter filter, Boolean lock,
            boolean cache) {
        return null;
    }

    @Override
    public List<UsageEventVO> searchIncludingRemoved(
            SearchCriteria<UsageEventVO> sc, Filter filter, Boolean lock,
            boolean cache, boolean enableQueryCache) {
        return null;
    }

    @Override
    public <M> List<M> customSearchIncludingRemoved(SearchCriteria<M> sc,
            Filter filter) {
        return null;
    }

    @Override
    public List<UsageEventVO> listAllIncludingRemoved() {
        return null;
    }

    @Override
    public List<UsageEventVO> listAllIncludingRemoved(Filter filter) {
        return null;
    }

    @Override
    public UsageEventVO persist(UsageEventVO entity) {
        persistedItems.add(entity);
        return entity;
    }

    @Override
    public boolean remove(Long id) {
        return false;
    }

    @Override
    public int remove(SearchCriteria<UsageEventVO> sc) {
        return 0;
    }

    @Override
    public boolean expunge(Long id) {
        return false;
    }

    @Override
    public int expunge(SearchCriteria<UsageEventVO> sc) {
        return 0;
    }

    @Override
    public void expunge() {

    }

    @Override
    public boolean unremove(Long id) {
        return false;
    }

    @Override
    public <K> K getNextInSequence(Class<K> clazz, String name) {
        return null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        return false;
    }

    @Override
    public <M> List<M> customSearch(SearchCriteria<M> sc, Filter filter) {
        return null;
    }

    @Override
    public boolean lockInLockTable(String id) {
        return false;
    }

    @Override
    public boolean lockInLockTable(String id, int seconds) {
        return false;
    }

    @Override
    public boolean unlockFromLockTable(String id) {
        return false;
    }

    @Override
    public <K> K getRandomlyIncreasingNextInSequence(Class<K> clazz, String name) {
        return null;
    }

    @Override
    public UsageEventVO findOneBy(SearchCriteria<UsageEventVO> sc) {
        return null;
    }

    @Override
    public Class<UsageEventVO> getEntityBeanType() {
        return null;
    }

    @Override
    public Pair<List<UsageEventVO>, Integer> searchAndCount(
            SearchCriteria<UsageEventVO> sc, Filter filter) {
        return null;
    }

    @Override
    public Pair<List<UsageEventVO>, Integer> searchAndDistinctCount(SearchCriteria<UsageEventVO> sc, Filter filter) {
        return null;
    }

    @Override
    public Map<String, Attribute> getAllAttributes() {
        return null;
    }

    @Override
    public Pair<List<UsageEventVO>, Integer> searchAndDistinctCount(SearchCriteria<UsageEventVO> sc, Filter filter, String[] distinctColumns) {
        return null;
    }

    @Override
    public Integer countAll() {
        return null;
    }

    @Override
    public List<UsageEventVO> listLatestEvents(Date endDate) {
        return null;
    }

    @Override
    public List<UsageEventVO> getLatestEvent() {
        return null;
    }

    @Override
    public List<UsageEventVO> getRecentEvents(Date endDate) {
        return null;
    }

    @Override
    public List<UsageEventVO> listDirectIpEvents(Date startDate, Date endDate,
            long zoneId) {
        return null;
    }

    @Override
    public void saveDetails(long eventId, Map<String, String> details) {

    }

    @Override
    public Pair<List<UsageEventVO>, Integer> searchAndCount(SearchCriteria<UsageEventVO> sc, Filter filter, boolean includeRemoved) {
        return null;
    }
}
