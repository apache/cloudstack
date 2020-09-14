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
package com.cloud.agent.api.to.deployasis;

import com.cloud.agent.api.LogLevel;

/**
 * End-user licence agreement
 */
public class OVFEulaSectionTO implements TemplateDeployAsIsInformationTO {
    private String info;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private byte[] compressedLicense;
    private int index;

    public OVFEulaSectionTO(String info, byte[] license, int eulaIndex) {
        this.info = info;
        this.compressedLicense = license;
        this.index = eulaIndex;
    }

    public String getInfo() {
        return this.info;
    }

    public byte[] getCompressedLicense() {
        return this.compressedLicense;
    }

    public int getIndex() {
        return index;
    }
}
