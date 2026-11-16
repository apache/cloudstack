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

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class SnapshotDaoImplTest {

    @Mock
    SearchBuilder<SnapshotVO> volumeIdNameNotInStatusSearchMock;

    @Mock
    SearchCriteria<SnapshotVO> searchCriteriaMock;

    @Spy
    SnapshotDaoImpl snapshotDao = new SnapshotDaoImpl();

    @Before
    public void setUp() throws Exception {
        // init() wires up several search builders that join other DAOs, which are not available in a
        // unit test; inject only the search builder this method uses and stub it to return a criteria.
        Field searchField = SnapshotDaoImpl.class.getDeclaredField("volumeIdNameNotInStatusSearch");
        searchField.setAccessible(true);
        searchField.set(snapshotDao, volumeIdNameNotInStatusSearchMock);
        Mockito.lenient().doReturn(searchCriteriaMock).when(volumeIdNameNotInStatusSearchMock).create();
    }

    @Test
    public void testFindByVolumeIdAndNameNotInStatusReturnsTheMatch() {
        SnapshotVO expected = Mockito.mock(SnapshotVO.class);
        Mockito.doReturn(expected).when(snapshotDao).findOneBy(Mockito.any(SearchCriteria.class));

        SnapshotVO result = snapshotDao.findByVolumeIdAndNameNotInStatus(5L, "snap-1", Snapshot.State.Destroyed);

        Assert.assertSame(expected, result);
        Mockito.verify(searchCriteriaMock).setParameters("volumeId", 5L);
        Mockito.verify(searchCriteriaMock).setParameters("name", "snap-1");
        Mockito.verify(snapshotDao).findOneBy(searchCriteriaMock);
    }

    @Test
    public void testFindByVolumeIdAndNameNotInStatusReturnsNullWhenNoMatch() {
        Mockito.doReturn(null).when(snapshotDao).findOneBy(Mockito.any(SearchCriteria.class));

        SnapshotVO result = snapshotDao.findByVolumeIdAndNameNotInStatus(5L, "snap-1");

        Assert.assertNull(result);
        Mockito.verify(searchCriteriaMock).setParameters("volumeId", 5L);
        Mockito.verify(snapshotDao).findOneBy(searchCriteriaMock);
    }
}
