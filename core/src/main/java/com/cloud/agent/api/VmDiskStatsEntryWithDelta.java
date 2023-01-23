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

package com.cloud.agent.api;

public class VmDiskStatsEntryWithDelta extends VmDiskStatsEntry {

    long deltaIoRead = 0;
    long deltaIoWrite = 0;
    long deltaBytesWrite = 0;
    long deltaBytesRead = 0;

    public VmDiskStatsEntryWithDelta() {
    }

    public VmDiskStatsEntryWithDelta(String vmName, String path, long ioWrite, long ioRead, long bytesWrite, long bytesRead) {
        super(vmName, path, ioWrite, ioRead, bytesWrite, bytesRead);
    }

    public long getDeltaIoRead() {
        return deltaIoRead;
    }

    public void setDeltaIoRead(long deltaIoRead) {
        this.deltaIoRead = deltaIoRead;
    }

    public long getDeltaIoWrite() {
        return deltaIoWrite;
    }

    public void setDeltaIoWrite(long deltaIoWrite) {
        this.deltaIoWrite = deltaIoWrite;
    }

    public long getDeltaBytesWrite() {
        return deltaBytesWrite;
    }

    public void setDeltaBytesWrite(long deltaBytesWrite) {
        this.deltaBytesWrite = deltaBytesWrite;
    }

    public long getDeltaBytesRead() {
        return deltaBytesRead;
    }

    public void setDeltaBytesRead(long deltaBytesRead) {
        this.deltaBytesRead = deltaBytesRead;
    }
}
