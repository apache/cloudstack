package org.apache.cloudstack.backup.networker.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "actionReferSchedule",
        "actionSpecificData",
        "comment",
        "completionNotification",
        "concurrent",
        "drivenBy",
        "enabled",
        "failureImpact",
        "hardLimit",
        "inactivityTimeoutInMin",
        "name",
        "parallelism",
        "retries",
        "retryDelayInSec",
        "softLimit",
        "scheduleActivities",
        "scheduleOverrides",
        "schedulePeriod"
})
@Generated("jsonschema2pojo")
public class Action implements Serializable {

    private final static long serialVersionUID = 1750989315434884936L;
    @JsonProperty("actionReferSchedule")
    private String actionReferSchedule;
    @JsonProperty("actionSpecificData")
    private ActionSpecificData actionSpecificData;
    @JsonProperty("comment")
    private String comment;
    @JsonProperty("completionNotification")
    private CompletionNotification completionNotification;
    @JsonProperty("concurrent")
    private Boolean concurrent;
    @JsonProperty("drivenBy")
    private String drivenBy;
    @JsonProperty("enabled")
    private Boolean enabled;
    @JsonProperty("failureImpact")
    private String failureImpact;
    @JsonProperty("hardLimit")
    private String hardLimit;
    @JsonProperty("inactivityTimeoutInMin")
    private Integer inactivityTimeoutInMin;
    @JsonProperty("name")
    private String name;
    @JsonProperty("parallelism")
    private Integer parallelism;
    @JsonProperty("retries")
    private Integer retries;
    @JsonProperty("retryDelayInSec")
    private Integer retryDelayInSec;
    @JsonProperty("softLimit")
    private String softLimit;
    @JsonProperty("scheduleActivities")
    private List<String> scheduleActivities = null;
    @JsonProperty("scheduleOverrides")
    private List<Object> scheduleOverrides = null;
    @JsonProperty("schedulePeriod")
    private String schedulePeriod;

    /**
     * No args constructor for use in serialization
     */
    public Action() {
    }

    /**
     * @param failureImpact
     * @param actionSpecificData
     * @param completionNotification
     * @param parallelism
     * @param concurrent
     * @param retryDelayInSec
     * @param drivenBy
     * @param enabled
     * @param scheduleActivities
     * @param retries
     * @param actionReferSchedule
     * @param name
     * @param inactivityTimeoutInMin
     * @param comment
     * @param hardLimit
     * @param scheduleOverrides
     * @param schedulePeriod
     * @param softLimit
     */
    public Action(String actionReferSchedule, ActionSpecificData actionSpecificData, String comment, CompletionNotification completionNotification, Boolean concurrent, String drivenBy, Boolean enabled, String failureImpact, String hardLimit, Integer inactivityTimeoutInMin, String name, Integer parallelism, Integer retries, Integer retryDelayInSec, String softLimit, List<String> scheduleActivities, List<Object> scheduleOverrides, String schedulePeriod) {
        super();
        this.actionReferSchedule = actionReferSchedule;
        this.actionSpecificData = actionSpecificData;
        this.comment = comment;
        this.completionNotification = completionNotification;
        this.concurrent = concurrent;
        this.drivenBy = drivenBy;
        this.enabled = enabled;
        this.failureImpact = failureImpact;
        this.hardLimit = hardLimit;
        this.inactivityTimeoutInMin = inactivityTimeoutInMin;
        this.name = name;
        this.parallelism = parallelism;
        this.retries = retries;
        this.retryDelayInSec = retryDelayInSec;
        this.softLimit = softLimit;
        this.scheduleActivities = scheduleActivities;
        this.scheduleOverrides = scheduleOverrides;
        this.schedulePeriod = schedulePeriod;
    }

    @JsonProperty("actionReferSchedule")
    public String getActionReferSchedule() {
        return actionReferSchedule;
    }

    @JsonProperty("actionReferSchedule")
    public void setActionReferSchedule(String actionReferSchedule) {
        this.actionReferSchedule = actionReferSchedule;
    }

    @JsonProperty("actionSpecificData")
    public ActionSpecificData getActionSpecificData() {
        return actionSpecificData;
    }

    @JsonProperty("actionSpecificData")
    public void setActionSpecificData(ActionSpecificData actionSpecificData) {
        this.actionSpecificData = actionSpecificData;
    }

    @JsonProperty("comment")
    public String getComment() {
        return comment;
    }

    @JsonProperty("comment")
    public void setComment(String comment) {
        this.comment = comment;
    }

    @JsonProperty("completionNotification")
    public CompletionNotification getCompletionNotification() {
        return completionNotification;
    }

    @JsonProperty("completionNotification")
    public void setCompletionNotification(CompletionNotification completionNotification) {
        this.completionNotification = completionNotification;
    }

    @JsonProperty("concurrent")
    public Boolean getConcurrent() {
        return concurrent;
    }

    @JsonProperty("concurrent")
    public void setConcurrent(Boolean concurrent) {
        this.concurrent = concurrent;
    }

    @JsonProperty("drivenBy")
    public String getDrivenBy() {
        return drivenBy;
    }

    @JsonProperty("drivenBy")
    public void setDrivenBy(String drivenBy) {
        this.drivenBy = drivenBy;
    }

    @JsonProperty("enabled")
    public Boolean getEnabled() {
        return enabled;
    }

    @JsonProperty("enabled")
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @JsonProperty("failureImpact")
    public String getFailureImpact() {
        return failureImpact;
    }

    @JsonProperty("failureImpact")
    public void setFailureImpact(String failureImpact) {
        this.failureImpact = failureImpact;
    }

    @JsonProperty("hardLimit")
    public String getHardLimit() {
        return hardLimit;
    }

    @JsonProperty("hardLimit")
    public void setHardLimit(String hardLimit) {
        this.hardLimit = hardLimit;
    }

    @JsonProperty("inactivityTimeoutInMin")
    public Integer getInactivityTimeoutInMin() {
        return inactivityTimeoutInMin;
    }

    @JsonProperty("inactivityTimeoutInMin")
    public void setInactivityTimeoutInMin(Integer inactivityTimeoutInMin) {
        this.inactivityTimeoutInMin = inactivityTimeoutInMin;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("parallelism")
    public Integer getParallelism() {
        return parallelism;
    }

    @JsonProperty("parallelism")
    public void setParallelism(Integer parallelism) {
        this.parallelism = parallelism;
    }

    @JsonProperty("retries")
    public Integer getRetries() {
        return retries;
    }

    @JsonProperty("retries")
    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    @JsonProperty("retryDelayInSec")
    public Integer getRetryDelayInSec() {
        return retryDelayInSec;
    }

    @JsonProperty("retryDelayInSec")
    public void setRetryDelayInSec(Integer retryDelayInSec) {
        this.retryDelayInSec = retryDelayInSec;
    }

    @JsonProperty("softLimit")
    public String getSoftLimit() {
        return softLimit;
    }

    @JsonProperty("softLimit")
    public void setSoftLimit(String softLimit) {
        this.softLimit = softLimit;
    }

    @JsonProperty("scheduleActivities")
    public List<String> getScheduleActivities() {
        return scheduleActivities;
    }

    @JsonProperty("scheduleActivities")
    public void setScheduleActivities(List<String> scheduleActivities) {
        this.scheduleActivities = scheduleActivities;
    }

    @JsonProperty("scheduleOverrides")
    public List<Object> getScheduleOverrides() {
        return scheduleOverrides;
    }

    @JsonProperty("scheduleOverrides")
    public void setScheduleOverrides(List<Object> scheduleOverrides) {
        this.scheduleOverrides = scheduleOverrides;
    }

    @JsonProperty("schedulePeriod")
    public String getSchedulePeriod() {
        return schedulePeriod;
    }

    @JsonProperty("schedulePeriod")
    public void setSchedulePeriod(String schedulePeriod) {
        this.schedulePeriod = schedulePeriod;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Action.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("actionReferSchedule");
        sb.append('=');
        sb.append(((this.actionReferSchedule == null) ? "<null>" : this.actionReferSchedule));
        sb.append(',');
        sb.append("actionSpecificData");
        sb.append('=');
        sb.append(((this.actionSpecificData == null) ? "<null>" : this.actionSpecificData));
        sb.append(',');
        sb.append("comment");
        sb.append('=');
        sb.append(((this.comment == null) ? "<null>" : this.comment));
        sb.append(',');
        sb.append("completionNotification");
        sb.append('=');
        sb.append(((this.completionNotification == null) ? "<null>" : this.completionNotification));
        sb.append(',');
        sb.append("concurrent");
        sb.append('=');
        sb.append(((this.concurrent == null) ? "<null>" : this.concurrent));
        sb.append(',');
        sb.append("drivenBy");
        sb.append('=');
        sb.append(((this.drivenBy == null) ? "<null>" : this.drivenBy));
        sb.append(',');
        sb.append("enabled");
        sb.append('=');
        sb.append(((this.enabled == null) ? "<null>" : this.enabled));
        sb.append(',');
        sb.append("failureImpact");
        sb.append('=');
        sb.append(((this.failureImpact == null) ? "<null>" : this.failureImpact));
        sb.append(',');
        sb.append("hardLimit");
        sb.append('=');
        sb.append(((this.hardLimit == null) ? "<null>" : this.hardLimit));
        sb.append(',');
        sb.append("inactivityTimeoutInMin");
        sb.append('=');
        sb.append(((this.inactivityTimeoutInMin == null) ? "<null>" : this.inactivityTimeoutInMin));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null) ? "<null>" : this.name));
        sb.append(',');
        sb.append("parallelism");
        sb.append('=');
        sb.append(((this.parallelism == null) ? "<null>" : this.parallelism));
        sb.append(',');
        sb.append("retries");
        sb.append('=');
        sb.append(((this.retries == null) ? "<null>" : this.retries));
        sb.append(',');
        sb.append("retryDelayInSec");
        sb.append('=');
        sb.append(((this.retryDelayInSec == null) ? "<null>" : this.retryDelayInSec));
        sb.append(',');
        sb.append("softLimit");
        sb.append('=');
        sb.append(((this.softLimit == null) ? "<null>" : this.softLimit));
        sb.append(',');
        sb.append("scheduleActivities");
        sb.append('=');
        sb.append(((this.scheduleActivities == null) ? "<null>" : this.scheduleActivities));
        sb.append(',');
        sb.append("scheduleOverrides");
        sb.append('=');
        sb.append(((this.scheduleOverrides == null) ? "<null>" : this.scheduleOverrides));
        sb.append(',');
        sb.append("schedulePeriod");
        sb.append('=');
        sb.append(((this.schedulePeriod == null) ? "<null>" : this.schedulePeriod));
        sb.append(',');
        if (sb.charAt((sb.length() - 1)) == ',') {
            sb.setCharAt((sb.length() - 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.failureImpact == null) ? 0 : this.failureImpact.hashCode()));
        result = ((result * 31) + ((this.actionSpecificData == null) ? 0 : this.actionSpecificData.hashCode()));
        result = ((result * 31) + ((this.completionNotification == null) ? 0 : this.completionNotification.hashCode()));
        result = ((result * 31) + ((this.parallelism == null) ? 0 : this.parallelism.hashCode()));
        result = ((result * 31) + ((this.concurrent == null) ? 0 : this.concurrent.hashCode()));
        result = ((result * 31) + ((this.retryDelayInSec == null) ? 0 : this.retryDelayInSec.hashCode()));
        result = ((result * 31) + ((this.drivenBy == null) ? 0 : this.drivenBy.hashCode()));
        result = ((result * 31) + ((this.enabled == null) ? 0 : this.enabled.hashCode()));
        result = ((result * 31) + ((this.scheduleActivities == null) ? 0 : this.scheduleActivities.hashCode()));
        result = ((result * 31) + ((this.retries == null) ? 0 : this.retries.hashCode()));
        result = ((result * 31) + ((this.actionReferSchedule == null) ? 0 : this.actionReferSchedule.hashCode()));
        result = ((result * 31) + ((this.name == null) ? 0 : this.name.hashCode()));
        result = ((result * 31) + ((this.inactivityTimeoutInMin == null) ? 0 : this.inactivityTimeoutInMin.hashCode()));
        result = ((result * 31) + ((this.comment == null) ? 0 : this.comment.hashCode()));
        result = ((result * 31) + ((this.hardLimit == null) ? 0 : this.hardLimit.hashCode()));
        result = ((result * 31) + ((this.scheduleOverrides == null) ? 0 : this.scheduleOverrides.hashCode()));
        result = ((result * 31) + ((this.schedulePeriod == null) ? 0 : this.schedulePeriod.hashCode()));
        result = ((result * 31) + ((this.softLimit == null) ? 0 : this.softLimit.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Action) == false) {
            return false;
        }
        Action rhs = ((Action) other);
        return (((((((((((((((((((this.failureImpact == rhs.failureImpact) || ((this.failureImpact != null) && this.failureImpact.equals(rhs.failureImpact))) && ((this.actionSpecificData == rhs.actionSpecificData) || ((this.actionSpecificData != null) && this.actionSpecificData.equals(rhs.actionSpecificData)))) && ((this.completionNotification == rhs.completionNotification) || ((this.completionNotification != null) &&
                this.completionNotification.equals(rhs.completionNotification)))) && ((this.parallelism == rhs.parallelism) || ((this.parallelism != null) && this.parallelism.equals(rhs.parallelism)))) && ((this.concurrent == rhs.concurrent) || ((this.concurrent != null) && this.concurrent.equals(rhs.concurrent)))) && ((this.retryDelayInSec == rhs.retryDelayInSec) || ((this.retryDelayInSec != null) && this.retryDelayInSec.equals(rhs.retryDelayInSec)))) && ((this.drivenBy == rhs.drivenBy) || ((this.drivenBy != null) &&
                this.drivenBy.equals(rhs.drivenBy)))) && ((this.enabled == rhs.enabled) || ((this.enabled != null) && this.enabled.equals(rhs.enabled)))) && ((this.scheduleActivities == rhs.scheduleActivities) || ((this.scheduleActivities != null) && this.scheduleActivities.equals(rhs.scheduleActivities)))) && ((this.retries == rhs.retries) || ((this.retries != null) && this.retries.equals(rhs.retries)))) &&
                ((this.actionReferSchedule == rhs.actionReferSchedule) || ((this.actionReferSchedule != null) && this.actionReferSchedule.equals(rhs.actionReferSchedule)))) && ((this.name == rhs.name) || ((this.name != null) && this.name.equals(rhs.name)))) && ((this.inactivityTimeoutInMin == rhs.inactivityTimeoutInMin) || ((this.inactivityTimeoutInMin != null) && this.inactivityTimeoutInMin.equals(rhs.inactivityTimeoutInMin)))) && ((this.comment == rhs.comment) || ((this.comment != null) &&
                this.comment.equals(rhs.comment)))) && ((this.hardLimit == rhs.hardLimit) || ((this.hardLimit != null) && this.hardLimit.equals(rhs.hardLimit)))) && ((this.scheduleOverrides == rhs.scheduleOverrides) || ((this.scheduleOverrides != null) && this.scheduleOverrides.equals(rhs.scheduleOverrides)))) && ((this.schedulePeriod == rhs.schedulePeriod) || ((this.schedulePeriod != null) && this.schedulePeriod.equals(rhs.schedulePeriod)))) &&
                ((this.softLimit == rhs.softLimit) || ((this.softLimit != null) && this.softLimit.equals(rhs.softLimit))));
    }

}
