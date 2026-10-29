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
package org.apache.cloudstack.metrics;

public interface PrometheusExporter {

    /**
     * Update the Prometheus metrics in text format.
     *
     * NOTE: capacity data is refreshed independently by {@code AlertManagerImpl}'s own
     * periodic {@code CapacityChecker} timer. Do NOT force a synchronous
     * {@code recalculateCapacity()} call here: it spins up a fresh thread pool per host
     * and per storage pool across ALL zones on every single scrape, so with Z zones a
     * single Prometheus scrape triggered Z redundant full recalculations. That extra,
     * uncoordinated load compounds over time and can lead to {@code scrape_duration_seconds}
     * climbing until a management-server restart.
     *
     * @see PrometheusExporterImpl#updateMetrics()
     */
    void updateMetrics();

    /**
     * @return the latest Prometheus metrics refreshed by {@link #updateMetrics()}.
     */
    String getMetrics();
}
