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

package org.apache.cloudstack.storage.volume;

import com.cloud.hypervisor.Hypervisor;

import java.util.HashMap;
import java.util.Map;

public class VolumeOnStorageTO {
    Hypervisor.HypervisorType hypervisorType;
    private String path;
    private String fullPath;

    private String name;

    private String format;
    private long size;
    private long virtualSize;
    private String qemuEncryptFormat;
    private Map<Detail, String> details = new HashMap<>();

    public enum Detail {
        BACKING_FILE, BACKING_FILE_FORMAT, CLUSTER_SIZE, FILE_FORMAT, IS_LOCKED, IS_ENCRYPTED
    }

    public VolumeOnStorageTO() {
    }

    public VolumeOnStorageTO(Hypervisor.HypervisorType hypervisorType, String path, String name, String fullPath, String format, long size, long virtualSize) {
        this.hypervisorType = hypervisorType;
        this.path = path;
        this.name = name;
        this.fullPath = fullPath;
        this.format = format;
        this.size = size;
        this.virtualSize = virtualSize;
    }

    public Hypervisor.HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(Hypervisor.HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getVirtualSize() {
        return virtualSize;
    }

    public void setVirtualSize(long virtualSize) {
        this.virtualSize = virtualSize;
    }

    public String getQemuEncryptFormat() {
        return qemuEncryptFormat;
    }

    public void setQemuEncryptFormat(String qemuEncryptFormat) {
        this.qemuEncryptFormat = qemuEncryptFormat;
    }

    public Map<Detail, String> getDetails() {
        return details;
    }

    public void setDetails(Map<Detail, String> details) {
        this.details = details;
    }

    public void addDetail(Detail detail, String value) {
        details.put(detail, value);
    }
}
