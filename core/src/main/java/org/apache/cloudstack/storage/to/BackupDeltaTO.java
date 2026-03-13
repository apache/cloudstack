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
package org.apache.cloudstack.storage.to;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class BackupDeltaTO implements DataTO {
    private DataStoreTO dataStoreTO;

    private Hypervisor.HypervisorType hypervisorType;

    private String path;

    private String screenshotPath;

    private Storage.ImageFormat format;

    // When set, represents the Backup ID, not the delta ID.
    private long id = 0;

    public BackupDeltaTO(DataStoreTO dataStoreTO, Hypervisor.HypervisorType hypervisorType, String path) {
        this.dataStoreTO = dataStoreTO;
        this.hypervisorType = hypervisorType;
        this.path = path;
        this.format = Storage.ImageFormat.QCOW2;
    }

    public BackupDeltaTO(long id, DataStoreTO dataStoreTO, Hypervisor.HypervisorType hypervisorType, String path) {
        this(dataStoreTO, hypervisorType, path);
        this.id = id;
    }

    @Override
    public DataObjectType getObjectType() {
        return DataObjectType.BACKUP;
    }

    @Override
    public DataStoreTO getDataStore() {
        return dataStoreTO;
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Storage.ImageFormat getFormat() {
        return this.format;
    }

    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, ToStringStyle.JSON_STYLE).setExcludeFieldNames("id").toString();
    }
}
