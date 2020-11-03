package org.apache.cloudstack.network.tungsten.agent.api;

public class CreateTungstenFloatingIpCommand extends TungstenCommand {
    private String projectUuid;
    private String networkUuid;
    private String vmiUuid;
    private String fipName;
    private String name;
    private String publicIp;
    private String privateIp;

    public CreateTungstenFloatingIpCommand(final String projectUuid, final String networkUuid, final String vmiUuid,
        final String fipName, final String name, final String publicIp, final String privateIp) {
        this.projectUuid = projectUuid;
        this.networkUuid = networkUuid;
        this.vmiUuid = vmiUuid;
        this.fipName = fipName;
        this.name = name;
        this.publicIp = publicIp;
        this.privateIp = privateIp;
    }

    public String getProjectUuid() {
        return projectUuid;
    }

    public void setProjectUuid(final String projectUuid) {
        this.projectUuid = projectUuid;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public void setNetworkUuid(final String networkUuid) {
        this.networkUuid = networkUuid;
    }

    public String getVmiUuid() {
        return vmiUuid;
    }

    public void setVmiUuid(final String vmiUuid) {
        this.vmiUuid = vmiUuid;
    }

    public String getFipName() {
        return fipName;
    }

    public void setFipName(final String fipName) {
        this.fipName = fipName;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(final String publicIp) {
        this.publicIp = publicIp;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(final String privateIp) {
        this.privateIp = privateIp;
    }
}
