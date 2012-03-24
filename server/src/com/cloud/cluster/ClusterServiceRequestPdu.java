package com.cloud.cluster;

public class ClusterServiceRequestPdu extends ClusterServicePdu {

    private String responseResult;
    private long startTick;
    private long timeout;
    
    public ClusterServiceRequestPdu() {
        startTick = System.currentTimeMillis();
        timeout = -1;
        setPduType(PDU_TYPE_REQUEST);
    }

    public String getResponseResult() {
        return responseResult;
    }

    public void setResponseResult(String responseResult) {
        this.responseResult = responseResult;
    }

    public long getStartTick() {
        return startTick;
    }

    public void setStartTick(long startTick) {
        this.startTick = startTick;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
