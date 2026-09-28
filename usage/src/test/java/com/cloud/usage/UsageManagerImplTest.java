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
package com.cloud.usage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.cloud.event.UsageEventDetailsVO;
import com.cloud.event.dao.UsageEventDetailsDao;
import com.cloud.usage.dao.UsageNetworkOfferingDao;
import com.cloud.usage.dao.UsageVMSnapshotDao;
import com.cloud.utils.db.SearchCriteria;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.usage.dao.UsageVPNUserDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UsageManagerImplTest {

    @Spy
    @InjectMocks
    private UsageManagerImpl usageManagerImpl;

    @Mock
    private UsageEventDetailsDao usageEventDetailsDao;

    @Mock
    private UsageEventVO usageEventVOMock;

    @Mock
    private UsageVPNUserDao usageVPNUserDaoMock;

    @Mock
    private UsageVMSnapshotDao usageVMSnapshotDaoMock;

    @Mock
    private AccountDao accountDaoMock;

    @Mock
    private UsageNetworkOfferingDao usageNetworkOfferingDaoMock;

    @Mock
    private UsageVPNUserVO vpnUserMock;

    @Mock
    private UsageVMSnapshotVO vmSnapshotMock;

    @Mock
    private AccountVO accountMock;

    private long accountMockId = 1l;
    private long acountDomainIdMock = 2l;

    @Before
    public void before() {
        Mockito.when(accountMock.getId()).thenReturn(accountMockId);
        Mockito.when(accountMock.getDomainId()).thenReturn(acountDomainIdMock);

        Mockito.doReturn(accountMock).when(accountDaoMock).findByIdIncludingRemoved(Mockito.anyLong());

    }

    @Test
    public void createUsageVpnUserTestUserExits() {
        List<UsageVPNUserVO> vpnUsersMock = new ArrayList<UsageVPNUserVO>();
        vpnUsersMock.add(vpnUserMock);

        Mockito.doReturn(vpnUsersMock).when(usageManagerImpl).findUsageVpnUsers(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());

        usageManagerImpl.createUsageVpnUser(usageEventVOMock, accountMock);

        Mockito.verify(usageVPNUserDaoMock, Mockito.never()).persist(Mockito.any(UsageVPNUserVO.class));

    }

    @Test
    public void createUsageVpnUserTestUserDoesNotExits() {
        List<UsageVPNUserVO> vpnUsersMock = new ArrayList<UsageVPNUserVO>();

        Mockito.doReturn(vpnUsersMock).when(usageManagerImpl).findUsageVpnUsers(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(vpnUserMock).when(usageVPNUserDaoMock).persist(Mockito.any(UsageVPNUserVO.class));

        usageManagerImpl.createUsageVpnUser(usageEventVOMock, accountMock);

        Mockito.verify(usageVPNUserDaoMock, Mockito.times(1)).persist(Mockito.any(UsageVPNUserVO.class));

    }

    @Test
    public void deleteUsageVpnUserNoUserFound() {
        List<UsageVPNUserVO> vpnUsersMock = new ArrayList<UsageVPNUserVO>();

        Mockito.doReturn(vpnUsersMock).when(usageManagerImpl).findUsageVpnUsers(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());

        usageManagerImpl.deleteUsageVpnUser(usageEventVOMock, accountMock);

        Mockito.verify(usageVPNUserDaoMock, Mockito.never()).update(Mockito.any(UsageVPNUserVO.class));
    }

    @Test
    public void deleteUsageVpnUserOneUserFound() {
        List<UsageVPNUserVO> vpnUsersMock = new ArrayList<UsageVPNUserVO>();
        vpnUsersMock.add(vpnUserMock);

        Mockito.doReturn(vpnUsersMock).when(usageManagerImpl).findUsageVpnUsers(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doNothing().when(usageVPNUserDaoMock).update(Mockito.any(UsageVPNUserVO.class));

        usageManagerImpl.deleteUsageVpnUser(usageEventVOMock, accountMock);

        Mockito.verify(usageVPNUserDaoMock, Mockito.times(1)).update(Mockito.any(UsageVPNUserVO.class));
    }

    @Test
    public void deleteUsageVpnUserMultipleUsersFound() {
        List<UsageVPNUserVO> vpnUsersMock = new ArrayList<UsageVPNUserVO>();
        vpnUsersMock.add(vpnUserMock);
        vpnUsersMock.add(Mockito.mock(UsageVPNUserVO.class));
        vpnUsersMock.add(Mockito.mock(UsageVPNUserVO.class));

        Mockito.doReturn(vpnUsersMock).when(usageManagerImpl).findUsageVpnUsers(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doNothing().when(usageVPNUserDaoMock).update(Mockito.any(UsageVPNUserVO.class));

        usageManagerImpl.deleteUsageVpnUser(usageEventVOMock, accountMock);

        Mockito.verify(usageVPNUserDaoMock, Mockito.times(3)).update(Mockito.any(UsageVPNUserVO.class));
    }

    @Test
    public void handleVpnUserEventTestAddUser() {
        Mockito.when(this.usageEventVOMock.getType()).thenReturn(EventTypes.EVENT_VPN_USER_ADD);
        Mockito.doNothing().when(this.usageManagerImpl).createUsageVpnUser(usageEventVOMock, accountMock);

        this.usageManagerImpl.handleVpnUserEvent(usageEventVOMock);

        Mockito.verify(usageManagerImpl).createUsageVpnUser(usageEventVOMock, accountMock);
        Mockito.verify(usageManagerImpl, Mockito.never()).deleteUsageVpnUser(usageEventVOMock, accountMock);
    }

    @Test
    public void handleVpnUserEventTestRemoveUser() {
        Mockito.when(this.usageEventVOMock.getType()).thenReturn(EventTypes.EVENT_VPN_USER_REMOVE);
        Mockito.doNothing().when(this.usageManagerImpl).deleteUsageVpnUser(usageEventVOMock, accountMock);

        this.usageManagerImpl.handleVpnUserEvent(usageEventVOMock);

        Mockito.verify(usageManagerImpl, Mockito.never()).createUsageVpnUser(usageEventVOMock, accountMock);
        Mockito.verify(usageManagerImpl).deleteUsageVpnUser(usageEventVOMock, accountMock);
    }

    @Test
    public void handleVpnUserEventTestEventIsNeitherAddNorRemove() {
        Mockito.when(this.usageEventVOMock.getType()).thenReturn("VPN.USER.UPDATE");

        this.usageManagerImpl.handleVpnUserEvent(usageEventVOMock);

        Mockito.verify(usageManagerImpl, Mockito.never()).createUsageVpnUser(usageEventVOMock,accountMock);
        Mockito.verify(usageManagerImpl, Mockito.never()).deleteUsageVpnUser(usageEventVOMock, accountMock);
    }

    @Test
    public void createUsageVMSnapshotTest() {
        Mockito.doReturn(vmSnapshotMock).when(usageVMSnapshotDaoMock).persist(Mockito.any(UsageVMSnapshotVO.class));
        usageManagerImpl.createUsageVMSnapshot(Mockito.mock(UsageEventVO.class));
        Mockito.verify(usageVMSnapshotDaoMock, Mockito.times(1)).persist(Mockito.any(UsageVMSnapshotVO.class));
    }

    @Test
    public void deleteUsageVMSnapshotTest() {
        List<UsageVMSnapshotVO> vmSnapshotsMock = new ArrayList<UsageVMSnapshotVO>();
        vmSnapshotsMock.add(vmSnapshotMock);

        Mockito.doReturn(vmSnapshotsMock).when(usageManagerImpl).findUsageVMSnapshots(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doNothing().when(usageVMSnapshotDaoMock).update(Mockito.any(UsageVMSnapshotVO.class));

        usageManagerImpl.deleteUsageVMSnapshot(usageEventVOMock);

        Mockito.verify(usageVMSnapshotDaoMock, Mockito.times(1)).update(Mockito.any(UsageVMSnapshotVO.class));
    }

    @Test
    public void deleteUsageVMSnapshotMultipleSnapshotsFoundTest() {
        List<UsageVMSnapshotVO> vmSnapshotsMock = new ArrayList<UsageVMSnapshotVO>();
        vmSnapshotsMock.add(vmSnapshotMock);
        vmSnapshotsMock.add(Mockito.mock(UsageVMSnapshotVO.class));
        vmSnapshotsMock.add(Mockito.mock(UsageVMSnapshotVO.class));


        Mockito.doReturn(vmSnapshotsMock).when(usageManagerImpl).findUsageVMSnapshots(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Mockito.doNothing().when(usageVMSnapshotDaoMock).update(Mockito.any(UsageVMSnapshotVO.class));

        usageManagerImpl.deleteUsageVMSnapshot(usageEventVOMock);

        Mockito.verify(usageVMSnapshotDaoMock, Mockito.times(3)).update(Mockito.any(UsageVMSnapshotVO.class));
    }

    @Test
    public void handleVMSnapshotEventTestCreateVMSnapshot() {
        Mockito.when(usageEventVOMock.getType()).thenReturn(EventTypes.EVENT_VM_SNAPSHOT_CREATE);
        Mockito.doNothing().when(this.usageManagerImpl).createUsageVMSnapshot(usageEventVOMock);

        this.usageManagerImpl.handleVMSnapshotEvent(usageEventVOMock);

        Mockito.verify(usageManagerImpl).createUsageVMSnapshot(usageEventVOMock);
        Mockito.verify(usageManagerImpl, Mockito.never()).deleteUsageVMSnapshot(usageEventVOMock);
    }

    @Test
    public void handleVMSnapshotEventTestDeleteVMSnapshot() {
        Mockito.when(usageEventVOMock.getType()).thenReturn(EventTypes.EVENT_VM_SNAPSHOT_DELETE);
        Mockito.doNothing().when(this.usageManagerImpl).deleteUsageVMSnapshot(usageEventVOMock);

        this.usageManagerImpl.handleVMSnapshotEvent(usageEventVOMock);

        Mockito.verify(usageManagerImpl).deleteUsageVMSnapshot(usageEventVOMock);
        Mockito.verify(usageManagerImpl, Mockito.never()).createUsageVMSnapshot(usageEventVOMock);
    }

    @Test
    public void handleVMSnapshotEventTestEventIsNeitherAddNorRemove() {
        Mockito.when(this.usageEventVOMock.getType()).thenReturn("VPN.USER.UPDATE");

        this.usageManagerImpl.handleVMSnapshotEvent(usageEventVOMock);

        Mockito.verify(usageManagerImpl, Mockito.never()).createUsageVpnUser(usageEventVOMock,accountMock);
        Mockito.verify(usageManagerImpl, Mockito.never()).deleteUsageVpnUser(usageEventVOMock, accountMock);
    }

    private void mockNetworkOfferingEvent(String type, long resourceId, long offeringId, String resourceName, long size, String networkIdDetailValue) {
        Mockito.when(usageEventVOMock.getType()).thenReturn(type);
        Mockito.when(usageEventVOMock.getResourceId()).thenReturn(resourceId);
        Mockito.when(usageEventVOMock.getOfferingId()).thenReturn(offeringId);
        Mockito.when(usageEventVOMock.getResourceName()).thenReturn(resourceName);
        Mockito.when(usageEventVOMock.getZoneId()).thenReturn(9L);
        Mockito.when(usageEventVOMock.getAccountId()).thenReturn(accountMockId);
        Mockito.when(usageEventVOMock.getSize()).thenReturn(size);
        Mockito.when(usageEventVOMock.getCreateDate()).thenReturn(new Date());
        Mockito.when(usageEventVOMock.getId()).thenReturn(42L);
        if (networkIdDetailValue != null) {
            Mockito.when(usageEventDetailsDao.findDetail(42L, UsageEventVO.DynamicParameters.networkId.name()))
                    .thenReturn(new UsageEventDetailsVO(42L, UsageEventVO.DynamicParameters.networkId.name(), networkIdDetailValue));
        }
    }

    @Test
    public void createNetworkOfferingEventAssignPersistsRowWithNetworkIdFromEventDetail() {
        // an ordinary, non-trunk nic's ASSIGN event carries a network id too now (every emit site resolves it),
        // so every new row - trunk or not - gets network_id populated going forward
        mockNetworkOfferingEvent(EventTypes.EVENT_NETWORK_OFFERING_ASSIGN, 3L, 7L, "11", 1L, "205");

        usageManagerImpl.createNetworkOfferingEvent(usageEventVOMock);

        ArgumentCaptor<UsageNetworkOfferingVO> captor = ArgumentCaptor.forClass(UsageNetworkOfferingVO.class);
        Mockito.verify(usageNetworkOfferingDaoMock, Mockito.times(1)).persist(captor.capture());
        UsageNetworkOfferingVO persisted = captor.getValue();
        Assert.assertEquals(Long.valueOf(205L), persisted.getNetworkId());
        Assert.assertEquals(Long.valueOf(7L), persisted.getNetworkOfferingId());
        Assert.assertEquals(11L, persisted.getNicId().longValue());
    }

    @Test
    public void createNetworkOfferingEventAssignPersistsRowWithNullNetworkIdWhenDetailMissing() {
        // an old agent/emit path with no networkId detail at all must still work exactly as it always has
        mockNetworkOfferingEvent(EventTypes.EVENT_NETWORK_OFFERING_ASSIGN, 3L, 7L, "11", 1L, null);

        usageManagerImpl.createNetworkOfferingEvent(usageEventVOMock);

        ArgumentCaptor<UsageNetworkOfferingVO> captor = ArgumentCaptor.forClass(UsageNetworkOfferingVO.class);
        Mockito.verify(usageNetworkOfferingDaoMock, Mockito.times(1)).persist(captor.capture());
        Assert.assertNull(captor.getValue().getNetworkId());
    }

    @Test
    public void createNetworkOfferingEventRemoveUpdatesTheOnlyCandidateRegardlessOfNetworkId() {
        // the common, non-ambiguous case (an ordinary nic, or a historic row): exactly one open row matches the
        // (account, vm, nic, offering) key, and it must always be the one closed, network_id or not
        mockNetworkOfferingEvent(EventTypes.EVENT_NETWORK_OFFERING_REMOVE, 3L, 7L, "11", 0L, "205");
        UsageNetworkOfferingVO onlyCandidate = new UsageNetworkOfferingVO(9L, accountMockId, 2L, 3L, 7L, 11L, 205L, true, new Date(), null);
        SearchCriteria<UsageNetworkOfferingVO> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(usageNetworkOfferingDaoMock.createSearchCriteria()).thenReturn(sc);
        Mockito.when(usageNetworkOfferingDaoMock.search(sc, null)).thenReturn(Arrays.asList(onlyCandidate));

        usageManagerImpl.createNetworkOfferingEvent(usageEventVOMock);

        Mockito.verify(usageNetworkOfferingDaoMock, Mockito.times(1)).update(onlyCandidate);
        Assert.assertNotNull(onlyCandidate.getDeleted());
    }

    @Test
    public void createNetworkOfferingEventRemoveNarrowsToTheMatchingNetworkWhenMultipleCandidatesCollide() {
        // a trunk nic with two associated networks sharing the same offering: the (account, vm, nic, offering)
        // key alone matches both rows, so only the one whose network_id matches this event's network may close
        mockNetworkOfferingEvent(EventTypes.EVENT_NETWORK_OFFERING_REMOVE, 3L, 7L, "11", 0L, "206");
        UsageNetworkOfferingVO stillActiveAssociation = new UsageNetworkOfferingVO(9L, accountMockId, 2L, 3L, 7L, 11L, 205L, false, new Date(), null);
        UsageNetworkOfferingVO removedAssociation = new UsageNetworkOfferingVO(9L, accountMockId, 2L, 3L, 7L, 11L, 206L, false, new Date(), null);
        SearchCriteria<UsageNetworkOfferingVO> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(usageNetworkOfferingDaoMock.createSearchCriteria()).thenReturn(sc);
        Mockito.when(usageNetworkOfferingDaoMock.search(sc, null)).thenReturn(Arrays.asList(stillActiveAssociation, removedAssociation));

        usageManagerImpl.createNetworkOfferingEvent(usageEventVOMock);

        Mockito.verify(usageNetworkOfferingDaoMock, Mockito.times(1)).update(removedAssociation);
        Mockito.verify(usageNetworkOfferingDaoMock, Mockito.never()).update(stillActiveAssociation);
        Assert.assertNull("the still-active association on the same nic/offering must not be closed", stillActiveAssociation.getDeleted());
    }

    @Test
    public void createNetworkOfferingEventRemoveFallsBackToClosingAllCandidatesWhenNetworkIdCannotDisambiguate() {
        // defence in depth: if more than one candidate remains even after narrowing by network id (should not
        // happen given nic_network_map's uniqueness constraint), preserve today's existing fallback behaviour
        // of closing every candidate, rather than silently picking one
        mockNetworkOfferingEvent(EventTypes.EVENT_NETWORK_OFFERING_REMOVE, 3L, 7L, "11", 0L, null);
        UsageNetworkOfferingVO candidate1 = new UsageNetworkOfferingVO(9L, accountMockId, 2L, 3L, 7L, 11L, 205L, false, new Date(), null);
        UsageNetworkOfferingVO candidate2 = new UsageNetworkOfferingVO(9L, accountMockId, 2L, 3L, 7L, 11L, 206L, false, new Date(), null);
        SearchCriteria<UsageNetworkOfferingVO> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(usageNetworkOfferingDaoMock.createSearchCriteria()).thenReturn(sc);
        Mockito.when(usageNetworkOfferingDaoMock.search(sc, null)).thenReturn(Arrays.asList(candidate1, candidate2));

        usageManagerImpl.createNetworkOfferingEvent(usageEventVOMock);

        Mockito.verify(usageNetworkOfferingDaoMock, Mockito.times(1)).update(candidate1);
        Mockito.verify(usageNetworkOfferingDaoMock, Mockito.times(1)).update(candidate2);
    }

    @Test
    public void backfillNicNetworkIdOnTrunkConversionBackfillsWhenDetailPresent() {
        Mockito.when(usageEventVOMock.getResourceName()).thenReturn("11");
        Mockito.when(usageEventVOMock.getId()).thenReturn(42L);
        Mockito.when(usageEventDetailsDao.findDetail(42L, UsageEventVO.DynamicParameters.networkId.name()))
                .thenReturn(new UsageEventDetailsVO(42L, UsageEventVO.DynamicParameters.networkId.name(), "205"));

        usageManagerImpl.backfillNicNetworkIdOnTrunkConversion(usageEventVOMock);

        Mockito.verify(usageNetworkOfferingDaoMock, Mockito.times(1)).backfillNetworkIdForNic(11L, 205L);
    }

    @Test
    public void backfillNicNetworkIdOnTrunkConversionSkipsWhenDetailMissing() {
        Mockito.when(usageEventVOMock.getResourceName()).thenReturn("11");
        Mockito.when(usageEventVOMock.getId()).thenReturn(42L);
        Mockito.when(usageEventDetailsDao.findDetail(42L, UsageEventVO.DynamicParameters.networkId.name())).thenReturn(null);

        usageManagerImpl.backfillNicNetworkIdOnTrunkConversion(usageEventVOMock);

        Mockito.verify(usageNetworkOfferingDaoMock, Mockito.never()).backfillNetworkIdForNic(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    public void backfillNicNetworkIdOnTrunkConversionSkipsWhenResourceNameNotParseable() {
        Mockito.when(usageEventVOMock.getResourceName()).thenReturn("not-a-number");

        usageManagerImpl.backfillNicNetworkIdOnTrunkConversion(usageEventVOMock);

        Mockito.verify(usageNetworkOfferingDaoMock, Mockito.never()).backfillNetworkIdForNic(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    public void backfillNicNetworkIdOnTrunkConversionSkipsWhenNetworkIdDetailNotParseable() {
        Mockito.when(usageEventVOMock.getResourceName()).thenReturn("11");
        Mockito.when(usageEventVOMock.getId()).thenReturn(42L);
        Mockito.when(usageEventDetailsDao.findDetail(42L, UsageEventVO.DynamicParameters.networkId.name()))
                .thenReturn(new UsageEventDetailsVO(42L, UsageEventVO.DynamicParameters.networkId.name(), "not-a-number"));

        usageManagerImpl.backfillNicNetworkIdOnTrunkConversion(usageEventVOMock);

        Mockito.verify(usageNetworkOfferingDaoMock, Mockito.never()).backfillNetworkIdForNic(Mockito.anyLong(), Mockito.anyLong());
    }
}
