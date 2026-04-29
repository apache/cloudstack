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

package com.cloud.agent.api.routing;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.LogLevel;
import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleMetricsValue;

import java.util.ArrayList;
import java.util.List;

@LogLevel(Log4jLevel.Debug)
public class GetAutoScaleMetricsAnswer extends Answer {
    private List<AutoScaleMetricsValue> values;

    public GetAutoScaleMetricsAnswer(Command cmd, boolean success) {
        super(cmd, success, null);
        this.values = new ArrayList<>();
    }

    public GetAutoScaleMetricsAnswer(Command cmd, boolean success, List<AutoScaleMetricsValue> values) {
        super(cmd, success, null);
        this.values = values;
    }


    public void addValue(AutoScaleMetricsValue value) {
        this.values.add(value);
    }

    public List<AutoScaleMetricsValue> getValues() {
        return values;
    }
}
