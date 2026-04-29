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
package org.apache.cloudstack.engine.datacenter.entity.api;

import com.cloud.hypervisor.Hypervisor.HypervisorType;

public interface HostEntity extends DataCenterResourceEntity {

    /**
     * @return total amount of memory.
     */
    Long getTotalMemory();

    /**
     * @return # of cores in a machine.  Note two cpus with two cores each returns 4.
     */
    Integer getCpus();

    /**
     * @return speed of each cpu in mhz.
     */
    Long getSpeed();

    /**
     * @return the pod.
     */
    Long getPodId();

    /**
     * @return availability zone.
     */
    long getDataCenterId();

    /**
     * @return type of hypervisor
     */
    HypervisorType getHypervisorType();

    /**
     * @return the mac address of the host.
     */
    String getGuid();

    Long getClusterId();

}
