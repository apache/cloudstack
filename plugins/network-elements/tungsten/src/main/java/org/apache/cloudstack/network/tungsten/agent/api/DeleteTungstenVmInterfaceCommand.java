package org.apache.cloudstack.network.tungsten.agent.api;

public class DeleteTungstenVmInterfaceCommand extends TungstenCommand {
    private final String projectUuid;
    private final String name;

    public DeleteTungstenVmInterfaceCommand(final String projectUuid, String name) {
        this.projectUuid = projectUuid;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getProjectUuid() {
        return projectUuid;
    }
}
