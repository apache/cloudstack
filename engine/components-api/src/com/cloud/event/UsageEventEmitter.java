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

package com.cloud.event;

import java.util.Map;

public interface UsageEventEmitter {

    public void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
            Long size, String entityType, String entityUUID);

    public void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
            Long size, String entityType, String entityUUID, boolean displayResource);

    public void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
            Long size, Long virtualSize, String entityType, String entityUUID);

    public  void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, String entityType, String entityUUID);


    public void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, String entityType, String entityUUID, boolean diplayResource);


    public void publishUsageEvent(String usageType, long accountId, long zoneId, long ipAddressId, String ipAddress, boolean isSourceNat, String guestType,
            boolean isSystem, String entityType, String entityUUID);


    public void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
            String resourceType, String entityType, String entityUUID, boolean displayResource);

    public void publishUsageEvent(String usageType, long accountId, long zoneId, long vmId, long securityGroupId, String entityType, String entityUUID);


    public void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
            String resourceType, String entityType, String entityUUID, Map<String, String> details, boolean displayResource);

    public void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, Long size);


    public void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, Long size,
            Long virtualSize);

    public void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName);


    public void saveUsageEvent(String usageType, long accountId, long zoneId, long ipAddressId, String ipAddress, boolean isSourceNat, String guestType,
            boolean isSystem);

    public void saveUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId,
            String resourceType);

    public void saveUsageEvent(String usageType, long accountId, long zoneId, long vmId, long securityGroupId);

}
