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
package org.apache.cloudstack.network.tungsten.agent.api;

import net.juniper.tungsten.api.ApiObjectBase;

import java.util.Objects;

public class DeleteTungstenObjectCommand extends TungstenCommand {
    private final ApiObjectBase apiObjectBase;

    public DeleteTungstenObjectCommand(final ApiObjectBase apiObjectBase) {
        this.apiObjectBase = apiObjectBase;
    }

    public ApiObjectBase getApiObjectBase() {
        return apiObjectBase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DeleteTungstenObjectCommand that = (DeleteTungstenObjectCommand) o;
        return Objects.equals(apiObjectBase, that.apiObjectBase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), apiObjectBase);
    }
}
