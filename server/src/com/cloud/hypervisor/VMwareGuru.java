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
package com.cloud.hypervisor;

import java.util.Map;

import javax.ejb.Local;

import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteSnapshotBackupCommand;
import com.cloud.agent.api.DeleteSnapshotsDirCommand;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.DetailsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.component.Inject;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=HypervisorGuru.class)
public class VMwareGuru extends HypervisorGuruBase implements HypervisorGuru {
    @Inject GuestOSDao _guestOsDao;
    @Inject HostDao _hostDao;
    @Inject DetailsDao _hostDetailsDao;

    protected VMwareGuru() {
    	super();
    }
    
    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.VMware;
    }

    @Override
    public <T extends VirtualMachine> VirtualMachineTO implement(VirtualMachineProfile<T> vm) {
        VirtualMachineTO to = toVirtualMachineTO(vm);
        to.setBootloader(BootloaderType.HVM);

        // Determine the VM's OS description
        GuestOSVO guestOS = _guestOsDao.findById(vm.getVirtualMachine().getGuestOSId());
        to.setOs(guestOS.getDisplayName());
        return to;
    }
    
    @Override
    public long getCommandHostDelegation(long hostId, Command cmd) {
    	boolean needDelegation = false;
    	
    	if(cmd instanceof PrimaryStorageDownloadCommand || 
    		cmd instanceof BackupSnapshotCommand ||
    		cmd instanceof DeleteSnapshotsDirCommand ||
    		cmd instanceof DeleteSnapshotBackupCommand ||
    		cmd instanceof CreatePrivateTemplateFromVolumeCommand ||
    		cmd instanceof CreatePrivateTemplateFromSnapshotCommand ||
    		cmd instanceof CopyVolumeCommand ||
    		cmd instanceof CreateVolumeFromSnapshotCommand) {
    		needDelegation = true;
    	}

    	if(needDelegation) {
    		HostVO host = _hostDao.findById(hostId);
    		assert(host != null);
    		assert(host.getHypervisorType() == HypervisorType.VMware);
    		long dcId = host.getDataCenterId();
    		
    		HostVO hostSecStorage = _hostDao.findSecondaryStorageHost(dcId);
    		if(hostSecStorage != null && hostSecStorage.getStatus() == Status.Up) {
    			// TODO, we need to make sure agent is actually connected too
    			cmd.setContextParam("hypervisor", HypervisorType.VMware.toString());
    		    Map<String, String> hostDetails = _hostDetailsDao.findDetails(hostId);
    		    cmd.setContextParam("guid", hostDetails.get("guid"));
    		    cmd.setContextParam("username", hostDetails.get("username"));
    		    cmd.setContextParam("password", hostDetails.get("password"));
    			
    			return hostSecStorage.getId();
    		}
    	}
  
    	return hostId;
    }
}
