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
package org.apache.cloudstack.vm;

public class VmwareCbtMigrationCutoverPolicy {

    public enum Decision {
        CONTINUE,
        READY_FOR_CUTOVER,
        READY_FOR_CUTOVER_MAX_CYCLES
    }

    private final int minCycles;
    private final int maxCycles;
    private final int quietCyclesRequired;
    private final long quietDirtyBytesThreshold;
    private final long quietDirtyRateBytesPerSecondThreshold;

    public VmwareCbtMigrationCutoverPolicy(int minCycles, int maxCycles, int quietCyclesRequired,
                                           long quietDirtyBytesThreshold, long quietDirtyRateBytesPerSecondThreshold) {
        if (minCycles < 1) {
            throw new IllegalArgumentException("Minimum CBT migration cycles must be at least 1");
        }
        if (maxCycles < minCycles) {
            throw new IllegalArgumentException("Maximum CBT migration cycles must be greater than or equal to minimum cycles");
        }
        if (quietCyclesRequired < 1) {
            throw new IllegalArgumentException("Required quiet CBT migration cycles must be at least 1");
        }
        this.minCycles = minCycles;
        this.maxCycles = maxCycles;
        this.quietCyclesRequired = quietCyclesRequired;
        this.quietDirtyBytesThreshold = quietDirtyBytesThreshold;
        this.quietDirtyRateBytesPerSecondThreshold = quietDirtyRateBytesPerSecondThreshold;
    }

    public Decision decide(int completedCycles, int quietCycles, long lastChangedBytes, long lastCycleDurationSeconds) {
        if (completedCycles >= maxCycles) {
            return Decision.READY_FOR_CUTOVER_MAX_CYCLES;
        }
        if (completedCycles < minCycles) {
            return Decision.CONTINUE;
        }
        if (lastChangedBytes == 0) {
            return Decision.READY_FOR_CUTOVER;
        }
        int updatedQuietCycles = isQuietCycle(lastChangedBytes, lastCycleDurationSeconds) ? quietCycles + 1 : 0;
        if (updatedQuietCycles >= quietCyclesRequired) {
            return Decision.READY_FOR_CUTOVER;
        }
        return Decision.CONTINUE;
    }

    public boolean isQuietCycle(long changedBytes, long cycleDurationSeconds) {
        boolean withinChangedBytes = quietDirtyBytesThreshold <= 0 || changedBytes <= quietDirtyBytesThreshold;
        boolean withinDirtyRate = quietDirtyRateBytesPerSecondThreshold <= 0 ||
                getDirtyRateBytesPerSecond(changedBytes, cycleDurationSeconds) <= quietDirtyRateBytesPerSecondThreshold;
        return withinChangedBytes && withinDirtyRate;
    }

    protected long getDirtyRateBytesPerSecond(long changedBytes, long cycleDurationSeconds) {
        if (cycleDurationSeconds <= 0) {
            return changedBytes;
        }
        return changedBytes / cycleDurationSeconds;
    }
}
