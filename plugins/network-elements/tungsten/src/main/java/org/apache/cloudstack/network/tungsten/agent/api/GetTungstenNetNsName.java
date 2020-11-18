package org.apache.cloudstack.network.tungsten.agent.api;

public class GetTungstenNetNsName extends TungstenCommand {
    private final String projectUuid;
    private final String logicalRouterName;

    public GetTungstenNetNsName(final String projectUuid, final String logicalRouterName) {
        this.projectUuid = projectUuid;
        this.logicalRouterName = logicalRouterName;
    }

    public String getProjectUuid() {
        return projectUuid;
    }

    public String getLogicalRouterName() {
        return logicalRouterName;
    }
}
