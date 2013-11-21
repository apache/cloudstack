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
package rdpclient;

import static rdpclient.MockServer.Packet.PacketType.CLIENT;
import static rdpclient.MockServer.Packet.PacketType.SERVER;
import static rdpclient.MockServer.Packet.PacketType.UPGRADE_TO_SSL;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import junit.framework.TestCase;
import rdpclient.MockServer.Packet;

public class MockServerTest extends TestCase {

    public void testIsMockServerCanRespond() throws Exception {

        final byte[] mockClientData = new byte[] {0x01, 0x02, 0x03};
        final byte[] mockServerData = new byte[] {0x03, 0x02, 0x01};

        MockServer server = new MockServer(new Packet[] {new Packet("Client hello") {
            {
                type = CLIENT;
                data = mockClientData;
            }
        }, new Packet("Server hello") {
            {
                type = SERVER;
                data = mockServerData;
            }
        }});

        server.start();

        // Connect to server and send and receive mock data

        Socket socket = SocketFactory.getDefault().createSocket();
        try {
            socket.connect(server.getAddress());

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            // Write mock data to server
            os.write(mockClientData);

            // Read data from server
            byte actualData[] = new byte[mockServerData.length];
            int actualDataLength = is.read(actualData);

            // Compare mock data with actual data
            assertEquals("Unexpected length of actual data read from server.", mockServerData.length, actualDataLength);

            for (int i = 0; i < actualDataLength; i++) {
                assertEquals("Unexpected byte #" + i + " in response", mockServerData[i], actualData[i]);
            }

            server.waitUntilShutdowned(1 * 1000 /* up to 1 second */);

            assertNull("Unexpected exception at mock server side.", server.getException());
            assertTrue("Server is not shutdowned at after conversation.", server.isShutdowned());

        } finally {
            socket.close();
        }
    }

    public void testIsMockServerCanUpgradeConnectionToSsl() throws Exception {

        final byte[] mockClientData1 = new byte[] {0x01, 0x02, 0x03};
        final byte[] mockServerData1 = new byte[] {0x03, 0x02, 0x01};

        final byte[] mockClientData2 = new byte[] {0x02, 0x04, 0x02, 0x03};
        final byte[] mockServerData2 = new byte[] {0x02, 0x02, 0x01, 0x04};

        MockServer server = new MockServer(new Packet[] {new Packet("Client hello") {
            {
                type = CLIENT;
                data = mockClientData1;
            }
        }, new Packet("Server hello") {
            {
                type = SERVER;
                data = mockServerData1;
            }
        }, new Packet("Upgrade connection to SSL") {
            {
                type = UPGRADE_TO_SSL;
            }
        }, new Packet("Client data over SSL") {
            {
                type = CLIENT;
                data = mockClientData2;
            }
        }, new Packet("Server data over SSL") {
            {
                type = SERVER;
                data = mockServerData2;
            }
        }});

        server.start();

        // Connect to server and send and receive mock data

        Socket socket = SocketFactory.getDefault().createSocket();
        try {
            InetSocketAddress address = server.getAddress();
            socket.connect(address);

            // Send hello data over plain connection
            {
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();

                // Write mock data to server
                os.write(mockClientData1);

                // Read data from server
                byte actualData[] = new byte[mockServerData1.length];
                int actualDataLength = is.read(actualData);

                // Compare mock data with actual data
                assertEquals("Unexpected length of actual data read from server.", mockServerData1.length, actualDataLength);

                for (int i = 0; i < actualDataLength; i++) {
                    assertEquals("Unexpected byte #" + i + " in response", mockServerData1[i], actualData[i]);
                }
            }

            // Upgrade connection to SSL and send mock data
            {
                //System.setProperty("javax.net.debug", "ssl");

                final SSLSocketFactory sslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
                SSLSocket sslSocket = (SSLSocket)sslSocketFactory.createSocket(socket, address.getHostName(), address.getPort(), true);
                sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
                sslSocket.startHandshake();

                InputStream is = sslSocket.getInputStream();
                OutputStream os = sslSocket.getOutputStream();

                // Write mock data to server
                os.write(mockClientData2);

                // Read data from server
                byte actualData[] = new byte[mockServerData2.length];
                int actualDataLength = is.read(actualData);

                // Compare mock data with actual data
                assertEquals("Unexpected length of actual data read from server.", mockServerData2.length, actualDataLength);

                for (int i = 0; i < actualDataLength; i++) {
                    assertEquals("Unexpected byte #" + i + " in response", mockServerData2[i], actualData[i]);
                }

            }

            server.waitUntilShutdowned(1 * 1000 /* up to 1 second */);

            assertNull("Unexpected exception at mock server side.", server.getException());
            assertTrue("Server is not shutdowned at after conversation.", server.isShutdowned());
        } finally {
            socket.close();
        }

    }
}
