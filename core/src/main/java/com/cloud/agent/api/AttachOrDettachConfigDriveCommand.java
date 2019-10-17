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

public class AttachOrDettachConfigDriveCommand extends Command {

    String vmName;
    List<String[]> vmData;
    String configDriveLabel;
    boolean isAttach = false;

    public AttachOrDettachConfigDriveCommand(String vmName, List<String[]> vmData, String label, boolean attach) {
        this.vmName = vmName;
        this.vmData = vmData;
        this.configDriveLabel = label;
        this.isAttach = attach;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getVmName() {
        return vmName;
    }

    public List<String[]> getVmData() {
        return vmData;
    }

    public boolean isAttach() {
        return isAttach;
    }

    public String getConfigDriveLabel() {
        return configDriveLabel;
    }
}
