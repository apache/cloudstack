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

import java.util.Objects;
import java.util.UUID;

public class CreateTungstenTagCommand extends TungstenCommand {
    private final String uuid;
    private final String tagType;
    private final String tagValue;

    public CreateTungstenTagCommand(final String tagType, final String tagValue) {
        this.uuid = UUID.randomUUID().toString();
        this.tagType = tagType;
        this.tagValue = tagValue;
    }

    public String getUuid() {
        return uuid;
    }

    public String getTagType() {
        return tagType;
    }

    public String getTagValue() {
        return tagValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CreateTungstenTagCommand that = (CreateTungstenTagCommand) o;
        return Objects.equals(uuid, that.uuid) && Objects.equals(tagType, that.tagType) && Objects.equals(tagValue, that.tagValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uuid, tagType, tagValue);
    }
}
