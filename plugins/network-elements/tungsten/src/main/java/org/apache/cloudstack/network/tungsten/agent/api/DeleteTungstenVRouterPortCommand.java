package org.apache.cloudstack.network.tungsten.agent.api;

public class DeleteTungstenVRouterPortCommand extends TungstenCommand {
    private final String host;
    private final String portId;

    public DeleteTungstenVRouterPortCommand(final String host, final String portId) {
        this.host = host;
        this.portId = portId;
    }

    public String getPortId() {
        return portId;
    }

    public String getHost() {
        return host;
    }
}
