package org.apache.cloudstack.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

public class CpuSocketsMetricsResponse extends BaseResponse {

    @SerializedName("hypervisorname")
    @Param(description = "Name of hypervisor")
    private String hypervisorname;

    @SerializedName("hostscount")
    @Param(description = "Total number of hosts")
    private Integer hostscount;

    @SerializedName("cpusocketscount")
    @Param(description = "Total number of CPU sockets")
    private Integer cpusocketscount;

    @SerializedName("hypervisorversion")
    @Param(description = "Version of hypervisor")
    private String hypervisorversion;

    public CpuSocketsMetricsResponse() {
        setObjectName("cpuSocketsCount");
    }

    public void setHypervisorname(String hypervisorname) {
        this.hypervisorname = hypervisorname;
    }

    public void setHostscount(Integer hostscount) {
        this.hostscount = hostscount;
    }

    public void setCpusocketscount(Integer cpusocketscount) {
        this.cpusocketscount = cpusocketscount;
    }

    public void setHypervisorversion(String hypervisorversion) {
        this.hypervisorversion = hypervisorversion;
    }
}
