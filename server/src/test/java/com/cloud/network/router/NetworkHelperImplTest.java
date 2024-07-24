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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class NetworkHelperImplTest {

    private static final long HOST_ID = 10L;

    @Mock
    protected AgentManager agentManager;

    @InjectMocks
    protected NetworkHelperImpl nwHelper = new NetworkHelperImpl();

    @Test(expected=ResourceUnavailableException.class)
    public void testSendCommandsToRouterWrongRouterVersion()
            throws AgentUnavailableException, OperationTimedoutException, ResourceUnavailableException {
        // Prepare
        NetworkHelperImpl nwHelperUT = spy(this.nwHelper);
        VirtualRouter vr = mock(VirtualRouter.class);
        doReturn(false).when(nwHelperUT).checkRouterVersion(vr);

        // Execute
        nwHelperUT.sendCommandsToRouter(vr, null);

        // Assert
        verify(this.agentManager, times(0)).send((Long) Matchers.anyObject(), (Command) Matchers.anyObject());
    }

    @Test
    public void testSendCommandsToRouter()
            throws AgentUnavailableException, OperationTimedoutException, ResourceUnavailableException {
        // Prepare
        NetworkHelperImpl nwHelperUT = spy(this.nwHelper);
        VirtualRouter vr = mock(VirtualRouter.class);
        when(vr.getHostId()).thenReturn(HOST_ID);
        doReturn(true).when(nwHelperUT).checkRouterVersion(vr);

        Commands commands = mock(Commands.class);
        when(commands.size()).thenReturn(3);
        Answer answer1 = mock(Answer.class);
        Answer answer2 = mock(Answer.class);
        Answer answer3 = mock(Answer.class);
        // In the second iteration it should match and return, without invoking the third
        Answer[] answers = {answer1, answer2, answer3};
        when(answer1.getResult()).thenReturn(true);
        when(answer2.getResult()).thenReturn(false);
        lenient().when(answer3.getResult()).thenReturn(false);
        when(this.agentManager.send(HOST_ID, commands)).thenReturn(answers);

        // Execute
        final boolean result = nwHelperUT.sendCommandsToRouter(vr, commands);

        // Assert
        verify(this.agentManager, times(1)).send(HOST_ID, commands);
        verify(answer1, times(1)).getResult();
        verify(answer2, times(1)).getResult();
        verify(answer3, times(0)).getResult();
        assertFalse(result);
    }

    /**
     * The only way result can be true is if each and every command receive a true result
     *
     * @throws AgentUnavailableException
     * @throws OperationTimedoutException
     */
    @Test
    public void testSendCommandsToRouterWithTrueResult()
            throws AgentUnavailableException, OperationTimedoutException, ResourceUnavailableException {
        // Prepare
        NetworkHelperImpl nwHelperUT = spy(this.nwHelper);
        VirtualRouter vr = mock(VirtualRouter.class);
        when(vr.getHostId()).thenReturn(HOST_ID);
        doReturn(true).when(nwHelperUT).checkRouterVersion(vr);

        Commands commands = mock(Commands.class);
        when(commands.size()).thenReturn(3);
        Answer answer1 = mock(Answer.class);
        Answer answer2 = mock(Answer.class);
        Answer answer3 = mock(Answer.class);
        // In the second iteration it should match and return, without invoking the third
        Answer[] answers = {answer1, answer2, answer3};
        when(answer1.getResult()).thenReturn(true);
        when(answer2.getResult()).thenReturn(true);
        when(answer3.getResult()).thenReturn(true);
        when(this.agentManager.send(HOST_ID, commands)).thenReturn(answers);

        // Execute
        final boolean result = nwHelperUT.sendCommandsToRouter(vr, commands);

        // Assert
        verify(this.agentManager, times(1)).send(HOST_ID, commands);
        verify(answer1, times(1)).getResult();
        verify(answer2, times(1)).getResult();
        verify(answer3, times(1)).getResult();
        assertTrue(result);
    }

    /**
     * If the number of answers is different to the number of commands the result is false
     *
     * @throws AgentUnavailableException
     * @throws OperationTimedoutException
     */
    @Test
    public void testSendCommandsToRouterWithNoAnswers()
            throws AgentUnavailableException, OperationTimedoutException, ResourceUnavailableException {
        // Prepare
        NetworkHelperImpl nwHelperUT = spy(this.nwHelper);
        VirtualRouter vr = mock(VirtualRouter.class);
        when(vr.getHostId()).thenReturn(HOST_ID);
        doReturn(true).when(nwHelperUT).checkRouterVersion(vr);

        Commands commands = mock(Commands.class);
        when(commands.size()).thenReturn(3);
        Answer answer1 = mock(Answer.class);
        Answer answer2 = mock(Answer.class);
        // In the second iteration it should match and return, without invoking the third
        Answer[] answers = {answer1, answer2};
        when(this.agentManager.send(HOST_ID, commands)).thenReturn(answers);

        // Execute
        final boolean result = nwHelperUT.sendCommandsToRouter(vr, commands);

        // Assert
        verify(this.agentManager, times(1)).send(HOST_ID, commands);
        verify(answer1, times(0)).getResult();
        assertFalse(result);
    }

}
