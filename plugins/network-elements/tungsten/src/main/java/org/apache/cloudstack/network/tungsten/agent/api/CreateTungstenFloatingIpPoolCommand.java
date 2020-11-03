package org.apache.cloudstack.network.tungsten.agent.api;

public class CreateTungstenFloatingIpPoolCommand extends TungstenCommand {
    private String networkUuid;
    private String fipName;

    public CreateTungstenFloatingIpPoolCommand(final String networkUuid, final String fipName) {
        this.networkUuid = networkUuid;
        this.fipName = fipName;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public void setNetworkUuid(final String networkUuid) {
        this.networkUuid = networkUuid;
    }

    public String getFipName() {
        return fipName;
    }

    public void setFipName(final String fipName) {
        this.fipName = fipName;
    }
}
