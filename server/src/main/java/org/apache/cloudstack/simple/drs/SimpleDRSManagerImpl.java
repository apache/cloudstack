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
package org.apache.cloudstack.simple.drs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import org.apache.cloudstack.api.command.admin.simple.drs.ScheduleDRSCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.framework.jobs.impl.JobSerializerHelper;
import org.apache.cloudstack.framework.simple.drs.SimpleDRSProvider;
import org.apache.cloudstack.framework.simple.drs.SimpleDRSRebalancingAlgorithm;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.poll.BackgroundPollManager;
import org.apache.cloudstack.poll.BackgroundPollTask;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

public class SimpleDRSManagerImpl extends ManagerBase implements SimpleDRSManager {

    public static final Logger LOG = Logger.getLogger(SimpleDRSManagerImpl.class);

    private static SimpleDRSProvider configuredDRSProvider;
    private static SimpleDRSRebalancingAlgorithm configuredDRSAlgorithm;
    private static Map<String, SimpleDRSProvider> drsProvidersMap = new HashMap<>();
    private static Map<String, SimpleDRSRebalancingAlgorithm> drsAlgorithmsMap = new HashMap<>();

    private List<SimpleDRSProvider> drsProviders;
    private List<SimpleDRSRebalancingAlgorithm> drsAlgorithms;

    @Inject
    private BackgroundPollManager backgroundPollManager;

    @Inject
    private AsyncJobManager asyncJobManager;

    @Inject
    private ClusterDao clusterDao;

    ////////////////////////////////////////////////////
    /////////////// Init DRS providers /////////////////
    ////////////////////////////////////////////////////

    public void setDrsProviders(List<SimpleDRSProvider> drsProviders) {
        this.drsProviders = drsProviders;
        initDrsProvidersMap();
    }

    public List<SimpleDRSProvider> getDrsProviders() {
        return drsProviders;
    }

    public List<SimpleDRSRebalancingAlgorithm> getDrsAlgorithms() {
        return drsAlgorithms;
    }

    private void initDrsProvidersMap() {
        if (CollectionUtils.isNotEmpty(drsProviders)) {
            for (SimpleDRSProvider provider : drsProviders) {
                drsProvidersMap.put(provider.getProviderName().toLowerCase(), provider);
            }
        }
    }

    ////////////////////////////////////////////////////
    /////////////// Init DRS algorithms ////////////////
    ////////////////////////////////////////////////////

    private void initDrsAlgorithmsMap() {
        if (CollectionUtils.isNotEmpty(drsAlgorithms)) {
            for (SimpleDRSRebalancingAlgorithm algorithm : drsAlgorithms) {
                drsAlgorithmsMap.put(algorithm.getAlgorithmName().toLowerCase(), algorithm);
            }
        }
    }

    public void setDrsAlgorithms(List<SimpleDRSRebalancingAlgorithm> drsAlgorithms) {
        this.drsAlgorithms = drsAlgorithms;
        initDrsAlgorithmsMap();
    }

    ////////////////////////////////////////////////////
    //////////////// Schedule DRS Task /////////////////
    ////////////////////////////////////////////////////

    private final class ScheduleDRSTask extends ManagedContextRunnable implements BackgroundPollTask {

        private SimpleDRSManager manager;
        private ClusterDao clusterDao;
        private SortedMap<Integer, List<Long>> wakeupMap;
        private List<Long> clusterIds;
        private SearchBuilder<ClusterVO> sb;
        private long delay = 5;
        private long now = 0;

        private static final long second = 1000;
        // TODO : Make this 60 * second. Kept it this way for easy testing
        private static final long minute = second;

        private ScheduleDRSTask(SimpleDRSManager manager, ClusterDao clusterDao) {
            this.manager = manager;
            this.clusterDao = clusterDao;
            this.wakeupMap = new TreeMap<>();
            this.clusterIds = new LinkedList<>();
            this.sb = this.clusterDao.createSearchBuilder();
            this.sb.and("id", this.sb.entity().getId(), SearchCriteria.Op.IN);
            init();
        }

        private void init() {
            List<ClusterVO> clusters = clusterDao.listAll();
            addClustersToSchedule(clusters);
        }

        private void addClustersToSchedule(List<ClusterVO> clusters) {
            if (clusters == null) {
                return;
            }
            for (ClusterVO cluster : clusters) {
                Long clusterId = cluster.getId();
                clusterIds.add(clusterId);
                Integer interval = SimpleDRSAutomaticInterval.valueIn(clusterId);
                addToWakeupMap(interval, clusterId);
            }
        }

        private void addToWakeupMap(Integer key, Long value) {
            if (wakeupMap.containsKey(key)) {
                wakeupMap.get(key).add(value);
            } else {
                List<Long> list = new LinkedList<Long>();
                list.add(value);
                wakeupMap.put(key, list);
            }
        }

        @Override
        protected void runInContext() {
            try {
                this.now += this.delay;
                LOG.info("DRS Bacground @ " + new Date());
                if (this.wakeupMap.isEmpty()) {
                    return;
                }
                Integer wakeup = this.wakeupMap.firstKey();
                if (this.now < wakeup) {
                    return;
                }
                LOG.info("WakeUpMap : " + wakeupMap);
                LOG.info("ClusterIds : " + clusterIds);
                LOG.info("Scheduling now : " + wakeup);
                List<Long> currentClusterIds = this.wakeupMap.get(wakeup);
                for (Long clusterId : currentClusterIds) {
                    // Schedule whatever needs rebalancing
                    LOG.info("Rebalancing : " + clusterId + " : " + SimpleDRSAutomaticEnable.valueIn(clusterId));
                    if (SimpleDRSAutomaticEnable.valueIn(clusterId)) {
                        manager.schedule(clusterId);
                    }
                    // Find out when they have to run next
                    Integer interval = SimpleDRSAutomaticInterval.valueIn(clusterId);
                    interval += wakeup;
                    addToWakeupMap(interval, clusterId);
                    this.wakeupMap.remove(wakeup);
                }

                // Were any new clusters added ?
                // TODO : maybe move this to a message bus subscriber ? Do we even publish
                // cluster add / remove events ?
                SearchCriteria<ClusterVO> sc = sb.create();
                sc.setParameters("id", clusterIds);
                List<ClusterVO> clusters = clusterDao.search(sc, null);
                addClustersToSchedule(clusters);

                // TODO : were clustes deleted ?
            } catch (final Throwable t) {
                LOG.error("Error trying to run schedule DRS task", t);
            }
        }

        // This gets called only once so we can't set variable delays. So we've
        // set it to wake up every 5 min and check the wakup map for clusters to
        // rebalance
        @Override
        public Long getDelay() {
            return delay * minute;
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        backgroundPollManager.submitTask(new ScheduleDRSTask(this, clusterDao));
        return true;
    }

    @Override
    public boolean start() {
        super.start();
        initDrsProvidersMap();
        initDrsAlgorithmsMap();

        if (drsProvidersMap.containsKey(SimpleDRSProvider.value())) {
            configuredDRSProvider = drsProvidersMap.get(SimpleDRSProvider.value());
        }
        if (configuredDRSProvider == null) {
            LOG.error("Failed to find valid configured DRS provider, please check!");
            return false;
        }
        if (drsAlgorithmsMap.containsKey(SimpleDRSRebalancingAlgorithm.value())) {
            configuredDRSAlgorithm = drsAlgorithmsMap.get(SimpleDRSRebalancingAlgorithm.value());
        }
        if (configuredDRSAlgorithm == null) {
            LOG.error("Failed to find valid configured DRS algorithm, please check!");
            return false;
        }

        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ScheduleDRSCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return SimpleDRSManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] { SimpleDRSProvider, SimpleDRSRebalancingAlgorithm, SimpleDRSAutomaticEnable,
                SimpleDRSAutomaticInterval, SimpleDRSIterations, SimpleDRSImbalanceThreshold };
    }

    @Override
    public List<String> listProviderNames() {
        List<String> list = new LinkedList<>();
        if (MapUtils.isNotEmpty(drsProvidersMap)) {
            for (String key : drsProvidersMap.keySet()) {
                list.add(key);
            }
        }
        return list;
    }

    @Override
    public double calculateClusterImbalance(long clusterId) {
        return configuredDRSProvider.calculateClusterImbalance(clusterId);
    }

    @Override
    public boolean performWorkloadRebalance(SimpleDRSWorkload workload, SimpleDRSResource destination) {
        return configuredDRSProvider.performWorkloadRebalance(workload, destination);
    }

    @Override
    public boolean isClusterImbalanced(long clusterId) {
        double clusterImbalance = calculateClusterImbalance(clusterId);
        Double threshold = SimpleDRSImbalanceThreshold.valueIn(clusterId);
        return configuredDRSAlgorithm.isClusterImbalanced(clusterImbalance, threshold);
    }

    @Override
    public List<SimpleDRSResource> findCompatibleDestinationResourcesForWorkloadRebalnce(SimpleDRSWorkload workload) {
        return null;
    }

    @Override
    public SimpleDRSRebalance calculateWorkloadRebalanceCostBenefit(SimpleDRSWorkload workload,
            SimpleDRSResource destination) {
        return null;
    }

    public Map<SimpleDRSResource, List<SimpleDRSWorkload>> findResourcesAndWorkloadsToBalance(long clusterId) {
        Map<SimpleDRSResource, List<SimpleDRSWorkload>> map = new HashMap<>();
        List<SimpleDRSResource> resources = configuredDRSProvider.findResourcesToBalance(clusterId);
        for (SimpleDRSResource resource : resources) {
            List<SimpleDRSWorkload> workloadsInResource = configuredDRSProvider.findWorkloadsInResource(clusterId,
                    resource.getId());
            map.put(resource, workloadsInResource);
        }
        return map;
    }

    public void schedule(long clusterId) {
        SimpleDRSJobInfo info = new SimpleDRSJobInfo(clusterId);
        schedule(info);
    }

    @Override
    public void schedule(SimpleDRSJobInfo info) {
        Long clusterId = info.getClusterId();

        LOG.info("Scheduling DRS for : " + clusterId);

        if (clusterId == null) {
            LOG.warn("Trying to balance a null cluster are we?");
            return;
        }

        // TODO : Check if drs enabled on cluster

        // if (!isClusterImbalanced(clusterId)) {
        // return;
        // }

        final CallContext context = CallContext.current();
        final User callingUser = context.getCallingUser();
        final Account callingAccount = context.getCallingAccount();

        AsyncJobVO job = new AsyncJobVO();
        job.setRelated("");
        job.setDispatcher(SimpleDRSDispatcher.SIMPLE_DRS_DISPATCHER);
        job.setCmd(info.getClass().getName());
        job.setAccountId(callingAccount.getId());
        job.setUserId(callingUser.getId());

        job.setCmdInfo(JobSerializerHelper.toObjectSerializedString(info));

        asyncJobManager.submitAsyncJob(job);
        // Maybe get the jobid and do something about it ?
    }

    @Override
    public void balanceCluster(SimpleDRSJobInfo info) {
        Long clusterId = info.getClusterId();
        if (clusterId == null) {
            LOG.warn("Trying to balance a null cluster are we?");
            return;
        }
        LOG.info("Balancing : " + clusterId);
    }

    @Override
    public List<SimpleDRSWorkload> getWorkloadsToRebalance(long clusterId) {
        return null;
    }
}
