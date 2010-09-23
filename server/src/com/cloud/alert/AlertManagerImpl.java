/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.alert.dao.AlertDao;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDaoImpl;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.service.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.sun.mail.smtp.SMTPMessage;
import com.sun.mail.smtp.SMTPSSLTransport;
import com.sun.mail.smtp.SMTPTransport;

@Local(value={AlertManager.class})
public class AlertManagerImpl implements AlertManager {
    private static final Logger s_logger = Logger.getLogger(AlertManagerImpl.class.getName());

    private static final long INITIAL_DELAY = 5L * 60L * 1000L; // five minutes expressed in milliseconds

    private static final DecimalFormat _dfPct = new DecimalFormat("###.##");
    private static final DecimalFormat _dfWhole = new DecimalFormat("########");

    private String _name = null;
    private EmailAlert _emailAlert;
    private AlertDao _alertDao;
    private HostDao _hostDao;
    @Inject protected StorageManager _storageMgr;
    private ServiceOfferingDao _offeringsDao;
    private CapacityDao _capacityDao;
    private VMInstanceDao _vmDao;
    private DomainRouterDao _routerDao;
    private ConsoleProxyDao _consoleProxyDao;
    private SecondaryStorageVmDao _secStorgaeVmDao;
    private UserVmDao _userVmDao;
    private DataCenterDao _dcDao;
    private HostPodDao _podDao;
    private VolumeDao _volumeDao;
    private IPAddressDao _publicIPAddressDao;
    private DataCenterIpAddressDaoImpl _privateIPAddressDao;
    private StoragePoolDao _storagePoolDao;
    
    private Timer _timer = null;
    private float _cpuOverProvisioningFactor = 1;
    private long _capacityCheckPeriod = 60L * 60L * 1000L; // one hour by default
    private double _memoryCapacityThreshold = 0.75;
    private double _cpuCapacityThreshold = 0.75;
    private double _storageCapacityThreshold = 0.75;
    private double _storageAllocCapacityThreshold = 0.75;
    private double _publicIPCapacityThreshold = 0.75;
    private double _privateIPCapacityThreshold = 0.75;

    private int _routerRamSize;
    private int _proxyRamSize;
    private final GlobalLock m_capacityCheckLock = GlobalLock.getInternLock("capacity.check");

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao == null) {
            s_logger.error("Unable to get the configuration dao.");
            return false;
        }

        Map<String, String> configs = configDao.getConfiguration("management-server", params);

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

        String storageCapacityThreshold = configs.get("storage.capacity.threshold");
        String cpuCapacityThreshold = configs.get("cpu.capacity.threshold");
        String memoryCapacityThreshold = configs.get("memory.capacity.threshold");
        String storageAllocCapacityThreshold = configs.get("storage.allocated.capacity.threshold");
        String publicIPCapacityThreshold = configs.get("public.ip.capacity.threshold");
        String privateIPCapacityThreshold = configs.get("private.ip.capacity.threshold");
        
        if (storageCapacityThreshold != null) {
            _storageCapacityThreshold = Double.parseDouble(storageCapacityThreshold);
        }
        if (storageAllocCapacityThreshold != null) {
            _storageAllocCapacityThreshold = Double.parseDouble(storageAllocCapacityThreshold);
        }
        if (cpuCapacityThreshold != null) {
            _cpuCapacityThreshold = Double.parseDouble(cpuCapacityThreshold);
        }
        if (memoryCapacityThreshold != null) {
            _memoryCapacityThreshold = Double.parseDouble(memoryCapacityThreshold);
        }
        if (publicIPCapacityThreshold != null) {
        	_publicIPCapacityThreshold = Double.parseDouble(publicIPCapacityThreshold);
        }
        if (privateIPCapacityThreshold != null) {
        	_privateIPCapacityThreshold = Double.parseDouble(privateIPCapacityThreshold);
        }

        _hostDao = locator.getDao(HostDao.class);
        if (_hostDao == null) {
            s_logger.error("Unable to get the host dao.");
            return false;
        }

        _vmDao = locator.getDao(VMInstanceDao.class);
        if (_vmDao == null) {
            s_logger.error("Unable to get the VM Instance dao.");
            return false;
        }
        _routerDao = locator.getDao(DomainRouterDao.class);
        _consoleProxyDao = locator.getDao(ConsoleProxyDao.class);
        _secStorgaeVmDao = locator.getDao(SecondaryStorageVmDao.class);
        
        _userVmDao = locator.getDao(UserVmDao.class);
        if (_userVmDao == null) {
            s_logger.error("Unable to get the UserVm dao.");
            return false;
        }

        _offeringsDao = locator.getDao(ServiceOfferingDao.class);
        if (_offeringsDao == null) {
            s_logger.error("Unable to get the ServiceOffering dao.");
            return false;
        }

        _capacityDao = locator.getDao(CapacityDao.class);
        if (_capacityDao == null) {
            s_logger.error("Unable to get the capacity dao.");
            return false;
        }

        _alertDao = locator.getDao(AlertDao.class);
        if (_alertDao == null) {
            s_logger.error("Unable to get the alert dao.");
            return false;
        }


        _dcDao = locator.getDao(DataCenterDao.class);
        if (_dcDao == null) {
            s_logger.error("Unable to get the DataCenter dao.");
            return false;
        }

        _podDao = locator.getDao(HostPodDao.class);
        if (_podDao == null) {
            s_logger.error("Unable to get the Pod dao.");
            return false;
        }

        _volumeDao = locator.getDao(VolumeDao.class);
        if (_volumeDao == null) {
            s_logger.error("Unable to get the Volume dao.");
            return false;
        }
        
        _publicIPAddressDao = locator.getDao(IPAddressDao.class);
        if (_publicIPAddressDao == null) {
            throw new ConfigurationException("Unable to get " + IPAddressDao.class.getName());
        }
        
        _privateIPAddressDao = locator.getDao(DataCenterIpAddressDaoImpl.class);
        if (_privateIPAddressDao == null) {
            throw new ConfigurationException("Unable to get " + DataCenterIpAddressDaoImpl.class.getName());
        }
        
        _storagePoolDao = locator.getDao(StoragePoolDao.class);
        if (_storagePoolDao == null) {
            throw new ConfigurationException("Unable to get " + StoragePoolDao.class.getName());
        }

        String capacityCheckPeriodStr = configs.get("capacity.check.period");
        if (capacityCheckPeriodStr != null) {
            _capacityCheckPeriod = Long.parseLong(capacityCheckPeriodStr);
        }
        
        String cpuOverProvisioningFactorStr = configs.get("cpu.overprovisioning.factor");
        if (cpuOverProvisioningFactorStr != null) {
            _cpuOverProvisioningFactor = NumbersUtil.parseFloat(cpuOverProvisioningFactorStr,1);
            if(_cpuOverProvisioningFactor < 1){
            	_cpuOverProvisioningFactor = 1;
            }
        }

        _routerRamSize = NumbersUtil.parseInt(configs.get("router.ram.size"), 128);
        _proxyRamSize = NumbersUtil.parseInt(configs.get("consoleproxy.ram.size"), ConsoleProxyManager.DEFAULT_PROXY_VM_RAMSIZE);

        _timer = new Timer("CapacityChecker");

        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        _timer.schedule(new CapacityChecker(), INITIAL_DELAY, _capacityCheckPeriod);
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
        // TODO:  queue up these messages and send them as one set of issues once a certain number of issues is reached?  If that's the case,
        //         shouldn't we have a type/severity as part of the API so that severe errors get sent right away?
        try {
            if (_emailAlert != null) {
                _emailAlert.sendAlert(alertType, dataCenterId, podId, subject, body);
            }
        } catch (Exception ex) {
            s_logger.error("Problem sending email alert", ex);
        }
    }

    @Override @DB
    public void recalculateCapacity() {
        // FIXME: the right way to do this is to register a listener (see RouterStatsListener, VMSyncListener)
        //        for the vm sync state.  The listener model has connects/disconnects to keep things in sync much better
        //        than this model right now, so when a VM is started, we update the amount allocated, and when a VM
        //        is stopped we updated the amount allocated, and when VM sync reports a changed state, we update
        //        the amount allocated.  Hopefully it's limited to 3 entry points and will keep the amount allocated
        //        per host accurate.

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("recalculating system capacity");
        }
        List<CapacityVO> newCapacities = new ArrayList<CapacityVO>();
        

        // get all hosts..
        SearchCriteria sc = _hostDao.createSearchCriteria();
        sc.addAnd("status", SearchCriteria.Op.EQ, Status.Up.toString());
        List<HostVO> hosts = _hostDao.search(sc, null);

        // prep the service offerings
        List<ServiceOfferingVO> offerings = _offeringsDao.listAll();
        Map<Long, ServiceOfferingVO> offeringsMap = new HashMap<Long, ServiceOfferingVO>();
        for (ServiceOfferingVO offering : offerings) {
            offeringsMap.put(offering.getId(), offering);
        }
        for (HostVO host : hosts) {
            if (host.getType() != Host.Type.Routing) {
                continue;
            }
            long cpu = 0;
            long usedMemory = 0;
            List<DomainRouterVO> domainRouters = _routerDao.listUpByHostId(host.getId());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found " + domainRouters.size() + " router domains on host " + host.getId());
            }
            for (DomainRouterVO router : domainRouters) {
                usedMemory += router.getRamSize() * 1024L * 1024L;
            }

            List<ConsoleProxyVO> proxys = _consoleProxyDao.listUpByHostId(host.getId());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found " + proxys.size() + " console proxy on host " + host.getId());
            }
            for(ConsoleProxyVO proxy : proxys) {
                usedMemory += proxy.getRamSize() * 1024L * 1024L;
            }

            List<SecondaryStorageVmVO> secStorageVms = _secStorgaeVmDao.listUpByHostId(host.getId());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found " + secStorageVms.size() + " secondary storage VM on host " + host.getId());
            }
            for(SecondaryStorageVmVO secStorageVm : secStorageVms) {
                usedMemory += secStorageVm.getRamSize() * 1024L * 1024L;
            }

            List<UserVmVO> vms = _userVmDao.listUpByHostId(host.getId());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found " + vms.size() + " user VM on host " + host.getId());
            }

            for (UserVmVO vm : vms) {
                ServiceOffering so = offeringsMap.get(vm.getServiceOfferingId());
                usedMemory += so.getRamSize() * 1024L * 1024L;
                cpu += so.getCpu() * (so.getSpeed() * 0.99);
            }

            long totalMemory = host.getTotalMemory();

            CapacityVO newMemoryCapacity = new CapacityVO(host.getId(), host.getDataCenterId(), host.getPodId(), usedMemory, totalMemory, CapacityVO.CAPACITY_TYPE_MEMORY);
            CapacityVO newCPUCapacity = new CapacityVO(host.getId(), host.getDataCenterId(), host.getPodId(), cpu, (long)(host.getCpus()*host.getSpeed()* _cpuOverProvisioningFactor), CapacityVO.CAPACITY_TYPE_CPU);
            newCapacities.add(newMemoryCapacity);
            newCapacities.add(newCPUCapacity);
//                    _capacityDao.persist(newMemoryCapacity);
  //                  _capacityDao.persist(newCPUCapacity);
        }

        // Calculate storage pool capacity
        List<StoragePoolVO> storagePools = _storagePoolDao.listAllActive();
        for (StoragePoolVO pool : storagePools) {
            long disk = 0l;
            Pair<Long, Long> sizes = _volumeDao.getCountAndTotalByPool(pool.getId());
            disk = sizes.second();
            _storageMgr.createCapacityEntry(pool, disk);
        }

        // Calculate new Public IP capacity
        List<DataCenterVO> datacenters = _dcDao.listAll();
        for (DataCenterVO datacenter : datacenters) {
            long dcId = datacenter.getId();

            int totalPublicIPs = _publicIPAddressDao.countIPs(dcId, -1, false);
            int allocatedPublicIPs = _publicIPAddressDao.countIPs(dcId, -1, true);

            CapacityVO newPublicIPCapacity = new CapacityVO(null, dcId, null, allocatedPublicIPs, totalPublicIPs, CapacityVO.CAPACITY_TYPE_PUBLIC_IP);
            newCapacities.add(newPublicIPCapacity);
//            _capacityDao.persist(newPublicIPCapacity);
        }

        // Calculate new Private IP capacity
        List<HostPodVO> pods = _podDao.listAll();
        for (HostPodVO pod : pods) {
            long podId = pod.getId();
            long dcId = pod.getDataCenterId();

            int totalPrivateIPs = _privateIPAddressDao.countIPs(podId, dcId, false);
            int allocatedPrivateIPs = _privateIPAddressDao.countIPs(podId, dcId, true);

            CapacityVO newPrivateIPCapacity = new CapacityVO(null, dcId, podId, allocatedPrivateIPs, totalPrivateIPs, CapacityVO.CAPACITY_TYPE_PRIVATE_IP);
            newCapacities.add(newPrivateIPCapacity);
//            _capacityDao.persist(newPrivateIPCapacity);
        }
        
//        long start = System.currentTimeMillis();
        
        Transaction txn = Transaction.currentTxn();
        try {
        	txn.start();
        	// delete the old records
            _capacityDao.clearNonStorageCapacities();

            for (CapacityVO newCapacity : newCapacities) {
            	s_logger.trace("Executing capacity update");
                _capacityDao.persist(newCapacity);
                s_logger.trace("Done with capacity update");
            }
            txn.commit();
        } catch (Exception ex) {
        	txn.rollback();
        	s_logger.error("Unable to start transaction for capacity update");
        }finally {
        	txn.close();
        }
    }

    class CapacityChecker extends TimerTask {
        @Override
		public void run() {
            recalculateCapacity();

            // abort if we can't possibly send an alert...
            if (_emailAlert == null) {
                return;
            }

            try {
                List<CapacityVO> capacityList = _capacityDao.listAll();
                Map<String, List<CapacityVO>> capacityDcTypeMap = new HashMap<String, List<CapacityVO>>();

                for (CapacityVO capacity : capacityList) {
                    long dataCenterId = capacity.getDataCenterId();
                    short type = capacity.getCapacityType();
                    String key = "dc" + dataCenterId + "t" + type;
                    List<CapacityVO> list = capacityDcTypeMap.get(key);
                    if (list == null) {
                        list = new ArrayList<CapacityVO>();
                    }
                    list.add(capacity);
                    capacityDcTypeMap.put(key, list);
                }

                for (String keyValue : capacityDcTypeMap.keySet()) {
                    List<CapacityVO> capacities = capacityDcTypeMap.get(keyValue);
                    double totalCapacity = 0d;
                    double usedCapacity = 0d;
                    CapacityVO cap = capacities.get(0);
                    short capacityType = cap.getCapacityType();
                    long dataCenterId = cap.getDataCenterId();

                    for (CapacityVO capacity : capacities) {
                        totalCapacity += capacity.getTotalCapacity();
                        usedCapacity += capacity.getUsedCapacity();
                    }

                    double capacityPct = (usedCapacity / totalCapacity);
                    double thresholdLimit = 1.0;
                    DataCenterVO dcVO = _dcDao.findById(dataCenterId);
                    String dcName = ((dcVO == null) ? "unknown" : dcVO.getName());
                    String msgSubject = "";
                    String msgContent = "";
                    String totalStr = "";
                    String usedStr = "";
                    String pctStr = formatPercent(capacityPct);

                    // check for over threshold
                    switch (capacityType) {
                    case CapacityVO.CAPACITY_TYPE_MEMORY:
                        thresholdLimit = _memoryCapacityThreshold;
                        msgSubject = "System Alert: Low Available Memory in availablity zone " + dcName;
                        totalStr = formatBytesToMegabytes(totalCapacity);
                        usedStr = formatBytesToMegabytes(usedCapacity);
                        msgContent = "System memory is low, total: " + totalStr + " MB, used: " + usedStr + " MB (" + pctStr + "%)";
                        break;
                    case CapacityVO.CAPACITY_TYPE_CPU:
                        thresholdLimit = _cpuCapacityThreshold;
                        msgSubject = "System Alert: Low Unallocated CPU in availablity zone " + dcName;
                        totalStr = _dfWhole.format(totalCapacity);
                        usedStr = _dfWhole.format(usedCapacity);
                        msgContent = "Unallocated CPU is low, total: " + totalStr + " Mhz, used: " + usedStr + " Mhz (" + pctStr + "%)";
                        break;
                    case CapacityVO.CAPACITY_TYPE_STORAGE:
                        thresholdLimit = _storageCapacityThreshold;
                        msgSubject = "System Alert: Low Available Storage in availablity zone " + dcName;
                        totalStr = formatBytesToMegabytes(totalCapacity);
                        usedStr = formatBytesToMegabytes(usedCapacity);
                        msgContent = "Available storage space is low, total: " + totalStr + " MB, used: " + usedStr + " MB (" + pctStr + "%)";
                        break;
                    case CapacityVO.CAPACITY_TYPE_STORAGE_ALLOCATED:
                        thresholdLimit = _storageAllocCapacityThreshold;
                        msgSubject = "System Alert: Remaining unallocated Storage is low in availablity zone " + dcName;
                        totalStr = formatBytesToMegabytes(totalCapacity);
                        usedStr = formatBytesToMegabytes(usedCapacity);
                        msgContent = "Unallocated storage space is low, total: " + totalStr + " MB, allocated: " + usedStr + " MB (" + pctStr + "%)";
                        break;
                    case CapacityVO.CAPACITY_TYPE_PUBLIC_IP:
                        thresholdLimit = _publicIPCapacityThreshold;
                        msgSubject = "System Alert: Number of unallocated public IPs is low in availablity zone " + dcName;
                        totalStr = Double.toString(totalCapacity);
                        usedStr = Double.toString(usedCapacity);
                        msgContent = "Number of unallocated public IPs is low, total: " + totalStr + ", allocated: " + usedStr + " (" + pctStr + "%)";
                        break;
                    case CapacityVO.CAPACITY_TYPE_PRIVATE_IP:
                    	thresholdLimit = _privateIPCapacityThreshold;
                    	msgSubject = "System Alert: Number of unallocated private IPs is low in availablity zone " + dcName;
                    	totalStr = Double.toString(totalCapacity);
                        usedStr = Double.toString(usedCapacity);
                    	msgContent = "Number of unallocated private IPs is low, total: " + totalStr + ", allocated: " + usedStr + " (" + pctStr + "%)";
                    	break;
                    }

                    if (capacityPct > thresholdLimit) {
                        _emailAlert.sendAlert(capacityType, dataCenterId, null, msgSubject, msgContent);
                    } else {
                        _emailAlert.clearAlert(capacityType, dataCenterId, null);
                    }
                }
            } catch (Exception ex) {
                s_logger.error("Exception in CapacityChecker", ex);
            }
        }
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
        public void sendAlert(short alertType, long dataCenterId, Long podId, String subject, String content) throws MessagingException, UnsupportedEncodingException {
            AlertVO alert = null;
            
            if ((alertType != AlertManager.ALERT_TYPE_HOST) &&
                (alertType != AlertManager.ALERT_TYPE_USERVM) &&
                (alertType != AlertManager.ALERT_TYPE_DOMAIN_ROUTER) &&
                (alertType != AlertManager.ALERT_TYPE_CONSOLE_PROXY) &&
                (alertType != AlertManager.ALERT_TYPE_STORAGE_MISC) &&
                (alertType != AlertManager.ALERT_TYPE_MANAGMENT_NODE)) {
                alert = _alertDao.getLastAlert(alertType, dataCenterId, podId);
            }
            
            if (alert == null) {
                // set up a new alert
                AlertVO newAlert = new AlertVO();
                newAlert.setType(alertType);
                newAlert.setSubject(subject);
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
                AlertVO alert = _alertDao.getLastAlert(alertType, dataCenterId, podId);
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
}
