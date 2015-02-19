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

    // FIXME - extract enum to another interface as its used both by resourceTags and resourceMetaData code
    public enum ResourceObjectType {
        UserVm(true, true),
        Template(true, true),
        ISO(true, false),
        Volume(true, true),
        Snapshot(true, false),
        Network(true, true),
        Nic(false, true),
        LoadBalancer(true, true),
        PortForwardingRule(true, true),
        FirewallRule(true, true),
        SecurityGroup(true, false),
        SecurityGroupRule(true, false),
        PublicIpAddress(true, true),
        Project(true, false),
        Vpc(true, true),
        NetworkACL(true, true),
        StaticRoute(true, false),
        VMSnapshot(true, false),
        RemoteAccessVpn(true, true),
        Zone(false, true),
        ServiceOffering(false, true),
        Storage(false, true),
        PrivateGateway(false, true),
        NetworkACLList(false, true),
        VpnGateway(false, true),
        CustomerGateway(false, true),
        VpnConnection(false, true),
        User(true, true),
        DiskOffering(false, true),
        AutoScaleVmProfile(false, true),
        AutoScaleVmGroup(false, true),
        LBStickinessPolicy(false, true),
        LBHealthCheckPolicy(false, true),
        SnapshotPolicy(false, true);


        ResourceObjectType(boolean resourceTagsSupport, boolean resourceMetadataSupport) {
            this.resourceTagsSupport = resourceTagsSupport;
            metadataSupport = resourceMetadataSupport;
        }

        private final boolean resourceTagsSupport;
        private final boolean metadataSupport;

        public boolean resourceTagsSupport() {
            return resourceTagsSupport;
        }

        public boolean resourceMetadataSupport() {
            return metadataSupport;
        }
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

    void setResourceId(long resourceId);

    /**
     * @return
     */
    ResourceObjectType getResourceType();

    /**
     * @return
     */
    String getCustomer();

    /**
     * @return
     */
    String getResourceUuid();

}
