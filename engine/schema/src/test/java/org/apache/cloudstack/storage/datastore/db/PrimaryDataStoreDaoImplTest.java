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
package org.apache.cloudstack.storage.datastore.db;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDaoImpl.ValueType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import com.cloud.storage.ScopeType;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.StoragePoolTagsDao;

import junit.framework.TestCase;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PrimaryDataStoreDaoImplTest extends TestCase {

    @Mock
    StoragePoolDetailsDao _detailsDao;
    @Mock
    StoragePoolHostDao _hostDao;
    @Mock
    StoragePoolTagsDao _tagsDao;

    @Spy
    @InjectMocks
    private static PrimaryDataStoreDaoImpl primaryDataStoreDao = new PrimaryDataStoreDaoImpl();

    @Mock
    StoragePoolVO storagePoolVO;

    private static final String STORAGE_TAG_1 = "NFS-A";
    private static final String STORAGE_TAG_2 = "NFS-B";
    private static final String[] STORAGE_TAGS_ARRAY = {STORAGE_TAG_1, STORAGE_TAG_2};

    private static final String DETAIL_KEY = "storage.overprovisioning.factor";
    private static final String DETAIL_VALUE = "2.0";
    private static final Map<String, String> STORAGE_POOL_DETAILS = new HashMap<>();

    private static final String EXPECTED_RESULT_SQL_STORAGE_TAGS = "(storage_pool_tags.tag='" + STORAGE_TAG_1 + "') OR (storage_pool_tags.tag='" + STORAGE_TAG_2 + "')";
    private static final String EXPECTED_RESULT_SQL_DETAILS = "((storage_pool_details.name='" + DETAIL_KEY + "') AND (storage_pool_details.value='" + DETAIL_VALUE +"'))";

    private static final String SQL_PREFIX = "XXXXXXXXXXXXXXXX";
    private static final String SQL_SUFFIX = "ZZZZZZZZZZZZZZZZ";
    private static final String SQL_VALUES = "YYYYYYYYYYYYYYYY";

    private static final Long DATACENTER_ID = 1l;
    private static final Long POD_ID = 1l;
    private static final Long CLUSTER_ID = null;
    private static final ScopeType SCOPE = ScopeType.ZONE;


    @Before
    public void setup() throws IOException, ClassNotFoundException, SQLException {
        STORAGE_POOL_DETAILS.put(DETAIL_KEY, DETAIL_VALUE);
    }

    @Test
    public void testGetSqlValuesFromStorageTagsNotNullStorageTags() {
        assertEquals(EXPECTED_RESULT_SQL_STORAGE_TAGS, primaryDataStoreDao.getSqlValuesFromStorageTags(STORAGE_TAGS_ARRAY));
    }

    @Test(expected=NullPointerException.class)
    public void testGetSqlValuesFromStorageTagsNullStorageTags() {
        primaryDataStoreDao.getSqlValuesFromStorageTags(null);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testGetSqlValuesFromStorageTagsEmptyStorageTags() {
        String[] emptyStorageTags = {};
        primaryDataStoreDao.getSqlValuesFromStorageTags(emptyStorageTags);
    }

    @Test
    public void testGetSqlValuesFromDetailsNotNullDetails() {
        assertEquals(EXPECTED_RESULT_SQL_DETAILS, primaryDataStoreDao.getSqlValuesFromDetails(STORAGE_POOL_DETAILS));
    }

    @Test(expected=NullPointerException.class)
    public void testGetSqlValuesFromDetailsNullDetails() {
        primaryDataStoreDao.getSqlValuesFromDetails(null);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testGetSqlValuesFromDetailsEmptyDetailss() {
        Map<String,String> emptyDetails = new HashMap<String, String>();
        primaryDataStoreDao.getSqlValuesFromDetails(emptyDetails);
    }

    @Test
    public void testGetSqlPreparedStatementNullClusterId() {
        String sqlPreparedStatement = primaryDataStoreDao.getSqlPreparedStatement(SQL_PREFIX, SQL_SUFFIX, SQL_VALUES, null);
        assertEquals(SQL_PREFIX + SQL_VALUES + SQL_SUFFIX, sqlPreparedStatement);
    }

    @Test
    public void testGetSqlPreparedStatementNotNullClusterId() {
        String clusterSql = "storage_pool.cluster_id = ? OR storage_pool.cluster_id IS NULL) AND (";
        String sqlPreparedStatement = primaryDataStoreDao.getSqlPreparedStatement(SQL_PREFIX, SQL_SUFFIX, SQL_VALUES, 1l);
        assertEquals(SQL_PREFIX + clusterSql + SQL_VALUES + SQL_SUFFIX, sqlPreparedStatement);
    }

    @Test
    public void testFindPoolsByDetailsOrTagsInternalStorageTagsType() {
        doReturn(Arrays.asList(storagePoolVO)).when(primaryDataStoreDao).
                searchStoragePoolsPreparedStatement(nullable(String.class), nullable(Long.class), nullable(Long.class), nullable(Long.class),
                        nullable(ScopeType.class), nullable(Integer.class));
        List<StoragePoolVO> storagePools = primaryDataStoreDao.findPoolsByDetailsOrTagsInternal(DATACENTER_ID, POD_ID, CLUSTER_ID, SCOPE, SQL_VALUES, ValueType.TAGS, STORAGE_TAGS_ARRAY.length);
        assertEquals(Arrays.asList(storagePoolVO), storagePools);
        verify(primaryDataStoreDao).getSqlPreparedStatement(
                primaryDataStoreDao.TagsSqlPrefix, primaryDataStoreDao.TagsSqlSuffix, SQL_VALUES, CLUSTER_ID);
        String expectedSql = primaryDataStoreDao.TagsSqlPrefix + SQL_VALUES + primaryDataStoreDao.TagsSqlSuffix;
        verify(primaryDataStoreDao).searchStoragePoolsPreparedStatement(expectedSql, DATACENTER_ID, POD_ID, CLUSTER_ID, SCOPE, STORAGE_TAGS_ARRAY.length);
    }

    @Test
    public void testFindPoolsByDetailsOrTagsInternalDetailsType() {
        doReturn(Arrays.asList(storagePoolVO)).when(primaryDataStoreDao).
                searchStoragePoolsPreparedStatement(nullable(String.class), nullable(Long.class), nullable(Long.class), nullable(Long.class),
                        nullable(ScopeType.class), nullable(Integer.class));
        List<StoragePoolVO> storagePools = primaryDataStoreDao.findPoolsByDetailsOrTagsInternal(DATACENTER_ID, POD_ID, CLUSTER_ID, SCOPE, SQL_VALUES, ValueType.DETAILS, STORAGE_POOL_DETAILS.size());
        assertEquals(Arrays.asList(storagePoolVO), storagePools);
        verify(primaryDataStoreDao).getSqlPreparedStatement(
                primaryDataStoreDao.DetailsSqlPrefix, primaryDataStoreDao.DetailsSqlSuffix, SQL_VALUES, CLUSTER_ID);
        String expectedSql = primaryDataStoreDao.DetailsSqlPrefix + SQL_VALUES + primaryDataStoreDao.DetailsSqlSuffix;
        verify(primaryDataStoreDao).searchStoragePoolsPreparedStatement(expectedSql, DATACENTER_ID, POD_ID, CLUSTER_ID, SCOPE, STORAGE_POOL_DETAILS.size());
    }
}
