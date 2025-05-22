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
package org.apache.cloudstack.hypervisor.external.provisioner.simpleprovisioner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.guru.ExternalHypervisorGuru;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.PostExternalProvisioningAnswer;
import com.cloud.agent.api.PostExternalProvisioningCommand;
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
import com.cloud.utils.component.PluggableService;
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

public class SimpleExternalProvisioner extends ManagerBase implements ExternalProvisioner, PluggableService, Configurable {

    ConfigKey<String> ExtensionsDirectory = new ConfigKey<>("Advanced", String.class,
            "external.provisioner.extensions.directory", "/usr/share/cloudstack-management/extensions",
            "Directory on the management server where extensions are present",
            false, ConfigKey.Scope.Global);

    public static final String BASE_EXTERNAL_PROVISIONER_SCRIPT = "scripts/vm/hypervisor/external/simpleExternalProvisioner/provisioner.sh";

    @Inject
    UserVmDao _uservmDao;

    @Inject
    HostDao hostDao;

    @Inject
    VMInstanceDao vmInstanceDao;

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

    private String extensionsDirectory;

    protected Map<String, String> loadAccessDetails(Map<String, String> accessDetails, VirtualMachineTO virtualMachineTO) {
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

        if (virtualMachineTO != null) {
            String vmTO = getVirtualMachineTOJsonString(virtualMachineTO);
            modifiedDetails.put(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineTO.getUuid());
            modifiedDetails.put(ApiConstants.VIRTUAL_MACHINE_NAME, virtualMachineTO.getName());
            modifiedDetails.put(VmDetailConstants.CLOUDSTACK_VM_DETAILS, vmTO);
        }

        logger.debug("Using these access details for VM instance operation: {}", accessDetails);

        return modifiedDetails;
    }

    protected String getExtensionCheckedScriptPath(String extensionName) {
        String path = getExtensionScriptPath(extensionName);
        File file = new File(path);
        if (!file.exists()) {
            logger.error("Provisioner script {} for extension: {} does not exist", path, extensionName);
            return null;
        }
        if (!file.isFile()) {
            logger.error("Provisioner script {} for extension: {} is not a file", path, extensionName);
            return null;
        }
        if (!file.canRead()) {
            logger.error("Provisioner script {} for extension: {} is not readable", path, extensionName);
            return null;
        }
        return path;

    }

    protected boolean checkExtensionsDirectory() {
        File dir = new File(extensionsDirectory);
        if (!dir.exists() || !dir.isDirectory() || !dir.canWrite()) {
            logger.error("Extension directory [{}] specified by config - {} is not properly set up. It must exist, be a directory, and be writeable",
                    extensionsDirectory, ExtensionsDirectory.key());
            return false;
        }
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        extensionsDirectory = ExtensionsDirectory.value();
        if (StringUtils.isBlank(extensionsDirectory)) {
            throw new ConfigurationException("Extension directory path is blank");
        }
        checkExtensionsDirectory();
        return true;
    }

    @Override
    public String getExtensionScriptPath(String extensionName) {
        return String.format("%s%s%s%sprovisioner.sh", extensionsDirectory, File.separator, extensionName,
                File.separator);
    }

    @Override
    public PrepareExternalProvisioningAnswer prepareExternalProvisioning(String extensionName, PrepareExternalProvisioningCommand cmd) {
        String extensionPath = getExtensionCheckedScriptPath(extensionName);
        if (StringUtils.isEmpty(extensionPath)) {
            return new PrepareExternalProvisioningAnswer(cmd, "Extension not configured", false);
        }
        VirtualMachineTO vmTO = cmd.getVirtualMachineTO();
        String vmUUID = vmTO.getUuid();
        logger.debug("Executing PrepareExternalProvisioningCommand in the external provisioner " +
                "for the VM {} as part of VM deployment", vmUUID);

        String prepareExternalScript = Script.findScript("", extensionPath);
        Map<String, String> accessDetails = loadAccessDetails(vmTO.getDetails(), vmTO);

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
            logger.error("Failed to parse the output from preparing external provisioning operation as part of VM deployment with error {}", e.getMessage());
            return new PrepareExternalProvisioningAnswer(cmd, e.getMessage(), false);
        }
    }

    @Override
    public StartAnswer startInstance(String extensionName, StartCommand cmd) {
        String extensionPath = getExtensionCheckedScriptPath(extensionName);
        if (StringUtils.isEmpty(extensionPath)) {
            return new StartAnswer(cmd, "Extension not configured");
        }
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        UserVmVO uservm = _uservmDao.findById(virtualMachineTO.getId());
        HostVO host = hostDao.findById(uservm.getHostId());
        Map<String, String> accessDetails = loadAccessDetails(virtualMachineTO.getDetails(), virtualMachineTO);
        String vmUUID = virtualMachineTO.getUuid();

        logger.debug(String.format("Executing StartCommand in the external provisioner for VM %s", vmUUID));

        String deployvm = accessDetails.get("deployvm");
        boolean isDeploy = (deployvm != null && Boolean.parseBoolean(deployvm));
        String operation = isDeploy ? "Deploying" : "Starting";
        String prepareExternalScript = Script.findScript("", extensionPath);

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
    public StopAnswer stopInstance(String extensionName, StopCommand cmd) {
        String extensionPath = getExtensionCheckedScriptPath(extensionName);
        if (StringUtils.isEmpty(extensionPath)) {
            return new StopAnswer(cmd, "Extension not configured", false);
        }
        logger.debug("Executing stop command on the external provisioner");
        VMInstanceVO uservm = vmInstanceDao.findVMByInstanceName(cmd.getVmName());
        final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(Hypervisor.HypervisorType.External);
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(uservm);
        VirtualMachineTO virtualMachineTO = hvGuru.implement(profile);
        String vmUUID = profile.getUuid();
        logger.debug("Executing stop command in the external system for the VM {}", vmUUID);

        String prepareExternalScript = Script.findScript("", extensionPath);
        HostVO host = hostDao.findById(uservm.getLastHostId());
        Map<String, String> accessDetails = loadAccessDetails(cmd.getDetails(), virtualMachineTO);

        Pair<Boolean, String> result = stopInstanceOnExternalSystem(prepareExternalScript, vmUUID, accessDetails, cmd.getWait());
        if (result.first()) {
            return new StopAnswer(cmd, null, true);
        } else {
            return new StopAnswer(cmd, result.second(), false);
        }
    }

    @Override
    public RebootAnswer rebootInstance(String extensionName, RebootCommand cmd) {
        String extensionPath = getExtensionCheckedScriptPath(extensionName);
        if (StringUtils.isEmpty(extensionPath)) {
            return new RebootAnswer(cmd, "Extension not configured", false);
        }
        logger.debug("Executing reboot command using IPMI in the external provisioner");
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        UserVmVO uservm = _uservmDao.findById(virtualMachineTO.getId());
        String vmUUID = uservm.getUuid();
        logger.debug("Executing reboot command in the external system for the VM {}", vmUUID);

        String prepareExternalScript = Script.findScript("", extensionPath);
        HostVO host = hostDao.findById(uservm.getLastHostId());
        Map<String, String> accessDetails = loadAccessDetails(cmd.getDetails(), virtualMachineTO);

        Pair<Boolean, String> result = rebootInstanceOnExternalSystem(prepareExternalScript, vmUUID, accessDetails, cmd.getWait());
        if (result.first()) {
            return new RebootAnswer(cmd, null, true);
        } else {
            return new RebootAnswer(cmd, result.second(), false);
        }
    }

    @Override
    public StopAnswer expungeInstance(String extensionName, StopCommand cmd) {
        String extensionPath = getExtensionCheckedScriptPath(extensionName);
        if (StringUtils.isEmpty(extensionPath)) {
            return new StopAnswer(cmd, "Extension not configured", false);
        }
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        UserVmVO userVm = _uservmDao.findById(virtualMachineTO.getId());
        String vmUUID = userVm.getUuid();
        logger.debug("Executing stop command in the external system for the VM {}", vmUUID);

        String prepareExternalScript = Script.findScript("", extensionPath);
        HostVO host = hostDao.findById(userVm.getLastHostId());
        Map<String, String> accessDetails = loadAccessDetails(cmd.getDetails(), virtualMachineTO);

        Pair<Boolean, String> result = deleteInstanceOnExternalSystem(prepareExternalScript, vmUUID, accessDetails, cmd.getWait());
        if (result.first()) {
            return new StopAnswer(cmd, null, true);
        } else {
            return new StopAnswer(cmd, result.second(), false);
        }
    }

    @Override
    public PostExternalProvisioningAnswer postSetupInstance(String extensionName, PostExternalProvisioningCommand cmd) {
        return new PostExternalProvisioningAnswer(cmd, null, null);
    }

    @Override
    public Map<String, HostVmStateReportEntry> getHostVmStateReport(String extensionName, long hostId) {
        final Map<String, HostVmStateReportEntry> vmStates = new HashMap<>();
        String extensionPath = getExtensionCheckedScriptPath(extensionName);
        if (StringUtils.isEmpty(extensionPath)) {
            return vmStates;
        }
        HostVO host = hostDao.findById(hostId);
        if (host == null) {
            logger.error("Host with ID: {} not found", hostId);
            return vmStates;
        }
        hostDao.loadDetails(host);
        Map<String, String> accessDetails = new HashMap<>();
        ExternalHypervisorGuru.loadExternalResourceAccessDetails(host.getDetails(), accessDetails);
        List<UserVmVO> allVms = _uservmDao.listByHostId(hostId);
        allVms.addAll(_uservmDao.listByLastHostId(hostId));
        if (CollectionUtils.isEmpty(allVms)) {
            logger.debug("No VMs found for the {}", host);
            return vmStates;
        }
        for (UserVmVO vm: allVms) {
            VirtualMachine.PowerState powerState = getVmPowerState(vm, accessDetails, extensionPath);
            vmStates.put(vm.getInstanceName(), new HostVmStateReportEntry(powerState, "host-" + hostId));
        }
        return vmStates;
    }

    @Override
    public RunCustomActionAnswer runCustomAction(String extensionName, RunCustomActionCommand cmd) {
        String extensionPath = getExtensionCheckedScriptPath(extensionName);
        if (StringUtils.isEmpty(extensionPath)) {
            return new RunCustomActionAnswer(cmd, false, "Extension not configured");
        }
        logger.debug("Executing custom action '{}' in the external provisioner", cmd.getActionName());

        String actionName = cmd.getActionName();
        Map<String, String> externalDetails = cmd.getExternalDetails();

        logger.debug("Executing custom action '{}' in the external system", actionName);

        String prepareExternalScript = Script.findScript("", extensionPath);
        Map<String, String> accessDetails = loadAccessDetails(externalDetails, null);

        Pair<Boolean, String> result = runCustomActionOnExternalSystem(prepareExternalScript, actionName, accessDetails, cmd.getWait());
        return new RunCustomActionAnswer(cmd, result.first(), result.second());
    }

    @Override
    public void prepareScripts(String extensionName) {
        String destinationPath = getExtensionScriptPath(extensionName);
        File destinationFile = new File(destinationPath);
        if (destinationFile.exists()) {
            logger.info("File already exists at {}, skipping copy.", destinationPath);
            return;
        }
        CloudRuntimeException exception =
                new CloudRuntimeException(String.format("Failed to prepare scripts for extension: %s", extensionName));
        if (!checkExtensionsDirectory()) {
            throw exception;
        }
        Path sourcePath = null;
        String sourceScriptPath = Script.findScript("", BASE_EXTERNAL_PROVISIONER_SCRIPT);
        if (sourceScriptPath != null) {
            sourcePath = Paths.get(sourceScriptPath);
        }
        if (sourcePath == null) {
            logger.error("Failed to find base script for preparing extension: {}",
                    extensionName);
            throw exception;
        }
        Path destinationPathObj = Paths.get(destinationPath);
        Path destinationDirPath = destinationPathObj.getParent();
        if (destinationDirPath == null) {
            logger.error("Failed to find parent directory for extension: {} script path {}",
                    extensionName, destinationPath);
            throw exception;
        }
        try {
            Files.createDirectories(destinationDirPath);
        } catch (IOException e) {
            logger.error("Failed to create directory: {} for extension: {}", destinationDirPath,
                    extensionName, e);
            throw exception;
        }
        try {
            Files.copy(sourcePath, destinationPathObj, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Failed to copy script from {} to {}", sourcePath, destinationPath, e);
            throw exception;
        }
        logger.debug("Successfully copied prepared script [{}] for extension: {}", destinationPath,
                extensionName);
    }

    public Pair<Boolean, String> runCustomActionOnExternalSystem(String filename, String actionName, Map<String, String> accessDetails, int wait) {
        return executeExternalCommand(filename, actionName, accessDetails, wait,
                String.format("Failed to execute custom action '%s' on external system", actionName));
    }

    private VirtualMachine.PowerState getVmPowerState(UserVmVO userVmVO, Map<String, String> accessDetails, String extensionPath) {
        ExternalHypervisorGuru.loadExternalResourceAccessDetails(userVmVO.getDetails(), accessDetails);
        final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(Hypervisor.HypervisorType.External);
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(userVmVO);
        VirtualMachineTO virtualMachineTO = hvGuru.implement(profile);

        Map<String, String> modifiedDetails = loadAccessDetails(accessDetails, virtualMachineTO);

        String vmUUID = userVmVO.getUuid();
        logger.debug("Trying to get VM power status from the external system for the VM {}", vmUUID);

        String prepareExternalScript = Script.findScript("", extensionPath);

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
            logger.debug("Exception occurred while trying to fetch the power status of the {} : {}", userVmVO, result.second());
            return VirtualMachine.PowerState.PowerUnknown;
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
                logger.debug("{}: External API execution failed with result {}", logPrefix, result);
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
        return objectMapper.writeValueAsString(details);
    }

    private String getVirtualMachineTOJsonString(VirtualMachineTO vmTO) {
        Gson s_gogger = GsonHelper.getGsonLogger();
        return s_gogger.toJson(vmTO);
    }

    @Override
    public List<Class<?>> getCommands() {
        return new ArrayList<>();
    }

    @Override
    public String getConfigComponentName() {
        return SimpleExternalProvisioner.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {
                ExtensionsDirectory
        };
    }
}
