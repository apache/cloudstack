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
import java.util.concurrent.ExecutionException;

public interface NuageVspElementClient {

    boolean implement(long networkId, String networkDomainUuid, String networkUuid, String networkName, String vpcOrSubnetUuid, boolean isL2Network, boolean isL3Network,
                             boolean isVpc, boolean isShared, String domainTemplateName, boolean isFirewallServiceSupported, List<String> dnsServers, List<Map<String, Object>> ingressFirewallRules,
                             List<Map<String, Object>> egressFirewallRules, List<String> acsFipUuid, boolean egressDefaultPolicy) throws ExecutionException;

    void applyStaticNats(String networkDomainUuid, String networkUuid, String vpcOrSubnetUuid, boolean isL3Network, boolean isVpc,
                                List<Map<String, Object>> staticNatDetails) throws ExecutionException;

    void applyAclRules(boolean isNetworkAcl, String networkUuid, String networkDomainUuid, String vpcOrSubnetUuid, String networkName, boolean isL2Network,
                              List<Map<String, Object>> rules, long networkId, boolean egressDefaultPolicy, Boolean isAcsIngressAcl, boolean networkReset, String domainTemplateName) throws ExecutionException;

    void shutdownVpc(String domainUuid, String vpcUuid, String domainTemplateName) throws ExecutionException;

    <C extends NuageVspApiClient> void setNuageVspApiClient(NuageVspApiClient nuageVspApiClient);

}
