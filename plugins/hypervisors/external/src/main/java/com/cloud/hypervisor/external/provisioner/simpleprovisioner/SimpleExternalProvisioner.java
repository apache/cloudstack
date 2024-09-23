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
package com.cloud.hypervisor.external.provisioner.simpleprovisioner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.PrepareExternalProvisioningAnswer;
import com.cloud.agent.api.PrepareExternalProvisioningCommand;
import com.cloud.agent.api.PostExternalProvisioningAnswer;
import com.cloud.agent.api.PostExternalProvisioningCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.ExternalProvisioner;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.HumanReadableJson;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.guru.ExternalHypervisorGuru;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleExternalProvisioner extends AdapterBase implements ExternalProvisioner, Configurable {

    private static final Logger logger = Logger.getLogger(SimpleExternalProvisioner.class);

    public static final ConfigKey<String> SampleExternalConfig = new ConfigKey<>(String.class, "sample.external.provisioning.config", "Advanced", "",
            "Sample external provisioning config, any value that has to be sent", true, ConfigKey.Scope.Cluster, null);

    String ExternalProvisioningConfig = "external.provisioning.config";

    public static final String EXTERNAL_PROVISIONER_SCRIPT = "scripts/vm/hypervisor/external/simpleExternalProvisioner/provisioner.sh";
    public static final String EXTERNAL_POWER_OPERATIONS_SCRIPT = "scripts/vm/hypervisor/external/simpleExternalProvisioner/powerOperations.sh";

    @Inject
    UserVmDao _uservmDao;

    @Inject
    HostDao _hostDao;

    @Inject
    VMInstanceDao _vmDao;


    @Inject
    private HypervisorGuruManager _hvGuruMgr;

    @Override
    public String getName() {
        return "simpleExternalProvisioner";
    }

    @Override
    public String getDescription() {
        return "Simple external provisioner";
    }

    private Map<String, String> loadAccessDetails(Map<String, String> accessDetails, Long clusterId, String vmTO) {
        Map<String, String> modifiedDetails = new HashMap<>();
        for (Map.Entry<String, String> entry : accessDetails.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(VmDetailConstants.EXTERNAL_DETAIL_PREFIX)) {
                key = key.substring(VmDetailConstants.EXTERNAL_DETAIL_PREFIX.length());
                modifiedDetails.put(key, entry.getValue());
            } else {
                modifiedDetails.put(key, entry.getValue());
            }
        }

        if (modifiedDetails.get(ExternalProvisioningConfig) == null) {
            modifiedDetails.put(ExternalProvisioningConfig, SampleExternalConfig.valueIn(clusterId));
        }

        modifiedDetails.put(VmDetailConstants.CLOUDSTACK_VM_DETAILS, vmTO);

        logger.debug(String.format("Using these access details for VM instance operation: %s", accessDetails));

        return modifiedDetails;
    }

    @Override
    public PrepareExternalProvisioningAnswer prepareExternalProvisioning(PrepareExternalProvisioningCommand cmd) {
        VirtualMachineTO vmTO = cmd.getVirtualMachineTO();
        String vmUUID = vmTO.getUuid();
        logger.debug(String.format("Executing PrepareExternalProvisioningCommand in the external provisioner " +
                "for the VM %s as part of VM deployment", vmUUID));

        String prepareExternalScript = Script.findScript("", EXTERNAL_PROVISIONER_SCRIPT);
        Long clusterId = cmd.getClusterId();
        Map<String, String> accessDetails = loadAccessDetails(vmTO.getDetails(), clusterId, getVirtualMachineTOJsonString(vmTO));

        Pair<Boolean, String> result = prepareExternalProvisioningInternal(prepareExternalScript, vmUUID, accessDetails, cmd.getWait());
        String output = result.second();

        if (!result.first()) {
            return new PrepareExternalProvisioningAnswer(cmd, output, false);
        }

        if (StringUtils.isEmpty(output)) {
            return new PrepareExternalProvisioningAnswer(cmd, "", true);
        }

        try {
            Map<String, String> resultMap = StringUtils.parseJsonToMap(output);
            Map<String, String> modifiedMap = new HashMap<>();
            for (Map.Entry<String, String> entry : resultMap.entrySet()) {
                modifiedMap.put(VmDetailConstants.EXTERNAL_DETAIL_PREFIX + entry.getKey(), entry.getValue());
            }
            return new PrepareExternalProvisioningAnswer(cmd, modifiedMap, null);
        } catch (CloudRuntimeException e) {
            logger.debug(String.format("Failed to parse the output from preparing external provisioning operation as part of VM deployment with error %s", e.getMessage()));
            return new PrepareExternalProvisioningAnswer(cmd, e.getMessage(), false);
        }
    }

    @Override
    public StartAnswer startInstance(StartCommand cmd) {
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        UserVmVO uservm = _uservmDao.findById(virtualMachineTO.getId());
        HostVO host = _hostDao.findById(uservm.getHostId());
        Map<String, String> accessDetails = loadAccessDetails(virtualMachineTO.getDetails(), host.getClusterId(), getVirtualMachineTOJsonString(virtualMachineTO));
        String vmUUID = virtualMachineTO.getUuid();

        logger.debug(String.format("Executing StartCommand in the external provisioner for VM %s", vmUUID));

        String deployvm = accessDetails.get("deployvm");
        boolean isDeploy = (deployvm != null && Boolean.parseBoolean(deployvm));
        String operation = isDeploy ? "Deploying" : "Starting";
        String prepareExternalScript = isDeploy ? Script.findScript("", EXTERNAL_PROVISIONER_SCRIPT) : Script.findScript("", EXTERNAL_POWER_OPERATIONS_SCRIPT);

        try {
            Pair<Boolean, String> result = executeStartCommandOnExternalSystem(isDeploy, prepareExternalScript, vmUUID, accessDetails, cmd.getWait());

            if (!result.first()) {
                String errMsg = String.format("%s VM %s on the external system failed: %s", operation, vmUUID, result.second());
                logger.debug(errMsg);
                return new StartAnswer(cmd, result.second());
            }
            logger.debug(String.format("%s VM %s on the external system", operation, vmUUID));
            return new StartAnswer(cmd);

        } catch (CloudRuntimeException e) {
            String errMsg = String.format("%s VM %s on the external system failed: %s", operation, vmUUID, e.getMessage());
            logger.debug(errMsg);
            return new StartAnswer(cmd, errMsg);
        }
    }

    private Pair<Boolean, String> executeStartCommandOnExternalSystem(boolean isDeploy, String filename, String vmUUID, Map<String, String> accessDetails, int wait) {
        if (isDeploy) {
            return deployInstanceOnExternalSystem(filename, vmUUID, accessDetails, wait);
        } else {
            return startInstanceOnExternalSystem(filename, vmUUID, accessDetails, wait);
        }
    }

    @Override
    public StopAnswer stopInstance(StopCommand cmd) {
        return stopInstanceOnExternalSystem(cmd);
    }

    @Override
    public RebootAnswer rebootInstance(RebootCommand cmd) {
        return rebootInstanceOnExternalSystem(cmd);
    }

    @Override
    public StopAnswer expungeInstance(StopCommand cmd) {
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        UserVmVO uservm = _uservmDao.findById(virtualMachineTO.getId());
        String vmUUID = uservm.getUuid();
        logger.debug(String.format("Executing stop command in the external system for the VM %s", vmUUID));

        String prepareExternalScript = Script.findScript("", EXTERNAL_PROVISIONER_SCRIPT);
        HostVO host = _hostDao.findById(uservm.getLastHostId());
        Map<String, String> accessDetails = loadAccessDetails(cmd.getDetails(), host.getClusterId(), getVirtualMachineTOJsonString(virtualMachineTO));

        Pair<Boolean, String> result = deleteInstanceOnExternalSystem(prepareExternalScript, vmUUID, accessDetails, cmd.getWait());
        if (result.first()) {
            return new StopAnswer(cmd, null, true);
        } else {
            return new StopAnswer(cmd, result.second(), false);
        }
    }

    @Override
    public PostExternalProvisioningAnswer postsetupInstance(PostExternalProvisioningCommand cmd) {
        PostExternalProvisioningAnswer answer = new PostExternalProvisioningAnswer(cmd, null, null);
        return answer;
    }

    @Override
    public HashMap<String, HostVmStateReportEntry> getHostVmStateReport(Long hostId) {
        List<UserVmVO> vms = _uservmDao.listByHostId(hostId);
        List<UserVmVO> stoppedVMs = _uservmDao.listByLastHostId(hostId);
        List<UserVmVO> allVms = new ArrayList<>();
        allVms.addAll(vms);
        allVms.addAll(stoppedVMs);
        final HashMap<String, HostVmStateReportEntry> vmStates = new HashMap<>();
        if (CollectionUtils.isNotEmpty(allVms)) {
            for (UserVmVO vm: allVms) {
                VirtualMachine.PowerState powerState = getVMpowerState(vm);
                vmStates.put(vm.getInstanceName(), new HostVmStateReportEntry(powerState, "host-" + hostId));
            }
        }

        return vmStates;
    }

    private VirtualMachine.PowerState getVMpowerState(UserVmVO uservm) {
        HostVO host;
        if (uservm.getHostId() != null) {
            host = _hostDao.findById(uservm.getHostId());
        } else if (uservm.getLastHostId() != null){
            host = _hostDao.findById(uservm.getLastHostId());
        } else {
            return VirtualMachine.PowerState.PowerUnknown;
        }
        _hostDao.loadDetails(host);
        _uservmDao.loadDetails(uservm);
        Map<String, String> hostDetails = host.getDetails();
        Map<String, String> userVmDetails = uservm.getDetails();
        HashMap<String, String> accessDetails = new HashMap<>();
        ExternalHypervisorGuru.loadExternalHostAccessDetails(hostDetails, accessDetails, host.getClusterId());
        ExternalHypervisorGuru.loadExternalInstanceDetails(userVmDetails, accessDetails);
        final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(Hypervisor.HypervisorType.External);
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(uservm);
        VirtualMachineTO virtualMachineTO = hvGuru.implement(profile);

        Map<String, String> modifiedDetails = loadAccessDetails(accessDetails, host.getClusterId(), getVirtualMachineTOJsonString(virtualMachineTO));

        String vmUUID = uservm.getUuid();
        logger.debug(String.format("Trying to get VM power status from the external system for the VM %s", vmUUID));

        String prepareExternalScript = Script.findScript("", EXTERNAL_POWER_OPERATIONS_SCRIPT);

        Pair<Boolean, String> result = getInstanceStatusOnExternalSystem(prepareExternalScript, vmUUID, modifiedDetails, AgentManager.Wait.value());
        if (result.first()) {
            if (result.second().equalsIgnoreCase(VirtualMachine.PowerState.PowerOn.toString())) {
                return VirtualMachine.PowerState.PowerOn;
            } else if (result.second().equalsIgnoreCase(VirtualMachine.PowerState.PowerOff.toString())) {
                return VirtualMachine.PowerState.PowerOff;
            } else {
                return VirtualMachine.PowerState.PowerUnknown;
            }
        } else {
            logger.debug(String.format("Exception occurred while trying to fetch the power status of the VM %d : %s", uservm.getId(), result.second()));
            return VirtualMachine.PowerState.PowerUnknown;
        }
    }

    private StopAnswer stopInstanceOnExternalSystem(StopCommand cmd) {
        logger.debug(String.format("Executing stop command on the external provisioner"));
        VMInstanceVO uservm = _vmDao.findVMByInstanceName(cmd.getVmName());
        final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(Hypervisor.HypervisorType.External);
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(uservm);
        VirtualMachineTO virtualMachineTO = hvGuru.implement(profile);
        String vmUUID = profile.getUuid();
        logger.debug(String.format("Executing stop command in the external system for the VM %s", vmUUID));

        String prepareExternalScript = Script.findScript("", EXTERNAL_POWER_OPERATIONS_SCRIPT);
        HostVO host = _hostDao.findById(uservm.getLastHostId());
        Map<String, String> accessDetails = loadAccessDetails(cmd.getDetails(), host.getClusterId(), getVirtualMachineTOJsonString(virtualMachineTO));

        Pair<Boolean, String> result = stopInstanceOnExternalSystem(prepareExternalScript, vmUUID, accessDetails, cmd.getWait());
        if (result.first()) {
            return new StopAnswer(cmd, null, true);
        } else {
            return new StopAnswer(cmd, result.second(), false);
        }
    }

    private RebootAnswer rebootInstanceOnExternalSystem(RebootCommand cmd) {
        logger.debug(String.format("Executing reboot command using IPMI in the external provisioner"));
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        UserVmVO uservm = _uservmDao.findById(virtualMachineTO.getId());
        String vmUUID = uservm.getUuid();
        logger.debug(String.format("Executing reboot command in the external system for the VM %s", vmUUID));

        String prepareExternalScript = Script.findScript("", EXTERNAL_POWER_OPERATIONS_SCRIPT);
        HostVO host = _hostDao.findById(uservm.getLastHostId());
        Map<String, String> accessDetails = loadAccessDetails(cmd.getDetails(), host.getClusterId(), getVirtualMachineTOJsonString(virtualMachineTO));

        Pair<Boolean, String> result = rebootInstanceOnExternalSystem(prepareExternalScript, vmUUID, accessDetails, cmd.getWait());
        if (result.first()) {
            return new RebootAnswer(cmd, null, true);
        } else {
            return new RebootAnswer(cmd, result.second(), false);
        }
    }

    public Pair<Boolean, String> prepareExternalProvisioningInternal(String filename, String vmUUID, Map<String, String> accessDetails, int wait) {
        return executeExternalCommand(filename, "prepare", accessDetails, wait,
                String.format("Failed to prepare external provisioner for deploying VM %s on external system", vmUUID));
    }

    public Pair<Boolean, String> deployInstanceOnExternalSystem(String filename, String vmUUID, Map<String, String> accessDetails, int wait) {
        return executeExternalCommand(filename, "create", accessDetails, wait,
                String.format("Failed to create the instance %s on external system", vmUUID));
    }

    public Pair<Boolean, String> startInstanceOnExternalSystem(String filename, String vmUUID, Map<String, String> accessDetails, int wait) {
        return executeExternalCommand(filename, "start", accessDetails, wait,
                String.format("Failed to start the instance %s on external system", vmUUID));
    }

    public Pair<Boolean, String> stopInstanceOnExternalSystem(String filename, String vmUUID, Map<String, String> accessDetails, int wait) {
        return executeExternalCommand(filename, "stop", accessDetails, wait,
                String.format("Failed to stop the instance %s on external system", vmUUID));
    }

    public Pair<Boolean, String> rebootInstanceOnExternalSystem(String filename, String vmUUID, Map<String, String> accessDetails, int wait) {
        return executeExternalCommand(filename, "reboot", accessDetails, wait,
                String.format("Failed to reboot the instance %s on external system", vmUUID));
    }

    public Pair<Boolean, String> deleteInstanceOnExternalSystem(String filename, String vmUUID, Map<String, String> accessDetails, int wait) {
        return executeExternalCommand(filename, "delete", accessDetails, wait,
                String.format("Failed to delete the instance %s on external system", vmUUID));
    }

    public Pair<Boolean, String> getInstanceStatusOnExternalSystem(String filename, String vmUUID, Map<String, String> accessDetails, int wait) {
        return executeExternalCommand(filename, "status", accessDetails, wait,
                String.format("Failed to get the instance power status %s on external system", vmUUID));
    }

    public Pair<Boolean, String> executeExternalCommand(String filename, String action, Map<String, String> accessDetails, int wait, String logPrefix) {
        try {
            String parameters = prepareParameters(accessDetails);
            final Script command = new Script("/bin/bash");
            command.add(filename);
            command.add(action);
            command.add(parameters);
            command.add(Integer.toString(wait));

            OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
            String result = command.execute(parser);

            if (result != null) {
                logger.debug(String.format("%s: External API execution failed with result %s", logPrefix, result));
                return new Pair<>(false, result);
            }

            result = parser.getLines();
            return new Pair<>(true, result);

        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("%s: External operation failed", logPrefix), e);
        }
    }

    private String prepareParameters(Map<String, String> details) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String parameters = objectMapper.writeValueAsString(details);
        return parameters;
    }

    private String getVirtualMachineTOJsonString(VirtualMachineTO vmTO) {
        StringBuilder content = new StringBuilder();
        Gson s_gogger = GsonHelper.getGsonLogger();
        s_gogger.toJson(vmTO, content);
        return HumanReadableJson.getHumanReadableBytesJson(content.toString());
    }

    @Override
    public String getConfigComponentName() {
        return SimpleExternalProvisioner.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {SampleExternalConfig};
    }
}
