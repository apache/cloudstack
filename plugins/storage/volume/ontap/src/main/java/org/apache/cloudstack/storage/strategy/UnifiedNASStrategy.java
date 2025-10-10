package org.apache.cloudstack.storage.strategy;

import java.util.Map;

public class UnifiedNASStrategy implements NASStrategy{
    public UnifiedNASStrategy(Map<String, String> details) {
    }

    @Override
    public String createExportPolicy(String svmName, String policyName) {
        return "";
    }

    @Override
    public String addExportRule(String policyName, String clientMatch, String[] protocols, String[] roRule, String[] rwRule) {
        return "";
    }

    @Override
    public String assignExportPolicyToVolume(String volumeUuid, String policyName) {
        return "";
    }

    @Override
    public String enableNFS(String svmUuid) {
        return "";
    }
}
