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

package org.apache.cloudstack.backup;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.LogLevel;

import java.util.Map;

public class TakeBackupCommand extends Command {
    private String vmName;
    private String backupStoragePath;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private Map<String, String> details;

    public TakeBackupCommand(String vmName, String backupStoragePath, Map<String, String> details) {
        super();
        this.vmName = vmName;
        this.backupStoragePath = backupStoragePath;
        this.details = details;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getBackupStoragePath() {
        return backupStoragePath;
    }

    public void setBackupStoragePath(String backupStoragePath) {
        this.backupStoragePath = backupStoragePath;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
