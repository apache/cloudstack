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
package org.apache.cloudstack.applicationcluster;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.command.user.applicationcluster.ListApplicationClusterCmd;
import org.apache.cloudstack.api.response.ApplicationClusterResponse;
import org.apache.cloudstack.api.response.ListResponse;

public interface ApplicationClusterService extends PluggableService {

    ApplicationCluster findById(final Long id);

    ApplicationCluster createContainerCluster(String name,
                                          String displayName,
                                          Long zoneId,
                                          Long serviceOffering,
                                          Account owner,
                                          Long networkId,
                                          String sshKeyPair,
                                          Long nodeCount,
                                          String dockerRegistryUsername,
                                          String dockerRegistryPassword,
                                          String dockerRegistryUrl,
                                          String dockerRegistryEmail
                                            ) throws InsufficientCapacityException,
                     ResourceAllocationException, ManagementServerException;

    boolean startContainerCluster(long containerClusterId, boolean onCreate) throws ManagementServerException,
            ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException;

    boolean stopContainerCluster(long containerClusterId) throws ManagementServerException;

    boolean deleteContainerCluster(Long containerClusterId) throws ManagementServerException;

    ListResponse<ApplicationClusterResponse>  listApplicationClusters(ListApplicationClusterCmd cmd);

    ApplicationClusterResponse createContainerClusterResponse(long containerClusterId);

}
