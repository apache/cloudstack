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

package com.cloud.hypervisor;

import org.apache.cloudstack.hypervisor.xenserver.XenserverConfigs;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class XenServerGuruTest {

    @InjectMocks
    private XenServerGuru xenServerGuru = new XenServerGuru();

    @Mock
    private HostDao hostDaoMock;

    @Mock
    private PrimaryDataStoreDao storagePoolDao;

    @Mock
    private CopyCommand copyCommandMock;

    @Mock
    private DataTO sourceDataMock;

    @Mock
    private DataTO destinationDataMock;

    private Long defaultHostId = 1l;

    @Mock
    private HostVO defaultHost;

    @Mock
    private HostVO changedHost;

    private Long changedHostId = 12l;

    private long zoneId = 100l;

    @Before
    public void beforeTest() {
        Mockito.when(sourceDataMock.getHypervisorType()).thenReturn(HypervisorType.XenServer);

        Mockito.when(copyCommandMock.getSrcTO()).thenReturn(sourceDataMock);
        Mockito.when(copyCommandMock.getDestTO()).thenReturn(destinationDataMock);

        Mockito.when(changedHost.getId()).thenReturn(changedHostId);
        Mockito.lenient().when(defaultHost.getId()).thenReturn(defaultHostId);
        Mockito.when(defaultHost.getDataCenterId()).thenReturn(zoneId);

        Mockito.when(hostDaoMock.findById(defaultHostId)).thenReturn(defaultHost);
        Mockito.lenient().when(hostDaoMock.findById(changedHostId)).thenReturn(changedHost);
    }

    @Test
    public void getCommandHostDelegationTestCommandNotCopyCommand() {
        Pair<Boolean, Long> pairHostToExecuteCommand = xenServerGuru.getCommandHostDelegation(defaultHostId, Mockito.mock(Command.class));

        assertPairOfHostToExecuteCommandIsTheDefaultHostId(pairHostToExecuteCommand);
    }

    private void assertPairOfHostToExecuteCommandIsTheDefaultHostId(Pair<Boolean, Long> pairHostToExecuteCommand) {
        Assert.assertFalse(pairHostToExecuteCommand.first());
        Assert.assertEquals(defaultHostId, pairHostToExecuteCommand.second());
    }

    @Test
    public void getCommandHostDelegationTestCommanIsStorageSubSystemCommand() {
        StorageSubSystemCommand storageSubSystemCommandMock = Mockito.mock(StorageSubSystemCommand.class);
        Pair<Boolean, Long> pairHostToExecuteCommand = xenServerGuru.getCommandHostDelegation(defaultHostId, storageSubSystemCommandMock);

        assertPairOfHostToExecuteCommandIsTheDefaultHostId(pairHostToExecuteCommand);

        Mockito.verify(storageSubSystemCommandMock).setExecuteInSequence(true);
    }

    @Test
    public void getCommandHostDelegationTestCommandIsCopyCommandButSourceDataHypervisorIsNotXenServer() {
        Mockito.when(sourceDataMock.getHypervisorType()).thenReturn(HypervisorType.Any);

        Pair<Boolean, Long> pairHostToExecuteCommand = xenServerGuru.getCommandHostDelegation(defaultHostId, copyCommandMock);

        assertPairOfHostToExecuteCommandIsTheDefaultHostId(pairHostToExecuteCommand);
    }

    @Test
    public void getCommandHostDelegationTestCommandIsCopyCommandAndSourceDataHypervisorIsXenServerButSourceAndDestinationAreNotNfsObjects() {
        Mockito.when(sourceDataMock.getDataStore()).thenReturn(Mockito.mock(DataStoreTO.class));
        Mockito.when(destinationDataMock.getDataStore()).thenReturn(Mockito.mock(DataStoreTO.class));

        Mockito.when(sourceDataMock.getHypervisorType()).thenReturn(HypervisorType.XenServer);

        Pair<Boolean, Long> pairHostToExecuteCommand = xenServerGuru.getCommandHostDelegation(defaultHostId, copyCommandMock);

        assertPairOfHostToExecuteCommandIsTheDefaultHostId(pairHostToExecuteCommand);
    }

    @Test
    public void getCommandHostDelegationTestCommandIsCopyCommandAndSourceDataHypervisorIsXenServerAndSourceAndDestinationAreNfsObjectsButSourceIsNotSnapshotType() {
        configureSourceAndDestinationDataMockDataStoreAsNfsToType();

        Mockito.when(sourceDataMock.getHypervisorType()).thenReturn(HypervisorType.XenServer);
        Mockito.when(sourceDataMock.getObjectType()).thenReturn(DataObjectType.VOLUME);

        Pair<Boolean, Long> pairHostToExecuteCommand = xenServerGuru.getCommandHostDelegation(defaultHostId, copyCommandMock);

        assertPairOfHostToExecuteCommandIsTheDefaultHostId(pairHostToExecuteCommand);
    }

    private void configureSourceAndDestinationDataMockDataStoreAsNfsToType() {
        Mockito.when(sourceDataMock.getDataStore()).thenReturn(Mockito.mock(NfsTO.class));
        Mockito.when(destinationDataMock.getDataStore()).thenReturn(Mockito.mock(NfsTO.class));
    }

    @Test
    public void getCommandHostDelegationTestCommandIsCopyCommandAndSourceDataHypervisorIsXenServerAndSourceAndDestinationAreNfsObjectsAndSourceIsSnapshotTypeButDestinationIsNotTemplateType() {
        configureSourceAndDestinationDataMockDataStoreAsNfsToType();

        Mockito.when(sourceDataMock.getHypervisorType()).thenReturn(HypervisorType.XenServer);
        Mockito.when(sourceDataMock.getObjectType()).thenReturn(DataObjectType.SNAPSHOT);
        Mockito.when(destinationDataMock.getObjectType()).thenReturn(DataObjectType.VOLUME);

        Pair<Boolean, Long> pairHostToExecuteCommand = xenServerGuru.getCommandHostDelegation(defaultHostId, copyCommandMock);

        assertPairOfHostToExecuteCommandIsTheDefaultHostId(pairHostToExecuteCommand);
    }

    @Test
    public void getCommandHostDelegationTestCommandIsCopyCommandAndSourceDataHypervisorIsXenServerAndSourceAndDestinationAreNfsObjectsAndSourceIsSnapshotAndDestinationIsTemplateButHypervisorVersionIsBlank() {
        configureSourceAndDestinationDataMockDataStoreAsNfsToType();
        configureSourceHypervisorAsXenServerAndSourceTypeAsSnapshotAndDestinationTypeAsTemplate();

        Mockito.when(changedHost.getHypervisorVersion()).thenReturn(StringUtils.EMPTY);
        Mockito.when(hostDaoMock.findHostInZoneToExecuteCommand(zoneId, HypervisorType.XenServer)).thenReturn(changedHost);

        Pair<Boolean, Long> pairHostToExecuteCommand = xenServerGuru.getCommandHostDelegation(defaultHostId, copyCommandMock);

        assertPairOfHostToExecuteCommandIsTheDefaultHostId(pairHostToExecuteCommand);
    }

    private void configureSourceHypervisorAsXenServerAndSourceTypeAsSnapshotAndDestinationTypeAsTemplate() {
        Mockito.when(sourceDataMock.getHypervisorType()).thenReturn(HypervisorType.XenServer);
        Mockito.when(sourceDataMock.getObjectType()).thenReturn(DataObjectType.SNAPSHOT);
        Mockito.when(destinationDataMock.getObjectType()).thenReturn(DataObjectType.TEMPLATE);
    }

    @Test
    public void getCommandHostDelegationTestCommandIsCopyCommandAndSourceDataHypervisorIsXenServerAndSourceAndDestinationAreNfsObjectsAndSourceIsSnapshotAndDestinationIsTemplateButHypervisorVersionIsXenServer610() {
        configureSourceAndDestinationDataMockDataStoreAsNfsToType();
        configureSourceHypervisorAsXenServerAndSourceTypeAsSnapshotAndDestinationTypeAsTemplate();

        Mockito.when(changedHost.getHypervisorVersion()).thenReturn("6.1.0");
        Mockito.when(hostDaoMock.findHostInZoneToExecuteCommand(zoneId, HypervisorType.XenServer)).thenReturn(changedHost);

        Pair<Boolean, Long> pairHostToExecuteCommand = xenServerGuru.getCommandHostDelegation(defaultHostId, copyCommandMock);

        assertPairOfHostToExecuteCommandIsTheDefaultHostId(pairHostToExecuteCommand);
    }

    @Test
    public void getCommandHostDelegationTestCommandIsCopyCommandAndSourceDataHypervisorIsXenServerAndSourceAndDestinationAreNfsObjectsAndSourceIsSnapshotAndDestinationIsTemplateAndHypervisorVersionIsXenServer620WithoutHotfixOfSnapshots() {
        configureSourceAndDestinationDataMockDataStoreAsNfsToType();
        configureSourceHypervisorAsXenServerAndSourceTypeAsSnapshotAndDestinationTypeAsTemplate();

        Mockito.when(changedHost.getHypervisorVersion()).thenReturn("6.2.0");
        Mockito.when(hostDaoMock.findHostInZoneToExecuteCommand(zoneId, HypervisorType.XenServer)).thenReturn(changedHost);

        Pair<Boolean, Long> pairHostToExecuteCommand = xenServerGuru.getCommandHostDelegation(defaultHostId, copyCommandMock);

        assertPairOfHostToExecuteCommandIsTheDefaultHostId(pairHostToExecuteCommand);
    }

    @Test
    public void getCommandHostDelegationTestCommandIsCopyCommandAndSourceDataHypervisorIsXenServerAndSourceAndDestinationAreNfsObjectsAndSourceIsSnapshotAndDestinationIsTemplateAndHypervisorVersionIsXenServer620WithHotfixOfSnapshots() {
        configureSourceAndDestinationDataMockDataStoreAsNfsToType();
        configureSourceHypervisorAsXenServerAndSourceTypeAsSnapshotAndDestinationTypeAsTemplate();

        Mockito.when(changedHost.getHypervisorVersion()).thenReturn("6.2.0");
        Mockito.when(changedHost.getDetail(XenserverConfigs.XS620HotFix)).thenReturn(XenserverConfigs.XSHotFix62ESP1004);

        Mockito.when(hostDaoMock.findHostInZoneToExecuteCommand(zoneId, HypervisorType.XenServer)).thenReturn(changedHost);

        Pair<Boolean, Long> pairHostToExecuteCommand = xenServerGuru.getCommandHostDelegation(defaultHostId, copyCommandMock);

        Assert.assertTrue(pairHostToExecuteCommand.first());
        Assert.assertEquals(changedHostId, pairHostToExecuteCommand.second());
    }

    @Test
    public void getCommandHostDelegationTestCommandIsCopyCommandAndSourceDataHypervisorIsXenServerAndSourceAndDestinationAreNfsObjectsAndSourceIsSnapshotAndDestinationIsTemplateAndHypervisorVersionIsXenServer650() {
        configureSourceAndDestinationDataMockDataStoreAsNfsToType();
        configureSourceHypervisorAsXenServerAndSourceTypeAsSnapshotAndDestinationTypeAsTemplate();

        Mockito.when(changedHost.getHypervisorVersion()).thenReturn("6.5.0");

        Mockito.when(hostDaoMock.findHostInZoneToExecuteCommand(zoneId, HypervisorType.XenServer)).thenReturn(changedHost);

        Pair<Boolean, Long> pairHostToExecuteCommand = xenServerGuru.getCommandHostDelegation(defaultHostId, copyCommandMock);

        Assert.assertTrue(pairHostToExecuteCommand.first());
        Assert.assertEquals(changedHostId, pairHostToExecuteCommand.second());
    }
}
