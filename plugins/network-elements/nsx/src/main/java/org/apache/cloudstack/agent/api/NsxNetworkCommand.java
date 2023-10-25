package org.apache.cloudstack.agent.api;

import java.util.Objects;

public class NsxNetworkCommand extends NsxCommand {
    private Long networkResourceId;
    private String networkResourceName;
    private boolean isResourceVpc;
    private Long vmId;
    private String publicIp;
    private String vmIp;

    public NsxNetworkCommand(long domainId, long accountId, long zoneId, Long networkResourceId, String networkResourceName,
                             boolean isResourceVpc, Long vmId, String publicIp, String vmIp) {
        super(domainId, accountId, zoneId);
        this.networkResourceId = networkResourceId;
        this.networkResourceName = networkResourceName;
        this.isResourceVpc = isResourceVpc;
        this.vmId = vmId;
        this.publicIp = publicIp;
        this.vmIp = vmIp;
    }

    public Long getNetworkResourceId() {
        return networkResourceId;
    }

    public void setNetworkResourceId(long networkResourceId) {
        this.networkResourceId = networkResourceId;
    }

    public String getNetworkResourceName() {
        return networkResourceName;
    }

    public void setNetworkResourceName(String networkResourceName) {
        this.networkResourceName = networkResourceName;
    }

    public boolean isResourceVpc() {
        return isResourceVpc;
    }

    public void setResourceVpc(boolean resourceVpc) {
        isResourceVpc = resourceVpc;
    }

    public Long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getVmIp() {
        return vmIp;
    }

    public void setVmIp(String vmIp) {
        this.vmIp = vmIp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NsxNetworkCommand that = (NsxNetworkCommand) o;
        return networkResourceId == that.networkResourceId && vmId == that.vmId &&
                Objects.equals(networkResourceName, that.networkResourceName) && Objects.equals(publicIp, that.publicIp)
                && Objects.equals(vmIp, that.vmIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), networkResourceId, networkResourceName, vmId, publicIp, vmIp);
    }
}
