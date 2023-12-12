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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimeraCpgSAUsage {
    private int rawTotalMiB;
    private int rawUsedMiB;
    private int totalMiB;
    private int usedMiB;
    public int getRawTotalMiB() {
        return rawTotalMiB;
    }
    public void setRawTotalMiB(int rawTotalMiB) {
        this.rawTotalMiB = rawTotalMiB;
    }
    public int getRawUsedMiB() {
        return rawUsedMiB;
    }
    public void setRawUsedMiB(int rawUsedMiB) {
        this.rawUsedMiB = rawUsedMiB;
    }
    public int getTotalMiB() {
        return totalMiB;
    }
    public void setTotalMiB(int totalMiB) {
        this.totalMiB = totalMiB;
    }
    public int getUsedMiB() {
        return usedMiB;
    }
    public void setUsedMiB(int usedMiB) {
        this.usedMiB = usedMiB;
    }

}
