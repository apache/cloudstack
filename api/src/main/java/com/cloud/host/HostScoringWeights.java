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
package com.cloud.host;

import org.apache.cloudstack.framework.config.ConfigKey;

/**
 * How much each signal counts when ranking hosts by how loaded they are.
 *
 * Shared by initial placement and by rebalancing on purpose. If the two weighted these differently
 * they would disagree about which host is the better one, and rebalancing could move VMs off hosts
 * that placement had just chosen, only for placement to put them back.
 *
 * Weights are relative to each other; only their ratios matter, and zero disables a term. Terms that
 * only make sense for one of the two - how many VMs a host carries, how many started recently - stay
 * with whichever uses them.
 */
public interface HostScoringWeights {

    String WEIGHT_DESCRIPTION_SUFFIX = " Relative weight, only meaningful compared with the other "
            + "host.weighted.* weights. Zero disables the term. Used by both the 'balancedweighted' "
            + "allocation algorithm and the 'weighted' DRS algorithm.";

    ConfigKey<Double> CpuAllocatedWeight = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Double.class, "host.weighted.cpu.allocated.weight", "1.0",
            "How much CPU allocated on a host counts against it." + WEIGHT_DESCRIPTION_SUFFIX,
            true, ConfigKey.Scope.Cluster);

    ConfigKey<Double> CpuUsedWeight = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Double.class, "host.weighted.cpu.used.weight", "2.0",
            "How much measured CPU utilisation counts against a host." + WEIGHT_DESCRIPTION_SUFFIX,
            true, ConfigKey.Scope.Cluster);

    ConfigKey<Double> MemoryAllocatedWeight = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Double.class, "host.weighted.memory.allocated.weight", "1.0",
            "How much memory allocated on a host counts against it." + WEIGHT_DESCRIPTION_SUFFIX,
            true, ConfigKey.Scope.Cluster);

    ConfigKey<Double> MemoryUsedWeight = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED,
            Double.class, "host.weighted.memory.used.weight", "2.0",
            "How much measured memory utilisation counts against a host." + WEIGHT_DESCRIPTION_SUFFIX,
            true, ConfigKey.Scope.Cluster);
}
