package org.apache.cloudstack.backup.networker.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

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
        StringBuilder sb = new StringBuilder();
        sb.append(ProtectionPolicy.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("comment");
        sb.append('=');
        sb.append(((this.comment == null) ? "<null>" : this.comment));
        sb.append(',');
        sb.append("links");
        sb.append('=');
        sb.append(((this.links == null) ? "<null>" : this.links));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null) ? "<null>" : this.name));
        sb.append(',');
        sb.append("policyProtectionEnable");
        sb.append('=');
        sb.append(((this.policyProtectionEnable == null) ? "<null>" : this.policyProtectionEnable));
        sb.append(',');
        sb.append("policyProtectionPeriod");
        sb.append('=');
        sb.append(((this.policyProtectionPeriod == null) ? "<null>" : this.policyProtectionPeriod));
        sb.append(',');
        sb.append("resourceId");
        sb.append('=');
        sb.append(((this.resourceId == null) ? "<null>" : this.resourceId));
        sb.append(',');
        sb.append("summaryNotification");
        sb.append('=');
        sb.append(((this.summaryNotification == null) ? "<null>" : this.summaryNotification));
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
        result = ((result * 31) + ((this.policyProtectionEnable == null) ? 0 : this.policyProtectionEnable.hashCode()));
        result = ((result * 31) + ((this.policyProtectionPeriod == null) ? 0 : this.policyProtectionPeriod.hashCode()));
        result = ((result * 31) + ((this.summaryNotification == null) ? 0 : this.summaryNotification.hashCode()));
        result = ((result * 31) + ((this.resourceId == null) ? 0 : this.resourceId.hashCode()));
        result = ((result * 31) + ((this.name == null) ? 0 : this.name.hashCode()));
        result = ((result * 31) + ((this.comment == null) ? 0 : this.comment.hashCode()));
        result = ((result * 31) + ((this.links == null) ? 0 : this.links.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ProtectionPolicy) == false) {
            return false;
        }
        ProtectionPolicy rhs = ((ProtectionPolicy) other);
        return (((((((((this.policyProtectionEnable == rhs.policyProtectionEnable) || ((this.policyProtectionEnable != null) && this.policyProtectionEnable.equals(rhs.policyProtectionEnable))) && ((this.policyProtectionPeriod == rhs.policyProtectionPeriod) || ((this.policyProtectionPeriod != null) && this.policyProtectionPeriod.equals(rhs.policyProtectionPeriod)))) && ((this.summaryNotification == rhs.summaryNotification) || ((this.summaryNotification != null) && this.summaryNotification.equals(rhs.summaryNotification)))) &&
                ((this.resourceId == rhs.resourceId) || ((this.resourceId != null) && this.resourceId.equals(rhs.resourceId)))) && ((this.name == rhs.name) || ((this.name != null) && this.name.equals(rhs.name)))) && ((this.comment == rhs.comment) || ((this.comment != null) && this.comment.equals(rhs.comment)))) && ((this.links == rhs.links) || ((this.links != null) && this.links.equals(rhs.links)))) );
    }

}
