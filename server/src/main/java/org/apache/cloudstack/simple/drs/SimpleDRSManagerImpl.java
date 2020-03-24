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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.component.ManagerBase;

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

        private ScheduleDRSTask(SimpleDRSManager manager, ClusterDao clusterDao) {
            this.manager = manager;
            this.clusterDao = clusterDao;
        }

        @Override
        protected void runInContext() {
            try {
                List<ClusterVO> clusters = clusterDao.listAll();
                LOG.info("Got clusters to shedule rebalancing : " + clusters);
                if (clusters == null) {
                    return;
                }
                for (ClusterVO cluster : clusters) {
                    Long clusterId = cluster.getId();
                    LOG.info("Rebalancing : " + clusterId + " : " + SimpleDRSAutomaticEnable.valueIn(clusterId));

                    if (SimpleDRSAutomaticEnable.valueIn(clusterId)) {
                        manager.schedule(cluster.getId());
                    }
                }
            } catch (final Throwable t) {
                LOG.error("Error trying to run schedule DRS task", t);
            }
        }

        @Override
        public Long getDelay() {
            return SimpleDRSAutomaticInterval.value() * 1000L;
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
    public double getClusterImbalance(long clusterId) {
        return configuredDRSProvider.calculateClusterImbalance(clusterId);
    }

    @Override
    public boolean isClusterImbalanced(long clusterId) {
        double clusterImbalance = getClusterImbalance(clusterId);
        Double threshold = SimpleDRSImbalanceThreshold.valueIn(clusterId);
        return configuredDRSAlgorithm.isClusterImbalanced(clusterImbalance, threshold);
    }

    @Override
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
        ScheduleDRSCmd cmd = new ScheduleDRSCmd(clusterId);
        schedule(cmd);
    }

    @Override
    public void schedule(ScheduleDRSCmd cmd) {
        Long clusterId = cmd.getClusterId();

        LOG.info("Scheduling DRS for : " + clusterId);

        // TODO : Check if drs enabled on cluster

        // if (!isClusterImbalanced(clusterId)) {
        //     return;
        // }

        final CallContext context = CallContext.current();
        final User callingUser = context.getCallingUser();
        final Account callingAccount = context.getCallingAccount();

        AsyncJobVO job = new AsyncJobVO();
        job.setRelated("");
        job.setDispatcher(SimpleDRSDispatcher.SIMPLE_DRS_DISPATCHER);
        job.setCmd(cmd.getClass().getName());
        job.setAccountId(callingAccount.getId());
        job.setUserId(callingUser.getId());

        job.setCmdInfo(JobSerializerHelper.toObjectSerializedString(cmd));

        asyncJobManager.submitAsyncJob(job);
        // Maybe get the jobid and do something about it ?
    }

    @Override
    public void balanceCluster(ScheduleDRSCmd cmd) {
        Long clusterId = cmd.getClusterId();
        LOG.info("Balancing : " + clusterId);
        return;
    }
}
