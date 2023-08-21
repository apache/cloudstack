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
package org.apache.cloudstack.storage.datastore.adapter.primera;

/**
 * https://support.hpe.com/hpesc/public/docDisplay?docId=a00118636en_us&page=v24885490.html
 */
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimeraVolumeCopyRequest {
    private String action = "createPhysicalCopy";
    private PrimeraVolumeCopyRequestParameters parameters;
    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public PrimeraVolumeCopyRequestParameters getParameters() {
        return parameters;
    }
    public void setParameters(PrimeraVolumeCopyRequestParameters parameters) {
        this.parameters = parameters;
    }

}