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

package com.cloud.cluster;

import com.cloud.cluster.dao.ManagementServerHostDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.naming.ConfigurationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterManagerImplTest {
    @Mock
    private ManagementServerHostDao mockMshostDao;

    @InjectMocks
    private ClusterManagerImpl clusterManager = new ClusterManagerImpl();

    private static final String TEST_CURRENT_IP = "192.168.1.100";
    private static final String TEST_CURRENT_HOSTNAME = "management-server-1";
    private static final String TEST_PEER_IP = "192.168.1.101";
    private static final String TEST_PEER_HOSTNAME = "management-server-2";
    private static final String LOCALHOST_IP = "127.0.0.1";

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(clusterManager, "_mshostDao", mockMshostDao);
        ReflectionTestUtils.setField(clusterManager, "_clusterNodeIP", TEST_CURRENT_IP);
    }

    @Test
    public void testGetSelfNodeIP() {
        String ip = "1.2.3.4";
        ReflectionTestUtils.setField(clusterManager, "_clusterNodeIP", ip);
        assertEquals(ip, clusterManager.getSelfNodeIP());
    }

    // ========== UNIT TESTS FOR checkNodeConflict METHOD ==========

    @Test(expected = ConfigurationException.class)
    public void testCheckNodeConflict_HostnameConflict_PeerActive() throws Exception {
        // Create peer with same hostname but different IP
        ManagementServerHostVO conflictingPeer = createMockPeer(1L, TEST_PEER_IP, TEST_CURRENT_HOSTNAME);

        // Mock ping to return true (peer is active)
        ClusterManagerImpl spyManager = spy(clusterManager);
        doReturn(true).when(spyManager).pingManagementNode(1L);

        // Test the method directly
        spyManager.checkNodeConflict(conflictingPeer, TEST_CURRENT_HOSTNAME);
    }

    @Test
    public void testCheckNodeConflict_HostnameConflict_PeerInactive() throws Exception {
        // Create peer with same hostname
        ManagementServerHostVO stalePeer = createMockPeer(2L, TEST_PEER_IP, TEST_CURRENT_HOSTNAME);

        // Mock ping to return false (peer is inactive)
        ClusterManagerImpl spyManager = spy(clusterManager);
        doReturn(false).when(spyManager).pingManagementNode(2L);

        // Should not throw exception (just log warning)
        spyManager.checkNodeConflict(stalePeer, TEST_CURRENT_HOSTNAME);

        // Verify ping was called
        verify(spyManager).pingManagementNode(2L);
    }

    @Test(expected = ConfigurationException.class)
    public void testCheckNodeConflict_HostnameConflict_CaseInsensitive() throws Exception {
        // Create peer with different case hostname
        ManagementServerHostVO conflictingPeer = createMockPeer(3L, TEST_PEER_IP, TEST_CURRENT_HOSTNAME.toUpperCase());

        ClusterManagerImpl spyManager = spy(clusterManager);
        doReturn(true).when(spyManager).pingManagementNode(3L);

        // Should detect conflict despite case difference
        spyManager.checkNodeConflict(conflictingPeer, TEST_CURRENT_HOSTNAME.toLowerCase());
    }

    @Test(expected = ConfigurationException.class)
    public void testCheckNodeConflict_IPConflict_PeerActive() throws Exception {
        // Create peer with same IP but different hostname
        ManagementServerHostVO conflictingPeer = createMockPeer(4L, TEST_CURRENT_IP, TEST_PEER_HOSTNAME);

        ClusterManagerImpl spyManager = spy(clusterManager);
        doReturn(true).when(spyManager).pingManagementNode(4L);

        // Should throw ConfigurationException
        spyManager.checkNodeConflict(conflictingPeer, TEST_CURRENT_HOSTNAME);
    }

    @Test
    public void testCheckNodeConflict_IPConflict_PeerInactive() throws Exception {
        // Create peer with same IP but different hostname
        ManagementServerHostVO stalePeer = createMockPeer(5L, TEST_CURRENT_IP, TEST_PEER_HOSTNAME);

        ClusterManagerImpl spyManager = spy(clusterManager);
        doReturn(false).when(spyManager).pingManagementNode(5L);

        // Should not throw exception
        spyManager.checkNodeConflict(stalePeer, TEST_CURRENT_HOSTNAME);

        verify(spyManager).pingManagementNode(5L);
    }

    @Test(expected = ConfigurationException.class)
    public void testCheckNodeConflict_LocalhostIPConflict_PeerActive() throws Exception {
        // Set current node to use localhost IP
        ReflectionTestUtils.setField(clusterManager, "_clusterNodeIP", LOCALHOST_IP);

        // Create peer with same localhost IP
        ManagementServerHostVO conflictingPeer = createMockPeer(6L, LOCALHOST_IP, TEST_PEER_HOSTNAME);

        ClusterManagerImpl spyManager = spy(clusterManager);
        doReturn(true).when(spyManager).pingManagementNode(6L);

        try {
            spyManager.checkNodeConflict(conflictingPeer, TEST_CURRENT_HOSTNAME);
            fail("Expected ConfigurationException");
        } catch (ConfigurationException e) {
            // Verify error message mentions "localhost IP"
            assertTrue("Error should mention localhost IP", e.getMessage().contains("localhost IP"));
            throw e;
        }
    }

    @Test
    public void testCheckNodeConflict_NoConflict() throws Exception {
        // Create peer with different hostname and IP
        ManagementServerHostVO validPeer = createMockPeer(7L, TEST_PEER_IP, TEST_PEER_HOSTNAME);

        ClusterManagerImpl spyManager = spy(clusterManager);

        // Should complete without exception or ping
        spyManager.checkNodeConflict(validPeer, TEST_CURRENT_HOSTNAME);

        // Verify no ping was attempted (no conflict detected)
        verify(spyManager, never()).pingManagementNode(anyLong());
    }

    @Test
    public void testCheckNodeConflict_CurrentHostnameNull() throws Exception {
        // Peer has valid hostname but current is null
        ManagementServerHostVO peer = createMockPeer(8L, TEST_PEER_IP, TEST_PEER_HOSTNAME);

        // Should complete without exception (hostname conflict check skipped)
        clusterManager.checkNodeConflict(peer, null);
    }

    @Test
    public void testCheckNodeConflict_PeerHostnameNull() throws Exception {
        // Peer has null hostname
        ManagementServerHostVO peer = createMockPeer(9L, TEST_PEER_IP, null);

        // Should complete without exception (hostname conflict check skipped)
        clusterManager.checkNodeConflict(peer, TEST_CURRENT_HOSTNAME);
    }

    @Test
    public void testCheckNodeConflict_PeerServiceIPNull() throws Exception {
        // Peer has null service IP
        ManagementServerHostVO peer = createMockPeer(10L, null, TEST_PEER_HOSTNAME);

        // Should complete without exception (IP gets converted to empty string)
        clusterManager.checkNodeConflict(peer, TEST_CURRENT_HOSTNAME);
    }

    @Test
    public void testCheckNodeConflict_ServiceIPWithWhitespace() throws Exception {
        // Peer has IP with whitespace that matches current IP after trimming
        ManagementServerHostVO peer = createMockPeer(11L, "  " + TEST_CURRENT_IP + "  ", TEST_PEER_HOSTNAME);

        ClusterManagerImpl spyManager = spy(clusterManager);
        doReturn(false).when(spyManager).pingManagementNode(11L);

        // Should detect IP conflict after trimming
        spyManager.checkNodeConflict(peer, TEST_CURRENT_HOSTNAME);
        verify(spyManager).pingManagementNode(11L);
    }

    @Test(expected = ConfigurationException.class)
    public void testCheckNodeConflict_HostnameConflictTakesPrecedenceOverIP() throws Exception {
        // Create peer with BOTH hostname and IP conflicts
        ManagementServerHostVO conflictingPeer = createMockPeer(12L, TEST_CURRENT_IP, TEST_CURRENT_HOSTNAME);

        ClusterManagerImpl spyManager = spy(clusterManager);
        doReturn(true).when(spyManager).pingManagementNode(12L);

        try {
            spyManager.checkNodeConflict(conflictingPeer, TEST_CURRENT_HOSTNAME);
            fail("Expected ConfigurationException");
        } catch (ConfigurationException e) {
            // Verify error message mentions hostname (not IP) since hostname takes precedence
            assertTrue("Error should mention hostname conflict", e.getMessage().contains("hostname"));
            assertFalse("Error should not mention IP conflict", e.getMessage().contains(" IP"));
            throw e;
        }
    }

    // ========== INTEGRATION TESTS FOR checkConflicts METHOD ==========

    @Test
    public void testCheckConflicts_EmptyPeerList() throws Exception {
        when(mockMshostDao.getActiveList(any(Date.class))).thenReturn(Collections.emptyList());

        // Should complete without exception
        clusterManager.checkConflicts();

        verify(mockMshostDao).getActiveList(any(Date.class));
    }

    @Test
    public void testCheckConflicts_DatabaseException() throws Exception {
        // DAO throws exception
        when(mockMshostDao.getActiveList(any(Date.class))).thenThrow(new RuntimeException("Database error"));

        try {
            clusterManager.checkConflicts();
            fail("Expected exception to be propagated");
        } catch (RuntimeException e) {
            assertEquals("Database error", e.getMessage());
        }
    }

    @Test
    public void testCheckConflicts_ValidPeersProcessed() throws Exception {
        // Create multiple valid peers
        ManagementServerHostVO peer1 = createMockPeer(13L, "192.168.1.13", "peer-1");
        ManagementServerHostVO peer2 = createMockPeer(14L, "192.168.1.14", "peer-2");
        ManagementServerHostVO peer3 = createMockPeer(15L, "192.168.1.15", "peer-3");

        when(mockMshostDao.getActiveList(any(Date.class))).thenReturn(Arrays.asList(peer1, peer2, peer3));

        // Should process all peers without exception
        clusterManager.checkConflicts();

        verify(mockMshostDao).getActiveList(any(Date.class));
    }

    // ========== UNIT TESTS FOR handleConflict METHOD ==========

    @Test(expected = ConfigurationException.class)
    public void testHandleConflict_ActivePeer() throws Exception {
        ManagementServerHostVO peer = createMockPeer(16L, TEST_PEER_IP, TEST_PEER_HOSTNAME);

        ClusterManagerImpl spyManager = spy(clusterManager);
        doReturn(true).when(spyManager).pingManagementNode(16L);

        // Use reflection to call private method
        try {
            ReflectionTestUtils.invokeMethod(spyManager, "handleConflict", peer, "hostname", "test-value", TEST_CURRENT_HOSTNAME);
        } catch (Exception e) {
            if (e.getCause() instanceof ConfigurationException) {
                throw new ConfigurationException("Expected ConfigurationException");
            }
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testHandleConflict_InactivePeer() throws Exception {
        ManagementServerHostVO peer = createMockPeer(17L, TEST_PEER_IP, TEST_PEER_HOSTNAME);

        ClusterManagerImpl spyManager = spy(clusterManager);
        doReturn(false).when(spyManager).pingManagementNode(17L);

        // Should not throw exception (just log info)
        ReflectionTestUtils.invokeMethod(spyManager, "handleConflict", peer, "IP", "test-value", TEST_CURRENT_HOSTNAME);

        verify(spyManager).pingManagementNode(17L);
    }

    @Test
    public void testHandleConflict_LocalhostIPMessage() throws Exception {
        ManagementServerHostVO peer = createMockPeer(19L, LOCALHOST_IP, TEST_PEER_HOSTNAME);

        ClusterManagerImpl spyManager = spy(clusterManager);
        doReturn(false).when(spyManager).pingManagementNode(19L);

        // Test localhost IP specific message
        ReflectionTestUtils.invokeMethod(spyManager, "handleConflict", peer, "localhost IP", LOCALHOST_IP, TEST_CURRENT_HOSTNAME);

        verify(spyManager).pingManagementNode(19L);
    }

    @Test
    public void testHandleConflict_RegularIPMessage() throws Exception {
        ManagementServerHostVO peer = createMockPeer(20L, TEST_PEER_IP, TEST_PEER_HOSTNAME);

        ClusterManagerImpl spyManager = spy(clusterManager);
        doReturn(false).when(spyManager).pingManagementNode(20L);

        // Test regular IP message
        ReflectionTestUtils.invokeMethod(spyManager, "handleConflict", peer, "IP", TEST_PEER_IP, TEST_CURRENT_HOSTNAME);

        verify(spyManager).pingManagementNode(20L);
    }

    // ========== EDGE CASES AND BOUNDARY CONDITIONS ==========

    @Test
    public void testCheckNodeConflict_EmptyStringIP() throws Exception {
        // Current IP is empty string
        ReflectionTestUtils.setField(clusterManager, "_clusterNodeIP", "");

        ManagementServerHostVO peer = createMockPeer(21L, "", TEST_PEER_HOSTNAME);

        ClusterManagerImpl spyManager = spy(clusterManager);
        doReturn(false).when(spyManager).pingManagementNode(21L);

        // Should detect conflict with empty strings
        spyManager.checkNodeConflict(peer, TEST_CURRENT_HOSTNAME);
        verify(spyManager).pingManagementNode(21L);
    }

    @Test
    public void testCheckNodeConflict_EmptyStringHostname() throws Exception {
        ManagementServerHostVO peer = createMockPeer(22L, TEST_PEER_IP, "");

        // Should skip hostname conflict check with empty string
        clusterManager.checkNodeConflict(peer, "");
    }

    // ========== HELPER METHODS ==========

    private ManagementServerHostVO createMockPeer(long msid, String serviceIP, String name) {
        ManagementServerHostVO peer = mock(ManagementServerHostVO.class);
        when(peer.getMsid()).thenReturn(msid);
        when(peer.getServiceIP()).thenReturn(serviceIP);
        when(peer.getName()).thenReturn(name);
        return peer;
    }
}
