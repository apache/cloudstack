//
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
//

package com.cloud.utils.time;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.management.StandardMBean;

import org.apache.log4j.Logger;

import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.mgmt.JmxUtil;

/**
 */

public class InaccurateClock extends StandardMBean implements InaccurateClockMBean {
    private static final Logger s_logger = Logger.getLogger(InaccurateClock.class);
    static ScheduledExecutorService s_executor = null;
    static final InaccurateClock s_timer = new InaccurateClock();
    private static long time;

    public InaccurateClock() {
        super(InaccurateClockMBean.class, false);
        time = System.currentTimeMillis();
        restart();
        try {
            JmxUtil.registerMBean("InaccurateClock", "InaccurateClock", this);
        } catch (Exception e) {
            s_logger.warn("Unable to initialize inaccurate clock", e);
        }
    }

    @Override
    public long[] getCurrentTimes() {
        long[] results = new long[2];
        results[0] = time;
        results[1] = System.currentTimeMillis();

        return results;
    }

    @Override
    public synchronized String restart() {
        turnOff();
        s_executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("InaccurateClock"));
        s_executor.scheduleAtFixedRate(new SetTimeTask(), 0, 60, TimeUnit.SECONDS);
        return "Restarted";
    }

    @Override
    public String turnOff() {
        if (s_executor != null) {
            try {
                s_executor.shutdown();
            } catch (Throwable th) {
                s_logger.error("Unable to shutdown the Executor", th);
                return "Unable to turn off check logs";
            }
        }
        s_executor = null;
        return "Off";
    }

    public static long getTime() {
        return s_executor != null ? time : System.currentTimeMillis();
    }

    public static long getTimeInSeconds() {
        return time / 1000;
    }

    protected class SetTimeTask implements Runnable {
        @Override
        public void run() {
            try {
                time = System.currentTimeMillis();
            } catch (Throwable th) {
                s_logger.error("Unable to time", th);
            }
        }
    }
}
