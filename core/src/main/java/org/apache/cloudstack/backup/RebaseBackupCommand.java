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

/**
 * Tells the KVM agent to rebase a NAS backup qcow2 onto a new backing parent. Used by the
 * NAS backup provider during chain repair when a middle incremental is being deleted: the
 * immediate child must absorb the soon-to-be-deleted parent's blocks and then re-link to
 * the grandparent. Both target and new-backing paths are NAS-mount-relative.
 */
public class RebaseBackupCommand extends Command {
    private String targetPath;       // mount-relative path of the qcow2 to repoint
    private String newBackingPath;   // mount-relative path of the new backing parent
    private String backupRepoType;
    private String backupRepoAddress;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private String mountOptions;

    public RebaseBackupCommand(String targetPath, String newBackingPath,
                               String backupRepoType, String backupRepoAddress, String mountOptions) {
        super();
        this.targetPath = targetPath;
        this.newBackingPath = newBackingPath;
        this.backupRepoType = backupRepoType;
        this.backupRepoAddress = backupRepoAddress;
        this.mountOptions = mountOptions;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getNewBackingPath() {
        return newBackingPath;
    }

    public String getBackupRepoType() {
        return backupRepoType;
    }

    public String getBackupRepoAddress() {
        return backupRepoAddress;
    }

    public String getMountOptions() {
        return mountOptions == null ? "" : mountOptions;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
