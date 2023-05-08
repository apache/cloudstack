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
package com.cloud.network;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.ExternalLoadBalancerResponse;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupExternalLoadBalancerCommand;
import com.cloud.agent.api.routing.CreateLoadBalancerApplianceCommand;
import com.cloud.agent.api.routing.DestroyLoadBalancerApplianceCommand;
import com.cloud.agent.api.routing.HealthCheckLBConfigAnswer;
import com.cloud.agent.api.routing.HealthCheckLBConfigCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.configuration.Config;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.ExternalFirewallDeviceDao;
import com.cloud.network.dao.ExternalLoadBalancerDeviceDao;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO.LBDeviceAllocationState;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO.LBDeviceState;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.InlineLoadBalancerNicMapDao;
import com.cloud.network.dao.InlineLoadBalancerNicMapVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkExternalFirewallDao;
import com.cloud.network.dao.NetworkExternalLoadBalancerDao;
import com.cloud.network.dao.NetworkExternalLoadBalancerVO;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.resource.CreateLoadBalancerApplianceAnswer;
import com.cloud.network.resource.DestroyLoadBalancerApplianceAnswer;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.rules.StaticNatImpl;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.net.UrlUtil;
import com.cloud.vm.Nic;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public abstract class ExternalLoadBalancerDeviceManagerImpl extends AdapterBase implements ExternalLoadBalancerDeviceManager, ResourceStateAdapter {

    @Inject
    NetworkExternalLoadBalancerDao _networkExternalLBDao;
    @Inject
    ExternalLoadBalancerDeviceDao _externalLoadBalancerDeviceDao;
    @Inject
    HostDao _hostDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    InlineLoadBalancerNicMapDao _inlineLoadBalancerNicMapDao;
    @Inject
    NicDao _nicDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    UserStatisticsDao _userStatsDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    LoadBalancerDao _loadBalancerDao;
    @Inject
    PortForwardingRulesDao _portForwardingRulesDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    HostDetailsDao _hostDetailDao;
    @Inject
    NetworkExternalLoadBalancerDao _networkLBDao;
    @Inject
    NetworkServiceMapDao _ntwkSrvcProviderDao;
    @Inject
    NetworkExternalFirewallDao _networkExternalFirewallDao;
    @Inject
    ExternalFirewallDeviceDao _externalFirewallDeviceDao;
    @Inject
    protected HostPodDao _podDao = null;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    protected
    VirtualMachineManager _itMgr;
    @Inject
    VMInstanceDao _vmDao;
    @Inject
    VMTemplateDao _templateDao;
    @Inject
    ServiceOfferingDao _serviceOfferingDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalProviderDao;
    @Inject
    VirtualRouterProviderDao _vrProviderDao;
    @Inject
    private LoadBalancingRulesManager lbRulesManager;

    private long _defaultLbCapacity;
    private static final org.apache.log4j.Logger s_logger = Logger.getLogger(ExternalLoadBalancerDeviceManagerImpl.class);

    @Override
    @DB
    public ExternalLoadBalancerDeviceVO addExternalLoadBalancer(long physicalNetworkId, String url, String username, String password, final String deviceName,
        ServerResource resource, final boolean gslbProvider, final boolean exclusiveGslbProivider,
        final String gslbSitePublicIp, final String gslbSitePrivateIp) {

        PhysicalNetworkVO pNetwork = null;
        final NetworkDevice ntwkDevice = NetworkDevice.getNetworkDevice(deviceName);
        long zoneId;

        if ((ntwkDevice == null) || (url == null) || (username == null) || (resource == null) || (password == null)) {
            throw new InvalidParameterValueException("Atleast one of the required parameters (url, username, password,"
                + " server resource, zone id/physical network id) is not specified or a valid parameter.");
        }

        pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (pNetwork == null) {
            throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
        }

        zoneId = pNetwork.getDataCenterId();
        PhysicalNetworkServiceProviderVO ntwkSvcProvider =
            _physicalNetworkServiceProviderDao.findByServiceProvider(pNetwork.getId(), ntwkDevice.getNetworkServiceProvder());

        ntwkSvcProvider = _physicalNetworkServiceProviderDao.findByServiceProvider(pNetwork.getId(), ntwkDevice.getNetworkServiceProvder());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkDevice.getNetworkServiceProvder() + " is not enabled in the physical network: " +
                physicalNetworkId + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() + " is in shutdown state in the physical network: " +
                physicalNetworkId + "to add this device");
        }

        if (gslbProvider) {
            ExternalLoadBalancerDeviceVO zoneGslbProvider =
                _externalLoadBalancerDeviceDao.findGslbServiceProvider(physicalNetworkId, ntwkDevice.getNetworkServiceProvder());
            if (zoneGslbProvider != null) {
                throw new CloudRuntimeException("There is a GSLB service provider configured in the zone already.");
            }
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            s_logger.debug(e);
            throw new InvalidParameterValueException(e.getMessage());
        }

        String ipAddress = uri.getHost();
        Map hostDetails = new HashMap<String, String>();
        String hostName = getExternalLoadBalancerResourceGuid(pNetwork.getId(), deviceName, ipAddress);
        hostDetails.put("name", hostName);
        hostDetails.put("guid", UUID.randomUUID().toString());
        hostDetails.put("zoneId", String.valueOf(pNetwork.getDataCenterId()));
        hostDetails.put("ip", ipAddress);
        hostDetails.put("physicalNetworkId", String.valueOf(pNetwork.getId()));
        hostDetails.put("username", username);
        hostDetails.put("password", password);
        hostDetails.put("deviceName", deviceName);

        // leave parameter validation to be part server resource configure
        Map<String, String> configParams = new HashMap<String, String>();
        UrlUtil.parseQueryParameters(uri.getQuery(), false, configParams);
        hostDetails.putAll(configParams);

        try {
            resource.configure(hostName, hostDetails);

            final Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.ExternalLoadBalancer, hostDetails);
            if (host != null) {

                final boolean dedicatedUse =
                    (configParams.get(ApiConstants.LOAD_BALANCER_DEVICE_DEDICATED) != null) ? Boolean.parseBoolean(configParams.get(ApiConstants.LOAD_BALANCER_DEVICE_DEDICATED))
                        : false;
                long capacity = NumbersUtil.parseLong(configParams.get(ApiConstants.LOAD_BALANCER_DEVICE_CAPACITY), 0);
                if (capacity == 0) {
                    capacity = _defaultLbCapacity;
                }

                final long capacityFinal = capacity;
                final PhysicalNetworkVO pNetworkFinal = pNetwork;
                return Transaction.execute(new TransactionCallback<ExternalLoadBalancerDeviceVO>() {
                    @Override
                    public ExternalLoadBalancerDeviceVO doInTransaction(TransactionStatus status) {
                        ExternalLoadBalancerDeviceVO lbDeviceVO =
                            new ExternalLoadBalancerDeviceVO(host.getId(), pNetworkFinal.getId(), ntwkDevice.getNetworkServiceProvder(), deviceName, capacityFinal,
                                dedicatedUse, gslbProvider);
                        if (gslbProvider) {
                            lbDeviceVO.setGslbSitePublicIP(gslbSitePublicIp);
                            lbDeviceVO.setGslbSitePrivateIP(gslbSitePrivateIp);
                            lbDeviceVO.setExclusiveGslbProvider(exclusiveGslbProivider);
                        }
                        _externalLoadBalancerDeviceDao.persist(lbDeviceVO);
                        DetailVO hostDetail = new DetailVO(host.getId(), ApiConstants.LOAD_BALANCER_DEVICE_ID, String.valueOf(lbDeviceVO.getId()));
                        _hostDetailDao.persist(hostDetail);

                        return lbDeviceVO;
                    }
                });
            } else {
                throw new CloudRuntimeException("Failed to add load balancer device due to internal error.");
            }
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public boolean deleteExternalLoadBalancer(long hostId) {
        HostVO externalLoadBalancer = _hostDao.findById(hostId);
        if (externalLoadBalancer == null) {
            throw new InvalidParameterValueException("Could not find an external load balancer with ID: " + hostId);
        }

        DetailVO lbHostDetails = _hostDetailDao.findDetail(hostId, ApiConstants.LOAD_BALANCER_DEVICE_ID);
        long lbDeviceId = Long.parseLong(lbHostDetails.getValue());

        ExternalLoadBalancerDeviceVO lbDeviceVo = _externalLoadBalancerDeviceDao.findById(lbDeviceId);
        if (lbDeviceVo.getAllocationState() == LBDeviceAllocationState.Provider) {
            // check if cloudstack has provisioned any load balancer appliance on the device before deleting
            List<ExternalLoadBalancerDeviceVO> lbDevices = _externalLoadBalancerDeviceDao.listAll();
            if (lbDevices != null) {
                for (ExternalLoadBalancerDeviceVO lbDevice : lbDevices) {
                    if (lbDevice.getParentHostId() == hostId) {
                        throw new CloudRuntimeException(
                            "This load balancer device can not be deleted as there are one or more load balancers applainces provisioned by cloudstack on the device.");
                    }
                }
            }
        } else {
            // check if any networks are using this load balancer device
            List<NetworkExternalLoadBalancerVO> networks = _networkLBDao.listByLoadBalancerDeviceId(lbDeviceId);
            if ((networks != null) && !networks.isEmpty()) {
                throw new CloudRuntimeException("Delete can not be done as there are networks using this load balancer device ");
            }
        }

        try {
            // put the host in maintenance state in order for it to be deleted
            externalLoadBalancer.setResourceState(ResourceState.Maintenance);
            _hostDao.update(hostId, externalLoadBalancer);
            _resourceMgr.deleteHost(hostId, false, false);

            // delete the external load balancer entry
            _externalLoadBalancerDeviceDao.remove(lbDeviceId);

            return true;
        } catch (Exception e) {
            s_logger.debug(e);
            return false;
        }
    }

    @Override
    public List<Host> listExternalLoadBalancers(long physicalNetworkId, String deviceName) {
        List<Host> lbHosts = new ArrayList<Host>();
        NetworkDevice lbNetworkDevice = NetworkDevice.getNetworkDevice(deviceName);
        PhysicalNetworkVO pNetwork = null;

        pNetwork = _physicalNetworkDao.findById(physicalNetworkId);

        if ((pNetwork == null) || (lbNetworkDevice == null)) {
            throw new InvalidParameterValueException("Atleast one of the required parameter physical networkId, device name is invalid.");
        }

        PhysicalNetworkServiceProviderVO ntwkSvcProvider =
            _physicalNetworkServiceProviderDao.findByServiceProvider(pNetwork.getId(), lbNetworkDevice.getNetworkServiceProvder());
        // if provider not configured in to physical network, then there can be no instances
        if (ntwkSvcProvider == null) {
            return null;
        }

        List<ExternalLoadBalancerDeviceVO> lbDevices =
            _externalLoadBalancerDeviceDao.listByPhysicalNetworkAndProvider(physicalNetworkId, ntwkSvcProvider.getProviderName());
        for (ExternalLoadBalancerDeviceVO provderInstance : lbDevices) {
            lbHosts.add(_hostDao.findById(provderInstance.getHostId()));
        }
        return lbHosts;
    }

    public ExternalLoadBalancerResponse createExternalLoadBalancerResponse(Host externalLoadBalancer) {
        Map<String, String> lbDetails = _hostDetailDao.findDetails(externalLoadBalancer.getId());
        ExternalLoadBalancerResponse response = new ExternalLoadBalancerResponse();
        response.setId(externalLoadBalancer.getUuid());
        response.setIpAddress(externalLoadBalancer.getPrivateIpAddress());
        response.setUsername(lbDetails.get("username"));
        response.setPublicInterface(lbDetails.get("publicInterface"));
        response.setPrivateInterface(lbDetails.get("privateInterface"));
        response.setNumRetries(lbDetails.get("numRetries"));
        return response;
    }

    public String getExternalLoadBalancerResourceGuid(long physicalNetworkId, String deviceName, String ip) {
        return physicalNetworkId + "-" + deviceName + "-" + ip;
    }

    @Override
    public ExternalLoadBalancerDeviceVO getExternalLoadBalancerForNetwork(Network network) {
        NetworkExternalLoadBalancerVO lbDeviceForNetwork = _networkExternalLBDao.findByNetworkId(network.getId());
        if (lbDeviceForNetwork != null) {
            long lbDeviceId = lbDeviceForNetwork.getExternalLBDeviceId();
            ExternalLoadBalancerDeviceVO lbDeviceVo = _externalLoadBalancerDeviceDao.findById(lbDeviceId);
            assert (lbDeviceVo != null);
            return lbDeviceVo;
        }
        return null;
    }

    public void setExternalLoadBalancerForNetwork(Network network, long externalLBDeviceID) {
        NetworkExternalLoadBalancerVO lbDeviceForNetwork = new NetworkExternalLoadBalancerVO(network.getId(), externalLBDeviceID);
        _networkExternalLBDao.persist(lbDeviceForNetwork);
    }

    @DB
    protected ExternalLoadBalancerDeviceVO allocateLoadBalancerForNetwork(final Network guestConfig) throws InsufficientCapacityException {
        boolean retry = true;
        boolean tryLbProvisioning = false;
        ExternalLoadBalancerDeviceVO lbDevice = null;
        long physicalNetworkId = guestConfig.getPhysicalNetworkId();
        NetworkOfferingVO offering = _networkOfferingDao.findById(guestConfig.getNetworkOfferingId());
        String provider = _ntwkSrvcProviderDao.getProviderForServiceInNetwork(guestConfig.getId(), Service.Lb);

        while (retry) {
            GlobalLock deviceMapLock = GlobalLock.getInternLock("LoadBalancerAllocLock");
            try {
                if (deviceMapLock.lock(120)) {
                    try {
                        final boolean dedicatedLB = offering.isDedicatedLB(); // does network offering supports a dedicated load balancer?

                        try {
                            lbDevice = Transaction.execute(new TransactionCallbackWithException<ExternalLoadBalancerDeviceVO, InsufficientCapacityException>() {
                                @Override
                                public ExternalLoadBalancerDeviceVO doInTransaction(TransactionStatus status) throws InsufficientCapacityException {
                                    // FIXME: should the device allocation be done during network implement phase or do a
                                    // lazy allocation when first rule for the network is configured??

                                    // find a load balancer device for this network as per the network offering
                                    ExternalLoadBalancerDeviceVO lbDevice = findSuitableLoadBalancerForNetwork(guestConfig, dedicatedLB);
                                    long lbDeviceId = lbDevice.getId();

                                    // persist the load balancer device id that will be used for this network. Once a network
                                    // is implemented on a LB device then later on all rules will be programmed on to same device
                                    NetworkExternalLoadBalancerVO networkLB = new NetworkExternalLoadBalancerVO(guestConfig.getId(), lbDeviceId);
                                    _networkExternalLBDao.persist(networkLB);

                                    // mark device to be either dedicated or shared use
                                    lbDevice.setAllocationState(dedicatedLB ? LBDeviceAllocationState.Dedicated : LBDeviceAllocationState.Shared);
                                    _externalLoadBalancerDeviceDao.update(lbDeviceId, lbDevice);
                                    return lbDevice;
                                }
                            });

                            // allocated load balancer for the network, so skip retry
                            tryLbProvisioning = false;
                            retry = false;
                        } catch (InsufficientCapacityException exception) {
                            // if already attempted to provision load balancer then throw out of capacity exception,
                            if (tryLbProvisioning) {
                                retry = false;
                                // TODO: throwing warning instead of error for now as its possible another provider can service this network
                                s_logger.warn("There are no load balancer device with the capacity for implementing this network");
                                throw exception;
                            } else {
                                tryLbProvisioning = true; // if possible provision a LB appliance in to the physical network
                            }
                        }
                    } finally {
                        deviceMapLock.unlock();
                    }
                }
            } finally {
                deviceMapLock.releaseRef();
            }

            // there are no LB devices or there is no free capacity on the devices in the physical network so provision a new LB appliance
            if (tryLbProvisioning) {
                // check if LB appliance can be dynamically provisioned
                List<ExternalLoadBalancerDeviceVO> providerLbDevices =
                    _externalLoadBalancerDeviceDao.listByProviderAndDeviceAllocationState(physicalNetworkId, provider, LBDeviceAllocationState.Provider);
                if ((providerLbDevices != null) && (!providerLbDevices.isEmpty())) {
                    for (ExternalLoadBalancerDeviceVO lbProviderDevice : providerLbDevices) {
                        if (lbProviderDevice.getState() == LBDeviceState.Enabled) {
                            // acquire a private IP from the data center which will be used as management IP of provisioned LB appliance,
                            DataCenterIpAddressVO dcPrivateIp = _dcDao.allocatePrivateIpAddress(guestConfig.getDataCenterId(), lbProviderDevice.getUuid());
                            if (dcPrivateIp == null) {
                                throw new InsufficientNetworkCapacityException("failed to acquire a priavate IP in the zone " + guestConfig.getDataCenterId() +
                                    " needed for management IP of the load balancer appliance", DataCenter.class, guestConfig.getDataCenterId());
                            }
                            Pod pod = _podDao.findById(dcPrivateIp.getPodId());
                            String lbIP = dcPrivateIp.getIpAddress();
                            String netmask = NetUtils.getCidrNetmask(pod.getCidrSize());
                            String gateway = pod.getGateway();

                            // send CreateLoadBalancerApplianceCommand to the host capable of provisioning
                            CreateLoadBalancerApplianceCommand lbProvisionCmd = new CreateLoadBalancerApplianceCommand(lbIP, netmask, gateway);
                            CreateLoadBalancerApplianceAnswer createLbAnswer = null;
                            try {
                                createLbAnswer = (CreateLoadBalancerApplianceAnswer)_agentMgr.easySend(lbProviderDevice.getHostId(), lbProvisionCmd);
                                if (createLbAnswer == null || !createLbAnswer.getResult()) {
                                    s_logger.error("Could not provision load balancer instance on the load balancer device " + lbProviderDevice.getId());
                                    continue;
                                }
                            } catch (Exception agentException) {
                                s_logger.error("Could not provision load balancer instance on the load balancer device " + lbProviderDevice.getId() + " due to " +
                                    agentException.getMessage());
                                continue;
                            }

                            String username = createLbAnswer.getUsername();
                            String password = createLbAnswer.getPassword();
                            String publicIf = createLbAnswer.getPublicInterface();
                            String privateIf = createLbAnswer.getPrivateInterface();

                            // we have provisioned load balancer so add the appliance as cloudstack provisioned external load balancer
                            String dedicatedLb = offering.isDedicatedLB() ? "true" : "false";
                            String capacity = Long.toString(lbProviderDevice.getCapacity());

                            // acquire a public IP to associate with lb appliance (used as subnet IP to make the appliance part of private network)
                            PublicIp publicIp =
                                _ipAddrMgr.assignPublicIpAddress(guestConfig.getDataCenterId(), null, _accountMgr.getSystemAccount(), VlanType.VirtualNetwork, null,
                                    null, false, false);
                            String publicIPNetmask = publicIp.getVlanNetmask();
                            String publicIPgateway = publicIp.getVlanGateway();
                            String publicIP = publicIp.getAddress().toString();
                            String publicIPVlanTag="";
                            try {
                                publicIPVlanTag = BroadcastDomainType.getValue(publicIp.getVlanTag());
                            } catch (URISyntaxException e) {
                                s_logger.error("Failed to parse public ip vlan tag" + e.getMessage());
                            }

                            String url =
                                "https://" + lbIP + "?publicinterface=" + publicIf + "&privateinterface=" + privateIf + "&lbdevicededicated=" + dedicatedLb +
                                    "&cloudmanaged=true" + "&publicip=" + publicIP + "&publicipnetmask=" + publicIPNetmask + "&lbdevicecapacity=" + capacity +
                                    "&publicipvlan=" + publicIPVlanTag + "&publicipgateway=" + publicIPgateway;
                            ExternalLoadBalancerDeviceVO lbAppliance = null;
                            try {
                                lbAppliance =
                                    addExternalLoadBalancer(physicalNetworkId, url, username, password, createLbAnswer.getDeviceName(),
                                        createLbAnswer.getServerResource(), false, false, null, null);
                            } catch (Exception e) {
                                s_logger.error("Failed to add load balancer appliance in to cloudstack due to " + e.getMessage() +
                                    ". So provisioned load balancer appliance will be destroyed.");
                            }

                            if (lbAppliance != null) {
                                // mark the load balancer as cloudstack managed and set parent host id on which lb appliance is provisioned
                                ExternalLoadBalancerDeviceVO managedLb = _externalLoadBalancerDeviceDao.findById(lbAppliance.getId());
                                managedLb.setIsManagedDevice(true);
                                managedLb.setParentHostId(lbProviderDevice.getHostId());
                                _externalLoadBalancerDeviceDao.update(lbAppliance.getId(), managedLb);
                            } else {
                                // failed to add the provisioned load balancer into cloudstack so destroy the appliance
                                DestroyLoadBalancerApplianceCommand lbDeleteCmd = new DestroyLoadBalancerApplianceCommand(lbIP);
                                DestroyLoadBalancerApplianceAnswer answer = null;
                                try {
                                    answer = (DestroyLoadBalancerApplianceAnswer)_agentMgr.easySend(lbProviderDevice.getHostId(), lbDeleteCmd);
                                    if (answer == null || !answer.getResult()) {
                                        s_logger.warn("Failed to destroy load balancer appliance created");
                                    } else {
                                        // release the public & private IP back to dc pool, as the load balancer appliance is now destroyed
                                        _dcDao.releasePrivateIpAddress(lbIP, guestConfig.getDataCenterId(), null);
                                        _ipAddrMgr.disassociatePublicIpAddress(publicIp.getId(), _accountMgr.getSystemUser().getId(), _accountMgr.getSystemAccount());
                                    }
                                } catch (Exception e) {
                                    s_logger.warn("Failed to destroy load balancer appliance created for the network" + guestConfig.getId() + " due to " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        }

        return lbDevice;
    }

    @Override
    public ExternalLoadBalancerDeviceVO findSuitableLoadBalancerForNetwork(Network network, boolean dedicatedLb) throws InsufficientCapacityException {
        long physicalNetworkId = network.getPhysicalNetworkId();
        List<ExternalLoadBalancerDeviceVO> lbDevices = null;
        String provider = _ntwkSrvcProviderDao.getProviderForServiceInNetwork(network.getId(), Service.Lb);
        assert (provider != null);

        if (dedicatedLb) {
            lbDevices = _externalLoadBalancerDeviceDao.listByProviderAndDeviceAllocationState(physicalNetworkId, provider, LBDeviceAllocationState.Free);
            if (lbDevices != null && !lbDevices.isEmpty()) {
                // return first device that is free, fully configured and meant for dedicated use
                for (ExternalLoadBalancerDeviceVO lbdevice : lbDevices) {
                    if (lbdevice.getState() == LBDeviceState.Enabled && lbdevice.getIsDedicatedDevice()) {
                        return lbdevice;
                    }
                }
            }
        } else {
            // get the LB devices that are already allocated for shared use
            lbDevices = _externalLoadBalancerDeviceDao.listByProviderAndDeviceAllocationState(physicalNetworkId, provider, LBDeviceAllocationState.Shared);

            if (lbDevices != null) {

                ExternalLoadBalancerDeviceVO maxFreeCapacityLbdevice = null;
                long maxFreeCapacity = 0;

                // loop through the LB device in the physical network and pick the one with maximum free capacity
                for (ExternalLoadBalancerDeviceVO lbdevice : lbDevices) {

                    // skip if device is not enabled
                    if (lbdevice.getState() != LBDeviceState.Enabled) {
                        continue;
                    }

                    // get the used capacity from the list of guest networks that are mapped to this load balancer
                    List<NetworkExternalLoadBalancerVO> mappedNetworks = _networkExternalLBDao.listByLoadBalancerDeviceId(lbdevice.getId());
                    long usedCapacity = ((mappedNetworks == null) || (mappedNetworks.isEmpty())) ? 0 : mappedNetworks.size();

                    // get the configured capacity for this device
                    long fullCapacity = lbdevice.getCapacity();
                    if (fullCapacity == 0) {
                        fullCapacity = _defaultLbCapacity; // if capacity not configured then use the default
                    }

                    long freeCapacity = fullCapacity - usedCapacity;
                    if (freeCapacity > 0) {
                        if (maxFreeCapacityLbdevice == null) {
                            maxFreeCapacityLbdevice = lbdevice;
                            maxFreeCapacity = freeCapacity;
                        } else if (freeCapacity > maxFreeCapacity) {
                            maxFreeCapacityLbdevice = lbdevice;
                            maxFreeCapacity = freeCapacity;
                        }
                    }
                }

                // return the device with maximum free capacity and is meant for shared use
                if (maxFreeCapacityLbdevice != null) {
                    return maxFreeCapacityLbdevice;
                }
            }

            // if we are here then there are no existing LB devices in shared use or the devices in shared use has no
// free capacity left
            // so allocate a new load balancer configured for shared use from the pool of free LB devices
            lbDevices = _externalLoadBalancerDeviceDao.listByProviderAndDeviceAllocationState(physicalNetworkId, provider, LBDeviceAllocationState.Free);
            if (lbDevices != null && !lbDevices.isEmpty()) {
                for (ExternalLoadBalancerDeviceVO lbdevice : lbDevices) {
                    if (lbdevice.getState() == LBDeviceState.Enabled && !lbdevice.getIsDedicatedDevice()) {
                        return lbdevice;
                    }
                }
            }
        }

        // there are no devices which capacity
        throw new InsufficientNetworkCapacityException("Unable to find a load balancing provider with sufficient capcity " + " to implement the network", Network.class,
            network.getId());
    }

    @DB
    protected boolean freeLoadBalancerForNetwork(final Network guestConfig) {
        GlobalLock deviceMapLock = GlobalLock.getInternLock("LoadBalancerAllocLock");

        try {
            if (deviceMapLock.lock(120)) {
                ExternalLoadBalancerDeviceVO lbDevice = Transaction.execute(new TransactionCallback<ExternalLoadBalancerDeviceVO>() {
                    @Override
                    public ExternalLoadBalancerDeviceVO doInTransaction(TransactionStatus status) {
                        // since network is shutdown remove the network mapping to the load balancer device
                        NetworkExternalLoadBalancerVO networkLBDevice = _networkExternalLBDao.findByNetworkId(guestConfig.getId());
                        long lbDeviceId = networkLBDevice.getExternalLBDeviceId();
                        _networkExternalLBDao.remove(networkLBDevice.getId());

                        List<NetworkExternalLoadBalancerVO> ntwksMapped = _networkExternalLBDao.listByLoadBalancerDeviceId(networkLBDevice.getExternalLBDeviceId());
                        ExternalLoadBalancerDeviceVO lbDevice = _externalLoadBalancerDeviceDao.findById(lbDeviceId);
                        boolean lbInUse = !(ntwksMapped == null || ntwksMapped.isEmpty());
                        boolean lbCloudManaged = lbDevice.getIsManagedDevice();

                        if (!lbInUse && !lbCloudManaged) {
                            // this is the last network mapped to the load balancer device so set device allocation state to be free
                            lbDevice.setAllocationState(LBDeviceAllocationState.Free);
                            _externalLoadBalancerDeviceDao.update(lbDevice.getId(), lbDevice);
                        }

                        // commit the changes before sending agent command to destroy cloudstack managed LB
                        if (!lbInUse && lbCloudManaged) {
                            return lbDevice;
                        } else {
                            return null;
                        }
                    }
                });

                if (lbDevice != null) {
                    // send DestroyLoadBalancerApplianceCommand to the host where load balancer appliance is provisioned
                    Host lbHost = _hostDao.findById(lbDevice.getHostId());
                    String lbIP = lbHost.getPrivateIpAddress();
                    DestroyLoadBalancerApplianceCommand lbDeleteCmd = new DestroyLoadBalancerApplianceCommand(lbIP);
                    DestroyLoadBalancerApplianceAnswer answer = null;
                    try {
                        answer = (DestroyLoadBalancerApplianceAnswer)_agentMgr.easySend(lbDevice.getParentHostId(), lbDeleteCmd);
                        if (answer == null) {
                            s_logger.warn(String.format("Failed to destroy load balancer appliance used by the network [%s] due to a communication error with agent.", guestConfig.getId()));
                        } else if (!answer.getResult()) {
                            s_logger.warn(String.format("Failed to destroy load balancer appliance used by the network [%s] due to [%s].", guestConfig.getId(),  answer.getDetails()));
                        }
                    } catch (Exception e) {
                        s_logger.warn("Failed to destroy load balancer appliance used by the network" + guestConfig.getId() + " due to " + e.getMessage());
                    }

                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully destroyed load balancer appliance used for the network" + guestConfig.getId());
                    }
                    deviceMapLock.unlock();

                    // remove the provisioned load balancer appliance from cloudstack
                    deleteExternalLoadBalancer(lbHost.getId());

                    // release the private IP back to dc pool, as the load balancer appliance is now destroyed
                    _dcDao.releasePrivateIpAddress(lbHost.getPrivateIpAddress(), guestConfig.getDataCenterId(), null);

                    // release the public IP allocated for this LB appliance
                    DetailVO publicIpDetail = _hostDetailDao.findDetail(lbHost.getId(), "publicip");
                    IPAddressVO ipVo = _ipAddressDao.findByIpAndDcId(guestConfig.getDataCenterId(), publicIpDetail.toString());
                    _ipAddrMgr.disassociatePublicIpAddress(ipVo.getId(), _accountMgr.getSystemUser().getId(), _accountMgr.getSystemAccount());
                } else {
                    deviceMapLock.unlock();
                }

                return true;
            } else {
                s_logger.error("Failed to release load balancer device for the network" + guestConfig.getId() + "as failed to acquire lock ");
                return false;
            }
        } catch (Exception exception) {
            s_logger.error("Failed to release load balancer device for the network" + guestConfig.getId() + " due to " + exception.getMessage());
        } finally {
            deviceMapLock.releaseRef();
        }

        return false;
    }

    private void applyStaticNatRuleForInlineLBRule(DataCenterVO zone, Network network, boolean revoked, String publicIp, String privateIp)
        throws ResourceUnavailableException {
        List<StaticNat> staticNats = new ArrayList<StaticNat>();
        IPAddressVO ipVO = _ipAddressDao.listByDcIdIpAddress(zone.getId(), publicIp).get(0);
        StaticNatImpl staticNat = new StaticNatImpl(ipVO.getAllocatedToAccountId(), ipVO.getAllocatedInDomainId(), network.getId(), ipVO.getId(), privateIp, revoked);
        staticNats.add(staticNat);
        StaticNatServiceProvider element = _networkMgr.getStaticNatProviderForNetwork(network);
        element.applyStaticNats(network, staticNats);
    }

    private enum MappingState {
        Create, Remove, Unchanged,
    };

    private class MappingNic {
        private Nic nic;
        private MappingState state;

        public Nic getNic() {
            return nic;
        }

        public void setNic(Nic nic) {
            this.nic = nic;
        }

        public MappingState getState() {
            return state;
        }

        public void setState(MappingState state) {
            this.state = state;
        }
    };

    private MappingNic getLoadBalancingIpNic(DataCenterVO zone, Network network, long sourceIpId, boolean revoked, String existedGuestIp)
        throws ResourceUnavailableException {
        String srcIp = _networkModel.getIp(sourceIpId).getAddress().addr();
        InlineLoadBalancerNicMapVO mapping = _inlineLoadBalancerNicMapDao.findByPublicIpAddress(srcIp);
        Nic loadBalancingIpNic = null;
        MappingNic nic = new MappingNic();
        nic.setState(MappingState.Unchanged);
        if (!revoked) {
            if (mapping == null) {
                // Acquire a new guest IP address and save it as the load balancing IP address
                String loadBalancingIpAddress = existedGuestIp;

                if (loadBalancingIpAddress == null) {
                    if (network.getGuestType() == Network.GuestType.Isolated) {
                        loadBalancingIpAddress = _ipAddrMgr.acquireGuestIpAddress(network, null);
                    } else if (network.getGuestType() == Network.GuestType.Shared) {
                        try {
                            PublicIp directIp =
                                _ipAddrMgr.assignPublicIpAddress(network.getDataCenterId(), null, _accountDao.findById(network.getAccountId()), VlanType.DirectAttached,
                                    network.getId(), null, true, false);
                            loadBalancingIpAddress = directIp.getAddress().addr();
                        } catch (InsufficientCapacityException capException) {
                            String msg = "Ran out of guest IP addresses from the shared network.";
                            s_logger.error(msg);
                            throw new ResourceUnavailableException(msg, DataCenter.class, network.getDataCenterId());
                        }
                    }
                }

                if (loadBalancingIpAddress == null) {
                    String msg = "Ran out of guest IP addresses.";
                    s_logger.error(msg);
                    throw new ResourceUnavailableException(msg, DataCenter.class, network.getDataCenterId());
                }

                // If a NIC doesn't exist for the load balancing IP address, create one
                loadBalancingIpNic = _nicDao.findByIp4AddressAndNetworkId(loadBalancingIpAddress, network.getId());
                if (loadBalancingIpNic == null) {
                    loadBalancingIpNic = _networkMgr.savePlaceholderNic(network, loadBalancingIpAddress, null, null);
                }

                // Save a mapping between the source IP address and the load balancing IP address NIC
                mapping = new InlineLoadBalancerNicMapVO(srcIp, loadBalancingIpNic.getId());
                _inlineLoadBalancerNicMapDao.persist(mapping);

                // On the firewall provider for the network, create a static NAT rule between the source IP
                // address and the load balancing IP address
                try {
                    applyStaticNatRuleForInlineLBRule(zone, network, revoked, srcIp, loadBalancingIpNic.getIPv4Address());
                } catch (ResourceUnavailableException ex) {
                    // Rollback db operation
                    _inlineLoadBalancerNicMapDao.expunge(mapping.getId());
                    _nicDao.expunge(loadBalancingIpNic.getId());
                    throw ex;
                }

                s_logger.debug("Created static nat rule for inline load balancer");
                nic.setState(MappingState.Create);
            } else {
                loadBalancingIpNic = _nicDao.findById(mapping.getNicId());
            }
        } else {
            if (mapping != null) {
                // Find the NIC that the mapping refers to
                loadBalancingIpNic = _nicDao.findById(mapping.getNicId());

                int count = _ipAddrMgr.getRuleCountForIp(sourceIpId, Purpose.LoadBalancing, FirewallRule.State.Active);
                if (count == 0) {
                    // On the firewall provider for the network, delete the static NAT rule between the source IP
                    // address and the load balancing IP address
                    applyStaticNatRuleForInlineLBRule(zone, network, revoked, srcIp, loadBalancingIpNic.getIPv4Address());

                    // Delete the mapping between the source IP address and the load balancing IP address
                    _inlineLoadBalancerNicMapDao.expunge(mapping.getId());

                    // Delete the NIC
                    _nicDao.expunge(loadBalancingIpNic.getId());

                    s_logger.debug("Revoked static nat rule for inline load balancer");
                    nic.setState(MappingState.Remove);
                }
            } else {
                s_logger.debug("Revoking a rule for an inline load balancer that has not been programmed yet.");
                nic.setNic(null);
                return nic;
            }
        }

        nic.setNic(loadBalancingIpNic);
        return nic;
    }

    public boolean isNccServiceProvider(Network network) {
        NetworkOffering networkOffering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        if(null!= networkOffering && networkOffering.getServicePackage() != null ) {
            return true;
        }
        else {
            return false;
        }
    }

    public HostVO getNetScalerControlCenterForNetwork(Network guestConfig) {
        long zoneId = guestConfig.getDataCenterId();
        return _hostDao.findByTypeNameAndZoneId(zoneId, "NetscalerControlCenter", Type.NetScalerControlCenter);
    }

    @Override
    public boolean applyLoadBalancerRules(Network network, List<LoadBalancingRule> loadBalancingRules) throws ResourceUnavailableException {
        // Find the external load balancer in this zone
        long zoneId = network.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);

        if (loadBalancingRules == null || loadBalancingRules.isEmpty()) {
            return true;
        }

        HostVO externalLoadBalancer = null;

        if(isNccServiceProvider(network)) {
            externalLoadBalancer  = getNetScalerControlCenterForNetwork(network);
        } else {
            ExternalLoadBalancerDeviceVO lbDeviceVO = getExternalLoadBalancerForNetwork(network);
            if (lbDeviceVO == null) {
                s_logger.warn("There is no external load balancer device assigned to this network either network is not implement are already shutdown so just returning");
                return true;
            } else {
                externalLoadBalancer = _hostDao.findById(lbDeviceVO.getHostId());
            }
        }



        boolean externalLoadBalancerIsInline = _networkMgr.isNetworkInlineMode(network);

        if (network.getState() == Network.State.Allocated) {
            s_logger.debug("External load balancer was asked to apply LB rules for network with ID " + network.getId() +
                "; this network is not implemented. Skipping backend commands.");
            return true;
        }

        List<LoadBalancerTO> loadBalancersToApply = new ArrayList<LoadBalancerTO>();
        List<MappingState> mappingStates = new ArrayList<MappingState>();
        for (int i = 0; i < loadBalancingRules.size(); i++) {
            LoadBalancingRule rule = loadBalancingRules.get(i);

            boolean revoked = (rule.getState().equals(FirewallRule.State.Revoke));
            String protocol = rule.getProtocol();
            String algorithm = rule.getAlgorithm();
            String uuid = rule.getUuid();
            String srcIp = rule.getSourceIp().addr();
            String srcIpVlan = null;
            String srcIpGateway = null;
            String srcIpNetmask = null;
            Long vlanid =  _networkModel.getPublicIpAddress(rule.getSourceIp().addr(), network.getDataCenterId()).getVlanId();
            if(vlanid != null ) {
              VlanVO publicVlan =   _vlanDao.findById(vlanid);
              srcIpVlan =  publicVlan.getVlanTag();
              srcIpGateway = publicVlan.getVlanGateway();
              srcIpNetmask = publicVlan.getVlanNetmask();
            }
            int srcPort = rule.getSourcePortStart();
            List<LbDestination> destinations = rule.getDestinations();

            if (externalLoadBalancerIsInline) {
                long ipId = _networkModel.getPublicIpAddress(rule.getSourceIp().addr(), network.getDataCenterId()).getId();
                MappingNic nic = getLoadBalancingIpNic(zone, network, ipId, revoked, null);
                mappingStates.add(nic.getState());
                Nic loadBalancingIpNic = nic.getNic();
                if (loadBalancingIpNic == null) {
                    continue;
                }

                // Change the source IP address for the load balancing rule to be the load balancing IP address
                srcIp = loadBalancingIpNic.getIPv4Address();
            }

            if ((destinations != null && !destinations.isEmpty()) || rule.isAutoScaleConfig()) {
                boolean inline = _networkMgr.isNetworkInlineMode(network);
                LoadBalancerTO loadBalancer =
                    new LoadBalancerTO(uuid, srcIp, srcPort, protocol, algorithm, revoked, false, inline, destinations, rule.getStickinessPolicies(),
                        rule.getHealthCheckPolicies(), rule.getLbSslCert(), rule.getLbProtocol());
                loadBalancer.setNetworkId(network.getId());
                loadBalancer.setSrcIpVlan(srcIpVlan);
                loadBalancer.setSrcIpNetmask(srcIpNetmask);
                loadBalancer.setSrcIpGateway(srcIpGateway);
                if (rule.isAutoScaleConfig()) {
                    loadBalancer.setAutoScaleVmGroupTO(lbRulesManager.toAutoScaleVmGroupTO(rule.getAutoScaleVmGroup()));
                }
                loadBalancersToApply.add(loadBalancer);
            }
        }

        try {
            if (loadBalancersToApply.size() > 0) {
                int numLoadBalancersForCommand = loadBalancersToApply.size();
                LoadBalancerTO[] loadBalancersForCommand = loadBalancersToApply.toArray(new LoadBalancerTO[numLoadBalancersForCommand]);
                LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(loadBalancersForCommand, null);
                long guestVlanTag = Integer.parseInt(BroadcastDomainType.getValue(network.getBroadcastUri()));
                cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, String.valueOf(guestVlanTag));
                Answer answer = _agentMgr.easySend(externalLoadBalancer.getId(), cmd);
                if (answer == null || !answer.getResult()) {
                    String details = (answer != null) ? answer.getDetails() : "details unavailable";
                    String msg = "Unable to apply load balancer rules to the external load balancer appliance in zone " + zone.getName() + " due to: " + details + ".";
                    s_logger.error(msg);
                    throw new ResourceUnavailableException(msg, DataCenter.class, network.getDataCenterId());
                }
            }
        } catch (Exception ex) {
            if (externalLoadBalancerIsInline) {
                s_logger.error("Rollbacking static nat operation of inline mode load balancing due to error on applying LB rules!");
                String existedGuestIp = loadBalancersToApply.get(0).getSrcIp();
                // Rollback static NAT operation in current session
                for (int i = 0; i < loadBalancingRules.size(); i++) {
                    LoadBalancingRule rule = loadBalancingRules.get(i);
                    MappingState state = mappingStates.get(i);
                    boolean revoke;
                    if (state == MappingState.Create) {
                        revoke = true;
                    } else if (state == MappingState.Remove) {
                        revoke = false;
                    } else {
                        continue;
                    }
                    long sourceIpId = _networkModel.getPublicIpAddress(rule.getSourceIp().addr(), network.getDataCenterId()).getId();
                    getLoadBalancingIpNic(zone, network, sourceIpId, revoke, existedGuestIp);
                }
            }
            throw new ResourceUnavailableException(ex.getMessage(), DataCenter.class, network.getDataCenterId());
        }

        return true;
    }

    @Override
    public boolean manageGuestNetworkWithExternalLoadBalancer(boolean add, Network guestConfig) throws ResourceUnavailableException, InsufficientCapacityException {
        if (guestConfig.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("External load balancer can only be used for guest networks.");
            return false;
        }

        long zoneId = guestConfig.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);
        HostVO externalLoadBalancer = null;

        if (add) {
            ExternalLoadBalancerDeviceVO lbDeviceVO = null;
            // on restart network, device could have been allocated already, skip allocation if a device is assigned
            lbDeviceVO = getExternalLoadBalancerForNetwork(guestConfig);
            if (lbDeviceVO == null) {
                // allocate a load balancer device for the network
                lbDeviceVO = allocateLoadBalancerForNetwork(guestConfig);
                if (lbDeviceVO == null) {
                    String msg = "failed to alloacate a external load balancer for the network " + guestConfig.getId();
                    s_logger.error(msg);
                    throw new InsufficientNetworkCapacityException(msg, DataCenter.class, guestConfig.getDataCenterId());
                }
            }
            externalLoadBalancer = _hostDao.findById(lbDeviceVO.getHostId());
            s_logger.debug("Allocated external load balancer device:" + lbDeviceVO.getId() + " for the network: " + guestConfig.getId());
        } else {
            // find the load balancer device allocated for the network
            ExternalLoadBalancerDeviceVO lbDeviceVO = getExternalLoadBalancerForNetwork(guestConfig);
            if (lbDeviceVO == null) {
                s_logger.warn("Network shutdwon requested on external load balancer element, which did not implement the network."
                    + " Either network implement failed half way through or already network shutdown is completed. So just returning.");
                return true;
            }

            externalLoadBalancer = _hostDao.findById(lbDeviceVO.getHostId());
            assert (externalLoadBalancer != null) : "There is no device assigned to this network how did shutdown network ended up here??";
        }

        // Send a command to the external load balancer to implement or shutdown the guest network
        String guestVlanTag = BroadcastDomainType.getValue(guestConfig.getBroadcastUri());
        String selfIp = null;
        String guestVlanNetmask = NetUtils.cidr2Netmask(guestConfig.getCidr());
        Integer networkRate = _networkModel.getNetworkRate(guestConfig.getId(), null);

        if (add) {
            // on restart network, network could have already been implemented. If already implemented then return
            Nic selfipNic = getPlaceholderNic(guestConfig);
            if (selfipNic != null) {
                return true;
            }

            // Acquire a self-ip address from the guest network IP address range
            selfIp = _ipAddrMgr.acquireGuestIpAddress(guestConfig, null);
            if (selfIp == null) {
                String msg = "failed to acquire guest IP address so not implementing the network on the external load balancer ";
                s_logger.error(msg);
                throw new InsufficientNetworkCapacityException(msg, Network.class, guestConfig.getId());
            }
        } else {
            // get the self-ip used by the load balancer
            Nic selfipNic = getPlaceholderNic(guestConfig);
            if (selfipNic == null) {
                s_logger.warn("Network shutdwon requested on external load balancer element, which did not implement the network."
                    + " Either network implement failed half way through or already network shutdown is completed. So just returning.");
                return true;
            }
            selfIp = selfipNic.getIPv4Address();
        }

        // It's a hack, using isOneToOneNat field for indicate if it's inline or not
        boolean inline = _networkMgr.isNetworkInlineMode(guestConfig);
        IpAddressTO ip =
            new IpAddressTO(guestConfig.getAccountId(), null, add, false, true, guestVlanTag, selfIp, guestVlanNetmask, null, networkRate, inline);
        IpAddressTO[] ips = new IpAddressTO[1];
        ips[0] = ip;
        IpAssocCommand cmd = new IpAssocCommand(ips);
        Answer answer = _agentMgr.easySend(externalLoadBalancer.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            String action = add ? "implement" : "shutdown";
            String answerDetails = (answer != null) ? answer.getDetails() : null;
            answerDetails = (answerDetails != null) ? " due to " + answerDetails : "";
            String msg = "External load balancer was unable to " + action + " the guest network on the external load balancer in zone " + zone.getName() + answerDetails;
            s_logger.error(msg);
            throw new ResourceUnavailableException(msg, Network.class, guestConfig.getId());
        }

        if (add) {
            // Insert a new NIC for this guest network to reserve the self IP
            _networkMgr.savePlaceholderNic(guestConfig, selfIp, null, null);
        } else {
            // release the self-ip obtained from guest network
            Nic selfipNic = getPlaceholderNic(guestConfig);
            _nicDao.remove(selfipNic.getId());

            // release the load balancer allocated for the network
            boolean releasedLB = freeLoadBalancerForNetwork(guestConfig);
            if (!releasedLB) {
                String msg = "Failed to release the external load balancer used for the network: " + guestConfig.getId();
                s_logger.error(msg);
            }
        }

        if (s_logger.isDebugEnabled()) {
            Account account = _accountDao.findByIdIncludingRemoved(guestConfig.getAccountId());
            String action = add ? "implemented" : "shut down";
            s_logger.debug("External load balancer has " + action + " the guest network for account " + account.getAccountName() + "(id = " + account.getAccountId() +
                ") with VLAN tag " + guestVlanTag);
        }

        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _defaultLbCapacity = NumbersUtil.parseLong(_configDao.getValue(Config.DefaultExternalLoadBalancerCapacity.key()), 50);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupExternalLoadBalancerCommand)) {
            return null;
        }
        if(host.getName().equalsIgnoreCase("NetScalerControlCenter")) {
            host.setType(Host.Type.NetScalerControlCenter);
        }
        else {
            host.setType(Host.Type.ExternalLoadBalancer);
        }
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (host.getType() != com.cloud.host.Host.Type.ExternalLoadBalancer) {
            return null;
        }
        return new DeleteHostAnswer(true);
    }

    protected IpDeployer getIpDeployerForInlineMode(Network network) {
        //We won't deploy IP, instead the firewall in front of us would do it
        List<Provider> providers = _networkMgr.getProvidersForServiceInNetwork(network, Service.Firewall);
        //Only support one provider now
        if (providers == null) {
            s_logger.error("Cannot find firewall provider for network " + network.getId());
            return null;
        }
        if (providers.size() != 1) {
            s_logger.error("Found " + providers.size() + " firewall provider for network " + network.getId());
            return null;
        }

        NetworkElement element = _networkModel.getElementImplementingProvider(providers.get(0).getName());
        if (!(element instanceof IpDeployer)) {
            s_logger.error("The firewall provider for network " + network.getName() + " don't have ability to deploy IP address!");
            return null;
        }
        s_logger.info("Let " + element.getName() + " handle ip association for " + getName() + " in network " + network.getId());
        return (IpDeployer)element;
    }

    @Override
    public List<LoadBalancerTO> getLBHealthChecks(Network network, List<LoadBalancingRule> loadBalancingRules) throws ResourceUnavailableException {

        // Find the external load balancer in this zone
        long zoneId = network.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);

        if (loadBalancingRules == null || loadBalancingRules.isEmpty()) {
            return null;
        }

        HostVO externalLoadBalancer = null;

        if(isNccServiceProvider(network)) {
            externalLoadBalancer  = getNetScalerControlCenterForNetwork(network);
        } else {
            ExternalLoadBalancerDeviceVO lbDeviceVO = getExternalLoadBalancerForNetwork(network);
            if (lbDeviceVO == null) {
                s_logger.warn("There is no external load balancer device assigned to this network either network is not implement are already shutdown so just returning");
                return null;
            } else {
                externalLoadBalancer = _hostDao.findById(lbDeviceVO.getHostId());
            }
        }

        boolean externalLoadBalancerIsInline = _networkMgr.isNetworkInlineMode(network);

        if (network.getState() == Network.State.Allocated) {
            s_logger.debug("External load balancer was asked to apply LB rules for network with ID " + network.getId() +
                "; this network is not implemented. Skipping backend commands.");
            return null;
        }

        List<LoadBalancerTO> loadBalancersToApply = new ArrayList<LoadBalancerTO>();
        List<MappingState> mappingStates = new ArrayList<MappingState>();
        for (final LoadBalancingRule rule : loadBalancingRules) {
            boolean revoked = (FirewallRule.State.Revoke.equals(rule.getState()));
            String protocol = rule.getProtocol();
            String algorithm = rule.getAlgorithm();
            String uuid = rule.getUuid();
            String srcIp = rule.getSourceIp().addr();
            int srcPort = rule.getSourcePortStart();
            List<LbDestination> destinations = rule.getDestinations();

            if (externalLoadBalancerIsInline) {
                long sourceIpId = _networkModel.getPublicIpAddress(rule.getSourceIp().addr(), network.getDataCenterId()).getId();
                MappingNic nic = getLoadBalancingIpNic(zone, network, sourceIpId, revoked, null);
                mappingStates.add(nic.getState());
                Nic loadBalancingIpNic = nic.getNic();
                if (loadBalancingIpNic == null) {
                    continue;
                }

                // Change the source IP address for the load balancing rule to
                // be the load balancing IP address
                srcIp = loadBalancingIpNic.getIPv4Address();
            }

            if ((destinations != null && !destinations.isEmpty()) || !rule.isAutoScaleConfig()) {
                boolean inline = _networkMgr.isNetworkInlineMode(network);
                LoadBalancerTO loadBalancer =
                    new LoadBalancerTO(uuid, srcIp, srcPort, protocol, algorithm, revoked, false, inline, destinations, rule.getStickinessPolicies(),
                        rule.getHealthCheckPolicies(), rule.getLbSslCert(), rule.getLbProtocol());
                loadBalancersToApply.add(loadBalancer);
            }
        }

        try {
            if (loadBalancersToApply.size() > 0) {
                int numLoadBalancersForCommand = loadBalancersToApply.size();
                LoadBalancerTO[] loadBalancersForCommand = loadBalancersToApply.toArray(new LoadBalancerTO[numLoadBalancersForCommand]);
                HealthCheckLBConfigCommand cmd = new HealthCheckLBConfigCommand(loadBalancersForCommand, network.getId());
                long guestVlanTag = Integer.parseInt(BroadcastDomainType.getValue(network.getBroadcastUri()));
                cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, String.valueOf(guestVlanTag));

                HealthCheckLBConfigAnswer answer = (HealthCheckLBConfigAnswer) _agentMgr.easySend(externalLoadBalancer.getId(), cmd);
                // easySend will return null on error
                return answer == null ? null : answer.getLoadBalancers();
            }
        } catch (Exception ex) {
            s_logger.error("Exception Occurred ", ex);
        }
        //null return is handled by clients
        return null;
    }

    private NicVO getPlaceholderNic(Network network) {
        List<NicVO> guestIps = _nicDao.listByNetworkId(network.getId());
        for (NicVO guestIp : guestIps) {
            // only external firewall and external load balancer will create NicVO with PlaceHolder reservation strategy
            if (ReservationStrategy.PlaceHolder.equals(guestIp.getReservationStrategy()) && guestIp.getVmType() == null && guestIp.getReserver() == null &&
                !guestIp.getIPv4Address().equals(network.getGateway())) {
                return guestIp;
            }
        }
        return null;
    }

}
