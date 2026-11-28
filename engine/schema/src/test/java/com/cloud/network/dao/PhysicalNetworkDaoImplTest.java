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
package com.cloud.network.dao;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBase;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class PhysicalNetworkDaoImplTest {

    @Mock
    private PhysicalNetworkTrafficTypeDao trafficTypeDao;

    @Mock
    private GenericSearchBuilder<PhysicalNetworkTrafficTypeVO, String> kvmNetworkLabelsSearch;

    @Mock
    private PhysicalNetworkTrafficTypeVO trafficTypeEntity;

    @Mock
    private SearchCriteria<String> searchCriteria;

    private PhysicalNetworkDaoImpl createDao() {
        PhysicalNetworkDaoImpl dao = new PhysicalNetworkDaoImpl();
        dao._trafficTypeDao = trafficTypeDao;
        Mockito.when(trafficTypeDao.createSearchBuilder(String.class)).thenReturn(kvmNetworkLabelsSearch);
        Mockito.when(kvmNetworkLabelsSearch.entity()).thenReturn(trafficTypeEntity);
        Mockito.when(kvmNetworkLabelsSearch.create()).thenReturn(searchCriteria);
        dao.init();
        return dao;
    }

    @Test
    public void getKvmNetworkLabelsInZoneFiltersBlankAndDuplicateLabels() {
        PhysicalNetworkDaoImpl dao = createDao();
        Mockito.when(trafficTypeDao.customSearch(searchCriteria, null)).thenReturn(
                Arrays.asList("cloudbr0", " ", "", null, "brbond0-100", "cloudbr0"));

        Set<String> labels = dao.getKvmNetworkLabelsInZone(42L);

        Assert.assertEquals(new HashSet<>(Arrays.asList("cloudbr0", "brbond0-100")), labels);
        Mockito.verify(searchCriteria).setJoinParameters("pnSearch", "dataCenterId", 42L);

        ArgumentCaptor<SearchBase<?, ?, ?>> physicalNetworkSearchCaptor = ArgumentCaptor.forClass(SearchBase.class);
        Mockito.verify(kvmNetworkLabelsSearch).join(Mockito.eq("pnSearch"), physicalNetworkSearchCaptor.capture(),
                Mockito.any(), Mockito.any(), Mockito.eq(JoinBuilder.JoinType.INNER));
        SearchCriteria<?> physicalNetworkCriteria = physicalNetworkSearchCaptor.getValue().create();
        physicalNetworkCriteria.setParameters("dataCenterId", 42L);
        Assert.assertTrue(physicalNetworkCriteria.getWhereClause().contains("removed IS NULL"));
    }

    @Test
    public void getKvmNetworkLabelsInZoneReturnsEmptySetWhenNoLabelsAreFound() {
        PhysicalNetworkDaoImpl dao = createDao();
        Mockito.when(trafficTypeDao.customSearch(searchCriteria, null)).thenReturn(null);

        Assert.assertTrue(dao.getKvmNetworkLabelsInZone(42L).isEmpty());
    }
}
