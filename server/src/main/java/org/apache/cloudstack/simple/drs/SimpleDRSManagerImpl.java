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

import org.apache.cloudstack.api.command.admin.simple.drs.ScheduleDRSCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.simple.drs.DRSProvider;
import org.apache.cloudstack.framework.simple.drs.DRSRebalancingAlgorithm;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SimpleDRSManagerImpl implements SimpleDRSManager {

    private static Map<String, DRSProvider> drsProvidersMap = new HashMap<>();
    private static Map<String, DRSRebalancingAlgorithm> drsAlgorithmsMap = new HashMap<>();

    private List<DRSProvider> drsProviders;
    private List<DRSRebalancingAlgorithm> drsAlgorithms;

    ////////////////////////////////////////////////////
    /////////////// Init DRS providers /////////////////
    ////////////////////////////////////////////////////

    public void setDrsProviders(List<DRSProvider> drsProviders) {
        this.drsProviders = drsProviders;
        initDrsProvidersMap();
    }

    private void initDrsProvidersMap() {
        if (CollectionUtils.isNotEmpty(drsProviders)) {
            for (DRSProvider provider : drsProviders) {
                drsProvidersMap.put(provider.getProviderName().toLowerCase(), provider);
            }
        }
    }

    ////////////////////////////////////////////////////
    /////////////// Init DRS algorithms ////////////////
    ////////////////////////////////////////////////////

    private void initDrsAlgorithmsMap() {
        if (CollectionUtils.isNotEmpty(drsAlgorithms)) {
            for (DRSRebalancingAlgorithm algorithm : drsAlgorithms) {
                drsAlgorithmsMap.put(algorithm.getAlgorithmName().toLowerCase(), algorithm);
            }
        }
    }

    public void setDrsAlgorithms(List<DRSRebalancingAlgorithm> drsAlgorithms) {
        this.drsAlgorithms = drsAlgorithms;
        initDrsAlgorithmsMap();
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
                SimpleDRSProvider, SimpleDRSRebalancingAlgorithm
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
}
