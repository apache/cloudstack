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
        ReflectionToStringBuilderUtils sb = new ReflectionToStringBuilderUtils();
        sb.reflectOnlySelectedFields(this,"href","rel");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.rel == null) ? 0 : this.rel.hashCode()));
        result = ((result * 31) + ((this.href == null) ? 0 : this.href.hashCode()));
        return result;
    }
}
