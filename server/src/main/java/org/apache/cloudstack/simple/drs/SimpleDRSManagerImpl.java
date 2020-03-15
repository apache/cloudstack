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

import com.cloud.utils.component.ManagerBase;
import org.apache.cloudstack.api.command.admin.simple.drs.ScheduleDRSCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.simple.drs.SimpleDRSProvider;
import org.apache.cloudstack.framework.simple.drs.SimpleDRSRebalancingAlgorithm;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SimpleDRSManagerImpl extends ManagerBase implements SimpleDRSManager {

    public static final Logger LOG = Logger.getLogger(SimpleDRSManagerImpl.class);

    private static SimpleDRSProvider configuredDRSProvider;
    private static SimpleDRSRebalancingAlgorithm configuredDRSAlgorithm;
    private static Map<String, SimpleDRSProvider> drsProvidersMap = new HashMap<>();
    private static Map<String, SimpleDRSRebalancingAlgorithm> drsAlgorithmsMap = new HashMap<>();

    private List<SimpleDRSProvider> drsProviders;
    private List<SimpleDRSRebalancingAlgorithm> drsAlgorithms;

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
        return new ConfigKey[] {
                SimpleDRSProvider, SimpleDRSRebalancingAlgorithm, SimpleDRSAutomaticEnable, SimpleDRSAutomaticInterval,
                SimpleDRSIterations, SimpleDRSImbalanceThreshold
        };
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
}
