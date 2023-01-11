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
        "unit",
        "value"
})
@Generated("jsonschema2pojo")
public class Size implements Serializable {

    private final static long serialVersionUID = -8270448162523429622L;
    @JsonProperty("unit")
    private String unit;
    @JsonProperty("value")
    private Long value;
    /**
     * No args constructor for use in serialization
     */
    public Size() {
    }

    /**
     * @param unit
     * @param value
     */
    public Size(String unit, Long value) {
        super();
        this.unit = unit;
        this.value = value;
    }

    @JsonProperty("unit")
    public String getUnit() {
        return unit;
    }

    @JsonProperty("unit")
    public void setUnit(String unit) {
        this.unit = unit;
    }

    @JsonProperty("value")
    public Long getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(Long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        ReflectionToStringBuilderUtils sb = new ReflectionToStringBuilderUtils();
        sb.reflectOnlySelectedFields(this,"unit","value");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.value == null) ? 0 : this.value.hashCode()));
        result = ((result * 31) + ((this.unit == null) ? 0 : this.unit.hashCode()));
        return result;
    }
}
