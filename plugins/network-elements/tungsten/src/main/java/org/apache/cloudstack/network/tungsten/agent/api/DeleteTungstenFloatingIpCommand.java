package org.apache.cloudstack.network.tungsten.agent.api;

public class DeleteTungstenFloatingIpCommand extends TungstenCommand {
    private final String projectUuid;
    private final String vnUuid;
    private final String fipName;
    private final String name;

    public DeleteTungstenFloatingIpCommand(final String projectUuid, String vnUuid, final String fipName, final String name) {
        this.projectUuid = projectUuid;
        this.vnUuid = vnUuid;
        this.fipName = fipName;
        this.name = name;
    }

    public String getProjectUuid() {
        return projectUuid;
    }

    public String getVnUuid() {
        return vnUuid;
    }

    public String getFipName() {
        return fipName;
    }

    public String getName() {
        return name;
    }
}
