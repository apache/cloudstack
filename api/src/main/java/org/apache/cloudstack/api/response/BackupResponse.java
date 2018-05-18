package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.backup.Backup;

import java.util.List;

@EntityReference(value = Backup.class)
public class BackupResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "internal id of the backup")
    private String id;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "account id")
    private String accountId;

    @SerializedName(ApiConstants.USER_ID)
    @Param(description = "user id")
    private String userId;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "backup name")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "backup description")
    private String description;

    @SerializedName(ApiConstants.PARENT_ID)
    @Param(description = "backup parent id")
    private String parentId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "backup vm id")
    private String vmId;

    @SerializedName(ApiConstants.VOLUME_IDS)
    @Param(description = "backup volume ids")
    private List<String> volumeIds;

    @SerializedName(ApiConstants.STATUS)
    @Param(description = "backup volume ids")
    private Backup.Status status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getVmId() {
        return vmId;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

    public Backup.Status getStatus() {
        return status;
    }

    public void setStatus(Backup.Status status) {
        this.status = status;
    }

    public List<String> getVolumeIds() {
        return volumeIds;
    }

    public void setVolumeIds(List<String> volumeIds) {
        this.volumeIds = volumeIds;
    }
}
