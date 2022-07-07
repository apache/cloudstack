package org.apache.cloudstack.backup.networker.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "backup",
        "serverBackup",
        "expire"
})
@Generated("jsonschema2pojo")
public class ActionSpecificData implements Serializable {

    private final static long serialVersionUID = 2969226417055065194L;
    @JsonProperty("backup")
    private Backup backup;
    @JsonProperty("serverBackup")
    private ServerBackup serverBackup;
    @JsonProperty("expire")
    private Expire expire;

    /**
     * No args constructor for use in serialization
     */
    public ActionSpecificData() {
    }

    /**
     * @param backup
     * @param expire
     * @param serverBackup
     */
    public ActionSpecificData(Backup backup, ServerBackup serverBackup, Expire expire) {
        super();
        this.backup = backup;
        this.serverBackup = serverBackup;
        this.expire = expire;
    }

    @JsonProperty("backup")
    public Backup getBackup() {
        return backup;
    }

    @JsonProperty("backup")
    public void setBackup(Backup backup) {
        this.backup = backup;
    }

    @JsonProperty("serverBackup")
    public ServerBackup getServerBackup() {
        return serverBackup;
    }

    @JsonProperty("serverBackup")
    public void setServerBackup(ServerBackup serverBackup) {
        this.serverBackup = serverBackup;
    }

    @JsonProperty("expire")
    public Expire getExpire() {
        return expire;
    }

    @JsonProperty("expire")
    public void setExpire(Expire expire) {
        this.expire = expire;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ActionSpecificData.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("backup");
        sb.append('=');
        sb.append(((this.backup == null) ? "<null>" : this.backup));
        sb.append(',');
        sb.append("serverBackup");
        sb.append('=');
        sb.append(((this.serverBackup == null) ? "<null>" : this.serverBackup));
        sb.append(',');
        sb.append("expire");
        sb.append('=');
        sb.append(((this.expire == null) ? "<null>" : this.expire));
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
        result = ((result * 31) + ((this.backup == null) ? 0 : this.backup.hashCode()));
        result = ((result * 31) + ((this.serverBackup == null) ? 0 : this.serverBackup.hashCode()));
        result = ((result * 31) + ((this.expire == null) ? 0 : this.expire.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ActionSpecificData) == false) {
            return false;
        }
        ActionSpecificData rhs = ((ActionSpecificData) other);
        return ((((this.backup == rhs.backup) || ((this.backup != null) && this.backup.equals(rhs.backup))) && ((this.serverBackup == rhs.serverBackup) || ((this.serverBackup != null) && this.serverBackup.equals(rhs.serverBackup)))) && ((this.expire == rhs.expire) || ((this.expire != null) && this.expire.equals(rhs.expire))));
    }

}
