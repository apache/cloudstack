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
package com.cloud.kubernetes.cluster.utils;

import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;

/** CPU and memory as seen inside a node VM. */
public final class GuestResources {
    private final long onlineCpus;
    private final long memoryKiB;
    private final long onlineMemoryBytes;
    private final long memoryBlockBytes;

    /**
     * @param onlineCpus        CPUs online in the guest
     * @param memoryKiB         MemTotal from /proc/meminfo, which kubelet reports as the node's memory capacity
     * @param onlineMemoryBytes size of the online memory blocks under /sys/devices/system/memory
     * @param memoryBlockBytes  size of one of those memory blocks
     */
    public GuestResources(long onlineCpus,
                          long memoryKiB,
                          long onlineMemoryBytes,
                          long memoryBlockBytes) {
        this.onlineCpus = onlineCpus;
        this.memoryKiB = memoryKiB;
        this.onlineMemoryBytes = onlineMemoryBytes;
        this.memoryBlockBytes = memoryBlockBytes;
    }

    public long getOnlineCpus() {
        return onlineCpus;
    }

    public long getMemoryKiB() {
        return memoryKiB;
    }

    public long getMemoryBytes() {
        return memoryKiB * ByteScaleUtils.KiB;
    }

    public long getOnlineMemoryBytes() {
        return onlineMemoryBytes;
    }

    public long getMemoryBlockBytes() {
        return memoryBlockBytes;
    }
}
