package com.cloud.event;

public class UsageEventGenerator {

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, Long size) {
        EventUtils.saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, size);
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName) {
        EventUtils.saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName);
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long ipAddressId, String ipAddress, boolean isSourceNat, String guestType, boolean isSystem) {
        EventUtils.saveUsageEvent(usageType, accountId, zoneId, ipAddressId, ipAddress, isSourceNat, guestType, isSystem);
    }

    public static void publishUsageEvent(String usageType, long accountId, long zoneId, long resourceId, String resourceName, Long offeringId, Long templateId, String resourceType) {
        EventUtils.saveUsageEvent(usageType, accountId, zoneId, resourceId, resourceName, offeringId, templateId, resourceType);
    }

    public static void publishUsageEvent(String usageType, long accountId,long zoneId, long vmId, long securityGroupId) {
        EventUtils.saveUsageEvent(usageType, accountId, zoneId, vmId, securityGroupId);
    }
}
