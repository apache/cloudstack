/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */
package com.cloud.hypervisor.guru;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.cluster.CheckPointManager;
import com.cloud.cluster.ClusterManager;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruBase;
import com.cloud.hypervisor.vmware.VmwareCleanupMaid;
import com.cloud.hypervisor.vmware.manager.VmwareManager;
import com.cloud.hypervisor.vmware.mo.VirtualEthernetCardType;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.secstorage.CommandExecLogVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VmDetailConstants;

@Local(value=HypervisorGuru.class)
public class VMwareGuru extends HypervisorGuruBase implements HypervisorGuru {
	private static final Logger s_logger = Logger.getLogger(VMwareGuru.class);

	@Inject GuestOSDao _guestOsDao;
    @Inject HostDao _hostDao;
    @Inject HostDetailsDao _hostDetailsDao;
    @Inject CommandExecLogDao _cmdExecLogDao;
    @Inject ClusterManager _clusterMgr;
    @Inject VmwareManager _vmwareMgr;
    @Inject SecondaryStorageVmManager _secStorageMgr;
    @Inject CheckPointManager _checkPointMgr;

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

    	Map<String, String> details = to.getDetails();
    	if(details == null)
    		details = new HashMap<String, String>();
        
    	String nicDeviceType = details.get(VmDetailConstants.NIC_ADAPTER);
        if(vm.getVirtualMachine() instanceof DomainRouterVO || vm.getVirtualMachine() instanceof ConsoleProxyVO 
        	|| vm.getVirtualMachine() instanceof SecondaryStorageVmVO) {
        	
            // for system VMs, use Vmxnet3 as default
        	if(nicDeviceType == null) {
        		details.put(VmDetailConstants.NIC_ADAPTER, VirtualEthernetCardType.Vmxnet3.toString());
        	} else {
        		try {
        			VirtualEthernetCardType.valueOf(nicDeviceType);
        		} catch (Exception e) {
        			s_logger.warn("Invalid NIC device type " + nicDeviceType + " is specified in VM details, switch to default E1000");
            		details.put(VmDetailConstants.NIC_ADAPTER, VirtualEthernetCardType.E1000.toString());
        		}
        	}
        } else {
        	// for user-VM, use E1000 as default
        	if(nicDeviceType == null) {
        		details.put(VmDetailConstants.NIC_ADAPTER, VirtualEthernetCardType.E1000.toString());
        	} else {
        		try {
        			VirtualEthernetCardType.valueOf(nicDeviceType);
        		} catch (Exception e) {
        			s_logger.warn("Invalid NIC device type " + nicDeviceType + " is specified in VM details, switch to default E1000");
            		details.put(VmDetailConstants.NIC_ADAPTER, VirtualEthernetCardType.E1000.toString());
        		}
        	}
        }
    	to.setDetails(details);

        // Determine the VM's OS description
        GuestOSVO guestOS = _guestOsDao.findById(vm.getVirtualMachine().getGuestOSId());
        to.setOs(guestOS.getDisplayName());
        return to;
    }
    
    @Override @DB
    public long getCommandHostDelegation(long hostId, Command cmd) {
    	boolean needDelegation = false;
    	
    	if(cmd instanceof PrimaryStorageDownloadCommand || 
    		cmd instanceof BackupSnapshotCommand ||
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
    		
    		Pair<HostVO, SecondaryStorageVmVO> cmdTarget = _secStorageMgr.assignSecStorageVm(dcId, cmd);
    		if(cmdTarget != null) {
    			// TODO, we need to make sure agent is actually connected too
    			cmd.setContextParam("hypervisor", HypervisorType.VMware.toString());
    		    Map<String, String> hostDetails = _hostDetailsDao.findDetails(hostId);
    		    cmd.setContextParam("guid", resolveNameInGuid(hostDetails.get("guid")));
    		    cmd.setContextParam("username", hostDetails.get("username"));
    		    cmd.setContextParam("password", hostDetails.get("password"));
    			cmd.setContextParam("serviceconsole", _vmwareMgr.getServiceConsolePortGroupName());
    			cmd.setContextParam("manageportgroup", _vmwareMgr.getManagementPortGroupName());
    			
    			CommandExecLogVO execLog = new CommandExecLogVO(cmdTarget.first().getId(), cmdTarget.second().getId(), cmd.getClass().getSimpleName(), 1);
    			_cmdExecLogDao.persist(execLog);
    			cmd.setContextParam("execid", String.valueOf(execLog.getId()));
    			
    			if(cmd instanceof BackupSnapshotCommand || 
    				cmd instanceof CreatePrivateTemplateFromVolumeCommand || 
    				cmd instanceof CreatePrivateTemplateFromSnapshotCommand ||
    				cmd instanceof CopyVolumeCommand ||
    				cmd instanceof CreateVolumeFromSnapshotCommand) {
    				
    				String workerName = _vmwareMgr.composeWorkerName();
    				long checkPointId = _checkPointMgr.pushCheckPoint(new VmwareCleanupMaid(hostDetails.get("guid"), workerName));
    				cmd.setContextParam("worker", workerName);
    				cmd.setContextParam("checkpoint", String.valueOf(checkPointId));
    			}
    			
    			return cmdTarget.first().getId();
    		}
    	}
  
    	return hostId;
    }
    
    public boolean trackVmHostChange() {
    	return true;
    }
    
    private static String resolveNameInGuid(String guid) {
    	String tokens[] = guid.split("@");
    	assert(tokens.length == 2);

    	String vCenterIp = NetUtils.resolveToIp(tokens[1]);
    	if(vCenterIp == null) {
    		s_logger.error("Fatal : unable to resolve vCenter address " + tokens[1] + ", please check your DNS configuration");
    		return guid;
    	}
    	
    	if(vCenterIp.equals(tokens[1]))
    		return guid;
    	
    	return tokens[0] + "@" + vCenterIp;
    }
}
