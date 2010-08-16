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

import java.util.Map;

import com.cloud.hypervisor.Hypervisor;

public class VmCharacteristics {
    long id;
    int core;
    int speed; // in mhz
    long ram; // in bytes
    Hypervisor.Type hypervisorType;
    VirtualMachine.Type type;
    Map<String, String> params;
    
    public VmCharacteristics(VirtualMachine.Type type) {
        this.type = type;
    }
    
    public VirtualMachine.Type getType() {
        return type;
    }
    
    
    public int getCores() {
        return core;
    }
    
    public int getSpeed() {
        return speed;
    }
    
    public long getRam() {
        return ram;
    }
    
    public Hypervisor.Type getHypervisorType() {
        return hypervisorType;
    }
    
    public VmCharacteristics(long id, int core, int speed, long ram, Hypervisor.Type type, Map<String, String> params) {
        this.core = core;
        this.speed = speed;
        this.ram = ram;
        this.hypervisorType = type;
        this.params = params;
        this.id = id;
    }
    
    protected VmCharacteristics() {
    }
}