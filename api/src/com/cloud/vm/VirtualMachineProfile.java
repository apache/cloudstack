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
package com.cloud.vm;

import java.util.List;
import java.util.Map;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.ServiceOffering;

public class VirtualMachineProfile {
    VirtualMachine _vm;
    Integer _cpus;
    Integer _speed; // in mhz
    long _ram; // in bytes
    HypervisorType _hypervisorType;
    VirtualMachine.Type _type;
    Map<String, String> _params;
    Long _templateId;
    List<DiskProfile> _disks;
    List<NicProfile> _nics;
    String _os;
    
    public VirtualMachineProfile(VirtualMachine.Type type) {
        this._type = type;
    }
    
    public String getName() {
        return _vm.getInstanceName();
    }
    
    public String getOs() {
        return _os;
    }
    
    public long getId() {
        return _vm.getId();
    }
    
    public VirtualMachine.Type getType() {
        return _type;
    }
    
    public Long getTemplateId() {
        return _templateId;
    }
    
    public Integer getCpus() {
        return _cpus;
    }
    
    public Integer getSpeed() {
        return _speed;
    }
    
    public long getRam() {
        return _ram;
    }
    
    public void setNics(List<NicProfile> profiles) {
        this._nics = profiles;
    }
    
    public List<NicProfile> getNics() {
        return _nics;
    }
    
    public void setDisks(List<DiskProfile> profiles) {
        this._disks = profiles;
    }
    
    public List<DiskProfile> getDisks() {
        return _disks;
    }
    
    public HypervisorType getHypervisorType() {
        return _hypervisorType;
    }
    
    public VirtualMachine getVm() {
        return _vm;
    }
    
    public VirtualMachineProfile(long id, int core, int speed, long ram, Long templateId, HypervisorType type, Map<String, String> params) {
        this._cpus = core;
        this._speed = speed;
        this._ram = ram;
        this._hypervisorType = type;
        this._params = params;
        this._templateId = templateId;
    }
    
    public VirtualMachineProfile(VirtualMachine vm, ServiceOffering offering, String os, HypervisorType hypervisorType) {
        this._cpus = offering.getCpu();
        this._speed = offering.getSpeed();
        this._ram = offering.getRamSize() * 1024l * 1024l;
        this._templateId = vm.getTemplateId();
        this._type = vm.getType();
        this._vm = vm;
        this._os = os;
        this._hypervisorType = hypervisorType;
    }
    
    protected VirtualMachineProfile() {
    }
    
    @Override
    public String toString() {
        return "VM-" + _type + "-" + _vm.getId();
    }
}
