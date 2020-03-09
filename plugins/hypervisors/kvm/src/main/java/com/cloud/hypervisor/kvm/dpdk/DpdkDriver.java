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
package com.cloud.hypervisor.kvm.dpdk;

import com.cloud.utils.component.Adapter;

import java.util.Map;

public interface DpdkDriver extends Adapter {

    /**
     * Get the next DPDK port name to be created
     */
    String getNextDpdkPort();

    /**
     * Get the latest DPDK port number created on a DPDK enabled host
     */
    int getDpdkLatestPortNumberUsed();

    /**
     * Add OVS port (if it does not exist) to bridge with DPDK support
     */
    void addDpdkPort(String bridgeName, String port, String vlan, DpdkHelper.VHostUserMode vHostUserMode, String dpdkOvsPath);

    /**
     * Since DPDK user client/server mode, retrieve the guest interfaces mode from the DPDK vHost User mode
     */
    String getGuestInterfacesModeFromDpdkVhostUserMode(DpdkHelper.VHostUserMode dpdKvHostUserMode);

    /**
     * Get DPDK vHost User mode from extra config. If it is not present, server is returned as default
     */
    DpdkHelper.VHostUserMode getDpdkvHostUserMode(Map<String, String> extraConfig);

    /**
     * Check for additional extra 'dpdk-interface' configurations, return them appended
     */
    String getExtraDpdkProperties(Map<String, String> extraConfig);
}
