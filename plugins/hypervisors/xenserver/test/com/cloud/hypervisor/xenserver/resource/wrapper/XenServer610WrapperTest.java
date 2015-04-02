package com.cloud.hypervisor.xenserver.resource.wrapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.SetupCommand;
import com.cloud.host.HostEnvironment;
import com.cloud.hypervisor.xenserver.resource.XenServer610Resource;
import com.cloud.network.PhysicalNetworkSetupInfo;

@RunWith(PowerMockRunner.class)
public class XenServer610WrapperTest {

    @Mock
    protected XenServer610Resource xenServer610Resource;

    @Test
    public void testCheckNetworkCommandFailure() {
        final XenServer610Resource xenServer610Resource = new XenServer610Resource();

        final PhysicalNetworkSetupInfo info = new PhysicalNetworkSetupInfo();

        final List<PhysicalNetworkSetupInfo> setupInfos = new ArrayList<PhysicalNetworkSetupInfo>();
        setupInfos.add(info);

        final CheckNetworkCommand checkNet = new CheckNetworkCommand(setupInfos);

        final Answer answer = xenServer610Resource.executeRequest(checkNet);

        assertTrue(answer.getResult());
    }

    @Test
    public void testSetupCommand() {
        final XenServer610Resource xenServer610Resource = new XenServer610Resource();

        final HostEnvironment env = Mockito.mock(HostEnvironment.class);

        final SetupCommand setupCommand = new SetupCommand(env);

        final Answer answer = xenServer610Resource.executeRequest(setupCommand);

        assertFalse(answer.getResult());
    }
}