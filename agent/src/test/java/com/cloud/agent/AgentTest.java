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
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import javax.naming.ConfigurationException;

import com.cloud.agent.api.StartupAnswer;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.utils.backoff.BackoffAlgorithm;
import com.cloud.utils.nio.NioClient;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.resource.ServerResource;
import com.cloud.utils.backoff.impl.ConstantTimeBackoff;
import com.cloud.utils.nio.Link;

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
    public void testGetLinkLogLinkWithTraceEnabledReturnsLinkLogWithHashCode() throws ReflectiveOperationException {
        Link link = new Link(new InetSocketAddress("192.168.1.100", 8250), null);
        // fix LOGGER access to inject mock
        Field field = link.getClass().getDeclaredField("LOGGER");
        field.setAccessible(true);
        field.set(null, logger);
        when(logger.isTraceEnabled()).thenReturn(true);

        String result = link.toString();
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
        ServerAttache attache = new ServerAttache(mockLink);
        when(mockLink.attachment()).thenReturn(attache);
        agent.closeAndTerminateLink(mockLink);
        verify(mockLink).attachment();
        verify(mockLink).close();
        verify(mockLink).terminated();
    }

    @Test
    public void testStopAndCleanupConnectionConnectionIsNullDoesNothing() {
        agent.connection = null;
        agent.stopAndCleanupConnection();
    }

    @Test
    public void testStopAndCleanupConnectionValidConnectionNoWaitStopsAndCleansUp() throws IOException {
        NioClient mockConnection = mock(NioClient.class);
        agent.connection = mockConnection;
        agent.stopAndCleanupConnection();
        verify(mockConnection).stop();
        verify(mockConnection).cleanUp();
    }

    @Test
    public void testStopAndCleanupConnectionCleanupThrowsIOExceptionLogsWarning() throws IOException {
        NioClient mockConnection = mock(NioClient.class);
        agent.connection = mockConnection;
        doThrow(new IOException("Cleanup failed")).when(mockConnection).cleanUp();
        agent.stopAndCleanupConnection();
        verify(mockConnection).stop();
        verify(logger).warn(eq("Fail to clean up old connection"), isA(IOException.class));
    }

    @Test
    public void testStopAndCleanupConnectionValidConnectionWaitForStopWaitsForStartupToStop() throws IOException {
        NioClient mockConnection = mock(NioClient.class);
        ConstantTimeBackoff mockBackoff = mock(ConstantTimeBackoff.class);
        mockBackoff.setTimeToWait(0);
        agent.connection = mockConnection;
        when(shell.getBackoffAlgorithm()).thenReturn(mockBackoff);
        when(mockConnection.isStartup()).thenReturn(true, true, false);
        agent.stopAndCleanupConnection();
        verify(mockConnection, times(3)).stop();
        verify(mockConnection).cleanUp();
        verify(mockBackoff, times(2)).waitBeforeRetry();
    }

    @Test
    public void testSelectReconnectionHostWithPreferredHost() {
        Link mockLink = mock(Link.class);
        // No need to stub mockLink or shell since preferred host short-circuits
        String result = agent.selectReconnectionHost("preferred.host.com", mockLink);
        assertEquals("preferred.host.com", result);
        verify(shell, times(0)).getNextHost();
    }

    @Test
    public void testSelectReconnectionHostWithNullPreferredHostUsesLinkAddress() {
        Link mockLink = mock(Link.class);
        InetSocketAddress socketAddress = new InetSocketAddress("192.168.1.100", 8080);
        when(mockLink.getSocketAddress()).thenReturn(socketAddress);
        // No need to stub shell.getNextHost() since link address is used
        String result = agent.selectReconnectionHost(null, mockLink);
        assertEquals("192.168.1.100", result);
        verify(shell, times(0)).getNextHost();
    }

    @Test
    public void testSelectReconnectionHostWithNullLinkUsesShellNextHost() {
        when(shell.getNextHost()).thenReturn("fallback.host.com");
        String result = agent.selectReconnectionHost(null, null);
        assertEquals("fallback.host.com", result);
        verify(shell, times(1)).getNextHost();
    }

    @Test
    public void testSelectReconnectionHostWithNullSocketAddressUsesShellNextHost() {
        Link mockLink = mock(Link.class);
        when(mockLink.getSocketAddress()).thenReturn(null);
        when(shell.getNextHost()).thenReturn("fallback.host.com");
        String result = agent.selectReconnectionHost(null, mockLink);
        assertEquals("fallback.host.com", result);
        verify(shell, times(1)).getNextHost();
    }

    private Agent setupAgentSpyForStartupAnswerTests(String currentHost) throws ConfigurationException {
        Agent spyAgent = Mockito.spy(agent);
        doNothing().when(spyAgent).reconnect(any(), any(), eq(false));

        NioClient mockConnection = mock(NioClient.class);
        when(mockConnection.getHost()).thenReturn(currentHost);
        spyAgent.connection = mockConnection;

        spyAgent.requestHandler = mock(ExecutorService.class);
        when(serverResource.isExitOnFailures()).thenReturn(false);
        when(shell.getBackoffAlgorithm()).thenReturn(mock(BackoffAlgorithm.class));
        return spyAgent;
    }

    @Test
    public void testProcessStartupAnswerRetryCurrentMsReconnectsToCurrentHost() throws Exception {
        String currentHost = "ms-host-0.example.com";
        Agent spyAgent = setupAgentSpyForStartupAnswerTests(currentHost);
        Link link = mock(Link.class);

        StartupAnswer answer = new StartupAnswer(new StartupRoutingCommand(), "duplicate");
        answer.setRetryCurrentMs(true);

        spyAgent.processStartupAnswer(answer, null, link);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(spyAgent.requestHandler).submit(captor.capture());
        captor.getValue().run();

        verify(spyAgent).reconnect(eq(link), eq(currentHost), eq(false));
    }

    @Test
    public void testProcessStartupAnswerRetryCurrentMsAppliesBackoffBeforeReconnect() throws Exception {
        Agent spyAgent = setupAgentSpyForStartupAnswerTests("ms-host-0.example.com");
        Link link = mock(Link.class);

        BackoffAlgorithm mockBackoff = mock(BackoffAlgorithm.class);
        when(shell.getBackoffAlgorithm()).thenReturn(mockBackoff);

        StartupAnswer answer = new StartupAnswer(new StartupRoutingCommand(), "duplicate");
        answer.setRetryCurrentMs(true);

        spyAgent.processStartupAnswer(answer, null, link);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(spyAgent.requestHandler).submit(captor.capture());
        captor.getValue().run();

        verify(mockBackoff).waitBeforeRetry();
    }

    @Test
    public void testProcessStartupAnswerNoRetryCurrentMsReconnectsWithNullAndNoBackoff() throws Exception {
        Agent spyAgent = setupAgentSpyForStartupAnswerTests("ms-host-0.example.com");
        Link link = mock(Link.class);

        BackoffAlgorithm mockBackoff = mock(BackoffAlgorithm.class);
        when(shell.getBackoffAlgorithm()).thenReturn(mockBackoff);

        StartupAnswer answer = new StartupAnswer(new StartupRoutingCommand(), "not a duplicate");
        // retryCurrentMs defaults to false

        spyAgent.processStartupAnswer(answer, null, link);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(spyAgent.requestHandler).submit(captor.capture());
        captor.getValue().run();

        verify(spyAgent).reconnect(eq(link), isNull(), eq(false));
        verify(mockBackoff, never()).waitBeforeRetry();
    }
}
