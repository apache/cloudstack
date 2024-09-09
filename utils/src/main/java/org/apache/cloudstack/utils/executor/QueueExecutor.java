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

package org.apache.cloudstack.utils.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

public class QueueExecutor<K> {

    private final String name;
    private final int processingSize;
    private final int processingInterval;
    private final BlockingQueue<K> requestQueue;
    private final ScheduledExecutorService executorService;
    private final Logger logger;
    private final Consumer<K> consumer;

    public QueueExecutor(String name, int processingSize, int processingInterval, Logger logger,
             Consumer<K> consumer) {
        this.name = name;
        this.processingSize = processingSize;
        this.processingInterval = processingInterval;
        this.logger = logger;
        this.consumer = consumer;
        requestQueue = new LinkedBlockingQueue<>(processingSize);
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void queueRequest(K request) {
        try {
            requestQueue.put(request);
            if (requestQueue.size() >= processingSize) {
                processRequests();
            }
        } catch (InterruptedException e) {
            logger.warn(String.format("Error queuing request for %s", name), e);
        }
    }

    public void startProcessing() {
        executorService.scheduleAtFixedRate(this::processRequests, 0,
                processingInterval, TimeUnit.SECONDS);
    }

    private void processRequests() {
        List<K> requestsToProcess = new ArrayList<>();
        requestQueue.drainTo(requestsToProcess, processingSize);

        if (!requestsToProcess.isEmpty()) {
            for (K request : requestsToProcess) {
                consumer.accept(request);
            }
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
