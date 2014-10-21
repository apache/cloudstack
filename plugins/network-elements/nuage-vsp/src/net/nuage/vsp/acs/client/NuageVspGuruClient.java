//
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
//

package net.nuage.vsp.acs.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface NuageVspGuruClient {

    public void implement(String networkDomainName, String networkDomainPath, String networkDomainUuid, String networkAccountName, String networkAccountUuid, String networkName,
            String networkCidr, String networkGateway, String networkUuid, boolean isL3Network, String vpcName, String vpcUuid, boolean defaultEgressPolicy,
            Collection<String> ipAddressRange) throws Exception;

    public List<Map<String, String>> reserve(String nicUuid, String nicMacAddress, String networkUuid, boolean isL3Network, String vpcUuid, String networkDomainUuid,
            String networksAccountUuid, boolean isDomainRouter, String domainRouterIp, String vmInstanceName, String vmUuid) throws Exception;

    public void release(String networkUuid, String vmUuid, String vmInstanceName) throws Exception;

    public void deallocate(String networkUuid, String nicFrmDdUuid, String nicMacAddress, String nicIp4Address, boolean isL3Network, String vpcUuid, String networksDomainUuid,
            String vmInstanceName, String vmUuid) throws Exception;

    public void trash(String domainUuid, String networkUuid, boolean isL3Network, String vpcUuid) throws Exception;

    public void setNuageVspApiClient(NuageVspApiClient nuageVspApiClient);

}
