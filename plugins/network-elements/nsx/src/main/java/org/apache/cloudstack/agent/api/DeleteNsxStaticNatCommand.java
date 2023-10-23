package org.apache.cloudstack.agent.api;

import java.util.Objects;

public class DeleteNsxStaticNatCommand extends NsxCommand {
    private long vpcId;
    private String vpcName;
    public DeleteNsxStaticNatCommand(long domainId, long accountId, long zoneId, long vpcId, String vpcName) {
        super(domainId, accountId, zoneId);
        this.vpcId = vpcId;
        this.vpcName = vpcName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DeleteNsxStaticNatCommand that = (DeleteNsxStaticNatCommand) o;
        return vpcId == that.vpcId && Objects.equals(vpcName, that.vpcName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), vpcId, vpcName);
    }
}
