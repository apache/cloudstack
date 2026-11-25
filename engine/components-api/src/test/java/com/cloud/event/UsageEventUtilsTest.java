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
package com.cloud.event;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.vm.Nic;
import com.cloud.vm.dao.NicNetworkMapDao;
import com.cloud.vm.dao.NicNetworkMapVO;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

/**
 * Verifies {@link UsageEventUtils#publishNicNetworkOfferingUsageEvents} - the mechanism that emits one usage
 * event per (nic, network) a nic actually bills against, rather than one per nic. The key non-regression
 * assertion is that an ordinary, non-trunk nic still produces exactly the single event it always has.
 */
@RunWith(MockitoJUnitRunner.class)
public class UsageEventUtilsTest {

    private static final String USAGE_TYPE = EventTypes.EVENT_NETWORK_OFFERING_ASSIGN;
    private static final long ACCOUNT_ID = 1L;
    private static final long ZONE_ID = 2L;
    private static final long VM_ID = 3L;
    private static final long NIC_ID = 5L;
    private static final String ENTITY_TYPE = "VirtualMachine";
    private static final String ENTITY_UUID = "vm-uuid";

    protected Map<String, Object> staticFieldValues = new HashMap<>();

    @Mock
    protected UsageEventDao usageEventDao;
    @Mock
    protected AccountDao accountDao;
    @Mock
    protected DataCenterDao dcDao;
    @Mock
    protected ConfigurationDao configDao;
    @Mock
    protected NetworkDao networkDao;
    @Mock
    protected NicNetworkMapDao nicNetworkMapDao;

    @Before
    public void setup() throws Exception {
        staticFieldValues = new HashMap<>();
        UsageEventUtils utils = new UsageEventUtils();

        for (Field field : UsageEventUtils.class.getDeclaredFields()) {
            if (field.getAnnotation(Inject.class) != null) {
                field.setAccessible(true);
                try {
                    Field mockField = this.getClass().getDeclaredField(field.getName());
                    mockField.setAccessible(true);
                    field.set(utils, mockField.get(this));
                    Field staticField = UsageEventUtils.class.getDeclaredField("s_" + field.getName());
                    staticField.setAccessible(true);
                    staticFieldValues.put(field.getName(), staticField.get(null));
                } catch (Exception e) {
                    // ignore fields with no matching mock/static counterpart
                }
            }
        }
        utils.init();

        // configDao.getValue(...) defaults to null -> event-bus publish short-circuits without needing an
        // EventDistributor bean; only the usage_event persistence path (what billing actually reads) matters here
        Mockito.when(usageEventDao.persist(Mockito.any(UsageEventVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @After
    public void teardown() throws Exception {
        UsageEventUtils utils = new UsageEventUtils();
        for (String fieldName : staticFieldValues.keySet()) {
            Field field = UsageEventUtils.class.getDeclaredField("s_" + fieldName);
            field.setAccessible(true);
            field.set(utils, staticFieldValues.get(fieldName));
        }
    }

    private NetworkVO mockNetwork(long id, long offeringId) {
        NetworkVO network = Mockito.mock(NetworkVO.class);
        Mockito.when(network.getId()).thenReturn(id);
        Mockito.when(network.getNetworkOfferingId()).thenReturn(offeringId);
        return network;
    }

    @Test
    public void ordinaryNicEmitsExactlyOneEventForItsPrimaryNetworkOnly() {
        Nic nic = Mockito.mock(Nic.class);
        Mockito.when(nic.getId()).thenReturn(NIC_ID);
        Mockito.when(nic.getNetworkId()).thenReturn(100L);
        Mockito.when(nic.getMultiNetwork()).thenReturn(false);
        NetworkVO primaryNetwork = mockNetwork(100L, 7L);
        Mockito.when(networkDao.findById(100L)).thenReturn(primaryNetwork);

        UsageEventUtils.publishNicNetworkOfferingUsageEvents(USAGE_TYPE, ACCOUNT_ID, ZONE_ID, VM_ID, ENTITY_TYPE, ENTITY_UUID, nic, 1L, true);

        Mockito.verifyNoInteractions(nicNetworkMapDao);

        ArgumentCaptor<UsageEventVO> eventCaptor = ArgumentCaptor.forClass(UsageEventVO.class);
        Mockito.verify(usageEventDao, Mockito.times(1)).persist(eventCaptor.capture());
        UsageEventVO persisted = eventCaptor.getValue();
        Assert.assertEquals(Long.toString(NIC_ID), persisted.getResourceName());
        Assert.assertEquals(Long.valueOf(7L), persisted.getOfferingId());
        Assert.assertEquals(Long.valueOf(1L), persisted.getSize());

        ArgumentCaptor<Map<String, String>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(usageEventDao, Mockito.times(1)).saveDetails(Mockito.anyLong(), detailsCaptor.capture());
        Assert.assertEquals("100", detailsCaptor.getValue().get(UsageEventVO.DynamicParameters.networkId.name()));
    }

    @Test
    public void trunkNicEmitsOneEventForItsPrimaryNetworkAndOneForEachAssociatedNetwork() {
        Nic nic = Mockito.mock(Nic.class);
        Mockito.when(nic.getId()).thenReturn(NIC_ID);
        Mockito.when(nic.getNetworkId()).thenReturn(100L);
        Mockito.when(nic.getMultiNetwork()).thenReturn(true);
        NetworkVO primaryNetwork = mockNetwork(100L, 7L);
        NetworkVO associatedNetwork1 = mockNetwork(200L, 8L);
        NetworkVO associatedNetwork2 = mockNetwork(300L, 9L);
        Mockito.when(networkDao.findById(100L)).thenReturn(primaryNetwork);
        Mockito.when(networkDao.findById(200L)).thenReturn(associatedNetwork1);
        Mockito.when(networkDao.findById(300L)).thenReturn(associatedNetwork2);
        List<NicNetworkMapVO> associations = Arrays.asList(new NicNetworkMapVO(NIC_ID, 200L), new NicNetworkMapVO(NIC_ID, 300L));
        Mockito.when(nicNetworkMapDao.listByNicId(NIC_ID)).thenReturn(associations);

        UsageEventUtils.publishNicNetworkOfferingUsageEvents(USAGE_TYPE, ACCOUNT_ID, ZONE_ID, VM_ID, ENTITY_TYPE, ENTITY_UUID, nic, 0L, true);

        ArgumentCaptor<UsageEventVO> eventCaptor = ArgumentCaptor.forClass(UsageEventVO.class);
        Mockito.verify(usageEventDao, Mockito.times(3)).persist(eventCaptor.capture());
        List<Long> offeringIds = eventCaptor.getAllValues().stream().map(UsageEventVO::getOfferingId).collect(java.util.stream.Collectors.toList());
        Assert.assertTrue(offeringIds.containsAll(Arrays.asList(7L, 8L, 9L)));
        // every one of the three rows still belongs to the same nic
        for (UsageEventVO event : eventCaptor.getAllValues()) {
            Assert.assertEquals(Long.toString(NIC_ID), event.getResourceName());
        }

        ArgumentCaptor<Map<String, String>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(usageEventDao, Mockito.times(3)).saveDetails(Mockito.anyLong(), detailsCaptor.capture());
        List<String> networkIds = detailsCaptor.getAllValues().stream()
                .map(details -> details.get(UsageEventVO.DynamicParameters.networkId.name())).collect(java.util.stream.Collectors.toList());
        Assert.assertTrue(networkIds.containsAll(Arrays.asList("100", "200", "300")));
    }

    @Test
    public void displayResourceFalseNeverPersistsAnyUsageRow() {
        Nic nic = Mockito.mock(Nic.class);
        Mockito.when(nic.getId()).thenReturn(NIC_ID);
        Mockito.when(nic.getNetworkId()).thenReturn(100L);
        Mockito.when(nic.getMultiNetwork()).thenReturn(false);
        NetworkVO primaryNetwork = mockNetwork(100L, 7L);
        Mockito.when(networkDao.findById(100L)).thenReturn(primaryNetwork);

        UsageEventUtils.publishNicNetworkOfferingUsageEvents(USAGE_TYPE, ACCOUNT_ID, ZONE_ID, VM_ID, ENTITY_TYPE, ENTITY_UUID, nic, 1L, false);

        Mockito.verify(usageEventDao, Mockito.never()).persist(Mockito.any());
        Mockito.verify(usageEventDao, Mockito.never()).saveDetails(Mockito.anyLong(), Mockito.anyMap());
    }

    @Test
    public void missingAssociatedNetworkIsSkippedRatherThanFailing() {
        Nic nic = Mockito.mock(Nic.class);
        Mockito.when(nic.getId()).thenReturn(NIC_ID);
        Mockito.when(nic.getNetworkId()).thenReturn(100L);
        Mockito.when(nic.getMultiNetwork()).thenReturn(true);
        NetworkVO primaryNetwork = mockNetwork(100L, 7L);
        Mockito.when(networkDao.findById(100L)).thenReturn(primaryNetwork);
        Mockito.when(networkDao.findById(200L)).thenReturn(null);
        Mockito.when(nicNetworkMapDao.listByNicId(NIC_ID)).thenReturn(Collections.singletonList(new NicNetworkMapVO(NIC_ID, 200L)));

        UsageEventUtils.publishNicNetworkOfferingUsageEvents(USAGE_TYPE, ACCOUNT_ID, ZONE_ID, VM_ID, ENTITY_TYPE, ENTITY_UUID, nic, 1L, true);

        Mockito.verify(usageEventDao, Mockito.times(1)).persist(Mockito.any());
    }
}
