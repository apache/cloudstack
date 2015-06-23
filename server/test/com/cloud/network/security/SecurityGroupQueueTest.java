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
package com.cloud.network.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import com.cloud.utils.Profiler;

public class SecurityGroupQueueTest extends TestCase {
    public final static SecurityGroupWorkQueue queue = new LocalSecurityGroupWorkQueue();

    public static class Producer implements Runnable {
        int _maxVmId = 0;
        int _newWorkQueued = 0;
        Set<Long> vmIds = new HashSet<Long>();

        public Producer(int maxVmId) {
            this._maxVmId = maxVmId;
            for (long i = 1; i <= _maxVmId; i++) {
                vmIds.add(i);
            }
        }

        @Override
        public void run() {
            _newWorkQueued = queue.submitWorkForVms(vmIds);
        }

        public int getNewWork() {
            return _newWorkQueued;
        }

        public int getTotalWork() {
            return _maxVmId;
        }
    }

    public static class Consumer implements Runnable {
        private int _numJobsToDequeue = 0;
        private int _numJobsDequeued = 0;

        public Consumer(int numJobsToDequeu) {
            this._numJobsToDequeue = numJobsToDequeu;
        }

        @Override
        public void run() {
            List<SecurityGroupWork> result = new ArrayList<SecurityGroupWork>();
            try {
                result = queue.getWork(_numJobsToDequeue);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this._numJobsDequeued = result.size();
        }

        int getNumJobsToDequeue() {
            return _numJobsToDequeue;
        }

        int getNumJobsDequeued() {
            return _numJobsDequeued;
        }
    }

    public void testNumJobsEqToNumVms1() {
        queue.clear();
        final int numProducers = 50;
        Thread[] pThreads = new Thread[numProducers];

        Producer[] producers = new Producer[numProducers];
        int numProduced = 0;

        for (int i = 0; i < numProducers; i++) {
            producers[i] = new Producer(i + 1);
            pThreads[i] = new Thread(producers[i]);
            numProduced += i + 1;
            pThreads[i].start();
        }
        for (int i = 0; i < numProducers; i++) {
            try {
                pThreads[i].join();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        System.out.println("Num Vms= " + numProducers + " Queue size = " + queue.size());
        assertEquals(numProducers, queue.size());
    }

    protected void testNumJobsEqToNumVms2(int numProducers, int maxVmId) {
        queue.clear();

        Thread[] pThreads = new Thread[numProducers];

        Producer[] producers = new Producer[numProducers];
        int numProduced = 0;
        Profiler p = new Profiler();
        p.start();
        for (int i = 0; i < numProducers; i++) {
            producers[i] = new Producer(maxVmId);
            pThreads[i] = new Thread(producers[i]);
            numProduced += i + 1;
            pThreads[i].start();
        }
        for (int i = 0; i < numProducers; i++) {
            try {
                pThreads[i].join();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        p.stop();
        System.out.println("Num Vms= " + maxVmId + " Queue size = " + queue.size() + " time=" + p.getDurationInMillis() + " ms");
        assertEquals(maxVmId, queue.size());
    }

    public void testNumJobsEqToNumVms3() {
        testNumJobsEqToNumVms2(50, 20000);
        testNumJobsEqToNumVms2(400, 5000);
        testNumJobsEqToNumVms2(1, 1);
        testNumJobsEqToNumVms2(1, 1000000);
        testNumJobsEqToNumVms2(750, 1);

    }

    protected void _testDequeueOneJob(final int numConsumers, final int numProducers, final int maxVmId) {
        queue.clear();

        Thread[] pThreads = new Thread[numProducers];
        Thread[] cThreads = new Thread[numConsumers];

        Consumer[] consumers = new Consumer[numConsumers];
        Producer[] producers = new Producer[numProducers];

        int numProduced = 0;
        for (int i = 0; i < numConsumers; i++) {
            consumers[i] = new Consumer(1);
            cThreads[i] = new Thread(consumers[i]);
            cThreads[i].start();
        }
        for (int i = 0; i < numProducers; i++) {
            producers[i] = new Producer(maxVmId);
            pThreads[i] = new Thread(producers[i]);
            numProduced += maxVmId;
            pThreads[i].start();
        }
        for (int i = 0; i < numConsumers; i++) {
            try {
                cThreads[i].join();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        for (int i = 0; i < numProducers; i++) {
            try {
                pThreads[i].join();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }

        int totalDequeued = 0;
        for (int i = 0; i < numConsumers; i++) {
            //System.out.println("Consumer " + i + " ask to dequeue " + consumers[i].getNumJobsToDequeue() + ", dequeued " + consumers[i].getNumJobsDequeued());
            totalDequeued += consumers[i].getNumJobsDequeued();
        }
        int totalQueued = 0;
        for (int i = 0; i < numProducers; i++) {
            //System.out.println("Producer " + i + " ask to queue " + producers[i].getTotalWork() + ", queued " + producers[i].getNewWork());
            totalQueued += producers[i].getNewWork();
        }
        System.out.println("Total jobs dequeued = " + totalDequeued + ", num queued=" + totalQueued + " queue current size=" + queue.size());
        assertEquals(totalDequeued, numConsumers);
        assertEquals(totalQueued - totalDequeued, queue.size());
    }

    public void testDequeueOneJobAgain() {
        _testDequeueOneJob(10, 10, 1000);
        int queueSize = queue.size();
        Thread cThread = new Thread(new Consumer(1));
        cThread.start();
        try {
            cThread.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertEquals(queue.size(), queueSize - 1);
    }

    public void testDequeueOneJob() {
        _testDequeueOneJob(10, 10, 1000);
        _testDequeueOneJob(1, 10, 1000);
        _testDequeueOneJob(10, 1, 1000);
        _testDequeueOneJob(10, 1, 10);
    }

}
