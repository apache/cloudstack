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
package com.cloud.agent.api.storage;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataTO;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public final class CreateDatadiskTemplateCommand extends Command {
    private DataTO dataDiskTemplate;
    private String path;
    private long fileSize;
    private boolean bootable;
    private String diskId;

    public CreateDatadiskTemplateCommand(DataTO dataDiskTemplate, String path, String diskId, long fileSize, boolean bootable) {
        super();
        this.dataDiskTemplate = dataDiskTemplate;
        this.path = path;
        this.fileSize = fileSize;
        this.bootable = bootable;
        this.diskId = diskId;
    }

    protected CreateDatadiskTemplateCommand() {
        super();
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public DataTO getDataDiskTemplate() {
        return dataDiskTemplate;
    }

    public String getPath() {
        return path;
    }

    public long getFileSize() {
        return fileSize;
    }

    public boolean getBootable() {
        return bootable;
    }

    public String getDiskId() {
        return diskId;
    }

    public void setDiskId(String diskId) {
        this.diskId = diskId;
    }

    public String stringRepresentation() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SIMPLE_STYLE);
    }

}
