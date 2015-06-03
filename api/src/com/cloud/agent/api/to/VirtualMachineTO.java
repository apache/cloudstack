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

import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;

public class VirtualMachineTO {
    private long id;
    private String name;
    private BootloaderType bootloader;
    Type type;
    int cpus;

    /**
        'speed' is still here since 4.0.X/4.1.X management servers do not support
         the overcommit feature yet.

         The overcommit feature sends minSpeed and maxSpeed

         So this is here for backwards compatibility with 4.0.X/4.1.X management servers
         and newer agents.
    */
    Integer speed;
    Integer minSpeed;
    Integer maxSpeed;

    long minRam;
    long maxRam;
    String hostName;
    String arch;
    String os;
    String platformEmulator;
    String bootArgs;
    String[] bootupScripts;
    boolean enableHA;
    boolean limitCpuUse;
    boolean enableDynamicallyScaleVm;
    String vncPassword;
    String vncAddr;
    Map<String, String> params;
    String uuid;

    DiskTO[] disks;
    NicTO[] nics;
    GPUDeviceTO gpuDevice;
    Integer vcpuMaxLimit;
    List<String[]> vmData = null;

    String configDriveLabel = null;
    String configDriveIsoRootFolder = null;
    String configDriveIsoFile = null;


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

    public BootloaderType getBootloader() {
        return bootloader;
    }

    public void setBootloader(BootloaderType bootloader) {
        this.bootloader = bootloader;
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

}
