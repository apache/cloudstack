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

import com.cloud.agent.api.to.VolumeTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.ServiceOffering;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.user.Account;


/**
 * VirtualMachineProfile describes one virtual machine.  This object
 * is passed to various adapters to be processed.  Anything that is 
 * set in this object is transitional.  It does not get persisted 
 * back to the database.  This allows the adapters to process
 * the information in the virtual machine and make determinations
 * on what the virtual machine profile should look like before it is
 * actually started on the hypervisor.
 *
 * @param <T> a VirtualMachine
 */
public interface VirtualMachineProfile<T extends VirtualMachine> {
  
    
    public static class Param {
        
        public static final Param VmPassword = new Param("VmPassword");
        public static final Param ControlNic = new Param("ControlNic");
        public static final Param ReProgramNetwork = new Param("RestartNetwork");
        public static final Param PxeSeverType = new Param("PxeSeverType");
        
        private String name;
        
        public Param(String name) {
            synchronized(Param.class) {
                this.name = name;
            }
        }
        
        public String getName() {
            return name;
        }
     }
    
    String getHostName();
    
    String getInstanceName();
    
    Account getOwner();
    
    /**
     * @return the virtual machine that backs up this profile.
     */
    T getVirtualMachine();
    
    /**
     * @return service offering for this virtual machine.
     */
    ServiceOffering getServiceOffering();
    
    /**
     * @return parameter specific for this type of virtual machine.
     */
    Object getParameter(Param name);
    
    /**
     * @return the hypervisor type needed for this virtual machine.
     */
    HypervisorType getHypervisorType();
    
    /**
     * @return template the virtual machine is based on.
     */
    VirtualMachineTemplate getTemplate();
    
    /**
     * @return the template id
     */
    long getTemplateId();
    
    /**
     * @return the service offering id
     */
    long getServiceOfferingId();
    
    /**
     * @return virtual machine id.
     */
    long getId();
    
    List<NicProfile> getNics();
    
    List<VolumeTO> getDisks();
    
    void addNic(int index, NicProfile nic);
    
    void addDisk(int index, VolumeTO disk);
    
    StringBuilder getBootArgsBuilder();
    
    void addBootArgs(String... args);
    
    String getBootArgs();
    
    void addNic(NicProfile nic);
    
    void addDisk(VolumeTO disk);
    
    VirtualMachine.Type getType();
    
    void setParameter(Param name, Object value);

	void setBootLoaderType(BootloaderType bootLoader);
	BootloaderType getBootLoaderType();
	
	Map<Param, Object> getParameters();
}
