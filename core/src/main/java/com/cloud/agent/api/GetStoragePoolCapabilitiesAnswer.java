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
package com.cloud.agent.api;

import java.util.HashMap;
import java.util.Map;

public class GetStoragePoolCapabilitiesAnswer extends Answer {

    private Map<String, String> poolDetails;

    public GetStoragePoolCapabilitiesAnswer(GetStoragePoolCapabilitiesCommand cmd) {
        super(cmd);
        poolDetails = new HashMap<>();
    }

    public void setResult(boolean result){
        this.result = result;
    }

    public void setDetails(String details){
        this.details = details;
    }

    public Map<String, String> getPoolDetails() {
        return poolDetails;
    }

    public void setPoolDetails(Map<String, String> poolDetails) {
        this.poolDetails = poolDetails;
    }

}
