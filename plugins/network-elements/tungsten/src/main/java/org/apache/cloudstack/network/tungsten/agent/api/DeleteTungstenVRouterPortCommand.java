package org.apache.cloudstack.network.tungsten.agent.api;

public class DeleteTungstenVRouterPortCommand extends TungstenCommand {
    private final String portId;

    public DeleteTungstenVRouterPortCommand(final String portId) {
        this.portId = portId;
    }

    public String getPortId() {
        return portId;
    }
}
