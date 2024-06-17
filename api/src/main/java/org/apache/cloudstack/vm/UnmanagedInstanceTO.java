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

package org.apache.cloudstack.vm;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.util.List;

public class UnmanagedInstanceTO {

    public enum PowerState {
        PowerUnknown,
        PowerOn,
        PowerOff
    }

    private String name;

    private String internalCSName;

    private PowerState powerState;

    private PowerState cloneSourcePowerState;

    private Integer cpuCores;

    private Integer cpuCoresPerSocket;

    private Integer memory;

    private Integer cpuSpeed;

    private String operatingSystemId;

    private String operatingSystem;

    private String clusterName;

    private String hostName;

    private List<Disk> disks;

    private List<Nic> nics;

    private String vncPassword;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInternalCSName() {
        return internalCSName;
    }

    public void setInternalCSName(String internalCSName) {
        this.internalCSName = internalCSName;
    }

    public PowerState getPowerState() {
        return powerState;
    }

    public void setPowerState(PowerState powerState) {
        this.powerState = powerState;
    }

    public PowerState getCloneSourcePowerState() {
        return cloneSourcePowerState;
    }

    public void setCloneSourcePowerState(PowerState cloneSourcePowerState) {
        this.cloneSourcePowerState = cloneSourcePowerState;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(Integer cpuCores) {
        this.cpuCores = cpuCores;
    }

    public Integer getCpuCoresPerSocket() {
        return cpuCoresPerSocket;
    }

    public void setCpuCoresPerSocket(Integer cpuCoresPerSocket) {
        this.cpuCoresPerSocket = cpuCoresPerSocket;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public Integer getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(Integer cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    public String getOperatingSystemId() {
        return operatingSystemId;
    }

    public void setOperatingSystemId(String operatingSystemId) {
        this.operatingSystemId = operatingSystemId;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public List<Disk> getDisks() {
        return disks;
    }

    public void setDisks(List<Disk> disks) {
        this.disks = disks;
    }

    public List<Nic> getNics() {
        return nics;
    }

    public void setNics(List<Nic> nics) {
        this.nics = nics;
    }

    public String getVncPassword() {
        return vncPassword;
    }

    public void setVncPassword(String vncPassword) {
        this.vncPassword = vncPassword;
    }

    public static class Disk {
        private String diskId;

        private String label;

        private Long capacity;

        private String fileBaseName;

        private String imagePath;

        private String controller;

        private Integer controllerUnit;

        private Integer position;

        private String chainInfo;

        private String datastoreName;

        private String datastoreHost;

        private String datastorePath;

        private int datastorePort;

        private String datastoreType;

        public String getDiskId() {
            return diskId;
        }

        public void setDiskId(String diskId) {
            this.diskId = diskId;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Long getCapacity() {
            return capacity;
        }

        public void setCapacity(Long capacity) {
            this.capacity = capacity;
        }

        public String getFileBaseName() {
            return fileBaseName;
        }

        public void setFileBaseName(String fileBaseName) {
            this.fileBaseName = fileBaseName;
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getController() {
            return controller;
        }

        public void setController(String controller) {
            this.controller = controller;
        }

        public Integer getControllerUnit() {
            return controllerUnit;
        }

        public void setControllerUnit(Integer controllerUnit) {
            this.controllerUnit = controllerUnit;
        }

        public Integer getPosition() {
            return position;
        }

        public void setPosition(Integer position) {
            this.position = position;
        }

        public String getChainInfo() {
            return chainInfo;
        }

        public void setChainInfo(String chainInfo) {
            this.chainInfo = chainInfo;
        }

        public String getDatastoreName() {
            return datastoreName;
        }

        public void setDatastoreName(String datastoreName) {
            this.datastoreName = datastoreName;
        }

        public String getDatastoreHost() {
            return datastoreHost;
        }

        public void setDatastoreHost(String datastoreHost) {
            this.datastoreHost = datastoreHost;
        }

        public String getDatastorePath() {
            return datastorePath;
        }

        public void setDatastorePath(String datastorePath) {
            this.datastorePath = datastorePath;
        }

        public String getDatastoreType() {
            return datastoreType;
        }

        public void setDatastoreType(String datastoreType) {
            this.datastoreType = datastoreType;
        }

        public void setDatastorePort(int datastorePort) {
            this.datastorePort = datastorePort;
        }

        public int getDatastorePort() {
            return datastorePort;
        }

        @Override
        public String toString() {
            return "Disk {" +
                    "diskId='" + diskId + '\'' +
                    ", capacity=" + toHumanReadableSize(capacity) +
                    ", controller='" + controller + '\'' +
                    ", controllerUnit=" + controllerUnit +
                    "}";
        }
    }

    public static class Nic {
        private String nicId;

        private String adapterType;

        private String macAddress;

        private String network;

        private Integer vlan;

        private Integer pvlan;

        private String pvlanType;

        private List<String> ipAddress;

        private String pciSlot;

        public String getNicId() {
            return nicId;
        }

        public void setNicId(String nicId) {
            this.nicId = nicId;
        }

        public String getAdapterType() {
            return adapterType;
        }

        public void setAdapterType(String adapterType) {
            this.adapterType = adapterType;
        }

        public String getMacAddress() {
            return macAddress;
        }

        public void setMacAddress(String macAddress) {
            this.macAddress = macAddress;
        }

        public String getNetwork() {
            return network;
        }

        public void setNetwork(String network) {
            this.network = network;
        }

        public Integer getVlan() {
            return vlan;
        }

        public void setVlan(Integer vlan) {
            this.vlan = vlan;
        }

        public Integer getPvlan() {
            return pvlan;
        }

        public void setPvlan(Integer pvlan) {
            this.pvlan = pvlan;
        }

        public String getPvlanType() {
            return pvlanType;
        }

        public void setPvlanType(String pvlanType) {
            this.pvlanType = pvlanType;
        }

        public List<String> getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(List<String> ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getPciSlot() {
            return pciSlot;
        }

        public void setPciSlot(String pciSlot) {
            this.pciSlot = pciSlot;
        }

        @Override
        public String toString() {
            return "Nic{" +
                    "nicId='" + nicId + '\'' +
                    ", adapterType='" + adapterType + '\'' +
                    ", macAddress='" + macAddress + '\'' +
                    "}";
        }
    }
}
