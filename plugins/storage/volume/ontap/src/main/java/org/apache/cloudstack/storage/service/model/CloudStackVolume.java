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

package org.apache.cloudstack.storage.service.model;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.storage.feign.model.FileInfo;
import org.apache.cloudstack.storage.feign.model.Lun;

public class CloudStackVolume {

    private FileInfo file;
    private Lun lun;
    private String datastoreId;
    private DataObject volumeInfo; // This is needed as we need DataObject to be passed to agent to create volume
    public FileInfo getFile() {
        return file;
    }

    public void setFile(FileInfo file) {
        this.file = file;
    }

    public Lun getLun() {
        return lun;
    }

    public void setLun(Lun lun) {
        this.lun = lun;
    }
    public String getDatastoreId() {
        return datastoreId;
    }
    public void setDatastoreId(String datastoreId) {
        this.datastoreId = datastoreId;
    }
    public DataObject getVolumeInfo() {
        return volumeInfo;
    }
    public void setVolumeInfo(DataObject volumeInfo) {
        this.volumeInfo = volumeInfo;
    }
}
