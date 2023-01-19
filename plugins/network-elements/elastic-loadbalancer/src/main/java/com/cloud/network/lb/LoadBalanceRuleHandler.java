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

package com.cloud.network.lb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;

import org.apache.cloudstack.api.command.user.loadbalancer.CreateLoadBalancerRuleCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.configuration.ConfigurationManagerImpl;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.network.ElasticLbVmMapVO;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.lb.dao.ElasticLbVmMapDao;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;

public class LoadBalanceRuleHandler {

    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private IPAddressDao _ipAddressDao;
    @Inject
    private NetworkModel _networkModel;
    @Inject
    private NetworkOrchestrationService _networkMgr;
    @Inject
    private final LoadBalancerDao _loadBalancerDao = null;
    @Inject
    private LoadBalancingRulesManager _lbMgr;
    @Inject
    private final DomainRouterDao _routerDao = null;
    @Inject
    protected HostPodDao _podDao = null;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    private final DataCenterDao _dcDao = null;
    @Inject
    private IpAddressManager _ipAddrMgr;
    @Inject
    protected NetworkDao _networkDao;
    @Inject
    protected NetworkOfferingDao _networkOfferingDao;
    @Inject
    private final VMTemplateDao _templateDao = null;
    @Inject
    private VirtualMachineManager _itMgr;
    @Inject
    private AccountService _accountService;
    @Inject
    private LoadBalancerDao _lbDao;
    @Inject
    private PodVlanMapDao _podVlanMapDao;
    @Inject
    private ElasticLbVmMapDao _elbVmMapDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private PhysicalNetworkServiceProviderDao _physicalProviderDao;
    @Inject
    private VirtualRouterProviderDao _vrProviderDao;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;
    @Inject
    private UserDao _userDao;

    static final private String ELB_VM_NAME_PREFIX = "l";

    private final String _instance;
    private final Account _systemAcct;

    public LoadBalanceRuleHandler(String instance, Account systemAcct) {
        _instance = instance;
        _systemAcct = systemAcct;
    }

    public void handleDeleteLoadBalancerRule(final LoadBalancer lb, final long userId, final Account caller) {
        final List<LoadBalancerVO> remainingLbs = _loadBalancerDao.listByIpAddress(lb.getSourceIpAddressId());
        if (remainingLbs.size() == 0) {
            logger.debug("ELB mgr: releasing ip " + lb.getSourceIpAddressId() + " since  no LB rules remain for this ip address");
            releaseIp(lb.getSourceIpAddressId(), userId, caller);
        }
    }

    public LoadBalancer handleCreateLoadBalancerRule(final CreateLoadBalancerRuleCmd lb, Account account, final long networkId) throws InsufficientAddressCapacityException,
    NetworkRuleConflictException {
        //this part of code is executed when the LB provider is Elastic Load Balancer vm
        if (!_networkModel.isProviderSupportServiceInNetwork(lb.getNetworkId(), Service.Lb, Provider.ElasticLoadBalancerVm)) {
            return null;
        }

        final Long ipId = lb.getSourceIpAddressId();
        if (ipId != null) {
            return null;
        }

        account = _accountDao.acquireInLockTable(account.getId());
        if (account == null) {
            logger.warn("ELB: CreateLoadBalancer: Failed to acquire lock on account");
            throw new CloudRuntimeException("Failed to acquire lock on account");
        }
        try {
            return handleCreateLoadBalancerRuleWithLock(lb, account, networkId);
        } finally {
            if (account != null) {
                _accountDao.releaseFromLockTable(account.getId());
            }
        }
    }

    private DomainRouterVO deployLoadBalancerVM(final Long networkId, final IPAddressVO ipAddr) {
        final NetworkVO network = _networkDao.findById(networkId);
        final DataCenter dc = _dcDao.findById(network.getDataCenterId());
        final Long podId = getPodIdForDirectIp(ipAddr);
        final Pod pod = podId == null ? null : _podDao.findById(podId);
        final Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>(1);
        params.put(VirtualMachineProfile.Param.ReProgramGuestNetworks, true);
        final Account owner = _accountService.getActiveAccountByName("system", new Long(1));
        final DeployDestination dest = new DeployDestination(dc, pod, null, null);
        logger.debug("About to deploy ELB vm ");

        try {
            final DomainRouterVO elbVm = deployELBVm(network, dest, owner, params);
            if (elbVm == null) {
                throw new InvalidParameterValueException("Could not deploy or find existing ELB VM");
            }
            logger.debug("Deployed ELB  vm = " + elbVm);

            return elbVm;

        } catch (final Throwable t) {
            logger.warn("Error while deploying ELB VM:  ", t);
            return null;
        }

    }

    private DomainRouterVO deployELBVm(Network guestNetwork, final DeployDestination dest, Account owner, final Map<Param, Object> params) throws ConcurrentOperationException,
    InsufficientCapacityException {
        final long dcId = dest.getDataCenter().getId();

        // lock guest network
        final Long guestNetworkId = guestNetwork.getId();
        guestNetwork = _networkDao.acquireInLockTable(guestNetworkId);

        if (guestNetwork == null) {
            throw new ConcurrentOperationException("Unable to acquire network lock: " + guestNetworkId);
        }

        try {

            if (_networkModel.isNetworkSystem(guestNetwork) || guestNetwork.getGuestType() == Network.GuestType.Shared) {
                owner = _accountService.getSystemAccount();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Starting a ELB vm for network configurations: " + guestNetwork + " in " + dest);
            }
            assert guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup || guestNetwork.getState() == Network.State.Implementing : "Network is not yet fully implemented: "
                    + guestNetwork;

            DataCenterDeployment plan = null;
            DomainRouterVO elbVm = null;

            plan = new DataCenterDeployment(dcId, dest.getPod().getId(), null, null, null, null);

            if (elbVm == null) {
                final long id = _routerDao.getNextInSequence(Long.class, "id");
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating the ELB vm " + id);
                }

                final List<? extends NetworkOffering> offerings = _networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork);
                final NetworkOffering controlOffering = offerings.get(0);
                final Network controlConfig = _networkMgr.setupNetwork(_systemAcct, controlOffering, plan, null, null, false).get(0);

                final LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>(2);
                final NicProfile guestNic = new NicProfile();
                guestNic.setDefaultNic(true);
                networks.put(controlConfig, new ArrayList<NicProfile>());
                networks.put(guestNetwork, new ArrayList<NicProfile>(Arrays.asList(guestNic)));

                final VMTemplateVO template = _templateDao.findSystemVMTemplate(dcId);

                final String typeString = "ElasticLoadBalancerVm";
                final Long physicalNetworkId = _networkModel.getPhysicalNetworkId(guestNetwork);
                final PhysicalNetworkServiceProvider provider = _physicalProviderDao.findByServiceProvider(physicalNetworkId, typeString);
                if (provider == null) {
                    throw new CloudRuntimeException("Cannot find service provider " + typeString + " in physical network " + physicalNetworkId);
                }
                final VirtualRouterProvider vrProvider = _vrProviderDao.findByNspIdAndType(provider.getId(), Type.ElasticLoadBalancerVm);
                if (vrProvider == null) {
                    throw new CloudRuntimeException("Cannot find virtual router provider " + typeString + " as service provider " + provider.getId());
                }

                long userId = CallContext.current().getCallingUserId();
                if (CallContext.current().getCallingAccount().getId() != owner.getId()) {
                    List<UserVO> userVOs = _userDao.listByAccount(owner.getAccountId());
                    if (!userVOs.isEmpty()) {
                        userId =  userVOs.get(0).getId();
                    }
                }

                ServiceOfferingVO elasticLbVmOffering = _serviceOfferingDao.findDefaultSystemOffering(ServiceOffering.elbVmDefaultOffUniqueName, ConfigurationManagerImpl.SystemVMUseLocalStorage.valueIn(dest.getDataCenter().getId()));
                elbVm = new DomainRouterVO(id, elasticLbVmOffering.getId(), vrProvider.getId(), VirtualMachineName.getSystemVmName(id, _instance, ELB_VM_NAME_PREFIX),
                        template.getId(), template.getHypervisorType(), template.getGuestOSId(), owner.getDomainId(), owner.getId(), userId, false, RedundantState.UNKNOWN,
                        elasticLbVmOffering.isOfferHA(), false, null);
                elbVm.setRole(Role.LB);
                elbVm = _routerDao.persist(elbVm);
                _itMgr.allocate(elbVm.getInstanceName(), template, elasticLbVmOffering, networks, plan, null);
                elbVm = _routerDao.findById(elbVm.getId());
                //TODO: create usage stats
            }

            final State state = elbVm.getState();
            if (state != State.Running) {
                elbVm = start(elbVm, params);
            }

            return elbVm;
        } finally {
            _networkDao.releaseFromLockTable(guestNetworkId);
        }
    }

    private void releaseIp(final long ipId, final long userId, final Account caller) {
        logger.info("ELB: Release public IP for loadbalancing " + ipId);
        final IPAddressVO ipvo = _ipAddressDao.findById(ipId);
        ipvo.setAssociatedWithNetworkId(null);
        _ipAddressDao.update(ipvo.getId(), ipvo);
        _ipAddrMgr.disassociatePublicIpAddress(ipId, userId, caller);
        _ipAddressDao.unassignIpAddress(ipId);
    }

    protected Long getPodIdForDirectIp(final IPAddressVO ipAddr) {
        final PodVlanMapVO podVlanMaps = _podVlanMapDao.listPodVlanMapsByVlan(ipAddr.getVlanId());
        if (podVlanMaps == null) {
            return null;
        } else {
            return podVlanMaps.getPodId();
        }
    }

    private LoadBalancer handleCreateLoadBalancerRuleWithLock(final CreateLoadBalancerRuleCmd lb, final Account account, final long networkId) throws InsufficientAddressCapacityException,
    NetworkRuleConflictException {
        Long ipId = null;
        boolean newIp = false;
        List<LoadBalancerVO> existingLbs = findExistingLoadBalancers(lb.getName(), lb.getSourceIpAddressId(), lb.getAccountId(), lb.getDomainId(), lb.getSourcePortStart());
        if (existingLbs == null) {
            existingLbs = findExistingLoadBalancers(lb.getName(), lb.getSourceIpAddressId(), lb.getAccountId(), lb.getDomainId(), null);
            if (existingLbs == null) {
                if (lb.getSourceIpAddressId() != null) {
                    throwExceptionIfSuppliedlLbNameIsNotAssociatedWithIpAddress(lb);
                } else {
                    logger.debug("Could not find any existing frontend ips for this account for this LB rule, acquiring a new frontent IP for ELB");
                    final PublicIp ip = allocDirectIp(account, networkId);
                    ipId = ip.getId();
                    newIp = true;
                }
            } else {
                ipId = existingLbs.get(0).getSourceIpAddressId();
                logger.debug("ELB: Found existing frontend ip for this account for this LB rule " + ipId);
            }
        } else {
            logger.warn("ELB: Found existing load balancers matching requested new LB");
            throw new NetworkRuleConflictException("ELB: Found existing load balancers matching requested new LB");
        }

        final IPAddressVO ipAddr = _ipAddressDao.findById(ipId);

        LoadBalancer result = null;
        try {
            lb.setSourceIpAddressId(ipId);

            result = _lbMgr.createPublicLoadBalancer(lb.getXid(), lb.getName(), lb.getDescription(), lb.getSourcePortStart(), lb.getDefaultPortStart(), ipId.longValue(),
                    lb.getProtocol(), lb.getAlgorithm(), false, CallContext.current(), lb.getLbProtocol(), true, null);
        } catch (final NetworkRuleConflictException e) {
            logger.warn("Failed to create LB rule, not continuing with ELB deployment");
            if (newIp) {
                releaseIp(ipId, CallContext.current().getCallingUserId(), account);
            }
            throw e;
        }

        DomainRouterVO elbVm = null;

        if (existingLbs == null) {
            elbVm = findElbVmWithCapacity(ipAddr);
            if (elbVm == null) {
                elbVm = deployLoadBalancerVM(networkId, ipAddr);
                if (elbVm == null) {
                    final Network network = _networkModel.getNetwork(networkId);
                    logger.warn("Failed to deploy a new ELB vm for ip " + ipAddr + " in network " + network + "lb name=" + lb.getName());
                    if (newIp) {
                        releaseIp(ipId, CallContext.current().getCallingUserId(), account);
                    }
                }
            }

        } else {
            final ElasticLbVmMapVO elbVmMap = _elbVmMapDao.findOneByIp(ipId);
            if (elbVmMap != null) {
                elbVm = _routerDao.findById(elbVmMap.getElbVmId());
            }
        }

        if (elbVm == null) {
            logger.warn("No ELB VM can be found or deployed");
            logger.warn("Deleting LB since we failed to deploy ELB VM");
            _lbDao.remove(result.getId());
            return null;
        }

        final ElasticLbVmMapVO mapping = new ElasticLbVmMapVO(ipId, elbVm.getId(), result.getId());
        _elbVmMapDao.persist(mapping);
        return result;
    }

    private void throwExceptionIfSuppliedlLbNameIsNotAssociatedWithIpAddress(final CreateLoadBalancerRuleCmd lb) {
        final List<LoadBalancerVO> existingLbs = findExistingLoadBalancers(lb.getName(), null, lb.getAccountId(), lb.getDomainId(), null);
        if (existingLbs != null) {
            throw new InvalidParameterValueException("Supplied LB name " + lb.getName() + " is not associated with IP " + lb.getSourceIpAddressId());
        }
    }

    protected List<LoadBalancerVO> findExistingLoadBalancers(final String lbName, final Long ipId, final Long accountId, final Long domainId, final Integer publicPort) {
        final SearchBuilder<LoadBalancerVO> sb = _lbDao.createSearchBuilder();
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("publicPort", sb.entity().getSourcePortStart(), SearchCriteria.Op.EQ);
        if (ipId != null) {
            sb.and("sourceIpAddress", sb.entity().getSourceIpAddressId(), SearchCriteria.Op.EQ);
        }
        if (domainId != null) {
            sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        }
        if (publicPort != null) {
            sb.and("publicPort", sb.entity().getSourcePortStart(), SearchCriteria.Op.EQ);
        }
        final SearchCriteria<LoadBalancerVO> sc = sb.create();
        sc.setParameters("name", lbName);
        sc.setParameters("accountId", accountId);
        if (ipId != null) {
            sc.setParameters("sourceIpAddress", ipId);
        }
        if (domainId != null) {
            sc.setParameters("domainId", domainId);
        }
        if (publicPort != null) {
            sc.setParameters("publicPort", publicPort);
        }
        final List<LoadBalancerVO> lbs = _lbDao.search(sc, null);

        return lbs == null || lbs.size() == 0 ? null : lbs;
    }

    @DB
    private PublicIp allocDirectIp(final Account account, final long guestNetworkId) throws InsufficientAddressCapacityException {
        return Transaction.execute(new TransactionCallbackWithException<PublicIp, InsufficientAddressCapacityException>() {
            @Override
            public PublicIp doInTransaction(final TransactionStatus status) throws InsufficientAddressCapacityException {
                final Network frontEndNetwork = _networkModel.getNetwork(guestNetworkId);

                final PublicIp ip = _ipAddrMgr.assignPublicIpAddress(frontEndNetwork.getDataCenterId(), null, account, VlanType.DirectAttached, frontEndNetwork.getId(), null, true, false);
                final IPAddressVO ipvo = _ipAddressDao.findById(ip.getId());
                ipvo.setAssociatedWithNetworkId(frontEndNetwork.getId());
                _ipAddressDao.update(ipvo.getId(), ipvo);
                logger.info("Acquired frontend IP for ELB " + ip);

                return ip;
            }
        });
    }

    protected DomainRouterVO findElbVmWithCapacity(final IPAddressVO ipAddr) {
        final List<DomainRouterVO> unusedElbVms = _elbVmMapDao.listUnusedElbVms();
        if (unusedElbVms.size() > 0) {
            final List<DomainRouterVO> candidateVms = new ArrayList<DomainRouterVO>();
            for (final DomainRouterVO candidateVm : unusedElbVms) {
                addCandidateVmIsPodIpMatches(candidateVm, getPodIdForDirectIp(ipAddr), candidateVms);
            }
            return candidateVms.size() == 0 ? null : candidateVms.get(new Random().nextInt(candidateVms.size()));
        }
        return null;
    }

    protected static void addCandidateVmIsPodIpMatches(final DomainRouterVO candidateVm, final Long podIdForDirectIp, final List<DomainRouterVO> candidateVms) {
        if (candidateVm.getPodIdToDeployIn().equals(podIdForDirectIp)) {
            candidateVms.add(candidateVm);
        }
    }

    protected DomainRouterVO start(final DomainRouterVO elbVm, final Map<Param, Object> params) throws ConcurrentOperationException {
        logger.debug("Starting ELB VM " + elbVm);
        _itMgr.start(elbVm.getUuid(), params);
        return _routerDao.findById(elbVm.getId());
    }
}
