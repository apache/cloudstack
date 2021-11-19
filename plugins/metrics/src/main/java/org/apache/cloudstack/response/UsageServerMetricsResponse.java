package org.apache.cloudstack.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.management.ManagementServerHost.State;

import java.util.Date;

public class UsageServerMetricsResponse  extends BaseResponse {
    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "the name of the active usage server")
    private String hostname;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the usage server")
    private State state;

    @SerializedName(ApiConstants.LAST_HEARTBEAT)
    @Param(description = "the last time this Usage Server checked for jobs")
    private Date lastHeartbeat;

    @SerializedName(ApiConstants.LAST_SUCCESFUL_JOB)
    @Param(description = "the last time a usage job succefully completed")
    private Date lastSuccesfulJob;

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setLastHeartbeat(Date lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public void setLastSuccesfulJob(Date lastSuccesfulJob) {
        this.lastSuccesfulJob = lastSuccesfulJob;
    }
}
