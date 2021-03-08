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

package org.apache.cloudstack.network.contrail.management;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiConnectorFactory;
import net.juniper.contrail.api.ApiPropertyBase;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.FloatingIp;
import net.juniper.contrail.api.types.FloatingIpPool;
import net.juniper.contrail.api.types.NetworkPolicy;
import net.juniper.contrail.api.types.VirtualNetwork;

import org.apache.cloudstack.network.contrail.model.FloatingIpModel;
import org.apache.cloudstack.network.contrail.model.FloatingIpPoolModel;
import org.apache.cloudstack.network.contrail.model.ModelController;
import org.apache.cloudstack.network.contrail.model.VirtualNetworkModel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ConfigurationService;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.vpc.NetworkACLVO;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.network.vpc.VpcOfferingVO;
import com.cloud.network.vpc.VpcProvisioningService;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.server.ConfigurationServer;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.collect.ImmutableList;

public class ContrailManagerImpl extends ManagerBase implements ContrailManager {
    @Inject
    public ConfigurationService _configService;
    @Inject
    ConfigurationServer _configServer;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    NetworkDao _networksDao;
    @Inject
    VMInstanceDao _vmInstanceDao;
    @Inject
    ProjectDao _projectDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physProviderDao;
    @Inject
    NicDao _nicDao;
    @Inject
    ServerDBSync _dbSync;
    @Inject
    ServerEventHandler _eventHandler;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    UserVmDao _vmDao;
    @Inject
    VpcOfferingDao _vpcOffDao;
    @Inject
    VpcProvisioningService _vpcProvSvc;
    @Inject
    VpcDao _vpcDao;
    @Inject
    NetworkACLDao _networkAclDao;

    private static final Logger s_logger = Logger.getLogger(ContrailManager.class);

    private ApiConnector _api;

    private NetworkOffering _offering;
    private NetworkOffering _routerOffering;
    private NetworkOffering _routerPublicOffering;
    private NetworkOffering _vpcRouterOffering;
    private VpcOffering _vpcOffering;

    private Timer _dbSyncTimer;
    private int _dbSyncInterval = DB_SYNC_INTERVAL_DEFAULT;
    private final String configuration = "contrail.properties";
    private final ModelDatabase _database;
    private ModelController _controller;

    ContrailManagerImpl() {
        _database = new ModelDatabase();
    }

    @Override
    public boolean start() {
        if (_api == null) {
            return true;
        }
        /* Start background task */
        _dbSyncTimer = new Timer("DBSyncTimer");
        try {
            _dbSyncTimer.schedule(new DBSyncTask(), 0, _dbSyncInterval);
        } catch (Exception ex) {
            s_logger.debug("Unable to start DB Sync timer " + ex.getMessage());
            s_logger.debug("timer start", ex);
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (_dbSyncTimer != null) {
            _dbSyncTimer.cancel();
        }
        return true;
    }

    @Override
    public ModelDatabase getDatabase() {
        return _database;
    }

    private NetworkOffering locatePublicNetworkOffering(String offeringName,
            String offeringDisplayText, Provider provider) {
        List<? extends NetworkOffering> offerList = _configService.listNetworkOfferings(TrafficType.Public, false);
        for (NetworkOffering offer: offerList) {
            if (offer.getName().equals(offeringName)) {
                if (offer.getState() != NetworkOffering.State.Enabled) {
                    return EnableNetworkOffering(offer.getId());
                }
                return offer;
            }
        }
        Map<Service, Set<Provider>> serviceProviderMap = new HashMap<Service, Set<Provider>>();
        Set<Provider> providerSet = new HashSet<Provider>();
        providerSet.add(provider);
        final Service[] services = {
                Service.Connectivity,
                Service.Dhcp,
                Service.NetworkACL,
                Service.StaticNat,
                Service.SourceNat
        };
        for (Service svc: services) {
            serviceProviderMap.put(svc, providerSet);
        }
        ConfigurationManager configMgr = (ConfigurationManager) _configService;
        NetworkOfferingVO voffer = configMgr.createNetworkOffering(offeringName, offeringDisplayText,
                TrafficType.Public, null, true, Availability.Optional, null, serviceProviderMap, true,
                Network.GuestType.Shared, false, null, false, null, true, false, null, true, null, false, false, null, null, true);
        long id = voffer.getId();
        _networkOfferingDao.update(id, voffer);
        return _networkOfferingDao.findById(id);
    }

    private NetworkOffering locateNetworkOffering(String offeringName,
            String offeringDisplayText, Provider provider) {
        List<? extends NetworkOffering> offerList = _configService.listNetworkOfferings(TrafficType.Guest, false);
        for (NetworkOffering offer : offerList) {
            if (offer.getName().equals(offeringName)) {
                if (offer.getState() != NetworkOffering.State.Enabled) {
                    return EnableNetworkOffering(offer.getId());
                }
                return offer;
            }
        }
        Map<Service, Set<Provider>> serviceProviderMap = new HashMap<Service, Set<Provider>>();
        Set<Provider> providerSet = new HashSet<Provider>();
        providerSet.add(provider);
        final Service[] services = {Service.Connectivity, Service.Dhcp, Service.NetworkACL, Service.StaticNat, Service.SourceNat, Service.Lb};
        for (Service svc : services) {
            if (svc == Service.Lb) {
                if(offeringName.equals(vpcRouterOfferingName)) {
                    Set<Provider> lbProviderSet = new HashSet<Provider>();
                    lbProviderSet.add(Provider.InternalLbVm);
                    serviceProviderMap.put(svc, lbProviderSet);
                }
                continue;
            }
            serviceProviderMap.put(svc, providerSet);
        }
        ConfigurationManager configMgr = (ConfigurationManager)_configService;
        NetworkOfferingVO voffer =
                configMgr.createNetworkOffering(offeringName, offeringDisplayText, TrafficType.Guest, null, false, Availability.Optional, null, serviceProviderMap, true,
                        Network.GuestType.Isolated, false, null, false, null, false, true, null, true, null, false, offeringName.equals(vpcRouterOfferingName), null, null, true);
        if (offeringName.equals(vpcRouterOfferingName)) {
            voffer.setInternalLb(true);
        }
        long id = voffer.getId();
        _networkOfferingDao.update(id, voffer);
        return _networkOfferingDao.findById(id);
    }

    private VpcOffering locateVpcOffering() {
        VpcOffering vpcOffer = _vpcOffDao.findByUniqueName(juniperVPCOfferingName);
        if (vpcOffer != null) {
            if (((VpcOfferingVO)vpcOffer).getState() == VpcOffering.State.Enabled) {
                return vpcOffer;
            }
            ((VpcOfferingVO)vpcOffer).setState(VpcOffering.State.Enabled);
            long id = vpcOffer.getId();
            _vpcOffDao.update(id, (VpcOfferingVO)vpcOffer);
            return vpcOffer;
        }
        Map<String, List<String>> serviceProviderMap = new HashMap<String, List<String>>();
        List<String> providerSet = new ArrayList<String>();
        providerSet.add(Provider.JuniperContrailVpcRouter.getName());
        final List<String> services = new ArrayList<String>();
        services.add(Service.Connectivity.getName());
        services.add(Service.Dhcp.getName());
        services.add(Service.NetworkACL.getName());
        services.add(Service.StaticNat.getName());
        services.add(Service.SourceNat.getName());
        services.add(Service.Gateway.getName());
        services.add(Service.Lb.getName());

        for (String svc: services) {
            if (svc.equals(Service.Lb.getName())) {
                List<String> lbProviderSet = new ArrayList<String>();
                lbProviderSet.add(Provider.InternalLbVm.getName());
                serviceProviderMap.put(svc, lbProviderSet);
                continue;
            }
            serviceProviderMap.put(svc, providerSet);
        }
        vpcOffer = _vpcProvSvc.createVpcOffering(juniperVPCOfferingName, juniperVPCOfferingDisplayText, services, serviceProviderMap, null, null, null, null, VpcOffering.State.Enabled);
        long id = vpcOffer.getId();
        _vpcOffDao.update(id, (VpcOfferingVO)vpcOffer);
        return _vpcOffDao.findById(id);
    }

    private NetworkOffering EnableNetworkOffering(long id) {
        NetworkOfferingVO offering = _networkOfferingDao.createForUpdate(id);
        offering.setState(NetworkOffering.State.Enabled);
        _networkOfferingDao.update(id, offering);
        return _networkOfferingDao.findById(id);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        File configFile = PropertiesUtil.findConfigFile(configuration);
        FileInputStream fileStream = null;
        try {
            String hostname = null;
            int port = 0;
            if (configFile == null) {
                return false;
            } else {
                final Properties configProps = new Properties();
                fileStream = new FileInputStream(configFile);
                configProps.load(fileStream);

                String value = configProps.getProperty("management.db_sync_interval");
                if (value != null) {
                    _dbSyncInterval = Integer.parseInt(value);
                }
                hostname = configProps.getProperty("api.hostname");
                String portStr = configProps.getProperty("api.port");
                if (portStr != null && portStr.length() > 0) {
                    port = Integer.parseInt(portStr);
                }
            }
            _api = ApiConnectorFactory.build(hostname, port);
        } catch (IOException ex) {
            s_logger.warn("Unable to read " + configuration, ex);
            throw new ConfigurationException();
        } catch (Exception ex) {
            s_logger.debug("Exception in configure: " + ex);
            ex.printStackTrace();
            throw new ConfigurationException();
        } finally {
            IOUtils.closeQuietly(fileStream);
        }

        _controller = new ModelController(this, _api, _vmDao, _networksDao, _nicDao, _vlanDao, _ipAddressDao);
        try {
            _routerOffering = locateNetworkOffering(routerOfferingName, routerOfferingDisplayText,
                    Provider.JuniperContrailRouter);
            _routerPublicOffering = locatePublicNetworkOffering(routerPublicOfferingName, routerPublicOfferingDisplayText,
                    Provider.JuniperContrailRouter);
            _vpcRouterOffering = locateNetworkOffering(vpcRouterOfferingName, vpcRouterOfferingDisplayText,
                    Provider.JuniperContrailVpcRouter);
            _vpcOffering = locateVpcOffering();
        }catch (Exception ex) {
            s_logger.debug("Exception in locating network offerings: " + ex);
            ex.printStackTrace();
            throw new ConfigurationException();
        }

        _eventHandler.subscribe();

        initializeDefaultVirtualNetworkModels();

        return true;
    }

    @Override
    public NetworkOffering getPublicRouterOffering() {
        return _routerPublicOffering;
    }

    @Override
    public NetworkOffering getRouterOffering() {
        return _routerOffering;
    }

    @Override
    public NetworkOffering getVpcRouterOffering() {
        return _vpcRouterOffering;
    }

    @Override
    public VpcOffering getVpcOffering() {
        return _vpcOffering;
    }

    @Override
    public String getPhysicalNetworkName(PhysicalNetworkVO physNet) {
        String physname = physNet.getName();
        physname = physname.replaceAll("\\s", "").replace("_", "");
        return physname;
    }

    @Override
    public String getDomainCanonicalName(DomainVO domain) {
        if (domain.getId() == Domain.ROOT_DOMAIN) {
            return VNC_ROOT_DOMAIN;
        }
        return domain.getName();
    }

    @Override
    public String getProjectCanonicalName(ProjectVO project) {
        return project.getName();
    }

    @Override
    public String getCanonicalName(Network net) {
        String netname;
        if (net.getTrafficType() == TrafficType.Guest) {
            return net.getName();
        } else if (net.getTrafficType() == TrafficType.Management || net.getTrafficType() == TrafficType.Storage) {
            return managementNetworkName;
        } else if (net.getTrafficType() == TrafficType.Control) {
            return "__link_local__";
        } else {
            DataCenter zone = _dcDao.findById(net.getDataCenterId());
            String zonename = zone.getName();
            zonename = zonename.replaceAll("\\s", "");
            zonename = zonename.replace("-", "_");
            netname = "__" + zonename + "_" + net.getTrafficType().toString() + "__";
        }
        return netname;
    }

    @Override
    public String getDomainName(long domainId) {
        if (domainId != Domain.ROOT_DOMAIN) {
            DomainVO domain = _domainDao.findById(domainId);
            return domain.getName();
        }
        return VNC_ROOT_DOMAIN;
    }

    @Override
    public String getProjectName(long accountId) {
        Account account = _accountDao.findById(accountId);
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            ProjectVO project = _projectDao.findByProjectAccountId(account.getId());
            if (project != null) {
                return project.getName();
            }
        }
        return VNC_DEFAULT_PROJECT;
    }

    @Override
    public String getDefaultPublicNetworkFQN() {
        String name = VNC_ROOT_DOMAIN + ":" + VNC_DEFAULT_PROJECT + ":" + "__default_Public__";
        return name;
    }

    private ProjectVO getProject(long accountId) {
        Account account = _accountDao.findById(accountId);
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            return _projectDao.findByProjectAccountId(account.getId());
        }
        return null;
    }

    @Override
    public String getProjectId(long domainId, long accountId) throws IOException {
        ProjectVO project = getProject(accountId);
        if (project != null) {
            return project.getUuid();
        }
        DomainVO domain = _domainDao.findById(domainId);
        if (domain.getId() != Domain.ROOT_DOMAIN) {
            net.juniper.contrail.api.types.Domain vncDomain =
                    (net.juniper.contrail.api.types.Domain)_api.findById(net.juniper.contrail.api.types.Domain.class, domain.getUuid());
            return _api.findByName(net.juniper.contrail.api.types.Project.class, vncDomain, VNC_DEFAULT_PROJECT);
        }
        return null;
    }

    @Override
    public net.juniper.contrail.api.types.Project getVncProject(long domainId, long accountId) throws IOException {
        String projectId = getProjectId(domainId, accountId);
        if (projectId == null) {
            return getDefaultVncProject();
        }
        return (net.juniper.contrail.api.types.Project)_api.findById(net.juniper.contrail.api.types.Project.class, projectId);
    }

    @Override
    public net.juniper.contrail.api.types.Project getDefaultVncProject() throws IOException {
        net.juniper.contrail.api.types.Project project = null;
        project = (net.juniper.contrail.api.types.Project)_api.findByFQN(net.juniper.contrail.api.types.Project.class, VNC_ROOT_DOMAIN + ":" + VNC_DEFAULT_PROJECT);
        return project;
    }

    @Override
    public String getFQN(Network net) {
        // domain, project, name
        String fqname = getDomainName(net.getDomainId());
        fqname += ":" + getProjectName(net.getAccountId()) + ":";
        return fqname + getCanonicalName(net);
    }

    @Override
    public void findInfrastructureNetworks(PhysicalNetworkVO phys, List<NetworkVO> dbList) {
        final TrafficType[] ttypes = {TrafficType.Control,    // maps to __link_local__
                TrafficType.Management, // maps to ip-fabric
                TrafficType.Public, TrafficType.Storage        // maps to ip-fabric
        };

        for (int i = 0; i < ttypes.length; i++) {
            List<NetworkVO> phys_nets;
            phys_nets = _networksDao.listByZoneAndTrafficType(phys.getDataCenterId(), ttypes[i]);
            dbList.addAll(phys_nets);
        }

    }

    @Override
    public void syncNetworkDB(short syncMode) throws IOException {
        if (_dbSync.syncAll(syncMode) == ServerDBSync.SYNC_STATE_OUT_OF_SYNC) {
            if (syncMode == DBSyncGeneric.SYNC_MODE_CHECK) {
                s_logger.info("# Cloudstack DB & VNC are out of sync #");
            } else {
                s_logger.info("# Cloudstack DB & VNC were out of sync, performed re-sync operation #");
            }
        } else {
            s_logger.info("# Cloudstack DB & VNC are in sync #");
        }
    }

    public class DBSyncTask extends TimerTask {
        private short _syncMode = DBSyncGeneric.SYNC_MODE_UPDATE;

        @Override
        public void run() {
            try {
                s_logger.debug("DB Sync task is running");
                syncNetworkDB(_syncMode);
                // Change to check mode
                _syncMode = DBSyncGeneric.SYNC_MODE_CHECK;
            } catch (Exception ex) {
                s_logger.debug(ex);
                s_logger.info("Unable to sync network db");
            }
        }
    }

    @Override
    public boolean isManagedPhysicalNetwork(Network network) {
        List<PhysicalNetworkVO> net_list = _physicalNetworkDao.listByZone(network.getDataCenterId());
        for (PhysicalNetworkVO phys : net_list) {
            if(_physProviderDao.findByServiceProvider(phys.getId(), Provider.JuniperContrailRouter.getName()) != null ||
                    _physProviderDao.findByServiceProvider(phys.getId(), Provider.JuniperContrailVpcRouter.getName()) != null ||
                    _physProviderDao.findByServiceProvider(phys.getId(), Provider.InternalLbVm.getName()) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String findVirtualNetworkId(Network net) throws IOException {
        if (net.getTrafficType() == TrafficType.Guest) {
            return net.getUuid();
        }
        String netname = getDomainName(net.getDomainId()) + ":" + getProjectName(net.getAccountId()) + ":";

        if (net.getTrafficType() == TrafficType.Control) {
            netname += "__link_local__";
        } else if (net.getTrafficType() == TrafficType.Management || net.getTrafficType() == TrafficType.Storage) {
            netname += managementNetworkName;
        } else {
            netname = getFQN(net);
        }
        List<String> fqn = ImmutableList.copyOf(StringUtils.split(netname, ':'));
        return _api.findByName(VirtualNetwork.class, fqn);
    }

    @Override
    public List<NetworkVO> findSystemNetworks(List<TrafficType> types) {
        SearchBuilder<NetworkVO> searchBuilder = _networksDao.createSearchBuilder();
        searchBuilder.and("trafficType", searchBuilder.entity().getTrafficType(), Op.IN);
        SearchCriteria<NetworkVO> sc = searchBuilder.create();
        if (types == null || types.isEmpty()) {
            types = new ArrayList<TrafficType>();
            types.add(TrafficType.Control);
            types.add(TrafficType.Management);
            types.add(TrafficType.Public);
            types.add(TrafficType.Storage);
        }
        sc.setParameters("trafficType", types.toArray());
        List<NetworkVO> dbNets = _networksDao.search(sc, null);
        if (dbNets == null) {
            s_logger.debug("no system networks for the given traffic types: " + types.toString());
            dbNets = new ArrayList<NetworkVO>();
        }

        List<PhysicalNetworkVO> phys_list = _physicalNetworkDao.listAll();
        final String provider = Provider.JuniperContrailRouter.getName();
        for (Iterator<PhysicalNetworkVO> iter = phys_list.iterator(); iter.hasNext(); ) {
            PhysicalNetworkVO phys = iter.next();
            if (_physProviderDao.findByServiceProvider(phys.getId(), provider) != null) {
                List<NetworkVO> infraNets = new ArrayList<NetworkVO>();
                findInfrastructureNetworks(phys, infraNets);
                for (NetworkVO net:infraNets) {
                    if (types == null || types.isEmpty()) {
                        if (!dbNets.contains(net)) {
                            dbNets.add(net);
                        }
                        continue;
                    }
                    for(TrafficType type:types) {
                        if (net.getTrafficType() == type) {
                            if (!dbNets.contains(net)) {
                                dbNets.add(net);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return dbNets;
    }

    @Override
    public VirtualNetwork findDefaultVirtualNetwork(TrafficType trafficType) throws IOException {
        if (trafficType == TrafficType.Guest || trafficType == TrafficType.Public) {
            return null;
        }
        String netname = VNC_ROOT_DOMAIN + ":" + VNC_DEFAULT_PROJECT + ":";
        if (trafficType == TrafficType.Control) {
            netname += "__link_local__";
        } else if (trafficType == TrafficType.Management || trafficType == TrafficType.Storage) {
            netname += managementNetworkName;
        }
        return (VirtualNetwork)_api.findByFQN(VirtualNetwork.class, netname);
    }

    /*
     * Returns list of networks managed by Juniper VRouter filtered by traffic types
     */
    @Override
    public List<NetworkVO> findManagedNetworks(List<TrafficType> types) {

        SearchBuilder<NetworkVO> searchBuilder = _networksDao.createSearchBuilder();
        searchBuilder.and("trafficType", searchBuilder.entity().getTrafficType(), Op.IN);
        searchBuilder.and("networkOfferingId", searchBuilder.entity().getNetworkOfferingId(), Op.IN);

        SearchCriteria<NetworkVO> sc = searchBuilder.create();
        List<Long> offerings = new ArrayList<Long>();
        offerings.add(getRouterOffering().getId());
        offerings.add(getVpcRouterOffering().getId());
        offerings.add(getPublicRouterOffering().getId());
        sc.setParameters("networkOfferingId", offerings.toArray());

        if (types == null || types.isEmpty()) {
            types = new ArrayList<TrafficType>();
            types.add(TrafficType.Control);
            types.add(TrafficType.Management);
            types.add(TrafficType.Public);
            types.add(TrafficType.Storage);
            types.add(TrafficType.Guest);
        }
        sc.setParameters("trafficType", types.toArray());

        List<NetworkVO> dbNets = _networksDao.search(sc, null);
        if (dbNets == null) {
            s_logger.debug("no juniper managed networks for the given traffic types: " + types.toString());
            dbNets = new ArrayList<NetworkVO>();
        }

        List<PhysicalNetworkVO> phys_list = _physicalNetworkDao.listAll();
        final String provider = Network.Provider.JuniperContrailRouter.getName();
        final String vpcProvider = Provider.JuniperContrailVpcRouter.getName();
        for (Iterator<PhysicalNetworkVO> iter = phys_list.iterator(); iter.hasNext();) {
            PhysicalNetworkVO phys = iter.next();
            if (_physProviderDao.findByServiceProvider(phys.getId(), provider) != null ||
                    _physProviderDao.findByServiceProvider(phys.getId(), vpcProvider) != null) {
                List<NetworkVO> infraNets = new ArrayList<NetworkVO>();
                findInfrastructureNetworks(phys, infraNets);
                for (NetworkVO net : infraNets) {
                    if (types == null || types.isEmpty()) {
                        if (!dbNets.contains(net)) {
                            dbNets.add(net);
                        }
                        continue;
                    }
                    for (TrafficType type : types) {
                        if (net.getTrafficType() == type) {
                            if (!dbNets.contains(net)) {
                                dbNets.add(net);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return dbNets;
    }

    @Override
    public List<VpcVO> findManagedVpcs() {
        SearchBuilder<VpcVO> searchBuilder = _vpcDao.createSearchBuilder();
        searchBuilder.and("vpcOffering", searchBuilder.entity().getVpcOfferingId(), Op.EQ);
        SearchCriteria<VpcVO> sc = searchBuilder.create();
        sc.setParameters("vpcOffering", getVpcOffering().getId());
        List<VpcVO> vpcs = _vpcDao.search(sc, null);
        if (vpcs == null || vpcs.size() == 0) {
            s_logger.debug("no vpcs found");
            return null;
        }
        return vpcs;
    }

    @Override
    public List<NetworkACLVO> findManagedACLs() {
        List<VpcVO> vpcs = findManagedVpcs();
        if (vpcs == null || vpcs.isEmpty()) {
            return null;
        }
        List<Long> vpcIds = new ArrayList<Long>();
        /* default-allow, default-deny ACLs will be under vpcId '0', so include it*/
        vpcIds.add((long)0);
        for (VpcVO vpc:vpcs) {
            vpcIds.add(vpc.getId());
        }
        SearchBuilder<NetworkACLVO> searchBuilder = _networkAclDao.createSearchBuilder();
        searchBuilder.and("vpcId", searchBuilder.entity().getVpcId(), Op.IN);
        SearchCriteria<NetworkACLVO> sc = searchBuilder.create();
        sc.setParameters("vpcId", vpcIds.toArray());
        List<NetworkACLVO> acls = _networkAclDao.search(sc, null);
        if (acls == null || acls.size() == 0) {
            s_logger.debug("no acls found");
            return null;
        }
        /* only return if acl is associated to any network */
        List<NetworkACLVO> jnprAcls = new ArrayList<NetworkACLVO>();
        for (NetworkACLVO acl:acls) {
            List<NetworkVO> nets = _networksDao.listByAclId(acl.getId());
            if (nets == null || nets.isEmpty()) {
                continue;
            }
            jnprAcls.add(acl);
        }
        return jnprAcls;
    }

    /*
     * Returns list of public ip addresses managed by Juniper VRouter
     */
    @Override
    public List<IPAddressVO> findManagedPublicIps() {

        List<NetworkVO> dbNets = findManagedNetworks(null);

        if (dbNets == null || dbNets.isEmpty()) {
            s_logger.debug("Juniper managed networks is empty");
            return null;
        }

        SearchBuilder<IPAddressVO> searchBuilder = _ipAddressDao.createSearchBuilder();
        searchBuilder.and("sourceNat", searchBuilder.entity().isSourceNat(), Op.EQ);
        searchBuilder.and("network", searchBuilder.entity().getAssociatedWithNetworkId(), Op.IN);
        searchBuilder.and("oneToOneNat", searchBuilder.entity().isOneToOneNat(), Op.EQ);
        searchBuilder.and("associatedWithVmId", searchBuilder.entity().getAssociatedWithVmId(), Op.NNULL);

        List<Long> netIds = new ArrayList<Long>();
        for (NetworkVO net : dbNets) {
            netIds.add(net.getId());
        }

        SearchCriteria<IPAddressVO> sc = searchBuilder.create();
        sc.setParameters("oneToOneNat", true);
        sc.setParameters("sourceNat", false);
        sc.setParameters("network", netIds.toArray());

        List<IPAddressVO> publicIps = _ipAddressDao.search(sc, null);
        if (publicIps == null) {
            s_logger.debug("no public ips");
            return null;
        }

        return publicIps;
    }

    private void initializeDefaultVirtualNetworkModels() {
        List<TrafficType> types = new ArrayList<TrafficType>();
        types.add(TrafficType.Management);
        types.add(TrafficType.Storage);
        types.add(TrafficType.Control);

        List<NetworkVO> dbNets = findManagedNetworks(types);
        for (NetworkVO net : dbNets) {

            VirtualNetworkModel vnModel = getDatabase().lookupVirtualNetwork(null, getCanonicalName(net), net.getTrafficType());
            if (vnModel == null) {
                vnModel = new VirtualNetworkModel(net, null, getCanonicalName(net), net.getTrafficType());
                vnModel.build(getModelController(), net);
                try {
                    if (!vnModel.verify(getModelController())) {
                        vnModel.update(getModelController());
                    }
                } catch (Exception ex) {
                    s_logger.warn("virtual-network update: ", ex);
                }
                getDatabase().getVirtualNetworks().add(vnModel);
            }
        }
    }

    @Override
    public boolean isSystemDefaultNetwork(VirtualNetwork vnet) {
        List<String> fqn = vnet.getQualifiedName();
        if (fqn.size() < 3) {
            return false;
        }
        List<String> default_parent = vnet.getDefaultParent();
        int index = 0;
        for (Iterator<String> iter = default_parent.iterator(); iter.hasNext(); index++) {
            String piece = iter.next();
            if (!piece.equals(fqn.get(index))) {
                return false;
            }
        }
        List<String> default_networks = ImmutableList.of("__link_local__", "default-virtual-network", managementNetworkName);
        for (Iterator<String> iter = default_networks.iterator(); iter.hasNext();) {
            String name = iter.next();
            if (name.equals(fqn.get(index))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSystemDefaultNetwork(NetworkVO net) {
        if (net.getTrafficType() == TrafficType.Management || net.getTrafficType() == TrafficType.Storage || net.getTrafficType() == TrafficType.Control) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSystemRootDomain(net.juniper.contrail.api.types.Domain domain) {
        if (domain.getName().compareTo(VNC_ROOT_DOMAIN) == 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSystemRootDomain(DomainVO domain) {
        if (domain.getId() == Domain.ROOT_DOMAIN || domain.getName().compareTo("ROOT") == 0 || domain.getParent() == null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSystemDefaultProject(net.juniper.contrail.api.types.Project project) {
        if (project.getName().compareTo(VNC_DEFAULT_PROJECT) == 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSystemDefaultProject(ProjectVO project) {
        return false;
    }

    @Override
    public String getVifNameByVmName(String vmName, Integer deviceId) {
        String vif_name = vmName + "-" + deviceId.toString();
        return vif_name;
    }

    @Override
    public String getVifNameByVmUuid(String vmUuid, Integer deviceId) {
        VMInstanceVO vm = _vmInstanceDao.findByUuid(vmUuid);
        if (vm != null) {
            return vm.getInstanceName() + "-" + deviceId.toString();
        }
        return null;
    }

    @Override
    public ModelController getModelController() {
        return _controller;
    }

    @Override
    public ApiConnector getApiConnector() {
        return _api;
    }

    @Override
    public VirtualNetworkModel lookupPublicNetworkModel() {
        List<TrafficType> types = new ArrayList<TrafficType>();
        types.add(TrafficType.Public);
        List<NetworkVO> dbNets = findManagedNetworks(types);
        if (dbNets == null) {
            return null;
        }
        NetworkVO net = dbNets.get(0);

        VirtualNetworkModel vnModel = getDatabase().lookupVirtualNetwork(net.getUuid(), getCanonicalName(net), TrafficType.Public);
        if (vnModel == null) {
            vnModel = new VirtualNetworkModel(net, net.getUuid(),
                    getCanonicalName(net), net.getTrafficType());
            vnModel.setProperties(getModelController(), net);
        }
        try {
            if (!vnModel.verify(getModelController())) {
                vnModel.update(getModelController());
            }
            getDatabase().getVirtualNetworks().add(vnModel);
        } catch (Exception ex) {
            s_logger.warn("virtual-network update: ", ex);
        }
        return vnModel;
    }

    @Override
    public boolean createFloatingIp(PublicIpAddress ip) {
        VirtualNetworkModel vnModel = lookupPublicNetworkModel();
        assert vnModel != null : "public network vn model is null";
        FloatingIpPoolModel fipPoolModel = vnModel.getFipPoolModel();

        /* create only, no updates */
        if (fipPoolModel == null) {
            fipPoolModel = new FloatingIpPoolModel();
            fipPoolModel.addToVirtualNetwork(vnModel);
            fipPoolModel.build(getModelController());
            try {
                fipPoolModel.update(getModelController());
                vnModel.setFipPoolModel(fipPoolModel);
            } catch (Exception ex) {
                s_logger.warn("floating-ip-pool create: ", ex);
                return false;
            }
        }

        FloatingIpModel fipModel = fipPoolModel.getFloatingIpModel(ip.getUuid());
        /* create only, no updates*/
        if (fipModel == null) {
            fipModel = new FloatingIpModel(ip.getUuid());
            fipModel.addToFloatingIpPool(fipPoolModel);
            fipModel.build(getModelController(), ip);
            try {
                fipModel.update(getModelController());
            } catch (Exception ex) {
                s_logger.warn("floating-ip create: ", ex);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean deleteFloatingIp(PublicIpAddress ip) {
        VirtualNetworkModel vnModel = lookupPublicNetworkModel();
        assert vnModel != null : "public network model is null";
        FloatingIpPoolModel fipPoolModel = vnModel.getFipPoolModel();
        FloatingIpModel fipModel = fipPoolModel.getFloatingIpModel(ip.getUuid());
        if (fipModel != null) {
            try {
                fipModel.destroy(getModelController());
            } catch (IOException ex) {
                s_logger.warn("floating ip delete", ex);
                return false;
            }
            fipPoolModel.removeSuccessor(fipModel);
            if (!fipPoolModel.hasDescendents()) {
                try {
                    fipPoolModel.delete(getModelController());
                    vnModel.setFipPoolModel(null);
                } catch (IOException e) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<FloatingIp> getFloatingIps() {
        String fipPoolName = getDefaultPublicNetworkFQN() + ":PublicIpPool";
        FloatingIpPool fipPool = null;
        try {
            fipPool = (FloatingIpPool)_api.findByFQN(FloatingIpPool.class, fipPoolName);
        } catch (Exception ex) {
            s_logger.debug(ex);
        }
        if (fipPool == null) {
            return null;
        }
        List<ObjectReference<ApiPropertyBase>> ips = fipPool.getFloatingIps();
        if (ips != null) {
            try {
                return (List<FloatingIp>)_api.getObjects(FloatingIp.class, ips);
            } catch (IOException ex) {
                s_logger.debug(ex);
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean isSystemDefaultNetworkPolicy(NetworkPolicy policy) {
        if (policy.getName().equals("default-network-policy")) {
            return true;
        }
        return false;
    }
}
