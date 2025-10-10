package org.apache.cloudstack.storage.strategy;

public interface NASStrategy {
    String createExportPolicy(String svmName, String policyName);
    String addExportRule(String policyName, String clientMatch, String[] protocols, String[] roRule, String[] rwRule);
    String assignExportPolicyToVolume(String volumeUuid, String policyName);
    String enableNFS(String svmUuid);
}

