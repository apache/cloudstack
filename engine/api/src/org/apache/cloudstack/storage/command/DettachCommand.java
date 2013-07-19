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
package org.apache.cloudstack.storage.command;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DiskTO;

public class DettachCommand extends Command implements StorageSubSystemCommand {
    private DiskTO disk;
    private String vmName;
    private boolean _managed;
    private String _iScsiName;
    private String _storageHost;
    private int _storagePort;

    public DettachCommand(DiskTO disk, String vmName) {
        super();
        this.disk = disk;
        this.vmName = vmName;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public DiskTO getDisk() {
        return disk;
    }

    public void setDisk(DiskTO disk) {
        this.disk = disk;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public void setManaged(boolean managed) {
        _managed = managed;
    }

    public boolean isManaged() {
        return _managed;
    }

    public void set_iScsiName(String iScsiName) {
        _iScsiName = iScsiName;
    }

    public String get_iScsiName() {
        return _iScsiName;
    }

    public void setStorageHost(String storageHost) {
        _storageHost = storageHost;
    }

    public String getStorageHost() {
        return _storageHost;
    }

    public void setStoragePort(int storagePort) {
        _storagePort = storagePort;
    }

    public int getStoragePort() {
        return _storagePort;
    }
}
