/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.offering;

public class DiskOfferingInfo {
    private DiskOffering _diskOffering;
    private Long _size;
    private Long _minIops;
    private Long _maxIops;

    public DiskOfferingInfo() {
    }

    public DiskOfferingInfo(DiskOffering diskOffering) {
        _diskOffering = diskOffering;
    }

    public void setDiskOffering(DiskOffering diskOffering) {
        _diskOffering = diskOffering;
    }

    public DiskOffering getDiskOffering() {
        return _diskOffering;
    }

    public void setSize(Long size) {
        _size = size;
    }

    public Long getSize() {
        return _size;
    }

    public void setMinIops(Long minIops) {
        _minIops = minIops;
    }

    public Long getMinIops() {
        return _minIops;
    }

    public void setMaxIops(Long maxIops) {
        _maxIops = maxIops;
    }

    public Long getMaxIops() {
        return _maxIops;
    }
}
