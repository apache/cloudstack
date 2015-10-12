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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class MockUsageEventDao implements UsageEventDao{

    List<UsageEventVO> persistedItems;

    public MockUsageEventDao() {
        persistedItems = new ArrayList<UsageEventVO>();
    }

    @Override
    public UsageEventVO findById(Long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UsageEventVO findByIdIncludingRemoved(Long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UsageEventVO findById(Long id, boolean fresh) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UsageEventVO findByUuid(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UsageEventVO findByUuidIncludingRemoved(String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UsageEventVO createForUpdate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SearchBuilder<UsageEventVO> createSearchBuilder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <K> GenericSearchBuilder<UsageEventVO, K> createSearchBuilder(
            Class<K> clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UsageEventVO createForUpdate(Long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SearchCriteria<UsageEventVO> createSearchCriteria() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UsageEventVO> lockRows(SearchCriteria<UsageEventVO> sc,
            Filter filter, boolean exclusive) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UsageEventVO lockOneRandomRow(SearchCriteria<UsageEventVO> sc,
            boolean exclusive) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UsageEventVO lockRow(Long id, Boolean exclusive) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UsageEventVO acquireInLockTable(Long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UsageEventVO acquireInLockTable(Long id, int seconds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean releaseFromLockTable(Long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean update(Long id, UsageEventVO entity) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int update(UsageEventVO entity, SearchCriteria<UsageEventVO> sc) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<UsageEventVO> listAll() {

        return persistedItems;
    }

    @Override
    public List<UsageEventVO> listAll(Filter filter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UsageEventVO> search(SearchCriteria<UsageEventVO> sc,
            Filter filter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UsageEventVO> search(SearchCriteria<UsageEventVO> sc,
            Filter filter, boolean enableQueryCache) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UsageEventVO> searchIncludingRemoved(
            SearchCriteria<UsageEventVO> sc, Filter filter, Boolean lock,
            boolean cache) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UsageEventVO> searchIncludingRemoved(
            SearchCriteria<UsageEventVO> sc, Filter filter, Boolean lock,
            boolean cache, boolean enableQueryCache) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <M> List<M> customSearchIncludingRemoved(SearchCriteria<M> sc,
            Filter filter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UsageEventVO> listAllIncludingRemoved() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UsageEventVO> listAllIncludingRemoved(Filter filter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UsageEventVO persist(UsageEventVO entity) {
        persistedItems.add(entity);
        return entity;
    }

    @Override
    public boolean remove(Long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int remove(SearchCriteria<UsageEventVO> sc) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean expunge(Long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int expunge(SearchCriteria<UsageEventVO> sc) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void expunge() {
        // TODO Auto-generated method stub

    }

    @Override
    public <K> K getNextInSequence(Class<K> clazz, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <M> List<M> customSearch(SearchCriteria<M> sc, Filter filter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean lockInLockTable(String id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean lockInLockTable(String id, int seconds) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean unlockFromLockTable(String id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public <K> K getRandomlyIncreasingNextInSequence(Class<K> clazz, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UsageEventVO findOneBy(SearchCriteria<UsageEventVO> sc) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<UsageEventVO> getEntityBeanType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<List<UsageEventVO>, Integer> searchAndCount(
            SearchCriteria<UsageEventVO> sc, Filter filter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<List<UsageEventVO>, Integer> searchAndDistinctCount(SearchCriteria<UsageEventVO> sc, Filter filter) {
        //TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Attribute> getAllAttributes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UsageEventVO> listLatestEvents(Date endDate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UsageEventVO> getLatestEvent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UsageEventVO> getRecentEvents(Date endDate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<UsageEventVO> listDirectIpEvents(Date startDate, Date endDate,
            long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void saveDetails(long eventId, Map<String, String> details) {
        // TODO Auto-generated method stub

    }

}
