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

import com.cloud.utils.component.AdapterBase;
import org.apache.cloudstack.framework.simple.drs.DRSRebalancingAlgorithm;

import java.util.List;

public class SimpleDRSBalancedAlgorithm extends AdapterBase implements DRSRebalancingAlgorithm {

    private static final String ALGORITHM_NAME = "balanced";

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    @Override
    public boolean isClusterImbalanced(long clusterId) {
        return false;
    }

    @Override
    public List<String> findResourcesToBalance() {
        return null;
    }

    @Override
    public List<String> findWorkloadsToBalance() {
        return null;
    }

    @Override
    public void sortRebalancingPlansByCost() {

    }

    @Override
    public void sortRebalancingPlansByBenefit() {

    }
}
