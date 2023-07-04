package org.apache.cloudstack.cluster;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.server.ManagementServer;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.command.admin.cluster.ExecuteDrsCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.naming.ConfigurationException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ClusterDrsServiceImplTest {

    @Mock
    ClusterDrsAlgorithm condensedAlgorithm;

    @Mock
    ManagementServer managementServer;

    @Mock
    ClusterDrsAlgorithm balancedAlgorithm;

    @Mock
    ExecuteDrsCmd cmd;

    AutoCloseable closeable;

    @Mock
    private ClusterDao clusterDao;

    @Mock
    private UserVmService userVmService;

    @Mock
    private HostDao hostDao;

    @Mock
    private VMInstanceDao vmInstanceDao;

    @Spy
    @InjectMocks
    private ClusterDrsServiceImpl clusterDrsService = new ClusterDrsServiceImpl();

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        closeable = MockitoAnnotations.openMocks(this);

        HashMap<String, ClusterDrsAlgorithm> drsAlgorithmMap = new HashMap<>();
        drsAlgorithmMap.put("balanced", balancedAlgorithm);
        drsAlgorithmMap.put("condensed", condensedAlgorithm);

        clusterDrsService.setDrsAlgorithms(List.of(new ClusterDrsAlgorithm[]{balancedAlgorithm, condensedAlgorithm}));
        ReflectionTestUtils.setField(clusterDrsService, "drsAlgorithmMap", drsAlgorithmMap);
        Field f = ConfigKey.class.getDeclaredField("_defaultValue");
        f.setAccessible(true);
        f.set(clusterDrsService.ClusterDrsAlgorithm, "balanced");
        Mockito.when(cmd.getId()).thenReturn(1L);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(ExecuteDrsCmd.class);
        assertEquals(cmdList, clusterDrsService.getCommands());
    }

    @Test
    public void testExecuteDrs() throws ConfigurationException, ManagementServerException, ResourceUnavailableException, VirtualMachineMigrationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(cluster.getClusterType()).thenReturn(Cluster.ClusterType.CloudManaged);

        HostVO host1 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(1L);

        HostVO host2 = Mockito.mock(HostVO.class);
        Mockito.when(host2.getId()).thenReturn(2L);

        VMInstanceVO vm1 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm1.getId()).thenReturn(1L);
        Mockito.when(vm1.getInstanceName()).thenReturn("testVM1");
        Mockito.when(vm1.getHostId()).thenReturn(1L);

        VMInstanceVO vm2 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm2.getHostId()).thenReturn(2L);

        List<HostVO> hostList = new ArrayList<>();
        hostList.add(host1);
        hostList.add(host2);

        List<VMInstanceVO> vmList = new ArrayList<>();
        vmList.add(vm1);
        vmList.add(vm2);

        Mockito.when(hostDao.findByClusterId(1L)).thenReturn(hostList);
        Mockito.when(vmInstanceDao.listByClusterId(1L)).thenReturn(vmList);
        Mockito.when(balancedAlgorithm.needsDrs(Mockito.anyLong(), Mockito.anyMap())).thenReturn(true, false);
        Mockito.when(userVmService.migrateVirtualMachine(1L, host2)).thenReturn(vm1);
        Mockito.when(clusterDrsService.getBestMigration(Mockito.any(Cluster.class), Mockito.any(ClusterDrsAlgorithm.class), Mockito.anyList(), Mockito.anyMap())).thenReturn(new Pair<>(host2, vm1));

        int iterations = clusterDrsService.executeDrs(cluster, 0.5);

        Mockito.verify(hostDao, Mockito.times(1)).findByClusterId(1L);
        Mockito.verify(vmInstanceDao, Mockito.times(2)).listByClusterId(1L);
        Mockito.verify(balancedAlgorithm, Mockito.times(2)).needsDrs(Mockito.anyLong(), Mockito.anyMap());
        Mockito.verify(userVmService, Mockito.times(1)).migrateVirtualMachine(1L, host2);

        assertEquals(1, iterations);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testExecuteDrsClusterNotFound() {
        Mockito.when(clusterDao.findById(1L)).thenReturn(null);
        clusterDrsService.executeDrs(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testExecuteDrsClusterDisabled() {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getName()).thenReturn("testCluster");
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Disabled);

        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);

        clusterDrsService.executeDrs(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testExecuteDrsClusterNotCloudManaged() {

        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getName()).thenReturn("testCluster");
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(cluster.getClusterType()).thenReturn(Cluster.ClusterType.ExternalManaged);

        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);

        clusterDrsService.executeDrs(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testExecuteDrsInvalidIterations() {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getName()).thenReturn("testCluster");
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(cluster.getClusterType()).thenReturn(Cluster.ClusterType.CloudManaged);

        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);
        Mockito.when(cmd.getIterations()).thenReturn(0.0);

        clusterDrsService.executeDrs(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testExecuteDrsConfigurationException() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(cluster.getClusterType()).thenReturn(Cluster.ClusterType.CloudManaged);
        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);
        Mockito.when(clusterDrsService.executeDrs(cluster, 0.5)).thenThrow(new CloudRuntimeException("test"));
        Mockito.when(cmd.getIterations()).thenReturn(0.5);

        clusterDrsService.executeDrs(cmd);
    }

    @Test
    public void testGetBestMigration() {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);

        HostVO destHost = Mockito.mock(HostVO.class);

        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(2L);

        VMInstanceVO vm1 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm1.getId()).thenReturn(1L);

        VMInstanceVO vm2 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm2.getId()).thenReturn(2L);

        List<VMInstanceVO> vmList = new ArrayList<>();
        vmList.add(vm1);
        vmList.add(vm2);

        Map<Long, List<VirtualMachine>> hostVmMap = new HashMap<>();
        hostVmMap.put(host.getId(), new ArrayList<>());
        hostVmMap.get(host.getId()).add(vm1);
        hostVmMap.get(host.getId()).add(vm2);

        Mockito.when(managementServer.listHostsForMigrationOfVM(vm1.getId(), 0L, (long) hostVmMap.size(), null)).thenReturn(new Ternary<>(null, List.of(destHost), Map.of(destHost, false)));
        Mockito.when(managementServer.listHostsForMigrationOfVM(vm2.getId(), 0L, (long) hostVmMap.size(), null)).thenReturn(new Ternary<>(null, Collections.emptyList(), Collections.emptyMap()));
        Mockito.when(balancedAlgorithm.getMetrics(cluster.getId(), hostVmMap, vm1, destHost, false)).thenReturn(new Ternary<>(1.0, 0.5, 1.5));

        Pair<Host, VirtualMachine> bestMigration = clusterDrsService.getBestMigration(cluster, balancedAlgorithm, vmList, hostVmMap);

        assertEquals(destHost, bestMigration.first());
        assertEquals(vm1, bestMigration.second());
    }
}
