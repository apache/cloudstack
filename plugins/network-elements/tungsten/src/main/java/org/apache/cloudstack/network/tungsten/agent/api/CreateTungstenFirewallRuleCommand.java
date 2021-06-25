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

import java.util.UUID;

public class CreateTungstenFirewallRuleCommand extends TungstenCommand {
    private final String uuid;
    private final String name;
    private final String action;
    private final String serviceGroupUuid;
    private final String srcTagUuid;
    private final String srcAddressGroupUuid;
    private final String direction;
    private final String destTagUuid;
    private final String destAddressGroupUuid;
    private final String tagTypeUuid;

    public CreateTungstenFirewallRuleCommand(final String name, final String action, final String serviceGroupUuid,
        final String srcTagUuid, final String srcAddressGroupUuid, final String direction, final String destTagUuid,
        final String destAddressGroupUuid, final String tagTypeUuid) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.action = action;
        this.serviceGroupUuid = serviceGroupUuid;
        this.srcTagUuid = srcTagUuid;
        this.srcAddressGroupUuid = srcAddressGroupUuid;
        this.direction = direction;
        this.destTagUuid = destTagUuid;
        this.destAddressGroupUuid = destAddressGroupUuid;
        this.tagTypeUuid = tagTypeUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getAction() {
        return action;
    }

    public String getServiceGroupUuid() {
        return serviceGroupUuid;
    }

    public String getSrcAddressGroupUuid() {
        return srcAddressGroupUuid;
    }

    public String getDirection() {
        return direction;
    }

    public String getDestAddressGroupUuid() {
        return destAddressGroupUuid;
    }

    public String getTagTypeUuid() {
        return tagTypeUuid;
    }

    public String getSrcTagUuid() {
        return srcTagUuid;
    }

    public String getDestTagUuid() {
        return destTagUuid;
    }
}
