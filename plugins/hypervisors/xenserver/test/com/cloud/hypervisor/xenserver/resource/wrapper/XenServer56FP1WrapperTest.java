package com.cloud.hypervisor.xenserver.resource.wrapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FenceCommand;
import com.cloud.host.Host;
import com.cloud.hypervisor.xenserver.resource.XenServer56FP1Resource;
import com.cloud.vm.VMInstanceVO;
import com.xensource.xenapi.Connection;

@RunWith(PowerMockRunner.class)
public class XenServer56FP1WrapperTest {

    @Mock
    private XenServer56FP1Resource xenServer56Resource;

    @Test
    public void testFenceCommand() {
        final VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        final Host host = Mockito.mock(Host.class);

        final Connection conn = Mockito.mock(Connection.class);

        final FenceCommand fenceCommand = new FenceCommand(vm, host);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(xenServer56Resource.getConnection()).thenReturn(conn);

        final Answer answer = wrapper.execute(fenceCommand, xenServer56Resource);

        verify(xenServer56Resource, times(1)).getConnection();
        verify(xenServer56Resource, times(1)).checkHeartbeat(fenceCommand.getHostGuid());

        assertFalse(answer.getResult());
    }
}