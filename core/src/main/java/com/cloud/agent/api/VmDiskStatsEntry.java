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

package com.cloud.agent.api;

import com.cloud.vm.VmDiskStats;

public class VmDiskStatsEntry implements VmDiskStats {

    String vmName;
    String path;
    long ioRead = 0;
    long ioWrite = 0;
    long bytesWrite = 0;
    long bytesRead = 0;

    public VmDiskStatsEntry() {
    }

    public VmDiskStatsEntry(String vmName, String path, long ioWrite, long ioRead, long bytesWrite, long bytesRead) {
        this.ioRead = ioRead;
        this.ioWrite = ioWrite;
        this.bytesRead = bytesRead;
        this.bytesWrite = bytesWrite;
        this.vmName = vmName;
        this.path = path;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getVmName() {
        return vmName;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setBytesRead(long bytesRead) {
        this.bytesRead = bytesRead;
    }

    @Override
    public long getBytesRead() {
        return bytesRead;
    }

    public void setBytesWrite(long bytesWrite) {
        this.bytesWrite = bytesWrite;
    }

    @Override
    public long getBytesWrite() {
        return bytesWrite;
    }

    public void setIORead(long ioRead) {
        this.ioRead = ioRead;
    }

    @Override
    public long getIORead() {
        return ioRead;
    }

    public void setIOWrite(long ioWrite) {
        this.ioWrite = ioWrite;
    }

    @Override
    public long getIOWrite() {
        return ioWrite;
    }

}
