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
package org.apache.cloudstack.framework.simple.drs;

import com.cloud.utils.component.AdapterBase;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

public abstract class SimpleDRSProviderBase extends AdapterBase implements SimpleDRSProvider {

    public static final String GRANULAR_DRS_DETAIL_NAME = "DRS";

    @Override
    public double calculateClusterImbalance(long clusterId) {
        double[] clusterMetricValues = generateClusterMetricValues(clusterId);
        double mean = calculateClusterMean(clusterMetricValues);
        double standardDeviation = calculateClusterStandardDeviation(clusterMetricValues, mean);
        return standardDeviation / mean;
    }

    protected abstract double[] generateClusterMetricValues(long clusterId);

    private double calculateClusterStandardDeviation(double[] clusterMetricValues, double mean) {
        // Init StandardDeviation with constructor StandardDeviation(false) to use the population standard deviation
        StandardDeviation standardDeviation = new StandardDeviation(false);
        return standardDeviation.evaluate(clusterMetricValues, mean);
    }

    private double calculateClusterMean(double[] clusterMetricValues) {
        return new Mean().evaluate(clusterMetricValues);
    }
}
