package org.apache.cloudstack.storage.strategy;

public interface SANStrategy {
    String createLUN(String svmName, String volumeName, String lunName, long sizeBytes, String osType);
    String createIgroup(String svmName, String igroupName, String[] initiators);
    String mapLUNToIgroup(String lunName, String igroupName);
    String enableISCSI(String svmUuid);
}
