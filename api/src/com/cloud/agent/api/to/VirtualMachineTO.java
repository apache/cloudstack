/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.agent.api.to;

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
    Integer speed;
    long minRam;
    long maxRam;
    String hostName;
    String arch;
    String os;
    String bootArgs;
    String[] bootupScripts;
    boolean rebootOnCrash;

    VolumeTO[] disks;
    NicTO[] nics;

    public VirtualMachineTO(long id, String instanceName, VirtualMachine.Type type, int cpus, Integer speed, long minRam, long maxRam, BootloaderType bootloader, String os) {
        this.id = id;
        this.name = instanceName;
        this.type = type;
        this.cpus = cpus;
        this.speed = speed;
        this.minRam = minRam;
        this.maxRam = maxRam;
        this.bootloader = bootloader;
        this.os = os;
    }

    protected VirtualMachineTO() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
        StringBuilder buf = new StringBuilder(bootArgs != null ? bootArgs : "");
        buf.append(" ");
        for (NicTO nic : nics) {
            buf.append("");
        }
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

    public VolumeTO[] getDisks() {
        return disks;
    }

    public void setDisks(VolumeTO[] disks) {
        this.disks = disks;
    }

    public NicTO[] getNics() {
        return nics;
    }

    public void setNics(NicTO[] nics) {
        this.nics = nics;
    }
}
