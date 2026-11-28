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
package com.cloud.consoleproxy;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.consoleproxy.vnc.NoVncClient;
import com.cloud.consoleproxy.vnc.RfbConstants;

public class ConsoleProxyNoVncClientTest {
    @Test
    public void rewriteServerNameInServerInitTest() {
        String serverName = "server123, backend:TLS";
        byte[] serverInitTestBytes = new byte[]{ 4, 0, 3, 0, 32, 24, 0, 1, 0, -1, 0, -1, 0, -1, 16, 8, 0, 0, 0, 0, 0, 0, 0, 15, 81, 69, 77, 85, 32, 40, 105, 45, 50, 45, 56, 45, 86, 77, 41};
        byte[] newServerInit = ConsoleProxyNoVncClient.rewriteServerNameInServerInit(serverInitTestBytes, serverName);

        byte[] expectedBytes = new byte[]{4, 0, 3, 0, 32, 24, 0, 1, 0, -1, 0, -1, 0, -1, 16, 8, 0, 0, 0, 0, 0, 0, 0, 22, 115, 101, 114, 118, 101, 114, 49, 50, 51, 44, 32, 98, 97, 99, 107, 101, 110, 100, 58, 84, 76, 83};
        Assert.assertArrayEquals(newServerInit, expectedBytes);
    }

    @Test
    public void authenticateVNCServerThroughNioSocketExpectsNoVncReplyBeforeSendingTheMessage() throws Exception {
        Session session = Mockito.mock(Session.class);
        RemoteEndpoint remote = Mockito.mock(RemoteEndpoint.class);
        Mockito.when(session.getRemote()).thenReturn(remote);
        NoVncClient client = Mockito.mock(NoVncClient.class);
        Mockito.when(client.handshakeProtocolVersion()).thenReturn(ByteBuffer.wrap("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII)));
        Mockito.when(client.handshakeSecurityType()).thenReturn(RfbConstants.NO_AUTH);
        Mockito.when(client.readServerInit()).thenReturn(new byte[24]);
        ConsoleProxyClientParam param = new ConsoleProxyClientParam();
        param.setClientDisplayName("vm");

        ConsoleProxyNoVncClient viewer = new ConsoleProxyNoVncClient(session);
        ReflectionTestUtils.setField(viewer, "client", client);
        ReflectionTestUtils.setField(viewer, "clientParam", param);
        viewer.authenticateVNCServerThroughNioSocket();

        // A reply flagged after the message is sent can arrive first and be lost
        InOrder inOrder = Mockito.inOrder(client, remote);
        inOrder.verify(client).processSecurityResultMsg();
        inOrder.verify(client).setWaitForNoVnc(true);
        inOrder.verify(remote).sendBytes(Mockito.argThat(msg -> msg.remaining() == 4));
        inOrder.verify(client).waitForNoVNCReply();
        inOrder.verify(client).readServerInit();
        inOrder.verify(client).setWaitForNoVnc(true);
        inOrder.verify(remote).sendBytes(Mockito.argThat(msg -> msg.remaining() > 24));
        inOrder.verify(client).waitForNoVNCReply();
    }

    @Test(timeout = 20000)
    public void handshakeFailureClosesTheSession() throws Exception {
        try (ServerSocket vncServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Session session = Mockito.mock(Session.class);
            ConsoleProxyNoVncClient viewer = new ConsoleProxyNoVncClient(session);
            viewer.initClient(createClientParam(vncServer));

            // The VNC server goes away before sending its protocol version
            vncServer.accept().close();

            Mockito.verify(session, Mockito.timeout(10000)).close();
            Assert.assertFalse(viewer.isHostConnected());
        }
    }

    @Test(timeout = 20000)
    public void closingTheViewerWhileWaitingForNoVncStopsTheWorker() throws Exception {
        try (ServerSocket vncServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Session session = Mockito.mock(Session.class);
            RemoteEndpoint remote = Mockito.mock(RemoteEndpoint.class);
            Mockito.when(session.getRemote()).thenReturn(remote);
            CountDownLatch versionSentToNoVnc = new CountDownLatch(1);
            Mockito.doAnswer(invocation -> {
                versionSentToNoVnc.countDown();
                return null;
            }).when(remote).sendBytes(Mockito.any(ByteBuffer.class));

            ConsoleProxyNoVncClient viewer = new ConsoleProxyNoVncClient(session);
            viewer.initClient(createClientParam(vncServer));

            try (Socket vncConnection = vncServer.accept()) {
                OutputStream out = vncConnection.getOutputStream();
                out.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();
                Assert.assertTrue(versionSentToNoVnc.await(10, TimeUnit.SECONDS));

                // noVNC never replies, then its websocket is closed (ConsoleProxyNoVNCHandler.onClose)
                viewer.closeClient();

                Mockito.verify(session, Mockito.timeout(10000)).close();
                vncConnection.setSoTimeout(10000);
                Assert.assertEquals(-1, vncConnection.getInputStream().read());
            }
        }
    }

    private static ConsoleProxyClientParam createClientParam(ServerSocket vncServer) {
        ConsoleProxyClientParam param = new ConsoleProxyClientParam();
        param.setClientHostAddress(vncServer.getInetAddress().getHostAddress());
        param.setClientHostPort(vncServer.getLocalPort());
        param.setClientIp("127.0.0.1");
        return param;
    }
}
