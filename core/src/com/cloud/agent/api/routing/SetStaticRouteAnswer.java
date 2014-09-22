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

package com.cloud.agent.api.routing;

import java.util.Arrays;

import com.cloud.agent.api.Answer;

public class SetStaticRouteAnswer extends Answer {
    String[] results;

    protected SetStaticRouteAnswer() {
    }

    public SetStaticRouteAnswer(SetStaticRouteCommand cmd, boolean success, String[] results) {
        super(cmd, success, null);
        if (results != null) {
            assert (cmd.getStaticRoutes().length == results.length) : "Static routes and their results should be the same length";
            this.results = Arrays.copyOf(results, results.length);
        }
    }

    public String[] getResults() {
        if (results != null) {
            return Arrays.copyOf(results, results.length);
        }
        return null;
    }
}
