/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.cluster;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.org.Cluster;
import com.cloud.server.ManagementServer;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.cluster.ScheduleDrsCmd;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.cloud.org.Grouping.AllocationState.Disabled;
import static java.lang.Math.round;

public class ClusterDrsServiceImpl extends ManagerBase implements ClusterDrsService, PluggableService {

    private static final Logger logger = Logger.getLogger(ClusterDrsServiceImpl.class);
    @Inject
    ClusterDao clusterDao;

    @Inject
    HostDao hostDao;

    @Inject
    VMInstanceDao vmInstanceDao;

    @Inject
    UserVmService userVmService;

    @Inject
    ManagementServer managementServer;

    List<ClusterDrsAlgorithm> drsAlgorithms = new ArrayList<>();

    Map<String, ClusterDrsAlgorithm> drsAlgorithmMap = new HashMap<>();
    private Date currentTimestamp;

    public void setDrsAlgorithms(final List<ClusterDrsAlgorithm> drsAlgorithms) {
        this.drsAlgorithms = drsAlgorithms;
    }

    @Override
    public boolean start() {
        drsAlgorithmMap.clear();
        for (final ClusterDrsAlgorithm algorithm: drsAlgorithms) {
            drsAlgorithmMap.put(algorithm.getName(), algorithm);
        }

        currentTimestamp = DateUtils.addMinutes(new Date(), 1);

        final TimerTask schedulerPollTask = new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
                try {
                    poll(currentTimestamp);
                } catch (final Exception e) {
                    logger.error("Error while running DRS", e);
                }
            }
        };
        Timer vmSchedulerTimer = new Timer("VMSchedulerPollTask");
        vmSchedulerTimer.schedule(schedulerPollTask, 5000L, 60 * 1000L);
        return true;
    }

    private ClusterDrsAlgorithm getDrsAlgorithm(String algoName) {
        if (drsAlgorithmMap.containsKey(algoName)) {
            return drsAlgorithmMap.get(algoName);
        }
        throw new CloudRuntimeException("Invalid algorithm configured!");
    }


    @Override
    public String getConfigComponentName() {
        return ClusterDrsService.class.getSimpleName();
    }

    /**
     * @return The list of config keys provided by this configurable.
     */
    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {ClusterDrsEnabled, ClusterDrsInterval, ClusterDrsIterations, ClusterDrsAlgorithm, ClusterDrsThreshold, ClusterDrsMetric};
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(ScheduleDrsCmd.class);
        return cmdList;
    }

    @Override
    public SuccessResponse executeDrs(ScheduleDrsCmd cmd) {
        Cluster cluster = clusterDao.findById(cmd.getId());
        if (cluster == null) {
            throw new InvalidParameterValueException("Unable to find the cluster by id=" + cmd.getId());
        }
        SuccessResponse response = new SuccessResponse();
        // TODO: Send response with the exact cause
        if (cluster.getAllocationState() == Disabled || cluster.getClusterType() != Cluster.ClusterType.CloudManaged || cmd.getIterations() == 0) {
            response.setSuccess(false);
            return response;
        }
        try {
            int iterations = executeDrs(cluster, cmd.getIterations());
            response.setDisplayText("Executed " + iterations + " iterations for cluster " + cluster.getName());
            response.setSuccess(true);
            return response;
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException("Unable to schedule DRS", e);
        }

    }

    /**
     * Fetch DRS configuration for cluster.
     * Check if DRS needs to run.
     * Run DRS as per the configured algorithm.
     */
    private int executeDrs(Cluster cluster, double iterationPercentage) throws ConfigurationException {
        // TODO: Take a lock on drs for a cluster to avoid multiple drs operations at the same time on a cluster

        if (cluster.getAllocationState() == Disabled || cluster.getClusterType() != Cluster.ClusterType.CloudManaged || iterationPercentage == 0) {
            return -1;
        }
        ClusterDrsAlgorithm algorithm = getDrsAlgorithm(ClusterDrsAlgorithm.valueIn(cluster.getId()));
        int iteration = 0;
        List<HostVO> hostList = hostDao.findByClusterId(cluster.getId());
        List<VMInstanceVO> vmList = vmInstanceDao.listByClusterId(cluster.getId());
        long maxIterations = round(iterationPercentage * vmList.size());

        Map<Long, List<VirtualMachine>> originalHostVmMap = new HashMap<>();
        for (HostVO host : hostList) {
            originalHostVmMap.put(host.getId(), new ArrayList<>());
        }
        for (VirtualMachine vm : vmList) {
            originalHostVmMap.get(vm.getHostId()).add(vm);
        }
        Map<Long, List<VirtualMachine>> hostVmMap = originalHostVmMap;
        while (algorithm.needsDrs(cluster.getId(), hostVmMap) && iteration < maxIterations) {
            Pair<Host, VirtualMachine> bestMigration = null;
            double maxImprovement = 0;
            hostVmMap = new HashMap<>();
            for (HostVO host : hostList) {
                hostVmMap.put(host.getId(), new ArrayList<>());
            }
            for (VirtualMachine vm : vmList) {
                hostVmMap.get(vm.getHostId()).add(vm);
            }
            for (VirtualMachine vm : vmList) {
                Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>> hostsForMigrationOfVM = managementServer.listHostsForMigrationOfVM(vm.getId(), 0L, (long) hostList.size(), null);
                List<? extends Host> compatibleDestinationHosts = hostsForMigrationOfVM.second();
                Map<Host, Boolean> requiresStorageMotion = hostsForMigrationOfVM.third();
                for (Host destHost : compatibleDestinationHosts) {
                    Ternary<Double, Double, Double> metrics = algorithm.getMetrics(cluster.getId(), hostVmMap, vm, destHost, requiresStorageMotion.get(destHost));
                    Double improvement = metrics.first();
                    Double cost = metrics.second();
                    Double benefit = metrics.third();
                    if (benefit > cost && (improvement > maxImprovement)) {
                            bestMigration = new Pair<>(destHost, vm);
                            maxImprovement = improvement;
                    }
                }
            }
            if (bestMigration == null) {
                break;
            }
            Host destHost = bestMigration.first();
            VirtualMachine vm = bestMigration.second();

            if (originalHostVmMap.get(destHost.getId()).contains(vm)) {
                logger.warn("VM getting migrated to it's original host. Stopping DRS.");
                break;
            }

            try {
                userVmService.migrateVirtualMachine(vm.getId(), destHost);
                logger.debug("Migrated VM " + vm.getInstanceName() + " from host " + vm.getHostId() + " to host " + destHost.getId());
            } catch (ResourceUnavailableException e) {
                logger.warn("Exception: ", e);
                throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, e.getMessage());
            } catch (VirtualMachineMigrationException | ConcurrentOperationException | ManagementServerException e) {
                logger.warn("Exception: ", e);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
            }
            vmList = vmInstanceDao.listByClusterId(cluster.getId());
            iteration++;
        }

        return iteration;
    }

    private void executeDrsForAllClusters() {
        List<ClusterVO> clusterList = clusterDao.listAll();
        for (ClusterVO cluster : clusterList) {
            if (cluster.getAllocationState() == Disabled || cluster.getClusterType() != Cluster.ClusterType.CloudManaged || ClusterDrsEnabled.valueIn(cluster.getId()).equals(Boolean.FALSE)) {
                continue;
            }

            try {
                double iterations = executeDrs(cluster, ClusterDrsIterations.valueIn(cluster.getId()));
                logger.debug("Executed " + iterations + " iterations of DRS for cluster " + cluster.getName());
            } catch (ConfigurationException e) {
                throw new CloudRuntimeException("Unable to schedule DRS", e);
            }
        }
    }

    /**
     * This is called from the TimerTask thread periodically about every one minute.
     */
    @Override
    public void poll(Date timestamp) {
        currentTimestamp = DateUtils.round(timestamp, Calendar.MINUTE);
        String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, currentTimestamp);
        logger.debug(String.format("VM scheduler.poll is being called at %s", displayTime));

        GlobalLock lock = GlobalLock.getInternLock("ClusterDrs.poll");
        try {
            if (lock.lock(30)) {
                try {
                    executeDrsForAllClusters();
                } finally {
                    lock.unlock();
                }
            }
        } finally {
            lock.releaseRef();
        }
    }
}
