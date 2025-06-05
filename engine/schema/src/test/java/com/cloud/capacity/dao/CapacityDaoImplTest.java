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
package com.cloud.capacity.dao;

import com.cloud.capacity.CapacityVO;
import com.cloud.host.Host;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CapacityDaoImplTest {
    @Spy
    @InjectMocks
    CapacityDaoImpl capacityDao = new CapacityDaoImpl();

    @Mock
    private TransactionLegacy txn;
    @Mock
    private PreparedStatement pstmt;
    @Mock
    private ResultSet resultSet;
    private MockedStatic<TransactionLegacy> mockedTransactionLegacy;

    private SearchBuilder<CapacityVO> searchBuilder;
    private SearchCriteria<CapacityVO> searchCriteria;

    @Before
    public void setUp() {
        searchBuilder = mock(SearchBuilder.class);
        CapacityVO capacityVO = mock(CapacityVO.class);
        when(searchBuilder.entity()).thenReturn(capacityVO);
        searchCriteria = mock(SearchCriteria.class);
        doReturn(searchBuilder).when(capacityDao).createSearchBuilder();
        when(searchBuilder.create()).thenReturn(searchCriteria);

        mockedTransactionLegacy = Mockito.mockStatic(TransactionLegacy.class);
        mockedTransactionLegacy.when(TransactionLegacy::currentTxn).thenReturn(txn);
    }

    @After
    public void tearDown() {
        if (mockedTransactionLegacy != null) {
            mockedTransactionLegacy.close();
        }
    }

    @Test
    public void testListByHostIdTypes() {
        // Prepare inputs
        Long hostId = 1L;
        List<Short> capacityTypes = Arrays.asList((short)1, (short)2);
        CapacityVO capacity1 = new CapacityVO();
        CapacityVO capacity2 = new CapacityVO();
        List<CapacityVO> mockResult = Arrays.asList(capacity1, capacity2);
        doReturn(mockResult).when(capacityDao).listBy(any(SearchCriteria.class));
        List<CapacityVO> result = capacityDao.listByHostIdTypes(hostId, capacityTypes);
        verify(searchBuilder).and(eq("hostId"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchBuilder).and(eq("type"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder).done();
        verify(searchCriteria).setParameters("hostId", hostId);
        verify(searchCriteria).setParameters("type", capacityTypes.toArray());
        verify(capacityDao).listBy(searchCriteria);
        assertEquals(2, result.size());
        assertSame(capacity1, result.get(0));
        assertSame(capacity2, result.get(1));
    }

    @Test
    public void testListByHostIdTypesEmptyResult() {
        Long hostId = 1L;
        List<Short> capacityTypes = Arrays.asList((short)1, (short)2);
        doReturn(Collections.emptyList()).when(capacityDao).listBy(any(SearchCriteria.class));
        List<CapacityVO> result = capacityDao.listByHostIdTypes(hostId, capacityTypes);
        verify(searchBuilder).and(Mockito.eq("hostId"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchBuilder).and(eq("type"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder).done();
        verify(searchCriteria).setParameters("hostId", hostId);
        verify(searchCriteria).setParameters("type", capacityTypes.toArray());
        verify(capacityDao).listBy(searchCriteria);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testListClustersCrossingThresholdEmptyResult() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        List<Long> result = capacityDao.listClustersCrossingThreshold((short)1, 1L, "cpu.threshold", 5000L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindCapacityByZoneAndHostTagNoResults() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Ternary<Long, Long, Long> result = capacityDao.findCapacityByZoneAndHostTag(1L, "host-tag");
        assertNotNull(result);
        assertEquals(Long.valueOf(0L), result.first());
        assertEquals(Long.valueOf(0L), result.second());
        assertEquals(Long.valueOf(0L), result.third());
    }
    @Test
    public void testFindByHostIdType() {
        CapacityVO capacity = new CapacityVO();
        capacity.setHostId(1L);
        capacity.setCapacityType((short) 1);

        doReturn(capacity).when(capacityDao).findOneBy(any());

        CapacityVO found = capacityDao.findByHostIdType(1L, (short) 1);
        assertNotNull(found);
        assertEquals(Long.valueOf(1L), found.getHostOrPoolId());
    }

    @Test
    public void testUpdateAllocatedAddition() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        doNothing().when(txn).start();
        when(txn.commit()).thenReturn(true);

        capacityDao.updateAllocated(1L, 1000L, (short)1, true);

        verify(txn, times(1)).start();
        verify(txn, times(1)).commit();
        verify(pstmt, times(1)).executeUpdate();
    }

    @Test
    public void testUpdateAllocatedSubtraction() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        doNothing().when(txn).start();
        when(txn.commit()).thenReturn(true);

        capacityDao.updateAllocated(1L, 500L, (short)1, false);

        verify(txn, times(1)).start();
        verify(txn, times(1)).commit();
        verify(pstmt, times(1)).executeUpdate();
    }

    @Test
    public void testFindFilteredCapacityByEmptyResult() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        List<CapacityDaoImpl.SummedCapacity> result = capacityDao.findFilteredCapacityBy(null, null, null, null, Collections.emptyList(), Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testListClustersInZoneOrPodByHostCapacitiesEmpty() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<Long> resultZone = capacityDao.listClustersInZoneOrPodByHostCapacities(1L, 123L, 2, 2048L, (short)0, true);
        assertNotNull(resultZone);
        assertTrue(resultZone.isEmpty());

        List<Long> resultPod = capacityDao.listClustersInZoneOrPodByHostCapacities(1L, 123L, 2, 2048L, (short)0, false);
        assertNotNull(resultPod);
        assertTrue(resultPod.isEmpty());
    }


    @Test
    public void testListHostsWithEnoughCapacityEmptyResult() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<Long> result = capacityDao.listHostsWithEnoughCapacity(1, 100L, 200L, Host.Type.Routing.toString());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


    @Test
    public void testOrderClustersByAggregateCapacityEmptyResult() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Pair<List<Long>, Map<Long, Double>> result = capacityDao.orderClustersByAggregateCapacity(1L, 1L, (short) 1, true);
        assertNotNull(result);
        assertTrue(result.first().isEmpty());
        assertTrue(result.second().isEmpty());
    }


    @Test
    public void testOrderPodsByAggregateCapacityEmptyResult() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Pair<List<Long>, Map<Long, Double>> result = capacityDao.orderPodsByAggregateCapacity(1L, (short) 1);
        assertNotNull(result);
        assertTrue(result.first().isEmpty());
        assertTrue(result.second().isEmpty());
    }


    @Test
    public void testUpdateCapacityState() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeUpdate()).thenReturn(1);

        capacityDao.updateCapacityState(1L, 1L, 1L, 1L, "Enabled", new short[]{1});

        verify(pstmt, times(1)).executeUpdate();
    }


    @Test
    public void testFindClusterConsumption() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getFloat(1)).thenReturn(0.5f);

        float result = capacityDao.findClusterConsumption(1L, (short) 1, 1000L);
        assertEquals(0.5f, result, 0.0f);
    }

    @Test
    public void testListPodsByHostCapacitiesEmptyResult() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<Long> result = capacityDao.listPodsByHostCapacities(1L, 2, 1024L, (short)0);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOrderHostsByFreeCapacityEmptyResult() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Pair<List<Long>, Map<Long, Double>> result = capacityDao.orderHostsByFreeCapacity(1L, 1L, (short) 0);
        assertNotNull(result);
        assertTrue(result.first().isEmpty());
    }

    @Test
    public void testFindByClusterPodZoneEmptyResult() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<CapacityDaoImpl.SummedCapacity> result = capacityDao.findByClusterPodZone(1L, 1L, 1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testListCapacitiesGroupedByLevelAndTypeEmptyResult() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<CapacityDaoImpl.SummedCapacity> result = capacityDao.listCapacitiesGroupedByLevelAndType(0, 1L,
                1L, 1L, 0, Collections.emptyList(), Collections.emptyList(), 1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindCapacityByEmptyResult() throws Exception {
        when(txn.prepareAutoCloseStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<CapacityDaoImpl.SummedCapacity> result = capacityDao.findCapacityBy(1, 1L, 1L, 1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
