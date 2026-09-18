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
import com.cloud.agent.api.to.NfsTO;

import java.util.HashSet;
import java.util.Set;

public class ExtractBackupFileCommand extends Command {

    private long volumeId;
    private String filePath;
    private String destinationPath;
    private String filesystem;
    private String backupPath;
    private NfsTO datastore;
    private Set<String> secondaryStorageUrls;
    private boolean cleanup;

    public ExtractBackupFileCommand(long volumeId, String filesystem, String filePath, String destinationPath, String backupPath, NfsTO datastore, Set<String> secondaryStorageUrls) {
        this.volumeId = volumeId;
        this.filePath = filePath;
        this.destinationPath = destinationPath;
        this.filesystem = filesystem;
        this.backupPath = backupPath;
        this.datastore = datastore;
        this.secondaryStorageUrls = secondaryStorageUrls;
    }

    public ExtractBackupFileCommand(String destinationPath, NfsTO datastore) {
        this.cleanup = true;
        this.destinationPath = destinationPath;
        this.datastore = datastore;
        this.secondaryStorageUrls = new HashSet<>();
    }

    public long getVolumeId() {
        return volumeId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public String getFilesystem() {
        return filesystem;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public NfsTO getDatastore() {
        return datastore;
    }

    public Set<String> getSecondaryStorageUrls() {
        return secondaryStorageUrls;
    }

    public boolean isCleanup() {
        return cleanup;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
