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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "traditional"
})
@Generated("jsonschema2pojo")
public class BackupSpecificData implements Serializable {

    private final static long serialVersionUID = -73775366615011864L;
    @JsonProperty("traditional")
    private Traditional traditional;

    /**
     * No args constructor for use in serialization
     */
    public BackupSpecificData() {
    }

    /**
     * @param traditional
     */
    public BackupSpecificData(Traditional traditional) {
        super();
        this.traditional = traditional;
    }

    @JsonProperty("traditional")
    public Traditional getTraditional() {
        return traditional;
    }

    @JsonProperty("traditional")
    public void setTraditional(Traditional traditional) {
        this.traditional = traditional;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(BackupSpecificData.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("traditional");
        sb.append('=');
        sb.append(((this.traditional == null) ? "<null>" : this.traditional));
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
        result = ((result * 31) + ((this.traditional == null) ? 0 : this.traditional.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof BackupSpecificData) == false) {
            return false;
        }
        BackupSpecificData rhs = ((BackupSpecificData) other);
        return ((this.traditional == rhs.traditional) || ((this.traditional != null) && this.traditional.equals(rhs.traditional)));
    }

}
