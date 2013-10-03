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
package com.cloud.alert;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.sun.mail.smtp.SMTPMessage;
import com.sun.mail.smtp.SMTPSSLTransport;
import com.sun.mail.smtp.SMTPTransport;

import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.alert.dao.AlertDao;
import com.cloud.api.ApiDBUtils;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.CapacityState;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.capacity.dao.CapacityDaoImpl.SummedCapacity;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.event.AlertGenerator;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.StorageManager;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchCriteria;

@Local(value={AlertManager.class})
public class AlertManagerImpl extends ManagerBase implements AlertManager, Configurable {
    private static final Logger s_logger = Logger.getLogger(AlertManagerImpl.class.getName());
    private static final Logger s_alertsLogger = Logger.getLogger("org.apache.cloudstack.alerts");

    private static final long INITIAL_CAPACITY_CHECK_DELAY = 30L * 1000L; // thirty seconds expressed in milliseconds

    private static final DecimalFormat _dfPct = new DecimalFormat("###.##");
    private static final DecimalFormat _dfWhole = new DecimalFormat("########");

    private EmailAlert _emailAlert;
    @Inject private AlertDao _alertDao;
    @Inject protected StorageManager _storageMgr;
    @Inject protected CapacityManager _capacityMgr;
    @Inject private CapacityDao _capacityDao;
    @Inject private DataCenterDao _dcDao;
    @Inject private HostPodDao _podDao;
    @Inject private ClusterDao _clusterDao;
    @Inject private IPAddressDao _publicIPAddressDao;
    @Inject private DataCenterIpAddressDao _privateIPAddressDao;
    @Inject private PrimaryDataStoreDao _storagePoolDao;
    @Inject private ConfigurationDao _configDao;
    @Inject private ResourceManager _resourceMgr;
    @Inject private ConfigurationManager _configMgr;
    @Inject
    protected ConfigDepot _configDepot;

    private Timer _timer = null;
    private long _capacityCheckPeriod = 60L * 60L * 1000L; // one hour by default
    private double _publicIPCapacityThreshold = 0.75;
    private double _privateIPCapacityThreshold = 0.75;
    private double _secondaryStorageCapacityThreshold = 0.75;
    private double _vlanCapacityThreshold = 0.75;
    private double _directNetworkPublicIpCapacityThreshold = 0.75;
    private double _localStorageCapacityThreshold = 0.75;
    Map<Short,Double> _capacityTypeThresholdMap = new HashMap<Short, Double>();

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        Map<String, String> configs = _configDao.getConfiguration("management-server", params);

        // set up the email system for alerts
        String emailAddressList = configs.get("alert.email.addresses");
        String[] emailAddresses = null;
        if (emailAddressList != null) {
            emailAddresses = emailAddressList.split(",");
        }

        String smtpHost = configs.get("alert.smtp.host");
        int smtpPort = NumbersUtil.parseInt(configs.get("alert.smtp.port"), 25);
        String useAuthStr = configs.get("alert.smtp.useAuth");
        boolean useAuth = ((useAuthStr == null) ? false : Boolean.parseBoolean(useAuthStr));
        String smtpUsername = configs.get("alert.smtp.username");
        String smtpPassword = configs.get("alert.smtp.password");
        String emailSender = configs.get("alert.email.sender");
        String smtpDebugStr = configs.get("alert.smtp.debug");
        boolean smtpDebug = false;
        if (smtpDebugStr != null) {
            smtpDebug = Boolean.parseBoolean(smtpDebugStr);
        }

        _emailAlert = new EmailAlert(emailAddresses, smtpHost, smtpPort, useAuth, smtpUsername, smtpPassword, emailSender, smtpDebug);

        String publicIPCapacityThreshold = _configDao.getValue(Config.PublicIpCapacityThreshold.key());
        String privateIPCapacityThreshold = _configDao.getValue(Config.PrivateIpCapacityThreshold.key());
        String secondaryStorageCapacityThreshold = _configDao.getValue(Config.SecondaryStorageCapacityThreshold.key());
        String vlanCapacityThreshold = _configDao.getValue(Config.VlanCapacityThreshold.key());
        String directNetworkPublicIpCapacityThreshold = _configDao.getValue(Config.DirectNetworkPublicIpCapacityThreshold.key());
        String localStorageCapacityThreshold = _configDao.getValue(Config.LocalStorageCapacityThreshold.key());

        if (publicIPCapacityThreshold != null) {
            _publicIPCapacityThreshold = Double.parseDouble(publicIPCapacityThreshold);
        }
        if (privateIPCapacityThreshold != null) {
            _privateIPCapacityThreshold = Double.parseDouble(privateIPCapacityThreshold);
        }
        if (secondaryStorageCapacityThreshold != null) {
            _secondaryStorageCapacityThreshold = Double.parseDouble(secondaryStorageCapacityThreshold);
        }
        if (vlanCapacityThreshold != null) {
            _vlanCapacityThreshold = Double.parseDouble(vlanCapacityThreshold);
        }
        if (directNetworkPublicIpCapacityThreshold != null) {
            _directNetworkPublicIpCapacityThreshold = Double.parseDouble(directNetworkPublicIpCapacityThreshold);
        }
        if (localStorageCapacityThreshold != null) {
            _localStorageCapacityThreshold = Double.parseDouble(localStorageCapacityThreshold);
        }

        _capacityTypeThresholdMap.put(Capacity.CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP, _publicIPCapacityThreshold);
        _capacityTypeThresholdMap.put(Capacity.CAPACITY_TYPE_PRIVATE_IP, _privateIPCapacityThreshold);
        _capacityTypeThresholdMap.put(Capacity.CAPACITY_TYPE_SECONDARY_STORAGE, _secondaryStorageCapacityThreshold);
        _capacityTypeThresholdMap.put(Capacity.CAPACITY_TYPE_VLAN, _vlanCapacityThreshold);
        _capacityTypeThresholdMap.put(Capacity.CAPACITY_TYPE_DIRECT_ATTACHED_PUBLIC_IP, _directNetworkPublicIpCapacityThreshold);
        _capacityTypeThresholdMap.put(Capacity.CAPACITY_TYPE_LOCAL_STORAGE, _localStorageCapacityThreshold);


        String capacityCheckPeriodStr = configs.get("capacity.check.period");
        if (capacityCheckPeriodStr != null) {
            _capacityCheckPeriod = Long.parseLong(capacityCheckPeriodStr);
            if(_capacityCheckPeriod <= 0)
                _capacityCheckPeriod = Long.parseLong(Config.CapacityCheckPeriod.getDefaultValue());
        }

        _timer = new Timer("CapacityChecker");

        return true;
    }

    @Override
    public boolean start() {
        _timer.schedule(new CapacityChecker(), INITIAL_CAPACITY_CHECK_DELAY, _capacityCheckPeriod);
        return true;
    }

    @Override
    public boolean stop() {
        _timer.cancel();
        return true;
    }

    @Override
    public void clearAlert(short alertType, long dataCenterId, long podId) {
        try {
            if (_emailAlert != null) {
                _emailAlert.clearAlert(alertType, dataCenterId, podId);
            }
        } catch (Exception ex) {
            s_logger.error("Problem clearing email alert", ex);
        }
    }

    @Override
    public void sendAlert(short alertType, long dataCenterId, Long podId, String subject, String body) {

        // publish alert
        AlertGenerator.publishAlertOnEventBus(getAlertType(alertType), dataCenterId, podId, subject, body);

        // TODO:  queue up these messages and send them as one set of issues once a certain number of issues is reached?  If that's the case,
        //         shouldn't we have a type/severity as part of the API so that severe errors get sent right away?
        try {
            if (_emailAlert != null) {
                _emailAlert.sendAlert(alertType, dataCenterId, podId, null, subject, body);
            } else {
                s_alertsLogger.warn(" alertType:: " + alertType + " // dataCenterId:: " + dataCenterId + " // podId:: "
                    + podId + " // clusterId:: " + null + " // message:: " + subject );
            }
        } catch (Exception ex) {
            s_logger.error("Problem sending email alert", ex);
        }
    }

    private String getAlertType(short alertType) {
        if (alertType == ALERT_TYPE_MEMORY) {
            return "ALERT.MEMORY";
        } else if (alertType == ALERT_TYPE_CPU) {
            return "ALERT.MEMORY";
        } else if (alertType == ALERT_TYPE_STORAGE) {
            return "ALERT.STORAGE";
        } else if (alertType == ALERT_TYPE_STORAGE_ALLOCATED) {
            return "ALERT.STORAGE.ALLOCATED";
        } else if (alertType == ALERT_TYPE_VIRTUAL_NETWORK_PUBLIC_IP) {
            return "ALERT.NETWORK.PUBLICIP";
        } else if (alertType == ALERT_TYPE_PRIVATE_IP) {
            return "ALERT.NETWORK.PRIVATEIP";
        } else if (alertType == ALERT_TYPE_SECONDARY_STORAGE) {
            return "ALERT.STORAGE.SECONDARY";
        } else if (alertType == ALERT_TYPE_HOST) {
            return "ALERT.COMPUTE.HOST";
        } else if (alertType == ALERT_TYPE_USERVM) {
            return "ALERT.USERVM";
        } else if (alertType == ALERT_TYPE_DOMAIN_ROUTER) {
            return "ALERT.SERVICE.DOMAINROUTER";
        } else if (alertType == ALERT_TYPE_CONSOLE_PROXY) {
            return "ALERT.SERVICE.CONSOLEPROXY";
        } else if (alertType == ALERT_TYPE_ROUTING) {
            return "ALERT.NETWORK.ROUTING";
        } else if (alertType == ALERT_TYPE_STORAGE_MISC) {
            return "ALERT.STORAGE.MISC";
        } else if (alertType == ALERT_TYPE_USAGE_SERVER) {
            return "ALERT.USAGE";
        } else if (alertType == ALERT_TYPE_MANAGMENT_NODE) {
            return "ALERT.MANAGEMENT";
        } else if (alertType == ALERT_TYPE_DOMAIN_ROUTER_MIGRATE) {
            return "ALERT.NETWORK.DOMAINROUTERMIGRATE";
        } else if (alertType == ALERT_TYPE_CONSOLE_PROXY_MIGRATE) {
            return "ALERT.SERVICE.CONSOLEPROXYMIGRATE";
        } else if (alertType == ALERT_TYPE_USERVM_MIGRATE) {
            return "ALERT.USERVM.MIGRATE";
        } else if (alertType == ALERT_TYPE_VLAN) {
            return "ALERT.NETWORK.VLAN";
        } else if (alertType == ALERT_TYPE_SSVM) {
            return "ALERT.SERVICE.SSVM";
        } else if (alertType == ALERT_TYPE_USAGE_SERVER_RESULT) {
            return "ALERT.USAGE.RESULT";
        } else if (alertType == ALERT_TYPE_STORAGE_DELETE) {
            return "ALERT.STORAGE.DELETE";
        } else if (alertType == ALERT_TYPE_UPDATE_RESOURCE_COUNT) {
            return "ALERT.RESOURCE.COUNT";
        } else if (alertType == ALERT_TYPE_USAGE_SANITY_RESULT) {
            return "ALERT.USAGE.SANITY";
        } else if (alertType == ALERT_TYPE_DIRECT_ATTACHED_PUBLIC_IP) {
            return "ALERT.NETWORK.DIRECTPUBLICIP";
        } else if (alertType == ALERT_TYPE_LOCAL_STORAGE) {
            return "ALERT.STORAGE.LOCAL";
        } else if (alertType == ALERT_TYPE_RESOURCE_LIMIT_EXCEEDED) {
            return "ALERT.RESOURCE.EXCEED";
        }
        return "UNKNOWN";
    }

    @Override @DB
    public void recalculateCapacity() {
        // FIXME: the right way to do this is to register a listener (see RouterStatsListener, VMSyncListener)
        //        for the vm sync state.  The listener model has connects/disconnects to keep things in sync much better
        //        than this model right now, so when a VM is started, we update the amount allocated, and when a VM
        //        is stopped we updated the amount allocated, and when VM sync reports a changed state, we update
        //        the amount allocated.  Hopefully it's limited to 3 entry points and will keep the amount allocated
        //        per host accurate.

        try {

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("recalculating system capacity");
                s_logger.debug("Executing cpu/ram capacity update");
            }

            // Calculate CPU and RAM capacities
            // 	get all hosts...even if they are not in 'UP' state
            List<HostVO> hosts = _resourceMgr.listAllNotInMaintenanceHostsInOneZone(Host.Type.Routing, null);
            for (HostVO host : hosts) {
                _capacityMgr.updateCapacityForHost(host);
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Done executing cpu/ram capacity update");
                s_logger.debug("Executing storage capacity update");
            }
            // Calculate storage pool capacity
            List<StoragePoolVO> storagePools = _storagePoolDao.listAll();
            for (StoragePoolVO pool : storagePools) {
                long disk = _capacityMgr.getAllocatedPoolCapacity(pool, null);
                if (pool.isShared()){
                    _storageMgr.createCapacityEntry(pool, Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED, disk);
                }else {
                    _storageMgr.createCapacityEntry(pool, Capacity.CAPACITY_TYPE_LOCAL_STORAGE, disk);
                }
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Done executing storage capacity update");
                s_logger.debug("Executing capacity updates for public ip and Vlans");
            }

            List<DataCenterVO> datacenters = _dcDao.listAll();
            for (DataCenterVO datacenter : datacenters) {
                long dcId = datacenter.getId();

                //NOTE
                //What happens if we have multiple vlans? Dashboard currently shows stats
                //with no filter based on a vlan
                //ideal way would be to remove out the vlan param, and filter only on dcId
                //implementing the same

                // Calculate new Public IP capacity for Virtual Network
                if (datacenter.getNetworkType() == NetworkType.Advanced){
                    createOrUpdateIpCapacity(dcId, null, CapacityVO.CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP, datacenter.getAllocationState());
                }

                // Calculate new Public IP capacity for Direct Attached Network
                createOrUpdateIpCapacity(dcId, null, CapacityVO.CAPACITY_TYPE_DIRECT_ATTACHED_PUBLIC_IP, datacenter.getAllocationState());

                if (datacenter.getNetworkType() == NetworkType.Advanced){
                    //Calculate VLAN's capacity
                    createOrUpdateVlanCapacity(dcId, datacenter.getAllocationState());
                }
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Done capacity updates for public ip and Vlans");
                s_logger.debug("Executing capacity updates for private ip");
            }

            // Calculate new Private IP capacity
            List<HostPodVO> pods = _podDao.listAll();
            for (HostPodVO pod : pods) {
                long podId = pod.getId();
                long dcId = pod.getDataCenterId();

                createOrUpdateIpCapacity(dcId, podId, CapacityVO.CAPACITY_TYPE_PRIVATE_IP, _configMgr.findPodAllocationState(pod));
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Done executing capacity updates for private ip");
                s_logger.debug("Done recalculating system capacity");
            }

        } catch (Throwable t) {
            s_logger.error("Caught exception in recalculating capacity", t);
        }
    }



    private void createOrUpdateVlanCapacity(long dcId, AllocationState capacityState) {

        SearchCriteria<CapacityVO> capacitySC = _capacityDao.createSearchCriteria();

        List<CapacityVO> capacities = _capacityDao.search(capacitySC, null);
        capacitySC = _capacityDao.createSearchCriteria();
        capacitySC.addAnd("dataCenterId", SearchCriteria.Op.EQ, dcId);
        capacitySC.addAnd("capacityType", SearchCriteria.Op.EQ, Capacity.CAPACITY_TYPE_VLAN);
        capacities = _capacityDao.search(capacitySC, null);

        int totalVlans = _dcDao.countZoneVlans(dcId, false);
        int allocatedVlans = _dcDao.countZoneVlans(dcId, true);

        if (capacities.size() == 0){
            CapacityVO newVlanCapacity = new CapacityVO(null, dcId, null, null, allocatedVlans, totalVlans, Capacity.CAPACITY_TYPE_VLAN);
            if (capacityState == AllocationState.Disabled){
                newVlanCapacity.setCapacityState(CapacityState.Disabled);
            }
            _capacityDao.persist(newVlanCapacity);
        }else if ( !(capacities.get(0).getUsedCapacity() == allocatedVlans
                && capacities.get(0).getTotalCapacity() == totalVlans) ){
            CapacityVO capacity = capacities.get(0);
            capacity.setUsedCapacity(allocatedVlans);
            capacity.setTotalCapacity(totalVlans);
            _capacityDao.update(capacity.getId(), capacity);
        }


    }

    public void createOrUpdateIpCapacity(Long dcId, Long podId, short capacityType, AllocationState capacityState){
        SearchCriteria<CapacityVO> capacitySC = _capacityDao.createSearchCriteria();

        List<CapacityVO> capacities = _capacityDao.search(capacitySC, null);
        capacitySC = _capacityDao.createSearchCriteria();
        capacitySC.addAnd("podId", SearchCriteria.Op.EQ, podId);
        capacitySC.addAnd("dataCenterId", SearchCriteria.Op.EQ, dcId);
        capacitySC.addAnd("capacityType", SearchCriteria.Op.EQ, capacityType);

        int totalIPs;
        int allocatedIPs;
        capacities = _capacityDao.search(capacitySC, null);
        if (capacityType == CapacityVO.CAPACITY_TYPE_PRIVATE_IP){
            totalIPs = _privateIPAddressDao.countIPs(podId, dcId, false);
            allocatedIPs = _privateIPAddressDao.countIPs(podId, dcId, true);
        }else if (capacityType == CapacityVO.CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP){
            totalIPs = _publicIPAddressDao.countIPsForNetwork(dcId, false, VlanType.VirtualNetwork);
            allocatedIPs = _publicIPAddressDao.countIPsForNetwork(dcId, true, VlanType.VirtualNetwork);
        }else {
            totalIPs = _publicIPAddressDao.countIPsForNetwork(dcId, false, VlanType.DirectAttached);
            allocatedIPs = _publicIPAddressDao.countIPsForNetwork(dcId, true, VlanType.DirectAttached);
        }

        if (capacities.size() == 0){
            CapacityVO newPublicIPCapacity = new CapacityVO(null, dcId, podId, null, allocatedIPs, totalIPs, capacityType);
            if (capacityState == AllocationState.Disabled){
                newPublicIPCapacity.setCapacityState(CapacityState.Disabled);
            }
            _capacityDao.persist(newPublicIPCapacity);
        }else if ( !(capacities.get(0).getUsedCapacity() == allocatedIPs
                && capacities.get(0).getTotalCapacity() == totalIPs) ){
            CapacityVO capacity = capacities.get(0);
            capacity.setUsedCapacity(allocatedIPs);
            capacity.setTotalCapacity(totalIPs);
            _capacityDao.update(capacity.getId(), capacity);
        }

    }

    class CapacityChecker extends ManagedContextTimerTask {
        @Override
        protected void runInContext() {
            try {
                s_logger.debug("Running Capacity Checker ... ");
                checkForAlerts();
                s_logger.debug("Done running Capacity Checker ... ");
            } catch (Throwable t) {
                s_logger.error("Exception in CapacityChecker", t);
            }
        }
    }

    public void checkForAlerts(){

        recalculateCapacity();

        // abort if we can't possibly send an alert...
        if (_emailAlert == null) {
            return;
        }

        //Get all datacenters, pods and clusters in the system.
        List<DataCenterVO> dataCenterList = _dcDao.listAll();
        List<ClusterVO> clusterList = _clusterDao.listAll();
        List<HostPodVO> podList = _podDao.listAll();
        //Get capacity types at different levels
        List<Short> dataCenterCapacityTypes = getCapacityTypesAtZoneLevel();
        List<Short> podCapacityTypes = getCapacityTypesAtPodLevel();
        List<Short> clusterCapacityTypes = getCapacityTypesAtClusterLevel();

        // Generate Alerts for Zone Level capacities
        for(DataCenterVO dc : dataCenterList){
            for (Short capacityType : dataCenterCapacityTypes){
                List<SummedCapacity> capacity = new ArrayList<SummedCapacity>();
                capacity = _capacityDao.findCapacityBy(capacityType.intValue(), dc.getId(), null, null);

                if (capacityType == Capacity.CAPACITY_TYPE_SECONDARY_STORAGE){
                    capacity.add(getUsedStats(capacityType, dc.getId(), null, null));
                }
                if (capacity == null || capacity.size() == 0){
                    continue;
                }
                double totalCapacity = capacity.get(0).getTotalCapacity();
                double usedCapacity =  capacity.get(0).getUsedCapacity();
                if (totalCapacity != 0 && usedCapacity/totalCapacity > _capacityTypeThresholdMap.get(capacityType)){
                    generateEmailAlert(dc, null, null, totalCapacity, usedCapacity, capacityType);
                }
            }
        }

        // Generate Alerts for Pod Level capacities
        for( HostPodVO pod : podList){
            for (Short capacityType : podCapacityTypes){
                List<SummedCapacity> capacity = _capacityDao.findCapacityBy(capacityType.intValue(), pod.getDataCenterId(), pod.getId(), null);
                if (capacity == null || capacity.size() == 0){
                    continue;
                }
                double totalCapacity = capacity.get(0).getTotalCapacity();
                double usedCapacity =  capacity.get(0).getUsedCapacity();
                if (totalCapacity != 0 && usedCapacity/totalCapacity > _capacityTypeThresholdMap.get(capacityType)){
                    generateEmailAlert(ApiDBUtils.findZoneById(pod.getDataCenterId()), pod, null,
                            totalCapacity, usedCapacity, capacityType);
                }
            }
        }

        // Generate Alerts for Cluster Level capacities
        for( ClusterVO cluster : clusterList){
            for (Short capacityType : clusterCapacityTypes){
                List<SummedCapacity> capacity = new ArrayList<SummedCapacity>();
                capacity = _capacityDao.findCapacityBy(capacityType.intValue(), cluster.getDataCenterId(), null, cluster.getId());

                // cpu and memory allocated capacity notification threshold can be defined at cluster level, so getting the value if they are defined at cluster level
                double threshold = 0;
                switch (capacityType) {
                    case Capacity.CAPACITY_TYPE_STORAGE:
                        capacity.add(getUsedStats(capacityType, cluster.getDataCenterId(), cluster.getPodId(), cluster.getId()));
                        threshold = StorageCapacityThreshold.valueIn(cluster.getId());
                        break;
                    case Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED:
                        threshold = StorageAllocatedCapacityThreshold.valueIn(cluster.getId());
                        break;
                    case Capacity.CAPACITY_TYPE_CPU:
                        threshold = CPUCapacityThreshold.valueIn(cluster.getId());
                        break;
                    case Capacity.CAPACITY_TYPE_MEMORY:
                        threshold = MemoryCapacityThreshold.valueIn(cluster.getId());
                        break;
                    default:
                        threshold = _capacityTypeThresholdMap.get(capacityType);
                }
                if (capacity == null || capacity.size() == 0){
                    continue;
                }

                double totalCapacity = capacity.get(0).getTotalCapacity();
                double usedCapacity =  capacity.get(0).getUsedCapacity() + capacity.get(0).getReservedCapacity();
                if (totalCapacity != 0 && usedCapacity/totalCapacity > threshold){
                    generateEmailAlert(ApiDBUtils.findZoneById(cluster.getDataCenterId()), ApiDBUtils.findPodById(cluster.getPodId()), cluster,
                            totalCapacity, usedCapacity, capacityType);
                }
            }
        }

    }

    private SummedCapacity getUsedStats(short capacityType, long zoneId, Long podId, Long clusterId){
        CapacityVO capacity;
        if (capacityType == Capacity.CAPACITY_TYPE_SECONDARY_STORAGE){
            capacity = _storageMgr.getSecondaryStorageUsedStats(null, zoneId);
        }else{
            capacity = _storageMgr.getStoragePoolUsedStats(null, clusterId, podId, zoneId);
        }
        if (capacity != null){
            return new SummedCapacity(capacity.getUsedCapacity(), 0, capacity.getTotalCapacity(), capacityType, clusterId, podId);
        }else{
            return null;
        }

    }

    private void generateEmailAlert(DataCenterVO dc, HostPodVO pod, ClusterVO cluster, double totalCapacity, double usedCapacity, short capacityType){

        String msgSubject = null;
        String msgContent = null;
        String totalStr;
        String usedStr;
        String pctStr = formatPercent(usedCapacity/totalCapacity);
        short alertType = -1;
        Long podId = pod == null ? null : pod.getId();
        Long clusterId = cluster == null ? null : cluster.getId();

        switch (capacityType) {

        //Cluster Level
        case CapacityVO.CAPACITY_TYPE_MEMORY:
            msgSubject = "System Alert: Low Available Memory in cluster " +cluster.getName()+ " pod " +pod.getName()+ " of availability zone " + dc.getName();
            totalStr = formatBytesToMegabytes(totalCapacity);
            usedStr = formatBytesToMegabytes(usedCapacity);
            msgContent = "System memory is low, total: " + totalStr + " MB, used: " + usedStr + " MB (" + pctStr + "%)";
            alertType = ALERT_TYPE_MEMORY;
            break;
        case CapacityVO.CAPACITY_TYPE_CPU:
            msgSubject = "System Alert: Low Unallocated CPU in cluster " +cluster.getName()+ " pod " +pod.getName()+ " of availability zone " + dc.getName();
            totalStr = _dfWhole.format(totalCapacity);
            usedStr = _dfWhole.format(usedCapacity);
            msgContent = "Unallocated CPU is low, total: " + totalStr + " Mhz, used: " + usedStr + " Mhz (" + pctStr + "%)";
            alertType = ALERT_TYPE_CPU;
            break;
        case CapacityVO.CAPACITY_TYPE_STORAGE:
            msgSubject = "System Alert: Low Available Storage in cluster " +cluster.getName()+ " pod " +pod.getName()+ " of availability zone " + dc.getName();
            totalStr = formatBytesToMegabytes(totalCapacity);
            usedStr = formatBytesToMegabytes(usedCapacity);
            msgContent = "Available storage space is low, total: " + totalStr + " MB, used: " + usedStr + " MB (" + pctStr + "%)";
            alertType = ALERT_TYPE_STORAGE;
            break;
        case CapacityVO.CAPACITY_TYPE_STORAGE_ALLOCATED:
            msgSubject = "System Alert: Remaining unallocated Storage is low in cluster " +cluster.getName()+ " pod " +pod.getName()+ " of availability zone " + dc.getName();
            totalStr = formatBytesToMegabytes(totalCapacity);
            usedStr = formatBytesToMegabytes(usedCapacity);
            msgContent = "Unallocated storage space is low, total: " + totalStr + " MB, allocated: " + usedStr + " MB (" + pctStr + "%)";
            alertType = ALERT_TYPE_STORAGE_ALLOCATED;
            break;
        case CapacityVO.CAPACITY_TYPE_LOCAL_STORAGE:
            msgSubject = "System Alert: Remaining unallocated Local Storage is low in cluster " +cluster.getName()+ " pod " +pod.getName()+ " of availability zone " + dc.getName();
            totalStr = formatBytesToMegabytes(totalCapacity);
            usedStr = formatBytesToMegabytes(usedCapacity);
            msgContent = "Unallocated storage space is low, total: " + totalStr + " MB, allocated: " + usedStr + " MB (" + pctStr + "%)";
            alertType = ALERT_TYPE_LOCAL_STORAGE;
            break;

            //Pod Level
        case CapacityVO.CAPACITY_TYPE_PRIVATE_IP:
            msgSubject = "System Alert: Number of unallocated private IPs is low in pod " +pod.getName()+ " of availability zone " + dc.getName();
            totalStr = Double.toString(totalCapacity);
            usedStr = Double.toString(usedCapacity);
            msgContent = "Number of unallocated private IPs is low, total: " + totalStr + ", allocated: " + usedStr + " (" + pctStr + "%)";
            alertType = ALERT_TYPE_PRIVATE_IP;
            break;

            //Zone Level
        case CapacityVO.CAPACITY_TYPE_SECONDARY_STORAGE:
            msgSubject = "System Alert: Low Available Secondary Storage in availability zone " + dc.getName();
            totalStr = formatBytesToMegabytes(totalCapacity);
            usedStr = formatBytesToMegabytes(usedCapacity);
            msgContent = "Available secondary storage space is low, total: " + totalStr + " MB, used: " + usedStr + " MB (" + pctStr + "%)";
            alertType = ALERT_TYPE_SECONDARY_STORAGE;
            break;
        case CapacityVO.CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP:
            msgSubject = "System Alert: Number of unallocated virtual network public IPs is low in availability zone " + dc.getName();
            totalStr = Double.toString(totalCapacity);
            usedStr = Double.toString(usedCapacity);
            msgContent = "Number of unallocated public IPs is low, total: " + totalStr + ", allocated: " + usedStr + " (" + pctStr + "%)";
            alertType = ALERT_TYPE_VIRTUAL_NETWORK_PUBLIC_IP;
            break;
        case CapacityVO.CAPACITY_TYPE_DIRECT_ATTACHED_PUBLIC_IP:
            msgSubject = "System Alert: Number of unallocated shared network IPs is low in availability zone " + dc.getName();
            totalStr = Double.toString(totalCapacity);
            usedStr = Double.toString(usedCapacity);
            msgContent = "Number of unallocated shared network IPs is low, total: " + totalStr + ", allocated: " + usedStr + " (" + pctStr + "%)";
            alertType = ALERT_TYPE_DIRECT_ATTACHED_PUBLIC_IP;
            break;
        case CapacityVO.CAPACITY_TYPE_VLAN:
            msgSubject = "System Alert: Number of unallocated VLANs is low in availability zone " + dc.getName();
            totalStr = Double.toString(totalCapacity);
            usedStr = Double.toString(usedCapacity);
            msgContent = "Number of unallocated VLANs is low, total: " + totalStr + ", allocated: " + usedStr + " (" + pctStr + "%)";
            alertType = ALERT_TYPE_VLAN;
            break;
        }

        try {
            if (s_logger.isDebugEnabled()){
                s_logger.debug(msgSubject);
                s_logger.debug(msgContent);
            }
            _emailAlert.sendAlert(alertType, dc.getId(), podId, clusterId, msgSubject, msgContent);
        } catch (Exception ex) {
            s_logger.error("Exception in CapacityChecker", ex);
        }
    }

    private List<Short> getCapacityTypesAtZoneLevel(){

        List<Short> dataCenterCapacityTypes = new ArrayList<Short>();
        dataCenterCapacityTypes.add(Capacity.CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP);
        dataCenterCapacityTypes.add(Capacity.CAPACITY_TYPE_DIRECT_ATTACHED_PUBLIC_IP);
        dataCenterCapacityTypes.add(Capacity.CAPACITY_TYPE_SECONDARY_STORAGE);
        dataCenterCapacityTypes.add(Capacity.CAPACITY_TYPE_VLAN);
        return dataCenterCapacityTypes;

    }

    private List<Short> getCapacityTypesAtPodLevel(){

        List<Short> podCapacityTypes = new ArrayList<Short>();
        podCapacityTypes.add(Capacity.CAPACITY_TYPE_PRIVATE_IP);
        return podCapacityTypes;

    }

    private List<Short> getCapacityTypesAtClusterLevel(){

        List<Short> clusterCapacityTypes = new ArrayList<Short>();
        clusterCapacityTypes.add(Capacity.CAPACITY_TYPE_CPU);
        clusterCapacityTypes.add(Capacity.CAPACITY_TYPE_MEMORY);
        clusterCapacityTypes.add(Capacity.CAPACITY_TYPE_STORAGE);
        clusterCapacityTypes.add(Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED);
        clusterCapacityTypes.add(Capacity.CAPACITY_TYPE_LOCAL_STORAGE);
        return clusterCapacityTypes;

    }

    class EmailAlert {
        private Session _smtpSession;
        private InternetAddress[] _recipientList;
        private final String _smtpHost;
        private int _smtpPort = -1;
        private boolean _smtpUseAuth = false;
        private final String _smtpUsername;
        private final String _smtpPassword;
        private final String _emailSender;

        public EmailAlert(String[] recipientList, String smtpHost, int smtpPort, boolean smtpUseAuth, final String smtpUsername, final String smtpPassword, String emailSender, boolean smtpDebug) {
            if (recipientList != null) {
                _recipientList = new InternetAddress[recipientList.length];
                for (int i = 0; i < recipientList.length; i++) {
                    try {
                        _recipientList[i] = new InternetAddress(recipientList[i], recipientList[i]);
                    } catch (Exception ex) {
                        s_logger.error("Exception creating address for: " + recipientList[i], ex);
                    }
                }
            }

            _smtpHost = smtpHost;
            _smtpPort = smtpPort;
            _smtpUseAuth = smtpUseAuth;
            _smtpUsername = smtpUsername;
            _smtpPassword = smtpPassword;
            _emailSender = emailSender;

            if (_smtpHost != null) {
                Properties smtpProps = new Properties();
                smtpProps.put("mail.smtp.host", smtpHost);
                smtpProps.put("mail.smtp.port", smtpPort);
                smtpProps.put("mail.smtp.auth", ""+smtpUseAuth);
                if (smtpUsername != null) {
                    smtpProps.put("mail.smtp.user", smtpUsername);
                }

                smtpProps.put("mail.smtps.host", smtpHost);
                smtpProps.put("mail.smtps.port", smtpPort);
                smtpProps.put("mail.smtps.auth", ""+smtpUseAuth);
                if (smtpUsername != null) {
                    smtpProps.put("mail.smtps.user", smtpUsername);
                }

                if ((smtpUsername != null) && (smtpPassword != null)) {
                    _smtpSession = Session.getInstance(smtpProps, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(smtpUsername, smtpPassword);
                        }
                    });
                } else {
                    _smtpSession = Session.getInstance(smtpProps);
                }
                _smtpSession.setDebug(smtpDebug);
            } else {
                _smtpSession = null;
            }
        }

        // TODO:  make sure this handles SSL transport (useAuth is true) and regular
        public void sendAlert(short alertType, long dataCenterId, Long podId, Long clusterId, String subject, String content) throws MessagingException, UnsupportedEncodingException {
            s_alertsLogger.warn(" alertType:: " + alertType + " // dataCenterId:: " + dataCenterId + " // podId:: " +
                podId + " // clusterId:: " + null + " // message:: " + subject);
            AlertVO alert = null;
            if ((alertType != AlertManager.ALERT_TYPE_HOST) &&
                    (alertType != AlertManager.ALERT_TYPE_USERVM) &&
                    (alertType != AlertManager.ALERT_TYPE_DOMAIN_ROUTER) &&
                    (alertType != AlertManager.ALERT_TYPE_CONSOLE_PROXY) &&
                    (alertType != AlertManager.ALERT_TYPE_SSVM) &&
                    (alertType != AlertManager.ALERT_TYPE_STORAGE_MISC) &&
                    (alertType != AlertManager.ALERT_TYPE_MANAGMENT_NODE) &&
                    (alertType != AlertManager.ALERT_TYPE_RESOURCE_LIMIT_EXCEEDED)) {
                alert = _alertDao.getLastAlert(alertType, dataCenterId, podId, clusterId);
            }

            if (alert == null) {
                // set up a new alert
                AlertVO newAlert = new AlertVO();
                newAlert.setType(alertType);
                newAlert.setSubject(subject);
                newAlert.setClusterId(clusterId);
                newAlert.setPodId(podId);
                newAlert.setDataCenterId(dataCenterId);
                newAlert.setSentCount(1); // initialize sent count to 1 since we are now sending an alert
                newAlert.setLastSent(new Date());
                _alertDao.persist(newAlert);
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Have already sent: " + alert.getSentCount() + " emails for alert type '" + alertType + "' -- skipping send email");
                }
                return;
            }

            if (_smtpSession != null) {
                SMTPMessage msg = new SMTPMessage(_smtpSession);
                msg.setSender(new InternetAddress(_emailSender, _emailSender));
                msg.setFrom(new InternetAddress(_emailSender, _emailSender));
                for (InternetAddress address : _recipientList) {
                    msg.addRecipient(RecipientType.TO, address);
                }
                msg.setSubject(subject);
                msg.setSentDate(new Date());
                msg.setContent(content, "text/plain");
                msg.saveChanges();

                SMTPTransport smtpTrans = null;
                if (_smtpUseAuth) {
                    smtpTrans = new SMTPSSLTransport(_smtpSession, new URLName("smtp", _smtpHost, _smtpPort, null, _smtpUsername, _smtpPassword));
                } else {
                    smtpTrans = new SMTPTransport(_smtpSession, new URLName("smtp", _smtpHost, _smtpPort, null, _smtpUsername, _smtpPassword));
                }
                smtpTrans.connect();
                smtpTrans.sendMessage(msg, msg.getAllRecipients());
                smtpTrans.close();
            }
        }

        public void clearAlert(short alertType, long dataCenterId, Long podId) {
            if (alertType != -1) {
                AlertVO alert = _alertDao.getLastAlert(alertType, dataCenterId, podId, null);
                if (alert != null) {
                    AlertVO updatedAlert = _alertDao.createForUpdate();
                    updatedAlert.setResolved(new Date());
                    _alertDao.update(alert.getId(), updatedAlert);
                }
            }
        }
    }

    private static String formatPercent(double percentage) {
        return _dfPct.format(percentage*100);
    }

    private static String formatBytesToMegabytes(double bytes) {
        double megaBytes = (bytes / (1024 * 1024));
        return _dfWhole.format(megaBytes);
    }

    @Override
    public String getConfigComponentName() {
        return AlertManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {CPUCapacityThreshold, MemoryCapacityThreshold, StorageAllocatedCapacityThreshold, StorageCapacityThreshold};
    }
}
