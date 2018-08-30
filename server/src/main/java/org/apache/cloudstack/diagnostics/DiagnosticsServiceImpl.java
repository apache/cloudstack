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
package org.apache.cloudstack.diagnostics;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Strings;
import org.apache.cloudstack.api.command.admin.diagnostics.RunDiagnosticsCmd;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DiagnosticsServiceImpl extends ManagerBase implements PluggableService, DiagnosticsService {
    private static final Logger LOGGER = Logger.getLogger(DiagnosticsServiceImpl.class);

    @Inject
    private AgentManager agentManager;
    @Inject
    private VMInstanceDao instanceDao;
    @Inject
    private VirtualMachineManager vmManager;
    @Inject
    private NetworkOrchestrationService networkManager;

    @Override
    public Map<String, String> runDiagnosticsCommand(final RunDiagnosticsCmd cmd) {
        final Long vmId = cmd.getId();
        final String cmdType = cmd.getType().getValue();
        final String ipAddress = cmd.getAddress();
        final String optionalArguments = cmd.getOptionalArguments();
        final VMInstanceVO vmInstance = instanceDao.findByIdTypes(vmId, VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.DomainRouter, VirtualMachine.Type.SecondaryStorageVm);

        if (vmInstance == null) {
            throw new InvalidParameterValueException("Unable to find a system vm with id " + vmId);
        }
        final Long hostId = vmInstance.getHostId();

        if (hostId == null) {
            throw new CloudRuntimeException("Unable to find host for virtual machine instance: " + vmInstance.getInstanceName());
        }

        final String shellCmd = prepareShellCmd(cmdType, ipAddress, optionalArguments);

        if (Strings.isNullOrEmpty(shellCmd)) {
            throw new IllegalArgumentException("Optional parameters contain unwanted characters: " + optionalArguments);
        }

        final Hypervisor.HypervisorType hypervisorType = vmInstance.getHypervisorType();

        final DiagnosticsCommand command = new DiagnosticsCommand(shellCmd, vmManager.getExecuteInSequence(hypervisorType));
        final Map<String, String> accessDetails = networkManager.getSystemVMAccessDetails(vmInstance);

        if (Strings.isNullOrEmpty(accessDetails.get(NetworkElementCommand.ROUTER_IP))) {
            throw new CloudRuntimeException("Unable to set system vm ControlIP for system vm with ID: " + vmId);
        }

        command.setAccessDetail(accessDetails);

        Map<String, String> detailsMap;

        final Answer answer = agentManager.easySend(hostId, command);

        if (answer != null && (answer instanceof DiagnosticsAnswer)) {
            detailsMap = ((DiagnosticsAnswer) answer).getExecutionDetails();
            return detailsMap;
        } else {
            throw new CloudRuntimeException("Failed to execute diagnostics command on remote host: " + answer.getDetails());
        }
    }

    protected boolean hasValidChars(String optionalArgs) {
        if (Strings.isNullOrEmpty(optionalArgs)) {
            return true;
        } else {
            final String regex = "^[\\w\\-\\s.]+$";
            final Pattern pattern = Pattern.compile(regex);
            return pattern.matcher(optionalArgs).find();
        }

    }

    protected String prepareShellCmd(String cmdType, String ipAddress, String optionalParams) {
        final String CMD_TEMPLATE = String.format("%s %s", cmdType, ipAddress);
        if (Strings.isNullOrEmpty(optionalParams)) {
            return CMD_TEMPLATE;
        } else {
            if (hasValidChars(optionalParams)) {
                return String.format("%s %s", CMD_TEMPLATE, optionalParams);
            } else {
                return null;
            }
        }
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(RunDiagnosticsCmd.class);
        return cmdList;
    }
}