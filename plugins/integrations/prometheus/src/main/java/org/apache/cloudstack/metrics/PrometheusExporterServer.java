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

import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.utils.component.Manager;

public interface PrometheusExporterServer extends Manager {

    ConfigKey<Boolean> EnablePrometheusExporter = new ConfigKey<>("Advanced", Boolean.class, "prometheus.exporter.enable", "false",
            "Enable the prometheus exporter plugin, management server restart needed.", false);

    ConfigKey<Integer> PrometheusExporterServerPort = new ConfigKey<>("Advanced", Integer.class, "prometheus.exporter.port", "9595",
            "The prometheus exporter server port", true);

    ConfigKey<String> PrometheusExporterAllowedAddresses = new ConfigKey<>("Advanced", String.class, "prometheus.exporter.allowed.ips", "127.0.0.1",
            "List of comma separated prometheus server ips (with no spaces) that should be allowed to access the URLs", true);

    ConfigKey<Integer> PrometheusExporterOfferingCountLimit = new ConfigKey<>("Advanced", Integer.class, "prometheus.exporter.offering.output.limit", "-1",
            "Limit the number of output for cloudstack_vms_total_by_size to the provided value. -1 for unlimited output.", true);
}
