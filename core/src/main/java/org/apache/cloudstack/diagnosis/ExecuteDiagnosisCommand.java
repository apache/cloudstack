/*
 * // Licensed to the Apache Software Foundation (ASF) under one
 * // or more contributor license agreements.  See the NOTICE file
 * // distributed with this work for additional information
 * // regarding copyright ownership.  The ASF licenses this file
 * // to you under the Apache License, Version 2.0 (the
 * // "License"); you may not use this file except in compliance
 * // with the License.  You may obtain a copy of the License at
 * //
 * //   http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing,
 * // software distributed under the License is distributed on an
 * // "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * // KIND, either express or implied.  See the License for the
 * // specific language governing permissions and limitations
 * // under the License.
 */

package org.apache.cloudstack.diagnosis;

import com.cloud.agent.api.routing.NetworkElementCommand;

public class ExecuteDiagnosisCommand extends NetworkElementCommand {
    private final Long ssvmId;
    private final String pingDestination ;
    private final String diagnosisType;

    public ExecuteDiagnosisCommand(Long ssvmId, String pingDestination, String diagnosisType) {
        this.ssvmId = ssvmId;
        this.pingDestination = pingDestination;
        this.diagnosisType = diagnosisType;
    }

    public String getArgs(){
        return ssvmId + " " + pingDestination + " " + diagnosisType;
    }

    @Override
    public boolean isQuery() {
        return true;
    }
}
