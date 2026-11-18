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
package org.apache.cloudstack.utils;

import com.cloud.dc.DataCenter;
import com.cloud.domain.DomainVO;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.user.Account;
import org.apache.cloudstack.agent.api.CreateNsxDhcpRelayConfigCommand;
import org.apache.cloudstack.agent.api.CreateNsxSegmentCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNsxTier1NatRuleCommand;

import java.util.List;

public class NsxHelper {

    private NsxHelper() {
    }

    public static CreateNsxDhcpRelayConfigCommand createNsxDhcpRelayConfigCommand(DomainVO domain, Account account, DataCenter zone, VpcVO vpc, Network network, List<String> addresses) {
        Long vpcId = vpc != null ? vpc.getId() : null;
        String vpcName = vpc != null ? vpc.getName() : null;
        return new CreateNsxDhcpRelayConfigCommand(domain.getId(), account.getId(), zone.getId(),
                vpcId, vpcName, network.getId(), network.getName(), addresses);
    }

    public static CreateNsxSegmentCommand createNsxSegmentCommand(DomainVO domain, Account account, DataCenter zone, String vpcName, NetworkVO networkVO) {
        return new CreateNsxSegmentCommand(domain.getId(), account.getId(), zone.getId(),
                networkVO.getVpcId(), vpcName, networkVO.getId(), networkVO.getName(), networkVO.getGateway(), networkVO.getCidr());
    }

    public static CreateOrUpdateNsxTier1NatRuleCommand createOrUpdateNsxNatRuleCommand(long domainId, long accountId, long zoneId,
                                                                                       String tier1Gateway, String action, String ipAddress,
                                                                                       String natRuleId) {
        return new CreateOrUpdateNsxTier1NatRuleCommand(domainId, accountId, zoneId, tier1Gateway, action, ipAddress, natRuleId);
    }
}
