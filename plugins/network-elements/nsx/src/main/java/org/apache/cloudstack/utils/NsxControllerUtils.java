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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.NsxProviderDao;
import com.cloud.network.element.NsxProviderVO;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.NsxCommand;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static java.util.Objects.isNull;

@Component
public class NsxControllerUtils {
    private static final Logger s_logger = Logger.getLogger(NsxControllerUtils.class);

    @Inject
    AgentManager agentMgr;
    @Inject
    NsxProviderDao nsxProviderDao;

    public NsxAnswer sendNsxCommand(NsxCommand cmd, long zoneId) throws IllegalArgumentException {

        NsxProviderVO nsxProviderVO = nsxProviderDao.findByZoneId(zoneId);
        if (nsxProviderVO == null) {
            s_logger.error("No NSX controller was found!");
            throw new InvalidParameterValueException("Failed to find an NSX controller");
        }
        Answer answer = agentMgr.easySend(nsxProviderVO.getHostId(), cmd);

        if (answer == null || !answer.getResult()) {
            s_logger.error("NSX API Command failed");
            throw new InvalidParameterValueException("Failed API call to NSX controller");
        }

        return (NsxAnswer) answer;
    }

    public static String getTier1GatewayName(long domainId, long accountId, long zoneId, long vpcId) {
        return String.format("D%s-A%s-Z%s-V%s",  domainId, accountId, zoneId, vpcId);
    }

    public static String getNsxSegmentId(long domainId, long accountId, long zoneId, Long vpcId, long networkId) {
        String segmentName = String.format("D%s-A%s-Z%s",  domainId, accountId, zoneId);
        if (isNull(vpcId)) {
            return String.format("%s-S%s", segmentName, networkId);
        }
        return String.format("%s-V%s-S%s",segmentName, vpcId, networkId);
    }

    public static String getNsxDhcpRelayConfigId(long zoneId, long domainId, long accountId, Long vpcId, long networkId) {
        String suffix = "Relay";
        if (isNull(vpcId)) {
            return String.format("D%s-A%s-Z%s-S%s-%s", domainId, accountId, zoneId, networkId, suffix);
        }
        return String.format("D%s-A%s-Z%s-V%s-S%s-%s", domainId, accountId, zoneId, vpcId, networkId, suffix);
    }

    public static String getStaticNatRuleName(long zoneId, long domainId, long accountId, Long vpcId) {
        String suffix = "-STATICNAT";
       return getTier1GatewayName(domainId, accountId, zoneId, vpcId) + suffix;
    }
}
