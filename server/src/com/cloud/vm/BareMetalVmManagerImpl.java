package com.cloud.vm;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.baremetal.PrepareLinMinPxeServerCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.api.commands.AttachVolumeCmd;
import com.cloud.api.commands.CreateTemplateCmd;
import com.cloud.api.commands.DeployVMCmd;
import com.cloud.api.commands.DetachVolumeCmd;
import com.cloud.api.commands.UpgradeVMCmd;
import com.cloud.baremetal.LinMinPxeServerManager;
import com.cloud.baremetal.PxeServerManager;
import com.cloud.baremetal.PxeServerManager.PxeServerType;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.DomainVO;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.NetworkVO;
import com.cloud.network.IpAddrAllocator.IpAddr;
import com.cloud.network.Networks.TrafficType;
import com.cloud.server.ManagementService;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.UserContext;
import com.cloud.user.UserVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.crypt.RSAHelper;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineProfile.Param;

@Local(value={BareMetalVmManager.class, BareMetalVmService.class})
public class BareMetalVmManagerImpl extends UserVmManagerImpl implements BareMetalVmManager, BareMetalVmService, Manager {
	private static final Logger s_logger = Logger.getLogger(BareMetalVmManagerImpl.class); 
	private ConfigurationDao _configDao;

	@Override
	public boolean attachISOToVM(long vmId, long isoId, boolean attach) {
		s_logger.warn("attachISOToVM is not supported by Bare Metal, just fake a true");
		return true;
	}
	
	@Override
	public Volume attachVolumeToVM(AttachVolumeCmd command) {
		s_logger.warn("attachVolumeToVM is not supported by Bare Metal, return null");
		return null;
	}
	
	@Override
	public Volume detachVolumeFromVM(DetachVolumeCmd cmd) {
		s_logger.warn("detachVolumeFromVM is not supported by Bare Metal, return null");
		return null;
	}
	
	@Override
	public UserVm upgradeVirtualMachine(UpgradeVMCmd cmd) {
		s_logger.warn("upgradeVirtualMachine is not supported by Bare Metal, return null");
		return null;
	}
	
	@Override
    public VMTemplateVO createPrivateTemplateRecord(CreateTemplateCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, ResourceAllocationException {
		s_logger.warn("createPrivateTemplateRecord is not supported by Bare Metal, return null");
		return null;
	}
	
	@Override @DB
    public VMTemplateVO createPrivateTemplate(CreateTemplateCmd command) throws CloudRuntimeException {
		s_logger.warn("createPrivateTemplate is not supported by Bare Metal, return null");
		return null;
	}

	@Override
	public UserVm createVirtualMachine(DeployVMCmd cmd) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException,
			StorageUnavailableException, ResourceAllocationException {
		Account caller = UserContext.current().getCaller();

		String accountName = cmd.getAccountName();
		Long domainId = cmd.getDomainId();
		List<Long> networkList = cmd.getNetworkIds();
		String group = cmd.getGroup();

		Account owner = _accountDao.findActiveAccount(accountName, domainId);
		if (owner == null) {
			throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
		}

		_accountMgr.checkAccess(caller, owner);
		long accountId = owner.getId();

		DataCenterVO dc = _dcDao.findById(cmd.getZoneId());
		if (dc == null) {
			throw new InvalidParameterValueException("Unable to find zone: " + cmd.getZoneId());
		}

		if (dc.getDomainId() != null) {
			DomainVO domain = _domainDao.findById(dc.getDomainId());
			if (domain == null) {
				throw new CloudRuntimeException("Unable to find the domain " + dc.getDomainId() + " for the zone: " + dc);
			}
			_accountMgr.checkAccess(caller, domain);
			_accountMgr.checkAccess(owner, domain);
		}

		// check if account/domain is with in resource limits to create a new vm
		if (_accountMgr.resourceLimitExceeded(owner, ResourceType.user_vm)) {
			ResourceAllocationException rae = new ResourceAllocationException("Maximum number of virtual machines for account: " + owner.getAccountName()
					+ " has been exceeded.");
			rae.setResourceType("vm");
			throw rae;
		}
		
		ServiceOfferingVO offering = _serviceOfferingDao.findById(cmd.getServiceOfferingId());
        if (offering == null || offering.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find service offering: " + cmd.getServiceOfferingId());
        }
		
		VMTemplateVO template = _templateDao.findById(cmd.getTemplateId());
        // Make sure a valid template ID was specified
        if (template == null || template.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to use template " + cmd.getTemplateId());
        }
        
        if (template.getTemplateType().equals(TemplateType.SYSTEM)) {
        	throw new InvalidParameterValueException("Unable to use system template " + cmd.getTemplateId()+" to deploy a user vm");
        }

        if (template.getFormat() != Storage.ImageFormat.BAREMETAL) {
        	throw new InvalidParameterValueException("Unable to use non Bare Metal template" + cmd.getTemplateId() +" to deploy a bare metal vm");
        }
        
        String userData = cmd.getUserData();
        byte [] decodedUserData = null;
        if (userData != null) {
            if (userData.length() >= 2 * MAX_USER_DATA_LENGTH_BYTES) {
                throw new InvalidParameterValueException("User data is too long");
            }
            decodedUserData = org.apache.commons.codec.binary.Base64.decodeBase64(userData.getBytes());
            if (decodedUserData.length > MAX_USER_DATA_LENGTH_BYTES){
                throw new InvalidParameterValueException("User data is too long");
            }
            if (decodedUserData.length < 1) {
                throw new InvalidParameterValueException("User data is too short");
            }
        }
        
        // Find an SSH public key corresponding to the key pair name, if one is given
        String sshPublicKey = null;
        if (cmd.getSSHKeyPairName() != null && !cmd.getSSHKeyPairName().equals("")) {
            Account account = UserContext.current().getCaller();
        	SSHKeyPair pair = _sshKeyPairDao.findByName(account.getAccountId(), account.getDomainId(), cmd.getSSHKeyPairName());
    		if (pair == null) {
                throw new InvalidParameterValueException("A key pair with name '" + cmd.getSSHKeyPairName() + "' was not found.");
            }

			sshPublicKey = pair.getPublicKey();
		}

		_accountMgr.checkAccess(caller, template);

		DataCenterDeployment plan = new DataCenterDeployment(dc.getId());

		s_logger.debug("Allocating in the DB for bare metal vm");
		
		if (dc.getNetworkType() != NetworkType.Basic || networkList != null) {
			s_logger.warn("Bare Metal only supports basical network mode now, switch to baisc network automatically");
		}
		
		Network defaultNetwork = _networkMgr.getSystemNetworkByZoneAndTrafficType(dc.getId(), TrafficType.Guest);
		if (defaultNetwork == null) {
			throw new InvalidParameterValueException("Unable to find a default network to start a vm");
		}
		
		networkList = new ArrayList<Long>();
		networkList.add(defaultNetwork.getId());
		
		List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>();
        short defaultNetworkNumber = 0;
        for (Long networkId : networkList) {
            NetworkVO network = _networkDao.findById(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Unable to find network by id " + networkId);
            } else {
                if (!network.isShared()) {
                    //Check account permissions
                    List<NetworkVO> networkMap = _networkDao.listBy(accountId, networkId);
                    if (networkMap == null || networkMap.isEmpty()) {
                        throw new PermissionDeniedException("Unable to create a vm using network with id " + networkId + ", permission denied");
                    }
                } 
                
                if (network.isDefault()) {
                    defaultNetworkNumber++;
                }
                networks.add(new Pair<NetworkVO, NicProfile>(network, null));
            }
        }

        //at least one network default network has to be set
        if (defaultNetworkNumber == 0) {
            throw new InvalidParameterValueException("At least 1 default network has to be specified for the vm");
        } else if (defaultNetworkNumber >1) {
            throw new InvalidParameterValueException("Only 1 default network per vm is supported");
        }
        
        long id = _vmDao.getNextInSequence(Long.class, "id");
        
        String hostName = cmd.getName();
        String instanceName = VirtualMachineName.getVmName(id, owner.getId(), _instance);
        if (hostName == null) {
            hostName = instanceName;
        } else {
            //verify hostName (hostname doesn't have to be unique)
            if (!NetUtils.verifyDomainNameLabel(hostName, true)) {
                throw new InvalidParameterValueException("Invalid name. Vm name can contain ASCII letters 'a' through 'z', the digits '0' through '9', " +
                		                                "and the hyphen ('-'), must be between 1 and 63 characters long, and can't start or end with \"-\" and can't start with digit");
            }
        }
        
        UserVmVO vm = new UserVmVO(id, instanceName, cmd.getDisplayName(), template.getId(), HypervisorType.BareMetal,
                template.getGuestOSId(), offering.getOfferHA(), domainId, owner.getId(), offering.getId(), userData, hostName);
        
        if (sshPublicKey != null) {
            vm.setDetail("SSH.PublicKey", sshPublicKey);
        }

		if (_itMgr.allocate(vm, template, offering, null, null, networks, null, plan, cmd.getHypervisor(), owner) == null) {
			return null;
		}
		
		// startVirtualMachine() will retrieve this property
		vm.setDetail("pxeboot", "true");
		_vmDao.saveDetails(vm);

		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Successfully allocated DB entry for " + vm);
		}

		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Successfully allocated DB entry for " + vm);
		}
		UserContext.current().setEventDetails("Vm Id: " + vm.getId());
		UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VM_CREATE, accountId, dc.getId(), vm.getId(), vm.getName(), offering.getId(),
				template.getId(), null);
		_usageEventDao.persist(usageEvent);

		_accountMgr.incrementResourceCount(accountId, ResourceType.user_vm);

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

		return vm;
	}
	
	public UserVm startVirtualMachine(DeployVMCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
	    long vmId = cmd.getEntityId();
	    UserVmVO vm = _vmDao.findById(vmId);
	    _vmDao.loadDetails(vm);
	    
		List<HostVO> servers = _hostDao.listBy(Host.Type.PxeServer, vm.getDataCenterId()); 
	    if (servers.size() == 0) {
	    	throw new CloudRuntimeException("Cannot find PXE server, please make sure there is one PXE server per zone");
	    }
	    HostVO pxeServer = servers.get(0);
	    
	    VMTemplateVO template = _templateDao.findById(vm.getTemplateId());
	    if (template == null || template.getFormat() != Storage.ImageFormat.BAREMETAL) {
	    	throw new InvalidParameterValueException("Invalid template with id = " + vm.getTemplateId());
	    }
	    
		Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>();
		//TODO: have to ugly harding code here
		if (pxeServer.getResource().equalsIgnoreCase("com.cloud.baremetal.LinMinPxeServerResource")) {
			params.put(Param.PxeSeverType, PxeServerType.LinMin);
		} else {
			throw new CloudRuntimeException("Unkown PXE server resource " + pxeServer.getResource());
		}
		
		return startVirtualMachine(cmd, params);
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

		String workers = configs.get("expunge.workers");
		int wrks = NumbersUtil.parseInt(workers, 10);

		String time = configs.get("expunge.interval");
		_expungeInterval = NumbersUtil.parseInt(time, 86400);

		time = configs.get("expunge.delay");
		_expungeDelay = NumbersUtil.parseInt(time, _expungeInterval);

		_executor = Executors.newScheduledThreadPool(wrks, new NamedThreadFactory("UserVm-Scavenger"));

		_itMgr.registerGuru(Type.UserBareMetal, this);

		s_logger.info("User VM Manager is configured.");

		return true;
	}
	
	@Override
	public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context) {
        UserVmVO vm = profile.getVirtualMachine();
	    Account owner = _accountDao.findById(vm.getAccountId());
	    
	    if (owner == null || owner.getState() == Account.State.disabled) {
	        throw new PermissionDeniedException("The owner of " + vm + " either does not exist or is disabled: " + vm.getAccountId());
	    }
	    
	    PxeServerType pxeType = (PxeServerType) profile.getParameter(Param.PxeSeverType);
	    if (pxeType == null) {
	    	s_logger.debug("This is a normal IPMI start, skip prepartion of PXE server");
	    	return true;
	    }
	    s_logger.debug("This is a PXE start, prepare PXE server first");
	    
	    PxeServerManager pxeMgr = null;
	    ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
	    if (pxeType == PxeServerType.LinMin) {
	    	pxeMgr = locator.getManager(LinMinPxeServerManager.class);
	    } else {
	    	throw new CloudRuntimeException("Unsupport PXE type " + pxeType.toString());
	    }
	    
	    if (pxeMgr == null) {
	    	throw new CloudRuntimeException("No PXE manager find for type " + pxeType.toString());
	    }
	    
	    List<HostVO> servers = _hostDao.listBy(Host.Type.PxeServer, vm.getDataCenterId()); 
	    if (servers.size() == 0) {
	    	throw new CloudRuntimeException("Cannot find PXE server, please make sure there is one PXE server per zone");
	    }
	    HostVO pxeServer = servers.get(0);
	    
	    if (!pxeMgr.prepare(profile, dest, context, pxeServer.getId())) {
	    	throw new CloudRuntimeException("Pepare PXE server failed");
	    }
	    
	    profile.addBootArgs("PxeBoot");
	    
	    return true;
	}
	
	@Override
	public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context) {
		UserVmVO userVm = profile.getVirtualMachine();
		List<NicVO> nics = _nicDao.listByVmId(userVm.getId());
		for (NicVO nic : nics) {
			NetworkVO network = _networkDao.findById(nic.getNetworkId());
			if (network.getTrafficType() == TrafficType.Guest) {
				userVm.setPrivateIpAddress(nic.getIp4Address());
				userVm.setPrivateMacAddress(nic.getMacAddress());
			}
		}
		_vmDao.update(userVm.getId(), userVm);
		return true;
	}
	
	@Override
	public boolean finalizeStart(VirtualMachineProfile<UserVmVO> profile, long hostId, Commands cmds, ReservationContext context) {
		UserVmVO vm = profile.getVirtualMachine();
		UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VM_START, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), vm.getName(),
				vm.getServiceOfferingId(), vm.getTemplateId(), null);
		_usageEventDao.persist(usageEvent);

		List<NicVO> nics = _nicDao.listByVmId(vm.getId());
		for (NicVO nic : nics) {
			NetworkVO network = _networkDao.findById(nic.getNetworkId());
			long isDefault = (nic.isDefaultNic()) ? 1 : 0;
			usageEvent = new UsageEventVO(EventTypes.EVENT_NETWORK_OFFERING_CREATE, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), vm.getName(),
					network.getNetworkOfferingId(), null, isDefault);
			_usageEventDao.persist(usageEvent);
		}

		return true;
	}

	@Override
	public void finalizeStop(VirtualMachineProfile<UserVmVO> profile, StopAnswer answer) {
		super.finalizeStop(profile, answer);
	}

	@Override
	public UserVm destroyVm(long vmId) throws ResourceUnavailableException, ConcurrentOperationException {
		return super.destroyVm(vmId);
	}
}
