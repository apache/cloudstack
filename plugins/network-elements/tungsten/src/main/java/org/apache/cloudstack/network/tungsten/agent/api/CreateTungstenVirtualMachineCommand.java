package org.apache.cloudstack.network.tungsten.agent.api;

public class CreateTungstenVirtualMachineCommand extends TungstenCommand {
    private final String projectUuid;
    private final String vnUuid;
    private final String vmUuid;
    private final String vmName;
    private final String nicUuid;
    private final long nicId;
    private final String ip;
    private final String mac;
    private final String vmType;
    private final String trafficType;
    private final String host;

    public CreateTungstenVirtualMachineCommand(final String projectUuid, final String vnUuid, final String vmUuid,
        final String vmName, final String nicUuid, final long nicId, final String ip, final String mac,
        final String vmType, final String trafficType, final String host) {
        this.projectUuid = projectUuid;
        this.vnUuid = vnUuid;
        this.vmUuid = vmUuid;
        this.vmName = vmName;
        this.nicUuid = nicUuid;
        this.nicId = nicId;
        this.ip = ip;
        this.mac = mac;
        this.vmType = vmType;
        this.trafficType = trafficType;
        this.host = host;
    }

    public String getProjectUuid() {
        return projectUuid;
    }

    public String getVnUuid() {
        return vnUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public String getVmName() {
        return vmName;
    }

    public String getNicUuid() {
        return nicUuid;
    }

    public long getNicId() {
        return nicId;
    }

    public String getIp() {
        return ip;
    }

    public String getMac() {
        return mac;
    }

    public String getVmType() {
        return vmType;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public String getHost() {
        return host;
    }
}