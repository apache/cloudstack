package org.apache.cloudstack.backup.networker.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "count",
        "protectionPolicies"
})
@Generated("jsonschema2pojo")
public class ProtectionPolicies implements Serializable {

    private final static long serialVersionUID = 1357846434531599309L;
    @JsonProperty("count")
    private Integer count;
    @JsonProperty("protectionPolicies")
    private List<ProtectionPolicy> protectionPolicies = null;

    /**
     * No args constructor for use in serialization
     */
    public ProtectionPolicies() {
    }

    /**
     * @param protectionPolicies
     * @param count
     */
    public ProtectionPolicies(Integer count, List<ProtectionPolicy> protectionPolicies) {
        super();
        this.count = count;
        this.protectionPolicies = protectionPolicies;
    }

    @JsonProperty("count")
    public Integer getCount() {
        return count;
    }

    @JsonProperty("count")
    public void setCount(Integer count) {
        this.count = count;
    }

    @JsonProperty("protectionPolicies")
    public List<ProtectionPolicy> getProtectionPolicies() {
        return protectionPolicies;
    }

    @JsonProperty("protectionPolicies")
    public void setProtectionPolicies(List<ProtectionPolicy> protectionPolicies) {
        this.protectionPolicies = protectionPolicies;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ProtectionPolicies.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("count");
        sb.append('=');
        sb.append(((this.count == null) ? "<null>" : this.count));
        sb.append(',');
        sb.append("protectionPolicies");
        sb.append('=');
        sb.append(((this.protectionPolicies == null) ? "<null>" : this.protectionPolicies));
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
        result = ((result * 31) + ((this.count == null) ? 0 : this.count.hashCode()));
        result = ((result * 31) + ((this.protectionPolicies == null) ? 0 : this.protectionPolicies.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ProtectionPolicies) == false) {
            return false;
        }
        ProtectionPolicies rhs = ((ProtectionPolicies) other);
        return (((this.count == rhs.count) || ((this.count != null) && this.count.equals(rhs.count))) && ((this.protectionPolicies == rhs.protectionPolicies) || ((this.protectionPolicies != null) && this.protectionPolicies.equals(rhs.protectionPolicies))));
    }

}
