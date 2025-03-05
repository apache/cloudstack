package org.apache.cloudstack.agent.api;

public class UpdateNetrisVnetCommand extends NetrisCommand{
    private String prevNetworkName;
    private String vpcName;
    private Long vpcId;

    public UpdateNetrisVnetCommand(long zoneId, Long accountId, Long domainId, String name, Long id, boolean isVpc) {
        super(zoneId, accountId, domainId, name, id, isVpc);
    }

    public String getPrevNetworkName() {
        return prevNetworkName;
    }

    public void setPrevNetworkName(String prevNetworkName) {
        this.prevNetworkName = prevNetworkName;
    }

    public String getVpcName() {
        return vpcName;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public void setVpcId(Long vpcId) {
        this.vpcId = vpcId;
    }
}
