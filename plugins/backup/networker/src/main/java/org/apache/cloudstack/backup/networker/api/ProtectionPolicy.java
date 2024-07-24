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
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import javax.annotation.Generated;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "comment",
        "links",
        "name",
        "policyProtectionEnable",
        "policyProtectionPeriod",
        "resourceId",
        "summaryNotification",
        "workflows"
})
@Generated("jsonschema2pojo")
public class ProtectionPolicy implements Serializable {

    private final static long serialVersionUID = 5407494949453441445L;
    @JsonProperty("comment")
    private String comment;
    @JsonProperty("links")
    private List<Link> links = null;
    @JsonProperty("name")
    private String name;
    @JsonProperty("policyProtectionEnable")
    private Boolean policyProtectionEnable;
    @JsonProperty("policyProtectionPeriod")
    private String policyProtectionPeriod;
    @JsonProperty("resourceId")
    private ResourceId resourceId;
    @JsonProperty("summaryNotification")
    private SummaryNotification summaryNotification;

    /**
     * No args constructor for use in serialization
     */
    public ProtectionPolicy() {
    }

    /**
     * @param policyProtectionEnable
     * @param policyProtectionPeriod
     * @param summaryNotification
     * @param resourceId
     * @param name
     * @param comment
     * @param links
     */
    public ProtectionPolicy(String comment, List<Link> links, String name, Boolean policyProtectionEnable, String policyProtectionPeriod, ResourceId resourceId, SummaryNotification summaryNotification) {
        super();
        this.comment = comment;
        this.links = links;
        this.name = name;
        this.policyProtectionEnable = policyProtectionEnable;
        this.policyProtectionPeriod = policyProtectionPeriod;
        this.resourceId = resourceId;
        this.summaryNotification = summaryNotification;
    }

    @JsonProperty("comment")
    public String getComment() {
        return comment;
    }

    @JsonProperty("comment")
    public void setComment(String comment) {
        this.comment = comment;
    }

    @JsonProperty("links")
    public List<Link> getLinks() {
        return links;
    }

    @JsonProperty("links")
    public void setLinks(List<Link> links) {
        this.links = links;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("policyProtectionEnable")
    public Boolean getPolicyProtectionEnable() {
        return policyProtectionEnable;
    }

    @JsonProperty("policyProtectionEnable")
    public void setPolicyProtectionEnable(Boolean policyProtectionEnable) {
        this.policyProtectionEnable = policyProtectionEnable;
    }

    @JsonProperty("policyProtectionPeriod")
    public String getPolicyProtectionPeriod() {
        return policyProtectionPeriod;
    }

    @JsonProperty("policyProtectionPeriod")
    public void setPolicyProtectionPeriod(String policyProtectionPeriod) {
        this.policyProtectionPeriod = policyProtectionPeriod;
    }

    @JsonProperty("resourceId")
    public ResourceId getResourceId() {
        return resourceId;
    }

    @JsonProperty("resourceId")
    public void setResourceId(ResourceId resourceId) {
        this.resourceId = resourceId;
    }

    @JsonProperty("summaryNotification")
    public SummaryNotification getSummaryNotification() {
        return summaryNotification;
    }

    @JsonProperty("summaryNotification")
    public void setSummaryNotification(SummaryNotification summaryNotification) {
        this.summaryNotification = summaryNotification;
    }


    @Override
    public String toString() {
        ReflectionToStringBuilderUtils sb = new ReflectionToStringBuilderUtils();
        sb.reflectOnlySelectedFields(this,"comment","links","name","policyProtectionEnable","resourceId",
                "summaryNotification");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.policyProtectionEnable == null) ? 0 : this.policyProtectionEnable.hashCode()));
        result = ((result * 31) + ((this.policyProtectionPeriod == null) ? 0 : this.policyProtectionPeriod.hashCode()));
        result = ((result * 31) + ((this.summaryNotification == null) ? 0 : this.summaryNotification.hashCode()));
        result = ((result * 31) + ((this.resourceId == null) ? 0 : this.resourceId.hashCode()));
        result = ((result * 31) + ((this.name == null) ? 0 : this.name.hashCode()));
        result = ((result * 31) + ((this.comment == null) ? 0 : this.comment.hashCode()));
        result = ((result * 31) + ((this.links == null) ? 0 : this.links.hashCode()));
        return result;
    }
}
