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

package com.cloud.agent.api.storage;

import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

public class StorPoolSetVolumeEncryptionCommand extends StorageSubSystemCommand {
    private boolean isDataDisk;
    private VolumeObjectTO volumeObjectTO;
    private String srcVolumeName;

    public StorPoolSetVolumeEncryptionCommand(VolumeObjectTO volumeObjectTO, String srcVolumeName,
            boolean isDataDisk) {
        this.volumeObjectTO = volumeObjectTO;
        this.srcVolumeName = srcVolumeName;
        this.isDataDisk = isDataDisk;
    }

    public VolumeObjectTO getVolumeObjectTO() {
        return volumeObjectTO;
    }

    public void setVolumeObjectTO(VolumeObjectTO volumeObjectTO) {
        this.volumeObjectTO = volumeObjectTO;
    }

    public void setIsDataDisk(boolean isDataDisk) {
        this.isDataDisk = isDataDisk;
    }

    public boolean isDataDisk() {
        return isDataDisk;
    }

    public String getSrcVolumeName() {
        return srcVolumeName;
    }

    public void setSrcVolumeName(String srcVolumeName) {
        this.srcVolumeName = srcVolumeName;
    }

    @Override
    public void setExecuteInSequence(boolean inSeq) {
        inSeq = false;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
