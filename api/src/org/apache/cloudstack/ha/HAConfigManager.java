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

package org.apache.cloudstack.ha;

import com.cloud.dc.DataCenter;
import com.cloud.org.Cluster;

import java.util.List;

/**
 * @since 4.11
 */
public interface HAConfigManager {
    /**
     * Configures HA for a resource by accepting the resource type and HA provider
     * @param resourceId the ID of the resource
     * @param resourceType the type of the resource
     * @param haProvider the name of the HA provider
     */
    boolean configureHA(Long resourceId, HAResource.ResourceType resourceType, String haProvider);

    /**
     * Enables HA for resource Id of a specific resource type
     * @param resourceId the ID of the resource
     * @param resourceType the type of the resource
     * @return returns true on successful enable
     */
    boolean enableHA(Long resourceId, HAResource.ResourceType resourceType);

    /**
     * Disables HA for resource Id of a specific resource type
     * @param resourceId the ID of the resource
     * @param resourceType the type of the resource
     * @return returns true on successful disable
     */
    boolean disableHA(Long resourceId, HAResource.ResourceType resourceType);

    /**
     * Enables HA across a cluster
     * @param cluster the cluster
     * @return returns operation success
     */
    boolean enableHA(final Cluster cluster);

    /**
     * Disables HA across a cluster
     * @param cluster the cluster
     * @return returns operation success
     */
    boolean disableHA(final Cluster cluster);

    /**
     * Enables HA across a zone
     * @param zone the zone
     * @return returns operation success
     */
    boolean enableHA(final DataCenter zone);

    /**
     * Disables HA across a zone
     * @param zone the zone
     * @return returns operation success
     */
    boolean disableHA(final DataCenter zone);

    /**
     * Returns list of HA config for resources, by resource ID and/or type if provided
     * @param resourceId (optional) ID of the resource
     * @param resourceType (optional) type of the resource
     * @return returns list of ha config for the resource
     */
    List<HAConfig> listHAResources(final Long resourceId, final HAResource.ResourceType resourceType);

    /**
     * Returns list of HA providers for resources
     * @param resourceType type of the resource
     * @param entityType sub-type of the resource
     * @return returns list of ha provider names
     */
    List<String> listHAProviders(final HAResource.ResourceType resourceType, final HAResource.ResourceSubType entityType);
}
