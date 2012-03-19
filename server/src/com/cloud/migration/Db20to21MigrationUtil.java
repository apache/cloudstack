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

package com.cloud.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.migration.DiskOffering21VO.Type;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

public class Db20to21MigrationUtil {
    private static final Logger s_logger = Logger.getLogger(Db20to21MigrationUtil.class);

    protected DataCenterDao _dcDao;
    protected HostPodDao _podDao;
    protected ConfigurationDao _configDao;
    protected ClusterDao _clusterDao;
    protected HostDao _hostDao;
    protected StoragePoolDao _spDao;
    protected DomainDao _domainDao;
    protected ServiceOffering20Dao _serviceOffering20Dao;
    protected DiskOffering20Dao _diskOffering20Dao;
    protected ServiceOffering21Dao _serviceOffering21Dao;
    protected DiskOffering21Dao _diskOffering21Dao;
    protected ConsoleProxyDao _consoleProxyDao;
    protected SecondaryStorageVmDao _secStorageVmDao;
    protected VMInstanceDao _vmInstanceDao;
    protected VolumeDao _volumeDao;
    protected UserVmDao _userVmDao;
    protected DomainRouterDao _routerDao;
    protected StoragePoolDao _poolDao;

    protected long _consoleProxyServiceOfferingId;
    protected long _secStorageServiceOfferingId;
    protected long _domRServiceOfferingId;
    protected boolean _isPremium = false;

    protected static class DcPod {
        long id;
        String name;
        long count;

        public DcPod() {
        }
    }

    private void migrateZones() {
        boolean createCluster = false;
        String value = _configDao.getValue("xen.create.pools.in.pod");
        if (value == null || !value.equalsIgnoreCase("true")) {
            s_logger.info("System is not configured to use Xen server pool, we will skip creating cluster for pods");
        } else {
            createCluster = true;
        }

        // Displaying summarize data center/pod configuration from old DB before we continue
        GenericSearchBuilder<DataCenterVO, DcPod> sb = _dcDao.createSearchBuilder(DcPod.class);
        sb.selectField(sb.entity().getId());
        sb.selectField(sb.entity().getName());
        sb.select("count", Func.COUNT, null);
        sb.groupBy(sb.entity().getId(), sb.entity().getName());
        sb.done();

        SearchCriteria<DcPod> sc = sb.create();
        List<DcPod> results = _dcDao.customSearchIncludingRemoved(sc, (Filter) null);
        if (results.size() > 0) {
            System.out.println("We've found following zones are deployed in your database");
            for (DcPod cols : results) {
                System.out.println("\tid: " + cols.id + ",\tname: " + cols.name + ",\tpods in zone: " + cols.count);
            }
            System.out.println("From 2.0 to 2.1, pod is required to have gateway configuration");

            for (DcPod cols : results) {
                migrateZonePods(cols.id, cols.name, createCluster);

                s_logger.info("Set system VM guest MAC in zone" + cols.name);
                migrateSystemVmGuestMacAndState(cols.id);
            }
        } else {
            System.out.println("We couldn't find any zone being deployed. Skip Zone/Pod migration");
        }
    }

    private void migrateZonePods(long zoneId, String zoneName, boolean createCluster) {
        SearchBuilder<HostPodVO> sb = _podDao.createSearchBuilder();
        sb.and("zoneId", sb.entity().getDataCenterId(), Op.EQ);
        sb.done();

        SearchCriteria<HostPodVO> sc = sb.create();
        sc.setParameters("zoneId", zoneId);

        List<HostPodVO> pods = _podDao.searchIncludingRemoved(sc, null, false, false);
        if (pods.size() > 0) {
            for (HostPodVO pod : pods) {
                System.out.println("Migrating pod " + pod.getName() + " in zone " + zoneName + "...");
                System.out.println("Current pod " + pod.getName() + " configuration as");
                System.out.println("\tCIDR: " + pod.getCidrAddress() + "/" + pod.getCidrSize());
                System.out.println("\tGateway: " + pod.getGateway());
                System.out.print("Please type your gateway address for the pod: ");

                String gateway = readInput();
                pod.setGateway(gateway);
                _podDao.update(pod.getId(), pod);
                if (createCluster) {
                    migrateHostsInPod(zoneId, pod.getId(), pod.getName());
                }

                System.out.println("Set last_host_id for VMs in pod " + pod.getName());
                migrateVmInstanceLastHostId(zoneId, pod.getId());

                System.out.println("Setup link local addresses, it will take a while, please wait...");
                String ipNums = _configDao.getValue("linkLocalIp.nums");
                int nums = Integer.parseInt(ipNums);
                if (nums > 16 || nums <= 0) {
                    nums = 10;
                }

                /* local link ip address starts from 169.254.0.2 - 169.254.(nums) */
                String[] ipRanges = NetUtils.getLinkLocalIPRange(nums);
                _dcDao.addLinkLocalIpAddress(zoneId, pod.getId(), ipRanges[0], ipRanges[1]);
            }
        }
    }

    private void migrateHostsInPod(long zoneId, long podId, String podName) {
        System.out.println("Creating cluster for pod " + podName);

        ClusterVO cluster = null;

        SearchBuilder<HostVO> sb = _hostDao.createSearchBuilder();
        sb.and("dc", sb.entity().getDataCenterId(), Op.EQ);
        sb.and("pod", sb.entity().getPodId(), Op.EQ);
        sb.and("type", sb.entity().getType(), Op.EQ);
        sb.done();

        SearchCriteria<HostVO> sc = sb.create();
        sc.setParameters("dc", zoneId);
        sc.setParameters("pod", podId);
        sc.setParameters("type", Host.Type.Routing);

        // join cluster for hosts in pod
        List<HostVO> hostsInPod = _hostDao.searchIncludingRemoved(sc, null, false, false);
        if (hostsInPod.size() > 0) {
            if (cluster == null) {
                cluster = new ClusterVO(zoneId, podId, String.valueOf(podId));
                cluster = _clusterDao.persist(cluster);
            }

            for (HostVO host : hostsInPod) {
                host.setClusterId(cluster.getId());
                _hostDao.update(host.getId(), host);

                System.out.println("Join host " + host.getName() + " to auto-formed cluster");
            }
        }

        SearchBuilder<StoragePoolVO> sbPool = _spDao.createSearchBuilder();
        sbPool.and("dc", sbPool.entity().getDataCenterId(), Op.EQ);
        sbPool.and("pod", sbPool.entity().getPodId(), Op.EQ);
        sbPool.and("poolType", sbPool.entity().getPoolType(), Op.IN);
        sbPool.done();

        SearchCriteria<StoragePoolVO> scPool = sbPool.create();
        scPool.setParameters("dc", zoneId);
        scPool.setParameters("pod", podId);
        scPool.setParameters("poolType", StoragePoolType.NetworkFilesystem.toString(), StoragePoolType.IscsiLUN.toString());

        List<StoragePoolVO> sPoolsInPod = _spDao.searchIncludingRemoved(scPool, null, false, false);
        if (sPoolsInPod.size() > 0) {
            if (cluster == null) {
                cluster = new ClusterVO(zoneId, podId, String.valueOf(podId));
                cluster = _clusterDao.persist(cluster);
            }

            for (StoragePoolVO spool : sPoolsInPod) {
                spool.setClusterId(cluster.getId());
                _spDao.update(spool.getId(), spool);

                System.out.println("Join host " + spool.getName() + " to auto-formed cluster");
            }
        }
    }

    private void composeDomainPath(DomainVO domain, StringBuilder sb) {
        if (domain.getParent() == null) {
            sb.append("/");
        } else {
            DomainVO parent = _domainDao.findById(domain.getParent());
            composeDomainPath(parent, sb);

            if (domain.getName().contains("/")) {
                System.out.println("Domain " + domain.getName() + " contains invalid domain character, replace it with -");
                sb.append(domain.getName().replace('/', '-'));
            } else {
                sb.append(domain.getName());
            }
            sb.append("/");
        }
    }

    private void migrateDomains() {
        System.out.println("Migrating domains...");

        // we shouldn't have too many domains in the system, use a very dumb way to setup domain path
        List<DomainVO> domains = _domainDao.listAllIncludingRemoved();
        for (DomainVO domain : domains) {
            StringBuilder path = new StringBuilder();
            composeDomainPath(domain, path);

            System.out.println("Convert domain path, domin: " + domain.getId() + ", path:" + path.toString());

            domain.setPath(path.toString());
            _domainDao.update(domain.getId(), domain);
        }

        System.out.println("All domains have been migrated to 2.1 format");
    }

    private void migrateServiceOfferings() {
        System.out.println("Migrating service offering...");

        long seq = getServiceOfferingStartSequence();

        List<ServiceOffering20VO> oldServiceOfferings = _serviceOffering20Dao.listAllIncludingRemoved();
        for (ServiceOffering20VO so20 : oldServiceOfferings) {
            ServiceOffering21VO so21 = new ServiceOffering21VO(so20.getName(), so20.getCpu(), so20.getRamSize(), so20.getSpeed(), so20.getRateMbps(),
                    so20.getMulticastRateMbps(), so20.getOfferHA(), so20.getDisplayText(), so20.getUseLocalStorage(),
                    false, null);
            so21.setId(seq++);
            so21.setDiskSize(0);
            so21 = _serviceOffering21Dao.persist(so21);

            if (so20.getId().longValue() != so21.getId()) {
                // Update all foreign reference from old value to new value, need to be careful with foreign key
// constraints
                updateServiceOfferingReferences(so20.getId().longValue(), so21.getId());
            }
        }

        boolean useLocalStorage = Boolean.parseBoolean(_configDao.getValue(Config.SystemVMUseLocalStorage.key()));

        // create service offering for system VMs and update references
        int proxyRamSize = NumbersUtil.parseInt(
                _configDao.getValue(Config.ConsoleProxyRamSize.key()),
                ConsoleProxyManager.DEFAULT_PROXY_VM_RAMSIZE);
        ServiceOffering21VO soConsoleProxy = new ServiceOffering21VO("Fake Offering For DomP", 1,
                proxyRamSize, 0, 0, 0, false, null, useLocalStorage,
                true, null);
        soConsoleProxy.setId(seq++);
        soConsoleProxy.setUniqueName(ServiceOffering.consoleProxyDefaultOffUniqueName);
        soConsoleProxy = _serviceOffering21Dao.persist(soConsoleProxy);
        _consoleProxyServiceOfferingId = soConsoleProxy.getId();

        int secStorageVmRamSize = NumbersUtil.parseInt(
                _configDao.getValue(Config.SecStorageVmRamSize.key()),
                SecondaryStorageVmManager.DEFAULT_SS_VM_RAMSIZE);
        ServiceOffering21VO soSecondaryVm = new ServiceOffering21VO("Fake Offering For Secondary Storage VM", 1,
                secStorageVmRamSize, 0, 0, 0, false, null, useLocalStorage, true, null);
        soSecondaryVm.setId(seq++);
        soSecondaryVm.setUniqueName(ServiceOffering.ssvmDefaultOffUniqueName);
        soSecondaryVm = _serviceOffering21Dao.persist(soSecondaryVm);
        _secStorageServiceOfferingId = soSecondaryVm.getId();

        int routerRamSize = NumbersUtil.parseInt(_configDao.getValue("router.ram.size"), 128);
        ServiceOffering21VO soDomainRouter = new ServiceOffering21VO("Fake Offering For DomR", 1,
                routerRamSize, 0, 0, 0, false, null, useLocalStorage, true, null);
        soDomainRouter.setId(seq++);
        soDomainRouter.setUniqueName(ServiceOffering.routerDefaultOffUniqueName);
        soDomainRouter = _serviceOffering21Dao.persist(soDomainRouter);
        _domRServiceOfferingId = soDomainRouter.getId();

        System.out.println("Service offering has been migrated to 2.1 format");
    }

    private long getServiceOfferingStartSequence() {
        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
        long seq = 0;
        try {
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement("SELECT max(id) FROM service_offering");
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            seq = rs.getLong(1);
            pstmt.close();

            pstmt = txn.prepareAutoCloseStatement("SELECT max(id) FROM disk_offering");
            rs = pstmt.executeQuery();
            rs.next();
            seq += rs.getLong(1);
            pstmt.close();

            seq += 100; // add a gap
            return seq;
        } catch (SQLException e) {
            s_logger.error("Unhandled exception: ", e);
        } finally {
            txn.close();
        }

        return 10000;
    }

    private void updateConsoleProxyServiceOfferingReferences(long serviceOfferingId) {
        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
        try {
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(
                    "UPDATE volumes SET disk_offering_id=? WHERE instance_id IN (SELECT id FROM vm_instance WHERE type='ConsoleProxy')");
            pstmt.setLong(1, serviceOfferingId);

            int rows = pstmt.executeUpdate();
            s_logger.info("Update volumes for console proxy service offering change, affected rows: " + rows);
        } catch (SQLException e) {
            s_logger.error("Unhandled exception: ", e);
        } finally {
            txn.close();
        }
    }

    private void updateSecondaryStorageServiceOfferingReferences(long serviceOfferingId) {
        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
        try {
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(
                    "UPDATE volumes SET disk_offering_id=? WHERE instance_id IN (SELECT id FROM vm_instance WHERE type='SecondaryStorageVm')");
            pstmt.setLong(1, serviceOfferingId);

            int rows = pstmt.executeUpdate();
            s_logger.info("Update volumes for secondary storage service offering change, affected rows: " + rows);
        } catch (SQLException e) {
            s_logger.error("Unhandled exception: ", e);
        } finally {
            txn.close();
        }
    }

    private void updateDomainRouterServiceOfferingReferences(long serviceOfferingId) {
        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
        try {
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(
                    "UPDATE volumes SET disk_offering_id=? WHERE instance_id IN (SELECT id FROM vm_instance WHERE type='DomainRouter')");
            pstmt.setLong(1, serviceOfferingId);

            int rows = pstmt.executeUpdate();
            s_logger.info("Update volumes for secondary storage service offering change, affected rows: " + rows);
        } catch (SQLException e) {
            s_logger.error("Unhandled exception: ", e);
        } finally {
            txn.close();
        }
    }

    private void updateServiceOfferingReferences(long oldServiceOfferingId, long newServiceOfferingId) {
        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
        try {
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement("UPDATE user_vm SET service_offering_id=? WHERE service_offering_id=?");

            pstmt.setLong(1, newServiceOfferingId);
            pstmt.setLong(2, oldServiceOfferingId);

            int rows = pstmt.executeUpdate();
            s_logger.info("Update user_vm for service offering change (" + oldServiceOfferingId + "->" + newServiceOfferingId + "), affected rows: " + rows);

        } catch (SQLException e) {
            s_logger.error("Unhandled exception: ", e);
        } finally {
            txn.close();
        }
    }

    private void migrateDiskOfferings() {
        System.out.println("Migrating disk offering...");

        List<DiskOffering20VO> oldDiskOfferings = _diskOffering20Dao.listAllIncludingRemoved();
        long maxDiskOfferingId = _domRServiceOfferingId;
        maxDiskOfferingId += 100;

        for (DiskOffering20VO do20 : oldDiskOfferings) {
            DiskOffering21VO do21 = new DiskOffering21VO(do20.getDomainId(), do20.getName(), do20.getDisplayText(), do20.getDiskSize(),
                    do20.getMirrored(), null);
            do21.setType(Type.Disk);
            do21.setId(maxDiskOfferingId++);

            do21 = _diskOffering21Dao.persist(do21);
            if (do20.getId().longValue() != do21.getId()) {
                updateDiskOfferingReferences(do20.getId().longValue(), do21.getId());
            }
        }

        FixupNullDiskOfferingInVolumes();

        System.out.println("Disk offering has been migrated to 2.1 format");
    }

    private void FixupNullDiskOfferingInVolumes() {
        System.out.println("Fixup NULL disk_offering_id references in volumes table ...");

        SearchCriteria<DiskOffering21VO> scDiskOffering = _diskOffering21Dao.createSearchCriteria();
        List<DiskOffering21VO> offeringList = _diskOffering21Dao.searchIncludingRemoved(scDiskOffering,
                new Filter(DiskOffering21VO.class, "diskSize", true, null, null), false, false);

        for (DiskOffering21VO offering : offeringList) {
            s_logger.info("Disk offering name: " + offering.getName() + ", disk size: " + offering.getDiskSizeInBytes());
        }

        SearchBuilder<VolumeVO> sb = _volumeDao.createSearchBuilder();
        sb.and("diskOfferingId", sb.entity().getDiskOfferingId(), Op.NULL);
        sb.done();

        SearchCriteria<VolumeVO> sc = sb.create();
        List<VolumeVO> volumes = _volumeDao.searchIncludingRemoved(sc, null, false, false);

        if (volumes.size() > 0) {
            for (VolumeVO vol : volumes) {
                if (vol.getInstanceId() != null) {
                    VMInstanceVO vmInstance = _vmInstanceDao.findById(vol.getInstanceId());

                    if (vmInstance.getType() == VirtualMachine.Type.User) {
                        // if the volume is for user VM, we can retrieve the information from service_offering_id
                        UserVmVO userVm = _userVmDao.findById(vol.getInstanceId());
                        if (userVm != null) {
                            // following operation requires that all service offerings should have been fixed up already
                            vol.setDiskOfferingId(userVm.getServiceOfferingId());
                        } else {
                            System.out.println("Data integrity could not be fixed up for volume: " + vol.getId() + " because its owner user vm no longer exists");
                        }
                    } else if (vmInstance.getType() == VirtualMachine.Type.ConsoleProxy) {
                        vol.setDiskOfferingId(this._consoleProxyServiceOfferingId);
                    } else if (vmInstance.getType() == VirtualMachine.Type.SecondaryStorageVm) {
                        vol.setDiskOfferingId(this._secStorageServiceOfferingId);
                    } else if (vmInstance.getType() == VirtualMachine.Type.DomainRouter) {
                        vol.setDiskOfferingId(this._domRServiceOfferingId);
                    }
                } else {
                    System.out.println("volume: " + vol.getId() + " is standalone, fix disck_offering_id based on volume size");

                    // try to guess based on volume size and fill it in
                    boolean found = false;
                    for (DiskOffering21VO do21 : offeringList) {
                        if (do21.getType() == Type.Disk && vol.getSize() > do21.getDiskSizeInBytes()) {
                            found = true;
                            System.out.println("volume: " + vol.getId() + " disck_offering_id is fixed to " + do21.getId());
                            vol.setDiskOfferingId(do21.getId());
                            break;
                        }
                    }

                    if (!found) {
                        System.out.println("volume: " + vol.getId() + " disck_offering_id is fixed to " + offeringList.get(offeringList.size() - 1).getId());
                        vol.setDiskOfferingId(offeringList.get(offeringList.size() - 1).getId());
                    }
                }

                _volumeDao.update(vol.getId(), vol);
            }
        }

        System.out.println("Disk offering fixup is done");
    }

    private void updateDiskOfferingReferences(long oldDiskOfferingId, long newDiskOfferingId) {
        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
        try {
            PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement("UPDATE vm_disk SET disk_offering_id=? WHERE disk_offering_id=?");

            pstmt.setLong(1, newDiskOfferingId);
            pstmt.setLong(2, oldDiskOfferingId);

            int rows = pstmt.executeUpdate();
            pstmt.close();

            s_logger.info("Update vm_disk for disk offering change (" + oldDiskOfferingId + "->" + newDiskOfferingId + "), affected rows: " + rows);

            pstmt = txn.prepareAutoCloseStatement("UPDATE volumes SET disk_offering_id=? WHERE disk_offering_id=?");
            pstmt.setLong(1, newDiskOfferingId);
            pstmt.setLong(2, oldDiskOfferingId);
            rows = pstmt.executeUpdate();
            pstmt.close();
            s_logger.info("Update volumes for disk offering change (" + oldDiskOfferingId + "->" + newDiskOfferingId + "), affected rows: " + rows);
        } catch (SQLException e) {
            s_logger.error("Unhandled exception: ", e);
        } finally {
            txn.close();
        }
    }

    private void migrateSystemVmGuestMacAndState(long zoneId) {
        // for console proxy VMs
        SearchBuilder<ConsoleProxyVO> sb = _consoleProxyDao.createSearchBuilder();
        sb.and("zoneId", sb.entity().getDataCenterIdToDeployIn(), Op.EQ);
        sb.done();

        SearchCriteria<ConsoleProxyVO> sc = sb.create();
        sc.setParameters("zoneId", zoneId);

        List<ConsoleProxyVO> proxies = _consoleProxyDao.searchIncludingRemoved(sc, null, false, false);
        for (ConsoleProxyVO proxy : proxies) {
            String[] macAddresses = _dcDao.getNextAvailableMacAddressPair(zoneId, (1L << 31));
            String guestMacAddress = macAddresses[0];

            if (proxy.getState() == State.Running || proxy.getState() == State.Starting) {
                System.out.println("System VM " + proxy.getHostName() + " is in active state, mark it to Stopping state for migration");
                proxy.setState(State.Stopping);
            }

            String guestIpAddress = _dcDao.allocateLinkLocalIpAddress(proxy.getDataCenterIdToDeployIn(), proxy.getPodIdToDeployIn(), proxy.getId(), null);

            System.out.println("Assign link loal address to proxy " + proxy.getHostName() + ", link local address: " + guestIpAddress);
            _consoleProxyDao.update(proxy.getId(), proxy);
        }

        // for secondary storage VMs
        SearchBuilder<SecondaryStorageVmVO> sb2 = _secStorageVmDao.createSearchBuilder();
        sb2.and("zoneId", sb2.entity().getDataCenterIdToDeployIn(), Op.EQ);
        sb2.done();

        SearchCriteria<SecondaryStorageVmVO> sc2 = sb2.create();
        sc2.setParameters("zoneId", zoneId);

        List<SecondaryStorageVmVO> secStorageVms = _secStorageVmDao.searchIncludingRemoved(sc2, null, false, false);
        for (SecondaryStorageVmVO secStorageVm : secStorageVms) {
            String[] macAddresses = _dcDao.getNextAvailableMacAddressPair(zoneId, (1L << 31));
            String guestMacAddress = macAddresses[0];

            if (secStorageVm.getState() == State.Running || secStorageVm.getState() == State.Starting) {
                System.out.println("System VM " + secStorageVm.getHostName() + " is in active state, mark it to Stopping state for migration");
                secStorageVm.setState(State.Stopping);
            }

            String guestIpAddress = _dcDao.allocateLinkLocalIpAddress(secStorageVm.getDataCenterIdToDeployIn(), secStorageVm.getPodIdToDeployIn(), secStorageVm.getId(), null);

            System.out.println("Assign link loal address to secondary storage VM " + secStorageVm.getHostName() + ", link local address: " + guestIpAddress);
            _secStorageVmDao.update(secStorageVm.getId(), secStorageVm);
        }

        // for Domain Router VMs
        // Although we can list those we are interested, but just too lazy, list all of them and check their states.
        SearchBuilder<DomainRouterVO> sb3 = _routerDao.createSearchBuilder();
        sb3.and("zoneId", sb3.entity().getDataCenterIdToDeployIn(), Op.EQ);
        sb3.done();

        SearchCriteria<DomainRouterVO> sc3 = sb3.create();
        sc3.setParameters("zoneId", zoneId);
        List<DomainRouterVO> domRs = _routerDao.searchIncludingRemoved(sc3, null, false, false);
        for (DomainRouterVO router : domRs) {
            if (router.getState() == State.Running || router.getState() == State.Starting) {
                router.setState(State.Stopping);

                System.out.println("System VM " + router.getHostName() + " is in active state, mark it to Stopping state for migration");
                _routerDao.update(router.getId(), router);
            }
        }
    }

    private void migrateVmInstanceLastHostId(long zoneId, long podId) {
        SearchBuilder<VMInstanceVO> sb = _vmInstanceDao.createSearchBuilder();
        sb.and("zoneId", sb.entity().getDataCenterIdToDeployIn(), Op.EQ);
        sb.and("podId", sb.entity().getPodIdToDeployIn(), Op.EQ);
        sb.done();

        Random rand = new Random();
        SearchCriteria<VMInstanceVO> sc = sb.create();
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("podId", podId);
        List<VMInstanceVO> vmInstances = _vmInstanceDao.searchIncludingRemoved(sc, null, false, false);
        List<HostVO> podHosts = getHostsInPod(zoneId, podId);
        for (VMInstanceVO vm : vmInstances) {
            if (vm.getHostId() != null) {
                vm.setLastHostId(vm.getHostId());
            } else {
                if (podHosts.size() > 0) {
                    int next = rand.nextInt(podHosts.size());
                    vm.setLastHostId(podHosts.get(next).getId());
                }
            }
            _vmInstanceDao.update(vm.getId(), vm);
        }
    }

    private List<HostVO> getHostsInPod(long zoneId, long podId) {
        SearchBuilder<HostVO> sb = _hostDao.createSearchBuilder();
        sb.and("zoneId", sb.entity().getDataCenterId(), Op.EQ);
        sb.and("podId", sb.entity().getPodId(), Op.EQ);
        sb.and("type", sb.entity().getType(), Op.EQ);
        sb.done();

        SearchCriteria<HostVO> sc = sb.create();
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("podId", podId);
        sc.setParameters("type", Host.Type.Routing.toString());

        return _hostDao.searchIncludingRemoved(sc, null, false, false);
    }

    private void migrateVolumDeviceIds() {
        System.out.println("Migrating device_id for volumes, this may take a while, please wait...");
        SearchCriteria<VMInstanceVO> sc = _vmInstanceDao.createSearchCriteria();
        List<VMInstanceVO> vmInstances = _vmInstanceDao.searchIncludingRemoved(sc, null, false, false);

        long deviceId = 1;
        for (VMInstanceVO vm : vmInstances) {
            SearchBuilder<VolumeVO> sb = _volumeDao.createSearchBuilder();
            sb.and("instanceId", sb.entity().getInstanceId(), Op.EQ);
            sb.done();

            SearchCriteria<VolumeVO> sc2 = sb.create();
            sc2.setParameters("instanceId", vm.getId());

            List<VolumeVO> volumes = _volumeDao.searchIncludingRemoved(sc2, null, false, false);
            deviceId = 1; // reset for each VM iteration
            for (VolumeVO vol : volumes) {
                if (vol.getVolumeType() == Volume.Type.ROOT) {
                    System.out.println("Setting root volume device id to zero, vol: " + vol.getName() + ", instance: " + vm.getHostName());

                    vol.setDeviceId(0L);
                } else if (vol.getVolumeType() == Volume.Type.DATADISK) {
                    System.out.println("Setting data volume device id, vol: " + vol.getName() + ", instance: " + vm.getHostName() + ", device id: " + deviceId);

                    vol.setDeviceId(deviceId);

                    // don't use device ID 3
                    if (++deviceId == 3) {
                        deviceId++;
                    }
                } else {
                    System.out.println("Unsupported volume type found for volume: " + vol.getName());
                }

                _volumeDao.update(vol.getId(), vol);
            }
        }

        System.out.println("Migrating device_id for volumes done");
    }

    private void migrateVolumePoolType() {
        System.out.println("Migrating pool type for volumes...");

        SearchCriteria<VolumeVO> sc = _volumeDao.createSearchCriteria();
        List<VolumeVO> volumes = _volumeDao.searchIncludingRemoved(sc, null, false, false);
        for (VolumeVO vol : volumes) {
            if (vol.getPoolId() != null) {
                StoragePoolVO pool = _poolDao.findById(vol.getPoolId());
                if (pool != null) {
                    vol.setPoolType(pool.getPoolType());

                    _volumeDao.update(vol.getId(), vol);
                } else {
                    System.out.println("Unable to determine pool type for volume: " + vol.getName());
                }
            }
        }

        System.out.println("Migrating pool type for volumes done");
    }

    private void migrateConfiguration() {
        System.out.println("Migrating 2.1 configuration variables...");

        System.out.print("Are you migrating from 2.0 Premium Edition? (yes/no): ");
        String answer = readInput();
        if (answer != null && answer.equalsIgnoreCase("yes")) {
            _isPremium = true;
        }

        // Save default Configuration Table values
        List<String> categories = Config.getCategories();
        for (String category : categories) {
            // If this is not a premium environment, don't insert premium configuration values
            if (!_isPremium && category.equals("Premium")) {
                continue;
            }

            List<Config> configs = Config.getConfigs(category);
            for (Config c : configs) {
                String name = c.key();

                // If the value is already in the table, don't reinsert it
                if (_configDao.getValue(name) != null) {
                    continue;
                }

                String instance = "DEFAULT";
                String component = c.getComponent();
                String value = c.getDefaultValue();
                String description = c.getDescription();
                ConfigurationVO configVO = new ConfigurationVO(category, instance, component, name, value, description);
                _configDao.persist(configVO);
            }

            // If this is a premium environment, set the network type to be "vlan"
            if (_isPremium) {
                _configDao.update("network.type", "vlan");
                _configDao.update("hypervisor.type", "xenserver");
                _configDao.update("secondary.storage.vm", "true");
                _configDao.update("secstorage.encrypt.copy", "true");
                _configDao.update("secstorage.secure.copy.cert", "realhostip");
            }

            boolean externalIpAlloator = Boolean.parseBoolean(_configDao.getValue("direct.attach.network.externalIpAllocator.enabled"));
            String hyperVisor = _configDao.getValue("hypervisor.type");
            if (hyperVisor.equalsIgnoreCase("KVM") && !externalIpAlloator) {
                /* For KVM, it's enabled by default */
                _configDao.update("direct.attach.network.externalIpAllocator.enabled", "true");
            }

            // Save the mount parent to the configuration table
            String mountParent = getMountParent();
            if (mountParent != null) {
                _configDao.update("mount.parent", mountParent);
            }

            if (_configDao.getValue("host") == null) {
                String hostIpAdr = getHost();
                if (hostIpAdr != null) {
                    _configDao.update("host", hostIpAdr);
                }
            }

            // generate a single sign-on key
            updateSSOKey();
        }

        System.out.println("Migrating 2.1 configuration done");
    }

    private String getEthDevice() {
        String defaultRoute = Script.runSimpleBashScript("/sbin/route | grep default");

        if (defaultRoute == null) {
            return null;
        }

        String[] defaultRouteList = defaultRoute.split("\\s+");

        if (defaultRouteList.length != 8) {
            return null;
        }

        return defaultRouteList[7];
    }

    protected String getHost() {
        NetworkInterface nic = null;
        String pubNic = getEthDevice();

        if (pubNic == null) {
            return null;
        }

        try {
            nic = NetworkInterface.getByName(pubNic);
        } catch (final SocketException e) {
            return null;
        }

        String[] info = NetUtils.getNetworkParams(nic);
        return info[0];
    }

    private String getMountParent() {
        return getEnvironmentProperty("mount.parent");
    }

    private String getEnvironmentProperty(String name) {
        try {
            final File propsFile = PropertiesUtil.findConfigFile("environment.properties");

            if (propsFile == null) {
                return null;
            } else {
                final FileInputStream finputstream = new FileInputStream(propsFile);
                final Properties props = new Properties();
                props.load(finputstream);
                finputstream.close();
                return props.getProperty("mount.parent");
            }
        } catch (IOException e) {
            return null;
        }
    }

    private void updateSSOKey() {
        try {
            String encodedKey = null;

            // Algorithm for SSO Keys is SHA1, should this be configuable?
            KeyGenerator generator = KeyGenerator.getInstance("HmacSHA1");
            SecretKey key = generator.generateKey();
            encodedKey = Base64.encodeBase64URLSafeString(key.getEncoded());

            _configDao.update("security.singlesignon.key", encodedKey);
        } catch (NoSuchAlgorithmException ex) {
            s_logger.error("error generating sso key", ex);
        }
    }

    private void doMigration() {
        setupComponents();

        migrateZones();
        migrateDomains();
        migrateServiceOfferings();
        migrateDiskOfferings();
        migrateVolumDeviceIds();
        migrateVolumePoolType();
        migrateConfiguration();

        // update disk_offering_id for system VMs. As of the id-space collision for servercie_offering_ids
        // before and after migration, this should be done in the last step
        updateConsoleProxyServiceOfferingReferences(_consoleProxyServiceOfferingId);
        updateSecondaryStorageServiceOfferingReferences(_secStorageServiceOfferingId);
        updateDomainRouterServiceOfferingReferences(_domRServiceOfferingId);

        System.out.println("Migration done");
    }

    private String readInput() {
        try {
            Scanner in = new Scanner(System.in);
            String input = in.nextLine();
            return input;
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    private void setupComponents() {
        ComponentLocator.getLocator("migration", "migration-components.xml", "log4j-cloud.xml");
        ComponentLocator locator = ComponentLocator.getCurrentLocator();

        _configDao = locator.getDao(ConfigurationDao.class);
        _podDao = locator.getDao(HostPodDao.class);
        _dcDao = locator.getDao(DataCenterDao.class);
        _clusterDao = locator.getDao(ClusterDao.class);
        _hostDao = locator.getDao(HostDao.class);
        _spDao = locator.getDao(StoragePoolDao.class);
        _domainDao = locator.getDao(DomainDao.class);
        _serviceOffering20Dao = locator.getDao(ServiceOffering20Dao.class);
        _diskOffering20Dao = locator.getDao(DiskOffering20Dao.class);
        _serviceOffering21Dao = locator.getDao(ServiceOffering21Dao.class);
        _diskOffering21Dao = locator.getDao(DiskOffering21Dao.class);
        _consoleProxyDao = locator.getDao(ConsoleProxyDao.class);
        _secStorageVmDao = locator.getDao(SecondaryStorageVmDao.class);
        _vmInstanceDao = locator.getDao(VMInstanceDao.class);
        _volumeDao = locator.getDao(VolumeDao.class);
        _userVmDao = locator.getDao(UserVmDao.class);
        _routerDao = locator.getDao(DomainRouterDao.class);
        _poolDao = locator.getDao(StoragePoolDao.class);
    }

    public static void main(String[] args) {
        File file = PropertiesUtil.findConfigFile("log4j-cloud.xml");

        if (file != null) {
            System.out.println("Log4j configuration from : " + file.getAbsolutePath());
            DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
        } else {
            System.out.println("Configure log4j with default properties");
        }

        new Db20to21MigrationUtil().doMigration();
        System.exit(0);
    }
}
