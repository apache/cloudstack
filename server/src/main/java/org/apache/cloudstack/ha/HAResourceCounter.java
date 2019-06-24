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

package org.apache.cloudstack.ha;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public final class HAResourceCounter {
    private AtomicLong activityCheckCounter = new AtomicLong(0);
    private AtomicLong activityCheckFailureCounter = new AtomicLong(0);
    private AtomicLong recoveryOperationCounter = new AtomicLong(0);

    private Long firstHealthCheckFailureTimestamp;
    private Long lastActivityCheckTimestamp;
    private Long degradedTimestamp;
    private Long recoverTimestamp;
    private Future<Boolean> recoveryFuture;
    private Future<Boolean> fenceFuture;

    public long getActivityCheckCounter() {
        return activityCheckCounter.get();
    }

    public long getRecoveryCounter() {
        return recoveryOperationCounter.get();
    }

    public synchronized void incrActivityCounter(final boolean isFailure) {
        activityCheckCounter.incrementAndGet();
        if (isFailure) {
            activityCheckFailureCounter.incrementAndGet();
        }
    }

    public synchronized void incrRecoveryCounter() {
        recoveryOperationCounter.incrementAndGet();
    }

    public synchronized void resetActivityCounter() {
        activityCheckCounter.set(0);
        activityCheckFailureCounter.set(0);
    }

    public synchronized void resetRecoveryCounter() {
        recoverTimestamp = null;
        recoveryFuture = null;
        recoveryOperationCounter.set(0);
    }

    public synchronized void resetSuspectTimestamp() {
        firstHealthCheckFailureTimestamp = null;
    }

    public boolean hasActivityThresholdExceeded(final double failureRatio) {
        return activityCheckFailureCounter.get() > (activityCheckCounter.get() * failureRatio);
    }

    public synchronized boolean canPerformActivityCheck(final Long activityCheckInterval) {
        if (lastActivityCheckTimestamp == null || (System.currentTimeMillis() - lastActivityCheckTimestamp) > (activityCheckInterval * 1000)) {
            lastActivityCheckTimestamp = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public boolean canRecheckActivity(final Long maxDegradedPeriod) {
        return degradedTimestamp == null || (System.currentTimeMillis() - degradedTimestamp) > (maxDegradedPeriod * 1000);
    }

    public boolean canExitRecovery(final Long maxRecoveryWaitPeriod) {
        return recoverTimestamp != null && (System.currentTimeMillis() - recoverTimestamp) > (maxRecoveryWaitPeriod * 1000);
    }

    public long getSuspectTimeStamp() {
        if (firstHealthCheckFailureTimestamp == null) {
            firstHealthCheckFailureTimestamp = System.currentTimeMillis();
        }
        return firstHealthCheckFailureTimestamp;
    }

    public synchronized void markResourceSuspected() {
        firstHealthCheckFailureTimestamp = System.currentTimeMillis();
    }

    public synchronized void markResourceDegraded() {
        degradedTimestamp = System.currentTimeMillis();
    }

    public synchronized void markRecoveryStarted() {
        if (recoverTimestamp == null) {
            recoverTimestamp = System.currentTimeMillis();
        }
    }

    public synchronized void markRecoveryCompleted() {
        recoverTimestamp = null;
        recoveryFuture = null;
    }

    public void setRecoveryFuture(final Future<Boolean> future) {
        recoveryFuture = future;
    }

    public boolean canAttemptRecovery() {
        return recoveryFuture == null || recoveryFuture.isDone();
    }

    public void setFenceFuture(final Future<Boolean> future) {
        fenceFuture = future;
    }

    public boolean canAttemptFencing() {
        return fenceFuture == null || fenceFuture.isDone();
    }

}
