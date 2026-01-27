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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.VolumeVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@RunWith(MockitoJUnitRunner.class)
public class VolumeDaoImplTest {
    @Mock
    private PreparedStatement preparedStatementMock;

    @Mock
    private TransactionLegacy transactionMock;

    private static MockedStatic<TransactionLegacy> mockedTransactionLegacy;

    @Spy
    private final VolumeDaoImpl volumeDao = new VolumeDaoImpl();

    @BeforeClass
    public static void init() {
        mockedTransactionLegacy = Mockito.mockStatic(TransactionLegacy.class);
    }

    @AfterClass
    public static void close() {
        mockedTransactionLegacy.close();
    }

    @Test
    public void testListPoolIdsByVolumeCount_with_cluster_details() throws SQLException {
        final String ORDER_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT_QUERY_WITH_CLUSTER =
                "SELECT pool.id, SUM(IF(vol.state='Ready' AND vol.account_id = ?, 1, 0)) FROM `cloud`.`storage_pool` pool LEFT JOIN `cloud`.`volumes` vol ON pool.id = vol.pool_id WHERE pool.data_center_id = ?  AND pool.pod_id = ? AND pool.cluster_id = ? GROUP BY pool.id ORDER BY 2 ASC ";
        final long dcId = 1, accountId = 1;
        final Long podId = 1L, clusterId = 1L;

        when(TransactionLegacy.currentTxn()).thenReturn(transactionMock);
        when(transactionMock.prepareAutoCloseStatement(startsWith(ORDER_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT_QUERY_WITH_CLUSTER))).thenReturn(preparedStatementMock);
        ResultSet rs = Mockito.mock(ResultSet.class);
        when(preparedStatementMock.executeQuery()).thenReturn(rs, rs);

        volumeDao.listPoolIdsByVolumeCount(dcId, podId, clusterId, accountId);

        verify(transactionMock, times(1)).prepareAutoCloseStatement(ORDER_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT_QUERY_WITH_CLUSTER);
        verify(preparedStatementMock, times(1)).setLong(1, accountId);
        verify(preparedStatementMock, times(1)).setLong(2, dcId);
        verify(preparedStatementMock, times(1)).setLong(3, podId);
        verify(preparedStatementMock, times(1)).setLong(4, clusterId);
        verify(preparedStatementMock, times(4)).setLong(anyInt(), anyLong());
        verify(preparedStatementMock, times(1)).executeQuery();
    }

    @Test
    public void testListPoolIdsByVolumeCount_without_cluster_details() throws SQLException {
        final String ORDER_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT_QUERY_WITHOUT_CLUSTER =
                "SELECT pool.id, SUM(IF(vol.state='Ready' AND vol.account_id = ?, 1, 0)) FROM `cloud`.`storage_pool` pool LEFT JOIN `cloud`.`volumes` vol ON pool.id = vol.pool_id WHERE pool.data_center_id = ?  GROUP BY pool.id ORDER BY 2 ASC ";
        final long dcId = 1, accountId = 1;

        when(TransactionLegacy.currentTxn()).thenReturn(transactionMock);
        when(transactionMock.prepareAutoCloseStatement(startsWith(ORDER_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT_QUERY_WITHOUT_CLUSTER))).thenReturn(preparedStatementMock);
        ResultSet rs = Mockito.mock(ResultSet.class);
        when(preparedStatementMock.executeQuery()).thenReturn(rs, rs);

        volumeDao.listPoolIdsByVolumeCount(dcId, null, null, accountId);

        verify(transactionMock, times(1)).prepareAutoCloseStatement(ORDER_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT_QUERY_WITHOUT_CLUSTER);
        verify(preparedStatementMock, times(1)).setLong(1, accountId);
        verify(preparedStatementMock, times(1)).setLong(2, dcId);
        verify(preparedStatementMock, times(2)).setLong(anyInt(), anyLong());
        verify(preparedStatementMock, times(1)).executeQuery();
    }

    @Test
    public void testSearchRemovedByVmsNoVms() {
        Assert.assertTrue(CollectionUtils.isEmpty(volumeDao.searchRemovedByVms(
                new ArrayList<>(), 100L)));
        Assert.assertTrue(CollectionUtils.isEmpty(volumeDao.searchRemovedByVms(
                null, 100L)));
    }

    @Test
    public void testSearchRemovedByVms() {
        SearchBuilder<VolumeVO> sb = Mockito.mock(SearchBuilder.class);
        SearchCriteria<VolumeVO> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.doReturn(new ArrayList<>()).when(volumeDao).searchIncludingRemoved(
                Mockito.any(SearchCriteria.class), Mockito.any(Filter.class), Mockito.eq(null),
                Mockito.eq(false));
        Mockito.when(volumeDao.createSearchBuilder()).thenReturn(sb);
        final VolumeVO mockedVO = Mockito.mock(VolumeVO.class);
        Mockito.when(sb.entity()).thenReturn(mockedVO);
        List<Long> vmIds = List.of(1L, 2L);
        Object[] array = vmIds.toArray();
        Long batchSize = 50L;
        volumeDao.searchRemovedByVms(List.of(1L, 2L), batchSize);
        Mockito.verify(sc).setParameters("vmIds", array);
        Mockito.verify(volumeDao, Mockito.times(1)).searchIncludingRemoved(
                Mockito.any(SearchCriteria.class), Mockito.any(Filter.class), Mockito.eq(null),
                Mockito.eq(false));
    }

}
