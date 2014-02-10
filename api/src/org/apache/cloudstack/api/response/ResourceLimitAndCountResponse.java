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

/**This interface is implemented by AccountResponse and ProjectResponse as both of them
have limits and resource count **/

package org.apache.cloudstack.api.response;

public interface ResourceLimitAndCountResponse {

    public void setNetworkLimit(String networkLimit);

    public void setNetworkTotal(Long networkTotal);

    public void setNetworkAvailable(String networkAvailable);

    public void setVpcLimit(String vpcLimit);

    public void setVpcTotal(Long vpcTotal);

    public void setVpcAvailable(String vpcAvailable);

    public void setCpuLimit(String cpuLimit);

    public void setCpuTotal(Long cpuTotal);

    public void setCpuAvailable(String cpuAvailable);

    public void setMemoryLimit(String memoryLimit);

    public void setMemoryTotal(Long memoryTotal);

    public void setMemoryAvailable(String memoryAvailable);

    public void setPrimaryStorageLimit(String primaryStorageLimit);

    public void setPrimaryStorageTotal(Long primaryStorageTotal);

    public void setPrimaryStorageAvailable(String primaryStorageAvailable);

    public void setSecondaryStorageLimit(String secondaryStorageLimit);

    public void setSecondaryStorageTotal(Long secondaryStorageTotal);

    public void setSecondaryStorageAvailable(String secondaryStorageAvailable);

    public void setVmLimit(String vmLimit);

    public void setVmTotal(Long vmTotal);

    public void setVmAvailable(String vmAvailable);

    public void setIpLimit(String ipLimit);

    public void setIpTotal(Long ipTotal);

    public void setIpAvailable(String ipAvailable);

    public void setVolumeLimit(String volumeLimit);

    public void setVolumeTotal(Long volumeTotal);

    public void setVolumeAvailable(String volumeAvailable);

    public void setSnapshotLimit(String snapshotLimit);

    public void setSnapshotTotal(Long snapshotTotal);

    public void setSnapshotAvailable(String snapshotAvailable);

    public void setTemplateLimit(String templateLimit);

    public void setTemplateTotal(Long templateTotal);

    public void setTemplateAvailable(String templateAvailable);

    public void setVmStopped(Integer vmStopped);

    public void setVmRunning(Integer vmRunning);

}
