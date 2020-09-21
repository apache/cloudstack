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
package com.cloud.agent.api.to;

import com.cloud.agent.api.LogLevel;

import java.util.HashMap;
import java.util.Map;

public class DeployAsIsInfoTO {

    private String templatePath;
    private String destStoragePool;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private Map<String, String> properties = new HashMap<>();
    private Map<Integer, String> nicAdapterMap = new HashMap();

    public DeployAsIsInfoTO() {
    }

    public DeployAsIsInfoTO(String templatePath, String destStoragePool, Map<String, String> properties,
                            Map<Integer, String> nicAdapterMap) {
        this.templatePath = templatePath;
        this.destStoragePool = destStoragePool;
        this.properties = properties;
        this.nicAdapterMap = nicAdapterMap;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getDestStoragePool() {
        return destStoragePool;
    }

    public Map<Integer, String> getNicAdapterMap() {
        return nicAdapterMap;
    }
}
