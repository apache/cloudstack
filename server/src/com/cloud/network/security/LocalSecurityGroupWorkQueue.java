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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.cloud.network.security.SecurityGroupWork.Step;

/**
 * Security Group Work Queue that is not shared with other management servers
 *
 */
public class LocalSecurityGroupWorkQueue implements SecurityGroupWorkQueue {
    protected static Logger s_logger = Logger.getLogger(LocalSecurityGroupWorkQueue.class);

    //protected Set<SecurityGroupWork> _currentWork = new HashSet<SecurityGroupWork>();
    protected Set<SecurityGroupWork> _currentWork = new TreeSet<SecurityGroupWork>();

    private final ReentrantLock _lock = new ReentrantLock();
    private final Condition _notEmpty = _lock.newCondition();
    private final AtomicInteger _count = new AtomicInteger(0);

    public static class LocalSecurityGroupWork implements SecurityGroupWork, Comparable<LocalSecurityGroupWork> {
        Long _logSequenceNumber;
        Long _instanceId;
        Step _step;

        public LocalSecurityGroupWork(Long instanceId, Long logSequence, Step step) {
            this._instanceId = instanceId;
            this._logSequenceNumber = logSequence;
            this._step = step;
        }

        @Override
        public Long getInstanceId() {
            return _instanceId;
        }

        @Override
        public Long getLogsequenceNumber() {
            return _logSequenceNumber;
        }

        @Override
        public Step getStep() {
            return _step;
        }

        @Override
        public void setStep(Step step) {
            this._step = step;
        }

        @Override
        public void setLogsequenceNumber(Long logsequenceNumber) {
            this._logSequenceNumber = logsequenceNumber;

        }

        @Override
        public int compareTo(LocalSecurityGroupWork o) {
            //return this._instanceId.compareTo(o.getInstanceId());
            return o.getInstanceId().compareTo(this.getInstanceId());

        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof LocalSecurityGroupWork) {
                LocalSecurityGroupWork other = (LocalSecurityGroupWork)obj;
                return this.getInstanceId().longValue() == other.getInstanceId().longValue();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return getInstanceId().hashCode();
        }

    }

    @Override
    public void submitWorkForVm(long vmId, long sequenceNumber) {
        _lock.lock();
        try {
            SecurityGroupWork work = new LocalSecurityGroupWork(vmId, sequenceNumber, Step.Scheduled);
            boolean added = _currentWork.add(work);
            if (added)
                _count.incrementAndGet();
        } finally {
            _lock.unlock();
        }
        signalNotEmpty();

    }

    @Override
    public int submitWorkForVms(Set<Long> vmIds) {
        _lock.lock();
        int newWork = _count.get();
        try {
            for (Long vmId : vmIds) {
                SecurityGroupWork work = new LocalSecurityGroupWork(vmId, null, SecurityGroupWork.Step.Scheduled);
                boolean added = _currentWork.add(work);
                if (added)
                    _count.incrementAndGet();
            }
        } finally {
            newWork = _count.get() - newWork;
            _lock.unlock();
        }
        signalNotEmpty();
        return newWork;
    }

    @Override
    public List<SecurityGroupWork> getWork(int numberOfWorkItems) throws InterruptedException {
        List<SecurityGroupWork> work = new ArrayList<SecurityGroupWork>(numberOfWorkItems);
        _lock.lock();
        int i = 0;
        try {
            while (_count.get() == 0) {
                _notEmpty.await();
            }
            int n = Math.min(numberOfWorkItems, _count.get());
            Iterator<SecurityGroupWork> iter = _currentWork.iterator();
            while (i < n) {
                SecurityGroupWork w = iter.next();
                w.setStep(Step.Processing);
                work.add(w);
                iter.remove();
                ++i;
            }
        } finally {
            int c = _count.addAndGet(-i);
            if (c > 0)
                _notEmpty.signal();
            _lock.unlock();
        }
        return work;

    }

    private void signalNotEmpty() {
        _lock.lock();
        try {
            _notEmpty.signal();
        } finally {
            _lock.unlock();
        }
    }

    @Override
    public int size() {
        return _count.get();
    }

    @Override
    public void clear() {
        _lock.lock();
        try {
            _currentWork.clear();
            _count.set(0);
        } finally {
            _lock.unlock();
        }

    }

    @Override
    public List<Long> getVmsInQueue() {
        List<Long> vmIds = new ArrayList<Long>();
        _lock.lock();
        try {
            Iterator<SecurityGroupWork> iter = _currentWork.iterator();
            while (iter.hasNext()) {
                vmIds.add(iter.next().getInstanceId());
            }
        } finally {
            _lock.unlock();
        }
        return vmIds;
    }

}
