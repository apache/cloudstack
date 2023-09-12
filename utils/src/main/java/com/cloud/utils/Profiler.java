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

package com.cloud.utils;

public class Profiler {

    private static final long MILLIS_FACTOR = 1000l;
    private static final double EXPONENT = 2d;

    private Long startTickNanoSeconds;
    private Long stopTickNanoSeconds;

    public long start() {
        startTickNanoSeconds = System.nanoTime();
        return startTickNanoSeconds;
    }

    public long stop() {
        stopTickNanoSeconds = System.nanoTime();
        return stopTickNanoSeconds;
    }

    public void setStartTick(long value) {
        this.startTickNanoSeconds = value;
    }

    public void setStopTick(long value) {
        this.stopTickNanoSeconds = value;
    }

    /**
     * 1 millisecond = 1e+6 nanoseconds
     * 1 second = 1000 milliseconds = 1e+9 nanoseconds
     *
     * @return the duration in nanoseconds.
     */
    public long getDuration() {
        if (startTickNanoSeconds != null && stopTickNanoSeconds != null) {
            return stopTickNanoSeconds - startTickNanoSeconds;
        }

        return -1;
    }

    /**
     * 1 millisecond = 1e+6 nanoseconds
     * 1 second = 1000 millisecond = 1e+9 nanoseconds
     *
     * @return the duration in milliseconds.
     */
    public long getDurationInMillis() {
        if (startTickNanoSeconds != null && stopTickNanoSeconds != null) {
            return (stopTickNanoSeconds - startTickNanoSeconds) / (long)Math.pow(MILLIS_FACTOR, EXPONENT);
        }

        return -1;
    }

    public boolean isStarted() {
        return startTickNanoSeconds != null;
    }

    public boolean isStopped() {
        return stopTickNanoSeconds != null;
    }

    @Override
    public String toString() {
        if (startTickNanoSeconds == null) {
            return "Not Started";
        }

        if (stopTickNanoSeconds == null) {
            return "Started but not stopped";
        }

        return "Done. Duration: " + getDurationInMillis() + "ms";
    }
}
