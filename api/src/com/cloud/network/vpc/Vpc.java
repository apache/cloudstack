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
package com.cloud.network.vpc;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface Vpc extends ControlledEntity, Identity, InternalIdentity {

    public enum State {
        Enabled, Inactive
    }

    /**
     *
     * @return VPC name
     */
    String getName();

    /**
     * @return the id of the zone the VPC belongs to
     */
    long getZoneId();

    /**
     * @return super CIDR of the VPC. All the networks participating in VPC, should have CIDRs that are the part of the super cidr
     */
    String getCidr();

    /**
     *
     * @return VPC state
     */
    State getState();

    /**
     *
     * @return VPC offering id - the offering that VPC is created from
     */
    long getVpcOfferingId();

    /**
     *
     * @return VPC display text
     */
    String getDisplayText();

    /**
     *
     * @return VPC network domain. All networks participating in the VPC, become the part of the same network domain
     */
    String getNetworkDomain();

    /**
     *
     * @return true if restart is required for the VPC; false otherwise
     */
    boolean isRestartRequired();

    boolean isDisplay();

    boolean isRedundant();

    /**
     *
     * @return true if VPC is configured to use distributed router to provides one-hop forwarding and hypervisor based ACL
     */
    boolean usesDistributedRouter();

    /**
     *
     * @return true if VPC spans multiple zones in the region
     */
    boolean isRegionLevelVpc();
}
