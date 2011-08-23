package com.cloud.network.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class SecurityGroupQueueTest extends TestCase {
    public final static SecurityGroupWorkQueue queue = new LocalSecurityGroupWorkQueue();
  
    
    public static class Producer implements Runnable {
        int _maxVmId = 0;
        int _newWorkQueued=0;
        Set<Long> vmIds = new HashSet<Long>();
        
        public Producer(int maxVmId) {
           this._maxVmId = maxVmId; 
           for (long i=1; i <= _maxVmId; i++) {
               vmIds.add(i);
           }
        }
        
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
        
        public void run() {
            List<SecurityGroupWork> result = queue.getWork(_numJobsToDequeue);
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
        Thread [] pThreads = new Thread[numProducers];
        
        Producer [] producers = new Producer[numProducers];
        int numProduced = 0;

        for (int i=0; i < numProducers; i++) {
            producers[i] = new Producer(i+1);
            pThreads[i] = new Thread(producers[i]);
            numProduced += i+1;
            pThreads[i].start();
        }
        for (int i=0; i < numProducers ; i++) {
            try {
                pThreads[i].join();
            } catch (InterruptedException ie){
                ie.printStackTrace();
            }
        }
        System.out.println("Num Vms= " + numProducers + " Queue size = " + queue.size());
        assert(numProducers == queue.size());
    }
    
    public void testNumJobsEqToNumVms2() {
        queue.clear();

        final int numProducers = 50;
        Thread [] pThreads = new Thread[numProducers];
        
        Producer [] producers = new Producer[numProducers];
        int numProduced = 0;
        int maxVmId = 10000;
        for (int i=0; i < numProducers; i++) {
            producers[i] = new Producer(maxVmId);
            pThreads[i] = new Thread(producers[i]);
            numProduced += i+1;
            pThreads[i].start();
        }
        for (int i=0; i < numProducers ; i++) {
            try {
                pThreads[i].join();
            } catch (InterruptedException ie){
                ie.printStackTrace();
            }
        }
        System.out.println("Num Vms= " + maxVmId + " Queue size = " + queue.size());
        assert(maxVmId == queue.size());
    }
    
    public void testDequeueOneJob() {
        queue.clear();

        final int numProducers = 2;
        final int numConsumers = 5;
        final int maxVmId = 200;

        Thread [] pThreads = new Thread[numProducers];
        Thread [] cThreads = new Thread[numConsumers];
        
        
        Consumer [] consumers = new Consumer[numConsumers];
        Producer [] producers = new Producer[numProducers];
        
        int numProduced = 0;
        for (int i=0; i < numConsumers; i++) {
            consumers[i] = new Consumer(1);
            cThreads[i] = new Thread(consumers[i]);
            cThreads[i].start();
        }
        for (int i=0; i < numProducers; i++) {
            producers[i] = new Producer(maxVmId);
            pThreads[i] = new Thread(producers[i]);
            numProduced += maxVmId;
            pThreads[i].start();
        }
        for (int i=0; i < numConsumers ; i++) {
            try {
                cThreads[i].join();
            } catch (InterruptedException ie){
                ie.printStackTrace();
            }
        }
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        int totalDequeued = 0;
        for (int i=0; i < numConsumers; i++) {
            //System.out.println("Consumer " + i + " ask to dequeue " + consumers[i].getNumJobsToDequeue() + ", dequeued " + consumers[i].getNumJobsDequeued());
            totalDequeued += consumers[i].getNumJobsDequeued();
        }
        int totalQueued = 0;
        for (int i=0; i < numProducers; i++) {
            //System.out.println("Producer " + i + " ask to queue " + producers[i].getTotalWork() + ", queued " + producers[i].getNewWork());
            totalQueued += producers[i].getNewWork();
        }
        System.out.println("Total jobs dequeued = " + totalDequeued + ", num queued=" + totalQueued + " queue current size=" + queue.size());
        assert(totalDequeued == numConsumers);
        assert(totalQueued - totalDequeued == queue.size());
    }

}
