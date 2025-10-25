/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.vnf;

import com.cloud.network.Network;
import com.cloud.user.Account;
import org.apache.cloudstack.api.response.CreateNetworkResponse;
import javax.inject.Inject;
import javax.ejb.Local;

@Local(value=VnfNetworkService.class)
public class VnfNetworkServiceImpl implements VnfNetworkService {

    @Inject private dao.VnfDictionaryDao dictDao;
    @Inject private dao.VnfNetworkBindingDao bindingDao;
    @Inject private dao.VnfRuleMapDao ruleDao;
    @Inject private VnfTemplateRenderer renderer;
    @Inject private VnfTransport transport;

    @Override
    public CreateNetworkResponse createVnfNetwork(org.apache.cloudstack.api.command.user.vnf.CreateVnfNetworkCmd cmd) {
        // TODO: create network record (like Isolated), deploy VR broker + VNF VM from template,
        // persist binding + optional dictionary, return response with IDs
        return new CreateNetworkResponse();
    }

    @Override
    public void uploadDictionary(long networkId, String name, String yaml, long ownerId) {
        // TODO: validate YAML placeholders; persist versioned dictionary
    }

    @Override
    public void attachVnfVm(long networkId, long vmId, long ownerId) {
        // TODO: bind a VM as the VNF; update egress allowlist for broker
    }

    @Override
    public org.apache.cloudstack.api.response.SuccessResponse getStatus(long networkId) {
        // TODO: query broker health + VNF reachability + dict version
        return new org.apache.cloudstack.api.response.SuccessResponse("getvnfnetworkstatus");
    }
}
