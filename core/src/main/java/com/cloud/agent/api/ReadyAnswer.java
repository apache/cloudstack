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

package com.cloud.agent.api;

import java.util.Map;

public class ReadyAnswer extends Answer {

    private Map<String, String> detailsMap;

    protected ReadyAnswer() {
    }

    public ReadyAnswer(ReadyCommand cmd) {
        super(cmd, true, null);
    }

    public ReadyAnswer(ReadyCommand cmd, String details) {
        super(cmd, false, details);
    }

    public ReadyAnswer(ReadyCommand cmd, Map<String, String> detailsMap) {
        super(cmd, true, null);
        this.detailsMap = detailsMap;
    }

    public Map<String, String> getDetailsMap() {
        return detailsMap;
    }


}
