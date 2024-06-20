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
package com.cloud.agent.api.to;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.cloud.agent.api.LogLevel;
import com.cloud.network.element.NetworkElement;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;

public class VirtualMachineTO {
    private long id;
    private String name;
    private BootloaderType bootloader;
    private VirtualMachine.State state;
    private Type type;
    private int cpus;

    /**
        'speed' is still here since 4.0.X/4.1.X management servers do not support
         the overcommit feature yet.

         The overcommit feature sends minSpeed and maxSpeed

         So this is here for backwards compatibility with 4.0.X/4.1.X management servers
         and newer agents.
    */
    private Integer speed;
    private Integer minSpeed;
    private Integer maxSpeed;

    private long minRam;
    private long maxRam;
    private String hostName;
    private String arch;
    private String os;
    private String platformEmulator;
    private String bootArgs;
    private String[] bootupScripts;
    private boolean enableHA;
    private boolean limitCpuUse;
    private boolean enableDynamicallyScaleVm;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private String vncPassword;
    private String vncAddr;
    private Map<String, String> params;
    private String uuid;
    private String bootType;
    private String bootMode;
    private boolean enterHardwareSetup;

    private DiskTO[] disks;
    private NicTO[] nics;
    private GPUDeviceTO gpuDevice;
    private Integer vcpuMaxLimit;
    private List<String[]> vmData = null;

    private String configDriveLabel = null;
    private String configDriveIsoRootFolder = null;
    private String configDriveIsoFile = null;
    private NetworkElement.Location configDriveLocation = NetworkElement.Location.SECONDARY;

    private Double cpuQuotaPercentage = null;

    private Map<String, String> guestOsDetails = new HashMap<String, String>();
    private Map<String, String> extraConfig = new HashMap<>();
    private Map<Long, String> networkIdToNetworkNameMap = new HashMap<>();
    private DeployAsIsInfoTO deployAsIsInfo;

    public VirtualMachineTO(long id, String instanceName, VirtualMachine.Type type, int cpus, Integer speed, long minRam, long maxRam, BootloaderType bootloader,
            String os, boolean enableHA, boolean limitCpuUse, String vncPassword) {
        this.id = id;
        name = instanceName;
        this.type = type;
        this.cpus = cpus;
        this.speed = speed;
        this.minRam = minRam;
        this.maxRam = maxRam;
        this.bootloader = bootloader;
        this.os = os;
        this.enableHA = enableHA;
        this.limitCpuUse = limitCpuUse;
        this.vncPassword = vncPassword;
    }

    public VirtualMachineTO(long id, String instanceName, VirtualMachine.Type type, int cpus, Integer minSpeed, Integer maxSpeed, long minRam, long maxRam,
            BootloaderType bootloader, String os, boolean enableHA, boolean limitCpuUse, String vncPassword) {
        this.id = id;
        name = instanceName;
        this.type = type;
        this.cpus = cpus;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.minRam = minRam;
        this.maxRam = maxRam;
        this.bootloader = bootloader;
        this.os = os;
        this.enableHA = enableHA;
        this.limitCpuUse = limitCpuUse;
        this.vncPassword = vncPassword;
    }

    protected VirtualMachineTO() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isEnableDynamicallyScaleVm() {
        return enableDynamicallyScaleVm;
    }

    public void setEnableDynamicallyScaleVm(boolean enableDynamicallyScaleVm) {
        this.enableDynamicallyScaleVm = enableDynamicallyScaleVm;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public BootloaderType getBootloader() {
        return bootloader;
    }

    public void setBootloader(BootloaderType bootloader) {
        this.bootloader = bootloader;
    }

    public VirtualMachine.State getState() {
        return state;
    }

    public void setState(VirtualMachine.State state) {
        this.state = state;
    }

    public int getCpus() {
        return cpus;
    }

    public void setCpus(int cpus) {
        this.cpus = cpus;
    }

    public Integer getSpeed() {
        return speed;
    }

    public Integer getMinSpeed() {
        return minSpeed;
    }

    public Integer getMaxSpeed() {
        return maxSpeed;
    }

    public boolean getLimitCpuUse() {
        return limitCpuUse;
    }

    public long getMinRam() {
        return minRam;
    }

    public void setRam(long minRam, long maxRam) {
        this.minRam = minRam;
        this.maxRam = maxRam;
    }

    public long getMaxRam() {
        return maxRam;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getBootArgs() {
        return bootArgs;
    }

    public void setBootArgs(String bootArgs) {
        this.bootArgs = bootArgs;
    }

    public void setBootArgs(Map<String, String> bootParams) {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, String> entry : bootParams.entrySet()) {
            buf.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
        }
        bootArgs = buf.toString();
    }

    public String[] getBootupScripts() {
        return bootupScripts;
    }

    public void setBootupScripts(String[] bootupScripts) {
        this.bootupScripts = bootupScripts;
    }

    public boolean isEnableHA() {
        return enableHA;
    }

    public void setEnableHA(boolean enableHA) {
        this.enableHA = enableHA;
    }

    public DiskTO[] getDisks() {
        return disks;
    }

    public void setDisks(DiskTO[] disks) {
        this.disks = disks;
    }

    public NicTO[] getNics() {
        return nics;
    }

    public void setNics(NicTO[] nics) {
        this.nics = nics;
    }

    public String getVncPassword() {
        return vncPassword;
    }

    public void setVncPassword(String vncPassword) {
        this.vncPassword = vncPassword;
    }

    public String getVncAddr() {
        return vncAddr;
    }

    public void setVncAddr(String vncAddr) {
        this.vncAddr = vncAddr;
    }

    public Map<String, String> getDetails() {
        return params;
    }

    public void setDetails(Map<String, String> params) {
        this.params = params;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public GPUDeviceTO getGpuDevice() {
        return gpuDevice;
    }

    public void setGpuDevice(GPUDeviceTO gpuDevice) {
        this.gpuDevice = gpuDevice;
    }

    public String getPlatformEmulator() {
        return platformEmulator;
    }

    public void setPlatformEmulator(String platformEmulator) {
        this.platformEmulator = platformEmulator;
    }

    public Integer getVcpuMaxLimit() {
        return vcpuMaxLimit;
    }

    public void setVcpuMaxLimit(Integer vcpuMaxLimit) {
        this.vcpuMaxLimit = vcpuMaxLimit;
    }

    public List<String[]> getVmData() {
        return vmData;
    }

    public void setVmData(List<String[]> vmData) {
        this.vmData = vmData;
    }

    public String getConfigDriveLabel() {
        return configDriveLabel;
    }

    public void setConfigDriveLabel(String configDriveLabel) {
        this.configDriveLabel = configDriveLabel;
    }

    public String getConfigDriveIsoRootFolder() {
        return configDriveIsoRootFolder;
    }

    public void setConfigDriveIsoRootFolder(String configDriveIsoRootFolder) {
        this.configDriveIsoRootFolder = configDriveIsoRootFolder;
    }

    public String getConfigDriveIsoFile() {
        return configDriveIsoFile;
    }

    public void setConfigDriveIsoFile(String configDriveIsoFile) {
        this.configDriveIsoFile = configDriveIsoFile;
    }

    public boolean isConfigDriveOnHostCache() {
        return (this.configDriveLocation == NetworkElement.Location.HOST);
    }

    public NetworkElement.Location getConfigDriveLocation() {
        return configDriveLocation;
    }

    public void setConfigDriveLocation(NetworkElement.Location configDriveLocation) {
        this.configDriveLocation = configDriveLocation;
    }

    public Map<String, String> getGuestOsDetails() {
        return guestOsDetails;
    }

    public void setGuestOsDetails(Map<String, String> guestOsDetails) {
        this.guestOsDetails = guestOsDetails;
    }

    public Double getCpuQuotaPercentage() {
        return cpuQuotaPercentage;
    }

    public void setCpuQuotaPercentage(Double cpuQuotaPercentage) {
        this.cpuQuotaPercentage = cpuQuotaPercentage;
    }

    public void addExtraConfig(String key, String value) {
        extraConfig.put(key, value);
    }
    public Map<String, String> getExtraConfig() {
        return extraConfig;
    }

    public Map<Long, String> getNetworkIdToNetworkNameMap() {
        return networkIdToNetworkNameMap;
    }

    public void setNetworkIdToNetworkNameMap(Map<Long, String> networkIdToNetworkNameMap) {
        this.networkIdToNetworkNameMap = networkIdToNetworkNameMap;
    }

    public String getBootType() {
        return bootType;
    }

    public void setBootType(String bootType) {
        this.bootType = bootType;
    }

    public String getBootMode() { return bootMode; }

    public void setBootMode(String bootMode) { this.bootMode = bootMode; }

    public boolean isEnterHardwareSetup() {
        return enterHardwareSetup;
    }

    public void setEnterHardwareSetup(boolean enterHardwareSetup) {
        this.enterHardwareSetup = enterHardwareSetup;
    }

    public DeployAsIsInfoTO getDeployAsIsInfo() {
        return deployAsIsInfo;
    }

    public void setDeployAsIsInfo(DeployAsIsInfoTO deployAsIsInfo) {
        this.deployAsIsInfo = deployAsIsInfo;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    public void setMinSpeed(Integer minSpeed) {
        this.minSpeed = minSpeed;
    }

    public void setMaxSpeed(Integer maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public void setMinRam(long minRam) {
        this.minRam = minRam;
    }

    public void setMaxRam(long maxRam) {
        this.maxRam = maxRam;
    }

    public boolean isLimitCpuUse() {
        return limitCpuUse;
    }

    public void setLimitCpuUse(boolean limitCpuUse) {
        this.limitCpuUse = limitCpuUse;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public void setExtraConfig(Map<String, String> extraConfig) {
        this.extraConfig = extraConfig;
    }

    @Override
    public String toString() {
        return String.format("VM {id: \"%s\", name: \"%s\", uuid: \"%s\", type: \"%s\"}", id, name, uuid, type);
    }
}
