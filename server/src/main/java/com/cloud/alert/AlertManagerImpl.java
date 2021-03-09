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
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;

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
import com.cloud.event.ActionEvent;
import com.cloud.event.AlertGenerator;
import com.cloud.event.EventTypes;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.SearchCriteria;
import java.util.HashSet;
import java.util.Set;
import org.apache.cloudstack.utils.mailing.MailAddress;
import org.apache.cloudstack.utils.mailing.SMTPMailProperties;
import org.apache.cloudstack.utils.mailing.SMTPMailSender;
import org.apache.commons.lang3.math.NumberUtils;

public class AlertManagerImpl extends ManagerBase implements AlertManager, Configurable {
    private static final Logger s_logger = Logger.getLogger(AlertManagerImpl.class.getName());

    private static final long INITIAL_CAPACITY_CHECK_DELAY = 30L * 1000L; // Thirty seconds expressed in milliseconds.

    private static final DecimalFormat DfPct = new DecimalFormat("###.##");
    private static final DecimalFormat DfWhole = new DecimalFormat("########");

    @Inject
    private AlertDao _alertDao;
    @Inject
    protected StorageManager _storageMgr;
    @Inject
    protected CapacityManager _capacityMgr;
    @Inject
    private CapacityDao _capacityDao;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private HostPodDao _podDao;
    @Inject
    private ClusterDao _clusterDao;
    @Inject
    private IPAddressDao _publicIPAddressDao;
    @Inject
    private DataCenterIpAddressDao _privateIPAddressDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private ConfigurationManager _configMgr;
    @Inject
    protected ConfigDepot _configDepot;
    @Inject
    ServiceOfferingDao _offeringsDao;

    private Timer _timer = null;
    private long _capacityCheckPeriod = 60L * 60L * 1000L; // One hour by default.
    private double _publicIPCapacityThreshold = 0.75;
    private double _privateIPCapacityThreshold = 0.75;
    private double _secondaryStorageCapacityThreshold = 0.75;
    private double _vlanCapacityThreshold = 0.75;
    private double _directNetworkPublicIpCapacityThreshold = 0.75;
    private double _localStorageCapacityThreshold = 0.75;
    Map<Short, Double> _capacityTypeThresholdMap = new HashMap<Short, Double>();

    private final ExecutorService _executor;

    protected SMTPMailSender mailSender;
    protected String[] recipients = null;
    protected String senderAddress = null;

    public AlertManagerImpl() {
        _executor = Executors.newCachedThreadPool(new NamedThreadFactory("Email-Alerts-Sender"));
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        Map<String, String> configs = _configDao.getConfiguration("management-server", params);

        // set up the email system for alerts
        String emailAddressList = configs.get("alert.email.addresses");
        if (emailAddressList != null) {
            recipients = emailAddressList.split(",");
        }

        senderAddress = configs.get("alert.email.sender");

        String namespace = "alert.smtp";
        String timeoutConfig = String.format("%s.timeout", namespace);
        String connectionTimeoutConfig = String.format("%s.connectiontimeout", namespace);

        int smtpTimeout = NumberUtils.toInt(configs.get(timeoutConfig), 30000);
        int smtpConnectionTimeout = NumberUtils.toInt(configs.get(connectionTimeoutConfig), 30000);

        configs.put(timeoutConfig, String.valueOf(smtpTimeout));
        configs.put(connectionTimeoutConfig, String.valueOf(smtpConnectionTimeout));

        mailSender = new SMTPMailSender(configs, namespace);

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
            if (_capacityCheckPeriod <= 0) {
                _capacityCheckPeriod = Long.parseLong(Config.CapacityCheckPeriod.getDefaultValue());
            }
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
    public void clearAlert(AlertType alertType, long dataCenterId, long podId) {
        try {
            clearAlert(alertType.getType(), dataCenterId, podId);
        } catch (Exception ex) {
            s_logger.error("Problem clearing email alert", ex);
        }
    }

    @Override
    public void sendAlert(AlertType alertType, long dataCenterId, Long podId, String subject, String body) {

        // publish alert
        AlertGenerator.publishAlertOnEventBus(alertType.getName(), dataCenterId, podId, subject, body);

        // TODO:  queue up these messages and send them as one set of issues once a certain number of issues is reached?  If that's the case,
        //         shouldn't we have a type/severity as part of the API so that severe errors get sent right away?
        try {
            if (mailSender != null) {
                sendAlert(alertType, dataCenterId, podId, null, subject, body);
            } else {
                s_logger.warn("AlertType:: " + alertType + " | dataCenterId:: " + dataCenterId + " | podId:: " + podId +
                        " | message:: " + subject + " | body:: " + body);
            }
        } catch (Exception ex) {
            s_logger.error("Problem sending email alert", ex);
        }
    }

    @Override
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
            //     get all hosts...even if they are not in 'UP' state
            List<HostVO> hosts = _resourceMgr.listAllNotInMaintenanceHostsInOneZone(Host.Type.Routing, null);
            if (hosts != null) {
                // prepare the service offerings
                List<ServiceOfferingVO> offerings = _offeringsDao.listAllIncludingRemoved();
                Map<Long, ServiceOfferingVO> offeringsMap = new HashMap<Long, ServiceOfferingVO>();
                for (ServiceOfferingVO offering : offerings) {
                    offeringsMap.put(offering.getId(), offering);
                }
                for (HostVO host : hosts) {
                    _capacityMgr.updateCapacityForHost(host, offeringsMap);
                }
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Done executing cpu/ram capacity update");
                s_logger.debug("Executing storage capacity update");
            }
            // Calculate storage pool capacity
            List<StoragePoolVO> storagePools = _storagePoolDao.listAll();
            for (StoragePoolVO pool : storagePools) {
                long disk = _capacityMgr.getAllocatedPoolCapacity(pool, null);
                if (pool.isShared()) {
                    _storageMgr.createCapacityEntry(pool, Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED, disk);
                } else {
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
                if (datacenter.getNetworkType() == NetworkType.Advanced) {
                    createOrUpdateIpCapacity(dcId, null, Capacity.CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP, datacenter.getAllocationState());
                }

                // Calculate new Public IP capacity for Direct Attached Network
                createOrUpdateIpCapacity(dcId, null, Capacity.CAPACITY_TYPE_DIRECT_ATTACHED_PUBLIC_IP, datacenter.getAllocationState());

                if (datacenter.getNetworkType() == NetworkType.Advanced) {
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

                createOrUpdateIpCapacity(dcId, podId, Capacity.CAPACITY_TYPE_PRIVATE_IP, _configMgr.findPodAllocationState(pod));
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

        CapacityState vlanCapacityState = (capacityState == AllocationState.Disabled) ? CapacityState.Disabled : CapacityState.Enabled;
        if (capacities.size() == 0) {
            CapacityVO newVlanCapacity = new CapacityVO(null, dcId, null, null, allocatedVlans, totalVlans, Capacity.CAPACITY_TYPE_VLAN);
            newVlanCapacity.setCapacityState(vlanCapacityState);
            _capacityDao.persist(newVlanCapacity);
        } else if (!(capacities.get(0).getUsedCapacity() == allocatedVlans && capacities.get(0).getTotalCapacity() == totalVlans
                && capacities.get(0).getCapacityState() == vlanCapacityState)) {
            CapacityVO capacity = capacities.get(0);
            capacity.setUsedCapacity(allocatedVlans);
            capacity.setTotalCapacity(totalVlans);
            capacity.setCapacityState(vlanCapacityState);
            _capacityDao.update(capacity.getId(), capacity);
        }

    }

    public void createOrUpdateIpCapacity(Long dcId, Long podId, short capacityType, AllocationState capacityState) {
        SearchCriteria<CapacityVO> capacitySC = _capacityDao.createSearchCriteria();

        List<CapacityVO> capacities = _capacityDao.search(capacitySC, null);
        capacitySC = _capacityDao.createSearchCriteria();
        capacitySC.addAnd("podId", SearchCriteria.Op.EQ, podId);
        capacitySC.addAnd("dataCenterId", SearchCriteria.Op.EQ, dcId);
        capacitySC.addAnd("capacityType", SearchCriteria.Op.EQ, capacityType);

        int totalIPs;
        int allocatedIPs;
        capacities = _capacityDao.search(capacitySC, null);
        if (capacityType == Capacity.CAPACITY_TYPE_PRIVATE_IP) {
            totalIPs = _privateIPAddressDao.countIPs(podId, dcId, false);
            allocatedIPs = _privateIPAddressDao.countIPs(podId, dcId, true);
        } else if (capacityType == Capacity.CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP) {
            totalIPs = _publicIPAddressDao.countIPsForNetwork(dcId, false, VlanType.VirtualNetwork);
            allocatedIPs = _publicIPAddressDao.countIPsForNetwork(dcId, true, VlanType.VirtualNetwork);
        } else {
            totalIPs = _publicIPAddressDao.countIPsForNetwork(dcId, false, VlanType.DirectAttached);
            allocatedIPs = _publicIPAddressDao.countIPsForNetwork(dcId, true, VlanType.DirectAttached);
        }

        CapacityState ipCapacityState = (capacityState == AllocationState.Disabled) ? CapacityState.Disabled : CapacityState.Enabled;
        if (capacities.size() == 0) {
            CapacityVO newPublicIPCapacity = new CapacityVO(null, dcId, podId, null, allocatedIPs, totalIPs, capacityType);
            newPublicIPCapacity.setCapacityState(ipCapacityState);
            _capacityDao.persist(newPublicIPCapacity);
        } else if (!(capacities.get(0).getUsedCapacity() == allocatedIPs && capacities.get(0).getTotalCapacity() == totalIPs
                && capacities.get(0).getCapacityState() == ipCapacityState)) {
            CapacityVO capacity = capacities.get(0);
            capacity.setUsedCapacity(allocatedIPs);
            capacity.setTotalCapacity(totalIPs);
            capacity.setCapacityState(ipCapacityState);
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

    public void checkForAlerts() {

        recalculateCapacity();

        if (mailSender == null) {
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
        for (DataCenterVO dc : dataCenterList) {
            for (Short capacityType : dataCenterCapacityTypes) {
                List<SummedCapacity> capacity = new ArrayList<SummedCapacity>();
                capacity = _capacityDao.findCapacityBy(capacityType.intValue(), dc.getId(), null, null);

                if (capacityType == Capacity.CAPACITY_TYPE_SECONDARY_STORAGE) {
                    capacity.add(getUsedStats(capacityType, dc.getId(), null, null));
                }
                if (capacity == null || capacity.size() == 0) {
                    continue;
                }
                double totalCapacity = capacity.get(0).getTotalCapacity();
                double usedCapacity = capacity.get(0).getUsedCapacity();
                if (totalCapacity != 0 && usedCapacity / totalCapacity > _capacityTypeThresholdMap.get(capacityType)) {
                    generateEmailAlert(dc, null, null, totalCapacity, usedCapacity, capacityType);
                }
            }
        }

        // Generate Alerts for Pod Level capacities
        for (HostPodVO pod : podList) {
            for (Short capacityType : podCapacityTypes) {
                List<SummedCapacity> capacity = _capacityDao.findCapacityBy(capacityType.intValue(), pod.getDataCenterId(), pod.getId(), null);
                if (capacity == null || capacity.size() == 0) {
                    continue;
                }
                double totalCapacity = capacity.get(0).getTotalCapacity();
                double usedCapacity = capacity.get(0).getUsedCapacity();
                if (totalCapacity != 0 && usedCapacity / totalCapacity > _capacityTypeThresholdMap.get(capacityType)) {
                    generateEmailAlert(ApiDBUtils.findZoneById(pod.getDataCenterId()), pod, null, totalCapacity, usedCapacity, capacityType);
                }
            }
        }

        // Generate Alerts for Cluster Level capacities
        for (ClusterVO cluster : clusterList) {
            for (Short capacityType : clusterCapacityTypes) {
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
                if (capacity == null || capacity.size() == 0) {
                    continue;
                }

                double totalCapacity = capacity.get(0).getTotalCapacity();
                double usedCapacity = capacity.get(0).getUsedCapacity() + capacity.get(0).getReservedCapacity();
                if (totalCapacity != 0 && usedCapacity / totalCapacity > threshold) {
                    generateEmailAlert(ApiDBUtils.findZoneById(cluster.getDataCenterId()), ApiDBUtils.findPodById(cluster.getPodId()), cluster, totalCapacity,
                            usedCapacity, capacityType);
                }
            }
        }

    }

    private SummedCapacity getUsedStats(short capacityType, long zoneId, Long podId, Long clusterId) {
        CapacityVO capacity;
        if (capacityType == Capacity.CAPACITY_TYPE_SECONDARY_STORAGE) {
            capacity = _storageMgr.getSecondaryStorageUsedStats(null, zoneId);
        } else {
            capacity = _storageMgr.getStoragePoolUsedStats(null, clusterId, podId, zoneId);
        }
        if (capacity != null) {
            return new SummedCapacity(capacity.getUsedCapacity(), 0, capacity.getTotalCapacity(), capacityType, clusterId, podId);
        } else {
            return null;
        }

    }

    private void generateEmailAlert(DataCenterVO dc, HostPodVO pod, ClusterVO cluster, double totalCapacity, double usedCapacity, short capacityType) {

        String msgSubject = null;
        String msgContent = null;
        String totalStr;
        String usedStr;
        String pctStr = formatPercent(usedCapacity / totalCapacity);
        AlertType alertType = null;
        Long podId = pod == null ? null : pod.getId();
        Long clusterId = cluster == null ? null : cluster.getId();

        switch (capacityType) {

        //Cluster Level
        case Capacity.CAPACITY_TYPE_MEMORY:
            msgSubject = "System Alert: Low Available Memory in cluster " + cluster.getName() + " pod " + pod.getName() + " of availability zone " + dc.getName();
            totalStr = formatBytesToMegabytes(totalCapacity);
            usedStr = formatBytesToMegabytes(usedCapacity);
            msgContent = "System memory is low, total: " + totalStr + " MB, used: " + usedStr + " MB (" + pctStr + "%)";
            alertType = AlertManager.AlertType.ALERT_TYPE_MEMORY;
            break;
        case Capacity.CAPACITY_TYPE_CPU:
            msgSubject = "System Alert: Low Unallocated CPU in cluster " + cluster.getName() + " pod " + pod.getName() + " of availability zone " + dc.getName();
            totalStr = DfWhole.format(totalCapacity);
            usedStr = DfWhole.format(usedCapacity);
            msgContent = "Unallocated CPU is low, total: " + totalStr + " Mhz, used: " + usedStr + " Mhz (" + pctStr + "%)";
            alertType = AlertManager.AlertType.ALERT_TYPE_CPU;
            break;
        case Capacity.CAPACITY_TYPE_STORAGE:
            msgSubject = "System Alert: Low Available Storage in cluster " + cluster.getName() + " pod " + pod.getName() + " of availability zone " + dc.getName();
            totalStr = formatBytesToMegabytes(totalCapacity);
            usedStr = formatBytesToMegabytes(usedCapacity);
            msgContent = "Available storage space is low, total: " + totalStr + " MB, used: " + usedStr + " MB (" + pctStr + "%)";
            alertType = AlertManager.AlertType.ALERT_TYPE_STORAGE;
            break;
        case Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED:
            msgSubject = "System Alert: Remaining unallocated Storage is low in cluster " + cluster.getName() + " pod " + pod.getName() + " of availability zone " +
                    dc.getName();
            totalStr = formatBytesToMegabytes(totalCapacity);
            usedStr = formatBytesToMegabytes(usedCapacity);
            msgContent = "Unallocated storage space is low, total: " + totalStr + " MB, allocated: " + usedStr + " MB (" + pctStr + "%)";
            alertType = AlertManager.AlertType.ALERT_TYPE_STORAGE_ALLOCATED;
            break;
        case Capacity.CAPACITY_TYPE_LOCAL_STORAGE:
            msgSubject = "System Alert: Remaining unallocated Local Storage is low in cluster " + cluster.getName() + " pod " + pod.getName() + " of availability zone " +
                    dc.getName();
            totalStr = formatBytesToMegabytes(totalCapacity);
            usedStr = formatBytesToMegabytes(usedCapacity);
            msgContent = "Unallocated storage space is low, total: " + totalStr + " MB, allocated: " + usedStr + " MB (" + pctStr + "%)";
            alertType = AlertManager.AlertType.ALERT_TYPE_LOCAL_STORAGE;
            break;

            //Pod Level
        case Capacity.CAPACITY_TYPE_PRIVATE_IP:
            msgSubject = "System Alert: Number of unallocated private IPs is low in pod " + pod.getName() + " of availability zone " + dc.getName();
            totalStr = Double.toString(totalCapacity);
            usedStr = Double.toString(usedCapacity);
            msgContent = "Number of unallocated private IPs is low, total: " + totalStr + ", allocated: " + usedStr + " (" + pctStr + "%)";
            alertType = AlertManager.AlertType.ALERT_TYPE_PRIVATE_IP;
            break;

            //Zone Level
        case Capacity.CAPACITY_TYPE_SECONDARY_STORAGE:
            msgSubject = "System Alert: Low Available Secondary Storage in availability zone " + dc.getName();
            totalStr = formatBytesToMegabytes(totalCapacity);
            usedStr = formatBytesToMegabytes(usedCapacity);
            msgContent = "Available secondary storage space is low, total: " + totalStr + " MB, used: " + usedStr + " MB (" + pctStr + "%)";
            alertType = AlertManager.AlertType.ALERT_TYPE_SECONDARY_STORAGE;
            break;
        case Capacity.CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP:
            msgSubject = "System Alert: Number of unallocated virtual network public IPs is low in availability zone " + dc.getName();
            totalStr = Double.toString(totalCapacity);
            usedStr = Double.toString(usedCapacity);
            msgContent = "Number of unallocated public IPs is low, total: " + totalStr + ", allocated: " + usedStr + " (" + pctStr + "%)";
            alertType = AlertManager.AlertType.ALERT_TYPE_VIRTUAL_NETWORK_PUBLIC_IP;
            break;
        case Capacity.CAPACITY_TYPE_DIRECT_ATTACHED_PUBLIC_IP:
            msgSubject = "System Alert: Number of unallocated shared network IPs is low in availability zone " + dc.getName();
            totalStr = Double.toString(totalCapacity);
            usedStr = Double.toString(usedCapacity);
            msgContent = "Number of unallocated shared network IPs is low, total: " + totalStr + ", allocated: " + usedStr + " (" + pctStr + "%)";
            alertType = AlertManager.AlertType.ALERT_TYPE_DIRECT_ATTACHED_PUBLIC_IP;
            break;
        case Capacity.CAPACITY_TYPE_VLAN:
            msgSubject = "System Alert: Number of unallocated VLANs is low in availability zone " + dc.getName();
            totalStr = Double.toString(totalCapacity);
            usedStr = Double.toString(usedCapacity);
            msgContent = "Number of unallocated VLANs is low, total: " + totalStr + ", allocated: " + usedStr + " (" + pctStr + "%)";
            alertType = AlertManager.AlertType.ALERT_TYPE_VLAN;
            break;
        }

        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(msgSubject);
                s_logger.debug(msgContent);
            }
            sendAlert(alertType, dc.getId(), podId, clusterId, msgSubject, msgContent);
        } catch (Exception ex) {
            s_logger.error("Exception in CapacityChecker", ex);
        }
    }

    private List<Short> getCapacityTypesAtZoneLevel() {

        List<Short> dataCenterCapacityTypes = new ArrayList<Short>();
        dataCenterCapacityTypes.add(Capacity.CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP);
        dataCenterCapacityTypes.add(Capacity.CAPACITY_TYPE_DIRECT_ATTACHED_PUBLIC_IP);
        dataCenterCapacityTypes.add(Capacity.CAPACITY_TYPE_SECONDARY_STORAGE);
        dataCenterCapacityTypes.add(Capacity.CAPACITY_TYPE_VLAN);
        return dataCenterCapacityTypes;

    }

    private List<Short> getCapacityTypesAtPodLevel() {

        List<Short> podCapacityTypes = new ArrayList<Short>();
        podCapacityTypes.add(Capacity.CAPACITY_TYPE_PRIVATE_IP);
        return podCapacityTypes;

    }

    private List<Short> getCapacityTypesAtClusterLevel() {

        List<Short> clusterCapacityTypes = new ArrayList<Short>();
        clusterCapacityTypes.add(Capacity.CAPACITY_TYPE_CPU);
        clusterCapacityTypes.add(Capacity.CAPACITY_TYPE_MEMORY);
        clusterCapacityTypes.add(Capacity.CAPACITY_TYPE_STORAGE);
        clusterCapacityTypes.add(Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED);
        clusterCapacityTypes.add(Capacity.CAPACITY_TYPE_LOCAL_STORAGE);
        return clusterCapacityTypes;

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

    public void sendAlert(AlertType alertType, long dataCenterId, Long podId, Long clusterId, String subject, String content)
            throws MessagingException, UnsupportedEncodingException {
        s_logger.warn(String.format("alertType=[%s] dataCenterId=[%s] podId=[%s] clusterId=[%s] message=[%s].", alertType, dataCenterId, podId, clusterId, subject));
        AlertVO alert = null;
        if ((alertType != AlertManager.AlertType.ALERT_TYPE_HOST) && (alertType != AlertManager.AlertType.ALERT_TYPE_USERVM)
                && (alertType != AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER) && (alertType != AlertManager.AlertType.ALERT_TYPE_CONSOLE_PROXY)
                && (alertType != AlertManager.AlertType.ALERT_TYPE_SSVM) && (alertType != AlertManager.AlertType.ALERT_TYPE_STORAGE_MISC)
                && (alertType != AlertManager.AlertType.ALERT_TYPE_MANAGMENT_NODE) && (alertType != AlertManager.AlertType.ALERT_TYPE_RESOURCE_LIMIT_EXCEEDED)
                && (alertType != AlertManager.AlertType.ALERT_TYPE_UPLOAD_FAILED) && (alertType != AlertManager.AlertType.ALERT_TYPE_OOBM_AUTH_ERROR)
                && (alertType != AlertManager.AlertType.ALERT_TYPE_HA_ACTION) && (alertType != AlertManager.AlertType.ALERT_TYPE_CA_CERT)) {
            alert = _alertDao.getLastAlert(alertType.getType(), dataCenterId, podId, clusterId);
        }

        if (alert == null) {
            AlertVO newAlert = new AlertVO();
            newAlert.setType(alertType.getType());
            newAlert.setSubject(subject);
            newAlert.setContent(content);
            newAlert.setClusterId(clusterId);
            newAlert.setPodId(podId);
            newAlert.setDataCenterId(dataCenterId);
            newAlert.setSentCount(1);
            newAlert.setLastSent(new Date());
            newAlert.setName(alertType.getName());
            _alertDao.persist(newAlert);
        } else {
            if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Have already sent: " + alert.getSentCount() + " emails for alert type '" + alertType + "' -- skipping send email");
                }
            return;
        }

        if (recipients == null) {
            s_logger.warn(String.format("No recipients set in 'alert.email.addresses', skipping sending alert with subject: %s and content: %s", subject, content));
            return;
        }

        SMTPMailProperties mailProps = new SMTPMailProperties();
        mailProps.setSender(new MailAddress(senderAddress));
        mailProps.setSubject(subject);
        mailProps.setContent(content);
        mailProps.setContentType("text/plain");

        Set<MailAddress> addresses = new HashSet<>();
        for (String recipient : recipients) {
            addresses.add(new MailAddress(recipient));
        }

        mailProps.setRecipients(addresses);

        sendMessage(mailProps);

    }

    private void sendMessage(SMTPMailProperties mailProps) {
        _executor.execute(new Runnable() {
            @Override
            public void run() {
                mailSender.sendMail(mailProps);
            }
        });
    }

    private static String formatPercent(double percentage) {
        return DfPct.format(percentage * 100);
    }

    private static String formatBytesToMegabytes(double bytes) {
        double megaBytes = (bytes / (1024 * 1024));
        return DfWhole.format(megaBytes);
    }

    @Override
    public String getConfigComponentName() {
        return AlertManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {CPUCapacityThreshold, MemoryCapacityThreshold, StorageAllocatedCapacityThreshold, StorageCapacityThreshold};
    }

    @Override
    @ActionEvent(eventType = EventTypes.ALERT_GENERATE, eventDescription = "generating alert", async = true)
    public boolean generateAlert(AlertType alertType, long dataCenterId, Long podId, String msg) {
        try {
            sendAlert(alertType, dataCenterId, podId, msg, msg);
            return true;
        } catch (Exception ex) {
            s_logger.warn("Failed to generate an alert of type=" + alertType + "; msg=" + msg);
            return false;
        }
    }
}
