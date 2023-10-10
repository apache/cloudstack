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
package com.cloud.storage.dao;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.StoragePoolTagVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class StoragePoolTagsDaoImplTest extends TestCase {

    @Mock
    ConfigurationDao _configDao;
    @Mock
    SearchBuilder<StoragePoolTagVO> StoragePoolIdsSearch;

    @Spy
    @InjectMocks
    private StoragePoolTagsDaoImpl _storagePoolTagsDaoImpl = new StoragePoolTagsDaoImpl();

    @Mock
    StoragePoolTagVO storagePoolTag1;
    @Mock
    StoragePoolTagVO storagePoolTag2;

    private final String batchSizeConfigurationKey = "detail.batch.query.size";
    private final String batchSizeValue = "2800";
    private final String batchSizeDefaultValue = "2000";
    private final String batchSizeLow = "2";

    private final Long[] storageTagsIds = {1l,2l,3l,4l,5l};
    private final List<StoragePoolTagVO> storagePoolTagList = Arrays.asList(storagePoolTag1, storagePoolTag2);

    @Before
    public void setup() {
        when(_configDao.getValue(batchSizeConfigurationKey)).thenReturn(batchSizeValue);
        doReturn(storagePoolTagList).when(_storagePoolTagsDaoImpl).searchIncludingRemoved(
                Matchers.any(SearchCriteria.class), Matchers.isNull(Filter.class), Matchers.isNull(Boolean.class), Matchers.eq(false));
    }

    @Test
    public void testGetDetailsBatchSizeNotNull() {
        assertEquals(Integer.parseInt(batchSizeValue), _storagePoolTagsDaoImpl.getDetailsBatchSize());
    }

    @Test
    public void testGetDetailsBatchSizeNull() {
        when(_configDao.getValue(batchSizeConfigurationKey)).thenReturn(null);
        assertEquals(Integer.parseInt(batchSizeDefaultValue), _storagePoolTagsDaoImpl.getDetailsBatchSize());
    }

    @Test
    public void testSearchForStoragePoolIdsInternalStorageTagsNotNullSearch() {
        List<StoragePoolTagVO> storagePoolTags = new ArrayList<StoragePoolTagVO>();

        _storagePoolTagsDaoImpl.searchForStoragePoolIdsInternal(0, storageTagsIds.length, storageTagsIds, storagePoolTags);
        verify(_storagePoolTagsDaoImpl).searchIncludingRemoved(Matchers.any(SearchCriteria.class), Matchers.isNull(Filter.class), Matchers.isNull(Boolean.class), Matchers.eq(false));
        assertEquals(2, storagePoolTags.size());
    }

    @Test
    public void testSearchForStoragePoolIdsInternalStorageTagsNullSearch() {
        List<StoragePoolTagVO> storagePoolTags = new ArrayList<StoragePoolTagVO>();
        doReturn(null).when(_storagePoolTagsDaoImpl).searchIncludingRemoved(
                Matchers.any(SearchCriteria.class), Matchers.isNull(Filter.class), Matchers.isNull(Boolean.class), Matchers.eq(false));

        _storagePoolTagsDaoImpl.searchForStoragePoolIdsInternal(0, storageTagsIds.length, storageTagsIds, storagePoolTags);
        verify(_storagePoolTagsDaoImpl).searchIncludingRemoved(Matchers.any(SearchCriteria.class), Matchers.isNull(Filter.class), Matchers.isNull(Boolean.class), Matchers.eq(false));
        assertEquals(0, storagePoolTags.size());
    }

    @Test
    public void testSearchByIdsStorageTagsIdsGreaterOrEqualThanBatchSize() {
        when(_configDao.getValue(batchSizeConfigurationKey)).thenReturn(batchSizeLow);
        doNothing().when(_storagePoolTagsDaoImpl).searchForStoragePoolIdsInternal(Matchers.anyInt(), Matchers.anyInt(), Matchers.any(Long[].class), Matchers.anyList());
        _storagePoolTagsDaoImpl.searchByIds(storageTagsIds);

        int batchSize = Integer.parseInt(batchSizeLow);
        int difference = storageTagsIds.length - 2 * batchSize;
        verify(_storagePoolTagsDaoImpl, Mockito.times(2)).searchForStoragePoolIdsInternal(Matchers.anyInt(), Matchers.eq(batchSize), Matchers.any(Long[].class), Matchers.anyList());
        verify(_storagePoolTagsDaoImpl).searchForStoragePoolIdsInternal(Matchers.eq(2 * batchSize), Matchers.eq(difference), Matchers.any(Long[].class), Matchers.anyList());
    }

    @Test
    public void testSearchByIdsStorageTagsIdsLowerThanBatchSize() {
        doNothing().when(_storagePoolTagsDaoImpl).searchForStoragePoolIdsInternal(Matchers.anyInt(), Matchers.anyInt(), Matchers.any(Long[].class), Matchers.anyList());
        _storagePoolTagsDaoImpl.searchByIds(storageTagsIds);

        verify(_storagePoolTagsDaoImpl).searchForStoragePoolIdsInternal(Matchers.eq(0), Matchers.eq(storageTagsIds.length), Matchers.any(Long[].class), Matchers.anyList());
    }
}
