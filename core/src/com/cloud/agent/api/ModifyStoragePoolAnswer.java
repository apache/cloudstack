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

import java.util.Map;

import com.cloud.storage.template.TemplateProp;

public class ModifyStoragePoolAnswer extends Answer {
    StoragePoolInfo poolInfo;
    Map<String, TemplateProp> templateInfo;
    String localDatastoreName = null;

    protected ModifyStoragePoolAnswer() {
    }

    public ModifyStoragePoolAnswer(ModifyStoragePoolCommand cmd, long capacityBytes, long availableBytes, Map<String, TemplateProp> tInfo) {
        super(cmd);
        this.result = true;
        this.poolInfo =
            new StoragePoolInfo(null, cmd.getPool().getHost(), cmd.getPool().getPath(), cmd.getLocalPath(), cmd.getPool().getType(), capacityBytes, availableBytes);

        this.templateInfo = tInfo;
    }

    public StoragePoolInfo getPoolInfo() {
        return poolInfo;
    }

    public void setPoolInfo(StoragePoolInfo poolInfo) {
        this.poolInfo = poolInfo;
    }

    public Map<String, TemplateProp> getTemplateInfo() {
        return templateInfo;
    }

    public void setTemplateInfo(Map<String, TemplateProp> templateInfo) {
        this.templateInfo = templateInfo;
    }

    public String getLocalDatastoreName() {
        return localDatastoreName;
    }

    public void setLocalDatastoreName(String localDatastoreName) {
        this.localDatastoreName = localDatastoreName;
    }

}
