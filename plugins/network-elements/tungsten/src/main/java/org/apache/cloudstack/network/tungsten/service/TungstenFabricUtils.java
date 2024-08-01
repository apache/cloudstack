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
package org.apache.cloudstack.network.tungsten.service;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class TungstenFabricUtils {

    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    AgentManager agentMgr;
    @Inject
    TungstenProviderDao tungstenProviderDao;

    public TungstenAnswer sendTungstenCommand(TungstenCommand cmd, long zoneId) throws IllegalArgumentException {

        TungstenProviderVO tungstenProviderVO = tungstenProviderDao.findByZoneId(zoneId);
        if (tungstenProviderVO == null) {
            logger.error("No Tungsten-Fabric provider have been found!");
            throw new InvalidParameterValueException("Failed to find a Tungsten-Fabric provider");
        }

        Answer answer = agentMgr.easySend(tungstenProviderVO.getHostId(), cmd);

        if (answer == null || !answer.getResult()) {
            logger.error("Tungsten-Fabric API Command failed");
            throw new InvalidParameterValueException("Failed API call to Tungsten-Fabric Network plugin");
        }

        return (TungstenAnswer) answer;
    }
}
