package org.apache.cloudstack.backup.networker.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "command",
        "executeOn"
})
@Generated("jsonschema2pojo")
public class CompletionNotification implements Serializable {

    private final static long serialVersionUID = 3437745338486835459L;
    @JsonProperty("command")
    private String command;
    @JsonProperty("executeOn")
    private String executeOn;

    /**
     * No args constructor for use in serialization
     */
    public CompletionNotification() {
    }

    /**
     * @param executeOn
     * @param command
     */
    public CompletionNotification(String command, String executeOn) {
        super();
        this.command = command;
        this.executeOn = executeOn;
    }

    @JsonProperty("command")
    public String getCommand() {
        return command;
    }

    @JsonProperty("command")
    public void setCommand(String command) {
        this.command = command;
    }

    @JsonProperty("executeOn")
    public String getExecuteOn() {
        return executeOn;
    }

    @JsonProperty("executeOn")
    public void setExecuteOn(String executeOn) {
        this.executeOn = executeOn;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CompletionNotification.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("command");
        sb.append('=');
        sb.append(((this.command == null) ? "<null>" : this.command));
        sb.append(',');
        sb.append("executeOn");
        sb.append('=');
        sb.append(((this.executeOn == null) ? "<null>" : this.executeOn));
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
        result = ((result * 31) + ((this.executeOn == null) ? 0 : this.executeOn.hashCode()));
        result = ((result * 31) + ((this.command == null) ? 0 : this.command.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CompletionNotification) == false) {
            return false;
        }
        CompletionNotification rhs = ((CompletionNotification) other);
        return (((this.executeOn == rhs.executeOn) || ((this.executeOn != null) && this.executeOn.equals(rhs.executeOn))) && ((this.command == rhs.command) || ((this.command != null) && this.command.equals(rhs.command))));
    }

}
