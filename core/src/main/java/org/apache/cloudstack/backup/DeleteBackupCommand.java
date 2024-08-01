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

public class DeleteBackupCommand extends Command {
    private String backupPath;
    private String backupStoragePath;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private Map<String, String> details;

    public DeleteBackupCommand(String backupPath, String backupStoragePath, Map<String, String> details) {
        super();
        this.backupPath = backupPath;
        this.backupStoragePath = backupStoragePath;
        this.details = details;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
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
