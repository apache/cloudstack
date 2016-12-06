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
