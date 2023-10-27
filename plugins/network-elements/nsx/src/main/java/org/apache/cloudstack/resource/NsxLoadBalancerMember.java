package org.apache.cloudstack.resource;

public class NsxLoadBalancerMember {
    private long vmId;
    private String vmIp;
    private int port;

    public NsxLoadBalancerMember(long vmId, String vmIp, int port) {
        this.vmId = vmId;
        this.vmIp = vmIp;
        this.port = port;
    }

    public long getVmId() {
        return vmId;
    }

    public String getVmIp() {
        return vmIp;
    }

    public int getPort() {
        return port;
    }
}
