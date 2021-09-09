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


import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.command.admin.vpc.CreateVPCOfferingCmd;
import org.apache.cloudstack.api.command.admin.vpc.UpdateVPCOfferingCmd;
import org.apache.cloudstack.api.command.user.vpc.ListVPCOfferingsCmd;

import com.cloud.utils.Pair;

public interface VpcProvisioningService {

    VpcOffering getVpcOffering(long vpcOfferingId);

    VpcOffering createVpcOffering(CreateVPCOfferingCmd cmd);

    VpcOffering createVpcOffering(String name, String displayText, List<String> supportedServices,
                                  Map<String, List<String>> serviceProviders,
                                  Map serviceCapabilitystList,
                                  Long serviceOfferingId, List<Long> domainIds, List<Long> zoneIds, VpcOffering.State state);

    Pair<List<? extends VpcOffering>,Integer> listVpcOfferings(ListVPCOfferingsCmd cmd);

    /**
     * @param offId
     * @return
     */
    public boolean deleteVpcOffering(long offId);

    @Deprecated
    public VpcOffering updateVpcOffering(long vpcOffId, String vpcOfferingName, String displayText, String state);

    /**
     * @param cmd
     * @return
     */
    VpcOffering updateVpcOffering(final UpdateVPCOfferingCmd cmd);

    /**
     * Retrieve ID of domains for a VPC offering
     *
     * @param vpcOfferingId
     */
    List<Long> getVpcOfferingDomains(Long vpcOfferingId);

    /**
     * Retrieve ID of domains for a VPC offering
     *
     * @param vpcOfferingId
     */
    List<Long> getVpcOfferingZones(Long vpcOfferingId);
}
