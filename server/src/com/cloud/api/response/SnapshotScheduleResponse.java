package com.cloud.api.response;

import java.util.Date;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class SnapshotScheduleResponse implements ResponseObject {
    @Param(name="id")
    private Long id;

    @Param(name="volumeid")
    private Long volumeId;

    @Param(name="snapshotpolicyid")
    private Long snapshotPolicyId;

    @Param(name="scheduled")
    private Date scheduled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId = volumeId;
    }

    public Long getSnapshotPolicyId() {
        return snapshotPolicyId;
    }

    public void setSnapshotPolicyId(Long snapshotPolicyId) {
        this.snapshotPolicyId = snapshotPolicyId;
    }

    public Date getScheduled() {
        return scheduled;
    }

    public void setScheduled(Date scheduled) {
        this.scheduled = scheduled;
    }
}
