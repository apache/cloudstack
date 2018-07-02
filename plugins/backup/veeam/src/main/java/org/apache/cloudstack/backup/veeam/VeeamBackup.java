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

package org.apache.cloudstack.backup.veeam;

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.backup.VMBackup;

public class VeeamBackup implements VMBackup {

    private String name;
    private String uid;

    public VeeamBackup(String name, String uid) {
        this.name = name;
        this.uid = uid;
    }

    @Override
    public Long getZoneId() {
        return null;
    }

    @Override
    public Long getAccountId() {
        return null;
    }

    @Override
    public String getExternalId() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Veeam Backup";
    }

    @Override
    public Long getVmId() {
        return null;
    }

    @Override
    public List<VolumeInfo> getBackedUpVolumes() {
        return null;
    }

    @Override
    public Status getStatus() {
        return null;
    }

    @Override
    public Long getSize() {
        return null;
    }

    @Override
    public Long getProtectedSize() {
        return null;
    }

    @Override
    public Date getCreated() {
        return null;
    }

    @Override
    public Date getRemoved() {
        return null;
    }

    @Override
    public String getUuid() {
        return uid;
    }

    @Override
    public long getId() {
        return -1;
    }
}
