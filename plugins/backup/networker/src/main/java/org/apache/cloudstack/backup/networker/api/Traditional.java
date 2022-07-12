// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.


package org.apache.cloudstack.backup.networker.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "forceBackupLevel",
        "browsePeriod",
        "estimate",
        "enableDDRetentionLock",
        "ddRetentionLockTime",
        "destinationPool",
        "timestampFormat",
        "verifySyntheticFull",
        "revertToFullWhenSyntheticFullFails",
        "fileInactivityThresholdInDays",
        "fileInactivityAlertThresholdPercentage"
})
@Generated("jsonschema2pojo")
public class Traditional implements Serializable {

    private final static long serialVersionUID = -6349295255710627836L;
    @JsonProperty("forceBackupLevel")
    private String forceBackupLevel;
    @JsonProperty("browsePeriod")
    private String browsePeriod;
    @JsonProperty("estimate")
    private Boolean estimate;
    @JsonProperty("enableDDRetentionLock")
    private Boolean enableDDRetentionLock;
    @JsonProperty("ddRetentionLockTime")
    private String ddRetentionLockTime;
    @JsonProperty("destinationPool")
    private String destinationPool;
    @JsonProperty("timestampFormat")
    private String timestampFormat;
    @JsonProperty("verifySyntheticFull")
    private Boolean verifySyntheticFull;
    @JsonProperty("revertToFullWhenSyntheticFullFails")
    private Boolean revertToFullWhenSyntheticFullFails;
    @JsonProperty("fileInactivityThresholdInDays")
    private Integer fileInactivityThresholdInDays;
    @JsonProperty("fileInactivityAlertThresholdPercentage")
    private Integer fileInactivityAlertThresholdPercentage;

    /**
     * No args constructor for use in serialization
     */
    public Traditional() {
    }

    /**
     * @param enableDDRetentionLock
     * @param revertToFullWhenSyntheticFullFails
     * @param fileInactivityThresholdInDays
     * @param destinationPool
     * @param fileInactivityAlertThresholdPercentage
     * @param estimate
     * @param ddRetentionLockTime
     * @param timestampFormat
     * @param forceBackupLevel
     * @param verifySyntheticFull
     * @param browsePeriod
     */
    public Traditional(String forceBackupLevel, String browsePeriod, Boolean estimate, Boolean enableDDRetentionLock, String ddRetentionLockTime, String destinationPool, String timestampFormat, Boolean verifySyntheticFull, Boolean revertToFullWhenSyntheticFullFails, Integer fileInactivityThresholdInDays, Integer fileInactivityAlertThresholdPercentage) {
        super();
        this.forceBackupLevel = forceBackupLevel;
        this.browsePeriod = browsePeriod;
        this.estimate = estimate;
        this.enableDDRetentionLock = enableDDRetentionLock;
        this.ddRetentionLockTime = ddRetentionLockTime;
        this.destinationPool = destinationPool;
        this.timestampFormat = timestampFormat;
        this.verifySyntheticFull = verifySyntheticFull;
        this.revertToFullWhenSyntheticFullFails = revertToFullWhenSyntheticFullFails;
        this.fileInactivityThresholdInDays = fileInactivityThresholdInDays;
        this.fileInactivityAlertThresholdPercentage = fileInactivityAlertThresholdPercentage;
    }

    @JsonProperty("forceBackupLevel")
    public String getForceBackupLevel() {
        return forceBackupLevel;
    }

    @JsonProperty("forceBackupLevel")
    public void setForceBackupLevel(String forceBackupLevel) {
        this.forceBackupLevel = forceBackupLevel;
    }

    @JsonProperty("browsePeriod")
    public String getBrowsePeriod() {
        return browsePeriod;
    }

    @JsonProperty("browsePeriod")
    public void setBrowsePeriod(String browsePeriod) {
        this.browsePeriod = browsePeriod;
    }

    @JsonProperty("estimate")
    public Boolean getEstimate() {
        return estimate;
    }

    @JsonProperty("estimate")
    public void setEstimate(Boolean estimate) {
        this.estimate = estimate;
    }

    @JsonProperty("enableDDRetentionLock")
    public Boolean getEnableDDRetentionLock() {
        return enableDDRetentionLock;
    }

    @JsonProperty("enableDDRetentionLock")
    public void setEnableDDRetentionLock(Boolean enableDDRetentionLock) {
        this.enableDDRetentionLock = enableDDRetentionLock;
    }

    @JsonProperty("ddRetentionLockTime")
    public String getDdRetentionLockTime() {
        return ddRetentionLockTime;
    }

    @JsonProperty("ddRetentionLockTime")
    public void setDdRetentionLockTime(String ddRetentionLockTime) {
        this.ddRetentionLockTime = ddRetentionLockTime;
    }

    @JsonProperty("destinationPool")
    public String getDestinationPool() {
        return destinationPool;
    }

    @JsonProperty("destinationPool")
    public void setDestinationPool(String destinationPool) {
        this.destinationPool = destinationPool;
    }

    @JsonProperty("timestampFormat")
    public String getTimestampFormat() {
        return timestampFormat;
    }

    @JsonProperty("timestampFormat")
    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    @JsonProperty("verifySyntheticFull")
    public Boolean getVerifySyntheticFull() {
        return verifySyntheticFull;
    }

    @JsonProperty("verifySyntheticFull")
    public void setVerifySyntheticFull(Boolean verifySyntheticFull) {
        this.verifySyntheticFull = verifySyntheticFull;
    }

    @JsonProperty("revertToFullWhenSyntheticFullFails")
    public Boolean getRevertToFullWhenSyntheticFullFails() {
        return revertToFullWhenSyntheticFullFails;
    }

    @JsonProperty("revertToFullWhenSyntheticFullFails")
    public void setRevertToFullWhenSyntheticFullFails(Boolean revertToFullWhenSyntheticFullFails) {
        this.revertToFullWhenSyntheticFullFails = revertToFullWhenSyntheticFullFails;
    }

    @JsonProperty("fileInactivityThresholdInDays")
    public Integer getFileInactivityThresholdInDays() {
        return fileInactivityThresholdInDays;
    }

    @JsonProperty("fileInactivityThresholdInDays")
    public void setFileInactivityThresholdInDays(Integer fileInactivityThresholdInDays) {
        this.fileInactivityThresholdInDays = fileInactivityThresholdInDays;
    }

    @JsonProperty("fileInactivityAlertThresholdPercentage")
    public Integer getFileInactivityAlertThresholdPercentage() {
        return fileInactivityAlertThresholdPercentage;
    }

    @JsonProperty("fileInactivityAlertThresholdPercentage")
    public void setFileInactivityAlertThresholdPercentage(Integer fileInactivityAlertThresholdPercentage) {
        this.fileInactivityAlertThresholdPercentage = fileInactivityAlertThresholdPercentage;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Traditional.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("forceBackupLevel");
        sb.append('=');
        sb.append(((this.forceBackupLevel == null) ? "<null>" : this.forceBackupLevel));
        sb.append(',');
        sb.append("browsePeriod");
        sb.append('=');
        sb.append(((this.browsePeriod == null) ? "<null>" : this.browsePeriod));
        sb.append(',');
        sb.append("estimate");
        sb.append('=');
        sb.append(((this.estimate == null) ? "<null>" : this.estimate));
        sb.append(',');
        sb.append("enableDDRetentionLock");
        sb.append('=');
        sb.append(((this.enableDDRetentionLock == null) ? "<null>" : this.enableDDRetentionLock));
        sb.append(',');
        sb.append("ddRetentionLockTime");
        sb.append('=');
        sb.append(((this.ddRetentionLockTime == null) ? "<null>" : this.ddRetentionLockTime));
        sb.append(',');
        sb.append("destinationPool");
        sb.append('=');
        sb.append(((this.destinationPool == null) ? "<null>" : this.destinationPool));
        sb.append(',');
        sb.append("timestampFormat");
        sb.append('=');
        sb.append(((this.timestampFormat == null) ? "<null>" : this.timestampFormat));
        sb.append(',');
        sb.append("verifySyntheticFull");
        sb.append('=');
        sb.append(((this.verifySyntheticFull == null) ? "<null>" : this.verifySyntheticFull));
        sb.append(',');
        sb.append("revertToFullWhenSyntheticFullFails");
        sb.append('=');
        sb.append(((this.revertToFullWhenSyntheticFullFails == null) ? "<null>" : this.revertToFullWhenSyntheticFullFails));
        sb.append(',');
        sb.append("fileInactivityThresholdInDays");
        sb.append('=');
        sb.append(((this.fileInactivityThresholdInDays == null) ? "<null>" : this.fileInactivityThresholdInDays));
        sb.append(',');
        sb.append("fileInactivityAlertThresholdPercentage");
        sb.append('=');
        sb.append(((this.fileInactivityAlertThresholdPercentage == null) ? "<null>" : this.fileInactivityAlertThresholdPercentage));
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
        result = ((result * 31) + ((this.enableDDRetentionLock == null) ? 0 : this.enableDDRetentionLock.hashCode()));
        result = ((result * 31) + ((this.revertToFullWhenSyntheticFullFails == null) ? 0 : this.revertToFullWhenSyntheticFullFails.hashCode()));
        result = ((result * 31) + ((this.fileInactivityThresholdInDays == null) ? 0 : this.fileInactivityThresholdInDays.hashCode()));
        result = ((result * 31) + ((this.destinationPool == null) ? 0 : this.destinationPool.hashCode()));
        result = ((result * 31) + ((this.fileInactivityAlertThresholdPercentage == null) ? 0 : this.fileInactivityAlertThresholdPercentage.hashCode()));
        result = ((result * 31) + ((this.estimate == null) ? 0 : this.estimate.hashCode()));
        result = ((result * 31) + ((this.ddRetentionLockTime == null) ? 0 : this.ddRetentionLockTime.hashCode()));
        result = ((result * 31) + ((this.timestampFormat == null) ? 0 : this.timestampFormat.hashCode()));
        result = ((result * 31) + ((this.forceBackupLevel == null) ? 0 : this.forceBackupLevel.hashCode()));
        result = ((result * 31) + ((this.verifySyntheticFull == null) ? 0 : this.verifySyntheticFull.hashCode()));
        result = ((result * 31) + ((this.browsePeriod == null) ? 0 : this.browsePeriod.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Traditional) == false) {
            return false;
        }
        Traditional rhs = ((Traditional) other);
        return ((((((((((((this.enableDDRetentionLock == rhs.enableDDRetentionLock) || ((this.enableDDRetentionLock != null) && this.enableDDRetentionLock.equals(rhs.enableDDRetentionLock))) && ((this.revertToFullWhenSyntheticFullFails == rhs.revertToFullWhenSyntheticFullFails) || ((this.revertToFullWhenSyntheticFullFails != null) && this.revertToFullWhenSyntheticFullFails.equals(rhs.revertToFullWhenSyntheticFullFails)))) && ((this.fileInactivityThresholdInDays == rhs.fileInactivityThresholdInDays) || ((this.fileInactivityThresholdInDays != null) && this.fileInactivityThresholdInDays.equals(rhs.fileInactivityThresholdInDays)))) && ((this.destinationPool == rhs.destinationPool) || ((this.destinationPool != null) && this.destinationPool.equals(rhs.destinationPool)))) && ((this.fileInactivityAlertThresholdPercentage == rhs.fileInactivityAlertThresholdPercentage) || ((this.fileInactivityAlertThresholdPercentage != null) &&
                this.fileInactivityAlertThresholdPercentage.equals(rhs.fileInactivityAlertThresholdPercentage)))) && ((this.estimate == rhs.estimate) || ((this.estimate != null) && this.estimate.equals(rhs.estimate)))) && ((this.ddRetentionLockTime == rhs.ddRetentionLockTime) || ((this.ddRetentionLockTime != null) && this.ddRetentionLockTime.equals(rhs.ddRetentionLockTime)))) && ((this.timestampFormat == rhs.timestampFormat) || ((this.timestampFormat != null) && this.timestampFormat.equals(rhs.timestampFormat)))) && ((this.forceBackupLevel == rhs.forceBackupLevel) || ((this.forceBackupLevel != null) && this.forceBackupLevel.equals(rhs.forceBackupLevel)))) && ((this.verifySyntheticFull == rhs.verifySyntheticFull) || ((this.verifySyntheticFull != null) && this.verifySyntheticFull.equals(rhs.verifySyntheticFull)))) && ((this.browsePeriod == rhs.browsePeriod) || ((this.browsePeriod != null) && this.browsePeriod.equals(rhs.browsePeriod))));
    }

}
