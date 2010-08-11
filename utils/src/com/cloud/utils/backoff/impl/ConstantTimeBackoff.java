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
package com.cloud.utils.backoff.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.ejb.Local;

import com.cloud.utils.NumbersUtil;
import com.cloud.utils.backoff.BackoffAlgorithm;

/**
 * Implementation of the Agent Manager.  This class controls the connection
 * to the agents.
 * 
 * @config
 * {@table 
 *    || Param Name | Description | Values | Default ||
 *    || seconds    | seconds to sleep | integer | 5 ||
 *  }
 **/ 
@Local(value={BackoffAlgorithm.class})
public class ConstantTimeBackoff implements BackoffAlgorithm, ConstantTimeBackoffMBean {
    int _count = 0;
    long _time;
    String _name;
    ConcurrentHashMap<String, Thread> _asleep = new ConcurrentHashMap<String, Thread>();

    @Override
    public void waitBeforeRetry() {
        _count++;
        try {
            Thread current = Thread.currentThread();
            _asleep.put(current.getName(), current);
            Thread.sleep(_time);
            _asleep.remove(current.getName());
        } catch(InterruptedException e) {
            
        }
        return;
    }

    @Override
    public void reset() {
        _count = 0;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        _name = name;
        _time = NumbersUtil.parseLong((String)params.get("seconds"), 5) * 1000;
        return true;
    }

    @Override
    public String getName() {
        return _name;
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
    public boolean start() {
        _count = 0;
        return true;
    }

    @Override
    public boolean stop() {
        return true;
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
