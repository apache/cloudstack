package org.apache.cloudstack.backup.backroll.model.response.policy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BackupPoliciesResponse {
    @JsonProperty("state")
    public String state;

    @JsonProperty("info")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public List<BackrollBackupPolicyResponse> backupPolicies;
}
