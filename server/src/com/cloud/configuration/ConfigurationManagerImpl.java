/**
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
package com.cloud.configuration;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.acl.SecurityChecker;
import com.cloud.alert.AlertManager;
import com.cloud.api.commands.CreateCfgCmd;
import com.cloud.api.commands.CreateDiskOfferingCmd;
import com.cloud.api.commands.CreateNetworkOfferingCmd;
import com.cloud.api.commands.CreatePodCmd;
import com.cloud.api.commands.CreateServiceOfferingCmd;
import com.cloud.api.commands.CreateVlanIpRangeCmd;
import com.cloud.api.commands.CreateZoneCmd;
import com.cloud.api.commands.DeleteDiskOfferingCmd;
import com.cloud.api.commands.DeleteNetworkOfferingCmd;
import com.cloud.api.commands.DeletePodCmd;
import com.cloud.api.commands.DeleteServiceOfferingCmd;
import com.cloud.api.commands.DeleteVlanIpRangeCmd;
import com.cloud.api.commands.DeleteZoneCmd;
import com.cloud.api.commands.ListNetworkOfferingsCmd;
import com.cloud.api.commands.UpdateCfgCmd;
import com.cloud.api.commands.UpdateDiskOfferingCmd;
import com.cloud.api.commands.UpdateNetworkOfferingCmd;
import com.cloud.api.commands.UpdatePodCmd;
import com.cloud.api.commands.UpdateServiceOfferingCmd;
import com.cloud.api.commands.UpdateZoneCmd;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterLinkLocalIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDao;
import com.cloud.dc.dao.DataCenterLinkLocalIpAddressDaoImpl;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.test.IPRangeConfig;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = { ConfigurationManager.class, ConfigurationService.class })
public class ConfigurationManagerImpl implements ConfigurationManager, ConfigurationService {
    public static final Logger s_logger = Logger.getLogger(ConfigurationManagerImpl.class.getName());

    String _name;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    HostPodDao _podDao;
    @Inject
    AccountVlanMapDao _accountVlanMapDao;
    @Inject
    PodVlanMapDao _podVlanMapDao;
    @Inject
    DataCenterDao _zoneDao;
    @Inject
    DomainRouterDao _domrDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    ServiceOfferingDao _serviceOfferingDao;
    @Inject
    DiskOfferingDao _diskOfferingDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    HostDetailsDao _hostDetailsDao;
    @Inject
    IPAddressDao _publicIpAddressDao;
    @Inject
    DataCenterIpAddressDao _privateIpAddressDao;
    @Inject
    VMInstanceDao _vmInstanceDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    EventDao _eventDao;
    @Inject
    UserDao _userDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    ConsoleProxyDao _consoleDao;
    @Inject
    SecondaryStorageVmDao _secStorageDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkManager _networkMgr;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    AlertManager _alertMgr;
    @Inject(adapter = SecurityChecker.class)
    Adapters<SecurityChecker> _secChecker;
    @Inject
    CapacityDao _capacityDao;
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    ProjectManager _projectMgr;
    @Inject
    NetworkOfferingServiceMapDao _ntwkOffServiceMapDao;

    // FIXME - why don't we have interface for DataCenterLinkLocalIpAddressDao?
    protected static final DataCenterLinkLocalIpAddressDaoImpl _LinkLocalIpAllocDao = ComponentLocator.inject(DataCenterLinkLocalIpAddressDaoImpl.class);

    private int _maxVolumeSizeInGb;
    private long _defaultPageSize;
    protected Set<String> configValuesForValidation;

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        String maxVolumeSizeInGbString = _configDao.getValue("storage.max.volume.size");
        _maxVolumeSizeInGb = NumbersUtil.parseInt(maxVolumeSizeInGbString, 2000);

        String defaultPageSizeString = _configDao.getValue("default.page.size");
        _defaultPageSize = NumbersUtil.parseLong(defaultPageSizeString, 500L);

        populateConfigValuesForValidationSet();
        return true;
    }

    private void populateConfigValuesForValidationSet() {
        configValuesForValidation = new HashSet<String>();
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
        configValuesForValidation.add("router.stats.interval");
        configValuesForValidation.add("snapshot.poll.interval");
        configValuesForValidation.add("stop.retry.interval");
        configValuesForValidation.add("storage.stats.interval");
        configValuesForValidation.add("storage.cleanup.interval");
        configValuesForValidation.add("wait");
        configValuesForValidation.add("xen.heartbeat.interval");
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {

        // TODO : this may not be a good place to do integrity check here, we put it here as we need _alertMgr to be properly
        // configured
        // before we can use it

        // As it is so common for people to forget about configuring management.network.cidr,
        String mgtCidr = _configDao.getValue(Config.ManagementNetwork.key());
        if (mgtCidr == null || mgtCidr.trim().isEmpty()) {
            String[] localCidrs = NetUtils.getLocalCidrs();
            if (localCidrs.length > 0) {
                s_logger.warn("Management network CIDR is not configured originally. Set it default to " + localCidrs[0]);

                _alertMgr.sendAlert(AlertManager.ALERT_TYPE_MANAGMENT_NODE, 0, new Long(0), "Management network CIDR is not configured originally. Set it default to " + localCidrs[0], "");
                _configDao.update(Config.ManagementNetwork.key(), localCidrs[0]);
            } else {
                s_logger.warn("Management network CIDR is not properly configured and we are not able to find a default setting");
                _alertMgr.sendAlert(AlertManager.ALERT_TYPE_MANAGMENT_NODE, 0, new Long(0), "Management network CIDR is not properly configured and we are not able to find a default setting", "");
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
    public void updateConfiguration(long userId, String name, String value) {
        if (value != null && (value.trim().isEmpty() || value.equals("null"))) {
            value = null;
        }

        String validationMsg = validateConfigurationValue(name, value);

        if (validationMsg != null) {
            s_logger.error("Invalid configuration option, name: " + name + ", value:" + value);
            throw new InvalidParameterValueException(validationMsg);
        }

        if (!_configDao.update(name, value)) {
            s_logger.error("Failed to update configuration option, name: " + name + ", value:" + value);
            throw new CloudRuntimeException("Failed to update configuration value. Please contact Cloud Support.");
        }
        if ( Config.XenGuestNetwork.key().equals(name) ) {
            String sql = "update host_details set value=? where name=?";
            Transaction txn = Transaction.currentTxn();
            PreparedStatement pstmt = null;
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "guest.network.device");

                pstmt.executeUpdate();
            } catch (SQLException e) {
            } catch (Throwable e) {
            }
        } else if ( Config.XenPrivateNetwork.key().equals(name) ) {
            String sql = "update host_details set value=? where name=?";
            Transaction txn = Transaction.currentTxn();
            PreparedStatement pstmt = null;
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "private.network.device");

                pstmt.executeUpdate();
            } catch (SQLException e) {
            } catch (Throwable e) {
            }
        } else if ( Config.XenPublicNetwork.key().equals(name) ) {
            String sql = "update host_details set value=? where name=?";
            Transaction txn = Transaction.currentTxn();
            PreparedStatement pstmt = null;
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "public.network.device");

                pstmt.executeUpdate();
            } catch (SQLException e) {
            } catch (Throwable e) {
            }
        } else if ( Config.XenStorageNetwork1.key().equals(name) ) {
            String sql = "update host_details set value=? where name=?";
            Transaction txn = Transaction.currentTxn();
            PreparedStatement pstmt = null;
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "storage.network.device1");

                pstmt.executeUpdate();
            } catch (SQLException e) {
            } catch (Throwable e) {
            }
        } else if ( Config.XenStorageNetwork2.key().equals(name) ) {
            String sql = "update host_details set value=? where name=?";
            Transaction txn = Transaction.currentTxn();
            PreparedStatement pstmt = null;
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setString(1, value);
                pstmt.setString(2, "storage.network.device2");

                pstmt.executeUpdate();
            } catch (SQLException e) {
            } catch (Throwable e) {
            }
        } else if (Config.SystemVMUseLocalStorage.key().equalsIgnoreCase(name)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Config 'system.vm.use.local.storage' changed to value:" + value + ", need to update System VM offerings");
            }
            boolean useLocalStorage = Boolean.parseBoolean(_configDao.getValue(Config.SystemVMUseLocalStorage.key()));
            ServiceOfferingVO serviceOffering = _serviceOfferingDao.findByName("Cloud.com-ConsoleProxy");
            if (serviceOffering != null) {
                serviceOffering.setUseLocalStorage(useLocalStorage);
                if (!_serviceOfferingDao.update(serviceOffering.getId(), serviceOffering)) {
                    s_logger.error("Failed to update ConsoleProxy offering's use_local_storage option to value:" + useLocalStorage);
                }
            }

            serviceOffering = _serviceOfferingDao.findByName("Cloud.Com-SoftwareRouter");
            if (serviceOffering != null) {
                serviceOffering.setUseLocalStorage(useLocalStorage);
                if (!_serviceOfferingDao.update(serviceOffering.getId(), serviceOffering)) {
                    s_logger.error("Failed to update SoftwareRouter offering's use_local_storage option to value:" + useLocalStorage);
                }
            }

            serviceOffering = _serviceOfferingDao.findByName("Cloud.com-SecondaryStorage");
            if (serviceOffering != null) {
                serviceOffering.setUseLocalStorage(useLocalStorage);
                if (!_serviceOfferingDao.update(serviceOffering.getId(), serviceOffering)) {
                    s_logger.error("Failed to update SecondaryStorage offering's use_local_storage option to value:" + useLocalStorage);
                }
            }
        }

    }

    @Override @ActionEvent(eventType = EventTypes.EVENT_CONFIGURATION_VALUE_EDIT, eventDescription = "updating configuration")
    public Configuration updateConfiguration(UpdateCfgCmd cmd) {
        Long userId = UserContext.current().getCallerUserId();
        String name = cmd.getCfgName();
        String value = cmd.getValue();
        UserContext.current().setEventDetails(" Name: "+name +" New Value: "+((value == null) ? "" : value));
        // check if config value exists
        if (_configDao.findByName(name) == null) {
            throw new InvalidParameterValueException("Config parameter with name " + name + " doesn't exist");
        }

        if (value == null) {
            return _configDao.findByName(name);
        }

        updateConfiguration(userId, name, value);
        if (_configDao.getValue(name).equalsIgnoreCase(value)) {
            return _configDao.findByName(name);
        } else {
            throw new CloudRuntimeException("Unable to update configuration parameter " + name);
        }
    }

    private String validateConfigurationValue(String name, String value) {
        if (value == null) {
            return null;
        }

        Config c = Config.getConfig(name);
        value = value.trim();

        if (c == null) {
            s_logger.error("Missing configuration variable " + name + " in configuration table");
            return "Invalid configuration variable.";
        }

        Class<?> type = c.getType();
        if (type.equals(Boolean.class)) {
            if (!(value.equals("true") || value.equals("false"))) {
                s_logger.error("Configuration variable " + name + " is expecting true or false in stead of " + value);
                return "Please enter either 'true' or 'false'.";
            }
            return null;
        }

        if (type.equals(Integer.class) && configValuesForValidation.contains(name)) {
            try {
                int val = Integer.parseInt(value);
                if (val <= 0) {
                    throw new InvalidParameterValueException("Please enter a positive value for the configuration parameter:" + name);
                }
            } catch (NumberFormatException e) {
                s_logger.error("There was an error trying to parse the integer value for:" + name);
                throw new InvalidParameterValueException("There was an error trying to parse the integer value for:" + name);
            }
        }

        String range = c.getRange();
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
                } catch (NullPointerException e) {
                    s_logger.error("Error parsing ip address for " + name);
                    throw new InvalidParameterValueException("Error parsing ip address");
                }
            } else if (range.equals("netmask")) {
                if (!NetUtils.isValidNetmask(value)) {
                    s_logger.error("netmask " + value + " is not a valid net mask for configuration variable " + name);
                    return "Please enter a valid netmask.";
                }
            } else if (range.equals("hypervisorList")) {
                String[] hypervisors = value.split(",");
                if (hypervisors == null) {
                    return "Please enter hypervisor list, seperated by comma";
                }
                for (String hypervisor : hypervisors) {
                    if (HypervisorType.getType(hypervisor) == HypervisorType.Any || HypervisorType.getType(hypervisor) == HypervisorType.None) {
                        return "Please enter valid hypervisor type";
                    }
                }
            }else {
                String[] options = range.split(",");
                for (String option : options) {
                    if (option.trim().equalsIgnoreCase(value)) {
                        return null;
                    }
                }
                s_logger.error("configuration value for " + name + " is invalid");
                return "Please enter : " + range;

            }
        } else if (type.equals(Integer.class)) {
            String[] options = range.split("-");
            if (options.length != 2) {
                String msg = "configuration range " + range + " for " + name + " is invalid";
                s_logger.error(msg);
                return msg;
            }
            int min = Integer.parseInt(options[0]);
            int max = Integer.parseInt(options[1]);
            int val = Integer.parseInt(value);
            if (val < min || val > max) {
                s_logger.error("configuration value for " + name + " is invalid");
                return "Please enter : " + range;
            }
        }
        return null;
    }

    private boolean podHasAllocatedPrivateIPs(long podId) {
        HostPodVO pod = _podDao.findById(podId);
        int count = _privateIpAddressDao.countIPs(podId, pod.getDataCenterId(), true);
        return (count > 0);
    }

    @DB
    protected void checkIfPodIsDeletable(long podId) {
        List<List<String>> tablesToCheck = new ArrayList<List<String>>();

        HostPodVO pod = _podDao.findById(podId);

        // Check if there are allocated private IP addresses in the pod
        if (_privateIpAddressDao.countIPs(podId, pod.getDataCenterId(), true) != 0) {
            throw new CloudRuntimeException("There are private IP addresses allocated for this pod");
        }

        List<String> volumes = new ArrayList<String>();
        volumes.add(0, "volumes");
        volumes.add(1, "pod_id");
        volumes.add(2, "there are storage volumes for this pod");
        tablesToCheck.add(volumes);

        List<String> host = new ArrayList<String>();
        host.add(0, "host");
        host.add(1, "pod_id");
        host.add(2, "there are servers running in this pod");
        tablesToCheck.add(host);

        List<String> vmInstance = new ArrayList<String>();
        vmInstance.add(0, "vm_instance");
        vmInstance.add(1, "pod_id");
        vmInstance.add(2, "there are virtual machines running in this pod");
        tablesToCheck.add(vmInstance);

        List<String> cluster = new ArrayList<String>();
        cluster.add(0, "cluster");
        cluster.add(1, "pod_id");
        cluster.add(2, "there are clusters in this pod");
        tablesToCheck.add(cluster);

        for (List<String> table : tablesToCheck) {
            String tableName = table.get(0);
            String column = table.get(1);
            String errorMsg = table.get(2);

            String dbName;
            if (tableName.equals("event") || tableName.equals("cloud_usage") || tableName.equals("usage_vm_instance") || tableName.equals("usage_ip_address") || tableName.equals("usage_network")
                    || tableName.equals("usage_job") || tableName.equals("account") || tableName.equals("user_statistics")) {
                dbName = "cloud_usage";
            } else {
                dbName = "cloud";
            }

            String selectSql = "SELECT * FROM `" + dbName + "`.`" + tableName + "` WHERE " + column + " = ?";

            if (tableName.equals("host") || tableName.equals("cluster") || tableName.equals("volumes") || tableName.equals("vm_instance")) {
                selectSql += " and removed IS NULL";
            }

            Transaction txn = Transaction.currentTxn();
            try {
                PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
                stmt.setLong(1, podId);
                ResultSet rs = stmt.executeQuery();
                if (rs != null && rs.next()) {
                    throw new CloudRuntimeException("The pod cannot be deleted because " + errorMsg);
                }
            } catch (SQLException ex) {
                throw new CloudRuntimeException("The Management Server failed to detect if pod is deletable. Please contact Cloud Support.");
            }
        }
    }

    private void checkPodAttributes(long podId, String podName, long zoneId, String gateway, String cidr, String startIp, String endIp, String allocationStateStr, boolean checkForDuplicates, boolean skipGatewayOverlapCheck) {
        if (checkForDuplicates) {
            // Check if the pod already exists
            if (validPod(podName, zoneId)) {
                throw new InvalidParameterValueException("A pod with name: " + podName + " already exists in zone " + zoneId + ". Please specify a different pod name. ");
            }
        }

        String cidrAddress;
        long cidrSize;
        // Get the individual cidrAddress and cidrSize values, if the CIDR is valid. If it's not valid, return an error.
        if (NetUtils.isValidCIDR(cidr)) {
            cidrAddress = getCidrAddress(cidr);
            cidrSize = getCidrSize(cidr);
        } else {
            throw new InvalidParameterValueException("Please enter a valid CIDR for pod: " + podName);
        }

        // Check if the IP range is valid
        if (startIp != null || endIp != null) {
            checkIpRange(startIp, endIp, cidrAddress, cidrSize);
        }

        // Check if the gateway is a valid IP address
        if (!NetUtils.isValidIp(gateway)) {
            throw new InvalidParameterValueException("The gateway is not a valid IP address.");
        }

        // Check if the gateway is in the CIDR subnet
        if (!NetUtils.getCidrSubNet(gateway, cidrSize).equalsIgnoreCase(NetUtils.getCidrSubNet(cidrAddress, cidrSize))) {
            throw new InvalidParameterValueException("The gateway is not in the CIDR subnet.");
        }

        // Don't allow gateway to overlap with start/endIp
        if(!skipGatewayOverlapCheck){
            if (NetUtils.ipRangesOverlap(startIp, endIp, gateway, gateway)) {
                throw new InvalidParameterValueException("The gateway shouldn't overlap start/end ip addresses");
            }
        }

        String checkPodCIDRs = _configDao.getValue("check.pod.cidrs");
        if (checkPodCIDRs == null || checkPodCIDRs.trim().isEmpty() || Boolean.parseBoolean(checkPodCIDRs)) {
            // Check if the CIDR conflicts with the Guest Network or other pods
            HashMap<Long, List<Object>> currentPodCidrSubnets = _podDao.getCurrentPodCidrSubnets(zoneId, podId);
            List<Object> newCidrPair = new ArrayList<Object>();
            newCidrPair.add(0, cidrAddress);
            newCidrPair.add(1, new Long(cidrSize));
            currentPodCidrSubnets.put(new Long(-1), newCidrPair);
            checkPodCidrSubnets(zoneId, currentPodCidrSubnets);

            // Prevent using the same CIDR for POD and virtual networking
            List<VlanVO> vlans = _vlanDao.listByZoneAndType(zoneId, VlanType.VirtualNetwork);
            for (VlanVO vlan : vlans) {
                String vlanCidr = NetUtils.ipAndNetMaskToCidr(vlan.getVlanGateway(), vlan.getVlanNetmask());
                String[] cidrPairVlan = vlanCidr.split("\\/");
                String[] vlanIpRange = NetUtils.getIpRangeFromCidr(cidrPairVlan[0], Long.valueOf(cidrPairVlan[1]));

                String[] cidrPairPod = cidr.split("\\/");
                String[] podIpRange = NetUtils.getIpRangeFromCidr(cidrPairPod[0], Long.valueOf(cidrPairPod[1]));

                if (NetUtils.ipRangesOverlap(vlanIpRange[0], vlanIpRange[1], podIpRange[0], podIpRange[1])) {
                    throw new InvalidParameterValueException("Pod's cidr conflicts with cidr of virtual network in zone id=" + zoneId);
                }
            }
        }

        Grouping.AllocationState allocationState = null;
        if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
            try {
                allocationState = Grouping.AllocationState.valueOf(allocationStateStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve Allocation State '" + allocationStateStr + "' to a supported state");
            }
        }
    }

    @Override
    @DB
    public boolean deletePod(DeletePodCmd cmd) {
        Long podId = cmd.getId();
        
        Transaction txn = Transaction.currentTxn();

        // Make sure the pod exists
        if (!validPod(podId)) {
            throw new InvalidParameterValueException("A pod with ID: " + podId + " does not exist.");
        }

        checkIfPodIsDeletable(podId);

        HostPodVO pod = _podDao.findById(podId);

        txn.start();
        
        // Delete private ip addresses for the pod if there are any
        List<DataCenterIpAddressVO> privateIps = _privateIpAddressDao.listByPodIdDcId(Long.valueOf(podId), pod.getDataCenterId());
        if (!privateIps.isEmpty()) {
            if (!(_privateIpAddressDao.deleteIpAddressByPod(podId))) {
                throw new CloudRuntimeException("Failed to cleanup private ip addresses for pod " + podId);
            }
            
            // Delete corresponding capacity record
            _capacityDao.removeBy(Capacity.CAPACITY_TYPE_PRIVATE_IP, null, podId, null);
        }
        
        // Delete link local ip addresses for the pod
        List<DataCenterLinkLocalIpAddressVO> localIps = _LinkLocalIpAllocDao.listByPodIdDcId(podId, pod.getDataCenterId());
        if (!localIps.isEmpty()) {
            if (!(_LinkLocalIpAllocDao.deleteIpAddressByPod(podId))) {
                throw new CloudRuntimeException("Failed to cleanup private ip addresses for pod " + podId);
            }
        }

        // Delete vlans associated with the pod
        List<? extends Vlan> vlans = _networkMgr.listPodVlans(podId);
        if (vlans != null && !vlans.isEmpty()) {
            for (Vlan vlan : vlans) {
                _vlanDao.remove(vlan.getId());
            }
        }

        // Delete the pod
        if (!(_podDao.remove(podId))) {
            throw new CloudRuntimeException("Failed to delete pod " + podId);
        }
        
        txn.commit();

        return true;
    }

    @Override
    public Pod editPod(UpdatePodCmd cmd) {
        return editPod(cmd.getId(), cmd.getPodName(), cmd.getStartIp(), cmd.getEndIp(), cmd.getGateway(), cmd.getNetmask(), cmd.getAllocationState());
    }

    @Override
    @DB
    public Pod editPod(long id, String name, String startIp, String endIp, String gateway, String netmask, String allocationStateStr) {

        // verify parameters
        HostPodVO pod = _podDao.findById(id);
        ;
        if (pod == null) {
            throw new InvalidParameterValueException("Unable to find pod by id " + id);
        }

        String[] existingPodIpRange = pod.getDescription().split("-");
        String[] leftRangeToAdd = null;
        String[] rightRangeToAdd = null;
        boolean allowToDownsize = false;

        // If the gateway, CIDR, private IP range is being changed, check if the pod has allocated private IP addresses
        if (podHasAllocatedPrivateIPs(id)) {

            if (netmask != null) {
                long newCidr = NetUtils.getCidrSize(netmask);
                long oldCidr = pod.getCidrSize();

                if (newCidr > oldCidr) {
                    throw new CloudRuntimeException("The specified pod has allocated private IP addresses, so its IP address range can be extended only");
                }
            }

            if (startIp != null && !startIp.equals(existingPodIpRange[0])) {
                if (NetUtils.ipRangesOverlap(startIp, null, existingPodIpRange[0], existingPodIpRange[1])) {
                    throw new CloudRuntimeException("The specified pod has allocated private IP addresses, so its IP address range can be extended only");
                } else {
                    leftRangeToAdd = new String[2];
                    long endIpForUpdate = NetUtils.ip2Long(existingPodIpRange[0]) - 1;
                    leftRangeToAdd[0] = startIp;
                    leftRangeToAdd[1] = NetUtils.long2Ip(endIpForUpdate);
                }
            }

            if (endIp != null && !endIp.equals(existingPodIpRange[1])) {
                if (NetUtils.ipRangesOverlap(endIp, endIp, existingPodIpRange[0], existingPodIpRange[1])) {
                    throw new CloudRuntimeException("The specified pod has allocated private IP addresses, so its IP address range can be extended only");
                } else {
                    rightRangeToAdd = new String[2];
                    long startIpForUpdate = NetUtils.ip2Long(existingPodIpRange[1]) + 1;
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

        String oldPodName = pod.getName();
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
        String cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);
        boolean checkForDuplicates = !oldPodName.equals(name);
        checkPodAttributes(id, name, pod.getDataCenterId(), gateway, cidr, startIp, endIp, allocationStateStr, checkForDuplicates, false);

        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            long zoneId = pod.getDataCenterId();

            if (!allowToDownsize) {
                if (leftRangeToAdd != null) {
                    _zoneDao.addPrivateIpAddress(zoneId, pod.getId(), leftRangeToAdd[0], leftRangeToAdd[1]);
                }

                if (rightRangeToAdd != null) {
                    _zoneDao.addPrivateIpAddress(zoneId, pod.getId(), rightRangeToAdd[0], rightRangeToAdd[1]);
                }

            } else {
                // delete the old range
                _zoneDao.deletePrivateIpAddressByPod(pod.getId());

                // add the new one
                if (startIp == null) {
                    startIp = existingPodIpRange[0];
                }

                if (endIp == null) {
                    endIp = existingPodIpRange[1];
                }

                _zoneDao.addPrivateIpAddress(zoneId, pod.getId(), startIp, endIp);
            }

            pod.setName(name);
            pod.setDataCenterId(zoneId);
            pod.setGateway(gateway);
            pod.setCidrAddress(getCidrAddress(cidr));
            pod.setCidrSize(getCidrSize(cidr));

            String ipRange = startIp + "-" + endIp;
            pod.setDescription(ipRange);
            Grouping.AllocationState allocationState = null;
            if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
                allocationState = Grouping.AllocationState.valueOf(allocationStateStr);
                pod.setAllocationState(allocationState);
            }

            _podDao.update(id, pod);

            txn.commit();
        } catch (Exception e) {
            s_logger.error("Unable to edit pod due to " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to edit pod. Please contact Cloud Support.");
        }

        return pod;
    }

    @Override
    public Pod createPod(CreatePodCmd cmd) {
        String endIp = cmd.getEndIp();
        String gateway = cmd.getGateway();
        String name = cmd.getPodName();
        String startIp = cmd.getStartIp();
        String netmask = cmd.getNetmask();
        Long zoneId = cmd.getZoneId();
        String cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);
        Long userId = UserContext.current().getCallerUserId();
        String allocationState = cmd.getAllocationState();

        if (allocationState == null) {
            allocationState = Grouping.AllocationState.Enabled.toString();
        }
        return createPod(userId.longValue(), name, zoneId, gateway, cidr, startIp, endIp, allocationState, false);
    }

    @Override
    @DB
    public HostPodVO createPod(long userId, String podName, long zoneId, String gateway, String cidr, String startIp, String endIp, String allocationStateStr, boolean skipGatewayOverlapCheck) {

        // Check if the zone is valid
        if (!validZone(zoneId)) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }

        // Check if zone is disabled
        DataCenterVO zone = _zoneDao.findById(zoneId);
        Account account = UserContext.current().getCaller();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(account.getType())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zoneId);
        }

        String cidrAddress = getCidrAddress(cidr);
        int cidrSize = getCidrSize(cidr);

        // endIp is an optional parameter; if not specified - default it to the end ip of the pod's cidr
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

        HostPodVO pod = new HostPodVO(podName, zoneId, gateway, cidrAddress, cidrSize, ipRange);

        Grouping.AllocationState allocationState = null;
        if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
            allocationState = Grouping.AllocationState.valueOf(allocationStateStr);
            pod.setAllocationState(allocationState);
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        pod = _podDao.persist(pod);

        if (startIp != null) {
            _zoneDao.addPrivateIpAddress(zoneId, pod.getId(), startIp, endIp);
        }

        String[] linkLocalIpRanges = getLinkLocalIPRange();
        if (linkLocalIpRanges != null) {
            _zoneDao.addLinkLocalIpAddress(zoneId, pod.getId(), linkLocalIpRanges[0], linkLocalIpRanges[1]);
        }

        txn.commit();

        return pod;
    }

    
    @DB
    protected void checkIfZoneIsDeletable(long zoneId) {
        List<List<String>> tablesToCheck = new ArrayList<List<String>>();

        List<String> host = new ArrayList<String>();
        host.add(0, "host");
        host.add(1, "data_center_id");
        host.add(2, "there are servers running in this zone");
        tablesToCheck.add(host);

        List<String> hostPodRef = new ArrayList<String>();
        hostPodRef.add(0, "host_pod_ref");
        hostPodRef.add(1, "data_center_id");
        hostPodRef.add(2, "there are pods in this zone");
        tablesToCheck.add(hostPodRef);

        List<String> privateIP = new ArrayList<String>();
        privateIP.add(0, "op_dc_ip_address_alloc");
        privateIP.add(1, "data_center_id");
        privateIP.add(2, "there are private IP addresses allocated for this zone");
        tablesToCheck.add(privateIP);

        List<String> publicIP = new ArrayList<String>();
        publicIP.add(0, "user_ip_address");
        publicIP.add(1, "data_center_id");
        publicIP.add(2, "there are public IP addresses allocated for this zone");
        tablesToCheck.add(publicIP);

        List<String> vmInstance = new ArrayList<String>();
        vmInstance.add(0, "vm_instance");
        vmInstance.add(1, "data_center_id");
        vmInstance.add(2, "there are virtual machines running in this zone");
        tablesToCheck.add(vmInstance);

        List<String> volumes = new ArrayList<String>();
        volumes.add(0, "volumes");
        volumes.add(1, "data_center_id");
        volumes.add(2, "there are storage volumes for this zone");
        tablesToCheck.add(volumes);

        List<String> physicalNetworks = new ArrayList<String>();
        physicalNetworks.add(0, "physical_network");
        physicalNetworks.add(1, "data_center_id");
        physicalNetworks.add(2, "there are physical networks in this zone");
        tablesToCheck.add(physicalNetworks);
        
        for (List<String> table : tablesToCheck) {
            String tableName = table.get(0);
            String column = table.get(1);
            String errorMsg = table.get(2);

            String dbName = "cloud";

            String selectSql = "SELECT * FROM `" + dbName + "`.`" + tableName + "` WHERE " + column + " = ?";

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
                selectSql += " AND state != '" + VirtualMachine.State.Expunging.toString() + "'";
            }

            Transaction txn = Transaction.currentTxn();
            try {
                PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
                stmt.setLong(1, zoneId);
                ResultSet rs = stmt.executeQuery();
                if (rs != null && rs.next()) {
                    throw new CloudRuntimeException("The zone is not deletable because " + errorMsg);
                }
            } catch (SQLException ex) {
                throw new CloudRuntimeException("The Management Server failed to detect if zone is deletable. Please contact Cloud Support.");
            }
        }

    }

    private void checkZoneParameters(String zoneName, String dns1, String dns2, String internalDns1, String internalDns2, boolean checkForDuplicates, Long domainId, String allocationStateStr) {
        if (checkForDuplicates) {
            // Check if a zone with the specified name already exists
            if (validZone(zoneName)) {
                throw new InvalidParameterValueException("A zone with that name already exists. Please specify a unique zone name.");
            }
        }

        // check if valid domain
        if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);

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

        if ((internalDns1 != null && internalDns1.length() > 0 && !NetUtils.isValidIp(internalDns1))) {
            throw new InvalidParameterValueException("Please enter a valid IP address for internal DNS1");
        }

        if (internalDns2 != null && internalDns2.length() > 0 && !NetUtils.isValidIp(internalDns2)) {
            throw new InvalidParameterValueException("Please enter a valid IP address for internal DNS2");
        }

        Grouping.AllocationState allocationState = null;
        if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
            try {
                allocationState = Grouping.AllocationState.valueOf(allocationStateStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve Allocation State '" + allocationStateStr + "' to a supported state");
            }
        }
    }

    private void checkIpRange(String startIp, String endIp, String cidrAddress, long cidrSize) {
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

    @Override
    @DB
    public boolean deleteZone(DeleteZoneCmd cmd) {

        Transaction txn = Transaction.currentTxn();
        boolean success = false;

        Long userId = UserContext.current().getCallerUserId();
        Long zoneId = cmd.getId();

        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

        // Make sure the zone exists
        if (!validZone(zoneId)) {
            throw new InvalidParameterValueException("A zone with ID: " + zoneId + " does not exist.");
        }

        checkIfZoneIsDeletable(zoneId);

        txn.start();

        // delete vlans for this zone
        List<VlanVO> vlans = _vlanDao.listByZone(zoneId);
        for (VlanVO vlan : vlans) {
            _vlanDao.remove(vlan.getId());
        }

        success = _zoneDao.remove(zoneId);
        
        if (success) {
            //delete all capacity records for the zone
            _capacityDao.removeBy(null, zoneId, null, null);
        }

        txn.commit();

        return success;

    }

    @Override @DB
    public DataCenter editZone(UpdateZoneCmd cmd) {
        // Parameter validation as from execute() method in V1
        Long zoneId = cmd.getId();
        String zoneName = cmd.getZoneName();
        String dns1 = cmd.getDns1();
        String dns2 = cmd.getDns2();
        String internalDns1 = cmd.getInternalDns1();
        String internalDns2 = cmd.getInternalDns2();
        String guestCidr = cmd.getGuestCidrAddress();
        List<String> dnsSearchOrder = cmd.getDnsSearchOrder();
        Boolean isPublic = cmd.isPublic();
        String allocationStateStr = cmd.getAllocationState();
        String dhcpProvider = cmd.getDhcpProvider();        
        Map<?, ?> detailsMap = cmd.getDetails();
        String networkDomain = cmd.getDomain();

        Map<String, String> newDetails = new HashMap<String, String>();
        if (detailsMap != null) {
            Collection<?> zoneDetailsCollection = detailsMap.values();
            Iterator<?> iter = zoneDetailsCollection.iterator();
            while (iter.hasNext()) {
                HashMap<?, ?> detail = (HashMap<?, ?>)iter.next();
                String key = (String)detail.get("key");
                String value = (String)detail.get("value");
                if ((key == null) || (value == null)) {
                    throw new InvalidParameterValueException("Invalid Zone Detail specified, fields 'key' and 'value' cannot be null, please specify details in the form:  details[0].key=XXX&details[0].value=YYY");
                } 
                //validate the zone detail keys are known keys
                /*if(!ZoneConfig.doesKeyExist(key)){
    					throw new InvalidParameterValueException("Invalid Zone Detail parameter: "+ key);
    				}*/
                newDetails.put(key, value);
            }
        }  
        
        // add the domain prefix list to details if not null
        if (dnsSearchOrder != null){
            for(String dom : dnsSearchOrder){
                if (!NetUtils.verifyDomainName(dom)) {
                    throw new InvalidParameterValueException(
                            "Invalid network domain suffixes. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                            + "and the hyphen ('-'); can't start or end with \"-\"");
                }
            }
            newDetails.put(ZoneConfig.DnsSearchOrder.getName(), StringUtils.join(dnsSearchOrder, ","));
        }

        DataCenterVO zone = _zoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("unable to find zone by id " + zoneId);
        }

        if (zoneName == null) {
            zoneName = zone.getName();
        }

        if ((guestCidr != null) && !NetUtils.validateGuestCidr(guestCidr)) {
            throw new InvalidParameterValueException("Please enter a valid guest cidr");
        }

        // Make sure the zone exists
        if (!validZone(zoneId)) {
            throw new InvalidParameterValueException("A zone with ID: " + zoneId + " does not exist.");
        }

        String oldZoneName = zone.getName();

        if (zoneName == null) {
            zoneName = oldZoneName;
        }

        if (dns1 == null) {
            dns1 = zone.getDns1();
        }

        if (dns2 == null) {
            dns2 = zone.getDns2();
        }

        if (internalDns1 == null) {
            internalDns1 = zone.getInternalDns1();
        }

        if (guestCidr == null) {
            guestCidr = zone.getGuestNetworkCidr();
        }
        
        //validate network domain
        if (networkDomain != null) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                        + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        } else {
            networkDomain = zone.getDomain();
        }

        boolean checkForDuplicates = !zoneName.equals(oldZoneName);
        checkZoneParameters(zoneName, dns1, dns2, internalDns1, internalDns2, checkForDuplicates, null, allocationStateStr);// not
        // allowing
        // updating
        // domain
        // associated
        // with
        // a
        // zone,
        // once
        // created

        zone.setName(zoneName);
        zone.setDns1(dns1);
        zone.setDns2(dns2);
        zone.setInternalDns1(internalDns1);
        zone.setInternalDns2(internalDns2);
        zone.setGuestNetworkCidr(guestCidr);
        zone.setDomain(networkDomain);

        // update a private zone to public; not vice versa
        if (isPublic != null && isPublic) {
            zone.setDomainId(null);
            zone.setDomain(null);
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        Map<String, String> updatedDetails = new HashMap<String, String>();
        _zoneDao.loadDetails(zone);
        if(zone.getDetails() != null){
            updatedDetails.putAll(zone.getDetails());
        }
        updatedDetails.putAll(newDetails);
        zone.setDetails(updatedDetails);        

        if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
            Grouping.AllocationState allocationState = Grouping.AllocationState.valueOf(allocationStateStr);
            zone.setAllocationState(allocationState);
        }

        if(dhcpProvider != null){
            zone.setDhcpProvider(dhcpProvider);
        }
        
        if (!_zoneDao.update(zoneId, zone)) {
            throw new CloudRuntimeException("Failed to edit zone. Please contact Cloud Support.");
        }

        txn.commit();
        return zone;
    }

    @Override
    @DB
    public DataCenterVO createZone(long userId, String zoneName, String dns1, String dns2, String internalDns1, String internalDns2, String guestCidr, String domain, Long domainId,
            NetworkType zoneType, String allocationStateStr, String networkDomain) {

        // checking the following params outside checkzoneparams method as we do not use these params for updatezone
        // hence the method below is generic to check for common params
        if ((guestCidr != null) && !NetUtils.validateGuestCidr(guestCidr)) {
            throw new InvalidParameterValueException("Please enter a valid guest cidr");
        }
        
        //Validate network domain 
        if (networkDomain != null) {
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException(
                        "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                        + "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }

        checkZoneParameters(zoneName, dns1, dns2, internalDns1, internalDns2, true, domainId, allocationStateStr);

        byte[] bytes = (zoneName + System.currentTimeMillis()).getBytes();
        String zoneToken = UUID.nameUUIDFromBytes(bytes).toString();
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            // Create the new zone in the database
            DataCenterVO zone = new DataCenterVO(zoneName, null, dns1, dns2, internalDns1, internalDns2, guestCidr, domain, domainId, zoneType, zoneToken, networkDomain);
            if (allocationStateStr != null && !allocationStateStr.isEmpty()) {
                Grouping.AllocationState allocationState = Grouping.AllocationState.valueOf(allocationStateStr);
                zone.setAllocationState(allocationState);
            }
            zone = _zoneDao.persist(zone);

            // Create deafult networks
            createDefaultNetworks(zone.getId());
            txn.commit();
            return zone;
        } catch (Exception ex) {
            txn.rollback();
            s_logger.warn("Exception: ", ex);
            throw new CloudRuntimeException("Fail to create a network");
        } finally {
            txn.close();
        }
    }

    @Override
    public void createDefaultNetworks(long zoneId) throws ConcurrentOperationException {
        DataCenterVO zone = _zoneDao.findById(zoneId);
        String networkDomain = null;
        // Create public, management, control and storage networks as a part of the zone creation
        if (zone != null) {
            List<NetworkOfferingVO> ntwkOff = _networkOfferingDao.listSystemNetworkOfferings();

            for (NetworkOfferingVO offering : ntwkOff) {
                DataCenterDeployment plan = new DataCenterDeployment(zone.getId(), null, null, null, null, null);
                NetworkVO userNetwork = new NetworkVO();

                Account systemAccount = _accountDao.findById(Account.ACCOUNT_ID_SYSTEM);

                BroadcastDomainType broadcastDomainType = null;
                boolean isNetworkDefault = false;
                if (offering.getTrafficType() == TrafficType.Management) {
                    broadcastDomainType = BroadcastDomainType.Native;
                } else if (offering.getTrafficType() == TrafficType.Control) {
                    broadcastDomainType = BroadcastDomainType.LinkLocal;
                } else if (offering.getTrafficType() == TrafficType.Public) {
                    if ((zone.getNetworkType() == NetworkType.Advanced && !zone.isSecurityGroupEnabled()) || zone.getNetworkType() == NetworkType.Basic) {
                        broadcastDomainType = BroadcastDomainType.Vlan;
                    } else {
                        continue;
                    }
                } else if (offering.getTrafficType() == TrafficType.Guest) {
                    if (zone.getNetworkType() == NetworkType.Basic) {
                        isNetworkDefault = true;
                        broadcastDomainType = BroadcastDomainType.Native;
                        userNetwork.setSecurityGroupEnabled(offering.isSecurityGroupEnabled());
                    } else {
                        continue;
                    }
                    
                    networkDomain = "cs" + Long.toHexString(Account.ACCOUNT_ID_SYSTEM) + _networkMgr.getGlobalGuestDomainSuffix();
                }
                userNetwork.setBroadcastDomainType(broadcastDomainType);
                userNetwork.setNetworkDomain(networkDomain);
                _networkMgr.setupNetwork(systemAccount, offering, userNetwork, plan, null, null, isNetworkDefault, false, null, null, true);
            }
        }
    }

    @Override
    public DataCenter createZone(CreateZoneCmd cmd) {
        // grab parameters from the command
        Long userId = UserContext.current().getCallerUserId();
        String zoneName = cmd.getZoneName();
        String dns1 = cmd.getDns1();
        String dns2 = cmd.getDns2();
        String internalDns1 = cmd.getInternalDns1();
        String internalDns2 = cmd.getInternalDns2();
        String guestCidr = cmd.getGuestCidrAddress();
        Long domainId = cmd.getDomainId();
        String type = cmd.getNetworkType();
        Boolean isBasic = false;
        String allocationState = cmd.getAllocationState();
        String networkDomain = cmd.getDomain();
        
        if (allocationState == null) {
            allocationState = Grouping.AllocationState.Enabled.toString();
        }

        if (!(type.equalsIgnoreCase(NetworkType.Basic.toString())) && !(type.equalsIgnoreCase(NetworkType.Advanced.toString()))) {
            throw new InvalidParameterValueException("Invalid zone type; only Advanced and Basic values are supported");
        } else if (type.equalsIgnoreCase(NetworkType.Basic.toString())) {
            isBasic = true;
        }

        NetworkType zoneType = isBasic ? NetworkType.Basic : NetworkType.Advanced;

        /*Guest cidr is required for Advanced zone creation; error out when the parameter specified for Basic zone
        if (zoneType == NetworkType.Advanced && guestCidr == null && !securityGroupEnabled) {
            throw new InvalidParameterValueException("guestCidrAddress parameter is required for Advanced zone creation");
        } else if (zoneType == NetworkType.Basic && guestCidr != null) {
            throw new InvalidParameterValueException("guestCidrAddress parameter is not supported for Basic zone");
        }*/

        DomainVO domainVO = null;

        if (userId == null) {
            userId = User.UID_SYSTEM;
        }

        if (domainId != null) {
            domainVO = _domainDao.findById(domainId);
        }

        /* Verify zone type
        if (zoneType == NetworkType.Basic && vnetRange != null) {
            vnetRange = null;
        }

        if (zoneType == NetworkType.Basic) {
            securityGroupEnabled = true;
        }*/

        return createZone(userId, zoneName, dns1, dns2, internalDns1, internalDns2, guestCidr, domainVO != null ? domainVO.getName() : null, domainId, zoneType, allocationState, networkDomain);
    }

    @Override
    public ServiceOffering createServiceOffering(CreateServiceOfferingCmd cmd) {
        Long userId = UserContext.current().getCallerUserId();
      
        String name = cmd.getServiceOfferingName();
        if ((name == null) || (name.length() == 0)) {
            throw new InvalidParameterValueException("Failed to create service offering: specify the name that has non-zero length");
        }

        String displayText = cmd.getDisplayText();
        if ((displayText == null) || (displayText.length() == 0)) {
            throw new InvalidParameterValueException("Failed to create service offering " + name + ": specify the display text that has non-zero length");
        }

        Long cpuNumber = cmd.getCpuNumber();
        if ((cpuNumber == null) || (cpuNumber.intValue() <= 0) || (cpuNumber.intValue() > 2147483647)) {
            throw new InvalidParameterValueException("Failed to create service offering " + name + ": specify the cpu number value between 1 and 2147483647");
        }

        Long cpuSpeed = cmd.getCpuSpeed();
        if ((cpuSpeed == null) || (cpuSpeed.intValue() <= 0) || (cpuSpeed.intValue() > 2147483647)) {
            throw new InvalidParameterValueException("Failed to create service offering " + name + ": specify the cpu speed value between 1 and 2147483647");
        }

        Long memory = cmd.getMemory();
        if ((memory == null) || (memory.intValue() < 32) || (memory.intValue() > 2147483647)) {
            throw new InvalidParameterValueException("Failed to create service offering " + name + ": specify the memory value between 32 and 2147483647 MB");
        }

        // check if valid domain
        if (cmd.getDomainId() != null && _domainDao.findById(cmd.getDomainId()) == null) {
            throw new InvalidParameterValueException("Please specify a valid domain id");
        }

        boolean localStorageRequired = false;
        String storageType = cmd.getStorageType();
        if (storageType != null) {
            if (storageType.equalsIgnoreCase(ServiceOffering.StorageType.local.toString())) {
                localStorageRequired = true;
            } else if (!storageType.equalsIgnoreCase(ServiceOffering.StorageType.shared.toString())) {
                throw new InvalidParameterValueException("Invalid storage type " + storageType + " specified, valid types are: 'local' and 'shared'");
            }
        }

        Boolean offerHA = cmd.getOfferHa();
        if (offerHA == null) {
            offerHA = false;
        }

        Boolean limitCpuUse = cmd.GetLimitCpuUse();
        if (limitCpuUse == null) {
            limitCpuUse = false;
        }
        
        String vmTypeString = cmd.getSystemVmType();
        VirtualMachine.Type vmType = null;
        boolean allowNetworkRate = false;
        if (cmd.getIsSystem()) {
            if (vmTypeString == null || VirtualMachine.Type.DomainRouter.toString().toLowerCase().equals(vmTypeString)){
                vmType = VirtualMachine.Type.DomainRouter;
                allowNetworkRate = true;
            } else if (VirtualMachine.Type.ConsoleProxy.toString().toLowerCase().equals(vmTypeString)){
                vmType = VirtualMachine.Type.ConsoleProxy;
            } else if (VirtualMachine.Type.SecondaryStorageVm.toString().toLowerCase().equals(vmTypeString)){
                vmType = VirtualMachine.Type.SecondaryStorageVm;
            } else {
                throw new InvalidParameterValueException("Invalid systemVmType. Supported types are: " + VirtualMachine.Type.DomainRouter + ", " + VirtualMachine.Type.ConsoleProxy + ", " + VirtualMachine.Type.SecondaryStorageVm);
            }
        } else {
            allowNetworkRate = true;;
        }
        
        if (cmd.getNetworkRate() != null && !allowNetworkRate) {
            throw new InvalidParameterValueException("Network rate can be specified only for non-System offering and system offerings having \"domainrouter\" systemvmtype");
        }

        return createServiceOffering(userId, cmd.getIsSystem(), vmType, cmd.getServiceOfferingName(), cpuNumber.intValue(), memory.intValue(), cpuSpeed.intValue(), cmd.getDisplayText(), localStorageRequired, offerHA,
                limitCpuUse, cmd.getTags(), cmd.getDomainId(), cmd.getHostTag(), cmd.getNetworkRate());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_OFFERING_CREATE, eventDescription = "creating service offering")
    public ServiceOfferingVO createServiceOffering(long userId, boolean isSystem, VirtualMachine.Type vm_type, String name, int cpu, int ramSize, int speed, String displayText, boolean localStorageRequired, 
            boolean offerHA, boolean limitResourceUse, String tags,  Long domainId, String hostTag, Integer networkRate) {
        tags = cleanupTags(tags);
        ServiceOfferingVO offering = new ServiceOfferingVO(name, cpu, ramSize, speed, networkRate, null, offerHA, limitResourceUse, displayText, localStorageRequired, false, tags, isSystem, vm_type, domainId, hostTag);

        if ((offering = _serviceOfferingDao.persist(offering)) != null) {
            UserContext.current().setEventDetails("Service offering id=" + offering.getId());
            return offering;
        } else {
            return null;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_OFFERING_EDIT, eventDescription = "updating service offering")
    public ServiceOffering updateServiceOffering(UpdateServiceOfferingCmd cmd) {
        String displayText = cmd.getDisplayText();
        Long id = cmd.getId();
        String name = cmd.getServiceOfferingName();
        Long userId = UserContext.current().getCallerUserId();

        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

        // Verify input parameters
        ServiceOffering offeringHandle = getServiceOffering(id);

        if (offeringHandle == null) {
            throw new InvalidParameterValueException("unable to find service offering " + id);
        }

        boolean updateNeeded = (name != null || displayText != null);
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

        // Note: tag editing commented out for now; keeping the code intact, might need to re-enable in next releases
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
            UserContext.current().setEventDetails("Service offering id=" + offering.getId());
            return offering;
        } else {
            return null;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DISK_OFFERING_CREATE, eventDescription = "creating disk offering")
    public DiskOfferingVO createDiskOffering(Long domainId, String name, String description, Long numGibibytes, String tags, boolean isCustomized) {
        long diskSize = 0;// special case for custom disk offerings
        if (numGibibytes != null && (numGibibytes <= 0)) {
            throw new InvalidParameterValueException("Please specify a disk size of at least 1 Gb.");
        } else if (numGibibytes != null && (numGibibytes > _maxVolumeSizeInGb)) {
            throw new InvalidParameterValueException("The maximum size for a disk is " + _maxVolumeSizeInGb + " Gb.");
        }

        if (numGibibytes != null) {
            diskSize = numGibibytes * 1024 * 1024 * 1024;
        }

        if (diskSize == 0) {
            isCustomized = true;
        }

        tags = cleanupTags(tags);
        DiskOfferingVO newDiskOffering = new DiskOfferingVO(domainId, name, description, diskSize, tags, isCustomized);
        UserContext.current().setEventDetails("Disk offering id=" + newDiskOffering.getId());
        DiskOfferingVO offering = _diskOfferingDao.persist(newDiskOffering);
        if (offering != null) {
            UserContext.current().setEventDetails("Disk offering id=" + newDiskOffering.getId());
            return offering;
        } else {
            return null;
        }
    }

    @Override
    public DiskOffering createDiskOffering(CreateDiskOfferingCmd cmd) {
        String name = cmd.getOfferingName();
        String description = cmd.getDisplayText();
        Long numGibibytes = cmd.getDiskSize();
        boolean isCustomized = cmd.isCustomized() != null ? cmd.isCustomized() : false; // false by default
        String tags = cmd.getTags();
        // Long domainId = cmd.getDomainId() != null ? cmd.getDomainId() : Long.valueOf(DomainVO.ROOT_DOMAIN); // disk offering
        // always gets created under the root domain.Bug # 6055 if not passed in cmd
        Long domainId = cmd.getDomainId();

        if (!isCustomized && numGibibytes == null) {
            throw new InvalidParameterValueException("Disksize is required for non-customized disk offering");
        }

        return createDiskOffering(domainId, name, description, numGibibytes, tags, isCustomized);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DISK_OFFERING_EDIT, eventDescription = "updating disk offering")
    public DiskOffering updateDiskOffering(UpdateDiskOfferingCmd cmd) {
        Long diskOfferingId = cmd.getId();
        String name = cmd.getDiskOfferingName();
        String displayText = cmd.getDisplayText();

        // Check if diskOffering exists
        DiskOffering diskOfferingHandle = getDiskOffering(diskOfferingId);

        if (diskOfferingHandle == null) {
            throw new InvalidParameterValueException("Unable to find disk offering by id " + diskOfferingId);
        }

        boolean updateNeeded = (name != null || displayText != null);
        if (!updateNeeded) {
            return _diskOfferingDao.findById(diskOfferingId);
        }

        DiskOfferingVO diskOffering = _diskOfferingDao.createForUpdate(diskOfferingId);

        if (name != null) {
            diskOffering.setName(name);
        }

        if (displayText != null) {
            diskOffering.setDisplayText(displayText);
        }

        // Note: tag editing commented out for now;keeping the code intact, might need to re-enable in next releases
        // if (tags != null)
        // {
        // if (tags.trim().isEmpty() && diskOfferingHandle.getTags() == null)
        // {
        // //no new tags; no existing tags
        // diskOffering.setTagsArray(csvTagsToList(null));
        // }
        // else if (!tags.trim().isEmpty() && diskOfferingHandle.getTags() != null)
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
            UserContext.current().setEventDetails("Disk offering id=" + diskOffering.getId());
            return _diskOfferingDao.findById(diskOfferingId);
        } else {
            return null;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DISK_OFFERING_DELETE, eventDescription = "deleting disk offering")
    public boolean deleteDiskOffering(DeleteDiskOfferingCmd cmd) {
        Long diskOfferingId = cmd.getId();

        DiskOffering offering = getDiskOffering(diskOfferingId);

        if (offering == null) {
            throw new InvalidParameterValueException("Unable to find disk offering by id " + diskOfferingId);
        }

        if (_diskOfferingDao.remove(diskOfferingId)) {
            UserContext.current().setEventDetails("Disk offering id=" + diskOfferingId);
            return true;
        } else {
            return false;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_OFFERING_DELETE, eventDescription = "deleting service offering")
    public boolean deleteServiceOffering(DeleteServiceOfferingCmd cmd) {

        Long offeringId = cmd.getId();
        Long userId = UserContext.current().getCallerUserId();

        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

        // Verify service offering id
        ServiceOffering offering = getServiceOffering(offeringId);
        if (offering == null) {
            throw new InvalidParameterValueException("unable to find service offering " + offeringId);
        } 

        if(offering.getDefaultUse()){
            throw new InvalidParameterValueException("Default service offerings cannot be deleted");
        }

        if (_serviceOfferingDao.remove(offeringId)) {
            UserContext.current().setEventDetails("Service offering id=" + offeringId);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String changePrivateIPRange(boolean add, long podId, String startIP, String endIP) {
        checkPrivateIpRangeErrors(podId, startIP, endIP);

        long zoneId = _podDao.findById(podId).getDataCenterId();
        List<String> problemIPs = null;
        if (add) {
            problemIPs = savePrivateIPRange(startIP, endIP, podId, zoneId);
        } else {
            problemIPs = deletePrivateIPRange(startIP, endIP, podId, zoneId);
        }

        if (problemIPs == null) {
            throw new InvalidParameterValueException("Failed to change private IP range. Please contact Cloud Support.");
        } else {
            return genChangeRangeSuccessString(problemIPs, add);
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VLAN_IP_RANGE_CREATE, eventDescription = "creating vlan ip range", async = false)
    public Vlan createVlanAndPublicIpRange(CreateVlanIpRangeCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        Long zoneId = cmd.getZoneId();
        Long podId = cmd.getPodId();
        String startIP = cmd.getStartIp();
        String endIP = cmd.getEndIp();
        String vlanGateway = cmd.getGateway();
        String vlanNetmask = cmd.getNetmask();
        Long userId = UserContext.current().getCallerUserId();
        String vlanId = cmd.getVlan();
        Boolean forVirtualNetwork = cmd.isForVirtualNetwork();
        Long networkId = cmd.getNetworkID();
        String networkVlanId = null;
        
        //projectId and accountName can't be specified together
        String accountName = cmd.getAccountName();
        Long projectId = cmd.getProjectId();
        Long domainId = cmd.getDomainId();
        Account account = null;
        
        if (projectId != null) {
            if (accountName != null) {
                throw new InvalidParameterValueException("Account and projectId are mutually exclusive");
            }
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }
            
            account = _accountMgr.getAccount(project.getProjectAccountId());
        }

        if ((accountName != null) && (domainId != null)) {
            account = _accountDao.findActiveAccount(accountName, domainId);
            if (account == null) {
                throw new InvalidParameterValueException("Please specify a valid account.");
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
            }
        }

        // Verify that zone exists
        DataCenterVO zone = _zoneDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id " + zoneId);
        }

        // Check if zone is disabled
        Account caller = UserContext.current().getCaller();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getType())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zoneId);
        }

        if (zone.isSecurityGroupEnabled() && zone.getNetworkType() != DataCenter.NetworkType.Basic && forVirtualNetwork) {
            throw new InvalidParameterValueException("Can't add virtual network into a zone with security group enabled");
        }

        // If networkId is not specified, and vlan is Virtual or Direct Untagged, try to locate default networks
        if (forVirtualNetwork) {
            if (network == null) {
                // find default public network in the zone
                networkId = _networkMgr.getSystemNetworkByZoneAndTrafficType(zoneId, TrafficType.Public).getId();
            } else if (network.getType() != null || network.getTrafficType() != TrafficType.Public) {
                throw new InvalidParameterValueException("Can't find Public network by id=" + networkId);
            }
        } else {
            if (network == null) {
                if (zone.getNetworkType() == DataCenter.NetworkType.Basic) {
                    networkId = _networkMgr.getSystemNetworkByZoneAndTrafficType(zoneId, TrafficType.Guest).getId();
                } else {
                    network = _networkMgr.getNetworkWithSecurityGroupEnabled(zoneId);
                    if (network == null) {
                        throw new InvalidParameterValueException("Nework id is required for Direct vlan creation ");
                    }
                    networkId = network.getId();
                }
            } else if (network.getType() == null || network.getType()== Network.Type.Isolated) {
                throw new InvalidParameterValueException("Can't create direct vlan for network id=" + networkId + " with type: " + network.getType());
            }
        }

        // if end ip is not specified, default it to startIp
        if (endIP == null && startIP != null) {
            endIP = startIP;
        }

        // if vlan is specified, throw an error if it's not equal to network's vlanId
        if (network != null) {
            URI uri = network.getBroadcastUri();
            if (uri != null) {
                String[] vlan = uri.toString().split("vlan:\\/\\/");
                networkVlanId = vlan[1];
            }
        }

        if (vlanId != null && networkVlanId != null && !networkVlanId.equalsIgnoreCase(vlanId)) {
            throw new InvalidParameterValueException("Vlan doesn't match vlan of the network");
        }

        if (forVirtualNetwork || zone.getNetworkType() == DataCenter.NetworkType.Basic || network.isSecurityGroupEnabled()) {
            if (vlanGateway == null || vlanNetmask == null || zoneId == null) {
                throw new InvalidParameterValueException("Gateway, netmask and zoneId have to be passed in for virtual and direct untagged networks");
            }
        } else {
            // check if startIp and endIp belong to network Cidr
            String networkCidr = network.getCidr();
            String networkGateway = network.getGateway();
            Long networkZoneId = network.getDataCenterId();
            String networkNetmask = NetUtils.getCidrNetmask(networkCidr);

            // Check if ip addresses are in network range
            if (!NetUtils.sameSubnet(startIP, networkGateway, networkNetmask)) {
                throw new InvalidParameterValueException("Start ip is not in network cidr: " + networkCidr);
            }

            if (endIP != null) {
                if (!NetUtils.sameSubnet(endIP, networkGateway, networkNetmask)) {
                    throw new InvalidParameterValueException("End ip is not in network cidr: " + networkCidr);
                }
            }

            // set gateway, netmask, zone from network object
            vlanGateway = networkGateway;
            vlanNetmask = networkNetmask;
            zoneId = networkZoneId;

            // set vlanId if it's not null for the network
            if (networkVlanId != null) {
                vlanId = networkVlanId;
            }
        }

        // if it's an account specific range, associate ip address list to the account
        boolean associateIpRangeToAccount = false;

        if (forVirtualNetwork) {
            if (account != null) {
                // verify resource limits
                long ipResourceLimit = _resourceLimitMgr.findCorrectResourceLimitForAccount(account, ResourceType.public_ip);
                long accountIpRange = NetUtils.ip2Long(endIP) - NetUtils.ip2Long(startIP) + 1;
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(" IPResourceLimit " + ipResourceLimit + " accountIpRange " + accountIpRange);
                }
                if (ipResourceLimit != -1 && accountIpRange > ipResourceLimit) { // -1 means infinite
                    throw new InvalidParameterValueException(" Public IP Resource Limit is set to " + ipResourceLimit + " which is less than the IP range of " + accountIpRange + " provided");
                }
                associateIpRangeToAccount = true;
            }
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        Vlan vlan = createVlanAndPublicIpRange(userId, zoneId, podId, startIP, endIP, vlanGateway, vlanNetmask, forVirtualNetwork, vlanId, account, networkId);

        if (associateIpRangeToAccount) {
            _networkMgr.associateIpAddressListToAccount(userId, account.getId(), zoneId, vlan.getId(), network);
            if (network == null) {
                List<? extends Network> networks = _networkMgr.getIsolatedNetworksOwnedByAccountInZone(zoneId, account);
                network = networks.get(0);
            }
            if (network == null) {
                throw new CloudRuntimeException("Failed to associate vlan to the account id=" + account.getId() + ", default network failed to create");
            }
        }
        txn.commit();

        // Associate ips to the network
        if (associateIpRangeToAccount) {
            if (network.getState() == Network.State.Implemented) {
                s_logger.debug("Applying ip associations for vlan id=" + vlanId + " in network " + network);
                if (!_networkMgr.applyIpAssociations(network, false)) {
                    s_logger.warn("Failed to apply ip associations for vlan id=1 as a part of add vlan range for account id=" + account.getId());
                }
            } else {
                s_logger.trace("Network id=" + network.getId() + " is not Implemented, no need to apply ipAssociations");
            }
        }

        return vlan;
    }

    @Override
    @DB
    public Vlan createVlanAndPublicIpRange(Long userId, Long zoneId, Long podId, String startIP, String endIP, String vlanGateway, String vlanNetmask, boolean forVirtualNetwork, String vlanId,
            Account account, Long networkId) {
        // Check that the pod ID is valid
        if (podId != null && ((_podDao.findById(podId)) == null)) {
            throw new InvalidParameterValueException("Please specify a valid pod.");
        }

        if (podId != null && _podDao.findById(podId).getDataCenterId() != zoneId) {
            throw new InvalidParameterValueException("Pod id=" + podId + " doesn't belong to zone id=" + zoneId);
        }
        // If the VLAN id is null, default it to untagged
        if (vlanId == null) {
            vlanId = Vlan.UNTAGGED;
        }

        DataCenterVO zone;
        if (zoneId == null || ((zone = _zoneDao.findById(zoneId)) == null)) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }

        // Allow adding untagged direct vlan only for Basic zone
        if (zone.getNetworkType() == NetworkType.Advanced && vlanId.equals(Vlan.UNTAGGED) && (!forVirtualNetwork || zone.isSecurityGroupEnabled())) {
            throw new InvalidParameterValueException("Direct untagged network is not supported for the zone " + zone.getId() + " of type " + zone.getNetworkType());
        } else if (zone.getNetworkType() == NetworkType.Basic && !((vlanId.equals(Vlan.UNTAGGED) && !forVirtualNetwork) || (forVirtualNetwork))) {
            throw new InvalidParameterValueException("Only Direct Untagged and Virtual networks are supported in the zone " + zone.getId() + " of type " + zone.getNetworkType());
        }

        //TODO
        /*  don't allow to create a virtual vlan when zone's vnet is NULL in Advanced zone
        if ((zone.getNetworkType() == NetworkType.Advanced && zone.getVnet() == null) && forVirtualNetwork) {
            throw new InvalidParameterValueException("Can't add virtual network to the zone id=" + zone.getId() + " as zone doesn't have guest vlan configured");
        }*/

        VlanType vlanType = forVirtualNetwork ? VlanType.VirtualNetwork : VlanType.DirectAttached;

        // ACL check
        checkAccess(account, zone);

        if (vlanType.equals(VlanType.DirectAttached)) {
            if (account != null) {
                // VLANs for an account must be tagged
                if (vlanId.equals(Vlan.UNTAGGED)) {
                    throw new InvalidParameterValueException("Direct Attached IP ranges for an account must be tagged.");
                }

                // Make sure there aren't any pod VLANs in this zone
                List<HostPodVO> podsInZone = _podDao.listByDataCenterId(zone.getId());
                for (HostPodVO pod : podsInZone) {
                    if (_podVlanMapDao.listPodVlanMapsByPod(pod.getId()).size() > 0) {
                        throw new InvalidParameterValueException("Zone " + zone.getName()
                                + " already has pod-wide IP ranges. A zone may contain either pod-wide IP ranges or account-wide IP ranges, but not both.");
                    }
                }
            } else if (podId != null) {
                // Pod-wide VLANs must be untagged
                if (!vlanId.equals(Vlan.UNTAGGED)) {
                    throw new InvalidParameterValueException("Direct Attached IP ranges for a pod must be untagged.");
                }

                // Make sure there aren't any account VLANs in this zone
                List<AccountVlanMapVO> accountVlanMaps = _accountVlanMapDao.listAllIncludingRemoved();
                for (AccountVlanMapVO accountVlanMap : accountVlanMaps) {
                    VlanVO vlan = _vlanDao.findById(accountVlanMap.getVlanDbId());
                    if (vlan.getDataCenterId() == zone.getId()) {
                        throw new InvalidParameterValueException("Zone " + zone.getName()
                                + " already has account-wide IP ranges. A zone may contain either pod-wide IP ranges or account-wide IP ranges, but not both.");
                    }
                }
            }
        }

        // Make sure the gateway is valid
        if (!NetUtils.isValidIp(vlanGateway)) {
            throw new InvalidParameterValueException("Please specify a valid gateway");
        }

        // Make sure the netmask is valid
        if (!NetUtils.isValidIp(vlanNetmask)) {
            throw new InvalidParameterValueException("Please specify a valid netmask");
        }

        String newVlanSubnet = NetUtils.getSubNet(vlanGateway, vlanNetmask);

        // Check if the new VLAN's subnet conflicts with the guest network in the specified zone (guestCidr is null for basic
        // zone)
        String guestNetworkCidr = zone.getGuestNetworkCidr();
        if (guestNetworkCidr != null) {
            String[] cidrPair = guestNetworkCidr.split("\\/");
            String guestIpNetwork = NetUtils.getIpRangeStartIpFromCidr(cidrPair[0], Long.parseLong(cidrPair[1]));
            long guestCidrSize = Long.parseLong(cidrPair[1]);
            long vlanCidrSize = NetUtils.getCidrSize(vlanNetmask);

            long cidrSizeToUse = -1;
            if (vlanCidrSize < guestCidrSize) {
                cidrSizeToUse = vlanCidrSize;
            } else {
                cidrSizeToUse = guestCidrSize;
            }

            String guestSubnet = NetUtils.getCidrSubNet(guestIpNetwork, cidrSizeToUse);

            if (newVlanSubnet.equals(guestSubnet)) {
                throw new InvalidParameterValueException("The new IP range you have specified has the same subnet as the guest network in zone: " + zone.getName()
                        + ". Please specify a different gateway/netmask.");
            }
        }

        // Check if there are any errors with the IP range
        checkPublicIpRangeErrors(zoneId, vlanId, vlanGateway, vlanNetmask, startIP, endIP);

        // Throw an exception if any of the following is true:
        // 1. Another VLAN in the same zone has a different tag but the same subnet as the new VLAN. Make an exception for the
        // case when both vlans are Direct.
        // 2. Another VLAN in the same zone that has the same tag and subnet as the new VLAN has IPs that overlap with the IPs
        // being added
        // 3. Another VLAN in the same zone that has the same tag and subnet as the new VLAN has a different gateway than the
        // new VLAN
        // 4. If VLAN is untagged and Virtual, and there is existing UNTAGGED vlan with different subnet 
        List<VlanVO> vlans = _vlanDao.listByZone(zone.getId());
        for (VlanVO vlan : vlans) {
            String otherVlanGateway = vlan.getVlanGateway();
            String otherVlanSubnet = NetUtils.getSubNet(vlan.getVlanGateway(), vlan.getVlanNetmask());
            String[] otherVlanIpRange = vlan.getIpRange().split("\\-");
            String otherVlanStartIP = otherVlanIpRange[0];
            String otherVlanEndIP = null;
            if (otherVlanIpRange.length > 1) {
                otherVlanEndIP = otherVlanIpRange[1];
            }

            if (forVirtualNetwork && !vlanId.equals(vlan.getVlanTag()) && newVlanSubnet.equals(otherVlanSubnet) && !allowIpRangeOverlap(vlan, forVirtualNetwork, networkId)) {
                throw new InvalidParameterValueException("The IP range with tag: " + vlan.getVlanTag() + " in zone " + zone.getName()
                        + " has the same subnet. Please specify a different gateway/netmask.");
            }

            boolean vlansUntaggedAndVirtual = (vlanId.equals(Vlan.UNTAGGED) && vlanId.equals(vlan.getVlanTag()) && forVirtualNetwork && vlan.getVlanType() == VlanType.VirtualNetwork);

            if (vlansUntaggedAndVirtual && !newVlanSubnet.equals(otherVlanSubnet)) {
                throw new InvalidParameterValueException("The Untagged ip range with different subnet already exists in zone " + zone.getId());
            }

            if (vlanId.equals(vlan.getVlanTag()) && newVlanSubnet.equals(otherVlanSubnet)) {
                if (NetUtils.ipRangesOverlap(startIP, endIP, otherVlanStartIP, otherVlanEndIP)) {
                    throw new InvalidParameterValueException("The IP range with tag: " + vlan.getVlanTag()
                            + " already has IPs that overlap with the new range. Please specify a different start IP/end IP.");
                }

                if (!vlanGateway.equals(otherVlanGateway)) {
                    throw new InvalidParameterValueException("The IP range with tag: " + vlan.getVlanTag() + " has already been added with gateway " + otherVlanGateway
                            + ". Please specify a different tag.");
                }
            }
        }

        // Check if a guest VLAN is using the same tag
        if (_zoneDao.findVnet(zoneId, vlanId).size() > 0) {
            throw new InvalidParameterValueException("The VLAN tag " + vlanId + " is already being used for the guest network in zone " + zone.getName());
        }

        // For untagged vlan check if vlan per pod already exists. If yes, verify that new vlan range has the same netmask and
        // gateway
        if (zone.getNetworkType() == NetworkType.Basic && vlanId.equalsIgnoreCase(Vlan.UNTAGGED) && podId != null) {
            List<VlanVO> podVlans = _vlanDao.listVlansForPodByType(podId, VlanType.DirectAttached);
            if (podVlans != null && !podVlans.isEmpty()) {
                VlanVO podVlan = podVlans.get(0);
                if (!podVlan.getVlanNetmask().equals(vlanNetmask)) {
                    throw new InvalidParameterValueException("Vlan netmask is different from the netmask of Untagged vlan id=" + podVlan.getId() + " existing in the pod " + podId);
                } else if (!podVlan.getVlanGateway().equals(vlanGateway)) {
                    throw new InvalidParameterValueException("Vlan gateway is different from the gateway of Untagged vlan id=" + podVlan.getId() + " existing in the pod " + podId);
                }
            }
        }

        String ipRange = startIP;
        if (endIP != null) {
            ipRange += "-" + endIP;
        }

        // Everything was fine, so persist the VLAN
        Transaction txn = Transaction.currentTxn();
        txn.start();

        VlanVO vlan = new VlanVO(vlanType, vlanId, vlanGateway, vlanNetmask, zone.getId(), ipRange, networkId);
        vlan = _vlanDao.persist(vlan);

        if (!savePublicIPRange(startIP, endIP, zoneId, vlan.getId(), networkId)) {
            throw new CloudRuntimeException("Failed to save IP range. Please contact Cloud Support."); // It can be Direct IP or
            // Public IP.
        }

        if (account != null) {
            // This VLAN is account-specific, so create an AccountVlanMapVO entry
            AccountVlanMapVO accountVlanMapVO = new AccountVlanMapVO(account.getId(), vlan.getId());
            _accountVlanMapDao.persist(accountVlanMapVO);
        } else if (podId != null) {
            // This VLAN is pod-wide, so create a PodVlanMapVO entry
            PodVlanMapVO podVlanMapVO = new PodVlanMapVO(podId, vlan.getId());
            _podVlanMapDao.persist(podVlanMapVO);
        }

        txn.commit();

        return vlan;
    }

    @Override
    public boolean deleteVlanAndPublicIpRange(long userId, long vlanDbId) {
        VlanVO vlan = _vlanDao.findById(vlanDbId);
        if (vlan == null) {
            throw new InvalidParameterValueException("Please specify a valid IP range id.");
        }

        // Check if the VLAN has any allocated public IPs
        if (_publicIpAddressDao.countIPs(vlan.getDataCenterId(), vlanDbId, true) > 0) {
            throw new InvalidParameterValueException("The IP range can't be deleted because it has allocated public IP addresses.");
        }

        // Delete all public IPs in the VLAN
        if (!deletePublicIPRange(vlanDbId)) {
            return false;
        }

        // Delete the VLAN
        return _vlanDao.expunge(vlanDbId);
    }

    @Override
    public List<String> csvTagsToList(String tags) {
        List<String> tagsList = new ArrayList<String>();

        if (tags != null) {
            String[] tokens = tags.split(",");
            for (int i = 0; i < tokens.length; i++) {
                tagsList.add(tokens[i].trim());
            }
        }

        return tagsList;
    }

    @Override
    public String listToCsvTags(List<String> tagsList) {
        String tags = "";
        if (tagsList.size() > 0) {
            for (int i = 0; i < tagsList.size(); i++) {
                tags += tagsList.get(i);
                if (i != tagsList.size() - 1) {
                    tags += ",";
                }
            }
        }

        return tags;
    }

    private String cleanupTags(String tags) {
        if (tags != null) {
            String[] tokens = tags.split(",");
            StringBuilder t = new StringBuilder();
            for (int i = 0; i < tokens.length; i++) {
                t.append(tokens[i].trim()).append(",");
            }
            t.delete(t.length() - 1, t.length());
            tags = t.toString();
        }

        return tags;
    }

    private boolean isPrivateIPAllocated(String ip, long podId, long zoneId, PreparedStatement stmt) {
        try {
            stmt.clearParameters();
            stmt.setString(1, ip);
            stmt.setLong(2, zoneId);
            stmt.setLong(3, podId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return (rs.getString("taken") != null);
            } else {
                return false;
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            return true;
        }
    }

    @DB
    protected boolean deletePublicIPRange(long vlanDbId) {
        Transaction txn = Transaction.currentTxn();
        String deleteSql = "DELETE FROM `cloud`.`user_ip_address` WHERE vlan_db_id = ?";

        txn.start();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(deleteSql);
            stmt.setLong(1, vlanDbId);
            stmt.executeUpdate();
        } catch (Exception ex) {
            return false;
        }
        txn.commit();

        return true;
    }

    @DB
    protected List<String> deletePrivateIPRange(String startIP, String endIP, long podId, long zoneId) {
        long startIPLong = NetUtils.ip2Long(startIP);
        long endIPLong = NetUtils.ip2Long(endIP);
        Transaction txn = Transaction.currentTxn();
        String deleteSql = "DELETE FROM `cloud`.`op_dc_ip_address_alloc` WHERE ip_address = ? AND pod_id = ? AND data_center_id = ?";
        String isPrivateIPAllocatedSelectSql = "SELECT * FROM `cloud`.`op_dc_ip_address_alloc` WHERE ip_address = ? AND data_center_id = ? AND pod_id = ?";

        List<String> problemIPs = new ArrayList<String>();
        PreparedStatement deleteIPStmt = null;
        PreparedStatement isAllocatedStmt = null;

        txn.start();
        try {
            deleteIPStmt = txn.prepareAutoCloseStatement(deleteSql);
            isAllocatedStmt = txn.prepareAutoCloseStatement(isPrivateIPAllocatedSelectSql);
        } catch (SQLException e) {
            return null;
        }

        while (startIPLong <= endIPLong) {
            if (!isPrivateIPAllocated(NetUtils.long2Ip(startIPLong), podId, zoneId, isAllocatedStmt)) {
                try {
                    deleteIPStmt.clearParameters();
                    deleteIPStmt.setString(1, NetUtils.long2Ip(startIPLong));
                    deleteIPStmt.setLong(2, podId);
                    deleteIPStmt.setLong(3, zoneId);
                    deleteIPStmt.executeUpdate();
                } catch (Exception ex) {
                }
            } else {
                problemIPs.add(NetUtils.long2Ip(startIPLong));
            }
            startIPLong += 1;
        }
        txn.commit();

        return problemIPs;
    }

    @DB
    protected boolean savePublicIPRange(String startIP, String endIP, long zoneId, long vlanDbId, long sourceNetworkid) {
        long startIPLong = NetUtils.ip2Long(startIP);
        long endIPLong = NetUtils.ip2Long(endIP);
        Transaction txn = Transaction.currentTxn();
        txn.start();
        IPRangeConfig config = new IPRangeConfig();
        List<String> problemIps = config.savePublicIPRange(txn, startIPLong, endIPLong, zoneId, vlanDbId, sourceNetworkid);
        txn.commit();
        return problemIps != null && problemIps.size() == 0;
    }

    @DB
    protected List<String> savePrivateIPRange(String startIP, String endIP, long podId, long zoneId) {
        Transaction txn = Transaction.currentTxn();
        IPRangeConfig config = new IPRangeConfig();
        txn.start();
        List<String> ips = config.savePrivateIPRange(txn, NetUtils.ip2Long(startIP), NetUtils.ip2Long(endIP), podId, zoneId);
        txn.commit();
        return ips;
    }

    private String genChangeRangeSuccessString(List<String> problemIPs, boolean add) {
        if (problemIPs == null) {
            return "";
        }

        if (problemIPs.size() == 0) {
            if (add) {
                return "Successfully added all IPs in the specified range.";
            } else {
                return "Successfully deleted all IPs in the specified range.";
            }
        } else {
            String successString = "";
            if (add) {
                successString += "Failed to add the following IPs, because they are already in the database: ";
            } else {
                successString += "Failed to delete the following IPs, because they are in use: ";
            }

            for (int i = 0; i < problemIPs.size(); i++) {
                successString += problemIPs.get(i);
                if (i != (problemIPs.size() - 1)) {
                    successString += ", ";
                }
            }

            successString += ". ";

            if (add) {
                successString += "Successfully added all other IPs in the specified range.";
            } else {
                successString += "Successfully deleted all other IPs in the specified range.";
            }

            return successString;
        }
    }

    private void checkPublicIpRangeErrors(long zoneId, String vlanId, String vlanGateway, String vlanNetmask, String startIP, String endIP) {
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

        // Check that the IPs that are being added are compatible with the VLAN's gateway and netmask
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
    }

    private void checkPrivateIpRangeErrors(Long podId, String startIP, String endIP) {
        HostPodVO pod = _podDao.findById(podId);
        if (pod == null) {
            throw new InvalidParameterValueException("Please specify a valid pod.");
        }

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

        // Check that the IPs that are being added are compatible with the pod's CIDR
        String cidrAddress = getCidrAddress(podId);
        long cidrSize = getCidrSize(podId);

        if (endIP != null && !NetUtils.sameSubnetCIDR(startIP, endIP, cidrSize)) {
            throw new InvalidParameterValueException("Please ensure that your start IP and end IP are in the same subnet, as per the pod's CIDR size.");
        }

        if (!NetUtils.sameSubnetCIDR(startIP, cidrAddress, cidrSize)) {
            throw new InvalidParameterValueException("Please ensure that your start IP is in the same subnet as the pod's CIDR address.");
        }

        if (endIP != null && !NetUtils.sameSubnetCIDR(endIP, cidrAddress, cidrSize)) {
            throw new InvalidParameterValueException("Please ensure that your end IP is in the same subnet as the pod's CIDR address.");
        }
    }

    private String getCidrAddress(String cidr) {
        String[] cidrPair = cidr.split("\\/");
        return cidrPair[0];
    }

    private int getCidrSize(String cidr) {
        String[] cidrPair = cidr.split("\\/");
        return Integer.parseInt(cidrPair[1]);
    }

    private String getCidrAddress(long podId) {
        HostPodVO pod = _podDao.findById(podId);
        return pod.getCidrAddress();
    }

    private long getCidrSize(long podId) {
        HostPodVO pod = _podDao.findById(podId);
        return pod.getCidrSize();
    }

    private void checkPodCidrSubnets(long dcId, HashMap<Long, List<Object>> currentPodCidrSubnets) {
        // For each pod, return an error if any of the following is true:
        // 1. The pod's CIDR subnet conflicts with the guest network subnet
        // 2. The pod's CIDR subnet conflicts with the CIDR subnet of any other pod
        DataCenterVO dcVo = _zoneDao.findById(dcId);
        String guestNetworkCidr = dcVo.getGuestNetworkCidr();

        // Guest cidr can be null for Basic zone
        String guestIpNetwork = null;
        Long guestCidrSize = null;
        if (guestNetworkCidr != null) {
            String[] cidrTuple = guestNetworkCidr.split("\\/");
            guestIpNetwork = NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1]));
            guestCidrSize = Long.parseLong(cidrTuple[1]);
        }

        String zoneName = getZoneName(dcId);

        // Iterate through all pods in this zone
        for (Long podId : currentPodCidrSubnets.keySet()) {
            String podName;
            if (podId.longValue() == -1) {
                podName = "newPod";
            } else {
                podName = getPodName(podId.longValue());
            }

            List<Object> cidrPair = currentPodCidrSubnets.get(podId);
            String cidrAddress = (String) cidrPair.get(0);
            long cidrSize = ((Long) cidrPair.get(1)).longValue();

            long cidrSizeToUse = -1;
            if (guestCidrSize == null || cidrSize < guestCidrSize) {
                cidrSizeToUse = cidrSize;
            } else {
                cidrSizeToUse = guestCidrSize;
            }

            String cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);

            if (guestNetworkCidr != null) {
                String guestSubnet = NetUtils.getCidrSubNet(guestIpNetwork, cidrSizeToUse);
                // Check that cidrSubnet does not equal guestSubnet
                if (cidrSubnet.equals(guestSubnet)) {
                    if (podName.equals("newPod")) {
                        throw new InvalidParameterValueException("The subnet of the pod you are adding conflicts with the subnet of the Guest IP Network. Please specify a different CIDR.");
                    } else {
                        throw new InvalidParameterValueException("Warning: The subnet of pod " + podName + " in zone " + zoneName
                                + " conflicts with the subnet of the Guest IP Network. Please change either the pod's CIDR or the Guest IP Network's subnet, and re-run install-vmops-management.");
                    }
                }
            }

            // Iterate through the rest of the pods
            for (Long otherPodId : currentPodCidrSubnets.keySet()) {
                if (podId.equals(otherPodId)) {
                    continue;
                }

                // Check that cidrSubnet does not equal otherCidrSubnet
                List<Object> otherCidrPair = currentPodCidrSubnets.get(otherPodId);
                String otherCidrAddress = (String) otherCidrPair.get(0);
                long otherCidrSize = ((Long) otherCidrPair.get(1)).longValue();

                if (cidrSize < otherCidrSize) {
                    cidrSizeToUse = cidrSize;
                } else {
                    cidrSizeToUse = otherCidrSize;
                }

                cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);
                String otherCidrSubnet = NetUtils.getCidrSubNet(otherCidrAddress, cidrSizeToUse);

                if (cidrSubnet.equals(otherCidrSubnet)) {
                    String otherPodName = getPodName(otherPodId.longValue());
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

    private boolean validPod(long podId) {
        return (_podDao.findById(podId) != null);
    }

    private boolean validPod(String podName, long zoneId) {
        if (!validZone(zoneId)) {
            return false;
        }

        return (_podDao.findByName(podName, zoneId) != null);
    }

    private String getPodName(long podId) {
        return _podDao.findById(new Long(podId)).getName();
    }

    private boolean validZone(String zoneName) {
        return (_zoneDao.findByName(zoneName) != null);
    }

    private boolean validZone(long zoneId) {
        return (_zoneDao.findById(zoneId) != null);
    }

    private String getZoneName(long zoneId) {
        DataCenterVO zone = _zoneDao.findById(new Long(zoneId));
        if (zone != null) {
            return zone.getName();
        } else {
            return null;
        }
    }

    private String[] getLinkLocalIPRange() {
        String ipNums = _configDao.getValue("linkLocalIp.nums");
        int nums = Integer.parseInt(ipNums);
        if (nums > 16 || nums <= 0) {
            throw new InvalidParameterValueException("The linkLocalIp.nums: " + nums + "is wrong, should be 1~16");
        }
        /* local link ip address starts from 169.254.0.2 - 169.254.(nums) */
        String[] ipRanges = NetUtils.getLinkLocalIPRange(nums);
        if (ipRanges == null) {
            throw new InvalidParameterValueException("The linkLocalIp.nums: " + nums + "may be wrong, should be 1~16");
        }
        return ipRanges;
    }

    @Override
    public Configuration addConfig(CreateCfgCmd cmd) {
        String category = cmd.getCategory();
        String instance = cmd.getInstance();
        String component = cmd.getComponent();
        String name = cmd.getConfigPropName();
        String value = cmd.getValue();
        String description = cmd.getDescription();
        try {
            ConfigurationVO entity = new ConfigurationVO(category, instance, component, name, value, description);
            _configDao.persist(entity);
            s_logger.info("Successfully added configuration value into db: category:" + category + " instance:" + instance + " component:" + component + " name:" + name + " value:" + value);
            return _configDao.findByName(name);
        } catch (Exception ex) {
            s_logger.error("Unable to add the new config entry:", ex);
            throw new CloudRuntimeException("Unable to add configuration parameter " + name);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VLAN_IP_RANGE_DELETE, eventDescription = "deleting vlan ip range", async = false)
    public boolean deleteVlanIpRange(DeleteVlanIpRangeCmd cmd) {
        Long vlanDbId = cmd.getId();
        Long userId = UserContext.current().getCallerUserId();

        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

        VlanVO vlan = _vlanDao.findById(vlanDbId);
        if (vlan == null) {
            throw new InvalidParameterValueException("Please specify a valid IP range id.");
        }

        return deleteVlanAndPublicIpRange(userId, vlanDbId);

    }

    @Override
    public void checkDiskOfferingAccess(Account caller, DiskOffering dof) throws PermissionDeniedException {
        for (SecurityChecker checker : _secChecker) {
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
    public void checkServiceOfferingAccess(Account caller, ServiceOffering so) throws PermissionDeniedException {
        for (SecurityChecker checker : _secChecker) {
            if (checker.checkAccess(caller, so)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Access granted to " + caller + " to service offering:" + so.getId() + " by " + checker.getName());
                }
                return;
            } else {
                throw new PermissionDeniedException("Access denied to " + caller + " by " + checker.getName());
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + caller + " has access to service offering:" + so.getId());
    }

    @Override
    public void checkAccess(Account caller, DataCenter zone) throws PermissionDeniedException {
        for (SecurityChecker checker : _secChecker) {
            if (checker.checkAccess(caller, zone)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Access granted to " + caller + " to zone:" + zone.getId() + " by " + checker.getName());
                }
                return;
            } else {
                throw new PermissionDeniedException("Access denied to " + caller + " by " + checker.getName());
            }
        }

        assert false : "How can all of the security checkers pass on checking this caller?";
        throw new PermissionDeniedException("There's no way to confirm " + caller + " has access to zone:" + zone.getId());
    }

    @Override @ActionEvent(eventType = EventTypes.EVENT_NETWORK_OFFERING_CREATE, eventDescription = "creating network offering")
    public NetworkOffering createNetworkOffering(CreateNetworkOfferingCmd cmd) {
        Long userId = UserContext.current().getCallerUserId();
        String name = cmd.getNetworkOfferingName();
        String displayText = cmd.getDisplayText();
        String tags = cmd.getTags();
        String trafficTypeString = cmd.getTraffictype();
        Boolean specifyVlan = cmd.getSpecifyVlan();
        String availabilityStr = cmd.getAvailability();
        Boolean isSecurityGroupEnabled = cmd.getSecurityGroupEnabled();

        Integer networkRate = cmd.getNetworkRate();

        TrafficType trafficType = null;
        Availability availability = null;
        Network.Type type = null;

        // Verify traffic type
        for (TrafficType tType : TrafficType.values()) {
            if (tType.name().equalsIgnoreCase(trafficTypeString)) {
                trafficType = tType;
                break;
            }
        }
        if (trafficType == null) {
            throw new InvalidParameterValueException("Invalid value for traffictype. Supported traffic types: Public, Management, Control, Guest, Vlan or Storage");
        }

        
        //Verify offering type
        for (Network.Type offType : Network.Type.values()) {
            if (offType.name().equalsIgnoreCase(cmd.getType())){
                type = offType;
                break;
            }
        }
        
        if (type == null) {
            throw new InvalidParameterValueException("Invalid \"type\" parameter is given; can have Shared and Isolated values");
        }

        // Verify availability
        for (Availability avlb : Availability.values()) {
            if (avlb.name().equalsIgnoreCase(availabilityStr)) {
                availability = avlb;
            }
        }

        if (availability == null) {
            throw new InvalidParameterValueException("Invalid value for Availability. Supported types: " + Availability.Required + ", " + Availability.Optional + ", " + Availability.Unavailable);
        }

        Integer maxConnections = cmd.getMaxconnections();
        
        //configure service provider map
        Map<Network.Service, Set<Network.Provider>> serviceProviderMap = new HashMap<Network.Service, Set<Network.Provider>>();
        Set<Network.Provider> defaultProviders = new HashSet<Network.Provider>();
        defaultProviders.add(Network.Provider.defaultProvider);
        //populate all services first
        if (cmd.getDhcpService()) {
            serviceProviderMap.put(Network.Service.Dhcp, defaultProviders);            
        }
        
        if (cmd.getDnsService()) {
            serviceProviderMap.put(Network.Service.Dns, defaultProviders);    
        }
        
        if (cmd.getFirewallService()) {
            serviceProviderMap.put(Network.Service.Firewall, defaultProviders);    
        }
        
        if (cmd.getGatewayService()) {
            serviceProviderMap.put(Network.Service.Gateway, defaultProviders);    
        }
        
        if (cmd.getLbService()) {
            serviceProviderMap.put(Network.Service.Lb, defaultProviders);    
        }
        
        if (cmd.getSourceNatService()) {
            serviceProviderMap.put(Network.Service.SourceNat, defaultProviders);    
        }
        
        if (cmd.getUserdataService()) {
            serviceProviderMap.put(Network.Service.UserData, defaultProviders);    
        }
        
        if (cmd.getVpnService()) {
            serviceProviderMap.put(Network.Service.Vpn, defaultProviders);    
        } 
        
        //populate providers
        Map<String, List<String>> svcPrv = (Map<String, List<String>>)cmd.getServiceProviders();
        if (svcPrv != null) {
            for (String serviceStr : svcPrv.keySet()) {
                Network.Service service = Network.Service.getService(serviceStr);
                if (serviceProviderMap.containsKey(service)) {
                    serviceProviderMap.clear();
                    Set<Provider> providers = new HashSet<Provider>();
                    for (String prvNameStr : svcPrv.get(serviceStr)) {
                        //check if provider is supported
                        Network.Provider provider;
                        provider = Network.Provider.getProvider(prvNameStr);
                        if (provider == null) {
                            throw new InvalidParameterValueException("Invalid service provider: " + prvNameStr);
                        }
                        providers.add(provider);
                    }
                    serviceProviderMap.put(service, providers);
                } else {
                    throw new InvalidParameterValueException("Service " + serviceStr + " is not enabled for the network offering, can't add a provider to it");
                }
            }
        }
        
        return createNetworkOffering(userId, name, displayText, trafficType, tags, maxConnections, specifyVlan, availability, networkRate, serviceProviderMap, false, isSecurityGroupEnabled, type, false);
    }

    @Override @DB
    public NetworkOfferingVO createNetworkOffering(long userId, String name, String displayText, TrafficType trafficType, String tags, Integer maxConnections, boolean specifyVlan, 
            Availability availability, Integer networkRate, Map<Service, Set<Provider>> serviceProviderMap, boolean isDefault, boolean isSecurityGroupEnabled, Network.Type type, boolean systemOnly) {

        String multicastRateStr = _configDao.getValue("multicast.throttling.rate");
        int multicastRate = ((multicastRateStr == null) ? 10 : Integer.parseInt(multicastRateStr));
        tags = cleanupTags(tags);


        NetworkOfferingVO offering = new NetworkOfferingVO(name, displayText, trafficType, systemOnly, specifyVlan, networkRate, multicastRate, maxConnections, isDefault, availability, tags, isSecurityGroupEnabled, type);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        //create network offering object
        s_logger.debug("Adding network offering " + offering);
        offering = _networkOfferingDao.persist(offering);
        //populate services and providers
        if (serviceProviderMap != null) {
            for (Network.Service service : serviceProviderMap.keySet()) {
                for (Network.Provider provider : serviceProviderMap.get(service)) {
                    NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO(offering.getId(), service, provider);
                    _ntwkOffServiceMapDao.persist(offService);
                    s_logger.trace("Added service for the network offering: " + offService);
                }
            }
        }

        txn.commit();
        
        UserContext.current().setEventDetails(" Id: "+offering.getId()+" Name: "+name);
        return offering;
    }

    @Override
    public List<? extends NetworkOffering> searchForNetworkOfferings(ListNetworkOfferingsCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Filter searchFilter = new Filter(NetworkOfferingVO.class, "created", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<NetworkOfferingVO> sc = _networkOfferingDao.createSearchCriteria();

        Long id = cmd.getId();
        Object name = cmd.getNetworkOfferingName();
        Object displayText = cmd.getDisplayText();
        Object trafficType = cmd.getTrafficType();
        Object isDefault = cmd.getIsDefault();
        Object specifyVlan = cmd.getSpecifyVlan();
        Object isShared = cmd.getIsShared();
        Object availability = cmd.getAvailability();
        Object sgEnabled = cmd.getSecurityGroupEnabled();
        Object state = cmd.getState();
        Long zoneId = cmd.getZoneId();
        DataCenter zone = null;
        Long networkId = cmd.getNetworkId();
        String type = cmd.getType();

        if (zoneId != null) {
            zone = getZone(zoneId);
        }

        Object keyword = cmd.getKeyword();

        if (keyword != null) {
            SearchCriteria<NetworkOfferingVO> ssc = _networkOfferingDao.createSearchCriteria();
            ssc.addOr("displayText", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");

            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }
        
        if (type != null) {
            sc.addAnd("type", SearchCriteria.Op.EQ, type);
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

        if (specifyVlan != null) {
            sc.addAnd("specifyVlan", SearchCriteria.Op.EQ, specifyVlan);
        }

        if (isShared != null) {
            sc.addAnd("isShared", SearchCriteria.Op.EQ, isShared);
        }

        if (availability != null) {
            sc.addAnd("availability", SearchCriteria.Op.EQ, availability);
        }
        
        if (state != null) {
            sc.addAnd("state", SearchCriteria.Op.EQ, state);
        }
        
        if (sgEnabled != null) {
            sc.addAnd("securityGroupEnabled", SearchCriteria.Op.EQ, sgEnabled);
        }

        if (zone != null) {
            if (zone.getNetworkType() == NetworkType.Basic) {
                // return empty list as we don't allow to create networks in basic zone, and shouldn't display networkOfferings
                return new ArrayList<NetworkOffering>();
            }
        }
        
        // Don't return system network offerings to the user
        sc.addAnd("systemOnly", SearchCriteria.Op.EQ, false);
        
        //list offerings available for upgrade only
        if (networkId != null) {
            //check if network exists and the caller can operate with it
            Network network = _networkMgr.getNetwork(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Unable to find the network by id=" + networkId);
            }
            // Don't allow to update system network
            NetworkOffering offering = _networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
            if (offering.isSystemOnly()) {
                throw new InvalidParameterValueException("Can't update system networks");
            }
            
            _accountMgr.checkAccess(caller, null, network);
            
            List<Long> offeringIds = _networkMgr.listNetworkOfferingsForUpgrade(networkId);
            
            if (!offeringIds.isEmpty()) {
                sc.addAnd("id", SearchCriteria.Op.IN, offeringIds.toArray());  
            } else {
                return new ArrayList<NetworkOffering>();
            }
        }
        
        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        return _networkOfferingDao.search(sc, searchFilter);
    }

    @Override @ActionEvent(eventType = EventTypes.EVENT_NETWORK_OFFERING_DELETE, eventDescription = "deleting network offering")
    public boolean deleteNetworkOffering(DeleteNetworkOfferingCmd cmd) {
        Long offeringId = cmd.getId();
        UserContext.current().setEventDetails(" Id: "+offeringId);
        
        // Verify network offering id
        NetworkOfferingVO offering = _networkOfferingDao.findById(offeringId);
        if (offering == null) {
            throw new InvalidParameterValueException("unable to find network offering " + offeringId);
        } else if (offering.getRemoved() != null || offering.isSystemOnly()) {
            throw new InvalidParameterValueException("unable to find network offering " + offeringId);
        }

        // Don't allow to delete default network offerings
        if (offering.isDefault() == true) {
            throw new InvalidParameterValueException("Default network offering can't be deleted");
        }

        if (_networkOfferingDao.remove(offeringId)) {
            return true;
        } else {
            return false;
        }
    }

    @Override @ActionEvent(eventType = EventTypes.EVENT_NETWORK_OFFERING_EDIT, eventDescription = "updating network offering")
    @DB
    public NetworkOffering updateNetworkOffering(UpdateNetworkOfferingCmd cmd) {
        String displayText = cmd.getDisplayText();
        Long id = cmd.getId();
        String name = cmd.getNetworkOfferingName();
        String availabilityStr = cmd.getAvailability();
        Availability availability = null;
        Boolean sgEnabled = cmd.getSecurityGroupEnabled();
        String state = cmd.getState();
        UserContext.current().setEventDetails(" Id: "+id);
        
        // Verify input parameters
        NetworkOfferingVO offeringToUpdate = _networkOfferingDao.findById(id);
        if (offeringToUpdate == null) {
            throw new InvalidParameterValueException("unable to find network offering " + id);
        }

        // Don't allow to update system network offering
        if (offeringToUpdate.isSystemOnly()) {
            throw new InvalidParameterValueException("Can't update system network offerings");
        }

        NetworkOfferingVO offering = _networkOfferingDao.createForUpdate(id);

        if (name != null) {
            offering.setName(name);
        }

        if (displayText != null) {
            offering.setDisplayText(displayText);
        }
        
        if (state != null) {
            boolean validState = false;
            for (NetworkOffering.State st : NetworkOffering.State.values()) {
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
            for (Availability avlb : Availability.values()) {
                if (avlb.name().equalsIgnoreCase(availabilityStr)) {
                    availability = avlb;
                }
            }
            if (availability == null) {
                throw new InvalidParameterValueException("Invalid value for Availability. Supported types: " + Availability.Required + ", " + Availability.Optional + ", " + Availability.Unavailable);
            } else {
                offering.setAvailability(availability);
            }
        }
        
        //All parameters below can be updated only when there are no networks using this offering
        Long networks = _networkDao.getNetworkCountByOfferingId(id);
        boolean networksExist = (networks != null && networks.longValue() > 0);
        
        if (sgEnabled != null) {
            if (networksExist) {
                throw new InvalidParameterValueException("Unable to reset securityGroupEnabled property as there are existing networks using this network offering");
            }
            offering.setSecurityGroupEnabled(sgEnabled);
        }
        
        //configure service provider map
        Map<Network.Service, Set<Network.Provider>> serviceProviderMap = new HashMap<Network.Service, Set<Network.Provider>>();
        Set<Network.Provider> defaultProviders = new HashSet<Network.Provider>();
        defaultProviders.add(Network.Provider.defaultProvider);
        //populate all services first
        if (cmd.getDhcpService()) {
            serviceProviderMap.put(Network.Service.Dhcp, defaultProviders);            
        }
        
        if (cmd.getDnsService()) {
            serviceProviderMap.put(Network.Service.Dns, defaultProviders);    
        }
        
        if (cmd.getFirewallService()) {
            serviceProviderMap.put(Network.Service.Firewall, defaultProviders);    
        }
        
        if (cmd.getGatewayService()) {
            serviceProviderMap.put(Network.Service.Gateway, defaultProviders);    
        }
        
        if (cmd.getLbService()) {
            serviceProviderMap.put(Network.Service.Lb, defaultProviders);    
        }
        
        if (cmd.getSourceNatService()) {
            serviceProviderMap.put(Network.Service.SourceNat, defaultProviders);    
        }
        
        if (cmd.getUserdataService()) {
            serviceProviderMap.put(Network.Service.UserData, defaultProviders);    
        }
        
        if (cmd.getVpnService()) {
            serviceProviderMap.put(Network.Service.Vpn, defaultProviders);    
        } 
        
        //populate providers
        Map<String, List<String>> svcPrv = (Map<String, List<String>>)cmd.getServiceProviders();
        if (svcPrv != null) {
            for (String serviceStr : svcPrv.keySet()) {
                Network.Service service = Network.Service.getService(serviceStr);
                if (serviceProviderMap.containsKey(service)) {
                    serviceProviderMap.clear();
                    Set<Provider> providers = new HashSet<Provider>();
                    for (String prvNameStr : svcPrv.get(serviceStr)) {
                        //check if provider is supported
                        Network.Provider provider;
                        provider = Network.Provider.getProvider(prvNameStr);
                        if (provider == null) {
                            throw new InvalidParameterValueException("Invalid service provider: " + prvNameStr);
                        }
                        providers.add(provider);
                    }
                    serviceProviderMap.put(service, providers);
                } else {
                    throw new InvalidParameterValueException("Service " + serviceStr + " is not enabled for the network offering, can't add a provider to it");
                }
            }
        }
        
        if (svcPrv != null && !svcPrv.isEmpty()) {
            if (networksExist) {
                throw new InvalidParameterValueException("Unable to reset service providers as there are existing networks using this network offering");
            }
        }
        
        
        boolean success = true;
        Transaction txn = Transaction.currentTxn();
        txn.start();
        //update network offering
        success = success && _networkOfferingDao.update(id, offering);
        _ntwkOffServiceMapDao.deleteByOfferingId(id);
        //update services/providers - delete old ones, insert new ones
        if (serviceProviderMap != null) {
            for (Network.Service service : serviceProviderMap.keySet()) {
                for (Network.Provider provider : serviceProviderMap.get(service)) {
                    NetworkOfferingServiceMapVO offService = new NetworkOfferingServiceMapVO(offering.getId(), service, provider);
                    _ntwkOffServiceMapDao.persist(offService);
                    s_logger.trace("Added service for the network offering: " + offService);
                }
            }
        }
        
        txn.commit();
        
        if (success) {
            return _networkOfferingDao.findById(id);
        } else {
            return null;
        }
    }

    // Note: This method will be used for entity name validations in the coming releases (place holder for now)
    private void validateEntityName(String str) {
        String forbidden = "~!@#$%^&*()+=";
        char[] searchChars = forbidden.toCharArray();
        if (str == null || str.length() == 0 || searchChars == null || searchChars.length == 0) {
            return;
        }
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            for (int j = 0; j < searchChars.length; j++) {
                if (searchChars[j] == ch) {
                    throw new InvalidParameterValueException("Name cannot contain any of the following special characters:" + forbidden);
                }
            }
        }
    }

    @Override
    public DataCenterVO getZone(long id) {
        return _zoneDao.findById(id);
    }

    @Override
    public NetworkOffering getNetworkOffering(long id) {
        return _networkOfferingDao.findById(id);
    }

    @Override
    public Integer getNetworkOfferingNetworkRate(long networkOfferingId) {

        // validate network offering information
        NetworkOffering no = getNetworkOffering(networkOfferingId);
        if (no == null) {
            throw new InvalidParameterValueException("Unable to find network offering by id=" + networkOfferingId);
        }

        Integer networkRate;
        if (no.getRateMbps() != null) {
            networkRate = no.getRateMbps();
        } else {
            networkRate = Integer.parseInt(_configDao.getValue(Config.NetworkThrottlingRate.key()));
        }

        // networkRate is unsigned int in netowrkOfferings table, and can't be set to -1
        // so 0 means unlimited; we convert it to -1, so we are consistent with all our other resources where -1 means unlimited
        if (networkRate == 0) {
            networkRate = -1;
        }

        return networkRate;
    }

    @Override
    public Account getVlanAccount(long vlanId) {
        Vlan vlan = _vlanDao.findById(vlanId);
        Long accountId = null;

        // if vlan is Virtual Account specific, get vlan information from the accountVlanMap; otherwise get account information
        // from the network
        if (vlan.getVlanType() == VlanType.VirtualNetwork) {
            List<AccountVlanMapVO> maps = _accountVlanMapDao.listAccountVlanMapsByVlan(vlanId);
            if (maps != null && !maps.isEmpty()) {
                return _accountMgr.getAccount(maps.get(0).getAccountId());
            }
        }

        Long networkId = vlan.getNetworkId();
        if (networkId != null) {
            Network network = _networkMgr.getNetwork(networkId);
            if (network != null) {
                accountId = network.getAccountId();
            }
        }

        return _accountMgr.getAccount(accountId);
    }

    @Override
    public List<? extends NetworkOffering> listNetworkOfferings(TrafficType trafficType, boolean systemOnly) {
        Filter searchFilter = new Filter(NetworkOfferingVO.class, "created", false, null, null);
        SearchCriteria<NetworkOfferingVO> sc = _networkOfferingDao.createSearchCriteria();
        if (trafficType != null) {
            sc.addAnd("trafficType", SearchCriteria.Op.EQ, trafficType);
        }
        sc.addAnd("systemOnly", SearchCriteria.Op.EQ, systemOnly);

        return _networkOfferingDao.search(sc, searchFilter);
    }

    @Override
    @DB
    public boolean deleteAccountSpecificVirtualRanges(long accountId) {
        List<AccountVlanMapVO> maps = _accountVlanMapDao.listAccountVlanMapsByAccount(accountId);
        boolean result = true;
        if (maps != null && !maps.isEmpty()) {
            Transaction txn = Transaction.currentTxn();
            txn.start();
            for (AccountVlanMapVO map : maps) {
                if (!deleteVlanAndPublicIpRange(_accountMgr.getSystemUser().getId(), map.getVlanDbId())) {
                    result = false;
                }
            }
            if (result) {
                txn.commit();
            } else {
                s_logger.error("Failed to delete account specific virtual ip ranges for account id=" + accountId);
            }
        } else {
            s_logger.trace("Account id=" + accountId + " has no account specific virtual ip ranges, nothing to delete");
        }
        return result;
    }

    @Override
    public HostPodVO getPod(long id) {
        return _podDao.findById(id);
    }

    @Override
    public ClusterVO getCluster(long id) {
        return _clusterDao.findById(id);
    }

    private boolean allowIpRangeOverlap(VlanVO vlan, boolean forVirtualNetwork, long networkId) {
        //FIXME - delete restriction for virtual network in the future
        if (vlan.getVlanType() == VlanType.DirectAttached && !forVirtualNetwork) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ServiceOffering getServiceOffering(long serviceOfferingId) {
        ServiceOfferingVO offering = _serviceOfferingDao.findById(serviceOfferingId);
        if (offering != null && offering.getRemoved() == null) {
            return offering;
        }

        return null;
    }

    @Override
    public Long getDefaultPageSize() {
        return _defaultPageSize;
    }

    @Override
    public Integer getServiceOfferingNetworkRate(long serviceOfferingId) {

        // validate network offering information
        ServiceOffering offering = _serviceOfferingDao.findById(serviceOfferingId);
        if (offering == null) {
            throw new InvalidParameterValueException("Unable to find service offering by id=" + serviceOfferingId);
        }

        Integer networkRate;
        if (offering.getRateMbps() != null) {
            networkRate = offering.getRateMbps();
        } else {
            //for domain router service offering, get network rate from 
            if (offering.getSystemVmType() != null && offering.getSystemVmType().equalsIgnoreCase(VirtualMachine.Type.DomainRouter.toString())) {
                networkRate = Integer.parseInt(_configDao.getValue(Config.NetworkThrottlingRate.key()));
            } else {
                networkRate = Integer.parseInt(_configDao.getValue(Config.VmNetworkThrottlingRate.key()));
            }
        }

        // networkRate is unsigned int in serviceOffering table, and can't be set to -1
        // so 0 means unlimited; we convert it to -1, so we are consistent with all our other resources where -1 means unlimited
        if (networkRate == 0) {
            networkRate = -1;
        }

        return networkRate;
    }

    @Override
    public DiskOffering getDiskOffering(long diskOfferingId) {
        DiskOfferingVO offering = _diskOfferingDao.findById(diskOfferingId);
        if (offering != null && offering.getRemoved() == null) {
            return offering;
        }

        return null;
    }

}
