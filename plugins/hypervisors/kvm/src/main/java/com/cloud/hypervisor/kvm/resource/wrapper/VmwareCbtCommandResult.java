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
package com.cloud.hypervisor.kvm.resource.wrapper;

import org.apache.commons.lang3.StringUtils;

class VmwareCbtCommandResult {

    private final int exitValue;
    private final String lastCommandOutput;

    VmwareCbtCommandResult(int exitValue, String lastCommandOutput) {
        this.exitValue = exitValue;
        this.lastCommandOutput = lastCommandOutput;
    }

    int getExitValue() {
        return exitValue;
    }

    String appendLastCommandOutput(String details) {
        if (StringUtils.isBlank(lastCommandOutput) || StringUtils.contains(details, lastCommandOutput)) {
            return details;
        }
        return String.format("%s Last command output: %s", details, lastCommandOutput);
    }
}
