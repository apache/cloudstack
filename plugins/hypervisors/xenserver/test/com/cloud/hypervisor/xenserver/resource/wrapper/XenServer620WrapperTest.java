package com.cloud.hypervisor.xenserver.resource.wrapper;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.hypervisor.xenserver.resource.XenServer620Resource;
import com.cloud.network.PhysicalNetworkSetupInfo;

@RunWith(PowerMockRunner.class)
public class XenServer620WrapperTest {

    @Test
    public void testCheckNetworkCommandFailure() {
        final XenServer620Resource xenServer620Resource = new XenServer620Resource();

        final PhysicalNetworkSetupInfo info = new PhysicalNetworkSetupInfo();

        final List<PhysicalNetworkSetupInfo> setupInfos = new ArrayList<PhysicalNetworkSetupInfo>();
        setupInfos.add(info);

        final CheckNetworkCommand checkNet = new CheckNetworkCommand(setupInfos);

        final Answer answer = xenServer620Resource.executeRequest(checkNet);

        assertTrue(answer.getResult());
    }
}