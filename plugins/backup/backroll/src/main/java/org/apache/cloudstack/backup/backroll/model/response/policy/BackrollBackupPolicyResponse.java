package org.apache.cloudstack.backup.backroll.model.response.policy;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackrollBackupPolicyResponse {
    @JsonProperty("name")
    public String name;

    @JsonProperty("retention_day")
    public int retentionDay;

    @JsonProperty("schedule")
    public String schedule;

    @JsonProperty("retention_month")
    public int retentionMonth;

    @JsonProperty("storage")
    public String storage;

    @JsonProperty("enabled")
    public Boolean enabled;

    @JsonProperty("description")
    public String description;

    @JsonProperty("id")
    public String id;

    @JsonProperty("retention_week")
    public int retentionWeek;

    @JsonProperty("retention_year")
    public int retentionYear;

    @JsonProperty("externalhook")
    public String externalHook;
}
