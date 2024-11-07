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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetrisProviderDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.NetrisProviderVO;
import com.cloud.network.netris.NetrisService;
import com.cloud.network.vpc.Vpc;
import org.apache.cloudstack.agent.api.CreateNetrisVnetCommand;
import org.apache.cloudstack.agent.api.CreateNetrisVpcCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVnetCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVpcCommand;
import org.apache.cloudstack.agent.api.NetrisAnswer;
import org.apache.cloudstack.agent.api.NetrisCommand;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.Objects;

public class NetrisServiceImpl implements NetrisService, Configurable {

    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private NetrisProviderDao netrisProviderDao;
    @Inject
    private NetworkDao networkDao;
    @Inject
    private AgentManager agentMgr;

    @Override
    public String getConfigComponentName() {
        return NetrisService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[0];
    }

    private NetrisProviderVO getZoneNetrisProvider(long zoneId) {
        NetrisProviderVO netrisProviderVO = netrisProviderDao.findByZoneId(zoneId);
        if (netrisProviderVO == null) {
            logger.error("No Netris controller was found!");
            throw new InvalidParameterValueException("Failed to find a Netris controller");
        }
        return netrisProviderVO;
    }

    private NetrisAnswer sendNetrisCommand(NetrisCommand cmd, long zoneId) {
        NetrisProviderVO netrisProviderVO = getZoneNetrisProvider(zoneId);
        Answer answer = agentMgr.easySend(netrisProviderVO.getHostId(), cmd);
        if (answer == null || !answer.getResult()) {
            logger.error("Netris API Command failed");
            throw new InvalidParameterValueException("Failed API call to Netris controller");
        }
        return (NetrisAnswer) answer;
    }

    @Override
    public boolean createVpcResource(long zoneId, long accountId, long domainId, Long vpcId, String vpcName, boolean sourceNatEnabled, String cidr, boolean isVpc) {
        CreateNetrisVpcCommand cmd = new CreateNetrisVpcCommand(zoneId, accountId, domainId, vpcName, cidr, vpcId, isVpc);
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean deleteVpcResource(long zoneId, long accountId, long domainId, Vpc vpc) {
        DeleteNetrisVpcCommand cmd = new DeleteNetrisVpcCommand(zoneId, accountId, domainId, vpc.getName(), vpc.getCidr(), vpc.getId(), true);
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean createVnetResource(Long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, String cidr) {
        NetworkVO network = networkDao.findById(networkId);
        String vxlanId = Networks.BroadcastDomainType.getValue(network.getBroadcastUri());
        CreateNetrisVnetCommand cmd = new CreateNetrisVnetCommand(zoneId, accountId, domainId, vpcName, vpcId, networkName, networkId, cidr, !Objects.isNull(vpcId));
        cmd.setVxlanId(Integer.parseInt(vxlanId));
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }

    @Override
    public boolean deleteVnetResource(long zoneId, long accountId, long domainId, String vpcName, Long vpcId, String networkName, Long networkId, String cidr) {
        DeleteNetrisVnetCommand cmd = new DeleteNetrisVnetCommand(zoneId, accountId, domainId, networkName, networkId, vpcName, vpcId, cidr, Objects.nonNull(vpcName));
        NetrisAnswer answer = sendNetrisCommand(cmd, zoneId);
        return answer.getResult();
    }
}
