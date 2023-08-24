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
package org.apache.cloudstack.service;

import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;

import javax.inject.Inject;

public class NsxServiceImpl implements NsxService {
    @Inject
    private NsxControllerUtils nsxControllerUtils;
    public boolean createVpcNetwork(Long zoneId, String zoneName, Long accountId, String accountName, String vpcName) {
        CreateNsxTier1GatewayCommand createNsxTier1GatewayCommand =
                new CreateNsxTier1GatewayCommand(zoneName, zoneId, accountName, accountId, vpcName);
        NsxAnswer result = nsxControllerUtils.sendNsxCommand(createNsxTier1GatewayCommand, zoneId);
        return result.getResult();
    }
}
