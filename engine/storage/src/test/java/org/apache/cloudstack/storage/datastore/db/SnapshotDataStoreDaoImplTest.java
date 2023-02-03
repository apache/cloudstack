/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.datastore.db;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SnapshotDataStoreDaoImplTest {

    SnapshotDataStoreDaoImpl snapshotDataStoreDaoImplSpy = Mockito.spy(SnapshotDataStoreDaoImpl.class);

    @Mock
    SnapshotDataStoreVO snapshotDataStoreVoMock;

    @Mock
    SearchCriteria searchCriteriaMock;

    @Mock
    SearchBuilder searchBuilderMock;

    @Mock
    SnapshotDao snapshotDaoMock;

    @Mock
    SnapshotVO snapshotVoMock;

    @Before
    public void init(){
        snapshotDataStoreDaoImplSpy.searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq = searchBuilderMock;
        snapshotDataStoreDaoImplSpy.snapshotDao = snapshotDaoMock;
    }

    @Test
    public void validateExpungeReferenceBySnapshotIdAndDataStoreRoleNullReference(){
        Mockito.doReturn(searchCriteriaMock).when(snapshotDataStoreDaoImplSpy).createSearchCriteriaBySnapshotIdAndStoreRole(Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(null).when(snapshotDataStoreDaoImplSpy).findOneBy(searchCriteriaMock);
        Assert.assertTrue(snapshotDataStoreDaoImplSpy.expungeReferenceBySnapshotIdAndDataStoreRole(0, DataStoreRole.Image));
    }

    @Test
    public void validateExpungeReferenceBySnapshotIdAndDataStoreRole(){
        Mockito.doReturn(searchCriteriaMock).when(snapshotDataStoreDaoImplSpy).createSearchCriteriaBySnapshotIdAndStoreRole(Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(snapshotDataStoreVoMock).when(snapshotDataStoreDaoImplSpy).findOneBy(searchCriteriaMock);
        Mockito.doReturn(true).when(snapshotDataStoreDaoImplSpy).expunge(Mockito.anyLong());
        Assert.assertTrue(snapshotDataStoreDaoImplSpy.expungeReferenceBySnapshotIdAndDataStoreRole(0, DataStoreRole.Image));
    }

    @Test
    public void validateCreateSearchCriteriaBySnapshotIdAndStoreRole() {
        Mockito.doReturn(searchCriteriaMock).when(searchBuilderMock).create();
        Mockito.doNothing().when(searchCriteriaMock).setParameters(Mockito.anyString(), Mockito.any());
        SearchCriteria result = snapshotDataStoreDaoImplSpy.createSearchCriteriaBySnapshotIdAndStoreRole(0, DataStoreRole.Image);

        Mockito.verify(searchCriteriaMock).setParameters("snapshot_id", 0l);
        Mockito.verify(searchCriteriaMock).setParameters("store_role", DataStoreRole.Image);
    }

    @Test
    public void isSnapshotChainingRequiredTestSnapshotIsNullReturnFalse() {
        snapshotDataStoreDaoImplSpy.snapshotVOSearch = searchBuilderMock;
        Mockito.doReturn(searchCriteriaMock).when(searchBuilderMock).create();
        Mockito.doReturn(null).when(snapshotDaoMock).findOneBy(Mockito.any());
        Assert.assertFalse(snapshotDataStoreDaoImplSpy.isSnapshotChainingRequired(2));
    }

    @Test
    public void isSnapshotChainingRequiredTestSnapshotIsNotNullReturnAccordingHypervisorType() {
        snapshotDataStoreDaoImplSpy.snapshotVOSearch = searchBuilderMock;
        Mockito.doReturn(searchCriteriaMock).when(searchBuilderMock).create();
        Mockito.doReturn(snapshotVoMock).when(snapshotDaoMock).findOneBy(Mockito.any());

        for (Hypervisor.HypervisorType hypervisorType : Hypervisor.HypervisorType.values()) {
            Mockito.doReturn(hypervisorType).when(snapshotVoMock).getHypervisorType();
            boolean result = snapshotDataStoreDaoImplSpy.isSnapshotChainingRequired(2);

            if (SnapshotDataStoreDaoImpl.HYPERVISORS_SUPPORTING_SNAPSHOTS_CHAINING.contains(hypervisorType)) {
                Assert.assertTrue(result);
            } else {
                Assert.assertFalse(result);
            }
        }
    }

    @Test
    public void expungeReferenceBySnapshotIdAndDataStoreRoleTestSnapshotDataStoreIsNullReturnTrue() {
        Mockito.doReturn(searchCriteriaMock).when(snapshotDataStoreDaoImplSpy).createSearchCriteriaBySnapshotIdAndStoreRole(Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(null).when(snapshotDataStoreDaoImplSpy).findOneBy(Mockito.any());

        for (DataStoreRole value : DataStoreRole.values()) {
            Assert.assertTrue(snapshotDataStoreDaoImplSpy.expungeReferenceBySnapshotIdAndDataStoreRole(1, value));
        }
    }

    @Test
    public void expungeReferenceBySnapshotIdAndDataStoreRoleTestSnapshotDataStoreIsNotNullAndExpungeIsTrueReturnTrue() {
        Mockito.doReturn(searchCriteriaMock).when(snapshotDataStoreDaoImplSpy).createSearchCriteriaBySnapshotIdAndStoreRole(Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(snapshotDataStoreVoMock).when(snapshotDataStoreDaoImplSpy).findOneBy(Mockito.any());
        Mockito.doReturn(true).when(snapshotDataStoreDaoImplSpy).expunge(Mockito.anyLong());

        for (DataStoreRole value : DataStoreRole.values()) {
            Assert.assertTrue(snapshotDataStoreDaoImplSpy.expungeReferenceBySnapshotIdAndDataStoreRole(1, value));
        }
    }

    @Test
    public void expungeReferenceBySnapshotIdAndDataStoreRoleTestSnapshotDataStoreIsNotNullAndExpungeIsFalseReturnTrue() {
        Mockito.doReturn(searchCriteriaMock).when(snapshotDataStoreDaoImplSpy).createSearchCriteriaBySnapshotIdAndStoreRole(Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(snapshotDataStoreVoMock).when(snapshotDataStoreDaoImplSpy).findOneBy(Mockito.any());
        Mockito.doReturn(false).when(snapshotDataStoreDaoImplSpy).expunge(Mockito.anyLong());

        for (DataStoreRole value : DataStoreRole.values()) {
            Assert.assertFalse(snapshotDataStoreDaoImplSpy.expungeReferenceBySnapshotIdAndDataStoreRole(1, value));
        }
    }

}
