package org.apache.cloudstack.backup.networker.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "sequence"
})
@Generated("jsonschema2pojo")
public class ResourceId implements Serializable {

    private final static long serialVersionUID = -6987764740605099486L;
    @JsonProperty("id")
    private String id;
    @JsonProperty("sequence")
    private Integer sequence;

    /**
     * No args constructor for use in serialization
     */
    public ResourceId() {
    }

    /**
     * @param sequence
     * @param id
     */
    public ResourceId(String id, Integer sequence) {
        super();
        this.id = id;
        this.sequence = sequence;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("sequence")
    public Integer getSequence() {
        return sequence;
    }

    @JsonProperty("sequence")
    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ResourceId.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null) ? "<null>" : this.id));
        sb.append(',');
        sb.append("sequence");
        sb.append('=');
        sb.append(((this.sequence == null) ? "<null>" : this.sequence));
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
        result = ((result * 31) + ((this.sequence == null) ? 0 : this.sequence.hashCode()));
        result = ((result * 31) + ((this.id == null) ? 0 : this.id.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ResourceId) == false) {
            return false;
        }
        ResourceId rhs = ((ResourceId) other);
        return (((this.sequence == rhs.sequence) || ((this.sequence != null) && this.sequence.equals(rhs.sequence))) && ((this.id == rhs.id) || ((this.id != null) && this.id.equals(rhs.id))));
    }

}
