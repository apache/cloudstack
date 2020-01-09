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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.diagnostics.GetDiagnosticsDataCmd;
import org.apache.cloudstack.api.command.admin.diagnostics.RunDiagnosticsCmd;
import org.apache.cloudstack.diagnostics.fileprocessor.DiagnosticsFilesList;
import org.apache.cloudstack.diagnostics.fileprocessor.DiagnosticsFilesListFactory;
import org.apache.cloudstack.diagnostics.to.DiagnosticsDataObject;
import org.apache.cloudstack.diagnostics.to.DiagnosticsDataTO;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.poll.BackgroundPollManager;
import org.apache.cloudstack.poll.BackgroundPollTask;
import org.apache.cloudstack.storage.NfsMountManager;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.DataTO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.server.StatsCollector;
import com.cloud.storage.ImageStoreDetailsUtil;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.base.Strings;

import static org.apache.cloudstack.diagnostics.DiagnosticsHelper.getTimeDifference;
import static org.apache.cloudstack.diagnostics.DiagnosticsHelper.setDirFilePermissions;
import static org.apache.cloudstack.diagnostics.DiagnosticsHelper.umountSecondaryStorage;
import static org.apache.cloudstack.diagnostics.fileprocessor.DiagnosticsFilesList.CpvmDefaultSupportedFiles;
import static org.apache.cloudstack.diagnostics.fileprocessor.DiagnosticsFilesList.SsvmDefaultSupportedFiles;
import static org.apache.cloudstack.diagnostics.fileprocessor.DiagnosticsFilesList.VrDefaultSupportedFiles;

public class DiagnosticsServiceImpl extends ManagerBase implements PluggableService, DiagnosticsService, Configurable {
    private static final Logger LOGGER = Logger.getLogger(DiagnosticsServiceImpl.class);

    @Inject
    private AgentManager agentManager;
    @Inject
    private VMInstanceDao instanceDao;
    @Inject
    private VirtualMachineManager vmManager;
    @Inject
    private NetworkOrchestrationService networkManager;
    @Inject
    private StatsCollector statsCollector;
    @Inject
    private DataStoreManager storeMgr;
    @Inject
    private BackgroundPollManager backgroundPollManager;
    @Inject
    private ImageStoreDetailsUtil imageStoreDetailsUtil;
    @Inject
    private NfsMountManager mountManager;
    @Inject
    private DataCenterDao dataCenterDao;

    // This 2 settings should require a restart of the management server?
    private static final ConfigKey<Boolean> EnableGarbageCollector = new ConfigKey<>("Advanced", Boolean.class,
            "diagnostics.data.gc.enable", "true", "enable the diagnostics data files garbage collector", true);
    private static final ConfigKey<Integer> GarbageCollectionInterval = new ConfigKey<>("Advanced", Integer.class,
            "diagnostics.data.gc.interval", "86400", "garbage collection interval in seconds", true);

    // These are easily computed properties and need not need a restart of the management server
    private static final ConfigKey<Long> DataRetrievalTimeout = new ConfigKey<>("Advanced", Long.class,
            "diagnostics.data.retrieval.timeout", "3600", "overall data retrieval timeout in seconds", true);
    private static final ConfigKey<Long> MaximumFileAgeforGarbageCollection = new ConfigKey<>("Advanced", Long.class,
            "diagnostics.data.max.file.age", "86400", "maximum file age for garbage collection in seconds", true);
    private static final ConfigKey<Double> DiskQuotaPercentageThreshold = new ConfigKey<>("Advanced", Double.class,
            "diagnostics.data.disable.threshold", "0.95", "Minimum disk space percentage to initiate diagnostics file retrieval", true);

    private final static String DIAGNOSTICS_DATA_DIRECTORY = "diagnostics_data";

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SYSTEM_VM_DIAGNOSTICS, eventDescription = "running diagnostics on system vm", async = true)
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

        Answer answer = agentManager.easySend(hostId, command);

        if (answer != null) {
            detailsMap = ((DiagnosticsAnswer) answer).getExecutionDetails();
            return detailsMap;
        } else {
            throw new CloudRuntimeException("Failed to execute diagnostics command on remote host: " + vmInstance.getHostName());
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
    @ActionEvent(eventType = EventTypes.EVENT_SYSTEM_VM_DIAGNOSTICS, eventDescription = "getting diagnostics files on system vm", async = true)
    public String getDiagnosticsDataCommand(GetDiagnosticsDataCmd cmd) {
        Long vmId = cmd.getId();
        List<String> optionalFilesList = cmd.getFilesList();
        VMInstanceVO vmInstance = getSystemVMInstance(vmId);
        long zoneId = vmInstance.getDataCenterId();

        List<String> fileList = getFileListToBeRetrieved(optionalFilesList, vmInstance);

        if (CollectionUtils.isEmpty(fileList)) {
            throw new CloudRuntimeException("Failed to generate diagnostics file list for retrieval.");
        }

        Long vmHostId = vmInstance.getHostId();

        // Find Secondary Storage with enough Disk Quota in the current Zone
        final DataStore store = getImageStore(vmInstance.getDataCenterId());

        Answer zipFilesAnswer = zipDiagnosticsFilesInSystemVm(vmInstance, fileList);

        if (zipFilesAnswer == null) {
            throw new CloudRuntimeException(String.format("Failed to generate diagnostics zip file in VM %s", vmInstance.getUuid()));
        }

        if (!zipFilesAnswer.getResult()) {
            throw new CloudRuntimeException(String.format("Failed to generate diagnostics zip file file in VM %s due to %s", vmInstance.getUuid(), zipFilesAnswer.getDetails()));
        }

        // Copy zip file from system VM to secondary storage
        String zipFileInSystemVm = zipFilesAnswer.getDetails().replace("\n", "");
        Pair<Boolean, String> copyToSecondaryStorageResults = copyZipFileToSecondaryStorage(vmInstance, vmHostId, zipFileInSystemVm, store);

        // Send cleanup zip file cleanup command to system VM
        Answer fileCleanupAnswer = deleteDiagnosticsZipFileInsystemVm(vmInstance, zipFileInSystemVm);
        if (fileCleanupAnswer == null) {
            LOGGER.error(String.format("Failed to cleanup diagnostics zip file on vm: %s", vmInstance.getUuid()));
        } else {
            if (!fileCleanupAnswer.getResult()) {
                LOGGER.error(String.format("Zip file cleanup for vm %s has failed with: %s", vmInstance.getUuid(), fileCleanupAnswer.getDetails()));
            }
        }

        if (!copyToSecondaryStorageResults.first()) {
            throw new CloudRuntimeException(String.format("Failed to copy %s to secondary storage %s due to %s.", zipFileInSystemVm, store.getUri(), copyToSecondaryStorageResults.second()));
        }

        // Now we need to create the file download URL
        // Find ssvm of store
        VMInstanceVO ssvm = getSecondaryStorageVmInZone(zoneId);
        if (ssvm == null) {
            throw new CloudRuntimeException("No ssvm found in Zone with ID: " + zoneId);
        }
        // Secondary Storage install path = "diagnostics_data/diagnostics_files_xxxx.tar
        String installPath = DIAGNOSTICS_DATA_DIRECTORY + "/" + zipFileInSystemVm.replace("/root", "");
        return createFileDownloadUrl(store, ssvm.getHypervisorType(), installPath);
    }

    /**
     * Copy retrieved diagnostics zip file from system vm to secondary storage
     * For VMware use the mgmt server, and for Xen/KVM use the hyperhost of the target VM
     * The strategy is to mount secondary storage on mgmt server or host and scp directly to /mnt/SecStorage/diagnostics_data
     *
     * @param fileToCopy zip file in system vm to be copied
     * @param store      secondary storage to copy zip file to
     */
    private Pair<Boolean, String> copyZipFileToSecondaryStorage(VMInstanceVO vmInstance, Long vmHostId, String fileToCopy, DataStore store) {
        String vmControlIp = getVMSshIp(vmInstance);
        if (StringUtils.isBlank(vmControlIp)) {
            return new Pair<>(false, "Unable to find system vm ssh/control IP for  vm with ID: " + vmInstance.getId());
        }
        Pair<Boolean, String> copyResult;
        if (vmInstance.getHypervisorType() == Hypervisor.HypervisorType.VMware) {
            copyResult = orchestrateCopyToSecondaryStorageVMware(store, vmControlIp, fileToCopy);
        } else {
            copyResult = orchestrateCopyToSecondaryStorageNonVMware(store, vmControlIp, fileToCopy, vmHostId);
        }
        return copyResult;
    }

    private void configureNetworkElementCommand(NetworkElementCommand cmd, VMInstanceVO vmInstance) {
        Map<String, String> accessDetails = networkManager.getSystemVMAccessDetails(vmInstance);
        if (StringUtils.isBlank(accessDetails.get(NetworkElementCommand.ROUTER_IP))) {
            throw new CloudRuntimeException("Unable to set system vm ControlIP for system vm with ID: " + vmInstance.getId());
        }
        cmd.setAccessDetail(accessDetails);
    }

    private Answer zipDiagnosticsFilesInSystemVm(VMInstanceVO vmInstance, List<String> fileList) {
        final PrepareFilesCommand cmd = new PrepareFilesCommand(fileList, DataRetrievalTimeout.value());
        configureNetworkElementCommand(cmd, vmInstance);
        Answer answer = agentManager.easySend(vmInstance.getHostId(), cmd);
        return answer;
    }

    private Answer deleteDiagnosticsZipFileInsystemVm(VMInstanceVO vmInstance, String zipFileName) {
        final DeleteFileInVrCommand cmd = new DeleteFileInVrCommand(zipFileName);
        configureNetworkElementCommand(cmd, vmInstance);
        return agentManager.easySend(vmInstance.getHostId(), cmd);
    }

    /**
     * Generate a list of diagnostics file to be retrieved depending on the system VM type
     *
     * @param optionalFileList Optional list of files that user may want to retrieve, empty by default
     * @param vmInstance       system VM instance, either SSVM, CPVM or VR
     * @return a list of files to be retrieved for system VM, either generated from defaults depending on the VM type, or specified
     * by the optional list param
     */
    private List<String> getFileListToBeRetrieved(List<String> optionalFileList, VMInstanceVO vmInstance) {
        DiagnosticsFilesList fileListObject = DiagnosticsFilesListFactory.getDiagnosticsFilesList(optionalFileList, vmInstance);
        List<String> fileList = new ArrayList<>();

        if (fileListObject != null) {
            fileList = fileListObject.generateFileList();
        }
        return fileList;
    }

    private Pair<Boolean, String> orchestrateCopyToSecondaryStorageNonVMware(final DataStore store, final String vmControlIp, String fileToCopy, Long vmHostId) {
        CopyToSecondaryStorageCommand toSecondaryStorageCommand = new CopyToSecondaryStorageCommand(store.getUri(), vmControlIp, fileToCopy);
        Answer copyToSecondaryAnswer = agentManager.easySend(vmHostId, toSecondaryStorageCommand);
        Pair<Boolean, String> copyAnswer;
        if (copyToSecondaryAnswer != null) {
            copyAnswer = new Pair<>(copyToSecondaryAnswer.getResult(), copyToSecondaryAnswer.getDetails());
        } else {
            copyAnswer = new Pair<>(false, "Diagnostics Zip file to secondary storage failed");
        }
        return copyAnswer;
    }

    private Pair<Boolean, String> orchestrateCopyToSecondaryStorageVMware(final DataStore store, final String vmSshIp, String diagnosticsFile) {
        String mountPoint;
        boolean success;

        Integer nfsVersion = imageStoreDetailsUtil.getNfsVersion(store.getId());
        mountPoint = mountManager.getMountPoint(store.getUri(), nfsVersion);
        if (StringUtils.isNotBlank(mountPoint)) {
            LOGGER.info(String.format("Copying %s from %s to secondary store %s", diagnosticsFile, vmSshIp, store.getUri()));

            // dirIn/mnt/SecStorage/uuid/diagnostics_data
            String dataDirectoryInSecondaryStore = String.format("%s/%s", mountPoint, DIAGNOSTICS_DATA_DIRECTORY);
            try {
                File dataDirectory = new File(dataDirectoryInSecondaryStore);
                boolean existsInSecondaryStore = dataDirectory.exists() || dataDirectory.mkdir();

                // Modify directory file permissions
                Path path = Paths.get(dataDirectory.getAbsolutePath());
                setDirFilePermissions(path);

                if (existsInSecondaryStore) {
                    // scp from system VM to mounted sec storage directory
                    int port = 3922;
                    File permKey = new File("/var/cloudstack/management/.ssh/id_rsa");
                    SshHelper.scpFrom(vmSshIp, port, "root", permKey, dataDirectoryInSecondaryStore, diagnosticsFile);
                }
                // Verify File copy to Secondary Storage
                File fileInSecondaryStore = new File(dataDirectoryInSecondaryStore + diagnosticsFile.replace("/root", ""));
                success = fileInSecondaryStore.exists();
            } catch (Exception e) {
                String msg = String.format("Exception caught during scp from %s to secondary store %s: ", vmSshIp, dataDirectoryInSecondaryStore);
                LOGGER.error(msg);
                return new Pair<>(false, msg);
            } finally {
                // umount secondary storage
                umountSecondaryStorage(mountPoint);
            }
        } else {
            return new Pair<>(false, "Failed to mount secondary storage:" + store.getName());
        }
        return new Pair<>(success, "File copied to secondary storage successfully");
    }

    // Get ssvm from the zone to use for creating entity download URL
    private VMInstanceVO getSecondaryStorageVmInZone(Long zoneId) {
        List<VMInstanceVO> ssvm = instanceDao.listByZoneIdAndType(zoneId, VirtualMachine.Type.SecondaryStorageVm);
        return (CollectionUtils.isEmpty(ssvm)) ? null : ssvm.get(0);
    }

    /**
     * Iterate through all Image stores in the current running zone and select any that has less than 95% disk usage
     *
     * @param zoneId of the current running zone
     * @return a valid secondary storage with less than DiskQuotaPercentageThreshold set by global config
     */
    private DataStore getImageStore(Long zoneId) {
        List<DataStore> stores = storeMgr.getImageStoresByScope(new ZoneScope(zoneId));
        if (CollectionUtils.isEmpty(stores)) {
            throw new CloudRuntimeException("No Secondary storage found in Zone with Id: " + zoneId);
        }
        DataStore imageStore = null;
        for (DataStore store : stores) {
            // Return image store if used percentage is less then threshold value set by global config diagnostics.data.disable.threshold
            if (statsCollector.imageStoreHasEnoughCapacity(store, DiskQuotaPercentageThreshold.value())) {
                imageStore = store;
                break;
            }
        }
        if (imageStore == null) {
            throw new CloudRuntimeException("No suitable secondary storage found to retrieve diagnostics in Zone: " + zoneId);
        }
        return imageStore;
    }

    // createEntityExtractUrl throws CloudRuntime exception in case of failure
    private String createFileDownloadUrl(DataStore store, Hypervisor.HypervisorType hypervisorType, String filePath) {
        // Get image store driver
        ImageStoreEntity secStore = (ImageStoreEntity) store;

        //Create dummy TO with hyperType
        DataTO dataTO = new DiagnosticsDataTO(hypervisorType, store.getTO());
        DataObject dataObject = new DiagnosticsDataObject(dataTO, store);
        return secStore.createEntityExtractUrl(filePath, Storage.ImageFormat.TAR, dataObject);
    }

    private VMInstanceVO getSystemVMInstance(Long vmId) {
        VMInstanceVO vmInstance = instanceDao.findByIdTypes(vmId, VirtualMachine.Type.ConsoleProxy,
                VirtualMachine.Type.DomainRouter, VirtualMachine.Type.SecondaryStorageVm);
        if (vmInstance == null) {
            String msg = String.format("Unable to find vm instance with id: %s", vmId);
            LOGGER.error(msg);
            throw new CloudRuntimeException("Diagnostics command execution failed, " + msg);
        }

        final Long hostId = vmInstance.getHostId();
        if (hostId == null) {
            throw new CloudRuntimeException("Unable to find host for virtual machine instance: " + vmInstance.getInstanceName());
        }
        return vmInstance;
    }

    private String getVMSshIp(final VMInstanceVO vmInstance) {
        Map<String, String> accessDetails = networkManager.getSystemVMAccessDetails(vmInstance);
        String controlIP = accessDetails.get(NetworkElementCommand.ROUTER_IP);
        if (StringUtils.isBlank(controlIP)) {
            throw new CloudRuntimeException("Unable to find system vm ssh/control IP for  vm with ID: " + vmInstance.getId());
        }
        return controlIP;
    }

    @Override
    public boolean start() {
        super.start();
        return true;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        if (EnableGarbageCollector.value()) {
            backgroundPollManager.submitTask(new GCBackgroundTask(this));
            return true;
        }
        return false;
    }

    public static final class GCBackgroundTask extends ManagedContextRunnable implements BackgroundPollTask {
        private DiagnosticsServiceImpl serviceImpl;

        public GCBackgroundTask(DiagnosticsServiceImpl serviceImpl) {
            this.serviceImpl = serviceImpl;
        }

        private static void deleteOldDiagnosticsFiles(File directory, String storeName) {
            File[] fileList = directory.listFiles();
            if (fileList != null) {
                String msg = String.format("Found %s diagnostics files in store %s for garbage collection", fileList.length, storeName);
                LOGGER.info(msg);
                for (File file : fileList) {
                    if (file.isFile()) {
                        if (MaximumFileAgeforGarbageCollection.value() <= getTimeDifference(file)) {
                            boolean success = file.delete();
                            LOGGER.info(file.getName() + " delete status: " + success);
                        }
                    }
                }
            }
        }

        @Override
        protected void runInContext() {
            List<DataCenterVO> dcList = serviceImpl.dataCenterDao.listEnabledZones();
            for (DataCenterVO vo: dcList) {
                // Get All Image Stores in current running Zone
                List<DataStore> storeList = serviceImpl.storeMgr.getImageStoresByScope(new ZoneScope(vo.getId()));
                for (DataStore store : storeList) {
                    String mountPoint = null;
                    try {
                        mountPoint = serviceImpl.mountManager.getMountPoint(store.getUri(), null);
                        if (StringUtils.isNotBlank(mountPoint)) {
                            File directory = new File(mountPoint + "/" + DIAGNOSTICS_DATA_DIRECTORY);
                            if (directory.isDirectory()) {
                                deleteOldDiagnosticsFiles(directory, store.getName());
                            }
                        }
                    } finally {
                        // umount secondary storage
                        umountSecondaryStorage(mountPoint);
                    }
                }

            }
        }

        @Override
        public Long getDelay() {
            // In Milliseconds
            return GarbageCollectionInterval.value() * 1000L;
        }
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(RunDiagnosticsCmd.class);
        cmdList.add(GetDiagnosticsDataCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return DiagnosticsServiceImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{EnableGarbageCollector, DataRetrievalTimeout,
                MaximumFileAgeforGarbageCollection, GarbageCollectionInterval, DiskQuotaPercentageThreshold,
                SsvmDefaultSupportedFiles, VrDefaultSupportedFiles, CpvmDefaultSupportedFiles};
    }
}