// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils;

public class Profiler {
    private Long startTickInMs;
    private Long stopTickInMs;

    public Profiler() {
        startTickInMs = null;
        stopTickInMs = null;
    }

    public long start() {
        startTickInMs = System.currentTimeMillis();
        return startTickInMs.longValue();
    }

    public long stop() {
        stopTickInMs = System.currentTimeMillis();
        return stopTickInMs.longValue();
    }

    public long getDuration() {
        if (startTickInMs != null && stopTickInMs != null)
            return stopTickInMs.longValue() - startTickInMs.longValue();

        return -1;
    }

    public boolean isStarted() {
        return startTickInMs != null;
    }

    public boolean isStopped() {
        return stopTickInMs != null;
    }

    @Override
    public String toString() {
        if (startTickInMs == null)
            return "Not Started";

        if (stopTickInMs == null)
            return "Started but not stopped";

        return "Done. Duration: " + getDuration() + "ms";
    }
}
