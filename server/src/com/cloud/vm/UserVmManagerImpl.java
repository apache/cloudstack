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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.acl.ControlledEntity.ACLType;
import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.ComputeChecksumCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.SnapshotCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.UpgradeSnapshotCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.commands.AssignVMCmd;
import com.cloud.api.commands.AttachVolumeCmd;
import com.cloud.api.commands.CreateTemplateCmd;
import com.cloud.api.commands.CreateVMGroupCmd;
import com.cloud.api.commands.DeleteVMGroupCmd;
import com.cloud.api.commands.DeployVMCmd;
import com.cloud.api.commands.DestroyVMCmd;
import com.cloud.api.commands.DetachVolumeCmd;
import com.cloud.api.commands.ListVMsCmd;
import com.cloud.api.commands.RebootVMCmd;
import com.cloud.api.commands.RecoverVMCmd;
import com.cloud.api.commands.ResetVMPasswordCmd;
import com.cloud.api.commands.RestoreVMCmd;
import com.cloud.api.commands.StartVMCmd;
import com.cloud.api.commands.UpdateVMCmd;
import com.cloud.api.commands.UpgradeVMCmd;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVMMapVO;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.projects.ProjectManager;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.server.Criteria;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.crypt.RSAHelper;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.AnnotationHelper;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = { UserVmManager.class, UserVmService.class })
public class UserVmManagerImpl implements UserVmManager, UserVmService, Manager {
    private static final Logger s_logger = Logger.getLogger(UserVmManagerImpl.class);

    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3; // 3 seconds

    @Inject
    protected HostDao _hostDao = null;
    @Inject
    protected DomainRouterDao _routerDao = null;
    @Inject
    protected ServiceOfferingDao _offeringDao = null;
    @Inject
    protected DiskOfferingDao _diskOfferingDao = null;
    @Inject
    protected UserStatisticsDao _userStatsDao = null;
    @Inject
    protected VMTemplateDao _templateDao = null;
    @Inject
    protected VMTemplateDetailsDao _templateDetailsDao = null;
    @Inject
    protected VMTemplateHostDao _templateHostDao = null;
    @Inject
    protected VMTemplateZoneDao _templateZoneDao = null;
    @Inject
    protected DomainDao _domainDao = null;
    @Inject
    protected ResourceLimitDao _limitDao = null;
    @Inject
    protected UserVmDao _vmDao = null;
    @Inject
    protected VolumeDao _volsDao = null;
    @Inject
    protected DataCenterDao _dcDao = null;
    @Inject
    protected FirewallRulesDao _rulesDao = null;
    @Inject
    protected LoadBalancerVMMapDao _loadBalancerVMMapDao = null;
    @Inject
    protected LoadBalancerDao _loadBalancerDao = null;
    @Inject
    protected PortForwardingRulesDao _portForwardingDao;
    @Inject
    protected IPAddressDao _ipAddressDao = null;
    @Inject
    protected HostPodDao _podDao = null;
    @Inject
    protected CapacityDao _capacityDao = null;
    @Inject
    protected NetworkManager _networkMgr = null;
    @Inject
    protected StorageManager _storageMgr = null;
    @Inject
    protected SnapshotManager _snapshotMgr = null;
    @Inject
    protected AgentManager _agentMgr = null;
    @Inject
    protected ConfigurationManager _configMgr = null;
    @Inject
    protected AccountDao _accountDao = null;
    @Inject
    protected UserDao _userDao = null;
    @Inject
    protected SnapshotDao _snapshotDao = null;
    @Inject
    protected GuestOSDao _guestOSDao = null;
    @Inject
    protected GuestOSCategoryDao _guestOSCategoryDao = null;
    @Inject
    protected HighAvailabilityManager _haMgr = null;
    @Inject
    protected AlertManager _alertMgr = null;
    @Inject
    protected AccountManager _accountMgr;
    @Inject
    protected AccountService _accountService;
    @Inject
    protected AsyncJobManager _asyncMgr;
    @Inject
    protected VlanDao _vlanDao;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    protected StoragePoolDao _storagePoolDao;
    @Inject
    protected VMTemplateHostDao _vmTemplateHostDao;
    @Inject
    protected SecurityGroupManager _securityGroupMgr;
    @Inject
    protected ServiceOfferingDao _serviceOfferingDao;
    @Inject
    protected NetworkOfferingDao _networkOfferingDao;
    @Inject
    protected EventDao _eventDao = null;
    @Inject
    protected InstanceGroupDao _vmGroupDao;
    @Inject
    protected InstanceGroupVMMapDao _groupVMMapDao;
    @Inject
    protected VirtualMachineManager _itMgr;
    @Inject
    protected NetworkDao _networkDao;
    @Inject
    protected VirtualNetworkApplianceManager _routerMgr;
    @Inject
    protected NicDao _nicDao;
    @Inject
    protected RulesManager _rulesMgr;
    @Inject
    protected LoadBalancingRulesManager _lbMgr;
    @Inject
    protected UsageEventDao _usageEventDao;
    @Inject
    protected SSHKeyPairDao _sshKeyPairDao;
    @Inject
    protected UserVmDetailsDao _vmDetailsDao;
    @Inject
    protected SecurityGroupDao _securityGroupDao;
    @Inject 
    protected HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;
    @Inject 
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected ResourceLimitService _resourceLimitMgr;
    @Inject
    protected FirewallManager _firewallMgr;
    @Inject
    protected ProjectManager _projectMgr;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject 
    protected NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    SecurityGroupVMMapDao _securityGroupVMMapDao;

    protected ScheduledExecutorService _executor = null;
    protected int _expungeInterval;
    protected int _expungeDelay;

    protected String _name;
    protected String _instance;
    protected String _zone;

    private ConfigurationDao _configDao;
    private int _createprivatetemplatefromvolumewait;
    private int _createprivatetemplatefromsnapshotwait;
    @Override
    public UserVmVO getVirtualMachine(long vmId) {
        return _vmDao.findById(vmId);
    }

    @Override
    public List<? extends UserVm> getVirtualMachines(long hostId) {
        return _vmDao.listByHostId(hostId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_RESETPASSWORD, eventDescription = "resetting Vm password", async = true)
    public UserVm resetVMPassword(ResetVMPasswordCmd cmd, String password) throws ResourceUnavailableException, InsufficientCapacityException {
        Account caller = UserContext.current().getCaller();
        Long vmId = cmd.getId();
        UserVmVO userVm = _vmDao.findById(cmd.getId());
        _vmDao.loadDetails(userVm);

        // Do parameters input validation
        if (userVm == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + cmd.getId());
        }

        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(userVm.getTemplateId());
        if (template == null || !template.getEnablePassword()) {
            throw new InvalidParameterValueException("Fail to reset password for the virtual machine, the template is not password enabled");
        }

        if (userVm.getState() == State.Error || userVm.getState() == State.Expunging) {
            s_logger.error("vm is not in the right state: " + vmId);
            throw new InvalidParameterValueException("Vm with id " + vmId + " is not in the right state");
        }

        _accountMgr.checkAccess(caller, null, true, userVm);

        boolean result = resetVMPasswordInternal(cmd, password);

        if (result) {
            userVm.setPassword(password);
            //update the password in vm_details table too 
            // Check if an SSH key pair was selected for the instance and if so use it to encrypt & save the vm password
            String sshPublicKey = userVm.getDetail("SSH.PublicKey");
            if (sshPublicKey != null && !sshPublicKey.equals("") && password != null && !password.equals("saved_password")) {
                String encryptedPasswd = RSAHelper.encryptWithSSHPublicKey(sshPublicKey, password);
                if (encryptedPasswd == null) {
                    throw new CloudRuntimeException("Error encrypting password");
                }

                userVm.setDetail("Encrypted.Password", encryptedPasswd);
                _vmDao.saveDetails(userVm);
            }
        }

        return userVm;
    }

    private boolean resetVMPasswordInternal(ResetVMPasswordCmd cmd, String password) throws ResourceUnavailableException, InsufficientCapacityException {
        Long vmId = cmd.getId();
        Long userId = UserContext.current().getCallerUserId();
        VMInstanceVO vmInstance = _vmDao.findById(vmId);

        if (password == null || password.equals("")) {
            return false;
        }

        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(vmInstance.getTemplateId());
        if (template.getEnablePassword()) {
            Nic defaultNic = _networkMgr.getDefaultNic(vmId);
            if (defaultNic == null) {
                s_logger.error("Unable to reset password for vm " + vmInstance + " as the instance doesn't have default nic");
                return false;
            }

            Network defaultNetwork = _networkDao.findById(defaultNic.getNetworkId());
            NicProfile defaultNicProfile = new NicProfile(defaultNic, defaultNetwork, null, null, null, _networkMgr.isSecurityGroupSupportedInNetwork(defaultNetwork), _networkMgr.getNetworkTag(template.getHypervisorType(), defaultNetwork));
            VirtualMachineProfile<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vmInstance);
            vmProfile.setParameter(VirtualMachineProfile.Param.VmPassword, password);

            List<? extends UserDataServiceProvider> elements = _networkMgr.getPasswordResetElements();

            boolean result = true;
            for (UserDataServiceProvider element : elements) {
                if (!element.savePassword(defaultNetwork, defaultNicProfile, vmProfile)) {
                    result = false;
                }
            }

            // Need to reboot the virtual machine so that the password gets redownloaded from the DomR, and reset on the VM
            if (!result) {
                s_logger.debug("Failed to reset password for the virutal machine; no need to reboot the vm");
                return false;
            } else {
                if (vmInstance.getState() == State.Stopped) {
                    s_logger.debug("Vm " + vmInstance + " is stopped, not rebooting it as a part of password reset");
                    return true;
                }
                
                if (rebootVirtualMachine(userId, vmId) == null) { 
                    s_logger.warn("Failed to reboot the vm " + vmInstance);
                    return false;
                } else {
                    s_logger.debug("Vm " + vmInstance + " is rebooted successfully as a part of password reset");
                    return true;
                }
            }
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Reset password called for a vm that is not using a password enabled template");
            }
            return false;
        }
    }

    @Override
    public boolean stopVirtualMachine(long userId, long vmId) {
        boolean status = false;
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Stopping vm=" + vmId);
        }
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is either removed or deleted.");
            }
            return true;
        }

        User user = _userDao.findById(userId);
        Account account = _accountDao.findById(user.getAccountId());

        try {
            status = _itMgr.stop(vm, user, account);
        } catch (ResourceUnavailableException e) {
            s_logger.debug("Unable to stop due to ", e);
            status = false;
        }

        if (status) {
            return status;
        } else {
            return status;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_ATTACH, eventDescription = "attaching volume", async = true)
    public Volume attachVolumeToVM(AttachVolumeCmd command) {
        Long vmId = command.getVirtualMachineId();
        Long volumeId = command.getId();
        Long deviceId = command.getDeviceId();
        Account caller = UserContext.current().getCaller();

        // Check that the volume ID is valid
        VolumeVO volume = _volsDao.findById(volumeId);
        // Check that the volume is a data volume
        if (volume == null || volume.getVolumeType() != Volume.Type.DATADISK) {
            throw new InvalidParameterValueException("Please specify a valid data volume.");
        }

        // Check that the volume is stored on shared storage
        if (!(Volume.State.Allocated.equals(volume.getState())) && !_storageMgr.volumeOnSharedStoragePool(volume)) {
            throw new InvalidParameterValueException("Please specify a volume that has been created on a shared storage pool.");
        }

        if (!(Volume.State.Allocated.equals(volume.getState()) || Volume.State.Ready.equals(volume.getState()))) {
            throw new InvalidParameterValueException("Volume state must be in Allocated or Ready state");
        }

        // Check that the volume is not currently attached to any VM
        if (volume.getInstanceId() != null) {
            throw new InvalidParameterValueException("Please specify a volume that is not attached to any VM.");
        }

        // Check that the volume is not destroyed
        if (volume.getState() == Volume.State.Destroy) {
            throw new InvalidParameterValueException("Please specify a volume that is not destroyed.");
        }

        // Check that the virtual machine ID is valid and it's a user vm
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null || vm.getType() != VirtualMachine.Type.User) {
            throw new InvalidParameterValueException("Please specify a valid User VM.");
        }

        // Check that the VM is in the correct state
        if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
            throw new InvalidParameterValueException("Please specify a VM that is either running or stopped.");
        }

        // Check that the device ID is valid
        if (deviceId != null) {
            if (deviceId.longValue() == 0) {
                throw new InvalidParameterValueException("deviceId can't be 0, which is used by Root device");
            }
        }

        // Check that the VM has less than 6 data volumes attached
        List<VolumeVO> existingDataVolumes = _volsDao.findByInstanceAndType(vmId, Volume.Type.DATADISK);
        if (existingDataVolumes.size() >= 6) {
            throw new InvalidParameterValueException("The specified VM already has the maximum number of data disks (6). Please specify another VM.");
        }

        // Check that the VM and the volume are in the same zone
        if (vm.getDataCenterIdToDeployIn() != volume.getDataCenterId()) {
            throw new InvalidParameterValueException("Please specify a VM that is in the same zone as the volume.");
        }

        //permission check
        _accountMgr.checkAccess(caller, null, true, volume, vm);

        VolumeVO rootVolumeOfVm = null;
        List<VolumeVO> rootVolumesOfVm = _volsDao.findByInstanceAndType(vmId, Volume.Type.ROOT);
        if (rootVolumesOfVm.size() != 1) {
            throw new CloudRuntimeException("The VM " + vm.getHostName() + " has more than one ROOT volume and is in an invalid state. Please contact Cloud Support.");
        } else {
            rootVolumeOfVm = rootVolumesOfVm.get(0);
        }

        HypervisorType rootDiskHyperType = _volsDao.getHypervisorType(rootVolumeOfVm.getId());

        if (volume.getState().equals(Volume.State.Allocated)) {
            /* Need to create the volume */
            VMTemplateVO rootDiskTmplt = _templateDao.findById(vm.getTemplateId());
            DataCenterVO dcVO = _dcDao.findById(vm.getDataCenterIdToDeployIn());
            HostPodVO pod = _podDao.findById(vm.getPodIdToDeployIn());
            StoragePoolVO rootDiskPool = _storagePoolDao.findById(rootVolumeOfVm.getPoolId());
            ServiceOfferingVO svo = _serviceOfferingDao.findById(vm.getServiceOfferingId());
            DiskOfferingVO diskVO = _diskOfferingDao.findById(volume.getDiskOfferingId());

            volume = _storageMgr.createVolume(volume, vm, rootDiskTmplt, dcVO, pod, rootDiskPool.getClusterId(), svo, diskVO, new ArrayList<StoragePoolVO>(), volume.getSize(), rootDiskHyperType);

            if (volume == null) {
                throw new CloudRuntimeException("Failed to create volume when attaching it to VM: " + vm.getHostName());
            }
        }

        HypervisorType dataDiskHyperType = _volsDao.getHypervisorType(volume.getId());
        if (rootDiskHyperType != dataDiskHyperType) {
            throw new InvalidParameterValueException("Can't attach a volume created by: " + dataDiskHyperType + " to a " + rootDiskHyperType + " vm");
        }

        List<VolumeVO> vols = _volsDao.findByInstance(vmId);
        if (deviceId != null) {
            if (deviceId.longValue() > 15 || deviceId.longValue() == 0 || deviceId.longValue() == 3) {
                throw new RuntimeException("deviceId should be 1,2,4-15");
            }
            for (VolumeVO vol : vols) {
                if (vol.getDeviceId().equals(deviceId)) {
                    throw new RuntimeException("deviceId " + deviceId + " is used by VM " + vm.getHostName());
                }
            }
        } else {
            // allocate deviceId here
            List<String> devIds = new ArrayList<String>();
            for (int i = 1; i < 15; i++) {
                devIds.add(String.valueOf(i));
            }
            devIds.remove("3");
            for (VolumeVO vol : vols) {
                devIds.remove(vol.getDeviceId().toString().trim());
            }
            deviceId = Long.parseLong(devIds.iterator().next());
        }

        StoragePoolVO vmRootVolumePool = _storagePoolDao.findById(rootVolumeOfVm.getPoolId());
        DiskOfferingVO volumeDiskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
        String[] volumeTags = volumeDiskOffering.getTagsArray();

        StoragePoolVO sourcePool = _storagePoolDao.findById(volume.getPoolId());
        List<StoragePoolVO> sharedVMPools = _storagePoolDao.findPoolsByTags(vmRootVolumePool.getDataCenterId(), vmRootVolumePool.getPodId(), vmRootVolumePool.getClusterId(), volumeTags, true);
        boolean moveVolumeNeeded = true;
        if (sharedVMPools.size() == 0) {
            String poolType;
            if (vmRootVolumePool.getClusterId() != null) {
                poolType = "cluster";
            } else if (vmRootVolumePool.getPodId() != null) {
                poolType = "pod";
            } else {
                poolType = "zone";
            }
            throw new CloudRuntimeException("There are no storage pools in the VM's " + poolType + " with all of the volume's tags (" + volumeDiskOffering.getTags() + ").");
        } else {
            Long sourcePoolDcId = sourcePool.getDataCenterId();
            Long sourcePoolPodId = sourcePool.getPodId();
            Long sourcePoolClusterId = sourcePool.getClusterId();
            for (StoragePoolVO vmPool : sharedVMPools) {
                Long vmPoolDcId = vmPool.getDataCenterId();
                Long vmPoolPodId = vmPool.getPodId();
                Long vmPoolClusterId = vmPool.getClusterId();

                if (sourcePoolDcId == vmPoolDcId && sourcePoolPodId == vmPoolPodId && sourcePoolClusterId == vmPoolClusterId) {
                    moveVolumeNeeded = false;
                    break;
                }
            }
        }

        if (moveVolumeNeeded) {
            // Move the volume to a storage pool in the VM's zone, pod, or cluster
            try {
                volume = _storageMgr.moveVolume(volume, vmRootVolumePool.getDataCenterId(), vmRootVolumePool.getPodId(), vmRootVolumePool.getClusterId(), dataDiskHyperType);
            } catch (ConcurrentOperationException e) {
                throw new CloudRuntimeException(e.toString());
            }
        }

        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Trying to attaching volume " + volumeId + " to vm instance:" + vm.getId() + ", update async job-" + job.getId() + " progress status");
            }

            _asyncMgr.updateAsyncJobAttachment(job.getId(), "volume", volumeId);
            _asyncMgr.updateAsyncJobStatus(job.getId(), BaseCmd.PROGRESS_INSTANCE_CREATED, volumeId);
        }

        String errorMsg = "Failed to attach volume: " + volume.getName() + " to VM: " + vm.getHostName();
        boolean sendCommand = (vm.getState() == State.Running);
        AttachVolumeAnswer answer = null;
        Long hostId = vm.getHostId();
        if (hostId == null) {
            hostId = vm.getLastHostId();
            HostVO host = _hostDao.findById(hostId);
            if (host != null && host.getHypervisorType() == HypervisorType.VMware) {
                sendCommand = true;
            }
        }

        if (sendCommand) {
            StoragePoolVO volumePool = _storagePoolDao.findById(volume.getPoolId());
            AttachVolumeCommand cmd = new AttachVolumeCommand(true, vm.getInstanceName(), volume.getPoolType(), volume.getFolder(), volume.getPath(), volume.getName(), deviceId, volume.getChainInfo());
            cmd.setPoolUuid(volumePool.getUuid());

            try {
                answer = (AttachVolumeAnswer) _agentMgr.send(hostId, cmd);
            } catch (Exception e) {
                throw new CloudRuntimeException(errorMsg + " due to: " + e.getMessage());
            }
        }

        if (!sendCommand || (answer != null && answer.getResult())) {
            // Mark the volume as attached
            if (sendCommand) {
                _volsDao.attachVolume(volume.getId(), vmId, answer.getDeviceId());
            } else {
                _volsDao.attachVolume(volume.getId(), vmId, deviceId);
            }
            return _volsDao.findById(volumeId);
        } else {
            if (answer != null) {
                String details = answer.getDetails();
                if (details != null && !details.isEmpty()) {
                    errorMsg += "; " + details;
                }
            }
            throw new CloudRuntimeException(errorMsg);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_DETACH, eventDescription = "detaching volume", async = true)
    public Volume detachVolumeFromVM(DetachVolumeCmd cmmd) {
        Account caller = UserContext.current().getCaller();
        if ((cmmd.getId() == null && cmmd.getDeviceId() == null && cmmd.getVirtualMachineId() == null) || (cmmd.getId() != null && (cmmd.getDeviceId() != null || cmmd.getVirtualMachineId() != null))
                || (cmmd.getId() == null && (cmmd.getDeviceId() == null || cmmd.getVirtualMachineId() == null))) {
            throw new InvalidParameterValueException("Please provide either a volume id, or a tuple(device id, instance id)");
        }

        Long volumeId = cmmd.getId();
        VolumeVO volume = null;

        if (volumeId != null) {
            volume = _volsDao.findById(volumeId);
        } else {
            volume = _volsDao.findByInstanceAndDeviceId(cmmd.getVirtualMachineId(), cmmd.getDeviceId()).get(0);
        }

        Long vmId = null;

        if (cmmd.getVirtualMachineId() == null) {
            vmId = volume.getInstanceId();
        } else {
            vmId = cmmd.getVirtualMachineId();
        }

        // Check that the volume ID is valid
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to find volume with ID: " + volumeId);
        }

        // Permissions check
        _accountMgr.checkAccess(caller, null, true, volume);

        // Check that the volume is a data volume
        if (volume.getVolumeType() != Volume.Type.DATADISK) {
            throw new InvalidParameterValueException("Please specify a data volume.");
        }

        // Check that the volume is stored on shared storage
        if (!_storageMgr.volumeOnSharedStoragePool(volume)) {
            throw new InvalidParameterValueException("Please specify a volume that has been created on a shared storage pool.");
        }

        // Check that the volume is currently attached to a VM
        if (vmId == null) {
            throw new InvalidParameterValueException("The specified volume is not attached to a VM.");
        }

        // Check that the VM is in the correct state
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm.getState() != State.Running && vm.getState() != State.Stopped && vm.getState() != State.Destroyed) {
            throw new InvalidParameterValueException("Please specify a VM that is either running or stopped.");
        }

        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Trying to attaching volume " + volumeId + "to vm instance:" + vm.getId() + ", update async job-" + job.getId() + " progress status");
            }

            _asyncMgr.updateAsyncJobAttachment(job.getId(), "volume", volumeId);
            _asyncMgr.updateAsyncJobStatus(job.getId(), BaseCmd.PROGRESS_INSTANCE_CREATED, volumeId);
        }

        String errorMsg = "Failed to detach volume: " + volume.getName() + " from VM: " + vm.getHostName();
        boolean sendCommand = (vm.getState() == State.Running);
        Answer answer = null;

        if (sendCommand) {
            AttachVolumeCommand cmd = new AttachVolumeCommand(false, vm.getInstanceName(), volume.getPoolType(), volume.getFolder(), volume.getPath(), volume.getName(),
                    cmmd.getDeviceId() != null ? cmmd.getDeviceId() : volume.getDeviceId(), volume.getChainInfo());

            StoragePoolVO volumePool = _storagePoolDao.findById(volume.getPoolId());
            cmd.setPoolUuid(volumePool.getUuid());

            try {
                answer = _agentMgr.send(vm.getHostId(), cmd);
            } catch (Exception e) {
                throw new CloudRuntimeException(errorMsg + " due to: " + e.getMessage());
            }
        }

        if (!sendCommand || (answer != null && answer.getResult())) {
            // Mark the volume as detached
            _volsDao.detachVolume(volume.getId());
            if (answer != null && answer instanceof AttachVolumeAnswer) {
                volume.setChainInfo(((AttachVolumeAnswer) answer).getChainInfo());
                _volsDao.update(volume.getId(), volume);
            }

            return _volsDao.findById(volumeId);
        } else {

            if (answer != null) {
                String details = answer.getDetails();
                if (details != null && !details.isEmpty()) {
                    errorMsg += "; " + details;
                }
            }

            throw new CloudRuntimeException(errorMsg);
        }
    }

    @Override
    public boolean attachISOToVM(long vmId, long isoId, boolean attach) {
        UserVmVO vm = _vmDao.findById(vmId);

        if (vm == null) {
            return false;
        } else if (vm.getState() != State.Running) {
            return true;
        }
        String isoPath;
        VMTemplateVO tmplt = _templateDao.findById(isoId);
        if (tmplt == null) {
            s_logger.warn("ISO: " + isoId + " does not exist");
            return false;
        }
        // Get the path of the ISO
        Pair<String, String> isoPathPair = null;
        if (tmplt.getTemplateType() == TemplateType.PERHOST) {
            isoPath = tmplt.getName();
        } else {
            isoPathPair = _storageMgr.getAbsoluteIsoPath(isoId, vm.getDataCenterIdToDeployIn());
            if (isoPathPair == null) {
                s_logger.warn("Couldn't get absolute iso path");
                return false;
            } else {
                isoPath = isoPathPair.first();
            }
        }

        String vmName = vm.getInstanceName();

        HostVO host = _hostDao.findById(vm.getHostId());
        if (host == null) {
            s_logger.warn("Host: " + vm.getHostId() + " does not exist");
            return false;
        }
        AttachIsoCommand cmd = new AttachIsoCommand(vmName, isoPath, attach);
        if (isoPathPair != null) {
            cmd.setStoreUrl(isoPathPair.second());
        }
        Answer a = _agentMgr.easySend(vm.getHostId(), cmd);

        return (a != null && a.getResult());
    }

    private UserVm rebootVirtualMachine(long userId, long vmId) throws InsufficientCapacityException, ResourceUnavailableException {
        UserVmVO vm = _vmDao.findById(vmId);
        User caller = _accountMgr.getActiveUser(userId);
        Account owner = _accountMgr.getAccount(vm.getAccountId());

        if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getRemoved() != null) {
            s_logger.warn("Vm id=" + vmId + " doesn't exist");
            return null;
        }

        if (vm.getState() == State.Running && vm.getHostId() != null) {
            return _itMgr.reboot(vm, null, caller, owner);
        } else {
            s_logger.error("Vm id=" + vmId + " is not in Running state, failed to reboot");
            return null;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_UPGRADE, eventDescription = "upgrading Vm")
    /*
     * TODO: cleanup eventually - Refactored API call
     */
    public UserVm upgradeVirtualMachine(UpgradeVMCmd cmd) {
        Long virtualMachineId = cmd.getId();
        Long serviceOfferingId = cmd.getServiceOfferingId();
        Account caller = UserContext.current().getCaller();

        // Verify input parameters
        UserVmVO vmInstance = _vmDao.findById(virtualMachineId);
        if (vmInstance == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + virtualMachineId);
        }

        _accountMgr.checkAccess(caller, null, true, vmInstance);

        // Check that the specified service offering ID is valid
        ServiceOfferingVO newServiceOffering = _offeringDao.findById(serviceOfferingId);
        if (newServiceOffering == null) {
            throw new InvalidParameterValueException("Unable to find a service offering with id " + serviceOfferingId);
        }

        // Check that the VM is stopped
        if (!vmInstance.getState().equals(State.Stopped)) {
            s_logger.warn("Unable to upgrade virtual machine " + vmInstance.toString() + " in state " + vmInstance.getState());
            throw new InvalidParameterValueException("Unable to upgrade virtual machine " + vmInstance.toString() + " in state " + vmInstance.getState()
                    + "; make sure the virtual machine is stopped and not in an error state before upgrading.");
        }

        // Check if the service offering being upgraded to is what the VM is already running with
        if (vmInstance.getServiceOfferingId() == newServiceOffering.getId()) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Not upgrading vm " + vmInstance.toString() + " since it already has the requested service offering (" + newServiceOffering.getName() + ")");
            }

            throw new InvalidParameterValueException("Not upgrading vm " + vmInstance.toString() + " since it already has the requested service offering (" + newServiceOffering.getName() + ")");
        }

        ServiceOfferingVO currentServiceOffering = _offeringDao.findByIdIncludingRemoved(vmInstance.getServiceOfferingId());

        // Check that the service offering being upgraded to has the same Guest IP type as the VM's current service offering
        // NOTE: With the new network refactoring in 2.2, we shouldn't need the check for same guest IP type anymore.
        /*
         * if (!currentServiceOffering.getGuestIpType().equals(newServiceOffering.getGuestIpType())) { String errorMsg =
         * "The service offering being upgraded to has a guest IP type: " + newServiceOffering.getGuestIpType(); errorMsg +=
         * ". Please select a service offering with the same guest IP type as the VM's current service offering (" +
         * currentServiceOffering.getGuestIpType() + ")."; throw new InvalidParameterValueException(errorMsg); }
         */

        // Check that the service offering being upgraded to has the same storage pool preference as the VM's current service
        // offering
        if (currentServiceOffering.getUseLocalStorage() != newServiceOffering.getUseLocalStorage()) {
            throw new InvalidParameterValueException("Unable to upgrade virtual machine " + vmInstance.toString()
                    + ", cannot switch between local storage and shared storage service offerings.  Current offering useLocalStorage=" + currentServiceOffering.getUseLocalStorage()
                    + ", target offering useLocalStorage=" + newServiceOffering.getUseLocalStorage());
        }

        // Check that there are enough resources to upgrade the service offering
        if (!_itMgr.isVirtualMachineUpgradable(vmInstance, newServiceOffering)) {
            throw new InvalidParameterValueException("Unable to upgrade virtual machine, not enough resources available for an offering of " + newServiceOffering.getCpu() + " cpu(s) at "
                    + newServiceOffering.getSpeed() + " Mhz, and " + newServiceOffering.getRamSize() + " MB of memory");
        }

        // Check that the service offering being upgraded to has all the tags of the current service offering
        List<String> currentTags = _configMgr.csvTagsToList(currentServiceOffering.getTags());
        List<String> newTags = _configMgr.csvTagsToList(newServiceOffering.getTags());
        if (!newTags.containsAll(currentTags)) {
            throw new InvalidParameterValueException("Unable to upgrade virtual machine; the new service offering does not have all the tags of the "
                    + "current service offering. Current service offering tags: " + currentTags + "; " + "new service offering tags: " + newTags);
        }

        UserVmVO vmForUpdate = _vmDao.createForUpdate();
        vmForUpdate.setServiceOfferingId(serviceOfferingId);
        vmForUpdate.setHaEnabled(_serviceOfferingDao.findById(serviceOfferingId).getOfferHA());
        vmForUpdate.setLimitCpuUse(_serviceOfferingDao.findById(serviceOfferingId).getLimitCpuUse());
        _vmDao.update(vmInstance.getId(), vmForUpdate);

        return _vmDao.findById(vmInstance.getId());
    }

    @Override
    public HashMap<Long, VmStatsEntry> getVirtualMachineStatistics(long hostId, String hostName, List<Long> vmIds) throws CloudRuntimeException {
        HashMap<Long, VmStatsEntry> vmStatsById = new HashMap<Long, VmStatsEntry>();

        if (vmIds.isEmpty()) {
            return vmStatsById;
        }

        List<String> vmNames = new ArrayList<String>();

        for (Long vmId : vmIds) {
            UserVmVO vm = _vmDao.findById(vmId);
            vmNames.add(vm.getInstanceName());
        }

        Answer answer = _agentMgr.easySend(hostId, new GetVmStatsCommand(vmNames, _hostDao.findById(hostId).getGuid(), hostName));
        if (answer == null || !answer.getResult()) {
            s_logger.warn("Unable to obtain VM statistics.");
            return null;
        } else {
            HashMap<String, VmStatsEntry> vmStatsByName = ((GetVmStatsAnswer) answer).getVmStatsMap();

            if (vmStatsByName == null) {
                s_logger.warn("Unable to obtain VM statistics.");
                return null;
            }

            for (String vmName : vmStatsByName.keySet()) {
                vmStatsById.put(vmIds.get(vmNames.indexOf(vmName)), vmStatsByName.get(vmName));
            }
        }

        return vmStatsById;
    }

    @Override
    @DB
    public UserVm recoverVirtualMachine(RecoverVMCmd cmd) throws ResourceAllocationException, CloudRuntimeException {

        Long vmId = cmd.getId();
        Account caller = UserContext.current().getCaller();

        // Verify input parameters
        UserVmVO vm = _vmDao.findById(vmId.longValue());

        if (vm == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }

        //check permissions
        _accountMgr.checkAccess(caller, null, true, vm);

        if (vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is removed: " + vmId);
            }
            throw new InvalidParameterValueException("Unable to find vm by id " + vmId);
        }

        if (vm.getState() != State.Destroyed) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("vm is not in the right state: " + vmId);
            }
            throw new InvalidParameterValueException("Vm with id " + vmId + " is not in the right state");
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Recovering vm " + vmId);
        }

        Transaction txn = Transaction.currentTxn();
        AccountVO account = null;
        txn.start();

        account = _accountDao.lockRow(vm.getAccountId(), true);

        // if the account is deleted, throw error
        if (account.getRemoved() != null) {
            throw new CloudRuntimeException("Unable to recover VM as the account is deleted");
        }

        // First check that the maximum number of UserVMs for the given accountId will not be exceeded
        _resourceLimitMgr.checkResourceLimit(account, ResourceType.user_vm);

        _haMgr.cancelDestroy(vm, vm.getHostId());

        try {
            if (!_itMgr.stateTransitTo(vm, VirtualMachine.Event.RecoveryRequested, null)) {
                s_logger.debug("Unable to recover the vm because it is not in the correct state: " + vmId);
                throw new InvalidParameterValueException("Unable to recover the vm because it is not in the correct state: " + vmId);
            }
        } catch (NoTransitionException e) {
            throw new InvalidParameterValueException("Unable to recover the vm because it is not in the correct state: " + vmId);
        }

        // Recover the VM's disks
        List<VolumeVO> volumes = _volsDao.findByInstance(vmId);
        for (VolumeVO volume : volumes) {
            if (volume.getVolumeType().equals(Volume.Type.ROOT)) {
                // Create an event
                Long templateId = volume.getTemplateId();
                Long diskOfferingId = volume.getDiskOfferingId();
                Long offeringId = null;
                if (diskOfferingId != null) {
                    DiskOfferingVO offering = _diskOfferingDao.findById(diskOfferingId);
                    if (offering != null && (offering.getType() == DiskOfferingVO.Type.Disk)) {
                        offeringId = offering.getId();
                    }
                }
                UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), offeringId, templateId,
                        volume.getSize());
                _usageEventDao.persist(usageEvent);
            }
        }

        _resourceLimitMgr.incrementResourceCount(account.getId(), ResourceType.volume, new Long(volumes.size()));

        _resourceLimitMgr.incrementResourceCount(account.getId(), ResourceType.user_vm);

        txn.commit();

        return _vmDao.findById(vmId);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        _configDao = locator.getDao(ConfigurationDao.class);
        if (_configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }

        Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);

        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }

        String value = _configDao.getValue(Config.CreatePrivateTemplateFromVolumeWait.toString());
        _createprivatetemplatefromvolumewait = NumbersUtil.parseInt(value, Integer.parseInt(Config.CreatePrivateTemplateFromVolumeWait.getDefaultValue()));

        value = _configDao.getValue(Config.CreatePrivateTemplateFromSnapshotWait.toString());
        _createprivatetemplatefromsnapshotwait = NumbersUtil.parseInt(value, Integer.parseInt(Config.CreatePrivateTemplateFromSnapshotWait.getDefaultValue()));

        String workers = configs.get("expunge.workers");
        int wrks = NumbersUtil.parseInt(workers, 10);

        String time = configs.get("expunge.interval");
        _expungeInterval = NumbersUtil.parseInt(time, 86400);

        time = configs.get("expunge.delay");
        _expungeDelay = NumbersUtil.parseInt(time, _expungeInterval);

        _executor = Executors.newScheduledThreadPool(wrks, new NamedThreadFactory("UserVm-Scavenger"));

        _itMgr.registerGuru(VirtualMachine.Type.User, this);

        VirtualMachine.State.getStateMachine().registerListener(new UserVmStateListener(_usageEventDao, _networkDao, _nicDao));

        s_logger.info("User VM Manager is configured.");

        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        _executor.scheduleWithFixedDelay(new ExpungeTask(), _expungeInterval, _expungeInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        _executor.shutdown();
        return true;
    }

    protected UserVmManagerImpl() {
    }

    public String getRandomPrivateTemplateName() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Long convertToId(String vmName) {
        if (!VirtualMachineName.isValidVmName(vmName, _instance)) {
            return null;
        }
        return VirtualMachineName.getVmId(vmName);
    }

    @Override
    public boolean expunge(UserVmVO vm, long callerUserId, Account caller) {
        UserContext ctx = UserContext.current();
        ctx.setAccountId(vm.getAccountId());

        try {
        	//expunge the vm
            if (!_itMgr.advanceExpunge(vm, _accountMgr.getSystemUser(), caller)) {
                s_logger.info("Did not expunge " + vm);
                return false;
            }

            // Only if vm is not expunged already, cleanup it's resources
            if (vm != null && vm.getRemoved() == null) {
                // Cleanup vm resources - all the PF/LB/StaticNat rules associated with vm
                s_logger.debug("Starting cleaning up vm " + vm + " resources...");
                if (cleanupVmResources(vm.getId())) {
                    s_logger.debug("Successfully cleaned up vm " + vm + " resources as a part of expunge process");
                } else {
                    s_logger.warn("Failed to cleanup resources as a part of vm " + vm + " expunge");
                    return false;
                }

                _itMgr.remove(vm, _accountMgr.getSystemUser(), caller);
            }

            return true;

        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to expunge  " + vm, e);
            return false;
        } catch (OperationTimedoutException e) {
            s_logger.warn("Operation time out on expunging " + vm, e);
            return false;
        } catch (ConcurrentOperationException e) {
            s_logger.warn("Concurrent operations on expunging " + vm, e);
            return false;
        }
    }

    private boolean cleanupVmResources(long vmId) {
        boolean success = true;
        //Remove vm from security groups
        _securityGroupMgr.removeInstanceFromGroups(vmId);

        //Remove vm from instance group
        removeInstanceFromInstanceGroup(vmId);

        //cleanup firewall rules
        if (_firewallMgr.revokeFirewallRulesForVm(vmId)) {
            s_logger.debug("Firewall rules are removed successfully as a part of vm id=" + vmId + " expunge");
        } else {
            success = false;
            s_logger.warn("Fail to remove firewall rules as a part of vm id=" + vmId + " expunge");
        }

        //cleanup port forwarding rules
        if (_rulesMgr.revokePortForwardingRulesForVm(vmId)) {
            s_logger.debug("Port forwarding rules are removed successfully as a part of vm id=" + vmId + " expunge");
        } else {
            success = false;
            s_logger.warn("Fail to remove port forwarding rules as a part of vm id=" + vmId + " expunge");
        }

        // cleanup load balancer rules
        if (_lbMgr.removeVmFromLoadBalancers(vmId)) {
            s_logger.debug("Removed vm id=" + vmId + " from all load balancers as a part of expunge process");
        } else {
            success = false;
            s_logger.warn("Fail to remove vm id=" + vmId + " from load balancers as a part of expunge process");
        }

        // If vm is assigned to static nat, disable static nat for the ip address and disassociate ip if elasticIP is enabled
        IPAddressVO ip = _ipAddressDao.findByAssociatedVmId(vmId);
        try {
            if (ip != null) {
                if (_rulesMgr.disableStaticNat(ip.getId(), _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM), User.UID_SYSTEM, true)) {
                    s_logger.debug("Disabled 1-1 nat for ip address " + ip + " as a part of vm id=" + vmId + " expunge");
                } else {
                    s_logger.warn("Failed to disable static nat for ip address " + ip + " as a part of vm id=" + vmId + " expunge");
                    success = false;
                }
            }
        } catch (ResourceUnavailableException e) {
            success = false;
            s_logger.warn("Failed to disable static nat for ip address " + ip + " as a part of vm id=" + vmId + " expunge because resource is unavailable", e);
        }

        return success;
    }

    @Override
    public void deletePrivateTemplateRecord(Long templateId) {
        if (templateId != null) {
            _templateDao.remove(templateId);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_CREATE, eventDescription = "creating template", create = true)
    public VMTemplateVO createPrivateTemplateRecord(CreateTemplateCmd cmd, Account templateOwner) throws ResourceAllocationException {
        Long userId = UserContext.current().getCallerUserId();

        Account caller = UserContext.current().getCaller();
        boolean isAdmin = (isAdmin(caller.getType()));

        _accountMgr.checkAccess(caller, null, true, templateOwner);

        String name = cmd.getTemplateName();
        if ((name == null) || (name.length() > 32)) {
            throw new InvalidParameterValueException("Template name cannot be null and should be less than 32 characters");
        }

        if(cmd.getTemplateTag() != null){
            if (!_accountService.isRootAdmin(caller.getType())){
                throw new PermissionDeniedException("Parameter templatetag can only be specified by a Root Admin, permission denied");
            }
        }

        // do some parameter defaulting
        Integer bits = cmd.getBits();
        Boolean requiresHvm = cmd.getRequiresHvm();
        Boolean passwordEnabled = cmd.isPasswordEnabled();
        Boolean isPublic = cmd.isPublic();
        Boolean featured = cmd.isFeatured();
        int bitsValue = ((bits == null) ? 64 : bits.intValue());
        boolean requiresHvmValue = ((requiresHvm == null) ? true : requiresHvm.booleanValue());
        boolean passwordEnabledValue = ((passwordEnabled == null) ? false : passwordEnabled.booleanValue());
        if (isPublic == null) {
            isPublic = Boolean.FALSE;
        }
        boolean allowPublicUserTemplates = Boolean.parseBoolean(_configDao.getValue("allow.public.user.templates"));
        if (!isAdmin && !allowPublicUserTemplates && isPublic) {
            throw new PermissionDeniedException("Failed to create template " + name + ", only private templates can be created.");
        }

        Long volumeId = cmd.getVolumeId();
        Long snapshotId = cmd.getSnapshotId();
        if ((volumeId == null) && (snapshotId == null)) {
            throw new InvalidParameterValueException("Failed to create private template record, neither volume ID nor snapshot ID were specified.");
        }
        if ((volumeId != null) && (snapshotId != null)) {
            throw new InvalidParameterValueException("Failed to create private template record, please specify only one of volume ID (" + volumeId + ") and snapshot ID (" + snapshotId + ")");
        }

        HypervisorType hyperType;
        VolumeVO volume = null;
        VMTemplateVO privateTemplate = null;
        if (volumeId != null) { // create template from volume
            volume = _volsDao.findById(volumeId);
            if (volume == null) {
                throw new InvalidParameterValueException("Failed to create private template record, unable to find volume " + volumeId);
            }
            //check permissions
            _accountMgr.checkAccess(caller, null, true, volume);

            // If private template is created from Volume, check that the volume will not be active when the private template is
            // created
            if (!_storageMgr.volumeInactive(volume)) {
                String msg = "Unable to create private template for volume: " + volume.getName() + "; volume is attached to a non-stopped VM, please stop the VM first";
                if (s_logger.isInfoEnabled()) {
                    s_logger.info(msg);
                }
                throw new CloudRuntimeException(msg);
            }
            hyperType = _volsDao.getHypervisorType(volumeId);
        } else { // create template from snapshot
            SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
            if (snapshot == null) {
                throw new InvalidParameterValueException("Failed to create private template record, unable to find snapshot " + snapshotId);
            }
            
            volume = _volsDao.findById(snapshot.getVolumeId());
            VolumeVO snapshotVolume = _volsDao.findByIdIncludingRemoved(snapshot.getVolumeId());     

            //check permissions
            _accountMgr.checkAccess(caller, null, true, snapshot);

            if (snapshot.getStatus() != Snapshot.Status.BackedUp) {
                throw new InvalidParameterValueException("Snapshot id=" + snapshotId + " is not in " + Snapshot.Status.BackedUp + " state yet and can't be used for template creation");
            }

/*            
            // bug #11428. Operation not supported if vmware and snapshots parent volume = ROOT
            if(snapshot.getHypervisorType() == HypervisorType.VMware && snapshotVolume.getVolumeType() == Type.DATADISK){ 
                throw new UnsupportedServiceException("operation not supported, snapshot with id " + snapshotId + " is created from Data Disk");
            }
*/
            
            hyperType = snapshot.getHypervisorType();            
        }

        _resourceLimitMgr.checkResourceLimit(templateOwner, ResourceType.template);

        if (!isAdmin || featured == null) {
            featured = Boolean.FALSE;
        }
        Long guestOSId = cmd.getOsTypeId();
        GuestOSVO guestOS = _guestOSDao.findById(guestOSId);
        if (guestOS == null) {
            throw new InvalidParameterValueException("GuestOS with ID: " + guestOSId + " does not exist.");
        }

        String uniqueName = Long.valueOf((userId == null) ? 1 : userId).toString() + UUID.nameUUIDFromBytes(name.getBytes()).toString();
        Long nextTemplateId = _templateDao.getNextInSequence(Long.class, "id");
        String description = cmd.getDisplayText();
        boolean isExtractable = false;
        Long sourceTemplateId = null;
        if (volume != null) {
            VMTemplateVO template = ApiDBUtils.findTemplateById(volume.getTemplateId());
            isExtractable = template != null && template.isExtractable() && template.getTemplateType() != Storage.TemplateType.SYSTEM;
            if (template != null){
                sourceTemplateId = template.getId();
            }else if (volume.getVolumeType() == Type.ROOT){ //vm created out of blank template
                UserVm userVm = ApiDBUtils.findUserVmById(volume.getInstanceId());
                sourceTemplateId = userVm.getIsoId();
            }
        }
        String templateTag = cmd.getTemplateTag();
        if(templateTag != null){
            if(s_logger.isDebugEnabled()){
                s_logger.debug("Adding template tag: "+templateTag);
            }
        }
        privateTemplate = new VMTemplateVO(nextTemplateId, uniqueName, name, ImageFormat.RAW, isPublic, featured, isExtractable, TemplateType.USER, null, null, requiresHvmValue, bitsValue, templateOwner.getId(),
                null, description, passwordEnabledValue, guestOS.getId(), true, hyperType, templateTag, cmd.getDetails());
        if(sourceTemplateId != null){
            if(s_logger.isDebugEnabled()){
                s_logger.debug("This template is getting created from other template, setting source template Id to: "+sourceTemplateId);
            }
        }
        privateTemplate.setSourceTemplateId(sourceTemplateId);

        VMTemplateVO template = _templateDao.persist(privateTemplate);
        // Increment the number of templates
        if (template != null) {
        	if(cmd.getDetails() != null) {
        		_templateDetailsDao.persist(template.getId(), cmd.getDetails());
        	}
        	
            _resourceLimitMgr.incrementResourceCount(templateOwner.getId(), ResourceType.template);
        }

        if (template != null){
        	return template;
        }else {
        	throw new CloudRuntimeException("Failed to create a template");
        }
        
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_CREATE, eventDescription = "creating template", async = true)
    public VMTemplateVO createPrivateTemplate(CreateTemplateCmd command) throws CloudRuntimeException {
        Long userId = UserContext.current().getCallerUserId();
        if (userId == null) {
            userId = User.UID_SYSTEM;
        }
        long templateId = command.getEntityId();
        Long volumeId = command.getVolumeId();
        Long snapshotId = command.getSnapshotId();
        SnapshotCommand cmd = null;
        VMTemplateVO privateTemplate = null;

        String uniqueName = getRandomPrivateTemplateName();

        StoragePoolVO pool = null;
        HostVO secondaryStorageHost = null;
        Long zoneId = null;
        Long accountId = null;
        SnapshotVO snapshot = null;
        String secondaryStorageURL = null;
        try {
            if (snapshotId != null) { // create template from snapshot
                snapshot = _snapshotDao.findById(snapshotId);
                if (snapshot == null) {
                    throw new CloudRuntimeException("Unable to find Snapshot for Id " + snapshotId);
                }
                zoneId = snapshot.getDataCenterId();
                secondaryStorageHost = _snapshotMgr.getSecondaryStorageHost(snapshot);
                secondaryStorageURL = _snapshotMgr.getSecondaryStorageURL(snapshot);
                String name = command.getTemplateName();
                String backupSnapshotUUID = snapshot.getBackupSnapshotId();
                if (backupSnapshotUUID == null) {
                    throw new CloudRuntimeException("Unable to create private template from snapshot " + snapshotId + " due to there is no backupSnapshotUUID for this snapshot");
                }

                Long dcId = snapshot.getDataCenterId();
                accountId = snapshot.getAccountId();
                volumeId = snapshot.getVolumeId();

                String origTemplateInstallPath = null;
                List<StoragePoolVO> pools = _storageMgr.ListByDataCenterHypervisor(zoneId, snapshot.getHypervisorType());
                if (pools == null ||  pools.size() == 0 ) {
                    throw new CloudRuntimeException("Unable to find storage pools in zone " + zoneId);
                }
                pool = pools.get(0);
                if (snapshot.getVersion() != null && snapshot.getVersion().equalsIgnoreCase("2.1")) {
                    VolumeVO volume = _volsDao.findByIdIncludingRemoved(volumeId);
                    if (volume == null) {
                        throw new CloudRuntimeException("failed to upgrade snapshot " + snapshotId + " due to unable to find orignal volume:" + volumeId + ", try it later ");
                    }
                    if ( volume.getTemplateId() == null ) {
                        _snapshotDao.updateSnapshotVersion(volumeId, "2.1", "2.2");
                    } else {
                        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(volume.getTemplateId());
                        if (template == null) {
                            throw new CloudRuntimeException("failed to upgrade snapshot " + snapshotId + " due to unalbe to find orignal template :" + volume.getTemplateId() + ", try it later ");
                        }
                        Long origTemplateId = template.getId();
                        Long origTmpltAccountId = template.getAccountId();
                        if (!_volsDao.lockInLockTable(volumeId.toString(), 10)) {
                            throw new CloudRuntimeException("failed to upgrade snapshot " + snapshotId + " due to volume:" + volumeId + " is being used, try it later ");
                        }
                        cmd = new UpgradeSnapshotCommand(null, secondaryStorageURL, dcId, accountId, volumeId, origTemplateId, origTmpltAccountId, null, snapshot.getBackupSnapshotId(),
                                snapshot.getName(), "2.1");
                        if (!_volsDao.lockInLockTable(volumeId.toString(), 10)) {
                            throw new CloudRuntimeException("Creating template failed due to volume:" + volumeId + " is being used, try it later ");
                        }
                        Answer answer = null;
                        try {
                            answer = _storageMgr.sendToPool(pool, cmd);
                            cmd = null;
                        } catch (StorageUnavailableException e) {
                        } finally {
                            _volsDao.unlockFromLockTable(volumeId.toString());
                        }
                        if ((answer != null) && answer.getResult()) {
                            _snapshotDao.updateSnapshotVersion(volumeId, "2.1", "2.2");
                        } else {
                            throw new CloudRuntimeException("Unable to upgrade snapshot");
                        }
                    }
                }
                if( snapshot.getSwiftId() != null ) {
                    _snapshotMgr.downloadSnapshotsFromSwift(snapshot);
                }
                cmd = new CreatePrivateTemplateFromSnapshotCommand(pool.getUuid(), secondaryStorageURL, dcId, accountId, snapshot.getVolumeId(), backupSnapshotUUID, snapshot.getName(),
                        origTemplateInstallPath, templateId, name, _createprivatetemplatefromsnapshotwait);
            } else if (volumeId != null) {
                VolumeVO volume = _volsDao.findById(volumeId);
                if (volume == null) {
                    throw new CloudRuntimeException("Unable to find volume for Id " + volumeId);
                }
                accountId = volume.getAccountId();

                if (volume.getPoolId() == null) {
                    _templateDao.remove(templateId);
                    throw new CloudRuntimeException("Volume " + volumeId + " is empty, can't create template on it");
                }
                String vmName = _storageMgr.getVmNameOnVolume(volume);
                zoneId = volume.getDataCenterId();
                secondaryStorageHost = _storageMgr.getSecondaryStorageHost(zoneId);
                if (secondaryStorageHost == null) {
                    throw new CloudRuntimeException("Can not find the secondary storage for zoneId " + zoneId);
                }
                secondaryStorageURL = secondaryStorageHost.getStorageUrl();

                pool = _storagePoolDao.findById(volume.getPoolId());
                cmd = new CreatePrivateTemplateFromVolumeCommand(pool.getUuid(), secondaryStorageURL, templateId, accountId, command.getTemplateName(), uniqueName, volume.getPath(), vmName, _createprivatetemplatefromvolumewait);

            } else {
                throw new CloudRuntimeException("Creating private Template need to specify snapshotId or volumeId");
            }
            // FIXME: before sending the command, check if there's enough capacity
            // on the storage server to create the template

            // This can be sent to a KVM host too.
            CreatePrivateTemplateAnswer answer = null;
            if (snapshotId != null) {
                if (!_snapshotDao.lockInLockTable(snapshotId.toString(), 10)) {
                    throw new CloudRuntimeException("Creating template from snapshot failed due to snapshot:" + snapshotId + " is being used, try it later ");
                }
            } else {
                if (!_volsDao.lockInLockTable(volumeId.toString(), 10)) {
                    throw new CloudRuntimeException("Creating template from volume failed due to volume:" + volumeId + " is being used, try it later ");
                }
            }
            try {
                answer = (CreatePrivateTemplateAnswer) _storageMgr.sendToPool(pool, cmd);
            } catch (StorageUnavailableException e) {
            } finally {
                if (snapshotId != null) {
                    _snapshotDao.unlockFromLockTable(snapshotId.toString());
                } else {
                    _volsDao.unlockFromLockTable(volumeId.toString());
                }
            }
            if ((answer != null) && answer.getResult()) {
                privateTemplate = _templateDao.findById(templateId);
                String answerUniqueName = answer.getUniqueName();
                if (answerUniqueName != null) {
                    privateTemplate.setUniqueName(answerUniqueName);
                } else {
                    privateTemplate.setUniqueName(uniqueName);
                }
                ImageFormat format = answer.getImageFormat();
                if (format != null) {
                    privateTemplate.setFormat(format);
                } else {
                    // This never occurs.
                    // Specify RAW format makes it unusable for snapshots.
                    privateTemplate.setFormat(ImageFormat.RAW);
                }

                String checkSum = getChecksum(secondaryStorageHost.getId(), answer.getPath());

                Transaction txn = Transaction.currentTxn();

                txn.start();

                privateTemplate.setChecksum(checkSum);
                _templateDao.update(templateId, privateTemplate);

                // add template zone ref for this template
                _templateDao.addTemplateToZone(privateTemplate, zoneId);
                VMTemplateHostVO templateHostVO = new VMTemplateHostVO(secondaryStorageHost.getId(), templateId);
                templateHostVO.setDownloadPercent(100);
                templateHostVO.setDownloadState(Status.DOWNLOADED);
                templateHostVO.setInstallPath(answer.getPath());
                templateHostVO.setLastUpdated(new Date());
                templateHostVO.setSize(answer.getVirtualSize());
                templateHostVO.setPhysicalSize(answer.getphysicalSize());
                _templateHostDao.persist(templateHostVO);

                UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_TEMPLATE_CREATE, privateTemplate.getAccountId(), secondaryStorageHost.getDataCenterId(), privateTemplate.getId(),
                        privateTemplate.getName(), null, privateTemplate.getSourceTemplateId(), templateHostVO.getSize());
                _usageEventDao.persist(usageEvent);
                txn.commit();
            }
        } finally {
            if (snapshot != null && snapshot.getSwiftId() != null && secondaryStorageURL != null && zoneId != null && accountId != null && volumeId != null) {
                _snapshotMgr.deleteSnapshotsForVolume (secondaryStorageURL, zoneId, accountId, volumeId);
            }
            if (privateTemplate == null) {
                Transaction txn = Transaction.currentTxn();
                txn.start();
                // Remove the template record
                _templateDao.expunge(templateId);

                // decrement resource count
                if (accountId != null) {
                    _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.template);
                }
                txn.commit();
            }
        }
        
        if (privateTemplate != null){
        	return privateTemplate;
        }else {
        	throw new CloudRuntimeException("Failed to create a template");
        }        
    }

    @Override
    public String getChecksum(Long hostId, String templatePath){
        HostVO ssHost = _hostDao.findById(hostId);
        Host.Type type = ssHost.getType();
        if( type != Host.Type.SecondaryStorage && type != Host.Type.LocalSecondaryStorage ) {
            return null;
        }
        String secUrl = ssHost.getStorageUrl();
        Answer answer;
        answer = _agentMgr.sendToSecStorage(ssHost, new ComputeChecksumCommand(secUrl, templatePath));
        if(answer != null && answer.getResult()) {
            return answer.getDetails();
        }
        return null;
    }

    // used for vm transitioning to error state
    private void updateVmStateForFailedVmCreation(Long vmId) {
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm != null) {
            if (vm.getState().equals(State.Stopped)) {
                try {
                    _itMgr.stateTransitTo(vm, VirtualMachine.Event.OperationFailedToError, null);
                } catch (NoTransitionException e1) {
                    s_logger.warn(e1.getMessage());
                }
                // destroy associated volumes for vm in error state
                // get all volumes in non destroyed state
                List<VolumeVO> volumesForThisVm = _volsDao.findUsableVolumesForInstance(vm.getId());
                for (VolumeVO volume : volumesForThisVm) {
                    try {
                        if (volume.getState() != Volume.State.Destroy) {
                            _storageMgr.destroyVolume(volume);
                        }
                    } catch (ConcurrentOperationException e) {
                        s_logger.warn("Unable to delete volume:" + volume.getId() + " for vm:" + vmId + " whilst transitioning to error state");
                    }
                }
                String msg = "Failed to deploy Vm with Id: " + vmId;
                _alertMgr.sendAlert(AlertManager.ALERT_TYPE_USERVM, vm.getDataCenterIdToDeployIn(), vm.getPodIdToDeployIn(), msg, msg);

                _resourceLimitMgr.decrementResourceCount(vm.getAccountId(), ResourceType.user_vm);
            }
        }
    }

    protected class ExpungeTask implements Runnable {
        public ExpungeTask() {
        }

        @Override
        public void run() {
            GlobalLock scanLock = GlobalLock.getInternLock("UserVMExpunge");
            try {
                if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                    try {
                        List<UserVmVO> vms = _vmDao.findDestroyedVms(new Date(System.currentTimeMillis() - ((long) _expungeDelay << 10)));
                        if (s_logger.isInfoEnabled()) {
                            if (vms.size() == 0) {
                                s_logger.trace("Found " + vms.size() + " vms to expunge.");
                            } else {
                                s_logger.info("Found " + vms.size() + " vms to expunge.");
                            }
                        }
                        for (UserVmVO vm : vms) {
                            try {
                                expunge(vm, _accountMgr.getSystemUser().getId(), _accountMgr.getSystemAccount());
                            } catch (Exception e) {
                                s_logger.warn("Unable to expunge " + vm, e);
                            }
                        }
                    } catch (Exception e) {
                        s_logger.error("Caught the following Exception", e);
                    } finally {
                        scanLock.unlock();
                    }
                }
            } finally {
                scanLock.releaseRef();
            }
        }
    }

    private static boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) || (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_UPDATE, eventDescription = "updating Vm")
    public UserVm updateVirtualMachine(UpdateVMCmd cmd) {
        String displayName = cmd.getDisplayName();
        String group = cmd.getGroup();
        Boolean ha = cmd.getHaEnable();
        Long id = cmd.getId();
        Long osTypeId = cmd.getOsTypeId();
        String userData = cmd.getUserData();

        // Input validation
        UserVmVO vmInstance = null;

        // Verify input parameters
        vmInstance = _vmDao.findById(id.longValue());

        if (vmInstance == null) {
            throw new InvalidParameterValueException("unable to find virtual machine with id " + id);
        }

        ServiceOffering offering = _serviceOfferingDao.findById(vmInstance.getServiceOfferingId());
        if (!offering.getOfferHA() && ha != null && ha) {
            throw new InvalidParameterValueException("Can't enable ha for the vm as it's created from the Service offering having HA disabled");
        }

        _accountMgr.checkAccess(UserContext.current().getCaller(), null, true, vmInstance);

        if (displayName == null) {
            displayName = vmInstance.getDisplayName();
        }

        if (ha == null) {
            ha = vmInstance.isHaEnabled();
        }

        UserVmVO vm = _vmDao.findById(id);
        if (vm == null) {
            throw new CloudRuntimeException("Unable to find virual machine with id " + id);
        }

        if (vm.getState() == State.Error || vm.getState() == State.Expunging) {
            s_logger.error("vm is not in the right state: " + id);
            throw new InvalidParameterValueException("Vm with id " + id + " is not in the right state");
        }

        if (userData != null) {
            validateUserData(userData);
            // update userData on domain router.
        } else {
            userData = vmInstance.getUserData();
        }

        String description = "";

        if (displayName != vmInstance.getDisplayName()) {
            description += "New display name: " + displayName + ". ";
        }

        if (ha != vmInstance.isHaEnabled()) {
            if (ha) {
                description += "Enabled HA. ";
            } else {
                description += "Disabled HA. ";
            }
        }
        if (osTypeId == null) {
            osTypeId = vmInstance.getGuestOSId();
        } else {
            description += "Changed Guest OS Type to " + osTypeId + ". ";
        }

        if (group != null) {
            if (addInstanceToGroup(id, group)) {
                description += "Added to group: " + group + ".";
            }
        }

        _vmDao.updateVM(id, displayName, ha, osTypeId, userData);

        return _vmDao.findById(id);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_START, eventDescription = "starting Vm", async = true)
    public UserVm startVirtualMachine(StartVMCmd cmd) throws ExecutionException, ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return startVirtualMachine(cmd.getId(), cmd.getHostId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_REBOOT, eventDescription = "rebooting Vm", async = true)
    public UserVm rebootVirtualMachine(RebootVMCmd cmd) throws InsufficientCapacityException, ResourceUnavailableException {
        Account caller = UserContext.current().getCaller();
        Long vmId = cmd.getId();

        // Verify input parameters
        UserVmVO vmInstance = _vmDao.findById(vmId.longValue());
        if (vmInstance == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }

        _accountMgr.checkAccess(caller, null, true, vmInstance);

        return rebootVirtualMachine(UserContext.current().getCallerUserId(), vmId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_DESTROY, eventDescription = "destroying Vm", async = true)
    public UserVm destroyVm(DestroyVMCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException {
        return destroyVm(cmd.getId());
    }

    @Override
    @DB
    public InstanceGroupVO createVmGroup(CreateVMGroupCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        String groupName = cmd.getGroupName();
        Long projectId = cmd.getProjectId();

        Account owner = _accountMgr.finalizeOwner(caller, accountName, domainId, projectId);
        long accountId = owner.getId();

        // Check if name is already in use by this account
        boolean isNameInUse = _vmGroupDao.isNameInUse(accountId, groupName);

        if (isNameInUse) {
            throw new InvalidParameterValueException("Unable to create vm group, a group with name " + groupName + " already exisits for account " + accountId);
        }

        return createVmGroup(groupName, accountId);
    }

    @DB
    protected InstanceGroupVO createVmGroup(String groupName, long accountId) {
        Account account = null;
        final Transaction txn = Transaction.currentTxn();
        txn.start();
        try {
            account = _accountDao.acquireInLockTable(accountId); // to ensure duplicate vm group names are not created.
            if (account == null) {
                s_logger.warn("Failed to acquire lock on account");
                return null;
            }
            InstanceGroupVO group = _vmGroupDao.findByAccountAndName(accountId, groupName);
            if (group == null) {
                group = new InstanceGroupVO(groupName, accountId);
                group = _vmGroupDao.persist(group);
            }
            return group;
        } finally {
            if (account != null) {
                _accountDao.releaseFromLockTable(accountId);
            }
            txn.commit();
        }
    }

    @Override
    public boolean deleteVmGroup(DeleteVMGroupCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long groupId = cmd.getId();

        // Verify input parameters
        InstanceGroupVO group = _vmGroupDao.findById(groupId);
        if ((group == null) || (group.getRemoved() != null)) {
            throw new InvalidParameterValueException("unable to find a vm group with id " + groupId);
        }

        _accountMgr.checkAccess(caller, null, true, group);

        return deleteVmGroup(groupId);
    }

    @Override
    public boolean deleteVmGroup(long groupId) {
        // delete all the mappings from group_vm_map table
        List<InstanceGroupVMMapVO> groupVmMaps = _groupVMMapDao.listByGroupId(groupId);
        for (InstanceGroupVMMapVO groupMap : groupVmMaps) {
            SearchCriteria<InstanceGroupVMMapVO> sc = _groupVMMapDao.createSearchCriteria();
            sc.addAnd("instanceId", SearchCriteria.Op.EQ, groupMap.getInstanceId());
            _groupVMMapDao.expunge(sc);
        }

        if (_vmGroupDao.remove(groupId)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    @DB
    public boolean addInstanceToGroup(long userVmId, String groupName) {
        UserVmVO vm = _vmDao.findById(userVmId);

        InstanceGroupVO group = _vmGroupDao.findByAccountAndName(vm.getAccountId(), groupName);
        // Create vm group if the group doesn't exist for this account
        if (group == null) {
            group = createVmGroup(groupName, vm.getAccountId());
        }

        if (group != null) {
            final Transaction txn = Transaction.currentTxn();
            txn.start();
            UserVm userVm = _vmDao.acquireInLockTable(userVmId);
            if (userVm == null) {
                s_logger.warn("Failed to acquire lock on user vm id=" + userVmId);
            }
            try {
                // don't let the group be deleted when we are assigning vm to it.
                InstanceGroupVO ngrpLock = _vmGroupDao.lockRow(group.getId(), false);
                if (ngrpLock == null) {
                    s_logger.warn("Failed to acquire lock on vm group id=" + group.getId() + " name=" + group.getName());
                    txn.rollback();
                    return false;
                }

                // Currently don't allow to assign a vm to more than one group
                if (_groupVMMapDao.listByInstanceId(userVmId) != null) {
                    // Delete all mappings from group_vm_map table
                    List<InstanceGroupVMMapVO> groupVmMaps = _groupVMMapDao.listByInstanceId(userVmId);
                    for (InstanceGroupVMMapVO groupMap : groupVmMaps) {
                        SearchCriteria<InstanceGroupVMMapVO> sc = _groupVMMapDao.createSearchCriteria();
                        sc.addAnd("instanceId", SearchCriteria.Op.EQ, groupMap.getInstanceId());
                        _groupVMMapDao.expunge(sc);
                    }
                }
                InstanceGroupVMMapVO groupVmMapVO = new InstanceGroupVMMapVO(group.getId(), userVmId);
                _groupVMMapDao.persist(groupVmMapVO);

                txn.commit();
                return true;
            } finally {
                if (userVm != null) {
                    _vmDao.releaseFromLockTable(userVmId);
                }
            }
        }
        return false;
    }

    @Override
    public InstanceGroupVO getGroupForVm(long vmId) {
        // TODO - in future releases vm can be assigned to multiple groups; but currently return just one group per vm
        try {
            List<InstanceGroupVMMapVO> groupsToVmMap = _groupVMMapDao.listByInstanceId(vmId);

            if (groupsToVmMap != null && groupsToVmMap.size() != 0) {
                InstanceGroupVO group = _vmGroupDao.findById(groupsToVmMap.get(0).getGroupId());
                return group;
            } else {
                return null;
            }
        } catch (Exception e) {
            s_logger.warn("Error trying to get group for a vm: ", e);
            return null;
        }
    }

    @Override
    public void removeInstanceFromInstanceGroup(long vmId) {
        try {
            List<InstanceGroupVMMapVO> groupVmMaps = _groupVMMapDao.listByInstanceId(vmId);
            for (InstanceGroupVMMapVO groupMap : groupVmMaps) {
                SearchCriteria<InstanceGroupVMMapVO> sc = _groupVMMapDao.createSearchCriteria();
                sc.addAnd("instanceId", SearchCriteria.Op.EQ, groupMap.getInstanceId());
                _groupVMMapDao.expunge(sc);
            }
        } catch (Exception e) {
            s_logger.warn("Error trying to remove vm from group: ", e);
        }
    }

    protected boolean validPassword(String password) {
        if (password == null || password.length() == 0) {
            return false;
        }
        for (int i = 0; i < password.length(); i++) {
            if (password.charAt(i) == ' ') {
                return false;
            }
        }
        return true;
    }

    @Override
    public UserVm createBasicSecurityGroupVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> securityGroupIdList, Account owner,
            String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, String userData, String sshKeyPair, Map<Long, String> requestedIps, String defaultIp, String keyboard)
                    throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, StorageUnavailableException, ResourceAllocationException {

        Account caller = UserContext.current().getCaller();
        List<NetworkVO> networkList = new ArrayList<NetworkVO>();

        // Verify that caller can perform actions in behalf of vm owner
        _accountMgr.checkAccess(caller, null, true, owner);

        // Get default guest network in Basic zone
        Network defaultNetwork = _networkMgr.getExclusiveGuestNetwork(zone.getId());

        if (defaultNetwork == null) {
            throw new InvalidParameterValueException("Unable to find a default network to start a vm");
        } else {
            networkList.add(_networkDao.findById(defaultNetwork.getId()));
        }

        boolean isVmWare = (template.getHypervisorType() == HypervisorType.VMware || (hypervisor != null && hypervisor == HypervisorType.VMware));

        if (securityGroupIdList != null && isVmWare) {
            throw new InvalidParameterValueException("Security group feature is not supported for vmWare hypervisor");
        } else if (!isVmWare && _networkMgr.isSecurityGroupSupportedInNetwork(defaultNetwork) && _networkMgr.canAddDefaultSecurityGroup()) {
            if (securityGroupIdList == null) {
                securityGroupIdList = new ArrayList<Long>();
            }
            SecurityGroup defaultGroup = _securityGroupMgr.getDefaultSecurityGroup(owner.getId());
            if (defaultGroup != null) {
                //check if security group id list already contains Default security group, and if not - add it
                boolean defaultGroupPresent = false;
                for (Long securityGroupId : securityGroupIdList) {
                    if (securityGroupId.longValue() == defaultGroup.getId()) {
                        defaultGroupPresent = true;
                        break;
                    }
                }

                if (!defaultGroupPresent) {
                    securityGroupIdList.add(defaultGroup.getId());
                }

            } else {
                //create default security group for the account
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Couldn't find default security group for the account " + owner + " so creating a new one");
                }
                defaultGroup = _securityGroupMgr.createSecurityGroup(SecurityGroupManager.DEFAULT_GROUP_NAME, SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION, owner.getDomainId(), owner.getId(), owner.getAccountName());
                securityGroupIdList.add(defaultGroup.getId());
            }
        }

        return createVirtualMachine(zone, serviceOffering, template, hostName, displayName, owner, diskOfferingId,
                diskSize, networkList, securityGroupIdList, group, userData, sshKeyPair, hypervisor, caller, requestedIps, defaultIp, keyboard);
    }

    @Override
    public UserVm createAdvancedSecurityGroupVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList,
            List<Long> securityGroupIdList, Account owner, String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, String userData,
            String sshKeyPair, Map<Long, String> requestedIps, String defaultIp, String keyboard) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, StorageUnavailableException,
            ResourceAllocationException {

        Account caller = UserContext.current().getCaller();
        List<NetworkVO> networkList = new ArrayList<NetworkVO>();
        boolean isSecurityGroupEnabledNetworkUsed = false;
        boolean isVmWare = (template.getHypervisorType() == HypervisorType.VMware || (hypervisor != null && hypervisor == HypervisorType.VMware));

        //Verify that caller can perform actions in behalf of vm owner
        _accountMgr.checkAccess(caller, null, true, owner);

        // If no network is specified, find system security group enabled network
        if (networkIdList == null || networkIdList.isEmpty()) {
            NetworkVO networkWithSecurityGroup = _networkMgr.getNetworkWithSecurityGroupEnabled(zone.getId());
            if (networkWithSecurityGroup == null) {
                throw new InvalidParameterValueException("No network with security enabled is found in zone id=" + zone.getId());
            }

            networkList.add(networkWithSecurityGroup);
            isSecurityGroupEnabledNetworkUsed = true;

        } else if (securityGroupIdList != null && !securityGroupIdList.isEmpty()) {
            if (isVmWare) {
                throw new InvalidParameterValueException("Security group feature is not supported for vmWare hypervisor");
            }
            // Only one network can be specified, and it should be security group enabled
            if (networkIdList.size() > 1) {
                throw new InvalidParameterValueException("Only support one network per VM if security group enabled");
            }

            NetworkVO network = _networkDao.findById(networkIdList.get(0).longValue());

            if (network == null) {
                throw new InvalidParameterValueException("Unable to find network by id " + networkIdList.get(0).longValue());
            }

            if (!_networkMgr.isSecurityGroupSupportedInNetwork(network)) {
                throw new InvalidParameterValueException("Network is not security group enabled: " + network.getId());
            }

            networkList.add(network);
            isSecurityGroupEnabledNetworkUsed = true;

        } else {
            // Verify that all the networks are Direct/Guest/AccountSpecific; can't create combination of SG enabled network and
            // regular networks
            for (Long networkId : networkIdList) {
                NetworkVO network = _networkDao.findById(networkId);

                if (network == null) {
                    throw new InvalidParameterValueException("Unable to find network by id " + networkIdList.get(0).longValue());
                }

                boolean isSecurityGroupEnabled = _networkMgr.isSecurityGroupSupportedInNetwork(network);
                if (isSecurityGroupEnabled && networkIdList.size() > 1) {
                    throw new InvalidParameterValueException("Can't create a vm with multiple networks one of which is Security Group enabled");
                }

                if (network.getTrafficType() != TrafficType.Guest || network.getGuestType() != Network.GuestType.Shared || (network.getGuestType() == Network.GuestType.Shared && !isSecurityGroupEnabled)) {
                    throw new InvalidParameterValueException("Can specify only Direct Guest Account specific networks when deploy vm in Security Group enabled zone");
                }

                // Perform account permission check
                if (network.getGuestType() != Network.GuestType.Shared) {
                    // Check account permissions
                    List<NetworkVO> networkMap = _networkDao.listBy(owner.getId(), network.getId());
                    if (networkMap == null || networkMap.isEmpty()) {
                        throw new PermissionDeniedException("Unable to create a vm using network with id " + network.getId() + ", permission denied");
                    }
                }

                networkList.add(network);
            }
        }

        // if network is security group enabled, and default security group is not present in the list of groups specified, add it automatically
        if (isSecurityGroupEnabledNetworkUsed && !isVmWare && _networkMgr.canAddDefaultSecurityGroup()) {
            if (securityGroupIdList == null) {
                securityGroupIdList = new ArrayList<Long>();
            }

            SecurityGroup defaultGroup = _securityGroupMgr.getDefaultSecurityGroup(owner.getId());
            if (defaultGroup != null) {
                //check if security group id list already contains Default security group, and if not - add it
                boolean defaultGroupPresent = false;
                for (Long securityGroupId : securityGroupIdList) {
                    if (securityGroupId.longValue() == defaultGroup.getId()) {
                        defaultGroupPresent = true;
                        break;
                    }
                }

                if (!defaultGroupPresent) {
                    securityGroupIdList.add(defaultGroup.getId());
                }

            } else {
                //create default security group for the account
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Couldn't find default security group for the account " + owner + " so creating a new one");
                }
                defaultGroup = _securityGroupMgr.createSecurityGroup(SecurityGroupManager.DEFAULT_GROUP_NAME, SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION, owner.getDomainId(), owner.getId(), owner.getAccountName());
                securityGroupIdList.add(defaultGroup.getId());
            }
        }

        return createVirtualMachine(zone, serviceOffering, template, hostName, displayName, owner, diskOfferingId,
                diskSize, networkList, securityGroupIdList, group, userData, sshKeyPair, hypervisor, caller, requestedIps, defaultIp, keyboard);
    }

    @Override
    public UserVm createAdvancedVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList, Account owner, String hostName,
            String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, String userData, String sshKeyPair, Map<Long, String> requestedIps, String defaultIp, String keyboard)
                    throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, StorageUnavailableException, ResourceAllocationException {

        Account caller = UserContext.current().getCaller();
        List<NetworkVO> networkList = new ArrayList<NetworkVO>();

        // Verify that caller can perform actions in behalf of vm owner
        _accountMgr.checkAccess(caller, null, true, owner);
        
        if (networkIdList == null || networkIdList.isEmpty()) {
            NetworkVO defaultNetwork = null;

            // if no network is passed in
            // Check if default virtual network offering has Availability=Required. If it's true, search for corresponding
            // network
            // * if network is found, use it. If more than 1 virtual network is found, throw an error
            // * if network is not found, create a new one and use it

            List<NetworkOfferingVO> requiredOfferings = _networkOfferingDao.listByAvailability(Availability.Required, false);
            if (requiredOfferings.size() < 1) {
            	throw new InvalidParameterValueException("Unable to find network offering with availability=" + Availability.Required + " to automatically create the network as a part of vm creation");
            }
            
            PhysicalNetwork physicalNetwork = _networkMgr.translateZoneIdToPhysicalNetwork(zone.getId());
            if (requiredOfferings.get(0).getState() == NetworkOffering.State.Enabled) {
                // get Virtual networks
                List<NetworkVO> virtualNetworks = _networkMgr.listNetworksForAccount(owner.getId(), zone.getId(), Network.GuestType.Isolated);

                if (virtualNetworks.isEmpty()) {
                    s_logger.debug("Creating network for account " + owner + " from the network offering id=" + requiredOfferings.get(0).getId() + " as a part of deployVM process");
                    Network newNetwork = _networkMgr.createGuestNetwork(requiredOfferings.get(0).getId(), owner.getAccountName() + "-network", owner.getAccountName() + "-network", null, null,
                            null, null, owner, false, null, physicalNetwork, zone.getId(), ACLType.Account, null);
                    defaultNetwork = _networkDao.findById(newNetwork.getId());
                } else if (virtualNetworks.size() > 1) {
                    throw new InvalidParameterValueException("More than 1 default Isolated networks are found for account " + owner + "; please specify networkIds");
                } else {
                    defaultNetwork = virtualNetworks.get(0);
                }
            } else {
            	throw new InvalidParameterValueException("Required network offering id=" + requiredOfferings.get(0).getId() + " is not in " + NetworkOffering.State.Enabled); 
            }

            networkList.add(defaultNetwork);

        } else {
            for (Long networkId : networkIdList) {
                NetworkVO network = _networkDao.findById(networkId);
                if (network == null) {
                    throw new InvalidParameterValueException("Unable to find network by id " + networkIdList.get(0).longValue());
                }

                _networkMgr.checkNetworkPermissions(owner, network);

                //don't allow to use system networks 
                NetworkOffering networkOffering = _configMgr.getNetworkOffering(network.getNetworkOfferingId());
                if (networkOffering.isSystemOnly()) {
                    throw new InvalidParameterValueException("Network id=" + networkId + " is system only and can't be used for vm deployment");
                }
                networkList.add(network);
            }
        }

        return createVirtualMachine(zone, serviceOffering, template, hostName, displayName, owner, diskOfferingId, diskSize, networkList, null, group, userData, sshKeyPair, hypervisor, caller, requestedIps, defaultIp, keyboard);
    }

    @DB @ActionEvent(eventType = EventTypes.EVENT_VM_CREATE, eventDescription = "deploying Vm", create = true)
    protected UserVm createVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, String hostName, String displayName, Account owner, Long diskOfferingId,
            Long diskSize, List<NetworkVO> networkList, List<Long> securityGroupIdList, String group, String userData, String sshKeyPair, HypervisorType hypervisor, Account caller, Map<Long, String> requestedIps, String defaultNetworkIp, String keyboard) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException, StorageUnavailableException, ResourceAllocationException {

        _accountMgr.checkAccess(caller, null, true, owner);
        
        if (owner.getState() == Account.State.disabled) {
            throw new PermissionDeniedException("The owner of vm to deploy is disabled: " + owner);
        }
        
        long accountId = owner.getId();

        assert !(requestedIps != null && defaultNetworkIp != null) : "requestedIp list and defaultNetworkIp should never be specified together";

        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getType())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zone.getId());
        }

        if (zone.getDomainId() != null) {
            DomainVO domain = _domainDao.findById(zone.getDomainId());
            if (domain == null) {
                throw new CloudRuntimeException("Unable to find the domain " + zone.getDomainId() + " for the zone: " + zone);
            }
            // check that caller can operate with domain
            _configMgr.checkAccess(caller, zone);
            // check that vm owner can create vm in the domain
            _configMgr.checkAccess(owner, zone);
        }

        // check if account/domain is with in resource limits to create a new vm
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.user_vm);
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.volume);

        //verify security group ids
        if (securityGroupIdList != null) {
            for (Long securityGroupId : securityGroupIdList) {
            	SecurityGroup sg = _securityGroupDao.findById(securityGroupId);
                if (sg == null) {
                    throw new InvalidParameterValueException("Unable to find security group by id " + securityGroupId);
                } else {
                	//verify permissions
                	_accountMgr.checkAccess(caller, null, true, owner, sg);
                }
            }
        }

        // check if we have available pools for vm deployment
        long availablePools = _storagePoolDao.countPoolsByStatus(StoragePoolStatus.Up);
        if (availablePools  < 1) {
            throw new StorageUnavailableException("There are no available pools in the UP state for vm deployment", -1);
        }

        ServiceOfferingVO offering = _serviceOfferingDao.findById(serviceOffering.getId());

        if (template.getTemplateType().equals(TemplateType.SYSTEM)) {
            throw new InvalidParameterValueException("Unable to use system template " + template.getId() + " to deploy a user vm");
        }
        List<VMTemplateZoneVO> listZoneTemplate = _templateZoneDao.listByZoneTemplate(zone.getId(), template.getId());
        if (listZoneTemplate == null || listZoneTemplate.isEmpty()) {
            throw new InvalidParameterValueException("The template " + template.getId() + " is not available for use");
        }
        boolean isIso = Storage.ImageFormat.ISO == template.getFormat();
        if (isIso && !template.isBootable()) {
            throw new InvalidParameterValueException("Installing from ISO requires an ISO that is bootable: " + template.getId());
        }

        // Check templates permissions
        if (!template.isPublicTemplate()) {
            Account templateOwner = _accountMgr.getAccount(template.getAccountId());
            _accountMgr.checkAccess(owner, null, true, templateOwner);
        }

        // If the template represents an ISO, a disk offering must be passed in, and will be used to create the root disk
        // Else, a disk offering is optional, and if present will be used to create the data disk
        Pair<DiskOfferingVO, Long> rootDiskOffering = new Pair<DiskOfferingVO, Long>(null, null);
        List<Pair<DiskOfferingVO, Long>> dataDiskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>();

        if (isIso) {
            if (diskOfferingId == null) {
                throw new InvalidParameterValueException("Installing from ISO requires a disk offering to be specified for the root disk.");
            }
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
            if (diskOffering == null) {
                throw new InvalidParameterValueException("Unable to find disk offering " + diskOfferingId);
            }
            Long size = null;
            if (diskOffering.getDiskSize() == 0) {
                size = diskSize;
                if (size == null) {
                    throw new InvalidParameterValueException("Disk offering " + diskOffering + " requires size parameter.");
                }
            }
            rootDiskOffering.first(diskOffering);
            rootDiskOffering.second(size);
        } else {
            rootDiskOffering.first(offering);
            if (diskOfferingId != null) {
                DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
                if (diskOffering == null) {
                    throw new InvalidParameterValueException("Unable to find disk offering " + diskOfferingId);
                }
                Long size = null;
                if (diskOffering.getDiskSize() == 0) {
                    size = diskSize;
                    if (size == null) {
                        throw new InvalidParameterValueException("Disk offering " + diskOffering + " requires size parameter.");
                    }
                }
                dataDiskOfferings.add(new Pair<DiskOfferingVO, Long>(diskOffering, size));
            }
        }

        //check if the user data is correct
        validateUserData(userData);

        // Find an SSH public key corresponding to the key pair name, if one is given
        String sshPublicKey = null;
        if (sshKeyPair != null && !sshKeyPair.equals("")) {
            SSHKeyPair pair = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), sshKeyPair);
            if (pair == null) {
                throw new InvalidParameterValueException("A key pair with name '" + sshKeyPair + "' was not found.");
            }

            sshPublicKey = pair.getPublicKey();
        }

        List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>();
        short defaultNetworkNumber = 0;
        boolean securityGroupEnabled = false;
        for (NetworkVO network : networkList) {
            if (network.getDataCenterId() != zone.getId()) {
                throw new InvalidParameterValueException("Network id=" + network.getId() + " doesn't belong to zone " + zone.getId());
            }
            
            String requestedIp = null;
            if (requestedIps != null && !requestedIps.isEmpty()) {
            	requestedIp = requestedIps.get(network.getId());
            }

            NicProfile profile = new NicProfile(requestedIp);
            
            if (defaultNetworkNumber == 0) {
            	 defaultNetworkNumber++;
            	 profile.setDefaultNic(true);
                 // if user requested specific ip for default network, add it
                 if (defaultNetworkIp != null) {
                     profile = new NicProfile(defaultNetworkIp);
                 }
            }

            networks.add(new Pair<NetworkVO, NicProfile>(network, profile));

            if (_networkMgr.isSecurityGroupSupportedInNetwork(network)) {
                securityGroupEnabled = true;
            }
        }

        if (securityGroupIdList != null && !securityGroupIdList.isEmpty() && !securityGroupEnabled) {
            throw new InvalidParameterValueException("Unable to deploy vm with security groups as SecurityGroup service is not enabled for the vm's network");
        }

        // Verify network information - network default network has to be set; and vm can't have more than one default network
        // This is a part of business logic because default network is required by Agent Manager in order to configure default
        // gateway for the vm
        if (defaultNetworkNumber == 0) {
            throw new InvalidParameterValueException("At least 1 default network has to be specified for the vm");
        } else if (defaultNetworkNumber > 1) {
            throw new InvalidParameterValueException("Only 1 default network per vm is supported");
        }

        long id = _vmDao.getNextInSequence(Long.class, "id");

        String instanceName = VirtualMachineName.getVmName(id, owner.getId(), _instance);
        
        String uuidName = UUID.randomUUID().toString();
        if (hostName == null) {
            hostName = uuidName;
        } else {
            // verify hostName (hostname doesn't have to be unique)
            if (!NetUtils.verifyDomainNameLabel(hostName, true)) {
                throw new InvalidParameterValueException("Invalid name. Vm name can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                        + "and the hyphen ('-'), must be between 1 and 63 characters long, and can't start or end with \"-\" and can't start with digit");
            }
        }

        HypervisorType hypervisorType = null;
        if (template == null || template.getHypervisorType() == null || template.getHypervisorType() == HypervisorType.None) {
            hypervisorType = hypervisor;
        } else {
            hypervisorType = template.getHypervisorType();
        }
        Transaction txn = Transaction.currentTxn();
        txn.start();
        UserVmVO vm = new UserVmVO(id, instanceName, displayName, template.getId(), hypervisorType, template.getGuestOSId(), offering.getOfferHA(), offering.getLimitCpuUse(), owner.getDomainId(), owner.getId(),
                offering.getId(), userData, hostName);
        vm.setUuid(uuidName);
        
        if (sshPublicKey != null) {
            vm.setDetail("SSH.PublicKey", sshPublicKey);
        }

        if(keyboard != null && !keyboard.isEmpty())
            vm.setDetail(VmDetailConstants.KEYBOARD, keyboard);

        if (isIso) {
            vm.setIsoId(template.getId());
        }

        s_logger.debug("Allocating in the DB for vm");
        DataCenterDeployment plan = new DataCenterDeployment(zone.getId());

        if (_itMgr.allocate(vm, _templateDao.findById(template.getId()), offering, rootDiskOffering, dataDiskOfferings, networks, null, plan, hypervisorType, owner) == null) {
            return null;
        }

        _vmDao.saveDetails(vm);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully allocated DB entry for " + vm);
        }
        UserContext.current().setEventDetails("Vm Id: " + vm.getId());

        UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VM_CREATE, accountId, zone.getId(), vm.getId(), vm.getHostName(), offering.getId(), template.getId(), hypervisorType.toString());
        _usageEventDao.persist(usageEvent);

        _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.user_vm);
        txn.commit();
        // Assign instance to the group
        try {
            if (group != null) {
                boolean addToGroup = addInstanceToGroup(Long.valueOf(id), group);
                if (!addToGroup) {
                    throw new CloudRuntimeException("Unable to assign Vm to the group " + group);
                }
            }
        } catch (Exception ex) {
            throw new CloudRuntimeException("Unable to assign Vm to the group " + group);
        }

        _securityGroupMgr.addInstanceToGroups(vm.getId(), securityGroupIdList);

        return vm;
    }

    private void validateUserData(String userData) {
        byte[] decodedUserData = null;
        if (userData != null) {
            if (userData.length() >= 2 * MAX_USER_DATA_LENGTH_BYTES) {
                throw new InvalidParameterValueException("User data is too long");
            }
            decodedUserData = org.apache.commons.codec.binary.Base64.decodeBase64(userData.getBytes());
            if (decodedUserData.length > MAX_USER_DATA_LENGTH_BYTES) {
                throw new InvalidParameterValueException("User data is too long");
            }
            if (decodedUserData.length < 1) {
                throw new InvalidParameterValueException("User data is too short");
            }
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_CREATE, eventDescription = "starting Vm", async = true)
    public UserVm startVirtualMachine(DeployVMCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
        return startVirtualMachine(cmd, null);
    }

    protected UserVm startVirtualMachine(DeployVMCmd cmd, Map<VirtualMachineProfile.Param, Object> additonalParams) throws ResourceUnavailableException, InsufficientCapacityException,
    ConcurrentOperationException {

        Host destinationHost = null;
        if(cmd.getHostId() != null){
            Account account = UserContext.current().getCaller();
            if(!_accountService.isRootAdmin(account.getType())){
                throw new PermissionDeniedException("Parameter hostid can only be specified by a Root Admin, permission denied");
            }
            destinationHost = _hostDao.findById(cmd.getHostId());
            if (destinationHost == null) {
                throw new InvalidParameterValueException("Unable to find the host to deploy the VM, host id=" + cmd.getHostId());
            }
        }
        long vmId = cmd.getEntityId();
        UserVmVO vm = _vmDao.findById(vmId);
        _vmDao.loadDetails(vm);

        // Check that the password was passed in and is valid
        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(vm.getTemplateId());

        String password = "saved_password";
        if (template.getEnablePassword()) {
            password = generateRandomPassword();
        }

        if (!validPassword(password)) {
            throw new InvalidParameterValueException("A valid password for this virtual machine was not provided.");
        }

        // Check if an SSH key pair was selected for the instance and if so use it to encrypt & save the vm password
        String sshPublicKey = vm.getDetail("SSH.PublicKey");
        if (sshPublicKey != null && !sshPublicKey.equals("") && password != null && !password.equals("saved_password")) {
            String encryptedPasswd = RSAHelper.encryptWithSSHPublicKey(sshPublicKey, password);
            if (encryptedPasswd == null) {
                throw new CloudRuntimeException("Error encrypting password");
            }

            vm.setDetail("Encrypted.Password", encryptedPasswd);
            _vmDao.saveDetails(vm);
        }

        long userId = UserContext.current().getCallerUserId();
        UserVO caller = _userDao.findById(userId);

        AccountVO owner = _accountDao.findById(vm.getAccountId());

        try {
            Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>();
            if (additonalParams != null) {
                params.putAll(additonalParams);
            }
            params.put(VirtualMachineProfile.Param.VmPassword, password);

            DataCenterDeployment plan = null;
            if (destinationHost != null) {
                s_logger.debug("Destination Host to deploy the VM is specified, specifying a deployment plan to deploy the VM");
                plan = new DataCenterDeployment(vm.getDataCenterIdToDeployIn(), destinationHost.getPodId(), destinationHost.getClusterId(), destinationHost.getId(), null, null);
            }

            vm = _itMgr.start(vm, params, caller, owner, plan);
        } finally {
            updateVmStateForFailedVmCreation(vm.getId());
        }
        
        if (template.getEnablePassword()) {
            // this value is not being sent to the backend; need only for api display purposes
            vm.setPassword(password);
        }

        return vm;
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context) {
        UserVmVO vm = profile.getVirtualMachine();
        Map<String, String> details = _vmDetailsDao.findDetails(vm.getId());
        vm.setDetails(details);

        if (vm.getIsoId() != null) {
            String isoPath = null;

            VirtualMachineTemplate template = _templateDao.findById(vm.getIsoId());
            if (template == null || template.getFormat() != ImageFormat.ISO) {
                throw new CloudRuntimeException("Can not find ISO in vm_template table for id " + vm.getIsoId());
            }

            Pair<String, String> isoPathPair = _storageMgr.getAbsoluteIsoPath(template.getId(), vm.getDataCenterIdToDeployIn());

            if (isoPathPair == null) {
                s_logger.warn("Couldn't get absolute iso path");
                return false;
            } else {
                isoPath = isoPathPair.first();
            }

            if (template.isBootable()) {
                profile.setBootLoaderType(BootloaderType.CD);
            }
            GuestOSVO guestOS = _guestOSDao.findById(template.getGuestOSId());
            String displayName = null;
            if (guestOS != null) {
                displayName = guestOS.getDisplayName();
            }
            VolumeTO iso = new VolumeTO(profile.getId(), Volume.Type.ISO, StoragePoolType.ISO, null, template.getName(), null, isoPath, 0, null, displayName);

            iso.setDeviceId(3);
            profile.addDisk(iso);
        } else {
            VirtualMachineTemplate template = profile.getTemplate();
            /* create a iso placeholder */
            VolumeTO iso = new VolumeTO(profile.getId(), Volume.Type.ISO, StoragePoolType.ISO, null, template.getName(), null, null, 0, null);
            iso.setDeviceId(3);
            profile.addDisk(iso);
        }

        return true;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context) {
    	UserVmVO userVm = profile.getVirtualMachine();
        List<NicVO> nics = _nicDao.listByVmId(userVm.getId());
        for (NicVO nic : nics) {
            NetworkVO network = _networkDao.findById(nic.getNetworkId());
            if (network.getTrafficType() == TrafficType.Guest || network.getTrafficType() == TrafficType.Public) {
                userVm.setPrivateIpAddress(nic.getIp4Address());
                userVm.setPrivateMacAddress(nic.getMacAddress());
            }
        }
        return true;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile<UserVmVO> profile) {
        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile<UserVmVO> profile, long hostId, Commands cmds, ReservationContext context){
        UserVmVO vm = profile.getVirtualMachine();

        Answer[] answersToCmds = cmds.getAnswers();
        if(answersToCmds == null){
            if(s_logger.isDebugEnabled()){
                s_logger.debug("Returning from finalizeStart() since there are no answers to read");
            }
            return true;
        }
        Answer startAnswer = cmds.getAnswer(StartAnswer.class);
        String returnedIp = null;
        String originalIp = null;
        if (startAnswer != null) {
            StartAnswer startAns = (StartAnswer) startAnswer;
            VirtualMachineTO vmTO = startAns.getVirtualMachine();
            for (NicTO nicTO: vmTO.getNics()) {
                if (nicTO.getType() == TrafficType.Guest) {
                    returnedIp = nicTO.getIp();
                }
            }
        }

        List<NicVO> nics = _nicDao.listByVmId(vm.getId());
        NicVO guestNic = null;
        NetworkVO guestNetwork = null;
        for (NicVO nic : nics) {
            NetworkVO network = _networkDao.findById(nic.getNetworkId());
            long isDefault = (nic.isDefaultNic()) ? 1 : 0;
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NETWORK_OFFERING_ASSIGN, vm.getAccountId(), vm.getDataCenterIdToDeployIn(), vm.getId(), vm.getHostName(), network.getNetworkOfferingId(), null, isDefault);
            _usageEventDao.persist(usageEvent);
            if (network.getTrafficType() == TrafficType.Guest) {
                originalIp = nic.getIp4Address();
                guestNic = nic;
                guestNetwork = network;
            }
        }
        boolean ipChanged = false;
        if (originalIp != null && !originalIp.equalsIgnoreCase(returnedIp)) {
            if (returnedIp != null && guestNic != null) {
                guestNic.setIp4Address(returnedIp);
                ipChanged = true;
            }
        }
        if (returnedIp != null && !returnedIp.equalsIgnoreCase(originalIp)) {
            if (guestNic != null) {
                guestNic.setIp4Address(returnedIp);
                ipChanged = true;
            }
        }
        if (ipChanged) {
            DataCenterVO dc = _dcDao.findById(vm.getDataCenterIdToDeployIn());
            UserVmVO userVm = profile.getVirtualMachine();
            //dc.getDhcpProvider().equalsIgnoreCase(Provider.ExternalDhcpServer.getName())
            if (_ntwkSrvcDao.canProviderSupportServiceInNetwork(guestNetwork.getId(), Service.Dhcp, Provider.ExternalDhcpServer)){
                _nicDao.update(guestNic.getId(), guestNic);
                userVm.setPrivateIpAddress(guestNic.getIp4Address());
                _vmDao.update(userVm.getId(), userVm);

                s_logger.info("Detected that ip changed in the answer, updated nic in the db with new ip " + returnedIp);
            }
        }
        
        //get system ip and create static nat rule for the vm
        try {
            _rulesMgr.getSystemIpAndEnableStaticNatForVm(profile.getVirtualMachine(), false);
        } catch (Exception ex) {
            s_logger.warn("Failed to get system ip and enable static nat for the vm " + profile.getVirtualMachine() + " due to exception ", ex);
            return false;
        }
         
         return true;
    }

    @Override
    public void finalizeExpunge(UserVmVO vm) {
    }

    @Override
    public UserVmVO persist(UserVmVO vm) {
        return _vmDao.persist(vm);
    }

    @Override
    public UserVmVO findById(long id) {
        return _vmDao.findById(id);
    }

    @Override
    public UserVmVO findByName(String name) {
        if (!VirtualMachineName.isValidVmName(name)) {
            return null;
        }
        return findById(VirtualMachineName.getVmId(name));
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_STOP, eventDescription = "stopping Vm", async = true)
    public UserVm stopVirtualMachine(long vmId, boolean forced) throws ConcurrentOperationException {
        // Input validation
        Account caller = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();

        // if account is removed, return error
        if (caller != null && caller.getRemoved() != null) {
            throw new PermissionDeniedException("The account " + caller.getId() + " is removed");
        }

        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }

        _accountMgr.checkAccess(caller, null, true, vm);
        UserVO user = _userDao.findById(userId);

        try {
            _itMgr.advanceStop(vm, forced, user, caller);
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Unable to contact the agent to stop the virtual machine " + vm, e);
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Unable to contact the agent to stop the virtual machine " + vm, e);
        }

        return _vmDao.findById(vmId);
    }

    @Override
    public void finalizeStop(VirtualMachineProfile<UserVmVO> profile, StopAnswer answer) {
    	//release elastic IP here
    	IPAddressVO ip = _ipAddressDao.findByAssociatedVmId(profile.getId());
    	if (ip != null && ip.getSystem()) {
    		UserContext ctx = UserContext.current();
    		try {
            	_rulesMgr.disableStaticNat(ip.getId(), ctx.getCaller(), ctx.getCallerUserId(), true);
    		} catch (Exception ex) {
    			s_logger.warn("Failed to disable static nat and release system ip " + ip + " as a part of vm " + profile.getVirtualMachine() + " stop due to exception ", ex);
    		}
    	}
    }

    public String generateRandomPassword() {
        return PasswordGenerator.generateRandomPassword(6);
    }

    @Override
    public UserVm startVirtualMachine(long vmId, Long hostId) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // Input validation
        Account caller = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();

        // if account is removed, return error
        if (caller != null && caller.getRemoved() != null) {
            throw new InvalidParameterValueException("The account " + caller.getId() + " is removed");
        }
        
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }

        _accountMgr.checkAccess(caller, null, true, vm);
        
        Account owner = _accountDao.findById(vm.getAccountId());

        if (owner == null) {
            throw new InvalidParameterValueException("The owner of " + vm + " does not exist: " + vm.getAccountId());
        }

        if (owner.getState() == Account.State.disabled) {
            throw new PermissionDeniedException("The owner of " + vm + " is disabled: " + vm.getAccountId());
        }
        
        Host destinationHost = null;
        if(hostId != null){
            Account account = UserContext.current().getCaller();
            if(!_accountService.isRootAdmin(account.getType())){
                throw new PermissionDeniedException("Parameter hostid can only be specified by a Root Admin, permission denied");
            }
            destinationHost = _hostDao.findById(hostId);
            if (destinationHost == null) {
                throw new InvalidParameterValueException("Unable to find the host to deploy the VM, host id=" + hostId);
            }
        }
        
        UserVO user = _userDao.findById(userId);
        
        //check if vm is security group enabled
        if (_securityGroupMgr.isVmSecurityGroupEnabled(vmId) && !_securityGroupMgr.isVmMappedToDefaultSecurityGroup(vmId) && _networkMgr.canAddDefaultSecurityGroup()) {
            //if vm is not mapped to security group, create a mapping
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Vm " + vm + " is security group enabled, but not mapped to default security group; creating the mapping automatically");
            }

            SecurityGroup defaultSecurityGroup = _securityGroupMgr.getDefaultSecurityGroup(vm.getAccountId());
            if (defaultSecurityGroup != null) {
                List<Long> groupList = new ArrayList<Long>();
                groupList.add(defaultSecurityGroup.getId());
                _securityGroupMgr.addInstanceToGroups(vmId, groupList);
            }
        }
        
        DataCenterDeployment plan = null;
        if (destinationHost != null) {
            s_logger.debug("Destination Host to deploy the VM is specified, specifying a deployment plan to deploy the VM");
            plan = new DataCenterDeployment(vm.getDataCenterIdToDeployIn(), destinationHost.getPodId(), destinationHost.getClusterId(), destinationHost.getId(), null, null);
        }

        return _itMgr.start(vm, null, user, caller, plan);
    }

    @Override
    public UserVm destroyVm(long vmId) throws ResourceUnavailableException, ConcurrentOperationException {
        Account caller = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();

        // Verify input parameters
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null || vm.getRemoved() != null) {
        	InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find a virtual machine with specified vmId");
        	ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        } 

        if (vm.getState() == State.Destroyed || vm.getState() == State.Expunging) {
            s_logger.trace("Vm id=" + vmId + " is already destroyed");
            return vm;
        }

        _accountMgr.checkAccess(caller, null, true, vm);
        User userCaller = _userDao.findById(userId);

        boolean status;
        State vmState = vm.getState();

        try {
            status = _itMgr.destroy(vm, userCaller, caller);
        } catch (OperationTimedoutException e) {
        	CloudRuntimeException ex = new CloudRuntimeException("Unable to destroy with specified vmId", e);
        	ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }

        if (status) {
            // Mark the account's volumes as destroyed
            List<VolumeVO> volumes = _volsDao.findByInstance(vmId);
            for (VolumeVO volume : volumes) {
                if (volume.getVolumeType().equals(Volume.Type.ROOT)) {
                    UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_DELETE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName());
                    _usageEventDao.persist(usageEvent);
                }
            }

            if (vmState != State.Error) {
                _resourceLimitMgr.decrementResourceCount(vm.getAccountId(), ResourceType.user_vm);
            }

            return _vmDao.findById(vmId);
        } else {
        	CloudRuntimeException ex = new CloudRuntimeException("Failed to destroy vm with specified vmId");
        	ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }
    }

    @Override
    public List<UserVmVO> searchForUserVMs(ListVMsCmd cmd) {
    	 Account caller = UserContext.current().getCaller();
         List<Long> permittedAccounts = new ArrayList<Long>();
         String hypervisor = cmd.getHypervisor();
         boolean listAll = cmd.listAll();
         Long id = cmd.getId();

         Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, listAll, false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Criteria c = new Criteria("id", Boolean.TRUE, cmd.getStartIndex(), cmd.getPageSizeVal());
        c.addCriteria(Criteria.KEYWORD, cmd.getKeyword());
        c.addCriteria(Criteria.ID, cmd.getId());
        c.addCriteria(Criteria.NAME, cmd.getInstanceName());
        c.addCriteria(Criteria.STATE, cmd.getState());
        c.addCriteria(Criteria.DATACENTERID, cmd.getZoneId());
        c.addCriteria(Criteria.GROUPID, cmd.getGroupId());
        c.addCriteria(Criteria.FOR_VIRTUAL_NETWORK, cmd.getForVirtualNetwork());
        c.addCriteria(Criteria.NETWORKID, cmd.getNetworkId());

        if (domainId != null) {
            c.addCriteria(Criteria.DOMAINID, domainId);
        }

        if (HypervisorType.getType(hypervisor) != HypervisorType.None) {
            c.addCriteria(Criteria.HYPERVISOR, hypervisor);
        } else if (hypervisor != null) {
            throw new InvalidParameterValueException("Invalid HypervisorType " + hypervisor);
        }

        // ignore these search requests if it's not an admin
        if (_accountMgr.isAdmin(caller.getType())) {
            c.addCriteria(Criteria.PODID, cmd.getPodId());
            c.addCriteria(Criteria.HOSTID, cmd.getHostId());
            c.addCriteria(Criteria.STORAGE_ID, cmd.getStorageId());
        }

        if (!permittedAccounts.isEmpty()) {
            c.addCriteria(Criteria.ACCOUNTID, permittedAccounts.toArray());
        }
        c.addCriteria(Criteria.ISADMIN, _accountMgr.isAdmin(caller.getType()));

        return searchForUserVMs(c, caller, domainId, isRecursive, permittedAccounts, listAll, listProjectResourcesCriteria);
    }

    @Override
    public List<UserVmVO> searchForUserVMs(Criteria c, Account caller, Long domainId, boolean isRecursive, List<Long> permittedAccounts, boolean listAll, ListProjectResourcesCriteria listProjectResourcesCriteria) {
        Filter searchFilter = new Filter(UserVmVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());

        SearchBuilder<UserVmVO> sb = _vmDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        
        Object id = c.getCriteria(Criteria.ID);
        Object name = c.getCriteria(Criteria.NAME);
        Object state = c.getCriteria(Criteria.STATE);
        Object notState = c.getCriteria(Criteria.NOTSTATE);
        Object zone = c.getCriteria(Criteria.DATACENTERID);
        Object pod = c.getCriteria(Criteria.PODID);
        Object hostId = c.getCriteria(Criteria.HOSTID);
        Object hostName = c.getCriteria(Criteria.HOSTNAME);
        Object keyword = c.getCriteria(Criteria.KEYWORD);
        Object isAdmin = c.getCriteria(Criteria.ISADMIN);
        assert c.getCriteria(Criteria.IPADDRESS) == null : "We don't support search by ip address on VM any more.  If you see this assert, it means we have to find a different way to search by the nic table.";
        Object groupId = c.getCriteria(Criteria.GROUPID);
        Object networkId = c.getCriteria(Criteria.NETWORKID);
        Object hypervisor = c.getCriteria(Criteria.HYPERVISOR);
        Object storageId = c.getCriteria(Criteria.STORAGE_ID);

        sb.and("displayName", sb.entity().getDisplayName(), SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getHostName(), SearchCriteria.Op.LIKE);
        sb.and("stateEQ", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("stateNEQ", sb.entity().getState(), SearchCriteria.Op.NEQ);
        sb.and("stateNIN", sb.entity().getState(), SearchCriteria.Op.NIN);
        sb.and("dataCenterId", sb.entity().getDataCenterIdToDeployIn(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodIdToDeployIn(), SearchCriteria.Op.EQ);
        sb.and("hypervisorType", sb.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        sb.and("hostIdEQ", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        sb.and("hostIdIN", sb.entity().getHostId(), SearchCriteria.Op.IN);

        if (groupId != null && (Long) groupId == -1) {
            SearchBuilder<InstanceGroupVMMapVO> vmSearch = _groupVMMapDao.createSearchBuilder();
            vmSearch.and("instanceId", vmSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
            sb.join("vmSearch", vmSearch, sb.entity().getId(), vmSearch.entity().getInstanceId(), JoinBuilder.JoinType.LEFTOUTER);
        } else if (groupId != null) {
            SearchBuilder<InstanceGroupVMMapVO> groupSearch = _groupVMMapDao.createSearchBuilder();
            groupSearch.and("groupId", groupSearch.entity().getGroupId(), SearchCriteria.Op.EQ);
            sb.join("groupSearch", groupSearch, sb.entity().getId(), groupSearch.entity().getInstanceId(), JoinBuilder.JoinType.INNER);
        }

        if (networkId != null) {
            SearchBuilder<NicVO> nicSearch = _nicDao.createSearchBuilder();
            nicSearch.and("networkId", nicSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);

            SearchBuilder<NetworkVO> networkSearch = _networkDao.createSearchBuilder();
            networkSearch.and("networkId", networkSearch.entity().getId(), SearchCriteria.Op.EQ);
            nicSearch.join("networkSearch", networkSearch, nicSearch.entity().getNetworkId(), networkSearch.entity().getId(), JoinBuilder.JoinType.INNER);

            sb.join("nicSearch", nicSearch, sb.entity().getId(), nicSearch.entity().getInstanceId(), JoinBuilder.JoinType.INNER);
        }

        if (storageId != null) {
            SearchBuilder<VolumeVO> volumeSearch = _volsDao.createSearchBuilder();
            volumeSearch.and("poolId", volumeSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
            sb.join("volumeSearch", volumeSearch, sb.entity().getId(), volumeSearch.entity().getInstanceId(), JoinBuilder.JoinType.INNER);
        }

        // populate the search criteria with the values passed in
        SearchCriteria<UserVmVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);
        
        if (groupId != null && (Long) groupId == -1) {
            sc.setJoinParameters("vmSearch", "instanceId", (Object) null);
        } else if (groupId != null) {
            sc.setJoinParameters("groupSearch", "groupId", groupId);
        }

        if (keyword != null) {
            SearchCriteria<UserVmVO> ssc = _vmDao.createSearchCriteria();
            ssc.addOr("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("hostName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("state", SearchCriteria.Op.EQ, keyword);

            sc.addAnd("displayName", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (networkId != null) {
            sc.setJoinParameters("nicSearch", "networkId", networkId);
        }

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (state != null) {
            if (notState != null && (Boolean) notState == true) {
                sc.setParameters("stateNEQ", state);
            } else {
                sc.setParameters("stateEQ", state);
            }
        }

        if (hypervisor != null) {
            sc.setParameters("hypervisorType", hypervisor);
        }

        // Don't show Destroyed and Expunging vms to the end user
        if ((isAdmin != null) && ((Boolean) isAdmin != true)) {
            sc.setParameters("stateNIN", "Destroyed", "Expunging");
        }

        if (zone != null) {
            sc.setParameters("dataCenterId", zone);

            if (state == null) {
                sc.setParameters("stateNEQ", "Destroyed");
            }
        }
        if (pod != null) {
            sc.setParameters("podId", pod);

            if (state == null) {
                sc.setParameters("stateNEQ", "Destroyed");
            }
        }

        if (hostId != null) {
            sc.setParameters("hostIdEQ", hostId);
        } else {
            if (hostName != null) {
                List<HostVO> hosts = _resourceMgr.listHostsByNameLike((String) hostName);
                if (hosts != null & !hosts.isEmpty()) {
                    Long[] hostIds = new Long[hosts.size()];
                    for (int i = 0; i < hosts.size(); i++) {
                        HostVO host = hosts.get(i);
                        hostIds[i] = host.getId();
                    }
                    sc.setParameters("hostIdIN", (Object[]) hostIds);
                } else {
                    return new ArrayList<UserVmVO>();
                }
            }
        }

        if (storageId != null) {
            sc.setJoinParameters("volumeSearch", "poolId", storageId);
        }

        return _vmDao.search(sc, searchFilter);
    }

    @Override
    public HypervisorType getHypervisorTypeOfUserVM(long vmId) {
        UserVmVO userVm = _vmDao.findById(vmId);
        if (userVm == null) {
        	InvalidParameterValueException ex = new InvalidParameterValueException("unable to find a virtual machine with specified id");
        	ex.addProxyObject(userVm, vmId, "vmId");            
            throw ex;
        }

        return userVm.getHypervisorType();
    }

    @Override
    public UserVm createVirtualMachine(DeployVMCmd cmd) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException, StorageUnavailableException,
    ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserVm getUserVm(long vmId) {
        return _vmDao.findById(vmId);
    }

    @Override
    public VirtualMachine vmStorageMigration(Long vmId, StoragePool destPool) {
        // access check - only root admin can migrate VM
        Account caller = UserContext.current().getCaller();
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Caller is not a root admin, permission denied to migrate the VM");
            }
            throw new PermissionDeniedException("No permission to migrate VM, Only Root Admin can migrate a VM!");
        }

        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find the VM by id=" + vmId);
        }

        if (vm.getState() != State.Stopped) {
        	InvalidParameterValueException ex = new InvalidParameterValueException("VM is not Stopped, unable to migrate the vm having the specified id");
        	ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }

        if (vm.getType() != VirtualMachine.Type.User) {
            throw new InvalidParameterValueException("can only do storage migration on user vm");
        }
        
        List<VolumeVO> vols = _volsDao.findByInstance(vm.getId());
        if (vols.size() > 1) {
        	throw new InvalidParameterValueException("Data disks attached to the vm, can not migrate. Need to dettach data disks at first");
        }

        HypervisorType destHypervisorType = _clusterDao.findById(destPool.getClusterId()).getHypervisorType();
        if (vm.getHypervisorType() != destHypervisorType) {
            throw new InvalidParameterValueException("hypervisor is not compatible: dest: " + destHypervisorType.toString() + ", vm: " + vm.getHypervisorType().toString());
        }
        VMInstanceVO migratedVm = _itMgr.storageMigration(vm, destPool);
        return migratedVm;

    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_MIGRATE, eventDescription = "migrating VM", async = true)
    public VirtualMachine migrateVirtualMachine(Long vmId, Host destinationHost) throws ResourceUnavailableException, ConcurrentOperationException, ManagementServerException, VirtualMachineMigrationException {
        // access check - only root admin can migrate VM
        Account caller = UserContext.current().getCaller();
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Caller is not a root admin, permission denied to migrate the VM");
            }
            throw new PermissionDeniedException("No permission to migrate VM, Only Root Admin can migrate a VM!");
        }

        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find the VM by id=" + vmId);
        }
        // business logic
        if (vm.getState() != State.Running) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is not Running, unable to migrate the vm " + vm);
            }
            InvalidParameterValueException ex = new InvalidParameterValueException("VM is not Running, unable to migrate the vm with specified id");
            ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }
        if (!vm.getHypervisorType().equals(HypervisorType.XenServer) && !vm.getHypervisorType().equals(HypervisorType.VMware) && !vm.getHypervisorType().equals(HypervisorType.KVM) && !vm.getHypervisorType().equals(HypervisorType.Ovm)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(vm + " is not XenServer/VMware/KVM/Ovm, cannot migrate this VM.");
            }
            throw new InvalidParameterValueException("Unsupported Hypervisor Type for VM migration, we support XenServer/VMware/KVM only");
        }

        ServiceOfferingVO svcOffering = _serviceOfferingDao.findById(vm.getServiceOfferingId());
        if (svcOffering.getUseLocalStorage()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(vm + " is using Local Storage, cannot migrate this VM.");
            }
            throw new InvalidParameterValueException("Unsupported operation, VM uses Local storage, cannot migrate");
        }
        
        //check if migrating to same host
        long srcHostId = vm.getHostId();
        if(destinationHost.getId() == srcHostId){
            throw new InvalidParameterValueException("Cannot migrate VM, VM is already presnt on this host, please specify valid destination host to migrate the VM");
        }
        
        //check if host is UP
        if(destinationHost.getStatus() != com.cloud.host.Status.Up || destinationHost.getResourceState() != ResourceState.Enabled){
            throw new InvalidParameterValueException("Cannot migrate VM, destination host is not in correct state, has status: "+destinationHost.getStatus() + ", state: " +destinationHost.getResourceState());
        }

        // call to core process
        DataCenterVO dcVO = _dcDao.findById(destinationHost.getDataCenterId());
        HostPodVO pod = _podDao.findById(destinationHost.getPodId());
        Cluster cluster = _clusterDao.findById(destinationHost.getClusterId());
        DeployDestination dest = new DeployDestination(dcVO, pod, cluster, destinationHost);

        //check max guest vm limit for the destinationHost
        HypervisorType hypervisorType = destinationHost.getHypervisorType();
        String hypervisorVersion = destinationHost.getHypervisorVersion();
        Long maxGuestLimit = _hypervisorCapabilitiesDao.getMaxGuestsLimit(hypervisorType, hypervisorVersion);        
        Long vmCount = _vmInstanceDao.countRunningByHostId(destinationHost.getId());
        if (vmCount.longValue() == maxGuestLimit.longValue()){
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Host name: " + destinationHost.getName() + ", hostId: "+ destinationHost.getId() +" already has max Running VMs(count includes system VMs), limit is: " + maxGuestLimit + " , cannot migrate to this host");
            }
            throw new VirtualMachineMigrationException("Destination host, hostId: "+ destinationHost.getId() +" already has max Running VMs(count includes system VMs), limit is: " + maxGuestLimit + " , cannot migrate to this host");
        }

        VMInstanceVO migratedVm = _itMgr.migrate(vm, srcHostId, dest);
        return migratedVm;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_MOVE, eventDescription = "move VM to another user", async = false)
    public UserVm moveVMToUser(AssignVMCmd cmd) throws ResourceAllocationException, ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // VERIFICATIONS and VALIDATIONS

        //VV 1: verify the two users
        Account caller = UserContext.current().getCaller();
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN  && caller.getType() != Account.ACCOUNT_TYPE_DOMAIN_ADMIN){ // only root admin can assign VMs
        	throw new InvalidParameterValueException("Only domain admins are allowed to assign VMs and not " + caller.getType());
        }

        //get and check the valid VM
        UserVmVO vm = _vmDao.findById(cmd.getVmId());
        if (vm == null){
            throw new InvalidParameterValueException("There is no vm by that id " + cmd.getVmId());
        } else if (vm.getState() == State.Running) {  // VV 3: check if vm is running
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is Running, unable to move the vm " + vm);
            }
            InvalidParameterValueException ex = new InvalidParameterValueException("VM is Running, unable to move the vm with specified vmId");
            ex.addProxyObject(vm, cmd.getVmId(), "vmId");
            throw ex;
        }

        Account oldAccount = _accountService.getActiveAccountById(vm.getAccountId());
        if (oldAccount == null) {
            throw new InvalidParameterValueException("Invalid account for VM " + vm.getAccountId() + " in domain.");
        }
        //don't allow to move the vm from the project
        if (oldAccount.getType() == Account.ACCOUNT_TYPE_PROJECT) {
        	InvalidParameterValueException ex = new InvalidParameterValueException("Specified Vm id belongs to the project and can't be moved");
        	ex.addProxyObject(vm, cmd.getVmId(), "vmId");
            throw ex;
        }
        Account newAccount = _accountService.getActiveAccountByName(cmd.getAccountName(), cmd.getDomainId());
        if (newAccount == null || newAccount.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Invalid accountid=" + cmd.getAccountName() + " in domain " + cmd.getDomainId());
        }
        
        if (newAccount.getState() == Account.State.disabled) {
            throw new InvalidParameterValueException("The new account owner " + cmd.getAccountName() + " is disabled.");
        }
        
        // make sure the accounts are under same domain
        if (oldAccount.getDomainId() != newAccount.getDomainId()){
        	 throw new InvalidParameterValueException("The account should be under same domain for moving VM between two accounts. Old owner domain =" + oldAccount.getDomainId() +
        			 " New owner domain=" + newAccount.getDomainId());
        }
        
        // make sure the accounts are not same
        if (oldAccount.getAccountId() == newAccount.getAccountId()){
       	    throw new InvalidParameterValueException("The account should be same domain for moving VM between two accounts. Account id =" + oldAccount.getAccountId());
        }

        
        // don't allow to move the vm if there are existing PF/LB/Static Nat rules, or vm is assigned to static Nat ip
        List<PortForwardingRuleVO> pfrules = _portForwardingDao.listByVm(cmd.getVmId());
        if (pfrules != null && pfrules.size() > 0){
        	throw new InvalidParameterValueException("Remove the Port forwarding rules for this VM before assigning to another user.");
        }
        List<FirewallRuleVO> snrules = _rulesDao.listStaticNatByVmId(vm.getId());
        if (snrules != null && snrules.size() > 0){
        	throw new InvalidParameterValueException("Remove the StaticNat rules for this VM before assigning to another user.");
        }
        List<LoadBalancerVMMapVO> maps = _loadBalancerVMMapDao.listByInstanceId(vm.getId());
        if (maps != null && maps.size() > 0) {
        	throw new InvalidParameterValueException("Remove the load balancing rules for this VM before assigning to another user.");
        }
        // check for one on one nat
        IPAddressVO ip = _ipAddressDao.findByAssociatedVmId(cmd.getVmId());
        if (ip != null){
        	if (ip.isOneToOneNat()){
        		throw new InvalidParameterValueException("Remove the one to one nat rule for this VM for ip " + ip.toString());
        	}
        }
        
        DataCenterVO zone = _dcDao.findById(vm.getDataCenterIdToDeployIn());
    
        //Remove vm from instance group
        removeInstanceFromInstanceGroup(cmd.getVmId());

        //VV 2: check if account/domain is with in resource limits to create a new vm
        _resourceLimitMgr.checkResourceLimit(newAccount, ResourceType.user_vm);

        // VV 4: Check if new owner can use the vm template
        VirtualMachineTemplate template = _templateDao.findById(vm.getTemplateId());
        if (!template.isPublicTemplate()) {
            Account templateOwner = _accountMgr.getAccount(template.getAccountId());
            _accountMgr.checkAccess(newAccount, null, true, templateOwner);
        }

        // VV 5: check the new account can create vm in the domain
        DomainVO domain = _domainDao.findById(cmd.getDomainId());
        _accountMgr.checkAccess(newAccount, domain);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        //generate destroy vm event for usage
        _usageEventDao.persist(new UsageEventVO(EventTypes.EVENT_VM_DESTROY, vm.getAccountId(), vm.getDataCenterIdToDeployIn(), vm.getId(), 
                vm.getHostName(), vm.getServiceOfferingId(), vm.getTemplateId(), vm.getHypervisorType().toString()));
        // update resource counts
        _resourceLimitMgr.decrementResourceCount(oldAccount.getAccountId(), ResourceType.user_vm);

        // OWNERSHIP STEP 1: update the vm owner
        vm.setAccountId(newAccount.getAccountId());
        vm.setDomainId(cmd.getDomainId());
        _vmDao.persist(vm);
        
        // OS 2: update volume
        List<VolumeVO> volumes = _volsDao.findByInstance(cmd.getVmId());
        for (VolumeVO volume : volumes) {
            _usageEventDao.persist(new UsageEventVO(EventTypes.EVENT_VOLUME_DELETE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName()));
            _resourceLimitMgr.decrementResourceCount(oldAccount.getAccountId(), ResourceType.volume);
            volume.setAccountId(newAccount.getAccountId());
            _volsDao.persist(volume);
            _resourceLimitMgr.incrementResourceCount(newAccount.getAccountId(), ResourceType.volume);
            _usageEventDao.persist(new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(),
            		volume.getDiskOfferingId(), volume.getTemplateId(), volume.getSize()));
        }

        _resourceLimitMgr.incrementResourceCount(newAccount.getAccountId(), ResourceType.user_vm);
        //generate usage events to account for this change
        _usageEventDao.persist(new UsageEventVO(EventTypes.EVENT_VM_CREATE, vm.getAccountId(), vm.getDataCenterIdToDeployIn(), vm.getId(), 
                vm.getHostName(), vm.getServiceOfferingId(),  vm.getTemplateId(), vm.getHypervisorType().toString()));

        txn.commit();

        VMInstanceVO vmoi = _itMgr.findByIdAndType(vm.getType(), vm.getId());
        VirtualMachineProfileImpl<VMInstanceVO> vmOldProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vmoi);

        // OS 3: update the network
        List<Long> networkIdList = cmd.getNetworkIds();
        List<Long> securityGroupIdList = cmd.getSecurityGroupIdList();
        
        if (zone.getNetworkType() == NetworkType.Basic) {
        	 if (networkIdList != null && !networkIdList.isEmpty()) {
                 throw new InvalidParameterValueException("Can't move vm with network Ids; this is a basic zone VM");
             }
         	//cleanup the old security groups
             _securityGroupMgr.removeInstanceFromGroups(cmd.getVmId());
       	 	//cleanup the network for the oldOwner
            _networkMgr.cleanupNics(vmOldProfile);
            _networkMgr.expungeNics(vmOldProfile);
        	//security groups will be recreated for the new account, when the VM is started
            List<NetworkVO> networkList = new ArrayList<NetworkVO>();

            // Get default guest network in Basic zone
            Network defaultNetwork = _networkMgr.getExclusiveGuestNetwork(zone.getId());

            if (defaultNetwork == null) {
                throw new InvalidParameterValueException("Unable to find a default network to start a vm");
            } else {
                networkList.add(_networkDao.findById(defaultNetwork.getId()));
            }

            boolean isVmWare = (template.getHypervisorType() == HypervisorType.VMware);

            if (securityGroupIdList != null && isVmWare) {
                throw new InvalidParameterValueException("Security group feature is not supported for vmWare hypervisor");
            } else if (!isVmWare && _networkMgr.isSecurityGroupSupportedInNetwork(defaultNetwork) && _networkMgr.canAddDefaultSecurityGroup()) {
                if (securityGroupIdList == null) {
                    securityGroupIdList = new ArrayList<Long>();
                }
                SecurityGroup defaultGroup = _securityGroupMgr.getDefaultSecurityGroup(newAccount.getId());
                if (defaultGroup != null) {
                    //check if security group id list already contains Default security group, and if not - add it
                    boolean defaultGroupPresent = false;
                    for (Long securityGroupId : securityGroupIdList) {
                        if (securityGroupId.longValue() == defaultGroup.getId()) {
                            defaultGroupPresent = true;
                            break;
                        }
                    }

                    if (!defaultGroupPresent) {
                        securityGroupIdList.add(defaultGroup.getId());
                    }

                } else {
                    //create default security group for the account
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Couldn't find default security group for the account " + newAccount + " so creating a new one");
                    }
                    defaultGroup = _securityGroupMgr.createSecurityGroup(SecurityGroupManager.DEFAULT_GROUP_NAME, SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION, newAccount.getDomainId(), newAccount.getId(), newAccount.getAccountName());
                    securityGroupIdList.add(defaultGroup.getId());
                }
            }
            

            List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>();
            NicProfile profile = new NicProfile();
            profile.setDefaultNic(true);
            networks.add(new Pair<NetworkVO, NicProfile>(networkList.get(0), profile));
           
            VMInstanceVO vmi = _itMgr.findByIdAndType(vm.getType(), vm.getId());
            VirtualMachineProfileImpl<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vmi);
            _networkMgr.allocate(vmProfile, networks);

            _securityGroupMgr.addInstanceToGroups(vm.getId(), securityGroupIdList);
            
            s_logger.debug("AssignVM: Basic zone, adding security groups no " +  securityGroupIdList.size() + " to " + vm.getInstanceName() );
        } else {
            if (zone.isSecurityGroupEnabled())  {
            	throw new InvalidParameterValueException("Not yet implemented for SecurityGroupEnabled advanced networks.");
            } else {
                if (securityGroupIdList != null && !securityGroupIdList.isEmpty()) {
                    throw new InvalidParameterValueException("Can't move vm with security groups; security group feature is not enabled in this zone");
                }
            	 //cleanup the network for the oldOwner
                _networkMgr.cleanupNics(vmOldProfile);
                _networkMgr.expungeNics(vmOldProfile);
                
                Set<NetworkVO> applicableNetworks = new HashSet<NetworkVO>();
            
                if (networkIdList != null && !networkIdList.isEmpty()){
	                // add any additional networks
	                for (Long networkId : networkIdList) {
	                    NetworkVO network = _networkDao.findById(networkId);
	                    if (network == null) {
	                    	InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find specified network id");
	                    	ex.addProxyObject(network, networkId, "networkId");
	                        throw ex;
	                    }
	
	                    _networkMgr.checkNetworkPermissions(newAccount, network);
	
	                    //don't allow to use system networks 
	                    NetworkOffering networkOffering = _configMgr.getNetworkOffering(network.getNetworkOfferingId());
	                    if (networkOffering.isSystemOnly()) {
	                    	InvalidParameterValueException ex = new InvalidParameterValueException("Specified Network id is system only and can't be used for vm deployment");
	                    	ex.addProxyObject(network, networkId, "networkId");
	                        throw ex;
	                    }
	                    applicableNetworks.add(network);
	                }
                }
                else {
                	NetworkVO defaultNetwork = null;
                    List<NetworkOfferingVO> requiredOfferings = _networkOfferingDao.listByAvailability(Availability.Required, false);
                    if (requiredOfferings.size() < 1) {
                    	throw new InvalidParameterValueException("Unable to find network offering with availability=" + Availability.Required + " to automatically create the network as a part of vm creation");
                    }
                    
                    PhysicalNetwork physicalNetwork = _networkMgr.translateZoneIdToPhysicalNetwork(zone.getId());
                    if (requiredOfferings.get(0).getState() == NetworkOffering.State.Enabled) {
                        // get Virtual networks
                        List<NetworkVO> virtualNetworks = _networkMgr.listNetworksForAccount(newAccount.getId(), zone.getId(), Network.GuestType.Isolated);

                        if (virtualNetworks.isEmpty()) {
                            s_logger.debug("Creating network for account " + newAccount + " from the network offering id=" + requiredOfferings.get(0).getId() + " as a part of deployVM process");
                            Network newNetwork = _networkMgr.createGuestNetwork(requiredOfferings.get(0).getId(), newAccount.getAccountName() + "-network", newAccount.getAccountName() + "-network", null, null,
                                    null, null, newAccount, false, null, physicalNetwork, zone.getId(), ACLType.Account, null);
                            defaultNetwork = _networkDao.findById(newNetwork.getId());
                        } else if (virtualNetworks.size() > 1) {
                            throw new InvalidParameterValueException("More than 1 default Isolated networks are found for account " + newAccount + "; please specify networkIds");
                        } else {
                            defaultNetwork = virtualNetworks.get(0);
                        }
                    } else {
                    	throw new InvalidParameterValueException("Required network offering id=" + requiredOfferings.get(0).getId() + " is not in " + NetworkOffering.State.Enabled); 
                    }

                    applicableNetworks.add(defaultNetwork);
                }

                // add the new nics
                List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>();
                int toggle=0;
                for (NetworkVO appNet: applicableNetworks){                    
                    NicProfile defaultNic = new NicProfile();
                    if (toggle==0){
                        defaultNic.setDefaultNic(true);
                        toggle++;
                    }
                    networks.add(new Pair<NetworkVO, NicProfile>(appNet, defaultNic));
                }
                VMInstanceVO vmi = _itMgr.findByIdAndType(vm.getType(), vm.getId());
                VirtualMachineProfileImpl<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vmi);
                _networkMgr.allocate(vmProfile, networks);
                s_logger.debug("AssignVM: Advance virtual, adding networks no " +  networks.size() + " to " + vm.getInstanceName() );
            } //END IF NON SEC GRP ENABLED
        } // END IF ADVANCED
        s_logger.info("AssignVM: vm " + vm.getInstanceName() + " now belongs to account " + cmd.getAccountName());
        return vm;
    }


    @Override
    public UserVm restoreVM(RestoreVMCmd cmd) {
        // Input validation
        Account caller = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();
        UserVO user = _userDao.findById(userId);
        boolean needRestart = false;

        long vmId = cmd.getVmId();
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
        	InvalidParameterValueException ex = new InvalidParameterValueException("Cann not find VM with ID " + vmId);
        	ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }
        
        Account owner = _accountDao.findById(vm.getAccountId());
        if (owner == null) {
            throw new InvalidParameterValueException("The owner of " + vm + " does not exist: " + vm.getAccountId());
        }

        if (owner.getState() == Account.State.disabled) {
            throw new PermissionDeniedException("The owner of " + vm + " is disabled: " + vm.getAccountId());
        }

        if (vm.getState() != VirtualMachine.State.Running && vm.getState() != VirtualMachine.State.Stopped) {
            throw new CloudRuntimeException("Vm " + vmId + " currently in " + vm.getState() + " state, restore vm can only execute when VM in Running or Stopped");
        }

        if (vm.getState() == VirtualMachine.State.Running) {
            needRestart = true;
        }

        List<VolumeVO> rootVols = _volsDao.findByInstance(vmId);
        if (rootVols.isEmpty()) {
        	InvalidParameterValueException ex = new InvalidParameterValueException("Can not find root volume for VM " + vmId);
        	ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }

        VolumeVO root = rootVols.get(0);
        long templateId = root.getTemplateId();
        VMTemplateVO template = _templateDao.findById(templateId);
        if (template == null) {
        	InvalidParameterValueException ex = new InvalidParameterValueException("Cannot find template for specified volumeid and vmId");
        	ex.addProxyObject(vm, vmId, "vmId");
        	ex.addProxyObject(root, root.getId(), "volumeId");
            throw ex;
        }

        if (needRestart) {
            try {
                _itMgr.stop(vm, user, caller);
            } catch (ResourceUnavailableException e) {
                s_logger.debug("Stop vm " + vmId + " failed", e);
                CloudRuntimeException ex = new CloudRuntimeException("Stop vm failed for specified vmId");
                ex.addProxyObject(vm, vmId, "vmId");
                throw ex;
            }
        }

        /* allocate a new volume from original template*/
        VolumeVO newVol = _storageMgr.allocateDuplicateVolume(root, null);
        _volsDao.attachVolume(newVol.getId(), vmId, newVol.getDeviceId());

        /* Detach and destory the old root volume */
        try {
            _volsDao.detachVolume(root.getId());
            _storageMgr.destroyVolume(root);
        } catch (ConcurrentOperationException e) {
            s_logger.debug("Unable to delete old root volume " + root.getId() + ", user may manually delete it", e);
        }

        if (needRestart) {
            try {
                _itMgr.start(vm, null, user, caller);
            } catch (Exception e) {
                s_logger.debug("Unable to start VM " + vmId, e);
                CloudRuntimeException ex = new CloudRuntimeException("Unable to start VM with specified id" + e.getMessage());
                ex.addProxyObject(vm, vmId, "vmId");
                throw ex;
            }
        }

        s_logger.debug("Restore VM " + vmId + " with template " + root.getTemplateId() + " successfully");
        return vm;
    }
    
    
}
