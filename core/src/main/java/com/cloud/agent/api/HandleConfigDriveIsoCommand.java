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

import java.util.List;

import com.cloud.agent.api.to.DataStoreTO;

public class HandleConfigDriveIsoCommand extends Command {

    String isoFile;
    List<String[]> vmData;
    String configDriveLabel;
    boolean create = false;
    private boolean update = false;
    private DataStoreTO destStore;

    public HandleConfigDriveIsoCommand(List<String[]> vmData, String label, DataStoreTO destStore, String isoFile, boolean create, boolean update) {
        this.vmData = vmData;
        this.configDriveLabel = label;
        this.create = create;
        this.update = update;
        this.destStore = destStore;


        this.isoFile = isoFile;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public List<String[]> getVmData() {
        return vmData;
    }

    public void setVmData(List<String[]> vmData) {
        this.vmData = vmData;
    }

    public boolean isCreate() {
        return create;
    }

    public String getConfigDriveLabel() {
        return configDriveLabel;
    }

    public DataStoreTO getDestStore() {
        return destStore;
    }

    public String getIsoFile() {
        return isoFile;
    }

    public boolean isUpdate() {
        return update;
    }
}
