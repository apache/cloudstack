package com.cloud.server.api.response.netapp;

import com.cloud.api.ApiConstants;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ListVolumesOnFilerCmdResponse extends BaseResponse {
	@SerializedName(ApiConstants.ID) @Param(description="volume id")
    private Long id;
	
	@SerializedName(ApiConstants.POOL_NAME) @Param(description="pool name")
    private String poolName;
	
	@SerializedName(ApiConstants.IP_ADDRESS) @Param(description="ip address")
    private String ipAddress;
 
	@SerializedName(ApiConstants.AGGREGATE_NAME) @Param(description="Aggregate name")
    private String aggrName;
 
	@SerializedName(ApiConstants.VOLUME_NAME) @Param(description="Volume name")
    private String volumeName;
	
	@SerializedName(ApiConstants.SNAPSHOT_POLICY) @Param(description="snapshot policy")
    private String snapshotPolicy;
	
	@SerializedName(ApiConstants.SNAPSHOT_RESERVATION) @Param(description="snapshot reservation")
    private Integer snapshotReservation;
 
	@SerializedName(ApiConstants.SIZE) @Param(description="volume size")
    private String volumeSize;

	public Long getId() {
		return id;
	}
	
	public String getPoolName() {
		return poolName;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}
	
	public String getAggrName() {
		return aggrName;
	}
	
	public String getVolumeName() {
		return volumeName;
	}
	
	public String getSnapshotPolicy() {
		return snapshotPolicy;
	}
	
	public Integer getSnapshotReservation() {
		return snapshotReservation;
	}
	
	public String getVolumeSize() {
		return volumeSize;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}
	
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	public void setAggrName(String aggrName) {
		this.aggrName = aggrName;
	}
	
	public void setVolumeName(String volumeName) {
		this.volumeName = volumeName;
	}
	
	public void setSnapshotPolicy(String snapshotPolicy) {
		this.snapshotPolicy = snapshotPolicy;
	}
	
	public void setSnapshotReservation(Integer snapshotReservation) {
		this.snapshotReservation = snapshotReservation;
	}
	
	public void setVolumeSize(String size) {
		this.volumeSize = size;
	}
	
}

