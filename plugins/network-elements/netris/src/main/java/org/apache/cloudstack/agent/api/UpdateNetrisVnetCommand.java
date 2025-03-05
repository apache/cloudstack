package org.apache.cloudstack.agent.api;

public class UpdateNetrisVnetCommand extends NetrisCommand{
    private String prevNetworkName;

    public UpdateNetrisVnetCommand(long zoneId, Long accountId, Long domainId, String name, Long id, boolean isVpc) {
        super(zoneId, accountId, domainId, name, id, isVpc);
    }

    public String getPrevNetworkName() {
        return prevNetworkName;
    }

    public void setPrevNetworkName(String prevNetworkName) {
        this.prevNetworkName = prevNetworkName;
    }
}
