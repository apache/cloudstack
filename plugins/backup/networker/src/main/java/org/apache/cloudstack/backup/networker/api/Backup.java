package org.apache.cloudstack.backup.networker.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.io.Serializable;
import java.util.List;

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
public class Backup implements Serializable {

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
    public Backup() {
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
    public Backup(List<Attribute> attributes, String browseTime, String clientHostname, String clientId, String completionTime, String creationTime, Integer fileCount, String id, List<Instance> instances, String level, List<Link> links, String name, String retentionTime, String saveTime, String shortId, Size size, String type) {
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
        StringBuilder sb = new StringBuilder();
        sb.append(Backup.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("attributes");
        sb.append('=');
        sb.append(((this.attributes == null) ? "<null>" : this.attributes));
        sb.append(',');
        sb.append("browseTime");
        sb.append('=');
        sb.append(((this.browseTime == null) ? "<null>" : this.browseTime));
        sb.append(',');
        sb.append("clientHostname");
        sb.append('=');
        sb.append(((this.clientHostname == null) ? "<null>" : this.clientHostname));
        sb.append(',');
        sb.append("clientId");
        sb.append('=');
        sb.append(((this.clientId == null) ? "<null>" : this.clientId));
        sb.append(',');
        sb.append("completionTime");
        sb.append('=');
        sb.append(((this.completionTime == null) ? "<null>" : this.completionTime));
        sb.append(',');
        sb.append("creationTime");
        sb.append('=');
        sb.append(((this.creationTime == null) ? "<null>" : this.creationTime));
        sb.append(',');
        sb.append("fileCount");
        sb.append('=');
        sb.append(((this.fileCount == null) ? "<null>" : this.fileCount));
        sb.append(',');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null) ? "<null>" : this.id));
        sb.append(',');
        sb.append("instances");
        sb.append('=');
        sb.append(((this.instances == null) ? "<null>" : this.instances));
        sb.append(',');
        sb.append("level");
        sb.append('=');
        sb.append(((this.level == null) ? "<null>" : this.level));
        sb.append(',');
        sb.append("links");
        sb.append('=');
        sb.append(((this.links == null) ? "<null>" : this.links));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null) ? "<null>" : this.name));
        sb.append(',');
        sb.append("retentionTime");
        sb.append('=');
        sb.append(((this.retentionTime == null) ? "<null>" : this.retentionTime));
        sb.append(',');
        sb.append("saveTime");
        sb.append('=');
        sb.append(((this.saveTime == null) ? "<null>" : this.saveTime));
        sb.append(',');
        sb.append("shortId");
        sb.append('=');
        sb.append(((this.shortId == null) ? "<null>" : this.shortId));
        sb.append(',');
        sb.append("size");
        sb.append('=');
        sb.append(((this.size == null) ? "<null>" : this.size));
        sb.append(',');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null) ? "<null>" : this.type));
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

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Backup) == false) {
            return false;
        }
        Backup rhs = ((Backup) other);
        return ((((((((((((((((((this.shortId == rhs.shortId) || ((this.shortId != null) && this.shortId.equals(rhs.shortId))) && ((this.clientId == rhs.clientId) || ((this.clientId != null) && this.clientId.equals(rhs.clientId)))) && ((this.browseTime == rhs.browseTime) || ((this.browseTime != null) &&
                this.browseTime.equals(rhs.browseTime)))) && ((this.creationTime == rhs.creationTime) || ((this.creationTime != null) && this.creationTime.equals(rhs.creationTime)))) && ((this.instances == rhs.instances) || ((this.instances != null) && this.instances.equals(rhs.instances)))) && ((this.level == rhs.level) || ((this.level != null) &&
                this.level.equals(rhs.level)))) && ((this.retentionTime == rhs.retentionTime) || ((this.retentionTime != null) && this.retentionTime.equals(rhs.retentionTime)))) && ((this.type == rhs.type) || ((this.type != null) && this.type.equals(rhs.type)))) && ((this.fileCount == rhs.fileCount) || ((this.fileCount != null) && this.fileCount.equals(rhs.fileCount)))) &&
                ((this.clientHostname == rhs.clientHostname) || ((this.clientHostname != null) && this.clientHostname.equals(rhs.clientHostname)))) && ((this.completionTime == rhs.completionTime) || ((this.completionTime != null) && this.completionTime.equals(rhs.completionTime)))) && ((this.size == rhs.size) || ((this.size != null) && this.size.equals(rhs.size)))) &&
                ((this.name == rhs.name) || ((this.name != null) && this.name.equals(rhs.name)))) && ((this.attributes == rhs.attributes) || ((this.attributes != null) && this.attributes.equals(rhs.attributes)))) && ((this.links == rhs.links) || ((this.links != null) && this.links.equals(rhs.links)))) && ((this.id == rhs.id) || ((this.id != null) && this.id.equals(rhs.id)))) &&
                ((this.saveTime == rhs.saveTime) || ((this.saveTime != null) && this.saveTime.equals(rhs.saveTime))));
    }

}
