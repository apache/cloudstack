// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
//  with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.server;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface ResourceTag extends ControlledEntity, Identity, InternalIdentity {

    public enum  TaggedResourceType {
        UserVm,
        Template,
        ISO,
        Volume,
        Snapshot,
        Network,
        LoadBalancer,
        PortForwardingRule,
        FirewallRule,
        SecurityGroup,
        PublicIpAddress,
        Project,
        Vpc,
        NetworkACL,
        StaticRoute,
        VMSnapshot
    }

    /**
     * @return
     */
    String getKey();

    /**
     * @return
     */
    String getValue();

    /**
     * @return
     */
    long getResourceId();

    /**
     * @return
     */
    TaggedResourceType getResourceType();

    /**
     * @return
     */
    String getCustomer();

    /**
     * @return
     */
    String getResourceUuid();

}
