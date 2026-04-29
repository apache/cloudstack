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
package com.cloud.network.router;

/**
 *  bridge internal and external traffic.
 */
public interface VirtualRouterAutoScale {

    int MBITS_TO_BYTES = 1024 * 1024 / 8;

    enum VirtualRouterAutoScaleCounter {
        NETWORK_RECEIVED_AVERAGE_MBPS("public.network.received.average.mbps"),
        NETWORK_TRANSMIT_AVERAGE_MBPS("public.network.transmit.average.mbps"),
        LB_AVERAGE_CONNECTIONS("virtual.network.lb.average.connections");

        String value;
        VirtualRouterAutoScaleCounter(String value) {
            this.value = value;
        }

        String getValue() {
            return value;
        }
        public static VirtualRouterAutoScaleCounter fromValue(String value) {
            VirtualRouterAutoScaleCounter[] values = VirtualRouterAutoScaleCounter.values();
            for(VirtualRouterAutoScaleCounter v : values) {
                if(v.getValue().equals(value)) {
                    return v;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return getValue();
        }
    }

    enum AutoScaleValueType {
        INSTANT_VM ("Instant data for a VM"),
        INSTANT_VM_GROUP ("Instant data for AutoScale VM group"),
        AGGREGATED_VM ("Aggregated data for a VM"),
        AGGREGATED_VM_GROUP ("Aggregated data for AutoScale VM group");

        String description;

        private AutoScaleValueType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public class AutoScaleMetrics {
        VirtualRouterAutoScaleCounter counter;
        Long policyId;
        Long conditionId;
        Long counterId;
        Integer duration;

        public AutoScaleMetrics(VirtualRouterAutoScaleCounter counter, Long policyId, Long conditionId, Long counterId, Integer duration) {
            this.counter = counter;
            this.policyId = policyId;
            this.conditionId = conditionId;
            this.counterId = counterId;
            this.duration = duration;
        }

        public VirtualRouterAutoScaleCounter getCounter() {
            return counter;
        }

        public Long getPolicyId() {
            return policyId;
        }

        public Long getConditionId() {
            return conditionId;
        }

        public Long getCounterId() {
            return counterId;
        }

        public Integer getDuration() {
            return duration;
        }
    }

    public class AutoScaleMetricsValue {
        AutoScaleMetrics metrics;
        AutoScaleValueType type;
        Double value;

        public AutoScaleMetricsValue(AutoScaleMetrics metrics, AutoScaleValueType type, Double value) {
            this.metrics = metrics;
            this.type = type;
            this.value = value;
        }

        public AutoScaleMetrics getMetrics() {
            return metrics;
        }

        public AutoScaleValueType getType() {
            return type;
        }

        public Double getValue() {
            return value;
        }
    }

}
