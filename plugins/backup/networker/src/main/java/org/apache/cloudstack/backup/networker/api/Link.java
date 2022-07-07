package org.apache.cloudstack.backup.networker.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "href",
        "rel"
})
@Generated("jsonschema2pojo")
public class Link implements Serializable {

    private final static long serialVersionUID = 1110347626425938231L;
    @JsonProperty("href")
    private String href;
    @JsonProperty("rel")
    private String rel;

    /**
     * No args constructor for use in serialization
     */
    public Link() {
    }

    /**
     * @param rel
     * @param href
     */
    public Link(String href, String rel) {
        super();
        this.href = href;
        this.rel = rel;
    }

    @JsonProperty("href")
    public String getHref() {
        return href;
    }

    @JsonProperty("href")
    public void setHref(String href) {
        this.href = href;
    }

    @JsonProperty("rel")
    public String getRel() {
        return rel;
    }

    @JsonProperty("rel")
    public void setRel(String rel) {
        this.rel = rel;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Link.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("href");
        sb.append('=');
        sb.append(((this.href == null) ? "<null>" : this.href));
        sb.append(',');
        sb.append("rel");
        sb.append('=');
        sb.append(((this.rel == null) ? "<null>" : this.rel));
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
        result = ((result * 31) + ((this.rel == null) ? 0 : this.rel.hashCode()));
        result = ((result * 31) + ((this.href == null) ? 0 : this.href.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Link) == false) {
            return false;
        }
        Link rhs = ((Link) other);
        return (((this.rel == rhs.rel) || ((this.rel != null) && this.rel.equals(rhs.rel))) && ((this.href == rhs.href) || ((this.href != null) && this.href.equals(rhs.href))));
    }

}
