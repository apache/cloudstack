//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package com.cloud.network.vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.lb.dao.ApplicationLoadBalancerRuleDao;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.manager.Commands;
import com.cloud.api.commands.DeployNetscalerVpxCmd;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class NetScalerVMManagerImpl extends ManagerBase implements NetScalerVMManager, VirtualMachineGuru {
    static final private String NetScalerLbVmNamePrefix = "NS";

    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    DomainRouterDao _internalLbVmDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    VirtualRouterProviderDao _vrProviderDao;
    @Inject
    ApplicationLoadBalancerRuleDao _lbDao;
    @Inject
    NetworkModel _ntwkModel;
    @Inject
    LoadBalancingRulesManager _lbMgr;
    @Inject
    NicDao _nicDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    ServiceOfferingDao _serviceOfferingDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalProviderDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    VMTemplateDao _templateDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    VMInstanceDao _vmDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    DomainRouterDao _routerDao;

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) {
        //NetScalerLB vm starts up with 3 Nics
        //Nic #1 - NS IP
        //Nic #2 - locallink(guest network)
        //Nic #3 - public nic

        for (final NicProfile nic : profile.getNics()) {
            if(nic.getTrafficType() == TrafficType.Control) {
                nic.setTrafficType(TrafficType.Guest);
            }
        }
        return true;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile profile, long hostId, Commands cmds, ReservationContext context) {
        return true;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile profile) {
        return true;
    }

    @Override
    public void finalizeStop(VirtualMachineProfile profile, Answer answer) {
    }

    @Override
    public void finalizeExpunge(VirtualMachine vm) {
    }

    @Override
    public void prepareStop(VirtualMachineProfile profile) {
    }

    @Override
    public void finalizeUnmanage(VirtualMachine vm) {
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _itMgr.registerGuru(VirtualMachine.Type.NetScalerVm, this);
        if (logger.isInfoEnabled()) {
            logger.info(getName() + " has been configured");
        }
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    protected VirtualRouter stopInternalLbVm(DomainRouterVO internalLbVm, boolean forced, Account caller, long callerUserId) throws ResourceUnavailableException, ConcurrentOperationException {
        logger.debug("Stopping internal lb vm " + internalLbVm);
        try {
            _itMgr.advanceStop(internalLbVm.getUuid(), forced);
            return _internalLbVmDao.findById(internalLbVm.getId());
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Unable to stop " + internalLbVm, e);
        }
    }

    public VirtualRouterProvider addNetScalerLoadBalancerElement(long ntwkSvcProviderId) {
        VirtualRouterProviderVO element = _vrProviderDao.findByNspIdAndType(ntwkSvcProviderId, com.cloud.network.VirtualRouterProvider.Type.NetScalerVm);
        if (element != null) {
            logger.debug("There is already an " + getName() + " with service provider id " + ntwkSvcProviderId);
            return element;
        }

        PhysicalNetworkServiceProvider provider = _physicalProviderDao.findById(ntwkSvcProviderId);
        if (provider == null || !provider.getProviderName().equalsIgnoreCase(getName())) {
            throw new InvalidParameterValueException("Invalid network service provider is specified");
        }

        element = new VirtualRouterProviderVO(ntwkSvcProviderId, com.cloud.network.VirtualRouterProvider.Type.NetScalerVm);
        element.setEnabled(true);
        element = _vrProviderDao.persist(element);
        return element;
    }

    protected long getNetScalerLbProviderId(long physicalNetworkId) {
        //final long physicalNetworkId = _ntwkModel.getPhysicalNetworkId(guestNetwork);

        final PhysicalNetworkServiceProvider provider = _physicalProviderDao.findByServiceProvider(physicalNetworkId, "Netscaler");
        if (provider == null) {
            throw new CloudRuntimeException("Cannot find service provider " + Provider.Netscaler.toString() + " in physical network " + physicalNetworkId);
        }

        //TODO: get from type
        VirtualRouterProvider netScalerLbProvider = _vrProviderDao.findByNspIdAndType(provider.getId(), com.cloud.network.VirtualRouterProvider.Type.NetScalerVm);
        if (netScalerLbProvider == null) {
            //create the vrp for netscalerVM.
            netScalerLbProvider = addNetScalerLoadBalancerElement(provider.getId());
        }

        return netScalerLbProvider.getId();
    }

    @Override
    public Map<String, Object> deployNsVpx(Account owner, DeployDestination dest, DeploymentPlan plan, long svcOffId, long templateId) throws InsufficientCapacityException {

        VMTemplateVO template = _templateDao.findById(templateId) ;
        long id = _vmDao.getNextInSequence(Long.class, "id");
        Account systemAcct = _accountMgr.getSystemAccount();

        if (template == null) {
            logger.error(" Unable to find the NS VPX template");
            throw new CloudRuntimeException("Unable to find the Template" + templateId);
        }
        long dataCenterId = dest.getDataCenter().getId();
        DataCenterVO dc = _dcDao.findById(dest.getDataCenter().getId());

        String nxVpxName = VirtualMachineName.getSystemVmName(id, "Vpx", NetScalerLbVmNamePrefix);

        ServiceOfferingVO vpxOffering = _serviceOfferingDao.findById(svcOffId); //using 2GB and 2CPU  offering
        if(vpxOffering.getRamSize() < 2048 && vpxOffering.getCpu() <2 ) {
            throw new InvalidParameterValueException("Specified Service Offering :" + vpxOffering.getUuid() + " NS Vpx cannot be deployed. Min 2GB Ram and 2 CPU are required");
        }

        long userId = CallContext.current().getCallingUserId();
        //TODO change the os bits from 142  103 to the actual guest of bits
        if(template.getGuestOSId() !=  103 ) {
            throw new InvalidParameterValueException("Specified Template " + template.getUuid()+ " not suitable for NS VPX Deployment. Please register the template with guest os type as unknow(64-bit)");
        }

        NetworkVO defaultNetwork = null;
        NetworkVO defaultPublicNetwork = null;
        if (dc.getNetworkType() == NetworkType.Advanced && dc.isSecurityGroupEnabled()) {
            List<NetworkVO> networks = _networkDao.listByZoneSecurityGroup(dataCenterId);
            if (networks == null || networks.size() == 0) {
                throw new CloudRuntimeException("Can not found security enabled network in SG Zone " + dc);
            }
            defaultNetwork = networks.get(0);
        } else {
            TrafficType defaultTrafficType = TrafficType.Management;
            List<NetworkVO> defaultNetworks = _networkDao.listByZoneAndTrafficType(dataCenterId, defaultTrafficType);

            TrafficType publicTrafficType = TrafficType.Public;
            List<NetworkVO> publicNetworks = _networkDao.listByZoneAndTrafficType(dataCenterId, publicTrafficType);

            // api should never allow this situation to happen
            if (defaultNetworks.size() != 1) {
                throw new CloudRuntimeException("Found " + defaultNetworks.size() + " networks of type " + defaultTrafficType + " when expect to find 1");
            }

            if (publicNetworks.size() != 1) {
                throw new CloudRuntimeException("Found " + defaultNetworks.size() + " networks of type " + defaultTrafficType + " when expect to find 1");
            }
            defaultPublicNetwork = publicNetworks.get(0);
            defaultNetwork = defaultNetworks.get(0);
        }

        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>(4);
        NicProfile defaultNic = new NicProfile();
        defaultNic.setDefaultNic(true);
        defaultNic.setDeviceId(0);

        networks.put(_networkMgr.setupNetwork(_accountMgr.getSystemAccount() , _networkOfferingDao.findById(defaultNetwork.getNetworkOfferingId()), plan, null, null, false).get(0), new ArrayList<NicProfile>());

        NicProfile defaultNic1 = new NicProfile();
        defaultNic1.setDefaultNic(false);
        defaultNic1.setDeviceId(1);

        NicProfile defaultNic2 = new NicProfile();
        defaultNic2.setDefaultNic(false);
        defaultNic2.setDeviceId(2);
        defaultNic2.setIPv4Address("");
        defaultNic2.setIPv4Gateway("");
        defaultNic2.setIPv4Netmask("");
        String macAddress = _networkModel.getNextAvailableMacAddressInNetwork(defaultPublicNetwork.getId());
        defaultNic2.setMacAddress(macAddress);

        networks.put(_networkMgr.setupNetwork(_accountMgr.getSystemAccount(), _networkOfferingDao.findByUniqueName(NetworkOffering.SystemPublicNetwork), plan, null, null, false).get(0),
              new ArrayList<NicProfile>(Arrays.asList(defaultNic2)));

        networks.put(_networkMgr.setupNetwork(_accountMgr.getSystemAccount(), _networkOfferingDao.findByUniqueName(NetworkOffering.SystemControlNetwork), plan, null, null, false).get(0),
              new ArrayList<NicProfile>());

        long physicalNetworkId = _networkModel.findPhysicalNetworkId(dataCenterId, _networkOfferingDao.findById(defaultPublicNetwork.getNetworkOfferingId()).getTags(), TrafficType.Public);
        // Validate physical network
        PhysicalNetwork physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Unable to find physical network with id: " + physicalNetworkId + " and tag: "
                  + _networkOfferingDao.findById(defaultPublicNetwork.getNetworkOfferingId()).getTags());
        }
        String guestvnet = physicalNetwork.getVnetString();

        final List<VlanVO> vlans = _vlanDao.listByZone(dataCenterId);
        List<String> pvlan = new ArrayList<String>();
        for (final VlanVO vlan : vlans) {
            pvlan.add(vlan.getVlanTag());
        }

        long netScalerProvider = getNetScalerLbProviderId(physicalNetworkId);
        DomainRouterVO nsVpx = new DomainRouterVO(id, vpxOffering.getId(), netScalerProvider, nxVpxName,
              template.getId(), template.getHypervisorType(),  template.getGuestOSId(), owner.getDomainId(), owner.getId(), userId, false, RedundantState.UNKNOWN, false,
              false, VirtualMachine.Type.NetScalerVm, null);

        nsVpx.setRole(Role.NETSCALER_VM);

        nsVpx = _routerDao.persist(nsVpx);

        VMInstanceVO vmVO= _vmDao.findVMByHostName(nxVpxName);
        _itMgr.allocate(nxVpxName, template, vpxOffering, networks, plan, template.getHypervisorType());
        Map<Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>(1);
        try {
            if (vmVO != null) {
                startNsVpx(vmVO, params);
            } else {
                throw new NullPointerException();
            }
        } catch (StorageUnavailableException e) {
            e.printStackTrace();
        } catch (ConcurrentOperationException e) {
            e.printStackTrace();
        } catch (ResourceUnavailableException e) {
            e.printStackTrace();
        }
        vmVO= _vmDao.findByUuid(nsVpx.getUuid());
        Map<String, Object> deployResponse = new HashMap<String, Object>();
        deployResponse.put("vm", vmVO);
        deployResponse.put("guestvlan", guestvnet);
        deployResponse.put("publicvlan", pvlan);

        return deployResponse;
    }

    protected void startNsVpx(VMInstanceVO nsVpx, Map<Param, Object> params) throws StorageUnavailableException,
    InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        logger.debug("Starting NS Vpx " + nsVpx);
        _itMgr.start(nsVpx.getUuid(), params, null, null);
    }

    @Override
    public Map<String,Object> deployNetscalerServiceVm(DeployNetscalerVpxCmd cmd) {
        DataCenter zone = _dcDao.findById(cmd.getZoneId());
        DeployDestination dest = new DeployDestination(zone, null, null, null);
        VMInstanceVO vmvo = null;
        Map<String,Object> resp = new HashMap<String, Object>();
        Long templateId = cmd.getTemplateId();
        Long serviceOfferingId = cmd.getServiceOfferingId();
        DeploymentPlan plan = new DataCenterDeployment(dest.getDataCenter().getId());

        try {
            resp = deployNsVpx(cmd.getAccount(), dest, plan, serviceOfferingId, templateId);
        } catch (InsufficientCapacityException e) {
            e.printStackTrace();
        }

        return resp;
    }

    protected VirtualRouter stopNetScalerVm(final long vmId, final boolean forced, final Account caller, final long callerUserId) throws ResourceUnavailableException, ConcurrentOperationException {
        final DomainRouterVO netscalerVm = _routerDao.findById(vmId);
        logger.debug("Stopping NetScaler vm " + netscalerVm);

        if (netscalerVm == null || netscalerVm.getRole() != Role.NETSCALER_VM) {
            throw new InvalidParameterValueException("Can't find NetScaler vm by id specified");
        }

        _accountMgr.checkAccess(caller, null, true, netscalerVm);

        try {
            _itMgr.expunge(netscalerVm.getUuid());
            return _routerDao.findById(netscalerVm.getId());
        } catch (final Exception e) {
            throw new CloudRuntimeException("Unable to stop " + netscalerVm, e);
        }
    }

    @Override
    public VirtualRouter stopNetscalerServiceVm(Long id, boolean forced, Account callingAccount, long callingUserId) throws ConcurrentOperationException, ResourceUnavailableException {
        return stopNetScalerVm(id, forced, callingAccount, callingUserId);
    }

    @Override
    public VirtualRouter stopNetScalerVm(Long vmId, boolean forced, Account caller, long callingUserId) {
        final DomainRouterVO netscalerVm = _routerDao.findById(vmId);
        logger.debug("Stopping NetScaler vm " + netscalerVm);

        if (netscalerVm == null || netscalerVm.getRole() != Role.NETSCALER_VM) {
            throw new InvalidParameterValueException("Can't find NetScaler vm by id specified");
        }
        _accountMgr.checkAccess(caller, null, true, netscalerVm);
        try {
            _itMgr.expunge(netscalerVm.getUuid());
            return _routerDao.findById(netscalerVm.getId());
        } catch (final Exception e) {
            throw new CloudRuntimeException("Unable to stop " + netscalerVm, e);
        }
    }
}
