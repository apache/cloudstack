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

import java.util.Date;

import com.cloud.offering.NetworkOffering;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface VpcOffering extends InternalIdentity, Identity {
    public enum State {
        Disabled, Enabled
    }

    public static final String defaultVPCOfferingName = "Default VPC offering";
    public static final String defaultVPCNSOfferingName = "Default VPC  offering with Netscaler";
    public static final String redundantVPCOfferingName = "Redundant VPC offering";
    public static final String DEFAULT_VPC_NAT_NSX_OFFERING_NAME = "VPC offering with NSX - NAT Mode";
    public static final String DEFAULT_VPC_ROUTE_NSX_OFFERING_NAME = "VPC offering with NSX - Route Mode";

    /**
     *
     * @return VPC offering name
     */
    String getName();

    /**
     * @return VPC offering display text
     */
    String getDisplayText();

    /**
     *
     * @return VPC offering state
     */
    State getState();

    /**
     *
     * @return true if offering is default - came with the cloudStack fresh install; false otherwise
     */
    boolean isDefault();

    boolean isForNsx();

    NetworkOffering.NetworkMode getNetworkMode();

    /**
     * @return service offering id used by VPC virtual router
     */
    Long getServiceOfferingId();

    /**
     * @return true if the offering provides a distributed router capable of one-hop forwarding
     */
    boolean isSupportsDistributedRouter();

    /**
     * @return true if VPC created with the offering can span multiple zones in the region
     */
    boolean isOffersRegionLevelVPC();

    boolean isRedundantRouter();

    Date getRemoved();

    Date getCreated();

    NetworkOffering.RoutingMode getRoutingMode();

    Boolean isSpecifyAsNumber();
}
