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
package com.cloud.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.naming.ConfigurationException;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.resource.ServerResource;
import com.cloud.utils.backoff.impl.ConstantTimeBackoff;
import com.cloud.utils.nio.Link;
import com.cloud.utils.nio.NioConnection;

@RunWith(MockitoJUnitRunner.class)
public class AgentTest {
    Agent agent;
    private AgentShell shell;
    private ServerResource serverResource;
    private Logger logger;

    @Before
    public void setUp() throws ConfigurationException {
        shell = mock(AgentShell.class);
        serverResource = mock(ServerResource.class);
        doReturn(true).when(serverResource).configure(any(), any());
        doReturn(1).when(shell).getWorkers();
        doReturn(1).when(shell).getPingRetries();
        agent = new Agent(shell, 1, serverResource);
        logger = mock(Logger.class);
        ReflectionTestUtils.setField(agent, "logger", logger);
    }

    @Test
    public void testGetLinkLogNullLinkReturnsEmptyString() {
        Link link = null;
        String result = agent.getLinkLog(link);
        assertEquals("", result);
    }

    @Test
    public void testGetLinkLogLinkWithTraceEnabledReturnsLinkLogWithHashCode() {
        Link link = mock(Link.class);
        InetSocketAddress socketAddress = new InetSocketAddress("192.168.1.100", 1111);
        when(link.getSocketAddress()).thenReturn(socketAddress);
        when(logger.isTraceEnabled()).thenReturn(true);

        String result = agent.getLinkLog(link);
        System.out.println(result);
        assertTrue(result.startsWith(System.identityHashCode(link) + "-"));
        assertTrue(result.contains("192.168.1.100"));
    }

    @Test
    public void testGetAgentNameWhenServerResourceIsNull() {
        ReflectionTestUtils.setField(agent, "serverResource", null);
        assertEquals("Agent", agent.getAgentName());
    }

    @Test
    public void testGetAgentNameWhenAppendAgentNameIsTrue() {
        when(serverResource.isAppendAgentNameToLogs()).thenReturn(true);
        when(serverResource.getName()).thenReturn("TestAgent");

        String agentName = agent.getAgentName();
        assertEquals("TestAgent", agentName);
    }

    @Test
    public void testGetAgentNameWhenAppendAgentNameIsFalse() {
        when(serverResource.isAppendAgentNameToLogs()).thenReturn(false);

        String agentName = agent.getAgentName();
        assertEquals("Agent", agentName);
    }

    @Test
    public void testAgentInitialization() {
        Runtime.getRuntime().removeShutdownHook(agent.shutdownThread);
        when(shell.getPingRetries()).thenReturn(3);
        when(shell.getWorkers()).thenReturn(5);
        agent.setupShutdownHookAndInitExecutors();
        assertNotNull(agent.selfTaskExecutor);
        assertNotNull(agent.outRequestHandler);
        assertNotNull(agent.requestHandler);
    }

    @Test
    public void testAgentShutdownHookAdded() {
        Runtime.getRuntime().removeShutdownHook(agent.shutdownThread);
        agent.setupShutdownHookAndInitExecutors();
        verify(logger).trace("Adding shutdown hook");
    }

    @Test
    public void testGetResourceGuidValidGuidAndResourceName() {
        when(shell.getGuid()).thenReturn("12345");
        String result = agent.getResourceGuid();
        assertTrue(result.startsWith("12345-" + ServerResource.class.getSimpleName()));
    }

    @Test
    public void testGetZoneReturnsValidZone() {
        when(shell.getZone()).thenReturn("ZoneA");
        String result = agent.getZone();
        assertEquals("ZoneA", result);
    }

    @Test
    public void testGetPodReturnsValidPod() {
        when(shell.getPod()).thenReturn("PodA");
        String result = agent.getPod();
        assertEquals("PodA", result);
    }

    @Test
    public void testSetLinkAssignsLink() {
        Link mockLink = mock(Link.class);
        agent.setLink(mockLink);
        assertEquals(mockLink, agent.link);
    }

    @Test
    public void testGetResourceReturnsServerResource() {
        ServerResource mockResource = mock(ServerResource.class);
        ReflectionTestUtils.setField(agent, "serverResource", mockResource);
        ServerResource result = agent.getResource();
        assertSame(mockResource, result);
    }

    @Test
    public void testGetResourceName() {
        String result = agent.getResourceName();
        assertTrue(result.startsWith(ServerResource.class.getSimpleName()));
    }

    @Test
    public void testUpdateLastPingResponseTimeUpdatesCurrentTime() {
        long beforeUpdate = System.currentTimeMillis();
        agent.updateLastPingResponseTime();
        long updatedTime = agent.lastPingResponseTime.get();
        assertTrue(updatedTime >= beforeUpdate);
        assertTrue(updatedTime <= System.currentTimeMillis());
    }

    @Test
    public void testGetNextSequenceIncrementsSequence() {
        long initialSequence = agent.getNextSequence();
        long nextSequence = agent.getNextSequence();
        assertEquals(initialSequence + 1, nextSequence);
        long thirdSequence = agent.getNextSequence();
        assertEquals(nextSequence + 1, thirdSequence);
    }

    @Test
    public void testRegisterControlListenerAddsListener() {
        IAgentControlListener listener = mock(IAgentControlListener.class);
        agent.registerControlListener(listener);
        assertTrue(agent.controlListeners.contains(listener));
    }

    @Test
    public void testUnregisterControlListenerRemovesListener() {
        IAgentControlListener listener = mock(IAgentControlListener.class);
        agent.registerControlListener(listener);
        assertTrue(agent.controlListeners.contains(listener));
        agent.unregisterControlListener(listener);
        assertFalse(agent.controlListeners.contains(listener));
    }

    @Test
    public void testCloseAndTerminateLinkLinkIsNullDoesNothing() {
        agent.closeAndTerminateLink(null);
    }

    @Test
    public void testCloseAndTerminateLinkValidLinkCallsCloseAndTerminate() {
        Link mockLink = mock(Link.class);
        agent.closeAndTerminateLink(mockLink);
        verify(mockLink).close();
        verify(mockLink).terminated();
    }

    @Test
    public void testStopAndCleanupConnectionConnectionIsNullDoesNothing() {
        agent.connection = null;
        agent.stopAndCleanupConnection(false);
    }

    @Test
    public void testStopAndCleanupConnectionValidConnectionNoWaitStopsAndCleansUp() throws IOException {
        NioConnection mockConnection = mock(NioConnection.class);
        agent.connection = mockConnection;
        agent.stopAndCleanupConnection(false);
        verify(mockConnection).stop();
        verify(mockConnection).cleanUp();
    }

    @Test
    public void testStopAndCleanupConnectionCleanupThrowsIOExceptionLogsWarning() throws IOException {
        NioConnection mockConnection = mock(NioConnection.class);
        agent.connection = mockConnection;
        doThrow(new IOException("Cleanup failed")).when(mockConnection).cleanUp();
        agent.stopAndCleanupConnection(false);
        verify(mockConnection).stop();
        verify(logger).warn(eq("Fail to clean up old connection. {}"), any(IOException.class));
    }

    @Test
    public void testStopAndCleanupConnectionValidConnectionWaitForStopWaitsForStartupToStop() throws IOException {
        NioConnection mockConnection = mock(NioConnection.class);
        ConstantTimeBackoff mockBackoff = mock(ConstantTimeBackoff.class);
        mockBackoff.setTimeToWait(0);
        agent.connection = mockConnection;
        when(shell.getBackoffAlgorithm()).thenReturn(mockBackoff);
        when(mockConnection.isStartup()).thenReturn(true, true, false);
        agent.stopAndCleanupConnection(true);
        verify(mockConnection).stop();
        verify(mockConnection).cleanUp();
        verify(mockBackoff, times(3)).waitBeforeRetry();
    }
}
