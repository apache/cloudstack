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

import java.util.List;
import java.util.Map;

public interface NuageVspElementClient {

    public void applyStaticNats(String networkDomainUuid, String vpcOrSubnetUuid, boolean isL3Network, List<Map<String, Object>> staticNatDetails) throws Exception;

    public void applyAclRules(String networkUuid, String networkDomainUuid, String vpcOrSubnetUuid, boolean isL3Network, List<Map<String, Object>> aclRules, boolean isVpc, long networkId)
            throws Exception;

    public void shutDownVpc(String domainUuid, String vpcUuid) throws Exception;

    public <C extends NuageVspApiClient> void setNuageVspApiClient(NuageVspApiClient nuageVspApiClient);

}
