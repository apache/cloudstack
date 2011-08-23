package com.cloud.utils.db;

import org.apache.log4j.Logger;

import junit.framework.Assert;

import com.cloud.utils.Profiler;
import com.cloud.utils.testcase.Log4jEnabledTestCase;

public class GlobalLockTest extends Log4jEnabledTestCase{
    public static final Logger s_logger = Logger.getLogger(GlobalLockTest.class);
    private final static GlobalLock _workLock = GlobalLock.getInternLock("SecurityGroupWork");
    public static class Worker implements Runnable {
        int id = 0;
        int timeoutSeconds = 10;
        int jobDuration = 2;
        public Worker(int id, int timeout, int duration) {
            this.id = id;
            timeoutSeconds = timeout;
            jobDuration = duration;
        }
        public void run() {
            boolean locked = false;
            try {
                Profiler p = new Profiler();
                p.start();
                locked = _workLock.lock(timeoutSeconds);
                p.stop();
                System.out.println("Thread " + id + " waited " + p.getDuration() + " ms, locked=" + locked);
                if (locked) {
                    Thread.sleep(jobDuration*1000);
                }
            } catch (InterruptedException e) {
            } finally {
                if (locked) {
                    boolean unlocked = _workLock.unlock();
                    System.out.println("Thread " + id + "  unlocked=" + unlocked);
                }
            }
        }
    }

    public void testTimeout() {
        Thread [] pool = new Thread[50];
        for (int i=0; i < pool.length; i++) {
            pool[i] = new Thread(new Worker(i, 5, 3));
        }
        for (int i=0; i < pool.length; i++) {
            pool[i].start();
        }
        for (int i=0; i < pool.length; i++) {
            try {
                pool[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
