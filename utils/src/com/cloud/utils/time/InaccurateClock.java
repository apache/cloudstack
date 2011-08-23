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
package com.cloud.utils.time;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.management.StandardMBean;

import org.apache.log4j.Logger;

import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.mgmt.JmxUtil;

/**
 * This clock is only accurate at a second basis, which is useful for most applications.
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

    @Override public long[] getCurrentTimes() {
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

    @Override public String turnOff() {
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
        @Override public void run() {
            try {
                time = System.currentTimeMillis();
            } catch (Throwable th) {
                try {
                    s_logger.error("Unable to time", th);
                } catch (Throwable th2) {
                }
            }
        }
    }
}
