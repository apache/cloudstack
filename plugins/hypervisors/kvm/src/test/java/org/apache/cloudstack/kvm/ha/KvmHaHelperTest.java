/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cloudstack.kvm.ha;

import java.util.ArrayList;
import java.util.List;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.resource.ResourceManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.cloud.vm.VMInstanceVO;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KvmHaHelperTest {

    private static final int ERROR_CODE = -1;
    private static final int EXPECTED_RUNNING_VMS_EXAMPLE_3VMs = 3;

    @Spy
    @InjectMocks
    private KvmHaHelper kvmHaHelper;
    @Mock
    private KvmHaAgentClient kvmHaAgentClient;
    @Mock
    private HostVO host;
    @Mock
    private ResourceManager resourceManager;
    @Mock
    private ClusterDao clusterDao;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void isKvmHaAgentHealthyTestAllGood() {
        boolean result = prepareAndTestIsKvmHaAgentHealthy(EXPECTED_RUNNING_VMS_EXAMPLE_3VMs, EXPECTED_RUNNING_VMS_EXAMPLE_3VMs, true);
        Assert.assertTrue(result);
    }

    @Test
    public void isKvmHaAgentHealthyTestVMsDoNotMatchButDoNotReturnFalse() {
        boolean result = prepareAndTestIsKvmHaAgentHealthy(EXPECTED_RUNNING_VMS_EXAMPLE_3VMs, 1, true);
        Assert.assertTrue(result);
    }

    @Test
    public void isKvmHaAgentHealthyTestExpectedRunningVmsButNoneListed() {
        boolean result = prepareAndTestIsKvmHaAgentHealthy(EXPECTED_RUNNING_VMS_EXAMPLE_3VMs, 0, true);
        Assert.assertFalse(result);
    }

    @Test
    public void isKvmHaAgentHealthyTestExpectedRunningVmsButNoneListedUnreachable() {
        boolean result = prepareAndTestIsKvmHaAgentHealthy(EXPECTED_RUNNING_VMS_EXAMPLE_3VMs, 0, false);
        Assert.assertFalse(result);
    }

    @Test
    public void isKvmHaAgentHealthyTestReceivedErrorCode() {
        boolean result = prepareAndTestIsKvmHaAgentHealthy(EXPECTED_RUNNING_VMS_EXAMPLE_3VMs, ERROR_CODE, true);
        Assert.assertTrue(result);
    }

    @Test
    public void isKvmHaAgentHealthyTestReceivedErrorCodeHostUnreachable() {
        boolean result = prepareAndTestIsKvmHaAgentHealthy(EXPECTED_RUNNING_VMS_EXAMPLE_3VMs, ERROR_CODE, false);
        Assert.assertFalse(result);
    }

    private boolean prepareAndTestIsKvmHaAgentHealthy(int expectedNumberOfVms, int vmsRunningOnAgent, boolean isHostAgentReachableByNeighbour) {
        List<VMInstanceVO> vmsOnHostList = new ArrayList<>();
        for (int i = 0; i < expectedNumberOfVms; i++) {
            VMInstanceVO vmInstance = Mockito.mock(VMInstanceVO.class);
            vmsOnHostList.add(vmInstance);
        }

        Mockito.doReturn(vmsOnHostList).when(kvmHaAgentClient).listVmsOnHost(Mockito.any());
        Mockito.doReturn(vmsRunningOnAgent).when(kvmHaAgentClient).countRunningVmsOnAgent(Mockito.any());
        Mockito.doReturn(isHostAgentReachableByNeighbour).when(kvmHaHelper).isHostAgentReachableByNeighbour(Mockito.any());

        return kvmHaHelper.isKvmHaAgentHealthy(host);
    }

    @Test
    public void isKvmHaWebserviceEnabledTestDefault() {
        Assert.assertFalse(kvmHaHelper.isKvmHaWebserviceEnabled(Mockito.any()));
    }

    @Test
    public void listProblematicHostsTest() {
        List<HostVO> hostsInCluster = mockProblematicCluster();
        List<HostVO> problematicNeighbors = kvmHaHelper.listProblematicHosts(hostsInCluster);
        Assert.assertEquals(5, hostsInCluster.size());
        Assert.assertEquals(4, problematicNeighbors.size());
    }

    private List<HostVO> mockProblematicCluster() {
        HostVO hostDown = Mockito.mock(HostVO.class);
        Mockito.doReturn(Status.Down).when(hostDown).getStatus();
        HostVO hostDisconnected = Mockito.mock(HostVO.class);
        Mockito.doReturn(Status.Disconnected).when(hostDisconnected).getStatus();
        HostVO hostError = Mockito.mock(HostVO.class);
        Mockito.doReturn(Status.Error).when(hostError).getStatus();
        HostVO hostAlert = Mockito.mock(HostVO.class);
        Mockito.doReturn(Status.Alert).when(hostAlert).getStatus();
        List<HostVO> hostsInCluster = mockHealthyCluster(1);
        hostsInCluster.add(hostAlert);
        hostsInCluster.add(hostDown);
        hostsInCluster.add(hostDisconnected);
        hostsInCluster.add(hostError);
        return hostsInCluster;
    }

    private List<HostVO> mockHealthyCluster(int healthyHosts) {
        HostVO hostUp = Mockito.mock(HostVO.class);
        Mockito.doReturn(Status.Up).when(hostUp).getStatus();
        List<HostVO> hostsInCluster = new ArrayList<>();
        for (int i = 0; i < healthyHosts; i++) {
            hostsInCluster.add(hostUp);
        }
        return hostsInCluster;
    }

    @Test
    public void isClusteProblematicTestProblematicCluster() {
        prepareAndTestIsClusteProblematicTest(mockProblematicCluster(), true);
    }

    @Test
    public void isClusteProblematicTestProblematicCluster10Healthy4ProblematicHosts() {
        List<HostVO> hostsInCluster = mockHealthyCluster(9);
        hostsInCluster.addAll(mockProblematicCluster());
        prepareAndTestIsClusteProblematicTest(hostsInCluster, false);
    }

    @Test
    public void isClusteProblematicTestHealthyCluster() {
        List<HostVO> hostsInCluster = mockHealthyCluster(20);
        hostsInCluster.addAll(mockProblematicCluster());
        prepareAndTestIsClusteProblematicTest(hostsInCluster, false);
    }

    private void prepareAndTestIsClusteProblematicTest(List<HostVO> problematicCluster, boolean expectedProblematicCluster) {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.doReturn(0l).when(cluster).getId();
        Mockito.doReturn("cluster-name").when(cluster).getName();
        Mockito.doReturn(problematicCluster).when(resourceManager).listAllHostsInCluster(Mockito.anyLong());
        Mockito.doReturn(cluster).when(clusterDao).findById(Mockito.anyLong());
        boolean isClusteProblematic = kvmHaHelper.isClusteProblematic(host);
        Assert.assertEquals(expectedProblematicCluster, isClusteProblematic);
    }
}
