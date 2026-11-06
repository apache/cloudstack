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
package com.cloud.host;

/**
 * Smoothed view of what a host is actually doing, as opposed to what has been allocated on it.
 * Fractions are of the host's real capacity and ignore overprovisioning.
 */
public class HostLoad {

    public static final HostLoad UNKNOWN = new HostLoad(0, 0, 0);

    private final double cpuUtilisation;
    private final double memoryUtilisation;
    private final long samples;

    public HostLoad(double cpuUtilisation, double memoryUtilisation, long samples) {
        this.cpuUtilisation = cpuUtilisation;
        this.memoryUtilisation = memoryUtilisation;
        this.samples = samples;
    }

    public double getCpuUtilisation() {
        return cpuUtilisation;
    }

    public double getMemoryUtilisation() {
        return memoryUtilisation;
    }

    public long getSamples() {
        return samples;
    }

    /**
     * False until enough has been observed to rank on. Callers fall back to allocation figures.
     */
    public boolean isUsable() {
        return samples > 0;
    }

    @Override
    public String toString() {
        return String.format("HostLoad[cpu=%.3f, memory=%.3f, samples=%d]", cpuUtilisation, memoryUtilisation, samples);
    }
}
