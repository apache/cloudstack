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

import com.cloud.utils.Pair;

public interface VpcProvisioningService {

    public VpcOffering getVpcOffering(long vpcOfferingId);

    public VpcOffering createVpcOffering(String name, String displayText, List<String> supportedServices,
                                         Map<String, List<String>> serviceProviders,
                                         Map serviceCapabilitystList,
                                         Long serviceOfferingId);

    Pair<List<? extends VpcOffering>,Integer> listVpcOfferings(Long id, String name, String displayText, List<String> supportedServicesStr, Boolean isDefault, String keyword,
        String state, Long startIndex, Long pageSizeVal);

    /**
     * @param offId
     * @return
     */
    public boolean deleteVpcOffering(long offId);

    /**
     * @param vpcOffId
     * @param vpcOfferingName
     * @param displayText
     * @param state
     * @return
     */
    public VpcOffering updateVpcOffering(long vpcOffId, String vpcOfferingName, String displayText, String state);

}
