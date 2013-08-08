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
package org.apache.cloudstack.dedicated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.utils.component.AdapterBase;

import org.apache.cloudstack.api.commands.DedicateClusterCmd;
import org.apache.cloudstack.api.commands.DedicateHostCmd;
import org.apache.cloudstack.api.commands.DedicatePodCmd;
import org.apache.cloudstack.api.commands.DedicateZoneCmd;
import org.apache.cloudstack.api.commands.ListDedicatedClustersCmd;
import org.apache.cloudstack.api.commands.ListDedicatedHostsCmd;
import org.apache.cloudstack.api.commands.ListDedicatedPodsCmd;
import org.apache.cloudstack.api.commands.ListDedicatedZonesCmd;
import org.apache.cloudstack.api.commands.ReleaseDedicatedClusterCmd;
import org.apache.cloudstack.api.commands.ReleaseDedicatedHostCmd;
import org.apache.cloudstack.api.commands.ReleaseDedicatedPodCmd;
import org.apache.cloudstack.api.commands.ReleaseDedicatedZoneCmd;
import org.apache.cloudstack.api.response.DedicateClusterResponse;
import org.apache.cloudstack.api.response.DedicateHostResponse;
import org.apache.cloudstack.api.response.DedicatePodResponse;
import org.apache.cloudstack.api.response.DedicateZoneResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.DedicatedResources;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

@Component
@Local({DedicatedService.class })
public class DedicatedResourceManagerImpl implements DedicatedService {
    private static final Logger s_logger = Logger.getLogger(DedicatedResourceManagerImpl.class);

    @Inject AccountDao _accountDao;
    @Inject DomainDao _domainDao;
    @Inject HostPodDao _podDao;
    @Inject ClusterDao _clusterDao;
    @Inject HostDao _hostDao;
    @Inject DedicatedResourceDao _dedicatedDao;
    @Inject DataCenterDao _zoneDao;
    @Inject AccountManager _accountMgr;
    @Inject UserVmDao _userVmDao;
    @Inject ConfigurationDao _configDao;

    private int capacityReleaseInterval;

    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        capacityReleaseInterval = NumbersUtil.parseInt(_configDao.getValue(Config.CapacitySkipcountingHours.key()), 3600);
        return true;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_DEDICATE_RESOURCE, eventDescription = "dedicating a Zone")
    public List<DedicatedResourceVO> dedicateZone(Long zoneId, Long domainId, String accountName) {
        Long accountId = null;
        List<HostVO> hosts = null;
        if(accountName != null){
            Account caller = CallContext.current().getCallingAccount();
            Account owner = _accountMgr.finalizeOwner(caller, accountName, domainId, null);
            accountId = owner.getId();
        }
        List<Long> childDomainIds = getDomainChildIds(domainId);
        childDomainIds.add(domainId);
        checkAccountAndDomain(accountId, domainId);
        DataCenterVO dc = _zoneDao.findById(zoneId);
        if (dc == null) {
            throw new InvalidParameterValueException("Unable to find zone by id " + zoneId);
        } else {
            DedicatedResourceVO dedicatedZone = _dedicatedDao.findByZoneId(zoneId);
            //check if zone is dedicated
            if(dedicatedZone != null) {
                s_logger.error("Zone " + dc.getName() + " is already dedicated");
                throw new CloudRuntimeException("Zone  " + dc.getName() + " is already dedicated");
            }

            //check if any resource under this zone is dedicated to different account or sub-domain
            List<HostPodVO> pods = _podDao.listByDataCenterId(dc.getId());
            List<DedicatedResourceVO> podsToRelease = new ArrayList<DedicatedResourceVO>();
            List<DedicatedResourceVO> clustersToRelease = new ArrayList<DedicatedResourceVO>();
            List<DedicatedResourceVO> hostsToRelease = new ArrayList<DedicatedResourceVO>();
            for (HostPodVO pod : pods) {
                DedicatedResourceVO dPod = _dedicatedDao.findByPodId(pod.getId());
                if (dPod != null) {
                    if(!(childDomainIds.contains(dPod.getDomainId()))) {
                        throw new CloudRuntimeException("Pod " + pod.getName() + " under this Zone " + dc.getName() + " is dedicated to different account/domain");
                    }
                    if (accountId != null) {
                        if (dPod.getAccountId() == accountId) {
                            podsToRelease.add(dPod);
                        } else {
                            s_logger.error("Pod " + pod.getName() + " under this Zone " + dc.getName() + " is dedicated to different account/domain");
                            throw new CloudRuntimeException("Pod " + pod.getName() + " under this Zone " + dc.getName() + " is dedicated to different account/domain");
                        }
                    } else {
                        if (dPod.getAccountId() == null && dPod.getDomainId() == domainId) {
                            podsToRelease.add(dPod);
                        }
                    }
                }
            }

            for (DedicatedResourceVO dr : podsToRelease) {
                releaseDedicatedResource(null, dr.getPodId(), null, null);
            }

            List<ClusterVO> clusters = _clusterDao.listClustersByDcId(dc.getId());
            for (ClusterVO cluster : clusters) {
                DedicatedResourceVO dCluster = _dedicatedDao.findByClusterId(cluster.getId());
                if (dCluster != null) {
                    if(!(childDomainIds.contains(dCluster.getDomainId()))) {
                        throw new CloudRuntimeException("Cluster " + cluster.getName() + " under this Zone " + dc.getName() + " is dedicated to different account/domain");
                    }
                    if (accountId != null) {
                        if (dCluster.getAccountId() == accountId) {
                            clustersToRelease.add(dCluster);
                        } else {
                            s_logger.error("Cluster " + cluster.getName() + " under this Zone " + dc.getName() + " is dedicated to different account/domain");
                            throw new CloudRuntimeException("Cluster " + cluster.getName() + " under this Zone " + dc.getName() + " is dedicated to different account/domain");
                        }
                    } else {
                        if (dCluster.getAccountId() == null && dCluster.getDomainId() == domainId) {
                            clustersToRelease.add(dCluster);
                        }
                    }
                }
            }

            for (DedicatedResourceVO dr : clustersToRelease) {
                releaseDedicatedResource(null, null, dr.getClusterId(), null);
            }

            hosts = _hostDao.listByDataCenterId(dc.getId());
            for (HostVO host : hosts) {
                DedicatedResourceVO dHost = _dedicatedDao.findByHostId(host.getId());
                if (dHost != null) {
                    if(!(childDomainIds.contains(dHost.getDomainId()))) {
                        throw new CloudRuntimeException("Host " + host.getName() + " under this Zone " + dc.getName() + " is dedicated to different account/domain");
                    }
                    if (accountId != null) {
                        if (dHost.getAccountId() == accountId) {
                            hostsToRelease.add(dHost);
                        } else {
                            s_logger.error("Host " + host.getName() + " under this Zone " + dc.getName() + " is dedicated to different account/domain");
                            throw new CloudRuntimeException("Host " + host.getName() + " under this Zone " + dc.getName() + " is dedicated to different account/domain");
                        }
                    } else {
                        if (dHost.getAccountId() == null && dHost.getDomainId() == domainId) {
                            hostsToRelease.add(dHost);
                        }
                    }
                }
            }

            for (DedicatedResourceVO dr : hostsToRelease) {
                releaseDedicatedResource(null, null, null, dr.getHostId());
            }
        }

        checkHostsSuitabilityForExplicitDedication(accountId, childDomainIds, hosts);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        DedicatedResourceVO dedicatedResource = new DedicatedResourceVO(zoneId, null, null, null, null, null);
        try {
            dedicatedResource.setDomainId(domainId);
            if (accountId != null) {
                dedicatedResource.setAccountId(accountId);
            }
            dedicatedResource = _dedicatedDao.persist(dedicatedResource);
        } catch (Exception e) {
            s_logger.error("Unable to dedicate zone due to " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to dedicate zone. Please contact Cloud Support.");
        }
        txn.commit();

        List<DedicatedResourceVO> result = new ArrayList<DedicatedResourceVO>();
        result.add(dedicatedResource);
        return result;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_DEDICATE_RESOURCE, eventDescription = "dedicating a Pod")
    public List<DedicatedResourceVO> dedicatePod(Long podId, Long domainId, String accountName) {
        Long accountId = null;
        if(accountName != null){
            Account caller = CallContext.current().getCallingAccount();
            Account owner = _accountMgr.finalizeOwner(caller, accountName, domainId, null);
            accountId  = owner.getId();
        }
        List<Long> childDomainIds = getDomainChildIds(domainId);
        childDomainIds.add(domainId);
        checkAccountAndDomain(accountId, domainId);
        HostPodVO pod = _podDao.findById(podId);
        List<HostVO> hosts = null;
        if (pod == null) {
            throw new InvalidParameterValueException("Unable to find pod by id " + podId);
        } else {
            DedicatedResourceVO dedicatedPod = _dedicatedDao.findByPodId(podId);
            DedicatedResourceVO dedicatedZoneOfPod = _dedicatedDao.findByZoneId(pod.getDataCenterId());
            //check if pod is dedicated
            if(dedicatedPod != null ) {
                s_logger.error("Pod " + pod.getName() + " is already dedicated");
                throw new CloudRuntimeException("Pod " + pod.getName() + " is already dedicated");
            }

            if (dedicatedZoneOfPod != null) {
                boolean domainIdInChildreanList = getDomainChildIds(dedicatedZoneOfPod.getDomainId()).contains(domainId);
                //can dedicate a pod to an account/domain if zone is dedicated to parent-domain
                if (dedicatedZoneOfPod.getAccountId() != null || (accountId == null && !domainIdInChildreanList)
                        || (accountId != null && !(dedicatedZoneOfPod.getDomainId() == domainId || domainIdInChildreanList))) {
                    DataCenterVO zone = _zoneDao.findById(pod.getDataCenterId());
                    s_logger.error("Cannot dedicate Pod. Its zone is already dedicated");
                    throw new CloudRuntimeException("Pod's Zone " + zone.getName() + " is already dedicated");
                }
            }

            //check if any resource under this pod is dedicated to different account or sub-domain
            List<ClusterVO> clusters = _clusterDao.listByPodId(pod.getId());
            List<DedicatedResourceVO> clustersToRelease = new ArrayList<DedicatedResourceVO>();
            List<DedicatedResourceVO> hostsToRelease = new ArrayList<DedicatedResourceVO>();
            for (ClusterVO cluster : clusters) {
                DedicatedResourceVO dCluster = _dedicatedDao.findByClusterId(cluster.getId());
                if (dCluster != null) {
                    if(!(childDomainIds.contains(dCluster.getDomainId()))) {
                        throw new CloudRuntimeException("Cluster " + cluster.getName() + " under this Pod " + pod.getName() + " is dedicated to different account/domain");
                    }
                    /*if all dedicated resources belongs to same account and domain then we should release dedication
                    and make new entry for this Pod*/
                    if (accountId != null) {
                        if (dCluster.getAccountId() == accountId) {
                            clustersToRelease.add(dCluster);
                        } else {
                            s_logger.error("Cluster " + cluster.getName() + " under this Pod " + pod.getName() + " is dedicated to different account/domain");
                            throw new CloudRuntimeException("Cluster " + cluster.getName() + " under this Pod " + pod.getName() + " is dedicated to different account/domain");
                        }
                    } else {
                        if (dCluster.getAccountId() == null && dCluster.getDomainId() == domainId) {
                            clustersToRelease.add(dCluster);
                        }
                    }
                }
            }

            for (DedicatedResourceVO dr : clustersToRelease) {
                releaseDedicatedResource(null, null, dr.getClusterId(), null);
            }

            hosts = _hostDao.findByPodId(pod.getId());
            for (HostVO host : hosts) {
                DedicatedResourceVO dHost = _dedicatedDao.findByHostId(host.getId());
                if (dHost != null) {
                    if(!(getDomainChildIds(domainId).contains(dHost.getDomainId()))) {
                        throw new CloudRuntimeException("Host " + host.getName() + " under this Pod " + pod.getName() + " is dedicated to different account/domain");
                    }
                    if (accountId != null) {
                        if (dHost.getAccountId() == accountId) {
                            hostsToRelease.add(dHost);
                        } else {
                            s_logger.error("Host " + host.getName() + " under this Pod " + pod.getName() + " is dedicated to different account/domain");
                            throw new CloudRuntimeException("Host " + host.getName() + " under this Pod " + pod.getName() + " is dedicated to different account/domain");
                        }
                    } else {
                        if (dHost.getAccountId() == null && dHost.getDomainId() == domainId) {
                            hostsToRelease.add(dHost);
                        }
                    }
                }
            }

            for (DedicatedResourceVO dr : hostsToRelease) {
                releaseDedicatedResource(null, null, null, dr.getHostId());
            }
        }

        checkHostsSuitabilityForExplicitDedication(accountId, childDomainIds, hosts);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        DedicatedResourceVO dedicatedResource = new DedicatedResourceVO(null, podId, null, null, null, null);
        try {
            dedicatedResource.setDomainId(domainId);
            if (accountId != null) {
                dedicatedResource.setAccountId(accountId);
            }
            dedicatedResource = _dedicatedDao.persist(dedicatedResource);
        } catch (Exception e) {
            s_logger.error("Unable to dedicate pod due to " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to dedicate pod. Please contact Cloud Support.");
        }
        txn.commit();

        List<DedicatedResourceVO> result = new ArrayList<DedicatedResourceVO>();
        result.add(dedicatedResource);
        return result;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_DEDICATE_RESOURCE, eventDescription = "dedicating a Cluster")
    public List<DedicatedResourceVO> dedicateCluster(Long clusterId, Long domainId, String accountName) {
        Long accountId = null;
        List<HostVO> hosts = null;
        if(accountName != null){
            Account caller = CallContext.current().getCallingAccount();
            Account owner = _accountMgr.finalizeOwner(caller, accountName, domainId, null);
            accountId = owner.getId();
        }
        List<Long> childDomainIds = getDomainChildIds(domainId);
        childDomainIds.add(domainId);
        checkAccountAndDomain(accountId, domainId);
        ClusterVO cluster = _clusterDao.findById(clusterId);
        if (cluster == null) {
            throw new InvalidParameterValueException("Unable to find cluster by id " + clusterId);
        } else {
            DedicatedResourceVO dedicatedCluster = _dedicatedDao.findByClusterId(clusterId);
            DedicatedResourceVO dedicatedPodOfCluster = _dedicatedDao.findByPodId(cluster.getPodId());
            DedicatedResourceVO dedicatedZoneOfCluster = _dedicatedDao.findByZoneId(cluster.getDataCenterId());

            //check if cluster is dedicated
            if(dedicatedCluster != null) {
                s_logger.error("Cluster " + cluster.getName() + " is already dedicated");
                throw new CloudRuntimeException("Cluster "+ cluster.getName() + " is already dedicated");
            }

            if (dedicatedPodOfCluster != null) {
                boolean domainIdInChildreanList = getDomainChildIds(dedicatedPodOfCluster.getDomainId()).contains(domainId);
                //can dedicate a cluster to an account/domain if pod is dedicated to parent-domain
                if (dedicatedPodOfCluster.getAccountId() != null || (accountId == null && !domainIdInChildreanList)
                        || (accountId != null && !(dedicatedPodOfCluster.getDomainId() == domainId || domainIdInChildreanList))) {
                    s_logger.error("Cannot dedicate Cluster. Its Pod is already dedicated");
                    HostPodVO pod = _podDao.findById(cluster.getPodId());
                    throw new CloudRuntimeException("Cluster's Pod " +  pod.getName() + " is already dedicated");
                }
            }

            if (dedicatedZoneOfCluster != null) {
                boolean domainIdInChildreanList = getDomainChildIds(dedicatedZoneOfCluster.getDomainId()).contains(domainId);
                //can dedicate a cluster to an account/domain if zone is dedicated to parent-domain
                if (dedicatedZoneOfCluster.getAccountId() != null || (accountId == null && !domainIdInChildreanList)
                        || (accountId != null && !(dedicatedZoneOfCluster.getDomainId() == domainId || domainIdInChildreanList))) {
                    s_logger.error("Cannot dedicate Cluster. Its zone is already dedicated");
                    DataCenterVO zone = _zoneDao.findById(cluster.getDataCenterId());
                    throw new CloudRuntimeException("Cluster's Zone "+ zone.getName() + " is already dedicated");
                }
            }

            //check if any resource under this cluster is dedicated to different account or sub-domain
            hosts = _hostDao.findByClusterId(cluster.getId());
            List<DedicatedResourceVO> hostsToRelease = new ArrayList<DedicatedResourceVO>();
            for (HostVO host : hosts) {
                DedicatedResourceVO dHost = _dedicatedDao.findByHostId(host.getId());
                if (dHost != null) {
                    if(!(childDomainIds.contains(dHost.getDomainId()))) {
                        throw new CloudRuntimeException("Host " + host.getName() + " under this Cluster " + cluster.getName() + " is dedicated to different account/domain");
                    }
                    /*if all dedicated resources belongs to same account and domain then we should release dedication
                    and make new entry for this cluster */
                    if (accountId != null) {
                        if (dHost.getAccountId() == accountId) {
                            hostsToRelease.add(dHost);
                        } else {
                            s_logger.error("Cannot dedicate Cluster " + cluster.getName() + " to account" + accountName);
                            throw new CloudRuntimeException("Cannot dedicate Cluster " + cluster.getName() + " to account" + accountName);
                        }
                    } else {
                        if (dHost.getAccountId() == null && dHost.getDomainId() == domainId) {
                            hostsToRelease.add(dHost);
                        }
                    }
                }
            }

            for (DedicatedResourceVO dr : hostsToRelease) {
                releaseDedicatedResource(null, null, null, dr.getHostId());
            }
        }

        checkHostsSuitabilityForExplicitDedication(accountId, childDomainIds, hosts);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        DedicatedResourceVO dedicatedResource = new DedicatedResourceVO(null, null, clusterId, null, null, null);
        try {
            dedicatedResource.setDomainId(domainId);
            if (accountId != null) {
                dedicatedResource.setAccountId(accountId);
            }
            dedicatedResource = _dedicatedDao.persist(dedicatedResource);
        } catch (Exception e) {
            s_logger.error("Unable to dedicate host due to " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to dedicate cluster. Please contact Cloud Support.");
        }
        txn.commit();

        List<DedicatedResourceVO> result = new ArrayList<DedicatedResourceVO>();
        result.add(dedicatedResource);
        return result;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_DEDICATE_RESOURCE, eventDescription = "dedicating a Host")
    public List<DedicatedResourceVO> dedicateHost(Long hostId, Long domainId, String accountName) {
        Long accountId = null;
        if(accountName != null){
            Account caller = CallContext.current().getCallingAccount();
            Account owner = _accountMgr.finalizeOwner(caller, accountName, domainId, null);
            accountId = owner.getId();
        }
        checkAccountAndDomain(accountId, domainId);
        HostVO host = _hostDao.findById(hostId);
        if (host == null) {
            throw new InvalidParameterValueException("Unable to find host by id " + hostId);
        } else {
            //check if host is of routing type
            if (host.getType() != Host.Type.Routing) {
                throw new CloudRuntimeException("Invalid host type for host " + host.getName());
            }

            DedicatedResourceVO dedicatedHost = _dedicatedDao.findByHostId(hostId);
            DedicatedResourceVO dedicatedClusterOfHost = _dedicatedDao.findByClusterId(host.getClusterId());
            DedicatedResourceVO dedicatedPodOfHost = _dedicatedDao.findByPodId(host.getPodId());
            DedicatedResourceVO dedicatedZoneOfHost = _dedicatedDao.findByZoneId(host.getDataCenterId());

            if(dedicatedHost != null) {
                s_logger.error("Host "+  host.getName() + " is already dedicated");
                throw new CloudRuntimeException("Host "+  host.getName() + " is already dedicated");
            }

            if (dedicatedClusterOfHost != null) {
                boolean domainIdInChildreanList = getDomainChildIds(dedicatedClusterOfHost.getDomainId()).contains(domainId);
                //can dedicate a host to an account/domain if cluster is dedicated to parent-domain
                if (dedicatedClusterOfHost.getAccountId() != null || (accountId == null && !domainIdInChildreanList)
                        || (accountId != null && !(dedicatedClusterOfHost.getDomainId() == domainId || domainIdInChildreanList))) {
                    ClusterVO cluster = _clusterDao.findById(host.getClusterId());
                    s_logger.error("Host's Cluster " + cluster.getName() + " is already dedicated");
                    throw new CloudRuntimeException("Host's Cluster " + cluster.getName() + " is already dedicated");
                }
            }

            if (dedicatedPodOfHost != null){
                boolean domainIdInChildreanList = getDomainChildIds(dedicatedPodOfHost.getDomainId()).contains(domainId);
                //can dedicate a host to an account/domain if pod is dedicated to parent-domain
                if (dedicatedPodOfHost.getAccountId() != null || (accountId == null && !domainIdInChildreanList)
                        || (accountId != null && !(dedicatedPodOfHost.getDomainId() == domainId || domainIdInChildreanList))) {
                    HostPodVO pod = _podDao.findById(host.getPodId());
                    s_logger.error("Host's Pod " + pod.getName() + " is already dedicated");
                    throw new CloudRuntimeException("Host's Pod " + pod.getName() + " is already dedicated");
                }
            }

            if (dedicatedZoneOfHost !=  null) {
                boolean domainIdInChildreanList = getDomainChildIds(dedicatedZoneOfHost.getDomainId()).contains(domainId);
                //can dedicate a host to an account/domain if zone is dedicated to parent-domain
                if (dedicatedZoneOfHost.getAccountId() != null || (accountId == null && !domainIdInChildreanList)
                        || (accountId != null && !(dedicatedZoneOfHost.getDomainId() == domainId || domainIdInChildreanList))) {
                    DataCenterVO zone = _zoneDao.findById(host.getDataCenterId());
                    s_logger.error("Host's Data Center " + zone.getName() + " is already dedicated");
                    throw new CloudRuntimeException("Host's Data Center " + zone.getName() + " is already dedicated");
                }
            }
        }

        List<Long> childDomainIds = getDomainChildIds(domainId);
        childDomainIds.add(domainId);
        checkHostSuitabilityForExplicitDedication(accountId, childDomainIds, hostId);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        DedicatedResourceVO dedicatedResource = new DedicatedResourceVO(null, null, null, hostId, null, null);
        try {
            dedicatedResource.setDomainId(domainId);
            if (accountId != null) {
                dedicatedResource.setAccountId(accountId);
            }
            dedicatedResource = _dedicatedDao.persist(dedicatedResource);
        } catch (Exception e) {
            s_logger.error("Unable to dedicate host due to " + e.getMessage(), e);
            throw new CloudRuntimeException("Failed to dedicate host. Please contact Cloud Support.");
        }
        txn.commit();

        List<DedicatedResourceVO> result = new ArrayList<DedicatedResourceVO>();
        result.add(dedicatedResource);
        return result;
    }

    private List<UserVmVO> getVmsOnHost(long hostId) {
        List<UserVmVO> vms = _userVmDao.listUpByHostId(hostId);
        List<UserVmVO> vmsByLastHostId = _userVmDao.listByLastHostId(hostId);
        if (vmsByLastHostId.size() > 0) {
            // check if any VMs are within skip.counting.hours, if yes we have to consider the host.
            for (UserVmVO stoppedVM : vmsByLastHostId) {
                long secondsSinceLastUpdate = (DateUtil.currentGMTTime().getTime() - stoppedVM.getUpdateTime()
                        .getTime()) / 1000;
                if (secondsSinceLastUpdate < capacityReleaseInterval) {
                    vms.add(stoppedVM);
                }
            }
        }

        return vms;
    }

    private boolean checkHostSuitabilityForExplicitDedication(Long accountId, List<Long> domainIds, long hostId) {
        boolean suitable = true;
        List<UserVmVO> allVmsOnHost = getVmsOnHost(hostId);
        if (accountId != null) {
            for (UserVmVO vm : allVmsOnHost) {
                if (vm.getAccountId() != accountId) {
                    s_logger.info("Host " + vm.getHostId() + " found to be unsuitable for explicit dedication as it is " +
                            "running instances of another account");
                    throw new CloudRuntimeException("Host " + hostId + " found to be unsuitable for explicit dedication as it is " +
                            "running instances of another account");
                }
            }
        } else {
            for (UserVmVO vm : allVmsOnHost) {
                if (!domainIds.contains(vm.getDomainId())) {
                    s_logger.info("Host " + vm.getHostId() + " found to be unsuitable for explicit dedication as it is " +
                            "running instances of another domain");
                    throw new CloudRuntimeException("Host " + hostId + " found to be unsuitable for explicit dedication as it is " +
                            "running instances of another domain");
                }
            }
        }
        return suitable;
    }

    private boolean checkHostsSuitabilityForExplicitDedication(Long accountId, List<Long> domainIds, List<HostVO> hosts) {
        boolean suitable = true;
        for (HostVO host : hosts){
            checkHostSuitabilityForExplicitDedication(accountId, domainIds, host.getId());
        }
        return suitable;
    }

    private void checkAccountAndDomain(Long accountId, Long domainId) {
        DomainVO domain = _domainDao.findById(domainId);
        if (domain == null) {
            throw new InvalidParameterValueException("Unable to find the domain by id " + domainId + ", please specify valid domainId");
        }
        //check if account belongs to the domain id
        if (accountId != null) {
            AccountVO account = _accountDao.findById(accountId);
            if (account == null || domainId != account.getDomainId()){
                throw new InvalidParameterValueException("Please specify the domain id of the account: " + account.getAccountName());
            }
        }
    }

    private List<Long> getDomainChildIds(long domainId) {
        DomainVO domainRecord = _domainDao.findById(domainId);
        List<Long> domainIds = new ArrayList<Long>();
        domainIds.add(domainRecord.getId());
        // find all domain Ids till leaf
        List<DomainVO> allChildDomains = _domainDao.findAllChildren(domainRecord.getPath(), domainRecord.getId());
        for (DomainVO domain : allChildDomains) {
            domainIds.add(domain.getId());
        }
        return domainIds;
    }

    @Override
    public DedicateZoneResponse createDedicateZoneResponse(DedicatedResources resource) {
        DedicateZoneResponse dedicateZoneResponse = new DedicateZoneResponse();
        DataCenterVO dc = _zoneDao.findById(resource.getDataCenterId());
        DomainVO domain = _domainDao.findById(resource.getDomainId());
        AccountVO account = _accountDao.findById(resource.getAccountId());
        dedicateZoneResponse.setId(resource.getUuid());
        dedicateZoneResponse.setZoneId(dc.getUuid());
        dedicateZoneResponse.setZoneName(dc.getName());
        dedicateZoneResponse.setDomainId(domain.getUuid());
        if (account != null) {
            dedicateZoneResponse.setAccountId(account.getUuid());
        }
        dedicateZoneResponse.setObjectName("dedicatedzone");
        return dedicateZoneResponse;
    }

    @Override
    public DedicatePodResponse createDedicatePodResponse(DedicatedResources resource) {
        DedicatePodResponse dedicatePodResponse = new DedicatePodResponse();
        HostPodVO pod = _podDao.findById(resource.getPodId());
        DomainVO domain = _domainDao.findById(resource.getDomainId());
        AccountVO account = _accountDao.findById(resource.getAccountId());
        dedicatePodResponse.setId(resource.getUuid());
        dedicatePodResponse.setPodId(pod.getUuid());
        dedicatePodResponse.setPodName(pod.getName());
        dedicatePodResponse.setDomainId(domain.getUuid());
        if (account != null) {
            dedicatePodResponse.setAccountId(account.getUuid());
        }
        dedicatePodResponse.setObjectName("dedicatedpod");
        return dedicatePodResponse;
    }

    @Override
    public DedicateClusterResponse createDedicateClusterResponse(DedicatedResources resource) {
        DedicateClusterResponse dedicateClusterResponse = new DedicateClusterResponse();
        ClusterVO cluster = _clusterDao.findById(resource.getClusterId());
        DomainVO domain = _domainDao.findById(resource.getDomainId());
        AccountVO account = _accountDao.findById(resource.getAccountId());
        dedicateClusterResponse.setId(resource.getUuid());
        dedicateClusterResponse.setClusterId(cluster.getUuid());
        dedicateClusterResponse.setClusterName(cluster.getName());
        dedicateClusterResponse.setDomainId(domain.getUuid());
        if (account != null) {
            dedicateClusterResponse.setAccountId(account.getUuid());
        }
        dedicateClusterResponse.setObjectName("dedicatedcluster");
        return dedicateClusterResponse;
    }

    @Override
    public DedicateHostResponse createDedicateHostResponse(DedicatedResources resource) {
        DedicateHostResponse dedicateHostResponse = new DedicateHostResponse();
        HostVO host = _hostDao.findById(resource.getHostId());
        DomainVO domain = _domainDao.findById(resource.getDomainId());
        AccountVO account = _accountDao.findById(resource.getAccountId());
        dedicateHostResponse.setId(resource.getUuid());
        dedicateHostResponse.setHostId(host.getUuid());
        dedicateHostResponse.setHostName(host.getName());
        dedicateHostResponse.setDomainId(domain.getUuid());
        if (account != null) {
            dedicateHostResponse.setAccountId(account.getUuid());
        }
        dedicateHostResponse.setObjectName("dedicatedhost");
        return dedicateHostResponse;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(DedicateZoneCmd.class);
        cmdList.add(DedicatePodCmd.class);
        cmdList.add(DedicateClusterCmd.class);
        cmdList.add(DedicateHostCmd.class);
        cmdList.add(ListDedicatedZonesCmd.class);
        cmdList.add(ListDedicatedPodsCmd.class);
        cmdList.add(ListDedicatedClustersCmd.class);
        cmdList.add(ListDedicatedHostsCmd.class);
        cmdList.add(ReleaseDedicatedClusterCmd.class);
        cmdList.add(ReleaseDedicatedHostCmd.class);
        cmdList.add(ReleaseDedicatedPodCmd.class);
        cmdList.add(ReleaseDedicatedZoneCmd.class);
        return cmdList;
    }

    @Override
    public Pair<List<? extends DedicatedResourceVO>, Integer> listDedicatedZones(ListDedicatedZonesCmd cmd) {
        Long zoneId = cmd.getZoneId();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        if (accountName != null) {
            if (domainId != null) {
                Account account = _accountDao.findActiveAccount(accountName, domainId);
                if (account != null) {
                    accountId = account.getId();
                }
            } else {
                throw new InvalidParameterValueException("Please specify the domain id of the account: " + accountName);
            }
        }
        Pair<List<DedicatedResourceVO>, Integer> result = _dedicatedDao.searchDedicatedZones(zoneId, domainId, accountId);
        return new Pair<List<? extends DedicatedResourceVO>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends DedicatedResourceVO>, Integer> listDedicatedPods(ListDedicatedPodsCmd cmd) {
        Long podId = cmd.getPodId();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        if (accountName != null) {
            if (domainId != null) {
                Account account = _accountDao.findActiveAccount(accountName, domainId);
                if (account != null) {
                    accountId = account.getId();
                }
            } else {
                throw new InvalidParameterValueException("Please specify the domain id of the account: " + accountName);
            }
        }
        Pair<List<DedicatedResourceVO>, Integer> result = _dedicatedDao.searchDedicatedPods(podId, domainId, accountId);
        return new Pair<List<? extends DedicatedResourceVO>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends DedicatedResourceVO>, Integer> listDedicatedClusters(ListDedicatedClustersCmd cmd) {
        Long clusterId = cmd.getClusterId();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        if (accountName != null) {
            if (domainId != null) {
                Account account = _accountDao.findActiveAccount(accountName, domainId);
                if (account != null) {
                    accountId = account.getId();
                }
            } else {
                throw new InvalidParameterValueException("Please specify the domain id of the account: " + accountName);
            }
        }
        Pair<List<DedicatedResourceVO>, Integer> result = _dedicatedDao.searchDedicatedClusters(clusterId, domainId, accountId);
        return new Pair<List<? extends DedicatedResourceVO>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends DedicatedResourceVO>, Integer> listDedicatedHosts(ListDedicatedHostsCmd cmd) {
        Long hostId = cmd.getHostId();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
        if (accountName != null) {
            if (domainId != null) {
                Account account = _accountDao.findActiveAccount(accountName, domainId);
                if (account != null) {
                    accountId = account.getId();
                }
            } else {
                throw new InvalidParameterValueException("Please specify the domain id of the account: " + accountName);
            }
        }

        Pair<List<DedicatedResourceVO>, Integer> result = _dedicatedDao.searchDedicatedHosts(hostId, domainId, accountId);
        return new Pair<List<? extends DedicatedResourceVO>, Integer>(result.first(), result.second());
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_DEDICATE_RESOURCE_RELEASE, eventDescription = "Releasing dedicated resource")
    public boolean releaseDedicatedResource(Long zoneId, Long podId, Long clusterId, Long hostId) throws InvalidParameterValueException{
        DedicatedResourceVO resource = null;
        Long resourceId = null;
        if (zoneId != null) {
            resource = _dedicatedDao.findByZoneId(zoneId);
        }
        if (podId != null) {
            resource = _dedicatedDao.findByPodId(podId);
        }
        if (clusterId != null) {
            resource = _dedicatedDao.findByClusterId(clusterId);
        }
        if (hostId != null ) {
            resource = _dedicatedDao.findByHostId(hostId);
        }
        if (resource == null){
            throw new InvalidParameterValueException("No Dedicated Resource available to release");
        } else {
            Transaction txn = Transaction.currentTxn();
            txn.start();
            resourceId = resource.getId();
            if (!_dedicatedDao.remove(resourceId)) {
                throw new CloudRuntimeException("Failed to delete Resource " + resourceId);
            }
            txn.commit();
        }
        return true;
    }
}
