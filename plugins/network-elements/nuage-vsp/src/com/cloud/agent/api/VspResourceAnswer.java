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

public class VspResourceAnswer extends Answer {

    private String _resourceInfo;

    public VspResourceAnswer(Command cmd, String resourceInfo, String details) {
        super(cmd, true, details);
        this._resourceInfo = resourceInfo;
    }

    public VspResourceAnswer(VspResourceCommand cmd, boolean success, String details) {
        super(cmd, success, details);
    }

    public VspResourceAnswer(VspResourceCommand cmd, Exception e) {
        super(cmd, e);
    }

    public String getResourceInfo() {
        return this._resourceInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VspResourceAnswer that = (VspResourceAnswer) o;

        if (_resourceInfo != null ? !_resourceInfo.equals(that._resourceInfo) : that._resourceInfo != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return _resourceInfo != null ? _resourceInfo.hashCode() : 0;
    }
}
