package org.apache.cloudstack.agent.api;

import java.util.Objects;

public class CreateNsxStaticNatCommand extends NsxCommand {

    private long vpcId;
    private String vpcName;
    private long vmId;
    private String publicIp;
    private String vmIp;

    public CreateNsxStaticNatCommand(long domainId, long accountId, long zoneId, long vpcId, String vpcName,
                                     long vmId, String publicIp, String vmIp) {
        super(domainId, accountId, zoneId);
        this.vpcId = vpcId;
        this.vpcName = vpcName;
        this.vmId = vmId;
        this.publicIp = publicIp;
        this.vmIp = vmIp;
    }

    public long getVpcId() {
        return vpcId;
    }

    public void setVpcId(long vpcId) {
        this.vpcId = vpcId;
    }

    public String getVpcName() {
        return vpcName;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }

    public long getVmId() {
        return vmId;
    }

    public void setVmId(long vmId) {
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
        CreateNsxStaticNatCommand that = (CreateNsxStaticNatCommand) o;
        return vpcId == that.vpcId && Objects.equals(publicIp, that.publicIp) && Objects.equals(vmIp, that.vmIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), vpcId, publicIp, vmIp);
    }
}
