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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "command",
        "executeOn"
})
@Generated("jsonschema2pojo")
public class SummaryNotification implements Serializable {

    private final static long serialVersionUID = -1142867788732047105L;
    @JsonProperty("command")
    private String command;
    @JsonProperty("executeOn")
    private String executeOn;

    /**
     * No args constructor for use in serialization
     */
    public SummaryNotification() {
    }

    /**
     * @param executeOn
     * @param command
     */
    public SummaryNotification(String command, String executeOn) {
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
        ReflectionToStringBuilderUtils sb = new ReflectionToStringBuilderUtils();
        sb.reflectOnlySelectedFields(this,"command","executeOn");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.executeOn == null) ? 0 : this.executeOn.hashCode()));
        result = ((result * 31) + ((this.command == null) ? 0 : this.command.hashCode()));
        return result;
    }
}
