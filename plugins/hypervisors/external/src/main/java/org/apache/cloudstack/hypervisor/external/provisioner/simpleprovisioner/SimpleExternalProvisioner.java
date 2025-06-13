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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.utils.security.DigestHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthCommand;
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
import com.cloud.utils.FileUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

public class SimpleExternalProvisioner extends ManagerBase implements ExternalProvisioner, PluggableService, Configurable {

    ConfigKey<String> ExtensionsDirectory = new ConfigKey<>("Advanced", String.class,
            "external.provisioner.extensions.directory", "/etc/cloudstack/extensions",
            "Directory on the management server where extensions are present",
            false, ConfigKey.Scope.Global);

    public static final String BASE_EXTERNAL_PROVISIONER_SCRIPTS_DIR = "scripts/vm/hypervisor/external/simpleExternalProvisioner";
    public static final String BASE_EXTERNAL_PROVISIONER_SCRIPT = BASE_EXTERNAL_PROVISIONER_SCRIPTS_DIR + "/provisioner.sh";

    private static final boolean IS_DEBUG = true;

    @Inject
    UserVmDao _uservmDao;

    @Inject
    HostDao hostDao;

    @Inject
    VMInstanceDao vmInstanceDao;

    @Inject
    HypervisorGuruManager hypervisorGuruManager;

    @Override
    public String getName() {
        return "simpleExternalProvisioner";
    }

    @Override
    public String getDescription() {
        return "Simple external provisioner";
    }

    private String defaultExtensionsDirectory = "/etc/cloudstack/extensions";
    private String extensionsDirectory;

    protected Map<String, Object> loadAccessDetails(Map<String, Object> externalDetails, VirtualMachineTO virtualMachineTO) {
        Map<String, Object> modifiedDetails = new HashMap<>();
        if (MapUtils.isNotEmpty(externalDetails)) {
            modifiedDetails.put(ApiConstants.EXTERNAL, externalDetails);
        }
        if (virtualMachineTO != null) {
            modifiedDetails.put(ApiConstants.VIRTUAL_MACHINE_ID, virtualMachineTO.getUuid());
            modifiedDetails.put(ApiConstants.VIRTUAL_MACHINE_NAME, virtualMachineTO.getName());
            modifiedDetails.put(VmDetailConstants.CLOUDSTACK_VM_DETAILS, virtualMachineTO);
        }
        logger.debug("Using these access details for VM instance operation: {}", modifiedDetails);
        return modifiedDetails;
    }

    protected String getExtensionCheckedEntryPointPath(String extensionName, String extensionRelativeEntryPoint) {
        String path = getExtensionEntryPoint(extensionRelativeEntryPoint);
        File file = new File(path);
        String errorSuffix = String.format("Entry point [%s] for extension: %s", path, extensionName);
        if (!file.exists()) {
            logger.error("{} does not exist", errorSuffix);
            return null;
        }
        if (!file.isFile()) {
            logger.error("{} is not a file", errorSuffix);
            return null;
        }
        if (!file.canRead()) {
            logger.error("{} is not readable", errorSuffix);
            return null;
        }
        if (!file.canExecute()) {
            logger.error("{} is not executable", errorSuffix);
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

        final File extensionsPropertiesFile = PropertiesUtil.findConfigFile("server.properties");

        if (extensionsPropertiesFile == null) {
            logger.debug("extensions.properties file not found, using default extensions directory");
            extensionsDirectory = defaultExtensionsDirectory;
        } else {
            Properties properties = new Properties();
            try (FileInputStream fis = new FileInputStream(extensionsPropertiesFile)) {
                properties.load(fis);
                extensionsDirectory = properties.getProperty("extensions.file.path");
                logger.debug("Loaded extensions directory from properties file: {}", extensionsDirectory);

                if (StringUtils.isBlank(extensionsDirectory)) {
                    logger.warn("extensions.file.path property is blank in extensions.properties, using default");
                    extensionsDirectory = defaultExtensionsDirectory;
                }
            } catch (IOException e) {
                logger.warn("Failed to load extensions.properties file, falling back to default", e);
                extensionsDirectory = defaultExtensionsDirectory;
            }
        }

        checkExtensionsDirectory();
        return true;
    }

    @Override
    public String getExtensionEntryPoint(String relativeEntryPoint) {
        return String.format("%s%s%s", extensionsDirectory, File.separator, relativeEntryPoint);
    }

    @Override
    public String getChecksumForExtensionEntryPoint(String extensionName, String relativeEntryPoint) {
        String entryPoint = getExtensionCheckedEntryPointPath(extensionName, relativeEntryPoint);
        if (StringUtils.isBlank(entryPoint)) {
            return null;
        }
        try {
            return DigestHelper.calculateChecksum(new File(entryPoint));
        } catch (CloudRuntimeException ignored) {
            return null;
        }
    }

    @Override
    public PrepareExternalProvisioningAnswer prepareExternalProvisioning(String hostGuid,
                 String extensionName, String extensionRelativeEntryPoint, PrepareExternalProvisioningCommand cmd) {
        String extensionPath = getExtensionCheckedEntryPointPath(extensionName, extensionRelativeEntryPoint);
        if (StringUtils.isEmpty(extensionPath)) {
            return new PrepareExternalProvisioningAnswer(cmd, "Extension not configured", false);
        }
        VirtualMachineTO vmTO = cmd.getVirtualMachineTO();
        String vmUUID = vmTO.getUuid();
        logger.debug("Executing PrepareExternalProvisioningCommand in the external provisioner " +
                "for the VM {} as part of VM deployment", vmUUID);

        String prepareExternalScript = Script.findScript("", extensionPath);
        Map<String, Object> accessDetails = loadAccessDetails(cmd.getExternalDetails(), vmTO);
        Pair<Boolean, String> result = prepareExternalProvisioningInternal(prepareExternalScript, vmUUID, accessDetails, cmd.getWait());
        String output = result.second();
        if (!result.first()) {
            return new PrepareExternalProvisioningAnswer(cmd, output, false);
        }
        if (StringUtils.isEmpty(output)) {
            return new PrepareExternalProvisioningAnswer(cmd, "", true);
        }
        Map<String, String> resultMap = null;
        try {
            resultMap = StringUtils.parseJsonToMap(output);
        } catch (CloudRuntimeException e) {
            logger.warn("Failed to parse the output from preparing external provisioning operation as part of VM deployment");
        }
        return new PrepareExternalProvisioningAnswer(cmd, resultMap, null);
    }

    @Override
    public StartAnswer startInstance(String hostGuid, String extensionName, String extensionRelativeEntryPoint, StartCommand cmd) {
        String extensionPath = getExtensionCheckedEntryPointPath(extensionName, extensionRelativeEntryPoint);
        if (StringUtils.isEmpty(extensionPath)) {
            return new StartAnswer(cmd, "Extension not configured");
        }
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        Map<String, Object> accessDetails = loadAccessDetails(cmd.getExternalDetails(), virtualMachineTO);
        String vmUUID = virtualMachineTO.getUuid();

        logger.debug(String.format("Executing StartCommand in the external provisioner for VM %s", vmUUID));

        Object deployvm = accessDetails.get("deployvm");
        boolean isDeploy = (deployvm != null && Boolean.parseBoolean((String)deployvm));
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

    private Pair<Boolean, String> executeStartCommandOnExternalSystem(boolean isDeploy, String filename, String vmUUID, Map<String, Object> accessDetails, int wait) {
        if (isDeploy) {
            return deployInstanceOnExternalSystem(filename, vmUUID, accessDetails, wait);
        } else {
            return startInstanceOnExternalSystem(filename, vmUUID, accessDetails, wait);
        }
    }

    @Override
    public StopAnswer stopInstance(String hostGuid, String extensionName, String extensionRelativeEntryPoint, StopCommand cmd) {
        String extensionPath = getExtensionCheckedEntryPointPath(extensionName, extensionRelativeEntryPoint);
        if (StringUtils.isEmpty(extensionPath)) {
            return new StopAnswer(cmd, "Extension not configured", false);
        }
        logger.debug("Executing stop command on the external provisioner");
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        String vmUUID = cmd.getVirtualMachine().getUuid();
        logger.debug("Executing stop command in the external system for the VM {}", vmUUID);

        String prepareExternalScript = Script.findScript("", extensionPath);
        Map<String, Object> accessDetails = loadAccessDetails(cmd.getExternalDetails(), virtualMachineTO);

        Pair<Boolean, String> result = stopInstanceOnExternalSystem(prepareExternalScript, vmUUID, accessDetails, cmd.getWait());
        if (result.first()) {
            return new StopAnswer(cmd, null, true);
        } else {
            return new StopAnswer(cmd, result.second(), false);
        }
    }

    @Override
    public RebootAnswer rebootInstance(String hostGuid, String extensionName, String extensionRelativeEntryPoint, RebootCommand cmd) {
        String extensionPath = getExtensionCheckedEntryPointPath(extensionName, extensionRelativeEntryPoint);
        if (StringUtils.isEmpty(extensionPath)) {
            return new RebootAnswer(cmd, "Extension not configured", false);
        }
        logger.debug("Executing reboot command using IPMI in the external provisioner");
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        String vmUUID = virtualMachineTO.getUuid();
        logger.debug("Executing reboot command in the external system for the VM {}", vmUUID);

        String prepareExternalScript = Script.findScript("", extensionPath);
        Map<String, Object> accessDetails = loadAccessDetails(cmd.getExternalDetails(), virtualMachineTO);

        Pair<Boolean, String> result = rebootInstanceOnExternalSystem(prepareExternalScript, vmUUID, accessDetails, cmd.getWait());
        if (result.first()) {
            return new RebootAnswer(cmd, null, true);
        } else {
            return new RebootAnswer(cmd, result.second(), false);
        }
    }

    @Override
    public StopAnswer expungeInstance(String hostGuid, String extensionName, String extensionRelativeEntryPoint, StopCommand cmd) {
        String extensionPath = getExtensionCheckedEntryPointPath(extensionName, extensionRelativeEntryPoint);
        if (StringUtils.isEmpty(extensionPath)) {
            return new StopAnswer(cmd, "Extension not configured", false);
        }
        VirtualMachineTO virtualMachineTO = cmd.getVirtualMachine();
        String vmUUID = virtualMachineTO.getUuid();
        logger.debug("Executing stop command as part of expunge in the external system for the VM {}", vmUUID);

        String prepareExternalScript = Script.findScript("", extensionPath);
        Map<String, Object> accessDetails = loadAccessDetails(cmd.getExternalDetails(), virtualMachineTO);

        Pair<Boolean, String> result = deleteInstanceOnExternalSystem(prepareExternalScript, vmUUID, accessDetails, cmd.getWait());
        if (result.first()) {
            return new StopAnswer(cmd, null, true);
        } else {
            return new StopAnswer(cmd, result.second(), false);
        }
    }

    @Override
    public PostExternalProvisioningAnswer postSetupInstance(String hostGuid, String extensionName, String extensionRelativeEntryPoint, PostExternalProvisioningCommand cmd) {
        return new PostExternalProvisioningAnswer(cmd, null, null);
    }

    @Override
    public Map<String, HostVmStateReportEntry> getHostVmStateReport(long hostId, String extensionName, String extensionRelativeEntryPoint) {
        final Map<String, HostVmStateReportEntry> vmStates = new HashMap<>();
        String extensionPath = getExtensionCheckedEntryPointPath(extensionName, extensionRelativeEntryPoint);
        if (StringUtils.isEmpty(extensionPath)) {
            return vmStates;
        }
        HostVO host = hostDao.findById(hostId);
        if (host == null) {
            logger.error("Host with ID: {} not found", hostId);
            return vmStates;
        }
        // ToDo: ExternalHypervisorGuru.loadExternalResourceAccessDetails(userVmVO.getDetails(), accessDetails);
        Map<String, Object> accessDetails = new HashMap<>();
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
    public RunCustomActionAnswer runCustomAction(String hostGuid, String extensionName, String extensionRelativeEntryPoint, RunCustomActionCommand cmd) {
        String extensionPath = getExtensionCheckedEntryPointPath(extensionName, extensionRelativeEntryPoint);
        if (StringUtils.isEmpty(extensionPath)) {
            return new RunCustomActionAnswer(cmd, false, "Extension not configured");
        }
        final String actionName = cmd.getActionName();
        final Map<String, Object> parameters = cmd.getParameters();
        final Map<String, Object> externalDetails = cmd.getExternalDetails();
        logger.debug("Executing custom action '{}' in the external provisioner", actionName);
        VirtualMachineTO virtualMachineTO = null;
        if (cmd.getVmId() != null) {
            VMInstanceVO vm = vmInstanceDao.findById(cmd.getVmId());
            final HypervisorGuru hvGuru = hypervisorGuruManager.getGuru(Hypervisor.HypervisorType.External);
            VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
            virtualMachineTO = hvGuru.implement(profile);
        }

        logger.debug("Executing custom action '{}' in the external system", actionName);
        String prepareExternalScript = Script.findScript("", extensionPath);
        Map<String, Object> accessDetails = loadAccessDetails(externalDetails, virtualMachineTO);
        accessDetails.put(ApiConstants.ACTION, actionName);
        if (MapUtils.isNotEmpty(parameters)) {
            accessDetails.put(ApiConstants.PARAMETERS, GsonHelper.getGson().toJson(parameters));
        }
        Pair<Boolean, String> result = runCustomActionOnExternalSystem(prepareExternalScript, actionName, accessDetails, cmd.getWait());
        return new RunCustomActionAnswer(cmd, result.first(), result.second());
    }

    protected boolean createEntryPoint(String extensionName, boolean userDefined, String extensionRelativeEntryPoint,
               Path destinationPathObj) throws IOException {
        String baseEntryPointPath = BASE_EXTERNAL_PROVISIONER_SCRIPT;
        if (!userDefined) {
            String fileName = Paths.get(extensionRelativeEntryPoint).getFileName().toString();
            baseEntryPointPath = BASE_EXTERNAL_PROVISIONER_SCRIPTS_DIR + File.separator + fileName;
        }
        String sourceScriptPath = Script.findScript("", baseEntryPointPath);
        if(sourceScriptPath == null) {
            logger.error("Failed to find base script for preparing extension: {}",
                    extensionName);
            return false;
        }
        Path sourcePath = Paths.get(sourceScriptPath);
        Files.copy(sourcePath, destinationPathObj, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    @Override
    public void prepareExtensionEntryPoint(String extensionName, boolean userDefined, String extensionRelativeEntryPoint) {
        logger.debug("Preparing entry point for extension: {}, user-defined: {}", extensionName, userDefined);
        String destinationPath = getExtensionEntryPoint(extensionRelativeEntryPoint);
        if (userDefined && !destinationPath.endsWith(".sh")) {
            logger.info("File {} for extension: {} is not a bash script, skipping copy.", destinationPath,
                    extensionName);
            return;
        }
        File destinationFile = new File(destinationPath);
        if (destinationFile.exists()) {
            logger.info("File already exists at {} for extension: {}, skipping copy.", destinationPath,
                    extensionName);
            return;
        }
        CloudRuntimeException exception =
                new CloudRuntimeException(String.format("Failed to prepare scripts for extension: %s", extensionName));
        if (!checkExtensionsDirectory()) {
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
            if (!createEntryPoint(extensionName, userDefined, extensionRelativeEntryPoint, destinationPathObj)) {
                throw exception;
            }
        } catch (IOException e) {
            logger.error("Failed to copy entry point file to [{}] for extension: {}", destinationPath, extensionName, e);
            throw exception;
        }
        logger.debug("Successfully prepared entry point [{}] for extension: {}", destinationPath,
                extensionName);
    }

    @Override
    public void cleanupExtensionEntryPoint(String extensionName, String extensionRelativeEntryPoint) {
        String normalizedPath = extensionRelativeEntryPoint;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        try {
            Path rootPath = Paths.get(extensionsDirectory).toAbsolutePath().normalize();
            String extensionDirName = Extension.getDirectoryName(extensionName);
            Path filePath = rootPath
                    .resolve(normalizedPath.startsWith(extensionDirName) ? extensionDirName : normalizedPath)
                    .normalize();
            if (!Files.isDirectory(filePath) && !Files.isRegularFile(filePath)) {
                throw new CloudRuntimeException(
                        String.format("Failed to cleanup extension entry-point: %s for extension: %s as it either does not exist or is not a regular file/directory",
                                extensionName, extensionRelativeEntryPoint));
            }
            if (!FileUtil.deleteRecursively(filePath)) {
                throw new CloudRuntimeException(
                        String.format("Failed to delete extension entry-point: %s for extension: %s",
                                extensionName, filePath));
            }
        } catch (IOException e) {
            throw new CloudRuntimeException(
                    String.format("Failed to cleanup extension entry-point: %s for extension: %s due to: %s",
                            extensionName, normalizedPath, e.getMessage()), e);
        }
    }

    public Pair<Boolean, String> runCustomActionOnExternalSystem(String filename, String actionName, Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(filename, actionName, accessDetails, wait,
                String.format("Failed to execute custom action '%s' on external system", actionName));
    }

    private VirtualMachine.PowerState getVmPowerState(UserVmVO userVmVO, Map<String, Object> accessDetails, String extensionPath) {
        // ToDo: ExternalHypervisorGuru.loadExternalResourceAccessDetails(userVmVO.getDetails(), accessDetails);
        final HypervisorGuru hvGuru = hypervisorGuruManager.getGuru(Hypervisor.HypervisorType.External);
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(userVmVO);
        VirtualMachineTO virtualMachineTO = hvGuru.implement(profile);

        Map<String, Object> modifiedDetails = loadAccessDetails(accessDetails, virtualMachineTO);

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

    public Pair<Boolean, String> prepareExternalProvisioningInternal(String filename, String vmUUID, Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(filename, "prepare", accessDetails, wait,
                String.format("Failed to prepare external provisioner for deploying VM %s on external system", vmUUID));
    }

    public Pair<Boolean, String> deployInstanceOnExternalSystem(String filename, String vmUUID, Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(filename, "create", accessDetails, wait,
                String.format("Failed to create the instance %s on external system", vmUUID));
    }

    public Pair<Boolean, String> startInstanceOnExternalSystem(String filename, String vmUUID, Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(filename, "start", accessDetails, wait,
                String.format("Failed to start the instance %s on external system", vmUUID));
    }

    public Pair<Boolean, String> stopInstanceOnExternalSystem(String filename, String vmUUID, Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(filename, "stop", accessDetails, wait,
                String.format("Failed to stop the instance %s on external system", vmUUID));
    }

    public Pair<Boolean, String> rebootInstanceOnExternalSystem(String filename, String vmUUID, Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(filename, "reboot", accessDetails, wait,
                String.format("Failed to reboot the instance %s on external system", vmUUID));
    }

    public Pair<Boolean, String> deleteInstanceOnExternalSystem(String filename, String vmUUID, Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(filename, "delete", accessDetails, wait,
                String.format("Failed to delete the instance %s on external system", vmUUID));
    }

    public Pair<Boolean, String> getInstanceStatusOnExternalSystem(String filename, String vmUUID, Map<String, Object> accessDetails, int wait) {
        return executeExternalCommand(filename, "status", accessDetails, wait,
                String.format("Failed to get the instance power status %s on external system", vmUUID));
    }

    public Pair<Boolean, String> executeExternalCommand(String file, String action, Map<String, Object> accessDetails, int wait, String errorLogPrefix) {
        try {
            Path executablePath = Paths.get(file).toAbsolutePath().normalize();
            if (!Files.isExecutable(executablePath)) {
                logger.error("{}: File is not executable: {}", errorLogPrefix, executablePath);
                return new Pair<>(false, "File is not executable");
            }
            List<String> command = new ArrayList<>();
            command.add(executablePath.toString());
            command.add(action);
            String dataFile = prepareActionData(accessDetails);
            command.add(dataFile);
            command.add(Integer.toString(wait));
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);

            if (IS_DEBUG) {
                logger.debug("Executable: {}",executablePath);
                logger.debug("Action: {} with wait: {}", action, wait);
                logger.debug("Data file: {}", dataFile);
//                return new Pair<>(true, "Operation successful!");
            }
            logger.debug("Executing command: {}", action);
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
            return new Pair<>(true, output.toString().trim());

        } catch (IOException | InterruptedException e) {
            logger.error("{}: External operation failed", errorLogPrefix, e);
            throw new CloudRuntimeException(String.format("%s: External operation failed", errorLogPrefix), e);
        }
    }

    @Override
    public Answer checkHealth(String hostGuid, String extensionName, String extensionRelativeEntryPoint, CheckHealthCommand cmd) {
        String extensionPath = getExtensionCheckedEntryPointPath(extensionName, extensionRelativeEntryPoint);
        if (StringUtils.isEmpty(extensionPath)) {
            return new Answer(cmd, false, "Extension not configured");
        }
        // ToDo: should we check with provisioner script?
        return new Answer(cmd);
    }

    private String prepareActionData(Map<String, Object> details) throws IOException {
        // ToDo: some mechanism to clean up these data files
        String json = GsonHelper.getGson().toJson(details);
        logger.debug("Data: {}", json);
        long epochMillis = System.currentTimeMillis();
        String fileName = epochMillis + ".json";
        Path tempDir = Files.createTempDirectory("orchestrator");
        Path tempFile = tempDir.resolve(fileName);
        Files.writeString(tempFile, json, StandardOpenOption.CREATE_NEW);
        return tempFile.toAbsolutePath().toString();
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
