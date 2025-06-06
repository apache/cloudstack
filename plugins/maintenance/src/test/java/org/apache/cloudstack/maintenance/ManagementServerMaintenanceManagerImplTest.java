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

package org.apache.cloudstack.maintenance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.agent.lb.IndirectAgentLB;
import org.apache.cloudstack.api.command.CancelMaintenanceCmd;
import org.apache.cloudstack.api.command.CancelShutdownCmd;
import org.apache.cloudstack.api.command.PrepareForMaintenanceCmd;
import org.apache.cloudstack.api.command.PrepareForShutdownCmd;
import org.apache.cloudstack.api.command.TriggerShutdownCmd;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.management.ManagementServerHost;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.exception.CloudRuntimeException;


@RunWith(MockitoJUnitRunner.class)
public class ManagementServerMaintenanceManagerImplTest {

    @Spy
    @InjectMocks
    ManagementServerMaintenanceManagerImpl spy;

    @Mock
    AsyncJobManager jobManagerMock;

    @Mock
    IndirectAgentLB indirectAgentLBMock;

    @Mock
    AgentManager agentManagerMock;

    @Mock
    ClusterManager clusterManagerMock;

    @Mock
    HostDao hostDao;

    @Mock
    ManagementServerHostDao msHostDao;

    private long prepareCountPendingJobs() {
        long expectedCount = 1L;
        Mockito.doReturn(expectedCount).when(jobManagerMock).countPendingNonPseudoJobs(1L);
        return expectedCount;
    }

    @Test
    public void countPendingJobs() {
        long expectedCount = prepareCountPendingJobs();
        long count = spy.countPendingJobs(1L);
        Assert.assertEquals(expectedCount, count);
    }

    @Test
    public void prepareForShutdown() {
        Mockito.doNothing().when(jobManagerMock).disableAsyncJobs();
        spy.prepareForShutdown();
        Mockito.verify(jobManagerMock).disableAsyncJobs();

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForShutdown();
        });

        Mockito.doNothing().when(jobManagerMock).enableAsyncJobs();
        spy.cancelShutdown();
        Mockito.verify(jobManagerMock).enableAsyncJobs();
    }

    @Test
    public void cancelShutdown() {
        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.cancelShutdown();
        });
    }

    @Test
    public void triggerShutdown() {
        Mockito.doNothing().when(jobManagerMock).disableAsyncJobs();
        Mockito.lenient().when(spy.isShutdownTriggered()).thenReturn(false);
        spy.triggerShutdown();
        Mockito.verify(jobManagerMock).disableAsyncJobs();

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.triggerShutdown();
        });
    }

    @Test
    public void prepareForShutdownCmdNoMsHost() {
        Mockito.when(msHostDao.findById(1L)).thenReturn(null);
        PrepareForShutdownCmd cmd = mock(PrepareForShutdownCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForShutdown(cmd);
        });
    }

    @Test
    public void prepareForShutdownCmdMsHostWithNonUpState() {
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getState()).thenReturn(ManagementServerHost.State.Maintenance);
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost);
        PrepareForShutdownCmd cmd = mock(PrepareForShutdownCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForShutdown(cmd);
        });
    }

    @Test
    public void prepareForShutdownCmdOtherMsHostsInPreparingState() {
        ManagementServerHostVO msHost1 = mock(ManagementServerHostVO.class);
        Mockito.when(msHost1.getState()).thenReturn(ManagementServerHost.State.Up);
        ManagementServerHostVO msHost2 = mock(ManagementServerHostVO.class);
        List<ManagementServerHostVO> msHostList = new ArrayList<>();
        msHostList.add(msHost2);
        Mockito.lenient().when(msHostDao.listBy(any())).thenReturn(msHostList);
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost1);
        PrepareForShutdownCmd cmd = mock(PrepareForShutdownCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForShutdown(cmd);
        });
    }

    @Test
    public void prepareForShutdownCmdNullResponseFromClusterManager() {
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getState()).thenReturn(ManagementServerHost.State.Up);
        List<ManagementServerHostVO> msHostList = new ArrayList<>();
        Mockito.lenient().when(msHostDao.listBy(any())).thenReturn(msHostList);
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost);
        PrepareForShutdownCmd cmd = mock(PrepareForShutdownCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);
        Mockito.when(clusterManagerMock.execute(anyString(), anyLong(), anyString(), anyBoolean())).thenReturn(null);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForShutdown(cmd);
        });
    }

    @Test
    public void prepareForShutdownCmdFailedResponseFromClusterManager() {
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getState()).thenReturn(ManagementServerHost.State.Up);
        List<ManagementServerHostVO> msHostList = new ArrayList<>();
        Mockito.lenient().when(msHostDao.listBy(any())).thenReturn(msHostList);
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost);
        PrepareForShutdownCmd cmd = mock(PrepareForShutdownCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);
        Mockito.when(clusterManagerMock.execute(anyString(), anyLong(), anyString(), anyBoolean())).thenReturn("Failed");

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForShutdown(cmd);
        });
    }

    @Test
    public void prepareForShutdownCmdSuccessResponseFromClusterManager() {
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getState()).thenReturn(ManagementServerHost.State.Up);
        Mockito.lenient().when(msHostDao.listBy(any())).thenReturn(new ArrayList<>());
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost);
        Mockito.when(hostDao.listByMs(anyLong())).thenReturn(new ArrayList<>());
        PrepareForShutdownCmd cmd = mock(PrepareForShutdownCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);
        Mockito.when(clusterManagerMock.execute(anyString(), anyLong(), anyString(), anyBoolean())).thenReturn("Success");

        spy.prepareForShutdown(cmd);
        Mockito.verify(clusterManagerMock, Mockito.times(1)).execute(anyString(), anyLong(), anyString(), anyBoolean());
    }

    @Test
    public void cancelShutdownCmdNoMsHost() {
        Mockito.when(msHostDao.findById(1L)).thenReturn(null);
        CancelShutdownCmd cmd = mock(CancelShutdownCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.cancelShutdown(cmd);
        });
    }

    @Test
    public void cancelShutdownCmdMsHostNotInShutdownState() {
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getState()).thenReturn(ManagementServerHost.State.Up);
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost);
        CancelShutdownCmd cmd = mock(CancelShutdownCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.cancelShutdown(cmd);
        });
    }

    @Test
    public void cancelShutdownCmd() {
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getState()).thenReturn(ManagementServerHost.State.ReadyToShutDown);
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost);
        CancelShutdownCmd cmd = mock(CancelShutdownCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);
        Mockito.when(clusterManagerMock.execute(anyString(), anyLong(), anyString(), anyBoolean())).thenReturn("Success");

        spy.cancelShutdown(cmd);
        Mockito.verify(clusterManagerMock, Mockito.times(1)).execute(anyString(), anyLong(), anyString(), anyBoolean());
    }

    @Test
    public void triggerShutdownCmdNoMsHost() {
        Mockito.when(msHostDao.findById(1L)).thenReturn(null);
        TriggerShutdownCmd cmd = mock(TriggerShutdownCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.triggerShutdown(cmd);
        });
    }

    @Test
    public void triggerShutdownCmdMsHostWithNotRightState() {
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getState()).thenReturn(ManagementServerHost.State.PreparingForMaintenance);
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost);
        TriggerShutdownCmd cmd = mock(TriggerShutdownCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.triggerShutdown(cmd);
        });
    }

    @Test
    public void triggerShutdownCmdMsInUpStateAndOtherMsHostsInPreparingState() {
        ManagementServerHostVO msHost1 = mock(ManagementServerHostVO.class);
        Mockito.when(msHost1.getState()).thenReturn(ManagementServerHost.State.Up);
        ManagementServerHostVO msHost2 = mock(ManagementServerHostVO.class);
        List<ManagementServerHostVO> msHostList = new ArrayList<>();
        msHostList.add(msHost2);
        Mockito.lenient().when(msHostDao.listBy(any())).thenReturn(msHostList);
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost1);
        TriggerShutdownCmd cmd = mock(TriggerShutdownCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.triggerShutdown(cmd);
        });
    }

    @Test
    public void triggerShutdownCmd() {
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getState()).thenReturn(ManagementServerHost.State.ReadyToShutDown);
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost);
        TriggerShutdownCmd cmd = mock(TriggerShutdownCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);
        Mockito.when(clusterManagerMock.execute(anyString(), anyLong(), anyString(), anyBoolean())).thenReturn("Success");

        spy.triggerShutdown(cmd);
        Mockito.verify(clusterManagerMock, Mockito.times(1)).execute(anyString(), anyLong(), anyString(), anyBoolean());
    }

    @Test
    public void prepareForMaintenanceAndCancelFromMaintenanceState() {
        Mockito.doNothing().when(jobManagerMock).disableAsyncJobs();
        spy.prepareForMaintenance("static");
        Mockito.verify(jobManagerMock).disableAsyncJobs();

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForMaintenance("static");
        });

        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getState()).thenReturn(ManagementServerHost.State.Maintenance);
        Mockito.when(msHostDao.findByMsid(anyLong())).thenReturn(msHost);
        Mockito.doNothing().when(jobManagerMock).enableAsyncJobs();
        spy.cancelMaintenance();
        Mockito.verify(jobManagerMock).enableAsyncJobs();
        Mockito.verify(spy, Mockito.times(1)).onCancelMaintenance();
    }

    @Test
    public void prepareForMaintenanceAndCancelFromPreparingForMaintenanceState() {
        Mockito.doNothing().when(jobManagerMock).disableAsyncJobs();
        spy.prepareForMaintenance("static");
        Mockito.verify(jobManagerMock).disableAsyncJobs();

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForMaintenance("static");
        });

        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getState()).thenReturn(ManagementServerHost.State.PreparingForMaintenance);
        Mockito.when(msHostDao.findByMsid(anyLong())).thenReturn(msHost);
        Mockito.doNothing().when(jobManagerMock).enableAsyncJobs();
        spy.cancelMaintenance();
        Mockito.verify(jobManagerMock).enableAsyncJobs();
        Mockito.verify(spy, Mockito.times(1)).onCancelPreparingForMaintenance();
    }

    @Test
    public void cancelMaintenance() {
        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.cancelMaintenance();
        });
    }

    @Test
    public void cancelPreparingForMaintenance() {
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Mockito.when(msHostDao.findByMsid(anyLong())).thenReturn(msHost);

        spy.cancelPreparingForMaintenance(null);
        Mockito.verify(jobManagerMock).enableAsyncJobs();
        Mockito.verify(spy, Mockito.times(1)).onCancelPreparingForMaintenance();
    }

    @Test
    public void prepareForMaintenanceCmdNoOtherMsHostsWithUpState() {
        Mockito.when(msHostDao.listBy(any())).thenReturn(new ArrayList<>());
        PrepareForMaintenanceCmd cmd = mock(PrepareForMaintenanceCmd.class);
        Mockito.when(cmd.getAlgorithm()).thenReturn("test algorithm");
        Mockito.doNothing().when(indirectAgentLBMock).checkLBAlgorithmName(anyString());

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForMaintenance(cmd);
        });
    }

    @Test
    public void prepareForMaintenanceCmdOnlyOneMsHostsWithUpState() {
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        List<ManagementServerHostVO> msHostList = new ArrayList<>();
        msHostList.add(msHost);
        Mockito.when(msHostDao.listBy(ManagementServerHost.State.Up)).thenReturn(msHostList);
        PrepareForMaintenanceCmd cmd = mock(PrepareForMaintenanceCmd.class);
        Mockito.when(cmd.getAlgorithm()).thenReturn("test algorithm");
        Mockito.doNothing().when(indirectAgentLBMock).checkLBAlgorithmName(anyString());

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForMaintenance(cmd);
        });
    }

    @Test
    public void prepareForMaintenanceCmdNoMsHost() {
        ManagementServerHostVO msHost1 = mock(ManagementServerHostVO.class);
        ManagementServerHostVO msHost2 = mock(ManagementServerHostVO.class);
        List<ManagementServerHostVO> msHostList = new ArrayList<>();
        msHostList.add(msHost1);
        msHostList.add(msHost2);
        Mockito.when(msHostDao.listBy(ManagementServerHost.State.Up)).thenReturn(msHostList);
        Mockito.when(msHostDao.findById(1L)).thenReturn(null);
        PrepareForMaintenanceCmd cmd = mock(PrepareForMaintenanceCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForMaintenance(cmd);
        });
    }

    @Test
    public void prepareForMaintenanceCmdMsHostWithNonUpState() {
        ManagementServerHostVO msHost1 = mock(ManagementServerHostVO.class);
        Mockito.when(msHost1.getState()).thenReturn(ManagementServerHost.State.Maintenance);
        ManagementServerHostVO msHost2 = mock(ManagementServerHostVO.class);
        List<ManagementServerHostVO> msHostList = new ArrayList<>();
        msHostList.add(msHost1);
        msHostList.add(msHost2);
        Mockito.when(msHostDao.listBy(ManagementServerHost.State.Up)).thenReturn(msHostList);
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost1);
        PrepareForMaintenanceCmd cmd = mock(PrepareForMaintenanceCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForMaintenance(cmd);
        });
    }

    @Test
    public void prepareForMaintenanceCmdOtherMsHostsInPreparingState() {
        ManagementServerHostVO msHost1 = mock(ManagementServerHostVO.class);
        Mockito.when(msHost1.getState()).thenReturn(ManagementServerHost.State.Up);
        ManagementServerHostVO msHost2 = mock(ManagementServerHostVO.class);
        List<ManagementServerHostVO> msHostList1 = new ArrayList<>();
        msHostList1.add(msHost1);
        msHostList1.add(msHost2);
        ManagementServerHostVO msHost3 = mock(ManagementServerHostVO.class);
        List<ManagementServerHostVO> msHostList2 = new ArrayList<>();
        msHostList2.add(msHost3);
        Mockito.when(msHostDao.listBy(ManagementServerHost.State.Up)).thenReturn(msHostList1);
        Mockito.when(msHostDao.listBy(ManagementServerHost.State.PreparingForMaintenance, ManagementServerHost.State.PreparingForShutDown)).thenReturn(msHostList2);
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost1);
        PrepareForMaintenanceCmd cmd = mock(PrepareForMaintenanceCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForMaintenance(cmd);
        });
    }

    @Test
    public void prepareForMaintenanceCmdNoIndirectMsHosts() {
        ManagementServerHostVO msHost1 = mock(ManagementServerHostVO.class);
        Mockito.when(msHost1.getState()).thenReturn(ManagementServerHost.State.Up);
        ManagementServerHostVO msHost2 = mock(ManagementServerHostVO.class);
        List<ManagementServerHostVO> msHostList = new ArrayList<>();
        msHostList.add(msHost1);
        msHostList.add(msHost2);
        Mockito.when(msHostDao.listBy(ManagementServerHost.State.Up)).thenReturn(msHostList);
        Mockito.when(msHostDao.listBy(ManagementServerHost.State.PreparingForMaintenance, ManagementServerHost.State.PreparingForShutDown)).thenReturn(new ArrayList<>());
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost1);
        Mockito.when(msHostDao.listNonUpStateMsIPs()).thenReturn(new ArrayList<>());
        PrepareForMaintenanceCmd cmd = mock(PrepareForMaintenanceCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);
        Mockito.when(indirectAgentLBMock.haveAgentBasedHosts(anyLong())).thenReturn(true);
        Mockito.when(indirectAgentLBMock.getManagementServerList()).thenReturn(new ArrayList<>());

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForMaintenance(cmd);
        });
    }

    @Test
    public void prepareForMaintenanceCmdNullResponseFromClusterManager() {
        ManagementServerHostVO msHost1 = mock(ManagementServerHostVO.class);
        Mockito.when(msHost1.getState()).thenReturn(ManagementServerHost.State.Up);
        ManagementServerHostVO msHost2 = mock(ManagementServerHostVO.class);
        List<ManagementServerHostVO> msHostList = new ArrayList<>();
        msHostList.add(msHost1);
        msHostList.add(msHost2);
        Mockito.when(msHostDao.listBy(ManagementServerHost.State.Up)).thenReturn(msHostList);
        Mockito.when(msHostDao.listBy(ManagementServerHost.State.PreparingForMaintenance, ManagementServerHost.State.PreparingForShutDown)).thenReturn(new ArrayList<>());
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost1);
        PrepareForMaintenanceCmd cmd = mock(PrepareForMaintenanceCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);
        Mockito.when(indirectAgentLBMock.haveAgentBasedHosts(anyLong())).thenReturn(false);
        Mockito.when(clusterManagerMock.execute(anyString(), anyLong(), anyString(), anyBoolean())).thenReturn(null);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForMaintenance(cmd);
        });
    }

    @Test
    public void prepareForMaintenanceCmdFailedResponseFromClusterManager() {
        ManagementServerHostVO msHost1 = mock(ManagementServerHostVO.class);
        Mockito.when(msHost1.getState()).thenReturn(ManagementServerHost.State.Up);
        ManagementServerHostVO msHost2 = mock(ManagementServerHostVO.class);
        List<ManagementServerHostVO> msHostList = new ArrayList<>();
        msHostList.add(msHost1);
        msHostList.add(msHost2);
        Mockito.when(msHostDao.listBy(ManagementServerHost.State.Up)).thenReturn(msHostList);
        Mockito.when(msHostDao.listBy(ManagementServerHost.State.PreparingForMaintenance, ManagementServerHost.State.PreparingForShutDown)).thenReturn(new ArrayList<>());
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost1);
        PrepareForMaintenanceCmd cmd = mock(PrepareForMaintenanceCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);
        Mockito.when(indirectAgentLBMock.haveAgentBasedHosts(anyLong())).thenReturn(false);
        Mockito.when(clusterManagerMock.execute(anyString(), anyLong(), anyString(), anyBoolean())).thenReturn("Failed");

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForMaintenance(cmd);
        });
    }

    @Test
    public void prepareForMaintenanceCmdSuccessResponseFromClusterManager() {
        ManagementServerHostVO msHost1 = mock(ManagementServerHostVO.class);
        Mockito.when(msHost1.getState()).thenReturn(ManagementServerHost.State.Up);
        ManagementServerHostVO msHost2 = mock(ManagementServerHostVO.class);
        List<ManagementServerHostVO> msHostList = new ArrayList<>();
        msHostList.add(msHost1);
        msHostList.add(msHost2);
        Mockito.when(msHostDao.listBy(ManagementServerHost.State.Up)).thenReturn(msHostList);
        Mockito.when(msHostDao.listBy(ManagementServerHost.State.PreparingForMaintenance, ManagementServerHost.State.PreparingForShutDown)).thenReturn(new ArrayList<>());
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost1);
        PrepareForMaintenanceCmd cmd = mock(PrepareForMaintenanceCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);
        Mockito.when(indirectAgentLBMock.haveAgentBasedHosts(anyLong())).thenReturn(false);
        Mockito.when(hostDao.listByMs(anyLong())).thenReturn(new ArrayList<>());
        Mockito.when(clusterManagerMock.execute(anyString(), anyLong(), anyString(), anyBoolean())).thenReturn("Success");

        spy.prepareForMaintenance(cmd);
        Mockito.verify(clusterManagerMock, Mockito.times(1)).execute(anyString(), anyLong(), anyString(), anyBoolean());
    }

    @Test
    public void cancelMaintenanceCmdNoMsHost() {
        Mockito.when(msHostDao.findById(1L)).thenReturn(null);
        CancelMaintenanceCmd cmd = mock(CancelMaintenanceCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.cancelMaintenance(cmd);
        });
    }

    @Test
    public void cancelMaintenanceCmdMsHostNotInMaintenanceState() {
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getState()).thenReturn(ManagementServerHost.State.Up);
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost);
        CancelMaintenanceCmd cmd = mock(CancelMaintenanceCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.cancelMaintenance(cmd);
        });
    }

    @Test
    public void cancelMaintenanceCmd() {
        ManagementServerHostVO msHost = mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getState()).thenReturn(ManagementServerHost.State.Maintenance);
        Mockito.when(msHostDao.findById(1L)).thenReturn(msHost);
        CancelMaintenanceCmd cmd = mock(CancelMaintenanceCmd.class);
        Mockito.when(cmd.getManagementServerId()).thenReturn(1L);
        Mockito.when(clusterManagerMock.execute(anyString(), anyLong(), anyString(), anyBoolean())).thenReturn("Success");

        spy.cancelMaintenance(cmd);
        Mockito.verify(clusterManagerMock, Mockito.times(1)).execute(anyString(), anyLong(), anyString(), anyBoolean());
    }
}
