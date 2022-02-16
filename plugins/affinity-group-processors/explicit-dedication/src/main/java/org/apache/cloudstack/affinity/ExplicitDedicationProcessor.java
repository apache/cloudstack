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
package org.apache.cloudstack.affinity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.log4j.Logger;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.AffinityConflictException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

public class ExplicitDedicationProcessor extends AffinityProcessorBase implements AffinityGroupProcessor {

    private static final Logger s_logger = Logger.getLogger(ExplicitDedicationProcessor.class);
    @Inject
    protected UserVmDao _vmDao;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected DataCenterDao _dcDao;
    @Inject
    protected DedicatedResourceDao _dedicatedDao;
    @Inject
    protected HostPodDao _podDao;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected DomainDao _domainDao;
    @Inject
    protected AffinityGroupDao _affinityGroupDao;
    @Inject
    protected AffinityGroupVMMapDao _affinityGroupVMMapDao;

    /**
     * This method will process the affinity group of type 'Explicit Dedication' for a deployment of a VM that demands dedicated resources.
     * For ExplicitDedicationProcessor we need to add dedicated resources into the IncludeList based on the level we have dedicated resources available.
     * For eg. if admin dedicates a pod to a domain, then all the user in that domain can use the resources of that pod.
     * We need to take care of the situation when dedicated resources further have resources dedicated to sub-domain/account.
     * This IncludeList is then used to update the avoid list for a given data center.
     */
    @Override
    public void process(VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid) throws AffinityConflictException {
        VirtualMachine vm = vmProfile.getVirtualMachine();
        List<AffinityGroupVMMapVO> vmGroupMappings = _affinityGroupVMMapDao.findByVmIdType(vm.getId(), getType());
        DataCenter dc = _dcDao.findById(vm.getDataCenterId());
        List<DedicatedResourceVO> resourceList = new ArrayList<DedicatedResourceVO>();

        if (vmGroupMappings != null && !vmGroupMappings.isEmpty()) {

            for (AffinityGroupVMMapVO vmGroupMapping : vmGroupMappings) {
                if (vmGroupMapping != null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Processing affinity group " + vmGroupMapping.getAffinityGroupId() + "of type 'ExplicitDedication' for VM Id: " + vm.getId());
                    }

                    long affinityGroupId = vmGroupMapping.getAffinityGroupId();

                    List<DedicatedResourceVO> dr = _dedicatedDao.listByAffinityGroupId(affinityGroupId);
                    resourceList.addAll(dr);

                }
            }

            boolean canUse = false;

            if (plan.getHostId() != null) {
                HostVO host = _hostDao.findById(plan.getHostId());
                ClusterVO clusterofHost = _clusterDao.findById(host.getClusterId());
                HostPodVO podOfHost = _podDao.findById(host.getPodId());
                DataCenterVO zoneOfHost = _dcDao.findById(host.getDataCenterId());
                if (resourceList != null && resourceList.size() != 0) {
                    for (DedicatedResourceVO resource : resourceList) {
                        if ((resource.getHostId() != null && resource.getHostId().longValue() == plan.getHostId().longValue()) ||
                                (resource.getClusterId() != null && resource.getClusterId().longValue() == clusterofHost.getId()) ||
                                (resource.getPodId() != null && resource.getPodId().longValue() == podOfHost.getId()) ||
                                (resource.getDataCenterId() != null && resource.getDataCenterId().longValue() == zoneOfHost.getId())) {
                            canUse = true;
                        }
                    }
                }
                if (!canUse) {
                    throw new CloudRuntimeException("Cannot use this host " + host.getName() + " for explicit dedication");
                }
            } else if (plan.getClusterId() != null) {
                ClusterVO cluster = _clusterDao.findById(plan.getClusterId());
                HostPodVO podOfCluster = _podDao.findById(cluster.getPodId());
                DataCenterVO zoneOfCluster = _dcDao.findById(cluster.getDataCenterId());
                List<HostVO> hostToUse = new ArrayList<HostVO>();
                // check whether this cluster or its pod is dedicated
                if (resourceList != null && resourceList.size() != 0) {
                    for (DedicatedResourceVO resource : resourceList) {
                        if ((resource.getClusterId() != null && resource.getClusterId() == cluster.getId()) ||
                            (resource.getPodId() != null && resource.getPodId() == podOfCluster.getId()) ||
                            (resource.getDataCenterId() != null && resource.getDataCenterId() == zoneOfCluster.getId())) {
                            canUse = true;
                        }

                        // check for all dedicated host; if it belongs to this
                        // cluster
                        if (!canUse) {
                            if (resource.getHostId() != null) {
                                HostVO dHost = _hostDao.findById(resource.getHostId());
                                if (dHost.getClusterId() == cluster.getId()) {
                                    hostToUse.add(dHost);
                                }
                            }
                        }

                    }
                }

                if (hostToUse.isEmpty() && !canUse) {
                    throw new CloudRuntimeException("Cannot use this cluster " + cluster.getName() + " for explicit dedication");
                }

                if (hostToUse != null && hostToUse.size() != 0) {
                    // add other non-dedicated hosts to avoid list
                    List<HostVO> hostList = _hostDao.findByClusterId(cluster.getId());
                    for (HostVO host : hostList) {
                        if (!hostToUse.contains(host)) {
                            avoid.addHost(host.getId());
                        }
                    }
                }

            } else if (plan.getPodId() != null) {
                HostPodVO pod = _podDao.findById(plan.getPodId());
                DataCenterVO zoneOfPod = _dcDao.findById(pod.getDataCenterId());
                List<ClusterVO> clustersToUse = new ArrayList<ClusterVO>();
                List<HostVO> hostsToUse = new ArrayList<HostVO>();
                // check whether this cluster or its pod is dedicated
                if (resourceList != null && resourceList.size() != 0) {
                    for (DedicatedResourceVO resource : resourceList) {
                        if ((resource.getPodId() != null && resource.getPodId() == pod.getId()) ||
                            (resource.getDataCenterId() != null && resource.getDataCenterId() == zoneOfPod.getId())) {
                            canUse = true;
                        }

                        // check for all dedicated cluster/host; if it belongs
                        // to
                        // this pod
                        if (!canUse) {
                            if (resource.getClusterId() != null) {
                                ClusterVO dCluster = _clusterDao.findById(resource.getClusterId());
                                if (dCluster.getPodId() == pod.getId()) {
                                    clustersToUse.add(dCluster);
                                }
                            }
                            if (resource.getHostId() != null) {
                                HostVO dHost = _hostDao.findById(resource.getHostId());
                                if (dHost.getPodId() == pod.getId()) {
                                    hostsToUse.add(dHost);
                                }
                            }
                        }

                    }
                }

                if (hostsToUse.isEmpty() && clustersToUse.isEmpty() && !canUse) {
                    throw new CloudRuntimeException("Cannot use this pod " + pod.getName() + " for explicit dedication");
                }

                if (clustersToUse != null && clustersToUse.size() != 0) {
                    // add other non-dedicated clusters to avoid list
                    List<ClusterVO> clusterList = _clusterDao.listByPodId(pod.getId());
                    for (ClusterVO cluster : clusterList) {
                        if (!clustersToUse.contains(cluster)) {
                            avoid.addCluster(cluster.getId());
                        }
                    }
                }

                if (hostsToUse != null && hostsToUse.size() != 0) {
                    // add other non-dedicated hosts to avoid list
                    List<HostVO> hostList = _hostDao.findByPodId(pod.getId());
                    for (HostVO host : hostList) {
                        if (!hostsToUse.contains(host)) {
                            avoid.addHost(host.getId());
                        }
                    }
                }

            } else {
                // check all resources under this zone
                if (resourceList != null && resourceList.size() != 0) {
                    avoid = updateAvoidList(resourceList, avoid, dc);
                } else {
                    avoid.addDataCenter(dc.getId());
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("No dedicated resources available for this domain or account under this group");
                    }
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("ExplicitDedicationProcessor returns Avoid List as: Deploy avoids pods: " + avoid.getPodsToAvoid() + ", clusters: " +
                        avoid.getClustersToAvoid() + ", hosts: " + avoid.getHostsToAvoid());
                }
            }
        }

    }

    private ExcludeList updateAvoidList(List<DedicatedResourceVO> dedicatedResources, ExcludeList avoidList, DataCenter dc) {
        ExcludeList includeList = new ExcludeList();
        for (DedicatedResourceVO dr : dedicatedResources) {
            if (dr.getHostId() != null) {
                includeList.addHost(dr.getHostId());
                HostVO dedicatedHost = _hostDao.findById(dr.getHostId());
                includeList.addCluster(dedicatedHost.getClusterId());
                includeList.addPod(dedicatedHost.getPodId());
            }

            if (dr.getClusterId() != null) {
                includeList.addCluster(dr.getClusterId());
                //add all hosts inside this in includeList
                List<HostVO> hostList = _hostDao.findByClusterId(dr.getClusterId());
                for (HostVO host : hostList) {
                    DedicatedResourceVO dHost = _dedicatedDao.findByHostId(host.getId());
                    if (dHost != null && !dedicatedResources.contains(dHost)) {
                        avoidList.addHost(host.getId());
                    } else {
                        includeList.addHost(host.getId());
                    }
                }
                ClusterVO dedicatedCluster = _clusterDao.findById(dr.getClusterId());
                includeList.addPod(dedicatedCluster.getPodId());
            }

            if (dr.getPodId() != null) {
                includeList.addPod(dr.getPodId());
                //add all cluster under this pod in includeList
                List<ClusterVO> clusterList = _clusterDao.listByPodId(dr.getPodId());
                for (ClusterVO cluster : clusterList) {
                    DedicatedResourceVO dCluster = _dedicatedDao.findByClusterId(cluster.getId());
                    if (dCluster != null && !dedicatedResources.contains(dCluster)) {
                        avoidList.addCluster(cluster.getId());
                    } else {
                        includeList.addCluster(cluster.getId());
                    }
                }
                //add all hosts inside this pod in includeList
                List<HostVO> hostList = _hostDao.findByPodId(dr.getPodId());
                for (HostVO host : hostList) {
                    DedicatedResourceVO dHost = _dedicatedDao.findByHostId(host.getId());
                    if (dHost != null && !dedicatedResources.contains(dHost)) {
                        avoidList.addHost(host.getId());
                    } else {
                        includeList.addHost(host.getId());
                    }
                }
            }

            if (dr.getDataCenterId() != null) {
                includeList.addDataCenter(dr.getDataCenterId());
                //add all Pod under this data center in includeList
                List<HostPodVO> podList = _podDao.listByDataCenterId(dr.getDataCenterId());
                for (HostPodVO pod : podList) {
                    DedicatedResourceVO dPod = _dedicatedDao.findByPodId(pod.getId());
                    if (dPod != null && !dedicatedResources.contains(dPod)) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug(String.format("Avoiding POD %s [%s] because it is not dedicated.", pod.getName(), pod.getUuid()));
                        }
                        avoidList.addPod(pod.getId());
                    } else {
                        includeList.addPod(pod.getId());
                    }
                }
                List<ClusterVO> clusterList = _clusterDao.listClustersByDcId(dr.getDataCenterId());
                for (ClusterVO cluster : clusterList) {
                    DedicatedResourceVO dCluster = _dedicatedDao.findByClusterId(cluster.getId());
                    if (dCluster != null && !dedicatedResources.contains(dCluster)) {
                        avoidList.addCluster(cluster.getId());
                    } else {
                        includeList.addCluster(cluster.getId());
                    }
                }
                //add all hosts inside this in includeList
                List<HostVO> hostList = _hostDao.listByDataCenterId(dr.getDataCenterId());
                for (HostVO host : hostList) {
                    DedicatedResourceVO dHost = _dedicatedDao.findByHostId(host.getId());
                    if (dHost != null && !dedicatedResources.contains(dHost)) {
                        avoidList.addHost(host.getId());
                    } else {
                        includeList.addHost(host.getId());
                    }
                }
            }
        }
        //Update avoid list using includeList.
        //add resources in avoid list which are not in include list.

        List<HostPodVO> pods = _podDao.listByDataCenterId(dc.getId());
        List<ClusterVO> clusters = _clusterDao.listClustersByDcId(dc.getId());
        List<HostVO> hosts = _hostDao.listByDataCenterId(dc.getId());
        Set<Long> podsInIncludeList = includeList.getPodsToAvoid();
        Set<Long> clustersInIncludeList = includeList.getClustersToAvoid();
        Set<Long> hostsInIncludeList = includeList.getHostsToAvoid();

        for (HostPodVO pod : pods) {
            if (podsInIncludeList != null && !podsInIncludeList.contains(pod.getId())) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("Avoiding POD %s [%s], as it is not in include list.", pod.getName(), pod.getUuid()));
                }
                avoidList.addPod(pod.getId());
            }
        }

        for (ClusterVO cluster : clusters) {
            if (clustersInIncludeList != null && !clustersInIncludeList.contains(cluster.getId())) {
                avoidList.addCluster(cluster.getId());
            }
        }

        for (HostVO host : hosts) {
            if (hostsInIncludeList != null && !hostsInIncludeList.contains(host.getId())) {
                avoidList.addHost(host.getId());
            }
        }
        return avoidList;
    }

    private List<DedicatedResourceVO> searchInParentDomainResources(long domainId) {
        List<Long> domainIds = getDomainParentIds(domainId);
        List<DedicatedResourceVO> dr = new ArrayList<DedicatedResourceVO>();
        for (Long id : domainIds) {
            List<DedicatedResourceVO> resource = _dedicatedDao.listByDomainId(id);
            if (resource != null) {
                dr.addAll(resource);
            }
        }
        return dr;
    }

    private List<DedicatedResourceVO> searchInDomainResources(long domainId) {
        List<DedicatedResourceVO> dr = _dedicatedDao.listByDomainId(domainId);
        return dr;
    }

    private List<Long> getDomainParentIds(long domainId) {
        DomainVO domainRecord = _domainDao.findById(domainId);
        List<Long> domainIds = new ArrayList<Long>();
        domainIds.add(domainRecord.getId());
        while (domainRecord.getParent() != null) {
            domainRecord = _domainDao.findById(domainRecord.getParent());
            domainIds.add(domainRecord.getId());
        }
        return domainIds;
    }

    @Override
    public boolean isAdminControlledGroup() {
        return true;
    }

    @Override
    public boolean canBeSharedDomainWide() {
        return true;
    }

    @DB
    @Override
    public void handleDeleteGroup(final AffinityGroup group) {
        // When a group of the 'ExplicitDedication' type gets deleted, make sure
        // to remove the dedicated resources in the group as well.
        if (group != null) {
            List<DedicatedResourceVO> dedicatedResources = _dedicatedDao.listByAffinityGroupId(group.getId());
            if (!dedicatedResources.isEmpty()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Releasing the dedicated resources under group: " + group);
                }

                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        SearchBuilder<DedicatedResourceVO> listByAffinityGroup = _dedicatedDao.createSearchBuilder();
                        listByAffinityGroup.and("affinityGroupId", listByAffinityGroup.entity().getAffinityGroupId(), SearchCriteria.Op.EQ);
                        listByAffinityGroup.done();
                        SearchCriteria<DedicatedResourceVO> sc = listByAffinityGroup.create();
                        sc.setParameters("affinityGroupId", group.getId());

                        _dedicatedDao.lockRows(sc, null, true);
                        _dedicatedDao.remove(sc);
                    }
                });
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("No dedicated resources to releease under group: " + group);
                }
            }
        }

        return;
    }

    @Override
    public boolean subDomainAccess() {
        return true;
    }
}
