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
import java.util.List;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "attributes",
        "browseTime",
        "clientHostname",
        "clientId",
        "completionTime",
        "creationTime",
        "fileCount",
        "id",
        "instances",
        "level",
        "links",
        "name",
        "retentionTime",
        "saveTime",
        "shortId",
        "size",
        "type"
})
@Generated("jsonschema2pojo")
public class NetworkerBackup implements Serializable {

    private final static long serialVersionUID = -4474500098917286405L;
    @JsonProperty("attributes")
    private List<Attribute> attributes = null;
    @JsonProperty("browseTime")
    private String browseTime;
    @JsonProperty("clientHostname")
    private String clientHostname;
    @JsonProperty("clientId")
    private String clientId;
    @JsonProperty("completionTime")
    private String completionTime;
    @JsonProperty("creationTime")
    private String creationTime;
    @JsonProperty("fileCount")
    private Integer fileCount;
    @JsonProperty("id")
    private String id;
    @JsonProperty("instances")
    private List<Instance> instances = null;
    @JsonProperty("level")
    private String level;
    @JsonProperty("links")
    private List<Link> links = null;
    @JsonProperty("name")
    private String name;
    @JsonProperty("retentionTime")
    private String retentionTime;
    @JsonProperty("saveTime")
    private String saveTime;
    @JsonProperty("shortId")
    private String shortId;
    @JsonProperty("size")
    private Size size;
    @JsonProperty("type")
    private String type;

    /**
     * No args constructor for use in serialization
     */
    public NetworkerBackup() {
    }

    /**
     * @param shortId
     * @param clientId
     * @param browseTime
     * @param creationTime
     * @param instances
     * @param level
     * @param retentionTime
     * @param type
     * @param fileCount
     * @param clientHostname
     * @param completionTime
     * @param size
     * @param name
     * @param attributes
     * @param links
     * @param id
     * @param saveTime
     */
    public NetworkerBackup(List<Attribute> attributes, String browseTime, String clientHostname, String clientId, String completionTime, String creationTime, Integer fileCount, String id, List<Instance> instances, String level, List<Link> links, String name, String retentionTime, String saveTime, String shortId, Size size, String type) {
        super();
        this.attributes = attributes;
        this.browseTime = browseTime;
        this.clientHostname = clientHostname;
        this.clientId = clientId;
        this.completionTime = completionTime;
        this.creationTime = creationTime;
        this.fileCount = fileCount;
        this.id = id;
        this.instances = instances;
        this.level = level;
        this.links = links;
        this.name = name;
        this.retentionTime = retentionTime;
        this.saveTime = saveTime;
        this.shortId = shortId;
        this.size = size;
        this.type = type;
    }

    @JsonProperty("attributes")
    public List<Attribute> getAttributes() {
        return attributes;
    }

    @JsonProperty("attributes")
    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    @JsonProperty("browseTime")
    public String getBrowseTime() {
        return browseTime;
    }

    @JsonProperty("browseTime")
    public void setBrowseTime(String browseTime) {
        this.browseTime = browseTime;
    }

    @JsonProperty("clientHostname")
    public String getClientHostname() {
        return clientHostname;
    }

    @JsonProperty("clientHostname")
    public void setClientHostname(String clientHostname) {
        this.clientHostname = clientHostname;
    }

    @JsonProperty("clientId")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("clientId")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty("completionTime")
    public String getCompletionTime() {
        return completionTime;
    }

    @JsonProperty("completionTime")
    public void setCompletionTime(String completionTime) {
        this.completionTime = completionTime;
    }

    @JsonProperty("creationTime")
    public String getCreationTime() {
        return creationTime;
    }

    @JsonProperty("creationTime")
    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    @JsonProperty("fileCount")
    public Integer getFileCount() {
        return fileCount;
    }

    @JsonProperty("fileCount")
    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("instances")
    public List<Instance> getInstances() {
        return instances;
    }

    @JsonProperty("instances")
    public void setInstances(List<Instance> instances) {
        this.instances = instances;
    }

    @JsonProperty("level")
    public String getLevel() {
        return level;
    }

    @JsonProperty("level")
    public void setLevel(String level) {
        this.level = level;
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

    @JsonProperty("retentionTime")
    public String getRetentionTime() {
        return retentionTime;
    }

    @JsonProperty("retentionTime")
    public void setRetentionTime(String retentionTime) {
        this.retentionTime = retentionTime;
    }

    @JsonProperty("saveTime")
    public String getSaveTime() {
        return saveTime;
    }

    @JsonProperty("saveTime")
    public void setSaveTime(String saveTime) {
        this.saveTime = saveTime;
    }

    @JsonProperty("shortId")
    public String getShortId() {
        return shortId;
    }

    @JsonProperty("shortId")
    public void setShortId(String shortId) {
        this.shortId = shortId;
    }

    @JsonProperty("size")
    public Size getSize() {
        return size;
    }

    @JsonProperty("size")
    public void setSize(Size size) {
        this.size = size;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        ReflectionToStringBuilderUtils sb = new ReflectionToStringBuilderUtils();
        sb.reflectOnlySelectedFields(this,"attributes","browseTime","clientHostname","clientId",
                "completionTime","creationTime","fileCount","id","instances","level","links","name","retentionTime",
                "saveTime","shortId","size","type");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.shortId == null) ? 0 : this.shortId.hashCode()));
        result = ((result * 31) + ((this.clientId == null) ? 0 : this.clientId.hashCode()));
        result = ((result * 31) + ((this.browseTime == null) ? 0 : this.browseTime.hashCode()));
        result = ((result * 31) + ((this.creationTime == null) ? 0 : this.creationTime.hashCode()));
        result = ((result * 31) + ((this.instances == null) ? 0 : this.instances.hashCode()));
        result = ((result * 31) + ((this.level == null) ? 0 : this.level.hashCode()));
        result = ((result * 31) + ((this.retentionTime == null) ? 0 : this.retentionTime.hashCode()));
        result = ((result * 31) + ((this.type == null) ? 0 : this.type.hashCode()));
        result = ((result * 31) + ((this.fileCount == null) ? 0 : this.fileCount.hashCode()));
        result = ((result * 31) + ((this.clientHostname == null) ? 0 : this.clientHostname.hashCode()));
        result = ((result * 31) + ((this.completionTime == null) ? 0 : this.completionTime.hashCode()));
        result = ((result * 31) + ((this.size == null) ? 0 : this.size.hashCode()));
        result = ((result * 31) + ((this.name == null) ? 0 : this.name.hashCode()));
        result = ((result * 31) + ((this.attributes == null) ? 0 : this.attributes.hashCode()));
        result = ((result * 31) + ((this.links == null) ? 0 : this.links.hashCode()));
        result = ((result * 31) + ((this.id == null) ? 0 : this.id.hashCode()));
        result = ((result * 31) + ((this.saveTime == null) ? 0 : this.saveTime.hashCode()));
        return result;
    }
}
