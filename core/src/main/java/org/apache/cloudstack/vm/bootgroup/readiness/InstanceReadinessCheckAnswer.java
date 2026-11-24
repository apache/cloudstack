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

package org.apache.cloudstack.vm.bootgroup.readiness;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.utils.exception.CloudRuntimeException;

public class InstanceReadinessCheckAnswer extends Answer {

    public static final String STDOUT = "stdout";
    public static final String STDERR = "stderr";
    public static final String EXITCODE = "exitcode";

    public InstanceReadinessCheckAnswer(InstanceReadinessCheckCommand cmd, boolean result, String details) {
        super(cmd, result, details);
    }

    public Map<String, String> getExecutionDetails() {
        final Map<String, String> executionDetails = new HashMap<>();
        final String details = getDetails();
        if (StringUtils.isNotEmpty(details) && details.contains("&&")) {
            final String[] parts = details.split("&&");
            if (parts.length >= 3) {
                executionDetails.put(STDOUT, parts[0].trim());
                executionDetails.put(STDERR, parts[1].trim());
                executionDetails.put(EXITCODE, parts[2].trim());
            } else {
                throw new CloudRuntimeException("Unsupported instance boot group readiness check output format");
            }
        } else {
            executionDetails.put(STDOUT, "");
            executionDetails.put(STDERR, details);
            executionDetails.put(EXITCODE, "-1");
        }
        return executionDetails;
    }
}
