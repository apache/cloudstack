package org.apache.cloudstack.network.tungsten.agent.api;

public class DeleteTungstenLogicalRouterCommand extends TungstenCommand {
    private final long id;
    private final String projectUuid;

    public DeleteTungstenLogicalRouterCommand(final long id, final String projectUuid) {
        this.id = id;
        this.projectUuid = projectUuid;
    }

    public long getId() {
        return id;
    }

    public String getProjectUuid() {
        return projectUuid;
    }
}