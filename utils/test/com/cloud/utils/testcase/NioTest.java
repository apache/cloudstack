package com.cloud.utils.testcase;

import java.nio.channels.ClosedChannelException;
import java.util.Random;

import org.apache.log4j.Logger;

import com.cloud.utils.nio.HandlerFactory;
import com.cloud.utils.nio.Link;
import com.cloud.utils.nio.NioClient;
import com.cloud.utils.nio.NioServer;
import com.cloud.utils.nio.Task;
import com.cloud.utils.nio.Task.Type;

import org.junit.Assert;
import junit.framework.TestCase;

/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

public class NioTest extends TestCase {
    
    private static final Logger s_logger = Logger.getLogger(NioTest.class);
    
    private NioServer _server;
    private NioClient _client;
    
    private Link _clientLink;
    
    private int _testCount;
    private int _completedCount;
    
    private boolean isTestsDone() {
        boolean result;
        synchronized(this) {
            result = (_testCount == _completedCount);
        }
        return result;
    }
    
    private void getOneMoreTest() {
        synchronized(this) {
            _testCount ++;
        }
    }
    private void oneMoreTestDone() {
        synchronized(this) {
            _completedCount ++;
        }
    }
    
    public void setUp() {
        s_logger.info("Test");
        
        _testCount = 0;
        _completedCount = 0;
        
        _server = new NioServer("NioTestServer", 7777, 5, new NioTestServer());
        _server.start();
        
        _client = new NioClient("NioTestServer", "127.0.0.1", 7777, 5, new NioTestClient());
        _client.start();
        
        while (_clientLink == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public void tearDown() {
        while (!isTestsDone()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        stopClient();
        stopServer();
    }
    
    protected void stopClient(){
        _client.stop();
        s_logger.info("Client stopped.");
    }
    
    protected void stopServer(){
        _server.stop();
        s_logger.info("Server stopped.");
    }
    
    protected void setClientLink(Link link)
    {
        _clientLink = link;
    }
    
    Random randomGenerator = new Random();
    
    byte[] _testBytes;
    
    public void testConnection() {
        _testBytes = new byte[1000000];
        randomGenerator.nextBytes(_testBytes);
        try {
            getOneMoreTest();
            _clientLink.send(_testBytes);
            s_logger.info("Client: Data sent");
            getOneMoreTest();
            _clientLink.send(_testBytes);
            s_logger.info("Client: Data sent");
        } catch (ClosedChannelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    protected void doServerProcess(byte[] data) {
        oneMoreTestDone();
        Assert.assertArrayEquals(_testBytes, data);
        s_logger.info("Verify done.");
    }
    
    public class NioTestClient implements HandlerFactory {

        @Override
        public Task create(Type type, Link link, byte[] data) {
            return new NioTestClientHandler(type, link, data);
        }
        
        public class NioTestClientHandler extends Task {

            public NioTestClientHandler(Type type, Link link, byte[] data) {
                super(type, link, data);
            }
            
            @Override
            public void doTask(final Task task) {
                if (task.getType() == Task.Type.CONNECT) {
                    s_logger.info("Client: Received CONNECT task");
                    setClientLink(task.getLink());
                } else if (task.getType() == Task.Type.DATA) {
                    s_logger.info("Client: Received DATA task");
                } else if (task.getType() == Task.Type.DISCONNECT) {
                    s_logger.info("Client: Received DISCONNECT task");
                    stopClient();
                } else if (task.getType() == Task.Type.OTHER) {
                    s_logger.info("Client: Received OTHER task");
                }
            }
            
        }
    }

    public class NioTestServer implements HandlerFactory {

        @Override
        public Task create(Type type, Link link, byte[] data) {
            return new NioTestServerHandler(type, link, data);
        }
        
        public class NioTestServerHandler extends Task {

            public NioTestServerHandler(Type type, Link link, byte[] data) {
                super(type, link, data);
            }
            
            @Override
            public void doTask(final Task task) {
                if (task.getType() == Task.Type.CONNECT) {
                    s_logger.info("Server: Received CONNECT task");
                } else if (task.getType() == Task.Type.DATA) {
                    s_logger.info("Server: Received DATA task");
                    doServerProcess(task.getData());
                } else if (task.getType() == Task.Type.DISCONNECT) {
                    s_logger.info("Server: Received DISCONNECT task");
                    stopServer();
                } else if (task.getType() == Task.Type.OTHER) {
                    s_logger.info("Server: Received OTHER task");
                }
            }
            
        }
    }
}
