/*
 * // Licensed to the Apache Software Foundation (ASF) under one
 * // or more contributor license agreements.  See the NOTICE file
 * // distributed with this work for additional information
 * // regarding copyright ownership.  The ASF licenses this file
 * // to you under the Apache License, Version 2.0 (the
 * // "License"); you may not use this file except in compliance
 * // with the License.  You may obtain a copy of the License at
 * //
 * //   http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing,
 * // software distributed under the License is distributed on an
 * // "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * // KIND, either express or implied.  See the License for the
 * // specific language governing permissions and limitations
 * // under the License.
 */

package org.apache.cloudstack.diagnosis;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.router.RouterControlHelper;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.dao.DomainRouterDao;
import org.apache.cloudstack.api.command.admin.diagnosis.RemoteDiagnosisCmd;
import org.apache.cloudstack.api.response.RemoteDiagnosisResponse;
import org.apache.cloudstack.diangosis.RemoteDiagnosisService;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class RemoteDiagnosisServiceImpl extends ManagerBase implements PluggableService, RemoteDiagnosisService {
    private static final Logger s_logger = Logger.getLogger(RemoteDiagnosisServiceImpl.class);

    @Inject
    private DomainRouterDao domainRouterDao;
    @Inject
    private HostDao hostDao;
    @Inject
    RouterControlHelper routerControlHelper;
    @Inject
    private AgentManager agentManager;

    @Override
    public RemoteDiagnosisResponse pingAddress(final RemoteDiagnosisCmd cmd) throws AgentUnavailableException {
        final Long routerId = cmd.getId();
        final DomainRouterVO router = domainRouterDao.findById(routerId);
        final HostVO host = hostDao.findById(router.getHostId());
        final Long hostId = host.getId();

        // Verify parameter
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router by id " + routerId + ".");
        }

        final ExecuteDiagnosisCommand command = new ExecuteDiagnosisCommand(routerId,
                cmd.getIpaddress(), cmd.getDiagnosisType());
        command.setAccessDetail(NetworkElementCommand.ROUTER_IP, routerControlHelper.getRouterControlIp(router.getId()));

        Answer origAnswer;
        try{
            origAnswer = agentManager.send(hostId, command);
        } catch (final OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
            throw new AgentUnavailableException("Unable to send commands to virtual router ", hostId, e);
        }

        ExecuteDiagnosisAnswer answer = null;
        if (origAnswer instanceof ExecuteDiagnosisAnswer) {
            answer = (ExecuteDiagnosisAnswer) origAnswer;
        } else {
            s_logger.warn("Unable to update router " + router.getHostName() + "'s status");
        }

        return createRemoteDiagnosisResponse(answer);
    }

    public static RemoteDiagnosisResponse createRemoteDiagnosisResponse(ExecuteDiagnosisAnswer answer){
        RemoteDiagnosisResponse response = new RemoteDiagnosisResponse();
        response.setResult(answer.getResult());
        response.setDetails(answer.getDetails());
        return response;
    }










    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(RemoteDiagnosisCmd.class);
        return cmdList;
    }


}
