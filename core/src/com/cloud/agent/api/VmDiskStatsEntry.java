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

import com.cloud.vm.VmDiskStats;

public class VmDiskStatsEntry implements VmDiskStats {
	
	String vmName;
	String path;
	Long ioRead = 0L;
	Long ioWrite = 0L;
	Long bytesWrite = 0L;
	Long bytesRead = 0L;
	    
    public VmDiskStatsEntry() {
    }
    
    public VmDiskStatsEntry(String vmName, String path, Long ioWrite, Long ioRead, Long bytesWrite, Long bytesRead) {
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
    
    public void setBytesRead(Long bytesRead) {
    	this.bytesRead = bytesRead;
    }

    public Long getBytesRead() {
    	return bytesRead;
    }
    
    public void setBytesWrite(Long bytesWrite) {
    	this.bytesWrite = bytesWrite;
    }
    
    public Long getBytesWrite() {
    	return bytesWrite;
    }
    
    public void setIORead(Long ioRead) {
        this.ioRead = ioRead;
    }

    public Long getIORead() {
        return ioRead;
    }
    
    public void setIOWrite(Long ioWrite) {
        this.ioWrite = ioWrite;
    }
    
    public Long getIOWrite() {
        return ioWrite;
    }

}
