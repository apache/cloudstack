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
package com.cloud.vm;

public class VmWorkResizeVolume extends VmWork {
    private static final long serialVersionUID = 6112366316907642498L;

    private long volumeId;
    private long currentSize;
    private long newSize;
    private Long newMinIops;
    private Long newMaxIops;
    private Long newServiceOfferingId;
    private boolean shrinkOk;

    public VmWorkResizeVolume(long userId, long accountId, long vmId, String handlerName,
            long volumeId, long currentSize, long newSize, Long newMinIops, Long newMaxIops, Long newServiceOfferingId, boolean shrinkOk) {

        super(userId, accountId, vmId, handlerName);

        this.volumeId = volumeId;
        this.currentSize = currentSize;
        this.newSize = newSize;
        this.newMinIops = newMinIops;
        this.newMaxIops = newMaxIops;
        this.newServiceOfferingId = newServiceOfferingId;
        this.shrinkOk = shrinkOk;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public long getCurrentSize() {
        return currentSize;
    }

    public long getNewSize() {
        return newSize;
    }

    public Long getNewMinIops() {
        return newMinIops;
    }

    public Long getNewMaxIops() {
        return newMaxIops;
    }

    public Long getNewServiceOfferingId() {
        return newServiceOfferingId;
    }

    public boolean isShrinkOk() {
        return shrinkOk;
    }
}
