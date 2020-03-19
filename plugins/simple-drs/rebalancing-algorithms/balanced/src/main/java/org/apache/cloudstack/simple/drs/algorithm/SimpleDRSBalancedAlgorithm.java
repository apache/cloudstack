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
package org.apache.cloudstack.simple.drs.algorithm;

import org.apache.cloudstack.framework.simple.drs.SimpleDRSRebalancingAlgorithm;
import org.apache.cloudstack.framework.simple.drs.SimpleDRSRebalancingAlgorithmBase;
import org.apache.cloudstack.simple.drs.SimpleDRSResource;
import org.apache.cloudstack.simple.drs.SimpleDRSWorkload;
import org.apache.log4j.Logger;

import java.util.List;

public class SimpleDRSBalancedAlgorithm extends SimpleDRSRebalancingAlgorithmBase implements SimpleDRSRebalancingAlgorithm {

    private static final String ALGORITHM_NAME = "balanced";
    public static final Logger LOG = Logger.getLogger(SimpleDRSBalancedAlgorithm.class);

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    @Override
    public boolean isClusterImbalanced(double clusterImbalance, double clusterImbalanceThreshold) {
        return clusterImbalance > clusterImbalanceThreshold;
    }

    @Override
    public List<SimpleDRSResource> findResourcesToBalance(long clusterId) {
        return null;
    }

    @Override
    public List<SimpleDRSWorkload> findWorkloadsToBalance() {
        return null;
    }

    @Override
    public void sortRebalancingPlansByCost() {
    }

    @Override
    public void sortRebalancingPlansByBenefit() {

    }
}
