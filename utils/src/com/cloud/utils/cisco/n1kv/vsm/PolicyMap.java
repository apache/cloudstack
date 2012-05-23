package com.cloud.utils.cisco.n1kv.vsm;

public class PolicyMap {
    public String policyMapName;
    public int committedRate;
    public int burstRate;
    public int peakRate;

    PolicyMap() {
        policyMapName = null;
        committedRate = 0;
        burstRate = 0;
        peakRate = 0;
    }
}
