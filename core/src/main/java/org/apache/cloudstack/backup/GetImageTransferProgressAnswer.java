//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.backup;

import java.util.Map;

import com.cloud.agent.api.Answer;

public class GetImageTransferProgressAnswer extends Answer {
    private Map<String, Long> progressMap; // transferId -> progress percentage (0-100)

    public GetImageTransferProgressAnswer() {
    }

    public GetImageTransferProgressAnswer(GetImageTransferProgressCommand cmd, boolean success, String details) {
        super(cmd, success, details);
    }

    public GetImageTransferProgressAnswer(GetImageTransferProgressCommand cmd, boolean success, String details,
                                         Map<String, Long> progressMap) {
        super(cmd, success, details);
        this.progressMap = progressMap;
    }

    public Map<String, Long> getProgressMap() {
        return progressMap;
    }

    public void setProgressMap(Map<String, Long> progressMap) {
        this.progressMap = progressMap;
    }
}
