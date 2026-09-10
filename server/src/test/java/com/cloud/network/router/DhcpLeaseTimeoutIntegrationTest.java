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
package com.cloud.network.router;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingDetailsDao;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.NicDao;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Integration tests for DHCP lease timeout functionality.
 * Tests the end-to-end flow from ConfigKey through DhcpEntryCommand creation.
 */
@RunWith(MockitoJUnitRunner.class)
public class DhcpLeaseTimeoutIntegrationTest {

    @InjectMocks
    protected CommandSetupHelper commandSetupHelper = new CommandSetupHelper();

    @Mock
    NicDao nicDao;
    @Mock
    NetworkDao networkDao;
    @Mock
    NetworkModel networkModel;
    @Mock
    NetworkOfferingDao networkOfferingDao;
    @Mock
    NetworkOfferingDetailsDao networkOfferingDetailsDao;
    @Mock
    NetworkDetailsDao networkDetailsDao;
    @Mock
    RouterControlHelper routerControlHelper;
    @Mock
    DataCenterDao dcDao;

    private VirtualRouter mockRouter;
    private UserVmVO mockVm;
    private NicVO mockNic;
    private DataCenterVO mockDc;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(commandSetupHelper, "_nicDao", nicDao);
        ReflectionTestUtils.setField(commandSetupHelper, "_networkDao", networkDao);
        ReflectionTestUtils.setField(commandSetupHelper, "_networkModel", networkModel);
        ReflectionTestUtils.setField(commandSetupHelper, "_networkOfferingDao", networkOfferingDao);
        ReflectionTestUtils.setField(commandSetupHelper, "networkOfferingDetailsDao", networkOfferingDetailsDao);
        ReflectionTestUtils.setField(commandSetupHelper, "networkDetailsDao", networkDetailsDao);
        ReflectionTestUtils.setField(commandSetupHelper, "_routerControlHelper", routerControlHelper);
        ReflectionTestUtils.setField(commandSetupHelper, "_dcDao", dcDao);

        // Create common mocks
        mockRouter = Mockito.mock(VirtualRouter.class);
        mockVm = Mockito.mock(UserVmVO.class);
        mockNic = Mockito.mock(NicVO.class);
        mockDc = Mockito.mock(DataCenterVO.class);

        // Setup default mock behaviors
        when(mockRouter.getId()).thenReturn(100L);
        when(mockRouter.getInstanceName()).thenReturn("r-100-VM");
        when(mockRouter.getDataCenterId()).thenReturn(1L);

        when(mockVm.getHostName()).thenReturn("test-vm");

        when(mockNic.getMacAddress()).thenReturn("02:00:0a:0b:0c:0d");
        when(mockNic.getIPv4Address()).thenReturn("10.1.1.10");
        when(mockNic.getIPv6Address()).thenReturn(null);
        when(mockNic.getNetworkId()).thenReturn(400L);
        when(mockNic.isDefaultNic()).thenReturn(true);

        when(dcDao.findById(anyLong())).thenReturn(mockDc);
        when(mockDc.getNetworkType()).thenReturn(com.cloud.dc.DataCenter.NetworkType.Advanced);
        when(routerControlHelper.getRouterControlIp(anyLong())).thenReturn("10.1.1.1");
        when(routerControlHelper.getRouterIpInNetwork(anyLong(), anyLong())).thenReturn("10.1.1.1");
        when(networkModel.getExecuteInSeqNtwkElmtCmd()).thenReturn(false);
    }

    @Test
    public void testDhcpEntryCommandContainsLeaseTime() {
        // Test that DhcpEntryCommand includes the lease time from ConfigKey
        Commands cmds = new Commands(Command.OnError.Continue);
        commandSetupHelper.createDhcpEntryCommand(mockRouter, mockVm, mockNic, false, cmds);

        Assert.assertEquals("Should have one DHCP command", 1, cmds.size());
        DhcpEntryCommand dhcpCmd = (DhcpEntryCommand) cmds.toCommands()[0];
        Assert.assertNotNull("DHCP command should not be null", dhcpCmd);
        Assert.assertNotNull("Lease time should not be null", dhcpCmd.getLeaseTime());

        // Default value should be 0 (infinite)
        Assert.assertEquals("Default lease time should be 0", Long.valueOf(0L), dhcpCmd.getLeaseTime());
    }

    @Test
    public void testDhcpEntryCommandUsesZoneScopedValue() {
        // Test that the command uses zone-scoped configuration
        Long zoneId = mockRouter.getDataCenterId();
        Integer expectedLeaseTime = NetworkOrchestrationService.DhcpLeaseTimeout.valueIn(zoneId);

        Commands cmds = new Commands(Command.OnError.Continue);
        commandSetupHelper.createDhcpEntryCommand(mockRouter, mockVm, mockNic, false, cmds);

        DhcpEntryCommand dhcpCmd = (DhcpEntryCommand) cmds.toCommands()[0];
        Assert.assertEquals("Lease time should match zone-scoped config",
                expectedLeaseTime.longValue(), dhcpCmd.getLeaseTime().longValue());
    }

    @Test
    public void testInfiniteLeaseWithZeroValue() {
        // Test that 0 value represents infinite lease
        ConfigKey<Integer> configKey = NetworkOrchestrationService.DhcpLeaseTimeout;
        Assert.assertEquals("Default value should be 0 for infinite lease", "0", configKey.defaultValue());

        Commands cmds = new Commands(Command.OnError.Continue);
        commandSetupHelper.createDhcpEntryCommand(mockRouter, mockVm, mockNic, false, cmds);

        DhcpEntryCommand dhcpCmd = (DhcpEntryCommand) cmds.toCommands()[0];
        Assert.assertEquals("Lease time 0 represents infinite lease", Long.valueOf(0L), dhcpCmd.getLeaseTime());
    }

    @Test
    public void testDhcpCommandForNonDefaultNic() {
        // Test DHCP command creation for non-default NIC
        when(mockNic.isDefaultNic()).thenReturn(false);

        Commands cmds = new Commands(Command.OnError.Continue);
        commandSetupHelper.createDhcpEntryCommand(mockRouter, mockVm, mockNic, false, cmds);

        DhcpEntryCommand dhcpCmd = (DhcpEntryCommand) cmds.toCommands()[0];
        Assert.assertNotNull("DHCP command should be created for non-default NIC", dhcpCmd);
        Assert.assertNotNull("Lease time should be set even for non-default NIC", dhcpCmd.getLeaseTime());
        Assert.assertFalse("Command should reflect non-default NIC", dhcpCmd.isDefault());
    }

    @Test
    public void testDhcpCommandWithRemoveFlag() {
        // Test DHCP command with remove flag set
        Commands cmds = new Commands(Command.OnError.Continue);
        commandSetupHelper.createDhcpEntryCommand(mockRouter, mockVm, mockNic, true, cmds);

        DhcpEntryCommand dhcpCmd = (DhcpEntryCommand) cmds.toCommands()[0];
        Assert.assertNotNull("DHCP command should be created even with remove flag", dhcpCmd);
        Assert.assertTrue("Remove flag should be set", dhcpCmd.isRemove());
        // Lease time should still be included even for removal
        Assert.assertNotNull("Lease time should be present even for removal", dhcpCmd.getLeaseTime());
    }
}
