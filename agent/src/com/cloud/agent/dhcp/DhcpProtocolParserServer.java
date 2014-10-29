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
package com.cloud.agent.dhcp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cloud.utils.concurrency.NamedThreadFactory;

public class DhcpProtocolParserServer extends Thread {
    private static final Logger s_logger = Logger.getLogger(DhcpProtocolParserServer.class);;
    protected ExecutorService _executor;
    private int dhcpServerPort = 67;
    private int bufferSize = 300;
    protected boolean _running = false;

    public DhcpProtocolParserServer(int workers) {
        _executor = new ThreadPoolExecutor(workers, 10 * workers, 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("DhcpListener"));
        _running = true;
    }

    @Override
    public void run() {
        while (_running) {
            try {
                DatagramSocket dhcpSocket = new DatagramSocket(dhcpServerPort, InetAddress.getByAddress(new byte[] {0, 0, 0, 0}));
                dhcpSocket.setBroadcast(true);

                while (true) {
                    byte[] buf = new byte[bufferSize];
                    DatagramPacket dgp = new DatagramPacket(buf, buf.length);
                    dhcpSocket.receive(dgp);
                    // _executor.execute(new DhcpPacketParser(buf));
                }
            } catch (IOException e) {
                s_logger.debug(e.getMessage());
            }
        }
    }
}
