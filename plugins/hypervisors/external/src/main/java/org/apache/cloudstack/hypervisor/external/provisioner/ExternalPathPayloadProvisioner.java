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
package org.apache.cloudstack.hypervisor.external.provisioner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsFilesystemManager;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.GetExternalConsoleAnswer;
import com.cloud.agent.api.GetExternalConsoleCommand;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.PrepareExternalProvisioningAnswer;
import com.cloud.agent.api.PrepareExternalProvisioningCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RunCustomActionAnswer;
import com.cloud.agent.api.RunCustomActionCommand;
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
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.json.JsonMergeUtil;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDao;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ExternalPathPayloadProvisioner extends ManagerBase implements ExternalProvisioner {

    @Inject
    UserVmDao _uservmDao;

    @Inject
    HostDao hostDao;

    @Inject
    HypervisorGuruManager hypervisorGuruManager;

    @Inject
    ExtensionsManager extensionsManager;

    @Inject
    ExtensionsFilesystemManager extensionsFilesystemManager;

    private static final List<String> TRIVIAL_ACTIONS = Arrays.asList(
            "status"
    );

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    protected Map<String, Object> loadAccessDetails(Map<String, Map<String, String>> externalDetails,
                            VirtualMachineTO virtualMachineTO) {
        Map<String, Object> modifiedDetails = new HashMap<>();
        if (MapUtils.isNotEmpty(externalDetails) && externalDetails.containsKey(ApiConstants.CALLER)) {
            modifiedDetails.put(ApiConstants.CALLER, externalDetails.get(ApiConstants.CALLER));
            externalDetails.remove(ApiConstants.CALLER);
        }
        if (MapUtils.isNotEmpty(externalDetails)) {
            modifiedDetails.put(ApiConstants.EXTERNAL_DETAILS, externalDetails);
        }
        if (virtualMachineTO != null) {
            modifiedDetails.put(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineTO.getUuid());
            modifiedDetails.put(ApiConstants.VIRTUAL_MACHINE_NAME, virtualMachineTO.getName());
            modifiedDetails.put(VmDetailConstants.CLOUDSTACK_VM_DETAILS, virtualMachineTO);
        }
        return modifiedDetails;
    }

    protected VirtualMachineTO getVirtualMachineTO(VirtualMachine vm) {
        if (vm == null) {
            return null;
        }
        final HypervisorGuru hvGuru = hypervisorGuruManager.getGuru(Hypervisor.HypervisorType.External);
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
        return hvGuru.implement(profile);
    }

    protected String getSanitizedJsonStringForLog(String json) {
        if (StringUtils.isBlank(json)) {
            return json;
        }
        return json.replaceAll("(\"password\"\\s*:\\s*\")([^\"]*)(\")", "$1****$3");
    }

    protected String getExtensionConfigureError(String extensionName, String hostName) {
        StringBuilder sb = new StringBuilder("Extension: ").append(extensionName).append(" not configured");
        if (StringUtils.isNotBlank(hostName)) {
            sb.append(" for host: ").append(hostName);
        }
        return sb.toString();
    }

    @Override
    public PrepareExternalProvisioningAnswer prepareExternalProvisioning(String hostName,
                 String extensionName, String extensionRelativePath, PrepareExternalProvisioningCommand cmd) {
        String extensionPath = extensionsFilesystemManager.getExtensionCheckedPath(extensionName, extensionRelativePath);
        if (StringUtils.isEmpty(extensionPath)) {
            return new PrepareExternalProvisioningAnswer(cmd, false, getExtensionConfigureError(extensionName, hostName));
        }
        VirtualMachineTO vmTO = cmd.getVirtualMachineTO();
        String vmUUID = vmTO.getUuid();
        logger.debug("Executing PrepareExternalProvisioningCommand in the external provisioner " +
                "for the VM {} as part of VM deployment", vmUUID);
        Map<String, Object> accessDetails = loadAccessDetails(cmd.getExternalDetails(), vmTO);
        Pair<Boolean, String> result = prepareExternalProvisioningInternal(extensionName, extensionPath,
                vmUUID, accessDetails, cmd.getWait());
        String output = result.second();
        if (!result.first()) {
            return new PrepareExternalProvisioningAnswer(cmd, false, output);
        }
        if (StringUtils.isEmpty(output)) {
            return new PrepareExternalProvisioningAnswer(cmd, result.first(), "");
        }
        try {
            String merged = JsonMergeUtil.mergeJsonPatch(GsonHelper.getGson().toJson(vmTO), result.second());
            VirtualMachineTO virtualMachineTO  = GsonHelper.getGson().fromJson(merged, VirtualMachineTO.class);
            return new PrepareExternalProvisioningAnswer(cmd, null, virtualMachineTO, null);
        } catch (Exception e) {
            logger.warn("Failed to parse the output from preparing external provisioning operation as " +
                    "part of VM deployment: {}", e.getMessage(), e);
            return new PrepareExternalProvisioningAnswer(cmd, false, "Failed to parse VM");
        }
    }

    @Override
    public StartAnswer startInstance(String hostName, String extensionName, String extensionRelativePath,
                             StartCommand cmd) {
        String extensionPath = extensionsFilesystemManager.getExtensionCheckedPath(extensionName, extensionRelativePath);
        if (StringUtils.isEmpty(extensionPath)) {
            return new StartAnswer(cmd, getExtensionConfigureError(extensionName, hostName));
        }
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        Map<String, Object> accessDetails = loadAccessDetails(cmd.getExternalDetails(), virtualMachineTO);
        String vmUUID = virtualMachineTO.getUuid();

        logger.debug(String.format("Executing StartCommand in the external provisioner for VM %s", vmUUID));

        Object deployvm = virtualMachineTO.getDetails().get("deployvm");
        boolean isDeploy = (deployvm != null && Boolean.parseBoolean((String)deployvm));
        String operation = isDeploy ? "Deploying" : "Starting";
        try {
            Pair<Boolean, String> result = executeStartCommandOnExternalSystem(extensionName, isDeploy,
                    extensionPath, vmUUID, accessDetails, cmd.getWait());

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

    private Pair<Boolean, String> executeStartCommandOnExternalSystem(String extensionName, boolean isDeploy,
                          String filename, String vmUUID, Map<String, Object> accessDetails, int wait) {
        if (isDeploy) {
            return deployInstanceOnExternalSystem(extensionName, filename, vmUUID, accessDetails, wait);
        } else {
            return startInstanceOnExternalSystem(extensionName, filename, vmUUID, accessDetails, wait);
        }
    }

    @Override
    public StopAnswer stopInstance(String hostName, String extensionName, String extensionRelativePath,
                           StopCommand cmd) {
        String extensionPath = extensionsFilesystemManager.getExtensionCheckedPath(extensionName, extensionRelativePath);
        if (StringUtils.isEmpty(extensionPath)) {
            return new StopAnswer(cmd, getExtensionConfigureError(extensionName, hostName), false);
        }
        logger.debug("Executing stop command on the external provisioner");
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        String vmUUID = cmd.getVirtualMachine().getUuid();
        logger.debug("Executing stop command in the external system for the VM {}", vmUUID);
        Map<String, Object> accessDetails = loadAccessDetails(cmd.getExternalDetails(), virtualMachineTO);
        Pair<Boolean, String> result = stopInstanceOnExternalSystem(extensionName, extensionPath, vmUUID,
                accessDetails, cmd.getWait());
        if (result.first()) {
            return new StopAnswer(cmd, null, true);
        } else {
            return new StopAnswer(cmd, result.second(), false);
        }
    }

    @Override
    public RebootAnswer rebootInstance(String hostName, String extensionName, String extensionRelativePath,
                           RebootCommand cmd) {
        String extensionPath = extensionsFilesystemManager.getExtensionCheckedPath(extensionName, extensionRelativePath);
        if (StringUtils.isEmpty(extensionPath)) {
            return new RebootAnswer(cmd, getExtensionConfigureError(extensionName, hostName), false);
        }
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        String vmUUID = virtualMachineTO.getUuid();
        logger.debug("Executing reboot command in the external system for the VM {}", vmUUID);
        Map<String, Object> accessDetails = loadAccessDetails(cmd.getExternalDetails(), virtualMachineTO);
        Pair<Boolean, String> result = rebootInstanceOnExternalSystem(extensionName, extensionPath, vmUUID,
                accessDetails, cmd.getWait());
        if (result.first()) {
            return new RebootAnswer(cmd, null, true);
        } else {
            return new RebootAnswer(cmd, result.second(), false);
        }
    }

    @Override
    public StopAnswer expungeInstance(String hostName, String extensionName, String extensionRelativePath,
                          StopCommand cmd) {
        String extensionPath = extensionsFilesystemManager.getExtensionCheckedPath(extensionName, extensionRelativePath);
        if (StringUtils.isEmpty(extensionPath)) {
            return new StopAnswer(cmd, getExtensionConfigureError(extensionName, hostName), false);
        }
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        String vmUUID = virtualMachineTO.getUuid();
        logger.debug("Executing stop command as part of expunge in the external system for the VM {}", vmUUID);
        Map<String, Object> accessDetails = loadAccessDetails(cmd.getExternalDetails(), virtualMachineTO);
        Pair<Boolean, String> result = deleteInstanceOnExternalSystem(extensionName, extensionPath, vmUUID,
                accessDetails, cmd.getWait());
        if (result.first()) {
            return new StopAnswer(cmd, null, true);
        } else {
            return new StopAnswer(cmd, result.second(), false);
        }
    }

    @Override
    public Map<String, HostVmStateReportEntry> getHostVmStateReport(long hostId, String extensionName,
                            String extensionRelativePath) {
        final Map<String, HostVmStateReportEntry> vmStates = new HashMap<>();
        String extensionPath = extensionsFilesystemManager.getExtensionCheckedPath(extensionName, extensionRelativePath);
        if (StringUtils.isEmpty(extensionPath)) {
            return vmStates;
        }
        HostVO host = hostDao.findById(hostId);
        if (host == null) {
            logger.error("Host with ID: {} not found", hostId);
            return vmStates;
        }
        List<UserVmVO> allVms = _uservmDao.listByHostId(hostId);
        allVms.addAll(_uservmDao.listByLastHostId(hostId));
        if (CollectionUtils.isEmpty(allVms)) {
            logger.debug("No VMs found for the {}", host);
            return vmStates;
        }
        Map<String, Map<String, String>> accessDetails =
                extensionsManager.getExternalAccessDetails(host, null);
        for (UserVmVO vm: allVms) {
            VirtualMachine.PowerState powerState = getVmPowerState(vm, accessDetails, extensionName, extensionPath);
            vmStates.put(vm.getInstanceName(), new HostVmStateReportEntry(powerState, "host-" + hostId));
        }
        return vmStates;
    }

    @Override
    public GetExternalConsoleAnswer getInstanceConsole(String hostName, String extensionName,
                           String extensionRelativePath, GetExternalConsoleCommand cmd) {
        String extensionPath = extensionsFilesystemManager.getExtensionCheckedPath(extensionName, extensionRelativePath);
        if (StringUtils.isEmpty(extensionPath)) {
            return new GetExternalConsoleAnswer(cmd, getExtensionConfigureError(extensionName, hostName));
        }
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        String vmUUID = virtualMachineTO.getUuid();
        logger.debug("Executing getconsole command in the external system for the VM {}", vmUUID);
        Map<String, Object> accessDetails = loadAccessDetails(cmd.getExternalDetails(), virtualMachineTO);
        Pair<Boolean, String> result = getInstanceConsoleOnExternalSystem(extensionName, extensionPath, vmUUID,
                accessDetails, cmd.getWait());
        if (result == null) {
            return new GetExternalConsoleAnswer(cmd, "No response from external system");
        }
        String output = result.second();
        if (!result.first()) {
            return new GetExternalConsoleAnswer(cmd, output);
        }
        logger.debug("Received console details from the external system: {}",
                getSanitizedJsonStringForLog(output));
        try {
            JsonObject jsonObj = JsonParser.parseString(output).getAsJsonObject();
            JsonObject consoleObj = jsonObj.has("console") ? jsonObj.getAsJsonObject("console") : null;
            if (consoleObj == null) {
                logger.error("Missing console object in external console output: {}",
                        getSanitizedJsonStringForLog(output));
                return new GetExternalConsoleAnswer(cmd, "Missing console object in output");
            }
            String url = consoleObj.has("url") ? consoleObj.get("url").getAsString() : null;
            String host = consoleObj.has("host") ? consoleObj.get("host").getAsString() : null;
            Integer port = consoleObj.has("port") ? Integer.valueOf(consoleObj.get("port").getAsString()) : null;
            String password = consoleObj.has("password") ? consoleObj.get("password").getAsString() : null;
            boolean passwordOneTimeUseOnly = consoleObj.has("passwordonetimeuseonly") &&
                    consoleObj.get("passwordonetimeuseonly").getAsBoolean();
            String protocol = consoleObj.has("protocol") ? consoleObj.get("protocol").getAsString() : null;
            if (url == null && ObjectUtils.anyNull(host, port)) {
                logger.error("Missing required fields in external console output: {}",
                        getSanitizedJsonStringForLog(output));
                return new GetExternalConsoleAnswer(cmd, "Missing required fields in output");
            }
            return new GetExternalConsoleAnswer(cmd, url, host, port, password, passwordOneTimeUseOnly, protocol);
        } catch (RuntimeException e) {
            logger.error("Failed to parse output for getInstanceConsole: {}", e.getMessage(), e);
            return new GetExternalConsoleAnswer(cmd, "Failed to parse output");
        }
    }

    @Override
    public RunCustomActionAnswer runCustomAction(String hostName, String extensionName,
                         String extensionRelativePath, RunCustomActionCommand cmd) {
        String extensionPath = extensionsFilesystemManager.getExtensionCheckedPath(extensionName, extensionRelativePath);
        if (StringUtils.isEmpty(extensionPath)) {
            return new RunCustomActionAnswer(cmd, false, getExtensionConfigureError(extensionName, hostName));
        }
        final String actionName = cmd.getActionName();
        final Map<String, Object> parameters = cmd.getParameters();
        logger.debug("Executing custom action '{}' in the external system", actionName);
        Map<String, Object> accessDetails = loadAccessDetails(cmd.getExternalDetails(), cmd.getVmTO());
        accessDetails.put(ApiConstants.ACTION, actionName);
        if (MapUtils.isNotEmpty(parameters)) {
            accessDetails.put(ApiConstants.PARAMETERS, parameters);
        }
        Pair<Boolean, String> result = runCustomActionOnExternalSystem(extensionName, extensionPath,
                actionName, accessDetails, cmd.getWait());
        return new RunCustomActionAnswer(cmd, result.first(), result.second());
    }

    protected Pair<Boolean, String> runCustomActionOnExternalSystem(String extensionName, String filename,
                 String actionName, Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(extensionName, actionName, accessDetails, wait,
                String.format("Failed to execute custom action '%s' on external system", actionName), filename);
    }

    protected VirtualMachine.PowerState getPowerStateFromString(String powerStateStr) {
        if (StringUtils.isBlank(powerStateStr)) {
            return VirtualMachine.PowerState.PowerUnknown;
        }
        if (powerStateStr.equalsIgnoreCase(VirtualMachine.PowerState.PowerOn.toString())) {
            return VirtualMachine.PowerState.PowerOn;
        } else if (powerStateStr.equalsIgnoreCase(VirtualMachine.PowerState.PowerOff.toString())) {
            return VirtualMachine.PowerState.PowerOff;
        }
        return VirtualMachine.PowerState.PowerUnknown;
    }

    protected VirtualMachine.PowerState parsePowerStateFromResponse(UserVmVO userVmVO, String response) {
        logger.debug("Power status response from the external system for {} : {}", userVmVO, response);
        if (StringUtils.isBlank(response)) {
            logger.warn("Empty response while trying to fetch the power status of the {}", userVmVO);
            return VirtualMachine.PowerState.PowerUnknown;
        }
        if (!response.trim().startsWith("{")) {
            return getPowerStateFromString(response);
        }
        try {
            JsonObject jsonObj = new JsonParser().parse(response).getAsJsonObject();
            String powerState = jsonObj.has("power_state") ? jsonObj.get("power_state").getAsString() : null;
            return getPowerStateFromString(powerState);
        } catch (Exception e) {
            logger.warn("Failed to parse power status response: {} for {} as JSON: {}",
                    response, userVmVO, e.getMessage());
            return VirtualMachine.PowerState.PowerUnknown;
        }
    }

    private VirtualMachine.PowerState getVmPowerState(UserVmVO userVmVO, Map<String, Map<String, String>> accessDetails,
                  String extensionName, String extensionPath) {
        VirtualMachineTO virtualMachineTO = getVirtualMachineTO(userVmVO);
        accessDetails.put(ApiConstants.VIRTUAL_MACHINE, virtualMachineTO.getExternalDetails());
        Map<String, Object> modifiedDetails = loadAccessDetails(accessDetails, virtualMachineTO);
        String vmUUID = userVmVO.getUuid();
        logger.debug("Trying to get VM power status from the external system for {}", userVmVO);
        Pair<Boolean, String> result = getInstanceStatusOnExternalSystem(extensionName, extensionPath, vmUUID,
                modifiedDetails, AgentManager.Wait.value());
        if (!result.first()) {
            logger.warn("Failure response received while trying to fetch the power status of the {} : {}",
                    userVmVO, result.second());
            return VirtualMachine.PowerState.PowerUnknown;
        }
        return parsePowerStateFromResponse(userVmVO, result.second());
    }
    public Pair<Boolean, String> prepareExternalProvisioningInternal(String extensionName, String filename,
                             String vmUUID, Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(extensionName, "prepare", accessDetails, wait,
                String.format("Failed to prepare external provisioner for deploying VM %s on external system", vmUUID),
                filename);
    }

    public Pair<Boolean, String> deployInstanceOnExternalSystem(String extensionName, String filename, String vmUUID,
                            Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(extensionName, "create", accessDetails, wait,
                String.format("Failed to create the instance %s on external system", vmUUID), filename);
    }

    public Pair<Boolean, String> startInstanceOnExternalSystem(String extensionName, String filename, String vmUUID,
                               Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(extensionName, "start", accessDetails, wait,
                String.format("Failed to start the instance %s on external system", vmUUID), filename);
    }

    public Pair<Boolean, String> stopInstanceOnExternalSystem(String extensionName, String filename, String vmUUID,
                              Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(extensionName, "stop", accessDetails, wait,
                String.format("Failed to stop the instance %s on external system", vmUUID), filename);
    }

    public Pair<Boolean, String> rebootInstanceOnExternalSystem(String extensionName, String filename, String vmUUID,
                                Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(extensionName, "reboot", accessDetails, wait,
                String.format("Failed to reboot the instance %s on external system", vmUUID), filename);
    }

    public Pair<Boolean, String> deleteInstanceOnExternalSystem(String extensionName, String filename, String vmUUID,
                                Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(extensionName, "delete", accessDetails, wait,
                String.format("Failed to delete the instance %s on external system", vmUUID), filename);
    }

    public Pair<Boolean, String> getInstanceStatusOnExternalSystem(String extensionName, String filename, String vmUUID,
                               Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(extensionName, "status", accessDetails, wait,
                String.format("Failed to get the instance power status %s on external system", vmUUID), filename);
    }

    public Pair<Boolean, String> getInstanceConsoleOnExternalSystem(String extensionName, String filename,
                            String vmUUID, Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(extensionName, "getconsole", accessDetails, wait,
                String.format("Failed to get the instance console %s on external system", vmUUID), filename);
    }

    public Pair<Boolean, String> executeExternalCommand(String extensionName, String action,
                        Map<String, Object> accessDetails, int wait, String errorLogPrefix, String file) {
        try {
            Path executablePath = Paths.get(file).toAbsolutePath().normalize();
            if (!Files.isExecutable(executablePath)) {
                logger.error("{}: File is not executable: {}", errorLogPrefix, executablePath);
                return new Pair<>(false, "File is not executable");
            }
            if (wait == 0) {
                wait = AgentManager.Wait.value();
            }
            List<String> command = new ArrayList<>();
            command.add(executablePath.toString());
            command.add(action);
            String dataFile = extensionsFilesystemManager.prepareExternalPayload(extensionName, accessDetails);
            command.add(dataFile);
            command.add(Integer.toString(wait));
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);

            logger.debug("Executing {} for command: {} with wait: {} and data file: {}", executablePath,
                    action, wait, dataFile);

            Process process = builder.start();
            boolean finished = process.waitFor(wait, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.error("{}: External API execution timed out after {} seconds", errorLogPrefix, wait);
                return new Pair<>(false, "Timeout");
            }
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.warn("{}: External API execution failed with exit code {}", errorLogPrefix, exitCode);
                return new Pair<>(false, "Exit code: " + exitCode + ", Output: " + output.toString().trim());
            }
            deleteExtensionPayloadFile(extensionName, action, dataFile);
            return new Pair<>(true, output.toString().trim());

        } catch (IOException | InterruptedException e) {
            logger.error("{}: External operation failed", errorLogPrefix, e);
            throw new CloudRuntimeException(String.format("%s: External operation failed", errorLogPrefix), e);
        }
    }

    protected void deleteExtensionPayloadFile(String extensionName, String action, String payloadFileName) {
        if (!TRIVIAL_ACTIONS.contains(action)) {
            logger.trace("Skipping deletion of payload file: {} for extension: {}, action: {}",
                    payloadFileName, extensionName, action);
            return;
        }
        extensionsFilesystemManager.deleteExtensionPayload(extensionName, payloadFileName);
    }
}
