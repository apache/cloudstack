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
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.admin.vm.AssignVMCmd;
import org.apache.cloudstack.api.command.admin.vm.RecoverVMCmd;
import org.apache.cloudstack.api.command.user.vm.AddNicToVMCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.api.command.user.vm.RebootVMCmd;
import org.apache.cloudstack.api.command.user.vm.RemoveNicFromVMCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMPasswordCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMSSHKeyCmd;
import org.apache.cloudstack.api.command.user.vm.RestoreVMCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateDefaultNicForVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpgradeVMCmd;
import org.apache.cloudstack.api.command.user.vmgroup.CreateVMGroupCmd;
import org.apache.cloudstack.api.command.user.vmgroup.DeleteVMGroupCmd;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntity;
import org.apache.cloudstack.engine.service.api.OrchestrationService;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.async.AsyncJobManager;
import com.cloud.capacity.CapacityManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeployPlannerSelector;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.CloudException;
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
import com.cloud.network.Network;
import com.cloud.network.Network.IpAddresses;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVMMapVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.VpcDao;
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
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeManager;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeHostDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Journal;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.crypt.RSAHelper;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmCloneSettingDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshotManager;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@Local(value = { UserVmManager.class, UserVmService.class })
public class UserVmManagerImpl extends ManagerBase implements UserVmManager, UserVmService {
    private static final Logger s_logger = Logger
            .getLogger(UserVmManagerImpl.class);

    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3; // 3
    // seconds

    public enum UserVmCloneType {
        full,
        linked
    }

    @Inject
    protected HostDao _hostDao = null;
    @Inject
    protected ServiceOfferingDao _offeringDao = null;
    @Inject
    protected DiskOfferingDao _diskOfferingDao = null;
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
    protected UserVmCloneSettingDao _vmCloneSettingDao = null;
    @Inject
    protected UserVmDao _vmDao = null;
    @Inject
    protected UserVmJoinDao _vmJoinDao = null;
    @Inject
    protected VolumeDao _volsDao = null;
    @Inject
    protected DataCenterDao _dcDao = null;
    @Inject
    protected FirewallRulesDao _rulesDao = null;
    @Inject
    protected LoadBalancerVMMapDao _loadBalancerVMMapDao = null;
    @Inject
    protected PortForwardingRulesDao _portForwardingDao;
    @Inject
    protected IPAddressDao _ipAddressDao = null;
    @Inject
    protected HostPodDao _podDao = null;
    @Inject
    protected NetworkModel _networkModel = null;
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
    protected ClusterDao _clusterDao;
    @Inject
    protected PrimaryDataStoreDao _storagePoolDao;
    @Inject
    protected SecurityGroupManager _securityGroupMgr;
    @Inject
    protected ServiceOfferingDao _serviceOfferingDao;
    @Inject
    protected NetworkOfferingDao _networkOfferingDao;
    @Inject
    protected InstanceGroupDao _vmGroupDao;
    @Inject
    protected InstanceGroupVMMapDao _groupVMMapDao;
    @Inject
    protected VirtualMachineManager _itMgr;
    @Inject
    protected NetworkDao _networkDao;
    @Inject
    protected NicDao _nicDao;
    @Inject
    protected VpcDao _vpcDao;
    @Inject
    protected RulesManager _rulesMgr;
    @Inject
    protected LoadBalancingRulesManager _lbMgr;
    @Inject
    protected SSHKeyPairDao _sshKeyPairDao;
    @Inject
    protected UserVmDetailsDao _vmDetailsDao;
    @Inject
    protected HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;
    @Inject
    protected SecurityGroupDao _securityGroupDao;
    @Inject
    protected CapacityManager _capacityMgr;;
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
    @Inject
    protected ItWorkDao _workDao;
    @Inject
    protected VolumeHostDao _volumeHostDao;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    TemplateManager templateMgr;
    @Inject 
    protected GuestOSCategoryDao _guestOSCategoryDao;
    @Inject
    UsageEventDao _usageEventDao;
    @Inject
    protected VMSnapshotDao _vmSnapshotDao;
    @Inject
    protected VMSnapshotManager _vmSnapshotMgr;

    @Inject
    List<DeployPlannerSelector> plannerSelectors;

    protected ScheduledExecutorService _executor = null;
    protected int _expungeInterval;
    protected int _expungeDelay;

    protected String _name;
    protected String _instance;
    protected String _zone;
    protected boolean _instanceNameFlag;

    @Inject ConfigurationDao _configDao;
    private int _createprivatetemplatefromvolumewait;
    private int _createprivatetemplatefromsnapshotwait;
    private final int MAX_VM_NAME_LEN = 80;

    @Inject
    protected OrchestrationService _orchSrvc;

    @Inject VolumeManager volumeMgr;

    @Override
    public UserVmVO getVirtualMachine(long vmId) {
        return _vmDao.findById(vmId);
    }

    @Override
    public List<? extends UserVm> getVirtualMachines(long hostId) {
        return _vmDao.listByHostId(hostId);
    }

    protected void resourceLimitCheck (Account owner, Long cpu, Long memory) throws ResourceAllocationException {
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.user_vm);
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.cpu, cpu);
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.memory, memory);
    }

    protected void resourceCountIncrement (long accountId, Long cpu, Long memory) {
        _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.user_vm);
        _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.cpu, cpu);
        _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.memory, memory);
    }

    protected void resourceCountDecrement (long accountId, Long cpu, Long memory) {
        _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.user_vm);
        _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.cpu, cpu);
        _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.memory, memory);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_RESETPASSWORD, eventDescription = "resetting Vm password", async = true)
    public UserVm resetVMPassword(ResetVMPasswordCmd cmd, String password)
            throws ResourceUnavailableException, InsufficientCapacityException {
        Account caller = UserContext.current().getCaller();
        Long vmId = cmd.getId();
        UserVmVO userVm = _vmDao.findById(cmd.getId());
        _vmDao.loadDetails(userVm);

        // Do parameters input validation
        if (userVm == null) {
            throw new InvalidParameterValueException(
                    "unable to find a virtual machine with id " + cmd.getId());
        }

        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(userVm
                .getTemplateId());
        if (template == null || !template.getEnablePassword()) {
            throw new InvalidParameterValueException(
                    "Fail to reset password for the virtual machine, the template is not password enabled");
        }

        if (userVm.getState() == State.Error
                || userVm.getState() == State.Expunging) {
            s_logger.error("vm is not in the right state: " + vmId);
            throw new InvalidParameterValueException("Vm with id " + vmId
                    + " is not in the right state");
        }

        _accountMgr.checkAccess(caller, null, true, userVm);

        boolean result = resetVMPasswordInternal(cmd, password);

        if (result) {
            userVm.setPassword(password);
            // update the password in vm_details table too
            // Check if an SSH key pair was selected for the instance and if so
            // use it to encrypt & save the vm password
            String sshPublicKey = userVm.getDetail("SSH.PublicKey");
            if (sshPublicKey != null && !sshPublicKey.equals("")
                    && password != null && !password.equals("saved_password")) {
                String encryptedPasswd = RSAHelper.encryptWithSSHPublicKey(
                        sshPublicKey, password);
                if (encryptedPasswd == null) {
                    throw new CloudRuntimeException("Error encrypting password");
                }

                userVm.setDetail("Encrypted.Password", encryptedPasswd);
                _vmDao.saveDetails(userVm);
            }
        } else {
            throw new CloudRuntimeException(
                    "Failed to reset password for the virtual machine ");
        }

        return userVm;
    }

    private boolean resetVMPasswordInternal(ResetVMPasswordCmd cmd,
            String password) throws ResourceUnavailableException,
            InsufficientCapacityException {
        Long vmId = cmd.getId();
        Long userId = UserContext.current().getCallerUserId();
        VMInstanceVO vmInstance = _vmDao.findById(vmId);

        if (password == null || password.equals("")) {
            return false;
        }

        VMTemplateVO template = _templateDao
                .findByIdIncludingRemoved(vmInstance.getTemplateId());
        if (template.getEnablePassword()) {
            Nic defaultNic = _networkModel.getDefaultNic(vmId);
            if (defaultNic == null) {
                s_logger.error("Unable to reset password for vm " + vmInstance
                        + " as the instance doesn't have default nic");
                return false;
            }

            Network defaultNetwork = _networkDao.findById(defaultNic.getNetworkId());
            NicProfile defaultNicProfile = new NicProfile(defaultNic, defaultNetwork, null, null, null, _networkModel.isSecurityGroupSupportedInNetwork(defaultNetwork), _networkModel.getNetworkTag(template.getHypervisorType(), defaultNetwork));
            VirtualMachineProfile<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vmInstance);
            vmProfile.setParameter(VirtualMachineProfile.Param.VmPassword, password);

            UserDataServiceProvider element = _networkMgr.getPasswordResetProvider(defaultNetwork);
            if (element == null) {
                throw new CloudRuntimeException(
                        "Can't find network element for "
                                + Service.UserData.getName()
                                + " provider needed for password reset");
            }

            boolean result = element.savePassword(defaultNetwork,
                    defaultNicProfile, vmProfile);

            // Need to reboot the virtual machine so that the password gets
            // redownloaded from the DomR, and reset on the VM
            if (!result) {
                s_logger.debug("Failed to reset password for the virutal machine; no need to reboot the vm");
                return false;
            } else {
                if (vmInstance.getState() == State.Stopped) {
                    s_logger.debug("Vm "
                            + vmInstance
                            + " is stopped, not rebooting it as a part of password reset");
                    return true;
                }

                if (rebootVirtualMachine(userId, vmId) == null) {
                    s_logger.warn("Failed to reboot the vm " + vmInstance);
                    return false;
                } else {
                    s_logger.debug("Vm "
                            + vmInstance
                            + " is rebooted successfully as a part of password reset");
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
    @ActionEvent(eventType = EventTypes.EVENT_VM_RESETSSHKEY, eventDescription = "resetting Vm SSHKey", async = true)
    public UserVm resetVMSSHKey(ResetVMSSHKeyCmd cmd)
            throws ResourceUnavailableException, InsufficientCapacityException {

        Account caller = UserContext.current().getCaller();
        Account owner = _accountMgr.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
        Long vmId = cmd.getId();

        UserVmVO userVm = _vmDao.findById(cmd.getId());
        _vmDao.loadDetails(userVm);
        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(userVm.getTemplateId());

        // Do parameters input validation

        if (userVm == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine by id" + cmd.getId());
        }

        if (userVm.getState() == State.Error || userVm.getState() == State.Expunging) {
            s_logger.error("vm is not in the right state: " + vmId);
            throw new InvalidParameterValueException("Vm with specified id is not in the right state");
        }
        if (userVm.getState() != State.Stopped) {
            s_logger.error("vm is not in the right state: " + vmId);
            throw new InvalidParameterValueException("Vm " + userVm + " should be stopped to do SSH Key reset");
        }

        SSHKeyPairVO s = _sshKeyPairDao.findByName(owner.getAccountId(), owner.getDomainId(), cmd.getName());
        if (s == null) {
            throw new InvalidParameterValueException("A key pair with name '" + cmd.getName() + "' does not exist for account " + owner.getAccountName() + " in specified domain id");
        }

        _accountMgr.checkAccess(caller, null, true, userVm);
        String password = null;
        String sshPublicKey = s.getPublicKey();
        if (template != null && template.getEnablePassword()) {
            password = generateRandomPassword();
        }

        boolean result = resetVMSSHKeyInternal(vmId, sshPublicKey, password);

        if (result) {
            userVm.setDetail("SSH.PublicKey", sshPublicKey);
            if (template != null && template.getEnablePassword()) {
                userVm.setPassword(password);
                //update the encrypted password in vm_details table too
                if (sshPublicKey != null && !sshPublicKey.equals("") && password != null && !password.equals("saved_password")) {
                    String encryptedPasswd = RSAHelper.encryptWithSSHPublicKey(sshPublicKey, password);
                    if (encryptedPasswd == null) {
                        throw new CloudRuntimeException("Error encrypting password");
                    }
                    userVm.setDetail("Encrypted.Password", encryptedPasswd);
                }
            }
            _vmDao.saveDetails(userVm);
        } else {
            throw new CloudRuntimeException("Failed to reset SSH Key for the virtual machine ");
        }
        return userVm;
    }

    private boolean resetVMSSHKeyInternal(Long vmId, String SSHPublicKey, String password) throws ResourceUnavailableException, InsufficientCapacityException {
        Long userId = UserContext.current().getCallerUserId();
        VMInstanceVO vmInstance = _vmDao.findById(vmId);

        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(vmInstance.getTemplateId());
        Nic defaultNic = _networkModel.getDefaultNic(vmId);
        if (defaultNic == null) {
            s_logger.error("Unable to reset SSH Key for vm " + vmInstance + " as the instance doesn't have default nic");
            return false;
        }

        Network defaultNetwork = _networkDao.findById(defaultNic.getNetworkId());
        NicProfile defaultNicProfile = new NicProfile(defaultNic, defaultNetwork, null, null, null,
                _networkModel.isSecurityGroupSupportedInNetwork(defaultNetwork),
                _networkModel.getNetworkTag(template.getHypervisorType(), defaultNetwork));

        VirtualMachineProfile<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vmInstance);

        if (template != null && template.getEnablePassword()) {
            vmProfile.setParameter(VirtualMachineProfile.Param.VmPassword, password);
        }

        UserDataServiceProvider element = _networkMgr.getSSHKeyResetProvider(defaultNetwork);
        if (element == null) {
            throw new CloudRuntimeException("Can't find network element for " + Service.UserData.getName() + " provider needed for SSH Key reset");
        }
        boolean result = element.saveSSHKey(defaultNetwork, defaultNicProfile, vmProfile, SSHPublicKey);

        // Need to reboot the virtual machine so that the password gets redownloaded from the DomR, and reset on the VM
        if (!result) {
            s_logger.debug("Failed to reset SSH Key for the virutal machine; no need to reboot the vm");
            return false;
        } else {
            if (vmInstance.getState() == State.Stopped) {
                s_logger.debug("Vm " + vmInstance + " is stopped, not rebooting it as a part of SSH Key reset");
                return true;
            }
            if (rebootVirtualMachine(userId, vmId) == null) {
                s_logger.warn("Failed to reboot the vm " + vmInstance);
                return false;
            } else {
                s_logger.debug("Vm " + vmInstance + " is rebooted successfully as a part of SSH Key reset");
                return true;
            }
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
            VirtualMachineEntity vmEntity = _orchSrvc.getVirtualMachine(vm.getUuid());
            status = vmEntity.stop(new Long(userId).toString());            
        } catch (ResourceUnavailableException e) {
            s_logger.debug("Unable to stop due to ", e);
            status = false;
        } catch (CloudException e) {
            throw new CloudRuntimeException(
                    "Unable to contact the agent to stop the virtual machine "
                            + vm, e);
        }

        if (status) {
            return status;
        } else {
            return status;
        }
    }




    private void checkVMSnapshots(UserVmVO vm, Long volumeId, boolean attach) {
        // Check that if vm has any VM snapshot
        /*Long vmId = vm.getId();
        List<VMSnapshotVO> listSnapshot = _vmSnapshotDao.listByInstanceId(vmId,
                VMSnapshot.State.Ready, VMSnapshot.State.Creating, VMSnapshot.State.Reverting, VMSnapshot.State.Expunging);
        if (listSnapshot != null && listSnapshot.size() != 0) {
            throw new InvalidParameterValueException(
                        "The VM has VM snapshots, do not allowed to attach volume. Please delete the VM snapshots first.");
        }*/
    }



    private UserVm rebootVirtualMachine(long userId, long vmId)
            throws InsufficientCapacityException, ResourceUnavailableException {
        UserVmVO vm = _vmDao.findById(vmId);
        User caller = _accountMgr.getActiveUser(userId);
        Account owner = _accountMgr.getAccount(vm.getAccountId());

        if (vm == null || vm.getState() == State.Destroyed
                || vm.getState() == State.Expunging || vm.getRemoved() != null) {
            s_logger.warn("Vm id=" + vmId + " doesn't exist");
            return null;
        }

        if (vm.getState() == State.Running && vm.getHostId() != null) {
            return _itMgr.reboot(vm, null, caller, owner);
        } else {
            s_logger.error("Vm id=" + vmId
                    + " is not in Running state, failed to reboot");
            return null;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_UPGRADE, eventDescription = "upgrading Vm")
    /*
     * TODO: cleanup eventually - Refactored API call
     */
    public UserVm upgradeVirtualMachine(UpgradeVMCmd cmd) throws ResourceAllocationException {
        Long vmId = cmd.getId();
        Long svcOffId = cmd.getServiceOfferingId();
        Account caller = UserContext.current().getCaller();

        // Verify input parameters
        UserVmVO vmInstance = _vmDao.findById(vmId);
        if (vmInstance == null) {
            throw new InvalidParameterValueException(
                    "unable to find a virtual machine with id " + vmId);
        }

        _accountMgr.checkAccess(caller, null, true, vmInstance);

        // Check resource limits for CPU and Memory.
        ServiceOfferingVO newServiceOffering = _offeringDao.findById(svcOffId);
        ServiceOfferingVO currentServiceOffering = _offeringDao.findByIdIncludingRemoved(vmInstance.getServiceOfferingId());

        int newCpu = newServiceOffering.getCpu();
        int newMemory = newServiceOffering.getRamSize();
        int currentCpu = currentServiceOffering.getCpu();
        int currentMemory = currentServiceOffering.getRamSize();

        if (newCpu > currentCpu) {
            _resourceLimitMgr.checkResourceLimit(caller, ResourceType.cpu,
                    newCpu - currentCpu);
        }
        if (newMemory > currentMemory) {
            _resourceLimitMgr.checkResourceLimit(caller, ResourceType.memory,
                    newMemory - currentMemory);
        }

        // Check that the specified service offering ID is valid
        _itMgr.checkIfCanUpgrade(vmInstance, svcOffId);

        // remove diskAndMemory VM snapshots
        /* List<VMSnapshotVO> vmSnapshots = _vmSnapshotDao.findByVm(vmId);
        for (VMSnapshotVO vmSnapshotVO : vmSnapshots) {
            if(vmSnapshotVO.getType() == VMSnapshot.Type.DiskAndMemory){
                if(!_vmSnapshotMgr.deleteAllVMSnapshots(vmId, VMSnapshot.Type.DiskAndMemory)){
                    String errMsg = "Failed to remove VM snapshot during upgrading, snapshot id " + vmSnapshotVO.getId();
                    s_logger.debug(errMsg);
                    throw new CloudRuntimeException(errMsg);
                }

            }
        }*/

        _itMgr.upgradeVmDb(vmId, svcOffId);

        // Increment or decrement CPU and Memory count accordingly.
        if (newCpu > currentCpu) {
            _resourceLimitMgr.incrementResourceCount(caller.getAccountId(), ResourceType.cpu, new Long (newCpu - currentCpu));
        } else if (currentCpu > newCpu) {
            _resourceLimitMgr.decrementResourceCount(caller.getAccountId(), ResourceType.cpu, new Long (currentCpu - newCpu));
        }
        if (newMemory > currentMemory) {
            _resourceLimitMgr.incrementResourceCount(caller.getAccountId(), ResourceType.memory, new Long (newMemory - currentMemory));
        } else if (currentMemory > newMemory) {
            _resourceLimitMgr.decrementResourceCount(caller.getAccountId(), ResourceType.memory, new Long (currentMemory - newMemory));
        }

        return _vmDao.findById(vmInstance.getId());
    }

    @Override
    public UserVm addNicToVirtualMachine(AddNicToVMCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, CloudRuntimeException {
        Long vmId = cmd.getVmId();
        Long networkId = cmd.getNetworkId();
        String ipAddress = cmd.getIpAddress();
        Account caller = UserContext.current().getCaller();

        UserVmVO vmInstance = _vmDao.findById(vmId);
        if(vmInstance == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }
        NetworkVO network = _networkDao.findById(networkId);
        if(network == null) {
            throw new InvalidParameterValueException("unable to find a network with id " + networkId);
        }
        NicProfile profile = new NicProfile(null, null);
        if(ipAddress != null) {
            profile = new NicProfile(ipAddress, null);
        }

        // Perform permission check on VM
        _accountMgr.checkAccess(caller, null, true, vmInstance);

        // Verify that zone is not Basic
        DataCenterVO dc = _dcDao.findById(vmInstance.getDataCenterId());
        if (dc.getNetworkType() == DataCenter.NetworkType.Basic) {
            throw new CloudRuntimeException("Zone " + vmInstance.getDataCenterId() + ", has a NetworkType of Basic. Can't add a new NIC to a VM on a Basic Network");
        }

        // Perform account permission check on network
        if (network.getGuestType() != Network.GuestType.Shared) {
            // Check account permissions
            List<NetworkVO> networkMap = _networkDao.listBy(caller.getId(), network.getId());
            if ((networkMap == null || networkMap.isEmpty() ) && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                throw new PermissionDeniedException("Unable to modify a vm using network with id " + network.getId() + ", permission denied");
            }
        }

        //ensure network belongs in zone
        if (network.getDataCenterId() != vmInstance.getDataCenterId()) {
            throw new CloudRuntimeException(vmInstance + " is in zone:" + vmInstance.getDataCenterId() + " but " + network + " is in zone:" + network.getDataCenterId());
        }

        if(_networkModel.getNicInNetwork(vmInstance.getId(),network.getId()) != null){
            s_logger.debug(vmInstance + " already in " + network + " going to add another NIC");
        } else {
            //* get all vms hostNames in the network
            List<String> hostNames = _vmInstanceDao.listDistinctHostNames(network.getId());
            //* verify that there are no duplicates
            if (hostNames.contains(vmInstance.getHostName())) {
                throw new CloudRuntimeException(network + " already has a vm with host name: '" + vmInstance.getHostName());
            }
        }

        NicProfile guestNic = null;

        try {
            guestNic = _itMgr.addVmToNetwork(vmInstance, network, profile);
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Unable to add NIC to " + vmInstance + ": " + e);
        } catch (InsufficientCapacityException e) {
            throw new CloudRuntimeException("Insufficient capacity when adding NIC to " + vmInstance + ": " + e);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Concurrent operations on adding NIC to " + vmInstance + ": " +e);
        }
        if (guestNic == null) {
            throw new CloudRuntimeException("Unable to add NIC to " + vmInstance);
        }

        s_logger.debug("Successful addition of " + network + " from " + vmInstance);
        return _vmDao.findById(vmInstance.getId());
    }

    @Override
    public UserVm removeNicFromVirtualMachine(RemoveNicFromVMCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, CloudRuntimeException {
        Long vmId = cmd.getVmId();
        Long nicId = cmd.getNicId();
        Account caller = UserContext.current().getCaller();

        UserVmVO vmInstance = _vmDao.findById(vmId);
        if(vmInstance == null) {
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }
        NicVO nic = _nicDao.findById(nicId);
        if (nic == null){
            throw new InvalidParameterValueException("unable to find a nic with id " + nicId);
        }
        NetworkVO network = _networkDao.findById(nic.getNetworkId());
        if(network == null) {
            throw new InvalidParameterValueException("unable to find a network with id " + nic.getNetworkId());
        }

        // Perform permission check on VM
        _accountMgr.checkAccess(caller, null, true, vmInstance);

        // Verify that zone is not Basic
        DataCenterVO dc = _dcDao.findById(vmInstance.getDataCenterId());
        if (dc.getNetworkType() == DataCenter.NetworkType.Basic) {
            throw new CloudRuntimeException("Zone " + vmInstance.getDataCenterId() + ", has a NetworkType of Basic. Can't remove a NIC from a VM on a Basic Network");
        }

        //check to see if nic is attached to VM
        if (nic.getInstanceId() != vmId) {
            throw new InvalidParameterValueException(nic + " is not a nic on  " + vmInstance);
        }

        // Perform account permission check on network
        if (network.getGuestType() != Network.GuestType.Shared) {
            // Check account permissions
            List<NetworkVO> networkMap = _networkDao.listBy(caller.getId(), network.getId());
            if ((networkMap == null || networkMap.isEmpty() ) && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                throw new PermissionDeniedException("Unable to modify a vm using network with id " + network.getId() + ", permission denied");
            }
        }

        boolean nicremoved = false;

        try {
            nicremoved = _itMgr.removeNicFromVm(vmInstance, nic);
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Unable to remove " + network + " from " + vmInstance +": " + e);

        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Concurrent operations on removing " + network + " from " + vmInstance + ": " + e);
        }

        if (!nicremoved) {
            throw new CloudRuntimeException("Unable to remove " + network +  " from " + vmInstance );
        }

        s_logger.debug("Successful removal of " + network + " from " + vmInstance);
        return _vmDao.findById(vmInstance.getId());


    }

    @Override
    public UserVm updateDefaultNicForVirtualMachine(UpdateDefaultNicForVMCmd cmd) throws InvalidParameterValueException, CloudRuntimeException {
        Long vmId = cmd.getVmId();
        Long nicId = cmd.getNicId();
        Account caller = UserContext.current().getCaller();

        UserVmVO vmInstance = _vmDao.findById(vmId);
        if (vmInstance == null){
            throw new InvalidParameterValueException("unable to find a virtual machine with id " + vmId);
        }
        NicVO nic = _nicDao.findById(nicId);
        if (nic == null){
            throw new InvalidParameterValueException("unable to find a nic with id " + nicId);
        }
        NetworkVO network = _networkDao.findById(nic.getNetworkId());
        if (network == null){
            throw new InvalidParameterValueException("unable to find a network with id " + nic.getNetworkId());
        }

        // Perform permission check on VM
        _accountMgr.checkAccess(caller, null, true, vmInstance);

        // Verify that zone is not Basic
        DataCenterVO dc = _dcDao.findById(vmInstance.getDataCenterId());
        if (dc.getNetworkType() == DataCenter.NetworkType.Basic) {
            throw new CloudRuntimeException("Zone " + vmInstance.getDataCenterId() + ", has a NetworkType of Basic. Can't change default NIC on a Basic Network");
        }

        // no need to check permissions for network, we'll enumerate the ones they already have access to
        Network existingdefaultnet = _networkModel.getDefaultNetworkForVm(vmId);

        //check to see if nic is attached to VM
        if (nic.getInstanceId() != vmId) {
            throw new InvalidParameterValueException(nic + " is not a nic on  " + vmInstance);
        }
        // if current default equals chosen new default, Throw an exception
        if (nic.isDefaultNic()){
            throw new CloudRuntimeException("refusing to set default nic because chosen nic is already the default");
        }

        //make sure the VM is Running or Stopped
        if ((vmInstance.getState() != State.Running) && (vmInstance.getState() != State.Stopped)) {
            throw new CloudRuntimeException("refusing to set default " + vmInstance + " is not Running or Stopped");
        }

        NicProfile existing = null;
        List<NicProfile> nicProfiles = _networkMgr.getNicProfiles(vmInstance);
        for (NicProfile nicProfile : nicProfiles) {
            if(nicProfile.isDefaultNic() && nicProfile.getNetworkId() == existingdefaultnet.getId()){
                existing = nicProfile;
                continue;
            }
        }

        if (existing == null){
            s_logger.warn("Failed to update default nic, no nic profile found for existing default network");
            throw new CloudRuntimeException("Failed to find a nic profile for the existing default network. This is bad and probably means some sort of configuration corruption");
        }

        NicVO existingVO = _nicDao.findById(existing.id);
        Integer chosenID = nic.getDeviceId();
        Integer existingID = existing.getDeviceId();

        nic.setDefaultNic(true);
        nic.setDeviceId(existingID);
        existingVO.setDefaultNic(false);
        existingVO.setDeviceId(chosenID);

        nic = _nicDao.persist(nic);
        existingVO = _nicDao.persist(existingVO);

        Network newdefault = null;
        newdefault = _networkModel.getDefaultNetworkForVm(vmId);

        if (newdefault == null){
            nic.setDefaultNic(false);
            nic.setDeviceId(chosenID);
            existingVO.setDefaultNic(true);
            existingVO.setDeviceId(existingID);

            nic = _nicDao.persist(nic);
            existingVO = _nicDao.persist(existingVO);

            newdefault = _networkModel.getDefaultNetworkForVm(vmId);
            if (newdefault.getId() == existingdefaultnet.getId()) {
                throw new CloudRuntimeException("Setting a default nic failed, and we had no default nic, but we were able to set it back to the original");
            }
            throw new CloudRuntimeException("Failed to change default nic to " + nic + " and now we have no default");
        } else if (newdefault.getId() == nic.getNetworkId()) {
            s_logger.debug("successfully set default network to " + network + " for " + vmInstance);
            return _vmDao.findById(vmInstance.getId());
        }

        throw new CloudRuntimeException("something strange happened, new default network(" + newdefault.getId() + ") is not null, and is not equal to the network(" + nic.getNetworkId() + ") of the chosen nic");
    }

    @Override
    public HashMap<Long, VmStatsEntry> getVirtualMachineStatistics(long hostId,
            String hostName, List<Long> vmIds) throws CloudRuntimeException {
        HashMap<Long, VmStatsEntry> vmStatsById = new HashMap<Long, VmStatsEntry>();

        if (vmIds.isEmpty()) {
            return vmStatsById;
        }

        List<String> vmNames = new ArrayList<String>();

        for (Long vmId : vmIds) {
            UserVmVO vm = _vmDao.findById(vmId);
            vmNames.add(vm.getInstanceName());
        }

        Answer answer = _agentMgr.easySend(hostId, new GetVmStatsCommand(
                vmNames, _hostDao.findById(hostId).getGuid(), hostName));
        if (answer == null || !answer.getResult()) {
            s_logger.warn("Unable to obtain VM statistics.");
            return null;
        } else {
            HashMap<String, VmStatsEntry> vmStatsByName = ((GetVmStatsAnswer) answer)
                    .getVmStatsMap();

            if (vmStatsByName == null) {
                s_logger.warn("Unable to obtain VM statistics.");
                return null;
            }

            for (String vmName : vmStatsByName.keySet()) {
                vmStatsById.put(vmIds.get(vmNames.indexOf(vmName)),
                        vmStatsByName.get(vmName));
            }
        }

        return vmStatsById;
    }

    @Override
    @DB
    public UserVm recoverVirtualMachine(RecoverVMCmd cmd)
            throws ResourceAllocationException, CloudRuntimeException {

        Long vmId = cmd.getId();
        Account caller = UserContext.current().getCaller();

        // Verify input parameters
        UserVmVO vm = _vmDao.findById(vmId.longValue());

        if (vm == null) {
            throw new InvalidParameterValueException(
                    "unable to find a virtual machine with id " + vmId);
        }

        // check permissions
        _accountMgr.checkAccess(caller, null, true, vm);

        if (vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is removed: " + vmId);
            }
            throw new InvalidParameterValueException("Unable to find vm by id "
                    + vmId);
        }

        if (vm.getState() != State.Destroyed) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("vm is not in the right state: " + vmId);
            }
            throw new InvalidParameterValueException("Vm with id " + vmId
                    + " is not in the right state");
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
            throw new CloudRuntimeException(
                    "Unable to recover VM as the account is deleted");
        }

        // Get serviceOffering for Virtual Machine
        ServiceOfferingVO serviceOffering = _serviceOfferingDao.findById(vm.getServiceOfferingId());

        // First check that the maximum number of UserVMs, CPU and Memory limit for the given
        // accountId will not be exceeded
        resourceLimitCheck(account, new Long(serviceOffering.getCpu()), new Long(serviceOffering.getRamSize()));

        _haMgr.cancelDestroy(vm, vm.getHostId());

        try {
            if (!_itMgr.stateTransitTo(vm,
                    VirtualMachine.Event.RecoveryRequested, null)) {
                s_logger.debug("Unable to recover the vm because it is not in the correct state: "
                        + vmId);
                throw new InvalidParameterValueException(
                        "Unable to recover the vm because it is not in the correct state: "
                                + vmId);
            }
        } catch (NoTransitionException e) {
            throw new InvalidParameterValueException(
                    "Unable to recover the vm because it is not in the correct state: "
                            + vmId);
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
                    DiskOfferingVO offering = _diskOfferingDao
                            .findById(diskOfferingId);
                    if (offering != null
                            && (offering.getType() == DiskOfferingVO.Type.Disk)) {
                        offeringId = offering.getId();
                    }
                }
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(),
                        volume.getDataCenterId(), volume.getId(), volume.getName(), offeringId, templateId,
                        volume.getSize(), Volume.class.getName(), volume.getUuid());
            }
        }

        //Update Resource Count for the given account
        _resourceLimitMgr.incrementResourceCount(account.getId(),
                ResourceType.volume, new Long(volumes.size()));
        resourceCountIncrement(account.getId(), new Long(serviceOffering.getCpu()),
                new Long(serviceOffering.getRamSize()));
        txn.commit();

        return _vmDao.findById(vmId);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        _name = name;

        if (_configDao == null) {
            throw new ConfigurationException(
                    "Unable to get the configuration dao.");
        }

        Map<String, String> configs = _configDao.getConfiguration(
                "AgentManager", params);

        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }

        String value = _configDao
                .getValue(Config.CreatePrivateTemplateFromVolumeWait.toString());
        _createprivatetemplatefromvolumewait = NumbersUtil.parseInt(value,
                Integer.parseInt(Config.CreatePrivateTemplateFromVolumeWait
                        .getDefaultValue()));

        value = _configDao
                .getValue(Config.CreatePrivateTemplateFromSnapshotWait
                        .toString());
        _createprivatetemplatefromsnapshotwait = NumbersUtil.parseInt(value,
                Integer.parseInt(Config.CreatePrivateTemplateFromSnapshotWait
                        .getDefaultValue()));

        String workers = configs.get("expunge.workers");
        int wrks = NumbersUtil.parseInt(workers, 10);

        String time = configs.get("expunge.interval");
        _expungeInterval = NumbersUtil.parseInt(time, 86400);
        time = configs.get("expunge.delay");
        _expungeDelay = NumbersUtil.parseInt(time, _expungeInterval);

        _executor = Executors.newScheduledThreadPool(wrks, new NamedThreadFactory("UserVm-Scavenger"));

        _itMgr.registerGuru(VirtualMachine.Type.User, this);

        VirtualMachine.State.getStateMachine().registerListener(
                new UserVmStateListener(_usageEventDao, _networkDao, _nicDao));

        value = _configDao.getValue(Config.SetVmInternalNameUsingDisplayName.key());
        if(value == null) {
            _instanceNameFlag = false;
        }
        else
        {
            _instanceNameFlag = Boolean.parseBoolean(value);
        }

        s_logger.info("User VM Manager is configured.");

        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        _executor.scheduleWithFixedDelay(new ExpungeTask(), _expungeInterval,
                _expungeInterval, TimeUnit.SECONDS);
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
            // expunge the vm
            if (!_itMgr.advanceExpunge(vm, _accountMgr.getSystemUser(), caller)) {
                s_logger.info("Did not expunge " + vm);
                return false;
            }

            // Only if vm is not expunged already, cleanup it's resources
            if (vm != null && vm.getRemoved() == null) {
                // Cleanup vm resources - all the PF/LB/StaticNat rules
                // associated with vm
                s_logger.debug("Starting cleaning up vm " + vm
                        + " resources...");
                if (cleanupVmResources(vm.getId())) {
                    s_logger.debug("Successfully cleaned up vm " + vm
                            + " resources as a part of expunge process");
                } else {
                    s_logger.warn("Failed to cleanup resources as a part of vm "
                            + vm + " expunge");
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
        // Remove vm from security groups
        _securityGroupMgr.removeInstanceFromGroups(vmId);

        // Remove vm from instance group
        removeInstanceFromInstanceGroup(vmId);

        // cleanup firewall rules
        if (_firewallMgr.revokeFirewallRulesForVm(vmId)) {
            s_logger.debug("Firewall rules are removed successfully as a part of vm id="
                    + vmId + " expunge");
        } else {
            success = false;
            s_logger.warn("Fail to remove firewall rules as a part of vm id="
                    + vmId + " expunge");
        }

        // cleanup port forwarding rules
        if (_rulesMgr.revokePortForwardingRulesForVm(vmId)) {
            s_logger.debug("Port forwarding rules are removed successfully as a part of vm id="
                    + vmId + " expunge");
        } else {
            success = false;
            s_logger.warn("Fail to remove port forwarding rules as a part of vm id="
                    + vmId + " expunge");
        }

        // cleanup load balancer rules
        if (_lbMgr.removeVmFromLoadBalancers(vmId)) {
            s_logger.debug("Removed vm id=" + vmId
                    + " from all load balancers as a part of expunge process");
        } else {
            success = false;
            s_logger.warn("Fail to remove vm id=" + vmId
                    + " from load balancers as a part of expunge process");
        }

        // If vm is assigned to static nat, disable static nat for the ip
        // address and disassociate ip if elasticIP is enabled
        IPAddressVO ip = _ipAddressDao.findByAssociatedVmId(vmId);
        try {
            if (ip != null) {
                if (_rulesMgr.disableStaticNat(ip.getId(),
                        _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM),
                        User.UID_SYSTEM, true)) {
                    s_logger.debug("Disabled 1-1 nat for ip address " + ip
                            + " as a part of vm id=" + vmId + " expunge");
                } else {
                    s_logger.warn("Failed to disable static nat for ip address "
                            + ip + " as a part of vm id=" + vmId + " expunge");
                    success = false;
                }
            }
        } catch (ResourceUnavailableException e) {
            success = false;
            s_logger.warn("Failed to disable static nat for ip address " + ip
                    + " as a part of vm id=" + vmId
                    + " expunge because resource is unavailable", e);
        }

        return success;
    }

    @Override
    public void deletePrivateTemplateRecord(Long templateId) {
        if (templateId != null) {
            _templateDao.remove(templateId);
        }
    }

    // used for vm transitioning to error state
    private void updateVmStateForFailedVmCreation(Long vmId, Long hostId) {

        UserVmVO vm = _vmDao.findById(vmId);

        if (vm != null) {
            if (vm.getState().equals(State.Stopped)) {
                s_logger.debug("Destroying vm " + vm + " as it failed to create on Host with Id:" + hostId);
                try {
                    _itMgr.stateTransitTo(vm,
                            VirtualMachine.Event.OperationFailedToError, null);
                } catch (NoTransitionException e1) {
                    s_logger.warn(e1.getMessage());
                }
                // destroy associated volumes for vm in error state
                // get all volumes in non destroyed state
                List<VolumeVO> volumesForThisVm = _volsDao
                        .findUsableVolumesForInstance(vm.getId());
                for (VolumeVO volume : volumesForThisVm) {
                    if (volume.getState() != Volume.State.Destroy) {
                        this.volumeMgr.destroyVolume(volume);
                    }
                }
                String msg = "Failed to deploy Vm with Id: " + vmId + ", on Host with Id: " + hostId;
                _alertMgr.sendAlert(AlertManager.ALERT_TYPE_USERVM, vm.getDataCenterId(), vm.getPodIdToDeployIn(), msg, msg);

                // Get serviceOffering for Virtual Machine
                ServiceOfferingVO offering = _serviceOfferingDao.findById(vm.getServiceOfferingId());

                // Update Resource Count for the given account
                resourceCountDecrement(vm.getAccountId(), new Long(offering.getCpu()),
                        new Long(offering.getRamSize()));
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
                        List<UserVmVO> vms = _vmDao.findDestroyedVms(new Date(
                                System.currentTimeMillis()
                                - ((long) _expungeDelay << 10)));
                        if (s_logger.isInfoEnabled()) {
                            if (vms.size() == 0) {
                                s_logger.trace("Found " + vms.size()
                                        + " vms to expunge.");
                            } else {
                                s_logger.info("Found " + vms.size()
                                        + " vms to expunge.");
                            }
                        }
                        for (UserVmVO vm : vms) {
                            try {
                                expunge(vm,
                                        _accountMgr.getSystemUser().getId(),
                                        _accountMgr.getSystemAccount());
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
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN)
                || (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN)
                || (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_UPDATE, eventDescription = "updating Vm")
    public UserVm updateVirtualMachine(UpdateVMCmd cmd)
            throws ResourceUnavailableException, InsufficientCapacityException {
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
            throw new InvalidParameterValueException(
                    "unable to find virtual machine with id " + id);
        }

        ServiceOffering offering = _serviceOfferingDao.findById(vmInstance
                .getServiceOfferingId());
        if (!offering.getOfferHA() && ha != null && ha) {
            throw new InvalidParameterValueException(
                    "Can't enable ha for the vm as it's created from the Service offering having HA disabled");
        }

        _accountMgr.checkAccess(UserContext.current().getCaller(), null, true,
                vmInstance);

        if (displayName == null) {
            displayName = vmInstance.getDisplayName();
        }

        if (ha == null) {
            ha = vmInstance.isHaEnabled();
        }

        UserVmVO vm = _vmDao.findById(id);
        if (vm == null) {
            throw new CloudRuntimeException(
                    "Unable to find virual machine with id " + id);
        }

        if (vm.getState() == State.Error || vm.getState() == State.Expunging) {
            s_logger.error("vm is not in the right state: " + id);
            throw new InvalidParameterValueException("Vm with id " + id
                    + " is not in the right state");
        }

        boolean updateUserdata = false;
        if (userData != null) {
            // check and replace newlines
            userData = userData.replace("\\n", "");
            validateUserData(userData);
            // update userData on domain router.
            updateUserdata = true;
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

        if (updateUserdata) {
            boolean result = updateUserDataInternal(_vmDao.findById(id));
            if (result) {
                s_logger.debug("User data successfully updated for vm id="+id);
            } else {
                throw new CloudRuntimeException("Failed to reset userdata for the virtual machine ");
            }
        }

        return _vmDao.findById(id);
    }

    private boolean updateUserDataInternal(UserVm vm)
            throws ResourceUnavailableException, InsufficientCapacityException {
        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(vm.getTemplateId());
        Nic defaultNic = _networkModel.getDefaultNic(vm.getId());
        if (defaultNic == null) {
            s_logger.error("Unable to update userdata for vm id=" + vm.getId() + " as the instance doesn't have default nic");
            return false;
        }

        Network defaultNetwork = _networkDao.findById(defaultNic.getNetworkId());
        NicProfile defaultNicProfile = new NicProfile(defaultNic, defaultNetwork, null, null, null,
                _networkModel.isSecurityGroupSupportedInNetwork(defaultNetwork),
                _networkModel.getNetworkTag(template.getHypervisorType(), defaultNetwork));

        VirtualMachineProfile<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>((VMInstanceVO)vm);

        UserDataServiceProvider element = _networkModel.getUserDataUpdateProvider(defaultNetwork);
        if (element == null) {
            throw new CloudRuntimeException("Can't find network element for " + Service.UserData.getName() + " provider needed for UserData update");
        }
        boolean result = element.saveUserData(defaultNetwork, defaultNicProfile, vmProfile);

        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_START, eventDescription = "starting Vm", async = true)
    public UserVm startVirtualMachine(StartVMCmd cmd)
            throws ExecutionException, ConcurrentOperationException,
            ResourceUnavailableException, InsufficientCapacityException {
        return startVirtualMachine(cmd.getId(), cmd.getHostId(), null).first();
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_REBOOT, eventDescription = "rebooting Vm", async = true)
    public UserVm rebootVirtualMachine(RebootVMCmd cmd)
            throws InsufficientCapacityException, ResourceUnavailableException {
        Account caller = UserContext.current().getCaller();
        Long vmId = cmd.getId();

        // Verify input parameters
        UserVmVO vmInstance = _vmDao.findById(vmId.longValue());
        if (vmInstance == null) {
            throw new InvalidParameterValueException(
                    "unable to find a virtual machine with id " + vmId);
        }

        _accountMgr.checkAccess(caller, null, true, vmInstance);

        // If the VM is Volatile in nature, on reboot discard the VM's root disk and create a new root disk for it: by calling restoreVM
        long serviceOfferingId = vmInstance.getServiceOfferingId();
        ServiceOfferingVO offering = _serviceOfferingDao.findById(serviceOfferingId);
        if(offering != null && offering.getRemoved() == null) {
            if(offering.getVolatileVm()){
                return restoreVMInternal(caller, vmInstance, null);
            }
        } else {
            throw new InvalidParameterValueException("Unable to find service offering: " + serviceOfferingId + " corresponding to the vm");
        }

        return rebootVirtualMachine(UserContext.current().getCallerUserId(),
                vmId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_DESTROY, eventDescription = "destroying Vm", async = true)
    public UserVm destroyVm(DestroyVMCmd cmd)
            throws ResourceUnavailableException, ConcurrentOperationException {
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

        Account owner = _accountMgr.finalizeOwner(caller, accountName,
                domainId, projectId);
        long accountId = owner.getId();

        // Check if name is already in use by this account
        boolean isNameInUse = _vmGroupDao.isNameInUse(accountId, groupName);

        if (isNameInUse) {
            throw new InvalidParameterValueException(
                    "Unable to create vm group, a group with name " + groupName
                    + " already exisits for account " + accountId);
        }

        return createVmGroup(groupName, accountId);
    }

    @DB
    protected InstanceGroupVO createVmGroup(String groupName, long accountId) {
        Account account = null;
        final Transaction txn = Transaction.currentTxn();
        txn.start();
        try {
            account = _accountDao.acquireInLockTable(accountId); // to ensure
            // duplicate
            // vm group
            // names are
            // not
            // created.
            if (account == null) {
                s_logger.warn("Failed to acquire lock on account");
                return null;
            }
            InstanceGroupVO group = _vmGroupDao.findByAccountAndName(accountId,
                    groupName);
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
            throw new InvalidParameterValueException(
                    "unable to find a vm group with id " + groupId);
        }

        _accountMgr.checkAccess(caller, null, true, group);

        return deleteVmGroup(groupId);
    }

    @Override
    public boolean deleteVmGroup(long groupId) {
        // delete all the mappings from group_vm_map table
        List<InstanceGroupVMMapVO> groupVmMaps = _groupVMMapDao
                .listByGroupId(groupId);
        for (InstanceGroupVMMapVO groupMap : groupVmMaps) {
            SearchCriteria<InstanceGroupVMMapVO> sc = _groupVMMapDao
                    .createSearchCriteria();
            sc.addAnd("instanceId", SearchCriteria.Op.EQ,
                    groupMap.getInstanceId());
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

        InstanceGroupVO group = _vmGroupDao.findByAccountAndName(
                vm.getAccountId(), groupName);
        // Create vm group if the group doesn't exist for this account
        if (group == null) {
            group = createVmGroup(groupName, vm.getAccountId());
        }

        if (group != null) {
            final Transaction txn = Transaction.currentTxn();
            txn.start();
            UserVm userVm = _vmDao.acquireInLockTable(userVmId);
            if (userVm == null) {
                s_logger.warn("Failed to acquire lock on user vm id="
                        + userVmId);
            }
            try {
                // don't let the group be deleted when we are assigning vm to
                // it.
                InstanceGroupVO ngrpLock = _vmGroupDao.lockRow(group.getId(),
                        false);
                if (ngrpLock == null) {
                    s_logger.warn("Failed to acquire lock on vm group id="
                            + group.getId() + " name=" + group.getName());
                    txn.rollback();
                    return false;
                }

                // Currently don't allow to assign a vm to more than one group
                if (_groupVMMapDao.listByInstanceId(userVmId) != null) {
                    // Delete all mappings from group_vm_map table
                    List<InstanceGroupVMMapVO> groupVmMaps = _groupVMMapDao
                            .listByInstanceId(userVmId);
                    for (InstanceGroupVMMapVO groupMap : groupVmMaps) {
                        SearchCriteria<InstanceGroupVMMapVO> sc = _groupVMMapDao
                                .createSearchCriteria();
                        sc.addAnd("instanceId", SearchCriteria.Op.EQ,
                                groupMap.getInstanceId());
                        _groupVMMapDao.expunge(sc);
                    }
                }
                InstanceGroupVMMapVO groupVmMapVO = new InstanceGroupVMMapVO(
                        group.getId(), userVmId);
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
        // TODO - in future releases vm can be assigned to multiple groups; but
        // currently return just one group per vm
        try {
            List<InstanceGroupVMMapVO> groupsToVmMap = _groupVMMapDao
                    .listByInstanceId(vmId);

            if (groupsToVmMap != null && groupsToVmMap.size() != 0) {
                InstanceGroupVO group = _vmGroupDao.findById(groupsToVmMap.get(
                        0).getGroupId());
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
            List<InstanceGroupVMMapVO> groupVmMaps = _groupVMMapDao
                    .listByInstanceId(vmId);
            for (InstanceGroupVMMapVO groupMap : groupVmMaps) {
                SearchCriteria<InstanceGroupVMMapVO> sc = _groupVMMapDao
                        .createSearchCriteria();
                sc.addAnd("instanceId", SearchCriteria.Op.EQ,
                        groupMap.getInstanceId());
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
            String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, String userData, String sshKeyPair, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, String keyboard)
                    throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, StorageUnavailableException, ResourceAllocationException {

        Account caller = UserContext.current().getCaller();
        List<NetworkVO> networkList = new ArrayList<NetworkVO>();

        // Verify that caller can perform actions in behalf of vm owner
        _accountMgr.checkAccess(caller, null, true, owner);

        // Get default guest network in Basic zone
        Network defaultNetwork = _networkModel.getExclusiveGuestNetwork(zone.getId());

        if (defaultNetwork == null) {
            throw new InvalidParameterValueException(
                    "Unable to find a default network to start a vm");
        } else {
            networkList.add(_networkDao.findById(defaultNetwork.getId()));
        }

        boolean isVmWare = (template.getHypervisorType() == HypervisorType.VMware || (hypervisor != null && hypervisor == HypervisorType.VMware));

        if (securityGroupIdList != null && isVmWare) {
            throw new InvalidParameterValueException("Security group feature is not supported for vmWare hypervisor");
        } else if (!isVmWare && _networkModel.isSecurityGroupSupportedInNetwork(defaultNetwork) && _networkModel.canAddDefaultSecurityGroup()) {
            //add the default securityGroup only if no security group is specified
            if (securityGroupIdList == null || securityGroupIdList.isEmpty()) {
                if (securityGroupIdList == null) {
                    securityGroupIdList = new ArrayList<Long>();
                }
                SecurityGroup defaultGroup = _securityGroupMgr
                        .getDefaultSecurityGroup(owner.getId());
                if (defaultGroup != null) {
                    securityGroupIdList.add(defaultGroup.getId());
                } else {
                    // create default security group for the account
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Couldn't find default security group for the account "
                                + owner + " so creating a new one");
                    }
                    defaultGroup = _securityGroupMgr.createSecurityGroup(
                            SecurityGroupManager.DEFAULT_GROUP_NAME,
                            SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION,
                            owner.getDomainId(), owner.getId(),
                            owner.getAccountName());
                    securityGroupIdList.add(defaultGroup.getId());
                }
            }
        }

        return createVirtualMachine(zone, serviceOffering, template, hostName, displayName, owner, diskOfferingId,
                diskSize, networkList, securityGroupIdList, group, userData, sshKeyPair, hypervisor, caller, requestedIps, defaultIps, keyboard);
    }

    @Override
    public UserVm createAdvancedSecurityGroupVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList,
            List<Long> securityGroupIdList, Account owner, String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, String userData,
            String sshKeyPair, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, String keyboard) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, StorageUnavailableException,
            ResourceAllocationException {

        Account caller = UserContext.current().getCaller();
        List<NetworkVO> networkList = new ArrayList<NetworkVO>();
        boolean isSecurityGroupEnabledNetworkUsed = false;
        boolean isVmWare = (template.getHypervisorType() == HypervisorType.VMware || (hypervisor != null && hypervisor == HypervisorType.VMware));

        // Verify that caller can perform actions in behalf of vm owner
        _accountMgr.checkAccess(caller, null, true, owner);

        // If no network is specified, find system security group enabled network
        if (networkIdList == null || networkIdList.isEmpty()) {
            Network networkWithSecurityGroup = _networkModel.getNetworkWithSecurityGroupEnabled(zone.getId());
            if (networkWithSecurityGroup == null) {
                throw new InvalidParameterValueException("No network with security enabled is found in zone id=" + zone.getId());
            }

            networkList.add(_networkDao.findById(networkWithSecurityGroup.getId()));
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
                throw new InvalidParameterValueException(
                        "Unable to find network by id "
                                + networkIdList.get(0).longValue());
            }

            if (!_networkModel.isSecurityGroupSupportedInNetwork(network)) {
                throw new InvalidParameterValueException("Network is not security group enabled: " + network.getId());
            }

            networkList.add(network);
            isSecurityGroupEnabledNetworkUsed = true;

        } else {
            // Verify that all the networks are Shared/Guest; can't create combination of SG enabled and disabled networks 
            for (Long networkId : networkIdList) {
                NetworkVO network = _networkDao.findById(networkId);

                if (network == null) {
                    throw new InvalidParameterValueException("Unable to find network by id " + networkIdList.get(0).longValue());
                }

                boolean isSecurityGroupEnabled = _networkModel.isSecurityGroupSupportedInNetwork(network);
                if (isSecurityGroupEnabled) {
                    if (networkIdList.size() > 1) {
                        throw new InvalidParameterValueException("Can't create a vm with multiple networks one of" +
                                " which is Security Group enabled");
                    }

                    isSecurityGroupEnabledNetworkUsed = true;
                }            

                if (!(network.getTrafficType() == TrafficType.Guest && network.getGuestType() == Network.GuestType.Shared)) {
                    throw new InvalidParameterValueException("Can specify only Shared Guest networks when" +
                            " deploy vm in Advance Security Group enabled zone");
                }

                // Perform account permission check
                if (network.getAclType() == ACLType.Account) {
                    _accountMgr.checkAccess(caller, AccessType.UseNetwork, false, network);
                }
                networkList.add(network);
            }
        }

        // if network is security group enabled, and no security group is specified, then add the default security group automatically
        if (isSecurityGroupEnabledNetworkUsed && !isVmWare && _networkModel.canAddDefaultSecurityGroup()) {

            //add the default securityGroup only if no security group is specified
            if(securityGroupIdList == null || securityGroupIdList.isEmpty()){
                if (securityGroupIdList == null) {
                    securityGroupIdList = new ArrayList<Long>();
                }

                SecurityGroup defaultGroup = _securityGroupMgr
                        .getDefaultSecurityGroup(owner.getId());
                if (defaultGroup != null) {
                    securityGroupIdList.add(defaultGroup.getId());
                } else {
                    // create default security group for the account
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Couldn't find default security group for the account "
                                + owner + " so creating a new one");
                    }
                    defaultGroup = _securityGroupMgr.createSecurityGroup(
                            SecurityGroupManager.DEFAULT_GROUP_NAME,
                            SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION,
                            owner.getDomainId(), owner.getId(),
                            owner.getAccountName());
                    securityGroupIdList.add(defaultGroup.getId());
                }
            }
        }

        return createVirtualMachine(zone, serviceOffering, template, hostName, displayName, owner, diskOfferingId,
                diskSize, networkList, securityGroupIdList, group, userData, sshKeyPair, hypervisor, caller, requestedIps, defaultIps, keyboard);
    }

    @Override
    public UserVm createAdvancedVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList, Account owner, String hostName,
            String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, String userData, String sshKeyPair, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, String keyboard)
                    throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, StorageUnavailableException, ResourceAllocationException {

        Account caller = UserContext.current().getCaller();
        List<NetworkVO> networkList = new ArrayList<NetworkVO>();

        // Verify that caller can perform actions in behalf of vm owner
        _accountMgr.checkAccess(caller, null, true, owner);

        List<HypervisorType> vpcSupportedHTypes = _vpcMgr
                .getSupportedVpcHypervisors();
        if (networkIdList == null || networkIdList.isEmpty()) {
            NetworkVO defaultNetwork = null;

            // if no network is passed in
            // Check if default virtual network offering has
            // Availability=Required. If it's true, search for corresponding
            // network
            // * if network is found, use it. If more than 1 virtual network is
            // found, throw an error
            // * if network is not found, create a new one and use it

            List<NetworkOfferingVO> requiredOfferings = _networkOfferingDao
                    .listByAvailability(Availability.Required, false);
            if (requiredOfferings.size() < 1) {
                throw new InvalidParameterValueException(
                        "Unable to find network offering with availability="
                                + Availability.Required
                                + " to automatically create the network as a part of vm creation");
            }

            if (requiredOfferings.get(0).getState() == NetworkOffering.State.Enabled) {
                // get Virtual networks
                List<? extends Network> virtualNetworks = _networkModel.listNetworksForAccount(owner.getId(), zone.getId(), Network.GuestType.Isolated);
                if (virtualNetworks.isEmpty()) {
                    long physicalNetworkId = _networkModel.findPhysicalNetworkId(zone.getId(), requiredOfferings.get(0).getTags(), requiredOfferings.get(0).getTrafficType());
                    // Validate physical network
                    PhysicalNetwork physicalNetwork = _physicalNetworkDao
                            .findById(physicalNetworkId);
                    if (physicalNetwork == null) {
                        throw new InvalidParameterValueException("Unable to find physical network with id: "+physicalNetworkId   + " and tag: " +requiredOfferings.get(0).getTags());
                    }
                    s_logger.debug("Creating network for account " + owner + " from the network offering id=" +requiredOfferings.get(0).getId() + " as a part of deployVM process");
                    Network newNetwork = _networkMgr.createGuestNetwork(requiredOfferings.get(0).getId(),
                            owner.getAccountName() + "-network", owner.getAccountName() + "-network", null, null,
                            null, null, owner, null, physicalNetwork, zone.getId(), ACLType.Account, null, null, null, null);
                    defaultNetwork = _networkDao.findById(newNetwork.getId());
                } else if (virtualNetworks.size() > 1) {
                    throw new InvalidParameterValueException(
                            "More than 1 default Isolated networks are found for account "
                                    + owner + "; please specify networkIds");
                } else {
                    defaultNetwork = _networkDao.findById(virtualNetworks.get(0).getId());
                }
            } else {
                throw new InvalidParameterValueException(
                        "Required network offering id="
                                + requiredOfferings.get(0).getId()
                                + " is not in " + NetworkOffering.State.Enabled);
            }

            networkList.add(defaultNetwork);

        } else {
            for (Long networkId : networkIdList) {
                NetworkVO network = _networkDao.findById(networkId);
                if (network == null) {
                    throw new InvalidParameterValueException(
                            "Unable to find network by id "
                                    + networkIdList.get(0).longValue());
                }
                if (network.getVpcId() != null) {
                    // Only ISOs, XenServer, KVM, and VmWare template types are
                    // supported for vpc networks
                    if (template.getFormat() != ImageFormat.ISO
                            && !vpcSupportedHTypes.contains(template
                                    .getHypervisorType())) {
                        throw new InvalidParameterValueException(
                                "Can't create vm from template with hypervisor "
                                        + template.getHypervisorType()
                                        + " in vpc network " + network);
                    }

                    // Only XenServer, KVM, and VMware hypervisors are supported
                    // for vpc networks
                    if (!vpcSupportedHTypes.contains(hypervisor)) {
                        throw new InvalidParameterValueException(
                                "Can't create vm of hypervisor type "
                                        + hypervisor + " in vpc network");
                    }

                }

                _networkModel.checkNetworkPermissions(owner, network);

                // don't allow to use system networks
                NetworkOffering networkOffering = _configMgr
                        .getNetworkOffering(network.getNetworkOfferingId());
                if (networkOffering.isSystemOnly()) {
                    throw new InvalidParameterValueException(
                            "Network id="
                                    + networkId
                                    + " is system only and can't be used for vm deployment");
                }
                networkList.add(network);
            }
        }

        return createVirtualMachine(zone, serviceOffering, template, hostName, displayName, owner, diskOfferingId, diskSize, networkList, null, group, userData, sshKeyPair, hypervisor, caller, requestedIps, defaultIps, keyboard);
    }


    public void checkNameForRFCCompliance(String name) {
        if (!NetUtils.verifyDomainNameLabel(name, true)) {
            throw new InvalidParameterValueException("Invalid name. Vm name can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                    + "and the hyphen ('-'), must be between 1 and 63 characters long, and can't start or end with \"-\" and can't start with digit");
        }
    }

    @DB @ActionEvent(eventType = EventTypes.EVENT_VM_CREATE, eventDescription = "deploying Vm", create = true)
    protected UserVm createVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, String hostName, String displayName, Account owner, Long diskOfferingId,
            Long diskSize, List<NetworkVO> networkList, List<Long> securityGroupIdList, String group, String userData, String sshKeyPair, HypervisorType hypervisor, Account caller, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, String keyboard)
                    throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException, StorageUnavailableException, ResourceAllocationException {

        _accountMgr.checkAccess(caller, null, true, owner);

        if (owner.getState() == Account.State.disabled) {
            throw new PermissionDeniedException(
                    "The owner of vm to deploy is disabled: " + owner);
        }

        long accountId = owner.getId();

        assert !(requestedIps != null && (defaultIps.getIp4Address() != null || defaultIps.getIp6Address() != null)) : "requestedIp list and defaultNetworkIp should never be specified together";

        if (Grouping.AllocationState.Disabled == zone.getAllocationState()
                && !_accountMgr.isRootAdmin(caller.getType())) {
            throw new PermissionDeniedException(
                    "Cannot perform this operation, Zone is currently disabled: "
                            + zone.getId());
        }

        if (zone.getDomainId() != null) {
            DomainVO domain = _domainDao.findById(zone.getDomainId());
            if (domain == null) {
                throw new CloudRuntimeException("Unable to find the domain "
                        + zone.getDomainId() + " for the zone: " + zone);
            }
            // check that caller can operate with domain
            _configMgr.checkZoneAccess(caller, zone);
            // check that vm owner can create vm in the domain
            _configMgr.checkZoneAccess(owner, zone);
        }

        ServiceOfferingVO offering = _serviceOfferingDao.findById(serviceOffering.getId());

        // check if account/domain is with in resource limits to create a new vm
        boolean isIso = Storage.ImageFormat.ISO == template.getFormat();
        resourceLimitCheck(owner, new Long(offering.getCpu()), new Long(offering.getRamSize()));
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.volume, (isIso
                || diskOfferingId == null ? 1 : 2));

        // verify security group ids
        if (securityGroupIdList != null) {
            for (Long securityGroupId : securityGroupIdList) {
                SecurityGroup sg = _securityGroupDao.findById(securityGroupId);
                if (sg == null) {
                    throw new InvalidParameterValueException(
                            "Unable to find security group by id "
                                    + securityGroupId);
                } else {
                    // verify permissions
                    _accountMgr.checkAccess(caller, null, true, owner, sg);
                }
            }
        }

        if (template.getHypervisorType() != null && template.getHypervisorType() != HypervisorType.BareMetal) {
            // check if we have available pools for vm deployment
            long availablePools = _storagePoolDao.countPoolsByStatus(StoragePoolStatus.Up);
            if (availablePools < 1) {
                throw new StorageUnavailableException("There are no available pools in the UP state for vm deployment", -1);
            }
        }

        if (template.getTemplateType().equals(TemplateType.SYSTEM)) {
            throw new InvalidParameterValueException(
                    "Unable to use system template " + template.getId()
                    + " to deploy a user vm");
        }
        List<VMTemplateZoneVO> listZoneTemplate = _templateZoneDao
                .listByZoneTemplate(zone.getId(), template.getId());
        if (listZoneTemplate == null || listZoneTemplate.isEmpty()) {
            throw new InvalidParameterValueException("The template "
                    + template.getId() + " is not available for use");
        }

        if (isIso && !template.isBootable()) {
            throw new InvalidParameterValueException(
                    "Installing from ISO requires an ISO that is bootable: "
                            + template.getId());
        }

        // Check templates permissions
        if (!template.isPublicTemplate()) {
            Account templateOwner = _accountMgr.getAccount(template
                    .getAccountId());
            _accountMgr.checkAccess(owner, null, true, templateOwner);
        }

        // check if the user data is correct
        validateUserData(userData);

        // Find an SSH public key corresponding to the key pair name, if one is
        // given
        String sshPublicKey = null;
        if (sshKeyPair != null && !sshKeyPair.equals("")) {
            SSHKeyPair pair = _sshKeyPairDao.findByName(owner.getAccountId(),
                    owner.getDomainId(), sshKeyPair);
            if (pair == null) {
                throw new InvalidParameterValueException(
                        "A key pair with name '" + sshKeyPair
                        + "' was not found.");
            }

            sshPublicKey = pair.getPublicKey();
        }

        List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>();

        Map<String, NicProfile> networkNicMap = new HashMap<String, NicProfile>();

        short defaultNetworkNumber = 0;
        boolean securityGroupEnabled = false;
        boolean vpcNetwork = false;
        for (NetworkVO network : networkList) {
            if (network.getDataCenterId() != zone.getId()) {
                throw new InvalidParameterValueException("Network id="
                        + network.getId() + " doesn't belong to zone "
                        + zone.getId());
            }

            IpAddresses requestedIpPair = null;
            if (requestedIps != null && !requestedIps.isEmpty()) {
                requestedIpPair = requestedIps.get(network.getId());
            }

            if (requestedIpPair == null) {
                requestedIpPair = new IpAddresses(null, null);
            } else {
                _networkModel.checkRequestedIpAddresses(network.getId(), requestedIpPair.getIp4Address(), requestedIpPair.getIp6Address());
            }

            NicProfile profile = new NicProfile(requestedIpPair.getIp4Address(), requestedIpPair.getIp6Address());

            if (defaultNetworkNumber == 0) {
                defaultNetworkNumber++;
                // if user requested specific ip for default network, add it
                if (defaultIps.getIp4Address() != null || defaultIps.getIp6Address() != null) {
                    _networkModel.checkRequestedIpAddresses(network.getId(), defaultIps.getIp4Address(), defaultIps.getIp6Address());
                    profile = new NicProfile(defaultIps.getIp4Address(), defaultIps.getIp6Address());
                }

                profile.setDefaultNic(true);
            }

            networks.add(new Pair<NetworkVO, NicProfile>(network, profile));

            if (_networkModel.isSecurityGroupSupportedInNetwork(network)) {
                securityGroupEnabled = true;
            }

            // vm can't be a part of more than 1 VPC network
            if (network.getVpcId() != null) {
                if (vpcNetwork) {
                    throw new InvalidParameterValueException(
                            "Vm can't be a part of more than 1 VPC network");
                }
                vpcNetwork = true;
            }

            networkNicMap.put(network.getUuid(), profile);
        }

        if (securityGroupIdList != null && !securityGroupIdList.isEmpty()
                && !securityGroupEnabled) {
            throw new InvalidParameterValueException(
                    "Unable to deploy vm with security groups as SecurityGroup service is not enabled for the vm's network");
        }

        // Verify network information - network default network has to be set;
        // and vm can't have more than one default network
        // This is a part of business logic because default network is required
        // by Agent Manager in order to configure default
        // gateway for the vm
        if (defaultNetworkNumber == 0) {
            throw new InvalidParameterValueException(
                    "At least 1 default network has to be specified for the vm");
        } else if (defaultNetworkNumber > 1) {
            throw new InvalidParameterValueException(
                    "Only 1 default network per vm is supported");
        }

        long id = _vmDao.getNextInSequence(Long.class, "id");

        String instanceName;
        if (_instanceNameFlag && displayName != null) {
            // Check if the displayName conforms to RFC standards.
            checkNameForRFCCompliance(displayName);
            instanceName = VirtualMachineName.getVmName(id, owner.getId(), displayName);
            if (instanceName.length() > MAX_VM_NAME_LEN) {
                throw new InvalidParameterValueException("Specified display name " + displayName + " causes VM name to exceed 80 characters in length");
            }
            // Search whether there is already an instance with the same instance name
            // that is not in the destroyed or expunging state.
            VMInstanceVO vm = _vmInstanceDao.findVMByInstanceName(instanceName);
            if (vm != null && vm.getState() != VirtualMachine.State.Expunging) {
                throw new InvalidParameterValueException("There already exists a VM by the display name supplied");
            }
        } else {
            instanceName = VirtualMachineName.getVmName(id, owner.getId(), _instance);
        }

        String uuidName = UUID.randomUUID().toString();

        // verify hostname information
        if (hostName == null) {
            hostName = uuidName;
        } else {
            //1) check is hostName is RFC compliant
            checkNameForRFCCompliance(hostName);
            // 2) hostName has to be unique in the network domain
            Map<String, List<Long>> ntwkDomains = new HashMap<String, List<Long>>();
            for (NetworkVO network : networkList) {
                String ntwkDomain = network.getNetworkDomain();
                if (!ntwkDomains.containsKey(ntwkDomain)) {
                    List<Long> ntwkIds = new ArrayList<Long>();
                    ntwkIds.add(network.getId());
                    ntwkDomains.put(ntwkDomain, ntwkIds);
                } else {
                    List<Long> ntwkIds = ntwkDomains.get(ntwkDomain);
                    ntwkIds.add(network.getId());
                    ntwkDomains.put(ntwkDomain, ntwkIds);
                }
            }

            for (String ntwkDomain : ntwkDomains.keySet()) {
                for (Long ntwkId : ntwkDomains.get(ntwkDomain)) {
                    // * get all vms hostNames in the network
                    List<String> hostNames = _vmInstanceDao
                            .listDistinctHostNames(ntwkId);
                    // * verify that there are no duplicates
                    if (hostNames.contains(hostName)) {
                        throw new InvalidParameterValueException("The vm with hostName " + hostName
                                + " already exists in the network domain: " + ntwkDomain + "; network=" 
                                + _networkModel.getNetwork(ntwkId));
                    }
                }
            }
        }

        HypervisorType hypervisorType = null;
        if (template == null || template.getHypervisorType() == null
                || template.getHypervisorType() == HypervisorType.None) {
            hypervisorType = hypervisor;
        } else {
            hypervisorType = template.getHypervisorType();
        }
        Transaction txn = Transaction.currentTxn();
        txn.start();
        UserVmVO vm = new UserVmVO(id, instanceName, displayName,
                template.getId(), hypervisorType, template.getGuestOSId(),
                offering.getOfferHA(), offering.getLimitCpuUse(),
                owner.getDomainId(), owner.getId(), offering.getId(), userData,
                hostName, diskOfferingId);
        vm.setUuid(uuidName);

        if (sshPublicKey != null) {
            vm.setDetail("SSH.PublicKey", sshPublicKey);
        }

        if (keyboard != null && !keyboard.isEmpty())
            vm.setDetail(VmDetailConstants.KEYBOARD, keyboard);

        if (isIso) {
            vm.setIsoId(template.getId());
        }

        // If hypervisor is vSphere, check for clone type setting.
        if (hypervisorType.equals(HypervisorType.VMware)) {
            // retrieve clone flag.
            UserVmCloneType cloneType = UserVmCloneType.linked;
            String value = _configDao.getValue(Config.VmwareCreateFullClone.key());
            if (value != null) {
                if (Boolean.parseBoolean(value) == true)
                    cloneType = UserVmCloneType.full;
            }
            UserVmCloneSettingVO vmCloneSettingVO = new UserVmCloneSettingVO(id, cloneType.toString());
            _vmCloneSettingDao.persist(vmCloneSettingVO);
        }


        _vmDao.persist(vm);
        _vmDao.saveDetails(vm);

        s_logger.debug("Allocating in the DB for vm");
        DataCenterDeployment plan = new DataCenterDeployment(zone.getId());


        long guestOSId = template.getGuestOSId();
        GuestOSVO guestOS = _guestOSDao.findById(guestOSId);
        long guestOSCategoryId = guestOS.getCategoryId();
        GuestOSCategoryVO guestOSCategory = _guestOSCategoryDao.findById(guestOSCategoryId);

        List<String> computeTags = new ArrayList<String>();
        computeTags.add(offering.getHostTag());

        List<String> rootDiskTags =	new ArrayList<String>();    	
        rootDiskTags.add(offering.getTags());

        if(isIso){
            VirtualMachineEntity vmEntity = _orchSrvc.createVirtualMachineFromScratch(vm.getUuid(), new Long(owner.getAccountId()).toString(), vm.getIsoId().toString(), hostName, displayName, hypervisor.name(), guestOSCategory.getName(), offering.getCpu(), offering.getSpeed(), offering.getRamSize(), diskSize,  computeTags, rootDiskTags, networkNicMap, plan);
        }else {
            VirtualMachineEntity vmEntity = _orchSrvc.createVirtualMachine(vm.getUuid(), new Long(owner.getAccountId()).toString(), new Long(template.getId()).toString(), hostName, displayName, hypervisor.name(), offering.getCpu(),  offering.getSpeed(), offering.getRamSize(), diskSize, computeTags, rootDiskTags, networkNicMap, plan);
        }



        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully allocated DB entry for " + vm);
        }
        UserContext.current().setEventDetails("Vm Id: " + vm.getId());

        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_CREATE, accountId, zone.getId(), vm.getId(),
                vm.getHostName(), offering.getId(), template.getId(), hypervisorType.toString(),
                VirtualMachine.class.getName(), vm.getUuid());

        //Update Resource Count for the given account
        resourceCountIncrement(accountId, new Long(offering.getCpu()),
                new Long(offering.getRamSize()));

        txn.commit();

        // Assign instance to the group
        try {
            if (group != null) {
                boolean addToGroup = addInstanceToGroup(Long.valueOf(id), group);
                if (!addToGroup) {
                    throw new CloudRuntimeException(
                            "Unable to assign Vm to the group " + group);
                }
            }
        } catch (Exception ex) {
            throw new CloudRuntimeException("Unable to assign Vm to the group "
                    + group);
        }

        _securityGroupMgr.addInstanceToGroups(vm.getId(), securityGroupIdList);

        return vm;
    }

    private void validateUserData(String userData) {
        byte[] decodedUserData = null;
        if (userData != null) {
            if (!Base64.isBase64(userData)) {
                throw new InvalidParameterValueException(
                        "User data is not base64 encoded");
            }
            if (userData.length() >= 2 * MAX_USER_DATA_LENGTH_BYTES) {
                throw new InvalidParameterValueException(
                        "User data is too long");
            }
            decodedUserData = Base64.decodeBase64(userData.getBytes());
            if (decodedUserData.length > MAX_USER_DATA_LENGTH_BYTES) {
                throw new InvalidParameterValueException(
                        "User data is too long");
            }
            if (decodedUserData.length < 1) {
                throw new InvalidParameterValueException(
                        "User data is too short");
            }
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_CREATE, eventDescription = "starting Vm", async = true)
    public UserVm startVirtualMachine(DeployVMCmd cmd)
            throws ResourceUnavailableException, InsufficientCapacityException,
            ConcurrentOperationException {
        return startVirtualMachine(cmd, null);
    }

    protected UserVm startVirtualMachine(DeployVMCmd cmd,
            Map<VirtualMachineProfile.Param, Object> additonalParams)
                    throws ResourceUnavailableException, InsufficientCapacityException,
                    ConcurrentOperationException {

        long vmId = cmd.getEntityId();
        Long hostId = cmd.getHostId();
        UserVmVO vm = _vmDao.findById(vmId);

        Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> vmParamPair = null;
        try {
            vmParamPair = startVirtualMachine(vmId, hostId, additonalParams);
            vm = vmParamPair.first();
        } finally {
            updateVmStateForFailedVmCreation(vm.getId(), hostId);
        }

        // Check that the password was passed in and is valid
        VMTemplateVO template = _templateDao.findByIdIncludingRemoved(vm
                .getTemplateId());
        if (template.getEnablePassword()) {
            // this value is not being sent to the backend; need only for api
            // display purposes
            vm.setPassword((String) vmParamPair.second().get(
                    VirtualMachineProfile.Param.VmPassword));
        }

        return vm;
    }

    @Override
    public boolean finalizeVirtualMachineProfile(
            VirtualMachineProfile<UserVmVO> profile, DeployDestination dest,
            ReservationContext context) {
        UserVmVO vm = profile.getVirtualMachine();
        Map<String, String> details = _vmDetailsDao.findDetails(vm.getId());
        vm.setDetails(details);

        if (vm.getIsoId() != null) {
            String isoPath = null;

            VirtualMachineTemplate template = _templateDao.findById(vm
                    .getIsoId());
            if (template == null || template.getFormat() != ImageFormat.ISO) {
                throw new CloudRuntimeException(
                        "Can not find ISO in vm_template table for id "
                                + vm.getIsoId());
            }

            Pair<String, String> isoPathPair = this.templateMgr.getAbsoluteIsoPath(
                    template.getId(), vm.getDataCenterId());

            if (template.getTemplateType() == TemplateType.PERHOST) {
                isoPath = template.getName();
            } else {
                if (isoPathPair == null) {
                    s_logger.warn("Couldn't get absolute iso path");
                    return false;
                } else {
                    isoPath = isoPathPair.first();
                }
            }

            if (template.isBootable()) {
                profile.setBootLoaderType(BootloaderType.CD);
            }
            GuestOSVO guestOS = _guestOSDao.findById(template.getGuestOSId());
            String displayName = null;
            if (guestOS != null) {
                displayName = guestOS.getDisplayName();
            }
            VolumeTO iso = new VolumeTO(profile.getId(), Volume.Type.ISO,
                    StoragePoolType.ISO, null, template.getName(), null,
                    isoPath, 0, null, displayName);

            iso.setDeviceId(3);
            profile.addDisk(iso);
        } else {
            VirtualMachineTemplate template = profile.getTemplate();
            /* create a iso placeholder */
            VolumeTO iso = new VolumeTO(profile.getId(), Volume.Type.ISO,
                    StoragePoolType.ISO, null, template.getName(), null, null,
                    0, null);
            iso.setDeviceId(3);
            profile.addDisk(iso);
        }

        return true;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds,
            VirtualMachineProfile<UserVmVO> profile, DeployDestination dest,
            ReservationContext context) {
        UserVmVO userVm = profile.getVirtualMachine();
        List<NicVO> nics = _nicDao.listByVmId(userVm.getId());
        for (NicVO nic : nics) {
            NetworkVO network = _networkDao.findById(nic.getNetworkId());
            if (network.getTrafficType() == TrafficType.Guest
                    || network.getTrafficType() == TrafficType.Public) {
                userVm.setPrivateIpAddress(nic.getIp4Address());
                userVm.setPrivateMacAddress(nic.getMacAddress());
            }
        }
        return true;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds,
            VirtualMachineProfile<UserVmVO> profile) {
        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile<UserVmVO> profile,
            long hostId, Commands cmds, ReservationContext context) {
        UserVmVO vm = profile.getVirtualMachine();

        Answer[] answersToCmds = cmds.getAnswers();
        if (answersToCmds == null) {
            if (s_logger.isDebugEnabled()) {
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
            for (NicTO nicTO : vmTO.getNics()) {
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
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_ASSIGN, vm.getAccountId(),
                    vm.getDataCenterId(), vm.getId(), vm.getHostName(), network.getNetworkOfferingId(),
                    null, isDefault, VirtualMachine.class.getName(), vm.getUuid());
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
            DataCenterVO dc = _dcDao.findById(vm.getDataCenterId());
            UserVmVO userVm = profile.getVirtualMachine();
            // dc.getDhcpProvider().equalsIgnoreCase(Provider.ExternalDhcpServer.getName())
            if (_ntwkSrvcDao.canProviderSupportServiceInNetwork(
                    guestNetwork.getId(), Service.Dhcp,
                    Provider.ExternalDhcpServer)) {
                _nicDao.update(guestNic.getId(), guestNic);
                userVm.setPrivateIpAddress(guestNic.getIp4Address());
                _vmDao.update(userVm.getId(), userVm);

                s_logger.info("Detected that ip changed in the answer, updated nic in the db with new ip "
                        + returnedIp);
            }
        }

        // get system ip and create static nat rule for the vm
        try {
            _rulesMgr.getSystemIpAndEnableStaticNatForVm(
                    profile.getVirtualMachine(), false);
        } catch (Exception ex) {
            s_logger.warn(
                    "Failed to get system ip and enable static nat for the vm "
                            + profile.getVirtualMachine()
                            + " due to exception ", ex);
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
    public UserVm stopVirtualMachine(long vmId, boolean forced)
            throws ConcurrentOperationException {
        // Input validation
        Account caller = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();

        // if account is removed, return error
        if (caller != null && caller.getRemoved() != null) {
            throw new PermissionDeniedException("The account " + caller.getId()
                    + " is removed");
        }

        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException(
                    "unable to find a virtual machine with id " + vmId);
        }

        UserVO user = _userDao.findById(userId);

        try {
            VirtualMachineEntity vmEntity = _orchSrvc.getVirtualMachine(vm.getUuid());
            vmEntity.stop(new Long(userId).toString());            
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException(
                    "Unable to contact the agent to stop the virtual machine "
                            + vm, e);
        } catch (CloudException e) {
            throw new CloudRuntimeException(
                    "Unable to contact the agent to stop the virtual machine "
                            + vm, e);
        }

        return _vmDao.findById(vmId);
    }

    @Override
    public void finalizeStop(VirtualMachineProfile<UserVmVO> profile,
            StopAnswer answer) {
        // release elastic IP here
        IPAddressVO ip = _ipAddressDao.findByAssociatedVmId(profile.getId());
        if (ip != null && ip.getSystem()) {
            UserContext ctx = UserContext.current();
            try {
                _rulesMgr.disableStaticNat(ip.getId(), ctx.getCaller(), ctx.getCallerUserId(), true);
            } catch (Exception ex) {
                s_logger.warn(
                        "Failed to disable static nat and release system ip "
                                + ip + " as a part of vm "
                                + profile.getVirtualMachine()
                                + " stop due to exception ", ex);
            }
        }
    }

    public String generateRandomPassword() {
        return PasswordGenerator.generateRandomPassword(6);
    }

    @Override
    public Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> startVirtualMachine(
            long vmId, Long hostId,
            Map<VirtualMachineProfile.Param, Object> additionalParams)
                    throws ConcurrentOperationException, ResourceUnavailableException,
                    InsufficientCapacityException {
        // Input validation
        Account callerAccount = UserContext.current().getCaller();
        UserVO callerUser = _userDao.findById(UserContext.current()
                .getCallerUserId());

        // if account is removed, return error
        if (callerAccount != null && callerAccount.getRemoved() != null) {
            throw new InvalidParameterValueException("The account "
                    + callerAccount.getId() + " is removed");
        }

        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException(
                    "unable to find a virtual machine with id " + vmId);
        }

        _accountMgr.checkAccess(callerAccount, null, true, vm);

        Account owner = _accountDao.findById(vm.getAccountId());

        if (owner == null) {
            throw new InvalidParameterValueException("The owner of " + vm
                    + " does not exist: " + vm.getAccountId());
        }

        if (owner.getState() == Account.State.disabled) {
            throw new PermissionDeniedException("The owner of " + vm
                    + " is disabled: " + vm.getAccountId());
        }

        Host destinationHost = null;
        if (hostId != null) {
            Account account = UserContext.current().getCaller();
            if (!_accountService.isRootAdmin(account.getType())) {
                throw new PermissionDeniedException(
                        "Parameter hostid can only be specified by a Root Admin, permission denied");
            }
            destinationHost = _hostDao.findById(hostId);
            if (destinationHost == null) {
                throw new InvalidParameterValueException(
                        "Unable to find the host to deploy the VM, host id="
                                + hostId);
            }
        }

        // check if vm is security group enabled
        if (_securityGroupMgr.isVmSecurityGroupEnabled(vmId) && _securityGroupMgr.getSecurityGroupsForVm(vmId).isEmpty() && !_securityGroupMgr.isVmMappedToDefaultSecurityGroup(vmId) && _networkModel.canAddDefaultSecurityGroup()) {
            // if vm is not mapped to security group, create a mapping
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Vm "
                        + vm
                        + " is security group enabled, but not mapped to default security group; creating the mapping automatically");
            }

            SecurityGroup defaultSecurityGroup = _securityGroupMgr
                    .getDefaultSecurityGroup(vm.getAccountId());
            if (defaultSecurityGroup != null) {
                List<Long> groupList = new ArrayList<Long>();
                groupList.add(defaultSecurityGroup.getId());
                _securityGroupMgr.addInstanceToGroups(vmId, groupList);
            }
        }

        DataCenterDeployment plan = null;
        if (destinationHost != null) {
            s_logger.debug("Destination Host to deploy the VM is specified, specifying a deployment plan to deploy the VM");
            plan = new DataCenterDeployment(vm.getDataCenterId(),
                    destinationHost.getPodId(), destinationHost.getClusterId(),
                    destinationHost.getId(), null, null);
        }

        // Set parameters
        Map<VirtualMachineProfile.Param, Object> params = null;
        VMTemplateVO template = null;
        if (vm.isUpdateParameters()) {
            _vmDao.loadDetails(vm);
            // Check that the password was passed in and is valid
            template = _templateDao
                    .findByIdIncludingRemoved(vm.getTemplateId());

            String password = "saved_password";
            if (template.getEnablePassword()) {
                password = generateRandomPassword();
            }

            if (!validPassword(password)) {
                throw new InvalidParameterValueException(
                        "A valid password for this virtual machine was not provided.");
            }

            // Check if an SSH key pair was selected for the instance and if so
            // use it to encrypt & save the vm password
            String sshPublicKey = vm.getDetail("SSH.PublicKey");
            if (sshPublicKey != null && !sshPublicKey.equals("")
                    && password != null && !password.equals("saved_password")) {
                String encryptedPasswd = RSAHelper.encryptWithSSHPublicKey(
                        sshPublicKey, password);
                if (encryptedPasswd == null) {
                    throw new CloudRuntimeException("Error encrypting password");
                }

                vm.setDetail("Encrypted.Password", encryptedPasswd);
                _vmDao.saveDetails(vm);
            }

            params = new HashMap<VirtualMachineProfile.Param, Object>();
            if (additionalParams != null) {
                params.putAll(additionalParams);
            }
            params.put(VirtualMachineProfile.Param.VmPassword, password);
        }

        VirtualMachineEntity vmEntity = _orchSrvc.getVirtualMachine(vm.getUuid());

        String plannerName = null;
        for (DeployPlannerSelector dps : plannerSelectors) {
            plannerName = dps.selectPlanner(vm);
            if (plannerName != null) {
                break;
            }
        }
        if (plannerName == null) {
            throw new CloudRuntimeException(String.format("cannot find DeployPlannerSelector for vm[uuid:%s, hypervisorType:%s]", vm.getUuid(), vm.getHypervisorType()));
        }

        String reservationId = vmEntity.reserve(plannerName, plan, new ExcludeList(), new Long(callerUser.getId()).toString());
        vmEntity.deploy(reservationId, new Long(callerUser.getId()).toString(), params);

        Pair<UserVmVO, Map<VirtualMachineProfile.Param, Object>> vmParamPair = new Pair(vm, params);
        if (vm != null && vm.isUpdateParameters()) {
            // this value is not being sent to the backend; need only for api
            // display purposes
            if (template.getEnablePassword()) {
                vm.setPassword((String) vmParamPair.second().get(VirtualMachineProfile.Param.VmPassword));
                vm.setUpdateParameters(false);
                _vmDao.update(vm.getId(), vm);
            }
        }

        return vmParamPair;
    }

    @Override
    public UserVm destroyVm(long vmId) throws ResourceUnavailableException,
    ConcurrentOperationException {
        Account caller = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();

        // Verify input parameters
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null || vm.getRemoved() != null) {
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "Unable to find a virtual machine with specified vmId");
            ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }

        if (vm.getState() == State.Destroyed
                || vm.getState() == State.Expunging) {
            s_logger.trace("Vm id=" + vmId + " is already destroyed");
            return vm;
        }

        _accountMgr.checkAccess(caller, null, true, vm);
        User userCaller = _userDao.findById(userId);

        boolean status;
        State vmState = vm.getState();

        try {
            VirtualMachineEntity vmEntity = _orchSrvc.getVirtualMachine(vm.getUuid());
            status = vmEntity.destroy(new Long(userId).toString());    
        } catch (CloudException e) {
            CloudRuntimeException ex = new CloudRuntimeException(
                    "Unable to destroy with specified vmId", e);
            ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }

        if (status) {
            // Mark the account's volumes as destroyed
            List<VolumeVO> volumes = _volsDao.findByInstance(vmId);
            for (VolumeVO volume : volumes) {
                if (volume.getVolumeType().equals(Volume.Type.ROOT)) {
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_DELETE, volume.getAccountId(),
                            volume.getDataCenterId(), volume.getId(), volume.getName(), Volume.class.getName(),
                            volume.getUuid());
                }
            }

            if (vmState != State.Error) {
                // Get serviceOffering for Virtual Machine
                ServiceOfferingVO offering = _serviceOfferingDao.findByIdIncludingRemoved(vm.getServiceOfferingId());

                //Update Resource Count for the given account
                resourceCountDecrement(vm.getAccountId(), new Long(offering.getCpu()),
                        new Long(offering.getRamSize()));
            }
            return _vmDao.findById(vmId);
        } else {
            CloudRuntimeException ex = new CloudRuntimeException(
                    "Failed to destroy vm with specified vmId");
            ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }
    }



    @Override
    public Pair<List<UserVmJoinVO>, Integer> searchForUserVMs(Criteria c, Account caller, Long domainId, boolean isRecursive,
            List<Long> permittedAccounts, boolean listAll, ListProjectResourcesCriteria listProjectResourcesCriteria, Map<String, String> tags) {
        Filter searchFilter = new Filter(UserVmJoinVO.class, c.getOrderBy(), c.getAscending(), c.getOffset(), c.getLimit());

        //first search distinct vm id by using query criteria and pagination
        SearchBuilder<UserVmJoinVO> sb = _vmJoinDao.createSearchBuilder();
        sb.select(null, Func.DISTINCT, sb.entity().getId()); // select distinct ids
        _accountMgr.buildACLViewSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

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
        Object templateId = c.getCriteria(Criteria.TEMPLATE_ID);
        Object isoId = c.getCriteria(Criteria.ISO_ID);
        Object vpcId = c.getCriteria(Criteria.VPC_ID);

        sb.and("displayName", sb.entity().getDisplayName(),
                SearchCriteria.Op.LIKE);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getHostName(), SearchCriteria.Op.LIKE);
        sb.and("stateEQ", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and("stateNEQ", sb.entity().getState(), SearchCriteria.Op.NEQ);
        sb.and("stateNIN", sb.entity().getState(), SearchCriteria.Op.NIN);
        sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        sb.and("podId", sb.entity().getPodId(), SearchCriteria.Op.EQ);
        sb.and("hypervisorType", sb.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        sb.and("hostIdEQ", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        sb.and("hostName", sb.entity().getHostName(), SearchCriteria.Op.LIKE);
        sb.and("templateId", sb.entity().getTemplateId(), SearchCriteria.Op.EQ);
        sb.and("isoId", sb.entity().getIsoId(), SearchCriteria.Op.EQ);
        sb.and("instanceGroupId", sb.entity().getInstanceGroupId(), SearchCriteria.Op.EQ);

        if (groupId != null && (Long) groupId != -1) {
            sb.and("instanceGroupId", sb.entity().getInstanceGroupId(), SearchCriteria.Op.EQ);
        }

        if (tags != null && !tags.isEmpty()) {
            for (int count=0; count < tags.size(); count++) {
                sb.or().op("key" + String.valueOf(count), sb.entity().getTagKey(), SearchCriteria.Op.EQ);
                sb.and("value" + String.valueOf(count), sb.entity().getTagValue(), SearchCriteria.Op.EQ);
                sb.cp();
            }
        }

        if (networkId != null) {
            sb.and("networkId", sb.entity().getNetworkId(), SearchCriteria.Op.EQ);
        }

        if(vpcId != null && networkId == null){
            sb.and("vpcId", sb.entity().getVpcId(), SearchCriteria.Op.EQ);
        }

        if (storageId != null) {
            sb.and("poolId", sb.entity().getPoolId(), SearchCriteria.Op.EQ);
        }

        // populate the search criteria with the values passed in
        SearchCriteria<UserVmJoinVO> sc = sb.create();

        // building ACL condition
        _accountMgr.buildACLViewSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            for (String key : tags.keySet()) {
                sc.setParameters("key" + String.valueOf(count), key);
                sc.setParameters("value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        if (groupId != null && (Long)groupId != -1) {
            sc.setParameters("instanceGroupId", groupId);
        }

        if (keyword != null) {
            SearchCriteria<UserVmJoinVO> ssc = _vmJoinDao.createSearchCriteria();
            ssc.addOr("displayName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("hostName", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("instanceName", SearchCriteria.Op.LIKE, "%" + keyword
                    + "%");
            ssc.addOr("state", SearchCriteria.Op.EQ, keyword);

            sc.addAnd("displayName", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (templateId != null) {
            sc.setParameters("templateId", templateId);
        }

        if (isoId != null) {
            sc.setParameters("isoId", isoId);
        }

        if (networkId != null) {
            sc.setParameters("networkId", networkId);
        }

        if(vpcId != null && networkId == null){
            sc.setParameters("vpcId", vpcId);
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
                sc.setParameters("hostName", hostName);
            }
        }

        if (storageId != null) {
            sc.setParameters("poolId", storageId);
        }

        // search vm details by ids
        Pair<List<UserVmJoinVO>, Integer> uniqueVmPair =  _vmJoinDao.searchAndCount(sc, searchFilter);
        Integer count = uniqueVmPair.second();
        if ( count.intValue() == 0 ){
            // handle empty result cases
            return uniqueVmPair;
        }
        List<UserVmJoinVO> uniqueVms = uniqueVmPair.first();
        Long[] vmIds = new Long[uniqueVms.size()];
        int i = 0;
        for (UserVmJoinVO v : uniqueVms ){
            vmIds[i++] = v.getId();
        }
        List<UserVmJoinVO> vms = _vmJoinDao.searchByIds(vmIds);
        return new Pair<List<UserVmJoinVO>, Integer>(vms, count);
    }

    @Override
    public HypervisorType getHypervisorTypeOfUserVM(long vmId) {
        UserVmVO userVm = _vmDao.findById(vmId);
        if (userVm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "unable to find a virtual machine with specified id");
            ex.addProxyObject(userVm, vmId, "vmId");
            throw ex;
        }

        return userVm.getHypervisorType();
    }

    @Override
    public UserVm createVirtualMachine(DeployVMCmd cmd)
            throws InsufficientCapacityException, ResourceUnavailableException,
            ConcurrentOperationException, StorageUnavailableException,
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
            throw new PermissionDeniedException(
                    "No permission to migrate VM, Only Root Admin can migrate a VM!");
        }

        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException(
                    "Unable to find the VM by id=" + vmId);
        }

        if (vm.getState() != State.Stopped) {
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "VM is not Stopped, unable to migrate the vm having the specified id");
            ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }

        if (vm.getType() != VirtualMachine.Type.User) {
            throw new InvalidParameterValueException(
                    "can only do storage migration on user vm");
        }

        List<VolumeVO> vols = _volsDao.findByInstance(vm.getId());
        if (vols.size() > 1) {
            throw new InvalidParameterValueException(
                    "Data disks attached to the vm, can not migrate. Need to dettach data disks at first");
        }

        HypervisorType destHypervisorType = _clusterDao.findById(
                destPool.getClusterId()).getHypervisorType();
        if (vm.getHypervisorType() != destHypervisorType) {
            throw new InvalidParameterValueException(
                    "hypervisor is not compatible: dest: "
                            + destHypervisorType.toString() + ", vm: "
                            + vm.getHypervisorType().toString());
        }
        VMInstanceVO migratedVm = _itMgr.storageMigration(vm, destPool);
        return migratedVm;

    }

    private boolean isVMUsingLocalStorage(VMInstanceVO vm) {
        boolean usesLocalStorage = false;
        ServiceOfferingVO svcOffering = _serviceOfferingDao.findById(vm
                .getServiceOfferingId());
        if (svcOffering.getUseLocalStorage()) {
            usesLocalStorage = true;
        } else {
            List<VolumeVO> volumes = _volsDao.findByInstanceAndType(vm.getId(),
                    Volume.Type.DATADISK);
            for (VolumeVO vol : volumes) {
                DiskOfferingVO diskOffering = _diskOfferingDao.findById(vol
                        .getDiskOfferingId());
                if (diskOffering.getUseLocalStorage()) {
                    usesLocalStorage = true;
                    break;
                }
            }
        }
        return usesLocalStorage;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_MIGRATE, eventDescription = "migrating VM", async = true)
    public VirtualMachine migrateVirtualMachine(Long vmId, Host destinationHost)
            throws ResourceUnavailableException, ConcurrentOperationException,
            ManagementServerException, VirtualMachineMigrationException {
        // access check - only root admin can migrate VM
        Account caller = UserContext.current().getCaller();
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Caller is not a root admin, permission denied to migrate the VM");
            }
            throw new PermissionDeniedException(
                    "No permission to migrate VM, Only Root Admin can migrate a VM!");
        }

        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException(
                    "Unable to find the VM by id=" + vmId);
        }
        // business logic
        if (vm.getState() != State.Running) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is not Running, unable to migrate the vm "
                        + vm);
            }
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "VM is not Running, unable to migrate the vm with specified id");
            ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }
        if (!vm.getHypervisorType().equals(HypervisorType.XenServer)
                && !vm.getHypervisorType().equals(HypervisorType.VMware)
                && !vm.getHypervisorType().equals(HypervisorType.KVM)
                && !vm.getHypervisorType().equals(HypervisorType.Ovm)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(vm
                        + " is not XenServer/VMware/KVM/Ovm, cannot migrate this VM.");
            }
            throw new InvalidParameterValueException(
                    "Unsupported Hypervisor Type for VM migration, we support XenServer/VMware/KVM only");
        }

        if (isVMUsingLocalStorage(vm)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(vm
                        + " is using Local Storage, cannot migrate this VM.");
            }
            throw new InvalidParameterValueException(
                    "Unsupported operation, VM uses Local storage, cannot migrate");
        }

        // check if migrating to same host
        long srcHostId = vm.getHostId();
        if (destinationHost.getId() == srcHostId) {
            throw new InvalidParameterValueException(
                    "Cannot migrate VM, VM is already presnt on this host, please specify valid destination host to migrate the VM");
        }

        // check if host is UP
        if (destinationHost.getStatus() != com.cloud.host.Status.Up
                || destinationHost.getResourceState() != ResourceState.Enabled) {
            throw new InvalidParameterValueException(
                    "Cannot migrate VM, destination host is not in correct state, has status: "
                            + destinationHost.getStatus() + ", state: "
                            + destinationHost.getResourceState());
        }

        // call to core process
        DataCenterVO dcVO = _dcDao.findById(destinationHost.getDataCenterId());
        HostPodVO pod = _podDao.findById(destinationHost.getPodId());
        Cluster cluster = _clusterDao.findById(destinationHost.getClusterId());
        DeployDestination dest = new DeployDestination(dcVO, pod, cluster,
                destinationHost);

        // check max guest vm limit for the destinationHost
        HostVO destinationHostVO = _hostDao.findById(destinationHost.getId());
        if (_capacityMgr.checkIfHostReachMaxGuestLimit(destinationHostVO)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Host name: "
                        + destinationHost.getName()
                        + ", hostId: "
                        + destinationHost.getId()
                        + " already has max Running VMs(count includes system VMs), cannot migrate to this host");
            }
            throw new VirtualMachineMigrationException(
                    "Destination host, hostId: "
                            + destinationHost.getId()
                            + " already has max Running VMs(count includes system VMs), cannot migrate to this host");
        }

        VMInstanceVO migratedVm = _itMgr.migrate(vm, srcHostId, dest);
        return migratedVm;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VM_MOVE, eventDescription = "move VM to another user", async = false)
    public UserVm moveVMToUser(AssignVMCmd cmd)
            throws ResourceAllocationException, ConcurrentOperationException,
            ResourceUnavailableException, InsufficientCapacityException {
        // VERIFICATIONS and VALIDATIONS

        // VV 1: verify the two users
        Account caller = UserContext.current().getCaller();
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN
                && caller.getType() != Account.ACCOUNT_TYPE_DOMAIN_ADMIN) { // only
            // root
            // admin
            // can
            // assign
            // VMs
            throw new InvalidParameterValueException(
                    "Only domain admins are allowed to assign VMs and not "
                            + caller.getType());
        }

        // get and check the valid VM
        UserVmVO vm = _vmDao.findById(cmd.getVmId());
        if (vm == null) {
            throw new InvalidParameterValueException(
                    "There is no vm by that id " + cmd.getVmId());
        } else if (vm.getState() == State.Running) { // VV 3: check if vm is
            // running
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is Running, unable to move the vm " + vm);
            }
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "VM is Running, unable to move the vm with specified vmId");
            ex.addProxyObject(vm, cmd.getVmId(), "vmId");
            throw ex;
        }

        Account oldAccount = _accountService.getActiveAccountById(vm
                .getAccountId());
        if (oldAccount == null) {
            throw new InvalidParameterValueException("Invalid account for VM "
                    + vm.getAccountId() + " in domain.");
        }
        // don't allow to move the vm from the project
        if (oldAccount.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "Specified Vm id belongs to the project and can't be moved");
            ex.addProxyObject(vm, cmd.getVmId(), "vmId");
            throw ex;
        }
        Account newAccount = _accountService.getActiveAccountByName(
                cmd.getAccountName(), cmd.getDomainId());
        if (newAccount == null
                || newAccount.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InvalidParameterValueException("Invalid accountid="
                    + cmd.getAccountName() + " in domain " + cmd.getDomainId());
        }

        if (newAccount.getState() == Account.State.disabled) {
            throw new InvalidParameterValueException("The new account owner "
                    + cmd.getAccountName() + " is disabled.");
        }

        // make sure the accounts are under same domain
        if (oldAccount.getDomainId() != newAccount.getDomainId()) {
            throw new InvalidParameterValueException(
                    "The account should be under same domain for moving VM between two accounts. Old owner domain ="
                            + oldAccount.getDomainId()
                            + " New owner domain="
                            + newAccount.getDomainId());
        }

        // make sure the accounts are not same
        if (oldAccount.getAccountId() == newAccount.getAccountId()) {
            throw new InvalidParameterValueException(
                    "The account should be same domain for moving VM between two accounts. Account id ="
                            + oldAccount.getAccountId());
        }

        // don't allow to move the vm if there are existing PF/LB/Static Nat
        // rules, or vm is assigned to static Nat ip
        List<PortForwardingRuleVO> pfrules = _portForwardingDao.listByVm(cmd
                .getVmId());
        if (pfrules != null && pfrules.size() > 0) {
            throw new InvalidParameterValueException(
                    "Remove the Port forwarding rules for this VM before assigning to another user.");
        }
        List<FirewallRuleVO> snrules = _rulesDao
                .listStaticNatByVmId(vm.getId());
        if (snrules != null && snrules.size() > 0) {
            throw new InvalidParameterValueException(
                    "Remove the StaticNat rules for this VM before assigning to another user.");
        }
        List<LoadBalancerVMMapVO> maps = _loadBalancerVMMapDao
                .listByInstanceId(vm.getId());
        if (maps != null && maps.size() > 0) {
            throw new InvalidParameterValueException(
                    "Remove the load balancing rules for this VM before assigning to another user.");
        }
        // check for one on one nat
        IPAddressVO ip = _ipAddressDao.findByAssociatedVmId(cmd.getVmId());
        if (ip != null) {
            if (ip.isOneToOneNat()) {
                throw new InvalidParameterValueException(
                        "Remove the one to one nat rule for this VM for ip "
                                + ip.toString());
            }
        }

        DataCenterVO zone = _dcDao.findById(vm.getDataCenterId());

        // Get serviceOffering for Virtual Machine
        ServiceOfferingVO offering = _serviceOfferingDao.findByIdIncludingRemoved(vm.getServiceOfferingId());

        //Remove vm from instance group
        removeInstanceFromInstanceGroup(cmd.getVmId());

        // VV 2: check if account/domain is with in resource limits to create a new vm
        resourceLimitCheck(newAccount, new Long(offering.getCpu()), new Long(offering.getRamSize()));

        // VV 3: check if volumes are with in resource limits
        _resourceLimitMgr.checkResourceLimit(newAccount, ResourceType.volume,
                _volsDao.findByInstance(cmd.getVmId()).size());

        // VV 4: Check if new owner can use the vm template
        VirtualMachineTemplate template = _templateDao.findById(vm
                .getTemplateId());
        if (!template.isPublicTemplate()) {
            Account templateOwner = _accountMgr.getAccount(template
                    .getAccountId());
            _accountMgr.checkAccess(newAccount, null, true, templateOwner);
        }

        // VV 5: check the new account can create vm in the domain
        DomainVO domain = _domainDao.findById(cmd.getDomainId());
        _accountMgr.checkAccess(newAccount, domain);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        //generate destroy vm event for usage
        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_DESTROY, vm.getAccountId(), vm.getDataCenterId(), vm.getId(),
                vm.getHostName(), vm.getServiceOfferingId(), vm.getTemplateId(), vm.getHypervisorType().toString(),
                VirtualMachine.class.getName(), vm.getUuid());

        // update resource counts for old account
        resourceCountDecrement(oldAccount.getAccountId(), new Long(offering.getCpu()),
                new Long(offering.getRamSize()));

        // OWNERSHIP STEP 1: update the vm owner
        vm.setAccountId(newAccount.getAccountId());
        vm.setDomainId(cmd.getDomainId());
        _vmDao.persist(vm);

        // OS 2: update volume
        List<VolumeVO> volumes = _volsDao.findByInstance(cmd.getVmId());
        for (VolumeVO volume : volumes) {
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_DELETE, volume.getAccountId(),
                    volume.getDataCenterId(), volume.getId(), volume.getName(), Volume.class.getName(), volume.getUuid());
            _resourceLimitMgr.decrementResourceCount(oldAccount.getAccountId(), ResourceType.volume);
            volume.setAccountId(newAccount.getAccountId());
            _volsDao.persist(volume);
            _resourceLimitMgr.incrementResourceCount(newAccount.getAccountId(), ResourceType.volume);
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(),
                    volume.getDataCenterId(), volume.getId(), volume.getName(),
                    volume.getDiskOfferingId(), volume.getTemplateId(), volume.getSize(), Volume.class.getName(),
                    volume.getUuid());
            //snapshots: mark these removed in db
            List<SnapshotVO> snapshots = _snapshotDao.listByVolumeIdIncludingRemoved(volume.getId());
            for (SnapshotVO snapshot: snapshots){
                _snapshotDao.remove(snapshot.getId());
            }
        }

        //update resource count of new account
        resourceCountIncrement(newAccount.getAccountId(), new Long(offering.getCpu()), new Long(offering.getRamSize()));

        //generate usage events to account for this change
        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_CREATE, vm.getAccountId(), vm.getDataCenterId(), vm.getId(),
                vm.getHostName(), vm.getServiceOfferingId(), vm.getTemplateId(), vm.getHypervisorType().toString(),
                VirtualMachine.class.getName(), vm.getUuid());

        txn.commit();

        VMInstanceVO vmoi = _itMgr.findByIdAndType(vm.getType(), vm.getId());
        VirtualMachineProfileImpl<VMInstanceVO> vmOldProfile = new VirtualMachineProfileImpl<VMInstanceVO>(
                vmoi);

        // OS 3: update the network
        List<Long> networkIdList = cmd.getNetworkIds();
        List<Long> securityGroupIdList = cmd.getSecurityGroupIdList();

        if (zone.getNetworkType() == NetworkType.Basic) {
            if (networkIdList != null && !networkIdList.isEmpty()) {
                throw new InvalidParameterValueException(
                        "Can't move vm with network Ids; this is a basic zone VM");
            }
            // cleanup the old security groups
            _securityGroupMgr.removeInstanceFromGroups(cmd.getVmId());
            // cleanup the network for the oldOwner
            _networkMgr.cleanupNics(vmOldProfile);
            _networkMgr.expungeNics(vmOldProfile);
            // security groups will be recreated for the new account, when the
            // VM is started
            List<NetworkVO> networkList = new ArrayList<NetworkVO>();

            // Get default guest network in Basic zone
            Network defaultNetwork = _networkModel.getExclusiveGuestNetwork(zone.getId());

            if (defaultNetwork == null) {
                throw new InvalidParameterValueException(
                        "Unable to find a default network to start a vm");
            } else {
                networkList.add(_networkDao.findById(defaultNetwork.getId()));
            }

            boolean isVmWare = (template.getHypervisorType() == HypervisorType.VMware);

            if (securityGroupIdList != null && isVmWare) {
                throw new InvalidParameterValueException("Security group feature is not supported for vmWare hypervisor");
            } else if (!isVmWare && _networkModel.isSecurityGroupSupportedInNetwork(defaultNetwork) && _networkModel.canAddDefaultSecurityGroup()) {
                if (securityGroupIdList == null) {
                    securityGroupIdList = new ArrayList<Long>();
                }
                SecurityGroup defaultGroup = _securityGroupMgr
                        .getDefaultSecurityGroup(newAccount.getId());
                if (defaultGroup != null) {
                    // check if security group id list already contains Default
                    // security group, and if not - add it
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
                    // create default security group for the account
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Couldn't find default security group for the account "
                                + newAccount + " so creating a new one");
                    }
                    defaultGroup = _securityGroupMgr.createSecurityGroup(
                            SecurityGroupManager.DEFAULT_GROUP_NAME,
                            SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION,
                            newAccount.getDomainId(), newAccount.getId(),
                            newAccount.getAccountName());
                    securityGroupIdList.add(defaultGroup.getId());
                }
            }

            List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>();
            NicProfile profile = new NicProfile();
            profile.setDefaultNic(true);
            networks.add(new Pair<NetworkVO, NicProfile>(networkList.get(0),
                    profile));

            VMInstanceVO vmi = _itMgr.findByIdAndType(vm.getType(), vm.getId());
            VirtualMachineProfileImpl<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(
                    vmi);
            _networkMgr.allocate(vmProfile, networks);

            _securityGroupMgr.addInstanceToGroups(vm.getId(),
                    securityGroupIdList);

            s_logger.debug("AssignVM: Basic zone, adding security groups no "
                    + securityGroupIdList.size() + " to "
                    + vm.getInstanceName());
        } else {
            if (zone.isSecurityGroupEnabled())  {
                throw new InvalidParameterValueException(
                        "Not yet implemented for SecurityGroupEnabled advanced networks.");
            } else {
                if (securityGroupIdList != null
                        && !securityGroupIdList.isEmpty()) {
                    throw new InvalidParameterValueException(
                            "Can't move vm with security groups; security group feature is not enabled in this zone");
                }
                // cleanup the network for the oldOwner
                _networkMgr.cleanupNics(vmOldProfile);
                _networkMgr.expungeNics(vmOldProfile);

                Set<NetworkVO> applicableNetworks = new HashSet<NetworkVO>();

                if (networkIdList != null && !networkIdList.isEmpty()) {
                    // add any additional networks
                    for (Long networkId : networkIdList) {
                        NetworkVO network = _networkDao.findById(networkId);
                        if (network == null) {
                            InvalidParameterValueException ex = new InvalidParameterValueException(
                                    "Unable to find specified network id");
                            ex.addProxyObject(network, networkId, "networkId");
                            throw ex;
                        }

                        _networkModel.checkNetworkPermissions(newAccount, network);

                        // don't allow to use system networks
                        NetworkOffering networkOffering = _configMgr
                                .getNetworkOffering(network
                                        .getNetworkOfferingId());
                        if (networkOffering.isSystemOnly()) {
                            InvalidParameterValueException ex = new InvalidParameterValueException(
                                    "Specified Network id is system only and can't be used for vm deployment");
                            ex.addProxyObject(network, networkId, "networkId");
                            throw ex;
                        }
                        applicableNetworks.add(network);
                    }
                } else {
                    NetworkVO defaultNetwork = null;
                    List<NetworkOfferingVO> requiredOfferings = _networkOfferingDao
                            .listByAvailability(Availability.Required, false);
                    if (requiredOfferings.size() < 1) {
                        throw new InvalidParameterValueException(
                                "Unable to find network offering with availability="
                                        + Availability.Required
                                        + " to automatically create the network as a part of vm creation");
                    }
                    if (requiredOfferings.get(0).getState() == NetworkOffering.State.Enabled) {
                        // get Virtual networks
                        List<? extends Network> virtualNetworks = _networkModel.listNetworksForAccount(newAccount.getId(), zone.getId(), Network.GuestType.Isolated);
                        if (virtualNetworks.isEmpty()) {
                            long physicalNetworkId = _networkModel.findPhysicalNetworkId(zone.getId(), requiredOfferings.get(0).getTags(), requiredOfferings.get(0).getTrafficType());
                            // Validate physical network
                            PhysicalNetwork physicalNetwork = _physicalNetworkDao
                                    .findById(physicalNetworkId);
                            if (physicalNetwork == null) {
                                throw new InvalidParameterValueException("Unable to find physical network with id: "+physicalNetworkId   + " and tag: " +requiredOfferings.get(0).getTags());
                            }
                            s_logger.debug("Creating network for account " + newAccount + " from the network offering id=" +
                                    requiredOfferings.get(0).getId() + " as a part of deployVM process");
                            Network newNetwork = _networkMgr.createGuestNetwork(requiredOfferings.get(0).getId(),
                                    newAccount.getAccountName() + "-network", newAccount.getAccountName() + "-network", null, null,
                                    null, null, newAccount, null, physicalNetwork, zone.getId(), ACLType.Account, null, null, null, null);
                            // if the network offering has persistent set to true, implement the network
                            if (requiredOfferings.get(0).getIsPersistent()) {
                                DeployDestination dest = new DeployDestination(zone, null, null, null);
                                UserVO callerUser = _userDao.findById(UserContext.current().getCallerUserId());
                                Journal journal = new Journal.LogJournal("Implementing " + newNetwork, s_logger);
                                ReservationContext context = new ReservationContextImpl(UUID.randomUUID().toString(),
                                        journal, callerUser, caller);
                                s_logger.debug("Implementing the network for account" + newNetwork + " as a part of" +
                                        " network provision for persistent networks");
                                try {
                                    Pair<NetworkGuru, NetworkVO> implementedNetwork = _networkMgr.implementNetwork(newNetwork.getId(), dest, context);
                                    if (implementedNetwork.first() == null) {
                                        s_logger.warn("Failed to implement the network " + newNetwork);
                                    }
                                    newNetwork = implementedNetwork.second();
                                } catch (Exception ex) {
                                    s_logger.warn("Failed to implement network " + newNetwork + " elements and" +
                                            " resources as a part of network provision for persistent network due to ", ex);
                                    CloudRuntimeException e = new CloudRuntimeException("Failed to implement network" +
                                            " (with specified id) elements and resources as a part of network provision");
                                    e.addProxyObject(newNetwork, newNetwork.getId(), "networkId");
                                    throw e;
                                }
                            }
                            defaultNetwork = _networkDao.findById(newNetwork.getId());
                        } else if (virtualNetworks.size() > 1) {
                            throw new InvalidParameterValueException(
                                    "More than 1 default Isolated networks are found "
                                            + "for account " + newAccount
                                            + "; please specify networkIds");
                        } else {
                            defaultNetwork = _networkDao.findById(virtualNetworks.get(0).getId());
                        }
                    } else {
                        throw new InvalidParameterValueException(
                                "Required network offering id="
                                        + requiredOfferings.get(0).getId()
                                        + " is not in "
                                        + NetworkOffering.State.Enabled);
                    }

                    applicableNetworks.add(defaultNetwork);
                }

                // add the new nics
                List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>();
                int toggle = 0;
                for (NetworkVO appNet : applicableNetworks) {
                    NicProfile defaultNic = new NicProfile();
                    if (toggle == 0) {
                        defaultNic.setDefaultNic(true);
                        toggle++;
                    }
                    networks.add(new Pair<NetworkVO, NicProfile>(appNet,
                            defaultNic));
                }
                VMInstanceVO vmi = _itMgr.findByIdAndType(vm.getType(),
                        vm.getId());
                VirtualMachineProfileImpl<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(
                        vmi);
                _networkMgr.allocate(vmProfile, networks);
                s_logger.debug("AssignVM: Advance virtual, adding networks no "
                        + networks.size() + " to " + vm.getInstanceName());
            } // END IF NON SEC GRP ENABLED
        } // END IF ADVANCED
        s_logger.info("AssignVM: vm " + vm.getInstanceName()
                + " now belongs to account " + cmd.getAccountName());
        return vm;
    }

    @Override
    public UserVm restoreVM(RestoreVMCmd cmd) {
        // Input validation
        Account caller = UserContext.current().getCaller();

        long vmId = cmd.getVmId();
        Long newTemplateId = cmd.getTemplateId();
        UserVmVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Cannot find VM with ID " + vmId);
            ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }

        _accountMgr.checkAccess(caller, null, true, vm);

        return restoreVMInternal(caller, vm, newTemplateId);
    }

    public UserVm restoreVMInternal(Account caller, UserVmVO vm, Long newTemplateId){

        Long userId = caller.getId();
        Account owner = _accountDao.findById(vm.getAccountId());
        UserVO user = _userDao.findById(userId);
        long vmId = vm.getId();
        boolean needRestart = false;

        // Input validation
        if (owner == null) {
            throw new InvalidParameterValueException("The owner of " + vm
                    + " does not exist: " + vm.getAccountId());
        }

        if (owner.getState() == Account.State.disabled) {
            throw new PermissionDeniedException("The owner of " + vm
                    + " is disabled: " + vm.getAccountId());
        }

        if (vm.getState() != VirtualMachine.State.Running
                && vm.getState() != VirtualMachine.State.Stopped) {
            throw new CloudRuntimeException(
                    "Vm "
                            + vm.getUuid()
                            + " currently in "
                            + vm.getState()
                            + " state, restore vm can only execute when VM in Running or Stopped");
        }

        if (vm.getState() == VirtualMachine.State.Running) {
            needRestart = true;
        }

        List<VolumeVO> rootVols = _volsDao.findByInstance(vmId);
        if (rootVols.isEmpty()) {
            InvalidParameterValueException ex = new InvalidParameterValueException(
                    "Can not find root volume for VM " + vm.getUuid());
            ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }

        VolumeVO root = rootVols.get(0);
        Long templateId = root.getTemplateId();
        if(templateId == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Currently there is no support to reset a vm that is deployed using ISO " + vm.getUuid());
            ex.addProxyObject(vm, vmId, "vmId");
            throw ex;
        }

        VMTemplateVO template = null;
        if(newTemplateId != null) {
            template = _templateDao.findById(newTemplateId);
            _accountMgr.checkAccess(caller, null, true, template);
        } else {
            template = _templateDao.findById(templateId);
            if (template == null) {
                InvalidParameterValueException ex = new InvalidParameterValueException(
                        "Cannot find template for specified volumeid and vmId");
                ex.addProxyObject(vm, vmId, "vmId");
                ex.addProxyObject(root, root.getId(), "volumeId");
                throw ex;
            }
        }

        if (needRestart) {
            try {
                _itMgr.stop(vm, user, caller);
            } catch (ResourceUnavailableException e) {
                s_logger.debug("Stop vm " + vm.getUuid() + " failed", e);
                CloudRuntimeException ex = new CloudRuntimeException(
                        "Stop vm failed for specified vmId");
                ex.addProxyObject(vm, vmId, "vmId");
                throw ex;
            }
        }

        /* If new template is provided allocate a new volume from new template otherwise allocate new volume from original template */
        VolumeVO newVol = null;
        if (newTemplateId != null){
            newVol = volumeMgr.allocateDuplicateVolume(root, newTemplateId);
            vm.setGuestOSId(template.getGuestOSId());
            vm.setTemplateId(newTemplateId);
            _vmDao.update(vmId, vm);
        } else {
            newVol = volumeMgr.allocateDuplicateVolume(root, null);
        }

        _volsDao.attachVolume(newVol.getId(), vmId, newVol.getDeviceId());

        /* Detach and destory the old root volume */

        _volsDao.detachVolume(root.getId());
        this.volumeMgr.destroyVolume(root);

        if (needRestart) {
            try {
                _itMgr.start(vm, null, user, caller);
            } catch (Exception e) {
                s_logger.debug("Unable to start VM " + vm.getUuid(), e);
                CloudRuntimeException ex = new CloudRuntimeException(
                        "Unable to start VM with specified id" + e.getMessage());
                ex.addProxyObject(vm, vmId, "vmId");
                throw ex;
            }
        }

        s_logger.debug("Restore VM " + vmId + " with template "
                + template.getUuid() + " done successfully");
        return vm;

    }

    @Override
    public boolean plugNic(Network network, NicTO nic, VirtualMachineTO vm,
            ReservationContext context, DeployDestination dest)
                    throws ConcurrentOperationException, ResourceUnavailableException,
                    InsufficientCapacityException {
        UserVmVO vmVO = _vmDao.findById(vm.getId());
        if (vmVO.getState() == State.Running) {
            try {
                PlugNicCommand plugNicCmd = new PlugNicCommand(nic,vm.getName());
                Commands cmds = new Commands(OnError.Stop);
                cmds.addCommand("plugnic",plugNicCmd);
                _agentMgr.send(dest.getHost().getId(),cmds);
                PlugNicAnswer plugNicAnswer = cmds.getAnswer(PlugNicAnswer.class);
                if (!(plugNicAnswer != null && plugNicAnswer.getResult())) {
                    s_logger.warn("Unable to plug nic for " + vmVO);
                    return false;
                }
            } catch (OperationTimedoutException e) {
                throw new AgentUnavailableException("Unable to plug nic for " + vmVO + " in network " + network, dest.getHost().getId(), e);
            }
        } else if (vmVO.getState() == State.Stopped || vmVO.getState() == State.Stopping) {
            s_logger.warn(vmVO + " is Stopped, not sending PlugNicCommand.  Currently " + vmVO.getState());
        } else {
            s_logger.warn("Unable to plug nic, " + vmVO + " is not in the right state " + vmVO.getState());
            throw new ResourceUnavailableException("Unable to plug nic on the backend," +
                    vmVO + " is not in the right state", DataCenter.class, vmVO.getDataCenterId());
        }
        return true;
    }

    @Override
    public boolean unplugNic(Network network, NicTO nic, VirtualMachineTO vm,
            ReservationContext context, DeployDestination dest) throws ConcurrentOperationException, ResourceUnavailableException {
        UserVmVO vmVO = _vmDao.findById(vm.getId());
        if (vmVO.getState() == State.Running) {
            try {
                UnPlugNicCommand unplugNicCmd = new UnPlugNicCommand(nic,vm.getName());
                Commands cmds = new Commands(OnError.Stop);
                cmds.addCommand("unplugnic",unplugNicCmd);
                _agentMgr.send(dest.getHost().getId(),cmds);
                UnPlugNicAnswer unplugNicAnswer = cmds.getAnswer(UnPlugNicAnswer.class);
                if (!(unplugNicAnswer != null && unplugNicAnswer.getResult())) {
                    s_logger.warn("Unable to unplug nic for " + vmVO);
                    return false;
                }
            } catch (OperationTimedoutException e) {
                throw new AgentUnavailableException("Unable to unplug nic for " + vmVO + " in network " + network, dest.getHost().getId(), e);
            }
        } else if (vmVO.getState() == State.Stopped || vmVO.getState() == State.Stopping) {
            s_logger.warn(vmVO + " is Stopped, not sending UnPlugNicCommand.  Currently " + vmVO.getState());
        } else {
            s_logger.warn("Unable to unplug nic, " + vmVO + " is not in the right state " + vmVO.getState());
            throw new ResourceUnavailableException("Unable to unplug nic on the backend," +
                    vmVO + " is not in the right state", DataCenter.class, vmVO.getDataCenterId());
        }
        return true;
    }

    @Override
    public void prepareStop(VirtualMachineProfile<UserVmVO> profile) {
    }

}
