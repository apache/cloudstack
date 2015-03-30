package com.cloud.hypervisor.xenserver.resource.wrapper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.hypervisor.xenserver.resource.XenServer56Resource;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Connection.class, Host.Record.class })
public class XenServer56WrapperTest {

    @Mock
    private XenServer56Resource xenServer56Resource;

    @Test
    public void testCheckOnHostCommand() {
        final com.cloud.host.Host host = Mockito.mock(com.cloud.host.Host.class);
        final CheckOnHostCommand onHostCommand = new CheckOnHostCommand(host);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(onHostCommand, xenServer56Resource);

        assertTrue(answer.getResult());
    }
}
