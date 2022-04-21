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
package com.cloud.agent.api;

import java.util.HashMap;
import java.util.Map;

public class PatchSystemVmCommand extends Command {
    boolean forced;
    HashMap<String, String> accessDetails = new HashMap<String, String>(0);

    public boolean isForced() {
        return forced;
    }

    public void setForced(boolean forced) {
        this.forced = forced;
    }

    public void setAccessDetail(final Map<String, String> details) {
        if (details == null) {
            return;
        }
        for (final Map.Entry<String, String> detail : details.entrySet()) {
            if (detail == null) {
                continue;
            }
            setAccessDetail(detail.getKey(), detail.getValue());
        }
    }

    public void setAccessDetail(final String name, final String value) {
        accessDetails.put(name, value);
    }

    public String getAccessDetail(final String name) {
        return accessDetails.get(name);
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
