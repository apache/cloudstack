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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.cloudstack.utils.security.DigestHelper;
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
import com.cloud.utils.FileUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.json.JsonMergeUtil;
import com.cloud.utils.script.Script;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDao;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ExternalPathPayloadProvisioner extends ManagerBase implements ExternalProvisioner, PluggableService {

    public static final String BASE_EXTERNAL_PROVISIONER_SCRIPTS_DIR = "scripts/vm/hypervisor/external/provisioner";
    public static final String BASE_EXTERNAL_PROVISIONER_SHELL_SCRIPT =
            BASE_EXTERNAL_PROVISIONER_SCRIPTS_DIR + "/provisioner.sh";

    private static final String PROPERTIES_FILE = "server.properties";
    private static final String EXTENSIONS_DEPLOYMENT_MODE_NAME = "extensions.deployment.mode";
    private static final String EXTENSIONS_DIRECTORY_PROD = "/usr/share/cloudstack-management/extensions";
    private static final String EXTENSIONS_DATA_DIRECTORY_PROD = "/var/lib/cloudstack/management/extensions";
    private static final String EXTENSIONS_DIRECTORY_DEV = "extensions";
    private static final String EXTENSIONS_DATA_DIRECTORY_DEV = "client/target/extensions-data";

    @Inject
    UserVmDao _uservmDao;

    @Inject
    HostDao hostDao;

    @Inject
    HypervisorGuruManager hypervisorGuruManager;

    @Inject
    ExtensionsManager extensionsManager;

    private static final AtomicReference<Properties> propertiesRef = new AtomicReference<>();
    private String extensionsDirectory;
    private String extensionsDataDirectory;
    private ExecutorService payloadCleanupExecutor;
    private ScheduledExecutorService payloadCleanupScheduler;
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

    protected String getExtensionCheckedPath(String extensionName, String extensionRelativePath) {
        String path = getExtensionPath(extensionRelativePath);
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
            logger.error("Extension directory [{}] is not properly set up. It must exist, be a directory, and be writeable",
                    dir.getAbsolutePath());
            return false;
        }
        if (!extensionsDirectory.equals(dir.getAbsolutePath())) {
            extensionsDirectory = dir.getAbsolutePath();
        }
        logger.info("Extensions directory path: {}", extensionsDirectory);
        return true;
    }

    protected void createOrCheckExtensionsDataDirectory() throws ConfigurationException {
        File dir = new File(extensionsDataDirectory);
        if (!dir.exists()) {
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException e) {
                logger.error("Unable to create extensions data directory [{}]", dir.getAbsolutePath(), e);
                throw new ConfigurationException("Unable to create extensions data directory path");
            }
        }
        if (!dir.isDirectory() || !dir.canWrite()) {
            logger.error("Extensions data directory [{}] is not properly set up. It must exist, be a directory, and be writeable",
                    dir.getAbsolutePath());
            throw new ConfigurationException("Extensions data directory path is not accessible");
        }
        extensionsDataDirectory = dir.getAbsolutePath();
        logger.info("Extensions data directory path: {}", extensionsDataDirectory);
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

    private String getServerProperty(String name) {
        Properties props = propertiesRef.get();
        if (props == null) {
            File propsFile = PropertiesUtil.findConfigFile(PROPERTIES_FILE);
            if (propsFile == null) {
                logger.error("{} file not found", PROPERTIES_FILE);
                return null;
            }
            Properties tempProps = new Properties();
            try (FileInputStream is = new FileInputStream(propsFile)) {
                tempProps.load(is);
            } catch (IOException e) {
                logger.error("Error loading {}: {}", PROPERTIES_FILE, e.getMessage(), e);
                return null;
            }
            if (!propertiesRef.compareAndSet(null, tempProps)) {
                tempProps = propertiesRef.get();
            }
            props = tempProps;
        }
        return props.getProperty(name);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        initializeExtensionDirectories();
        checkExtensionsDirectory();
        createOrCheckExtensionsDataDirectory();
        return true;
    }

    private void initializeExtensionDirectories() {
        String deploymentMode = getServerProperty(EXTENSIONS_DEPLOYMENT_MODE_NAME);
        if ("developer".equals(deploymentMode)) {
            extensionsDirectory = EXTENSIONS_DIRECTORY_DEV;
            extensionsDataDirectory = EXTENSIONS_DATA_DIRECTORY_DEV;
        } else {
            extensionsDirectory = EXTENSIONS_DIRECTORY_PROD;
            extensionsDataDirectory = EXTENSIONS_DATA_DIRECTORY_PROD;
        }
    }

    @Override
    public boolean start() {
        payloadCleanupExecutor = Executors.newSingleThreadExecutor();
        payloadCleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        return true;
    }

    @Override
    public boolean stop() {
        payloadCleanupExecutor.shutdown();
        payloadCleanupScheduler.shutdown();
        return true;
    }

    @Override
    public String getExtensionsPath() {
        return extensionsDirectory;
    }

    @Override
    public String getExtensionPath(String relativePath) {
        return String.format("%s%s%s", extensionsDirectory, File.separator, relativePath);
    }

    @Override
    public String getChecksumForExtensionPath(String extensionName, String relativePath) {
        String path = getExtensionCheckedPath(extensionName, relativePath);
        if (StringUtils.isBlank(path)) {
            return null;
        }
        try {
            return DigestHelper.calculateChecksum(new File(path));
        } catch (CloudRuntimeException ignored) {
            return null;
        }
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
        String extensionPath = getExtensionCheckedPath(extensionName, extensionRelativePath);
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
        String extensionPath = getExtensionCheckedPath(extensionName, extensionRelativePath);
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
        String extensionPath = getExtensionCheckedPath(extensionName, extensionRelativePath);
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
        String extensionPath = getExtensionCheckedPath(extensionName, extensionRelativePath);
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
        String extensionPath = getExtensionCheckedPath(extensionName, extensionRelativePath);
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
        String extensionPath = getExtensionCheckedPath(extensionName, extensionRelativePath);
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
        String extensionPath = getExtensionCheckedPath(extensionName, extensionRelativePath);
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
        String extensionPath = getExtensionCheckedPath(extensionName, extensionRelativePath);
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

    protected boolean createExtensionPath(String extensionName, Path destinationPathObj) throws IOException {
        String sourceScriptPath = Script.findScript("", BASE_EXTERNAL_PROVISIONER_SHELL_SCRIPT);
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
    public void prepareExtensionPath(String extensionName, boolean userDefined,
                           String extensionRelativePath) {
        logger.debug("Preparing entry point for Extension [name: {}, user-defined: {}]", extensionName, userDefined);
        if (!userDefined) {
            logger.debug("Skipping preparing entry point for inbuilt extension: {}", extensionName);
            return;
        }
        String destinationPath = getExtensionPath(extensionRelativePath);
        if (!destinationPath.endsWith(".sh")) {
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
            if (!createExtensionPath(extensionName, destinationPathObj)) {
                throw exception;
            }
        } catch (IOException e) {
            logger.error("Failed to copy entry point file to [{}] for extension: {}",
                    destinationPath, extensionName, e);
            throw exception;
        }
        logger.debug("Successfully prepared entry point [{}] for extension: {}", destinationPath,
                extensionName);
    }

    @Override
    public void cleanupExtensionPath(String extensionName, String extensionRelativePath) {
        String normalizedPath = extensionRelativePath;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        try {
            Path rootPath = Paths.get(extensionsDirectory).toAbsolutePath().normalize();
            String extensionDirName = Extension.getDirectoryName(extensionName);
            Path filePath = rootPath
                    .resolve(normalizedPath.startsWith(extensionDirName) ? extensionDirName : normalizedPath)
                    .normalize();
            if (!Files.exists(filePath)) {
                return;
            }
            if (!Files.isDirectory(filePath) && !Files.isRegularFile(filePath)) {
                throw new CloudRuntimeException(
                        String.format("Failed to cleanup extension entry-point: %s for extension: %s as it either " +
                                        "does not exist or is not a regular file/directory",
                                extensionName, extensionRelativePath));
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

    @Override
    public void cleanupExtensionData(String extensionName, int olderThanDays, boolean cleanupDirectory) {
        String extensionPayloadDirPath = extensionsDataDirectory + File.separator + extensionName;
        Path dirPath = Paths.get(extensionPayloadDirPath);
        if (!Files.exists(dirPath)) {
            return;
        }
        try {
            if (cleanupDirectory) {
                try (Stream<Path> paths = Files.walk(dirPath)) {
                    paths.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
                return;
            }
            long cutoffMillis = System.currentTimeMillis() - (olderThanDays * 24L * 60 * 60 * 1000);
            long lastModified = Files.getLastModifiedTime(dirPath).toMillis();
            if (lastModified < cutoffMillis) {
                return;
            }
            try (Stream<Path> paths = Files.walk(dirPath)) {
                paths.filter(path -> !path.equals(dirPath))
                        .filter(path -> {
                            try {
                                return Files.getLastModifiedTime(path).toMillis() < cutoffMillis;
                            } catch (IOException e) {
                                return false;
                            }
                        })
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            logger.warn("Failed to clean up extension payloads for {}: {}", extensionName, e.getMessage());
        }
    }

    public Pair<Boolean, String> runCustomActionOnExternalSystem(String extensionName, String filename,
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
            String dataFile = prepareExternalPayload(extensionName, accessDetails);
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
            return;
        }
        logger.trace("Deleting payload file: {} for extension: {}, action: {}, file: {}",
                payloadFileName, extensionName, action);
        FileUtil.deletePath(payloadFileName);
    }

    protected void scheduleExtensionPayloadDirectoryCleanup(String extensionName) {
        try {
            Future<?> future = payloadCleanupExecutor.submit(() -> {
                try {
                    cleanupExtensionData(extensionName, 1, false);
                    logger.trace("Cleaned up payload directory for extension: {}", extensionName);
                } catch (Exception e) {
                    logger.warn("Exception during payload cleanup for extension: {} due to {}", extensionName,
                            e.getMessage());
                    logger.trace(e);
                }
            });
            payloadCleanupScheduler.schedule(() -> {
                try {
                    if (!future.isDone()) {
                        future.cancel(true);
                        logger.trace("Cancelled cleaning up payload directory for extension: {} as it " +
                                "running for more than 3 seconds", extensionName);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to cancel payload cleanup task for extension: {} due to {}",
                            extensionName, e.getMessage());
                    logger.trace(e);
                }
            }, 3, TimeUnit.SECONDS);
        } catch (RejectedExecutionException e) {
            logger.warn("Payload cleanup task for extension: {} was rejected due to: {}", extensionName,
                    e.getMessage());
            logger.trace(e);
        }
    }

    protected String prepareExternalPayload(String extensionName, Map<String, Object> details) throws IOException {
        String json = GsonHelper.getGson().toJson(details);
        String fileName = UUID.randomUUID() + ".json";
        String extensionPayloadDir = extensionsDataDirectory + File.separator + extensionName;
        Path payloadDirPath = Paths.get(extensionPayloadDir);
        if (!Files.exists(payloadDirPath)) {
            Files.createDirectories(payloadDirPath);
        } else {
            scheduleExtensionPayloadDirectoryCleanup(extensionName);
        }
        Path payloadFile = payloadDirPath.resolve(fileName);
        Files.writeString(payloadFile, json, StandardOpenOption.CREATE_NEW);
        return payloadFile.toAbsolutePath().toString();
    }

    @Override
    public List<Class<?>> getCommands() {
        return new ArrayList<>();
    }
}
