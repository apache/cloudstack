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

import java.util.List;

public class NsxHelper {

    public static CreateNsxDhcpRelayConfigCommand createNsxDhcpRelayConfigCommand(DomainVO domain, Account account, DataCenter zone, VpcVO vpc, Network network, List<String> addresses) {
        return new CreateNsxDhcpRelayConfigCommand(domain.getName(), account.getAccountName(), zone.getName(),
                vpc.getName(), network.getName(), addresses);
    }

    public static CreateNsxSegmentCommand createNsxSegmentCommand(DomainVO domain, Account account, DataCenter zone, String vpcName, NetworkVO networkVO) {
        return new CreateNsxSegmentCommand(domain.getName(), account.getAccountName(), zone.getName(),
                vpcName, networkVO.getName(), networkVO.getGateway(), networkVO.getCidr());
    }
}
