package com.cloud.hypervisor.xenserver.resource.wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;

@RunWith(MockitoJUnitRunner.class)
public class CitrixRequestWrapperTest {

    @Mock
    protected CitrixResourceBase citrixResourceBase;
    @Mock
    protected RebootAnswer rebootAnswer;

    @Test
    public void testWrapperInstance() {
        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);
    }

    @Test
    public void testExecuteRebootRouterCommand() {
        final RebootRouterCommand rebootCommand = new RebootRouterCommand("", "");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        doReturn(rebootAnswer).when(citrixResourceBase).execute((RebootCommand)rebootCommand);
        doReturn(false).when(rebootAnswer).getResult();

        final Answer answer = wrapper.execute(rebootCommand, citrixResourceBase);

        verify(citrixResourceBase, times(1)).execute((RebootCommand)rebootCommand);

        assertFalse(rebootAnswer.getResult());
        assertEquals(answer, rebootAnswer);
    }
}