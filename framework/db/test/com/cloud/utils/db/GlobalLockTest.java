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
package com.cloud.utils.db;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.utils.Profiler;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/testContext.xml")
public class GlobalLockTest {
    public static final Logger s_logger = Logger.getLogger(GlobalLockTest.class);
    private final static GlobalLock WorkLock = GlobalLock.getInternLock("SecurityGroupWork");

    public static class Worker implements Runnable {
        int id = 0;
        int timeoutSeconds = 10;
        int jobDuration = 2;

        public Worker(int id, int timeout, int duration) {
            this.id = id;
            timeoutSeconds = timeout;
            jobDuration = duration;
        }

        @Override
        public void run() {
            boolean locked = false;
            try {
                Profiler p = new Profiler();
                p.start();
                locked = WorkLock.lock(timeoutSeconds);
                p.stop();
                System.out.println("Thread " + id + " waited " + p.getDurationInMillis() + " ms, locked=" + locked);
                if (locked) {
                    Thread.sleep(jobDuration * 1000);
                }
            } catch (InterruptedException e) {
                s_logger.debug("[ignored] interupted while testing global lock.");
            } finally {
                if (locked) {
                    boolean unlocked = WorkLock.unlock();
                    System.out.println("Thread " + id + "  unlocked=" + unlocked);
                }
            }
        }
    }

    @Test
    public void testTimeout() {
        Thread[] pool = new Thread[50];
        for (int i = 0; i < pool.length; i++) {
            pool[i] = new Thread(new Worker(i, 5, 3));
        }
        for (int i = 0; i < pool.length; i++) {
            pool[i].start();
        }
        for (int i = 0; i < pool.length; i++) {
            try {
                pool[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
