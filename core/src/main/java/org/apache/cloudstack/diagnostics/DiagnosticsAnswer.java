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

package org.apache.cloudstack.diagnostics;

import com.cloud.agent.api.Answer;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class DiagnosticsAnswer extends Answer {
    public static final Logger LOGGER = Logger.getLogger(DiagnosticsAnswer.class);

    public DiagnosticsAnswer(DiagnosticsCommand cmd, boolean result, String details) {
        super(cmd, result, details);
    }

    public Map<String, String> getExecutionDetails() {
        final Map<String, String> executionDetailsMap = new HashMap<>();
        if (result == true && StringUtils.isNotEmpty(details)) {
            final String[] parseDetails = details.split("&&");
            if (parseDetails.length >= 3) {
                executionDetailsMap.put(ApiConstants.STDOUT, parseDetails[0].trim());
                executionDetailsMap.put(ApiConstants.STDERR, parseDetails[1].trim());
                executionDetailsMap.put(ApiConstants.EXITCODE, String.valueOf(parseDetails[2]).trim());
            } else {
                throw new CloudRuntimeException("Unsupported diagnostics command type supplied");
            }
        } else {
            executionDetailsMap.put(ApiConstants.STDOUT, "");
            executionDetailsMap.put(ApiConstants.STDERR, details);
            executionDetailsMap.put(ApiConstants.EXITCODE, "-1");
        }
        return executionDetailsMap;
    }
}
