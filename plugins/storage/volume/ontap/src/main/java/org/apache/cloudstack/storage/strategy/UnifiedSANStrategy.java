package org.apache.cloudstack.storage.strategy;

import java.util.Map;

public class UnifiedSANStrategy implements SANStrategy{
    public UnifiedSANStrategy(Map<String, String> details) {

    }

    @Override
    public String createLUN(String svmName, String volumeName, String lunName, long sizeBytes, String osType) {
        return "";
    }

    @Override
    public String createIgroup(String svmName, String igroupName, String[] initiators) {
        return "";
    }

    @Override
    public String mapLUNToIgroup(String lunName, String igroupName) {
        return "";
    }

    @Override
    public String enableISCSI(String svmUuid) {
        return "";
    }
}
