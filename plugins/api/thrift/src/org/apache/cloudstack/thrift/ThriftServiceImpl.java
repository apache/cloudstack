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
package org.apache.cloudstack.thrift;

import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ComponentLifecycleBase;
import org.apache.cloudstack.thrift.api.CloudStack;
import org.apache.log4j.Logger;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.ArrayList;
import java.util.List;

@Component
@Local(value = ThriftService.class)
public class ThriftServiceImpl extends ComponentLifecycleBase implements ThriftService {
    private static final Logger s_logger = Logger.getLogger(ThriftService.class);

    private static int thriftPort = 9600;
    private static int totalSelectorThreads = 4;
    private static int totalWorkerThreads = 16;
    private TServer server;
    private Thread thread;

    protected ThriftServiceImpl() {
        super();
    }

    @Override
    public boolean start() {
        s_logger.info("Starting Thrift API Server");
        thread = new Thread() {
            public void run() {
                try {
                    TNonblockingServerTransport trans = new TNonblockingServerSocket(thriftPort);
                    TThreadedSelectorServer.Args args = new TThreadedSelectorServer.Args(trans);
                    args.transportFactory(new TFramedTransport.Factory());
                    args.protocolFactory(new TBinaryProtocol.Factory());

                    CloudStackHandler handler = new CloudStackHandler();
                    handler = ComponentContext.inject(handler);
                    TProcessor processor = new CloudStack.Processor<>(handler);

                    args.processor(processor);
                    args.selectorThreads(totalSelectorThreads);
                    args.workerThreads(totalWorkerThreads);
                    server = new TThreadedSelectorServer(args);
                    server.serve();
                    s_logger.info("Thrift API started");
                } catch(TTransportException e) {
                    s_logger.error("Thrift API Server could not be started: " + e.getMessage());
                }
            }
        };
        thread.start();
        return true;
    }

    @Override
    public boolean stop() {
        server.stop();
        thread.stop();
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        return new ArrayList<Class<?>>();
    }
}
