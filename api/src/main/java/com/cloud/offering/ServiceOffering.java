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
package com.cloud.offering;

import java.util.Date;

import org.apache.cloudstack.acl.InfrastructureEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

/**
 * offered.
 */
public interface ServiceOffering extends DiskOffering, InfrastructureEntity, InternalIdentity, Identity {
    public static final String consoleProxyDefaultOffUniqueName = "Cloud.com-ConsoleProxy";
    public static final String ssvmDefaultOffUniqueName = "Cloud.com-SecondaryStorage";
    public static final String routerDefaultOffUniqueName = "Cloud.Com-SoftwareRouter";
    public static final String elbVmDefaultOffUniqueName = "Cloud.Com-ElasticLBVm";
    public static final String internalLbVmDefaultOffUniqueName = "Cloud.Com-InternalLBVm";
    // leaving cloud.com references as these are identifyers and no real world addresses (check against DB)

    public enum StorageType {
        local, shared
    }

    @Override
    String getDisplayText();

    @Override
    Date getCreated();

    @Override
    String getTags();

    /**
     * @return user readable description
     */
    @Override
    String getName();

    /**
     * @return is this a system service offering
     */
    @Override
    boolean isSystemUse();

    /**
     * @return # of cpu.
     */
    Integer getCpu();

    /**
     * @return speed in mhz
     */
    Integer getSpeed();

    /**
     * @return ram size in megabytes
     */
    Integer getRamSize();

    /**
     * @return Does this service plan offer HA?
     */
    boolean isOfferHA();

    /**
     * @return Does this service plan offer VM to use CPU resources beyond the service offering limits?
     */
    boolean getLimitCpuUse();

    /**
     * @return Does this service plan support Volatile VM that is, discard VM's root disk and create a new one on reboot?
     */
    boolean isVolatileVm();

    /**
     * @return the rate in megabits per sec to which a VM's network interface is throttled to
     */
    Integer getRateMbps();

    /**
     * @return the rate megabits per sec to which a VM's multicast&broadcast traffic is throttled to
     */
    Integer getMulticastRateMbps();

    /**
     * @return whether or not the service offering requires local storage
     */
    @Override
    boolean isUseLocalStorage();

    /**
     * @return tag that should be present on the host needed, optional parameter
     */
    String getHostTag();

    boolean getDefaultUse();

    String getSystemVmType();

    String getDeploymentPlanner();

    boolean isDynamic();

    boolean isDynamicScalingEnabled();
}
