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
package com.cloud.configuration;

import java.net.URI;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.affinity.AffinityGroup;
import org.apache.cloudstack.affinity.AffinityGroupService;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.api.command.admin.config.UpdateCfgCmd;
import org.apache.cloudstack.api.command.admin.network.CreateNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateNetworkOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.CreateDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.CreateServiceOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.DeleteDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.DeleteServiceOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.UpdateDiskOfferingCmd;
import org.apache.cloudstack.api.command.admin.offering.UpdateServiceOfferingCmd;
import org.apache.cloudstack.api.command.admin.pod.DeletePodCmd;
import org.apache.cloudstack.api.command.admin.pod.UpdatePodCmd;
import org.apache.cloudstack.api.command.admin.region.CreatePortableIpRangeCmd;
import org.apache.cloudstack.api.command.admin.region.DeletePortableIpRangeCmd;
import org.apache.cloudstack.api.command.admin.region.ListPortableIpRangesCmd;
import org.apache.cloudstack.api.command.admin.vlan.CreateVlanIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.DedicatePublicIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.DeleteVlanIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.ReleasePublicIpRangeCmd;
import org.apache.cloudstack.api.command.admin.zone.CreateZoneCmd;
import org.apache.cloudstack.api.command.admin.zone.DeleteZoneCmd;
import org.apache.cloudstack.api.command.admin.zone.UpdateZoneCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkOfferingsCmd;
import org.apache.cloudstack.config.Configuration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.region.PortableIp;
import org.apache.cloudstack.region.PortableIpDao;
import org.apache.cloudstack.region.PortableIpRange;
import org.apache.cloudstack.region.PortableIpRangeDao;
import org.apache.cloudstack.region.PortableIpRangeVO;
import org.apache.cloudstack.region.PortableIpVO;
import org.apache.cloudstack.region.Region;
import org.apache.cloudstack.region.RegionVO;
import org.apache.cloudstack.region.dao.RegionDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterLinkLocalIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterDetailsDao;
import com.cloud.dc.dao.DataCenterIpAddressDao;
import com.cloud.dc.dao.DataCenterLinkLocalIpAddressDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentClusterPlanner;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.gpu.GPU;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.network.vpc.VpcManager;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offering.NetworkOffering.Detail;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Grouping;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.server.ConfigurationServer;
import com.cloud.server.ManagementService;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.test.IPRangeConfig;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicIpAlias;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicIpAliasDao;
import com.cloud.vm.dao.NicIpAliasVO;
import com.cloud.vm.dao.NicSecondaryIpDao;

@Local(value = {ConfigurationManager.class, ConfigurationService.class})
public class ConfigurationManagerImpl extends ManagerBase implements ConfigurationManager, ConfigurationService, Configurable {
    public static final Logger s_logger = Logger.getLogger(ConfigurationManagerImpl.class);

    @Inject
    EntityManager _entityMgr;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    ConfigDepot _configDepot;
    @Inject
    HostPodDao _podDao;
    @Inject
    AccountVlanMapDao _accountVlanMapDao;
    @Inject
    PodVlanMapDao _podVlanMapDao;
    @Inject
    DataCenterDao _zoneDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    ServiceOfferingDao _serviceOfferingDao;
    @Inject
    ServiceOfferingDetailsDao _serviceOfferingDetailsDao;
    @Inject
    DiskOfferingDao _diskOfferingDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    IPAddressDao _publicIpAddressDao;
    @Inject
    DataCenterIpAddressDao _privateIpAddressDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    NetworkService _networkSvc;
    @Inject
    NetworkModel _networkModel;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    AlertManager _alertMgr;
    // @com.cloud.utils.component.Inject(adapter = SecurityChecker.class)
    List<SecurityChecker> _secChecker;

    @Inject
    CapacityDao _capacityDao;
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    ProjectManager _projectMgr;
    @Inject
    DataStoreManager _dataStoreMgr;
    @Inject
    NetworkOfferingServiceMapDao _ntwkOffServiceMapDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkTrafficTypeDao _trafficTypeDao;
    @Inject
    NicDao _nicDao;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    UserDao _userDao;
    @Inject
    PortableIpRangeDao _portableIpRangeDao;
    @Inject
    RegionDao _regionDao;
    @Inject
    PortableIpDao _portableIpDao;
    @Inject
    ConfigurationServer _configServer;
    @Inject
    DataCenterDetailsDao _dcDetailsDao;
    @Inject
    ClusterDetailsDao _clusterDetailsDao;
    @Inject
    StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject
    AccountDetailsDao _accountDetailsDao;
    @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    NicSecondaryIpDao _nicSecondaryIpDao;
    @Inject
    NicIpAliasDao _nicIpAliasDao;
    @Inject
    public ManagementService _mgr;
    @Inject
    DedicatedResourceDao _dedicatedDao;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    AffinityGroupDao _affinityGroupDao;
    @Inject
    AffinityGroupService _affinityGroupService;
    @Inject
    StorageManager _storageManager;

    // FIXME - why don't we have interface for DataCenterLinkLocalIpAddressDao?
    @Inject
    protected DataCenterLinkLocalIpAddressDao _linkLocalIpAllocDao;

    private int _maxVolumeSizeInGb = Integer.parseInt(Config.MaxVolumeSize.getDefaultValue());
    private long _defaultPageSize = Long.parseLong(Config.DefaultPageSize.getDefaultValue());
    private static final String DOMAIN_NAME_PATTERN = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{1,63}$";
    protected Set<String> configValuesForValidation;
    private Set<String> weightBasedParametersForValidation;
    private Set<String> overprovisioningFactorsForValidation;

    public static final ConfigKey<Boolean> SystemVMUseLocalStorage = new ConfigKey<Boolean>(Boolean.class, "system.vm.use.local.storage", "Advanced", "false",
            "Indicates whether to use local storage pools or shared storage pools for system VMs.", false, ConfigKey.Scope.Zone, null);

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        final String maxVolumeSizeInGbString = _configDao.getValue(Config.MaxVolumeSize.key());
        _maxVolumeSizeInGb = NumbersUtil.parseInt(maxVolumeSizeInGbString, Integer.parseInt(Config.MaxVolumeSize.getDefaultValue()));

        final String defaultPageSizeString = _configDao.getValue(Config.DefaultPageSize.key());
        _defaultPageSize = NumbersUtil.parseLong(defaultPageSizeString, Long.parseLong(Config.DefaultPageSize.getDefaultValue()));

        populateConfigValuesForValidationSet();
        weightBasedParametersForValidation();
        overProvisioningFactorsForValidation();
        return true;
    }

    private void populateConfigValuesForValidationSet() {
        configValuesForValidation = new HashSet<String>();
        configValuesForValidation.add("event.purge.interval");
        configValuesForValidation.add("account.cleanup.interval");
        configValuesForValidation.add("alert.wait");
        configValuesForValidation.add("consoleproxy.capacityscan.interval");
        configValuesForValidation.add("consoleproxy.loadscan.interval");
        configValuesForValidation.add("expunge.interval");
        configValuesForValidation.add("host.stats.interval");
        configValuesForValidation.add("investigate.retry.interval");
        configValuesForValidation.add("migrate.retry.interval");
        configValuesForValidation.add("network.gc.interval");
        configValuesForValidation.add("ping.interval");
        configValuesForValidation.add("snapshot.poll.interval");
        configValuesForValidation.add("stop.retry.interval");
        configValuesForValidation.add("storage.stats.interval");
        configValuesForValidation.add("storage.cleanup.interval");
        configValuesForValidation.add("wait");
        configValuesForValidation.add("xenserver.heartbeat.interval");
        configValuesForValidation.add("xenserver.heartbeat.timeout");
        configValuesForValidation.add("ovm3.heartbeat.interval");
        configValuesForValidation.add("ovm3.heartbeat.timeout");
        configValuesForValidation.add("incorrect.login.attempts.allowed");
        configValuesForValidation.add("vm.password.length");
    }

    private void weightBasedParametersForValidation() {
        weightBasedParametersForValidation = new HashSet<String>();
        weightBasedParametersForValidation.add(AlertManager.CPUCapacityThreshold.key());
        weightBasedParametersForValidation.add(AlertManager.StorageAllocatedCapacityThreshold.key());
        weightBasedParametersForValidation.add(AlertManager.StorageCapacityThreshold.key());
        weightBasedParametersForValidation.add(AlertManager.MemoryCapacityThreshold.key());
        weightBasedParametersForValidation.add(Config.PublicIpCapacityThreshold.key());
        weightBasedParametersForValidation.add(Config.PrivateIpCapacityThreshold.key());
        weightBasedParametersForValidation.add(Config.SecondaryStorageCapacityThreshold.key());
        weightBasedParametersForValidation.add(Config.VlanCapacityThreshold.key());
        weightBasedParametersForValidation.add(Config.DirectNetworkPublicIpCapacityThreshold.key());
        weightBasedParametersForValidation.add(Config.LocalStorageCapacityThreshold.key());
        weightBasedParametersForValidation.add(CapacityManager.StorageAllocatedCapacityDisableThreshold.key());
        weightBasedParametersForValidation.add(CapacityManager.StorageCapacityDisableThreshold.key());
        weightBasedParametersForValidation.add(DeploymentClusterPlanner.ClusterCPUCapacityDisableThreshold.key());
        weightBasedParametersForValidation.add(DeploymentClusterPlanner.ClusterMemoryCapacityDisableThreshold.key());
        weightBasedParametersForValidation.add(Config.AgentLoadThreshold.key());
        weightBasedParametersForValidation.add(Config.VmUserDispersionWeight.key());

    }

    private void overProvisioningFactorsForValidation() {
        overprovisioningFactorsForValidation = new HashSet<String>();
        overprovisioningFactorsForValidation.add(CapacityManager.MemOverprovisioningFactor.key());
        overprovisioningFactorsForValidation.add(CapacityManager.CpuOverprovisioningFactor.key());
        overprovisioningFactorsForValidation.add(CapacityManager.StorageOverprovisioningFactor.key());
    }

    @Override
    public boolean start() {

        // TODO : this may not be a good place to do integrity check here, we
        // put it here as we need _alertMgr to be properly
        // configured
        // before we can use it

        // As it is so common for people to forget about configuring
        // management.network.cidr,
        final String mgtCidr = _configDao.getValue(Config.ManagementNetwork.key());
        if (mgtCidr == null || mgtCidr.trim().isEmpty()) {
            final String[] localCidrs = NetUtils.getLocalCidrs();
            if (localCidrs != null && localCidrs.length > 0) {
                s_logger.warn("Management network CIDR is not configured originally. Set it default to " + localCidrs[0]);

                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_MANAGMENT_NODE, 0, new Long(0), "Management network CIDR is not configured originally. Set it default to "
                        + localCidrs[0], "");
                _configDao.update(Config.ManagementNetwork.key(), Config.ManagementNetwork.getCategory(), localCidrs[0]);
            } else {
                s_logger.warn("Management network CIDR is not properly configured and we are not able to find a default setting");
                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_MANAGMENT_NODE, 0, new Long(0),
                        "Management network CIDR is not properly configured and we are not able to find a default setting", "");
            }
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @DB
    public String updateConfiguration(final long userId, final String name, final String category, final String value, final String scope, final Long resourceId) {
        final String validationMsg = validateConfigurationValue(name, value, scope);

        if (validationMsg != null) {
            s_logger.error("Invalid configuration option, name: " + name + ", value:" + value);
            throw new InvalidParameterValueException(validationMsg);
        }

        // If scope of the parameter is given then it needs to be updated in the
        // corresponding details table,
        // if scope is mentioned as global or not mentioned then it is normal
        // global parameter updation
        if (scope != null && !scope.isEmpty() && !ConfigKey.Scope.Global.toString().equalsIgnoreCase(scope)) {
            switch (ConfigKey.Scope.valueOf(scope)) {
            case Zone:
                final DataCenterVO zone = _zoneDao.findById(resourceId);
                if (zone == null) {
                    throw new InvalidParameterValueException("unable to find zone by id " + resourceId);
                }
                _dcDetailsDao.addDetail(resourceId, name, value, true);
                break;
            case Cluster:
                final ClusterVO cluster = _clusterDao.findById(resourceId);
                if (cluster == null) {
                    throw new InvalidParameterValueException("unable to find cluster by id " + resourceId);
                }
                ClusterDetailsVO clusterDetailsVO = _clusterDetailsDao.findDetail(resourceId, name);
                if (clusterDetailsVO == null) {
                    clusterDetailsVO = new ClusterDetailsVO(resourceId, name, value);
                    _clusterDetailsDao.persist(clusterDetailsVO);
                } else {
                    clusterDetailsVO.setValue(value);
                    _clusterDetailsDao.update(clusterDetailsVO.getId(), clusterDetailsVO);
                }
                break;

            case StoragePool:
                final StoragePoolVO pool = _storagePoolDao.findById(resourceId);
                if (pool == null) {
                    throw new InvalidParameterValueException("unable to find storage pool by id " + resourceId);
                }
                if(name.equals(CapacityManager.StorageOverprovisioningFactor.key())) {
                    if(pool.getPoolType() != StoragePoolType.NetworkFilesystem && pool.getPoolType() != StoragePoolType.VMFS) {
                        throw new InvalidParameterValueException("Unable to update  storage pool with id " + resourceId + ". Overprovision not supported for " + pool.getPoolType());
                    }
                }

                _storagePoolDetailsDao.addDetail(resourceId, name, value, true);

                break;

            case Account:
                final AccountVO account = _accountDao.findById(resourceId);
                if (account == null) {
                    throw new InvalidParameterValueException("unable to find account by id " + resourceId);
                }
                AccountDetailVO accountDetailVO = _accountDetailsDao.findDetail(resourceId, name);
                if (accountDetailVO == null) {
                    accountDetailVO = new AccountDetailVO(resourceId, name, value);
                    _accountDetailsDao.persist(accountDetailVO);
                } else {
                    accountDetailVO.setValue(value);
                    _accountDetailsDao.update(accountDetailVO.getId(), accountDetailVO);
                }
                break;
            default:
                throw new InvalidParameterValueException("Scope provided is invalid");
            }
            return value;
        }

        // Execute all updates in a single transaction
        final TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();

        if (!_configDao.update(name, category, value)) {
            s_logger.error("Failed to update configuration option, name: " + name + ", value:" + value);
            throw new CloudRuntimeException("Failed to update configuration value. Please contact Cloud Support.");
        }

        PreparedStatement pstmt = null;
        if (Config.XenServerGuestNetwork.key().equalsIgnoreCase(name)) {
            final String sql = "update host_details set value=? where name=?";
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "guest.network.device");

                pstmt.executeUpdate();
            } catch (final Throwable e) {
                throw new CloudRuntimeException("Failed to update guest.network.device in host_details due to exception ", e);
            }
        } else if (Config.XenServerPrivateNetwork.key().equalsIgnoreCase(name)) {
            final String sql = "update host_details set value=? where name=?";
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "private.network.device");

                pstmt.executeUpdate();
            } catch (final Throwable e) {
                throw new CloudRuntimeException("Failed to update private.network.device in host_details due to exception ", e);
            }
        } else if (Config.XenServerPublicNetwork.key().equalsIgnoreCase(name)) {
            final String sql = "update host_details set value=? where name=?";
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "public.network.device");

                pstmt.executeUpdate();
            } catch (final Throwable e) {
                throw new CloudRuntimeException("Failed to update public.network.device in host_details due to exception ", e);
            }
        } else if (Config.XenServerStorageNetwork1.key().equalsIgnoreCase(name)) {
            final String sql = "update host_details set value=? where name=?";
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "storage.network.device1");

                pstmt.executeUpdate();
            } catch (final Throwable e) {
                throw new CloudRuntimeException("Failed to update storage.network.device1 in host_details due to exception ", e);
            }
        } else if (Config.XenServerStorageNetwork2.key().equals(name)) {
            final String sql = "update host_details set value=? where name=?";
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "storage.network.device2");

                pstmt.executeUpdate();
            } catch (final Throwable e) {
                throw new CloudRuntimeException("Failed to update storage.network.device2 in host_details due to exception ", e);
            }
        } else if (Config.SecStorageSecureCopyCert.key().equalsIgnoreCase(name)) {
            //FIXME - Ideally there should be a listener model to listen to global config changes and be able to take action gracefully.
            //Expire the download urls
            final String sqlTemplate = "update template_store_ref set download_url_created=?";
            final String sqlVolume = "update volume_store_ref set download_url_created=?";
            try {
                // Change for templates
                pstmt = txn.prepareAutoCloseStatement(sqlTemplate);
                pstmt.setDate(1, new Date(-1l));// Set the time before the epoch time.
                pstmt.executeUpdate();
                // Change for volumes
                pstmt = txn.prepareAutoCloseStatement(sqlVolume);
                pstmt.setDate(1, new Date(-1l));// Set the time before the epoch time.
                pstmt.executeUpdate();
                // Cleanup the download urls
                _storageManager.cleanupDownloadUrls();
            } catch (final Throwable e) {
                throw new CloudRuntimeException("Failed to clean up download URLs in template_store_ref or volume_store_ref due to exception ", e);
            }
        }

        txn.commit();
        return _configDao.getValue(name);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_CONFIGURATION_VALUE_EDIT, eventDescription = "updating configuration")
    public Configuration updateConfiguration(final UpdateCfgCmd cmd) throws InvalidParameterValueException {
        final Long userId = CallContext.current().getCallingUserId();
        final String name = cmd.getCfgName();
        String value = cmd.getValue();
        final Long zoneId = cmd.getZoneId();
        final Long clusterId = cmd.getClusterId();
        final Long storagepoolId = cmd.getStoragepoolId();
        final Long accountId = cmd.getAccountId();
        CallContext.current().setEventDetails(" Name: " + name + " New Value: " + (name.toLowerCase().contains("password") ? "*****" : value == null ? "" : value));
        // check if config value exists
        final ConfigurationVO config = _configDao.findByName(name);
        String catergory = null;

        // FIX ME - All configuration parameters are not moved from config.java to configKey
        if (config == null) {
            if (_configDepot.get(name) == null) {
                s_logger.warn("Probably the component manager where configuration variable " + name + " is defined needs to implement Configurable interface");
                throw new InvalidParameterValueException("Config parameter with name " + name + " doesn't exist");
            }
            catergory = _configDepot.get(name).category();
        } else {
            catergory = config.getCategory();
        }

        if (value == null) {
            return _configDao.findByName(name);
        }

        value = value.trim();

        if (value.isEmpty() || value.equals("null")) {
            value = null;
        }

        String scope = null;
        Long id = null;
        int paramCountCheck = 0;

        if (zoneId != null) {
            scope = ConfigKey.Scope.Zone.toString();
            id = zoneId;
            paramCountCheck++;
        }
        if (clusterId != null) {
            scope = ConfigKey.Scope.Cluster.toString();
            id = clusterId;
            paramCountCheck++;
        }
        if (accountId != null) {
            scope = ConfigKey.Scope.Account.toString();
            id = accountId;
            paramCountCheck++;
        }
        if (storagepoolId != null) {
            scope = ConfigKey.Scope.StoragePool.toString();
            id = storagepoolId;
            paramCountCheck++;
        }

        if (paramCountCheck > 1) {
            throw new InvalidParameterValueException("cannot handle multiple IDs, provide only one ID corresponding to the scope");
        }

        final String updatedValue = updateConfiguration(userId, name, catergory, value, scope, id);
        if (value == null && updatedValue == null || updatedValue.equalsIgnoreCase(value)) {
            return _configDao.findByName(name);
        } else {
            throw new CloudRuntimeException("Unable to update configuration parameter " + name);
        }
    }

    private String validateConfigurationValue(final String name, String value, final String scope) {

        final ConfigurationVO cfg = _configDao.findByName(name);
        if (cfg == null) {
            s_logger.error("Missing configuration variable " + name + " in configuration table");
            return "Invalid configuration variable.";
        }

        final String configScope = cfg.getScope();
        if (scope != null) {
            if (!configScope.contains(scope)) {
                s_logger.error("Invalid scope id provided for the parameter " + name);
                return "Invalid scope id provided for the parameter " + name;
            }
        }
        Class<?> type = null;
        final Config c = Config.getConfig(name);
        if (c == null) {
            s_logger.warn("Did not find configuration " + name + " in Config.java. Perhaps moved to ConfigDepot");
            final ConfigKey<?> configKey = _configDepot.get(name);
            if(configKey == null) {
                s_logger.warn("Did not find configuration " + name + " in ConfigDepot too.");
                return null;
            }
            type = configKey.type();
        } else {
            type = c.getType();
        }

        String errMsg = null;
        try {
            if (type.equals(Integer.class)) {
                errMsg = "There was error in trying to parse value: " + value + ". Please enter a valid integer value for parameter " + name;
                Integer.parseInt(value);
            } else if (type.equals(Float.class)) {
                errMsg = "There was error in trying to parse value: " + value + ". Please enter a valid float value for parameter " + name;
                Float.parseFloat(value);
            }
        } catch (final Exception e) {
            // catching generic exception as some throws NullPointerException and some throws NumberFormatExcpeion
            s_logger.error(errMsg);
            return errMsg;
        }

        if (value == null) {
            if (type.equals(Boolean.class)) {
                return "Please enter either 'true' or 'false'.";
            }
            if (overprovisioningFactorsForValidation.contains(name)) {
                final String msg = "value cannot be null for the parameter " + name;
                s_logger.error(msg);
                return msg;
            }
            return null;
        }

        value = value.trim();
        try {
            if (overprovisioningFactorsForValidation.contains(name) && Float.parseFloat(value) < 1f) {
                final String msg = name + " should be greater than or equal to 1";
                s_logger.error(msg);
                throw new InvalidParameterValueException(msg);
            }
        } catch (final NumberFormatException e) {
            final String msg = "There was an error trying to parse the float value for: " + name;
            s_logger.error(msg);
            throw new InvalidParameterValueException(msg);
        }

        if (type.equals(Boolean.class)) {
            if (!(value.equals("true") || value.equals("false"))) {
                s_logger.error("Configuration variable " + name + " is expecting true or false instead of " + value);
                return "Please enter either 'true' or 'false'.";
            }
            return null;
        }

        if (type.equals(Integer.class) && configValuesForValidation.contains(name)) {
            try {
                final int val = Integer.parseInt(value);
                if (val <= 0) {
                    throw new InvalidParameterValueException("Please enter a positive value for the configuration parameter:" + name);
                }
                //TODO - better validation for all password pamameters
                if ("vm.password.length".equalsIgnoreCase(name) && val < 6) {
                    throw new InvalidParameterValueException("Please enter a value greater than 6 for the configuration parameter:" + name);
                }
            } catch (final NumberFormatException e) {
                s_logger.error("There was an error trying to parse the integer value for:" + name);
                throw new InvalidParameterValueException("There was an error trying to parse the integer value for:" + name);
            }
        }

        if (type.equals(Float.class)) {
            try {
                final Float val = Float.parseFloat(value);
                if (weightBasedParametersForValidation.contains(name) && (val < 0f || val > 1f)) {
                    throw new InvalidParameterValueException("Please enter a value between 0 and 1 for the configuration parameter: " + name);
                }
            } catch (final NumberFormatException e) {
                s_logger.error("There was an error trying to parse the float value for:" + name);
                throw new InvalidParameterValueException("There was an error trying to parse the float value for:" + name);
            }
        }

        if(c == null ) {
            //range validation has to be done per case basis, for now
            //return in case of Configkey parameters
            return null;
        }

        final String range = c.getRange();
        if (range == null) {
            return null;
        }

        if (type.equals(String.class)) {
            if (range.equals("privateip")) {
                try {
                    if (!NetUtils.isSiteLocalAddress(value)) {
                        s_logger.error("privateip range " + value + " is not a site local address for configuration variable " + name);
                        return "Please enter a site local IP address.";
                    }
                } catch (final NullPointerException e) {
                    s_logger.error("Error parsing ip address for " + name);
                    throw new InvalidParameterValueException("Error parsing ip address");
                }
            } else if (range.equals("netmask")) {
                if (!NetUtils.isValidNetmask(value)) {
                    s_logger.error("netmask " + value + " is not a valid net mask for configuration variable " + name);
                    return "Please enter a valid netmask.";
                }
            } else if (range.equals("hypervisorList")) {
                final String[] hypervisors = value.split(",");
                if (hypervisors == null) {
                    return "Please enter hypervisor list, separated by comma";
                }
                for (final String hypervisor : hypervisors) {
                    if (HypervisorType.getType(hypervisor) == HypervisorType.Any || HypervisorType.getType(hypervisor) == HypervisorType.None) {
                        return "Please enter a valid hypervisor type";
                    }
                }
            } else if (range.equalsIgnoreCase("instanceName")) {
                if (!NetUtils.verifyInstanceName(value)) {
                    return "Instance name can not contain hyphen, space or plus sign";
                }
            } else if (range.equalsIgnoreCase("domainName")) {
                String domainName = value;
                if (value.startsWith("*")) {
                    domainName = value.substring(2); //skip the "*."
                }
                //max length for FQDN is 253 + 2, code adds xxx-xxx-xxx-xxx to domain name when creating URL
                if (domainName.length() >= 238 || !domainName.matches(DOMAIN_NAME_PATTERN)) {
                    return "Please enter a valid string for domain name, prefixed with '*.' if applicable";
                }
            } else if (range.equals("routes")) {
                final String[] routes = value.split(",");
                for (final String route : routes) {
                    if (route != null) {
                        final String routeToVerify = route.trim();
                        if (!NetUtils.isValidCIDR(routeToVerify)) {
                            throw new InvalidParameterValueException("Invalid value for blacklisted route: " + route + ". Valid format is list"
                                    + " of cidrs separated by coma. Example: 10.1.1.0/24,192.168.0.0/24");
                        }
                    }
                }
            } else {
                final String[] options = range.split(",");
                for (final String option : options) {
                    if (option.trim().equalsIgnoreCase(value)) {
                        return null;
                    }
                }
                s_logger.error("configuration value for " + name + " is invalid");
                return "Please enter : " + range;

            }
        } else if (type.equals(Integer.class)) {
            final String[] options = range.split("-");
            if (options.length != 2) {
                final String msg = "configuration range " + range + " for " + name + " is invalid";
                s_logger.error(msg);
                return msg;
            }
            final int min = Integer.parseInt(options[0]);
            final int max = Integer.parseInt(options[1]);
            final int val = Integer.parseInt(value);
            if (val < min || val > max) {
                s_logger.error("configuration value for " + name + " is invalid");
                return "Please enter : " + range;
            }
        }
        return null;
    }

    private boolean podHasAllocatedPrivateIPs(final long podId) {
        final HostPodVO pod = _podDao.findById(podId);
        final int count = _privateIpAddressDao.countIPs(podId, pod.getDataCenterId(), true);
        return count > 0;
    }

    @DB
    protected void checkIfPodIsDeletable(final long podId) {
        final List<List<String>> tablesToCheck = new ArrayList<List<String>>();

        final HostPodVO pod = _podDao.findById(podId);

        // Check if there are allocated private IP addresses in the pod
        if (_privateIpAddressDao.countIPs(podId, pod.getDataCenterId(), true) != 0) {
            throw new CloudRuntimeException("There are private IP addresses allocated for this pod");
        }

        final List<String> volumes = new ArrayList<String>();
        volumes.add(0, "volumes");
        volumes.add(1, "pod_id");
        volumes.add(2, "there are storage volumes for this pod");
        tablesToCheck.add(volumes);

        final List<String> host = new ArrayList<String>();
        host.add(0, "host");
        host.add(1, "pod_id");
        host.add(2, "there are servers running in this pod");
        tablesToCheck.add(host);

        final List<String> vmInstance = new ArrayList<String>();
        vmInstance.add(0, "vm_instance");
        vmInstance.add(1, "pod_id");
        vmInstance.add(2, "there are virtual machines running in this pod");
        tablesToCheck.add(vmInstance);

        final List<String> cluster = new ArrayList<String>();
        cluster.add(0, "cluster");
        cluster.add(1, "pod_id");
        cluster.add(2, "there are clusters in this pod");
        tablesToCheck.add(cluster);

        for (final List<String> table : tablesToCheck) {
            final String tableName = table.get(0);
            final String column = table.get(1);
            final String errorMsg = table.get(2);

            String dbName;
            if (tableName.equals("event") || tableName.equals("cloud_usage") || tableName.equals("usage_vm_instance") || tableName.equals("usage_ip_address")
                    || tableName.equals("usage_network") || tableName.equals("usage_job") || tableName.equals("account") || tableName.equals("user_statistics")) {
                dbName = "cloud_usage";
            } else {
                dbName = "cloud";
            }

            String selectSql = "SELECT * FROM `?`.`?` WHERE ? = ?";

            if(tableName.equals("vm_instance")) {
                selectSql += " AND state != ? AND removed IS NULL";
            }

            if (tableName.equals("host") || tableName.equals("cluster") || tableName.equals("volumes")) {
                selectSql += " and removed IS NULL";
            }

            final TransactionLegacy txn = TransactionLegacy.currentTxn();
            try {
                final PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
                stmt.setString(1,dbName);
                stmt.setString(2,tableName);
                stmt.setString(3,column);
                stmt.setLong(4, podId);
                if(tableName.equals("vm_instance")) {
                    stmt.setString(5, VirtualMachine.State.Expunging.toString());
                }
                final ResultSet rs = stmt.executeQuery();
                if (rs != null && rs.next()) {
                    throw new CloudRuntimeException("The pod cannot be deleted because " + errorMsg);
                }
            } catch (final SQLException ex) {
                throw new CloudRuntimeException("The Management Server failed to detect if pod is deletable. Please contact Cloud Support.");
            }
        }
    }

    private void checkPodAttributes(final long podId, final String podName, final long zoneId, final String gateway, final String cidr, final String startIp, final String endIp, final String allocationStateStr,
            final boolean checkForDuplicates, final boolean skipGatewayOverlapCheck) {
        if (checkForDuplicates) {
            // Check if the pod already exists
            if (validPod(podName, zoneId)) {
                throw new InvalidParameterValueException("A pod with name: " + podName + " already exists in zone " + zoneId + ". Please specify a different pod name. ");
            }
        }

        String cidrAddress;
        long cidrSize;
        // Get the individual cidrAddress and cidrSize values, if the CIDR is
        // valid. If it's not valid, return an error.
        if (NetUtils.isValidCIDR(cidr)) {
            cidrAddress = getCidrAddress(cidr);
            cidrSize = getCidrSize(cidr);
        } else {
            throw new InvalidParameterValueException("Please enter a valid CIDR for pod: " + podName);
        }

        // Check if the IP range is valid
        checkIpRange(startIp, endIp, cidrAddress, cidrSize);

        // Check if the IP range overlaps with the public ip
        checkOverlapPublicIpRange(zoneId, startIp, endIp);

        // Check if the gateway is a valid IP address
        if (!NetUtils.isValidIp(gateway)) {
            throw new InvalidParameterValueException("The gateway is not a valid IP address.");
        }

        // Check if the gateway is in the CIDR subnet
        if (!NetUtils.getCidrSubNet(gateway, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
            throw new InvalidParameterValueException("The gateway is not in the CIDR subnet.");
        }

        // Don't allow gateway to overlap with start/endIp
        if (!skipGatewayOverlapCheck) {
            if (NetUtils.ipRangesOverlap(startIp, endIp, gateway, gateway)) {
                throw new InvalidParameterValueException("The gateway shouldn't overlap start/end ip addresses");
            }
        }

        final String checkPodCIDRs = _configDao.getValue("check.pod.cidrs");
        if (checkPodCIDRs == null || checkPodCIDRs.trim().isEmpty() || Boolean.parseBoolean(checkPodCIDRs)) {
            checkPodCidrSubnets(zoneId, podId, cidr);
            /*
             * Commenting out due to Bug 11593 - CIDR conflicts with zone when
             * extending pod but not when creating it
             *
             * checkCidrVlanOverlap(zoneId, cidr);
             */
        }

        if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
            try {
                Grouping.AllocationState.valueOf(allocationStateStr);
            } catch (final IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve Allocation State '" + allocationStateStr + "' to a supported state");
            }
        }
    }

    @Override
    @DB
    public boolean deletePod(final DeletePodCmd cmd) {
        final Long podId = cmd.getId();

        // Make sure the pod exists
        if (!validPod(podId)) {
            throw new InvalidParameterValueException("A pod with ID: " + podId + " does not exist.");
        }

        checkIfPodIsDeletable(podId);

        final HostPodVO pod = _podDao.findById(podId);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                // Delete private ip addresses for the pod if there are any
                final List<DataCenterIpAddressVO> privateIps = _privateIpAddressDao.listByPodIdDcId(podId, pod.getDataCenterId());
                if (!privateIps.isEmpty()) {
                    if (!_privateIpAddressDao.deleteIpAddressByPod(podId)) {
                        throw new CloudRuntimeException("Failed to cleanup private ip addresses for pod " + podId);
                    }
                }

                // Delete link local ip addresses for the pod
                final List<DataCenterLinkLocalIpAddressVO> localIps = _linkLocalIpAllocDao.listByPodIdDcId(podId, pod.getDataCenterId());
                if (!localIps.isEmpty()) {
                    if (!_linkLocalIpAllocDao.deleteIpAddressByPod(podId)) {
                        throw new CloudRuntimeException("Failed to cleanup private ip addresses for pod " + podId);
                    }
                }

                // Delete vlans associated with the pod
                final List<? extends Vlan> vlans = _networkModel.listPodVlans(podId);
                if (vlans != null && !vlans.isEmpty()) {
                    for (final Vlan vlan : vlans) {
                        _vlanDao.remove(vlan.getId());
                    }
                }

                // Delete corresponding capacity records
                _capacityDao.removeBy(null, null, podId, null, null);

                // Delete the pod
                if (!_podDao.remove(podId)) {
                    throw new CloudRuntimeException("Failed to delete pod " + podId);
                }

                // remove from dedicated resources
                final DedicatedResourceVO dr = _dedicatedDao.findByPodId(podId);
                if (dr != null) {
                    _dedicatedDao.remove(dr.getId());
                }
            }
        });

        return true;
    }

    @Override
    public Pod editPod(final UpdatePodCmd cmd) {
        return editPod(cmd.getId(), cmd.getPodName(), cmd.getStartIp(), cmd.getEndIp(), cmd.getGateway(), cmd.getNetmask(), cmd.getAllocationState());
    }

    @Override
    @DB
    public Pod editPod(final long id, String name, String startIp, String endIp, String gateway, String netmask, String allocationStateStr) {

        // verify parameters
        final HostPodVO pod = _podDao.findById(id);

        if (pod == null) {
            throw new InvalidParameterValueException("Unable to find pod by id " + id);
        }

        final String[] existingPodIpRange = pod.getDescription().split("-");
        String[] leftRangeToAdd = null;
        String[] rightRangeToAdd = null;
        boolean allowToDownsize = false;

        // If the gateway, CIDR, private IP range is being changed, check if the
        // pod has allocated private IP addresses
        if (podHasAllocatedPrivateIPs(id)) {

            if (netmask != null) {
                final long newCidr = NetUtils.getCidrSize(netmask);
                final long oldCidr = pod.getCidrSize();

                if (newCidr > oldCidr) {
                    throw new CloudRuntimeException("The specified pod has allocated private IP addresses, so its IP address range can be extended only");
                }
            }

            if (startIp != null && !startIp.equals(existingPodIpRange[0])) {
                if (NetUtils.ipRangesOverlap(startIp, null, existingPodIpRange[0], existingPodIpRange[1])) {
                    throw new CloudRuntimeException("The specified pod has allocated private IP addresses, so its IP address range can be extended only");
                } else {
                    leftRangeToAdd = new String[2];
                    final long endIpForUpdate = NetUtils.ip2Long(existingPodIpRange[0]) - 1;
                    leftRangeToAdd[0] = startIp;
                    leftRangeToAdd[1] = NetUtils.long2Ip(endIpForUpdate);
                }
            }

            if (endIp != null && !endIp.equals(existingPodIpRange[1])) {
                if (NetUtils.ipRangesOverlap(endIp, endIp, existingPodIpRange[0], existingPodIpRange[1])) {
                    throw new CloudRuntimeException("The specified pod has allocated private IP addresses, so its IP address range can be extended only");
                } else {
                    rightRangeToAdd = new String[2];
                    final long startIpForUpdate = NetUtils.ip2Long(existingPodIpRange[1]) + 1;
                    rightRangeToAdd[0] = NetUtils.long2Ip(startIpForUpdate);
                    rightRangeToAdd[1] = endIp;
                }
            }

        } else {
            allowToDownsize = true;
        }

        if (gateway == null) {
            gateway = pod.getGateway();
        }

        if (netmask == null) {
            netmask = NetUtils.getCidrNetmask(pod.getCidrSize());
        }

        final String oldPodName = pod.getName();
        if (name == null) {
            name = oldPodName;
        }

        if (gateway == null) {
            gateway = pod.getGateway();
        }

        if (startIp == null) {
            startIp = existingPodIpRange[0];
        }

        if (endIp == null) {
            endIp = existingPodIpRange[1];
        }

        if (allocationStateStr == null) {
            allocationStateStr = pod.getAllocationState().toString();
        }

        // Verify pod's attributes
        final String cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);
        final boolean checkForDuplicates = !oldPodName.equals(name);
        checkPodAttributes(id, name, pod.getDataCenterId(), gateway, cidr, startIp, endIp, allocationStateStr, checkForDuplicates, false);

        try {

            final String[] existingPodIpRangeFinal = existingPodIpRange;
            final String[] leftRangeToAddFinal = leftRangeToAdd;
            final String[] rightRangeToAddFinal = rightRangeToAdd;
            final boolean allowToDownsizeFinal = allowToDownsize;
            final String allocationStateStrFinal = allocationStateStr;
            final String startIpFinal = startIp;
            final String endIpFinal = endIp;
            final String nameFinal = name;
            final String gatewayFinal = gateway;
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(final TransactionStatus status) {
                    final long zoneId = pod.getDataCenterId();

                    String startIp = startIpFinal;
                    String endIp = endIpFinal;

                    if (!allowToDownsizeFinal) {
                        if (leftRangeToAddFinal != null) {
                            _zoneDao.addPrivateIpAddress(zoneId, pod.getId(), leftRangeToAddFinal[0], leftRangeToAddFinal[1]);
                        }

                        if (rightRangeToAddFinal != null) {
                            _zoneDao.addPrivateIpAddress(zoneId, pod.getId(), rightRangeToAddFinal[0], rightRangeToAddFinal[1]);
                        }

                    } else {
                        // delete the old range
                        _zoneDao.deletePrivateIpAddressByPod(pod.getId());

                        // add the new one
                        if (startIp == null) {
                            startIp = existingPodIpRangeFinal[0];
                        }

                        if (endIp == null) {
                            endIp = existingPodIpRangeFinal[1];
                        }

                        _zoneDao.addPrivateIpAddress(zoneId, pod.getId(), startIp, endIp);
                    }

                    pod.setName(nameFinal);
                    pod.setDataCenterId(zoneId);
                    pod.setGateway(gatewayFinal);
                    pod.setCidrAddress(getCidrAddress(cidr));
                    pod.setCidrSize(getCidrSize(cidr));

                    final String ipRange = startIp + "-" + endIp;
                    pod.setDescription(ipRange);
                    Grouping.AllocationState allocationState = null;
                    if (allocationStateStrFinal != null && !allocationStateStrFinal.isEmpty()) {
                        allocationState = Grouping.AllocationState.valueOf(allocationStateStrFinal);
                        pod.setAllocationState(allocationState);
                    }

                    _podDao.update(id, pod);
                }
            });
        } catch (final Exception e) {
            s_logger.error("Unable to edit pod due to " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to edit pod. Please contact Cloud Support.");
        }

        return pod;
    }

    @Override
    public Pod createPod(final long zoneId, final String name, final String startIp, final String endIp, final String gateway, final String netmask, String allocationState) {
        // Check if the gateway is a valid IP address
        if (!NetUtils.isValidIp(gateway)) {
            throw new InvalidParameterValueException("The gateway is invalid");
        }

        if (!NetUtils.isValidNetmask(netmask)) {
            throw new InvalidParameterValueException("The netmask is invalid");
        }

        final String cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);

        final Long userId = CallContext.current().getCallingUserId();

        if (allocationState == null) {
            allocationState = Grouping.AllocationState.Enabled.toString();
        }
        return createPod(userId.longValue(), name, zoneId, gateway, cidr, startIp, endIp, allocationState, false);
    }

    @Override
    @DB
    public HostPodVO createPod(final long userId, final String podName, final long zoneId, final String gateway, final String cidr, final String startIp, String endIp, final String allocationStateStr,
            final boolean skipGatewayOverlapCheck) {

        // Check if the zone is valid
        if (!validZone(zoneId)) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }

        // Check if zone is disabled
        final DataCenterVO zone = _zoneDao.findById(zoneId);
        final Account account = CallContext.current().getCallingAccount();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState()
                && !_accountMgr.isRootAdmin(account.getId())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zoneId);
        }

        final String cidrAddress = getCidrAddress(cidr);
        final int cidrSize = getCidrSize(cidr);

        // endIp is an optional parameter; if not specified - default it to the
        // end ip of the pod's cidr
        if (startIp != null) {
            if (endIp == null) {
                endIp = NetUtils.getIpRangeEndIpFromCidr(cidrAddress, cidrSize);
            }
        }

        // Validate new pod settings
        checkPodAttributes(-1, podName, zoneId, gateway, cidr, startIp, endIp, allocationStateStr, true, skipGatewayOverlapCheck);

        // Create the new pod in the database
        String ipRange;
        if (startIp != null) {
            ipRange = startIp + "-" + endIp;
        } else {
            throw new InvalidParameterValueException("Start ip is required parameter");
        }

        final HostPodVO podFinal = new HostPodVO(podName, zoneId, gateway, cidrAddress, cidrSize, ipRange);

        Grouping.AllocationState allocationState = null;
        if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
            allocationState = Grouping.AllocationState.valueOf(allocationStateStr);
            podFinal.setAllocationState(allocationState);
        }

        final String endIpFinal = endIp;
        return Transaction.execute(new TransactionCallback<HostPodVO>() {
            @Override
            public HostPodVO doInTransaction(final TransactionStatus status) {

                final HostPodVO pod = _podDao.persist(podFinal);

                if (startIp != null) {
                    _zoneDao.addPrivateIpAddress(zoneId, pod.getId(), startIp, endIpFinal);
                }

                final String[] linkLocalIpRanges = getLinkLocalIPRange();
                if (linkLocalIpRanges != null) {
                    _zoneDao.addLinkLocalIpAddress(zoneId, pod.getId(), linkLocalIpRanges[0], linkLocalIpRanges[1]);
                }

                return pod;
            }
        });
    }

    @DB
    protected void checkIfZoneIsDeletable(final long zoneId) {
        final List<List<String>> tablesToCheck = new ArrayList<List<String>>();

        final List<String> host = new ArrayList<String>();
        host.add(0, "host");
        host.add(1, "data_center_id");
        host.add(2, "there are servers running in this zone");
        tablesToCheck.add(host);

        final List<String> hostPodRef = new ArrayList<String>();
        hostPodRef.add(0, "host_pod_ref");
        hostPodRef.add(1, "data_center_id");
        hostPodRef.add(2, "there are pods in this zone");
        tablesToCheck.add(hostPodRef);

        final List<String> privateIP = new ArrayList<String>();
        privateIP.add(0, "op_dc_ip_address_alloc");
        privateIP.add(1, "data_center_id");
        privateIP.add(2, "there are private IP addresses allocated for this zone");
        tablesToCheck.add(privateIP);

        final List<String> publicIP = new ArrayList<String>();
        publicIP.add(0, "user_ip_address");
        publicIP.add(1, "data_center_id");
        publicIP.add(2, "there are public IP addresses allocated for this zone");
        tablesToCheck.add(publicIP);

        final List<String> vmInstance = new ArrayList<String>();
        vmInstance.add(0, "vm_instance");
        vmInstance.add(1, "data_center_id");
        vmInstance.add(2, "there are virtual machines running in this zone");
        tablesToCheck.add(vmInstance);

        final List<String> volumes = new ArrayList<String>();
        volumes.add(0, "volumes");
        volumes.add(1, "data_center_id");
        volumes.add(2, "there are storage volumes for this zone");
        tablesToCheck.add(volumes);

        final List<String> physicalNetworks = new ArrayList<String>();
        physicalNetworks.add(0, "physical_network");
        physicalNetworks.add(1, "data_center_id");
        physicalNetworks.add(2, "there are physical networks in this zone");
        tablesToCheck.add(physicalNetworks);

        final List<String> vmwareDcs = new ArrayList<String>();
        vmwareDcs.add(0, "vmware_data_center_zone_map");
        vmwareDcs.add(1, "zone_id");
        vmwareDcs.add(2, "there are VMware datacenters associated with this zone. Remove VMware DC from this zone.");
        tablesToCheck.add(vmwareDcs);

        for (final List<String> table : tablesToCheck) {
            final String tableName = table.get(0);
            final String column = table.get(1);
            final String errorMsg = table.get(2);

            final String dbName = "cloud";

            String selectSql = "SELECT * FROM `?`.`?` WHERE ? = ?";

            if (tableName.equals("op_dc_vnet_alloc")) {
                selectSql += " AND taken IS NOT NULL";
            }

            if (tableName.equals("user_ip_address")) {
                selectSql += " AND state!='Free'";
            }

            if (tableName.equals("op_dc_ip_address_alloc")) {
                selectSql += " AND taken IS NOT NULL";
            }

            if (tableName.equals("host_pod_ref") || tableName.equals("host") || tableName.equals("volumes") || tableName.equals("physical_network")) {
                selectSql += " AND removed is NULL";
            }

            if (tableName.equals("vm_instance")) {
                selectSql += " AND state != ? AND removed IS NULL";
            }

            final TransactionLegacy txn = TransactionLegacy.currentTxn();
            try {
                final PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
                stmt.setString(1,dbName);
                stmt.setString(2,tableName);
                stmt.setString(3,column);
                stmt.setLong(4, zoneId);
                if (tableName.equals("vm_instance")) {
                    stmt.setString(5, VirtualMachine.State.Expunging.toString());
                }
                final ResultSet rs = stmt.executeQuery();
                if (rs != null && rs.next()) {
                    throw new CloudRuntimeException("The zone is not deletable because " + errorMsg);
                }
            } catch (final SQLException ex) {
                throw new CloudRuntimeException("The Management Server failed to detect if zone is deletable. Please contact Cloud Support.");
            }
        }

    }

    private void checkZoneParameters(final String zoneName, final String dns1, final String dns2, final String internalDns1, final String internalDns2, final boolean checkForDuplicates, final Long domainId,
            final String allocationStateStr, final String ip6Dns1, final String ip6Dns2) {
        if (checkForDuplicates) {
            // Check if a zone with the specified name already exists
            if (validZone(zoneName)) {
                throw new InvalidParameterValueException("A zone with that name already exists. Please specify a unique zone name.");
            }
        }

        // check if valid domain
        if (domainId != null) {
            final DomainVO domain = _domainDao.findById(domainId);

            if (domain == null) {
                throw new InvalidParameterValueException("Please specify a valid domain id");
            }
        }

        // Check IP validity for DNS addresses
        // Empty strings is a valid input -- hence the length check
        if (dns1 != null && dns1.length() > 0 && !NetUtils.isValidIp(dns1)) {
            throw new InvalidParameterValueException("Please enter a valid IP address for DNS1");
        }

        if (dns2 != null && dns2.length() > 0 && !NetUtils.isValidIp(dns2)) {
            throw new InvalidParameterValueException("Please enter a valid IP address for DNS2");
        }

        if (internalDns1 != null && internalDns1.length() > 0 && !NetUtils.isValidIp(internalDns1)) {
            throw new InvalidParameterValueException("Please enter a valid IP address for internal DNS1");
        }

        if (internalDns2 != null && internalDns2.length() > 0 && !NetUtils.isValidIp(internalDns2)) {
            throw new InvalidParameterValueException("Please enter a valid IP address for internal DNS2");
        }

        if (ip6Dns1 != null && ip6Dns1.length() > 0 && !NetUtils.isValidIpv6(ip6Dns1)) {
            throw new InvalidParameterValueException("Please enter a valid IPv6 address for IP6 DNS1");
        }

        if (ip6Dns2 != null && ip6Dns2.length() > 0 && !NetUtils.isValidIpv6(ip6Dns2)) {
            throw new InvalidParameterValueException("Please enter a valid IPv6 address for IP6 DNS2");
        }

        if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
            try {
                Grouping.AllocationState.valueOf(allocationStateStr);
            } catch (final IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve Allocation State '" + allocationStateStr + "' to a supported state");
            }
        }
    }

    private void checkIpRange(final String startIp, final String endIp, final String cidrAddress, final long cidrSize) {
        if (!NetUtils.isValidIp(startIp)) {
            throw new InvalidParameterValueException("The start address of the IP range is not a valid IP address.");
        }

        if (endIp != null && !NetUtils.isValidIp(endIp)) {
            throw new InvalidParameterValueException("The end address of the IP range is not a valid IP address.");
        }

        if (!NetUtils.getCidrSubNet(startIp, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
            throw new InvalidParameterValueException("The start address of the IP range is not in the CIDR subnet.");
        }

        if (endIp != null && !NetUtils.getCidrSubNet(endIp, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
            throw new InvalidParameterValueException("The end address of the IP range is not in the CIDR subnet.");
        }

        if (endIp != null && NetUtils.ip2Long(startIp) > NetUtils.ip2Long(endIp)) {
            throw new InvalidParameterValueException("The start IP address must have a lower value than the end IP address.");
        }

    }

    private void checkOverlapPublicIpRange(final Long zoneId, final String startIp, final String endIp) {
        final long privateStartIp = NetUtils.ip2Long(startIp);
        final long privateEndIp = NetUtils.ip2Long(endIp);

        final List<IPAddressVO> existingPublicIPs = _publicIpAddressDao.listByDcId(zoneId);
        for (final IPAddressVO publicIPVO : existingPublicIPs) {
            final long publicIP = NetUtils.ip2Long(publicIPVO.getAddress().addr());
            if (publicIP >= privateStartIp && publicIP <= privateEndIp) {
                throw new InvalidParameterValueException("The Start IP and endIP address range overlap with Public IP :" + publicIPVO.getAddress().addr());
            }
        }
    }

    private void checkOverlapPrivateIpRange(final Long zoneId, final String startIp, final String endIp) {

        final List<HostPodVO> podsInZone = _podDao.listByDataCenterId(zoneId);
        for (final HostPodVO hostPod : podsInZone) {
            final String[] IpRange = hostPod.getDescription().split("-");
            if (IpRange[0] == null || IpRange[1] == null) {
                continue;
            }
            if (!NetUtils.isValidIp(IpRange[0]) || !NetUtils.isValidIp(IpRange[1])) {
                continue;
            }
            if (NetUtils.ipRangesOverlap(startIp, endIp, IpRange[0], IpRange[1])) {
                throw new InvalidParameterValueException("The Start IP and endIP address range overlap with private IP :" + IpRange[0] + ":" + IpRange[1]);
            }
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_ZONE_DELETE, eventDescription = "deleting zone", async = false)
    public boolean deleteZone(final DeleteZoneCmd cmd) {

        final Long zoneId = cmd.getId();

        // Make sure the zone exists
        if (!validZone(zoneId)) {
            throw new InvalidParameterValueException("A zone with ID: " + zoneId + " does not exist.");
        }

        checkIfZoneIsDeletable(zoneId);

        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(final TransactionStatus status) {
                // delete vlans for this zone
                final List<VlanVO> vlans = _vlanDao.listByZone(zoneId);
                for (final VlanVO vlan : vlans) {
                    _vlanDao.remove(vlan.getId());
                }

                final boolean success = _zoneDao.remove(zoneId);

                if (success) {
                    // delete all capacity records for the zone
                    _capacityDao.removeBy(null, zoneId, null, null, null);
                    // remove from dedicated resources
                    final DedicatedResourceVO dr = _dedicatedDao.findByZoneId(zoneId);
                    if (dr != null) {
                        _dedicatedDao.remove(dr.getId());
                        // find the group associated and check if there are any more
                        // resources under that group
                        final List<DedicatedResourceVO> resourcesInGroup = _dedicatedDao.listByAffinityGroupId(dr.getAffinityGroupId());
                        if (resourcesInGroup.isEmpty()) {
                            // delete the group
                            _affinityGroupService.deleteAffinityGroup(dr.getAffinityGroupId(), null, null, null);
                        }
                    }
                }

                return success;
            }
        });
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_ZONE_EDIT, eventDescription = "editing zone", async = false)
    public DataCenter editZone(final UpdateZoneCmd cmd) {
        // Parameter validation as from execute() method in V1
        final Long zoneId = cmd.getId();
        String zoneName = cmd.getZoneName();
        String dns1 = cmd.getDns1();
        String dns2 = cmd.getDns2();
        String ip6Dns1 = cmd.getIp6Dns1();
        String ip6Dns2 = cmd.getIp6Dns2();
        String internalDns1 = cmd.getInternalDns1();
        String internalDns2 = cmd.getInternalDns2();
        String guestCidr = cmd.getGuestCidrAddress();
        final List<String> dnsSearchOrder = cmd.getDnsSearchOrder();
        final Boolean isPublic = cmd.isPublic();
        final String allocationStateStr = cmd.getAllocationState();
        final String dhcpProvider = cmd.getDhcpProvider();
        final Map<?, ?> detailsMap = cmd.getDetails();
        final String networkDomain = cmd.getDomain();
        final Boolean localStorageEnabled = cmd.getLocalStorageEnabled();

        final Map<String, String> newDetails = new HashMap<String, String>();
        if (detailsMap != null) {
            final Collection<?> zoneDetailsCollection = detailsMap.values();
            final Iterator<?> iter = zoneDetailsCollection.iterator();
            while (iter.hasNext()) {
                final HashMap<?, ?> detail = (HashMap<?, ?>)iter.next();
                final String key = (String)detail.get("key");
                final String value = (String)detail.get("value");
                if (key == null || value == null) {
                    throw new InvalidParameterValueException(
                            "Invalid Zone Detail specified, fields 'key' and 'value' cannot be null, please specify details in the form:  details[0].key=XXX&details[0].value=YYY");
                }
                // validate the zone detail keys are known keys
                /*
                 * if(!ZoneConfig.doesKeyExist(key)){ throw new
                 * InvalidParameterValueException
                 * ("Invalid Zone Detail parameter: "+ key); }
                 */
                newDetails.put(key, value);
            }
        }

        // add the domain prefix list to details if not null
        if (dnsSearchOrder != null) {
            for (final String dom : dnsSearchOrder) {
                if (!NetUtils.verifyDomainName(dom)) {
                    throw new InvalidParameterValueException(
                            "Invalid network domain suffixes. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                    + "and the hyphen ('-'); can't start or end with \"-\"");
                }
            }
            newDetails.put(ZoneConfig.DnsSearchOrder.getName(), StringUtils.join(dnsSearchOrder, ","));
        }

        final DataCenterVO zone = _zoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("unable to find zone by id " + zoneId);
        }

        if (zoneName == null) {
            zoneName = zone.getName();
        }

        if (guestCidr != null && !NetUtils.validateGuestCidr(guestCidr)) {
            throw new InvalidParameterValueException("Please enter a valid guest cidr");
        }

        // Make sure the zone exists
        if (!validZone(zoneId)) {
            throw new InvalidParameterValueException("A zone with ID: " + zoneId + " does not exist.");
        }

        final String oldZoneName = zone.getName();

        if (zoneName == null) {
            zoneName = oldZoneName;
        }

        if (dns1 == null) {
            dns1 = zone.getDns1();
        }

        if (dns2 == null) {
            dns2 = zone.getDns2();
        }

        if (ip6Dns1 == null) {
            ip6Dns1 = zone.getIp6Dns1();
        }

        if (ip6Dns2 == null) {
            ip6Dns2 = zone.getIp6Dns2();
        }

        if (internalDns1 == null) {
            internalDns1 = zone.getInternalDns1();
        }

        if (internalDns2 == null) {
            internalDns2 = zone.getInternalDns2();
        }

        if (guestCidr == null) {
            guestCidr = zone.getGuestNetworkCidr();
        }

        // validate network domain
        if (networkDomain != null && !networkDomain.isEmpty()) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        final boolean checkForDuplicates = !zoneName.equals(oldZoneName);
        checkZoneParameters(zoneName, dns1, dns2, internalDns1, internalDns2, checkForDuplicates, null, allocationStateStr, ip6Dns1, ip6Dns2);// not allowing updating
        // domain associated with
        // a zone, once created

        zone.setName(zoneName);
        zone.setDns1(dns1);
        zone.setDns2(dns2);
        zone.setIp6Dns1(ip6Dns1);
        zone.setIp6Dns2(ip6Dns2);
        zone.setInternalDns1(internalDns1);
        zone.setInternalDns2(internalDns2);
        zone.setGuestNetworkCidr(guestCidr);
        if (localStorageEnabled != null) {
            zone.setLocalStorageEnabled(localStorageEnabled.booleanValue());
        }

        if (networkDomain != null) {
            if (networkDomain.isEmpty()) {
                zone.setDomain(null);
            } else {
                zone.setDomain(networkDomain);
            }
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                final Map<String, String> updatedDetails = new HashMap<String, String>();
                _zoneDao.loadDetails(zone);
                if (zone.getDetails() != null) {
                    updatedDetails.putAll(zone.getDetails());
                }
                updatedDetails.putAll(newDetails);
                zone.setDetails(updatedDetails);

                if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
                    final Grouping.AllocationState allocationState = Grouping.AllocationState.valueOf(allocationStateStr);

                    if (allocationState == Grouping.AllocationState.Enabled) {
                        // check if zone has necessary trafficTypes before enabling
                        try {
                            PhysicalNetwork mgmtPhyNetwork;
                            // zone should have a physical network with management
                            // traffiType
                            mgmtPhyNetwork = _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Management);
                            if (NetworkType.Advanced == zone.getNetworkType() && !zone.isSecurityGroupEnabled()) {
                                // advanced zone without SG should have a physical
                                // network with public Thpe
                                _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Public);
                            }

                            try {
                                _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Storage);
                            } catch (final InvalidParameterValueException noStorage) {
                                final PhysicalNetworkTrafficTypeVO mgmtTraffic = _trafficTypeDao.findBy(mgmtPhyNetwork.getId(), TrafficType.Management);
                                _networkSvc.addTrafficTypeToPhysicalNetwork(mgmtPhyNetwork.getId(), TrafficType.Storage.toString(), "vlan", mgmtTraffic.getXenNetworkLabel(),
                                        mgmtTraffic.getKvmNetworkLabel(), mgmtTraffic.getVmwareNetworkLabel(), mgmtTraffic.getSimulatorNetworkLabel(), mgmtTraffic.getVlan(),
                                        mgmtTraffic.getHypervNetworkLabel(), mgmtTraffic.getOvm3NetworkLabel());
                                s_logger.info("No storage traffic type was specified by admin, create default storage traffic on physical network " + mgmtPhyNetwork.getId()
                                        + " with same configure of management traffic type");
                            }
                        } catch (final InvalidParameterValueException ex) {
                            throw new InvalidParameterValueException("Cannot enable this Zone since: " + ex.getMessage());
                        }
                    }
                    zone.setAllocationState(allocationState);
                }

                if (dhcpProvider != null) {
                    zone.setDhcpProvider(dhcpProvider);
                }

                // update a private zone to public; not vice versa
                if (isPublic != null && isPublic) {
                    zone.setDomainId(null);
                    zone.setDomain(null);

                    // release the dedication for this zone
                    final DedicatedResourceVO resource = _dedicatedDao.findByZoneId(zoneId);
                    Long resourceId = null;
                    if (resource != null) {
                        resourceId = resource.getId();
                        if (!_dedicatedDao.remove(resourceId)) {
                            throw new CloudRuntimeException("Failed to delete dedicated Zone Resource " + resourceId);
                        }
                        // find the group associated and check if there are any more
                        // resources under that group
                        final List<DedicatedResourceVO> resourcesInGroup = _dedicatedDao.listByAffinityGroupId(resource.getAffinityGroupId());
                        if (resourcesInGroup.isEmpty()) {
                            // delete the group
                            _affinityGroupService.deleteAffinityGroup(resource.getAffinityGroupId(), null, null, null);
                        }
                    }
                }

                if (!_zoneDao.update(zoneId, zone)) {
                    throw new CloudRuntimeException("Failed to edit zone. Please contact Cloud Support.");
                }
            }
        });

        return zone;
    }

    @Override
    @DB
    public DataCenterVO createZone(final long userId, final String zoneName, final String dns1, final String dns2, final String internalDns1, final String internalDns2, final String guestCidr, final String domain,
            final Long domainId, final NetworkType zoneType, final String allocationStateStr, final String networkDomain, final boolean isSecurityGroupEnabled, final boolean isLocalStorageEnabled,
            final String ip6Dns1, final String ip6Dns2) {

        // checking the following params outside checkzoneparams method as we do
        // not use these params for updatezone
        // hence the method below is generic to check for common params
        if (guestCidr != null && !NetUtils.validateGuestCidr(guestCidr)) {
            throw new InvalidParameterValueException("Please enter a valid guest cidr");
        }

        // Validate network domain
        if (networkDomain != null) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        checkZoneParameters(zoneName, dns1, dns2, internalDns1, internalDns2, true, domainId, allocationStateStr, ip6Dns1, ip6Dns2);

        final byte[] bytes = (zoneName + System.currentTimeMillis()).getBytes();
        final String zoneToken = UUID.nameUUIDFromBytes(bytes).toString();

        // Create the new zone in the database
        final DataCenterVO zoneFinal = new DataCenterVO(zoneName, null, dns1, dns2, internalDns1, internalDns2, guestCidr, domain, domainId, zoneType, zoneToken, networkDomain,
                isSecurityGroupEnabled, isLocalStorageEnabled, ip6Dns1, ip6Dns2);
        if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
            final Grouping.AllocationState allocationState = Grouping.AllocationState.valueOf(allocationStateStr);
            zoneFinal.setAllocationState(allocationState);
        } else {
            // Zone will be disabled since 3.0. Admin should enable it after
            // physical network and providers setup.
            zoneFinal.setAllocationState(Grouping.AllocationState.Disabled);
        }

        return Transaction.execute(new TransactionCallback<DataCenterVO>() {
            @Override
            public DataCenterVO doInTransaction(final TransactionStatus status) {
                final DataCenterVO zone = _zoneDao.persist(zoneFinal);
                if (domainId != null) {
                    // zone is explicitly dedicated to this domain
                    // create affinity group associated and dedicate the zone.
                    final AffinityGroup group = createDedicatedAffinityGroup(null, domainId, null);
                    final DedicatedResourceVO dedicatedResource = new DedicatedResourceVO(zone.getId(), null, null, null, domainId, null, group.getId());
                    _dedicatedDao.persist(dedicatedResource);
                }

                // Create default system networks
                createDefaultSystemNetworks(zone.getId());

                return zone;
            }
        });
    }

    private AffinityGroup createDedicatedAffinityGroup(String affinityGroupName, final Long domainId, final Long accountId) {
        if (affinityGroupName == null) {
            // default to a groupname with account/domain information
            affinityGroupName = "ZoneDedicatedGrp-domain-" + domainId + (accountId != null ? "-acct-" + accountId : "");
        }

        AffinityGroup group = null;
        String accountName = null;

        if (accountId != null) {
            final AccountVO account = _accountDao.findById(accountId);
            accountName = account.getAccountName();

            group = _affinityGroupDao.findByAccountAndName(accountId, affinityGroupName);
            if (group != null) {
                return group;
            }
        } else {
            // domain level group
            group = _affinityGroupDao.findDomainLevelGroupByName(domainId, affinityGroupName);
            if (group != null) {
                return group;
            }
        }

        group = _affinityGroupService.createAffinityGroupInternal(accountName, domainId, affinityGroupName, "ExplicitDedication", "dedicated resources group");

        return group;

    }

    @Override
    public void createDefaultSystemNetworks(final long zoneId) throws ConcurrentOperationException {
        final DataCenterVO zone = _zoneDao.findById(zoneId);
        final String networkDomain = null;
        // Create public, management, control and storage networks as a part of
        // the zone creation
        if (zone != null) {
            final List<NetworkOfferingVO> ntwkOff = _networkOfferingDao.listSystemNetworkOfferings();

            for (final NetworkOfferingVO offering : ntwkOff) {
                final DataCenterDeployment plan = new DataCenterDeployment(zone.getId(), null, null, null, null, null);
                final NetworkVO userNetwork = new NetworkVO();

                final Account systemAccount = _accountDao.findById(Account.ACCOUNT_ID_SYSTEM);

                BroadcastDomainType broadcastDomainType = null;
                if (offering.getTrafficType() == TrafficType.Management) {
                    broadcastDomainType = BroadcastDomainType.Native;
                } else if (offering.getTrafficType() == TrafficType.Control) {
                    broadcastDomainType = BroadcastDomainType.LinkLocal;
                } else if (offering.getTrafficType() == TrafficType.Public) {
                    if (zone.getNetworkType() == NetworkType.Advanced && !zone.isSecurityGroupEnabled() || zone.getNetworkType() == NetworkType.Basic) {
                        broadcastDomainType = BroadcastDomainType.Vlan;
                    } else {
                        continue; // so broadcastDomainType remains null! why have None/Undecided/UnKnown?
                    }
                } else if (offering.getTrafficType() == TrafficType.Guest) {
                    continue;
                }

                userNetwork.setBroadcastDomainType(broadcastDomainType);
                userNetwork.setNetworkDomain(networkDomain);
                _networkMgr.setupNetwork(systemAccount, offering, userNetwork, plan, null, null, false, Domain.ROOT_DOMAIN, null, null, null, true);
            }
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ZONE_CREATE, eventDescription = "creating zone", async = false)
    public DataCenter createZone(final CreateZoneCmd cmd) {
        // grab parameters from the command
        final Long userId = CallContext.current().getCallingUserId();
        final String zoneName = cmd.getZoneName();
        final String dns1 = cmd.getDns1();
        final String dns2 = cmd.getDns2();
        final String ip6Dns1 = cmd.getIp6Dns1();
        final String ip6Dns2 = cmd.getIp6Dns2();
        final String internalDns1 = cmd.getInternalDns1();
        final String internalDns2 = cmd.getInternalDns2();
        final String guestCidr = cmd.getGuestCidrAddress();
        final Long domainId = cmd.getDomainId();
        final String type = cmd.getNetworkType();
        Boolean isBasic = false;
        String allocationState = cmd.getAllocationState();
        final String networkDomain = cmd.getDomain();
        boolean isSecurityGroupEnabled = cmd.getSecuritygroupenabled();
        final boolean isLocalStorageEnabled = cmd.getLocalStorageEnabled();

        if (allocationState == null) {
            allocationState = Grouping.AllocationState.Disabled.toString();
        }

        if (!type.equalsIgnoreCase(NetworkType.Basic.toString()) && !type.equalsIgnoreCase(NetworkType.Advanced.toString())) {
            throw new InvalidParameterValueException("Invalid zone type; only Advanced and Basic values are supported");
        } else if (type.equalsIgnoreCase(NetworkType.Basic.toString())) {
            isBasic = true;
        }

        final NetworkType zoneType = isBasic ? NetworkType.Basic : NetworkType.Advanced;

        // error out when the parameter specified for Basic zone
        if (zoneType == NetworkType.Basic && guestCidr != null) {
            throw new InvalidParameterValueException("guestCidrAddress parameter is not supported for Basic zone");
        }

        DomainVO domainVO = null;

        if (domainId != null) {
            domainVO = _domainDao.findById(domainId);
        }

        if (zoneType == NetworkType.Basic) {
            isSecurityGroupEnabled = true;
        }

        return createZone(userId, zoneName, dns1, dns2, internalDns1, internalDns2, guestCidr, domainVO != null ? domainVO.getName() : null, domainId, zoneType, allocationState,
                networkDomain, isSecurityGroupEnabled, isLocalStorageEnabled, ip6Dns1, ip6Dns2);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_OFFERING_CREATE, eventDescription = "creating service offering")
    public ServiceOffering createServiceOffering(final CreateServiceOfferingCmd cmd) {
        final Long userId = CallContext.current().getCallingUserId();

        final String name = cmd.getServiceOfferingName();
        if (name == null || name.length() == 0) {
            throw new InvalidParameterValueException("Failed to create service offering: specify the name that has non-zero length");
        }

        final String displayText = cmd.getDisplayText();
        if (displayText == null || displayText.length() == 0) {
            throw new InvalidParameterValueException("Failed to create service offering " + name + ": specify the display text that has non-zero length");
        }

        final Integer cpuNumber = cmd.getCpuNumber();
        final Integer cpuSpeed = cmd.getCpuSpeed();
        final Integer memory = cmd.getMemory();

        //restricting the createserviceoffering to allow setting all or none of the dynamic parameters to null
        if (cpuNumber == null || cpuSpeed == null || memory == null) {
            if (cpuNumber != null || cpuSpeed != null || memory != null) {
                throw new InvalidParameterValueException("For creating a custom compute offering cpu, cpu speed and memory all should be null");
            }
        }

        if (cpuNumber != null && (cpuNumber.intValue() <= 0 || cpuNumber.longValue() > Integer.MAX_VALUE)) {
            throw new InvalidParameterValueException("Failed to create service offering " + name + ": specify the cpu number value between 1 and " + Integer.MAX_VALUE);
        }
        if (cpuSpeed != null && (cpuSpeed.intValue() < 0 || cpuSpeed.longValue() > Integer.MAX_VALUE)) {
            throw new InvalidParameterValueException("Failed to create service offering " + name + ": specify the cpu speed value between 0 and " + Integer.MAX_VALUE);
        }
        if (memory != null && (memory.intValue() < 32 || memory.longValue() > Integer.MAX_VALUE)) {
            throw new InvalidParameterValueException("Failed to create service offering " + name + ": specify the memory value between 32 and " + Integer.MAX_VALUE + " MB");
        }

        // check if valid domain
        if (cmd.getDomainId() != null && _domainDao.findById(cmd.getDomainId()) == null) {
            throw new InvalidParameterValueException("Please specify a valid domain id");
        }

        final Boolean offerHA = cmd.getOfferHa();

        boolean localStorageRequired = false;
        final String storageType = cmd.getStorageType();
        if (storageType != null) {
            if (storageType.equalsIgnoreCase(ServiceOffering.StorageType.local.toString())) {
                if(offerHA) {
                    throw new InvalidParameterValueException("HA offering with local storage is not supported. ");
                }
                localStorageRequired = true;
            } else if (!storageType.equalsIgnoreCase(ServiceOffering.StorageType.shared.toString())) {
                throw new InvalidParameterValueException("Invalid storage type " + storageType + " specified, valid types are: 'local' and 'shared'");
            }
        }

        final Boolean limitCpuUse = cmd.GetLimitCpuUse();
        final Boolean volatileVm = cmd.getVolatileVm();

        final String vmTypeString = cmd.getSystemVmType();
        VirtualMachine.Type vmType = null;
        boolean allowNetworkRate = false;
        if (cmd.getIsSystem()) {
            if (vmTypeString == null || VirtualMachine.Type.DomainRouter.toString().toLowerCase().equals(vmTypeString)) {
                vmType = VirtualMachine.Type.DomainRouter;
                allowNetworkRate = true;
            } else if (VirtualMachine.Type.ConsoleProxy.toString().toLowerCase().equals(vmTypeString)) {
                vmType = VirtualMachine.Type.ConsoleProxy;
            } else if (VirtualMachine.Type.SecondaryStorageVm.toString().toLowerCase().equals(vmTypeString)) {
                vmType = VirtualMachine.Type.SecondaryStorageVm;
            } else if (VirtualMachine.Type.InternalLoadBalancerVm.toString().toLowerCase().equals(vmTypeString)) {
                vmType = VirtualMachine.Type.InternalLoadBalancerVm;
            } else {
                throw new InvalidParameterValueException("Invalid systemVmType. Supported types are: " + VirtualMachine.Type.DomainRouter + ", " + VirtualMachine.Type.ConsoleProxy
                        + ", " + VirtualMachine.Type.SecondaryStorageVm);
            }
        } else {
            allowNetworkRate = true;
        }

        if (cmd.getNetworkRate() != null) {
            if(!allowNetworkRate) {
                throw new InvalidParameterValueException("Network rate can be specified only for non-System offering and system offerings having \"domainrouter\" systemvmtype");
            }
            if(cmd.getNetworkRate().intValue() < 1) {
                throw new InvalidParameterValueException("Failed to create service offering " + name + ": specify the network rate value more than 0");
            }
        }

        if (cmd.getDeploymentPlanner() != null) {
            final List<String> planners = _mgr.listDeploymentPlanners();
            if (planners != null && !planners.isEmpty()) {
                if (!planners.contains(cmd.getDeploymentPlanner())) {
                    throw new InvalidParameterValueException("Invalid name for Deployment Planner specified, please use listDeploymentPlanners to get the valid set");
                }
            } else {
                throw new InvalidParameterValueException("No deployment planners found");
            }
        }

        return createServiceOffering(userId, cmd.getIsSystem(), vmType, cmd.getServiceOfferingName(), cpuNumber, memory, cpuSpeed, cmd.getDisplayText(),
                cmd.getProvisioningType(), localStorageRequired, offerHA, limitCpuUse, volatileVm, cmd.getTags(), cmd.getDomainId(), cmd.getHostTag(),
                cmd.getNetworkRate(), cmd.getDeploymentPlanner(), cmd.getDetails(), cmd.isCustomizedIops(), cmd.getMinIops(), cmd.getMaxIops(),
                cmd.getBytesReadRate(), cmd.getBytesWriteRate(), cmd.getIopsReadRate(), cmd.getIopsWriteRate(), cmd.getHypervisorSnapshotReserve());
    }

    protected ServiceOfferingVO createServiceOffering(final long userId, final boolean isSystem, final VirtualMachine.Type vmType,
            final String name, final Integer cpu, final Integer ramSize, final Integer speed, final String displayText, final String provisioningType, final boolean localStorageRequired,
            final boolean offerHA, final boolean limitResourceUse, final boolean volatileVm,  String tags, final Long domainId, final String hostTag,
            final Integer networkRate, final String deploymentPlanner, final Map<String, String> details, final Boolean isCustomizedIops, Long minIops, Long maxIops,
            Long bytesReadRate, Long bytesWriteRate, Long iopsReadRate, Long iopsWriteRate, final Integer hypervisorSnapshotReserve) {

        // Check if user exists in the system
        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        final Account account = _accountDao.findById(user.getAccountId());
        if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            if (domainId == null) {
                throw new InvalidParameterValueException("Unable to create public service offering by id " + userId + " because it is domain-admin");
            }
            if (tags != null || hostTag != null) {
                throw new InvalidParameterValueException("Unable to create service offering with storage tags or host tags by id " + userId + " because it is domain-admin");
            }
            if (! _domainDao.isChildDomain(account.getDomainId(), domainId)) {
                throw new InvalidParameterValueException("Unable to create service offering by another domain admin with id " + userId);
            }
        } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            throw new InvalidParameterValueException("Unable to create service offering by id " + userId + " because it is not root-admin or domain-admin");
        }

        final ProvisioningType typedProvisioningType = ProvisioningType.getProvisioningType(provisioningType);

        tags = StringUtils.cleanupTags(tags);

        ServiceOfferingVO offering = new ServiceOfferingVO(name, cpu, ramSize, speed, networkRate, null, offerHA,
                limitResourceUse, volatileVm, displayText, typedProvisioningType, localStorageRequired, false, tags, isSystem, vmType,
                domainId, hostTag, deploymentPlanner);

        if (isCustomizedIops != null) {
            bytesReadRate = null;
            bytesWriteRate = null;
            iopsReadRate = null;
            iopsWriteRate = null;

            if (isCustomizedIops) {
                minIops = null;
                maxIops = null;
            } else {
                if (minIops == null && maxIops == null) {
                    minIops = 0L;
                    maxIops = 0L;
                } else {
                    if (minIops == null || minIops <= 0) {
                        throw new InvalidParameterValueException("The min IOPS must be greater than 0.");
                    }

                    if (maxIops == null) {
                        maxIops = 0L;
                    }

                    if (minIops > maxIops) {
                        throw new InvalidParameterValueException("The min IOPS must be less than or equal to the max IOPS.");
                    }
                }
            }
        } else {
            minIops = null;
            maxIops = null;
        }

        offering.setCustomizedIops(isCustomizedIops);
        offering.setMinIops(minIops);
        offering.setMaxIops(maxIops);

        if (bytesReadRate != null && bytesReadRate > 0) {
            offering.setBytesReadRate(bytesReadRate);
        }
        if (bytesWriteRate != null && bytesWriteRate > 0) {
            offering.setBytesWriteRate(bytesWriteRate);
        }
        if (iopsReadRate != null && iopsReadRate > 0) {
            offering.setIopsReadRate(iopsReadRate);
        }
        if (iopsWriteRate != null && iopsWriteRate > 0) {
            offering.setIopsWriteRate(iopsWriteRate);
        }

        if (hypervisorSnapshotReserve != null && hypervisorSnapshotReserve < 0) {
            throw new InvalidParameterValueException("If provided, Hypervisor Snapshot Reserve must be greater than or equal to 0.");
        }

        offering.setHypervisorSnapshotReserve(hypervisorSnapshotReserve);

        List<ServiceOfferingDetailsVO> detailsVO = null;
        if (details != null) {
            // To have correct input, either both gpu card name and VGPU type should be passed or nothing should be passed.
            // Use XOR condition to verify that.
            final boolean entry1 = details.containsKey(GPU.Keys.pciDevice.toString());
            final boolean entry2 = details.containsKey(GPU.Keys.vgpuType.toString());
            if ((entry1 || entry2) && !(entry1 && entry2)) {
                throw new InvalidParameterValueException("Please specify the pciDevice and vgpuType correctly.");
            }
            detailsVO = new ArrayList<ServiceOfferingDetailsVO>();
            for (final Entry<String, String> detailEntry : details.entrySet()) {
                if (detailEntry.getKey().equals(GPU.Keys.pciDevice.toString())) {
                    if (detailEntry.getValue() == null) {
                        throw new InvalidParameterValueException("Please specify a GPU Card.");
                    }
                }
                if (detailEntry.getKey().equals(GPU.Keys.vgpuType.toString())) {
                    if (detailEntry.getValue() == null) {
                        throw new InvalidParameterValueException("vGPUType value cannot be null");
                    }
                }
                detailsVO.add(new ServiceOfferingDetailsVO(offering.getId(), detailEntry.getKey(), detailEntry.getValue(), true));
            }
        }

        if ((offering = _serviceOfferingDao.persist(offering)) != null) {
            if (detailsVO != null && !detailsVO.isEmpty()) {
                for (int index = 0; index < detailsVO.size(); index++) {
                    detailsVO.get(index).setResourceId(offering.getId());
                }
                _serviceOfferingDetailsDao.saveDetails(detailsVO);
            }
            CallContext.current().setEventDetails("Service offering id=" + offering.getId());
            return offering;
        } else {
            return null;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_OFFERING_EDIT, eventDescription = "updating service offering")
    public ServiceOffering updateServiceOffering(final UpdateServiceOfferingCmd cmd) {
        final String displayText = cmd.getDisplayText();
        final Long id = cmd.getId();
        final String name = cmd.getServiceOfferingName();
        final Integer sortKey = cmd.getSortKey();
        Long userId = CallContext.current().getCallingUserId();

        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

        // Verify input parameters
        final ServiceOffering offeringHandle = _entityMgr.findById(ServiceOffering.class, id);

        if (offeringHandle == null) {
            throw new InvalidParameterValueException("unable to find service offering " + id);
        }

        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        final Account account = _accountDao.findById(user.getAccountId());
        if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            if (offeringHandle.getDomainId() == null) {
                throw new InvalidParameterValueException("Unable to update public service offering by id " + userId + " because it is domain-admin");
            }
            if (! _domainDao.isChildDomain(account.getDomainId(), offeringHandle.getDomainId() )) {
                throw new InvalidParameterValueException("Unable to update service offering by another domain admin with id " + userId);
            }
        } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            throw new InvalidParameterValueException("Unable to update service offering by id " + userId + " because it is not root-admin or domain-admin");
        }

        final boolean updateNeeded = name != null || displayText != null || sortKey != null;
        if (!updateNeeded) {
            return _serviceOfferingDao.findById(id);
        }

        ServiceOfferingVO offering = _serviceOfferingDao.createForUpdate(id);

        if (name != null) {
            offering.setName(name);
        }

        if (displayText != null) {
            offering.setDisplayText(displayText);
        }

        if (sortKey != null) {
            offering.setSortKey(sortKey);
        }

        // Note: tag editing commented out for now; keeping the code intact,
        // might need to re-enable in next releases
        // if (tags != null)
        // {
        // if (tags.trim().isEmpty() && offeringHandle.getTags() == null)
        // {
        // //no new tags; no existing tags
        // offering.setTagsArray(csvTagsToList(null));
        // }
        // else if (!tags.trim().isEmpty() && offeringHandle.getTags() != null)
        // {
        // //new tags + existing tags
        // List<String> oldTags = csvTagsToList(offeringHandle.getTags());
        // List<String> newTags = csvTagsToList(tags);
        // oldTags.addAll(newTags);
        // offering.setTagsArray(oldTags);
        // }
        // else if(!tags.trim().isEmpty())
        // {
        // //new tags; NO existing tags
        // offering.setTagsArray(csvTagsToList(tags));
        // }
        // }

        if (_serviceOfferingDao.update(id, offering)) {
            offering = _serviceOfferingDao.findById(id);
            CallContext.current().setEventDetails("Service offering id=" + offering.getId());
            return offering;
        } else {
            return null;
        }
    }

    protected DiskOfferingVO createDiskOffering(final Long userId, final Long domainId, final String name, final String description, final String provisioningType,
            final Long numGibibytes, String tags, boolean isCustomized, final boolean localStorageRequired,
            final boolean isDisplayOfferingEnabled, final Boolean isCustomizedIops, Long minIops, Long maxIops,
            Long bytesReadRate, Long bytesWriteRate, Long iopsReadRate, Long iopsWriteRate,
            final Integer hypervisorSnapshotReserve) {
        long diskSize = 0;// special case for custom disk offerings
        if (numGibibytes != null && numGibibytes <= 0) {
            throw new InvalidParameterValueException("Please specify a disk size of at least 1 Gb.");
        } else if (numGibibytes != null && numGibibytes > _maxVolumeSizeInGb) {
            throw new InvalidParameterValueException("The maximum size for a disk is " + _maxVolumeSizeInGb + " Gb.");
        }
        final ProvisioningType typedProvisioningType = ProvisioningType.getProvisioningType(provisioningType);

        if (numGibibytes != null) {
            diskSize = numGibibytes * 1024 * 1024 * 1024;
        }

        if (diskSize == 0) {
            isCustomized = true;
        }

        if (isCustomizedIops != null) {
            bytesReadRate = null;
            bytesWriteRate = null;
            iopsReadRate = null;
            iopsWriteRate = null;

            if (isCustomizedIops) {
                minIops = null;
                maxIops = null;
            } else {
                if (minIops == null && maxIops == null) {
                    minIops = 0L;
                    maxIops = 0L;
                } else {
                    if (minIops == null || minIops <= 0) {
                        throw new InvalidParameterValueException("The min IOPS must be greater than 0.");
                    }

                    if (maxIops == null) {
                        maxIops = 0L;
                    }

                    if (minIops > maxIops) {
                        throw new InvalidParameterValueException("The min IOPS must be less than or equal to the max IOPS.");
                    }
                }
            }
        } else {
            minIops = null;
            maxIops = null;
        }

        // Check if user exists in the system
        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        final Account account = _accountDao.findById(user.getAccountId());
        if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            if (domainId == null) {
                throw new InvalidParameterValueException("Unable to create public disk offering by id " + userId + " because it is domain-admin");
            }
            if (tags != null) {
                throw new InvalidParameterValueException("Unable to create disk offering with storage tags by id " + userId + " because it is domain-admin");
            }
            if (! _domainDao.isChildDomain(account.getDomainId(), domainId)) {
                throw new InvalidParameterValueException("Unable to create disk offering by another domain admin with id " + userId);
            }
        } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            throw new InvalidParameterValueException("Unable to create disk offering by id " + userId + " because it is not root-admin or domain-admin");
        }

        tags = StringUtils.cleanupTags(tags);
        final DiskOfferingVO newDiskOffering = new DiskOfferingVO(domainId, name, description, typedProvisioningType, diskSize, tags, isCustomized,
                isCustomizedIops, minIops, maxIops);
        newDiskOffering.setUseLocalStorage(localStorageRequired);
        newDiskOffering.setDisplayOffering(isDisplayOfferingEnabled);

        if (bytesReadRate != null && bytesReadRate > 0) {
            newDiskOffering.setBytesReadRate(bytesReadRate);
        }
        if (bytesWriteRate != null && bytesWriteRate > 0) {
            newDiskOffering.setBytesWriteRate(bytesWriteRate);
        }
        if (iopsReadRate != null && iopsReadRate > 0) {
            newDiskOffering.setIopsReadRate(iopsReadRate);
        }
        if (iopsWriteRate != null && iopsWriteRate > 0) {
            newDiskOffering.setIopsWriteRate(iopsWriteRate);
        }

        if (hypervisorSnapshotReserve != null && hypervisorSnapshotReserve < 0) {
            throw new InvalidParameterValueException("If provided, Hypervisor Snapshot Reserve must be greater than or equal to 0.");
        }

        newDiskOffering.setHypervisorSnapshotReserve(hypervisorSnapshotReserve);

        CallContext.current().setEventDetails("Disk offering id=" + newDiskOffering.getId());
        final DiskOfferingVO offering = _diskOfferingDao.persist(newDiskOffering);
        if (offering != null) {
            CallContext.current().setEventDetails("Disk offering id=" + newDiskOffering.getId());
            return offering;
        } else {
            return null;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DISK_OFFERING_CREATE, eventDescription = "creating disk offering")
    public DiskOffering createDiskOffering(final CreateDiskOfferingCmd cmd) {
        final String name = cmd.getOfferingName();
        final String description = cmd.getDisplayText();
        final String provisioningType = cmd.getProvisioningType();
        final Long numGibibytes = cmd.getDiskSize();
        final boolean isDisplayOfferingEnabled = cmd.getDisplayOffering() != null ? cmd.getDisplayOffering() : true;
        final boolean isCustomized = cmd.isCustomized() != null ? cmd.isCustomized() : false; // false
        // by
        // default
        final String tags = cmd.getTags();
        // Long domainId = cmd.getDomainId() != null ? cmd.getDomainId() :
        // Long.valueOf(DomainVO.ROOT_DOMAIN); // disk offering
        // always gets created under the root domain.Bug # 6055 if not passed in
        // cmd
        final Long domainId = cmd.getDomainId();

        if (!isCustomized && numGibibytes == null) {
            throw new InvalidParameterValueException("Disksize is required for a non-customized disk offering");
        }

        if (isCustomized && numGibibytes != null) {
            throw new InvalidParameterValueException("Disksize is not allowed for a customized disk offering");
        }

        boolean localStorageRequired = false;
        final String storageType = cmd.getStorageType();
        if (storageType != null) {
            if (storageType.equalsIgnoreCase(ServiceOffering.StorageType.local.toString())) {
                localStorageRequired = true;
            } else if (!storageType.equalsIgnoreCase(ServiceOffering.StorageType.shared.toString())) {
                throw new InvalidParameterValueException("Invalid storage type " + storageType + " specified, valid types are: 'local' and 'shared'");
            }
        }

        final Boolean isCustomizedIops = cmd.isCustomizedIops();
        final Long minIops = cmd.getMinIops();
        final Long maxIops = cmd.getMaxIops();
        final Long bytesReadRate = cmd.getBytesReadRate();
        final Long bytesWriteRate = cmd.getBytesWriteRate();
        final Long iopsReadRate = cmd.getIopsReadRate();
        final Long iopsWriteRate = cmd.getIopsWriteRate();
        final Integer hypervisorSnapshotReserve = cmd.getHypervisorSnapshotReserve();

        final Long userId = CallContext.current().getCallingUserId();
        return createDiskOffering(userId, domainId, name, description, provisioningType, numGibibytes, tags, isCustomized,
                localStorageRequired, isDisplayOfferingEnabled, isCustomizedIops, minIops,
                maxIops, bytesReadRate, bytesWriteRate, iopsReadRate, iopsWriteRate, hypervisorSnapshotReserve);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DISK_OFFERING_EDIT, eventDescription = "updating disk offering")
    public DiskOffering updateDiskOffering(final UpdateDiskOfferingCmd cmd) {
        final Long diskOfferingId = cmd.getId();
        final String name = cmd.getDiskOfferingName();
        final String displayText = cmd.getDisplayText();
        final Integer sortKey = cmd.getSortKey();
        final Boolean displayDiskOffering = cmd.getDisplayOffering();

        // Check if diskOffering exists
        final DiskOffering diskOfferingHandle = _entityMgr.findById(DiskOffering.class, diskOfferingId);

        if (diskOfferingHandle == null) {
            throw new InvalidParameterValueException("Unable to find disk offering by id " + diskOfferingId);
        }

        Long userId = CallContext.current().getCallingUserId();
        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        final Account account = _accountDao.findById(user.getAccountId());
        if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            if (diskOfferingHandle.getDomainId() == null) {
                throw new InvalidParameterValueException("Unable to update public disk offering by id " + userId + " because it is domain-admin");
            }
            if (! _domainDao.isChildDomain(account.getDomainId(), diskOfferingHandle.getDomainId() )) {
                throw new InvalidParameterValueException("Unable to update disk offering by another domain admin with id " + userId);
            }
        } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            throw new InvalidParameterValueException("Unable to update disk offering by id " + userId + " because it is not root-admin or domain-admin");
        }

        final boolean updateNeeded = name != null || displayText != null || sortKey != null || displayDiskOffering != null;
        if (!updateNeeded) {
            return _diskOfferingDao.findById(diskOfferingId);
        }

        final DiskOfferingVO diskOffering = _diskOfferingDao.createForUpdate(diskOfferingId);

        if (name != null) {
            diskOffering.setName(name);
        }

        if (displayText != null) {
            diskOffering.setDisplayText(displayText);
        }

        if (sortKey != null) {
            diskOffering.setSortKey(sortKey);
        }

        if (displayDiskOffering != null) {
            diskOffering.setDisplayOffering(displayDiskOffering);
        }

        // Note: tag editing commented out for now;keeping the code intact,
        // might need to re-enable in next releases
        // if (tags != null)
        // {
        // if (tags.trim().isEmpty() && diskOfferingHandle.getTags() == null)
        // {
        // //no new tags; no existing tags
        // diskOffering.setTagsArray(csvTagsToList(null));
        // }
        // else if (!tags.trim().isEmpty() && diskOfferingHandle.getTags() !=
        // null)
        // {
        // //new tags + existing tags
        // List<String> oldTags = csvTagsToList(diskOfferingHandle.getTags());
        // List<String> newTags = csvTagsToList(tags);
        // oldTags.addAll(newTags);
        // diskOffering.setTagsArray(oldTags);
        // }
        // else if(!tags.trim().isEmpty())
        // {
        // //new tags; NO existing tags
        // diskOffering.setTagsArray(csvTagsToList(tags));
        // }
        // }

        if (_diskOfferingDao.update(diskOfferingId, diskOffering)) {
            CallContext.current().setEventDetails("Disk offering id=" + diskOffering.getId());
            return _diskOfferingDao.findById(diskOfferingId);
        } else {
            return null;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DISK_OFFERING_DELETE, eventDescription = "deleting disk offering")
    public boolean deleteDiskOffering(final DeleteDiskOfferingCmd cmd) {
        final Long diskOfferingId = cmd.getId();

        final DiskOfferingVO offering = _diskOfferingDao.findById(diskOfferingId);

        if (offering == null) {
            throw new InvalidParameterValueException("Unable to find disk offering by id " + diskOfferingId);
        }

        Long userId = CallContext.current().getCallingUserId();
        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        final Account account = _accountDao.findById(user.getAccountId());
        if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            if (offering.getDomainId() == null) {
                throw new InvalidParameterValueException("Unable to delete public disk offering by id " + userId + " because it is domain-admin");
            }
            if (! _domainDao.isChildDomain(account.getDomainId(), offering.getDomainId() )) {
                throw new InvalidParameterValueException("Unable to delete disk offering by another domain admin with id " + userId);
            }
        } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            throw new InvalidParameterValueException("Unable to delete disk offering by id " + userId + " because it is not root-admin or domain-admin");
        }

        offering.setState(DiskOffering.State.Inactive);
        if (_diskOfferingDao.update(offering.getId(), offering)) {
            CallContext.current().setEventDetails("Disk offering id=" + diskOfferingId);
            return true;
        } else {
            return false;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_OFFERING_DELETE, eventDescription = "deleting service offering")
    public boolean deleteServiceOffering(final DeleteServiceOfferingCmd cmd) {

        final Long offeringId = cmd.getId();
        Long userId = CallContext.current().getCallingUserId();

        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

        // Verify service offering id
        final ServiceOfferingVO offering = _serviceOfferingDao.findById(offeringId);
        if (offering == null) {
            throw new InvalidParameterValueException("unable to find service offering " + offeringId);
        }

        if (offering.getDefaultUse()) {
            throw new InvalidParameterValueException("Default service offerings cannot be deleted");
        }

        final User user = _userDao.findById(userId);
        if (user == null || user.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find active user by id " + userId);
        }
        final Account account = _accountDao.findById(user.getAccountId());
        if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            if (offering.getDomainId() == null) {
                throw new InvalidParameterValueException("Unable to delete public service offering by id " + userId + " because it is domain-admin");
            }
            if (! _domainDao.isChildDomain(account.getDomainId(), offering.getDomainId() )) {
                throw new InvalidParameterValueException("Unable to delete service offering by another domain admin with id " + userId);
            }
        } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            throw new InvalidParameterValueException("Unable to delete service offering by id " + userId + " because it is not root-admin or domain-admin");
        }

        offering.setState(DiskOffering.State.Inactive);
        if (_serviceOfferingDao.update(offeringId, offering)) {
            CallContext.current().setEventDetails("Service offering id=" + offeringId);
            return true;
        } else {
            return false;
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VLAN_IP_RANGE_CREATE, eventDescription = "creating vlan ip range", async = false)
    public Vlan createVlanAndPublicIpRange(final CreateVlanIpRangeCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException,
    ResourceAllocationException {
        Long zoneId = cmd.getZoneId();
        final Long podId = cmd.getPodId();
        final String startIP = cmd.getStartIp();
        String endIP = cmd.getEndIp();
        final String newVlanGateway = cmd.getGateway();
        final String newVlanNetmask = cmd.getNetmask();
        String vlanId = cmd.getVlan();
        // TODO decide if we should be forgiving or demand a valid and complete URI
        if (!(vlanId == null || "".equals(vlanId) || vlanId.startsWith(BroadcastDomainType.Vlan.scheme()))) {
            vlanId = BroadcastDomainType.Vlan.toUri(vlanId).toString();
        }
        final Boolean forVirtualNetwork = cmd.isForVirtualNetwork();
        Long networkId = cmd.getNetworkID();
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        final String accountName = cmd.getAccountName();
        final Long projectId = cmd.getProjectId();
        final Long domainId = cmd.getDomainId();
        final String startIPv6 = cmd.getStartIpv6();
        String endIPv6 = cmd.getEndIpv6();
        final String ip6Gateway = cmd.getIp6Gateway();
        final String ip6Cidr = cmd.getIp6Cidr();

        Account vlanOwner = null;

        final boolean ipv4 = startIP != null;
        final boolean ipv6 = startIPv6 != null;

        if (!ipv4 && !ipv6) {
            throw new InvalidParameterValueException("StartIP or StartIPv6 is missing in the parameters!");
        }

        if (ipv4) {
            // if end ip is not specified, default it to startIp
            if (endIP == null && startIP != null) {
                endIP = startIP;
            }
        }

        if (ipv6) {
            // if end ip is not specified, default it to startIp
            if (endIPv6 == null && startIPv6 != null) {
                endIPv6 = startIPv6;
            }
        }

        if (projectId != null) {
            if (accountName != null) {
                throw new InvalidParameterValueException("Account and projectId are mutually exclusive");
            }
            final Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }

            vlanOwner = _accountMgr.getAccount(project.getProjectAccountId());
        }

        if (accountName != null && domainId != null) {
            vlanOwner = _accountDao.findActiveAccount(accountName, domainId);
            if (vlanOwner == null) {
                throw new InvalidParameterValueException("Please specify a valid account.");
            } else if (vlanOwner.getId() == Account.ACCOUNT_ID_SYSTEM) {
                // by default vlan is dedicated to system account
                vlanOwner = null;
            }
        }

        // Verify that network exists
        Network network = null;
        if (networkId != null) {
            network = _networkDao.findById(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Unable to find network by id " + networkId);
            } else {
                zoneId = network.getDataCenterId();
                physicalNetworkId = network.getPhysicalNetworkId();
            }
        } else if (ipv6) {
            throw new InvalidParameterValueException("Only support IPv6 on extending existed network");
        }

        // Verify that zone exists
        final DataCenterVO zone = _zoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id " + zoneId);
        }

        if (ipv6) {
            if (network.getGuestType() != GuestType.Shared || zone.isSecurityGroupEnabled()) {
                throw new InvalidParameterValueException("Only support IPv6 on extending existed share network without SG");
            }
        }
        // verify that physical network exists
        PhysicalNetworkVO pNtwk = null;
        if (physicalNetworkId != null) {
            pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
            if (pNtwk == null) {
                throw new InvalidParameterValueException("Unable to find Physical Network with id=" + physicalNetworkId);
            }
            if (zoneId == null) {
                zoneId = pNtwk.getDataCenterId();
            }
        } else {
            if (zoneId == null) {
                throw new InvalidParameterValueException("");
            }
            // deduce physicalNetworkFrom Zone or Network.
            if (network != null && network.getPhysicalNetworkId() != null) {
                physicalNetworkId = network.getPhysicalNetworkId();
            } else {
                if (forVirtualNetwork) {
                    // default physical network with public traffic in the zone
                    physicalNetworkId = _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Public).getId();
                } else {
                    if (zone.getNetworkType() == DataCenter.NetworkType.Basic) {
                        // default physical network with guest traffic in the
                        // zone
                        physicalNetworkId = _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Guest).getId();
                    } else if (zone.getNetworkType() == DataCenter.NetworkType.Advanced) {
                        if (zone.isSecurityGroupEnabled()) {
                            physicalNetworkId = _networkModel.getDefaultPhysicalNetworkByZoneAndTrafficType(zoneId, TrafficType.Guest).getId();
                        } else {
                            throw new InvalidParameterValueException("Physical Network Id is null, please provide the Network id for Direct vlan creation ");
                        }
                    }
                }
            }
        }

        // Check if zone is enabled
        final Account caller = CallContext.current().getCallingAccount();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState()
                && !_accountMgr.isRootAdmin(caller.getId())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zoneId);
        }

        if (zone.isSecurityGroupEnabled() && zone.getNetworkType() != DataCenter.NetworkType.Basic && forVirtualNetwork) {
            throw new InvalidParameterValueException("Can't add virtual ip range into a zone with security group enabled");
        }

        // If networkId is not specified, and vlan is Virtual or Direct
        // Untagged, try to locate default networks
        if (forVirtualNetwork) {
            if (network == null) {
                // find default public network in the zone
                networkId = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, TrafficType.Public).getId();
                network = _networkModel.getNetwork(networkId);
            } else if (network.getGuestType() != null || network.getTrafficType() != TrafficType.Public) {
                throw new InvalidParameterValueException("Can't find Public network by id=" + networkId);
            }
        } else {
            if (network == null) {
                if (zone.getNetworkType() == DataCenter.NetworkType.Basic) {
                    networkId = _networkModel.getExclusiveGuestNetwork(zoneId).getId();
                    network = _networkModel.getNetwork(networkId);
                } else {
                    network = _networkModel.getNetworkWithSecurityGroupEnabled(zoneId);
                    if (network == null) {
                        throw new InvalidParameterValueException("Nework id is required for Direct vlan creation ");
                    }
                    networkId = network.getId();
                    zoneId = network.getDataCenterId();
                }
            } else if (network.getGuestType() == null ||
                    network.getGuestType() == Network.GuestType.Isolated
                    && _ntwkOffServiceMapDao.areServicesSupportedByNetworkOffering(network.getNetworkOfferingId(), Service.SourceNat)) {
                throw new InvalidParameterValueException("Can't create direct vlan for network id=" + networkId + " with type: " + network.getGuestType());
            }
        }

        Pair<Boolean, Pair<String, String>> sameSubnet = null;
        // Can add vlan range only to the network which allows it
        if (!network.getSpecifyIpRanges()) {
            throw new InvalidParameterValueException("Network " + network + " doesn't support adding ip ranges");
        }

        if (zone.getNetworkType() == DataCenter.NetworkType.Advanced) {
            if (network.getTrafficType() == TrafficType.Guest) {
                if (network.getGuestType() != GuestType.Shared) {
                    throw new InvalidParameterValueException("Can execute createVLANIpRanges on shared guest network, but type of this guest network " + network.getId() + " is "
                            + network.getGuestType());
                }

                final List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(network.getId());
                if (vlans != null && vlans.size() > 0) {
                    final VlanVO vlan = vlans.get(0);
                    if (vlanId == null || vlanId.contains(Vlan.UNTAGGED)) {
                        vlanId = vlan.getVlanTag();
                    } else if (!NetUtils.isSameIsolationId(vlan.getVlanTag(), vlanId)) {
                        throw new InvalidParameterValueException("there is already one vlan " + vlan.getVlanTag() + " on network :" + +network.getId()
                                + ", only one vlan is allowed on guest network");
                    }
                }
                sameSubnet = validateIpRange(startIP, endIP, newVlanGateway, newVlanNetmask, vlans, ipv4, ipv6, ip6Gateway, ip6Cidr, startIPv6, endIPv6, network);

            }

        } else if (network.getTrafficType() == TrafficType.Management) {
            throw new InvalidParameterValueException("Cannot execute createVLANIpRanges on management network");
        } else if (zone.getNetworkType() == NetworkType.Basic) {
            final List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(network.getId());
            sameSubnet = validateIpRange(startIP, endIP, newVlanGateway, newVlanNetmask, vlans, ipv4, ipv6, ip6Gateway, ip6Cidr, startIPv6, endIPv6, network);
        }

        if (zoneId == null || ipv6 && (ip6Gateway == null || ip6Cidr == null)) {
            throw new InvalidParameterValueException("Gateway, netmask and zoneId have to be passed in for virtual and direct untagged networks");
        }

        if (forVirtualNetwork) {
            if (vlanOwner != null) {

                final long accountIpRange = NetUtils.ip2Long(endIP) - NetUtils.ip2Long(startIP) + 1;

                // check resource limits
                _resourceLimitMgr.checkResourceLimit(vlanOwner, ResourceType.public_ip, accountIpRange);
            }
        }
        // Check if the IP range overlaps with the private ip
        if (ipv4) {
            checkOverlapPrivateIpRange(zoneId, startIP, endIP);
        }

        return commitVlan(zoneId, podId, startIP, endIP, newVlanGateway, newVlanNetmask, vlanId, forVirtualNetwork, networkId, physicalNetworkId, startIPv6, endIPv6, ip6Gateway,
                ip6Cidr, vlanOwner, network, sameSubnet);
    }

    private Vlan commitVlan(final Long zoneId, final Long podId, final String startIP, final String endIP, final String newVlanGatewayFinal, final String newVlanNetmaskFinal,
            final String vlanId, final Boolean forVirtualNetwork, final Long networkId, final Long physicalNetworkId, final String startIPv6, final String endIPv6,
            final String ip6Gateway, final String ip6Cidr, final Account vlanOwner, final Network network, final Pair<Boolean, Pair<String, String>> sameSubnet) {
        return Transaction.execute(new TransactionCallback<Vlan>() {
            @Override
            public Vlan doInTransaction(final TransactionStatus status) {
                String newVlanNetmask = newVlanNetmaskFinal;
                String newVlanGateway = newVlanGatewayFinal;

                if ((sameSubnet == null || sameSubnet.first() == false) && network.getTrafficType() == TrafficType.Guest && network.getGuestType() == GuestType.Shared
                        && _vlanDao.listVlansByNetworkId(networkId) != null) {
                    final Map<Capability, String> dhcpCapabilities = _networkSvc.getNetworkOfferingServiceCapabilities(_networkOfferingDao.findById(network.getNetworkOfferingId()),
                            Service.Dhcp);
                    final String supportsMultipleSubnets = dhcpCapabilities.get(Capability.DhcpAccrossMultipleSubnets);
                    if (supportsMultipleSubnets == null || !Boolean.valueOf(supportsMultipleSubnets)) {
                        throw new  InvalidParameterValueException("The Dhcp serivice provider for this network dose not support the dhcp  across multiple subnets");
                    }
                    s_logger.info("adding a new subnet to the network " + network.getId());
                } else if (sameSubnet != null)  {
                    // if it is same subnet the user might not send the vlan and the
                    // netmask details. so we are
                    // figuring out while validation and setting them here.
                    newVlanGateway = sameSubnet.second().first();
                    newVlanNetmask = sameSubnet.second().second();
                }
                final Vlan vlan = createVlanAndPublicIpRange(zoneId, networkId, physicalNetworkId, forVirtualNetwork, podId, startIP, endIP, newVlanGateway, newVlanNetmask, vlanId,
                        vlanOwner, startIPv6, endIPv6, ip6Gateway, ip6Cidr);
                // create an entry in the nic_secondary table. This will be the new
                // gateway that will be configured on the corresponding routervm.
                return vlan;
            }
        });
    }

    public NetUtils.SupersetOrSubset checkIfSubsetOrSuperset(String newVlanGateway, String newVlanNetmask, final VlanVO vlan, final String startIP, final String endIP) {
        if (newVlanGateway == null && newVlanNetmask == null) {
            newVlanGateway = vlan.getVlanGateway();
            newVlanNetmask = vlan.getVlanNetmask();
            // this means he is trying to add to the existing subnet.
            if (NetUtils.sameSubnet(startIP, newVlanGateway, newVlanNetmask)) {
                if (NetUtils.sameSubnet(endIP, newVlanGateway, newVlanNetmask)) {
                    return NetUtils.SupersetOrSubset.sameSubnet;
                }
            }
            return NetUtils.SupersetOrSubset.neitherSubetNorSuperset;
        } else if (newVlanGateway == null || newVlanNetmask == null) {
            throw new InvalidParameterValueException(
                    "either both netmask and gateway should be passed or both should me omited.");
        } else {
            if (!NetUtils.sameSubnet(startIP, newVlanGateway, newVlanNetmask)) {
                throw new InvalidParameterValueException("The start ip and gateway do not belong to the same subnet");
            }
            if (!NetUtils.sameSubnet(endIP, newVlanGateway, newVlanNetmask)) {
                throw new InvalidParameterValueException("The end ip and gateway do not belong to the same subnet");
            }
        }
        final String cidrnew = NetUtils.getCidrFromGatewayAndNetmask(newVlanGateway, newVlanNetmask);
        final String existing_cidr = NetUtils.getCidrFromGatewayAndNetmask(vlan.getVlanGateway(), vlan.getVlanNetmask());

        return NetUtils.isNetowrkASubsetOrSupersetOfNetworkB(cidrnew, existing_cidr);
    }

    public Pair<Boolean, Pair<String, String>> validateIpRange(final String startIP, final String endIP, final String newVlanGateway, final String newVlanNetmask, final List<VlanVO> vlans, final boolean ipv4,
            final boolean ipv6, String ip6Gateway, String ip6Cidr, final String startIPv6, final String endIPv6, final Network network) {
        String vlanGateway = null;
        String vlanNetmask = null;
        boolean sameSubnet = false;
        if (vlans != null && vlans.size() > 0) {
            for (final VlanVO vlan : vlans) {
                if (ipv4) {
                    vlanGateway = vlan.getVlanGateway();
                    vlanNetmask = vlan.getVlanNetmask();
                    // check if subset or super set or neither.
                    final NetUtils.SupersetOrSubset val = checkIfSubsetOrSuperset(newVlanGateway, newVlanNetmask, vlan, startIP, endIP);
                    if (val == NetUtils.SupersetOrSubset.isSuperset) {
                        // this means that new cidr is a superset of the
                        // existing subnet.
                        throw new InvalidParameterValueException("The subnet you are trying to add is a superset of the existing subnet having gateway" + vlan.getVlanGateway()
                                + " and netmask  " + vlan.getVlanNetmask());
                    } else if (val == NetUtils.SupersetOrSubset.neitherSubetNorSuperset) {
                        // this implies the user is trying to add a new subnet
                        // which is not a superset or subset of this subnet.
                        // checking with the other subnets.
                        continue;
                    } else if (val == NetUtils.SupersetOrSubset.isSubset) {
                        // this means he is trying to add to the same subnet.
                        throw new InvalidParameterValueException("The subnet you are trying to add is a subset of the existing subnet having gateway" + vlan.getVlanGateway()
                                + " and netmask  " + vlan.getVlanNetmask());
                    } else if (val == NetUtils.SupersetOrSubset.sameSubnet) {
                        sameSubnet = true;
                        //check if the gateway provided by the user is same as that of the subnet.
                        if (newVlanGateway != null && !newVlanGateway.equals(vlanGateway)) {
                            throw new InvalidParameterValueException("The gateway of the subnet should be unique. The subnet alreaddy has a gateway " + vlanGateway);
                        }
                        break;
                    }
                }
                if (ipv6) {
                    if (ip6Gateway != null && !ip6Gateway.equals(network.getIp6Gateway())) {
                        throw new InvalidParameterValueException("The input gateway " + ip6Gateway + " is not same as network gateway " + network.getIp6Gateway());
                    }
                    if (ip6Cidr != null && !ip6Cidr.equals(network.getIp6Cidr())) {
                        throw new InvalidParameterValueException("The input cidr " + ip6Cidr + " is not same as network ciddr " + network.getIp6Cidr());
                    }
                    ip6Gateway = network.getIp6Gateway();
                    ip6Cidr = network.getIp6Cidr();
                    _networkModel.checkIp6Parameters(startIPv6, endIPv6, ip6Gateway, ip6Cidr);
                    sameSubnet = true;
                }
            }
        }
        if (newVlanGateway == null && newVlanNetmask == null && sameSubnet == false) {
            throw new InvalidParameterValueException("The ip range dose not belong to any of the existing subnets, Provide the netmask and gateway if you want to add new subnet");
        }
        Pair<String, String> vlanDetails = null;

        if (sameSubnet) {
            vlanDetails = new Pair<String, String>(vlanGateway, vlanNetmask);
        } else {
            vlanDetails = new Pair<String, String>(newVlanGateway, newVlanNetmask);
        }
        // check if the gatewayip is the part of the ip range being added.
        if (ipv4 && NetUtils.ipRangesOverlap(startIP, endIP, vlanDetails.first(), vlanDetails.first())) {
            throw new InvalidParameterValueException("The gateway ip should not be the part of the ip range being added.");
        }

        final Pair<Boolean, Pair<String, String>> result = new Pair<Boolean, Pair<String, String>>(sameSubnet, vlanDetails);
        return result;
    }

    @Override
    @DB
    public Vlan createVlanAndPublicIpRange(final long zoneId, final long networkId, final long physicalNetworkId, final boolean forVirtualNetwork, final Long podId, final String startIP, final String endIP,
            final String vlanGateway, final String vlanNetmask, String vlanId, final Account vlanOwner, final String startIPv6, final String endIPv6, final String vlanIp6Gateway, final String vlanIp6Cidr) {
        final Network network = _networkModel.getNetwork(networkId);

        boolean ipv4 = false, ipv6 = false;

        if (startIP != null) {
            ipv4 = true;
        }

        if (startIPv6 != null) {
            ipv6 = true;
        }

        if (!ipv4 && !ipv6) {
            throw new InvalidParameterValueException("Please specify IPv4 or IPv6 address.");
        }

        // Validate the zone
        final DataCenterVO zone = _zoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }

        // ACL check
        checkZoneAccess(CallContext.current().getCallingAccount(), zone);

        // Validate the physical network
        if (_physicalNetworkDao.findById(physicalNetworkId) == null) {
            throw new InvalidParameterValueException("Please specify a valid physical network id");
        }

        // Validate the pod
        if (podId != null) {
            final Pod pod = _podDao.findById(podId);
            if (pod == null) {
                throw new InvalidParameterValueException("Please specify a valid pod.");
            }
            if (pod.getDataCenterId() != zoneId) {
                throw new InvalidParameterValueException("Pod id=" + podId + " doesn't belong to zone id=" + zoneId);
            }
            // pod vlans can be created in basic zone only
            if (zone.getNetworkType() != NetworkType.Basic || network.getTrafficType() != TrafficType.Guest) {
                throw new InvalidParameterValueException("Pod id can be specified only for the networks of type " + TrafficType.Guest + " in zone of type " + NetworkType.Basic);
            }
        }

        // 1) if vlan is specified for the guest network range, it should be the
        // same as network's vlan
        // 2) if vlan is missing, default it to the guest network's vlan
        if (network.getTrafficType() == TrafficType.Guest) {
            String networkVlanId = null;
            final URI uri = network.getBroadcastUri();
            if (uri != null) {
                final String[] vlan = uri.toString().split("vlan:\\/\\/");
                networkVlanId = vlan[1];
                // For pvlan
                networkVlanId = networkVlanId.split("-")[0];
            }

            if (vlanId != null) {
                // if vlan is specified, throw an error if it's not equal to
                // network's vlanId
                if (networkVlanId != null && !NetUtils.isSameIsolationId(networkVlanId, vlanId)) {
                    throw new InvalidParameterValueException("Vlan doesn't match vlan of the network");
                }
            } else {
                vlanId = networkVlanId;
            }
        } else if (network.getTrafficType() == TrafficType.Public && vlanId == null) {
            throw new InvalidParameterValueException("Unable to determine vlan id or untagged vlan for public network");
        }

        if (vlanId == null) {
            vlanId = Vlan.UNTAGGED;
        }

        final VlanType vlanType = forVirtualNetwork ? VlanType.VirtualNetwork : VlanType.DirectAttached;

        if (vlanOwner != null && zone.getNetworkType() != NetworkType.Advanced) {
            throw new InvalidParameterValueException("Vlan owner can be defined only in the zone of type " + NetworkType.Advanced);
        }

        if (ipv4) {
            // Make sure the gateway is valid
            if (!NetUtils.isValidIp(vlanGateway)) {
                throw new InvalidParameterValueException("Please specify a valid gateway");
            }

            // Make sure the netmask is valid
            if (!NetUtils.isValidNetmask(vlanNetmask)) {
                throw new InvalidParameterValueException("Please specify a valid netmask");
            }
        }

        if (ipv6) {
            if (!NetUtils.isValidIpv6(vlanIp6Gateway)) {
                throw new InvalidParameterValueException("Please specify a valid IPv6 gateway");
            }
            if (!NetUtils.isValidIp6Cidr(vlanIp6Cidr)) {
                throw new InvalidParameterValueException("Please specify a valid IPv6 CIDR");
            }
        }

        if (ipv4) {
            final String newCidr = NetUtils.getCidrFromGatewayAndNetmask(vlanGateway, vlanNetmask);

            //Make sure start and end ips are with in the range of cidr calculated for this gateway and netmask {
            if (!NetUtils.isIpWithtInCidrRange(vlanGateway, newCidr) || !NetUtils.isIpWithtInCidrRange(startIP, newCidr) || !NetUtils.isIpWithtInCidrRange(endIP, newCidr)) {
                throw new InvalidParameterValueException("Please specify a valid IP range or valid netmask or valid gateway");
            }

            // Check if the new VLAN's subnet conflicts with the guest network
            // in
            // the specified zone (guestCidr is null for basic zone)
            final String guestNetworkCidr = zone.getGuestNetworkCidr();
            if (guestNetworkCidr != null) {
                if (NetUtils.isNetworksOverlap(newCidr, guestNetworkCidr)) {
                    throw new InvalidParameterValueException("The new IP range you have specified has  overlapped with the guest network in zone: " + zone.getName()
                            + ". Please specify a different gateway/netmask.");
                }
            }

            // Check if there are any errors with the IP range
            checkPublicIpRangeErrors(zoneId, vlanId, vlanGateway, vlanNetmask, startIP, endIP);

            checkConflictsWithPortableIpRange(zoneId, vlanId, vlanGateway, vlanNetmask, startIP, endIP);

            // Throw an exception if this subnet overlaps with subnet on other VLAN,
            // if this is ip range extension, gateway, network mask should be same and ip range should not overlap

            final List<VlanVO> vlans = _vlanDao.listByZone(zone.getId());
            for (final VlanVO vlan : vlans) {
                final String otherVlanGateway = vlan.getVlanGateway();
                final String otherVlanNetmask = vlan.getVlanNetmask();
                // Continue if it's not IPv4
                if ( otherVlanGateway == null || otherVlanNetmask == null ) {
                    continue;
                }
                if ( vlan.getNetworkId() == null ) {
                    continue;
                }
                final String otherCidr = NetUtils.getCidrFromGatewayAndNetmask(otherVlanGateway, otherVlanNetmask);
                if( !NetUtils.isNetworksOverlap(newCidr,  otherCidr)) {
                    continue;
                }
                // from here, subnet overlaps
                if ( !vlanId.equals(vlan.getVlanTag()) ) {
                    boolean overlapped = false;
                    if( network.getTrafficType() == TrafficType.Public ) {
                        overlapped = true;
                    } else {
                        final Long nwId = vlan.getNetworkId();
                        if ( nwId != null ) {
                            final Network nw = _networkModel.getNetwork(nwId);
                            if ( nw != null && nw.getTrafficType() == TrafficType.Public ) {
                                overlapped = true;
                            }
                        }

                    }
                    if ( overlapped ) {
                        throw new InvalidParameterValueException("The IP range with tag: " + vlan.getVlanTag()
                                + " in zone " + zone.getName()
                                + " has overlapped with the subnet. Please specify a different gateway/netmask.");
                    }
                } else {

                    final String[] otherVlanIpRange = vlan.getIpRange().split("\\-");
                    final String otherVlanStartIP = otherVlanIpRange[0];
                    String otherVlanEndIP = null;
                    if (otherVlanIpRange.length > 1) {
                        otherVlanEndIP = otherVlanIpRange[1];
                    }

                    // extend IP range
                    if (!vlanGateway.equals(otherVlanGateway) || !vlanNetmask.equals(vlan.getVlanNetmask())) {
                        throw new InvalidParameterValueException("The IP range has already been added with gateway "
                                + otherVlanGateway + " ,and netmask " + otherVlanNetmask
                                + ", Please specify the gateway/netmask if you want to extend ip range" );
                    }
                    if (!NetUtils.is31PrefixCidr(newCidr)) {
                        if (NetUtils.ipRangesOverlap(startIP, endIP, otherVlanStartIP, otherVlanEndIP)) {
                            throw new InvalidParameterValueException("The IP range already has IPs that overlap with the new range." +
                                " Please specify a different start IP/end IP.");
                        }
                    }
                }
            }
        }

        String ipv6Range = null;
        if (ipv6) {
            ipv6Range = startIPv6;
            if (endIPv6 != null) {
                ipv6Range += "-" + endIPv6;
            }

            final List<VlanVO> vlans = _vlanDao.listByZone(zone.getId());
            for (final VlanVO vlan : vlans) {
                if (vlan.getIp6Gateway() == null) {
                    continue;
                }
                if (NetUtils.isSameIsolationId(vlanId, vlan.getVlanTag())) {
                    if (NetUtils.isIp6RangeOverlap(ipv6Range, vlan.getIp6Range())) {
                        throw new InvalidParameterValueException("The IPv6 range with tag: " + vlan.getVlanTag()
                                + " already has IPs that overlap with the new range. Please specify a different start IP/end IP.");
                    }

                    if (!vlanIp6Gateway.equals(vlan.getIp6Gateway())) {
                        throw new InvalidParameterValueException("The IP range with tag: " + vlan.getVlanTag() + " has already been added with gateway " + vlan.getIp6Gateway()
                                + ". Please specify a different tag.");
                    }
                }
            }
        }

        // Check if the vlan is being used
        if (_zoneDao.findVnet(zoneId, physicalNetworkId, vlanId).size() > 0) {
            throw new InvalidParameterValueException("The VLAN tag " + vlanId + " is already being used for dynamic vlan allocation for the guest network in zone "
                    + zone.getName());
        }

        String ipRange = null;

        if (ipv4) {
            ipRange = startIP;
            if (endIP != null) {
                ipRange += "-" + endIP;
            }
        }

        // Everything was fine, so persist the VLAN
        final VlanVO vlan = commitVlanAndIpRange(zoneId, networkId, physicalNetworkId, podId, startIP, endIP, vlanGateway, vlanNetmask, vlanId, vlanOwner, vlanIp6Gateway, vlanIp6Cidr,
                ipv4, zone, vlanType, ipv6Range, ipRange);

        return vlan;
    }

    private VlanVO commitVlanAndIpRange(final long zoneId, final long networkId, final long physicalNetworkId, final Long podId, final String startIP, final String endIP,
            final String vlanGateway, final String vlanNetmask, final String vlanId, final Account vlanOwner, final String vlanIp6Gateway, final String vlanIp6Cidr,
            final boolean ipv4, final DataCenterVO zone, final VlanType vlanType, final String ipv6Range, final String ipRange) {
        return Transaction.execute(new TransactionCallback<VlanVO>() {
            @Override
            public VlanVO doInTransaction(final TransactionStatus status) {
                VlanVO vlan = new VlanVO(vlanType, vlanId, vlanGateway, vlanNetmask, zone.getId(), ipRange, networkId, physicalNetworkId, vlanIp6Gateway, vlanIp6Cidr, ipv6Range);
                s_logger.debug("Saving vlan range " + vlan);
                vlan = _vlanDao.persist(vlan);

                // IPv6 use a used ip map, is different from ipv4, no need to save
                // public ip range
                if (ipv4) {
                    if (!savePublicIPRange(startIP, endIP, zoneId, vlan.getId(), networkId, physicalNetworkId)) {
                        throw new CloudRuntimeException("Failed to save IPv4 range. Please contact Cloud Support.");
                    }
                }

                if (vlanOwner != null) {
                    // This VLAN is account-specific, so create an AccountVlanMapVO
                    // entry
                    final AccountVlanMapVO accountVlanMapVO = new AccountVlanMapVO(vlanOwner.getId(), vlan.getId());
                    _accountVlanMapDao.persist(accountVlanMapVO);

                    // generate usage event for dedication of every ip address in the
                    // range
                    final List<IPAddressVO> ips = _publicIpAddressDao.listByVlanId(vlan.getId());
                    for (final IPAddressVO ip : ips) {
                        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_IP_ASSIGN, vlanOwner.getId(), ip.getDataCenterId(), ip.getId(), ip.getAddress().toString(),
                                ip.isSourceNat(), vlan.getVlanType().toString(), ip.getSystem(), ip.getClass().getName(), ip.getUuid());
                    }
                    // increment resource count for dedicated public ip's
                    _resourceLimitMgr.incrementResourceCount(vlanOwner.getId(), ResourceType.public_ip, new Long(ips.size()));
                } else if (podId != null) {
                    // This VLAN is pod-wide, so create a PodVlanMapVO entry
                    final PodVlanMapVO podVlanMapVO = new PodVlanMapVO(podId, vlan.getId());
                    _podVlanMapDao.persist(podVlanMapVO);
                }
                return vlan;
            }
        });

    }

    @Override
    @DB
    public boolean deleteVlanAndPublicIpRange(final long userId, final long vlanDbId, final Account caller) {
        VlanVO vlanRange = _vlanDao.findById(vlanDbId);
        if (vlanRange == null) {
            throw new InvalidParameterValueException("Please specify a valid IP range id.");
        }

        boolean isAccountSpecific = false;
        final List<AccountVlanMapVO> acctVln = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanRange.getId());
        // Check for account wide pool. It will have an entry for
        // account_vlan_map.
        if (acctVln != null && !acctVln.isEmpty()) {
            isAccountSpecific = true;
        }

        // Check if the VLAN has any allocated public IPs
        final List<IPAddressVO> ips = _publicIpAddressDao.listByVlanId(vlanDbId);
        if (isAccountSpecific) {
            int resourceCountToBeDecrement = 0;
            try {
                vlanRange = _vlanDao.acquireInLockTable(vlanDbId, 30);
                if (vlanRange == null) {
                    throw new CloudRuntimeException("Unable to acquire vlan configuration: " + vlanDbId);
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("lock vlan " + vlanDbId + " is acquired");
                }
                for (final IPAddressVO ip : ips) {
                    boolean success = true;
                    if (ip.isOneToOneNat()) {
                        throw new InvalidParameterValueException("Can't delete account specific vlan " + vlanDbId + " as ip " + ip
                                + " belonging to the range is used for static nat purposes. Cleanup the rules first");
                    }

                    if (ip.isSourceNat()) {
                        throw new InvalidParameterValueException("Can't delete account specific vlan " + vlanDbId + " as ip " + ip
                                + " belonging to the range is a source nat ip for the network id=" + ip.getSourceNetworkId()
                                + ". IP range with the source nat ip address can be removed either as a part of Network, or account removal");
                    }

                    if (_firewallDao.countRulesByIpId(ip.getId()) > 0) {
                        throw new InvalidParameterValueException("Can't delete account specific vlan " + vlanDbId + " as ip " + ip
                                + " belonging to the range has firewall rules applied. Cleanup the rules first");
                    }
                    if (ip.getAllocatedTime() != null) {// This means IP is allocated
                        // release public ip address here
                        success = _ipAddrMgr.disassociatePublicIpAddress(ip.getId(), userId, caller);
                    }
                    if (!success) {
                        s_logger.warn("Some ip addresses failed to be released as a part of vlan " + vlanDbId + " removal");
                    } else {
                        resourceCountToBeDecrement++;
                        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_IP_RELEASE, acctVln.get(0).getAccountId(), ip.getDataCenterId(), ip.getId(),
                                ip.getAddress().toString(), ip.isSourceNat(), vlanRange.getVlanType().toString(), ip.getSystem(), ip.getClass().getName(), ip.getUuid());
                    }
                }
            } finally {
                _vlanDao.releaseFromLockTable(vlanDbId);
                if (resourceCountToBeDecrement > 0) {  //Making sure to decrement the count of only success operations above. For any reaason if disassociation fails then this number will vary from original range length.
                    _resourceLimitMgr.decrementResourceCount(acctVln.get(0).getAccountId(), ResourceType.public_ip, new Long(resourceCountToBeDecrement));
                }
            }
        } else {   // !isAccountSpecific
            final NicIpAliasVO ipAlias = _nicIpAliasDao.findByGatewayAndNetworkIdAndState(vlanRange.getVlanGateway(), vlanRange.getNetworkId(), NicIpAlias.state.active);
            //check if the ipalias belongs to the vlan range being deleted.
            if (ipAlias != null && vlanDbId == _publicIpAddressDao.findByIpAndSourceNetworkId(vlanRange.getNetworkId(), ipAlias.getIp4Address()).getVlanId()) {
                throw new InvalidParameterValueException("Cannot delete vlan range " + vlanDbId + " as " + ipAlias.getIp4Address()
                        + "is being used for providing dhcp service in this subnet. Delete all VMs in this subnet and try again");
            }
            final long allocIpCount = _publicIpAddressDao.countIPs(vlanRange.getDataCenterId(), vlanDbId, true);
            if (allocIpCount > 0) {
                throw new InvalidParameterValueException(allocIpCount + "  Ips are in use. Cannot delete this vlan");
            }
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                _publicIpAddressDao.deletePublicIPRange(vlanDbId);
                _vlanDao.remove(vlanDbId);
            }
        });

        return true;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VLAN_IP_RANGE_DEDICATE, eventDescription = "dedicating vlan ip range", async = false)
    public Vlan dedicatePublicIpRange(final DedicatePublicIpRangeCmd cmd) throws ResourceAllocationException {
        final Long vlanDbId = cmd.getId();
        final String accountName = cmd.getAccountName();
        final Long domainId = cmd.getDomainId();
        final Long projectId = cmd.getProjectId();

        // Check if account is valid
        Account vlanOwner = null;
        if (projectId != null) {
            if (accountName != null) {
                throw new InvalidParameterValueException("accountName and projectId are mutually exclusive");
            }
            final Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }
            vlanOwner = _accountMgr.getAccount(project.getProjectAccountId());
        }

        if (accountName != null && domainId != null) {
            vlanOwner = _accountDao.findActiveAccount(accountName, domainId);
        }
        if (vlanOwner == null) {
            throw new InvalidParameterValueException("Unable to find account by name " + accountName);
        } else if (vlanOwner.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("Please specify a valid account. Cannot dedicate IP range to system account");
        }

        // Check if range is valid
        final VlanVO vlan = _vlanDao.findById(vlanDbId);
        if (vlan == null) {
            throw new InvalidParameterValueException("Unable to find vlan by id " + vlanDbId);
        }

        // Check if range has already been dedicated
        final List<AccountVlanMapVO> maps = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanDbId);
        if (maps != null && !maps.isEmpty()) {
            throw new InvalidParameterValueException("Specified Public IP range has already been dedicated");
        }

        // Verify that zone exists and is advanced
        final Long zoneId = vlan.getDataCenterId();
        final DataCenterVO zone = _zoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id " + zoneId);
        }
        if (zone.getNetworkType() == NetworkType.Basic) {
            throw new InvalidParameterValueException("Public IP range can be dedicated to an account only in the zone of type " + NetworkType.Advanced);
        }

        // Check Public IP resource limits
        final int accountPublicIpRange = _publicIpAddressDao.countIPs(zoneId, vlanDbId, false);
        _resourceLimitMgr.checkResourceLimit(vlanOwner, ResourceType.public_ip, accountPublicIpRange);

        // Check if any of the Public IP addresses is allocated to another
        // account
        final List<IPAddressVO> ips = _publicIpAddressDao.listByVlanId(vlanDbId);
        for (final IPAddressVO ip : ips) {
            final Long allocatedToAccountId = ip.getAllocatedToAccountId();
            if (allocatedToAccountId != null) {
                final Account accountAllocatedTo = _accountMgr.getActiveAccountById(allocatedToAccountId);
                if (!accountAllocatedTo.getAccountName().equalsIgnoreCase(accountName)) {
                    throw new InvalidParameterValueException(ip.getAddress() + " Public IP address in range is allocated to another account ");
                }
            }
        }

        // Create an AccountVlanMapVO entry
        final AccountVlanMapVO accountVlanMapVO = new AccountVlanMapVO(vlanOwner.getId(), vlan.getId());
        _accountVlanMapDao.persist(accountVlanMapVO);

        // generate usage event for dedication of every ip address in the range
        for (final IPAddressVO ip : ips) {
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_IP_ASSIGN, vlanOwner.getId(), ip.getDataCenterId(), ip.getId(), ip.getAddress().toString(), ip.isSourceNat(),
                    vlan.getVlanType().toString(), ip.getSystem(), ip.getClass().getName(), ip.getUuid());
        }

        // increment resource count for dedicated public ip's
        _resourceLimitMgr.incrementResourceCount(vlanOwner.getId(), ResourceType.public_ip, new Long(ips.size()));

        return vlan;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VLAN_IP_RANGE_RELEASE, eventDescription = "releasing a public ip range", async = false)
    public boolean releasePublicIpRange(final ReleasePublicIpRangeCmd cmd) {
        final Long vlanDbId = cmd.getId();

        final VlanVO vlan = _vlanDao.findById(vlanDbId);
        if (vlan == null) {
            throw new InvalidParameterValueException("Please specify a valid IP range id.");
        }

        return releasePublicIpRange(vlanDbId, CallContext.current().getCallingUserId(), CallContext.current().getCallingAccount());
    }

    @DB
    public boolean releasePublicIpRange(final long vlanDbId, final long userId, final Account caller) {
        VlanVO vlan = _vlanDao.findById(vlanDbId);

        final List<AccountVlanMapVO> acctVln = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanDbId);
        // Verify range is dedicated
        if (acctVln == null || acctVln.isEmpty()) {
            throw new InvalidParameterValueException("Can't release Public IP range " + vlanDbId + " as it not dedicated to any account");
        }

        // Check if range has any allocated public IPs
        final long allocIpCount = _publicIpAddressDao.countIPs(vlan.getDataCenterId(), vlanDbId, true);
        final List<IPAddressVO> ips = _publicIpAddressDao.listByVlanId(vlanDbId);
        boolean success = true;
        final List<IPAddressVO> ipsInUse = new ArrayList<IPAddressVO>();
        if (allocIpCount > 0) {
            try {
                vlan = _vlanDao.acquireInLockTable(vlanDbId, 30);
                if (vlan == null) {
                    throw new CloudRuntimeException("Unable to acquire vlan configuration: " + vlanDbId);
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("lock vlan " + vlanDbId + " is acquired");
                }
                for (final IPAddressVO ip : ips) {
                    // Disassociate allocated IP's that are not in use
                    if (!ip.isOneToOneNat() && !ip.isSourceNat() && !(_firewallDao.countRulesByIpId(ip.getId()) > 0)) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Releasing Public IP addresses" + ip + " of vlan " + vlanDbId + " as part of Public IP" + " range release to the system pool");
                        }
                        success = success && _ipAddrMgr.disassociatePublicIpAddress(ip.getId(), userId, caller);
                    } else {
                        ipsInUse.add(ip);
                    }
                }
                if (!success) {
                    s_logger.warn("Some Public IP addresses that were not in use failed to be released as a part of" + " vlan " + vlanDbId + "release to the system pool");
                }
            } finally {
                _vlanDao.releaseFromLockTable(vlanDbId);
            }
        }

        // A Public IP range can only be dedicated to one account at a time
        if (_accountVlanMapDao.remove(acctVln.get(0).getId())) {
            // generate usage events to remove dedication for every ip in the range that has been disassociated
            for (final IPAddressVO ip : ips) {
                if (!ipsInUse.contains(ip)) {
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_IP_RELEASE, acctVln.get(0).getAccountId(), ip.getDataCenterId(), ip.getId(), ip.getAddress().toString(),
                            ip.isSourceNat(), vlan.getVlanType().toString(), ip.getSystem(), ip.getClass().getName(), ip.getUuid());
                }
            }
            // decrement resource count for dedicated public ip's
            _resourceLimitMgr.decrementResourceCount(acctVln.get(0).getAccountId(), ResourceType.public_ip, new Long(ips.size()));
            return true;
        } else {
            return false;
        }
    }

    @DB
    protected boolean savePublicIPRange(final String startIP, final String endIP, final long zoneId, final long vlanDbId, final long sourceNetworkid, final long physicalNetworkId) {
        final long startIPLong = NetUtils.ip2Long(startIP);
        final long endIPLong = NetUtils.ip2Long(endIP);

        final List<String> problemIps = Transaction.execute(new TransactionCallback<List<String>>() {
            @Override
            public List<String> doInTransaction(final TransactionStatus status) {
                final IPRangeConfig config = new IPRangeConfig();
                return config.savePublicIPRange(TransactionLegacy.currentTxn(), startIPLong, endIPLong, zoneId, vlanDbId, sourceNetworkid, physicalNetworkId);
            }
        });

        return problemIps != null && problemIps.size() == 0;
    }

    private void checkPublicIpRangeErrors(final long zoneId, final String vlanId, final String vlanGateway, final String vlanNetmask, final String startIP, final String endIP) {
        // Check that the start and end IPs are valid
        if (!NetUtils.isValidIp(startIP)) {
            throw new InvalidParameterValueException("Please specify a valid start IP");
        }

        if (endIP != null && !NetUtils.isValidIp(endIP)) {
            throw new InvalidParameterValueException("Please specify a valid end IP");
        }

        if (endIP != null && !NetUtils.validIpRange(startIP, endIP)) {
            throw new InvalidParameterValueException("Please specify a valid IP range.");
        }

        // Check that the IPs that are being added are compatible with the
        // VLAN's gateway and netmask
        if (vlanNetmask == null) {
            throw new InvalidParameterValueException("Please ensure that your IP range's netmask is specified");
        }

        if (endIP != null && !NetUtils.sameSubnet(startIP, endIP, vlanNetmask)) {
            throw new InvalidParameterValueException("Please ensure that your start IP and end IP are in the same subnet, as per the IP range's netmask.");
        }

        if (!NetUtils.sameSubnet(startIP, vlanGateway, vlanNetmask)) {
            throw new InvalidParameterValueException("Please ensure that your start IP is in the same subnet as your IP range's gateway, as per the IP range's netmask.");
        }

        if (endIP != null && !NetUtils.sameSubnet(endIP, vlanGateway, vlanNetmask)) {
            throw new InvalidParameterValueException("Please ensure that your end IP is in the same subnet as your IP range's gateway, as per the IP range's netmask.");
        }
        // check if the gatewayip is the part of the ip range being added.
        // RFC 3021 - 31-Bit Prefixes on IPv4 Point-to-Point Links
        //     GW              Netmask         Stat IP        End IP
        // 192.168.24.0 - 255.255.255.254 - 192.168.24.0 - 192.168.24.1
        // https://tools.ietf.org/html/rfc3021
        // Added by Wilder Rodrigues
        final String newCidr = NetUtils.getCidrFromGatewayAndNetmask(vlanGateway, vlanNetmask);
        if (!NetUtils.is31PrefixCidr(newCidr)) {
            if (NetUtils.ipRangesOverlap(startIP, endIP, vlanGateway, vlanGateway)) {
                throw new InvalidParameterValueException(
                        "The gateway ip should not be the part of the ip range being added.");
            }
        }
    }

    private void checkConflictsWithPortableIpRange(final long zoneId, final String vlanId, final String vlanGateway, final String vlanNetmask, final String startIP, final String endIP) {
        // check and throw exception if there is portable IP range that overlaps with public ip range being configured
        if (checkOverlapPortableIpRange(_regionDao.getRegionId(), startIP, endIP)) {
            throw new InvalidParameterValueException("Ip range: " + startIP + "-" + endIP + " overlaps with a portable" + " IP range already configured in the region "
                    + _regionDao.getRegionId());
        }

        // verify and throw exception if the VLAN Id is used by any portable IP range
        final List<PortableIpRangeVO> existingPortableIPRanges = _portableIpRangeDao.listByRegionId(_regionDao.getRegionId());
        if (existingPortableIPRanges != null && !existingPortableIPRanges.isEmpty()) {
            for (final PortableIpRangeVO portableIpRange : existingPortableIPRanges) {
                if (NetUtils.isSameIsolationId(portableIpRange.getVlanTag(), vlanId)) {
                    throw new InvalidParameterValueException("The VLAN tag " + vlanId + " is already being used for portable ip range in this region");
                }
            }
        }
    }

    private String getCidrAddress(final String cidr) {
        final String[] cidrPair = cidr.split("\\/");
        return cidrPair[0];
    }

    private int getCidrSize(final String cidr) {
        final String[] cidrPair = cidr.split("\\/");
        return Integer.parseInt(cidrPair[1]);
    }

    @Override
    public void checkPodCidrSubnets(final long dcId, final Long podIdToBeSkipped, final String cidr) {
        // For each pod, return an error if any of the following is true:
        // The pod's CIDR subnet conflicts with the CIDR subnet of any other pod

        // Check if the CIDR conflicts with the Guest Network or other pods
        long skipPod = 0;
        if (podIdToBeSkipped != null) {
            skipPod = podIdToBeSkipped;
        }
        final HashMap<Long, List<Object>> currentPodCidrSubnets = _podDao.getCurrentPodCidrSubnets(dcId, skipPod);
        final List<Object> newCidrPair = new ArrayList<Object>();
        newCidrPair.add(0, getCidrAddress(cidr));
        newCidrPair.add(1, (long)getCidrSize(cidr));
        currentPodCidrSubnets.put(new Long(-1), newCidrPair);

        final DataCenterVO dcVo = _zoneDao.findById(dcId);
        final String guestNetworkCidr = dcVo.getGuestNetworkCidr();

        // Guest cidr can be null for Basic zone
        String guestIpNetwork = null;
        Long guestCidrSize = null;
        if (guestNetworkCidr != null) {
            final String[] cidrTuple = guestNetworkCidr.split("\\/");
            guestIpNetwork = NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1]));
            guestCidrSize = Long.parseLong(cidrTuple[1]);
        }

        final String zoneName = getZoneName(dcId);

        // Iterate through all pods in this zone
        for (final Long podId : currentPodCidrSubnets.keySet()) {
            String podName;
            if (podId.longValue() == -1) {
                podName = "newPod";
            } else {
                podName = getPodName(podId.longValue());
            }

            final List<Object> cidrPair = currentPodCidrSubnets.get(podId);
            final String cidrAddress = (String)cidrPair.get(0);
            final long cidrSize = ((Long)cidrPair.get(1)).longValue();

            long cidrSizeToUse = -1;
            if (guestCidrSize == null || cidrSize < guestCidrSize) {
                cidrSizeToUse = cidrSize;
            } else {
                cidrSizeToUse = guestCidrSize;
            }

            String cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);

            if (guestNetworkCidr != null) {
                final String guestSubnet = NetUtils.getCidrSubNet(guestIpNetwork, cidrSizeToUse);
                // Check that cidrSubnet does not equal guestSubnet
                if (cidrSubnet.equals(guestSubnet)) {
                    if (podName.equals("newPod")) {
                        throw new InvalidParameterValueException(
                                "The subnet of the pod you are adding conflicts with the subnet of the Guest IP Network. Please specify a different CIDR.");
                    } else {
                        throw new InvalidParameterValueException(
                                "Warning: The subnet of pod "
                                        + podName
                                        + " in zone "
                                        + zoneName
                                        + " conflicts with the subnet of the Guest IP Network. Please change either the pod's CIDR or the Guest IP Network's subnet, and re-run install-vmops-management.");
                    }
                }
            }

            // Iterate through the rest of the pods
            for (final Long otherPodId : currentPodCidrSubnets.keySet()) {
                if (podId.equals(otherPodId)) {
                    continue;
                }

                // Check that cidrSubnet does not equal otherCidrSubnet
                final List<Object> otherCidrPair = currentPodCidrSubnets.get(otherPodId);
                final String otherCidrAddress = (String)otherCidrPair.get(0);
                final long otherCidrSize = ((Long)otherCidrPair.get(1)).longValue();

                if (cidrSize < otherCidrSize) {
                    cidrSizeToUse = cidrSize;
                } else {
                    cidrSizeToUse = otherCidrSize;
                }

                cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);
                final String otherCidrSubnet = NetUtils.getCidrSubNet(otherCidrAddress, cidrSizeToUse);

                if (cidrSubnet.equals(otherCidrSubnet)) {
                    final String otherPodName = getPodName(otherPodId.longValue());
                    if (podName.equals("newPod")) {
                        throw new InvalidParameterValueException("The subnet of the pod you are adding conflicts with the subnet of pod " + otherPodName + " in zone " + zoneName
                                + ". Please specify a different CIDR.");
                    } else {
                        throw new InvalidParameterValueException("Warning: The pods " + podName + " and " + otherPodName + " in zone " + zoneName
                                + " have conflicting CIDR subnets. Please change the CIDR of one of these pods.");
                    }
                }
            }
        }

    }

    private boolean validPod(final long podId) {
        return _podDao.findById(podId) != null;
    }

    private boolean validPod(final String podName, final long zoneId) {
        if (!validZone(zoneId)) {
            return false;
        }

        return _podDao.findByName(podName, zoneId) != null;
    }

    private String getPodName(final long podId) {
        return _podDao.findById(new Long(podId)).getName();
    }

    private boolean validZone(final String zoneName) {
        return _zoneDao.findByName(zoneName) != null;
    }

    private boolean validZone(final long zoneId) {
        return _zoneDao.findById(zoneId) != null;
    }

    private String getZoneName(final long zoneId) {
        final DataCenterVO zone = _zoneDao.findById(new Long(zoneId));
        if (zone != null) {
            return zone.getName();
        } else {
            return null;
        }
    }

    private String[] getLinkLocalIPRange() {
        final String ipNums = _configDao.getValue("linkLocalIp.nums");
        final int nums = Integer.parseInt(ipNums);
        if (nums > 16 || nums <= 0) {
            throw new InvalidParameterValueException("The linkLocalIp.nums: " + nums + "is wrong, should be 1~16");
        }
        /* local link ip address starts from 169.254.0.2 - 169.254.(nums) */
        final String[] ipRanges = NetUtils.getLinkLocalIPRange(nums);
        if (ipRanges == null) {
            throw new InvalidParameterValueException("The linkLocalIp.nums: " + nums + "may be wrong, should be 1~16");
        }
        return ipRanges;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VLAN_IP_RANGE_DELETE, eventDescription = "deleting vlan ip range", async = false)
    public boolean deleteVlanIpRange(final DeleteVlanIpRangeCmd cmd) {
        final Long vlanDbId = cmd.getId();

        final VlanVO vlan = _vlanDao.findById(vlanDbId);
        if (vlan == null) {
            throw new InvalidParameterValueException("Please specify a valid IP range id.");
        }

        return deleteVlanAndPublicIpRange(CallContext.current().getCallingUserId(), vlanDbId, CallContext.current().getCallingAccount());
    }

    @Override
    public void checkDiskOfferingAccess(final Account caller, final DiskOffering dof) {
        for (final SecurityChecker checker : _secChecker) {
            if (checker.checkAccess(caller, dof)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Access granted to " + caller + " to disk offering:" + dof.getId() + " by " + checker.getName());
                }
                return;
            } else {
                throw new PermissionDeniedException("Access denied to " + caller + " by " + checker.getName());
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + caller + " has access to disk offering:" + dof.getId());
    }

    @Override
    public void checkZoneAccess(final Account caller, final DataCenter zone) {
        for (final SecurityChecker checker : _secChecker) {
            if (checker.checkAccess(caller, zone)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Access granted to " + caller + " to zone:" + zone.getId() + " by " + checker.getName());
                }
                return;
            } else {
                throw new PermissionDeniedException("Access denied to " + caller + " by " + checker.getName() + " for zone " + zone.getId());
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + caller + " has access to zone:" + zone.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_OFFERING_CREATE, eventDescription = "creating network offering")
    public NetworkOffering createNetworkOffering(final CreateNetworkOfferingCmd cmd) {
        final String name = cmd.getNetworkOfferingName();
        final String displayText = cmd.getDisplayText();
        final String tags = cmd.getTags();
        final String trafficTypeString = cmd.getTraffictype();
        final boolean specifyVlan = cmd.getSpecifyVlan();
        final boolean conserveMode = cmd.getConserveMode();
        final String availabilityStr = cmd.getAvailability();
        Integer networkRate = cmd.getNetworkRate();
        TrafficType trafficType = null;
        Availability availability = null;
        Network.GuestType guestType = null;
        final boolean specifyIpRanges = cmd.getSpecifyIpRanges();
        final boolean isPersistent = cmd.getIsPersistent();
        final Map<String, String> detailsStr = cmd.getDetails();
        final Boolean egressDefaultPolicy = cmd.getEgressDefaultPolicy();
        Integer maxconn = null;
        boolean enableKeepAlive = false;

        // Verify traffic type
        for (final TrafficType tType : TrafficType.values()) {
            if (tType.name().equalsIgnoreCase(trafficTypeString)) {
                trafficType = tType;
                break;
            }
        }
        if (trafficType == null) {
            throw new InvalidParameterValueException("Invalid value for traffictype. Supported traffic types: Public, Management, Control, Guest, Vlan or Storage");
        }

        // Only GUEST traffic type is supported in Acton
        if (trafficType != TrafficType.Guest) {
            throw new InvalidParameterValueException("Only traffic type " + TrafficType.Guest + " is supported in the current release");
        }

        // Verify offering type
        for (final Network.GuestType offType : Network.GuestType.values()) {
            if (offType.name().equalsIgnoreCase(cmd.getGuestIpType())) {
                guestType = offType;
                break;
            }
        }

        if (guestType == null) {
            throw new InvalidParameterValueException("Invalid \"type\" parameter is given; can have Shared and Isolated values");
        }

        // Verify availability
        for (final Availability avlb : Availability.values()) {
            if (avlb.name().equalsIgnoreCase(availabilityStr)) {
                availability = avlb;
            }
        }

        if (availability == null) {
            throw new InvalidParameterValueException("Invalid value for Availability. Supported types: " + Availability.Required + ", " + Availability.Optional);
        }

        if (networkRate != null && networkRate < 0) {
            networkRate = 0;
        }

        final Long serviceOfferingId = cmd.getServiceOfferingId();

        if (serviceOfferingId != null) {
            final ServiceOfferingVO offering = _serviceOfferingDao.findById(serviceOfferingId);
            if (offering == null) {
                throw new InvalidParameterValueException("Cannot find specified service offering: " + serviceOfferingId);
            }
            if (!VirtualMachine.Type.DomainRouter.toString().equalsIgnoreCase(offering.getSystemVmType())) {
                throw new InvalidParameterValueException("The specified service offering " + serviceOfferingId + " cannot be used by virtual router!");
            }
        }

        // configure service provider map
        final Map<Network.Service, Set<Network.Provider>> serviceProviderMap = new HashMap<Network.Service, Set<Network.Provider>>();
        final Set<Network.Provider> defaultProviders = new HashSet<Network.Provider>();

        // populate the services first
        for (final String serviceName : cmd.getSupportedServices()) {
            // validate if the service is supported
            final Service service = Network.Service.getService(serviceName);
            if (service == null || service == Service.Gateway) {
                throw new InvalidParameterValueException("Invalid service " + serviceName);
            }

            if (service == Service.SecurityGroup) {
                // allow security group service for Shared networks only
                if (guestType != GuestType.Shared) {
                    throw new InvalidParameterValueException("Secrity group service is supported for network offerings with guest ip type " + GuestType.Shared);
                }
                final Set<Network.Provider> sgProviders = new HashSet<Network.Provider>();
                sgProviders.add(Provider.SecurityGroupProvider);
                serviceProviderMap.put(Network.Service.SecurityGroup, sgProviders);
                continue;
            }
            serviceProviderMap.put(service, defaultProviders);
        }

        // add gateway provider (if sourceNat provider is enabled)
        final Set<Provider> sourceNatServiceProviders = serviceProviderMap.get(Service.SourceNat);
        if (sourceNatServiceProviders != null && !sourceNatServiceProviders.isEmpty()) {
            serviceProviderMap.put(Service.Gateway, sourceNatServiceProviders);
        }

        // populate providers
        final Map<Provider, Set<Service>> providerCombinationToVerify = new HashMap<Provider, Set<Service>>();
        final Map<String, List<String>> svcPrv = cmd.getServiceProviders();
        Provider firewallProvider = null;
        Provider dhcpProvider = null;
        Boolean IsVrUserdataProvider = false;
        if (svcPrv != null) {
            for (final String serviceStr : svcPrv.keySet()) {
                final Network.Service service = Network.Service.getService(serviceStr);
                if (serviceProviderMap.containsKey(service)) {
                    final Set<Provider> providers = new HashSet<Provider>();
                    // Allow to specify more than 1 provider per service only if
                    // the service is LB
                    if (!serviceStr.equalsIgnoreCase(Service.Lb.getName()) && svcPrv.get(serviceStr) != null && svcPrv.get(serviceStr).size() > 1) {
                        throw new InvalidParameterValueException("In the current release only one provider can be " + "specified for the service if the service is not LB");
                    }
                    for (final String prvNameStr : svcPrv.get(serviceStr)) {
                        // check if provider is supported
                        final Network.Provider provider = Network.Provider.getProvider(prvNameStr);
                        if (provider == null) {
                            throw new InvalidParameterValueException("Invalid service provider: " + prvNameStr);
                        }

                        if (provider == Provider.JuniperSRX || provider == Provider.CiscoVnmc) {
                            firewallProvider = provider;
                        }

                        if (provider == Provider.PaloAlto) {
                            firewallProvider = Provider.PaloAlto;
                        }

                        if ((service == Service.PortForwarding || service == Service.StaticNat) && provider == Provider.VirtualRouter) {
                            firewallProvider = Provider.VirtualRouter;
                        }

                        if (service == Service.Dhcp) {
                            dhcpProvider = provider;
                        }

                        if (service == Service.UserData && provider == Provider.VirtualRouter) {
                            IsVrUserdataProvider = true;
                        }

                        providers.add(provider);

                        Set<Service> serviceSet = null;
                        if (providerCombinationToVerify.get(provider) == null) {
                            serviceSet = new HashSet<Service>();
                        } else {
                            serviceSet = providerCombinationToVerify.get(provider);
                        }
                        serviceSet.add(service);
                        providerCombinationToVerify.put(provider, serviceSet);

                    }
                    serviceProviderMap.put(service, providers);
                } else {
                    throw new InvalidParameterValueException("Service " + serviceStr + " is not enabled for the network " + "offering, can't add a provider to it");
                }
            }
        }

        // dhcp provider and userdata provider should be same because vm will be contacting dhcp server for user data.
        if (dhcpProvider == null && IsVrUserdataProvider) {
            s_logger.debug("User data provider VR can't be selected without VR as dhcp provider. In this case VM fails to contact the DHCP server for userdata");
            throw new InvalidParameterValueException("Without VR as dhcp provider, User data can't selected for VR. Please select VR as DHCP provider ");
        }

        // validate providers combination here
        _networkModel.canProviderSupportServices(providerCombinationToVerify);

        // validate the LB service capabilities specified in the network
        // offering
        final Map<Capability, String> lbServiceCapabilityMap = cmd.getServiceCapabilities(Service.Lb);
        if (!serviceProviderMap.containsKey(Service.Lb) && lbServiceCapabilityMap != null && !lbServiceCapabilityMap.isEmpty()) {
            throw new InvalidParameterValueException("Capabilities for LB service can be specifed only when LB service is enabled for network offering.");
        }
        validateLoadBalancerServiceCapabilities(lbServiceCapabilityMap);

        if (lbServiceCapabilityMap != null && !lbServiceCapabilityMap.isEmpty()) {
            maxconn = cmd.getMaxconnections();
            if (maxconn == null) {
                maxconn = Integer.parseInt(_configDao.getValue(Config.NetworkLBHaproxyMaxConn.key()));
            }
        }
        if (cmd.getKeepAliveEnabled() != null && cmd.getKeepAliveEnabled()) {
            enableKeepAlive = true;
        }

        // validate the Source NAT service capabilities specified in the network
        // offering
        final Map<Capability, String> sourceNatServiceCapabilityMap = cmd.getServiceCapabilities(Service.SourceNat);
        if (!serviceProviderMap.containsKey(Service.SourceNat) && sourceNatServiceCapabilityMap != null && !sourceNatServiceCapabilityMap.isEmpty()) {
            throw new InvalidParameterValueException("Capabilities for source NAT service can be specifed only when source NAT service is enabled for network offering.");
        }
        validateSourceNatServiceCapablities(sourceNatServiceCapabilityMap);

        // validate the Static Nat service capabilities specified in the network
        // offering
        final Map<Capability, String> staticNatServiceCapabilityMap = cmd.getServiceCapabilities(Service.StaticNat);
        if (!serviceProviderMap.containsKey(Service.StaticNat) && sourceNatServiceCapabilityMap != null && !staticNatServiceCapabilityMap.isEmpty()) {
            throw new InvalidParameterValueException("Capabilities for static NAT service can be specifed only when static NAT service is enabled for network offering.");
        }
        validateStaticNatServiceCapablities(staticNatServiceCapabilityMap);

        // validate the 'Connectivity' service capabilities specified in the network offering, if 'Connectivity' service
        // is in the supported services of network offering
        final Map<Capability, String> connectivityServiceCapabilityMap = cmd.getServiceCapabilities(Service.Connectivity);
        if (!serviceProviderMap.containsKey(Service.Connectivity) &&
                connectivityServiceCapabilityMap != null && !connectivityServiceCapabilityMap.isEmpty())  {
            throw new InvalidParameterValueException("Capabilities for 'Connectivity' service can be specified " +
                    "only when Connectivity service is enabled for network offering.");
        }
        validateConnectivityServiceCapablities(serviceProviderMap.get(Service.Connectivity), connectivityServiceCapabilityMap);

        final Map<Service, Map<Capability, String>> serviceCapabilityMap = new HashMap<Service, Map<Capability, String>>();
        serviceCapabilityMap.put(Service.Lb, lbServiceCapabilityMap);
        serviceCapabilityMap.put(Service.SourceNat, sourceNatServiceCapabilityMap);
        serviceCapabilityMap.put(Service.StaticNat, staticNatServiceCapabilityMap);
        serviceCapabilityMap.put(Service.Connectivity, connectivityServiceCapabilityMap);

        // if Firewall service is missing, add Firewall service/provider
        // combination
        if (firewallProvider != null) {
            s_logger.debug("Adding Firewall service with provider " + firewallProvider.getName());
            final Set<Provider> firewallProviderSet = new HashSet<Provider>();
            firewallProviderSet.add(firewallProvider);
            serviceProviderMap.put(Service.Firewall, firewallProviderSet);
            if (!(firewallProvider.getName().equals(Provider.JuniperSRX.getName()) || firewallProvider.getName().equals(Provider.PaloAlto.getName()) || firewallProvider.getName()
                    .equals(Provider.VirtualRouter.getName())) && egressDefaultPolicy == false) {
                throw new InvalidParameterValueException("Firewall egress with default policy " + egressDefaultPolicy + " is not supported by the provider "
                        + firewallProvider.getName());
            }
        }

        final Map<NetworkOffering.Detail, String> details = new HashMap<NetworkOffering.Detail, String>();
        if (detailsStr != null) {
            for (final String detailStr : detailsStr.keySet()) {
                NetworkOffering.Detail offDetail = null;
                for (final NetworkOffering.Detail supportedDetail : NetworkOffering.Detail.values()) {
                    if (detailStr.equalsIgnoreCase(supportedDetail.toString())) {
                        offDetail = supportedDetail;
                        break;
                    }
                }
                if (offDetail == null) {
                    throw new InvalidParameterValueException("Unsupported detail " + detailStr);
                }
                details.put(offDetail, detailsStr.get(detailStr));
            }
        }

        final NetworkOffering offering = createNetworkOffering(name, displayText, trafficType, tags, specifyVlan, availability, networkRate, serviceProviderMap, false, guestType, false,
                serviceOfferingId, conserveMode, serviceCapabilityMap, specifyIpRanges, isPersistent, details, egressDefaultPolicy, maxconn, enableKeepAlive);
        CallContext.current().setEventDetails(" Id: " + offering.getId() + " Name: " + name);
        return offering;
    }

    void validateLoadBalancerServiceCapabilities(final Map<Capability, String> lbServiceCapabilityMap) {
        if (lbServiceCapabilityMap != null && !lbServiceCapabilityMap.isEmpty()) {
            if (lbServiceCapabilityMap.keySet().size() > 3 || !lbServiceCapabilityMap.containsKey(Capability.SupportedLBIsolation)) {
                throw new InvalidParameterValueException("Only " + Capability.SupportedLBIsolation.getName() + ", " + Capability.ElasticLb.getName() + ", "
                        + Capability.InlineMode.getName() + " capabilities can be sepcified for LB service");
            }

            for (final Capability cap : lbServiceCapabilityMap.keySet()) {
                final String value = lbServiceCapabilityMap.get(cap);
                if (cap == Capability.SupportedLBIsolation) {
                    final boolean dedicatedLb = value.contains("dedicated");
                    final boolean sharedLB = value.contains("shared");
                    if (dedicatedLb && sharedLB || !dedicatedLb && !sharedLB) {
                        throw new InvalidParameterValueException("Either dedicated or shared isolation can be specified for " + Capability.SupportedLBIsolation.getName());
                    }
                } else if (cap == Capability.ElasticLb) {
                    final boolean enabled = value.contains("true");
                    final boolean disabled = value.contains("false");
                    if (!enabled && !disabled) {
                        throw new InvalidParameterValueException("Unknown specified value for " + Capability.ElasticLb.getName());
                    }
                } else if (cap == Capability.InlineMode) {
                    final boolean enabled = value.contains("true");
                    final boolean disabled = value.contains("false");
                    if (!enabled && !disabled) {
                        throw new InvalidParameterValueException("Unknown specified value for " + Capability.InlineMode.getName());
                    }
                } else if (cap == Capability.LbSchemes) {
                    final boolean internalLb = value.contains("internal");
                    final boolean publicLb = value.contains("public");
                    if (!internalLb && !publicLb) {
                        throw new InvalidParameterValueException("Unknown specified value for " + Capability.LbSchemes.getName());
                    }
                } else {
                    throw new InvalidParameterValueException("Only " + Capability.SupportedLBIsolation.getName() + ", " + Capability.ElasticLb.getName() + ", "
                            + Capability.InlineMode.getName() + ", " + Capability.LbSchemes.getName() + " capabilities can be sepcified for LB service");
                }
            }
        }
    }

    void validateSourceNatServiceCapablities(final Map<Capability, String> sourceNatServiceCapabilityMap) {
        if (sourceNatServiceCapabilityMap != null && !sourceNatServiceCapabilityMap.isEmpty()) {
            if (sourceNatServiceCapabilityMap.keySet().size() > 2) {
                throw new InvalidParameterValueException("Only " + Capability.SupportedSourceNatTypes.getName() + " and " + Capability.RedundantRouter
                        + " capabilities can be sepcified for source nat service");
            }

            for (final Map.Entry<Capability ,String> srcNatPair : sourceNatServiceCapabilityMap.entrySet()) {
                final Capability capability = srcNatPair.getKey();
                final String value = srcNatPair.getValue();
                if (capability == Capability.SupportedSourceNatTypes) {
                    final boolean perAccount = value.contains("peraccount");
                    final boolean perZone = value.contains("perzone");
                    if (perAccount && perZone || !perAccount && !perZone) {
                        throw new InvalidParameterValueException("Either peraccount or perzone source NAT type can be specified for "
                                + Capability.SupportedSourceNatTypes.getName());
                    }
                } else if (capability == Capability.RedundantRouter) {
                    final boolean enabled = value.contains("true");
                    final boolean disabled = value.contains("false");
                    if (!enabled && !disabled) {
                        throw new InvalidParameterValueException("Unknown specified value for " + Capability.RedundantRouter.getName());
                    }
                } else {
                    throw new InvalidParameterValueException("Only " + Capability.SupportedSourceNatTypes.getName() + " and " + Capability.RedundantRouter
                            + " capabilities can be sepcified for source nat service");
                }
            }
        }
    }

    void validateStaticNatServiceCapablities(final Map<Capability, String> staticNatServiceCapabilityMap) {
        if (staticNatServiceCapabilityMap != null && !staticNatServiceCapabilityMap.isEmpty()) {
            boolean eipEnabled = false;
            boolean associatePublicIP = true;
            for (final Capability capability : staticNatServiceCapabilityMap.keySet()) {
                final String value = staticNatServiceCapabilityMap.get(capability).toLowerCase();
                if (!(value.contains("true") ^ value.contains("false"))) {
                    throw new InvalidParameterValueException("Unknown specified value (" + value + ") for " + capability);
                }
                if (capability == Capability.ElasticIp) {
                    eipEnabled = value.contains("true");
                } else if (capability == Capability.AssociatePublicIP) {
                    associatePublicIP = value.contains("true");
                } else {
                    throw new InvalidParameterValueException("Only " + Capability.ElasticIp.getName() + " and " + Capability.AssociatePublicIP.getName()
                            + " capabilitiy can be sepcified for static nat service");
                }
            }
            if (!eipEnabled && associatePublicIP) {
                throw new InvalidParameterValueException("Capability " + Capability.AssociatePublicIP.getName() + " can only be set when capability "
                        + Capability.ElasticIp.getName() + " is true");
            }
        }
    }

    void validateConnectivityServiceCapablities(final Set<Provider> providers, final Map<Capability, String> connectivityServiceCapabilityMap) {
        if (connectivityServiceCapabilityMap != null && !connectivityServiceCapabilityMap.isEmpty()) {
            for (final Map.Entry<Capability, String>entry: connectivityServiceCapabilityMap.entrySet()) {
                final Capability capability = entry.getKey();
                if (capability == Capability.StretchedL2Subnet) {
                    final String value = entry.getValue().toLowerCase();
                    if (!(value.contains("true") ^ value.contains("false"))) {
                        throw new InvalidParameterValueException("Invalid value (" + value + ") for " + capability +
                                " should be true/false");
                    }
                } else {
                    throw new InvalidParameterValueException("Capability " + capability.getName() + " can not be "
                            + " specified with connectivity service.");
                }
            }

            // validate connectivity service provider actually supports specified capabilities
            if (providers != null && !providers.isEmpty()) {
                for (final Provider provider: providers) {
                    final NetworkElement element = _networkModel.getElementImplementingProvider(provider.getName());
                    final Map<Service, Map<Capability, String>> capabilities = element.getCapabilities();
                    if (capabilities != null && !capabilities.isEmpty()) {
                        final Map<Capability, String> connectivityCapabilities =  capabilities.get(Service.Connectivity);
                        if (connectivityCapabilities == null || connectivityCapabilities != null && !connectivityCapabilities.keySet().contains(Capability.StretchedL2Subnet)) {
                            throw new InvalidParameterValueException("Provider: " + provider.getName() + " does not support "
                                    + Capability.StretchedL2Subnet.getName());
                        }
                    }
                }
            }
        }
    }
    @Override
    @DB
    public NetworkOfferingVO createNetworkOffering(final String name, final String displayText, final TrafficType trafficType, String tags, final boolean specifyVlan, final Availability availability,
            final Integer networkRate, final Map<Service, Set<Provider>> serviceProviderMap, final boolean isDefault, final Network.GuestType type, final boolean systemOnly, final Long serviceOfferingId,
            final boolean conserveMode, final Map<Service, Map<Capability, String>> serviceCapabilityMap, final boolean specifyIpRanges, final boolean isPersistent,
            final Map<NetworkOffering.Detail, String> details, final boolean egressDefaultPolicy, final Integer maxconn, final boolean enableKeepAlive) {

        final String multicastRateStr = _configDao.getValue("multicast.throttling.rate");
        final int multicastRate = multicastRateStr == null ? 10 : Integer.parseInt(multicastRateStr);
        tags = StringUtils.cleanupTags(tags);

        // specifyVlan should always be true for Shared network offerings
        if (!specifyVlan && type == GuestType.Shared) {
            throw new InvalidParameterValueException("SpecifyVlan should be true if network offering's type is " + type);
        }

        // specifyIpRanges should always be true for Shared networks
        // specifyIpRanges can only be true for Isolated networks with no Source
        // Nat service
        if (specifyIpRanges) {
            if (type == GuestType.Isolated) {
                if (serviceProviderMap.containsKey(Service.SourceNat)) {
                    throw new InvalidParameterValueException("SpecifyIpRanges can only be true for Shared network offerings and Isolated with no SourceNat service");
                }
            }
        } else {
            if (type == GuestType.Shared) {
                throw new InvalidParameterValueException("SpecifyIpRanges should always be true for Shared network offerings");
            }
        }

        // isPersistent should always be false for Shared network Offerings
        if (isPersistent && type == GuestType.Shared) {
            throw new InvalidParameterValueException("isPersistent should be false if network offering's type is " + type);
        }

        // validate availability value
        if (availability == NetworkOffering.Availability.Required) {
            final boolean canOffBeRequired = type == GuestType.Isolated && serviceProviderMap.containsKey(Service.SourceNat);
            if (!canOffBeRequired) {
                throw new InvalidParameterValueException("Availability can be " + NetworkOffering.Availability.Required + " only for networkOfferings of type "
                        + GuestType.Isolated + " and with " + Service.SourceNat.getName() + " enabled");
            }

            // only one network offering in the system can be Required
            final List<NetworkOfferingVO> offerings = _networkOfferingDao.listByAvailability(Availability.Required, false);
            if (!offerings.isEmpty()) {
                throw new InvalidParameterValueException("System already has network offering id=" + offerings.get(0).getId() + " with availability " + Availability.Required);
            }
        }

        boolean dedicatedLb = false;
        boolean elasticLb = false;
        boolean sharedSourceNat = false;
        boolean redundantRouter = false;
        boolean elasticIp = false;
        boolean associatePublicIp = false;
        boolean inline = false;
        boolean publicLb = false;
        boolean internalLb = false;
        boolean strechedL2Subnet = false;

        if (serviceCapabilityMap != null && !serviceCapabilityMap.isEmpty()) {
            final Map<Capability, String> lbServiceCapabilityMap = serviceCapabilityMap.get(Service.Lb);

            if (lbServiceCapabilityMap != null && !lbServiceCapabilityMap.isEmpty()) {
                final String isolationCapability = lbServiceCapabilityMap.get(Capability.SupportedLBIsolation);
                if (isolationCapability != null) {
                    _networkModel.checkCapabilityForProvider(serviceProviderMap.get(Service.Lb), Service.Lb, Capability.SupportedLBIsolation, isolationCapability);
                    dedicatedLb = isolationCapability.contains("dedicated");
                } else {
                    dedicatedLb = true;
                }

                final String param = lbServiceCapabilityMap.get(Capability.ElasticLb);
                if (param != null) {
                    elasticLb = param.contains("true");
                }

                final String inlineMode = lbServiceCapabilityMap.get(Capability.InlineMode);
                if (inlineMode != null) {
                    _networkModel.checkCapabilityForProvider(serviceProviderMap.get(Service.Lb), Service.Lb, Capability.InlineMode, inlineMode);
                    inline = inlineMode.contains("true");
                } else {
                    inline = false;
                }

                final String publicLbStr = lbServiceCapabilityMap.get(Capability.LbSchemes);
                if (serviceProviderMap.containsKey(Service.Lb)) {
                    if (publicLbStr != null) {
                        _networkModel.checkCapabilityForProvider(serviceProviderMap.get(Service.Lb), Service.Lb, Capability.LbSchemes, publicLbStr);
                        internalLb = publicLbStr.contains("internal");
                        publicLb = publicLbStr.contains("public");
                    }
                }
            }

            // in the current version of the code, publicLb and specificLb can't
            // both be set to true for the same network offering
            if (publicLb && internalLb) {
                throw new InvalidParameterValueException("Public lb and internal lb can't be enabled at the same time on the offering");
            }

            final Map<Capability, String> sourceNatServiceCapabilityMap = serviceCapabilityMap.get(Service.SourceNat);
            if (sourceNatServiceCapabilityMap != null && !sourceNatServiceCapabilityMap.isEmpty()) {
                final String sourceNatType = sourceNatServiceCapabilityMap.get(Capability.SupportedSourceNatTypes);
                if (sourceNatType != null) {
                    _networkModel.checkCapabilityForProvider(serviceProviderMap.get(Service.SourceNat), Service.SourceNat, Capability.SupportedSourceNatTypes, sourceNatType);
                    sharedSourceNat = sourceNatType.contains("perzone");
                }

                final String param = sourceNatServiceCapabilityMap.get(Capability.RedundantRouter);
                if (param != null) {
                    _networkModel.checkCapabilityForProvider(serviceProviderMap.get(Service.SourceNat), Service.SourceNat, Capability.RedundantRouter, param);
                    redundantRouter = param.contains("true");
                }
            }

            final Map<Capability, String> staticNatServiceCapabilityMap = serviceCapabilityMap.get(Service.StaticNat);
            if (staticNatServiceCapabilityMap != null && !staticNatServiceCapabilityMap.isEmpty()) {
                final String param = staticNatServiceCapabilityMap.get(Capability.ElasticIp);
                if (param != null) {
                    elasticIp = param.contains("true");
                    final String associatePublicIP = staticNatServiceCapabilityMap.get(Capability.AssociatePublicIP);
                    if (associatePublicIP != null) {
                        associatePublicIp = associatePublicIP.contains("true");
                    }
                }
            }

            final Map<Capability, String> connectivityServiceCapabilityMap = serviceCapabilityMap.get(Service.Connectivity);
            if (connectivityServiceCapabilityMap != null && !connectivityServiceCapabilityMap.isEmpty()) {
                final String value = connectivityServiceCapabilityMap.get(Capability.StretchedL2Subnet);
                if ("true".equalsIgnoreCase(value)) {
                    strechedL2Subnet = true;
                }
            }
        }

        if (serviceProviderMap != null && serviceProviderMap.containsKey(Service.Lb) && !internalLb && !publicLb) {
            //if not specified, default public lb to true
            publicLb = true;
        }

        final NetworkOfferingVO offeringFinal = new NetworkOfferingVO(name, displayText, trafficType, systemOnly, specifyVlan, networkRate, multicastRate, isDefault, availability,
                tags, type, conserveMode, dedicatedLb, sharedSourceNat, redundantRouter, elasticIp, elasticLb, specifyIpRanges, inline, isPersistent, associatePublicIp, publicLb,
                internalLb, egressDefaultPolicy, strechedL2Subnet);

        if (serviceOfferingId != null) {
            offeringFinal.setServiceOfferingId(serviceOfferingId);
        }

        // validate the details
        if (details != null) {
            validateNtwkOffDetails(details, serviceProviderMap);
        }

        return Transaction.execute(new TransactionCallback<NetworkOfferingVO>() {
            @Override
            public NetworkOfferingVO doInTransaction(final TransactionStatus status) {
                NetworkOfferingVO offering = offeringFinal;

                // 1) create network offering object
                s_logger.debug("Adding network offering " + offering);
                offering.setConcurrentConnections(maxconn);
                offering.setKeepAliveEnabled(enableKeepAlive);
                offering = _networkOfferingDao.persist(offering, details);
                // 2) populate services and providers
                if (serviceProviderMap != null) {
                    for (final Network.Service service : serviceProviderMap.keySet()) {
                        final Set<Provider> providers = serviceProviderMap.get(service);
                        if (providers != null && !providers.isEmpty()) {
                            boolean vpcOff = false;
                            for (final Network.Provider provider : providers) {
                                if (provider == Provider.VPCVirtualRouter) {
                                    vpcOff = true;
                                }
                                final NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO(offering.getId(), service, provider);
                                _ntwkOffServiceMapDao.persist(offService);
                                s_logger.trace("Added service for the network offering: " + offService + " with provider " + provider.getName());
                            }

                            if (vpcOff) {
                                final List<Service> supportedSvcs = new ArrayList<Service>();
                                supportedSvcs.addAll(serviceProviderMap.keySet());
                                _vpcMgr.validateNtwkOffForVpc(offering, supportedSvcs);
                            }
                        } else {
                            final NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO(offering.getId(), service, null);
                            _ntwkOffServiceMapDao.persist(offService);
                            s_logger.trace("Added service for the network offering: " + offService + " with null provider");
                        }
                    }
                }

                return offering;
            }
        });
    }

    protected void validateNtwkOffDetails(final Map<Detail, String> details, final Map<Service, Set<Provider>> serviceProviderMap) {
        for (final Detail detail : details.keySet()) {

            Provider lbProvider = null;
            if (detail == NetworkOffering.Detail.InternalLbProvider || detail == NetworkOffering.Detail.PublicLbProvider) {
                // 1) Vaidate the detail values - have to match the lb provider
                // name
                final String providerStr = details.get(detail);
                if (Network.Provider.getProvider(providerStr) == null) {
                    throw new InvalidParameterValueException("Invalid value " + providerStr + " for the detail " + detail);
                }
                if (serviceProviderMap.get(Service.Lb) != null) {
                    for (final Provider provider : serviceProviderMap.get(Service.Lb)) {
                        if (provider.getName().equalsIgnoreCase(providerStr)) {
                            lbProvider = provider;
                            break;
                        }
                    }
                }

                if (lbProvider == null) {
                    throw new InvalidParameterValueException("Invalid value " + details.get(detail) + " for the detail " + detail
                            + ". The provider is not supported by the network offering");
                }

                // 2) validate if the provider supports the scheme
                final Set<Provider> lbProviders = new HashSet<Provider>();
                lbProviders.add(lbProvider);
                if (detail == NetworkOffering.Detail.InternalLbProvider) {
                    _networkModel.checkCapabilityForProvider(lbProviders, Service.Lb, Capability.LbSchemes, Scheme.Internal.toString());
                } else if (detail == NetworkOffering.Detail.PublicLbProvider) {
                    _networkModel.checkCapabilityForProvider(lbProviders, Service.Lb, Capability.LbSchemes, Scheme.Public.toString());
                }
            }
        }
    }

    @Override
    public Pair<List<? extends NetworkOffering>, Integer> searchForNetworkOfferings(final ListNetworkOfferingsCmd cmd) {
        Boolean isAscending = Boolean.parseBoolean(_configDao.getValue("sortkey.algorithm"));
        isAscending = isAscending == null ? Boolean.TRUE : isAscending;
        final Filter searchFilter = new Filter(NetworkOfferingVO.class, "sortKey", isAscending, null, null);
        final Account caller = CallContext.current().getCallingAccount();
        final SearchCriteria<NetworkOfferingVO> sc = _networkOfferingDao.createSearchCriteria();

        final Long id = cmd.getId();
        final Object name = cmd.getNetworkOfferingName();
        final Object displayText = cmd.getDisplayText();
        final Object trafficType = cmd.getTrafficType();
        final Object isDefault = cmd.getIsDefault();
        final Object specifyVlan = cmd.getSpecifyVlan();
        final Object availability = cmd.getAvailability();
        final Object state = cmd.getState();
        final Long zoneId = cmd.getZoneId();
        DataCenter zone = null;
        final Long networkId = cmd.getNetworkId();
        final String guestIpType = cmd.getGuestIpType();
        final List<String> supportedServicesStr = cmd.getSupportedServices();
        final Object specifyIpRanges = cmd.getSpecifyIpRanges();
        final String tags = cmd.getTags();
        final Boolean isTagged = cmd.isTagged();
        final Boolean forVpc = cmd.getForVpc();

        if (zoneId != null) {
            zone = _entityMgr.findById(DataCenter.class, zoneId);
            if (zone == null) {
                throw new InvalidParameterValueException("Unable to find the zone by id=" + zoneId);
            }
        }

        final Object keyword = cmd.getKeyword();

        if (keyword != null) {
            final SearchCriteria<NetworkOfferingVO> ssc = _networkOfferingDao.createSearchCriteria();
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }

        if (guestIpType != null) {
            sc.addAnd("guestType", SearchCriteria.Op.EQ, guestIpType);
        }

        if (displayText != null) {
            sc.addAnd("displayText", SearchCriteria.Op.LIKE, "%" + displayText + "%");
        }

        if (trafficType != null) {
            sc.addAnd("trafficType", SearchCriteria.Op.EQ, trafficType);
        }

        if (isDefault != null) {
            sc.addAnd("isDefault", SearchCriteria.Op.EQ, isDefault);
        }

        // only root admin can list network offering with specifyVlan = true
        if (specifyVlan != null) {
            sc.addAnd("specifyVlan", SearchCriteria.Op.EQ, specifyVlan);
        }

        if (availability != null) {
            sc.addAnd("availability", SearchCriteria.Op.EQ, availability);
        }

        if (state != null) {
            sc.addAnd("state", SearchCriteria.Op.EQ, state);
        }

        if (specifyIpRanges != null) {
            sc.addAnd("specifyIpRanges", SearchCriteria.Op.EQ, specifyIpRanges);
        }

        if (zone != null) {
            if (zone.getNetworkType() == NetworkType.Basic) {
                // return empty list as we don't allow to create networks in
                // basic zone, and shouldn't display networkOfferings
                return new Pair<List<? extends NetworkOffering>, Integer>(new ArrayList<NetworkOffering>(), 0);
            }
        }

        // Don't return system network offerings to the user
        sc.addAnd("systemOnly", SearchCriteria.Op.EQ, false);

        // if networkId is specified, list offerings available for upgrade only
        // (for this network)
        Network network = null;
        if (networkId != null) {
            // check if network exists and the caller can operate with it
            network = _networkModel.getNetwork(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Unable to find the network by id=" + networkId);
            }
            // Don't allow to update system network
            final NetworkOffering offering = _networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
            if (offering.isSystemOnly()) {
                throw new InvalidParameterValueException("Can't update system networks");
            }

            _accountMgr.checkAccess(caller, null, true, network);

            final List<Long> offeringIds = _networkModel.listNetworkOfferingsForUpgrade(networkId);

            if (!offeringIds.isEmpty()) {
                sc.addAnd("id", SearchCriteria.Op.IN, offeringIds.toArray());
            } else {
                return new Pair<List<? extends NetworkOffering>, Integer>(new ArrayList<NetworkOffering>(), 0);
            }
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (tags != null) {
            sc.addAnd("tags", SearchCriteria.Op.EQ, tags);
        }

        if (isTagged != null) {
            if (isTagged) {
                sc.addAnd("tags", SearchCriteria.Op.NNULL);
            } else {
                sc.addAnd("tags", SearchCriteria.Op.NULL);
            }
        }

        final List<NetworkOfferingVO> offerings = _networkOfferingDao.search(sc, searchFilter);
        final Boolean sourceNatSupported = cmd.getSourceNatSupported();
        final List<String> pNtwkTags = new ArrayList<String>();
        boolean checkForTags = false;
        if (zone != null) {
            final List<PhysicalNetworkVO> pNtwks = _physicalNetworkDao.listByZoneAndTrafficType(zoneId, TrafficType.Guest);
            if (pNtwks.size() > 1) {
                checkForTags = true;
                // go through tags
                for (final PhysicalNetworkVO pNtwk : pNtwks) {
                    final List<String> pNtwkTag = pNtwk.getTags();
                    if (pNtwkTag == null || pNtwkTag.isEmpty()) {
                        throw new CloudRuntimeException("Tags are not defined for physical network in the zone id=" + zoneId);
                    }
                    pNtwkTags.addAll(pNtwkTag);
                }
            }
        }

        // filter by supported services
        final boolean listBySupportedServices = supportedServicesStr != null && !supportedServicesStr.isEmpty() && !offerings.isEmpty();
        final boolean checkIfProvidersAreEnabled = zoneId != null;
        final boolean parseOfferings = listBySupportedServices || sourceNatSupported != null || checkIfProvidersAreEnabled || forVpc != null || network != null;

        if (parseOfferings) {
            final List<NetworkOfferingVO> supportedOfferings = new ArrayList<NetworkOfferingVO>();
            Service[] supportedServices = null;

            if (listBySupportedServices) {
                supportedServices = new Service[supportedServicesStr.size()];
                int i = 0;
                for (final String supportedServiceStr : supportedServicesStr) {
                    final Service service = Service.getService(supportedServiceStr);
                    if (service == null) {
                        throw new InvalidParameterValueException("Invalid service specified " + supportedServiceStr);
                    } else {
                        supportedServices[i] = service;
                    }
                    i++;
                }
            }

            for (final NetworkOfferingVO offering : offerings) {
                boolean addOffering = true;
                List<Service> checkForProviders = new ArrayList<Service>();

                if (checkForTags) {
                    if (!pNtwkTags.contains(offering.getTags())) {
                        continue;
                    }
                }

                if (listBySupportedServices) {
                    addOffering = addOffering && _networkModel.areServicesSupportedByNetworkOffering(offering.getId(), supportedServices);
                }

                if (checkIfProvidersAreEnabled) {
                    if (supportedServices != null && supportedServices.length > 0) {
                        checkForProviders = Arrays.asList(supportedServices);
                    } else {
                        checkForProviders = _networkModel.listNetworkOfferingServices(offering.getId());
                    }

                    addOffering = addOffering && _networkModel.areServicesEnabledInZone(zoneId, offering, checkForProviders);
                }

                if (sourceNatSupported != null) {
                    addOffering = addOffering && _networkModel.areServicesSupportedByNetworkOffering(offering.getId(), Network.Service.SourceNat) == sourceNatSupported;
                }

                if (forVpc != null) {
                    addOffering = addOffering && isOfferingForVpc(offering) == forVpc.booleanValue();
                } else if (network != null) {
                    addOffering = addOffering && isOfferingForVpc(offering) == (network.getVpcId() != null);
                }

                if (addOffering) {
                    supportedOfferings.add(offering);
                }

            }

            // Now apply pagination
            final List<? extends NetworkOffering> wPagination = StringUtils.applyPagination(supportedOfferings, cmd.getStartIndex(), cmd.getPageSizeVal());
            if (wPagination != null) {
                final Pair<List<? extends NetworkOffering>, Integer> listWPagination = new Pair<List<? extends NetworkOffering>, Integer>(wPagination, offerings.size());
                return listWPagination;
            }
            return new Pair<List<? extends NetworkOffering>, Integer>(supportedOfferings, supportedOfferings.size());
        } else {
            final List<? extends NetworkOffering> wPagination = StringUtils.applyPagination(offerings, cmd.getStartIndex(), cmd.getPageSizeVal());
            if (wPagination != null) {
                final Pair<List<? extends NetworkOffering>, Integer> listWPagination = new Pair<List<? extends NetworkOffering>, Integer>(wPagination, offerings.size());
                return listWPagination;
            }
            return new Pair<List<? extends NetworkOffering>, Integer>(offerings, offerings.size());
        }
    }

    @Override
    public boolean isOfferingForVpc(final NetworkOffering offering) {
        final boolean vpcProvider = _ntwkOffServiceMapDao.isProviderForNetworkOffering(offering.getId(), Provider.VPCVirtualRouter) ||
                _ntwkOffServiceMapDao.isProviderForNetworkOffering(offering.getId(), Provider.JuniperContrailVpcRouter) ||
                _ntwkOffServiceMapDao.getDistinctProviders(offering.getId()).contains(Provider.NuageVsp.getName());

        return vpcProvider;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_OFFERING_DELETE, eventDescription = "deleting network offering")
    public boolean deleteNetworkOffering(final DeleteNetworkOfferingCmd cmd) {
        final Long offeringId = cmd.getId();
        CallContext.current().setEventDetails(" Id: " + offeringId);

        // Verify network offering id
        final NetworkOfferingVO offering = _networkOfferingDao.findById(offeringId);
        if (offering == null) {
            throw new InvalidParameterValueException("unable to find network offering " + offeringId);
        } else if (offering.getRemoved() != null || offering.isSystemOnly()) {
            throw new InvalidParameterValueException("unable to find network offering " + offeringId);
        }

        // Don't allow to delete default network offerings
        if (offering.isDefault() == true) {
            throw new InvalidParameterValueException("Default network offering can't be deleted");
        }

        // don't allow to delete network offering if it's in use by existing
        // networks (the offering can be disabled
        // though)
        final int networkCount = _networkDao.getNetworkCountByNetworkOffId(offeringId);
        if (networkCount > 0) {
            throw new InvalidParameterValueException("Can't delete network offering " + offeringId + " as its used by " + networkCount + " networks. "
                    + "To make the network offering unavaiable, disable it");
        }

        if (_networkOfferingDao.remove(offeringId)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_OFFERING_EDIT, eventDescription = "updating network offering")
    public NetworkOffering updateNetworkOffering(final UpdateNetworkOfferingCmd cmd) {
        final String displayText = cmd.getDisplayText();
        final Long id = cmd.getId();
        final String name = cmd.getNetworkOfferingName();
        final String availabilityStr = cmd.getAvailability();
        final Integer sortKey = cmd.getSortKey();
        final Integer maxconn = cmd.getMaxconnections();
        Availability availability = null;
        final String state = cmd.getState();
        CallContext.current().setEventDetails(" Id: " + id);

        // Verify input parameters
        final NetworkOfferingVO offeringToUpdate = _networkOfferingDao.findById(id);
        if (offeringToUpdate == null) {
            throw new InvalidParameterValueException("unable to find network offering " + id);
        }

        // Don't allow to update system network offering
        if (offeringToUpdate.isSystemOnly()) {
            throw new InvalidParameterValueException("Can't update system network offerings");
        }

        final NetworkOfferingVO offering = _networkOfferingDao.createForUpdate(id);

        if (name != null) {
            offering.setName(name);
        }

        if (displayText != null) {
            offering.setDisplayText(displayText);
        }

        if (sortKey != null) {
            offering.setSortKey(sortKey);
        }

        if (state != null) {
            boolean validState = false;
            for (final NetworkOffering.State st : NetworkOffering.State.values()) {
                if (st.name().equalsIgnoreCase(state)) {
                    validState = true;
                    offering.setState(st);
                }
            }
            if (!validState) {
                throw new InvalidParameterValueException("Incorrect state value: " + state);
            }
        }

        // Verify availability
        if (availabilityStr != null) {
            for (final Availability avlb : Availability.values()) {
                if (avlb.name().equalsIgnoreCase(availabilityStr)) {
                    availability = avlb;
                }
            }
            if (availability == null) {
                throw new InvalidParameterValueException("Invalid value for Availability. Supported types: " + Availability.Required + ", " + Availability.Optional);
            } else {
                if (availability == NetworkOffering.Availability.Required) {
                    final boolean canOffBeRequired = offeringToUpdate.getGuestType() == GuestType.Isolated && _networkModel.areServicesSupportedByNetworkOffering(
                            offeringToUpdate.getId(), Service.SourceNat);
                    if (!canOffBeRequired) {
                        throw new InvalidParameterValueException("Availability can be " + NetworkOffering.Availability.Required + " only for networkOfferings of type "
                                + GuestType.Isolated + " and with " + Service.SourceNat.getName() + " enabled");
                    }

                    // only one network offering in the system can be Required
                    final List<NetworkOfferingVO> offerings = _networkOfferingDao.listByAvailability(Availability.Required, false);
                    if (!offerings.isEmpty() && offerings.get(0).getId() != offeringToUpdate.getId()) {
                        throw new InvalidParameterValueException("System already has network offering id=" + offerings.get(0).getId() + " with availability "
                                + Availability.Required);
                    }
                }
                offering.setAvailability(availability);
            }
        }
        if (_ntwkOffServiceMapDao.areServicesSupportedByNetworkOffering(offering.getId(), Service.Lb)) {
            if (maxconn != null) {
                offering.setConcurrentConnections(maxconn);
            }
        }

        if (_networkOfferingDao.update(id, offering)) {
            return _networkOfferingDao.findById(id);
        } else {
            return null;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACCOUNT_MARK_DEFAULT_ZONE, eventDescription = "Marking account with the " + "default zone", async = true)
    public AccountVO markDefaultZone(final String accountName, final long domainId, final long defaultZoneId) {

        // Check if the account exists
        final Account account = _accountDao.findEnabledAccount(accountName, domainId);
        if (account == null) {
            s_logger.error("Unable to find account by name: " + accountName + " in domain " + domainId);
            throw new InvalidParameterValueException("Account by name: " + accountName + " doesn't exist in domain " + domainId);
        }

        // Don't allow modification of system account
        if (account.getId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("Can not modify system account");
        }

        final AccountVO acctForUpdate = _accountDao.findById(account.getId());

        acctForUpdate.setDefaultZoneId(defaultZoneId);

        if (_accountDao.update(account.getId(), acctForUpdate)) {
            CallContext.current().setEventDetails("Default zone id= " + defaultZoneId);
            return _accountDao.findById(account.getId());
        } else {
            return null;
        }
    }

    // Note: This method will be used for entity name validations in the coming
    // releases (place holder for now)
    @SuppressWarnings("unused")
    private void validateEntityName(final String str) {
        final String forbidden = "~!@#$%^&*()+=";
        final char[] searchChars = forbidden.toCharArray();
        if (str == null || str.length() == 0 || searchChars == null || searchChars.length == 0) {
            return;
        }
        for (int i = 0; i < str.length(); i++) {
            final char ch = str.charAt(i);
            for (int j = 0; j < searchChars.length; j++) {
                if (searchChars[j] == ch) {
                    throw new InvalidParameterValueException("Name cannot contain any of the following special characters:" + forbidden);
                }
            }
        }
    }

    @Override
    public Integer getNetworkOfferingNetworkRate(final long networkOfferingId, final Long dataCenterId) {

        // validate network offering information
        final NetworkOffering no = _entityMgr.findById(NetworkOffering.class, networkOfferingId);
        if (no == null) {
            throw new InvalidParameterValueException("Unable to find network offering by id=" + networkOfferingId);
        }

        Integer networkRate;
        if (no.getRateMbps() != null) {
            networkRate = no.getRateMbps();
        } else {
            networkRate = NetworkOrchestrationService.NetworkThrottlingRate.valueIn(dataCenterId);
        }

        // networkRate is unsigned int in netowrkOfferings table, and can't be
        // set to -1
        // so 0 means unlimited; we convert it to -1, so we are consistent with
        // all our other resources where -1 means unlimited
        if (networkRate == 0) {
            networkRate = -1;
        }

        return networkRate;
    }

    @Override
    public Account getVlanAccount(final long vlanId) {
        final Vlan vlan = _vlanDao.findById(vlanId);

        // if vlan is Virtual Account specific, get vlan information from the
        // accountVlanMap; otherwise get account information
        // from the network
        if (vlan.getVlanType() == VlanType.VirtualNetwork) {
            final List<AccountVlanMapVO> maps = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanId);
            if (maps != null && !maps.isEmpty()) {
                return _accountMgr.getAccount(maps.get(0).getAccountId());
            }
        }

        final Long networkId = vlan.getNetworkId();
        if (networkId != null) {
            final Network network = _networkModel.getNetwork(networkId);
            if (network != null) {
                return _accountMgr.getAccount(network.getAccountId());
            }
        }
        return null;
    }

    @Override
    public List<? extends NetworkOffering> listNetworkOfferings(final TrafficType trafficType, final boolean systemOnly) {
        final Filter searchFilter = new Filter(NetworkOfferingVO.class, "created", false, null, null);
        final SearchCriteria<NetworkOfferingVO> sc = _networkOfferingDao.createSearchCriteria();
        if (trafficType != null) {
            sc.addAnd("trafficType", SearchCriteria.Op.EQ, trafficType);
        }
        sc.addAnd("systemOnly", SearchCriteria.Op.EQ, systemOnly);

        return _networkOfferingDao.search(sc, searchFilter);
    }

    @Override
    @DB
    public boolean releaseAccountSpecificVirtualRanges(final long accountId) {
        final List<AccountVlanMapVO> maps = _accountVlanMapDao.listAccountVlanMapsByAccount(accountId);
        if (maps != null && !maps.isEmpty()) {
            try {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(final TransactionStatus status) {
                        for (final AccountVlanMapVO map : maps) {
                            if (!releasePublicIpRange(map.getVlanDbId(), _accountMgr.getSystemUser().getId(), _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM))) {
                                throw new CloudRuntimeException("Failed to release account specific virtual ip ranges for account id=" + accountId);
                            }
                        }
                    }
                });
            } catch (final CloudRuntimeException e) {
                s_logger.error(e);
                return false;
            }
        } else {
            s_logger.trace("Account id=" + accountId + " has no account specific virtual ip ranges, nothing to release");
        }
        return true;
    }

    @Override
    public AllocationState findClusterAllocationState(final ClusterVO cluster) {

        if (cluster.getAllocationState() == AllocationState.Disabled) {
            return AllocationState.Disabled;
        } else if (ApiDBUtils.findPodById(cluster.getPodId()).getAllocationState() == AllocationState.Disabled) {
            return AllocationState.Disabled;
        } else {
            final DataCenterVO zone = ApiDBUtils.findZoneById(cluster.getDataCenterId());
            return zone.getAllocationState();
        }
    }

    @Override
    public AllocationState findPodAllocationState(final HostPodVO pod) {

        if (pod.getAllocationState() == AllocationState.Disabled) {
            return AllocationState.Disabled;
        } else {
            final DataCenterVO zone = ApiDBUtils.findZoneById(pod.getDataCenterId());
            return zone.getAllocationState();
        }
    }

    @Override
    public Long getDefaultPageSize() {
        return _defaultPageSize;
    }

    @Override
    public Integer getServiceOfferingNetworkRate(final long serviceOfferingId, final Long dataCenterId) {

        // validate network offering information
        final ServiceOffering offering = _serviceOfferingDao.findById(serviceOfferingId);
        if (offering == null) {
            throw new InvalidParameterValueException("Unable to find service offering by id=" + serviceOfferingId);
        }

        Integer networkRate;
        if (offering.getRateMbps() != null) {
            networkRate = offering.getRateMbps();
        } else {
            // for domain router service offering, get network rate from
            if (offering.getSystemVmType() != null && offering.getSystemVmType().equalsIgnoreCase(VirtualMachine.Type.DomainRouter.toString())) {
                networkRate = NetworkOrchestrationService.NetworkThrottlingRate.valueIn(dataCenterId);
            } else {
                networkRate = Integer.parseInt(_configDao.getValue(Config.VmNetworkThrottlingRate.key()));
            }
        }

        // networkRate is unsigned int in serviceOffering table, and can't be
        // set to -1
        // so 0 means unlimited; we convert it to -1, so we are consistent with
        // all our other resources where -1 means unlimited
        if (networkRate == 0) {
            networkRate = -1;
        }

        return networkRate;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PORTABLE_IP_RANGE_CREATE, eventDescription = "creating portable ip range", async = false)
    public PortableIpRange createPortableIpRange(final CreatePortableIpRangeCmd cmd) throws ConcurrentOperationException {
        final Integer regionId = cmd.getRegionId();
        final String startIP = cmd.getStartIp();
        final String endIP = cmd.getEndIp();
        final String gateway = cmd.getGateway();
        final String netmask = cmd.getNetmask();
        String vlanId = cmd.getVlan();

        final RegionVO region = _regionDao.findById(regionId);
        if (region == null) {
            throw new InvalidParameterValueException("Invalid region ID: " + regionId);
        }

        if (!NetUtils.isValidIp(startIP) || !NetUtils.isValidIp(endIP) || !NetUtils.validIpRange(startIP, endIP)) {
            throw new InvalidParameterValueException("Invalid portable ip  range: " + startIP + "-" + endIP);
        }

        if (!NetUtils.sameSubnet(startIP, gateway, netmask)) {
            throw new InvalidParameterValueException("Please ensure that your start IP is in the same subnet as "
                    + "your portable IP range's gateway and as per the IP range's netmask.");
        }

        if (!NetUtils.sameSubnet(endIP, gateway, netmask)) {
            throw new InvalidParameterValueException("Please ensure that your end IP is in the same subnet as "
                    + "your portable IP range's gateway and as per the IP range's netmask.");
        }

        if (checkOverlapPortableIpRange(regionId, startIP, endIP)) {
            throw new InvalidParameterValueException("Ip  range: " + startIP + "-" + endIP + " overlaps with a portable" + " IP range already configured in the region " + regionId);
        }

        if (vlanId == null) {
            vlanId = Vlan.UNTAGGED;
        } else {
            if (!NetUtils.isValidVlan(vlanId)) {
                throw new InvalidParameterValueException("Invalid vlan id " + vlanId);
            }

            final List<DataCenterVO> zones = _zoneDao.listAllZones();
            if (zones != null && !zones.isEmpty()) {
                for (final DataCenterVO zone : zones) {
                    // check if there is zone vlan with same id
                    if (_vlanDao.findByZoneAndVlanId(zone.getId(), vlanId) != null) {
                        throw new InvalidParameterValueException("Found a VLAN id " + vlanId + " already existing in" + " zone " + zone.getUuid()
                                + " that conflicts with VLAN id of the portable ip range being configured");
                    }
                    //check if there is a public ip range that overlaps with portable ip range being created
                    checkOverlapPublicIpRange(zone.getId(), startIP, endIP);
                }
            }

        }
        final GlobalLock portableIpLock = GlobalLock.getInternLock("PortablePublicIpRange");
        portableIpLock.lock(5);
        try {
            final String vlanIdFinal = vlanId;
            return Transaction.execute(new TransactionCallback<PortableIpRangeVO>() {
                @Override
                public PortableIpRangeVO doInTransaction(final TransactionStatus status) {
                    PortableIpRangeVO portableIpRange = new PortableIpRangeVO(regionId, vlanIdFinal, gateway, netmask, startIP, endIP);
                    portableIpRange = _portableIpRangeDao.persist(portableIpRange);

                    long startIpLong = NetUtils.ip2Long(startIP);
                    final long endIpLong = NetUtils.ip2Long(endIP);
                    while (startIpLong <= endIpLong) {
                        final PortableIpVO portableIP = new PortableIpVO(regionId, portableIpRange.getId(), vlanIdFinal, gateway, netmask, NetUtils.long2Ip(startIpLong));
                        _portableIpDao.persist(portableIP);
                        startIpLong++;
                    }

                    // implicitly enable portable IP service for the region
                    region.setPortableipEnabled(true);
                    _regionDao.update(region.getId(), region);

                    return portableIpRange;
                }
            });
        } finally {
            portableIpLock.unlock();
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PORTABLE_IP_RANGE_DELETE, eventDescription = "deleting portable ip range", async = false)
    public boolean deletePortableIpRange(final DeletePortableIpRangeCmd cmd) {
        final long rangeId = cmd.getId();

        final PortableIpRangeVO portableIpRange = _portableIpRangeDao.findById(rangeId);
        if (portableIpRange == null) {
            throw new InvalidParameterValueException("Please specify a valid portable IP range id.");
        }

        final List<PortableIpVO> fullIpRange = _portableIpDao.listByRangeId(portableIpRange.getId());
        final List<PortableIpVO> freeIpRange = _portableIpDao.listByRangeIdAndState(portableIpRange.getId(), PortableIp.State.Free);

        if (fullIpRange != null && freeIpRange != null) {
            if (fullIpRange.size() == freeIpRange.size()) {
                _portableIpRangeDao.expunge(portableIpRange.getId());
                final List<PortableIpRangeVO> pipranges = _portableIpRangeDao.listAll();
                if (pipranges == null || pipranges.isEmpty()) {
                    final RegionVO region = _regionDao.findById(portableIpRange.getRegionId());
                    region.setPortableipEnabled(false);
                    _regionDao.update(region.getId(), region);
                }
                return true;
            } else {
                throw new InvalidParameterValueException("Can't delete portable IP range as there are IP's assigned.");
            }
        }
        return false;
    }

    @Override
    public List<? extends PortableIpRange> listPortableIpRanges(final ListPortableIpRangesCmd cmd) {
        final Integer regionId = cmd.getRegionIdId();
        final Long rangeId = cmd.getPortableIpRangeId();

        final List<PortableIpRangeVO> ranges = new ArrayList<PortableIpRangeVO>();
        if (regionId != null) {
            final Region region = _regionDao.findById(regionId);
            if (region == null) {
                throw new InvalidParameterValueException("Invalid region ID: " + regionId);
            }
            return _portableIpRangeDao.listByRegionId(regionId);
        }

        if (rangeId != null) {
            final PortableIpRangeVO range = _portableIpRangeDao.findById(rangeId);
            if (range == null) {
                throw new InvalidParameterValueException("Invalid portable IP range ID: " + regionId);
            }
            ranges.add(range);
            return ranges;
        }

        return _portableIpRangeDao.listAll();
    }

    @Override
    public List<? extends PortableIp> listPortableIps(final long id) {

        final PortableIpRangeVO portableIpRange = _portableIpRangeDao.findById(id);
        if (portableIpRange == null) {
            throw new InvalidParameterValueException("Please specify a valid portable IP range id.");
        }

        return _portableIpDao.listByRangeId(portableIpRange.getId());
    }

    private boolean checkOverlapPortableIpRange(final int regionId, final String newStartIpStr, final String newEndIpStr) {
        final long newStartIp = NetUtils.ip2Long(newStartIpStr);
        final long newEndIp = NetUtils.ip2Long(newEndIpStr);

        final List<PortableIpRangeVO> existingPortableIPRanges = _portableIpRangeDao.listByRegionId(regionId);

        if (existingPortableIPRanges == null || existingPortableIPRanges.isEmpty()) {
            return false;
        }

        for (final PortableIpRangeVO portableIpRange : existingPortableIPRanges) {
            final String ipRangeStr = portableIpRange.getIpRange();
            final String[] range = ipRangeStr.split("-");
            final long startip = NetUtils.ip2Long(range[0]);
            final long endIp = NetUtils.ip2Long(range[1]);

            if (newStartIp >= startip && newStartIp <= endIp || newEndIp >= startip && newEndIp <= endIp) {
                return true;
            }

            if (startip >= newStartIp && startip <= newEndIp || endIp >= newStartIp && endIp <= newEndIp) {
                return true;
            }
        }
        return false;
    }

    public List<SecurityChecker> getSecChecker() {
        return _secChecker;
    }

    @Inject
    public void setSecChecker(final List<SecurityChecker> secChecker) {
        _secChecker = secChecker;
    }

    @Override
    public String getConfigComponentName() {
        return ConfigurationManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {SystemVMUseLocalStorage};
    }
}
