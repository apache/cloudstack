package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.Date;

public class BackupRepositoryResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the backup repository")
    private String id;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the Zone ID of the backup repository")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the Zone name of the backup repository")
    private String zoneName;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the backup repository")
    private String name;

    @SerializedName(ApiConstants.ADDRESS)
    @Param(description = "the address / url of the backup repository")
    private String address;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "name of the provider")
    private String providerName;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "backup type")
    private String type;

    @SerializedName(ApiConstants.MOUNT_OPTIONS)
    @Param(description = "mount options for the backup repository")
    private String mountOptions;

    @SerializedName(ApiConstants.CAPACITY_BYTES)
    @Param(description = "capacity of the backup repository")
    private Long capacityBytes;

    @SerializedName("created")
    @Param(description = "the date and time the backup repository was added")
    private Date created;

    public BackupRepositoryResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMountOptions() {
        return mountOptions;
    }

    public void setMountOptions(String mountOptions) {
        this.mountOptions = mountOptions;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getCapacityBytes() {
        return capacityBytes;
    }

    public void setCapacityBytes(Long capacityBytes) {
        this.capacityBytes = capacityBytes;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
