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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public interface ResourceTag extends ControlledEntity, Identity, InternalIdentity {

    // FIXME - extract enum to another interface as its used both by resourceTags and resourceMetaData code
    public enum ResourceObjectType {
        UserVm(true, true, true),
        Template(true, true, true),
        ISO(true, false, true),
        Volume(true, true),
        Snapshot(true, false),
        Backup(true, false),
        Network(true, true, true),
        Nic(false, true),
        LoadBalancer(true, true),
        PortForwardingRule(true, true),
        FirewallRule(true, true),
        SecurityGroup(true, false),
        SecurityGroupRule(true, false),
        PublicIpAddress(true, true),
        Project(true, false, true),
        Account(true, false, true),
        Vpc(true, true, true),
        NetworkACL(true, true),
        StaticRoute(true, false),
        VMSnapshot(true, false),
        RemoteAccessVpn(true, true),
        Zone(false, true, true),
        ServiceOffering(false, true),
        Storage(false, true),
        PrivateGateway(false, true),
        NetworkACLList(false, true),
        VpnGateway(false, true),
        CustomerGateway(false, true),
        VpnConnection(false, true),
        User(true, true, true),
        DiskOffering(false, true),
        AutoScaleVmProfile(false, true),
        AutoScaleVmGroup(false, true),
        LBStickinessPolicy(false, true),
        LBHealthCheckPolicy(false, true),
        SnapshotPolicy(true, true),
        GuestOs(false, true),
        NetworkOffering(false, true),
        VpcOffering(true, false),
        Domain(false, false, true);


        ResourceObjectType(boolean resourceTagsSupport, boolean resourceMetadataSupport) {
            this.resourceTagsSupport = resourceTagsSupport;
            metadataSupport = resourceMetadataSupport;
        }

        ResourceObjectType(boolean resourceTagsSupport, boolean resourceMetadataSupport, boolean resourceIconSupport) {
            this(resourceTagsSupport, resourceMetadataSupport);
            this.resourceIconSupport = resourceIconSupport;
        }

        private final boolean resourceTagsSupport;
        private final boolean metadataSupport;
        private boolean resourceIconSupport;
        private static final Map<String, ResourceObjectType> resourceObjectTypeMap = new HashMap<>();

        public boolean resourceTagsSupport() {
            return resourceTagsSupport;
        }

        public boolean resourceMetadataSupport() {
            return metadataSupport;
        }

        public boolean resourceIconSupport() {
            return resourceIconSupport;
        }

        static {
            resourceObjectTypeMap.put("uservm", UserVm);
            resourceObjectTypeMap.put("template", Template);
            resourceObjectTypeMap.put("iso", ISO);
            resourceObjectTypeMap.put("volume", Volume);
            resourceObjectTypeMap.put("snapshot", Snapshot);
            resourceObjectTypeMap.put("backup", Backup);
            resourceObjectTypeMap.put("network", Network);
            resourceObjectTypeMap.put("nic", Nic);
            resourceObjectTypeMap.put("loadbalancer", LoadBalancer);
            resourceObjectTypeMap.put("portforwardingrule", PortForwardingRule);
            resourceObjectTypeMap.put("firewallrule", FirewallRule);
            resourceObjectTypeMap.put("securitygroup", SecurityGroup);
            resourceObjectTypeMap.put("securitygrouprule", SecurityGroupRule);
            resourceObjectTypeMap.put("publicipaddress", PublicIpAddress);
            resourceObjectTypeMap.put("project", Project);
            resourceObjectTypeMap.put("account", Account);
            resourceObjectTypeMap.put("vpc", Vpc);
            resourceObjectTypeMap.put("networkacl", NetworkACL);
            resourceObjectTypeMap.put("staticroute", StaticRoute);
            resourceObjectTypeMap.put("vmsnapshot", VMSnapshot);
            resourceObjectTypeMap.put("remoteaccessvpn", RemoteAccessVpn);
            resourceObjectTypeMap.put("zone", Zone);
            resourceObjectTypeMap.put("serviceoffering", ServiceOffering);
            resourceObjectTypeMap.put("storage", Storage);
            resourceObjectTypeMap.put("privategateway", PrivateGateway);
            resourceObjectTypeMap.put("networkacllist", NetworkACLList);
            resourceObjectTypeMap.put("vpngateway", VpnGateway);
            resourceObjectTypeMap.put("customergateway", CustomerGateway);
            resourceObjectTypeMap.put("vpnconnection", VpnConnection);
            resourceObjectTypeMap.put("user", User);
            resourceObjectTypeMap.put("diskoffering", DiskOffering);
            resourceObjectTypeMap.put("autoscalevmgroup", AutoScaleVmGroup);
            resourceObjectTypeMap.put("autoscalevmprofile", AutoScaleVmProfile);
            resourceObjectTypeMap.put("lbstickinesspolicy", LBStickinessPolicy);
            resourceObjectTypeMap.put("lbhealthcheckpolicy", LBHealthCheckPolicy);
            resourceObjectTypeMap.put("snapshotpolicy", SnapshotPolicy);
            resourceObjectTypeMap.put("guestos", GuestOs);
            resourceObjectTypeMap.put("networkoffering", NetworkOffering);
            resourceObjectTypeMap.put("vpcoffering", VpcOffering);
            resourceObjectTypeMap.put("domain", Domain);
        }

        public static ResourceObjectType getResourceObjectType(String type) {
            return resourceObjectTypeMap.getOrDefault(type.toLowerCase(Locale.ROOT), null);
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
