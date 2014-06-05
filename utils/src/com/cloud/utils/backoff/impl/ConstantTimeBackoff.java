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

package com.cloud.utils.backoff.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Local;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.cloud.utils.NumbersUtil;
import com.cloud.utils.backoff.BackoffAlgorithm;
import com.cloud.utils.component.AdapterBase;

/**
 * An implementation of BackoffAlgorithm that waits for some seconds.
 * After the time the client can try to perform the operation again.
 *
 * @config
 * {@table
 *    || Param Name | Description | Values | Default ||
 *    || seconds    | seconds to sleep | integer | 5 ||
 *  }
 **/
@Local(value = {BackoffAlgorithm.class})
public class ConstantTimeBackoff extends AdapterBase implements BackoffAlgorithm, ConstantTimeBackoffMBean {
    long _time;
    private final Map<String, Thread> _asleep = new ConcurrentHashMap<String, Thread>();
    private final static Log LOG = LogFactory.getLog(ConstantTimeBackoff.class);

    @Override
    public void waitBeforeRetry() {
        Thread current = Thread.currentThread();
        try {
            _asleep.put(current.getName(), current);
            Thread.sleep(_time);
        } catch (InterruptedException e) {
            // JMX or other threads may interrupt this thread, but let's log it
            // anyway, no exception to log as this is not an error
            LOG.info("Thread " + current.getName() + " interrupted while waiting for retry");
        } finally {
            _asleep.remove(current.getName());
        }
        return;
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        _time = NumbersUtil.parseLong((String)params.get("seconds"), 5) * 1000;
        return true;
    }

    @Override
    public Collection<String> getWaiters() {
        return _asleep.keySet();
    }

    @Override
    public boolean wakeup(String threadName) {
        Thread th = _asleep.get(threadName);
        if (th != null) {
            th.interrupt();
            return true;
        }

        return false;
    }

    @Override
    public long getTimeToWait() {
        return _time;
    }

    @Override
    public void setTimeToWait(long seconds) {
        _time = seconds * 1000;
    }
}
